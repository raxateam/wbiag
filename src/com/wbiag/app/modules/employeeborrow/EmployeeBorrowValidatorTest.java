package com.wbiag.app.modules.employeeborrow ;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.workflow.*;
import com.wbiag.app.bo.*;
import com.workbrain.app.bo.*;
import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * EmployeeBorrowValidatorTest
 * @deprecated Use core functionality as of 5.0.3.2
 */
public class EmployeeBorrowValidatorTest
    extends com.workbrain.app.bo.InteractionTestCase {
    private static Logger logger = Logger.getLogger(EmployeeBorrowValidatorTest.class);
    private AbstractActionProcess node = null;

    private static final String TEST_USER1 = "WORKBRAIN";
    private BOFormInstance form = null;
    private Action action = null;

    public EmployeeBorrowValidatorTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(EmployeeBorrowValidatorTest.class);
        return result;
    }

    public void setUp() throws Exception {
        super.setUp();
        node = new EmployeeBorrowValidator();
        // *** load workflow and form from DB
        form = createInteraction(
            TEST_USER1, "Employee Borrow", "Employee Borrow",
            false, false, new Date(), null);
        // *** set node parameters
        Map params = new Hashtable();
        action = super.getAction(params);
        action = new Action();
        action.addProperty(EmployeeBorrowValidator.PROP_ROLE_NAME, "SUPERVISOR",
                           com.workbrain.app.workflow.properties.types.StringType.get() );
    }

    public void tearDown() throws Exception {
        super.tearDown();
        node = null;
        form = null;
        action = null;
    }

    /**
     * This test case runs Export payroll for one paygroup accross current pay period
     *
     * Expected result: branch "Success".
     */
    public void testValidate() throws Exception {
        ActionResponse result = null;

        // *** set action values
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon" );
        Date end = DateHelper.addDays(start, 2);
        // *** set form values
        int empId = 11 , wbtId = 10;
        Map valMap = new HashMap();
        valMap.put("value","2012");
        valMap.put("valueId", String.valueOf(empId) );
        form.setFieldValue(EmployeeBorrowValidator.FLD_EMPLOYEE , valMap);

        valMap = new HashMap();
        valMap.put("value", "RECEIVING");
        valMap.put("valueId", String.valueOf(wbtId) );
        form.setFieldValue(EmployeeBorrowValidator.FLD_STORE , valMap);

        form.setValue(EmployeeBorrowValidator.FLD_START_DATE,
                      DateHelper.convertDateString(start , WorkflowUtil.DATEPICKER_DATE_FORMAT) );
        form.setValue(EmployeeBorrowValidator.FLD_END_DATE,
                      DateHelper.convertDateString(end , WorkflowUtil.DATEPICKER_DATE_FORMAT));

        // *** set output branch names
        Branch[] branches = super.getBranches(new String[] {"Success",
                                              "Failure"});
        // *** run test

        result = node.processObject(action, form, branches, null);

        assertNotNull(result);
        assertEquals(ActionResponse.BRANCH, result.getType());
        assertEquals("Success", ( (Branch) result.getValue()).getLabel());
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
