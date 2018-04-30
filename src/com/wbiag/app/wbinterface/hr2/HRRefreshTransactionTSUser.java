package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Customization for TS_USER
 *
 **/
public class HRRefreshTransactionTSUser extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionTSUser.class);

    private static final int TS_USER_COL = 90;
    private static final int TS_USER_DAILY = 1;
    private static final int TS_USER_WEEKLY = 2;
    private static final String TS_USER_INSERT_SQL
        = "INSERT INTO ts_user(tsu_id, emp_id, tsu_effective_date,tsu_timesheet_flag) VALUES (?,?,?,?)";
    private static final String TS_USER_UPDATE_SQL
        = "UPDATE ts_user SET tsu_effective_date = ? , tsu_timesheet_flag = ? WHERE emp_id = ?";
    private static final String TS_USER_SELECT_SQL
        = "SELECT tsu_timesheet_flag, tsu_effective_date FROM ts_user WHERE emp_id = ? ";
    private int[] custColInds = new int[] {TS_USER_COL};
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
                processTsUser(data.getImportData().getField(custColInds[0]),
                              data,
                              conn);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionTSUser." , ex);}
                data.error("Error in HRRefreshTransactionTSUser." + ex.getMessage() );
            }
        }
    }

    protected void processTsUser(String tsUserVal,
                               HRRefreshTransactionData data,
                               DBConnection c) throws SQLException, WBInterfaceException {
        // *** only do this if TS_USER_COL is not blank in the file
        if (StringHelper.isEmpty(tsUserVal)) {
            return;
        }
        int tsuTimesheetFlag = Integer.parseInt(tsUserVal);
        if (tsuTimesheetFlag != TS_USER_DAILY && tsuTimesheetFlag != TS_USER_WEEKLY){
            throw new RuntimeException("tsuTimesheetFlag must be " +  TS_USER_DAILY + " or " + TS_USER_WEEKLY);
        }
        if (data.isNewEmployee()) {
            insertTSUser(data.getEmpId() , data.getOvrStartDate(),  tsuTimesheetFlag);
        }
        else {
            TSUser tsUser = getTSUser(data.getEmpId());
            if (tsUser == null) {
                insertTSUser(data.getEmpId() , data.getOvrStartDate(),
                             tsuTimesheetFlag);
            }
            else {
                // *** only update is smt changed
                if (tsUser.tsuTimesheetFlag != tsuTimesheetFlag
                    || !DateHelper.equals(tsUser.tsuEffectiveDate , data.getOvrStartDate())) {
                    updateTSUser(data.getEmpId(), data.getOvrStartDate(),
                                 tsuTimesheetFlag);
                }
            }
        }
    }

    protected int insertTSUser(int empId, Date effDate, int tsFlag) throws SQLException{
        int upd = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(TS_USER_INSERT_SQL);
            ps.setInt(1 , conn.getDBSequence("seq_tsu_id").getNextValue());
            ps.setInt(2 , empId);
            ps.setTimestamp(3 , new Timestamp(effDate.getTime()));
            ps.setInt(4 , tsFlag);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd;
    }

    protected int updateTSUser(int empId, Date effDate, int tsFlag) throws SQLException{
        int upd = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(TS_USER_UPDATE_SQL);
            ps.setTimestamp(1 , new Timestamp(effDate.getTime()));
            ps.setInt(2 , tsFlag);
            ps.setInt(3 , empId);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd;
    }

    protected TSUser getTSUser(int empId) throws SQLException{
        TSUser ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(TS_USER_SELECT_SQL);
            ps.setInt(1 , empId);
            rs = ps.executeQuery();
            if(rs.next()) {
                ret = new TSUser();
                ret.tsuTimesheetFlag = rs.getInt(1);
                ret.tsuEffectiveDate = rs.getDate(2);
            }
        }
        finally {
            if (rs != null)
                rs.close();
            if (ps != null)
                ps.close();
        }
        return ret;
    }

    private class TSUser {
        int tsuTimesheetFlag;
        Date tsuEffectiveDate;
    }

}