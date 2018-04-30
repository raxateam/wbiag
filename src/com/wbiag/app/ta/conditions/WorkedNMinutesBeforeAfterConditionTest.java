package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;
import com.workbrain.tool.overrides.*;
import junit.framework.*;

/**
 * Test for WorkedNMinutesBeforeAfterConditionTest.
 */

public class WorkedNMinutesBeforeAfterConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkedNMinutesBeforeAfterConditionTest.class);

    public WorkedNMinutesBeforeAfterConditionTest(String testName)
        throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedNMinutesBeforeAfterConditionTest.class);
        return result;
    }

    public void testWorkedNMinutesBeforeAfterCondition()
        throws Exception
    {
        final int empId = 11;
        Date wrksDate = DateHelper.getCurrentDate();
        new CreateDefaultRecords(getConnection() , new int[] {empId},  wrksDate,wrksDate).execute(false);
        Condition condition = new WorkedNMinutesBeforeAfterCondition();
        Parameters condParams = new Parameters();

        //true case
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_WORK_MINUTES , "60");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_BEFORE_TIME , "13:00");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_AFTER_TIME , "12:00");
        assertConditionTrue(empId, wrksDate, condition, condParams);

        //false case
        condParams.removeAllParameters();
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_WORK_MINUTES , "60");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_OPERATOR,RuleHelper.LESS);
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_BEFORE_TIME , "13:00");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_AFTER_TIME , "12:00");
        assertConditionFalse(empId, wrksDate, condition, condParams);
    }

    public void testWithClks()
        throws Exception
    {
        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1Start = DateHelper.addMinutes(start, 01*60);
        Datetime clk1End = DateHelper.addMinutes(start, 11*60);
        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1Start , clk1End));
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);
        assertOverrideAppliedOne(ovrBuilder);

        Condition condition = new WorkedNMinutesBeforeAfterCondition();
        Parameters condParams = new Parameters();

        //false case
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_WORK_MINUTES , "180");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_TCODE_NAME_LIST , "WRK");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_AFTER_TIME , "09:00");
        assertConditionFalse(empId, start, condition, condParams);

        //true case
        condParams.removeAllParameters();
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_WORK_MINUTES , "120");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_OPERATOR,RuleHelper.EQ);
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_BEFORE_TIME , "11:00");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_AFTER_TIME , "09:00");
        assertConditionTrue(empId, start, condition, condParams);
    }

    public void testBeforeAfterDay()
        throws Exception
    {
        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "FRI");

        // *** create defaults for 3 days ahead
        new CreateDefaultRecords(getConnection() ,
                                 new int[] {empId},
                                 start, DateHelper.addDays(start , 3)).execute(false);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1Start = DateHelper.addMinutes(start, 9*60);
        Datetime clk1End = DateHelper.addMinutes(start, 25*60);
        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1Start , clk1End));
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);
        assertOverrideAppliedOne(ovrBuilder);

        Condition condition = new WorkedNMinutesBeforeAfterCondition();
        Parameters condParams = new Parameters();

        //true case
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_WORK_MINUTES , "120");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_TCODE_NAME_LIST , "WRK");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_AFTER_TIME , "23:00");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_BEFORE_TIME , "05:00");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_AFTER_DAY , "FRIDAY");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_BEFORE_DAY , "MONDAY");
        assertConditionTrue(empId, start, condition, condParams);

        //false case
        condParams.removeAllParameters();
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_WORK_MINUTES , "60");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_TCODE_NAME_LIST , "WRK");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_AFTER_TIME , "23:00");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_BEFORE_TIME , "05:00");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_AFTER_DAY , "SATURDAY");
        condParams.addParameter(WorkedNMinutesBeforeAfterCondition.PARAM_BEFORE_DAY , "MONDAY");

        assertConditionFalse(empId, start, condition, condParams);
    }

    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
