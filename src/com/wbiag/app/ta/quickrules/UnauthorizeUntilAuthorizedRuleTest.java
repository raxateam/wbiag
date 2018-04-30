package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for UnauthorizeUntilAuthorizedRule.
 */
public class UnauthorizeUntilAuthorizedRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UnauthorizeUntilAuthorizedRuleTest.class);

    public UnauthorizeUntilAuthorizedRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(UnauthorizeUntilAuthorizedRuleTest.class);
        return result;
    }


    /**
     *
     * @throws Exception
     */
    public void testUnauthorize() throws Exception {
        DBConnection c = getConnection();

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");

        Rule rule = new UnauthorizeUntilAuthorizedRule();
        Parameters ruleparams = new Parameters();
        clearAndAddRule(empId , start , rule , ruleparams , ExecutionPointHelper.EXECPOINT_AUTHORIZATION);

        RuleEngine.runCalcGroup(getConnection() , empId , start, start, false, true);

        WorkSummaryData wsd = getWorkSummaryForDate(empId , start);
        assertRuleApplied(wsd , rule);
        assertFalse(wsd.isAuthorized());

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
