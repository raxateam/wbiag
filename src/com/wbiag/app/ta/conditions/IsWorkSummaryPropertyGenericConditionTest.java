package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for IsWorkSummaryPropertyGenericTest.
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class IsWorkSummaryPropertyGenericConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsWorkSummaryPropertyGenericConditionTest.class);

    public IsWorkSummaryPropertyGenericConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsWorkSummaryPropertyGenericConditionTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testWrks() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 15;
        final String jobName = "JANITOR";
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start,
                                 start).execute(false);
        // *** create the rule
        Condition cond = new IsWorkSummaryPropertyGenericCondition();
        Parameters params = new Parameters();
        params.addParameter(IsWorkSummaryPropertyGenericCondition.PARAM_EXPRESSION_STRING, "wrksFlag1" + RuleHelper.IS_EMPTY);

        System.out.println(getWorkSummaryForDate(empId , start).getWrksFlag1());
        assertConditionTrue(empId , start , cond , params);

        params.removeAllParameters();
        params.addParameter(IsWorkSummaryPropertyGenericCondition.PARAM_EXPRESSION_STRING, "wrksFlag1=Y");
        assertConditionFalse(empId , start , cond , params);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
