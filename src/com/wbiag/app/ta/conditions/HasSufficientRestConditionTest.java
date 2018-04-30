package com.wbiag.app.ta.conditions;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.util.*;
import com.workbrain.tool.overrides.*;
import java.util.*;

import junit.framework.*;

/**
 * Test for WorkedNMinutesOperatorCondition.
 */

public class HasSufficientRestConditionTest extends RuleTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(HasSufficientRestConditionTest.class);

    public HasSufficientRestConditionTest(String testName)
        throws Exception
    {
        super(testName);
    }

    public static TestSuite suite()
    {
        TestSuite result = new TestSuite();
        result.addTestSuite(HasSufficientRestConditionTest.class);
        return result;
    }

    public void testWorkedNMinsOperator()
        throws Exception
    {
        final int empId = 15;
        Date fri = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "FRI");
        Date mon = DateHelper.addDays(fri , -4);

        new CreateDefaultRecords(getConnection(), new int[] {empId}, mon, mon).execute(false);

        Condition condition = new HasSufficientRestCondition();
        Parameters condParams = new Parameters();

        // *** create condition to evaluate TRUE
        condParams.addParameter(HasSufficientRestCondition.PARAM_REST_MINUTES , "480");
        condParams.addParameter(HasSufficientRestCondition.PARAM_OPERATOR,RuleHelper.BIGGEREQ);
        assertConditionTrue(empId, fri, condition, condParams);

    }

    public static void main(String[] args)
        throws Exception
    {
        junit.textui.TestRunner.run(suite());
    }
}
