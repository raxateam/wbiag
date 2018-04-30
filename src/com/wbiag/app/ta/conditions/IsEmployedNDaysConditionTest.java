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
public class IsEmployedNDaysConditionTest
    extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsEmployedNDaysConditionTest.class);

    public IsEmployedNDaysConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsEmployedNDaysConditionTest.class);
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
        Condition cond = new IsEmployedNDaysCondition();
        Parameters params = new Parameters();
        params.addParameter(IsEmployedNDaysCondition.PARAM_N_DAYS,
                            Integer.toString(daysValue));
        params.addParameter(IsEmployedNDaysCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);

        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsEmployedNDaysCondition.PARAM_N_DAYS,
                            Integer.toString(daysValue + 1));
        params.addParameter(IsEmployedNDaysCondition.PARAM_OPERATOR,
                            RuleHelper.LESS);
        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsEmployedNDaysCondition.PARAM_N_DAYS,
                            Integer.toString(daysValue - 1));
        params.addParameter(IsEmployedNDaysCondition.PARAM_OPERATOR,
                            RuleHelper.BIGGER);
        assertConditionTrue(empId, start, cond, params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
