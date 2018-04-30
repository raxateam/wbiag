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
 * Test for DayLightSavingUnauthorizeRuleTest.
 */
public class DayLightSavingUnauthorizeRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DayLightSavingUnauthorizeRuleTest.class);

    public DayLightSavingUnauthorizeRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(DayLightSavingUnauthorizeRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testUnauthorizeForSchedule() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;
        Date start = DateHelper.parseSQLDate("2004-10-31");

        // *** create def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 DateHelper.addDays(start, -1) ,
                                 DateHelper.addDays(start, 1)).execute(false);
        // *** create the rule
        Rule rule = new DayLightSavingUnauthorizeRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DayLightSavingUnauthorizeRule.PARAM_UNAUTHORIZE_IF_SCHEDULE_OVERLAPS_DST_CHANGE,
                                "true");
        ruleparams.addParameter(DayLightSavingUnauthorizeRule.PARAM_UNAUTHORIZE_IF_NO_OF_CLOCKS_OVERLAPPING_DST_CHANGE,
                                "0");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertOverride ins = new InsertOverride();
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime skdStart = DateHelper.addMinutes(start, -120);
        Datetime skdEnd = DateHelper.addMinutes(start, 300);
        ins.setOvrNewValue("\"EMPSKD_ACT_START_TIME=" + DateHelper.convertDateString(skdStart , OverrideData.OVERRIDE_TIME_FORMAT_STR)  + "\","
                           + "\"EMPSKD_ACT_END_TIME=" + DateHelper.convertDateString(skdEnd , OverrideData.OVERRIDE_TIME_FORMAT_STR)  + "\"");

        ovrBuilder.setFirstStartDate(start);
        ovrBuilder.setLastEndDate(start);
        IntegerList emps = new IntegerList();
        emps.add(empId);
        ovrBuilder.setEmpIdList(emps);
        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkSummaryData ws = getWorkSummaryForDate(empId , start);
        assertEquals("Must be unauthorized" , "N" , ws.getWrksAuthorized());
        assertEquals(DayLightSavingUnauthorizeRule.UNAUTH_TYPE_SCHEDULE_OVERLAPS_DST_CHANGE , ws.getWrksMessages());
    }


    /**
     * @throws Exception
     */
    public void testUnauthorizeForClocks() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;
        Date start = DateHelper.parseSQLDate("2004-10-31");

        // *** create def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 DateHelper.addDays(start, -1) ,
                                 DateHelper.addDays(start, 1)).execute(false);
        // *** create the rule
        Rule rule = new DayLightSavingUnauthorizeRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DayLightSavingUnauthorizeRule.PARAM_UNAUTHORIZE_IF_SCHEDULE_OVERLAPS_DST_CHANGE,
                                "false");
        ruleparams.addParameter(DayLightSavingUnauthorizeRule.PARAM_UNAUTHORIZE_IF_NO_OF_CLOCKS_OVERLAPPING_DST_CHANGE,
                                "2");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertOverride ins = new InsertOverride();
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setOvrType(OverrideData.WORK_SUMMARY_TYPE_START);
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, -120);
        Datetime clk1Off = DateHelper.addMinutes(start, 90);
        Datetime clk2On = DateHelper.addMinutes(start, 120);
        Datetime clk2Off = DateHelper.addMinutes(start, 300);

        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off , clk2On , clk2Off);
        ins.setOvrNewValue("\"WRKS_CLOCKS=" + clks + "\"");

        ovrBuilder.setFirstStartDate(start);
        ovrBuilder.setLastEndDate(start);
        IntegerList emps = new IntegerList();
        emps.add(empId);
        ovrBuilder.setEmpIdList(emps);
        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        WorkSummaryData ws = getWorkSummaryForDate(empId , start);
        assertEquals("Must be unauthorized" , "N" , ws.getWrksAuthorized());
        assertEquals(DayLightSavingUnauthorizeRule.UNAUTH_TYPE_NO_OF_CLOCKS_OVERLAPPING_DST_CHANGE , ws.getWrksMessages());
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
