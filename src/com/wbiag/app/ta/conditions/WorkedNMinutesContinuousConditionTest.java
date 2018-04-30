package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.quickrules.EmployeeShiftPremiumRule;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.InsertWorkDetailOverride;
import com.workbrain.tool.overrides.InsertWorkSummaryOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.*;

import java.util.*;

import junit.framework.*;

/**
 * Test for WorkedNMinutesContinuousConditionTest.
 */

public class WorkedNMinutesContinuousConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkedNMinutesContinuousConditionTest.class);

    public WorkedNMinutesContinuousConditionTest(String testName) 
        throws Exception 
    {
        super(testName);
    }

    public static TestSuite suite() 
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedNMinutesContinuousConditionTest.class);
        return result;
    }

    public void testWorkedNMinutesContinuousCondition() 
        throws Exception 
    {
        final int empId = 15;
        Date fri = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "FRI");
        Date mon = DateHelper.addDays(fri , -4);

        new CreateDefaultRecords(getConnection(), new int[] {empId}, mon, fri).execute(false);

        Condition cond = new WorkedNMinutesContinuousCondition();
        Parameters params = new Parameters();
        
        //true case     
        params.addParameter(WorkedNMinutesContinuousCondition.PARAM_WORK_MINUTES,"480");
        params.addParameter(WorkedNMinutesContinuousCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);             
        params.addParameter(WorkedNMinutesContinuousCondition.PARAM_TIME_SPAN,WorkedNMinutesContinuousCondition.CHOICE_WITHIN_SCHEDULE);             
        params.addParameter(WorkedNMinutesContinuousCondition.PARAM_TCODE_NAME_LIST,"WRK");       
        assertConditionTrue(empId, fri, cond, params);

        //false case
        params.removeAllParameters();       
        params.addParameter(WorkedNMinutesContinuousCondition.PARAM_WORK_MINUTES,"480");
        params.addParameter(WorkedNMinutesContinuousCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        params.addParameter(WorkedNMinutesContinuousCondition.PARAM_TCODE_NAME_LIST,"UAT");       
        params.addParameter(WorkedNMinutesContinuousCondition.PARAM_TIME_SPAN,WorkedNMinutesContinuousCondition.CHOICE_WITHIN_SCHEDULE);             
        assertConditionFalse(empId, fri, cond, params);        
    }
    
    public void testWorkedNMinutesNotContinuous() throws Exception {

		final int empId = 15;
		Date fri = DateHelper.nextDay(DateHelper.addDays(DateHelper
				.getCurrentDate(), -7), "FRI");
		Date mon = DateHelper.addDays(fri, -4);

		new CreateDefaultRecords(getConnection(), new int[] { empId }, mon, fri)
				.execute(false);

		Date start = mon;
		final String jobName = "JANITOR";

		Condition cond = new WorkedNMinutesContinuousCondition();
		Parameters params = new Parameters();

		// true case
		params.addParameter(
				WorkedNMinutesContinuousCondition.PARAM_WORK_MINUTES, "360");
		params.addParameter(WorkedNMinutesContinuousCondition.PARAM_OPERATOR,
				RuleHelper.BIGGEREQ);
		params.addParameter(WorkedNMinutesContinuousCondition.PARAM_TIME_SPAN,
				WorkedNMinutesContinuousCondition.CHOICE_ALL);
		params.addParameter(
				WorkedNMinutesContinuousCondition.PARAM_TCODE_NAME_LIST, "WRK");
		// params.addParameter(WorkedNMinutesContinuousCondition.PARAM_CONTINUE_TCODE_NAME_LIST,"WRK");

		Rule rule = new EmployeeShiftPremiumRule();
		Parameters ruleparams = new Parameters();
		clearAndAddRule(empId, start, rule, ruleparams, cond,
				params == null ? new Parameters() : params);

		OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
		ovrBuilder.setCreatesDefaultRecords(true);

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(
				getConnection());
		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(start);
		ins.setEndDate(start);
		Datetime st1 = DateHelper.addMinutes(start, 540);
		Datetime end1 = DateHelper.addMinutes(start, 540 + 240);
		Datetime st2 = DateHelper.addMinutes(start, 540 + 240 + 30);
		Datetime end2 = DateHelper.addMinutes(start, 540 + 240 + 30 + 240);
		ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(st1, end1, st2,
				end2));
		ins.setStartTime(st1);
		ins.setEndTime(end2);
		ovrBuilder.add(ins);

		ovrBuilder.setFirstStartDate(start);
		ovrBuilder.setLastEndDate(start);
		IntegerList emps = new IntegerList();
		emps.add(empId);
		ovrBuilder.setEmpIdList(emps);
		ovrBuilder.execute(true, false);

		if (isRuleApplied(empId, start, rule)) {
			throw new RuntimeException("Condition (" + cond.getComponentName()
					+ ") was evaluated to TRUE");
		}

		// assertConditionFalse(empId, start, cond, params);

	}

    public static void main(String[] args) 
        throws Exception 
    {
        junit.textui.TestRunner.run(suite());
    }
}
