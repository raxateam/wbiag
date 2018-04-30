package com.wbiag.app.modules.entitlements;

import java.util.*;

import com.workbrain.app.modules.*;
import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.test.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for entitlements
 */
public class CentUnitDateYearWithLeapYearTest extends EntitlementCustomTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CentUnitDateYearWithLeapYearTest.class);

    public CentUnitDateYearWithLeapYearTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CentUnitDateYearWithLeapYearTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testEntitlements() throws Exception {

        final int empId = 15;
        Date jun182008 =  DateHelper.parseSQLDate("2008-06-18");
        final String polName = "TEST", entName = "TEST";
        final int vacBalId = 0;

        TestUtil.getInstance().setVarTemp("/" + ModuleHelper.MODULE_ENTITLEMENTS ,"TRUE");
        TestUtil.getInstance().setVarTemp("/system/WORKBRAIN_PARAMETERS/" + EntitlementData.MODULE_ENTITLEMENTS_CUSTOM_UNIT_DATE,
                                          "com.wbiag.app.modules.entitlements.CentUnitDateYearWithLeapYear");

        int entpolId = createEntPolicy(polName , DateHelper.DATE_1900 , DateHelper.DATE_3000);
        int entId = createEntitlement(entName ,
                                      EntitlementData.APPLY_ON_UNIT_CUSTOM,
                                      "169",
                                      DateHelper.DATE_1900 , DateHelper.DATE_3000);
        createEntPolicyEntitlement(entId, entpolId ,DateHelper.DATE_1900 , DateHelper.DATE_3000);
        // *** assign policy to emp
        associatePolicyWithEmployee(empId , entpolId);

        // *** create ent detail
        EntDetailData det = getDefaultEntDetail(entId);
        det.setBalId(vacBalId);
        det.setEntdetUnits(5);
        createEntDetail(det);

        RuleEngine.runCalcGroup(getConnection(), empId, jun182008, jun182008, false, true);

        assertEquals(5 , getEmployeeBalanceValueAsOfEndOfDate(empId, jun182008,  vacBalId) , 0);
        assertEquals(0 , getEmployeeBalanceValueAsOfEndOfDate(empId, DateHelper.addDays(jun182008,-1),  vacBalId) , 0);
    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
