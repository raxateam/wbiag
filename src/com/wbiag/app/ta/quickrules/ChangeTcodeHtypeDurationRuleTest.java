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
 * Test for RemoveWorkDetailRule.
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class ChangeTcodeHtypeDurationRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ChangeTcodeHtypeDurationRuleTest.class);

    public ChangeTcodeHtypeDurationRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ChangeTcodeHtypeDurationRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testChange() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_DURATION, "480");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_HOUR_TYPE, "OT2");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 540);
        Datetime clk1Off = DateHelper.addMinutes(start, 1200);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 2);
        assertEquals(180 , wds.getMinutes(null , null, "TRN", true, null, true));
        assertEquals(180 , wds.getMinutes(null , null, null, true, "OT2", true));
    }

    /**
     * @throws Exception
     */
    public void testChange2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_DURATION, "480");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 540);
        Datetime clk1Off = DateHelper.addMinutes(start, 1200);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start, 780));
        ins2.setEndTime(DateHelper.addMinutes(start, 900));
        ins2.setWrkdTcodeName("BRK");

        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        assertEquals(60 , wds.getMinutes(null , null, "TRN", true, null, true));
    }

    /**
     * @throws Exception
     */
    public void testChange3() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        Date start = DateHelper.createDate(1900,01,01);

        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_DURATION, "480");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_PREMIUM_DETAIL, "P");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start, 0));
        ins2.setEndTime(DateHelper.addMinutes(start, 0));
        ins2.setWrkdTcodeName("WRK");
        ins2.setWrkdMinutes(660);

        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 1);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkPremiumsForDate(empId , start);
        assertEquals(180 , wds.getMinutes(null , null, "TRN", true, null, true));
    }
    /**
     * @throws Exception
     */
    public void testChange4() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");

        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK,GUAR");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_DURATION, "480");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_PREMIUM_DETAIL, "P");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setWrkdTcodeName("WRK");
        ins2.setWrkdMinutes(660);

        ovrBuilder.add(ins2);

        InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride(getConnection());
        ins3.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ins3.setWbuNameBoth("JUNIT", "JUNIT");
        ins3.setEmpId(empId);
        ins3.setStartDate(start);      ins3.setEndDate(start);
        ins3.setWrkdTcodeName("GUAR");
        ins3.setWrkdMinutes(500);

        ovrBuilder.add(ins3);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkPremiumsForDate(empId , start);
        //System.out.println(wds.toDescription());
        //180 mins from WRK + 20mins from PREM
        assertEquals(200 , wds.getMinutes(null , null, "TRN", true, null, true));
    }

    public void testChange5() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");

        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK,GUAR");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_DURATION, "0");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_PREMIUM_DETAIL, "P");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setWrkdTcodeName("WRK");
        ins2.setWrkdMinutes(120);

        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 1);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkPremiumsForDate(empId , start);
        //120 mins from WRK should have been converted to TRN, should only have one work premium detail
        assertEquals(120 , wds.getMinutes(null , null, "TRN", true, null, true));
        assertEquals(1, wds.size());
    }

    /**
     * @throws Exception
     */
    public void testChangeCapTime() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_TIME, "12:00");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9 * 60);
        Datetime clk1Off = DateHelper.addMinutes(start, 20 * 60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        assertTrue(wds.size() == 2);
        assertEquals(480 , wds.getMinutes(null , null, "TRN", true, null, true));
    }

    /**
     * @throws Exception
     */
    public void testChangeExpression() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_DURATION, "480");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_EXPRESSION_STRING, "wrkdJobName=JANITOR");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 20*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        clk1On = DateHelper.addMinutes(start, 11*60);
        clk1Off = DateHelper.addMinutes(start, 20*60);
        ins2.setStartTime(clk1On) ; ins2.setEndTime(clk1Off) ;
        ins2.setWrkdTcodeName("WRK");
        ins2.setWrkdJobName("JANITOR");

        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertEquals(60 , wds.getMinutes(null , null, "TRN", true, null, true));
    }


    /**
     * @throws Exception
     */
    public void testChangeInsideSchedule() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_TIME,
                                ChangeTcodeHtypeDurationRule.PARAM_VAL_INSIDE_SCHEDULE );
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        clearAndAddRule(empId , start , rule , ruleparams);

        /*InsertEmployeeScheduleOverride insS = new InsertEmployeeScheduleOverride(getConnection());
        insS.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE  );
        insS.setWbuNameBoth("JUNIT", "JUNIT");
        insS.setEmpId(empId);
        insS.setStartDate(start);      insS.setEndDate(start);
        Datetime On = DateHelper.addMinutes(start, 9*60);
        Datetime Off = DateHelper.addMinutes(start, 17*60);
        insS.setEmpskdActStartTime(On);
        insS.setEmpskdActEndTime(Off);
        //On = DateHelper.addMinutes(start, 21*60);
        //Off = DateHelper.addMinutes(start, 23*60);
        //insS.setEmpskdActStartTime2(On);
        //insS.setEmpskdActEndTime2(Off);
        ovrBuilder.add(insS);*/

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start,  8*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 10*60);
        ins2.setStartTime(clk1On);      ins2.setEndTime(clk1Off);
        ins2.setWrkdTcodeName("HOL");

        ovrBuilder.add(ins2);


        InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride(getConnection());
        ins3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins3.setWbuNameBoth("JUNIT", "JUNIT");
        ins3.setEmpId(empId);
        ins3.setStartDate(start);      ins3.setEndDate(start);
        clk1On = DateHelper.addMinutes(start,  12*60);
        clk1Off = DateHelper.addMinutes(start, 14*60);
        ins3.setStartTime(clk1On);      ins3.setEndTime(clk1Off);
        ins3.setWrkdTcodeName("BRK");

        ovrBuilder.add(ins3);

        InsertWorkDetailOverride ins4 = new InsertWorkDetailOverride(getConnection());
        ins4.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins4.setWbuNameBoth("JUNIT", "JUNIT");
        ins4.setEmpId(empId);
        ins4.setStartDate(start);      ins4.setEndDate(start);
        clk1On = DateHelper.addMinutes(start,  14*60);
        clk1Off = DateHelper.addMinutes(start, 19*60);
        ins4.setStartTime(clk1On);      ins4.setEndTime(clk1Off);
        ins4.setWrkdTcodeName("WRK");

        ovrBuilder.add(ins4);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription() );
        //assertTrue(wds.size() == 2);
        assertEquals(300 , wds.getMinutes(null , null, "TRN", true, null, true));
    }

    /**
     * @throws Exception
     */
    public void testChangeOutsideSchedule() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_TIME,
                                ChangeTcodeHtypeDurationRule.PARAM_VAL_OUTSIDE_SCHEDULE );
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride insS = new InsertEmployeeScheduleOverride(getConnection());
        insS.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE  );
        insS.setWbuNameBoth("JUNIT", "JUNIT");
        insS.setEmpId(empId);
        insS.setStartDate(start);      insS.setEndDate(start);
        Datetime On = DateHelper.addMinutes(start, 9*60);
        Datetime Off = DateHelper.addMinutes(start, 17*60);
        insS.setEmpskdActStartTime(On);
        insS.setEmpskdActEndTime(Off);
        On = DateHelper.addMinutes(start, 21*60);
        Off = DateHelper.addMinutes(start, 23*60);
        insS.setEmpskdActStartTime2(On);
        insS.setEmpskdActEndTime2(Off);
        ovrBuilder.add(insS);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start,  8*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 23*60);
        ins2.setStartTime(clk1On);      ins2.setEndTime(clk1Off);
        ins2.setWrkdTcodeName("WRK");

        ovrBuilder.add(ins2);



        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription() );
        //assertTrue(wds.size() == 2);
        assertEquals(300 , wds.getMinutes(null , null, "TRN", true, null, true));
    }

    /**
     * @throws Exception
     */
    public void testChangeForEachDetail() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK,BRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_DURATION, "180");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_TIME,
                                ChangeTcodeHtypeDurationRule.PARAM_VAL_CAPDURATION_FOR_EACH_DETAIL_PREFIX + ",>=" );
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 540);
        Datetime clk1Off = DateHelper.addMinutes(start, 1200);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start, 780));
        ins2.setEndTime(DateHelper.addMinutes(start, 900));
        ins2.setWrkdTcodeName("BRK");

        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertEquals(540 , wds.getMinutes(null , null, "TRN", true, null, true));
    }

    /**
     * @throws Exception
     */
    public void testChangeCapTimeEnd() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_TIME, "00:00~06:00");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride insS = new InsertEmployeeScheduleOverride(getConnection());
        insS.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE  );
        insS.setWbuNameBoth("JUNIT", "JUNIT");
        insS.setEmpId(empId);
        insS.setStartDate(start);      insS.setEndDate(start);
        Datetime On = DateHelper.addMinutes(start, 21*60);
        Datetime Off = DateHelper.addMinutes(start, 28*60);
        insS.setEmpskdActStartTime(On);
        insS.setEmpskdActEndTime(Off);
        ovrBuilder.add(insS);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 23 * 60);
        Datetime clk1Off = DateHelper.addMinutes(start, 28 * 60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        //assertTrue(wds.size() == 2);
        //assertEquals(300 , wds.getMinutes(null , null, "TRN", true, null, true));
    }

    public void testChangeOutsideScheduleApplyTo() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_TIME,
                                ChangeTcodeHtypeDurationRule.PARAM_VAL_OUTSIDE_SCHEDULE );
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_APPLY_TO_DAY, ChangeTcodeHtypeDurationRule.PARAM_VAL_APPLY_TO_DAY_CURRENT_DAY);
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride insS = new InsertEmployeeScheduleOverride(getConnection());
        insS.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE  );
        insS.setWbuNameBoth("JUNIT", "JUNIT");
        insS.setEmpId(empId);
        insS.setStartDate(start);      insS.setEndDate(start);
        Datetime On = DateHelper.addMinutes(start, 9*60);
        Datetime Off = DateHelper.addMinutes(start, 17*60);
        insS.setEmpskdActStartTime(On);
        insS.setEmpskdActEndTime(Off);
        On = DateHelper.addMinutes(start, 21*60);
        Off = DateHelper.addMinutes(start, 23*60);
        insS.setEmpskdActStartTime2(On);
        insS.setEmpskdActEndTime2(Off);
        ovrBuilder.add(insS);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start,  8*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 26*60);
        ins2.setStartTime(clk1On);      ins2.setEndTime(clk1Off);
        ins2.setWrkdTcodeName("WRK");

        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription() );
        //assertTrue(wds.size() == 2);
        assertEquals(360 , wds.getMinutes(null , null, "TRN", true, null, true));


    }

    public void testChangeCapTimeApplyTo() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new ChangeTcodeHtypeDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CAP_TIME, "12:00");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CHANGE_TO_TIME_CODE, "TRN");
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_APPLY_TO_DAY, ChangeTcodeHtypeDurationRule.PARAM_VAL_APPLY_TO_DAY_NEXT_DAY);
        ruleparams.addParameter(ChangeTcodeHtypeDurationRule.PARAM_CONDITIONSET_IS_MUTUALLY_EXCLUSIVE, "false");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9 * 60);
        Datetime clk1Off = DateHelper.addMinutes(start, 28 * 60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription() );
        //assertTrue(wds.size() == 2);
        assertEquals(240 , wds.getMinutes(null , null, "TRN", true, null, true));
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
