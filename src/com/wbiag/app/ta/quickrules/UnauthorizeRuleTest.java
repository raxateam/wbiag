package com.wbiag.app.ta.quickrules;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.quickrules.*;
import com.workbrain.app.ta.conditions.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import junit.framework.*;
import com.workbrain.test.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.server.registry.*;
import com.workbrain.server.*;
/**
 * Test for UnuthorizeRule.
 */
public class UnauthorizeRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UnauthorizeRuleTest.class);

    public UnauthorizeRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(UnauthorizeRuleTest.class);
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

        Rule rule = new UnauthorizeRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(UnauthorizeRule.PARAM_UNAUTHORIZE_MESSAGE, "TEST");
        clearAndAddRule(empId , start , rule , ruleparams , ExecutionPointHelper.EXECPOINT_AUTHORIZATION);

        RuleEngine.runCalcGroup(getConnection() , empId , start, start, false, true);

        WorkSummaryData wsd = getWorkSummaryForDate(empId , start);
        assertRuleApplied(wsd , rule);
        assertFalse(wsd.isAuthorized());
        assertEquals(wsd.getWrksMessages() , "TEST");
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
