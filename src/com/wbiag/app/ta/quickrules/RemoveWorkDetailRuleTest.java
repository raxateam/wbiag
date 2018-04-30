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
 */
public class RemoveWorkDetailRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RemoveWorkDetailRuleTest.class);

    public RemoveWorkDetailRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(RemoveWorkDetailRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testRemove() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        // *** create the rule
        Rule rule = new RemoveWorkDetailRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(RemoveWorkDetailRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(RemoveWorkDetailRule.PARAM_WORK_DETAIL_FILTER, RemoveWorkDetailRule.PARAM_VAL_OUTSIDE_SCHEDULE);
        clearAndAddRule(empId , start , rule , ruleparams, ExecutionPointHelper.EXECPOINT_AUTHORIZATION);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 540);
        Datetime clk1Off = DateHelper.addMinutes(start, 1440);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        assertTrue(wds.size() == 1);
        WorkDetailData wd1 = wds.getWorkDetail(0);
        assertEquals(getEmployeeScheduleData(empId , start).getEmpskdActStartTime() , wd1.getWrkdStartTime() ) ;
        assertEquals(getEmployeeScheduleData(empId , start).getEmpskdActEndTime() , wd1.getWrkdEndTime());
    }

    /**
     * @throws Exception
     */
    public void xRemoveTwoShifts() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 12;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        // *** create the rule
        Rule rule = new RemoveWorkDetailRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(RemoveWorkDetailRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(RemoveWorkDetailRule.PARAM_WORK_DETAIL_FILTER, RemoveWorkDetailRule.PARAM_VAL_OUTSIDE_SCHEDULE);
        clearAndAddRule(empId , start , rule , ruleparams, ExecutionPointHelper.EXECPOINT_AUTHORIZATION);

        InsertEmployeeScheduleOverride insS = new InsertEmployeeScheduleOverride(getConnection());
        insS.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        insS.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insS.setEmpId(empId);
        insS.setStartDate(start);      insS.setEndDate(start);
        Datetime sh1Start = DateHelper.addMinutes(start, 9*60);
        Datetime sh1End = DateHelper.addMinutes(start, 17*60);
        insS.setEmpskdActStartTime(sh1Start);
        insS.setEmpskdActEndTime(sh1End);
        Datetime sh2Start = DateHelper.addMinutes(start, 20*60);
        Datetime sh2End = DateHelper.addMinutes(start, 21*60);
        insS.setEmpskdActStartTime2(sh2Start);
        insS.setEmpskdActEndTime2(sh2End);
        ovrBuilder.add(insS);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 21*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        System.out.println(wds.toDescription());
        assertTrue(wds.size() == 2);
        WorkDetailData wd11 = wds.getWorkDetail(0);
        assertEquals(getEmployeeScheduleData(empId , start).getEmpskdActStartTime() , wd11.getWrkdStartTime() ) ;
        assertEquals(getEmployeeScheduleData(empId , start).getEmpskdActEndTime() , wd11.getWrkdEndTime());
        WorkDetailData wd12 = wds.getWorkDetail(1);
        assertEquals(getEmployeeScheduleData(empId , start).getEmpskdActStartTime2() , wd12.getWrkdStartTime() ) ;
        assertEquals(getEmployeeScheduleData(empId , start).getEmpskdActEndTime2() , wd12.getWrkdEndTime());

    }

    /**
     * @throws Exception
     */
    public void testRemoveAll() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        // *** create the rule
        Rule rule = new RemoveWorkDetailRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(RemoveWorkDetailRule.PARAM_TIME_CODES, "");
        ruleparams.addParameter(RemoveWorkDetailRule.PARAM_WORK_DETAIL_FILTER, RemoveWorkDetailRule.PARAM_VAL_ALL);
        clearAndAddRule(empId , start , rule , ruleparams, ExecutionPointHelper.EXECPOINT_AUTHORIZATION);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 540);
        Datetime clk1Off = DateHelper.addMinutes(start, 1440);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        assertTrue(wds.size() == 0);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
