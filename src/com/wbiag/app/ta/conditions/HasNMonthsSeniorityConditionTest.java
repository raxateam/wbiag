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
 * @deprecated Use {@link IsEmployedPeriodCondition}
 */
public class HasNMonthsSeniorityConditionTest
    extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(HasNMonthsSeniorityConditionTest.class);

    public HasNMonthsSeniorityConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HasNMonthsSeniorityConditionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testEmpNDays() throws Exception {
        DBConnection conn = this.getConnection();

        final int empId = 15;
        Date start = DateHelper.getCurrentDate();
        // *** Get Seniority Months
        EmployeeAccess ea = new EmployeeAccess(conn, this.getCodeMapper());
        EmployeeData ed = ea.load(empId, start);

        int monthsValue = (int) DateHelper.getMonthsBetween(ed.getEmpHireDate(),
            DateHelper.getCurrentDate());

        // *** create the rule
        Condition cond = new HasNMonthsSeniorityCondition();
        Parameters params = new Parameters();
        params.addParameter(HasNMonthsSeniorityCondition.PARAM_N_MONTHS,
                            Integer.toString(monthsValue));
        params.addParameter(HasNMonthsSeniorityCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);

        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(HasNMonthsSeniorityCondition.PARAM_N_MONTHS,
                            Integer.toString(monthsValue + 1));
        params.addParameter(HasNMonthsSeniorityCondition.PARAM_OPERATOR,
                            RuleHelper.LESS);
        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(HasNMonthsSeniorityCondition.PARAM_N_MONTHS,
                            Integer.toString(monthsValue - 1));
        params.addParameter(HasNMonthsSeniorityCondition.PARAM_OPERATOR,
                            RuleHelper.BIGGER);
        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(HasNMonthsSeniorityCondition.PARAM_N_MONTHS,
                            Integer.toString(monthsValue - 1));
        params.addParameter(HasNMonthsSeniorityCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);
        assertConditionFalse(empId, start, cond, params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
