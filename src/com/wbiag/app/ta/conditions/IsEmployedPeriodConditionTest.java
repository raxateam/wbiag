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
 * Test for IsEmployeedNDaysConditionTest.
 */
public class IsEmployedPeriodConditionTest
    extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsEmployedPeriodConditionTest.class);

    public IsEmployedPeriodConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsEmployedPeriodConditionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testEmpNDays() throws Exception {
        DBConnection conn = this.getConnection();

        final int empId = 15;
        Date start = DateHelper.getCurrentDate();
        // *** Get Employeed Days
        EmployeeAccess ea = new EmployeeAccess(conn, this.getCodeMapper());
        EmployeeData ed = ea.load(empId, start);

        int daysValue = DateHelper.dateDifferenceInDays(DateHelper.getCurrentDate(),ed.getEmpHireDate());

        // *** create the rule
        Condition cond = new IsEmployedPeriodCondition();
        Parameters params = new Parameters();
        params.addParameter(IsEmployedPeriodCondition.PARAM_REFERENCE,
                            IsEmployedPeriodCondition.PARAM_VAL_REFERENCE_HIRE );
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_PERIOD,
                            DateHelper.APPLY_ON_UNIT_DAY);
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_VALUE,
                            Integer.toString(daysValue));
        params.addParameter(IsEmployedPeriodCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);

        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsEmployedPeriodCondition.PARAM_REFERENCE,
                            IsEmployedPeriodCondition.PARAM_VAL_REFERENCE_HIRE );
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_PERIOD,
                            DateHelper.APPLY_ON_UNIT_DAY);
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_VALUE,
                            Integer.toString(daysValue + 1));
        params.addParameter(IsEmployedPeriodCondition.PARAM_OPERATOR,
                            RuleHelper.LESS);
        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsEmployedPeriodCondition.PARAM_REFERENCE,
                            IsEmployedPeriodCondition.PARAM_VAL_REFERENCE_HIRE );
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_PERIOD,
                            DateHelper.APPLY_ON_UNIT_DAY);
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_VALUE,
                            Integer.toString(daysValue - 1));

        params.addParameter(IsEmployedPeriodCondition.PARAM_OPERATOR,
                            RuleHelper.BIGGER);
        assertConditionTrue(empId, start, cond, params);

    }

    /**
     * @throws Exception
     */
    public void testEmpMonths() throws Exception {
        DBConnection conn = this.getConnection();

        final int empId = 15;
        Date start = DateHelper.getCurrentDate();
        // *** Get Seniority Months
        EmployeeAccess ea = new EmployeeAccess(conn, this.getCodeMapper());
        EmployeeData ed = ea.load(empId, start);

        int monthsValue = (int) DateHelper.getMonthsBetween(ed.getEmpSeniorityDate(),
            DateHelper.getCurrentDate());

        // *** create the rule
        Condition cond = new IsEmployedPeriodCondition();
        Parameters params = new Parameters();

        params.addParameter(IsEmployedPeriodCondition.PARAM_REFERENCE,
                            IsEmployedPeriodCondition.PARAM_VAL_REFERENCE_SENIORITY );
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_PERIOD,
                            DateHelper.APPLY_ON_UNIT_MONTH);
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_VALUE,
                            Integer.toString(monthsValue ));
        params.addParameter(IsEmployedPeriodCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);

        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsEmployedPeriodCondition.PARAM_REFERENCE,
                            IsEmployedPeriodCondition.PARAM_VAL_REFERENCE_SENIORITY );
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_PERIOD,
                            DateHelper.APPLY_ON_UNIT_MONTH);
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_VALUE,
                            Integer.toString(monthsValue + 1));
        params.addParameter(IsEmployedPeriodCondition.PARAM_OPERATOR,
                            RuleHelper.LESS);
        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsEmployedPeriodCondition.PARAM_REFERENCE,
                            IsEmployedPeriodCondition.PARAM_VAL_REFERENCE_SENIORITY );
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_PERIOD,
                            DateHelper.APPLY_ON_UNIT_MONTH);
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_VALUE,
                            Integer.toString(monthsValue - 1));
        params.addParameter(IsEmployedPeriodCondition.PARAM_OPERATOR,
                            RuleHelper.BIGGER);
        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsEmployedPeriodCondition.PARAM_REFERENCE,
                            IsEmployedPeriodCondition.PARAM_VAL_REFERENCE_SENIORITY );
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_PERIOD,
                            DateHelper.APPLY_ON_UNIT_MONTH);
        params.addParameter(IsEmployedPeriodCondition.PARAM_UNIT_VALUE,
                            Integer.toString(monthsValue - 1));
        params.addParameter(IsEmployedPeriodCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);
        assertConditionFalse(empId, start, cond, params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
