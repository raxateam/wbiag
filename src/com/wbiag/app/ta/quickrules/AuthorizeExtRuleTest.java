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
 * Test for  AuthorizeExtRule
 */
public class AuthorizeExtRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(AuthorizeExtRuleTest.class);

    public AuthorizeExtRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(AuthorizeExtRuleTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void test1() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);
        ovrBuilder.setRuleEngineAutoRecalc(false);
        ovrBuilder.setRuleEngineFutureBalanceRecalc(Boolean.FALSE);

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
        // *** create the rule
        Rule rule = new AuthorizeExtRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(AuthorizeExtRule.PARAM_UNAUTHORIZE_IFEDIT_AFTERAUTHORIZATION, "true");
        ruleparams.addParameter(AuthorizeExtRule.PARAM_UNAUTHORIZE_IFEDIT_AFTERAUTHORIZATION_STRING, "wbuName!=TEST");
        clearAndAddRule(empId , start , rule , ruleparams);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setWrksAuthorized("Y");
        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertOverrideAppliedOne(ovrBuilder);

        assertRuleApplied(empId, start, rule);
        Thread.sleep(100);

        InsertWorkSummaryOverride ins2 = new InsertWorkSummaryOverride(getConnection());
        ins2.setWbuNameBoth("TEST", "TEST");
        ins2.setEmpId(empId);
        ins2.setStartDate(start);      ins2.setEndDate(start);
        ins2.setWrksFlag1("Y");
        ovrBuilder.add(ins2);
        ovrBuilder.execute(true , false);

        //assertOverrideAppliedOne(ovrBuilder);

        WorkSummaryData wsd = getWorkSummaryForDate(empId , start);
        assertTrue(wsd.isAuthorized() );
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
