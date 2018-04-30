package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.*;
import com.workbrain.app.ta.quickrules.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;
/**
 * Test for WeeklyOvertimePremiumRuleTest.
 */
public class WeeklyOvertimePremiumRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimePremiumRuleTest.class);

    public WeeklyOvertimePremiumRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WeeklyOvertimePremiumRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testGuar() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String premCode = "TRN";
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date fri = DateHelper.addDays(mon,4);

        new CreateDefaultRecords(getConnection() , new int[] {empId}, mon, fri).execute(false);

        Rule rule2 = new WeeklyOvertimePremiumRule();
        Parameters ruleparams2 = new Parameters();
        ruleparams2.addParameter(WeeklyOvertimePremiumRule.PARAM_HOURSET_DESCRIPTION, "REG=2400,OT1=240,OT2=9999");
        ruleparams2.addParameter(WeeklyOvertimePremiumRule.PARAM_DAY_WEEK_STARTS , "MONDAY");
        ruleparams2.addParameter(WeeklyOvertimePremiumRule.PARAM_PREMIUM_TIMECODES_COUNTED , premCode);
        ruleparams2.addParameter(WeeklyOvertimePremiumRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams2.addParameter(WeeklyOvertimePremiumRule.PARAM_WORKDETAIL_TIMECODES, "WRK," + premCode);
        clearAndAddRule(empId , mon , rule2 , ruleparams2);

        InsertWorkPremiumOverride ins2 = new InsertWorkPremiumOverride(getConnection());
        ins2.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(mon);      ins2.setEndDate(mon);
        ins2.setWrkdTcodeName(premCode);
        ins2.setWrkdHtypeName("REG");
        ins2.setWrkdMinutes(120);
        ovrBuilder.add(ins2);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        InsertWorkPremiumOverride ins = new InsertWorkPremiumOverride(getConnection());
        ins.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(fri);      ins.setEndDate(fri);
        ins.setWrkdTcodeName(premCode);
        ins.setWrkdHtypeName("REG");
        ins.setWrkdMinutes(120);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertRuleApplied(empId, fri, rule2);

        WorkDetailList wpl = getWorkPremiumsForDate(empId , fri) ;
        assertEquals(1, wpl.size());
        WorkDetailData wd = wpl.getWorkDetail(0);
        assertEquals(120 , wd.getWrkdMinutes());
        assertEquals("OT1" , wd.getWrkdHtypeName());
    }

    /**
     * @throws Exception
     */
    public void testDiscountTCodes() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "HOL";
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tues = DateHelper.addDays(mon,1);
        Date wed = DateHelper.addDays(tues,1);
        Date thurs = DateHelper.addDays(wed,1);
        Date fri = DateHelper.addDays(thurs,1);

        // new CreateDefaultRecords(getConnection() , new int[] {empId}, mon, fri).execute(false);

        Rule rule = new WeeklyOvertimePremiumRule();
        Parameters ruleParams = new Parameters();
        ruleParams.addParameter(WeeklyOvertimePremiumRule.PARAM_HOURSET_DESCRIPTION, "REG=2400,OT1=240,OT2=9999");
        ruleParams.addParameter(WeeklyOvertimePremiumRule.PARAM_DAY_WEEK_STARTS , "SUNDAY");
        ruleParams.addParameter(WeeklyOvertimePremiumRule.PARAM_PREMIUM_TIMECODES_COUNTED , premCode);
        ruleParams.addParameter(WeeklyOvertimePremiumRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleParams.addParameter(WeeklyOvertimePremiumRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleParams.addParameter(WeeklyOvertimePremiumRule.PARAM_DISCOUNT_TIMECODES, premCode);
        clearAndAddRule(empId , mon , rule , ruleParams);

        // Holiday Premium Monday.
        InsertWorkPremiumOverride premOvr = new InsertWorkPremiumOverride(getConnection());
        premOvr.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        premOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        premOvr.setEmpId(empId);
        premOvr.setStartDate(mon);      premOvr.setEndDate(mon);
        premOvr.setWrkdTcodeName(premCode);
        premOvr.setWrkdHtypeName("REG");
        premOvr.setWrkdMinutes(480);

        ovrBuilder.add(premOvr);

        // Holiday Premium Friday.
        premOvr = new InsertWorkPremiumOverride(getConnection());
        premOvr.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        premOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        premOvr.setEmpId(empId);
        premOvr.setStartDate(fri);      premOvr.setEndDate(fri);
        premOvr.setWrkdTcodeName(premCode);
        premOvr.setWrkdHtypeName("REG");
        premOvr.setWrkdMinutes(480);

        ovrBuilder.add(premOvr);

        // 8 hrs Tues.
        InsertWorkSummaryOverride clockOvr = new InsertWorkSummaryOverride(getConnection());
        clockOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        clockOvr.setEmpId(empId);
        clockOvr.setStartDate(tues);      clockOvr.setEndDate(tues);
        Datetime clk1On = DateHelper.addMinutes(tues, 540); // 9:00
        Datetime clk1Off = DateHelper.addMinutes(tues, 1020); // 17:00
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        clockOvr.setWrksClocks(clks);

        ovrBuilder.add(clockOvr);

        // 9 hrs Wed.
        clockOvr = new InsertWorkSummaryOverride(getConnection());
        clockOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        clockOvr.setEmpId(empId);
        clockOvr.setStartDate(wed);      clockOvr.setEndDate(wed);
        clk1On = DateHelper.addMinutes(wed, 540); // 9:00
        clk1Off = DateHelper.addMinutes(wed, 1080); // 18:00
        clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        clockOvr.setWrksClocks(clks);

        ovrBuilder.add(clockOvr);

        // 8 hrs Thurs.
        clockOvr = new InsertWorkSummaryOverride(getConnection());
        clockOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        clockOvr.setEmpId(empId);
        clockOvr.setStartDate(thurs);      clockOvr.setEndDate(thurs);
        clk1On = DateHelper.addMinutes(thurs, 540); // 9:00
        clk1Off = DateHelper.addMinutes(thurs, 1020); // 17:00
        clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        clockOvr.setWrksClocks(clks);

        ovrBuilder.add(clockOvr);

        ovrBuilder.execute(true , false);

        // Thurs should be 7 hrs REG, 1 hr OT1.
        WorkDetailList wdList = getWorkDetailsForDate(empId, thurs);
        assertEquals(420, wdList.getMinutes(null,null,null,true,"REG",true));
        assertEquals(60, wdList.getMinutes(null,null,null,true,"OT1",true));
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

