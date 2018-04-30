package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;

import java.util.*;

import junit.framework.*;

/**
 * Test for WorkedNMinutesPremiumConditionTest.
 */

public class WorkedNMinutesPremiumConditionTest extends RuleTestCase 
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkedNMinutesPremiumConditionTest.class);

    public WorkedNMinutesPremiumConditionTest(String testName) 
        throws Exception 
    {
        super(testName);
    }

    public static TestSuite suite() 
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkedNMinutesPremiumConditionTest.class);
        return result;
    }

    public void testWorkedNMinutesPremiumCondition() 
        throws Exception 
    {
        final int empId = 15;
        Date wrksDate = DateHelper.getCurrentDate();        

        Condition cond = new WorkedNMinutesPremiumCondition();
        
        Parameters params = new Parameters();
        params.addParameter(WorkedNMinutesPremiumCondition.PARAM_PREMIUM_MINUTES,"0");
        params.addParameter(WorkedNMinutesPremiumCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        assertConditionTrue(empId, wrksDate, cond, params);
    }

    public static void main(String[] args) 
        throws Exception 
    {
        junit.textui.TestRunner.run(suite());
    }
}
