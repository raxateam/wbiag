package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for BalanceTransferRuleTest.
 */
public class BalanceTransferRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BalanceTransferRuleTest.class);

    public BalanceTransferRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(BalanceTransferRuleTest.class);
        BasicConfigurator.configure();
        return result;
    }


    /**
     * @throws Exception
     */
    public void testBT() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String balFrom = "VACATION"; final int balFromId = 0;
        final String balTo = "SICK"; ; final int balToId = 1;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new BalanceTransferRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(BalanceTransferRule.PARAM_BALANCE_TRANSFER_FROM, balFrom);
        ruleparams.addParameter(BalanceTransferRule.PARAM_BALANCE_TRANSFER_TO, balTo);
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeBalanceOverride ins = new InsertEmployeeBalanceOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setEmpbalActionSET();
        ins.setEmpbalValue(10);
        ins.setBalName(balFrom);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        assertEquals(0 ,
                     getEmployeeBalanceValueAsOfEndOfDate(empId , start, balFromId),
                     0);
        assertEquals(10,
                     getEmployeeBalanceValueAsOfEndOfDate(empId , start, balToId),
                     0);

    }

    /**
     * @throws Exception
     */
    public void testBTDeduct() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        final String balFrom = "VACATION"; final int balFromId = 0;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new BalanceTransferRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(BalanceTransferRule.PARAM_BALANCE_TRANSFER_FROM, balFrom);
        ruleparams.addParameter(BalanceTransferRule.PARAM_BALANCE_TRANSFER_TO, BalanceTransferRule.PARAM_VAL_BTO_DEDUCT);
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertEmployeeBalanceOverride ins = new InsertEmployeeBalanceOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setEmpbalActionSET();
        ins.setEmpbalValue(10);
        ins.setBalName(balFrom);
        ovrBuilder.add(ins);

        ovrBuilder.execute(true , false);

        assertRuleApplied(empId, start, rule);

        assertEquals(0 ,
                     getEmployeeBalanceValueAsOfEndOfDate(empId , start, balFromId),
                     0);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
