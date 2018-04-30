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
 * Test for IsSpecificMonthCondition.
 */
public class IsSpecificMonthConditionTest extends RuleTestCase {

    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsSpecificMonthConditionTest.class);

    public IsSpecificMonthConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsSpecificMonthConditionTest.class);
        return result;
    }


    /**
     * Tests if IsSpecificMonthCondition
     * @throws Exception
     */
    public void testIsSpecificMonthCondition() throws Exception {

        final int empId = 15;
        Date start = DateHelper.getUnitYear(DateHelper.APPLY_ON_FIRST_DAY,
            false, DateHelper.getCurrentDate());
        // *** create condition to evaluate TRUE
        Condition condition = new IsSpecificMonthCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(IsSpecificMonthCondition.PARAM_MONTHS_OF_YEAR, "JAN");
        assertConditionTrue(empId, start, condition, condParams);
        // *** create condition to evaluate FALSE
        condParams.removeAllParameters();
        condParams.addParameter(IsSpecificMonthCondition.PARAM_MONTHS_OF_YEAR, "FEB,MAR");
        assertConditionFalse(empId, start, condition, condParams);
        // *** create condition to evaluate TRUE for PARAM_INCLUSIVE
    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
