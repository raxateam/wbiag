package com.wbiag.app.ta.conditions;

import java.util.*;

import org.apache.log4j.Logger;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;


/**
 * Test for IsWorkSummaryPropertyGenericTest.
 */
public class IsShiftPatternPropertyGenericConditionTest extends RuleTestCase {

    private static Logger logger = Logger.getLogger(IsShiftPatternPropertyGenericConditionTest.class);

    public IsShiftPatternPropertyGenericConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsShiftPatternPropertyGenericConditionTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testConditionTrue() throws Exception {

        final int empId = 15;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start,
                                 start).execute(false);

        // *** create the condition
        Condition cond = new IsShiftPatternPropertyGenericCondition();
        Parameters params = new Parameters();
        params.addParameter(IsShiftPatternPropertyGenericCondition.PARAM_EXPRESSION_STRING, "shftpatFlag1" + RuleHelper.IS_EMPTY);
        assertConditionTrue(empId , start , cond , params);
    }

    
    /**
     * @throws Exception
     */
    public void testConditionFalse() throws Exception {

        final int empId = 15;

        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start,
                                 start).execute(false);

        // *** create the condition
        Condition cond = new IsShiftPatternPropertyGenericCondition();
        Parameters params = new Parameters();
        params.addParameter(IsShiftPatternPropertyGenericCondition.PARAM_EXPRESSION_STRING, "wrksFlag1=Y");
        assertConditionFalse(empId , start , cond , params);
    }
    
    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
