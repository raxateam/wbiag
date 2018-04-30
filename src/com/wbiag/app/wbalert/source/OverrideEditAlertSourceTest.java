package com.wbiag.app.wbalert.source ;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbalert.*;
import com.workbrain.app.wbalert.db.*;
import com.workbrain.app.wbalert.model.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import junit.framework.*;
import java.sql.*;

public class OverrideEditAlertSourceTest extends TestCaseHW {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(OverrideEditAlertSourceTest.class);
    private int alertId;

    public OverrideEditAlertSourceTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(OverrideEditAlertSourceTest.class);
        return result;
    }

    /**
     */
    public void testAlert() throws Exception {
        int alertId = createAlert() ;

        int empId = 11;
        // *** update emp
		OverrideBuilder ob = new OverrideBuilder(getConnection());
        InsertEmployeeOverride ovr = new InsertEmployeeOverride(getConnection());
        ovr.setWbuNameBoth("TEST" , "TEST");
        ovr.setEmpId(empId);
        ovr.setStartDate(DateHelper.DATE_1900);
        ovr.setEndDate(DateHelper.DATE_3000);
        ovr.setEmpFlag2("Y");
        ob.add(ovr);
        ob.execute(false, false);
        assertEquals(1 , ob.getOverridesProcessed().size()  );

        Thread.sleep(500);

        WBAlertAccess access = new WBAlertAccess(getConnection());
        WBAlertData data = access.load(alertId);
        HashMap params = new HashMap();
        params.put(OverrideEditAlertSource.PARAM_LOOK_BACK_MINUTES, "60");
        params.put(OverrideEditAlertSource.PARAM_OVR_COLUMNS, OverrideEditAlertSource.getAllOverrideColumnString());
        params.put(OverrideEditAlertSource.PARAM_OVR_TYPES, "700");
        WBAlertProcess apr = new WBAlertProcess(getConnection() , alertId, params);
        apr.execute();
        // *** must be only sent to workbrain
        assertEquals(1 , apr.getSentUserCount());
    }

    private int createAlert() throws Exception{
        int alertId = getConnection().getDBSequence(WBAlertAccess.WB_ALERT_SEQ ).getNextValue();
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO wb_alert (wbal_id, wbal_name, wbal_src_type, wbal_src_class, wbal_rc_wbu_names, wbal_msg_cont_type)");
            sb.append(" VALUES (?,?,?,?,?, ?) ");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1  , alertId);
            ps.setString(2 , "Override Edit");
            ps.setString(3, WBAlertProcess.SOURCE_TYPE_ROWSOURCE);
            ps.setString(4, "com.wbiag.app.wbalert.source.OverrideEditAlertSourceBuilder");
            ps.setString(5, "WORKBRAIN");
            ps.setString(6, WBAlertProcess.MSG_CONTENT_TYPE_HTML);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return alertId;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // *** cleanup because WBAlertProcess commits
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM wb_alert WHERE wbal_id = ?");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1  , alertId);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        getConnection().commit();

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

