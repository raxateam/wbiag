package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;


/**
 * Test for GracesExtendedRule.
 */
public class GracesExtendedRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GracesExtendedRuleTest.class);

    public GracesExtendedRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(GracesExtendedRuleTest.class);
        return result;
    }

    /**
     * <ul>
     * <li>Create rule for PPARAM_GRACE_AFTER_SHIFT_STARTS = 6
     * <li>Creates clock override for a 9-17 shiftPattern=ALL DAYS employee for clocks <br>
     *     9:05 ON 17:00 OFF
     * <li>Expected details are <br>
     *     09:00 9:05 PGR <br>
     *     9:05 17:00 WRK <br>
     * <ul>
     * @throws Exception
     */
    public void testGraceLateStart() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 4;
        final String stCode = "PGR", endCode = "UGR";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        
        // *** create the rule
        Rule rule = new GracesExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GracesExtendedRule.PARAM_GRACE_AFTER_SHIFT_STARTS, String.valueOf(6));
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS, stCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS_2, stCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_STARTS, stCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_ENDS, endCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_ENDS, endCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9*60 + 5);
        Datetime clk1Off = DateHelper.addMinutes(start, 17*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        assertTrue(wds.size() == 2);
        WorkDetailData wd0 = wds.getWorkDetail(0);
        assertEquals(stCode , wd0.getWrkdTcodeName());
        assertEquals(5 , wd0.getWrkdMinutes());
        WorkDetailData wd2 = wds.getWorkDetail(1);
        assertEquals("WRK" , wd2.getWrkdTcodeName());
        assertEquals(475 , wd2.getWrkdMinutes());

    }
    
    /**
     * <ul>
     * <li>Create rule for PARAM_GRACE_BEFORE_SHIFT_ENDS = 6
     * <li>Creates clock override for a 9-17 shiftPattern=ALL DAYS employee for clocks <br>
     *     9:00 ON 16:57 OFF
     * <li>Expected details are <br>
     *     09:00 16:57 WRK <br>
     *     16:57 17:00 UGR <br>
     * <ul>
     * @throws Exception
     */
    public void testGraceEarlyEnd() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 4;
        final String stCode = "PGR", endCode = "UGR";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        
        // *** create the rule
        Rule rule = new GracesExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GracesExtendedRule.PARAM_GRACE_BEFORE_SHIFT_ENDS, String.valueOf(6));
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS, stCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS_2, stCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_STARTS, stCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_ENDS, endCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_ENDS, endCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 17*60 - 3);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        assertTrue(wds.size() == 2);
        WorkDetailData wd0 = wds.getWorkDetail(0);
        assertEquals("WRK" , wd0.getWrkdTcodeName());
        assertEquals(477 , wd0.getWrkdMinutes());
        WorkDetailData wd2 = wds.getWorkDetail(1);
        assertEquals(endCode , wd2.getWrkdTcodeName());
        assertEquals(3 , wd2.getWrkdMinutes());

    }
    /**
     * <ul>
     * <li>Create rule for PARAM_GRACE_BEFORE_SHIFT_STARTS = 6, PARAM_GRACE_AFTER_SHIFT_ENDS = 6
     * <li>Creates clock override for a 9-17 shiftPattern=ALL DAYS employee for clocks <br>
     *     8:55 ON 17:03 OFF
     * <li>Expected details are <br>
     *     8:55  08:00 PGR <br>
     *     08:00 17:00 BRK <br>
     *     17:00 17:03 UGR <br>
     * <ul>
     * @throws Exception
     */
    public void testGraceStartEnd() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 4;
        final String stCode = "PGR", endCode = "UGR";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        
        // *** create the rule
        Rule rule = new GracesExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GracesExtendedRule.PARAM_GRACE_BEFORE_SHIFT_STARTS, String.valueOf(6));
        ruleparams.addParameter(GracesExtendedRule.PARAM_GRACE_AFTER_SHIFT_ENDS, String.valueOf(6));
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS, stCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS_2, stCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_STARTS, stCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_ENDS, endCode);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_ENDS, endCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 8*60 + 55);
        Datetime clk1Off = DateHelper.addMinutes(start, 17*60 + 3);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        assertTrue(wds.size() == 3);
        WorkDetailData wd0 = wds.getWorkDetail(0);
        assertEquals(stCode , wd0.getWrkdTcodeName());
        assertEquals(5 , wd0.getWrkdMinutes());
        WorkDetailData wd2 = wds.getWorkDetail(2);
        assertEquals(endCode , wd2.getWrkdTcodeName());
        assertEquals(3 , wd2.getWrkdMinutes());

    }

    
    public void testGraceEarlyStart2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 4;
        final String beforeShiftCode1 = "PGR", beforeShiftCode2 = "UGR";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        
        // *** create the rule
        Rule rule = new GracesExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GracesExtendedRule.PARAM_GRACE_BEFORE_SHIFT_STARTS, "15");
        ruleparams.addParameter(GracesExtendedRule.PARAM_GRACE_BEFORE_SHIFT_STARTS_2, "30");
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS, beforeShiftCode1);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS_2, beforeShiftCode2);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_STARTS, beforeShiftCode2);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_ENDS, beforeShiftCode2);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_ENDS, beforeShiftCode2);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 8*60 + 35);
        Datetime clk1Off = DateHelper.addMinutes(start, 17*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        
        assertTrue(wds.size() == 3);
        
        WorkDetailData wd = null;

        // UGR
        wd = wds.getWorkDetail(0);
        assertEquals(beforeShiftCode2 , wd.getWrkdTcodeName());
        assertEquals(10 , wd.getWrkdMinutes());
        
        // PGR
        wd = wds.getWorkDetail(1);
        assertEquals(beforeShiftCode1 , wd.getWrkdTcodeName());
        assertEquals(15 , wd.getWrkdMinutes());
        
        // WRK
        wd = wds.getWorkDetail(2);
        assertEquals("WRK" , wd.getWrkdTcodeName());
        assertEquals(480 , wd.getWrkdMinutes());
    }
    
    /*Testcase for Incident# 1970026  Dec 18, 2008 - Neshan Kumar*/
    /**
     * <ul>
     * <li>Create rule for PPARAM_GRACE_BEFORE_SHIFT_STARTS = 6, PARAM_TIMECODE_BEFORE_SHIFT_STARTS = PGR
     * <li>Creates following clock override for an employee who is working in two shifts 04:00-07:00 and 14:00-17:00 on a day<br>
     *     03:58 ON  07:00 OFF
     *     13:56 ON  17:00 OFF
     * <li>Expected details are <br>
     *     03:58 - 04:00 PGR <br>
     *     04:00 - 07:00 WRK <br>
     *     07:00 - 13:56 GAP <br>
     *     13:56 - 14:00 PGR <br>
     *     14:00 - 17:00 WRK <br>
     * <ul>
     * @throws Exception
     */
    
    public void testMultiShiftGraceEarlyStart() throws Exception {
    	OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
        
        final int empId = 3;
        final String beforeShiftCode1 = "PGR", beforeShiftCode2 = "UGR";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        
        // *** create the rule
        Rule rule = new GracesExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GracesExtendedRule.PARAM_GRACE_BEFORE_SHIFT_STARTS, String.valueOf(6));
        ruleparams.addParameter(GracesExtendedRule.PARAM_GRACE_BEFORE_SHIFT_STARTS_2, String.valueOf(30));
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS, beforeShiftCode1);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_STARTS_2, beforeShiftCode2);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_STARTS, beforeShiftCode2);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_AFTER_SHIFT_ENDS, beforeShiftCode2);
        ruleparams.addParameter(GracesExtendedRule.PARAM_TIMECODE_BEFORE_SHIFT_ENDS, beforeShiftCode2);
        
        //Apply to all shifts
        ruleparams.addParameter(GracesExtendedRule.PARAM_APPLY_TO_ALL_SHIFTS, "true");
        
        clearAndAddRule(empId , start , rule , ruleparams);
        
        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        
        //Shift 1
        Datetime clk1On = DateHelper.addMinutes(start, 4*60 - 2);
        Datetime clk1Off = DateHelper.addMinutes(start, 7*60);
        
        //Shift 2
        Datetime clk2On = DateHelper.addMinutes(start, 14*60 - 4);
        Datetime clk2Off = DateHelper.addMinutes(start, 17*60);
        
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off, clk2On, clk2Off);
        
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        
        //Number of workdetail records
        assertTrue(wds.size() == 5);
        
        //PGR - 1st Shift
        WorkDetailData wd = wds.getWorkDetail(0);
        assertEquals(beforeShiftCode1 , wd.getWrkdTcodeName());
        assertEquals(2 , wd.getWrkdMinutes());
        
        //PGR - 2nd Shift
        wd = wds.getWorkDetail(3);
        assertEquals(beforeShiftCode1 , wd.getWrkdTcodeName());
        assertEquals(4 , wd.getWrkdMinutes());
    	
    }
    


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

