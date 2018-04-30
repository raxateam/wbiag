package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Customization for TEMP emp_name
 *
 **/
public class HRRefreshTransactionTempEmpName extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionTempEmpName.class);

    public static final int ONBOARD_EMPNAME_COL = 90;

    public static final String SCRIPT_ALTER_TRIGGER_DISABLE = "ALTER TRIGGER trg_emp_nm_bu DISABLE";
    public static final String SCRIPT_UPD_EMPLOYEE = "UPDATE employee SET emp_name = ? WHERE emp_name = ?";
    public static final String SCRIPT_UPD_EMPLOYEE_HIST = "UPDATE employee_history SET emp_name = ? WHERE emp_name = ?";
    public static final String SCRIPT_UPD_AC_EVENT =
        "UPDATE ac_event SET acevt_emp_names = REPLACE (',' || acevt_emp_names || ',', ',?,' , ',?,')";
    public static final String SCRIPT_UPD_AC_VIOLATION =
        "UPDATE ac_violation SET acviol_emp_names = REPLACE (',' || acviol_emp_names || ',', ',?,' , ',?,')";
    public static final String SCRIPT_ALTER_TRIGGER_ENABLE =
        "ALTER TRIGGER trg_emp_nm_bu ENABLE";

    private Map readerGroups = null;
    private int[] custColInds = new int[] {ONBOARD_EMPNAME_COL};
    private DBConnection conn = null;

    /**
     * Sets custom column indexes to be used by the customization. Indexes start at 0
     * @param inds
     */
    public void setCustomColInds(int[] inds) {
        custColInds = inds;
    }

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
        if (custColInds == null || custColInds.length != 1) {
            throw new WBInterfaceException ("Custom column index not supplied or too many, must be 1");
        }
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            try {
                processTempEmp(data.getImportData().getField(custColInds[0]),
                                   data,
                                   conn);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionTempEmpName." , ex);}
                data.error("Error in HRRefreshTransactionTempEmpName." + ex.getMessage() );
            }

        }
    }


    protected void processTempEmp(String onboardEmpName,
                               HRRefreshTransactionData data,
                               DBConnection c) throws SQLException, WBInterfaceException {
        // *** only do this if EMPRDRGRP_COL is not blank in the file
        if (StringHelper.isEmpty(onboardEmpName)) {
            return;
        }
        if (!data.isNewEmployee()) {
            String tempEmpName = data.getEmpName();
            executePS (SCRIPT_ALTER_TRIGGER_DISABLE , null, null);
            executePS (SCRIPT_UPD_EMPLOYEE , onboardEmpName, tempEmpName);
            executePS (SCRIPT_UPD_EMPLOYEE_HIST , onboardEmpName, tempEmpName);
            final String concat = conn.getDBServer().getConcatOperator();
            String acEvtUpd =  "UPDATE ac_event SET acevt_emp_names = "
                + " REPLACE (',' " + concat + " acevt_emp_names " + concat + " ',', '," + onboardEmpName +",' , '," + tempEmpName + ",')";
            executePS (acEvtUpd , null, null);
            String acViolUpd =  "UPDATE ac_violation SET acviol_emp_names = "
                + " REPLACE (',' " + concat + " acviol_emp_names " + concat + " ',', '," + onboardEmpName +",' , '," + tempEmpName + ",')";
            executePS (acViolUpd , null, null);
            executePS (SCRIPT_ALTER_TRIGGER_ENABLE , null, null);
        }
    }



    private int executePS(String sql, String var1, String var2) throws SQLException{
        int upd = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            if (!StringHelper.isEmpty(var1)) {
                ps.setString(1, var1);
            }
            if (!StringHelper.isEmpty(var2)) {
                ps.setString(2, var2);
            }

            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd;
    }

}