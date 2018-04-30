package com.wbiag.app.modules.employeeborrow ;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.workflow.*;
import com.wbiag.app.bo.*;
import com.workbrain.app.bo.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.util.*;
import junit.framework.*;
import java.sql.PreparedStatement;
import com.workbrain.app.ta.ruleengine.*;
/**
 * EmployeeBorrowProcessTest
 * @deprecated Use core functionality as of 5.0.3.2
 */
public class EmployeeBorrowProcessTest
    extends com.workbrain.app.bo.InteractionTestCase {
    private static Logger logger = Logger.getLogger(EmployeeBorrowProcessTest.class);
    private EmployeeBorrowProcess node = null;

    private static final String TEST_USER1 = "WORKBRAIN";
    private BOFormInstance form = null;
    private Action action = null;
    Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon" );
    Date end = DateHelper.addDays(start, 2);
    // *** set form values
    int empId = 9001 , wbtId = 10, stfgrpIdNew = 9002;

    public EmployeeBorrowProcessTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(EmployeeBorrowProcessTest.class);
        return result;
    }

    public void setUp() throws Exception {
        super.setUp();
        node = new EmployeeBorrowProcess();

        // *** load workflow and form from DB
        form = createInteraction(
            TEST_USER1, "Employee Borrow", "Employee Borrow",
            false, false, new Date(), null);
        // *** set node parameters
        Map params = new Hashtable();
        action = super.getAction(params);
        action = new Action();
        action.addProperty(EmployeeBorrowProcess.PROP_PERFORM_SCHEDULE_DETAIL_OVERRIDES,
                           "true",
                           com.workbrain.app.workflow.properties.types.StringType.get() );
        action.addProperty(EmployeeBorrowProcess.PROP_PERFORM_STAFF_GROUP_ASSIGNMENT,
                           "true",
                           com.workbrain.app.workflow.properties.types.StringType.get() );

    }

    public void tearDown() throws Exception {
        super.tearDown();
        node = null;
        form = null;
        action = null;

        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM employee_team WHERE emp_id = ? and wbt_id = ?");
            ps = getConnection() .prepareStatement(sb.toString());
            ps.setInt(1  , empId);
            ps.setInt(2  , wbtId);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;

        /*try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM change_history WHERE chnghist_record_id = ?");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1  , empId);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }*/

        getConnection().commit();
    }

    /**
     * This test case runs Export payroll for one paygroup accross current pay period
     *
     * Expected result: branch "Success".
     */
    public void testValidate() throws Exception {
        ActionResponse result = null;

        Map valMap = new HashMap();
        valMap.put("value","2012");
        valMap.put("valueId", String.valueOf(empId) );
        form.setFieldValue(EmployeeBorrowValidator.FLD_EMPLOYEE , valMap);

        valMap = new HashMap();
        valMap.put("value", "RECEIVING");
        valMap.put("valueId", String.valueOf(wbtId) );
        form.setFieldValue(EmployeeBorrowValidator.FLD_STORE, valMap);

        valMap = new HashMap();
        valMap.put("value", "BUILDING STORE #53 MINORS GROUP");
        valMap.put("valueId", String.valueOf(stfgrpIdNew) );
        form.setFieldValue(EmployeeBorrowValidator.FLD_STAFF_GROUP, valMap);

        form.setValue(EmployeeBorrowValidator.FLD_START_DATE,
                      DateHelper.convertDateString(start , WorkflowUtil.DATEPICKER_DATE_FORMAT) );
        form.setValue(EmployeeBorrowValidator.FLD_END_DATE,
                      DateHelper.convertDateString(end , WorkflowUtil.DATEPICKER_DATE_FORMAT));

        form.setValue(EmployeeBorrowValidator.FLD_SGMOVE_DATE,
                      DateHelper.convertDateString(start , WorkflowUtil.DATEPICKER_DATE_FORMAT) );
        form.setValue(EmployeeBorrowValidator.FLD_SGRETURN_DATE,
                      DateHelper.convertDateString(end , WorkflowUtil.DATEPICKER_DATE_FORMAT));

        valMap = new HashMap();
        valMap.put("value", "EDWARDSANITO");
        valMap.put("valueId", String.valueOf(10) );
        form.setFieldValue(EmployeeBorrowValidator.FLD_STORE_MANAGER, valMap);

        // *** set output branch names
        Branch[] branches = super.getBranches(new String[] {"Success",
                                              "Failure"});
        // *** run test

        result = node.processObject(action, form, branches, null);

        assertNotNull(result);
        assertEquals(ActionResponse.BRANCH, result.getType());
        assertEquals("Success", ( (Branch) result.getValue()).getLabel());

        int cnt = com.workbrain.tool.security.SecurityEmployee.getTempTeams(
            getConnection() ,
            empId , new java.sql.Date(start.getTime() )).size();      ;
        assertTrue(cnt > 0);

        CalcSimulationContext calcContext = new CalcSimulationContext();

        CalcSimulationAccess calcAccess = new CalcSimulationAccess(calcContext);
        calcAccess.addEmployeeDate(empId, start, start);
        calcAccess.load(getConnection() );
        CalcSimulationEmployee calcEmployee = calcAccess.getResultForEmp(empId);
        List skdDetails = calcEmployee.getScheduleDetails(start);

        Iterator iter = skdDetails.iterator();
        while (iter.hasNext()) {
            EmployeeScheduleDetailData item = (EmployeeScheduleDetailData)
                iter.next();
            assertEquals(wbtId , item.getWbtId());
        }


    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
