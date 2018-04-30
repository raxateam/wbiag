package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for EmployeeRateInPeriodConditionTest.
 */
public class EmployeeRateInPeriodConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeRateInPeriodConditionTest.class);

    public EmployeeRateInPeriodConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(EmployeeRateInPeriodConditionTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testCond() throws Exception {

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        new CreateDefaultRecords(getConnection() , new int[] {empId},
                                 DateHelper.getUnitWeek(DateHelper.APPLY_ON_FIRST_DAY , false, start),
                                 DateHelper.getUnitWeek(DateHelper.APPLY_ON_LAST_DAY , false, start)).execute(false);

        // *** create the cond
        Condition cond = new EmployeeRateInPeriodCondition();
        Parameters params = new Parameters();
        params.addParameter(EmployeeRateInPeriodCondition.PARAM_RATE_TOTAL, "10000");
        params.addParameter(EmployeeRateInPeriodCondition.PARAM_OPERATOR, "<");
        params.addParameter(EmployeeRateInPeriodCondition.PARAM_APPLY_ON_UNIT, DateHelper.APPLY_ON_UNIT_WEEK);
        params.addParameter(EmployeeRateInPeriodCondition.PARAM_APPLY_ON_VALUE_START, DateHelper.APPLY_ON_FIRST_DAY);
        params.addParameter(EmployeeRateInPeriodCondition.PARAM_APPLY_ON_VALUE_END, DateHelper.APPLY_ON_LAST_DAY);

        assertConditionTrue(empId , start , cond , params);

        params.removeAllParameters();
        //params.addParameter(EmployeeRateInPeriodCondition.PARAM_CLOCK_MODE,
        //                    EmployeeRateInPeriodCondition.
        //                    PARAM_VAL_CLOCK_MODE_READER);
        //assertConditionFalse(empId , start , cond , params);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
