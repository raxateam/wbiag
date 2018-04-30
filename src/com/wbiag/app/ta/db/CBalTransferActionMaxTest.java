package com.wbiag.app.ta.db;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import junit.framework.*;
/**
 * Test for CBalTransferActionMaxest.
 */
public class CBalTransferActionMaxTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CBalTransferActionMaxTest.class);

    public CBalTransferActionMaxTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CBalTransferActionMaxTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testMax() throws Exception {

        TestUtil.getInstance().setVarTemp("/system/WORKBRAIN_PARAMETERS/"  + RuleAccess.WBPARM_BALANCE_TRANSFERS_ENABLED,            "true");

        final int empId = 11;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate(), "Mon");

        BalanceTransferData btd = new BalanceTransferData();
        btd.setBaltrId(getConnection().getDBSequence(BalanceTransferAccess.BALANCE_TRANSFER_SEQ ).getNextValue()    ) ;
        btd.setBalId(0);
        btd.setCalcgrpId(RuleHelper.getAllGroupsId(getCodeMapper() ) );
        btd.setBaltrBalId(1);
        btd.setBaltrRatio("1");
        btd.setBaltrAction("com.wbiag.app.ta.db.CBalTransferActionMax");
        btd.setBaltrApplyUnit(BalanceTransferData.UNIT_CONSTANT_DATE );
        btd.setBaltrApplyValue(DateHelper.convertDateString(start , BalanceTransferData.UNIT_CONSTANT_DATE_FORMAT ) );
        btd.setBaltrUdf1("20");

        new BalanceTransferAccess(getConnection()).insertRecordData(btd,
            BalanceTransferAccess.BALANCE_TRANSFER_TABLE) ;

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeBalanceOverride insL = new InsertEmployeeBalanceOverride(getConnection());
        insL.setWbuNameBoth("JUNIT", "JUNIT");
        insL.setEmpId(empId);
        insL.setStartDate(start);
        insL.setEndDate(start);
        insL.setEmpbalActionSET();
        insL.setBalId(0);
        insL.setEmpbalValue(40);
        ovrBuilder.add(insL);

        ovrBuilder.execute(true, false);

        assertTrue(1 == ovrBuilder.getOverridesProcessed().size());

        assertEquals(20.0 , getEmployeeBalanceValueAsOfEndOfDate(empId , start , 0), 0);
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
