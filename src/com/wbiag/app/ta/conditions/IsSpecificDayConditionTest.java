package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.quickrules.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for IsSpecificDayCondition.
 */
public class IsSpecificDayConditionTest extends RuleTestCase {

    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsSpecificDayConditionTest.class);

    public IsSpecificDayConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsSpecificDayConditionTest.class);
        return result;
    }


    /**
     * Tests if IsSpecificDayCondition
     * @throws Exception
     */
    public void testIsSpecificDayCondition() throws Exception {

        final int empId = 15;
        Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        // *** create condition to evaluate TRUE
        Condition condition = new IsSpecificDayCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(IsSpecificDayCondition.PARAM_DAYS_OF_WEEK, "MONDAY,TUESDAY");
        assertConditionTrue(empId, start, condition, condParams);
        // *** create condition to evaluate FALSE
        condParams.removeAllParameters();
        condParams.addParameter(IsSpecificDayCondition.PARAM_DAYS_OF_WEEK, "WEDNESDAY,FRIDAY");
        assertConditionFalse(empId, start, condition, condParams);
        // *** create condition to evaluate TRUE for PARAM_INCLUSIVE
        condParams.removeAllParameters();
        condParams.addParameter(IsSpecificDayCondition.PARAM_DAYS_OF_WEEK, "WEDNESDAY,FRIDAY");
        condParams.addParameter(IsSpecificDayCondition.PARAM_INCLUSIVE, "FALSE");
        assertConditionTrue(empId, start, condition, condParams);
    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
