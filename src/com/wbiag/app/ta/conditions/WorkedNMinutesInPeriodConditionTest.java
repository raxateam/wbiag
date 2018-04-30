package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;

import junit.framework.*;

/**
 * Test for WorkedNMinutesInPeriodConditionTest.
 */

public class WorkedNMinutesInPeriodConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkedNMinutesInPeriodConditionTest.class);

    public WorkedNMinutesInPeriodConditionTest(String testName)
        throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedNMinutesInPeriodConditionTest.class);
        return result;
    }

    public void testWorkedNMinutesInPeriodCondition()
        throws Exception
    {
        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");
        Date end = DateHelper.addDays(start , 6) ;
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start,
                                 end).execute(false);

        // *** create the rule
        Condition cond = new WorkedNMinutesInPeriodCondition();

        Parameters params = new Parameters();
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_WORK_MINUTES,"2400");
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_APPLY_ON_UNIT,
                            DateHelper.APPLY_ON_UNIT_WEEK);
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_APPLY_ON_VALUE_START,
                            DateHelper.APPLY_ON_FIRST_DAY);
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_APPLY_ON_VALUE_END,
                            DateHelper.APPLY_ON_LAST_DAY);
        assertConditionTrue(empId, start, cond, params);

        //false case
        params.removeAllParameters();
        params.addParameter(WorkedNDaysInWeekCondition.PARAM_WORK_MINUTES,"2400");
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_OPERATOR,RuleHelper.LESS);
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_APPLY_ON_UNIT,
                            DateHelper.APPLY_ON_UNIT_WEEK);
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_APPLY_ON_VALUE_START,
                            DateHelper.APPLY_ON_FIRST_DAY);
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_APPLY_ON_VALUE_END,
                            DateHelper.APPLY_ON_LAST_DAY);
        assertConditionFalse(empId, start, cond, params);

    }

    public void testWorkedNMinutesInPeriodCondition2()
        throws Exception
    {
        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");
        Date end = DateHelper.addDays(start , 6) ;
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start,
                                 start).execute(false);

        // *** create the rule
        Condition cond = new WorkedNMinutesInPeriodCondition();

        Parameters params = new Parameters();
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_WORK_MINUTES,"0");
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_APPLY_ON_UNIT,
                            DateHelper.APPLY_ON_UNIT_DAY);
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_APPLY_ON_VALUE_START,
                            "-60");
        params.addParameter(WorkedNMinutesInPeriodCondition.PARAM_APPLY_ON_VALUE_END,
                            "1380");
        assertConditionTrue(empId, start, cond, params);


    }

    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
