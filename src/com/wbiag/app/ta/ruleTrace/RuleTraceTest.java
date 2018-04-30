package com.wbiag.app.ta.ruleTrace;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.wbiag.app.ta.ruleTrace.db.*;
import com.wbiag.app.ta.ruleTrace.model.*;
import com.wbiag.app.ta.conditions.*;
import com.wbiag.app.ta.model.*;
import com.wbiag.app.ta.quickrules.*;
import com.wbiag.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for RuleTraceTest.
 */
public class RuleTraceTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RuleTraceTest.class);

    public RuleTraceTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(RuleTraceTest.class);
        BasicConfigurator.configure();
        return result;
    }


    /**
     * @throws Exception
     */
    public void test1() throws Exception {

        setDataEventClassPath("com.wbiag.app.ta.ruleTrace.CDataEventRuleTrace");

        final int empId = 11;

        Date start = DateHelper.nextDay(DateHelper.addDays( DateHelper.getCurrentDate(), -7) , "Mon") ;
        // *** create the rule
        RuleTraceConfig cfg = RuleTraceConfig.getRuleTraceConfig(getConnection());
        assertTrue(cfg.isEnabled());
        RuleEngine.runCalcGroup(getConnection(), empId, start, start, false, true );
        // *** make sure XML is saved and parseable
        String xml = new RuleTraceAccess(getConnection()).getTraceByWrksId(getWorkSummaryForDate(empId, start).getWrksId());
        RuleTraceList rtl = RuleTraceList.fromXML(xml , cfg) ;
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
