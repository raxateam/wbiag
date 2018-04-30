package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for ErrorRuleTest.
 */
public class ErrorRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ErrorRuleTest.class);

    public ErrorRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ErrorRuleTest.class);
        BasicConfigurator.configure();
        return result;
    }


    /**
     * @throws Exception
     */
    public void testErr() throws Exception {

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new ErrorRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(ErrorRule.PARAM_ERROR_MESSAGE, "Test");
        ruleparams.addParameter(ErrorRule.PARAM_STOP_EXECUTION, "true");
        clearAndAddRule(empId , start , rule , ruleparams);

        RuleEngine.runCalcGroup(getConnection() , empId, start, start);

        WorkSummaryData ws = getWorkSummaryForDate(empId , start);
        assertTrue(ws.isError());

    }


     public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
