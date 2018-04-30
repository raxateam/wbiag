package com.wbiag.app.ta.ruleengine;

import java.lang.reflect.*;
import java.sql.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.test.*;
import junit.framework.*;
/**
 * Test case for DataEvents.
 */
public class DataEventTestCase extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DataEventTestCase.class);

    public DataEventTestCase(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(DataEventTestCase.class);
        return result;
    }

    public void setDataEventClassPath(String classPath) throws Exception{
        refreshDataEvent();
        // *** delete overridden reg entry, creates problems
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM workbrain_reg_ovrd WHERE wbreg_id = ?");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1  , 366);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        TestUtil.getInstance().setVarTemp("/system/customer/customerdataevent",
            classPath);
    }

    private void refreshDataEvent() throws Exception{
        Field fld = DataEvent.class.getDeclaredField("lookedForEvent");
        fld.setAccessible(true);
        fld.set(null, Boolean.FALSE);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
