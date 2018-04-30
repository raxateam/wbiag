package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for IsEmployeeEntPolCondition.
 */
public class IsEmployeeEntPolConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsEmployeeEntPolConditionTest.class);

    public IsEmployeeEntPolConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsEmployeeEntPolConditionTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void test1() throws Exception {

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");

        EntEmpPolicyData d = new EntEmpPolicyData ();
        d.setEmpId(empId) ;
        d.setEntemppolPriority(0);
        d.setEntpolId(10001);
        d.setEntemppolStartDate(DateHelper.DATE_1900 );
        d.setEntemppolEndDate(DateHelper.DATE_3000 );
        d.setEntemppolEnabled("Y");

        new EntEmpPolicyAccess(getConnection()).insert(d);

        // *** create condition to evaluate TRUE
        Condition condition = new IsEmployeeEntPolCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(IsEmployeeEntPolCondition.PARAM_ENT_POL_STRING , "TEST,FUN");
        condParams.addParameter(IsEmployeeEntPolCondition.PARAM_OPERATOR , IsEmployeeEntPolCondition.PARAM_VAL_IN);
        assertConditionTrue(empId, start, condition, condParams);

        // *** create condition to evaluate FALSE
        condParams.removeAllParameters();
        condParams.addParameter(IsEmployeeEntPolCondition.PARAM_ENT_POL_STRING , "VACATION");
        condParams.addParameter(IsEmployeeEntPolCondition.PARAM_OPERATOR , IsEmployeeEntPolCondition.PARAM_VAL_EQUALS);
        assertConditionFalse(empId, start, condition, condParams);

    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
