package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;

import junit.framework.*;

/**
 * Test for WorkedNMinutesInMonthConditionTest.
 */

public class WorkedNMinutesInMonthConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkedNMinutesInMonthConditionTest.class);

    public WorkedNMinutesInMonthConditionTest(String testName)
        throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedNMinutesInMonthConditionTest.class);
        return result;
    }

    public void testWorkedNMinutesInMonthCondition()
        throws Exception
    {
        final int empId = 15;
        // *** hard coded march days. Mar 1 is tue
        Date wrksDate = DateHelper.parseSQLDate("2005-03-07") ;
        Date monthStart = DateHelper.parseSQLDate("2005-03-01") ;
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 wrksDate,
                                 monthStart).execute(false);

        // *** create the rule
        Condition cond = new WorkedNMinutesInMonthCondition();

        Parameters params = new Parameters();
        params.addParameter(WorkedNMinutesInMonthCondition.PARAM_WORK_MINUTES,"480");
        params.addParameter(WorkedNMinutesInMonthCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        assertConditionTrue(empId, wrksDate, cond, params);

        //false case
        params.removeAllParameters();
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_WORK_MINUTES,"2400");
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_OPERATOR,RuleHelper.EQ);
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_INCLUDE_CURRENT_WORK_DATE , "false");
        assertConditionFalse(empId, wrksDate, cond, params);

    }

    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
