package com.wbiag.app.bo.ejb.actions;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.workflow.*;
import com.wbiag.app.bo.*;
import com.workbrain.app.bo.*;
import junit.framework.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * ReadinessPayrollActionTest
 */
public class ReadinessPayrollActionTest
    extends com.workbrain.app.bo.InteractionTestCase {
    private static Logger logger = Logger.getLogger(ReadinessPayrollActionTest.class);
    private AbstractActionProcess node = null;

    private static final String TEST_USER1 = "WORKBRAIN";
    private BOFormInstance form = null;
    private Action action = null;

    public ReadinessPayrollActionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ReadinessPayrollActionTest.class);
        return result;
    }

    public void setUp() throws Exception {
        super.setUp();
        node = new ReadinessPayrollAction();
        // *** load workflow and form from DB
        form = createInteraction(
            TEST_USER1, "Readiness Payroll", "Readiness Payroll",
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
     * This test case runs readiness payroll for one paygroup accross current pay period
     *
     * Expected result: branch "Success".

    public void testReadinessPayroll() throws Exception {
        ActionResponse result = null;

        // *** set action values
        action.addProperty("CHK_UNAUTH", "true",
                           com.workbrain.app.workflow.properties.types.
                           StringType.get());

        action.addProperty("LOOK_BACK_DAYS", "-14",
                           com.workbrain.app.workflow.properties.types.
                           StringType.get());


        // *** set form values
        int payId = createPayGroup();
        Map valueMap = new HashMap();
        valueMap.put("value", "1");
        valueMap.put("valueId", Integer.toString(payId));
        form.setFieldValue("paygrp_ids", valueMap);
        form.setValue("use_pay_period", "true");
        form.setValue("readycb", "true");

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
*/
    public void testReadinessPayrollGivenDates() throws Exception {
        ActionResponse result = null;

        // *** set action values
        action.addProperty("CHK_UNAUTH", "true",
                           com.workbrain.app.workflow.properties.types.
                           StringType.get());
        action.addProperty("LOOK_BACK_DAYS", "-14",
                   com.workbrain.app.workflow.properties.types.
                   StringType.get());


        // *** set form values
        Map valueMap = new HashMap();
        valueMap.put("value", "2");
        valueMap.put("valueId", "2");
        form.setFieldValue("paygrp_ids", valueMap);
        form.setValue("use_pay_period", "true");
        form.setValue("readycb", "true");
//        form.setValue("start_date", "20041109 000000");
//        form.setValue("end_date", "20041909 000000");

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

    private int createPayGroup() throws Exception {
        DBConnection conn = this.getConnection();
        PayGroupAccess pa = new PayGroupAccess(conn);
        PayGroupData pd = pa.load(2);

        int paygrpId = 100;
        pd.setPaygrpId(paygrpId);
        pd.setPaygrpName("100");
        pd.setPaygrpDesc("TEST");
        pd.setPgcId(null);
        pd.setPaygrpStartDate(DateHelper.getCurrentDate());
        pd.setPaygrpEndDate(DateHelper.addDays(DateHelper.getCurrentDate(), 7));
        pa.insert(pd);

        return paygrpId;

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
