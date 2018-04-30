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
 * Test for WeeklyOvertimeRule.
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class SplitDetailRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SplitDetailRuleTest.class);

    public SplitDetailRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(SplitDetailRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testSplit() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        // *** create the rule
        Rule rule = new SplitDetailRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(SplitDetailRule.PARAM_SPLIT_TIME, "19000101 000000");
        clearAndAddRule(empId , start , rule , ruleparams, ExecutionPointHelper.EXECPOINT_AUTHORIZATION);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60*8);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*30);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);

        assertTrue(wds.size() == 2);
        WorkDetailData wd1 = wds.getWorkDetail(0);
        assertEquals(start, wd1.getWrkdWorkDate());
        assertEquals("REG", wd1.getWrkdHtypeName() ) ;
        assertEquals(960 , wd1.getWrkdMinutes());

        WorkDetailData wd2 = wds.getWorkDetail(1);
        assertEquals(DateHelper.addDays(start , 1), wd2.getWrkdWorkDate());
        assertEquals("REG", wd2.getWrkdHtypeName() ) ;
        assertEquals(360 , wd2.getWrkdMinutes());
        assertEquals(DateHelper.addDays(start , 1) , wd2.getWrkdWorkDate());

    }

    /**
     * @throws Exception
     */
    public void testDST() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.parseSQLDate("2006-04-02");;
        // *** create the rule
        Rule rule = new SplitDetailRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(SplitDetailRule.PARAM_SPLIT_TIME, "19000101 050000");
        clearAndAddRule(empId , start , rule , ruleparams, ExecutionPointHelper.EXECPOINT_AUTHORIZATION);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 60*1);
        Datetime clk1Off = DateHelper.addMinutes(start, 60*15);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        System.out.println(wds.toDescription() );

        assertTrue(wds.size() == 2);
        WorkDetailData wd1 = wds.getWorkDetail(0);
        assertEquals("REG", wd1.getWrkdHtypeName() ) ;
        assertEquals(180 , wd1.getWrkdMinutes());

        WorkDetailData wd2 = wds.getWorkDetail(1);
        assertEquals("REG", wd2.getWrkdHtypeName() ) ;
        assertEquals(660 , wd2.getWrkdMinutes());

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
