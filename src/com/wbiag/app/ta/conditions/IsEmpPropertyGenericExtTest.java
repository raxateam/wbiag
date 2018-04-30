package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * Test for ExistsOverrideTest.
 */
public class IsEmpPropertyGenericExtTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsEmpPropertyGenericExtTest.class);

    public IsEmpPropertyGenericExtTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsEmpPropertyGenericExtTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void test1() throws Exception {

        Date start = DateHelper.getCurrentDate();
        int empId = 11;

        // Insert an override.
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeOverride ins = new InsertEmployeeOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        ins.setEmpVal2("TEST");

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);


        // *** create the condition
        Condition cond = new IsEmpPropertyGenericExt();

        Parameters params = new Parameters();
        params.addParameter(IsEmpPropertyGenericExt.PARAM_EXPRESSION_STRING, "empVal2[IN]TEST,TEST1");

        assertConditionTrue(empId, start, cond, params);


    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
