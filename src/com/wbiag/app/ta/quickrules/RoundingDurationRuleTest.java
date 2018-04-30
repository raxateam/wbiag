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
 * Test for RoundingDurationRule.
 */
public class RoundingDurationRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RoundingDurationRuleTest.class);

    public RoundingDurationRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(RoundingDurationRuleTest.class);
        return result;
    }


    /**
     * Tests round up
     */
    public void testRoundUp() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String premCode = "RND";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new RoundingDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(RoundingDurationRule.PARAM_ELIGIBLE_TIMECODES, "WRK");
        ruleparams.addParameter(RoundingDurationRule.PARAM_MULTIPLE, String.valueOf(15));
        ruleparams.addParameter(RoundingDurationRule.PARAM_SPLIT, String.valueOf(7));
        ruleparams.addParameter(RoundingDurationRule.PARAM_PREMIUM_TIMECODE, premCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 460);
        Datetime clk1Off = DateHelper.addMinutes(start, 993);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 1);
        assertWorkPremiumTimeCodeMinutes(empId , start, premCode, 7);
    }

    /**
     * Tests round down
     */
    public void testRoundDown() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String premCode = "RND";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new RoundingDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(RoundingDurationRule.PARAM_ELIGIBLE_TIMECODES, "WRK");
        ruleparams.addParameter(RoundingDurationRule.PARAM_MULTIPLE, String.valueOf(15));
        ruleparams.addParameter(RoundingDurationRule.PARAM_SPLIT, String.valueOf(7));
        ruleparams.addParameter(RoundingDurationRule.PARAM_PREMIUM_TIMECODE, premCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 460);
        Datetime clk1Off = DateHelper.addMinutes(start, 990);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 1);
        assertWorkPremiumTimeCodeMinutes(empId , start, premCode, -5);
    }

    /**
     * Tests round down
     */
    public void testRoundDownNoTcode() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String premCode = "RND";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new RoundingDurationRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(RoundingDurationRule.PARAM_ELIGIBLE_TIMECODES, "WRK,BRK");
        ruleparams.addParameter(RoundingDurationRule.PARAM_MULTIPLE, String.valueOf(15));
        ruleparams.addParameter(RoundingDurationRule.PARAM_SPLIT, String.valueOf(7));

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 460);
        Datetime clk1Off = DateHelper.addMinutes(start, 988);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start, 988));
        ins2.setEndTime(DateHelper.addMinutes(start, 990));
        ins2.setWrkdTcodeName("BRK");
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 2);
        WorkDetailData wp1 = wps.getWorkDetail(0);
        WorkDetailData wp2 = wps.getWorkDetail(1);
        assertTrue(wp1.getWrkdTcodeName().equals("BRK")
                   && wp1.getWrkdMinutes() == -2);
        assertTrue(wp2.getWrkdTcodeName().equals("WRK")
                   && wp2.getWrkdMinutes() == -3);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

