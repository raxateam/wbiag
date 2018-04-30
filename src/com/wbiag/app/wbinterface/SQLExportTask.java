package com.wbiag.app.wbinterface;

import java.io.*;
import java.sql.*;
import java.util.Map;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
import com.workbrain.sql.SQLHelper;
import com.workbrain.security.SecurityService;

public class SQLExportTask extends AbstractScheduledJob {

    public static final String SQL_PARAM = "SQL_PARAM";
    public static final String FILE_NAME_PARAM = "FILE_NAME_PARAM";
    public static final String TOSTAMP_PARAM = "TOSTAMP_PARAM";
    public static final String PERF_DIFF_PARAM = "PERF_DIFF_PARAM";
    public static final String ENCLOSE_PARAM = "ENCLOSE_PARAM";
    /**
     * Delimiter between fields
     */
    public static final String DELIMITER_PARAM = "DELIMITER_PARAM";
    /**
     * Enclosing char around fields
     */
    public static final String CLIENT_ID_PARAM = "CLIENT_ID";
    private static final boolean isUTF8Database = SystemProperties.isUTF8Database();
    private final String DATE_FORMAT_STRING = "yyyyMMdd_HHmmss";

    public SQLExportTask() {
    }

    public String getTaskUI() {
        return "/jobs/wbiag/SQLExportParams.jsp";
    }

    public Status run(int taskID, Map param) throws Exception {
        DBConnection conn = null;
        try {
            conn = getConnection();

            String sSql = (String) param.get(SQL_PARAM);
            String fileName = (String) param.get(FILE_NAME_PARAM);
            String toStampStr = (String) param.get(TOSTAMP_PARAM);
            boolean toStamp = "Y".equals(toStampStr) ? true : false;
            // **** tt9690 perform Difference only if param is supplied and true
            boolean performsDifference = false;
            if (param.get(PERF_DIFF_PARAM) != null) {
                String pdStr = (String) param.get(PERF_DIFF_PARAM);
                performsDifference = "Y".equals(pdStr) ? true : false;
            }

            int clientId = Integer.parseInt( (String) param.get( CLIENT_ID_PARAM ));            
            String enclose = (String) param.get(ENCLOSE_PARAM);            
            String delimiter = (String) param.get(DELIMITER_PARAM);
            if (StringHelper.isEmpty(delimiter)) {
                throw new WBInterfaceException("Field delimiter must be defined");
            }
            
            export( conn, sSql, fileName, toStamp , performsDifference , 
                    enclose, delimiter, clientId);

            if (!isInterrupted()){
                conn.commit(); 
                return jobOk( "CSVExportTask task finished successfully." );
            }
            else{ 
                conn.rollback();
                return jobInterrupted("CSVExportTask task has been interrupted.");
            }
        } catch (Exception e) {
            if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) { getLogger().error(e);}
            conn.rollback();
            throw(e);
        } finally {
            releaseConnection();
        }
    }


    public void export( DBConnection conn, String sSql, String fileName,
            boolean toStamp , boolean performsDifference, 
            String enclose, String delimiter, int clientId) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("CSVExportTask.export(" + sSql + "," +
                           fileName + "," + toStamp + ")" );
        }
        Statement stmt = null;
        ResultSet rs = null;
        // *** preserve current client ID
        String currentClientId = SecurityService.getCurrentClientId();
        SecurityService.setCurrentClientId(String.valueOf(clientId));

        try {
            File file = FileUtil.createFileWithDir(fileName);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sSql);
            PrintWriter writer;
            if (isUTF8Database) {
                FileOutputStream fos = new FileOutputStream(file);
                UTF8Writer utf8w = new UTF8Writer(fos);
                writer = new PrintWriter(utf8w);
            }
            else {
                writer = new PrintWriter(new FileOutputStream(file));
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Exporting to " + file.getAbsolutePath());
            }
            exportData(rs, writer, enclose, delimiter);
            if (performsDifference) {
                if (isInterrupted()) {
                    return;
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Performing difference with most recent export data");
                }
                WBInterfaceUtil.performDifferenceUnderNewCompare(fileName, isUTF8Database);
            }
            if (toStamp) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Timestamping the file " + fileName);
                }
                FileUtil.timeStampFile(fileName , DATE_FORMAT_STRING);
            }
        } finally {
            SQLHelper.cleanUp(stmt, rs);
            SecurityService.setCurrentClientId(currentClientId);
        }
    }

    /**
     * Returns the number of records for which database commit will be issued
     *
     *@return   int
     */
    protected int getCommitBatchSize() {
        return 1000;
    }

    public void exportData(ResultSet rs, PrintWriter writer, String enclose, String delimiter) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int nRec = 0;
        int checkInterruptedBatchSize = getCommitBatchSize();
        while (rs.next()) {
            if (nRec++ % checkInterruptedBatchSize == 0){
                if (isInterrupted())                
                    break ;                    
            }
            for (int i = 0, j = md.getColumnCount(); i < j; i++) {
                String fieldValue = rs.getString(i + 1);
                if (!StringHelper.isEmpty(enclose)) {
                    if ("\"".equals(enclose)) {
                        fieldValue = StringHelper.searchReplace(rs.getString(i + 1), "\"", "\"\"");
                    }
                    fieldValue = enclose + fieldValue + enclose;
                }
                if (i > 0 && !StringHelper.isEmpty(delimiter)) {
                    writer.print(",");
                }
                writer.print(fieldValue);
            }
            writer.println();
        }
        writer.flush();
        writer.close();
    }

}
