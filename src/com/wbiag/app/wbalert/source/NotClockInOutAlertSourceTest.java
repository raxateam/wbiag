/**
 * Created on May 25, 2005
 *
 * Title: NotClockInOutAlertSourceTest
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
import java.sql.PreparedStatement;

/**
 * @author BLi
 *
 * @version  1.0
 */
public class NotClockInOutAlertSourceTest extends TestCaseHW {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(NotClockInOutAlertSourceTest.class);

    private int alertId;
    /**
     *
     */
    public NotClockInOutAlertSourceTest(String testName) throws Exception {
        super(testName);
        // TODO Auto-generated constructor stub
    }


    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(NotClockInOutAlertSourceTest.class);
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
            ps.setString(2 , "Not ClockInOut");
            ps.setString(3, WBAlertProcess.SOURCE_TYPE_ROWSOURCE);
            ps.setString(4, "com.wbiag.app.wbalert.source.NotClockInOutAlertSourceBuilder");
            ps.setString(5, "WORKBRAIN");
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        WBAlertAccess access = new WBAlertAccess(getConnection());
        WBAlertData data = access.load(alertId);
        HashMap params = new HashMap();
        params.put(NotClockInOutAlertSource.PARAM_CHECK_NO_CLOCK_IN, "Y");
        params.put(NotClockInOutAlertSource.PARAM_CLOCK_IN_TYPES, "1");
        WBAlertProcess pr = new WBAlertProcess(getConnection() , alertId, params);
        pr.execute();
        // *** must be only sent to workbrain
        assertEquals(1 , pr.getSentUserCount());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // *** cleanup because WBAlertProcess commits
        deleteStatement ("DELETE FROM wb_alert WHERE wbal_id = ?" , alertId);
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



