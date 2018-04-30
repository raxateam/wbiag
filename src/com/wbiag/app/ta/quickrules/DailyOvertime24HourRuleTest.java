package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;
/**
 * Test for DailyOvertime24HourRuleTest.
 */
public class DailyOvertime24HourRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DailyOvertime24HourRuleTest.class);

    public DailyOvertime24HourRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
    	BasicConfigurator.configure();
        TestSuite result = new TestSuite();
        result.addTestSuite(DailyOvertime24HourRuleTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testEmpVal() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);

        // *** create the rule
        Rule rule = new DailyOvertime24HourRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME, "emp_val1");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES, "WRK,OT2");

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("JUNIT", "JUNIT");;
        insEmp.setEmpId(empId);
        insEmp.setStartDate(DateHelper.DATE_1900);      insEmp.setEndDate(DateHelper.DATE_3000);
        insEmp.setEmpVal1("19000101 010000");
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(false , false); ovrBuilder.clear();

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);      ins.setEndDate(mon);
        Datetime sh1Start = DateHelper.addMinutes(mon, 13*60);
        Datetime sh1End = DateHelper.addMinutes(mon, 20*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        InsertEmployeeScheduleOverride ins2 = new InsertEmployeeScheduleOverride(getConnection());
        ins2.setWbuNameBoth("JUNIT", "JUNIT");;
        ins2.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins2.setEmpId(empId);
        ins2.setStartDate(tue);      ins2.setEndDate(tue);
        sh1Start = DateHelper.addMinutes(tue, -2*60);
        sh1End = DateHelper.addMinutes(tue, 10*60);
        ins2.setEmpskdActStartTime(sh1Start);
        ins2.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertRuleApplied(empId, mon, rule);


        WorkDetailList wdl = getWorkDetailsForDate(empId , tue);
        int ind1 = wdl.getFirstRecordIndex(DateHelper.addMinutes(tue, -1*60) ,
            DateHelper.addMinutes(tue, 1*60) , false);
        assertFalse(ind1 == -1);
        assertEquals("OT2" , wdl.getWorkDetail(ind1).getWrkdHtypeName());
        int ind2 = wdl.getFirstRecordIndex(DateHelper.addMinutes(tue, 9*60) ,
            DateHelper.addMinutes(tue, 10*60) , false);
        assertFalse(ind2 == -1);
        assertEquals("OT2" , wdl.getWorkDetail(ind2).getWrkdHtypeName());

    }

    /**
     * Scenario was given by David Ho on Target project.
     *
     * @throws Exception
     */
    public void test24HourReset() throws Exception {

        final int empId = 3;

        //Date currentDate = DateHelper.convertStringToDate("04/03/2005", "MM/dd/yyyy");
        Date currentDate = DateHelper.nextDay(DateHelper.getCurrentDate() , "SUN" );
        Datetime clk1On = null;
        Datetime clk1Off = null;

        // *** create the rule
        Rule rule = new DailyOvertime24HourRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME, DailyOvertime24HourRule.CHOICE_24_HOUR_RESET);
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        clearAndAddRule(empId , currentDate , rule , ruleparams);

        // Create Default Records
        new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                  currentDate, DateHelper.addDays(currentDate, 6) ).execute(false);

        String clks = null;
        InsertWorkSummaryOverride wsOvr = null;

        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // Monday
        //currentDate = DateHelper.convertStringToDate("04/04/2005", "MM/dd/yyyy");
		//clk1On = DateHelper.parseDate("04/04/2005 10:00", "MM/dd/yyyy HH:mm");
        //clk1Off = DateHelper.parseDate("04/04/2005 18:00", "MM/dd/yyyy HH:mm");
        Date mon = DateHelper.addDays(currentDate , 1);
        clk1On = DateHelper.addMinutes(mon , 10*60);
        clk1Off = DateHelper.addMinutes(mon , 18*60);

        wsOvr = new InsertWorkSummaryOverride(getConnection());
        clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(mon);
        wsOvr.setEndDate(mon);
        ovrBuilder.add(wsOvr);

        // Tuesday
        //currentDate = DateHelper.convertStringToDate("04/05/2005", "MM/dd/yyyy");
		//clk1On = DateHelper.parseDate("04/05/2005 08:00", "MM/dd/yyyy HH:mm");
        //clk1Off = DateHelper.parseDate("04/05/2005 18:00", "MM/dd/yyyy HH:mm");
        Date tue = DateHelper.addDays(currentDate , 2);
        clk1On = DateHelper.addMinutes(tue , 8*60);
        clk1Off = DateHelper.addMinutes(tue , 18*60);


        wsOvr = new InsertWorkSummaryOverride(getConnection());
        clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(tue);
        wsOvr.setEndDate(tue);
        ovrBuilder.add(wsOvr);

        // Wednesday
        //currentDate = DateHelper.convertStringToDate("04/06/2005", "MM/dd/yyyy");
		//clk1On = DateHelper.parseDate("04/06/2005 06:00", "MM/dd/yyyy HH:mm");
        //clk1Off = DateHelper.parseDate("04/06/2005 12:00", "MM/dd/yyyy HH:mm");
        Date wed = DateHelper.addDays(currentDate , 3);
        clk1On = DateHelper.addMinutes(wed , 6*60);
        clk1Off = DateHelper.addMinutes(wed , 12*60);

        wsOvr = new InsertWorkSummaryOverride(getConnection());
        clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(wed);
        wsOvr.setEndDate(wed);
        ovrBuilder.add(wsOvr);

        // Thursday
        //currentDate = DateHelper.convertStringToDate("04/07/2005", "MM/dd/yyyy");
		//clk1On = DateHelper.parseDate("04/07/2005 11:00", "MM/dd/yyyy HH:mm");
        //clk1Off = DateHelper.parseDate("04/07/2005 19:00", "MM/dd/yyyy HH:mm");
        Date thu = DateHelper.addDays(currentDate , 4);
        clk1On = DateHelper.addMinutes(thu , 11*60);
        clk1Off = DateHelper.addMinutes(thu , 19*60);

        wsOvr = new InsertWorkSummaryOverride(getConnection());
        clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(thu);
        wsOvr.setEndDate(thu);
        ovrBuilder.add(wsOvr);

        // Friday
        //currentDate = DateHelper.convertStringToDate("04/08/2005", "MM/dd/yyyy");
		//clk1On = DateHelper.parseDate("04/08/2005 10:00", "MM/dd/yyyy HH:mm");
        //clk1Off = DateHelper.parseDate("04/08/2005 18:00", "MM/dd/yyyy HH:mm");
        Date fri = DateHelper.addDays(currentDate , 5);
        clk1On = DateHelper.addMinutes(fri , 10*60);
        clk1Off = DateHelper.addMinutes(fri , 18*60);

        wsOvr = new InsertWorkSummaryOverride(getConnection());
        clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(fri);
        wsOvr.setEndDate(fri);
        ovrBuilder.add(wsOvr);

        ovrBuilder.execute(true , false);

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 5);

        // Monday
        //currentDate = DateHelper.convertStringToDate("04/04/2005", "MM/dd/yyyy");
        WorkDetailList wdl = getWorkDetailsForDate(empId, mon);
        System.out.println(wdl.toDescription());
        assertEquals(480 , wdl.getWorkDetail(0).getWrkdMinutes());
        assertEquals("REG" , wdl.getWorkDetail(0).getWrkdHtypeName());

        // Tuesday
        //currentDate = DateHelper.convertStringToDate("04/05/2005", "MM/dd/yyyy");
        wdl = getWorkDetailsForDate(empId, tue);
        assertEquals(120 , wdl.getWorkDetail(0).getWrkdMinutes());
        assertEquals("OT2" , wdl.getWorkDetail(0).getWrkdHtypeName());
        assertEquals(480 , wdl.getWorkDetail(1).getWrkdMinutes());
        assertEquals("REG" , wdl.getWorkDetail(1).getWrkdHtypeName());

        // Wednesday
        //currentDate = DateHelper.convertStringToDate("04/06/2005", "MM/dd/yyyy");
        wdl = getWorkDetailsForDate(empId, wed);
        assertEquals(240 , wdl.getWorkDetail(0).getWrkdMinutes());
        assertEquals("OT2" , wdl.getWorkDetail(0).getWrkdHtypeName());
        assertEquals(120 , wdl.getWorkDetail(1).getWrkdMinutes());
        assertEquals("REG" , wdl.getWorkDetail(1).getWrkdHtypeName());

        // Thursday
        //currentDate = DateHelper.convertStringToDate("04/07/2005", "MM/dd/yyyy");
        wdl = getWorkDetailsForDate(empId, thu);
        assertEquals(480 , wdl.getWorkDetail(0).getWrkdMinutes());
        assertEquals("REG" , wdl.getWorkDetail(0).getWrkdHtypeName());

        // Friday
        //currentDate = DateHelper.convertStringToDate("04/08/2005", "MM/dd/yyyy");
        wdl = getWorkDetailsForDate(empId, fri);
        assertEquals(60 , wdl.getWorkDetail(0).getWrkdMinutes());
        assertEquals("OT2" , wdl.getWorkDetail(0).getWrkdHtypeName());
        assertEquals(420 , wdl.getWorkDetail(1).getWrkdMinutes());
        assertEquals("REG" , wdl.getWorkDetail(1).getWrkdHtypeName());
    }

    /**
     * @throws Exception
     */
    public void testPreviousDayStart() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);

        // *** create the rule
        Rule rule = new DailyOvertime24HourRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME, DailyOvertime24HourRule.CHOICE_PREVIOUS_DAY_START);
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES, "WRK");

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);      ins.setEndDate(mon);
        Datetime sh1Start = DateHelper.addMinutes(mon, 13*60);
        Datetime sh1End = DateHelper.addMinutes(mon, 20*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins);

        InsertEmployeeScheduleOverride ins2 = new InsertEmployeeScheduleOverride(getConnection());
        ins2.setWbuNameBoth("JUNIT", "JUNIT");;
        ins2.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins2.setEmpId(empId);
        ins2.setStartDate(tue);      ins2.setEndDate(tue);
        sh1Start = DateHelper.addMinutes(tue, 11*60);
        sh1End = DateHelper.addMinutes(tue, 20*60);
        ins2.setEmpskdActStartTime(sh1Start);
        ins2.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, mon, rule);


        WorkDetailList wdl = getWorkDetailsForDate(empId , tue);
        int ind1 = wdl.getFirstRecordIndex(DateHelper.addMinutes(tue, 12*60) ,
            DateHelper.addMinutes(tue, 13*60) , false);
        assertFalse(ind1 == -1);
        assertEquals("OT2" , wdl.getWorkDetail(ind1).getWrkdHtypeName());

    }

    /**
     * @throws Exception
     */
    public void testBeforeStartOnly() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);

        // *** create the rule
        Rule rule = new DailyOvertime24HourRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME, DailyOvertime24HourRule.CHOICE_PREVIOUS_DAY_START);
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_CALC_PERIOD, DailyOvertime24HourRule.PARAM_VAL_BEFORE_CUTOFF);

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = null;
        Datetime shiftStart = null;
        Datetime shiftEnd = null;

        // Monday Schedule
        ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);
        ins.setEndDate(mon);
        shiftStart = DateHelper.addMinutes(mon, 13*60);
        shiftEnd = DateHelper.addMinutes(mon, 21*60);
        ins.setEmpskdActStartTime(shiftStart);
        ins.setEmpskdActEndTime(shiftEnd);
        ovrBuilder.add(ins);

        // Tues Schedule
        ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(tue);
        ins.setEndDate(tue);
        shiftStart = DateHelper.addMinutes(tue, 13*60);
        shiftEnd = DateHelper.addMinutes(tue, 21*60);
        ins.setEmpskdActStartTime(shiftStart);
        ins.setEmpskdActEndTime(shiftEnd);
        ovrBuilder.add(ins);


        Datetime clk1On = null;
        Datetime clk1Off = null;
        String clks = null;
        InsertWorkSummaryOverride wsOvr = null;

        // Tuesday clocks
		clk1On = DateHelper.addMinutes(tue, 12*60); 	// 1 hr early
        clk1Off = DateHelper.addMinutes(tue, 22*60);	// 1 hr late
        wsOvr = new InsertWorkSummaryOverride(getConnection());
        clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(tue);
        wsOvr.setEndDate(tue);
        ovrBuilder.add(wsOvr);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, mon, rule);

        WorkDetailList wdl = getWorkDetailsForDate(empId , tue);

        // Only the Pre-cutoff was processed. Remainder of work details
        // stayed REG instead of OT2
        assertEquals(60 , wdl.getWorkDetail(0).getWrkdMinutes());
        assertEquals("OT2" , wdl.getWorkDetail(0).getWrkdHtypeName());

        assertEquals(540 , wdl.getWorkDetail(1).getWrkdMinutes());
        assertEquals("REG" , wdl.getWorkDetail(1).getWrkdHtypeName());
    }

    /**
     * Make sure empday start time is considered when calculating cutoff time
     * @throws Exception
     */
    public void testBeforeStartOnly2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);

        // *** create the rule
        Rule rule = new DailyOvertime24HourRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION, "OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME, "19000101 223000");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_CALC_PERIOD, DailyOvertime24HourRule.PARAM_VAL_BEFORE_CUTOFF);

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = null;
        Datetime shiftStart = null;
        Datetime shiftEnd = null;

        // Monday Schedule
        ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);
        ins.setEndDate(mon);
        shiftStart = DateHelper.addMinutes(mon, -2*60);
        shiftEnd = DateHelper.addMinutes(mon, 7*60);
        ins.setEmpskdActStartTime(shiftStart);
        ins.setEmpskdActEndTime(shiftEnd);
        ovrBuilder.add(ins);

        InsertEmployeeOverride insE = new InsertEmployeeOverride(getConnection());
        insE.setWbuNameBoth("JUNIT", "JUNIT");;
        insE.setEmpId(empId);
        insE.setStartDate(DateHelper.DATE_1900 );
        insE.setEndDate(DateHelper.DATE_3000 );
        insE.setEmpDayStartTime(DateHelper.convertStringToDate("19000101 1630000", "yyyyMMdd HHmmss"));
        ovrBuilder.add(insE);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, mon, rule);

        WorkDetailList wdl = getWorkDetailsForDate(empId , mon);
        System.out.println(wdl.toDescription() );
        // Only the Pre-cutoff was processed. Remainder of work details
        // stayed REG instead of OT2
        assertEquals(30 , wdl.getWorkDetail(0).getWrkdMinutes());
        assertEquals("OT2" , wdl.getWorkDetail(0).getWrkdHtypeName());

    }

    /**
     * @throws Exception
     */
    public void testAfterStartOnly() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);

        // *** create the rule
        Rule rule = new DailyOvertime24HourRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME, DailyOvertime24HourRule.CHOICE_PREVIOUS_DAY_START);
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_CALC_PERIOD, DailyOvertime24HourRule.PARAM_VAL_AFTER_CUTOFF);

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeScheduleOverride ins = null;
        Datetime shiftStart = null;
        Datetime shiftEnd = null;

        // Monday Schedule
        ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);
        ins.setEndDate(mon);
        shiftStart = DateHelper.addMinutes(mon, 13*60);
        shiftEnd = DateHelper.addMinutes(mon, 21*60);
        ins.setEmpskdActStartTime(shiftStart);
        ins.setEmpskdActEndTime(shiftEnd);
        ovrBuilder.add(ins);

        // Tues Schedule
        ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(tue);
        ins.setEndDate(tue);
        shiftStart = DateHelper.addMinutes(tue, 13*60);
        shiftEnd = DateHelper.addMinutes(tue, 21*60);
        ins.setEmpskdActStartTime(shiftStart);
        ins.setEmpskdActEndTime(shiftEnd);
        ovrBuilder.add(ins);


        Datetime clk1On = null;
        Datetime clk1Off = null;
        String clks = null;
        InsertWorkSummaryOverride wsOvr = null;

        // Tuesday clocks
		clk1On = DateHelper.addMinutes(tue, 12*60); 	// 1 hr early
        clk1Off = DateHelper.addMinutes(tue, 22*60);	// 1 hr late
        wsOvr = new InsertWorkSummaryOverride(getConnection());
        clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(tue);
        wsOvr.setEndDate(tue);
        ovrBuilder.add(wsOvr);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, mon, rule);

        WorkDetailList wdl = getWorkDetailsForDate(empId , tue);

        // Pre-cutoff was not processed so stayed REG instead of OT2
        assertEquals(540 , wdl.getWorkDetail(0).getWrkdMinutes());
        assertEquals("REG" , wdl.getWorkDetail(0).getWrkdHtypeName());

        assertEquals(60 , wdl.getWorkDetail(1).getWrkdMinutes());
        assertEquals("OT2" , wdl.getWorkDetail(1).getWrkdHtypeName());
    }

    /**
     * @throws Exception
     */
    public void testEmpVal2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);

        // *** create the rule
        Rule rule = new DailyOvertime24HourRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT1=240,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME, "emp_val1");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES, "REG,OT1,OT2");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES, "WRK");

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("JUNIT", "JUNIT");;
        insEmp.setEmpId(empId);
        insEmp.setStartDate(DateHelper.DATE_1900);      insEmp.setEndDate(DateHelper.DATE_3000);
        insEmp.setEmpVal1("19000101 070000");
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(false , false); ovrBuilder.clear();

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);      ins.setEndDate(mon);
        Datetime sh1Start = DateHelper.addMinutes(mon, 7*60);
        Datetime sh1End = DateHelper.addMinutes(mon, 15*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins);

        InsertEmployeeScheduleOverride ins2 = new InsertEmployeeScheduleOverride(getConnection());
        ins2.setWbuNameBoth("JUNIT", "JUNIT");;
        ins2.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins2.setEmpId(empId);
        ins2.setStartDate(tue);      ins2.setEndDate(tue);
        Datetime sh2Start = DateHelper.addMinutes(tue, 7*60);
        Datetime sh2End = DateHelper.addMinutes(tue, 15*60);
        ins2.setEmpskdActStartTime(sh2Start);
        ins2.setEmpskdActEndTime(sh2End);
        ovrBuilder.add(ins2);

        InsertWorkSummaryOverride ins3 = new InsertWorkSummaryOverride(getConnection());
        ins3.setWbuNameBoth("JUNIT", "JUNIT");;
        ins3.setEmpId(empId);
        ins3.setStartDate(mon);      ins3.setEndDate(mon);
        Datetime clk1On = DateHelper.addMinutes(mon, 7*60);
        Datetime clk1Off= DateHelper.addMinutes(mon, 19*60);
        ins3.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1On , clk1Off));
        ovrBuilder.add(ins3);

        InsertWorkSummaryOverride ins4 = new InsertWorkSummaryOverride(getConnection());
        ins4.setWbuNameBoth("JUNIT", "JUNIT");;
        ins4.setEmpId(empId);
        ins4.setStartDate(tue);      ins4.setEndDate(tue);
        clk1On = DateHelper.addMinutes(tue, 5*60);
        clk1Off= DateHelper.addMinutes(tue, 15*60);
        ins4.setWrksClocks(createWorkSummaryClockStringForOnOffs(clk1On , clk1Off));
        ovrBuilder.add(ins4);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, mon, rule);


        WorkDetailList wdl0 = getWorkDetailsForDate(empId , mon);

        WorkDetailList wdl = getWorkDetailsForDate(empId , tue);
        int ind1 = wdl.getFirstRecordIndex(DateHelper.addMinutes(tue, 5*60) ,
            DateHelper.addMinutes(tue, 7*60) , false);
        assertFalse(ind1 == -1);
        assertEquals("OT2" , wdl.getWorkDetail(ind1).getWrkdHtypeName());
    }

    public void xxxxAfter24HrsOnly() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        // *** create the rule
        Rule rule = new DailyOvertime24HourRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION, "REG=60,OT1=9999");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME, "19000101 000000");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_CALC_PERIOD, DailyOvertime24HourRule.PARAM_VAL_AFTER_24HOURS);

        clearAndAddRule(empId , mon , rule , ruleparams);

        Datetime clk1On = null;
        Datetime clk1Off = null;
        String clks = null;
        InsertWorkSummaryOverride wsOvr = null;

        // Tuesday clocks
		clk1On = DateHelper.addMinutes(mon, 23*60); 	// 1 hr early
        clk1Off = DateHelper.addMinutes(DateHelper.addDays(mon, 1), 2*60);	// 1 hr late
        wsOvr = new InsertWorkSummaryOverride(getConnection());
        clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("JUNIT", "JUNIT");;
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(mon);
        wsOvr.setEndDate(mon);
        ovrBuilder.add(wsOvr);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, mon, rule);

        WorkDetailList wdl = getWorkDetailsForDate(empId , mon);

        // Only the Pre-cutoff was processed. Remainder of work details
        // stayed REG instead of OT2
        assertEquals(120 , wdl.getWorkDetail(0).getWrkdMinutes());
        assertEquals("REG" , wdl.getWorkDetail(0).getWrkdHtypeName());

        assertEquals(60 , wdl.getWorkDetail(1).getWrkdMinutes());
        assertEquals("OT1" , wdl.getWorkDetail(1).getWrkdHtypeName());
    }

    /**
     * @throws Exception
     */
    public void testDiscountCode() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);

        // *** create the rule
        Rule rule = new DailyOvertime24HourRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT2=9999");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_P24_HOUR_STARTTIME, "emp_val1");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        ruleparams.addParameter(DailyOvertime24HourRule.PARAM_DISCOUNT_TIMECODES, "TRN");

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("JUNIT", "JUNIT");;
        insEmp.setEmpId(empId);
        insEmp.setStartDate(DateHelper.DATE_1900);      insEmp.setEndDate(DateHelper.DATE_3000);
        insEmp.setEmpVal1("19000101 010000");
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(false , false); ovrBuilder.clear();

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);      ins.setEndDate(mon);
        Datetime sh1Start = DateHelper.addMinutes(mon, 13*60);
        Datetime sh1End = DateHelper.addMinutes(mon, 22*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setWbuNameBoth("JUNIT", "JUNIT");;
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setEmpId(empId);
        ins2.setStartDate(mon);      ins2.setEndDate(mon);
        sh1Start = DateHelper.addMinutes(mon, 15*60);
        sh1End = DateHelper.addMinutes(mon, 17*60);
        ins2.setStartTime(sh1Start);
        ins2.setEndTime(sh1End);
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertRuleApplied(empId, mon, rule);


        WorkDetailList wdl = getWorkDetailsForDate(empId , mon);
        int otmins = wdl.getMinutes(null ,
            null , "WRK", true, "OT2", true);
        assertEquals(60, otmins);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

