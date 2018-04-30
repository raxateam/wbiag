package com.wbiag.app.bo.ejb.actions;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.workflow.*;
import com.wbiag.app.bo.*;
import com.workbrain.app.bo.*;
import junit.framework.*;

/**
 * ExportPayrollActionTest
 */
public class ExportPayrollActionTest
    extends com.workbrain.app.bo.InteractionTestCase {
    private static Logger logger = Logger.getLogger(ExportPayrollActionTest.class);
    private AbstractActionProcess node = null;

    private static final String TEST_USER1 = "WORKBRAIN";
    private BOFormInstance form = null;
    private Action action = null;

    public ExportPayrollActionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ExportPayrollActionTest.class);
        return result;
    }

    public void setUp() throws Exception {
        super.setUp();
        node = new ExportPayrollAction();
        // *** load workflow and form from DB
        form = createInteraction(
            TEST_USER1, "Export Payroll", "Export Payroll",
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
     * This test case runs Export payroll for one paygroup accross current pay period
     *
     * Expected result: branch "Success".
     */
    public void testExportPayroll() throws Exception {
        ActionResponse result = null;

        // *** set action values


        // *** set form values
        Map pgValueMap = new HashMap();
        pgValueMap.put("value","2");
        pgValueMap.put("valueId","2");
        form.setFieldValue("paygrp_ids", pgValueMap);

        Map psValueMap = new HashMap();
        psValueMap.put("value", "PEOPLESOFT");
        psValueMap.put("valueId", "1");
        form.setFieldValue("paysys_id", psValueMap);

        form.setValue("cycle", "On Board");

        form.setValue("reset", "false");
        form.setValue("use_pay_period", "true");
        form.setValue("all_ready_paygrps", "false");
        form.setValue("adjust_dates", "false");
        form.setValue("write_to_file", "false");
        form.setValue("merge_files", "false");
        form.setValue("write_to_table", "false");
        form.setValue("check_pgs_ready", "false");

        form.setValue("start_date", "11/4/2004");
        form.setValue("end_date", "11/11/2004");

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
/**
     *
    public void testExportPayrollAllReady() throws Exception {
        ActionResponse result = null;

        // *** set action values


        // *** set form values
        Map pgValueMap = new HashMap();
        pgValueMap.put("value","1");
        pgValueMap.put("valueId","1");
        form.setFieldValue("paygrp_ids", pgValueMap);

        Map psValueMap = new HashMap();
        psValueMap.put("value", "PEOPLESOFT");
        psValueMap.put("valueId", "1");
        form.setFieldValue("paysys_id", psValueMap);

        form.setValue("cycle", "Regular");

        form.setValue("reset", "false");
        form.setValue("use_pay_period", "true");
        form.setValue("all_ready_paygrps", "false");
        form.setValue("adjust_dates", "false");
        form.setValue("write_to_file", "true");
        form.setValue("merge_files", "true");
        form.setValue("write_to_table", "false");

        form.setValue("start_date", "11/4/2004");
        form.setValue("end_date", "11/11/2004");

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

    public void testExportPayrollReset() throws Exception {
        ActionResponse result = null;

        // *** set action values


        // *** set form values
        Map pgValueMap = new HashMap();
        //pgValueMap.put("value","1");
        //pgValueMap.put("valueId","1");
        form.setFieldValue("paygrp_ids", pgValueMap);

        Map psValueMap = new HashMap();
        //psValueMap.put("value","CERIDIAN");
        //psValueMap.put("valueId","2");
        form.setFieldValue("paysys_id", psValueMap);

        form.setValue("reset", "true");
        form.setValue("use_pay_period", "true");
        form.setValue("all_ready_paygrps", "false");
        form.setValue("adjust_dates", "false");
        form.setValue("write_to_file", "true");
        form.setValue("merge_files", "true");
        form.setValue("write_to_table", "false");

        form.setValue("start_date", "11/4/2004");
        form.setValue("end_date", "11/11/2004");

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
    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
