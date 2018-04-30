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
 * Test for IsSOEmpPropertyGenericTest.
 */
public class IsSOEmpPropertyGenericTest
    extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsSOEmpPropertyGenericTest.class);

    public IsSOEmpPropertyGenericTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsSOEmpPropertyGenericTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testEmpNDays() throws Exception {
        DBConnection conn = this.getConnection();

        final int empId = 9001;
        Date start = DateHelper.getCurrentDate();

        // *** create the rule
        Condition cond = new IsSOEmpPropertyGeneric();
        Parameters params = new Parameters();
        params.addParameter(IsSOEmpPropertyGeneric.PARAM_EXPRESSION_STRING,
                            "sempIsMinor=0");

        assertConditionTrue(empId, start, cond, params);

        params.removeAllParameters();
        params.addParameter(IsSOEmpPropertyGeneric.PARAM_EXPRESSION_STRING,
                            "sempExemptStat=M");
        assertConditionFalse(empId, start, cond, params);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
