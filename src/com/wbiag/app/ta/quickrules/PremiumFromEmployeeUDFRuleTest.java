package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;

import junit.framework.*;

/**
 * Test for PremiumFromEmployeeUDFRuleTest.
 */

public class PremiumFromEmployeeUDFRuleTest extends RuleTestCase 
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(PremiumFromEmployeeUDFRuleTest.class);

    public PremiumFromEmployeeUDFRuleTest(String testName) 
        throws Exception 
    {
        super(testName);
    }

    public static TestSuite suite() 
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(PremiumFromEmployeeUDFRuleTest.class);
        return result;
    }


    public void testPremiumFromEmployeeUDFRule() 
        throws Exception 
    {
        final int empId = 15;        
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        
        // *** create the rule
        Rule rule = new PremiumFromEmployeeUDFRule();
        Parameters ruleparams = new Parameters();
        
        ruleparams.addParameter(PremiumFromEmployeeUDFRule.PARAM_PREMIUM_TIME_CODE, "WRK");
        ruleparams.addParameter(PremiumFromEmployeeUDFRule.PARAM_EMPLOYEE_UDF, "JUnitTestPremiumFromEmployeeUDFRule");
        ruleparams.addParameter(PremiumFromEmployeeUDFRule.PARAM_DIVISOR, "2");
        
        clearAndAddRule(empId , start , rule , ruleparams);
        RuleEngine.runCalcGroup(getConnection() , empId , start, start ,false, true);
        assertRuleApplied(empId, start, rule);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

