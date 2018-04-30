package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;
/**
 * Test for GuaranteeExtendedRule.
 */
public class GuaranteeExtendedRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GuaranteeExtendedRuleTest.class);

    public GuaranteeExtendedRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(GuaranteeExtendedRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testGuaranteeShftPct() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "WRK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteeExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_MINUTES, "-80");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_DISCOUNT_MINUTES, "30");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_INDEPENDENT_OCCURRENCES, "false");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_TIMECODELIST, checkCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setStartTime(DateHelper.addMinutes(start , 12*60));
        ins.setEndTime(DateHelper.addMinutes(start , 12*60 + 30));
        ins.setWrkdTcodeName("BRK");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride insCl1 = new InsertWorkSummaryOverride(getConnection());
        insCl1.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insCl1.setEmpId(empId);
        insCl1.setStartDate(start);      insCl1.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9*60);
        Datetime clk1Off= DateHelper.addMinutes(start, 14*60);
        insCl1.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1On , clk1Off));
        ovrBuilder.add(insCl1);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(wps.toDescription());
        assertTrue(wps.size() == 1);
        // *** premiums
        assertWorkPremiumTimeCodeMinutes(empId , start, guarCode , 90);
    }

    /**
     * @throws Exception
     */
    public void testGuaranteeEmpVal() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "WRK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteeExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_MINUTES, "EMP_VAL1");
        //ruleparams.addParameter(GuaranteeExtendedRule.PARAM_DISCOUNT_MINUTES, "30");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_INDEPENDENT_OCCURRENCES, "false");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_TIMECODELIST, checkCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeOverride insE = new InsertEmployeeOverride(getConnection());
        insE.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insE.setEmpId(empId);
        insE.setStartDate(start);      insE.setEndDate(start);
        insE.setEmpVal1("420");
        ovrBuilder.add(insE);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setStartTime(DateHelper.addMinutes(start , 12*60));
        ins.setEndTime(DateHelper.addMinutes(start , 12*60 + 30));
        ins.setWrkdTcodeName("BRK");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride insCl1 = new InsertWorkSummaryOverride(getConnection());
        insCl1.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insCl1.setEmpId(empId);
        insCl1.setStartDate(start);      insCl1.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9*60);
        Datetime clk1Off= DateHelper.addMinutes(start, 14*60);
        insCl1.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1On , clk1Off));
        ovrBuilder.add(insCl1);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(wps.toDescription());
        assertTrue(wps.size() == 1);
        // *** premiums
        assertWorkPremiumTimeCodeMinutes(empId , start, guarCode , 150);
    }


    /**
     * @throws Exception
     */
    public void testGuaranteeExt() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String guarCode = "GUAR" , checkCode = "WRK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "SUN");
        // *** create the rule
        Rule rule = new GuaranteeExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_MINUTES, "240");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_INDEPENDENT_OCCURRENCES, GuaranteeExtendedRule.PARAM_VAL_INDEPENDENT_OCCURRENCES_EXTENDED);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_TIMECODELIST, checkCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride ins0 = new InsertEmployeeScheduleOverride(getConnection());
        ins0.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins0.setEmpId(empId);
        ins0.setStartDate(start);      ins0.setEndDate(start);
        ins0.setEmpskdActShiftName("OFF");
        ins0.setOvrType(OverrideData.SCHEDULE_SHIFT_TYPE);
        ovrBuilder.add(ins0);

        InsertWorkSummaryOverride ins00 = new InsertWorkSummaryOverride(getConnection());
        ins00.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins00.setEmpId(empId);
        ins00.setStartDate(start);      ins00.setEndDate(start);
        ins00.setWrksClocks("");
        ovrBuilder.add(ins00);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start , 9*60));
        ins2.setEndTime(DateHelper.addMinutes(start , 11*60));
        ins2.setWrkdJobName("JANITOR");
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins2);

        InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride(getConnection());
        ins3.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins3.setEmpId(empId);
        ins3.setStartDate(start);      ins3.setEndDate(start);
        ins3.setStartTime(DateHelper.addMinutes(start , 11*60));
        ins3.setEndTime(DateHelper.addMinutes(start , 12*60));
        ins3.setWrkdJobName("MANAGER");
        ins3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins3);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setStartTime(DateHelper.addMinutes(start , 12*60));
        ins.setEndTime(DateHelper.addMinutes(start , 12*60 + 30));
        ins.setWrkdTcodeName("BRK");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins4 = new InsertWorkDetailOverride(getConnection());
        ins4.setWbuNameBoth("JUNIT", "JUNIT");
        ins4.setEmpId(empId);
        ins4.setStartDate(start);      ins4.setEndDate(start);
        ins4.setStartTime(DateHelper.addMinutes(start , 12*60 + 30));
        ins4.setEndTime(DateHelper.addMinutes(start , 13*60 + 30));
        ins4.setWrkdJobName("MANAGER");
        ins4.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins4);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 6);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        assertTrue(wps.size() == 2);
        // *** premiums
        assertEquals(wps.getWorkDetail(0).getWrkdMinutes() , 60);
        assertEquals(wps.getWorkDetail(1).getWrkdMinutes() , 180);
    }

    /**
     * @throws Exception
     */
    public void testGuaranteeExtCont() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "WRK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "SUN");
        // *** create the rule
        Rule rule = new GuaranteeExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_MINUTES, "360");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_INDEPENDENT_OCCURRENCES, GuaranteeExtendedRule.PARAM_VAL_INDEPENDENT_OCCURRENCES_EXTENDED);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_INDEPENDENT_CONTINUE_TIMECODES, "BRK");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_TIMECODELIST, checkCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeScheduleOverride ins0 = new InsertEmployeeScheduleOverride(getConnection());
        ins0.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins0.setEmpId(empId);
        ins0.setStartDate(start);      ins0.setEndDate(start);
        ins0.setEmpskdActShiftName("OFF");
        ins0.setOvrType(OverrideData.SCHEDULE_SHIFT_TYPE);
        ovrBuilder.add(ins0);

        InsertWorkSummaryOverride ins00 = new InsertWorkSummaryOverride(getConnection());
        ins00.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins00.setEmpId(empId);
        ins00.setStartDate(start);      ins00.setEndDate(start);
        ins00.setWrksClocks("");
        ovrBuilder.add(ins00);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start , 9*60));
        ins2.setEndTime(DateHelper.addMinutes(start , 11*60));
        ins2.setWrkdJobName("JANITOR");
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins2);

        InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride(getConnection());
        ins3.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins3.setEmpId(empId);
        ins3.setStartDate(start);      ins3.setEndDate(start);
        ins3.setStartTime(DateHelper.addMinutes(start , 11*60));
        ins3.setEndTime(DateHelper.addMinutes(start , 12*60));
        ins3.setWrkdJobName("MANAGER");
        ins3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins3);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setStartTime(DateHelper.addMinutes(start , 12*60));
        ins.setEndTime(DateHelper.addMinutes(start , 12*60 + 30));
        ins.setWrkdTcodeName("BRK");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins4 = new InsertWorkDetailOverride(getConnection());
        ins4.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins4.setEmpId(empId);
        ins4.setStartDate(start);      ins4.setEndDate(start);
        ins4.setStartTime(DateHelper.addMinutes(start , 12*60 + 30));
        ins4.setEndTime(DateHelper.addMinutes(start , 13*60 + 30));
        ins4.setWrkdJobName("MANAGER");
        ins4.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins4);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 6);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(wps.toDescription());
        // *** premiums
        assertWorkPremiumTimeCodeMinutes(empId , start, guarCode , 120);
    }

    /**
     * @throws Exception
     */
    public void testGuaranteePrem() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "TRN";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteeExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_MINUTES, "240");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_INDEPENDENT_OCCURRENCES, "false");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_TIMECODELIST, checkCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_PREMIUM_DETAIL, WorkDetailData.PREMIUM_TYPE);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkPremiumOverride ins = new InsertWorkPremiumOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setWrkdMinutes(60);
        ins.setWrkdTcodeName(checkCode);
        ins.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 1);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(wps.toDescription());
        //assertTrue(wps.size() == 1);
        // *** premiums
        assertEquals(180 , wps.getMinutes(null, null, guarCode , true, null, true) );
    }

    public void testDistributePremium()
    	throws Exception
    {
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final String deptName1 = "TEST DEPT1";
        final String deptName2 = "TEST DEPT2";
        final int empId = 15;
        final String guarCode = "GUAR" , checkCode = "TRN";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        //create test department
        DepartmentAccess deptAccess = new DepartmentAccess(getConnection());
        DepartmentData deptData = new DepartmentData();
        deptData.setDeptName(deptName1);
        deptData.setDeptVal1("22.13");
        deptData.setDeptStartDate(start);
        deptData.setDeptEndDate(start);
        deptData.setLmsId(1);
        deptData.setDeptUnauth("N");
        deptAccess.insert(deptData);

        deptData = new DepartmentData();
        deptData.setDeptName(deptName2);
        deptData.setDeptVal1("22.13");
        deptData.setDeptStartDate(start);
        deptData.setDeptEndDate(start);
        deptData.setLmsId(1);
        deptData.setDeptUnauth("N");
        deptAccess.insert(deptData);

        //find test deptment id
        int deptId1 = getCodeMapper().getDepartmentByName(deptName1).getDeptId();
        int deptId2 = getCodeMapper().getDepartmentByName(deptName2).getDeptId();

        // *** create the rule
        Rule rule = new GuaranteeExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_MINUTES, String.valueOf(240));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_COPY_DETAIL_FIELD, "dept_id");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_TIMECODELIST, checkCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_DISTRIBUTE_PREMIUM, "true");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setStartTime(DateHelper.addMinutes(start , 360));
        ins.setEndTime(DateHelper.addMinutes(start , 420));
        ins.setWrkdTcodeName(checkCode);
        ins.setDeptId(deptId1);
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins1 = new InsertWorkDetailOverride(getConnection());
        ins1.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins1.setEmpId(empId);
        ins1.setStartDate(start);
        ins1.setEndDate(start);
        ins1.setStartTime(DateHelper.addMinutes(start , 420));
        ins1.setEndTime(DateHelper.addMinutes(start , 480));
        ins1.setWrkdTcodeName(checkCode);
        ins1.setDeptId(deptId2);
        ins1.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins1);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        // assert that premiums is distributed correctly
        WorkDetailList wdl = (WorkDetailList)getWorkPremiumsForDate(empId , start);

        WorkDetailData wdd = (WorkDetailData)wdl.get(0);
        assertTrue(deptId1 == wdd.getDeptId());
        assertTrue(180 == wdd.getWrkdMinutes());

        wdd = (WorkDetailData)wdl.get(1);
        assertTrue(deptId2 == wdd.getDeptId());
        assertTrue(180 == wdd.getWrkdMinutes());
    }

    /**
     * @throws Exception
     */
    public void testGuaranteeEmpUdf() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String guarCode = "GUAR" , checkCode = "WRK";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteeExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_MINUTES, "EMP_UDF~TIMEZONE*60|480");
        //ruleparams.addParameter(GuaranteeExtendedRule.PARAM_DISCOUNT_MINUTES, "30");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_INDEPENDENT_OCCURRENCES, "false");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_TIMECODELIST, checkCode);

        clearAndAddRule(empId , start , rule , ruleparams);

        // *** use TIMEZONE udf for testing
        final int empudfdefTimezone = 100;
        EmpUdfAccess acc = new EmpUdfAccess (getConnection());
        EmpUdfData data = acc.loadByEmpAndUdfDefId(empId, empudfdefTimezone);
        assertNotNull("TIMEZONE udf should exist", data);
        data.setEudfdValue("7");
        acc.update(data);
        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setStartTime(DateHelper.addMinutes(start , 12*60));
        ins.setEndTime(DateHelper.addMinutes(start , 12*60 + 30));
        ins.setWrkdTcodeName("BRK");
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride insCl1 = new InsertWorkSummaryOverride(getConnection());
        insCl1.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insCl1.setEmpId(empId);
        insCl1.setStartDate(start);      insCl1.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9*60);
        Datetime clk1Off= DateHelper.addMinutes(start, 14*60);
        insCl1.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1On , clk1Off));
        ovrBuilder.add(insCl1);

        ovrBuilder.execute(true , false);

        //assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        //System.out.println(wps.toDescription());
        assertTrue(wps.size() == 1);
        // *** premiums
        assertWorkPremiumTimeCodeMinutes(empId , start, guarCode , 150);
    }

    /**
     * @throws Exception
     */
    public void testGuaranteePremDet() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String guarCode = "GUAR" , checkCode = "TRN";
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create the rule
        Rule rule = new GuaranteeExtendedRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_MINUTES, "240");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_GUARANTEED_PREMIUM_TIMECODE, guarCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MAXIMUM_MINUTES_TO_QUALIFY, String.valueOf(480));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_MINIMUM_MINUTES_TO_QUALIFY, String.valueOf(60));
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_INDEPENDENT_OCCURRENCES, "false");
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_TIMECODELIST, checkCode);
        ruleparams.addParameter(GuaranteeExtendedRule.PARAM_PREMIUM_DETAIL, GuaranteeExtendedRule.PARAM_VAL_BOTH);

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkPremiumOverride ins = new InsertWorkPremiumOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setWrkdMinutes(60);
        ins.setWrkdTcodeName(checkCode);
        ins.setOvrType(OverrideData.PRECALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start , 12*60));
        ins2.setEndTime(DateHelper.addMinutes(start , 12*60 + 30));
        ins2.setWrkdTcodeName(checkCode);
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertOverrideAppliedCount(ovrBuilder , 2);

        assertRuleApplied(empId, start, rule);
        WorkDetailList wps = getWorkPremiumsForDate(empId , start);
        System.out.println(wps.toDescription());
        //assertTrue(wps.size() == 1);
        // *** premiums
        assertEquals(150 , wps.getMinutes(null, null, guarCode , true, null, true) );
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

