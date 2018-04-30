package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;
/**
 * Test for WeeklyOvertimeRule.
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class WeeklyOvertimeSplitRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimeSplitRuleTest.class);

    public WeeklyOvertimeSplitRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WeeklyOvertimeSplitRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testStartPreviousDay() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;
        Date weekStart = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        Date weekEnd = DateHelper.addDays(weekStart , 6);
        //Date start = DateHelper.addDays(weekStart , 4);
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 weekStart, weekEnd).execute(false);
        // *** create the rule
        Rule rule = new WeeklyOvertimeSplitRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_HOURSET_DESCRIPTION, "REG=2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_DAY_WEEK_STARTS, "Monday");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_P24_HOUR_STARTTIME, "19000101 000000");
        clearAndAddRule(empId , weekStart , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekEnd);      ins.setEndDate(weekEnd);
        Datetime clk1On = DateHelper.addMinutes(weekEnd, 1200);
        Datetime clk1Off = DateHelper.addMinutes(weekEnd, 1800);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        RuleEngine.runCalcGroup(getConnection() , empId, weekStart, weekEnd, false) ;

        assertRuleApplied(empId, weekEnd, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , weekEnd);

        assertTrue(wds.size() == 2);
        WorkDetailData wd1 = wds.getWorkDetail(0);
        assertEquals("OT2", wd1.getWrkdHtypeName() ) ;
        assertEquals(240 , wd1.getWrkdMinutes());
        WorkDetailData wd2 = wds.getWorkDetail(1);
        assertEquals("REG", wd2.getWrkdHtypeName() ) ;
        assertEquals(360 , wd2.getWrkdMinutes());

    }

    /**
     * @throws Exception
     */
    public void xStartPreviousDay2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 11;
        Date weekStart = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        Date weekEnd = DateHelper.addDays(weekStart , 6);
        Date lastWeekEnd = DateHelper.addDays(weekStart , -1);
        Date lastWeekStart = DateHelper.addDays(weekStart , -7);
        //Date start = DateHelper.addDays(weekStart , 4);
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 lastWeekStart, weekEnd).execute(false);
        // *** create the rule
        Rule rule = new WeeklyOvertimeSplitRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_HOURSET_DESCRIPTION, "REG=2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_DAY_WEEK_STARTS, "Monday");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_P24_HOUR_STARTTIME, "19000101 223000");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_STARTTIME_DAY, WeeklyOvertimeSplitRule.PARAM_VALUE_PREVIOUS_DAY);
        clearAndAddRule(empId , lastWeekStart , rule , ruleparams);

        InsertEmployeeScheduleOverride insS = new InsertEmployeeScheduleOverride(getConnection());
        insS.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE );
        insS.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insS.setEmpId(empId);
        insS.setStartDate(weekStart);      insS.setEndDate(weekStart);
        Datetime sOn = DateHelper.addMinutes(weekStart, -90);
        Datetime sOff = DateHelper.addMinutes(weekStart, 60 * 6 + 30);
        insS.setEmpskdActStartTime(sOn);
        insS.setEmpskdActEndTime(sOff);
        ovrBuilder.add(insS);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekStart);      ins.setEndDate(weekStart);
        Datetime clk1On = DateHelper.addMinutes(weekStart, -360);
        Datetime clk1Off = DateHelper.addMinutes(weekStart, 60 * 6 + 30);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride ins2 = new InsertWorkSummaryOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(lastWeekEnd);      ins2.setEndDate(lastWeekEnd);
        clk1On = DateHelper.addMinutes(lastWeekEnd, 10*60);
        clk1Off = DateHelper.addMinutes(lastWeekEnd, 17*60);
        clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins2.setWrksClocks(clks);
        ovrBuilder.add(ins2);

        InsertEmployeeOverride insE = new InsertEmployeeOverride(getConnection());
        insE.setWbuNameBoth("JUNIT", "JUNIT");;
        insE.setEmpId(empId);
        insE.setStartDate(DateHelper.DATE_1900 );
        insE.setEndDate(DateHelper.DATE_3000 );
        insE.setEmpDayStartTime(DateHelper.convertStringToDate("19000101 1630000", "yyyyMMdd HHmmss"));
        ovrBuilder.add(insE);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 3);

        RuleEngine.runCalcGroup(getConnection() , empId, lastWeekEnd, weekStart, false) ;

        assertRuleApplied(empId, weekStart, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , weekStart);

        assertTrue(wds.size() == 2);
        WorkDetailData wd1 = wds.getWorkDetail(0);
        assertEquals("OT2", wd1.getWrkdHtypeName() ) ;
        assertEquals(270 , wd1.getWrkdMinutes());
        WorkDetailData wd2 = wds.getWorkDetail(1);
        assertEquals("REG", wd2.getWrkdHtypeName() ) ;
        assertEquals(480 , wd2.getWrkdMinutes());

    }

    public void testStartCurrentDay() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        // An employee on Days shift pattern.
        final int empId = 13;
        Date weekStart = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        Date weekEnd = DateHelper.addDays(weekStart , 6);

        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 DateHelper.addDays(weekStart ,- 8),
								 weekEnd).execute(false);
        // *** create the rule
        Rule rule = new WeeklyOvertimeSplitRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_HOURSET_DESCRIPTION, "REG=2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_DAY_WEEK_STARTS, "Monday");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_STARTTIME_DAY, WeeklyOvertimeSplitRule.PARAM_VALUE_DAY_WEEK_STARTS);
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_P24_HOUR_STARTTIME, "19000101 090000");
        clearAndAddRule(empId , weekStart , rule , ruleparams);

        InsertWorkSummaryOverride ins = null;
        Datetime clk1On = null;
        Datetime clk1Off = null;
        String clks = null;

        ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekStart);
        ins.setEndDate(weekStart);
        clk1On = DateHelper.addMinutes(weekStart, 8*60);
        clk1Off = DateHelper.addMinutes(weekStart, 16*60);
        clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekEnd);
        ins.setEndDate(weekEnd);
        clk1On = DateHelper.addMinutes(weekEnd, 22*60);
        clk1Off = DateHelper.addMinutes(weekEnd, 35*60);
        clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        // Apply the overrides.
        ovrBuilder.execute(true , false);

        // Assert that the overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 2);

        // Recalculate the week.
        RuleEngine.runCalcGroup(getConnection() , empId, weekStart, weekEnd, false) ;

        // Assert that the rule was fired.
        assertRuleApplied(empId, weekEnd, rule);

        WorkDetailData wd = null;
        WorkDetailList wds = null;

        /*
         * Verify the work details on the fist day of the week.
         */
        wds = getWorkDetailsForDate(empId , weekStart);
        assertTrue(wds.size() == 3);

        // 08:00 to 09:00
        wd = wds.getWorkDetail(0);
        assertEquals("OT2", wd.getWrkdHtypeName() ) ;
        assertEquals(60 , wd.getWrkdMinutes());

        // 9:00 to 16:00
        wd = wds.getWorkDetail(1);
        assertEquals("REG", wd.getWrkdHtypeName() ) ;
        assertEquals(420 , wd.getWrkdMinutes());

        // 16:00 to 17:00
        wd = wds.getWorkDetail(2);
        assertEquals("UNPAID", wd.getWrkdHtypeName() ) ;
        assertEquals(60 , wd.getWrkdMinutes());

        // Verify the work detail on the last day of the week.
        wds = getWorkDetailsForDate(empId , weekEnd);
        System.out.println(wds.toDescription() );

        assertTrue(wds.size() == 3);

        // 22:00 to 23:00
        wd = wds.getWorkDetail(0);
        assertEquals("REG", wd.getWrkdHtypeName() ) ;
        assertEquals(60 , wd.getWrkdMinutes());

        // 23:00 to 09:00
        wd = wds.getWorkDetail(1);
        assertEquals("OT2", wd.getWrkdHtypeName() ) ;
        assertEquals(600 , wd.getWrkdMinutes());

        // 09:00 to 11:00
        wd = wds.getWorkDetail(2);
        assertEquals("REG", wd.getWrkdHtypeName() ) ;
        assertEquals(120 , wd.getWrkdMinutes());
    }

    /**
     * @throws Exception
     */
    public void testDiscount() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 11;
        Date weekStart = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        Date weekEnd = DateHelper.addDays(weekStart , 6);
        //Date start = DateHelper.addDays(weekStart , 4);
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 weekStart, weekEnd).execute(false);
        // *** create the rule
        Rule rule = new WeeklyOvertimeSplitRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_HOURSET_DESCRIPTION, "REG=2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_DAY_WEEK_STARTS, "Monday");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_DISCOUNT_TIMECODES, "TRN");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_DISCOUNT_HOURTYPES, "OT2");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_P24_HOUR_STARTTIME, "19000101 000000");
        clearAndAddRule(empId , weekStart , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekEnd);      ins.setEndDate(weekEnd);
        Datetime clk1On = DateHelper.addMinutes(weekEnd, 20*60);
        Datetime clk1Off = DateHelper.addMinutes(weekEnd, 30*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(weekStart);      ins2.setEndDate(weekStart);
        ins2.setStartTime(DateHelper.addMinutes(weekStart, 10*60));
        ins2.setEndTime(DateHelper.addMinutes(weekStart, 12*60));
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        RuleEngine.runCalcGroup(getConnection() , empId, weekStart, weekEnd, false) ;

        assertRuleApplied(empId, weekEnd, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , weekEnd);


        assertTrue(wds.size() == 3);
        WorkDetailData wd1 = wds.getWorkDetail(0);
        assertEquals(120 , wds.getMinutes(null, null, null, true, "OT2", true));

    }


    public void testDST() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        // An employee on Days shift pattern.
        final int empId = 13;
        Date weekStart = DateHelper.parseSQLDate("2006-03-26");;
        Date weekEnd = DateHelper.addDays(weekStart , 6);

        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 DateHelper.addDays(weekStart ,- 8),
                                 weekEnd).execute(false);
        // *** create the rule
        Rule rule = new WeeklyOvertimeSplitRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_HOURSET_DESCRIPTION, "REG=2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_DAY_WEEK_STARTS, "Sunday");
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_STARTTIME_DAY, WeeklyOvertimeSplitRule.PARAM_VALUE_DAY_WEEK_STARTS);
        ruleparams.addParameter(WeeklyOvertimeSplitRule.PARAM_P24_HOUR_STARTTIME, "19000101 090000");
        clearAndAddRule(empId , weekStart , rule , ruleparams);

        InsertWorkSummaryOverride ins = null;
        Datetime clk1On = null;
        Datetime clk1Off = null;
        String clks = null;

        ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekStart);
        ins.setEndDate(weekStart);
        clk1On = DateHelper.addMinutes(weekStart, 8*60);
        clk1Off = DateHelper.addMinutes(weekStart, 16*60);
        clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekEnd);
        ins.setEndDate(weekEnd);
        clk1On = DateHelper.addMinutes(weekEnd, 22*60);
        clk1Off = DateHelper.addMinutes(weekEnd, 35*60);
        clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        // Apply the overrides.
        ovrBuilder.execute(true , false);

        // Assert that the overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 2);

        // Recalculate the week.
        RuleEngine.runCalcGroup(getConnection() , empId, weekStart, weekEnd, false) ;

        // Assert that the rule was fired.
        assertRuleApplied(empId, weekEnd, rule);

        WorkDetailData wd = null;
        WorkDetailList wds = null;

        wds = getWorkDetailsForDate(empId , weekEnd);
        //System.out.println(wds.toDescription() );
        assertTrue(wds.size() == 2);

        // 22:00 to 09:00
        wd = wds.getWorkDetail(0);
        assertEquals("OT2", wd.getWrkdHtypeName() ) ;
        assertEquals(600 , wd.getWrkdMinutes());

        // 09:00 to 11:00
        wd = wds.getWorkDetail(1);
        assertEquals("REG", wd.getWrkdHtypeName() ) ;
        assertEquals(180 , wd.getWrkdMinutes());
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
