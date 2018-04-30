package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.rules.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for DailyOvertime24HourSkdRollRuleTest.
 */
public class DailyOvertime24HourSkdRollRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DailyOvertime24HourSkdRollRuleTest.class);

    public DailyOvertime24HourSkdRollRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(DailyOvertime24HourSkdRollRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void test24HourSkd() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);

        // *** create the rule
        Rule rule = new DailyOvertime24HourSkdRollRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT1=240,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_P24_HOUR_STARTTIME, DailyOvertime24HourSkdRollRule.PARAM_VAL_24_HOUR_SCHEDULE);
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_ELIGIBLE_HOURTYPES, "REG,OT1,OT2");
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_WORKDETAIL_TIMECODES, "WRK");

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeScheduleOverride ins0 = new InsertEmployeeScheduleOverride(getConnection());
        ins0.setWbuNameBoth("JUNIT", "JUNIT");;
        ins0.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins0.setEmpId(empId);
        ins0.setStartDate(mon);      ins0.setEndDate(mon);
        Datetime sh0Start = DateHelper.addMinutes(mon, 7*60);
        Datetime sh0End = DateHelper.addMinutes(mon, 15*60);
        ins0.setEmpskdActStartTime(sh0Start);
        ins0.setEmpskdActEndTime(sh0End);
        ovrBuilder.add(ins0);
        ovrBuilder.execute(true , false);
        InsertEmployeeScheduleOverride ins1 = new InsertEmployeeScheduleOverride(getConnection());
        ins1.setWbuNameBoth("JUNIT", "JUNIT");;
        ins1.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins1.setEmpId(empId);
        ins1.setStartDate(tue);      ins1.setEndDate(tue);
        Datetime sh1Start = DateHelper.addMinutes(tue, 7*60);
        Datetime sh1End = DateHelper.addMinutes(tue, 15*60);
        ins1.setEmpskdActStartTime(sh1Start);
        ins1.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins1);
        ovrBuilder.execute(true , false);         ovrBuilder.clear();

        InsertWorkSummaryOverride insCl1 = new InsertWorkSummaryOverride(getConnection());
        insCl1.setWbuNameBoth("JUNIT", "JUNIT");;
        insCl1.setEmpId(empId);
        insCl1.setStartDate(mon);      insCl1.setEndDate(mon);
        Datetime clk1On = DateHelper.addMinutes(mon, 7*60);
        Datetime clk1Off= DateHelper.addMinutes(mon, 16*60);
        insCl1.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1On , clk1Off));
        ovrBuilder.add(insCl1);

        InsertWorkSummaryOverride insCl2 = new InsertWorkSummaryOverride(getConnection());
        insCl2.setWbuNameBoth("JUNIT", "JUNIT");;
        insCl2.setEmpId(empId);
        insCl2.setStartDate(tue);      insCl2.setEndDate(tue);
        Datetime clk2On = DateHelper.addMinutes(tue, 3*60);
        Datetime clk2Off= DateHelper.addMinutes(tue, 15*60);
        insCl2.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk2On , clk2Off));
        ovrBuilder.add(insCl2);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, mon, rule);


        //WorkDetailList wd1 = getWorkDetailsForDate(empId , mon);
        //System.out.println(wd1.toDescription());
        WorkDetailList wd2 = getWorkDetailsForDate(empId , tue);
        System.out.println(wd2.toDescription());
        int ind1 = wd2.getFirstRecordIndex(DateHelper.addMinutes(tue, 6*60) ,
            DateHelper.addMinutes(tue, 7*60) , false);
        assertFalse(ind1 == -1);
        assertEquals("OT2" , wd2.getWorkDetail(ind1).getWrkdHtypeName());
        ind1 = wd2.getFirstRecordIndex(DateHelper.addMinutes(tue, 3*60) ,
                    DateHelper.addMinutes(tue, 6*60) , false);
        assertFalse(ind1 == -1);
        assertEquals("OT1" , wd2.getWorkDetail(ind1).getWrkdHtypeName());
    }

    /**
     * @throws Exception
     */
    public void x24HourSkdRolling() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date tue = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "TUE");
        Date mon = DateHelper.addDays(tue , -1);

        InsertEmployeeScheduleOverride ins0 = new InsertEmployeeScheduleOverride(getConnection());
        ins0.setWbuNameBoth("JUNIT", "JUNIT");;
        ins0.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins0.setEmpId(empId);
        ins0.setStartDate(mon);      ins0.setEndDate(mon);
        Datetime sh0Start = DateHelper.addMinutes(mon, 7*60);
        Datetime sh0End = DateHelper.addMinutes(mon, 15*60);
        ins0.setEmpskdActStartTime(sh0Start);
        ins0.setEmpskdActEndTime(sh0End);
        ovrBuilder.add(ins0);
        ovrBuilder.execute(true , false);
        InsertEmployeeScheduleOverride ins1 = new InsertEmployeeScheduleOverride(getConnection());
        ins1.setWbuNameBoth("JUNIT", "JUNIT");;
        ins1.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins1.setEmpId(empId);
        ins1.setStartDate(tue);      ins1.setEndDate(tue);
        Datetime sh1Start = DateHelper.addMinutes(tue, 7*60);
        Datetime sh1End = DateHelper.addMinutes(tue, 15*60);
        ins1.setEmpskdActStartTime(sh1Start);
        ins1.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins1);
        ovrBuilder.execute(true , false);         ovrBuilder.clear();



        // *** create the rule
        Rule rule = new DailyOvertime24HourSkdRollRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT1=240,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_P24_HOUR_STARTTIME, DailyOvertime24HourSkdRollRule.PARAM_VAL_SCHEDULE_ROLLING_LAST_HSET);
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_ELIGIBLE_HOURTYPES, "REG,OT1,OT2");
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_WORKDETAIL_TIMECODES, "WRK");

        clearAndAddRule(empId , tue , rule , ruleparams);

        InsertWorkSummaryOverride insCl1 = new InsertWorkSummaryOverride(getConnection());
        insCl1.setWbuNameBoth("JUNIT", "JUNIT");;
        insCl1.setEmpId(empId);
        insCl1.setStartDate(mon);      insCl1.setEndDate(mon);
        Datetime clk1On = DateHelper.addMinutes(mon, 7*60);
        Datetime clk1Off= DateHelper.addMinutes(mon, 19*60);
        insCl1.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1On , clk1Off));
        ovrBuilder.add(insCl1);

        InsertWorkSummaryOverride insCl2 = new InsertWorkSummaryOverride(getConnection());
        insCl2.setWbuNameBoth("JUNIT", "JUNIT");;
        insCl2.setEmpId(empId);
        insCl2.setStartDate(tue);      insCl2.setEndDate(tue);
        Datetime clkT1On = DateHelper.addMinutes(tue, 5*60);
        Datetime clkT1Off= DateHelper.addMinutes(tue, 14*60);
        Datetime clkT2On = DateHelper.addMinutes(tue, 16*60);
        Datetime clkT2Off= DateHelper.addMinutes(tue, 18*60);
        insCl2.setWrksClocks(createWorkSummaryClockStringForOnOffs(clkT1On , clkT1Off , clkT2On, clkT2Off));
        ovrBuilder.add(insCl2);

        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertRuleApplied(empId, tue, rule);


        WorkDetailList wd2 = getWorkDetailsForDate(empId , tue);
        System.out.println(wd2.toDescription());
        int ind1 = wd2.getFirstRecordIndex(DateHelper.addMinutes(tue, 7*60) ,
            DateHelper.addMinutes(tue, 14*60) , false);
        assertFalse(ind1 == -1);
        assertEquals("OT2" , wd2.getWorkDetail(ind1).getWrkdHtypeName());
        ind1 = wd2.getFirstRecordIndex(DateHelper.addMinutes(tue, 16*60) ,
                    DateHelper.addMinutes(tue, 18*60) , false);
        assertFalse(ind1 == -1);
        assertEquals("OT1" , wd2.getWorkDetail(ind1).getWrkdHtypeName());
    }

    /**
     * @throws Exception
     */
    public void x24HourSkdRolling2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date tue = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "TUE");
        Date mon = DateHelper.addDays(tue , -1);

        InsertEmployeeScheduleOverride ins0 = new InsertEmployeeScheduleOverride(getConnection());
        ins0.setWbuNameBoth("JUNIT", "JUNIT");;
        ins0.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins0.setEmpId(empId);
        ins0.setStartDate(mon);      ins0.setEndDate(mon);
        Datetime sh0Start = DateHelper.addMinutes(mon, 7*60);
        Datetime sh0End = DateHelper.addMinutes(mon, 15*60);
        ins0.setEmpskdActStartTime(sh0Start);
        ins0.setEmpskdActEndTime(sh0End);
        ovrBuilder.add(ins0);
        ovrBuilder.execute(true , false);
        InsertEmployeeScheduleOverride ins1 = new InsertEmployeeScheduleOverride(getConnection());
        ins1.setWbuNameBoth("JUNIT", "JUNIT");;
        ins1.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins1.setEmpId(empId);
        ins1.setStartDate(tue);      ins1.setEndDate(tue);
        Datetime sh1Start = DateHelper.addMinutes(tue, 7*60);
        Datetime sh1End = DateHelper.addMinutes(tue, 15*60);
        ins1.setEmpskdActStartTime(sh1Start);
        ins1.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins1);
        ovrBuilder.execute(true , false);         ovrBuilder.clear();



        // *** create the rule
        Rule rule = new DailyOvertime24HourSkdRollRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT1=240,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_P24_HOUR_STARTTIME, DailyOvertime24HourSkdRollRule.PARAM_VAL_SCHEDULE_ROLLING_LAST_HSET);
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_ELIGIBLE_HOURTYPES, "REG,OT1,OT2");
        ruleparams.addParameter(DailyOvertime24HourSkdRollRule.PARAM_WORKDETAIL_TIMECODES, "WRK");

        clearAndAddRule(empId , tue , rule , ruleparams);

        InsertWorkSummaryOverride insCl1 = new InsertWorkSummaryOverride(getConnection());
        insCl1.setWbuNameBoth("JUNIT", "JUNIT");;
        insCl1.setEmpId(empId);
        insCl1.setStartDate(mon);      insCl1.setEndDate(mon);
        Datetime clk1On = DateHelper.addMinutes(mon, 7*60);
        Datetime clk1Off= DateHelper.addMinutes(mon, 19*60);
        insCl1.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1On , clk1Off));
        ovrBuilder.add(insCl1);

        InsertWorkSummaryOverride insCl2 = new InsertWorkSummaryOverride(getConnection());
        insCl2.setWbuNameBoth("JUNIT", "JUNIT");;
        insCl2.setEmpId(empId);
        insCl2.setStartDate(tue);      insCl2.setEndDate(tue);
        Datetime clkT1On = DateHelper.addMinutes(tue, 7*60);
        Datetime clkT1Off= DateHelper.addMinutes(tue, 14*60);
        Datetime clkT2On = DateHelper.addMinutes(tue, 16*60);
        Datetime clkT2Off= DateHelper.addMinutes(tue, 18*60);
        insCl2.setWrksClocks(createWorkSummaryClockStringForOnOffs(clkT1On , clkT1Off , clkT2On, clkT2Off));
        ovrBuilder.add(insCl2);

        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertRuleApplied(empId, tue, rule);


        WorkDetailList wd2 = getWorkDetailsForDate(empId , tue);
        System.out.println(wd2.toDescription());
        int ind1 = wd2.getFirstRecordIndex(DateHelper.addMinutes(tue, 7*60) ,
            DateHelper.addMinutes(tue, 14*60) , false);
        assertFalse(ind1 == -1);
        assertEquals("REG" , wd2.getWorkDetail(ind1).getWrkdHtypeName());
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

