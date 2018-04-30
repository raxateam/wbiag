package com.wbiag.app.export.payroll;

import java.util.*;

import junit.framework.*;

import com.workbrain.app.scheduler.enterprise.*;
import com.workbrain.test.TestCaseHW;


public class PayrollExporterTaskTest
    extends TestCaseHW {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(PayrollExporterTaskTest.class);

    public PayrollExporterTaskTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(PayrollExporterTaskTest.class);
        return result;
    }

    public void testPayrollExporterTask() throws Exception {

        PayrollExporterTask task = new PayrollExporterTask();

        Map params = new HashMap();
        params.put(PayrollExporterTask.PARAM_PET_ID, "1");
        params.put(PayrollExporterTask.PARAM_PAYGRP_ID, "2");
        params.put(PayrollExporterTask.PARAM_CYCLE, "Regular");
        params.put(PayrollExporterTask.PARAM_CLIENT_ID, "1");
        params.put(PayrollExporterTask.PARAM_CHECK_UNAUTH, "true");
        params.put(PayrollExporterTask.PARAM_LOOK_BACK_DAYS, "14");
        params.put(PayrollExporterTask.PARAM_CONTINUE_ON_UNAUTH, "true");

        params.put(PayrollExporterTask.PARAM_ALL_READY_PAYGRPS,
                   "False");
        params.put(PayrollExporterTask.PARAM_RESET_READINESS,
                   "False");
        params.put(PayrollExporterTask.PARAM_USE_PAY_PERIOD,
                   "True");
        params.put(PayrollExporterTask.PARAM_WRITE_TO_TABLE,
                   "False");
        params.put(PayrollExporterTask.PARAM_WRITE_TO_FILE,
                   "True");
        params.put(PayrollExporterTask.PARAM_MERGE_FILES,
                   "False");
        params.put(PayrollExporterTask.PARAM_ADJUST_DATES,
                   "True");

        ScheduledJob.Status result = null;
        result = task.run(1, params);
        System.out.println("Checking results...");
        assertNotNull(result);
        assertFalse( result.isJobFailed());


    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
