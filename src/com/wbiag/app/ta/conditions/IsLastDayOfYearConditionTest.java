package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;

import junit.framework.*;

/**
 * Test for IsLastDayOfYearConditionTest.
 */

public class IsLastDayOfYearConditionTest extends RuleTestCase 
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsLastDayOfYearConditionTest.class);

    public IsLastDayOfYearConditionTest(String testName) 
        throws Exception 
    {
        super(testName);
    }

    public static TestSuite suite() 
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsLastDayOfYearConditionTest.class);
        return result;
    }

    public void testLastDayOfYear() 
        throws Exception 
    {
        final int empId = 15;
        Date lastDayOfYear = DateHelper.parseDate("12/31/2000", "MM/dd/yyyy");
        Date notLastDayOfYear = DateHelper.parseDate("01/01/2000", "MM/dd/yyyy");

        Condition cond = new IsLastDayOfYearCondition();
        Parameters params = new Parameters();

        //test true case
        assertConditionTrue(empId, lastDayOfYear, cond, params);
        
        //test false case
        assertConditionFalse(empId, notLastDayOfYear, cond, params);
    }

    public static void main(String[] args) 
        throws Exception 
    {
        junit.textui.TestRunner.run(suite());
    }
}
