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
public class WeeklyOvertimeBorrowRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimeBorrowRuleTest.class);

    public WeeklyOvertimeBorrowRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WeeklyOvertimeBorrowRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void test2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date week_1 = DateHelper.nextDay(DateHelper.getCurrentDate() , "Sat");
        Date week_2 = DateHelper.addDays(week_1 , 1);
        Date week_3 = DateHelper.addDays(week_1 , 2);
        Date week_4 = DateHelper.addDays(week_1 , 3);
        Date week_5 = DateHelper.addDays(week_1 , 4);
        Date week_6 = DateHelper.addDays(week_1 , 5);
        Date week_7 = DateHelper.addDays(week_1 , 6);

        // *** create the rule
        Rule rule = new WeeklyOvertimeBorrowRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_HOURSET_DESCRIPTION,
                                "REG=2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_ELIGIBLE_HOURTYPES, "REG,OT2");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_DAY_WEEK_STARTS, "Sat");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_WORK_DETAIL_BORROW_FIELD, "Udf1");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_EMP_BORROW_FIELD, "empVal1");
        clearAndAddRule(empId , week_1 , rule , ruleparams);

        InsertEmployeeOverride insE = new InsertEmployeeOverride(getConnection());
        insE.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insE.setEmpId(empId);
        insE.setStartDate(DateHelper.DATE_1900);      insE.setEndDate(DateHelper.DATE_3000);
        insE.setShftpatId(0);
        insE.setEmpVal1("HOME");
        ovrBuilder.add(insE);

        InsertWorkDetailOverride insD1 = new InsertWorkDetailOverride(getConnection());
        insD1.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD1.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD1.setEmpId(empId);
        insD1.setStartDate(week_1);      insD1.setEndDate(week_1);
        insD1.setStartTime(DateHelper.addMinutes(week_1, 10*60));
        insD1.setEndTime(DateHelper.addMinutes(week_1, 20*60));
        insD1.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD1);

        InsertWorkDetailOverride insD2 = new InsertWorkDetailOverride(getConnection());
        insD2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD2.setEmpId(empId);
        insD2.setStartDate(week_2);      insD2.setEndDate(week_2);
        insD2.setStartTime(DateHelper.addMinutes(week_2, 15*60));
        insD2.setEndTime(DateHelper.addMinutes(week_2, 19*60));
        insD2.setWrkdTcodeName("WRK");
        insD2.setWrkdUdf1("ELECTRICAL");
        ovrBuilder.add(insD2);

        InsertWorkDetailOverride insD3 = new InsertWorkDetailOverride(getConnection());
        insD3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD3.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD3.setEmpId(empId);
        insD3.setStartDate(week_3);      insD3.setEndDate(week_3);
        insD3.setStartTime(DateHelper.addMinutes(week_3, 10*60));
        insD3.setEndTime(DateHelper.addMinutes(week_3, 20*60));
        insD3.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD3);

        InsertWorkDetailOverride insD4 = new InsertWorkDetailOverride(getConnection());
        insD4.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD4.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD4.setEmpId(empId);
        insD4.setStartDate(week_4);      insD4.setEndDate(week_4);
        insD4.setStartTime(DateHelper.addMinutes(week_4, 10*60));
        insD4.setEndTime(DateHelper.addMinutes(week_4, 20*60));
        insD4.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD4);

        InsertWorkDetailOverride insD5 = new InsertWorkDetailOverride(getConnection());
        insD5.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD5.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD5.setEmpId(empId);
        insD5.setStartDate(week_5);      insD5.setEndDate(week_5);
        insD5.setStartTime(DateHelper.addMinutes(week_5, 10*60));
        insD5.setEndTime(DateHelper.addMinutes(week_5, 20*60));
        insD5.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD5);

        InsertWorkDetailOverride insD6 = new InsertWorkDetailOverride(getConnection());
        insD6.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD6.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD6.setEmpId(empId);
        insD6.setStartDate(week_6);      insD6.setEndDate(week_6);
        insD6.setStartTime(DateHelper.addMinutes(week_6, 10*60));
        insD6.setEndTime(DateHelper.addMinutes(week_6, 14*60));
        insD6.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD6);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, week_1, rule);

        // mimic recalc engine
        RuleEngine.runCalcGroup(getConnection() , empId, week_1, week_7, false);


        WorkDetailList wds = getWorkDetailsForDate(empId , week_2);
        //System.out.println(wds.toDescription());
        assertTrue(240 == wds.getMinutes(null, null, null, true, "OT2", true));
        wds = getWorkDetailsForDate(empId , week_6);
        //System.out.println(wds.toDescription());
        assertTrue(240 == wds.getMinutes(null, null, null, true, "OT2", true));

    }

    /**
     * @throws Exception
     */
    public void test3() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date week_1 = DateHelper.nextDay(DateHelper.getCurrentDate() , "Sat");
        Date week_2 = DateHelper.addDays(week_1 , 1);
        Date week_3 = DateHelper.addDays(week_1 , 2);
        Date week_4 = DateHelper.addDays(week_1 , 3);
        Date week_5 = DateHelper.addDays(week_1 , 4);
        Date week_6 = DateHelper.addDays(week_1 , 5);
        Date week_7 = DateHelper.addDays(week_1 , 6);

        // *** create the rule
        Rule rule = new WeeklyOvertimeBorrowRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_HOURSET_DESCRIPTION,
                                "REG=2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_ELIGIBLE_HOURTYPES, "REG,OT2");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_DAY_WEEK_STARTS, "Sat");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_WORK_DETAIL_BORROW_FIELD, "Udf1");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_EMP_BORROW_FIELD, "empVal1");
        clearAndAddRule(empId , week_1 , rule , ruleparams);

        InsertEmployeeOverride insE = new InsertEmployeeOverride(getConnection());
        insE.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insE.setEmpId(empId);
        insE.setStartDate(DateHelper.DATE_1900);      insE.setEndDate(DateHelper.DATE_3000);
        insE.setShftpatId(0);
        insE.setEmpVal1("HOME");
        ovrBuilder.add(insE);

        InsertWorkDetailOverride insD1 = new InsertWorkDetailOverride(getConnection());
        insD1.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD1.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD1.setEmpId(empId);
        insD1.setStartDate(week_1);      insD1.setEndDate(week_1);
        insD1.setStartTime(DateHelper.addMinutes(week_1, 10*60));
        insD1.setEndTime(DateHelper.addMinutes(week_1, 14*60));
        insD1.setWrkdTcodeName("WRK");
        insD1.setWrkdUdf1("HARDWARE");
        ovrBuilder.add(insD1);

        InsertWorkDetailOverride insD2 = new InsertWorkDetailOverride(getConnection());
        insD2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD2.setEmpId(empId);
        insD2.setStartDate(week_2);      insD2.setEndDate(week_2);
        insD2.setStartTime(DateHelper.addMinutes(week_2, 15*60));
        insD2.setEndTime(DateHelper.addMinutes(week_2, 19*60));
        insD2.setWrkdTcodeName("WRK");
        insD2.setWrkdUdf1("ELECTRICAL");
        ovrBuilder.add(insD2);

        InsertWorkDetailOverride insD3 = new InsertWorkDetailOverride(getConnection());
        insD3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD3.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD3.setEmpId(empId);
        insD3.setStartDate(week_3);      insD3.setEndDate(week_3);
        insD3.setStartTime(DateHelper.addMinutes(week_3, 10*60));
        insD3.setEndTime(DateHelper.addMinutes(week_3, 20*60));
        insD3.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD3);

        InsertWorkDetailOverride insD4 = new InsertWorkDetailOverride(getConnection());
        insD4.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD4.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD4.setEmpId(empId);
        insD4.setStartDate(week_4);      insD4.setEndDate(week_4);
        insD4.setStartTime(DateHelper.addMinutes(week_4, 10*60));
        insD4.setEndTime(DateHelper.addMinutes(week_4, 18*60));
        insD4.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD4);

        InsertWorkDetailOverride insD5 = new InsertWorkDetailOverride(getConnection());
        insD5.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD5.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD5.setEmpId(empId);
        insD5.setStartDate(week_5);      insD5.setEndDate(week_5);
        insD5.setStartTime(DateHelper.addMinutes(week_5, 10*60));
        insD5.setEndTime(DateHelper.addMinutes(week_5, 18*60));
        insD5.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD5);

        InsertWorkDetailOverride insD6 = new InsertWorkDetailOverride(getConnection());
        insD6.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD6.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD6.setEmpId(empId);
        insD6.setStartDate(week_6);      insD6.setEndDate(week_6);
        insD6.setStartTime(DateHelper.addMinutes(week_6, 10*60));
        insD6.setEndTime(DateHelper.addMinutes(week_6, 18*60));
        insD6.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD6);

        InsertWorkDetailOverride insD7 = new InsertWorkDetailOverride(getConnection());
        insD7.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD7.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD7.setEmpId(empId);
        insD7.setStartDate(week_7);      insD7.setEndDate(week_7);
        insD7.setStartTime(DateHelper.addMinutes(week_7, 10*60));
        insD7.setEndTime(DateHelper.addMinutes(week_7, 18*60));
        insD7.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD7);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, week_1, rule);

        // mimic recalc engine
        RuleEngine.runCalcGroup(getConnection() , empId, week_1, week_7, false);


        WorkDetailList wds = getWorkDetailsForDate(empId , week_1);
        //System.out.println(wds.toDescription());
        assertTrue(240 == wds.getMinutes(null, null, null, true, "OT2", true));
        wds = getWorkDetailsForDate(empId , week_2);
        //System.out.println(wds.toDescription());
        assertTrue(240 == wds.getMinutes(null, null, null, true, "OT2", true));
        wds = getWorkDetailsForDate(empId , week_7);
        assertTrue(120 == wds.getMinutes(null, null, null, true, "OT2", true));
    }


    /**
     * @throws Exception
     */
    public void test4() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
        ovrBuilder.setRuleEngineAutoRecalc(true);

        final int empId = 11;
        Date week_1 = DateHelper.nextDay(DateHelper.getCurrentDate() , "Sat");
        Date week_2 = DateHelper.addDays(week_1 , 1);
        Date week_3 = DateHelper.addDays(week_1 , 2);
        Date week_4 = DateHelper.addDays(week_1 , 3);
        Date week_5 = DateHelper.addDays(week_1 , 4);
        Date week_6 = DateHelper.addDays(week_1 , 5);
        Date week_7 = DateHelper.addDays(week_1 , 6);

        // *** create the rule
        Rule rule = new WeeklyOvertimeBorrowRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_HOURSET_DESCRIPTION,
                                "REG=2400,OT2=99999");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_ELIGIBLE_HOURTYPES, "REG,OT2");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_DAY_WEEK_STARTS, "Sat");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_WORK_DETAIL_BORROW_FIELD, "Udf1");
        ruleparams.addParameter(WeeklyOvertimeBorrowRule.PARAM_EMP_BORROW_FIELD, "empVal1");
        clearAndAddRule(empId , week_1 , rule , ruleparams);

        InsertEmployeeOverride insE = new InsertEmployeeOverride(getConnection());
        insE.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insE.setEmpId(empId);
        insE.setStartDate(DateHelper.DATE_1900);      insE.setEndDate(DateHelper.DATE_3000);
        insE.setShftpatId(0);
        insE.setEmpVal1("HOME");
        ovrBuilder.add(insE);

        InsertWorkDetailOverride insD1 = new InsertWorkDetailOverride(getConnection());
        insD1.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD1.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD1.setEmpId(empId);
        insD1.setStartDate(week_1);      insD1.setEndDate(week_1);
        insD1.setStartTime(DateHelper.addMinutes(week_1, 10*60));
        insD1.setEndTime(DateHelper.addMinutes(week_1, 18*60));
        insD1.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD1);

        InsertWorkDetailOverride insD2 = new InsertWorkDetailOverride(getConnection());
        insD2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD2.setEmpId(empId);
        insD2.setStartDate(week_2);      insD2.setEndDate(week_2);
        insD2.setStartTime(DateHelper.addMinutes(week_2, 10*60));
        insD2.setEndTime(DateHelper.addMinutes(week_2, 18*60));
        insD2.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD2);

        InsertWorkDetailOverride insD3 = new InsertWorkDetailOverride(getConnection());
        insD3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD3.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD3.setEmpId(empId);
        insD3.setStartDate(week_3);      insD3.setEndDate(week_3);
        insD3.setStartTime(DateHelper.addMinutes(week_3, 10*60));
        insD3.setEndTime(DateHelper.addMinutes(week_3, 18*60));
        insD3.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD3);

        InsertWorkDetailOverride insD4 = new InsertWorkDetailOverride(getConnection());
        insD4.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD4.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD4.setEmpId(empId);
        insD4.setStartDate(week_4);      insD4.setEndDate(week_4);
        insD4.setStartTime(DateHelper.addMinutes(week_4, 10*60));
        insD4.setEndTime(DateHelper.addMinutes(week_4, 18*60));
        insD4.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD4);

        InsertWorkDetailOverride insD5 = new InsertWorkDetailOverride(getConnection());
        insD5.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD5.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD5.setEmpId(empId);
        insD5.setStartDate(week_5);      insD5.setEndDate(week_5);
        insD5.setStartTime(DateHelper.addMinutes(week_5, 10*60));
        insD5.setEndTime(DateHelper.addMinutes(week_5, 18*60));
        insD5.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD5);

        InsertWorkDetailOverride insD6 = new InsertWorkDetailOverride(getConnection());
        insD6.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD6.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD6.setEmpId(empId);
        insD6.setStartDate(week_6);      insD6.setEndDate(week_6);
        insD6.setStartTime(DateHelper.addMinutes(week_6, 10*60));
        insD6.setEndTime(DateHelper.addMinutes(week_6, 18*60));
        insD6.setWrkdTcodeName("WRK");
        ovrBuilder.add(insD6);

        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertRuleApplied(empId, week_1, rule);

        // mimic recalc engine
        //RuleEngine.runCalcGroup(getConnection() , empId, week_1, week_1, false);


        insD1 = new InsertWorkDetailOverride(getConnection());
        insD1.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insD1.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insD1.setEmpId(empId);
        insD1.setStartDate(week_1);      insD1.setEndDate(week_1);
        insD1.setStartTime(DateHelper.addMinutes(week_1, 10*60));
        insD1.setEndTime(DateHelper.addMinutes(week_1, 18*60));
        insD1.setWrkdTcodeName("WRK");
        insD1.setWrkdUdf1("ELECTRICAL");
        ovrBuilder.add(insD1);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        WorkDetailList wds = getWorkDetailsForDate(empId , week_1);
        assertTrue(480 == wds.getMinutes(null, null, null, true, "OT2", true));
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
