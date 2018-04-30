package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for IsEmployeeTeamCondition.
 */
public class IsEmployeeTeamConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsEmployeeTeamConditionTest.class);

    public IsEmployeeTeamConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsEmployeeTeamConditionTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void test1() throws Exception {

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "MON");


        // *** create condition to evaluate TRUE
        Condition condition = new IsEmployeeTeamCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(IsEmployeeTeamCondition.PARAM_HOME_TEAM , "Y");
        condParams.addParameter(IsEmployeeTeamCondition.PARAM_OPERATOR , IsEmployeeTeamCondition.PARAM_VAL_WILDCARD);
        condParams.addParameter(IsEmployeeTeamCondition.PARAM_TEAM_STRING , "*SHIP*");
        assertConditionTrue(empId, start, condition, condParams);

        // *** create condition to evaluate FALSE
        condParams.removeAllParameters();
        condParams.addParameter(IsEmployeeTeamCondition.PARAM_HOME_TEAM , "Y");
        condParams.addParameter(IsEmployeeTeamCondition.PARAM_OPERATOR , IsEmployeeTeamCondition.PARAM_VAL_WILDCARD);
        condParams.addParameter(IsEmployeeTeamCondition.PARAM_TEAM_STRING , "?SHIP?");
        assertConditionFalse(empId, start, condition, condParams);

    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
