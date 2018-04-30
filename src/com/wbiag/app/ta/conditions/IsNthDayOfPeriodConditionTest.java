package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;

import junit.framework.*;

/**
 * Test for IsNthDayOfPeriodConditionTest.
 */

public class IsNthDayOfPeriodConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsNthDayOfPeriodConditionTest.class);

    public IsNthDayOfPeriodConditionTest(String testName)
        throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsNthDayOfPeriodConditionTest.class);
        return result;
    }

    public void testPeriod()   throws Exception
    {
        final int empId = 15;
        Date lastDayOfYear = DateHelper.parseDate("12/31/2000", "MM/dd/yyyy");
        Date firstDayOfYear = DateHelper.parseDate("01/01/2000", "MM/dd/yyyy");

        Condition cond = new IsNthDayOfPeriodCondition();
        Parameters params = new Parameters();
        params.addParameter(IsNthDayOfPeriodCondition.PARAM_APPLY_ON_UNIT, DateHelper.APPLY_ON_UNIT_YEAR);
        params.addParameter(IsNthDayOfPeriodCondition.PARAM_APPLY_ON_VALUE, DateHelper.APPLY_ON_LAST_DAY);
        //test true case
        assertConditionTrue(empId, lastDayOfYear, cond, params);

        //test false case
        assertConditionFalse(empId, firstDayOfYear, cond, params);

        params.removeAllParameters();
        params.addParameter(IsNthDayOfPeriodCondition.PARAM_APPLY_ON_UNIT, DateHelper.APPLY_ON_UNIT_YEAR);
        params.addParameter(IsNthDayOfPeriodCondition.PARAM_APPLY_ON_VALUE, DateHelper.APPLY_ON_FIRST_DAY);
        //test true case
        assertConditionTrue(empId, firstDayOfYear, cond, params);

        Date firstDayOfQtr = DateHelper.parseDate("04/01/2000", "MM/dd/yyyy");
        params.removeAllParameters();
        params.addParameter(IsNthDayOfPeriodCondition.PARAM_APPLY_ON_UNIT, DateHelper.APPLY_ON_UNIT_QTR);
        params.addParameter(IsNthDayOfPeriodCondition.PARAM_APPLY_ON_VALUE, DateHelper.APPLY_ON_FIRST_DAY);
        //test true case
        assertConditionTrue(empId, firstDayOfQtr, cond, params);
        assertConditionFalse(empId, DateHelper.addDays(firstDayOfQtr , 1), cond, params);
    }

    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
