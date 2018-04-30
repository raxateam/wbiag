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
 * Test for IsCalcDateToCurrentDateConditionTest.
 */
public class IsCalcDateToCurrentDateConditionTest
    extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsCalcDateToCurrentDateConditionTest.class);

    public IsCalcDateToCurrentDateConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsCalcDateToCurrentDateConditionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testDaysPast() throws Exception {
        DBConnection conn = this.getConnection();

        final int empId = 15;
        Date start = DateHelper.addDays(DateHelper.getCurrentDate() , -2);
        // *** create the rule
        Condition cond = new IsCalcDateToCurrentDateCondition();
        Parameters params = new Parameters();
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_NUMBER_OF_DAYS,
                            Integer.toString(0));
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_OPERATOR,
                            RuleHelper.BIGGER);
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_PERIOD,
                            IsCalcDateToCurrentDateCondition.PARAM_VAL_PERIOD_PAST);

        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_NUMBER_OF_DAYS,
                            Integer.toString(1));
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_OPERATOR,
                            RuleHelper.LESSEQ);
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_PERIOD,
                            IsCalcDateToCurrentDateCondition.PARAM_VAL_PERIOD_PAST);
        assertConditionFalse(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_NUMBER_OF_DAYS,
                            Integer.toString(2));
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_OPERATOR,
                            RuleHelper.BIGGEREQ);
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_PERIOD,
                            IsCalcDateToCurrentDateCondition.PARAM_VAL_PERIOD_PAST);
        assertConditionTrue(empId, start, cond, params);

    }

    /**
     * @throws Exception
     */
    public void testDaysFuture() throws Exception {
        DBConnection conn = this.getConnection();

        final int empId = 15;
        Date start = DateHelper.addDays(DateHelper.getCurrentDate() , 1);
        // *** create the rule
        Condition cond = new IsCalcDateToCurrentDateCondition();
        Parameters params = new Parameters();
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_NUMBER_OF_DAYS,
                            Integer.toString(1));
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_PERIOD,
                            IsCalcDateToCurrentDateCondition.PARAM_VAL_PERIOD_FUTURE);

        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_NUMBER_OF_DAYS,
                            Integer.toString(2));
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);
        params.addParameter(IsCalcDateToCurrentDateCondition.PARAM_PERIOD,
                            IsCalcDateToCurrentDateCondition.PARAM_VAL_PERIOD_FUTURE);
        assertConditionFalse(empId, start, cond, params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
