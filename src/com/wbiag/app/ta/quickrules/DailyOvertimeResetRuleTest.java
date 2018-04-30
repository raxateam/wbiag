package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for DailyOvertimeResetRuleTest.
 * Tests from \\Toriag\IAGSpecs\TimeAttendance\WBIAG_QuickRules_Functional.doc#TT43747
 */
public class DailyOvertimeResetRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DailyOvertimeResetRuleTest.class);

    public DailyOvertimeResetRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(DailyOvertimeResetRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testDuration0() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule
        Rule rule = new DailyOvertimeResetRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_TIME_CODES, "UAT");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_DURATION, "0");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1Start = DateHelper.addMinutes(start, 10*60);
        Datetime clk1End = DateHelper.addMinutes(start, 20*60);
        Datetime clk2Start = DateHelper.addMinutes(start, 22*60);
        Datetime clk2End = DateHelper.addMinutes(start, 23*60);

        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1Start , clk1End , clk2Start , clk2End));
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        int ind1 = wdl.getFirstRecordIndex(clk2Start, clk2End , false);
        assertFalse(ind1 == -1);
        assertEquals("REG" , wdl.getWorkDetail(ind1).getWrkdHtypeName());
        assertEquals(120 , wdl.getMinutes(null, null, null, true, "OT2", true));

    }

    /**
     * @throws Exception
     */
    public void testDurationGreaterThan0() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule
        Rule rule = new DailyOvertimeResetRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_TIME_CODES, "UAT");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_DURATION, "240");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1Start = DateHelper.addMinutes(start, 10*60);
        Datetime clk1End = DateHelper.addMinutes(start, 20*60);
        Datetime clk2Start = DateHelper.addMinutes(start, 22*60);
        Datetime clk2End = DateHelper.addMinutes(start, 23*60);

        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1Start , clk1End , clk2Start , clk2End));
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        int ind1 = wdl.getFirstRecordIndex(clk2Start, clk2End , false);
        assertFalse(ind1 == -1);
        assertEquals("OT2" , wdl.getWorkDetail(ind1).getWrkdHtypeName());
        assertEquals(180 , wdl.getMinutes(null, null, null, true, "OT2", true));

    }

    /**
     * @throws Exception
     */
    public void testDurationGreaterThan0NoOt() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule
        Rule rule = new DailyOvertimeResetRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_TIME_CODES, "UAT");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_DURATION, "240");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1Start = DateHelper.addMinutes(start, 10*60);
        Datetime clk1End = DateHelper.addMinutes(start, 20*60);
        Datetime clk2Start = DateHelper.addMinutes(start, 27*60);
        Datetime clk2End = DateHelper.addMinutes(start, 28*60);

        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1Start , clk1End , clk2Start , clk2End));
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        int ind1 = wdl.getFirstRecordIndex(clk2Start, clk2End , false);
        assertFalse(ind1 == -1);
        assertEquals("REG" , wdl.getWorkDetail(ind1).getWrkdHtypeName());
        assertEquals(120 , wdl.getMinutes(null, null, null, true, "OT2", true));

    }

    /**
     * @throws Exception
     */
    public void testDurationMulShift() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule
        Rule rule = new DailyOvertimeResetRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_TIME_CODES, "UAT,GAP");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_DURATION, "120");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime skd1Start = DateHelper.addMinutes(start, 9*60);
        Datetime skd1End = DateHelper.addMinutes(start, 17*60);
        Datetime skd2Start = DateHelper.addMinutes(start, 21*60);
        Datetime skd2End = DateHelper.addMinutes(start, 22*60);
        ins.setEmpskdActStartTime(skd1Start);
        ins.setEmpskdActEndTime(skd1End);
        ins.setEmpskdActStartTime2(skd2Start);
        ins.setEmpskdActEndTime2(skd2End);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        assertEquals(0 , wdl.getMinutes(null, null, null, true, "OT2", true));

    }

    /**
     * @throws Exception
     */
    public void testDurationTT46492() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "SUN");

        // *** create the rule
        Rule rule = new DailyOvertimeResetRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT1=240,OT2=99999");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_TIME_CODES, "UAT");
        ruleparams.addParameter(DailyOvertimeResetRule.PARAM_RESET_DURATION, "240");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1Start = DateHelper.addMinutes(start, 1*60);
        Datetime clk1End = DateHelper.addMinutes(start, 10*60);
        Datetime clk2Start = DateHelper.addMinutes(start, 14*60 + 1);
        Datetime clk2End = DateHelper.addMinutes(start, 27*60);

        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1Start , clk1End , clk2Start , clk2End));
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setWrkdUdf1("TEST");
        Datetime udfStart = DateHelper.addMinutes(start, 19*60);
        Datetime udfEnd = DateHelper.addMinutes(start, 27*60);
        ins2.setStartTime(udfStart);         ins2.setEndTime(udfEnd);
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wdl = getWorkDetailsForDate(empId , start);
        System.out.println(wdl.toDescription());
        Datetime ot1Start = DateHelper.addMinutes(start, 9*60);
        Datetime ot1End = DateHelper.addMinutes(start, 10*60);
        int ind1 = wdl.getFirstRecordIndex(ot1Start , ot1End , false);
        assertFalse(ind1 == -1);
        assertEquals("OT1" , wdl.getWorkDetail(ind1).getWrkdHtypeName());
        Datetime ot2Start = DateHelper.addMinutes(start, 22*60 + 1);
        Datetime ot2End = DateHelper.addMinutes(start, 26*60 + 1);
        int ind2 = wdl.getFirstRecordIndex(ot2Start , ot2End , false);
        assertFalse(ind2 == -1);
        assertEquals("OT1" , wdl.getWorkDetail(ind2).getWrkdHtypeName());


    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

