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
 * Test for LateBreakRule.
 */
public class LateBreakRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LateBreakRuleTest.class);

    public LateBreakRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(LateBreakRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testShiftStart() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "TRN" , checkCode = "WRK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateBreakRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateBreakRule.PARAM_BREAK_TIMECODELIST, "BRK");
        ruleparams.addParameter(LateBreakRule.PARAM_LATE_BREAK_PREMIUM_TIMECODE, premCode);
        ruleparams.addParameter(LateBreakRule.PARAM_MAX_WORK_MINS_PRE_BREAK, "240");
        ruleparams.addParameter(LateBreakRule.PARAM_WORK_TIMECODELIST, checkCode);
        ruleparams.addParameter(LateBreakRule.PARAM_WORK_STARTTIME, LateBreakRule.PARAM_VAL_WS_SHIFT_START );

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setStartTime(DateHelper.addMinutes(start , 13*60 + 20));
        ins.setEndTime(DateHelper.addMinutes(start , 13*60 + 50));
        ins.setWrkdTcodeName("BRK");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 1);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 1);
        // *** premiums
        assertWorkPremiumTimeCodeMinutes(empId , start, premCode , 20);
    }

    /**
     * @throws Exception
     */
    public void testAnytime() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String premCode = "TRN" , checkCode = "WRK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new LateBreakRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(LateBreakRule.PARAM_BREAK_TIMECODELIST, "BRK");
        ruleparams.addParameter(LateBreakRule.PARAM_LATE_BREAK_PREMIUM_TIMECODE, premCode);
        ruleparams.addParameter(LateBreakRule.PARAM_MAX_WORK_MINS_PRE_BREAK, "240");
        ruleparams.addParameter(LateBreakRule.PARAM_WORK_TIMECODELIST, checkCode);
        ruleparams.addParameter(LateBreakRule.PARAM_WORK_STARTTIME, LateBreakRule.PARAM_VAL_WS_ANYTIME_WITHIN_SHIFT );

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setStartTime(DateHelper.addMinutes(start , 13*60 + 20));
        ins.setEndTime(DateHelper.addMinutes(start , 13*60 + 50));
        ins.setWrkdTcodeName("BRK");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start , 18*60));
        ins2.setEndTime(DateHelper.addMinutes(start , 18*60 + 30));
        ins2.setWrkdTcodeName("BRK");
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins2);

        InsertWorkSummaryOverride insCl1 = new InsertWorkSummaryOverride(getConnection());
        insCl1.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insCl1.setEmpId(empId);
        insCl1.setStartDate(start);      insCl1.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9*60);
        Datetime clk1Off= DateHelper.addMinutes(start, 20*60);
        insCl1.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1On , clk1Off));
        ovrBuilder.add(insCl1);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 3);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 2);
        // *** premiums
        assertEquals(20 , wps.getWorkDetail(0).getWrkdMinutes());
        assertEquals(10 , wps.getWorkDetail(1).getWrkdMinutes());
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

