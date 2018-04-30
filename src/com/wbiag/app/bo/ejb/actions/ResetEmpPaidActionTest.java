package com.wbiag.app.bo.ejb.actions;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.workflow.*;
import com.wbiag.app.bo.*;
import com.workbrain.app.bo.*;
import junit.framework.*;

/**
 * ReadinessPayrollActionTest
 */
public class ResetEmpPaidActionTest
    extends com.workbrain.app.bo.InteractionTestCase {
    private static Logger logger = Logger.getLogger(ResetEmpPaidActionTest.class);
    private AbstractActionProcess node = null;

    private static final String TEST_USER1 = "WORKBRAIN";
    private BOFormInstance form = null;
    private Action action = null;

    public ResetEmpPaidActionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ResetEmpPaidActionTest.class);
        return result;
    }

    public void setUp() throws Exception {
        super.setUp();
        node = new ResetEmpPaidAction();
        // *** load workflow and form from DB
        form = createInteraction(
            TEST_USER1, "Reset Employee Paid Flag", "Reset Employee Paid Flag",
            false, false, new Date(), null);
        // *** set node parameters
        Map params = new Hashtable();
        action = super.getAction(params);
        action = new Action();

    }

    public void tearDown() throws Exception {
        super.tearDown();
        node = null;
        form = null;
        action = null;
    }

    /**
     * This test case runs reset paid flag for an employee accross a date range
     *
     * Expected result: branch "Success".
     */
    public void testResetEmpPaid() throws Exception {
        ActionResponse result = null;

        // *** set form values
        Map valueMap = new HashMap();
        valueMap.put("value", "10067");
        valueMap.put("valueId", "10038");
        form.setFieldValue("emp_ids", valueMap);
        form.setValue("start_date", "20041027 000000");
        form.setValue("end_date", "20041031 000000");

        // *** set output branch names
        Branch[] branches = super.getBranches(new String[] {"Success",
                                              "Failure"});
        // *** run test

        result = node.processObject(action, form, branches, null);
        System.out.println("Checking results...");
        assertNotNull(result);
        assertEquals(ActionResponse.BRANCH, result.getType());
        assertEquals("Success", ( (Branch) result.getValue()).getLabel());
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
