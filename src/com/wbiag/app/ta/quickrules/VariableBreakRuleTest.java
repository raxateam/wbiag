package com.wbiag.app.ta.quickrules;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.workbrain.app.ta.model.EmployeeScheduleData;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.ShiftBreakData;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleTestCase;
import com.workbrain.tool.overrides.InsertEmployeeScheduleOverride;
import com.workbrain.tool.overrides.InsertWorkSummaryOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;

import junit.framework.TestSuite;

/**
 * Title:			Variable Break Rule Test
 * Description:		Junit test for VariableBreakRule
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Jan 18, 2006
 * @author         	Kevin Tsoi
 */
public class VariableBreakRuleTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(VariableBreakRuleTest.class);

    public VariableBreakRuleTest(String testName)
    	throws Exception
	{
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(VariableBreakRuleTest.class);
        return result;
    }

    public void testNoBreaks()
    	throws Exception
	{
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String breakCode = "BRK";
        final String arriveEarlyCode = "GUAR";
        final String arriveLateCode = "TRN";
        final String wrkCode = "WRK";
        final String uatCode = "UAT";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule
        Rule rule = new VariableBreakRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
        ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
        ies.setWbuNameBoth("JUNIT", "JUNIT");
        ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ies.setEmpId(empId);
        ies.setStartDate(start);
        ies.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 8*60);
        Datetime sh1End = DateHelper.addMinutes(start, 10*60);
        ies.setEmpskdActStartTime(sh1Start);
        ies.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ies);

        Datetime clk1On = DateHelper.addMinutes(start , 8*60);
        Datetime clk1Off = DateHelper.addMinutes(start , 9*60);
        Datetime clk1On1 = DateHelper.addMinutes(start , 9*60+15);
        Datetime clk1Off1 = DateHelper.addMinutes(start , 10*60);


        InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
        String clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off,clk1On1,clk1Off1);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(start);
        wsOvr.setEndDate(start);
        ovrBuilder.add(wsOvr);

        ovrBuilder.execute(true , false);
        ovrBuilder.clear();

        WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

        WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
        assertTrue(workDetail.getWrkdMinutes() == 60);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

        workDetail = (WorkDetailData)wdl.get(1);
        assertTrue(workDetail.getWrkdMinutes() == 15);
        assertTrue(uatCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

        workDetail = (WorkDetailData)wdl.get(2);
        assertTrue(workDetail.getWrkdMinutes() == 45);
        assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public void test2BreaksOnTime()
		throws Exception
	{
	    OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
	    ovrBuilder.setCreatesDefaultRecords(true);

	    final int empId = 11;
	    final String breakCode = "BRK";
	    final String arriveEarlyCode = "GUAR";
	    final String arriveLateCode = "TRN";
	    final String wrkCode = "WRK";
	    final String uatCode = "UAT";
	    Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
	    int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();

	    // *** create the rule
	    Rule rule = new VariableBreakRule();
	    Parameters ruleparams = new Parameters();
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

	    clearAndAddRule(empId , start , rule , ruleparams);

	    InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
	    ies.setWbuNameBoth("JUNIT", "JUNIT");
	    ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
	    ies.setEmpId(empId);
	    ies.setStartDate(start);
	    ies.setEndDate(start);
	    Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
	    Datetime sh1End = DateHelper.addMinutes(start, 14*60);
	    ies.setEmpskdActStartTime(sh1Start);
	    ies.setEmpskdActEndTime(sh1End);

	    //create breaks
	    List breakList = new ArrayList();

	    //break 1
	    ShiftBreakData shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 10*60+30));
	    shiftBreak.setShftbrkMinutes(30);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //break 2
	    shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 12*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 12*60+30));
	    shiftBreak.setShftbrkMinutes(30);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //add breaks to override
	    EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
	    ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));

	    ovrBuilder.add(ies);

	    Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
	    Datetime clk1Off = DateHelper.addMinutes(start , 10*60);
	    Datetime clk1On1 = DateHelper.addMinutes(start , 10*60+30);
	    Datetime clk1Off1 = DateHelper.addMinutes(start , 12*60);
	    Datetime clk1On2 = DateHelper.addMinutes(start , 12*60+30);
	    Datetime clk1Off2 = DateHelper.addMinutes(start , 14*60);

	    //create clocks list
	    List clockTimes = new ArrayList();
	    clockTimes.add(clk1On);
	    clockTimes.add(clk1Off);
	    clockTimes.add(clk1On1);
	    clockTimes.add(clk1Off1);
	    clockTimes.add(clk1On2);
	    clockTimes.add(clk1Off2);

	    InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
	    String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
	    wsOvr.setWrksClocks(clks);
	    wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
	    wsOvr.setEmpId(empId);
	    wsOvr.setStartDate(start);
	    wsOvr.setEndDate(start);
	    ovrBuilder.add(wsOvr);

	    ovrBuilder.execute(true , false);
	    ovrBuilder.clear();

	    WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

	    WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
	    assertTrue(workDetail.getWrkdMinutes() == 2*60+30);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(1);
	    assertTrue(workDetail.getWrkdMinutes() == 30);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(2);
	    assertTrue(workDetail.getWrkdMinutes() == 90);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(3);
	    assertTrue(workDetail.getWrkdMinutes() == 30);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(4);
	    assertTrue(workDetail.getWrkdMinutes() == 90);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public void test2BreaksArriveEarly()
		throws Exception
	{
	    OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
	    ovrBuilder.setCreatesDefaultRecords(true);

	    final int empId = 11;
	    final String breakCode = "BRK";
	    final String arriveEarlyCode = "GUAR";
	    final String arriveLateCode = "TRN";
	    final String wrkCode = "WRK";
	    final String uatCode = "UAT";
	    Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
	    int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();

	    // *** create the rule
	    Rule rule = new VariableBreakRule();
	    Parameters ruleparams = new Parameters();
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

	    clearAndAddRule(empId , start , rule , ruleparams);

	    InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
	    ies.setWbuNameBoth("JUNIT", "JUNIT");
	    ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
	    ies.setEmpId(empId);
	    ies.setStartDate(start);
	    ies.setEndDate(start);
	    Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
	    Datetime sh1End = DateHelper.addMinutes(start, 14*60);
	    ies.setEmpskdActStartTime(sh1Start);
	    ies.setEmpskdActEndTime(sh1End);

	    //create breaks
	    List breakList = new ArrayList();

	    //break 1
	    ShiftBreakData shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 10*60+30));
	    shiftBreak.setShftbrkMinutes(30);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //break 2
	    shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 12*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 12*60+30));
	    shiftBreak.setShftbrkMinutes(30);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //add breaks to override
	    EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
	    ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));

	    ovrBuilder.add(ies);

	    Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
	    Datetime clk1Off = DateHelper.addMinutes(start , 10*60);
	    Datetime clk1On1 = DateHelper.addMinutes(start , 10*60+15);
	    Datetime clk1Off1 = DateHelper.addMinutes(start , 12*60);
	    Datetime clk1On2 = DateHelper.addMinutes(start , 12*60+30);
	    Datetime clk1Off2 = DateHelper.addMinutes(start , 14*60);

	    //create clocks list
	    List clockTimes = new ArrayList();
	    clockTimes.add(clk1On);
	    clockTimes.add(clk1Off);
	    clockTimes.add(clk1On1);
	    clockTimes.add(clk1Off1);
	    clockTimes.add(clk1On2);
	    clockTimes.add(clk1Off2);

	    InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
	    String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
	    wsOvr.setWrksClocks(clks);
	    wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
	    wsOvr.setEmpId(empId);
	    wsOvr.setStartDate(start);
	    wsOvr.setEndDate(start);
	    ovrBuilder.add(wsOvr);

	    ovrBuilder.execute(true , false);
	    ovrBuilder.clear();

	    WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

	    WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
	    assertTrue(workDetail.getWrkdMinutes() == 2*60+30);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(1);
	    assertTrue(workDetail.getWrkdMinutes() == 15);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(2);
	    assertTrue(workDetail.getWrkdMinutes() == 15);
	    assertTrue(arriveEarlyCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(3);
	    assertTrue(workDetail.getWrkdMinutes() == 90);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(4);
	    assertTrue(workDetail.getWrkdMinutes() == 30);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(5);
	    assertTrue(workDetail.getWrkdMinutes() == 90);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public void test2BreaksArriveLate()
		throws Exception
	{
	    OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
	    ovrBuilder.setCreatesDefaultRecords(true);

	    final int empId = 11;
	    final String breakCode = "BRK";
	    final String arriveEarlyCode = "GUAR";
	    final String arriveLateCode = "TRN";
	    final String wrkCode = "WRK";
	    final String uatCode = "UAT";
	    Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
	    int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();

	    // *** create the rule
	    Rule rule = new VariableBreakRule();
	    Parameters ruleparams = new Parameters();
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

	    clearAndAddRule(empId , start , rule , ruleparams);

	    InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
	    ies.setWbuNameBoth("JUNIT", "JUNIT");
	    ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
	    ies.setEmpId(empId);
	    ies.setStartDate(start);
	    ies.setEndDate(start);
	    Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
	    Datetime sh1End = DateHelper.addMinutes(start, 14*60);
	    ies.setEmpskdActStartTime(sh1Start);
	    ies.setEmpskdActEndTime(sh1End);

	    //create breaks
	    List breakList = new ArrayList();

	    //break 1
	    ShiftBreakData shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 10*60+30));
	    shiftBreak.setShftbrkMinutes(30);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //break 2
	    shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 12*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 12*60+30));
	    shiftBreak.setShftbrkMinutes(30);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //add breaks to override
	    EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
	    ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));

	    ovrBuilder.add(ies);

	    Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
	    Datetime clk1Off = DateHelper.addMinutes(start , 10*60);
	    Datetime clk1On1 = DateHelper.addMinutes(start , 10*60+45);
	    Datetime clk1Off1 = DateHelper.addMinutes(start , 12*60);
	    Datetime clk1On2 = DateHelper.addMinutes(start , 12*60+30);
	    Datetime clk1Off2 = DateHelper.addMinutes(start , 14*60);

	    //create clocks list
	    List clockTimes = new ArrayList();
	    clockTimes.add(clk1On);
	    clockTimes.add(clk1Off);
	    clockTimes.add(clk1On1);
	    clockTimes.add(clk1Off1);
	    clockTimes.add(clk1On2);
	    clockTimes.add(clk1Off2);

	    InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
	    String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
	    wsOvr.setWrksClocks(clks);
	    wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
	    wsOvr.setEmpId(empId);
	    wsOvr.setStartDate(start);
	    wsOvr.setEndDate(start);
	    ovrBuilder.add(wsOvr);

	    ovrBuilder.execute(true , false);
	    ovrBuilder.clear();

	    WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

	    WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
	    assertTrue(workDetail.getWrkdMinutes() == 2*60+30);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(1);
	    assertTrue(workDetail.getWrkdMinutes() == 30);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(2);
	    assertTrue(workDetail.getWrkdMinutes() == 15);
	    assertTrue(arriveLateCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(3);
	    assertTrue(workDetail.getWrkdMinutes() == 75);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(4);
	    assertTrue(workDetail.getWrkdMinutes() == 30);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(5);
	    assertTrue(workDetail.getWrkdMinutes() == 90);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public void test1BreakOnTime()
		throws Exception
	{
	    OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
	    ovrBuilder.setCreatesDefaultRecords(true);

	    final int empId = 11;
	    final String breakCode = "BRK";
	    final String arriveEarlyCode = "GUAR";
	    final String arriveLateCode = "TRN";
	    final String wrkCode = "WRK";
	    final String uatCode = "UAT";
	    Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
	    int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();

	    // *** create the rule
	    Rule rule = new VariableBreakRule();
	    Parameters ruleparams = new Parameters();
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

	    clearAndAddRule(empId , start , rule , ruleparams);

	    InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
	    ies.setWbuNameBoth("JUNIT", "JUNIT");
	    ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
	    ies.setEmpId(empId);
	    ies.setStartDate(start);
	    ies.setEndDate(start);
	    Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
	    Datetime sh1End = DateHelper.addMinutes(start, 14*60);
	    ies.setEmpskdActStartTime(sh1Start);
	    ies.setEmpskdActEndTime(sh1End);

	    //create breaks
	    List breakList = new ArrayList();

	    //break 1
	    ShiftBreakData shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 11*60));
	    shiftBreak.setShftbrkMinutes(60);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //add breaks to override
	    EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
	    ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));

	    ovrBuilder.add(ies);

	    Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
	    Datetime clk1Off = DateHelper.addMinutes(start , 10*60);
	    Datetime clk1On1 = DateHelper.addMinutes(start , 11*60);
	    Datetime clk1Off1 = DateHelper.addMinutes(start , 14*60);

	    //create clocks list
	    List clockTimes = new ArrayList();
	    clockTimes.add(clk1On);
	    clockTimes.add(clk1Off);
	    clockTimes.add(clk1On1);
	    clockTimes.add(clk1Off1);

	    InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
	    String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
	    wsOvr.setWrksClocks(clks);
	    wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
	    wsOvr.setEmpId(empId);
	    wsOvr.setStartDate(start);
	    wsOvr.setEndDate(start);
	    ovrBuilder.add(wsOvr);

	    ovrBuilder.execute(true , false);
	    ovrBuilder.clear();

	    WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

	    WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
	    assertTrue(workDetail.getWrkdMinutes() == 2*60+30);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(1);
	    assertTrue(workDetail.getWrkdMinutes() == 60);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(2);
	    assertTrue(workDetail.getWrkdMinutes() == 3*60);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public void test1BreakArriveEarly()
		throws Exception
	{
	    OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
	    ovrBuilder.setCreatesDefaultRecords(true);

	    final int empId = 11;
	    final String breakCode = "BRK";
	    final String arriveEarlyCode = "GUAR";
	    final String arriveLateCode = "TRN";
	    final String wrkCode = "WRK";
	    final String uatCode = "UAT";
	    Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
	    int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();

	    // *** create the rule
	    Rule rule = new VariableBreakRule();
	    Parameters ruleparams = new Parameters();
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

	    clearAndAddRule(empId , start , rule , ruleparams);

	    InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
	    ies.setWbuNameBoth("JUNIT", "JUNIT");
	    ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
	    ies.setEmpId(empId);
	    ies.setStartDate(start);
	    ies.setEndDate(start);
	    Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
	    Datetime sh1End = DateHelper.addMinutes(start, 14*60);
	    ies.setEmpskdActStartTime(sh1Start);
	    ies.setEmpskdActEndTime(sh1End);

	    //create breaks
	    List breakList = new ArrayList();

	    //break 1
	    ShiftBreakData shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 11*60));
	    shiftBreak.setShftbrkMinutes(60);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //add breaks to override
	    EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
	    ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));

	    ovrBuilder.add(ies);

	    Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
	    Datetime clk1Off = DateHelper.addMinutes(start , 10*60);
	    Datetime clk1On1 = DateHelper.addMinutes(start , 10*60+45);
	    Datetime clk1Off1 = DateHelper.addMinutes(start , 14*60);

	    //create clocks list
	    List clockTimes = new ArrayList();
	    clockTimes.add(clk1On);
	    clockTimes.add(clk1Off);
	    clockTimes.add(clk1On1);
	    clockTimes.add(clk1Off1);

	    InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
	    String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
	    wsOvr.setWrksClocks(clks);
	    wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
	    wsOvr.setEmpId(empId);
	    wsOvr.setStartDate(start);
	    wsOvr.setEndDate(start);
	    ovrBuilder.add(wsOvr);

	    ovrBuilder.execute(true , false);
	    ovrBuilder.clear();

	    WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

	    WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
	    assertTrue(workDetail.getWrkdMinutes() == 2*60+30);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(1);
	    assertTrue(workDetail.getWrkdMinutes() == 45);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(2);
	    assertTrue(workDetail.getWrkdMinutes() == 15);
	    assertTrue(arriveEarlyCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(3);
	    assertTrue(workDetail.getWrkdMinutes() == 3*60);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public void test1BreakArriveLate()
		throws Exception
	{
	    OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
	    ovrBuilder.setCreatesDefaultRecords(true);

	    final int empId = 11;
	    final String breakCode = "BRK";
	    final String arriveEarlyCode = "GUAR";
	    final String arriveLateCode = "TRN";
	    final String wrkCode = "WRK";
	    final String uatCode = "UAT";
	    Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
	    int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();

	    // *** create the rule
	    Rule rule = new VariableBreakRule();
	    Parameters ruleparams = new Parameters();
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

	    clearAndAddRule(empId , start , rule , ruleparams);

	    InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
	    ies.setWbuNameBoth("JUNIT", "JUNIT");
	    ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
	    ies.setEmpId(empId);
	    ies.setStartDate(start);
	    ies.setEndDate(start);
	    Datetime sh1Start = DateHelper.addMinutes(start, 7*60+30);
	    Datetime sh1End = DateHelper.addMinutes(start, 14*60);
	    ies.setEmpskdActStartTime(sh1Start);
	    ies.setEmpskdActEndTime(sh1End);

	    //create breaks
	    List breakList = new ArrayList();

	    //break 1
	    ShiftBreakData shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 11*60));
	    shiftBreak.setShftbrkMinutes(60);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //add breaks to override
	    EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
	    ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));

	    ovrBuilder.add(ies);

	    Datetime clk1On = DateHelper.addMinutes(start , 7*60+30);
	    Datetime clk1Off = DateHelper.addMinutes(start , 10*60);
	    Datetime clk1On1 = DateHelper.addMinutes(start , 11*60+15);
	    Datetime clk1Off1 = DateHelper.addMinutes(start , 14*60);

	    //create clocks list
	    List clockTimes = new ArrayList();
	    clockTimes.add(clk1On);
	    clockTimes.add(clk1Off);
	    clockTimes.add(clk1On1);
	    clockTimes.add(clk1Off1);

	    InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
	    String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
	    wsOvr.setWrksClocks(clks);
	    wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
	    wsOvr.setEmpId(empId);
	    wsOvr.setStartDate(start);
	    wsOvr.setEndDate(start);
	    ovrBuilder.add(wsOvr);

	    ovrBuilder.execute(true , false);
	    ovrBuilder.clear();

	    WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

	    WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
	    assertTrue(workDetail.getWrkdMinutes() == 2*60+30);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(1);
	    assertTrue(workDetail.getWrkdMinutes() == 60);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(2);
	    assertTrue(workDetail.getWrkdMinutes() == 15);
	    assertTrue(arriveLateCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(3);
	    assertTrue(workDetail.getWrkdMinutes() == 2*60+45);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public void testBreakInsideWindow()
		throws Exception
	{
	    OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
	    ovrBuilder.setCreatesDefaultRecords(true);

	    final int empId = 11;
	    final String breakCode = "BRK";
	    final String wrkCode = "WRK";
	    final String uatCode = "UAT";
	    Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
	    int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();

	    // *** create the rule
	    Rule rule = new VariableBreakRule();
	    Parameters ruleparams = new Parameters();
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

	    clearAndAddRule(empId , start , rule , ruleparams);

	    InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
	    ies.setWbuNameBoth("JUNIT", "JUNIT");
	    ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
	    ies.setEmpId(empId);
	    ies.setStartDate(start);
	    ies.setEndDate(start);
	    Datetime sh1Start = DateHelper.addMinutes(start, 7*60);
	    Datetime sh1End = DateHelper.addMinutes(start, 14*60);
	    ies.setEmpskdActStartTime(sh1Start);
	    ies.setEmpskdActEndTime(sh1End);

	    //create breaks
	    List breakList = new ArrayList();

	    //break 1
	    ShiftBreakData shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 12*60));
	    shiftBreak.setShftbrkDefStart(DateHelper.addMinutes(start , 11*60));
	    shiftBreak.setShftbrkMinutes(30);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //add breaks to override
	    EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
	    ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));

	    ovrBuilder.add(ies);

	    Datetime clk1On = DateHelper.addMinutes(start , 7*60);
	    Datetime clk1Off = DateHelper.addMinutes(start , 9*60+30);
	    Datetime clk1On1 = DateHelper.addMinutes(start , 10*60);
	    Datetime clk1Off1 = DateHelper.addMinutes(start , 14*60);

	    //create clocks list
	    List clockTimes = new ArrayList();
	    clockTimes.add(clk1On);
	    clockTimes.add(clk1Off);
	    clockTimes.add(clk1On1);
	    clockTimes.add(clk1Off1);

	    InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
	    String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
	    wsOvr.setWrksClocks(clks);
	    wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
	    wsOvr.setEmpId(empId);
	    wsOvr.setStartDate(start);
	    wsOvr.setEndDate(start);
	    ovrBuilder.add(wsOvr);

	    ovrBuilder.execute(true , false);
	    ovrBuilder.clear();

	    WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

	    WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
	    assertTrue(workDetail.getWrkdMinutes() == 2*60+30);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(1);
	    assertTrue(workDetail.getWrkdMinutes() == 30);
	    assertTrue(uatCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(2);
	    assertTrue(workDetail.getWrkdMinutes() == 4*60);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public void testBreakStartBeforeEndInsideWindow()
		throws Exception
	{
	    OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
	    ovrBuilder.setCreatesDefaultRecords(true);

	    final int empId = 11;
	    final String breakCode = "BRK";
	    final String wrkCode = "WRK";
	    final String uatCode = "UAT";
	    final String arriveEarlyCode = "GUAR";
	    final String arriveLateCode = "TRN";
	    Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
	    int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();

	    // *** create the rule
	    Rule rule = new VariableBreakRule();
	    Parameters ruleparams = new Parameters();
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

	    clearAndAddRule(empId , start , rule , ruleparams);

	    InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
	    ies.setWbuNameBoth("JUNIT", "JUNIT");
	    ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
	    ies.setEmpId(empId);
	    ies.setStartDate(start);
	    ies.setEndDate(start);
	    Datetime sh1Start = DateHelper.addMinutes(start, 7*60);
	    Datetime sh1End = DateHelper.addMinutes(start, 14*60);
	    ies.setEmpskdActStartTime(sh1Start);
	    ies.setEmpskdActEndTime(sh1End);

	    //create breaks
	    List breakList = new ArrayList();

	    //break 1
	    ShiftBreakData shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 12*60));
	    shiftBreak.setShftbrkDefStart(DateHelper.addMinutes(start , 11*60));
	    shiftBreak.setShftbrkMinutes(30);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //add breaks to override
	    EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
	    ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));

	    ovrBuilder.add(ies);

	    Datetime clk1On = DateHelper.addMinutes(start , 7*60);
	    Datetime clk1Off = DateHelper.addMinutes(start , 9*60+40);
	    Datetime clk1On1 = DateHelper.addMinutes(start , 10*60+10);
	    Datetime clk1Off1 = DateHelper.addMinutes(start , 14*60);

	    //create clocks list
	    List clockTimes = new ArrayList();
	    clockTimes.add(clk1On);
	    clockTimes.add(clk1Off);
	    clockTimes.add(clk1On1);
	    clockTimes.add(clk1Off1);

	    InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
	    String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
	    wsOvr.setWrksClocks(clks);
	    wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
	    wsOvr.setEmpId(empId);
	    wsOvr.setStartDate(start);
	    wsOvr.setEndDate(start);
	    ovrBuilder.add(wsOvr);

	    ovrBuilder.execute(true , false);
	    ovrBuilder.clear();

	    WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

	    WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
	    assertTrue(workDetail.getWrkdMinutes() == 2*60+40);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(1);
	    assertTrue(workDetail.getWrkdMinutes() == 20);
	    assertTrue(uatCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(2);
	    assertTrue(workDetail.getWrkdMinutes() == 10);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(3);
	    assertTrue(workDetail.getWrkdMinutes() == 20);
	    assertTrue(arriveEarlyCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(4);
	    assertTrue(workDetail.getWrkdMinutes() == 3*60+30);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public void testBreakStartInsideEndoutsideWindow()
		throws Exception
	{
	    OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
	    ovrBuilder.setCreatesDefaultRecords(true);

	    final int empId = 11;
	    final String breakCode = "BRK";
	    final String wrkCode = "WRK";
	    final String uatCode = "UAT";
	    final String arriveEarlyCode = "GUAR";
	    final String arriveLateCode = "TRN";
	    Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
	    int breakCodeId = getCodeMapper().getTimeCodeByName(breakCode).getTcodeId();

	    // *** create the rule
	    Rule rule = new VariableBreakRule();
	    Parameters ruleparams = new Parameters();
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_TIMECODE, breakCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, arriveEarlyCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_BREAK_ARRIVELATE_TIMECODE, arriveLateCode);
	    ruleparams.addParameter(VariableBreakRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");

	    clearAndAddRule(empId , start , rule , ruleparams);

	    InsertEmployeeScheduleOverride ies = new InsertEmployeeScheduleOverride(getConnection());
	    ies.setWbuNameBoth("JUNIT", "JUNIT");
	    ies.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
	    ies.setEmpId(empId);
	    ies.setStartDate(start);
	    ies.setEndDate(start);
	    Datetime sh1Start = DateHelper.addMinutes(start, 7*60);
	    Datetime sh1End = DateHelper.addMinutes(start, 14*60);
	    ies.setEmpskdActStartTime(sh1Start);
	    ies.setEmpskdActEndTime(sh1End);

	    //create breaks
	    List breakList = new ArrayList();

	    //break 1
	    ShiftBreakData shiftBreak = new ShiftBreakData();
	    shiftBreak.setShftbrkStartTime(DateHelper.addMinutes(start , 10*60));
	    shiftBreak.setShftbrkEndTime(DateHelper.addMinutes(start , 12*60));
	    shiftBreak.setShftbrkDefStart(DateHelper.addMinutes(start , 11*60));
	    shiftBreak.setShftbrkMinutes(30);
	    shiftBreak.setTcodeId(breakCodeId);
	    breakList.add(shiftBreak);

	    //add breaks to override
	    EmployeeScheduleData ovrSchData = ies.getEmployeeScheduleData();
	    ovrSchData.setEmpskdBrks(ovrSchData.convertBreakListToString(breakList));

	    ovrBuilder.add(ies);

	    Datetime clk1On = DateHelper.addMinutes(start , 7*60);
	    Datetime clk1Off = DateHelper.addMinutes(start , 11*60+40);
	    Datetime clk1On1 = DateHelper.addMinutes(start , 12*60+10);
	    Datetime clk1Off1 = DateHelper.addMinutes(start , 14*60);

	    //create clocks list
	    List clockTimes = new ArrayList();
	    clockTimes.add(clk1On);
	    clockTimes.add(clk1Off);
	    clockTimes.add(clk1On1);
	    clockTimes.add(clk1Off1);

	    InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
	    String clks = createWorkSummaryClockStringForOnOffs(clockTimes);
	    wsOvr.setWrksClocks(clks);
	    wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
	    wsOvr.setEmpId(empId);
	    wsOvr.setStartDate(start);
	    wsOvr.setEndDate(start);
	    ovrBuilder.add(wsOvr);

	    ovrBuilder.execute(true , false);
	    ovrBuilder.clear();

	    WorkDetailList wdl = (WorkDetailList)getWorkDetailsForDate(empId , start);

	    WorkDetailData workDetail = (WorkDetailData)wdl.get(0);
	    assertTrue(workDetail.getWrkdMinutes() == 4*60+40);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(1);
	    assertTrue(workDetail.getWrkdMinutes() == 20);
	    assertTrue(breakCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(2);
	    assertTrue(workDetail.getWrkdMinutes() == 10);
	    assertTrue(arriveLateCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));

	    workDetail = (WorkDetailData)wdl.get(3);
	    assertTrue(workDetail.getWrkdMinutes() == 60+50);
	    assertTrue(wrkCode.equalsIgnoreCase(workDetail.getWrkdTcodeName()));
	}

    public static void main(String[] args)
    	throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
