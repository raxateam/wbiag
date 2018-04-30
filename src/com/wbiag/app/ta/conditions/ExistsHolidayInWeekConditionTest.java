package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * Test for IsHolidayInWeekTest.
 * @deprecated Use {@link #ExistsOverrideCondition} as of 5.0 with OverrideIDRange=900-999
 */
public class ExistsHolidayInWeekConditionTest
    extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(ExistsHolidayInWeekConditionTest.class);

    public ExistsHolidayInWeekConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ExistsHolidayInWeekConditionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testHolInWeek() throws Exception {
        DBConnection conn = this.getConnection();


        final int empId = 15;
        Date start = DateHelper.parseDate("01/01/2000", "MM/dd/yyyy");


        // *** create the rule
        Condition cond = new ExistsHolidayInWeekCondition();
        Parameters params = new Parameters();

        assertConditionFalse(empId, start, cond, params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
