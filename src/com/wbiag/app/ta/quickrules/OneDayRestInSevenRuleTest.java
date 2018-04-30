/*
 * Created on Mar 13, 2006
 *
 * State Pay Rules Project
 * Meal Break Rule Test Case
 * Description:  J-Unit tests for Meal Break Rule.
 *
 */

package com.wbiag.app.ta.quickrules;

import java.util.*;

import junit.framework.*;

import com.workbrain.tool.overrides.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 *  Title:        Meal Break Rule Test
 *  Description:  J-Unit tests for Meal Break Rule.
 *  Copyright:    Copyright (c) 2006
 *  Company:      Workbrain Inc
 *
 *@deprecated As of 5.0.2.0, use core classes 
 *@version    1.0
 *
 */
public class OneDayRestInSevenRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(OneDayRestInSevenRuleTest.class);

	DBConnection dbConnection;			//Database Connection
	Date start;
	final int empId = 11;

    public OneDayRestInSevenRuleTest(String testName) throws Exception {
        super(testName);
        dbConnection = getConnection();
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(OneDayRestInSevenRuleTest.class);
        return result;
    }

    /**
     * Case #1
     * 	- Description
     * 			- Employee is schedule from 9am-17pm Monday - Saturday.  System tries to added schedule
     * 			  override on Sunday, however One Day Rest in Seven Rule, throws an exception not allowing
     *			  the override to go through.
     *
     * 	- Result:
     * 			- Rule should be appiled on ALL days.
     */

    private void setParameters(Parameters params,String val1, String val2, String val3, String val4,
    		String val5, String val6, String val7, String val8, String val9, String val10, String val11,
    		String val12)
    {
    	params.addParameter(OneDayRestInSevenRule.PARAM_LIMIT_TO_CALENDAR_WEEK, val1);
    	params.addParameter(OneDayRestInSevenRule.PARAM_DAY_WEEK_START, val2);
    	params.addParameter(OneDayRestInSevenRule.PARAM_DAYS_TO_LOOK_BACK, val3);
    	params.addParameter(OneDayRestInSevenRule.PARAM_MIN_WORKED_HOURS, val4);
    	params.addParameter(OneDayRestInSevenRule.PARAM_VALID_WORK_TIME_CODE, val5);
    	params.addParameter(OneDayRestInSevenRule.PARAM_VALID_WORK_HOUR_TYPE, val6);
    	params.addParameter(OneDayRestInSevenRule.PARAM_VALID_NONWORK_NONREST_TIME_CODE, val7);
    	params.addParameter(OneDayRestInSevenRule.PARAM_VALID_NONWORK_NONREST_HOUR_TYPE, val8);
    	params.addParameter(OneDayRestInSevenRule.PARAM_SPLIT_TIME, val9);
    	params.addParameter(OneDayRestInSevenRule.PARAM_MIN_REST_PERIOD_DURATION, val10);
    	params.addParameter(OneDayRestInSevenRule.PARAM_REST_PERIOD_START_TIME, val11);
    	params.addParameter(OneDayRestInSevenRule.PARAM_REST_PERIOD_END_TIME, val12);

    }
    public void testCase1() throws Exception
    {
    	logger.debug("START TEST CASE #1 - Trying scheduling employees for seven days in a row.");
    	start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -14), "MON");

    	int shiftId=getShiftID(1);

    	OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
		ovrBuilder.setCreatesDefaultRecords(true);

		/*Set up Rule*/
        Rule ruleObject = new OneDayRestInSevenRule();
        Parameters params = new Parameters();

        params.removeAllParameters();
        setParameters(params,"TRUE", "MONDAY", "7", "20", "WRK", "REG,OT1,OT2,OT3,OTS", "VAC,HOL,SICK",
        		"REG,UNPAID", "19000101 000000", "24", "19000101 080000", "19000101 170000");

        //Insert Employee Override to set the flag
        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmp.setEmpId(empId);

        insEmp.setStartDate(start);
        insEmp.setEndDate(start);
        insEmp.setEmpVal2("FALSE");
        ovrBuilder.add(insEmp);

        InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(getConnection());
        insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setEmpskdActShiftId(shiftId);
		Datetime skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		Datetime skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pm

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(dbConnection);
		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);

		Datetime clksArr[] = new Datetime[2];

		String clks;

		/**************************************************************/
        //Monday Schedule
		clearAndAddRule(empId, start, ruleObject, params);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Tuesday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Wednesday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Thursday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Friday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Saturday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Sunday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		ovrBuilder.clear();
		logger.debug("END TEST CASE #1 - Trying scheduling employees for seven days in a row.");
    }

    /**
     * Case #2
     * 	- Description
     * 			- Employee is schedule from 9am-17pm Monday - Thursday, Saturday and Sunday.  System tries to
     * 			  added schedule override on friday, however One Day Rest in Seven Rule, throws an exception not allowing
     *			  the override to go through.
     *
     * 	- Result:
     * 			- Rule should be appiled on all days but Friday.
     */
    public void testCase2() throws Exception
    {
    	logger.debug("START TEST CASE #2 - Trying scheduling employees for seven days in a row.");
    	start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -14), "MON");

    	int shiftId=getShiftID(1);

    	OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
		ovrBuilder.setCreatesDefaultRecords(true);

		/*Set up Rule*/
        Rule ruleObject = new OneDayRestInSevenRule();
        Parameters params = new Parameters();

        params.removeAllParameters();
        setParameters(params,"TRUE", "MONDAY", "7", "20", "WRK", "REG,OT1,OT2,OT3,OTS", "VAC,HOL,SICK",
        		"REG,UNPAID", "19000101 000000", "24", "19000101 080000", "19000101 170000");

        //Insert Employee Override to set the flag
        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmp.setEmpId(empId);

        insEmp.setStartDate(start);
        insEmp.setEndDate(start);
        insEmp.setEmpVal2("FALSE");
        ovrBuilder.add(insEmp);

        InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(getConnection());
        insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setEmpskdActShiftId(shiftId);
		Datetime skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		Datetime skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pm

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(dbConnection);
		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);

		Datetime clksArr[] = new Datetime[2];

		String clks;

		/**************************************************************/
        //Monday Schedule
		clearAndAddRule(empId, start, ruleObject, params);

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Tuesday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);


		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Wednesday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Thursday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Saturday Schedule
		start = DateHelper.addDays(start, 2);
		clearAndAddRule(empId, start, ruleObject, params);

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Sunday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);
		RuleEngine.runCalcGroup(getConnection() , empId, start, start, false) ;

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);

		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		RuleEngine.runCalcGroup(getConnection() , empId, start, start, false) ;
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		RuleEngine.runCalcGroup(getConnection() , empId, DateHelper.addDays(start, -7), start, false) ;
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Friday Schedule
		start = DateHelper.addDays(start, -2);
		clearAndAddRule(empId, start, ruleObject, params);
		RuleEngine.runCalcGroup(getConnection() , empId, start, start, false) ;

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);

		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		RuleEngine.runCalcGroup(getConnection() , empId, start, start, false) ;
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		RuleEngine.runCalcGroup(getConnection() , empId, DateHelper.addDays(start, -7), start, false) ;
		assertRuleApplied(empId, start, ruleObject);

		ovrBuilder.clear();
		logger.debug("END TEST CASE #2 - Trying scheduling employees for seven days in a row.");
    }
    /*Method has been taken from WeeklySplitOT Junit Test Class in the WBIAG Jar.*/


    /**
     * Case #3
     *
     * 	- Description
     * 			- LIMIT TO CALENDAR WEEK = FALSE
     * 			- Start schedule the employee on wednesday of one week and loop into next
     *
     * 	- Result:
     * 			- Rule should be appiled on all days but OFF Day.
     */
    public void testCase3() throws Exception
    {
    	logger.debug("START TEST CASE #3 - Trying scheduling employees for seven days in a row.");
    	start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -14), "WED");

    	System.out.println("CASE 3");

    	int shiftId=getShiftID(1);

    	OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
		ovrBuilder.setCreatesDefaultRecords(true);

		/*Set up Rule*/
        Rule ruleObject = new OneDayRestInSevenRule();
        Parameters params = new Parameters();

        params.removeAllParameters();
        setParameters(params,"TRUE", "MONDAY", "7", "20", "WRK", "REG,OT1,OT2,OT3,OTS", "VAC,HOL,SICK",
        		"REG,UNPAID", "19000101 000000", "24", "19000101 080000", "19000101 170000");


        //Insert Employee Override to set the flag
        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmp.setEmpId(empId);

        insEmp.setStartDate(start);
        insEmp.setEndDate(start);
        insEmp.setEmpVal2("FALSE");
        ovrBuilder.add(insEmp);


        InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(getConnection());
        insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setEmpskdActShiftId(shiftId);
		Datetime skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		Datetime skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pm

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(dbConnection);
		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);

		Datetime clksArr[] = new Datetime[2];

		String clks;

		/**************************************************************/
        //Wednesday Schedule
		clearAndAddRule(empId, start, ruleObject, params);

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Thursday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);


		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Friday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Saturday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Sunday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Monday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);
		RuleEngine.runCalcGroup(getConnection() , empId, start, start, false) ;

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		RuleEngine.runCalcGroup(getConnection() , empId, start, start, false) ;
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		RuleEngine.runCalcGroup(getConnection() , empId, DateHelper.addDays(start, -7), start, false) ;
		assertRuleApplied(empId, start, ruleObject);

		/**************************************************************/
        //Tuesday Schedule
		start = DateHelper.addDays(start, 1);
		clearAndAddRule(empId, start, ruleObject, params);
		RuleEngine.runCalcGroup(getConnection() , empId, start, start, false) ;

		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		skdStart = DateHelper.addMinutes(start, 540);		//9:00am
		skdEnd = DateHelper.addMinutes(start, 1020);		//17:00pms
		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);
		ovrBuilder.add(insSkd);


		ins.setStartDate(start);
		ins.setEndDate(start);
		clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
		clksArr[0] = DateHelper.addMinutes(start, 540);// on
		clksArr[1] = DateHelper.addMinutes(start, 1020);// off
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		RuleEngine.runCalcGroup(getConnection() , empId, start, start, false) ;
		ovrBuilder.execute(true, false);

		/*Assert Statements*/
		RuleEngine.runCalcGroup(getConnection() , empId, DateHelper.addDays(start, -7), start, false) ;
		assertRuleApplied(empId, start, ruleObject);

		ovrBuilder.clear();
		logger.debug("END TEST CASE #3 - Trying scheduling employees for seven days in a row.");
    }



    private int getShiftID(int shiftTestNo)throws Exception
    {
        int shiftId=0;
        if (shiftTestNo==1)
        {
            ShiftAccess sha1=new ShiftAccess(dbConnection);

            ShiftData shiftData1=new ShiftData();

            shiftData1.setShftStartTime(DateHelper.addMinutes(start, 540));
            shiftData1.setShftEndTime(DateHelper.addMinutes(start, 1020));
            shiftData1.setShftName("J-Unit Day Shift - 10");
            shiftData1.setGeneratesPrimaryKeyValue(true);
            shiftData1.setShftDesc("Testing SVP Rule");
            shiftData1.setColrId(0);
            shiftData1.setShftgrpId(0);

            sha1.insert(shiftData1);
            shiftId=shiftData1.getShftId();
            return shiftId;
        }
        else
        {
            ShiftAccess sha1=new ShiftAccess(dbConnection);
            ShiftData shiftData1=new ShiftData();

            shiftData1.setShftStartTime(DateHelper.addMinutes(start, 540));
            shiftData1.setShftEndTime(DateHelper.addMinutes(start, 600));
            shiftData1.setShftName("J-Unit Night Shift"); //shiftData.setShftgrpId(46);
            shiftData1.setGeneratesPrimaryKeyValue(true);
            shiftData1.setShftDesc("Testing SVP Rules");
            shiftData1.setColrId(0);
            shiftData1.setShftgrpId(0);

            sha1.insert(shiftData1);
            shiftId=shiftData1.getShftId();

            return shiftId;
        }
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

