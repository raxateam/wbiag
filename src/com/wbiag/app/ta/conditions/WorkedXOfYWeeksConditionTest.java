package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;
import com.workbrain.tool.overrides.*;
import junit.framework.*;

/**
 * Test for WorkedXOfYWeeksConditionTest.
 */

public class WorkedXOfYWeeksConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkedXOfYWeeksConditionTest.class);

    public WorkedXOfYWeeksConditionTest(String testName)  throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedXOfYWeeksConditionTest.class);
        return result;
    }

    public void testWorkedXOfYWeeksCondition()  throws Exception  {
        final int empId = 11;

        Date wrksDate = DateHelper.nextDay(DateHelper.addDays( DateHelper.getCurrentDate() , -7) , "Mon") ;

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
        Date ovrDate = DateHelper.addDays(wrksDate, -7*2);
        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(ovrDate);      ins.setEndDate(ovrDate);
        Datetime clk1Start = DateHelper.addMinutes(ovrDate, 10*60);
        Datetime clk1End = DateHelper.addMinutes(ovrDate, 18*60);
        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1Start , clk1End));
        ovrBuilder.add(ins);

        ovrDate = DateHelper.addDays(wrksDate, -7*1);
        ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(ovrDate);      ins.setEndDate(ovrDate);
        clk1Start = DateHelper.addMinutes(ovrDate, 10*60);
        clk1End = DateHelper.addMinutes(ovrDate, 20*60);
        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1Start , clk1End));
        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);


        Condition condition = new WorkedXOfYWeeksCondition();
        Parameters condParams = new Parameters();

        //true case
        condParams.addParameter(WorkedXOfYWeeksCondition.PARAM_MIN_WEEKS_MUST_WORKED , "1");
        condParams.addParameter(WorkedXOfYWeeksCondition.PARAM_WEEKS_TO_LOOK_BACK, "2");
        condParams.addParameter(WorkedXOfYWeeksCondition.PARAM_MIN_WORKED_MINUTES , "480");
        condParams.addParameter(WorkedXOfYWeeksCondition.PARAM_TCODENAME_LIST , "WRK");
        assertConditionTrue(empId, wrksDate, condition, condParams);

        //false case
        condParams.removeAllParameters();
        condParams.addParameter(WorkedXOfYWeeksCondition.PARAM_MIN_WEEKS_MUST_WORKED , "1");
        condParams.addParameter(WorkedXOfYWeeksCondition.PARAM_WEEKS_TO_LOOK_BACK, "2");
        condParams.addParameter(WorkedXOfYWeeksCondition.PARAM_MIN_WORKED_MINUTES , "700");
        condParams.addParameter(WorkedXOfYWeeksCondition.PARAM_TCODENAME_LIST , "WRK");
        assertConditionFalse(empId, wrksDate, condition, condParams);

    }


    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
