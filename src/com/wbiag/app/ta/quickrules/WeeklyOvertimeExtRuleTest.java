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
 * Test for WeeklyOvertimeRuleExt.
 */
public class WeeklyOvertimeExtRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimeExtRuleTest.class);

    public WeeklyOvertimeExtRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WeeklyOvertimeExtRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testEmpVal() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;
        Date weekStart = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        Date weekEnd = DateHelper.addDays(weekStart , 6);
        //Date start = DateHelper.addDays(weekStart , 4);
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 weekStart, weekEnd).execute(false);
        // *** create the rule
        Rule rule = new WeeklyOvertimeExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_HOURSET_DESCRIPTION, "REG=EMP_VAL1*60|2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_DAY_WEEK_STARTS, "Monday");
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

        InsertEmployeeOverride ins2 = new InsertEmployeeOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(weekEnd);      ins2.setEndDate(weekEnd);
        ins2.setEmpVal1("40");

        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        RuleEngine.runCalcGroup(getConnection() , empId, weekStart, weekEnd, false) ;

        assertRuleApplied(empId, weekEnd, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , weekEnd);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 1);
        assertTrue(600 == wds.getMinutes(null, null, null, true, "OT2", true));
    }

    /**
     * @throws Exception
     */
    public void testEmpUdf() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;
        Date weekStart = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        Date weekEnd = DateHelper.addDays(weekStart , 6);
        //Date start = DateHelper.addDays(weekStart , 4);
        // *** use TIMEZONE udf for testing
        final int empudfdefTimezone = 100;
        EmpUdfAccess acc = new EmpUdfAccess (getConnection());
        EmpUdfData data = acc.loadByEmpAndUdfDefId(empId, empudfdefTimezone);
        assertNotNull("TIMEZONE udf should exist", data);
        data.setEudfdValue("40");
        acc.update(data);
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 weekStart, weekEnd).execute(false);
        // *** create the rule
        Rule rule = new WeeklyOvertimeExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_HOURSET_DESCRIPTION,
                                "REG=EMP_UDF~TIMEZONE*60|2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_DAY_WEEK_STARTS, "Monday");
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

        //assertOverrideAppliedCount(ovrBuilder , 2);

        RuleEngine.runCalcGroup(getConnection() , empId, weekStart, weekEnd, false) ;

        assertRuleApplied(empId, weekEnd, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , weekEnd);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 1);
        assertTrue(600 == wds.getMinutes(null, null, null, true, "OT2", true));
    }

    /**
     * @throws Exception
     */
    public void testEmpUnscheduled() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;
        Date weekStart = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        Date weekEnd = DateHelper.addDays(weekStart , 6);
        //Date start = DateHelper.addDays(weekStart , 4);
        // *** use TIMEZONE udf for testing
        final int empudfdefTimezone = 100;
        EmpUdfAccess acc = new EmpUdfAccess (getConnection());
        EmpUdfData data = acc.loadByEmpAndUdfDefId(empId, empudfdefTimezone);
        assertNotNull("TIMEZONE udf should exist", data);
        data.setEudfdValue("2400");
        acc.update(data);
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 weekStart, weekEnd).execute(false);
        // *** create the rule
        Rule rule = new WeeklyOvertimeExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_HOURSET_DESCRIPTION,
                                "REG=EMP_UDF~TIMEZONE|2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_DAY_WEEK_STARTS, "Monday");
        clearAndAddRule(empId , weekStart , rule , ruleparams);

        InsertEmployeeOverride ins2 = new InsertEmployeeOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(weekStart);      ins2.setEndDate(weekStart);
        ins2.setShftpatId(0);

        ovrBuilder.add(ins2);
        ovrBuilder.execute(true , false);
        EmployeeScheduleData esd = getEmployeeScheduleData(empId, weekStart);
        //System.out.println(esd);

        for (Date date = weekStart; date.compareTo(weekEnd ) <= 0;
             date = DateHelper.addDays(date, 1)) {

            InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
            ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
            ins.setEmpId(empId);
            ins.setStartDate(date);      ins.setEndDate(date);
            Datetime clk1On = DateHelper.addMinutes(date, 9*60);
            Datetime clk1Off = DateHelper.addMinutes(date, 17*60);
            String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
            ins.setWrksClocks(clks);

            ovrBuilder.add(ins);
        }

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        RuleEngine.runCalcGroup(getConnection() , empId, weekStart, weekEnd, false) ;

        assertRuleApplied(empId, weekEnd, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , weekEnd);
        //System.out.println(wds.toDescription());
        assertTrue(wds.size() == 1);
        assertTrue(480 == wds.getMinutes(null, null, null, true, "OT2", true));

    }

    /**
     * @throws Exception
     */
    public void testEmpApplySkdExt() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 11;
        Date weekStart = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        Date weekEnd = DateHelper.addDays(weekStart , 6);
        Date fri = DateHelper.addDays(weekStart , 4);
        Date weekStartDayafter = DateHelper.addDays(weekStart , 1);
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 weekStart, weekEnd).execute(false);

        // *** create the rule
        Rule rule = new WeeklyOvertimeExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_HOURSET_DESCRIPTION,
                                "REG=2400,OT1=240,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_ELIGIBLE_HOURTYPES, "REG,OT2,OT1");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_DAY_WEEK_STARTS, "Monday");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_APPLY_BASED_ON_SCHEDULE, WeeklyOvertimeExtRule.PARAM_VAL_APPLY_BASED_ON_SCHEDULE_EXT_ED);
        clearAndAddRule(empId , weekStart , rule , ruleparams);

        //for (Date date = weekStart; date.compareTo(weekStart ) <= 0;
        //     date = DateHelper.addDays(date, 1)) {

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekStart);      ins.setEndDate(weekStart);
        Datetime clk1On = DateHelper.addMinutes(weekStart, 7*60);
        Datetime clk1Off = DateHelper.addMinutes(weekStart, 19*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);

        ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekStartDayafter);      ins.setEndDate(weekStartDayafter);
        clk1On = DateHelper.addMinutes(weekStartDayafter, 7*60);
        clk1Off = DateHelper.addMinutes(weekStartDayafter, 19*60);
        clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);

        //}

        ovrBuilder.execute(true , false);


        assertOverrideAppliedCount(ovrBuilder , 2);

        //assertRuleApplied(empId, fri, rule);

        RuleEngine.runCalcGroup(getConnection() , empId, weekStart, weekStartDayafter, false);
        WorkDetailList wdlStart = getWorkDetailsForDate(empId , weekStart);
        //System.out.println(wdlStart.toDescription());
        assertTrue(240 == wdlStart.getMinutes(null, null, null, true, "OT1", true));
        WorkDetailList wdlStartAfter = getWorkDetailsForDate(empId , weekStartDayafter);
        //System.out.println(getWorkDetailsForDate(empId , weekStartDayafter).toDescription());
        assertTrue(240 == wdlStartAfter.getMinutes(null, null, null, true, "OT2", true));

    }

    /**
     * @throws Exception
     */
    public void testEmpApplySkdExt2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 11;
        Date weekStart = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        Date weekEnd = DateHelper.addDays(weekStart , 6);
        Date weekStartDayafter = DateHelper.addDays(weekStart , 1);
        Date fri = DateHelper.addDays(weekStart , 4);

        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 weekStart, weekEnd).execute(false);

        // *** create the rule
        Rule rule = new WeeklyOvertimeExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_HOURSET_DESCRIPTION,
                                "REG=2400,OT1=240,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_ELIGIBLE_HOURTYPES, "REG,OT2,OT1");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_DAY_WEEK_STARTS, "Monday");
        ruleparams.addParameter(WeeklyOvertimeExtRule.PARAM_APPLY_BASED_ON_SCHEDULE, WeeklyOvertimeExtRule.PARAM_VAL_APPLY_BASED_ON_SCHEDULE_EXT_ED);
        clearAndAddRule(empId , weekStart , rule , ruleparams);

        //for (Date date = weekStart; date.compareTo(weekStart ) <= 0;
        //     date = DateHelper.addDays(date, 1)) {

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekStart);      ins.setEndDate(weekStart);
        Datetime clk1On = DateHelper.addMinutes(weekStart, 7*60);
        Datetime clk1Off = DateHelper.addMinutes(weekStart, 19*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);

        ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(weekStartDayafter);      ins.setEndDate(weekStartDayafter);
        clk1On = DateHelper.addMinutes(weekStartDayafter, 7*60);
        clk1Off = DateHelper.addMinutes(weekStartDayafter, 19*60);
        clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);


        InsertWorkDetailOverride insD = new InsertWorkDetailOverride(getConnection());
        insD.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD.setEmpId(empId);
        insD.setStartDate(weekStartDayafter);      insD.setEndDate(weekStartDayafter);
        insD.setStartTime(DateHelper.addMinutes(weekStartDayafter, 15*60));
        insD.setEndTime(DateHelper.addMinutes(weekStartDayafter, 17*60));
        insD.setWrkdTcodeName("TRN");

        ovrBuilder.add(insD);

        //}

        ovrBuilder.execute(true , false);


        assertOverrideAppliedCount(ovrBuilder , 3);

        assertRuleApplied(empId, weekStart, rule);

        //WorkDetailList wds = getWorkDetailsForDate(empId , fri);
        //System.out.println(wds.toDescription());
        RuleEngine.runCalcGroup(getConnection() , empId, weekStart, weekStartDayafter, false);
        WorkDetailList wdlStart = getWorkDetailsForDate(empId , weekStart);
        //System.out.println(wdlStart.toDescription());
        assertTrue(120 == wdlStart.getMinutes(null, null, null, true, "OT1", true));
        WorkDetailList wdlStartAfter = getWorkDetailsForDate(empId , weekStartDayafter);
        //System.out.println(getWorkDetailsForDate(empId , weekStartDayafter).toDescription());
        assertTrue(120 == wdlStartAfter.getMinutes(null, null, null, true, "OT1", true));
        assertTrue(120 == wdlStartAfter.getMinutes(null, null, null, true, "OT2", true));

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
