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
public class CEntActionAccrualMaxYearTest extends EntitlementCustomTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CEntActionAccrualMaxYearTest.class);

    public CEntActionAccrualMaxYearTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CEntActionAccrualMaxYearTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testEntitlementsEmployee() throws Exception {

        final int empId = 15;
        Date jan1 = DateHelper.getUnitYear("1" , false , DateHelper.getCurrentDate());
        Date feb1 = DateHelper.getUnitMonth("1" , false , DateHelper.addDays(jan1 , 35));
        Date mar1 = DateHelper.getUnitMonth("1" , false , DateHelper.addDays(jan1 , 65));
        final String polName = "TEST", entName = "TEST";
        final int vacBalId = 0;

        TestUtil.getInstance().setVarTemp("/" + ModuleHelper.MODULE_ENTITLEMENTS ,"TRUE");


        int entpolId = createEntPolicy(polName , DateHelper.DATE_1900 , DateHelper.DATE_3000);
        int entId = createEntitlement(entName ,
                                      EntitlementData.APPLY_ON_UNIT_MONTH,
                                      String.valueOf(EntitlementData.APPLY_ON_FIRST_DAY),
                                      DateHelper.DATE_1900 , DateHelper.DATE_3000);
        createEntPolicyEntitlement(entId, entpolId ,DateHelper.DATE_1900 , DateHelper.DATE_3000);
        // *** assign policy to emp
        associatePolicyWithEmployee(empId , entpolId);

        // *** create ent detail
        EntDetailData det = getDefaultEntDetail(entId);
        det.setBalId(vacBalId);
        det.setEntdetUnits(5);
        det.setEntdetUdf1("10");
        det.setEntdetAction("com.wbiag.app.modules.entitlements.CEntActionAccrualMaxYear");
        createEntDetail(det);

        RuleEngine.runCalcGroup(getConnection(), empId, jan1, jan1, false, true);
        RuleEngine.runCalcGroup(getConnection(), empId, feb1, feb1, false, true);
        RuleEngine.runCalcGroup(getConnection(), empId, mar1, mar1, false, true);

        assertEquals(10,
                     getEmployeeBalanceValueAsOfEndOfDate(empId, mar1,  vacBalId),
                     0);
    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
