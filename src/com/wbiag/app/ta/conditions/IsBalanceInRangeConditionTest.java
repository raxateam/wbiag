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
 * Test for IsBalanceInRangeConditionTest.
 */
public class IsBalanceInRangeConditionTest
    extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsBalanceInRangeConditionTest.class);

    public IsBalanceInRangeConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsBalanceInRangeConditionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testWrks() throws Exception {
        DBConnection conn = this.getConnection();

        final int empId = 15;
        Date start = DateHelper.getCurrentDate();
        // *** Set Balance
        EmployeeBalanceAccess eba = new EmployeeBalanceAccess(conn);
        List ebList = eba.loadByEmployee(empId, start);
        EmployeeBalanceData ebd = (EmployeeBalanceData) ebList.get(0);
        String balanceName = this.getCodeMapper().getBalanceById(ebd.getBalId()).
            getBalName();
        int balanceValue = (int) ebd.getEmpbalValue();
        System.out.println(balanceName + " - " + balanceValue + ", " +
                           RuleHelper.EQ);
        // *** create the rule
        Condition cond = new IsBalanceInRangeCondition();
        Parameters params = new Parameters();
        params.addParameter(IsBalanceInRangeCondition.PARAM_BALANCE_NAME,
                            balanceName);
        params.addParameter(IsBalanceInRangeCondition.PARAM_BALANCE_VALUE,
                            Integer.toString(balanceValue));
        params.addParameter(IsBalanceInRangeCondition.PARAM_OPERATOR,
                            RuleHelper.EQ);

        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsBalanceInRangeCondition.PARAM_BALANCE_NAME,
                            balanceName);
        params.addParameter(IsBalanceInRangeCondition.PARAM_BALANCE_VALUE,
                            Integer.toString(balanceValue+1));
        params.addParameter(IsBalanceInRangeCondition.PARAM_OPERATOR,
                            RuleHelper.LESS);
        assertConditionTrue(empId, start, cond, params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
