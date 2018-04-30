package com.wbiag.app.ta.quickrules;

import java.util.Arrays;
import java.util.Date;
import junit.framework.TestSuite;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.ShiftBreakAccess;
import com.workbrain.app.ta.model.HourTypeData;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.ShiftBreakData;
import com.workbrain.app.ta.model.TimeCodeData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleTestCase;
import com.workbrain.sql.DBConnection;
import com.workbrain.tool.overrides.InsertEmployeeOverride;
import com.workbrain.tool.overrides.InsertEmployeeScheduleOverride;
import com.workbrain.tool.overrides.InsertWorkSummaryOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;
import com.workbrain.util.RegistryHelper;
//import com.workbrain.server.registry.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
//import com.workbrain.sql.*;
/**
 *
 * Test for ArrangeBreakMultipleRule.
 *
 */

public class ArrangeBreakMultipleRuleTest extends RuleTestCase {

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(ArrangeBreakMultipleRuleTest.class);

	Date start;

	DBConnection dbConnection;

	//int shiftId = 10065;
	//int shiftId2 = 10061; // [ for Test Case 5 & 6 ]

	public ArrangeBreakMultipleRuleTest(String testName) throws Exception {
		super(testName);
		start = DateHelper.nextDay(DateHelper.addDays(DateHelper
				.getCurrentDate(), -7), "MON");
		dbConnection = getConnection();


	}

	public static TestSuite suite() {
		TestSuite result = new TestSuite();
		result.addTestSuite(ArrangeBreakMultipleRuleTest.class);
       	return result;
	}

	/**
	 * @throws Exception
	 */


	public void testLunchBreakRule1() throws Exception {
		if (logger.isDebugEnabled()) logger.debug("******STARTING UNIT TEST NO 1  ...PARAM_BREAK_MINUTES_UP_TO starts with 'S'. TAKING BREAK MINUTES FROM SHIFT *******");

		OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
		ovrBuilder.setCreatesDefaultRecords(true);

		final int empId = 3;// * was 10
		final String brkCode = "BRK", aeCode = "PGR", alCode = "UGR";
		int brkMinutes = 0;// was 30
		int shiftId=getShiftID(1);

		// *** create the rule
		final CodeMapper cm = CodeMapper.createBrandNewCodeMapper(dbConnection);

		Rule rule = new ArrangeBreakMultipleRule();
		Parameters ruleparams = new Parameters();
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS,"true");
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "99");
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES,String.valueOf(brkMinutes));
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE,brkCode);
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, aeCode);
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVELATE_TIMECODE, alCode);

		clearAndAddRule(empId, start, rule, ruleparams);

		ShiftBreakData sbd = new ShiftBreakData();
		sbd.setShftbrkId(0);
		sbd.setShftbrkMinutes(60);
		sbd.setShftbrkStartTime(DateHelper.addMinutes(start, 11 * 60));
		sbd.setShftbrkEndTime(DateHelper.addMinutes(start, 12 * 60));
		sbd.setShftId(shiftId);
		final TimeCodeData tcd = cm.getTimeCodeByName("UAT");
		final HourTypeData htd = cm.getHourTypeByName("UNPAID");
		sbd.setTcodeId(tcd.getTcodeId());
		sbd.setHtypeId(htd.getHtypeId());

		ShiftBreakAccess sba = new ShiftBreakAccess(dbConnection);
		sba.insert(sbd);

		InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(
				dbConnection);
		insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);

		insSkd.setEmpskdActShiftId(shiftId);

		Datetime skdStart = DateHelper.addMinutes(start, 8 * 60);
		Datetime skdEnd = DateHelper.addMinutes(start, 17 * 60);

		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);

		ovrBuilder.add(insSkd);

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(
				dbConnection);

		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(start);
		ins.setEndDate(start);

		Datetime clksArr[] = new Datetime[10];
		clksArr[0] = DateHelper.addMinutes(start, 8 * 60);// on

		clksArr[1] = DateHelper.addMinutes(start, 10 * 60 + 45);// off
		clksArr[2] = DateHelper.addMinutes(start, 12 * 60);// on
		clksArr[3] = DateHelper.addMinutes(start, 17 * 60);// off

		String clks = createWorkSummaryClockStringForOnOffs(Arrays
				.asList(clksArr));
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

		if (logger.isDebugEnabled()) logger.debug("schdata=" + insSkd.getEmployeeScheduleData());

		assertOverrideAppliedCount(ovrBuilder, 2);
		// assertRuleApplied(empId, start, rule);
/*
		WorkDetailList wds = getWorkDetailsForDate(empId, start);
		// System.out.println(wds.toDescription());
		assertTrue(wds.size() > 0);
		// *** BRK1
		int brkIndex = wds.getFirstRecordIndex(clksArr[1], DateHelper
				.addMinutes(clksArr[1], brkMinutes), false);

		assertTrue(brkIndex != -1);
		assertEquals("Must be break code", brkCode, wds.getWorkDetail(brkIndex)
				.getWrkdTcodeName());

		// *** BRK2

		brkIndex = wds.getFirstRecordIndex(clksArr[3], clksArr[4], false);

		assertTrue(brkIndex != -1);
		assertEquals("Must be break code", brkCode, wds.getWorkDetail(brkIndex)
				.getWrkdTcodeName());

		// *** BRK2

		brkIndex = wds.getFirstRecordIndex(clksArr[7], clksArr[8], false);
		assertTrue(brkIndex != -1);
		assertEquals("Must be break code", brkCode, wds.getWorkDetail(brkIndex)
				.getWrkdTcodeName());
*/
	}

	public void testLunchBreakRule2() throws Exception {
		logger
				.debug("******STARTING UNIT TEST NO 2  ...PARAM_BREAK_MINUTES_UP_TO starts with 'S'. TAKING BREAK MINUTES FROM SHIFT *******");

		OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
		ovrBuilder.setCreatesDefaultRecords(true);
        int shiftId=getShiftID(1);
		final int empId = 3;// * was 10
		final String brkCode = "BRK", aeCode = "PGR", alCode = "UGR";
		int brkMinutes = 0;// was 30

		// *** create the rule
		final CodeMapper cm = CodeMapper.createBrandNewCodeMapper(dbConnection);

		Rule rule = new ArrangeBreakMultipleRule();
		Parameters ruleparams = new Parameters();
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS,"true");
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "99");
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES,
				String.valueOf(brkMinutes));
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE,
				brkCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, aeCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVELATE_TIMECODE, alCode);

		// ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ASSIGN_MINUTES,
		// "60");
		RegistryHelper regHelper = new RegistryHelper();
		regHelper.setVar("system/WORKBRAIN_PARAMETERS/ALLOW_PARTIAL_BREAKS",
				"TRUE");

		regHelper.setVar("system/WORKBRAIN_PARAMETERS/NO_SWIPE_FOR_BREAKS",
				"FALSE");

		clearAndAddRule(empId, start, rule, ruleparams);

		ShiftBreakData sbd = new ShiftBreakData();
		sbd.setShftbrkId(0);
		sbd.setShftbrkMinutes(60);
		sbd.setShftbrkStartTime(DateHelper.addMinutes(start, 11 * 60));
		sbd.setShftbrkEndTime(DateHelper.addMinutes(start, 12 * 60));
		sbd.setShftId(shiftId);
		final TimeCodeData tcd = cm.getTimeCodeByName("UAT");
		final HourTypeData htd = cm.getHourTypeByName("UNPAID");
		sbd.setTcodeId(tcd.getTcodeId());
		sbd.setHtypeId(htd.getHtypeId());

		ShiftBreakAccess sba = new ShiftBreakAccess(dbConnection);
		sba.insert(sbd);

		InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(
				dbConnection);
		insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);

		insSkd.setEmpskdActShiftId(shiftId);

		Datetime skdStart = DateHelper.addMinutes(start, 8 * 60);
		Datetime skdEnd = DateHelper.addMinutes(start, 17 * 60);

		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);

		ovrBuilder.add(insSkd);

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(
				dbConnection);

		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(start);
		ins.setEndDate(start);

		Datetime clksArr[] = new Datetime[6];
		clksArr[0] = DateHelper.addMinutes(start, 8 * 60);// on
		clksArr[1] = DateHelper.addMinutes(start, 9 * 60 + 30);// off
		clksArr[2] = DateHelper.addMinutes(start, 10 * 60);// on
		clksArr[3] = DateHelper.addMinutes(start, 11 * 60);// off
		clksArr[4] = DateHelper.addMinutes(start, 12 * 60);// off
		clksArr[5] = DateHelper.addMinutes(start, 17 * 60);// off

		String clks = createWorkSummaryClockStringForOnOffs(Arrays
				.asList(clksArr));
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);
		if (logger.isDebugEnabled()) logger.debug("schdata=" + insSkd.getEmployeeScheduleData());

	}

	public void testLunchBreakRule3() throws Exception {
		logger
				.debug("******STARTING UNIT TEST NO 3  ...PARAM_BREAK_MINUTES_UP_TO starts with 'S'. TAKING BREAK MINUTES FROM SHIFT *******");

		OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
		ovrBuilder.setCreatesDefaultRecords(true);

		final int empId = 3;// * was 10
		final String brkCode = "BRK", aeCode = "PGR", alCode = "UGR";
		int brkMinutes = 0;// was 30
        int shiftId=getShiftID(1);

		// *** create the rule
		final CodeMapper cm = CodeMapper.createBrandNewCodeMapper(dbConnection);

		Rule rule = new ArrangeBreakMultipleRule();
		Parameters ruleparams = new Parameters();
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS,
				"true");
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "99");
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES,
				String.valueOf(brkMinutes));
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE,
				brkCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, aeCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVELATE_TIMECODE, alCode);

		// ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ASSIGN_MINUTES,
		// "60");
		RegistryHelper regHelper = new RegistryHelper();
		regHelper.setVar("system/WORKBRAIN_PARAMETERS/ALLOW_PARTIAL_BREAKS",
				"TRUE");

		regHelper.setVar("system/WORKBRAIN_PARAMETERS/NO_SWIPE_FOR_BREAKS",
				"FALSE");

		clearAndAddRule(empId, start, rule, ruleparams);

		ShiftBreakData sbd = new ShiftBreakData();
		sbd.setShftbrkId(0);
		sbd.setShftbrkMinutes(60);
		sbd.setShftbrkStartTime(DateHelper.addMinutes(start, 11 * 60));
		sbd.setShftbrkEndTime(DateHelper.addMinutes(start, 12 * 60));
		sbd.setShftId(shiftId);
		final TimeCodeData tcd = cm.getTimeCodeByName("UAT");
		final HourTypeData htd = cm.getHourTypeByName("UNPAID");
		sbd.setTcodeId(tcd.getTcodeId());
		sbd.setHtypeId(htd.getHtypeId());

		ShiftBreakAccess sba = new ShiftBreakAccess(dbConnection);
		sba.insert(sbd);

		InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(
				dbConnection);
		insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);

		insSkd.setEmpskdActShiftId(shiftId);

		Datetime skdStart = DateHelper.addMinutes(start, 8 * 60);
		Datetime skdEnd = DateHelper.addMinutes(start, 17 * 60);

		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);

		ovrBuilder.add(insSkd);

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(
				dbConnection);

		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(start);
		ins.setEndDate(start);

		Datetime clksArr[] = new Datetime[4];
		clksArr[0] = DateHelper.addMinutes(start, 8 * 60);// on
		clksArr[1] = DateHelper.addMinutes(start, 12 * 60 + 30);// off
		clksArr[2] = DateHelper.addMinutes(start, 13 * 60);// on
		clksArr[3] = DateHelper.addMinutes(start, 17 * 60);// off

		String clks = createWorkSummaryClockStringForOnOffs(Arrays
				.asList(clksArr));
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

	}

	public void testLunchBreakRule4() throws Exception {
		logger
				.debug("******");
		if (logger.isDebugEnabled()) logger.debug("STARTING UNIT TEST NO 4  ...PARAM_BREAK_MINUTES_UP_TO starts with 'S'. TAKING BREAK MINUTES FROM SHIFT *******");

        int shiftId=getShiftID(1);
		OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
		ovrBuilder.setCreatesDefaultRecords(true);
		/*
		ShiftAccess sha=new ShiftAccess(dbConnection);
		ShiftData shiftData=new ShiftData();
		shiftId = dbConnection.getDBSequence("seq_shft_id").getNextValue();

		shiftData.setShftStartTime(DateHelper.addMinutes(start, 8*60));
		shiftData.setShftEndTime(DateHelper.addMinutes(start, 17*60));
		shiftData.setShftId(shiftId);
		shiftData.setShftName("Test case 4"); //shiftData.setShftgrpId(46);
		shiftData.setGeneratesPrimaryKeyValue(true);
		shiftData.setShftDesc("Test case 4");
		sha.insert(shiftData);

		*/

		final int empId = 3;// * was 10
		final String brkCode = "BRK", aeCode = "PGR", alCode = "UGR";
		int brkMinutes = 0;// was 30

		// *** create the rule
		final CodeMapper cm = CodeMapper.createBrandNewCodeMapper(dbConnection);

		Rule rule = new ArrangeBreakMultipleRule();
		Parameters ruleparams = new Parameters();
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS,
				"true");
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "99");
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES,
				String.valueOf(brkMinutes));
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE,
				brkCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, aeCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVELATE_TIMECODE, alCode);

		RegistryHelper regHelper = new RegistryHelper();
		regHelper.setVar("system/WORKBRAIN_PARAMETERS/ALLOW_PARTIAL_BREAKS",
				"TRUE");

		regHelper.setVar("system/WORKBRAIN_PARAMETERS/NO_SWIPE_FOR_BREAKS",
				"FALSE");

		clearAndAddRule(empId, start, rule, ruleparams);

		ShiftBreakData sbd = new ShiftBreakData();
		int shftBrkId = dbConnection.getDBSequence("seq_shftbrk_id").getNextValue();
		sbd.setShftbrkId(shftBrkId);
		sbd.setShftbrkMinutes(60);
		sbd.setShftbrkStartTime(DateHelper.addMinutes(start, 11 * 60));
		sbd.setShftbrkEndTime(DateHelper.addMinutes(start, 12 * 60));
		sbd.setShftId(shiftId);
		final TimeCodeData tcd = cm.getTimeCodeByName("UAT");
		final HourTypeData htd = cm.getHourTypeByName("UNPAID");
		sbd.setTcodeId(tcd.getTcodeId());
		sbd.setHtypeId(htd.getHtypeId());

		ShiftBreakAccess sba = new ShiftBreakAccess(dbConnection);
		sba.insert(sbd);

		InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(
				dbConnection);
		insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);
		// if (logger.isDebugEnabled()) logger.debug("shiftData.getShftId())="+shiftData.getShftId());
		insSkd.setEmpskdActShiftId(shiftId);

		Datetime skdStart = DateHelper.addMinutes(start, 8 * 60);
		Datetime skdEnd = DateHelper.addMinutes(start, 17 * 60);

		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);

		ovrBuilder.add(insSkd);

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(
				dbConnection);

		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(start);
		ins.setEndDate(start);

		Datetime clksArr[] = new Datetime[2];
		clksArr[0] = DateHelper.addMinutes(start, 8 * 60);// on
		clksArr[1] = DateHelper.addMinutes(start, 17 * 60);// off

		String clks = createWorkSummaryClockStringForOnOffs(Arrays
				.asList(clksArr));
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);
		if (logger.isDebugEnabled()) logger.debug("schdata=" + insSkd.getEmployeeScheduleData());

	}

	public void testLunchBreakRule5() throws Exception {
		logger
				.debug("******STARTING UNIT TEST NO 5  ...PARAM_BREAK_MINUTES_UP_TO starts with 'S'. TAKING BREAK MINUTES FROM SHIFT *******");

		OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
		ovrBuilder.setCreatesDefaultRecords(true);

		final int empId = 3;// * was 10
		final String brkCode = "BRK", aeCode = "PGR", alCode = "UGR";
		int brkMinutes = 0;// was 30
        int shiftId2=getShiftID(2);
		// *** create the rule
		final CodeMapper cm = CodeMapper.createBrandNewCodeMapper(dbConnection);

		Rule rule = new ArrangeBreakMultipleRule();
		Parameters ruleparams = new Parameters();
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS,
				"true");
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "99");
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES,
				String.valueOf(brkMinutes));
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE,
				brkCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, aeCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVELATE_TIMECODE, alCode);

		// ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ASSIGN_MINUTES,
		// "60");
		RegistryHelper regHelper = new RegistryHelper();
		regHelper.setVar("system/WORKBRAIN_PARAMETERS/ALLOW_PARTIAL_BREAKS",
				"TRUE");

		regHelper.setVar("system/WORKBRAIN_PARAMETERS/NO_SWIPE_FOR_BREAKS",
				"FALSE");
		clearAndAddRule(empId, start, rule, ruleparams);

		ShiftBreakData sbd = new ShiftBreakData();
		sbd.setShftbrkId(0);
		sbd.setShftbrkMinutes(30);
		sbd.setShftbrkStartTime(DateHelper.addMinutes(start, 10 * 60 + 30));
		sbd.setShftbrkEndTime(DateHelper.addMinutes(start, 11 * 60));
		sbd.setShftId(shiftId2);
		final TimeCodeData tcd = cm.getTimeCodeByName("UAT");
		final HourTypeData htd = cm.getHourTypeByName("UNPAID");
		sbd.setTcodeId(tcd.getTcodeId());
		sbd.setHtypeId(htd.getHtypeId());

		ShiftBreakAccess sba = new ShiftBreakAccess(dbConnection);
		sba.insert(sbd);

		InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(
				dbConnection);
		insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);

		insSkd.setEmpskdActShiftId(shiftId2);

		Datetime skdStart = DateHelper.addMinutes(start, 6 * 60);
		Datetime skdEnd = DateHelper.addMinutes(start, 14 * 60 + 30);

		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);

		ovrBuilder.add(insSkd);

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(
				dbConnection);

		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(start);
		ins.setEndDate(start);

		Datetime clksArr[] = new Datetime[4];
		clksArr[0] = DateHelper.addMinutes(start, 6 * 60);// on
		clksArr[1] = DateHelper.addMinutes(start, 11 * 60);// off
		clksArr[2] = DateHelper.addMinutes(start, 12 * 60);// on
		clksArr[3] = DateHelper.addMinutes(start, 14 * 60 + 30);// off

		String clks = createWorkSummaryClockStringForOnOffs(Arrays
				.asList(clksArr));
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

	}

	public void testLunchBreakRule6() throws Exception {
		logger
				.debug("******STARTING UNIT TEST NO 6  ...PARAM_BREAK_MINUTES_UP_TO starts with 'S'. TAKING BREAK MINUTES FROM SHIFT *******");

		OverrideBuilder ovrBuilder = new OverrideBuilder(dbConnection);
		ovrBuilder.setCreatesDefaultRecords(true);

		final int empId = 3;// * was 10
		final String brkCode = "BRK", aeCode = "PGR", alCode = "UGR";
		int brkMinutes = 0;// was 30
         int shiftId2=getShiftID(2);
		// *** create the rule
		final CodeMapper cm = CodeMapper.createBrandNewCodeMapper(dbConnection);

		Rule rule = new ArrangeBreakMultipleRule();
		Parameters ruleparams = new Parameters();
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS,
				"true");
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "99");
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES,
				String.valueOf(brkMinutes));
		ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE,
				brkCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, aeCode);
		ruleparams.addParameter(
				ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVELATE_TIMECODE, alCode);

		// ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ASSIGN_MINUTES,
		// "60");
		RegistryHelper regHelper = new RegistryHelper();
		regHelper.setVar("system/WORKBRAIN_PARAMETERS/ALLOW_PARTIAL_BREAKS",
				"TRUE");

		regHelper.setVar("system/WORKBRAIN_PARAMETERS/NO_SWIPE_FOR_BREAKS",
				"FALSE");
		clearAndAddRule(empId, start, rule, ruleparams);

		ShiftBreakData sbd = new ShiftBreakData();
		sbd.setShftbrkId(0);
		sbd.setShftbrkMinutes(30);
		sbd.setShftbrkStartTime(DateHelper.addMinutes(start, 10 * 60 + 30));
		sbd.setShftbrkEndTime(DateHelper.addMinutes(start, 11 * 60));
		sbd.setShftId(shiftId2);
		final TimeCodeData tcd = cm.getTimeCodeByName("UAT");
		final HourTypeData htd = cm.getHourTypeByName("UNPAID");
		sbd.setTcodeId(tcd.getTcodeId());
		sbd.setHtypeId(htd.getHtypeId());

		ShiftBreakAccess sba = new ShiftBreakAccess(dbConnection);
		sba.insert(sbd);

		InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(
				dbConnection);
		insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
		insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		insSkd.setEmpId(empId);
		insSkd.setStartDate(start);
		insSkd.setEndDate(start);

		insSkd.setEmpskdActShiftId(shiftId2);

		Datetime skdStart = DateHelper.addMinutes(start, 6 * 60);
		Datetime skdEnd = DateHelper.addMinutes(start, 14 * 60 + 30);

		insSkd.setEmpskdActStartTime(skdStart);
		insSkd.setEmpskdActEndTime(skdEnd);

		ovrBuilder.add(insSkd);

		InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(
				dbConnection);

		ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ins.setEmpId(empId);
		ins.setStartDate(start);
		ins.setEndDate(start);

		Datetime clksArr[] = new Datetime[6];
		clksArr[0] = DateHelper.addMinutes(start, 6 * 60);// on
		clksArr[1] = DateHelper.addMinutes(start, 9 * 60);// off
		clksArr[2] = DateHelper.addMinutes(start, 9 * 60 + 30);// on
		clksArr[3] = DateHelper.addMinutes(start, 10 * 60 + 30);// off
		clksArr[4] = DateHelper.addMinutes(start, 11 * 60);// on
		clksArr[5] = DateHelper.addMinutes(start, 14 * 60 + 30);// off

		String clks = createWorkSummaryClockStringForOnOffs(Arrays
				.asList(clksArr));
		ins.setWrksClocks(clks);

		ovrBuilder.add(ins);
		ovrBuilder.execute(true, false);

	}
    int getShiftID(int shiftTestNo)throws Exception{

        int shiftId=0;
        if (shiftTestNo==1){
            ShiftAccess sha1=new ShiftAccess(dbConnection);
            ShiftData shiftData1=new ShiftData();

            shiftData1.setShftStartTime(DateHelper.addMinutes(start, 8*60));
            shiftData1.setShftEndTime(DateHelper.addMinutes(start, 17*60));
            //shiftData1.setShftId(shiftId);
            shiftData1.setShftName("Regular Test"); //shiftData.setShftgrpId(46);
            shiftData1.setGeneratesPrimaryKeyValue(true);
            shiftData1.setShftDesc("For Test case 1,2,3,4");
            shiftData1.setColrId(0);
            shiftData1.setShftgrpId(0);


            sha1.insert(shiftData1);
            shiftId=shiftData1.getShftId();
            if (logger.isDebugEnabled()) logger.debug("shiftId in test case [shiftTestNo]="+shiftId);

            return shiftId;
        }
        else  {
            ShiftAccess sha1=new ShiftAccess(dbConnection);
            ShiftData shiftData1=new ShiftData();

            shiftData1.setShftStartTime(DateHelper.addMinutes(start, 6*60));
            shiftData1.setShftEndTime(DateHelper.addMinutes(start, 14*60+30));
            //shiftData1.setShftId(shiftId);
            shiftData1.setShftName("Regular Test"); //shiftData.setShftgrpId(46);
            shiftData1.setGeneratesPrimaryKeyValue(true);
            shiftData1.setShftDesc("For Test case 5,6");
            shiftData1.setColrId(0);
            shiftData1.setShftgrpId(0);


            sha1.insert(shiftData1);
            shiftId=shiftData1.getShftId();
            if (logger.isDebugEnabled()) logger.debug("shiftId in test case[shiftTestNo]="+shiftId);

            return shiftId;

        }


        }

public void testMultipleShiftBrks() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String brkCode = "BRK", aeCode = "PGR", alCode="UGR"; int brkMinutes = 30;

        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule

        Rule rule = new ArrangeBreakMultipleRule();
        Parameters ruleparams = new Parameters();

        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "2");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES, String.valueOf(brkMinutes));
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE, brkCode);
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, aeCode);
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVELATE_TIMECODE, alCode);
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(getConnection());
        insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insSkd.setEmpId(empId);
        insSkd.setStartDate(start);      insSkd.setEndDate(start);

        Datetime s1Start = DateHelper.addMinutes(start, 10*60);
        Datetime s1End = DateHelper.addMinutes(start, 18*60);

        Datetime s2Start = DateHelper.addMinutes(start, 20*60);
        Datetime s2End = DateHelper.addMinutes(start, 22*60);

        insSkd.setEmpskdActStartTime(s1Start);      insSkd.setEmpskdActEndTime(s1End);
        insSkd.setEmpskdActStartTime2(s2Start);      insSkd.setEmpskdActEndTime2(s2End);

        ovrBuilder.add(insSkd);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);

        Datetime clksArr[] = new Datetime[10];
        clksArr[0] = DateHelper.addMinutes(start, 10*60);
        clksArr[1] = DateHelper.addMinutes(start, 12*60);
        clksArr[2] = DateHelper.addMinutes(start, 12*60+40);
        clksArr[3] = DateHelper.addMinutes(start, 16*60+40);
        clksArr[4] = DateHelper.addMinutes(start, 17*60);
        clksArr[5] = DateHelper.addMinutes(start, 19*60+40);
        clksArr[6] = DateHelper.addMinutes(start, 20*60+0);
        clksArr[7] = DateHelper.addMinutes(start, 21*60+10);
        clksArr[8] = DateHelper.addMinutes(start, 21*60+40);
        clksArr[9] = DateHelper.addMinutes(start, 22*60+10);
        String clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));

        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);
        //assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() > 0);
        // *** BRK1

        int brkIndex = wds.getFirstRecordIndex(clksArr[1] ,
                DateHelper.addMinutes(clksArr[1] , brkMinutes) , false);

        assertTrue(brkIndex != -1);
        assertEquals("Must be break code",
                brkCode, wds.getWorkDetail(brkIndex).getWrkdTcodeName());

        // *** BRK2

        brkIndex = wds.getFirstRecordIndex(clksArr[3] ,
                clksArr[4], false);

        assertTrue(brkIndex != -1);
        assertEquals("Must be break code",
                brkCode, wds.getWorkDetail(brkIndex).getWrkdTcodeName());

        // *** BRK2

        brkIndex = wds.getFirstRecordIndex(clksArr[7],
                clksArr[8], false);
        assertTrue(brkIndex != -1);
        assertEquals("Must be break code",
                brkCode, wds.getWorkDetail(brkIndex).getWrkdTcodeName());

    }



    /**

     * @throws Exception

     */

    public void testMultipleShiftOneBrk() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;

        final String brkCode = "BRK", aeCode = "PGR", alCode="UGR"; int brkMinutes = 30;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule

        Rule rule = new ArrangeBreakMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "1");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES, String.valueOf(brkMinutes));
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE, brkCode);
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVEEARLY_TIMECODE, aeCode);
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_ARRIVELATE_TIMECODE, alCode);
        clearAndAddRule(empId , start , rule , ruleparams);


        InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(getConnection());
        insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);

        insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insSkd.setEmpId(empId);
        insSkd.setStartDate(start);      insSkd.setEndDate(start);

        Datetime s1Start = DateHelper.addMinutes(start, 10*60);
        Datetime s1End = DateHelper.addMinutes(start, 18*60);
        Datetime s2Start = DateHelper.addMinutes(start, 20*60);
        Datetime s2End = DateHelper.addMinutes(start, 22*60);

        insSkd.setEmpskdActStartTime(s1Start);      insSkd.setEmpskdActEndTime(s1End);
        insSkd.setEmpskdActStartTime2(s2Start);      insSkd.setEmpskdActEndTime2(s2End);

        ovrBuilder.add(insSkd);


        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clksArr[] = new Datetime[8];
        clksArr[0] = DateHelper.addMinutes(start, 10*60);
        clksArr[1] = DateHelper.addMinutes(start, 12*60);
        clksArr[2] = DateHelper.addMinutes(start, 12*60+40);
        clksArr[3] = DateHelper.addMinutes(start, 16*60+40);
        clksArr[4] = DateHelper.addMinutes(start, 17*60);
        clksArr[5] = DateHelper.addMinutes(start, 19*60+40);
        clksArr[6] = DateHelper.addMinutes(start, 20*60+0);
        clksArr[7] = DateHelper.addMinutes(start, 22*60+10);

        String clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));

        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        //assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);

        //System.out.println(wds.toDescription());

        assertTrue(wds.size() > 0);

        // *** BRK1

        int brkIndex = wds.getFirstRecordIndex(clksArr[1] ,

                DateHelper.addMinutes(clksArr[1] , brkMinutes) , false);

        assertTrue(brkIndex != -1);
        assertEquals("Must be break code",
                brkCode, wds.getWorkDetail(brkIndex).getWrkdTcodeName());

        // *** BRK2

        brkIndex = wds.getFirstRecordIndex(clksArr[3] ,
                clksArr[4], false);

        assertTrue(brkIndex != -1);
        assertEquals("Must be UAT code",
                "UAT", wds.getWorkDetail(brkIndex).getWrkdTcodeName());

    }



    /**

     * @throws Exception

     */

    public void testMultipleShiftUpto() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String brkCode = "BRK"; int brkMinutes = 60;

        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule

        Rule rule = new ArrangeBreakMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "2");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES, ArrangeBreakMultipleRule.PARAM_VAL_BREAK_MINUTES_UP_TO + brkMinutes);
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE, brkCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(getConnection());
        insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insSkd.setEmpId(empId);

        insSkd.setStartDate(start);      insSkd.setEndDate(start);
        Datetime s1Start = DateHelper.addMinutes(start, 10*60);
        Datetime s1End = DateHelper.addMinutes(start, 18*60);
        Datetime s2Start = DateHelper.addMinutes(start, 20*60);
        Datetime s2End = DateHelper.addMinutes(start, 22*60);
        insSkd.setEmpskdActStartTime(s1Start);      insSkd.setEmpskdActEndTime(s1End);

        insSkd.setEmpskdActStartTime2(s2Start);      insSkd.setEmpskdActEndTime2(s2End);
        ovrBuilder.add(insSkd);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clksArr[] = new Datetime[10];

        clksArr[0] = DateHelper.addMinutes(start, 10*60);
        clksArr[1] = DateHelper.addMinutes(start, 12*60); // start BRK
        clksArr[2] = DateHelper.addMinutes(start, 12*60+40); // end BRK
        clksArr[3] = DateHelper.addMinutes(start, 15*60+40); // start BRK
        clksArr[4] = DateHelper.addMinutes(start, 17*60); // end BRK
        clksArr[5] = DateHelper.addMinutes(start, 19*60+40);
        clksArr[6] = DateHelper.addMinutes(start, 20*60+0);
        clksArr[7] = DateHelper.addMinutes(start, 22*60+10);
        clksArr[8] = DateHelper.addMinutes(start, 21*60+40);
        clksArr[9] = DateHelper.addMinutes(start, 22*60+10);

        String clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));

        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);

        assertTrue(wds.size() > 0);

        // *** BRK1
        int brkIndex = wds.getFirstRecordIndex(clksArr[1], clksArr[2], false);

        assertTrue(brkIndex != -1);
        assertEquals("Must be break code", brkCode, wds.getWorkDetail(brkIndex).getWrkdTcodeName());

        // *** BRK2
        brkIndex = wds.getFirstRecordIndex(clksArr[3], clksArr[4], false);
        assertTrue(brkIndex != -1);
        assertEquals("Must be UAT code", "UAT", wds.getWorkDetail(brkIndex).getWrkdTcodeName());

    }

    public void testShiftUptoMaxFixed() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String brkCode = "BRK";

        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule

        Rule rule = new ArrangeBreakMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "2");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES, "0");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE, brkCode);
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TOTAL_MINUTES_UP_TO, "60");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(getConnection());
        insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insSkd.setEmpId(empId);
        insSkd.setStartDate(start);
        insSkd.setEndDate(start);
        Datetime s1Start = DateHelper.addMinutes(start, 10*60);
        Datetime s1End = DateHelper.addMinutes(start, 18*60);
        insSkd.setEmpskdActStartTime(s1Start);
        insSkd.setEmpskdActEndTime(s1End);
        ovrBuilder.add(insSkd);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);

        Datetime clksArr[] = new Datetime[6];
        clksArr[0] = DateHelper.addMinutes(start, 10*60);
        clksArr[1] = DateHelper.addMinutes(start, 12*60); // start BRK
        clksArr[2] = DateHelper.addMinutes(start, 12*60+45); // end BRK
        clksArr[3] = DateHelper.addMinutes(start, 14*60); // start BRK
        clksArr[4] = DateHelper.addMinutes(start, 14*60+45); // end BRK
        clksArr[5] = DateHelper.addMinutes(start, 18*60);
        String clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);

        assertTrue(wds.size() > 0);

        // *** BRK1
        int brkIndex = wds.getFirstRecordIndex(clksArr[1], clksArr[2], false);

        assertTrue(brkIndex != -1);
        assertEquals("Must be break code", brkCode, wds.getWorkDetail(brkIndex).getWrkdTcodeName());

        // *** BRK2
        // First 15 mins are BRK, then we hit the max so last 30 mins is UAT.
        brkIndex = wds.getFirstRecordIndex(DateHelper.addMinutes(start, 14*60+15),
        									clksArr[4], false);
        assertTrue(brkIndex != -1);
        assertEquals("Must be UAT code", "UAT", wds.getWorkDetail(brkIndex).getWrkdTcodeName());
    }


    public void testShiftUptoMaxFromSchedule() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String brkCode = "BRK";

        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule

        Rule rule = new ArrangeBreakMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "2");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES, "0");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE, brkCode);
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TOTAL_MINUTES_UP_TO, "S");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride insSkd = new InsertEmployeeScheduleOverride(getConnection());
        insSkd.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        insSkd.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insSkd.setEmpId(empId);
        insSkd.setStartDate(start);
        insSkd.setEndDate(start);
        Datetime s1Start = DateHelper.addMinutes(start, 10*60);
        Datetime s1End = DateHelper.addMinutes(start, 18*60);
        insSkd.setEmpskdActStartTime(s1Start);
        insSkd.setEmpskdActEndTime(s1End);
        ovrBuilder.add(insSkd);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);

        Datetime clksArr[] = new Datetime[4];
        clksArr[0] = DateHelper.addMinutes(start, 10*60);
        clksArr[1] = DateHelper.addMinutes(start, 12*60); // start BRK
        clksArr[2] = DateHelper.addMinutes(start, 12*60+45); // end BRK
        clksArr[3] = DateHelper.addMinutes(start, 18*60);
        String clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);

        assertTrue(wds.size() > 0);

        // UpTo minutes is set to "S" but shift breaks are not defined.
        // The rule should do nothing so the break should remain UAT.

        // *** BRK1
        int brkIndex = wds.getFirstRecordIndex(clksArr[1], clksArr[2], false);

        assertTrue(brkIndex != -1);
        assertEquals("Must be UAT", "UAT", wds.getWorkDetail(brkIndex).getWrkdTcodeName());
    }

    public void testShiftUptoMaxFromSchedule2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String brkCode = "BRK";
        int shiftId = 0;

        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // Create the rule.
        Rule rule = new ArrangeBreakMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_MAX_NUMBER_OF_BREAKS, "2");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_MINUTES, "0");
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TIMECODE, brkCode);
        ruleparams.addParameter(ArrangeBreakMultipleRule.PARAM_BREAK_TOTAL_MINUTES_UP_TO, "S");
        clearAndAddRule(empId , start , rule , ruleparams);


        // Create the shift.
        ShiftData shiftData = new ShiftData();
        shiftData.setShftName("JUnit Shift");
        shiftData.setShftDesc("JUnit Shift");
        shiftData.setShftStartTime(DateHelper.addMinutes(start, 9*60));
        shiftData.setShftEndTime(DateHelper.addMinutes(start, 17*60));
        shiftData.setShftgrpId(0);

        ShiftAccess shftAccess = new ShiftAccess(getConnection());
        shftAccess.insert(shiftData);
        shiftId = shiftData.getShftId();

        // Assign a shift break.
        ShiftBreakData shftBrk = new ShiftBreakData();
        shftBrk.setShftId(shiftId);
        shftBrk.setShftbrkStartTime(DateHelper.addMinutes(start, 12*60));
        shftBrk.setShftbrkEndTime(DateHelper.addMinutes(start, 13*60));
        shftBrk.setShftbrkMinutes(60);
        shftBrk.setTcodeId(0);
        shftBrk.setHtypeId(0);

        ShiftBreakAccess shftBrkAccess = new ShiftBreakAccess(getConnection());
        shftBrkAccess.insert(shftBrk);


        // Assign the shift to the employee.
        InsertEmployeeScheduleOverride insEmp = new InsertEmployeeScheduleOverride(getConnection());
        insEmp.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        insEmp.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmp.setEmpId(empId);
        insEmp.setStartDate(start);
        insEmp.setEndDate(start);
        insEmp.setEmpskdActShiftId(shiftId);
        ovrBuilder.add(insEmp);

        // Create Clocks.
        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);

        Datetime clksArr[] = new Datetime[6];
        clksArr[0] = DateHelper.addMinutes(start, 10*60);
        clksArr[1] = DateHelper.addMinutes(start, 12*60); // start BRK
        clksArr[2] = DateHelper.addMinutes(start, 12*60+45); // end BRK
        clksArr[3] = DateHelper.addMinutes(start, 14*60); // start BRK
        clksArr[4] = DateHelper.addMinutes(start, 14*60+45); // end BRK
        clksArr[5] = DateHelper.addMinutes(start, 18*60);
        String clks = createWorkSummaryClockStringForOnOffs(Arrays.asList(clksArr));
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        // Apply the overrides.
        ovrBuilder.execute(true , false);

        // Assert the overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 2);

        // Get the work details.
        WorkDetailList wds = getWorkDetailsForDate(empId , start);

        // Assert that work details exist.
        assertTrue(wds.size() > 0);

        // *** BRK1
        int brkIndex = wds.getFirstRecordIndex(clksArr[1], clksArr[2], false);

        assertTrue(brkIndex != -1);
        assertEquals("Must be break code", brkCode, wds.getWorkDetail(brkIndex).getWrkdTcodeName());

        // *** BRK2
        // First 15 mins are BRK, then we hit the max so last 30 mins is UAT.
        brkIndex = wds.getFirstRecordIndex(DateHelper.addMinutes(start, 14*60+15),
        									clksArr[4], false);
        assertTrue(brkIndex != -1);
        assertEquals("Must be UAT code", "UAT", wds.getWorkDetail(brkIndex).getWrkdTcodeName());
    }

    public static void main(String[] args) throws Exception {
		junit.textui.TestRunner.run(suite());
	}

}




/*
 * ShiftAccess sha=new ShiftAccess(dbConnection);
 *
 * ShiftData shiftData=new ShiftData();
 * shiftData.setShftStartTime(DateHelper.addMinutes(start, 11*60));
 * shiftData.setShftEndTime(DateHelper.addMinutes(start, 17*60));
 * //shiftData.setShftId(shiftData.getShftId()); shiftData.setShftName("New
 * Shift"); //shiftData.setShftgrpId(46);
 * shiftData.setGeneratesPrimaryKeyValue(true);
 * shiftData.setShftDesc("something"); sha.insert(shiftData);
 */
