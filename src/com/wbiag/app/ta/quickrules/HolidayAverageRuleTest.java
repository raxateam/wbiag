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
 * Test for HolidayAverageRuleTest.
 */
public class HolidayAverageRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HolidayAverageRuleTest.class);

    public HolidayAverageRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HolidayAverageRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testRangeOvertimeHourset() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String prem = "TRN";
        Date wrkDate = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");
        Date start = DateHelper.addDays(wrkDate , -14);
        Date end = DateHelper.addDays(wrkDate , -1);

        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start, wrkDate).execute(false);
        // *** create the rule
        Rule rule = new HolidayAverageRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(HolidayAverageRule.PARAM_NUMBER_OF_DAYS, "14");
        ruleparams.addParameter(HolidayAverageRule.PARAM_AVG_TIME_CODES, "WRK");
        ruleparams.addParameter(HolidayAverageRule.PARAM_HOLIDAY_PREMIUM_TIMECODE, prem);
        ruleparams.addParameter(HolidayAverageRule.PARAM_DETAIL_PREMIUM, WorkDetailData.DETAIL_TYPE);
        clearAndAddRule(empId , start , rule , ruleparams);

        RuleEngine.runCalcGroup(getConnection() , empId, wrkDate, wrkDate, false);
        assertRuleApplied(empId, wrkDate, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , wrkDate);
       assertWorkPremiumTimeCodeMinutes(empId, wrkDate, prem, 480);
    }
    
    //#2539477 - Tesing added new parameter MaximumPremiumMinutes
    public void testMaximumPremiumMinutes() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String prem = "TRN";
        Date wrkDate = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");
        Date start = DateHelper.addDays(wrkDate , -14);
        Date end = DateHelper.addDays(wrkDate , -1);

        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start, wrkDate).execute(false);
        // *** create the rule
        Rule rule = new HolidayAverageRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(HolidayAverageRule.PARAM_NUMBER_OF_DAYS, "14");
        ruleparams.addParameter(HolidayAverageRule.PARAM_AVG_TIME_CODES, "WRK");
        ruleparams.addParameter(HolidayAverageRule.PARAM_HOLIDAY_PREMIUM_TIMECODE, prem);
        ruleparams.addParameter(HolidayAverageRule.PARAM_DETAIL_PREMIUM, WorkDetailData.DETAIL_TYPE);
        
        ruleparams.addParameter(HolidayAverageRule.PARAM_MAXIMUM_PREMIUM_MINUTES, "420");
        
        clearAndAddRule(empId , start , rule , ruleparams);

        RuleEngine.runCalcGroup(getConnection() , empId, wrkDate, wrkDate, false);
        assertRuleApplied(empId, wrkDate, rule);

        WorkDetailList wps = getWorkPremiumsForDate(empId , wrkDate);
       assertWorkPremiumTimeCodeMinutes(empId, wrkDate, prem, 480);
    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
