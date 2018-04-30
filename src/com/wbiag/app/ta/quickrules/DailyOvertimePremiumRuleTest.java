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
 * Test for DailyOvertimePremiumRuleTest.
 */
public class DailyOvertimePremiumRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DailyOvertimePremiumRuleTest.class);

    public DailyOvertimePremiumRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(DailyOvertimePremiumRuleTest.class);
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
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule
        Rule rule1 = new GuaranteesRule();
        Parameters ruleparams1 = new Parameters();
        ruleparams1.addParameter(GuaranteesRule.PARAM_GUARANTEED_MINUTES , "240");
        ruleparams1.addParameter(GuaranteesRule.PARAM_TIMECODELIST , "RECALL");
        ruleparams1.addParameter(GuaranteesRule.PARAM_GUARANTEED_PREMIUM_TIMECODE , premCode);

        clearAndAddRule(empId , start , rule1 , ruleparams1);

        Rule rule2 = new DailyOvertimePremiumRule();
        Parameters ruleparams2 = new Parameters();
        ruleparams2.addParameter(DailyOvertimePremiumRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT1=240,OT2=9999");
        ruleparams2.addParameter(DailyOvertimePremiumRule.PARAM_PREMIUM_TIMECODES_COUNTED , premCode);
        ruleparams2.addParameter(DailyOvertimePremiumRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams2.addParameter(DailyOvertimePremiumRule.PARAM_WORKDETAIL_TIMECODES, "WRK,RECALL," + premCode);
        addRule(empId , start , rule2 , ruleparams2);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime on1 = DateHelper.addMinutes(start, 18*60);
        Datetime off1 = DateHelper.addMinutes(start, 20*60);
        ins.setStartTime(on1);
        ins.setEndTime(off1);
        ins.setWrkdTcodeName("RECALL");
        ins.setWrkdHtypeName("REG");
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule1);
        assertRuleApplied(empId, start, rule2);

        WorkDetailList wpl = getWorkPremiumsForDate(empId , start) ;
        assertEquals(1, wpl.size());
        WorkDetailData wd = wpl.getWorkDetail(0);
        assertEquals(120 , wd.getWrkdMinutes());
        assertEquals("OT1" , wd.getWrkdHtypeName());
    }


    /**
     * @throws Exception
     */
    public void testOff() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String premCode = "TRN";
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate(), "SAT");

        // *** create the rule
        Rule rule2 = new DailyOvertimePremiumRule();
        Parameters ruleparams2 = new Parameters();
        ruleparams2.addParameter(DailyOvertimePremiumRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT1=240,OT2=9999");
        ruleparams2.addParameter(DailyOvertimePremiumRule.PARAM_PREMIUM_TIMECODES_COUNTED , premCode);
        ruleparams2.addParameter(DailyOvertimePremiumRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams2.addParameter(DailyOvertimePremiumRule.PARAM_WORKDETAIL_TIMECODES, "WRK," + premCode);
        addRule(empId , start , rule2 , ruleparams2);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setOvrType(OverrideData.SCHEDULE_SHIFT_TYPE);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setEmpskdActShiftName("OFF");
        ovrBuilder.add(ins);

        InsertWorkPremiumOverride ins2 = new InsertWorkPremiumOverride(getConnection());
        ins2.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setWrkdMinutes(540);
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule2);

        WorkDetailList wpl = getWorkPremiumsForDate(empId , start) ;
        assertEquals(2, wpl.size());
        WorkDetailData wd = wpl.getWorkDetail(0);
        assertEquals(60 , wd.getWrkdMinutes());
        assertEquals("OT1" , wd.getWrkdHtypeName());
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

