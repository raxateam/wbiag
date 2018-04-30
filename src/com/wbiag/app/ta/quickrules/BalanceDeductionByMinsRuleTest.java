package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for BalanceDeductionByMinsRuleTest.
 */
public class BalanceDeductionByMinsRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BalanceDeductionByMinsRuleTest.class);

    public BalanceDeductionByMinsRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(BalanceDeductionByMinsRuleTest.class);
        BasicConfigurator.configure();
        return result;
    }


    /**
     * Scenario 1.34.4.1
     * @throws Exception
     */
    public void testDed() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String balName = "VACATION"; final int balId = 0;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new BalanceDeductionByMinsRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_TIMECODE_LIST, "TRN");
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_AFFECTS_BALANCE_LIST, balName);
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_AFFECTS_BALANCE_RATIO, "-1");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeBalanceOverride ins = new InsertEmployeeBalanceOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setEmpbalActionSET();
        ins.setEmpbalValue(10);
        ins.setBalName(balName);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start , 9*60));
        ins2.setEndTime(DateHelper.addMinutes(start , 15*60 + 30));
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        assertEquals(3.5 ,
                     getEmployeeBalanceValueAsOfEndOfDate(empId , start, balId),
                     0);

    }

    /**
     * @throws Exception
     */
    public void testDedError() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String balName = "VACATION"; final int balId = 0;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new BalanceDeductionByMinsRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_TIMECODE_LIST, "TRN,VAC");
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_AFFECTS_BALANCE_LIST, balName);
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_AFFECTS_BALANCE_RATIO, "-1");

        clearAndAddRule(empId , start , rule , ruleparams);

        RuleEngine.runCalcGroup(getConnection() , empId, start, start, false);
        assertEquals(BalanceDeductionByMinsRule.ERRMSG_NOT_SAME_TOKENS ,
                     getWorkSummaryForDate(empId, start).getWrksError());

        ruleparams = new Parameters();
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_TIMECODE_LIST, "VAC");
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_AFFECTS_BALANCE_LIST, balName);
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_AFFECTS_BALANCE_RATIO, "-1");

        clearAndAddRule(empId , start , rule , ruleparams);
        RuleEngine.runCalcGroup(getConnection() , empId, start, start, false);
        assertTrue(getWorkSummaryForDate(empId, start).getWrksError().startsWith(BalanceDeductionByMinsRule.ERRMSG_DBL_AFFECT.toPattern().substring(0 , 10)  ) );

    }

    /**
     * Scenario 1.34.4.3
     * @throws Exception
     */
    public void testDedEmpVal() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String balName = "VACATION"; final int balId = 0;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new BalanceDeductionByMinsRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_TIMECODE_LIST, "TRN");
        ruleparams.addParameter(BalanceDeductionByMinsRule.PARAM_AFFECTS_BALANCE_LIST, balName);
        ruleparams.addParameter(
            BalanceDeductionByMinsRule.PARAM_AFFECTS_BALANCE_RATIO,
            "-1 / emp_val1");

        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeOverride insE = new InsertEmployeeOverride(getConnection());
        insE.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insE.setEmpId(empId);
        insE.setStartDate(start);      insE.setEndDate(start);
        insE.setEmpVal1("8");
        ovrBuilder.add(insE);

        InsertEmployeeBalanceOverride ins = new InsertEmployeeBalanceOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setEmpbalActionSET();
        ins.setEmpbalValue(10);
        ins.setBalName(balName);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setStartTime(DateHelper.addMinutes(start , 9*60));
        ins2.setEndTime(DateHelper.addMinutes(start , 13*60));
        ins2.setWrkdTcodeName("TRN");
        ovrBuilder.add(ins2);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        assertEquals(9.5 ,
                     getEmployeeBalanceValueAsOfEndOfDate(empId , start, balId),
                     0);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
