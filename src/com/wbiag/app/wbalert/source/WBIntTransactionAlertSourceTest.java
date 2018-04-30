package com.wbiag.app.wbalert.source;

import java.util.HashMap;

import junit.framework.TestSuite;

import com.workbrain.app.wbalert.WBAlertProcess;
import com.workbrain.app.wbalert.db.WBAlertAccess;
import com.workbrain.app.wbalert.model.WBAlertData;
import com.workbrain.test.TestCaseHW;
import com.workbrain.util.*;
import java.sql.PreparedStatement;
import java.util.*;

/**
 */
public class WBIntTransactionAlertSourceTest extends TestCaseHW {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBIntTransactionAlertSourceTest.class);

    private int alertId;
    private int tranId;
    /**
     *
     */
    public WBIntTransactionAlertSourceTest(String testName) throws Exception {
        super(testName);
        // TODO Auto-generated constructor stub
    }


    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WBIntTransactionAlertSourceTest.class);
        return result;
    }

    /**
     */
    public void testAlert() throws Exception {

        alertId = getConnection().getDBSequence(WBAlertAccess.WB_ALERT_SEQ ).getNextValue();
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO wb_alert (wbal_id, wbal_name, wbal_src_type, wbal_src_class, wbal_rc_wbu_names)");
            sb.append(" VALUES (?,?,?,?,?) ");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1  , alertId);
            ps.setString(2 , "Transaction failure");
            ps.setString(3, WBAlertProcess.SOURCE_TYPE_ROWSOURCE);
            ps.setString(4, "com.wbiag.app.wbalert.source.WBIntTransactionAlertSourceBuilder");
            ps.setString(5, "WORKBRAIN");
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;
        tranId = getConnection().getDBSequence("seq_wbitran_id").getNextValue();
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO wbint_transaction(wbitran_id, wbityp_name, wbitran_status, wbitran_start_date) ");
            sb.append(" VALUES (?,?,?,?)");
            ps = getConnection() .prepareStatement(sb.toString());
            ps.setInt(1, tranId);
            ps.setString(2, "HR REFRESH") ;
            ps.setString(3  , "PENDING");
            ps.setTimestamp(4  , new java.sql.Timestamp(new Date().getTime()));
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        HashMap params = new HashMap();
        params.put(WBIntTransactionAlertSource.PARAM_LOOK_BACK_MINUTES, "60");
        params.put(WBIntTransactionAlertSource.PARAM_WBINT_TYPE_NAMES, "HR REFRESH" );

        WBAlertProcess pr = new WBAlertProcess(getConnection() , alertId, params);
        pr.execute();
        // *** must be only sent to workbrain
        assertEquals(1 , pr.getSentMessageCount());



    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // *** cleanup because WBAlertProcess commits
        deleteStatement ("DELETE FROM wb_alert WHERE wbal_id = ?" , alertId);
        deleteStatement ("DELETE FROM jobskd_log WHERE jstsk_id = ?" , tranId);
        getConnection().commit();

    }

    private void deleteStatement (String sql, int id) throws Exception{
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            ps = getConnection().prepareStatement(sql);
            ps.setInt(1  , id);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}

