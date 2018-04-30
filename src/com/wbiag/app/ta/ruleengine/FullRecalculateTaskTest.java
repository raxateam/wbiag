package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import junit.framework.*;
import java.math.BigDecimal;
/**
 * Test for FullRecalculateTaskTest.
 */
public class FullRecalculateTaskTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FullRecalculateTaskTest.class);

    public FullRecalculateTaskTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(FullRecalculateTaskTest.class);
        return result;
    }

    /**
     * Dummy to avoid warning, tests have been deprecated
     * @throws Exception
     */
    public void testDummy() throws Exception {
        assertTrue(1==1);
    }

    /**
     * @throws Exception
     */
    public void xFullRecalculateTask() throws Exception {


        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 11;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");
        Date end = DateHelper.addDays(start , 100);
        // *** craete def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start, end).execute(false);

        FullRecalculateTask task = new FullRecalculateTask();
        task.setCheckForInterrupt(false); task.setShouldCommit(false);
        Map params = new HashMap();
        params.put(FullRecalculateTask.PARAM_APPLY_TO_EMPS ,
                   String.valueOf(empId));
        params.put(FullRecalculateTask.PARAM_CALCULATION_THREAD_COUNT , "1");
        params.put(FullRecalculateTask.PARAM_BATCH_SIZE, "50");
        params.put(FullRecalculateTask.PARAM_TASK_TYPE, FullRecalculateTask.PARAM_TASK_TYPE_ABSOLUTE);
        params.put(FullRecalculateTask.PARAM_ABS_START_DATE, DateHelper.convertDateString(start, FullRecalculateTask.ABS_DATE_FORMAT));
        params.put(FullRecalculateTask.PARAM_ABS_END_DATE, DateHelper.convertDateString(end, FullRecalculateTask.ABS_DATE_FORMAT));

        task.execute(getConnection() , params);

        assertTrue(task.getEmployeeDatesCalculated().containsKey(String.valueOf(empId)));
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
