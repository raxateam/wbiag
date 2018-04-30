package com.wbiag.app.ta.quickrules;

import java.util.Date;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.quickrules.ApplyPayRateRule;
import com.workbrain.app.ta.quickrules.WeeklyOvertimeRule;
import com.workbrain.app.ta.ruleengine.CreateDefaultRecords;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleEngine;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.app.ta.ruleengine.RuleTestCase;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.DateHelper;
import com.workbrain.util.Datetime;

import junit.framework.*;

/**
 * @author wwoo
 *
 * Test for FLSAWeeklyOvertimeRule.
 */
public class FLSAWeeklyOTRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RoundingDurationRuleTest.class);

    public FLSAWeeklyOTRuleTest(String testName) throws Exception {
        super( testName );
    }

    public static TestSuite suite() {
    	BasicConfigurator.configure();
        TestSuite result = new TestSuite();
        result.addTestSuite(FLSAWeeklyOTRuleTest.class);
        return result;
    }

    /**
     * Business Scenario 1 in \\Toriag\IAGSpecs\TimeAttendance\WBIAG_QuickRules_Functional.doc
     *
     * @throws Exception
     */
    public void testFLSA1() throws Exception {

        final int empId = 11;

        Date sun = DateHelper.nextDay(DateHelper.getCurrentDate(), "SUN");
        Date sat = DateHelper.addDays(sun , 6);
        final String jobName = "MANAGER"; int jobId = 7;
        // Create Default Records
        new CreateDefaultRecords(getConnection(), new int[] {empId}
                                 ,
                                 sun, sat).
            execute(false);
        JobRateData jrd = new JobRateData();
        jrd.setJobId(new Integer(jobId)  );
        jrd.setJobrateRate(new java.math.BigDecimal(20) );
        jrd.setJobrateEffectiveDate(DateHelper.DATE_1900) ;
        jrd.setJobrateIndex(new Integer(1));
        new JobRateAccess(getConnection()).insert(jrd);

        //*** create job with rank 1
        EmployeeJobData ejd = new EmployeeJobData();
        ejd.setEmpjobId(getConnection().getDBSequence(EmployeeJobAccess.EMP_JOB_SEQ).getNextValue());
        ejd.setEmpId(empId );
        ejd.setJobId(7);
        ejd.setEmpjobStartDate(DateHelper.DATE_1900);
        ejd.setEmpjobEndDate(DateHelper.DATE_3000);
        ejd.setEmpjobRank(1);
        ejd.setEmpjobRateIndex(1);
        new EmployeeJobAccess(getConnection()).insert(ejd);


        Parameters ruleparams = null;
        Rule rule = null;

        // Create Weekly OT Rule
        rule = new WeeklyOvertimeRule();
        ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeRule.PARAM_HOURSET_DESCRIPTION, "REG=2400,OT2=9999");
        ruleparams.addParameter(WeeklyOvertimeRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        ruleparams.addParameter(WeeklyOvertimeRule.PARAM_WORKDETAIL_TIMECODES, "");
        ruleparams.addParameter(WeeklyOvertimeRule.PARAM_DAY_WEEK_STARTS, "MON");
        clearAndAddRule( empId, sun, rule, ruleparams );


        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // *** set empbase rate to 10
        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmp.setEmpId(empId);
        insEmp.setStartDate(DateHelper.DATE_1900);      insEmp.setEndDate(DateHelper.DATE_3000);
        insEmp.setEmpBaseRate(10);
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        InsertWorkSummaryOverride wsOvr = new InsertWorkSummaryOverride(getConnection());
        Datetime clk1On = DateHelper.addMinutes(sat, 9*60);
        Datetime clk1Off = DateHelper.addMinutes(sat, 19*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off);
        wsOvr.setWrksClocks(clks);
        wsOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        wsOvr.setEmpId(empId);
        wsOvr.setStartDate(sat);
        wsOvr.setEndDate(sat);
        ovrBuilder.add(wsOvr);

        // ECHO - Premium override on sun for
        InsertWorkPremiumOverride premiumOverride = new InsertWorkPremiumOverride(getConnection());
        premiumOverride.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        premiumOverride.setEmpId(empId);
        premiumOverride.setStartDate(sun);
        premiumOverride.setEndDate(sun);
        premiumOverride.setWrkdTcodeName("TRN");
        premiumOverride.setWrkdHtypeName("REG");
        premiumOverride.setWrkdRate(25);
        premiumOverride.setWrkdMinutes(20*60);
        premiumOverride.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(premiumOverride);

        // Process the overrides.
        ovrBuilder.execute( true , false );

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 2);

        RuleEngine.runCalcGroup(getConnection(), empId,
                                    sun,
                                    sat,
                                    false);

        // Create the FLSA rule.
        rule = new FLSAWeeklyOTRule();
        ruleparams = new Parameters();
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_MUST_WORKED_MINUTES, "2400");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_FLSA_HOURS_TIMECODES, "WRK");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_FLSA_HOURS_HOURTYPES, "");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_FLSA_DOLLARS_TIMECODES, "WRK,TRN");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_FLSA_DOLLARS_HOURTYPES, "");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_OT_EARNED_TIMECODES, "WRK");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_OT_EARNED_HOURTYPES, "OT2");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_PREMIUM_TIMECODE, "TRN");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_PREMIUM_HOURTYPE, "REG");
        addRule( empId, sat, rule, ruleparams );

        RuleEngine.runCalcGroup(getConnection(), empId,
                                    sat,
                                    sat,
                                    false);

        // Ensure the premium is paid at $100
        assertWorkPremiumTimeCodeMinutes(empId , sat, "TRN" , 60, new Double(25));
    }

    /**
     * From Lifespan test case 6.3.2-1
     * FLSA adjustment should be $3.84
     * Using dates 3/27 - 4/02
     *
     * @throws Exception
     */
    public void xFLSAWeeklyOTRuleAdjustment() throws Exception {

    	// The employee does not have a schedule, and requireS a weekly OT
    	// rule for REG=2400,OT1=9999
        final int empId = 30710;
        String clks = null;
        InsertWorkSummaryOverride wsOvr = null;

        Date currentDate = DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy");
        Datetime clk1On = DateHelper.parseDate("03/27/2005 08:00", "MM/dd/yyyy HH:mm");
        Datetime clk1Off = DateHelper.parseDate("03/27/2005 12:00", "MM/dd/yyyy HH:mm");
        Datetime clk2On = DateHelper.parseDate("03/27/2005 12:30", "MM/dd/yyyy HH:mm");
        Datetime clk2Off = DateHelper.parseDate("03/27/2005 16:30", "MM/dd/yyyy HH:mm");

        Parameters ruleparams = null;
        Rule rule = null;


        // Create Weekly OT Rule
        rule = new WeeklyOvertimeRule();
        ruleparams = new Parameters();
        ruleparams.addParameter(WeeklyOvertimeRule.PARAM_HOURSET_DESCRIPTION, "REG=2400,OT1=9999");
        ruleparams.addParameter(WeeklyOvertimeRule.PARAM_ELIGIBLE_HOURTYPES, "REG,EVENT");
        ruleparams.addParameter(WeeklyOvertimeRule.PARAM_WORKDETAIL_TIMECODES, "");
        ruleparams.addParameter(WeeklyOvertimeRule.PARAM_DAY_WEEK_STARTS, "SUNDAY");
        clearAndAddRule( empId, currentDate, rule, ruleparams );

        // Create Apply Rates rule
        rule = new ApplyPayRateRule();
        ruleparams = new Parameters();
        ruleparams.addParameter(ApplyPayRateRule.PARAM_USE_RATE_TYPE, ApplyPayRateRule.RATE_TYPE_JOB);
        ruleparams.addParameter(ApplyPayRateRule.PARAM_USE_RATE_MODE, RuleHelper.GREATEST_FLAG);
        addRule( empId, currentDate, rule, ruleparams );

        // Create the FLSA rule.
        rule = new FLSAWeeklyOTRule();
        ruleparams = new Parameters();
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_MUST_WORKED_MINUTES, "2400");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_FLSA_HOURS_TIMECODES, "WRK,LEFTL,INERLY,REFR-H,EVTHRS,CALL");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_FLSA_HOURS_HOURTYPES, "");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_FLSA_DOLLARS_TIMECODES, "WRK,LEFTL,INERLY,REFR-H,ECHO,EVE,NIGHT,CALL,GUAR,TRAV");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_FLSA_DOLLARS_HOURTYPES, "");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_OT_EARNED_TIMECODES, "WRK,DBL,LEFTL,INERLY,CALL,TRAV,GUAR");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_OT_EARNED_HOURTYPES, "OT1,OT2,REG1.5");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_PREMIUM_TIMECODE, "FLSA");
        ruleparams.addParameter(FLSAWeeklyOTRule.PARAM_PREMIUM_HOURTYPE, "REG");
        addRule( empId, currentDate, rule, ruleparams );

        // Create Default Records
        new CreateDefaultRecords( getConnection() , new int[]{ empId },
                                  currentDate, DateHelper.addDays(currentDate, 6) ).execute(false);

        // Create the required overrides.
        OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );

        // Create the same clocks for Monday to Friday
        for (int i=1; i <= 5; i++) {

	        // Set the clocks to the next day.
	        currentDate = DateHelper.addDays(currentDate, 1);
	        clk1On = DateHelper.addDays(clk1On, 1);
	        clk1Off = DateHelper.addDays(clk1Off, 1);
	        clk2On = DateHelper.addDays(clk2On, 1);
	        clk2Off = DateHelper.addDays(clk2Off, 1);

	        wsOvr = new InsertWorkSummaryOverride(getConnection());

	        clks = createWorkSummaryClockStringForOnOffs(clk1On,clk1Off,clk2On,clk2Off);
	        wsOvr.setWrksClocks(clks);
	        wsOvr.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
	        wsOvr.setEmpId(empId);
	        wsOvr.setStartDate(currentDate);
	        wsOvr.setEndDate(currentDate);

	        ovrBuilder.add(wsOvr);
        }

        // EVTHRS - Work Detail override for 3 hours @ $0
        InsertWorkDetailOverride wdOverride = new InsertWorkDetailOverride(getConnection());
        Date tuesday = DateHelper.convertStringToDate("03/29/2005", "MM/dd/yyyy");
        wdOverride.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        wdOverride.setEmpId(empId);
        wdOverride.setStartDate(tuesday);
        wdOverride.setEndDate(tuesday);
        wdOverride.setStartTime(DateHelper.parseDate("03/29/2005 16:30", "MM/dd/yyyy HH:mm"));
        wdOverride.setEndTime(DateHelper.parseDate("03/29/2005 19:30", "MM/dd/yyyy HH:mm"));
        wdOverride.setWrkdTcodeName("EVTHRS");
        wdOverride.setWrkdHtypeName("REG");
        wdOverride.setWrkdRate(0);
        wdOverride.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ovrBuilder.add(wdOverride);

        // ECHO - Premium override on Tusday for 8hrs @ $25
        InsertWorkPremiumOverride premiumOverride = new InsertWorkPremiumOverride(getConnection());
        premiumOverride.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        premiumOverride.setEmpId(empId);
        premiumOverride.setStartDate(tuesday);
        premiumOverride.setEndDate(tuesday);
        premiumOverride.setWrkdTcodeName("ECHO");
        premiumOverride.setWrkdHtypeName("REG");
        premiumOverride.setWrkdRate(25);
        premiumOverride.setWrkdMinutes(480);
        premiumOverride.setOvrType(OverrideData.POSTCALC_WORK_PREMIUM_TYPE_START);
        ovrBuilder.add(premiumOverride);

        // Process the overrides.
        ovrBuilder.execute( true , false );

        // Ensure all overrides were applied.
        assertOverrideAppliedCount(ovrBuilder , 7);

        RuleEngine.runCalcGroup(getConnection(), empId,
        							DateHelper.convertStringToDate("03/27/2005", "MM/dd/yyyy"),
									DateHelper.convertStringToDate("04/02/2005", "MM/dd/yyyy"),
									false);

		// Ensure the premium is paid at $3.84
        Date saturday = DateHelper.convertStringToDate("04/02/2005", "MM/dd/yyyy");
        assertWorkPremiumTimeCodeMinutes(empId , saturday, "FLSA" , 60, new Double(3.84));
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run( suite() );
    }
}
