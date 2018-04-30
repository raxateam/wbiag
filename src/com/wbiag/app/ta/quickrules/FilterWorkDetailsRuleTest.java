package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.quickrules.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for RemoveWorkDetailRule.
 */
public class FilterWorkDetailsRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FilterWorkDetailsRuleTest.class);

    public FilterWorkDetailsRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(FilterWorkDetailsRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testFiltert() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Tue");
        // *** create the rule
        Rule rule = new FilterWorkDetailsRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(FilterWorkDetailsRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(FilterWorkDetailsRule.PARAM_EXPRESSION_STRING, "wrkdFlag1=Y");
        clearAndAddRule(empId , start , rule , ruleparams);

        Rule rule1 = new DailyOvertimeRule();
        Parameters ruleparams1 = new Parameters();
        ruleparams1.addParameter(DailyOvertimePremiumRule.PARAM_HOURSET_DESCRIPTION, "REG=480,OT1=9999");
        //ruleparams1.addParameter(DailyOvertimePremiumRule.PARAM_ELIGIBLE_HOURTYPES, "REG");
        //ruleparams1.addParameter(DailyOvertimePremiumRule.PARAM_WORKDETAIL_TIMECODES, "WRK");
        addRule(empId , start , rule1 , ruleparams1);

        Rule rule2 = new UnFilterWorkDetailsRule();
        addRule(empId , start , rule2 , new Parameters());


        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        Datetime clk1On = DateHelper.addMinutes(start, 9*60);
        Datetime clk1Off = DateHelper.addMinutes(start, 20*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins.setWrksClocks(clks);

        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start, 9*60));
        ins2.setEndTime(DateHelper.addMinutes(start, 18*60));
        ins2.setWrkdFlag1("Y");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);


        assertRuleApplied(empId, start, rule);

        WorkDetailList wds = getWorkDetailsForDate(empId , start);
        //System.out.println(wds.toDescription());
        assertEquals(60 , wds.getMinutes(null , null, "WRK", true, "OT1", true));
        //sassertEquals(180 , wds.getMinutes(null , null, null, true, "OT2", true));
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
