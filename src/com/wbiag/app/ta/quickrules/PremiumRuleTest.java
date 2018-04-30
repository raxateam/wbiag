package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for PremiumRuleTest.
 */
public class PremiumRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumRuleTest.class);

    public PremiumRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(PremiumRuleTest.class);
        BasicConfigurator.configure();
        return result;
    }


    /**
     * @throws Exception
     */
    public void testJob() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String jobName = "JANITOR";
        final String premCode = "TRN";
        final double premRate = 10;
        final int maxMins = 300;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        ruleparams.addParameter(PremiumRule.PARAM_USE_DAY_MAX_MINUTES, String.valueOf(maxMins));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 600);
        Datetime end1 = DateHelper.addMinutes(start, 960);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdJobName(jobName);
        ovrBuilder.add(ins);

        ovrBuilder.setFirstStartDate(start);
        ovrBuilder.setLastEndDate(start);
        IntegerList emps = new IntegerList();
        emps.add(empId);
        ovrBuilder.setEmpIdList(emps);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertWorkPremiumTimeCodeMinutes(empId , start , premCode, maxMins);

    }


    /**
     * @throws Exception
     */
    public void testJobFlag() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String jobName = "JANITOR";
        final String premCode = "TRN";
        final double premRate = 10;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumRule.PARAM_ELIGIBLE_WORKDETAIL_CONDITION, "wrkdFlag1=Y");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode );
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_USE_MINUTES, "60");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 600);
        Datetime end1 = DateHelper.addMinutes(start, 960);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdJobName(jobName);
        ins.setWrkdFlag1("Y");
        ovrBuilder.add(ins);

        ovrBuilder.setFirstStartDate(start);
        ovrBuilder.setLastEndDate(start);
        IntegerList emps = new IntegerList();
        emps.add(empId);
        ovrBuilder.setEmpIdList(emps);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertWorkPremiumTimeCodeMinutes(empId , start , premCode, 60);

    }


    /**
     * @throws Exception
     */
    public void testJobDetailPremium() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String tcodeName = "HOL";
        final String premCode = "TRN";
        final double premRate = 1;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_ELIGIBLE_TCODENAMES, "HOL");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode );
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        ruleparams.addParameter(PremiumRule.PARAM_USE_DETAIL_PREMIUM, PremiumRule.PARAM_VAL_USE_DETAIL_PREMIUM_BOTH);
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 600);
        Datetime end1 = DateHelper.addMinutes(start, 960);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdTcodeName(tcodeName);
        ovrBuilder.add(ins);

        InsertWorkPremiumOverride ins2 = new InsertWorkPremiumOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setWrkdTcodeName(tcodeName);
        ins2.setWrkdMinutes(480);
        ovrBuilder.add(ins2);

        ovrBuilder.setFirstStartDate(start);
        ovrBuilder.setLastEndDate(start);
        IntegerList emps = new IntegerList();
        emps.add(empId);
        ovrBuilder.setEmpIdList(emps);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(wps.toDescription());
        //System.out.println(getWorkDetailsForDate(empId , start).toDescription());
        assertEquals( 60*14 , wps.getMinutes(null, null, "TRN", true, null, true)  );

    }

    /**
     * @throws Exception
     */
    public void test24Hour() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "TRN";
        final double premRate = 10;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Sun") ;
        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_START_TIME, "00:00");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_END_TIME, "00:00");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_IS_24_HOUR, "true");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, -120);
        Datetime end1 = DateHelper.addMinutes(start, 420);
        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(st1 , end1));
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertWorkPremiumTimeCodeMinutes(empId , start , premCode, 420);

    }

    /**
     * @throws Exception
     */
    public void test24HourPreviousDay() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "TRN";
        final double premRate = 10;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Sun") ;
        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_START_TIME, "00:00");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_END_TIME, "00:00");
        ruleparams.addParameter(PremiumRule.PARAM_APPLIY_TO, "PreviousDay");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_IS_24_HOUR, "true");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, -120);
        Datetime end1 = DateHelper.addMinutes(start, 420);
        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(st1 , end1));
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertWorkPremiumTimeCodeMinutes(empId , start , premCode,120);

    }

    /**
     * @throws Exception
     */
    public void test24HourNextDay() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "TRN";
        final double premRate = 10;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_START_TIME, "00:00");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_END_TIME, "00:00");
        ruleparams.addParameter(PremiumRule.PARAM_APPLIY_TO, "NextDay");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_IS_24_HOUR, "true");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 1020);
        Datetime end1 = DateHelper.addMinutes(start, 1500);
        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(st1 , end1));
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertWorkPremiumTimeCodeMinutes(empId , start , premCode,60);

    }

    /**
     * @throws Exception
     */
    public void testWeekMax() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String jobName = "JANITOR";
        final String premCode = "TRN";
        final double premRate = 10;
        final int maxMins = 300;
        final int maxWeekMins = 1320;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        Date end = DateHelper.addDays(start, 4);

        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        ruleparams.addParameter(PremiumRule.PARAM_USE_DAY_MAX_MINUTES, String.valueOf(maxMins));
        ruleparams.addParameter(PremiumRule.PARAM_USE_WEEK_MAX_MINUTES, String.valueOf(maxWeekMins));
        clearAndAddRule(empId , start , rule , ruleparams);

        for (Date date = start; date.compareTo(end) <= 0;
             date = DateHelper.addDays(date, 1)) {
            InsertWorkDetailOverride ins = new InsertWorkDetailOverride(
                getConnection());
            ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
            ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
            ins.setEmpId(empId);
            ins.setStartDate(date);
            ins.setEndDate(date);
            Datetime st1 = DateHelper.addMinutes(date, 600);
            Datetime end1 = DateHelper.addMinutes(date, 960);
            ins.setStartTime(st1);
            ins.setEndTime(end1);
            ins.setWrkdJobName(jobName);
            ovrBuilder.add(ins);
        }

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , end);
        assertWorkPremiumTimeCodeMinutes(empId , end , premCode, 120);

    }

    /**
     * @throws Exception
     */
    public void testMin() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String jobName = "JANITOR";
        final String premCode = "TRN";
        final double premRate = 10;
        final int minMins = 420;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        ruleparams.addParameter(PremiumRule.PARAM_USE_DAY_MIN_MINUTES, String.valueOf(minMins));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 600);
        Datetime end1 = DateHelper.addMinutes(start, 960);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdJobName(jobName);
        ovrBuilder.add(ins);

        ovrBuilder.setFirstStartDate(start);
        ovrBuilder.setLastEndDate(start);
        IntegerList emps = new IntegerList();
        emps.add(empId);
        ovrBuilder.setEmpIdList(emps);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 0);

    }

    public void testEmpVal() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "TRN";

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_EMPLOYEE_VAL, "empVal1");
        ruleparams.addParameter(PremiumRule.PARAM_MULTIPLE, "60");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeOverride ins = new InsertEmployeeOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setEmpVal1("8");

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 1);
        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 1);
        assertTrue(((WorkDetailData)wps.get(0)).getWrkdMinutes() == 480);

    }

    public void testCrossMidnightWorkDetailFromYesterday() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "TRN";

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        Date dayBeforeStart = DateHelper.addDays(start, -1);

        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_START_TIME, "00:00");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_END_TIME, "00:00");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_IS_24_HOUR, "true");
        ruleparams.addParameter(PremiumRule.PARAM_ELIGIBLE_TCODENAMES, "WRK");
        ruleparams.addParameter(PremiumRule.PARAM_USE_DETAIL_PREMIUM, "D");
        ruleparams.addParameter(PremiumRule.PARAM_USE_HOURTYPE_MULTIPLIER, "false");
        clearAndAddRule(empId , start , rule , ruleparams);

        //create a wrks ovr for dayBeforeStart date (cross-midnight)
        InsertWorkSummaryOverride wdForDayBeforeStartDate = new InsertWorkSummaryOverride(getConnection());
        wdForDayBeforeStartDate.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        wdForDayBeforeStartDate.setEmpId(empId);

        wdForDayBeforeStartDate.setStartDate(dayBeforeStart);       //for any override
        wdForDayBeforeStartDate.setEndDate(dayBeforeStart);         //for any override

        Datetime st1 = DateHelper.addMinutes(dayBeforeStart, 1110);
        Datetime end1 = DateHelper.addMinutes(start, 150);
        wdForDayBeforeStartDate.setWrksClocks(createWorkSummaryClockStringForOnOffs(st1 , end1));
        wdForDayBeforeStartDate.setStartTime(st1);                  //specific to wrks
        wdForDayBeforeStartDate.setEndTime(end1);                   //specific to wrks

        //create a wrks ovr for start date
        InsertWorkSummaryOverride wdForStartDate = new InsertWorkSummaryOverride(getConnection());
        wdForStartDate.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        wdForStartDate.setEmpId(empId);
        wdForStartDate.setStartDate(start);
        wdForStartDate.setEndDate(start);
        Datetime st2 = DateHelper.addMinutes(start, 540);
        Datetime end2 = DateHelper.addMinutes(start, 720);
        wdForStartDate.setWrksClocks(createWorkSummaryClockStringForOnOffs(st2 , end2));
        wdForStartDate.setStartTime(st2);
        wdForStartDate.setEndTime(end2);



        ovrBuilder.add(wdForDayBeforeStartDate);
        ovrBuilder.add(wdForStartDate);
        ovrBuilder.execute(true , true);

        assertOverrideAppliedCount(ovrBuilder , 2);
        //assertRuleApplied(empId, start, rule);


        WorkDetailList wds = getWorkDetailsForDate(empId, start);
        //System.out.println(wds);
        assertTrue(wds.size() == 2);
        assertTrue(wds.getWorkDetail(0).getWrkdTcodeName().equals("WRK"));


        WorkDetailList wds2 = getWorkDetailsForDate(empId, dayBeforeStart);
        assertTrue(wds2.size() == 1);
        assertTrue(wds2.getWorkDetail(0).getWrkdTcodeName().equals("WRK"));


        WorkDetailList wps = getWorkPremiumsForDate(empId, start);
        //System.out.println(wps);
        //there must be one premium added.
        assertTrue(wps.size() == 1);
        assertTrue(wps.getWorkDetail(0).getWrkdMinutes() == 330);


    }

    /**
     * @throws Exception
     */
    public void testHourMultiplier() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String premCode = "TRN";
        final double premRate = 10;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new PremiumRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_START_TIME, "00:00");
        ruleparams.addParameter(PremiumRule.PARAM_ZONE_END_TIME, "00:00");
        //ruleparams.addParameter(PremiumRule.PARAM_ZONE_IS_24_HOUR, "true");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumRule.PARAM_USE_HOURTYPE_MULTIPLIER, "true");
        ruleparams.addParameter(PremiumRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, 9*60);
        Datetime end1 = DateHelper.addMinutes(start, 17*60);
        ins.setStartTime(st1);      ins.setEndTime(end1);
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ins.setWrkdHtypeName("OT2");
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertWorkPremiumTimeCodeMinutes(empId , start , premCode, 960);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
