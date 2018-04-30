package com.wbiag.app.modules.employeeborrow;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.modules.availability.db.*;
import com.workbrain.app.modules.availability.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.tool.security.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * @deprecated Use core functionality as of 5.0.3.2
 */
public class EmployeeBorrowEmpGroupTaskTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeBorrowEmpGroupTaskTest.class);


    public EmployeeBorrowEmpGroupTaskTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(EmployeeBorrowEmpGroupTaskTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testTask() throws Exception {
        final int empId = 15;
        final Date start = DateHelper.addDays(DateHelper.getCurrentDate() ,  - 10);
        final int stfgrpId = 9001;

        PreparedStatement ps = null;
        int id = 0;
        try {
            ps = getConnection().prepareStatement(EmployeeBorrowProcess.SQL_CHNG_HIST_INSERT);
            id = getConnection().getDBSequence("seq_chnghist_id").getNextValue() ;
            ps.setInt(1  , id );
            ps.setTimestamp(2, new Timestamp(start.getTime()));
            ps.setString(3, EmployeeBorrowEmpGroupTask.CHNGHIST_TABLE_NAME);
            ps.setString(4, EmployeeBorrowEmpGroupTask.CHNGHIST_TYPE_ASSIGN_TO);
            ps.setInt(5, empId);
            String recValMove = EmployeeBorrowEmpGroupTask.makeRecNameString(stfgrpId, 10, 3);
            ps.setString(6, recValMove);

            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        EmployeeBorrowEmpGroupTask task =    new EmployeeBorrowEmpGroupTask();
        task.setCheckForInterrupt(false);
        Map params = new HashMap();
        params.put(EmployeeBorrowEmpGroupTask.PARAM_CLIENT_ID, "1");
        params.put(EmployeeBorrowEmpGroupTask.PARAM_SENDS_MESSAGE, "Y");
        task.execute(getConnection(), params, false);

        PreparedStatement ps2 = null;
        ResultSet rs = null;
        String typ = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT chnghist_change_type FROM change_history WHERE chnghist_id = ?");
            ps = getConnection() .prepareStatement(sb.toString());
            ps.setInt(1 , id);
            rs = ps.executeQuery();
            if (rs.next()) {
                typ = rs.getString(1) ;
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps2 != null) ps.close();
        }

        assertEquals(EmployeeBorrowEmpGroupTask.CHNGHIST_TYPE_FINISHED, typ);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
