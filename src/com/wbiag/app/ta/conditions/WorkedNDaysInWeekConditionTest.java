package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;

import junit.framework.*;

/**
 * Test for WorkedNDaysInWeekConditionTest.
 */

public class WorkedNDaysInWeekConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkedNDaysInWeekConditionTest.class);

    public WorkedNDaysInWeekConditionTest(String testName)
        throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedNDaysInWeekConditionTest.class);
        return result;
    }

    public void testWorkedNDaysInWeekCondition()
        throws Exception
    {
        final int empId = 15;
        Date wrksDate = DateHelper.nextDay(DateHelper.getCurrentDate() , "THU");
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 DateHelper.addDays(wrksDate , -3),
                                 DateHelper.addDays(wrksDate , 3)).execute(false);

        Condition cond = new WorkedNDaysInWeekCondition();
        Parameters params = new Parameters();

        //true case
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_NUM_OF_DAYS,"1");
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_WORK_MINUTES,"480");
        assertConditionTrue(empId, wrksDate, cond, params);

        //false case
        params.removeAllParameters();
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_NUM_OF_DAYS,"1");
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_OPERATOR,RuleHelper.LESS);
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_WORK_MINUTES,"480");
        assertConditionFalse(empId, wrksDate, cond, params);

        //false case
        params.removeAllParameters();
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_NUM_OF_DAYS,"4");
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_OPERATOR,RuleHelper.EQ);
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_WORK_MINUTES,"480");
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_INCLUDE_CURRENT_WORK_DATE,"false");
        assertConditionFalse(empId, wrksDate, cond, params);

    }

    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
