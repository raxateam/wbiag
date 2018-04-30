package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;


/**
 * Test for PremumRuleTest.
 */
public class PremiumMultipleRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumMultipleRuleTest.class);

    public PremiumMultipleRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(PremiumMultipleRuleTest.class);
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
        Rule rule = new PremiumMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        ruleparams.addParameter(PremiumMultipleRule.PARAM_USE_DAY_MAX_MINUTES, String.valueOf(maxMins));
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

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 1);

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
        Rule rule = new PremiumMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ELIGIBLE_WORKDETAIL_CONDITION, "wrkdFlag1=Y");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_TCODE_NAME, premCode );
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_USE_MINUTES, "60");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
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

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 1);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertWorkPremiumTimeCodeMinutes(empId , start , premCode, 60);

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
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new PremiumMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ZONE_START_TIME, "00:00");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ZONE_END_TIME, "00:00");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ZONE_IS_24_HOUR, "true");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime st1 = DateHelper.addMinutes(start, -120);
        Datetime end1 = DateHelper.addMinutes(start, 1020);
        ins.setWrksClocks(createWorkSummaryClockStringForOnOffs(st1 , end1));
        ins.setStartTime(st1);  ins.setEndTime(end1);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 1);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertWorkPremiumTimeCodeMinutes(empId , start , premCode, 1020);

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
        Rule rule = new PremiumMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        ruleparams.addParameter(PremiumMultipleRule.PARAM_USE_DAY_MAX_MINUTES, String.valueOf(maxMins));
        ruleparams.addParameter(PremiumMultipleRule.PARAM_USE_WEEK_MAX_MINUTES, String.valueOf(maxWeekMins));
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

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 5);

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
        Rule rule = new PremiumMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ELIGIBLE_JOBNAMES, jobName);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        ruleparams.addParameter(PremiumMultipleRule.PARAM_USE_DAY_MIN_MINUTES, String.valueOf(minMins));
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

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 1);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 0);

    }

    /**
     * @throws Exception
     */
    public void testMultiple() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "TRN";
        final double premRate = 10;
        final int minMins = 420;
        Date start = DateHelper.convertStringToDate("04/18/2005", "MM/dd/yyyy");

        // *** create the rule
        Rule rule = new PremiumMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ELIGIBLE_TCODENAMES, "TRN");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setStartTime(DateHelper.parseDate("04/18/2005 10:00", "MM/dd/yyyy HH:mm"));
        ins.setEndTime(DateHelper.parseDate("04/18/2005 12:00", "MM/dd/yyyy HH:mm"));
        ins.setWrkdTcodeName("TRN");
        ins.setWrkdHtypeName("REG");
        ins.setWrkdRate(0);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setStartTime(DateHelper.parseDate("04/18/2005 13:00", "MM/dd/yyyy HH:mm"));
        ins.setEndTime(DateHelper.parseDate("04/18/2005 14:00", "MM/dd/yyyy HH:mm"));
        ins.setWrkdTcodeName("TRN");
        ins.setWrkdHtypeName("REG");
        ins.setWrkdRate(0);
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 2);

    }

    public void testConsecutiveWorkDetails() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "TRN";
        final double premRate = 10;
        final int minMins = 420;
        Date start = DateHelper.convertStringToDate("04/19/2005", "MM/dd/yyyy");

        // *** create the rule
        Rule rule = new PremiumMultipleRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(PremiumMultipleRule.PARAM_ELIGIBLE_TCODENAMES, "TRN");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_TCODE_NAME, premCode);
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_HTYPE_NAME, "REG");
        ruleparams.addParameter(PremiumMultipleRule.PARAM_PREMIUM_RATE, String.valueOf(premRate));
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setStartTime(DateHelper.parseDate("04/19/2005 10:00", "MM/dd/yyyy HH:mm"));
        ins.setEndTime(DateHelper.parseDate("04/19/2005 12:00", "MM/dd/yyyy HH:mm"));
        ins.setWrkdTcodeName("TRN");
        ins.setWrkdHtypeName("REG");
        ins.setWrkdJobName("JANITOR");
        ins.setWrkdRate(0);
        ovrBuilder.add(ins);

        ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setStartTime(DateHelper.parseDate("04/19/2005 12:00", "MM/dd/yyyy HH:mm"));
        ins.setEndTime(DateHelper.parseDate("04/19/2005 14:00", "MM/dd/yyyy HH:mm"));
        ins.setWrkdTcodeName("TRN");
        ins.setWrkdHtypeName("REG");
        ins.setWrkdRate(0);
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 1);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
