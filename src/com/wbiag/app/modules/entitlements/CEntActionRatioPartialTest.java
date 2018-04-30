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
public class CEntActionRatioPartialTest extends EntitlementCustomTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CEntActionRatioPartialTest.class);

    public CEntActionRatioPartialTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CEntActionRatioPartialTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testEntitlementsEmployee() throws Exception {

        final int empId = 15;
        Date start = DateHelper.getUnitYear("1" , false , DateHelper.getCurrentDate());
        final String polName = "TEST", entName = "TEST";
        final int vacBalId = 0;

        TestUtil.getInstance().setVarTemp("/" + ModuleHelper.MODULE_ENTITLEMENTS ,"TRUE");



        int entpolId = createEntPolicy(polName , DateHelper.DATE_1900 , DateHelper.DATE_3000);
        int entId = createEntitlement(entName ,
                                      EntitlementData.APPLY_ON_UNIT_YEAR,
                                      String.valueOf(EntitlementData.APPLY_ON_FIRST_DAY),
                                      DateHelper.DATE_1900 , DateHelper.DATE_3000);
        createEntPolicyEntitlement(entId, entpolId ,DateHelper.DATE_1900 , DateHelper.DATE_3000);
        // *** assign policy to emp
        associatePolicyWithEmployee(empId , entpolId);

        // *** create ent detail
        EntDetailData det = getDefaultEntDetail(entId);
        det.setBalId(vacBalId);
        det.setEntdetUnits(10);
        det.setEntdetRatio("com.wbiag.app.modules.entitlements.CEntActionRatioPartialEmployeeAttribute");
        createEntDetail(det);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmp.setEmpId(empId);
        insEmp.setStartDate(DateHelper.DATE_1900);      insEmp.setEndDate(DateHelper.DATE_3000);
        insEmp.setEmpVal10("20");
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(false , false); ovrBuilder.clear();

        RuleEngine.runCalcGroup(getConnection(), empId, start, start, false, true);

        assertEquals(5,
                     getEmployeeBalanceValueAsOfEndOfDate(empId, start,  vacBalId),
                     0);
    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
