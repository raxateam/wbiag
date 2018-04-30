package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;
/**
 * Test for LateLELLRuleExt.
 */
public class LateLELLRuleExtTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LateLELLRuleExtTest.class);

    public LateLELLRuleExtTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(LateLELLRuleExtTest.class);
        return result;
    }


    /**
     * <ul>
     * <li>Creates clock override for a 9-17 shiftPattern=ALL DAYS employee for clocks <br>
     *     9:05 ON 16:57 OFF
     * <li>Expected details are <br>
     *     09:00 09:05 LATE <br>
     *     09:05 16:57 WRK <br>
     *     16:57 17:00 LE <br>
     * <ul>
     * @throws Exception
     */
    public void testIEParsingOriginal() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String lateCode = "LATE", leCode = "LE";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_LATE_CODE, lateCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OUT_EARLY_CODE, leCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9 + 5);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17 - 3);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 3);
        WorkDetailData wd0 = wds.getWorkDetail(0);
        assertEquals(lateCode , wd0.getWrkdTcodeName());
        assertEquals(5 , wd0.getWrkdMinutes());
        WorkDetailData wd2 = wds.getWorkDetail(2);
        assertEquals(leCode , wd2.getWrkdTcodeName());
        assertEquals(3 , wd2.getWrkdMinutes());

    }


    /**
     * @throws Exception
     */
    public void testIEParsing1() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String leCode = "LE";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_CODE, leCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_STOP_TCODES, "TRN");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9 -120);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 -105);
        clk1Off = DateHelper.addMinutes(start, 60*9 - 60);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);


        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        //assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        //assertTrue(wds.size() == 4);
        WorkDetailData wd0 = wds.getWorkDetail(2);
        assertEquals(leCode , wd0.getWrkdTcodeName());
    }

    /**
     * @throws Exception
     */
    public void testIEParsing2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String leCode = "LE";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_CODE, leCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_NUM_MINS_LIMIT, "30");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_INCLUDE_NUM_MINS_LIMIT_RECORD, "false");


        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9 -60);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 -20);
        clk1Off = DateHelper.addMinutes(start, 60*9);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);


        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 3);
        WorkDetailData wd0 = wds.getWorkDetail(0);
        assertEquals("WRK" , wd0.getWrkdTcodeName());

    }

    /**
     * @throws Exception
     */
    public void testIEParsing3() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String leCode = "LE";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_CODE, leCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_STOP_CLOCKIN, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9 -60);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 -20);
        clk1Off = DateHelper.addMinutes(start, 60*9);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);


        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 3);
        WorkDetailData wd0 = wds.getWorkDetail(0);
        assertEquals(leCode , wd0.getWrkdTcodeName());

    }

    /**
     * @throws Exception
     */
    public void testIEParsing4() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String leCode = "LE";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_CODE, leCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_NUM_MINS_LIMIT, "30");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_INCLUDE_NUM_MINS_LIMIT_RECORD, "false");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_STOP_TCODES, "TRN");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_STOP_CLOCKIN, "true");


        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9 -120);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 -105);
        clk1Off = DateHelper.addMinutes(start, 60*9 - 60);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 - 20);
        clk1Off = DateHelper.addMinutes(start, 60*9);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("BRK");
        ovrBuilder.add(ins2);


        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 5);
        WorkDetailData wd0 = wds.getWorkDetail(2);
        assertEquals("WRK" , wd0.getWrkdTcodeName());

    }

    /**
     * @throws Exception
     */
    public void testIEParsing5() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String leCode = "LE";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_CODE, leCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_PARSING, "true");
        // ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_NUM_MINS_LIMIT, "30");
        //ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_INCLUDE_NUM_MINS_LIMIT_RECORD, "false");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_STOP_TCODES, "GAP");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_STOP_CLOCKIN, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride insS = new InsertEmployeeScheduleOverride(getConnection());
        insS.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE) ;
        insS.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insS.setEmpId(empId);
        insS.setStartDate(start);      insS.setEndDate(start);
        Datetime skd2on = DateHelper.addMinutes(start, 60 *19);
        Datetime skd2off = DateHelper.addMinutes(start, 60*21);
        insS.setEmpskdActStartTime2(skd2on);
        insS.setEmpskdActEndTime2(skd2off);
        ovrBuilder.add(insS);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9 -120);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17);
        Datetime clk2On = DateHelper.addMinutes(start, 60 *19 - 40);
        Datetime clk2Off = DateHelper.addMinutes(start, 60*21);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off, clk2On, clk2Off );
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);


        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 -40);
        clk1Off = DateHelper.addMinutes(start, 60*9);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 6);
        WorkDetailData wd0 = wds.getWorkDetail(0);
        assertEquals(leCode , wd0.getWrkdTcodeName());
        WorkDetailData wd5 = wds.getWorkDetail(5);
        assertEquals(leCode , wd0.getWrkdTcodeName());
    }

    /**
     * @throws Exception
     */
    public void testIEParsingStopCodesBug() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String leCode = "LE";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_CODE, leCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_STOP_TCODES, "TRN");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9 -180);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 -120);
        clk1Off = DateHelper.addMinutes(start, 60*9);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);


        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 3);
        WorkDetailData wd0 = wds.getWorkDetail(1);
        assertEquals("TRN" , wd0.getWrkdTcodeName());
    }

    /**
     * @throws Exception
     */
    public void testIEParsing6() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String leCode = "LE";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_CODE, leCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_NUM_MINS_LIMIT, "99999");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_INCLUDE_NUM_MINS_LIMIT_RECORD, "false");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_STOP_TCODES, "TRN");


        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9 -120);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 -105);
        clk1Off = DateHelper.addMinutes(start, 60*9 - 60);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 - 60);
        clk1Off = DateHelper.addMinutes(start, 60*9);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("BRK");
        ovrBuilder.add(ins2);


        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        //assertTrue(wds.size() == 5);
        WorkDetailData wd0 = wds.getWorkDetail(2);
        assertEquals("BRK Shouldn't change", "BRK" , wd0.getWrkdTcodeName());

    }

    /**
     * @throws Exception
     */
    public void testIEParsing7() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String leCode = "LE";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_CODE, leCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IN_EARLY_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_NUM_MINS_LIMIT, "99999");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_INCLUDE_NUM_MINS_LIMIT_RECORD, "false");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_IE_STOP_TCODES, "TRN");


        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9 -120);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *9 - 60);
        clk1Off = DateHelper.addMinutes(start, 60*9 - 30);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("BRK");
        ovrBuilder.add(ins2);


        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        //assertTrue(wds.size() == 5);
        WorkDetailData wd0 = wds.getWorkDetail(0);
        assertEquals("First record shouldn't change, no stop codes found",
                     "WRK" , wd0.getWrkdTcodeName());

    }

    /**
     * @throws Exception
     */
    public void testOLParsing1() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String llCode = "LL";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OUT_LATE_CODE, llCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OUT_LATE_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OL_STOP_TCODES, "TRN");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17 + 120);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *17 + 60);
        clk1Off = DateHelper.addMinutes(start, 60*17 + 105);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride(getConnection());
        ins3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins3.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins3.setEmpId(empId);
        ins3.setStartDate(start);      ins3.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *17);
        clk1Off = DateHelper.addMinutes(start, 60*17 + 40);
        ins3.setStartTime(clk1On) ; ins3.setEndTime(clk1Off) ;
        ins3.setWrkdTcodeName("BRK");
        ovrBuilder.add(ins3);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        //assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        //assertTrue(wds.size() == 4);
        WorkDetailData wd0 = wds.getWorkDetail(2);
        assertEquals(llCode , wd0.getWrkdTcodeName());
    }

    public void testOLParsing2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String llCode = "LL";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OUT_LATE_CODE, llCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OUT_LATE_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OL_NUM_MINS_LIMIT, "30");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OL_INCLUDE_NUM_MINS_LIMIT_RECORD, "false");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17 + 40);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *17 );
        clk1Off = DateHelper.addMinutes(start, 60*17 + 20);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        //assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        //assertTrue(wds.size() == 4);
        WorkDetailData wd0 = wds.getWorkDetail(2);
        assertEquals("WRK" , wd0.getWrkdTcodeName());
    }

    public void testOLParsing3() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String llCode = "LL";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OUT_LATE_CODE, llCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OUT_LATE_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OL_STOP_CLOCKOUT, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*18);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *17 );
        clk1Off = DateHelper.addMinutes(start, 60*17 + 20);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        //assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        //assertTrue(wds.size() == 4);
        WorkDetailData wd0 = wds.getWorkDetail(2);
        assertEquals(llCode , wd0.getWrkdTcodeName());
    }

    /**
     * @throws Exception
     */
    public void testOLParsing4() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String llCode = "LL";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateLELLRuleExt();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OUT_LATE_CODE, llCode);
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OUT_LATE_PARSING, "true");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OL_STOP_TCODES, "TRN");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OL_NUM_MINS_LIMIT, "30");
        ruleparams.addParameter(LateLELLRuleExt.PARAM_OL_INCLUDE_NUM_MINS_LIMIT_RECORD, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60 *9);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*17 + 120);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *17 + 60);
        clk1Off = DateHelper.addMinutes(start, 60*17 + 105);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride(getConnection());
        ins3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins3.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins3.setEmpId(empId);
        ins3.setStartDate(start);      ins3.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 60 *17);
        clk1Off = DateHelper.addMinutes(start, 60*17 + 20);
        ins3.setStartTime(clk1On) ; ins3.setEndTime(clk1Off) ;
        ins3.setWrkdTcodeName("BRK");
        ovrBuilder.add(ins3);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        //assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        //assertTrue(wds.size() == 4);
        WorkDetailData wd0 = wds.getWorkDetail(2);
        assertEquals(llCode , wd0.getWrkdTcodeName());
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}