package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;
import java.sql.PreparedStatement;

/**
 * Test for HolidayReversalRuleTest.
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class HolidayReversalRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HolidayReversalRuleTest.class);
    private static final String HOLNAME_MARCH9 = "MARCH 9";
    //TT1171: Add consecutive holiday
    private static final String HOLNAME_MARCH8 = "MARCH 8";
    

    public HolidayReversalRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        BasicConfigurator.configure();

        TestSuite result = new TestSuite();
        result.addTestSuite(HolidayReversalRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testWorkedNextScheduled() throws Exception {

        logger.debug("Testing: testWorkedNextScheduled");

    	// The empId does not have a schedule.
    	final int empId = 3;
        Date weekStart = DateHelper.convertStringToDate("03/06/2005", "MM/dd/yyyy");
    	Date holidayDate = DateHelper.convertStringToDate("03/09/2005", "MM/dd/yyyy");

        Rule rule = new HolidayReversalRule();
        Parameters ruleparams = new Parameters();

        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_TIMECODES, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_REVERSAL_TIMECODE, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_ELIGIBLE_WORK_TIMECODES, "WRK");
        ruleparams.addParameter(HolidayReversalRule.PARAM_MAX_PAY_PERIODS, "2");
        ruleparams.addParameter(HolidayReversalRule.PARAM_REVERSE_IF_NOT_SCHEDULED, "true");
        //ruleparams.addParameter(HolidayReversalRule.PARAM_INSERT_NEGATIVE_MINUTES, "false");

        clearAndAddRule( empId, weekStart, rule, ruleparams );

        // Create Default Records
        new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                  weekStart, DateHelper.addDays(weekStart, 21) ).execute(false);

        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // Holiday
    	InsertEmployeeHolidayOverride holOvr = new InsertEmployeeHolidayOverride(getConnection());
        holOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holOvr.setEmpId(empId);
    	holOvr.setHolName(HOLNAME_MARCH9);
    	holOvr.setStartDate(holidayDate);
    	holOvr.setEndDate(holidayDate);
        ovrBuilder.add(holOvr);

        // Holiday Premium
        InsertWorkPremiumOverride holPremOvr = new InsertWorkPremiumOverride(getConnection());
        holPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holPremOvr.setEmpId(empId);
        holPremOvr.setStartDate(holidayDate);
        holPremOvr.setEndDate(holidayDate);
        holPremOvr.setWrkdTcodeName("HOL");
        holPremOvr.setWrkdHtypeName("REG");
        holPremOvr.setWrkdRate(25);
        holPremOvr.setWrkdMinutes(480);
        holPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(holPremOvr);

        // Next scheduled shift.  The last day of the max pay periods.
        InsertEmployeeScheduleOverride schedTimesOvr = new InsertEmployeeScheduleOverride(getConnection());
        Date scheduleDate = DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy");
        schedTimesOvr.setWbuNameBoth("JUNIT", "JUNIT");
        schedTimesOvr.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        schedTimesOvr.setEmpId(empId);
        schedTimesOvr.setStartDate(scheduleDate);
        schedTimesOvr.setEndDate(scheduleDate);
        Datetime sh1Start = DateHelper.addMinutes(scheduleDate, 9*60);
        Datetime sh1End = DateHelper.addMinutes(scheduleDate, 13*60);
        schedTimesOvr.setEmpskdActStartTime(sh1Start);
        schedTimesOvr.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(schedTimesOvr);

        // Clocks on next scheduled shift.
        Datetime clk1On = sh1Start;
        Datetime clk1Off = sh1End;
        InsertWorkSummaryOverride clocksOvr = new InsertWorkSummaryOverride(getConnection());
        String clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        clocksOvr.setWrksClocks(clks);
        clocksOvr.setWbuNameBoth("JUNIT", "JUNIT");
        clocksOvr.setEmpId(empId);
        clocksOvr.setStartDate(scheduleDate);
        clocksOvr.setEndDate(scheduleDate);
        ovrBuilder.add(clocksOvr);

        // Process the overrides.
        ovrBuilder.execute( true , false );

        assertOverrideAppliedCount(ovrBuilder, 4);

       // Worked the next scheduled shift so should not be a reversal premium.
        WorkDetailList wps = getWorkPremiumsForDate(empId , scheduleDate);
        assertEquals(0, wps.size());
    }


    /**
     * @throws Exception
     */
    public void testNotWorkedNextScheduled() throws Exception {

     	logger.debug("Testing: testNotWorkedNextScheduled");

     	// The empId does not have a schedule.
    	final int empId = 3;
        Date weekStart = DateHelper.convertStringToDate("03/06/2005", "MM/dd/yyyy");
    	Date holidayDate = DateHelper.convertStringToDate("03/09/2005", "MM/dd/yyyy");

        Rule rule = new HolidayReversalRule();
        Parameters ruleparams = new Parameters();

        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_TIMECODES, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_REVERSAL_TIMECODE, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_ELIGIBLE_WORK_TIMECODES, "WRK");
        ruleparams.addParameter(HolidayReversalRule.PARAM_MAX_PAY_PERIODS, "2");
        ruleparams.addParameter(HolidayReversalRule.PARAM_REVERSE_IF_NOT_SCHEDULED, "true");
        //ruleparams.addParameter(HolidayReversalRule.PARAM_INSERT_NEGATIVE_MINUTES, "false");
        clearAndAddRule( empId, weekStart, rule, ruleparams );

        // Create Default Records
        new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                  weekStart, DateHelper.addDays(weekStart, 21) ).execute(false);

        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // Holiday
    	InsertEmployeeHolidayOverride holOvr = new InsertEmployeeHolidayOverride(getConnection());
        holOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holOvr.setEmpId(empId);
    	holOvr.setHolName(HOLNAME_MARCH9);
    	holOvr.setStartDate(holidayDate);
    	holOvr.setEndDate(holidayDate);
        ovrBuilder.add(holOvr);

        // Holiday Premium
        InsertWorkPremiumOverride holPremOvr = new InsertWorkPremiumOverride(getConnection());
        holPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holPremOvr.setEmpId(empId);
        holPremOvr.setStartDate(holidayDate);
        holPremOvr.setEndDate(holidayDate);
        holPremOvr.setWrkdTcodeName("HOL");
        holPremOvr.setWrkdHtypeName("REG");
        holPremOvr.setWrkdRate(25);
        holPremOvr.setWrkdMinutes(480);
        holPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(holPremOvr);

        // Next scheduled shift.
        InsertEmployeeScheduleOverride schedTimesOvr = new InsertEmployeeScheduleOverride(getConnection());
        Date scheduleDate = DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy");
        schedTimesOvr.setWbuNameBoth("JUNIT", "JUNIT");
        schedTimesOvr.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        schedTimesOvr.setEmpId(empId);
        schedTimesOvr.setStartDate(scheduleDate);
        schedTimesOvr.setEndDate(scheduleDate);
        Datetime sh1Start = DateHelper.addMinutes(scheduleDate, 9*60);
        Datetime sh1End = DateHelper.addMinutes(scheduleDate, 13*60);
        schedTimesOvr.setEmpskdActStartTime(sh1Start);
        schedTimesOvr.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(schedTimesOvr);

        // No clocks on next scheduled shift.

        // Process the overrides.
        ovrBuilder.execute( true , false );

        assertOverrideAppliedCount(ovrBuilder, 3);

        // Did not work the next scheduled shift so should be a reversal premium.
        assertWorkPremiumTimeCodeMinutes(empId , scheduleDate, "HOL" , 480, new Double(-25));
    }


    /**
     * @throws Exception
     */
     public void testNoScheduleBeforeMax() throws Exception {

     	logger.debug("Testing: testNoScheduleBeforeMax");

     	// The empId does not have a schedule.
    	final int empId = 3;
        Date weekStart = DateHelper.convertStringToDate("03/06/2005", "MM/dd/yyyy");
    	Date holidayDate = DateHelper.convertStringToDate("03/09/2005", "MM/dd/yyyy");

        Rule rule = new HolidayReversalRule();
        Parameters ruleparams = new Parameters();

        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_TIMECODES, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_REVERSAL_TIMECODE, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_ELIGIBLE_WORK_TIMECODES, "WRK");
        ruleparams.addParameter(HolidayReversalRule.PARAM_MAX_PAY_PERIODS, "2");
        ruleparams.addParameter(HolidayReversalRule.PARAM_REVERSE_IF_NOT_SCHEDULED, "true");
        //ruleparams.addParameter(HolidayReversalRule.PARAM_INSERT_NEGATIVE_MINUTES, "false");
        clearAndAddRule( empId, weekStart, rule, ruleparams );

        // Create Default Records
        new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                  weekStart, DateHelper.addDays(weekStart, 22) ).execute(false);

        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // Holiday
    	InsertEmployeeHolidayOverride holOvr = new InsertEmployeeHolidayOverride(getConnection());
        holOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holOvr.setEmpId(empId);
    	holOvr.setHolName(HOLNAME_MARCH9);
    	holOvr.setStartDate(holidayDate);
    	holOvr.setEndDate(holidayDate);
        ovrBuilder.add(holOvr);

        // Holiday Premium
        InsertWorkPremiumOverride holPremOvr = new InsertWorkPremiumOverride(getConnection());
        holPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holPremOvr.setEmpId(empId);
        holPremOvr.setStartDate(holidayDate);
        holPremOvr.setEndDate(holidayDate);
        holPremOvr.setWrkdTcodeName("HOL");
        holPremOvr.setWrkdHtypeName("REG");
        holPremOvr.setWrkdRate(25);
        holPremOvr.setWrkdMinutes(480);
        holPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(holPremOvr);

        // Next scheduled shift is past the max pay periods.
        InsertEmployeeScheduleOverride schedTimesOvr = new InsertEmployeeScheduleOverride(getConnection());
        Date scheduleDate = DateHelper.convertStringToDate("03/28/2005", "MM/dd/yyyy");
        schedTimesOvr.setWbuNameBoth("JUNIT", "JUNIT");
        schedTimesOvr.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        schedTimesOvr.setEmpId(empId);
        schedTimesOvr.setStartDate(scheduleDate);
        schedTimesOvr.setEndDate(scheduleDate);
        Datetime sh1Start = DateHelper.addMinutes(scheduleDate, 9*60);
        Datetime sh1End = DateHelper.addMinutes(scheduleDate, 13*60);
        schedTimesOvr.setEmpskdActStartTime(sh1Start);
        schedTimesOvr.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(schedTimesOvr);

        // Process the overrides.
        ovrBuilder.execute( true , false );

        assertOverrideAppliedCount(ovrBuilder, 3);

        RuleEngine.runCalcGroup(getConnection(), empId,
        							DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy"),
									DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy"),
									false);

        // Did not work the next scheduled shift so should be a reversal premium.
        assertWorkPremiumTimeCodeMinutes(empId , DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy"), "HOL" , 480, new Double(-25));
    }

    /**
     * @throws Exception
     */
    public void testNotTheNextScheduledShift() throws Exception {

     	logger.debug("Testing: testNotTheNextScheduledShift");

     	// The empId does not have a schedule.
    	final int empId = 3;
        Date weekStart = DateHelper.convertStringToDate("03/06/2005", "MM/dd/yyyy");
    	Date holidayDate = DateHelper.convertStringToDate("03/09/2005", "MM/dd/yyyy");

        Rule rule = new HolidayReversalRule();
        Parameters ruleparams = new Parameters();

        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_TIMECODES, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_REVERSAL_TIMECODE, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_ELIGIBLE_WORK_TIMECODES, "WRK");
        ruleparams.addParameter(HolidayReversalRule.PARAM_MAX_PAY_PERIODS, "2");
        ruleparams.addParameter(HolidayReversalRule.PARAM_REVERSE_IF_NOT_SCHEDULED, "true");
        //ruleparams.addParameter(HolidayReversalRule.PARAM_INSERT_NEGATIVE_MINUTES, "false");
        clearAndAddRule( empId, weekStart, rule, ruleparams );

        // Create Default Records
        new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                  weekStart, DateHelper.addDays(weekStart, 6) ).execute(false);

        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // Holiday
    	InsertEmployeeHolidayOverride holOvr = new InsertEmployeeHolidayOverride(getConnection());
        holOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holOvr.setEmpId(empId);
    	holOvr.setHolName(HOLNAME_MARCH9);
    	holOvr.setStartDate(holidayDate);
    	holOvr.setEndDate(holidayDate);
        ovrBuilder.add(holOvr);

        // Holiday Premium
        InsertWorkPremiumOverride holPremOvr = new InsertWorkPremiumOverride(getConnection());
        holPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holPremOvr.setEmpId(empId);
        holPremOvr.setStartDate(holidayDate);
        holPremOvr.setEndDate(holidayDate);
        holPremOvr.setWrkdTcodeName("HOL");
        holPremOvr.setWrkdHtypeName("REG");
        holPremOvr.setWrkdRate(25);
        holPremOvr.setWrkdMinutes(480);
        holPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(holPremOvr);

        // Next scheduled shift.
        InsertEmployeeScheduleOverride schedTimesOvr = new InsertEmployeeScheduleOverride(getConnection());
        Date scheduleDate = DateHelper.convertStringToDate("03/10/2005", "MM/dd/yyyy");
        schedTimesOvr.setWbuNameBoth("JUNIT", "JUNIT");
        schedTimesOvr.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        schedTimesOvr.setEmpId(empId);
        schedTimesOvr.setStartDate(scheduleDate);
        schedTimesOvr.setEndDate(scheduleDate);
        Datetime sh1Start = DateHelper.addMinutes(scheduleDate, 9*60);
        Datetime sh1End = DateHelper.addMinutes(scheduleDate, 13*60);
        schedTimesOvr.setEmpskdActStartTime(sh1Start);
        schedTimesOvr.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(schedTimesOvr);

        // Second shift.
        schedTimesOvr = new InsertEmployeeScheduleOverride(getConnection());
        scheduleDate = DateHelper.convertStringToDate("03/11/2005", "MM/dd/yyyy");
        schedTimesOvr.setWbuNameBoth("JUNIT", "JUNIT");
        schedTimesOvr.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        schedTimesOvr.setEmpId(empId);
        schedTimesOvr.setStartDate(scheduleDate);
        schedTimesOvr.setEndDate(scheduleDate);
        sh1Start = DateHelper.addMinutes(scheduleDate, 9*60);
        sh1End = DateHelper.addMinutes(scheduleDate, 13*60);
        schedTimesOvr.setEmpskdActStartTime(sh1Start);
        schedTimesOvr.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(schedTimesOvr);

        // Process the overrides.
        ovrBuilder.execute( true , false );

        assertOverrideAppliedCount(ovrBuilder, 4);

        // Reversal should occur on 3/10 so not on 3/11
        assertWorkPremiumTimeCodeMinutes(empId , DateHelper.addDays(scheduleDate, -1), "HOL" , 480, new Double(-25));

        WorkDetailList wps = getWorkPremiumsForDate(empId , scheduleDate);
        assertEquals(wps.size(), 0);
    }

    /**
     * Added on 9/20/2005 to cater to InsertNegativeMinutes @Author snimbkar
     * @throws Exception
     */
    public void testNotWorkedNextScheduledNegativeMinutes() throws Exception {

        logger.debug("Testing: testNotWorkedNextScheduledNegativeMinutes");

        // The empId does not have a schedule.
        final int empId = 3;
        Date weekStart = DateHelper.convertStringToDate("03/06/2005", "MM/dd/yyyy");
        Date holidayDate = DateHelper.convertStringToDate("03/09/2005", "MM/dd/yyyy");

        Rule rule = new HolidayReversalRule();
        Parameters ruleparams = new Parameters();

        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_TIMECODES, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_REVERSAL_TIMECODE, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_ELIGIBLE_WORK_TIMECODES, "WRK");
        ruleparams.addParameter(HolidayReversalRule.PARAM_MAX_PAY_PERIODS, "2");
        ruleparams.addParameter(HolidayReversalRule.PARAM_REVERSE_IF_NOT_SCHEDULED, "true");
        ruleparams.addParameter(HolidayReversalRule.PARAM_INSERT_NEGATIVE_MINUTES, "true");
        clearAndAddRule( empId, weekStart, rule, ruleparams );

        // Create Default Records
        new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                  weekStart, DateHelper.addDays(weekStart, 21) ).execute(false);

        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // Holiday
        InsertEmployeeHolidayOverride holOvr = new InsertEmployeeHolidayOverride(getConnection());
        holOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holOvr.setEmpId(empId);
        holOvr.setHolName(HOLNAME_MARCH9);
        holOvr.setStartDate(holidayDate);
        holOvr.setEndDate(holidayDate);
        ovrBuilder.add(holOvr);

        // Holiday Premium
        InsertWorkPremiumOverride holPremOvr = new InsertWorkPremiumOverride(getConnection());
        holPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holPremOvr.setEmpId(empId);
        holPremOvr.setStartDate(holidayDate);
        holPremOvr.setEndDate(holidayDate);
        holPremOvr.setWrkdTcodeName("HOL");
        holPremOvr.setWrkdHtypeName("REG");
        holPremOvr.setWrkdRate(25);
        holPremOvr.setWrkdMinutes(480);
        holPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(holPremOvr);

        // Next scheduled shift.
        InsertEmployeeScheduleOverride schedTimesOvr = new InsertEmployeeScheduleOverride(getConnection());
        Date scheduleDate = DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy");
        schedTimesOvr.setWbuNameBoth("JUNIT", "JUNIT");
        schedTimesOvr.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        schedTimesOvr.setEmpId(empId);
        schedTimesOvr.setStartDate(scheduleDate);
        schedTimesOvr.setEndDate(scheduleDate);
        Datetime sh1Start = DateHelper.addMinutes(scheduleDate, 9*60);
        Datetime sh1End = DateHelper.addMinutes(scheduleDate, 13*60);
        schedTimesOvr.setEmpskdActStartTime(sh1Start);
        schedTimesOvr.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(schedTimesOvr);

        // No clocks on next scheduled shift.

        // Process the overrides.
        ovrBuilder.execute( true , false );

        assertOverrideAppliedCount(ovrBuilder, 3);
        WorkDetailList wds = getWorkPremiumsForDate(empId, scheduleDate);
        logger.debug(wds.toDescription());
        // Did not work the next scheduled shift so should be a reversal premium.
        assertWorkPremiumTimeCodeMinutes(empId , scheduleDate, "HOL" , -480, new Double(25));
    }

    /**
     * @throws Exception
     */
    public void testWorkedNextScheduledWithViolation() throws Exception {

    	// The empId does not have a schedule.
    	final int empId = 3;
        Date weekStart = DateHelper.convertStringToDate("03/06/2005", "MM/dd/yyyy");
    	Date holidayDate = DateHelper.convertStringToDate("03/09/2005", "MM/dd/yyyy");

        Rule rule = new HolidayReversalRule();
        Parameters ruleparams = new Parameters();

        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_TIMECODES, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_REVERSAL_TIMECODE, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_ELIGIBLE_WORK_TIMECODES, "WRK");
        ruleparams.addParameter(HolidayReversalRule.PARAM_MAX_PAY_PERIODS, "2");
        ruleparams.addParameter(HolidayReversalRule.PARAM_REVERSE_IF_NOT_SCHEDULED, "true");
        ruleparams.addParameter(HolidayReversalRule.PARAM_VIOLATION_TIMECODES, "LATE");

        clearAndAddRule( empId, weekStart, rule, ruleparams );

        // Create Default Records
        new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                  weekStart, DateHelper.addDays(weekStart, 21) ).execute(false);

        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // Holiday
    	InsertEmployeeHolidayOverride holOvr = new InsertEmployeeHolidayOverride(getConnection());
        holOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holOvr.setEmpId(empId);
    	holOvr.setHolName(HOLNAME_MARCH9);
    	holOvr.setStartDate(holidayDate);
    	holOvr.setEndDate(holidayDate);
        ovrBuilder.add(holOvr);

        // Holiday Premium
        InsertWorkPremiumOverride holPremOvr = new InsertWorkPremiumOverride(getConnection());
        holPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holPremOvr.setEmpId(empId);
        holPremOvr.setStartDate(holidayDate);
        holPremOvr.setEndDate(holidayDate);
        holPremOvr.setWrkdTcodeName("HOL");
        holPremOvr.setWrkdHtypeName("REG");
        holPremOvr.setWrkdRate(25);
        holPremOvr.setWrkdMinutes(480);
        holPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(holPremOvr);

        // Next scheduled shift.  The last day of the max pay periods.
        InsertEmployeeScheduleOverride schedTimesOvr = new InsertEmployeeScheduleOverride(getConnection());
        Date scheduleDate = DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy");
        schedTimesOvr.setWbuNameBoth("JUNIT", "JUNIT");
        schedTimesOvr.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        schedTimesOvr.setEmpId(empId);
        schedTimesOvr.setStartDate(scheduleDate);
        schedTimesOvr.setEndDate(scheduleDate);
        Datetime sh1Start = DateHelper.addMinutes(scheduleDate, 9*60);
        Datetime sh1End = DateHelper.addMinutes(scheduleDate, 13*60);
        schedTimesOvr.setEmpskdActStartTime(sh1Start);
        schedTimesOvr.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(schedTimesOvr);

        // Clocks on next scheduled shift.
        Datetime clk1On = sh1Start;
        Datetime clk1Off = sh1End;
        InsertWorkSummaryOverride clocksOvr = new InsertWorkSummaryOverride(getConnection());
        String clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        clocksOvr.setWrksClocks(clks);
        clocksOvr.setWbuNameBoth("JUNIT", "JUNIT");
        clocksOvr.setEmpId(empId);
        clocksOvr.setStartDate(scheduleDate);
        clocksOvr.setEndDate(scheduleDate);
        ovrBuilder.add(clocksOvr);

		InsertWorkDetailOverride insWrkd = new InsertWorkDetailOverride(getConnection());
		insWrkd.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        insWrkd.setWbuNameBoth("JUNIT", "JUNIT");
        insWrkd.setEmpId(empId);
        insWrkd.setStartDate(scheduleDate);
        insWrkd.setEndDate(scheduleDate);
        insWrkd.setStartTime(DateHelper.addMinutes(scheduleDate, 9*60));
        insWrkd.setEndTime(DateHelper.addMinutes(scheduleDate, 9*60 + 15));
        insWrkd.setTcodeId(getCodeMapper().getTimeCodeByName("LATE").getTcodeId());
        insWrkd.setHtypeId(getCodeMapper().getHourTypeByName("UNPAID").getHtypeId());
        ovrBuilder.add(insWrkd);

        // Process the overrides.
        ovrBuilder.execute( true , false );

        assertOverrideAppliedCount(ovrBuilder, 5);

        // Worked the next scheduled shift, but there exists some violation time codes
        // so it does not count as worked.  There will be a reversal premium.
        WorkDetailList wps = getWorkPremiumsForDate(empId , scheduleDate);
        assertEquals(1, wps.size());
    }

   /**
    * TT1171 Testing
    * @throws Exception
    */
    
    public void testConsecutiveHolidayWorkedNextScheduled() throws Exception {
    	   
    	logger.debug("Testing: testConsecutiveHolidayWorkedNextScheduled");

       	// The empId does not have a schedule.
       	final int empId = 3;
        Date weekStart = DateHelper.convertStringToDate("03/06/2005", "MM/dd/yyyy");
       	Date holidayDate = DateHelper.convertStringToDate("03/09/2005", "MM/dd/yyyy");
       	Date consecHolidayDate = DateHelper.convertStringToDate("03/08/2005", "MM/dd/yyyy");

        Rule rule = new HolidayReversalRule();
        Parameters ruleparams = new Parameters();

           ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_TIMECODES, "HOL");
           ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_REVERSAL_TIMECODE, "HOL");
           ruleparams.addParameter(HolidayReversalRule.PARAM_CHECK_FOR_CONSEC_HOLIDAY, "true");
           ruleparams.addParameter(HolidayReversalRule.PARAM_CONSEC_HOLIDAY_REVERSAL_TIMECODE, "HOL");
           ruleparams.addParameter(HolidayReversalRule.PARAM_ELIGIBLE_WORK_TIMECODES, "WRK");
           ruleparams.addParameter(HolidayReversalRule.PARAM_MAX_PAY_PERIODS, "2");
           ruleparams.addParameter(HolidayReversalRule.PARAM_REVERSE_IF_NOT_SCHEDULED, "true");
           //ruleparams.addParameter(HolidayReversalRule.PARAM_INSERT_NEGATIVE_MINUTES, "false");

           clearAndAddRule( empId, weekStart, rule, ruleparams );

           // Create Default Records
           new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                     weekStart, DateHelper.addDays(weekStart, 21) ).execute(false);

           // Create the required overrides.
           OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

           // Holiday
           InsertEmployeeHolidayOverride holOvr = new InsertEmployeeHolidayOverride(getConnection());
           holOvr.setWbuNameBoth("JUNIT", "JUNIT");
           holOvr.setEmpId(empId);
       		holOvr.setHolName(HOLNAME_MARCH9);
       	holOvr.setStartDate(holidayDate);
       	holOvr.setEndDate(holidayDate);
           ovrBuilder.add(holOvr);
           
           //create consectuive holiday override
           InsertEmployeeHolidayOverride consHolOvr = new InsertEmployeeHolidayOverride(getConnection());
           consHolOvr.setWbuNameBoth("JUNIT", "JUNIT");
           consHolOvr.setEmpId(empId);
           consHolOvr.setHolName(HOLNAME_MARCH8);
           consHolOvr.setStartDate(consecHolidayDate);
           consHolOvr.setEndDate(consecHolidayDate);
           ovrBuilder.add(consHolOvr);

            
           // Holiday Premium
           InsertWorkPremiumOverride holPremOvr = new InsertWorkPremiumOverride(getConnection());
           holPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
           holPremOvr.setEmpId(empId);
           holPremOvr.setStartDate(holidayDate);
           holPremOvr.setEndDate(holidayDate);
           holPremOvr.setWrkdTcodeName("HOL");
           holPremOvr.setWrkdHtypeName("REG");
           holPremOvr.setWrkdRate(25);
           holPremOvr.setWrkdMinutes(480);
           holPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
           ovrBuilder.add(holPremOvr);
           //add consecutive holiday premium
           InsertWorkPremiumOverride consecHolPremOvr = new InsertWorkPremiumOverride(getConnection());
           consecHolPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
           consecHolPremOvr.setEmpId(empId);
           consecHolPremOvr.setStartDate(consecHolidayDate);
           consecHolPremOvr.setEndDate(consecHolidayDate);
           consecHolPremOvr.setWrkdTcodeName("HOL");
           consecHolPremOvr.setWrkdHtypeName("REG");
           consecHolPremOvr.setWrkdRate(25);
           consecHolPremOvr.setWrkdMinutes(480);
           consecHolPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
           ovrBuilder.add(consecHolPremOvr);
           
           // Next scheduled shift.  The last day of the max pay periods.
           InsertEmployeeScheduleOverride schedTimesOvr = new InsertEmployeeScheduleOverride(getConnection());
           Date scheduleDate = DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy");
           schedTimesOvr.setWbuNameBoth("JUNIT", "JUNIT");
           schedTimesOvr.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
           schedTimesOvr.setEmpId(empId);
           schedTimesOvr.setStartDate(scheduleDate);
           schedTimesOvr.setEndDate(scheduleDate);
           Datetime sh1Start = DateHelper.addMinutes(scheduleDate, 9*60);
           Datetime sh1End = DateHelper.addMinutes(scheduleDate, 13*60);
           schedTimesOvr.setEmpskdActStartTime(sh1Start);
           schedTimesOvr.setEmpskdActEndTime(sh1End);
           ovrBuilder.add(schedTimesOvr);

           // Clocks on next scheduled shift.
           Datetime clk1On = sh1Start;
           Datetime clk1Off = sh1End;
           InsertWorkSummaryOverride clocksOvr = new InsertWorkSummaryOverride(getConnection());
           String clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
           clocksOvr.setWrksClocks(clks);
           clocksOvr.setWbuNameBoth("JUNIT", "JUNIT");
           clocksOvr.setEmpId(empId);
           clocksOvr.setStartDate(scheduleDate);
           clocksOvr.setEndDate(scheduleDate);
           ovrBuilder.add(clocksOvr);

           // Process the overrides.
           ovrBuilder.execute( true , false );

           assertOverrideAppliedCount(ovrBuilder, 6);

          // Worked the next scheduled shift so should not be a reversal premium.
           WorkDetailList wps = getWorkPremiumsForDate(empId , scheduleDate);
           assertEquals(0, wps.size());	
    }
    /**********
     * TT1171 Test - consecutive holidays and the next scheduled shift is not worked, using neg min
     * 
     * This test case isn't working right now because of limitations to the testing API
     * AssertWorkPremiumTimeCodeMinutes only checks the first row so it will only check
     * for the HOL vacation code
     * 
     * But, we can see from the debug logger that the other premium has been entered correctly
     * 
     * @throws Exception
     */
    
    public void testConsecutiveHolidayNotWorkedNextScheduledNegativeMinutes() throws Exception {
        logger.debug("Testing: testConsecutiveHolidayNotWorkedNextScheduledNegativeMinutes");

        // The empId does not have a schedule.
        final int empId = 3;
        Date weekStart = DateHelper.convertStringToDate("03/06/2005", "MM/dd/yyyy");
        Date holidayDate = DateHelper.convertStringToDate("03/09/2005", "MM/dd/yyyy");
        Date consecHolidayDate = DateHelper.convertStringToDate("03/08/2005", "MM/dd/yyyy");
        
        Rule rule = new HolidayReversalRule();
        Parameters ruleparams = new Parameters();

        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_TIMECODES, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_HOLIDAY_REVERSAL_TIMECODE, "HOL");
        ruleparams.addParameter(HolidayReversalRule.PARAM_CHECK_FOR_CONSEC_HOLIDAY, "true");
        ruleparams.addParameter(HolidayReversalRule.PARAM_CONSEC_HOLIDAY_REVERSAL_TIMECODE, "VAC");
        ruleparams.addParameter(HolidayReversalRule.PARAM_ELIGIBLE_WORK_TIMECODES, "WRK");
        ruleparams.addParameter(HolidayReversalRule.PARAM_MAX_PAY_PERIODS, "2");
        ruleparams.addParameter(HolidayReversalRule.PARAM_REVERSE_IF_NOT_SCHEDULED, "true");
        ruleparams.addParameter(HolidayReversalRule.PARAM_INSERT_NEGATIVE_MINUTES, "true");
        clearAndAddRule( empId, weekStart, rule, ruleparams );

        // Create Default Records
        new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                  weekStart, DateHelper.addDays(weekStart, 21) ).execute(false);

        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // Holiday
        InsertEmployeeHolidayOverride holOvr = new InsertEmployeeHolidayOverride(getConnection());
        holOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holOvr.setEmpId(empId);
        holOvr.setHolName(HOLNAME_MARCH9);
        holOvr.setStartDate(holidayDate);
        holOvr.setEndDate(holidayDate);
        ovrBuilder.add(holOvr);
        
        //Consecutive Holiday
        InsertEmployeeHolidayOverride consecHolOvr = new InsertEmployeeHolidayOverride(getConnection());
        consecHolOvr.setWbuNameBoth("JUNIT", "JUNIT");
        consecHolOvr.setEmpId(empId);
        consecHolOvr.setHolName(HOLNAME_MARCH8);
        consecHolOvr.setStartDate(consecHolidayDate);
        consecHolOvr.setEndDate(consecHolidayDate);
        ovrBuilder.add(consecHolOvr);
        
        // Holiday Premium
        InsertWorkPremiumOverride holPremOvr = new InsertWorkPremiumOverride(getConnection());
        holPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
        holPremOvr.setEmpId(empId);
        holPremOvr.setStartDate(holidayDate);
        holPremOvr.setEndDate(holidayDate);
        holPremOvr.setWrkdTcodeName("HOL");
        holPremOvr.setWrkdHtypeName("REG");
        holPremOvr.setWrkdRate(25);
        holPremOvr.setWrkdMinutes(480);
        holPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(holPremOvr);
        //add consecutive holiday premium
        InsertWorkPremiumOverride consecHolPremOvr = new InsertWorkPremiumOverride(getConnection());
        consecHolPremOvr.setWbuNameBoth("JUNIT", "JUNIT");
        consecHolPremOvr.setEmpId(empId);
        consecHolPremOvr.setStartDate(consecHolidayDate);
        consecHolPremOvr.setEndDate(consecHolidayDate);
        consecHolPremOvr.setWrkdTcodeName("HOL");
        consecHolPremOvr.setWrkdHtypeName("REG");
        consecHolPremOvr.setWrkdRate(25);
        consecHolPremOvr.setWrkdMinutes(480);
        consecHolPremOvr.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(consecHolPremOvr);
        
        // Next scheduled shift.
        InsertEmployeeScheduleOverride schedTimesOvr = new InsertEmployeeScheduleOverride(getConnection());
        Date scheduleDate = DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy");
        schedTimesOvr.setWbuNameBoth("JUNIT", "JUNIT");
        schedTimesOvr.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        schedTimesOvr.setEmpId(empId);
        schedTimesOvr.setStartDate(scheduleDate);
        schedTimesOvr.setEndDate(scheduleDate);
        Datetime sh1Start = DateHelper.addMinutes(scheduleDate, 9*60);
        Datetime sh1End = DateHelper.addMinutes(scheduleDate, 13*60);
        schedTimesOvr.setEmpskdActStartTime(sh1Start);
        schedTimesOvr.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(schedTimesOvr);

        // No clocks on next scheduled shift.

        // Process the overrides.
        ovrBuilder.execute( true , false );

        assertOverrideAppliedCount(ovrBuilder, 5);
        WorkDetailList wds2 = getWorkDetailsForDate(empId, scheduleDate);
        WorkDetailList wds = getWorkPremiumsForDate(empId, scheduleDate);
        logger.debug(wds.toDescription());
        logger.debug(wds2.toDescription());
        // Did not work the next scheduled shift so should be a reversal premium.
        assertWorkPremiumTimeCodeMinutes(empId , scheduleDate, "HOL" , -480, new Double(25));
    }
    
    protected void setUp() throws Exception {
        super.setUp();
        // *** set up HOLIDAY data
        PreparedStatement ps = null;
        int holId = getConnection().getDBSequence("seq_stskltyp_id").getNextValue();
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO holiday (hol_id, hol_name, hol_date, lms_id) VALUES (?,?,?, ?)");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1  , holId );
            ps.setString(2, HOLNAME_MARCH9);
            ps.setTimestamp(3, new java.sql.Timestamp(DateHelper.convertStringToDate("03/09/2005", "MM/dd/yyyy").getTime()) );
            ps.setInt(4, 1);
            int upd = ps.executeUpdate();          
        }
        finally {
            if (ps != null) ps.close();
        }

        //****TT1171: Set up consecutive holiday data*******
        PreparedStatement ps2 = null;
        holId = getConnection().getDBSequence("seq_stskltyp_id").getNextValue();
        try {
            StringBuffer sb2 = new StringBuffer(200);
            sb2.append("INSERT INTO holiday (hol_id, hol_name, hol_date, lms_id) VALUES (?,?,?, ?)");
            ps2 = getConnection().prepareStatement(sb2.toString());
            ps2.setInt(1  , holId );
            ps2.setString(2, HOLNAME_MARCH8);
            ps2.setTimestamp(3, new java.sql.Timestamp(DateHelper.convertStringToDate("03/08/2005", "MM/dd/yyyy").getTime()) );
            ps2.setInt(4, 1);
            int upd2 = ps2.executeUpdate();          
        }
        finally {
            if (ps2 != null) ps2.close();
        }
        //****************************************************

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // *** remove holiday data
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            //TT1171: Delete both created holidays
            //sb.append("DELETE FROM holiday WHERE hol_name = ?");
            sb.append("DELETE FROM holiday WHERE hol_name IN (?, ?)");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setString(1  , HOLNAME_MARCH9);
            ps.setString(2, HOLNAME_MARCH8);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }


        getConnection().commit();
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
