/**
 * Created on Mar 11, 2005
 *
 * Title:
 * Description:
 * <p>Copyright:  Copyright (c) 2004</p>
 * <p>Company:    Workbrain Inc.</p>
 */
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
 * @author BLi
 *
 * @version  1.0
 */
public class ScheduledTaskFailureAlertSourceTest extends TestCaseHW {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScheduledTaskFailureAlertSourceTest.class);

    private int alertId;
    private int tskId;
    /**
     *
     */
    public ScheduledTaskFailureAlertSourceTest(String testName) throws Exception {
        super(testName);
        // TODO Auto-generated constructor stub
    }


    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ScheduledTaskFailureAlertSourceTest.class);
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
            ps.setString(2 , "Scheduled Task Failure");
            ps.setString(3, WBAlertProcess.SOURCE_TYPE_ROWSOURCE);
            ps.setString(4, "com.wbiag.app.wbalert.source.ScheduledTaskFailureAlertSourceBuilder");
            ps.setString(5, "WORKBRAIN");
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }


        ps = null;
        tskId = getConnection().getDBSequence("seq_jstsk_id").getNextValue();
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO jobskd_task(jstsk_id, jstsk_desc,jstsk_class_name,jstsk_schedule) ");
            sb.append(" VALUES (?,?,?,?)");
            ps = getConnection() .prepareStatement(sb.toString());
            ps.setInt(1, tskId);
            ps.setString(2, "Dummy") ;
            ps.setString(3  , "com.dummy");
            ps.setString(4  , "dummy skd");
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO jobskd_log(jslog_id, jstsk_id,jslog_time,jslog_status) ");
            sb.append(" VALUES (?,?,?,?)");
            ps = getConnection() .prepareStatement(sb.toString());
            ps.setInt(1, getConnection() .getDBSequence("seq_jslog_id").getNextValue());
            ps.setInt(2, tskId) ;
            ps.setTimestamp(3  , new java.sql.Timestamp(DateHelper.addMinutes(new Date(), -30).getTime()));
            ps.setString(4  , "ERROR");
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        HashMap params = new HashMap();
        params.put(ScheduledTaskFailureAlertSource.PARAM_LOOK_BACK_MINUTES, "60");
        params.put(ScheduledTaskFailureAlertSource.PARAM_SKD_TASK_IDS, String.valueOf(tskId) );

        WBAlertProcess pr = new WBAlertProcess(getConnection() , alertId, params);
        pr.execute();
        // *** must be only sent to workbrain
        assertEquals(1 , pr.getSentMessageCount());

    }


    protected void tearDown() throws Exception {
        super.tearDown();
        // *** cleanup because WBAlertProcess commits
        deleteStatement ("DELETE FROM wb_alert WHERE wbal_id = ?" , alertId);
        deleteStatement ("DELETE FROM jobskd_log WHERE jstsk_id = ?" , tskId);
        deleteStatement ("DELETE FROM jobskd_task WHERE jstsk_id = ?" , tskId);
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

