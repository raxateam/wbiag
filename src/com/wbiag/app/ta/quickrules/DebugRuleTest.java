package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.*;
import com.wbiag.app.ta.model.*;
import com.wbiag.app.ta.ruleengine.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for DebugRuleTest.
 */
public class DebugRuleTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(DebugRuleTest.class);

    public DebugRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(DebugRuleTest.class);
        BasicConfigurator.configure();
        return result;
    }


    /**
     * @throws Exception
     */
    public void testErr() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventCalcLog");

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** create the rule
        Rule rule = new ErrorRule();
        Parameters ruleparams = new Parameters();
        ruleparams = new Parameters();
        ruleparams.addParameter(ErrorRule.PARAM_ERROR_MESSAGE, "Test");
        clearAndAddRule(empId , start , rule , ruleparams);

        rule = new DebugRule();
        ruleparams = new Parameters();
        ruleparams.addParameter(DebugRule.PARAM_MESSAGE, "After Error Rule");
        ruleparams.addParameter(DebugRule.PARAM_LOG_DETAILS, "true");
        ruleparams.addParameter(DebugRule.PARAM_LOG_WORK_SUMMARY, "true");
        ruleparams.addParameter(DebugRule.PARAM_LOG_BALANCE_NAMES, "VACATION");
        clearAndAddRule(empId, start, rule, ruleparams);



        RuleEngine.runCalcGroup(getConnection() , empId, start, start, false);

        WorkSummaryData ws = getWorkSummaryForDate(empId , start);

        List calcLogs = new RecordAccess(getConnection()).loadRecordData(new CalcLogData(),
            "CALC_LOG", "wrks_id", ws.getWrksId() );
        System.out.println(calcLogs);
        assertTrue(calcLogs.size() > 0);

    }


     public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
