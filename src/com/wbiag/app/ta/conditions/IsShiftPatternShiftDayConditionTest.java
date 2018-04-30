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
 * Test for IsShiftPatternShiftDayTest.
 */
public class IsShiftPatternShiftDayConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsShiftPatternShiftDayConditionTest.class);

    public IsShiftPatternShiftDayConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsShiftPatternShiftDayConditionTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testWrks() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        // *** crate def records
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 start,
                                 start).execute(false);
        // *** create the rule
        Condition cond = new IsShiftPatternShiftDayCondition();
        Parameters params = new Parameters();
        params.addParameter(IsShiftPatternShiftDayCondition.PARAM_SHFTPATSHFT_DAY, "1");

        assertConditionTrue(empId , start , cond , params);

        params.removeAllParameters();
        params.addParameter(IsShiftPatternShiftDayCondition.PARAM_SHFTPATSHFT_DAY, "2");
        assertConditionFalse(empId , start , cond , params);
    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
