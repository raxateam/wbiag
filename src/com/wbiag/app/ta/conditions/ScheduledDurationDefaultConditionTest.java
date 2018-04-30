package com.wbiag.app.ta.conditions;

import java.util.*;


import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * Test for ScheduledDurationDefaultCondition.
 */
public class ScheduledDurationDefaultConditionTest
    extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(ScheduledDurationDefaultConditionTest.class);

    public ScheduledDurationDefaultConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ScheduledDurationDefaultConditionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void test1() throws Exception {
        DBConnection conn = this.getConnection();

        final int empId = 11;
        Date start = DateHelper.getCurrentDate();
        new CreateDefaultRecords(conn, new int[] {empId}, start, start).execute(false);
        EmployeeAccess ea = new EmployeeAccess(conn, this.getCodeMapper());
        EmployeeData ed = ea.load(empId, start);
        EmployeeScheduleAccess esa = new EmployeeScheduleAccess(conn, this.getCodeMapper());
        EmployeeScheduleData esd = esa.load(ed, start);
        // default day shift
        int defaultValue = 480;

        // *** create the rule
        Condition cond = new ScheduledDurationDefaultCondition();
        Parameters params = new Parameters();
        params.addParameter(ScheduledDurationDefaultCondition.PARAM_COMPARE_TO_DURATION,
                            Integer.toString(defaultValue));
        params.addParameter(ScheduledDurationDefaultCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);

        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(ScheduledDurationDefaultCondition.PARAM_COMPARE_TO_DURATION,
                            Integer.toString(defaultValue+1));
        params.addParameter(ScheduledDurationDefaultCondition.PARAM_OPERATOR,
                            RuleHelper.LESS);
        assertConditionTrue(empId, start, cond, params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
