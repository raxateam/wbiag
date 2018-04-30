package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;

import junit.framework.*;

/**
 * Test for IsWhatPayPeriodConditionTest.
 */

public class IsWhatPayPeriodConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsWhatPayPeriodConditionTest.class);

    public IsWhatPayPeriodConditionTest(String testName)
        throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsWhatPayPeriodConditionTest.class);
        return result;
    }

    public void testPeriod()   throws Exception
    {
        final int empId = 11;
        Date wrksDate = DateHelper.parseDate("01/21/2006", "MM/dd/yyyy");
        new CreateDefaultRecords(getConnection(), new int[] {empId}, wrksDate,wrksDate).execute(false);

        Condition cond = new IsWhatPayPeriodCondition();
        Parameters params = new Parameters();
        params.addParameter(IsWhatPayPeriodCondition.PARAM_OPERATOR, RuleHelper.BIGGER);
        params.addParameter(IsWhatPayPeriodCondition.PARAM_PAY_PERIOD_NUMBER, "2");
        //test true case
        assertConditionTrue(empId, wrksDate, cond, params);

        params.removeAllParameters() ;
        params.addParameter(IsWhatPayPeriodCondition.PARAM_OPERATOR, RuleHelper.EQ);
        params.addParameter(IsWhatPayPeriodCondition.PARAM_PAY_PERIOD_NUMBER, "3");
        //test true case
        assertConditionTrue(empId, wrksDate, cond, params);

    }

    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
