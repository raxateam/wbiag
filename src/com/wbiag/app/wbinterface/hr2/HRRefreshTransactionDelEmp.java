package com.wbiag.app.wbinterface.hr2;

import java.io.FileInputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.util.*;

import com.wbiag.server.cleanup.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * Customization for employee deletion if emp_status is set to D
 *
 **/
public class HRRefreshTransactionDelEmp extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionDelEmp.class);

    private static final String EMP_STATUS_DEL = "D";
    private static final String REG_CLEANUP_PROPERTIES_FILE = "system/WORKBRAIN_PARAMETERS/WBIAG_CLEANUP_PROP_PATH";
    /**
     * Override this class to customize after process batch events.
     * At this time, all HRRefreshTransactionData data has been processed.
     * All processed is available through <code>HRRefreshCache</code>.
     *
     * @param conn                          DBConnection
     * @param hrRefreshTransactionDataList  List of <code>HRRefreshTransactionData</code>
     * @param process                       HRRefreshProcessor
     * @throws Exception
     */
    public void postProcessBatch(DBConnection conn,
                                    List hrRefreshTransactionDataList,
                                    HRRefreshProcessor process) throws Exception {
        if (hrRefreshTransactionDataList == null || hrRefreshTransactionDataList.size() == 0) {
                return;
        }
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            try {
                if (EMP_STATUS_DEL.equals(data.getImportData().getField(EMP_STATUS_COL))) {
                    processDelEmp(data, conn);
                }
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionDelEmp." , ex);}
                data.error("Error in HRRefreshTransactionDelEmp." + ex.getMessage() );
            }

        }
    }


    protected void processDelEmp(HRRefreshTransactionData data,
                               DBConnection c) throws Exception {
        if (!data.isNewEmployee()) {
            Date chDate = new Date();
            EmployeeData edSaved = new EmployeeAccess(conn, CodeMapper.createCodeMapper(conn)).loadByName(data.getEmpName() , chDate) ;
            data.getHRRefreshData().getEmployeeData().setEmpId(edSaved.getEmpId());

            CleanupProcess.CleanupProcessContext ctx = new CleanupProcess.CleanupProcessContext();
            String propFile = Registry.getVarString(REG_CLEANUP_PROPERTIES_FILE , "");
            Properties props = new Properties();
            if (StringHelper.isEmpty(propFile)) {
                URL defPropsFile = ctx.getClass().getResource(
                    "CleanupProcess.properties");
                props.load(defPropsFile.openStream());
            }
            else {
                props.load(new FileInputStream(propFile));
            }
            ctx.setDeleteEmployeeTables(StringHelper.detokenizeStringAsList(
                props.getProperty(CleanupProcess.PROP_DELETE_EMPLOYEE_TABLES) , ","));

            ctx.setDBConnection(c);
            ctx.setShouldCommit(false);
            ctx.setConfirmationOnly(false);
            ctx.setClientId(com.workbrain.security.SecurityService.getCurrentClientId()) ;
            ctx.setDeleteEmployeeWhereClause("emp_name = '" + data.getEmpName() + "'");
            CleanupProcess pr = new CleanupProcess(ctx);
            pr.execute();

            insertAudit(c, chDate, data.getHRRefreshData().getEmployeeData());
            if (logger.isDebugEnabled()) if (pr.getTotalDeletedCount() == 1) logger.debug("Deleted employee : " + data.getEmpName()); else logger.debug("Could not delete employee : " + data.getEmpName());

        }
    }

    private static final String INSERT_AUDIT_SQL =
        "INSERT INTO audit_log(audlog_id,audlog_Action,wbu_name,audlog_change_date, audlog_key_id, audlog_tablename, audlog_fieldname, audlog_old_value, audlog_new_value) "
        + " VALUES (?,?,?,?,?,?,?,?,?)";
    private static final String AUD_WBU_NAME = "HRREFRESH_DEL_EMPLOYEE";
    private static final String AUD_DEL_ACTION = "D";
    private static final String AUD_NEWVAL_DELETED = "DELETED";

    /**
     * Creates audit record for EmployeeData to be deleted
     * @param conn
     * @param chDate
     * @param ed
     * @throws SQLException
     */
    protected void insertAudit(DBConnection conn, Date chDate, EmployeeData ed) throws SQLException{
        PreparedStatement ps = null;

        int keyId = ed.getEmpId() ;  ;
        try {
            ps = conn.prepareStatement(INSERT_AUDIT_SQL);
            for (int i=0; i< ed.getPropertyCount(); i++) {
                String field = ed.getFieldName(i);
                Object val = ed.getField(field);
                ps.setInt(1, conn.getDBSequence("seq_audlog_id").getNextValue());
                ps.setString(2, AUD_DEL_ACTION);
                ps.setString(3, AUD_WBU_NAME);
                ps.setTimestamp(4, new Timestamp(chDate.getTime()));
                ps.setInt(5, keyId);
                ps.setString(6, EmployeeAccess.EMPLOYEE_TABLE);
                ps.setString(7, field);
                ps.setString(8, val != null ? val.toString() : "");
                ps.setString(9, AUD_NEWVAL_DELETED);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        finally {
            if (ps != null) ps.close();
        }

    }

}