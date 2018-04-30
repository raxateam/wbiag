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
 * Test for IsDateRetroConditionTest.
 */
public class IsDateRetroConditionTest
    extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsDateRetroConditionTest.class);

    public IsDateRetroConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsDateRetroConditionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testRetro() throws Exception {
        DBConnection conn = this.getConnection();

        final int empId = 15;
        Date start = DateHelper.addDays(DateHelper.getCurrentDate() , -2);
        EmployeeData ed = getEmployeeData(empId, start);
        PayGroupData pg = getCodeMapper().getPayGroupById(ed.getPaygrpId() );
        // *** create the rule
        Condition cond = new IsDateRetroCondition();
        Parameters params = new Parameters();

        assertConditionTrue(empId, DateHelper.addDays(pg.getPaygrpAdjustDate() , - 1) , cond, params);

        params.removeAllParameters();

        assertConditionFalse(empId, DateHelper.addDays(pg.getPaygrpAdjustDate() , 1), cond, params);


    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
