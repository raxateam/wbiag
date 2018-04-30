package com.wbiag.app.wbinterface;

import java.io.*;
import java.sql.*;
import java.util.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.util.*;
import com.workbrain.sql.SQLHelper;
import com.workbrain.security.SecurityService;
import com.workbrain.server.registry.Registry;

/**
 * A scheduled task importing external data. This class uses DelimetedInputStream
 * to read data from external source and takes the delimiter as parameter
 */
public class GenericDelimitedImportTask extends AbstractImportTask {

    public static final String PARAM_DELIMITER = "delimiter";    
    public static final String PARAM_DEL_VAL_TAB = "TAB";
    public static final String PARAM_DEL_VAL_NEW_LINE_FEED = "NEW_LINE_FEED";
    public static final String PARAM_DEL_VAL_CARRIAGE_RETURN = "CARRIAGE_RETURN";
    public static final String PARAM_DEL_VAL_BACK_SLASH = "BACK_SLASH";
    public static final String PARAM_SORT_FILES_BY = "sortFilesBy";
    public static final String PARAM_SORT_VAL_DATE = "DATE";
    public static final String PARAM_SORT_VAL_NAME_ASC = "NAME_ASC";    
    public static final String PARAM_SORT_VAL_NAME_DESC = "NAME_DESC";
    
    public GenericDelimitedImportTask() {}

    public Status run(int taskID, Map param) throws Exception {
        DBConnection conn = null;
        String originalCurrentClientId = SecurityService.getCurrentClientId();

        try {
            conn = getConnection();
            conn.turnTraceOff();
            conn.setAutoCommit(false);

            int typeId = Integer.parseInt(String.valueOf(param.get(TYPE_ID_PARAM_NAME)));
            String typeName = getTransactionTypeString( conn, typeId );

            Class c = Class.forName(String.valueOf(param.get(TRANSFORMER_CLASS_PARAM_NAME )));
            ImportTransformer transformer = (ImportTransformer) c.newInstance();

            String clientId = String.valueOf(param.get( CLIENT_ID_PARAM_NAME ));
            SecurityService.setCurrentClientId(clientId);

            String fileName = String.valueOf(param.get( FILENAME_PARAM_NAME )) ;

            // *** tt9690 perform Difference only if param is supplied and true
            boolean performsDifference = false;
            if (param.containsKey(PERFORMANCE_DIFFERENCE_PARAM_NAME)) {
            	performsDifference = (Boolean.valueOf(String.valueOf(param.get(PERFORMANCE_DIFFERENCE_PARAM_NAME)))).booleanValue();
            }

            String delimiter = String.valueOf(param.get( PARAM_DELIMITER )) ;
            if (delimiter == null || delimiter == "") {
                logError("Delimiter was not supplied");
                conn.rollback();
                return jobFailed( "GenericDelimitedImportTask task failed. " + taskLogMessage.toString() );
            }
            String orderFilesBy = String.valueOf(param.get( PARAM_DELIMITER )) ;
            if (!StringHelper.isEmpty(orderFilesBy)) {
                orderFilesBy = PARAM_SORT_VAL_DATE;
            }
            List transactionList = importFileOneClient( conn, transformer,
                typeName, fileName,
                performsDifference , Integer.parseInt(clientId), delimiter, orderFilesBy);

            //commit the import
            if (!isInterrupted()){
                if (transactionList == null || transactionList.size() == 0) {
                    logError("No Files found for GenericDelimitedImportTask: " + taskID);
                    conn.rollback();
                    return jobFailed( "GenericDelimitedImportTask task failed. "+taskLogMessage.toString() );
                }
                // check if there is any task failed
                boolean taskFailed = false;
                Iterator iterator = transactionList.iterator();
                while (iterator.hasNext()) {
                    TransactionData tranData = (TransactionData) iterator.next();
                    if (tranData.getWbitranStatus() == TransactionData.STATUS_FAILURE) {
                        taskFailed = true;
                        break;
                    }
                }
                if (!taskFailed) {
                    conn.commit();
                    return jobOk( "GenericDelimitedImportTask task finished successfully. "+taskLogMessage.toString() );
                } else {
                    conn.rollback();
                    return jobFailed( "GenericDelimitedImportTask task failed. "+taskLogMessage.toString() );
                }
            } else {
                conn.rollback();
                return jobInterrupted("GenericDelimitedImportTask task has been interrupted.");
            }
        } catch (Exception e) {
            if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) { getLogger().error("com.workbrain.app.wbinterface.GenericDelimitedImportTask.class" , e);}
            conn.rollback();
            throw e;
        } finally {
            SecurityService.setCurrentClientId(originalCurrentClientId);
            releaseConnection();
        }
    }

    /**
     * Imports file(s) for one client by locating
     * the file from fileName absolute path (i.e \\interfaces\hrFile.txt). <p>
     * If fileName is a directory, all files under fileName are processed in chronological order.
     *
     *@param conn                 DBConnection
     *@param transformer          transformer class
     *@param filePath             root path where client directories reside
     *@param fileName             filename
     *@param typeName             transaction type name
     *@param performsDifference   whether perform difference will be done
     *@param clientId             client Id which transactions will be created with
     *@return  List list of TransactionData created
     *@throws SQLException
     *@throws IOException
     */
    public List importFileOneClient( DBConnection conn, ImportTransformer transformer,
            String typeName, String fileName, boolean performsDifference,
            int clientId, String delimiter, String orderFilesBy)  throws SQLException, IOException {
        return importFile( conn, transformer, null, fileName,
                          typeName , performsDifference, new Integer(clientId),
                          true, delimiter, orderFilesBy);
    }


    /**
     * Imports file(s) for all clients in the system by searching
     * client directories under filePath (i.e \\interfaces\). <p>
     * If fileName (i.e hrFile.txt) is supplied, exact file name is searched under filePath\ClientX. <p>
     * If fileName is not supplied, all files under filePath\ClientX are processed in chronological order.
     *
     *@param conn                 DBConnection
     *@param transformer          transformer class
     *@param filePath             root path where client directories reside
     *@param fileName             filename
     *@param typeName             transaction type name
     *@param performsDifference   whether perform difference will be done
     *@return  List list of TransactionData created
     *@throws SQLException
     *@throws IOException
     */
    public List importFileMultiClient( DBConnection conn, ImportTransformer transformer,
            String typeName, String filePath, String fileName,
            boolean performsDifference, String delimiter, String orderFilesBy)  throws SQLException, IOException {
        return importFile( conn, transformer, filePath, fileName,
                          typeName , performsDifference, null, false, delimiter, orderFilesBy);
    }


    /**
     * Imports file(s) in a directory/path specified. <p>
     * If isOneClient is true , clientId has to be supplied and fileName has to be full path of the file. <p>
     * If isOneClient is false , since the interface will be run for all clients,
     *    both filePath and  fileName have to be supplied in order to search files under filePaths/clientX.
     *
     *@param conn                 DBConnection
     *@param transformer          transformer class
     *@param filePath             root path where client directories reside
     *@param fileName             filename
     *@param typeName             transaction type name
     *@param performsDifference   whether perform difference will be done
     *@param clientId             clientId, required if isOneClient is true
     *@param isOneClient          whether to run for all clients or one client
     *@return  List list of TransactionData created
     *@throws SQLException
     *@throws IOException
     */
    protected List importFile( DBConnection conn, ImportTransformer transformer,
            String filePath, String fileName, String typeName , boolean performsDifference,
            Integer clientId, boolean isOneClient, String delimiter, String orderFilesBy)
            throws SQLException, IOException {
        log("GenericDelimitedImportTask.importFile(" + transformer.getClass().getName()
            + "," + filePath + "," + fileName + "," + typeName + "," + performsDifference);

        super.validateImportParameters(conn , transformer , filePath,
                fileName , typeName , clientId, isOneClient);

        List retTrans = new ArrayList();
        String currentClientId = null;
        try {
            // *** store current client id
            currentClientId = SecurityService.getCurrentClientId();

            importAccess = new ImportAccess( conn );
            Map clients =  getClients(conn , clientId);
            Iterator it =  clients.entrySet().iterator();

            while (it.hasNext()) {

                Map.Entry client = (Map.Entry) it.next();
                String clientName = client.getKey().toString() + File.separator;
                SecurityService.setCurrentClientId(client.getValue().toString());
                String newPath = !StringHelper.isEmpty(filePath)
                                 ? filePath +
                                     (filePath.endsWith(File.separator) ? "" : File.separator) +
                                     (isOneClient ? "" : clientName)
                                 : "";
                String fullNewPath = newPath + (StringHelper.isEmpty(fileName) ? "" : fileName);
                File importPathFile = new File (fullNewPath);

                if( importPathFile.isDirectory()) {
                    File files[] = sortFilesUnderDir(fullNewPath, orderFilesBy);
                    int thisProcessedFileCount = 0;
                    for( int i = 0; i < files.length; i++ ) {
                        File file = files[i];
                        if( file.isFile() && !file.getName().toUpperCase().endsWith( "BAK" )) {
                            String dirFullNewPath = fullNewPath
                                    + (fullNewPath.endsWith(File.separator)
                                            ? "" : File.separator) + file.getName();
                            TransactionData trans = processOneFile(conn ,
                                    transformer , typeName ,
                                    dirFullNewPath , file ,
                                    performsDifference, delimiter );
                            retTrans.add(trans);
                            thisProcessedFileCount++;
                            if( isInterrupted() ) {
                                break;
                            }
                        }
                    }
                    if (thisProcessedFileCount == 0) {
                        appendToTaskLogMessage(" No Files found for client : "
                                + client.getKey().toString()
                                + " under folder : " + fullNewPath);
                    }
                } else {
                    if (importPathFile.exists()) {
                        TransactionData trans = processOneFile(conn ,
                                transformer , typeName ,
                                fullNewPath , importPathFile ,
                                performsDifference, delimiter);
                        retTrans.add(trans);
                    } else {
                        String logMsg = " File not found : " + fullNewPath +
                                " for client : " + client.getKey().toString();

                        // *** 12864, do not throw exception
                        appendToTaskLogMessage(logMsg) ;

                        // TT 32808: Add entry to import transaction table
                        retTrans.add(createFailedTransaction(typeName, fullNewPath, logMsg));
                    }
                }
                if( isInterrupted() ) {
                    break;
                }
            }
        }finally {
            // *** restore client ID
            if (currentClientId != null) SecurityService.setCurrentClientId(currentClientId);
        }
        return retTrans;
    }


    private TransactionData processOneFile(DBConnection conn , ImportTransformer transformer,
            String typeName,  String filePath, File file,
            boolean performsDifference, String delimiter) throws SQLException, IOException{
        TransactionData transactionData = null;
        try {
            transactionData =  super.prepareOneFileForProcess(conn , typeName ,
                    filePath , file , performsDifference);
            /* make sure transaction is created */
            conn.commit();
            importOneFile( conn, importData, file, oldFilePath, badFilePath,
                           transformer, delimiter );
            // set the transaction to PENDING 
            super.updateTransaction(transactionData.getWbitranId(), TransactionData.STATUS_PENDING,null,null,null);
            transactionData.setWbitranStatus(TransactionData.STATUS_PENDING);            
        } catch (Throwable t) {
            conn.rollback();
            if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) {
                getLogger().error("Error in importing file" , t);
            }
            /* if transactionData created, update transaction as failure and proceed to other files */
            if (transactionData != null) {
                super.updateTransaction(transactionData.getWbitranId() ,
                        TransactionData.STATUS_FAILURE,
                        "Error in importing file : " + filePath + " - " +
                        t.getMessage() ,
                        new java.util.Date(), new java.util.Date());
                transactionData.setWbitranStatus(TransactionData.STATUS_FAILURE);
                transactionData.setWbitranMsg(t.getMessage());
            /* otw throw it because smt serious is wrong and all processing has to stop */
            } else {
                throw new NestedRuntimeException(
                    "Error in creating transaction for file : " + filePath, t);
            }
        }
        /* always commit after each file regardless */
        conn.commit();
        return transactionData;
    }

    private void importOneFile( DBConnection conn, ImportData importData,
            File importFile, String oldFilePath, String badFilePath,
            ImportTransformer transformer,
            String delimiter  )
            throws IOException, java.sql.SQLException {
        if (getLogger().isEnabledFor(org.apache.log4j.Level.DEBUG)) {
            getLogger().debug("GenericDelimitedImportTask.importOneFile(" +
                    importFile.getAbsolutePath() + ")" );
        }

        boolean validateIllegalChars = false;
        try {
            String temp = (String)Registry.getVar("/system/WORKBRAIN_PARAMETERS/INPUT_VALIDATION");
            validateIllegalChars = "true".equalsIgnoreCase(temp);
        } catch(Exception e) {
            getLogger().warn("/system/WORKBRAIN_PARAMETERS/INPUT_VALIDATION not found in registry.");
        }
        DelimitedInputStream is =
                new DelimitedInputStream( importFile,
                        isUTF8Database,
                        validateIllegalChars );  // validate the illegal characters
        // *** check escape delims
        delimiter = convertEscapeDelims(delimiter);
        if (logger.isDebugEnabled()) logger.debug("validateIllegalChars=" + validateIllegalChars + "-delimiter" + delimiter);

        is.setDelimiters(new String[] {delimiter});
        PrintWriter oldOs = null;
        PrintWriter badOs = null;
        int recordCount = 0;

        try {
            int commitBatchSize = getCommitBatchSize();
            while( is.nextRow() ) {
                try {
                    importData.resetDataFields();
                    transformer.transform( conn, importData, is );
                    // commented out and being set to null
                    //importData.setProcDate( new Date( 0 ) );
                    importData.setRecNum(++recordCount);
                    importAccess.insert( importData );

                    if (recordCount % commitBatchSize == 0) {
                        if( isInterrupted() ) {
                            break;
                        }
                        conn.commit();
                    }
                    if( oldOs == null ) {
                        if (isUTF8Database) {
                            FileOutputStream fos = new FileOutputStream(
                                oldFilePath, true);
                            UTF8Writer utf8w = new UTF8Writer(fos);
                            oldOs = new PrintWriter(utf8w);
                        }
                        else {
                            oldOs = new PrintWriter( new FileOutputStream( oldFilePath, true ) );
                        }
                    }
                    copyRow( is, oldOs );
                } catch( Throwable t ) {
                    /* all other exceptions should fail the transaction as they are serious  */
                    if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) {
                        getLogger().error("com.workbrain.app.wbinterface.GenericDelimitedImportTask.class" , t);
                    }
                    try {
                        if( badOs == null ) {
                            if (isUTF8Database) {
                                FileOutputStream fos = new FileOutputStream(
                                    badFilePath, true);
                                UTF8Writer utf8w = new UTF8Writer(fos);
                                badOs = new PrintWriter(utf8w);
                            }
                            else {
                                badOs = new PrintWriter(new FileOutputStream(
                                    badFilePath, true));
                            }
                        }
                        copyRow( is, badOs );
                        badOs.println("Error : " + t.getMessage());
                        log("Insertion failed, " + importData);
                    } catch( java.io.IOException ie ) {
                        if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) {
                            getLogger().error("Error in writing to bad file" , ie);
                        }
                        throw new NestedRuntimeException(
                                "Error in writing to bad file" , ie);
                    }
                    throw new NestedRuntimeException(t);
                }
            }
            if (recordCount % commitBatchSize != 0) {
                conn.commit();
            }

        } finally {
            try {
                if (recordCount == 0) {
                    copyFile(originalFilePath, badFilePath);
                }

                if( is != null ) {
                    is.close();
                }

                importFile.delete();

                if( oldOs != null ) {
                    oldOs.close();
                }
                if( badOs != null ) {
                    badOs.close();
                }
            } catch( Exception e ) {
                if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) {
                    getLogger().error("com.workbrain.app.wbinterface.GenericDelimitedImportTask.class" ,
                            e);
                }
                throw new NestedRuntimeException(e);
            }
        }
    }

    private String convertEscapeDelims(String delim) {
        return StringHelper.searchReplace(delim,
            new String[] {PARAM_DEL_VAL_BACK_SLASH, PARAM_DEL_VAL_CARRIAGE_RETURN , PARAM_DEL_VAL_NEW_LINE_FEED , PARAM_DEL_VAL_TAB},
            new String[] {"\\", "\r" , "\n" , "\t"}
            ) ;
    }

    public String getTaskUI() {
        return "/jobs/wbiag/wbimportGenericDelimetedParams.jsp";
    }


    protected void log( String message ) {
        if (getLogger().isEnabledFor(org.apache.log4j.Level.DEBUG)) { getLogger().debug( message );}
    }

    private File[] sortFilesUnderDir(String dirPath, String sortFilesBy) throws IOException{
        File[] files = FileUtil.getFilesUnderDir(dirPath);
        SFile[] sfiles = new SFile[files.length];
        for (int i = 0,size = files.length;i < size;i++){
            sfiles[i] = new SFile(files[i].getAbsolutePath(),files[i].lastModified(), sortFilesBy);
        }
        java.util.Arrays.sort(sfiles,new SFile());
        File[] sortedFiles = new File[files.length];

        for (int i=0,size = sfiles.length;i < size;i++){
            sortedFiles[i] = new File(sfiles[i].getName());
        }
        return sortedFiles;
    }

    // **** private class for sorting files
    private class SFile implements java.util.Comparator {
        private String fName = "";
        private long lmDate = 0;
        private String sortFilesBy = "";        

        public SFile(){}

        public SFile(String name,long date, String sortFilesBy){
           this.fName = name;
           this.lmDate = date;
           this.sortFilesBy = sortFilesBy;
        }

        public boolean equals(Object f){
            boolean res = false;
            if (this.lmDate == ( (SFile) f).getDate()) {
                if (this.fName.compareTo( ( (SFile) f).getName()) == 0) {
                    res = true;
                }
            }
            return res;
        }

        public int hashCode() {
            return (fName == null ? 0 : fName.hashCode()) +
                new Long(lmDate).hashCode();
        }

        public int compare(Object o1,Object o2){
            int res = 0;
            SFile sf2 = (SFile)o2;
            SFile sf1 = (SFile)o1;
            if (PARAM_SORT_VAL_DATE.equals(this.sortFilesBy)) {
                if (sf1.getDate() > sf2.getDate()){
                    res = 1;
                } else if (sf1.getDate() == sf2.getDate()){
                    res = sf1.getName().compareTo(sf2.getName());
                } else if (sf1.getDate() < sf2.getDate()){
                    res = -1;
                }
            }
            else  if (PARAM_SORT_VAL_NAME_DESC.equals(this.sortFilesBy)) {
                int cmp = sf1.getName().compareTo(sf2.getName());
                if (cmp != 0)  {
                    res = cmp;
                } 
                else {
                    res = new java.util.Date(sf1.getDate()).compareTo(new java.util.Date(sf2.getDate()));
                }                
            }
            else  if (PARAM_SORT_VAL_NAME_ASC.equals(this.sortFilesBy)) {
                int cmp = sf2.getName().compareTo(sf1.getName());
                if (cmp != 0)  {
                    res = cmp;
                } 
                else {
                    res = new java.util.Date(sf2.getDate()).compareTo(new java.util.Date(sf1.getDate()));
                }                
            }
            
            return res;
        }

        public String getName(){
            return this.fName;
        }

        public long getDate(){
            return this.lmDate;
        }
    }
}
