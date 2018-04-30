package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import junit.framework.*;

/**
 * Test for PayrollGroupLockDatesTaskTest.
 */
public class PayrollGroupLockDatesTaskTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PayrollGroupLockDatesTaskTest.class);

    public PayrollGroupLockDatesTaskTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(PayrollGroupLockDatesTaskTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testLock() throws Exception {

        Date taskDatetime = new java.util.Date();
        PayGroupAccess pga = new PayGroupAccess(getConnection());
        WorkbrainGroupAccess wga  = new WorkbrainGroupAccess(getConnection());

        PayrollGroupLockDatesTask task = new PayrollGroupLockDatesTask();
        task.setCheckForInterrupt(false);

        Map params = new HashMap();
        params.put(PayrollGroupLockDatesTask.PARAM_PAYROLL_COORD_SEC_GROUPS , "OTHERS");
        params.put(PayrollGroupLockDatesTask.PARAM_SUPERVISOR_SEC_GROUPS, "SUPERVISORS");
        params.put(PayrollGroupLockDatesTask.PARAM_PAYROLL_DATES_LOCK_MODE,
                   PayrollGroupLockDatesTask.PARAM_VAL_MODE_LOCK);
        params.put(PayrollGroupLockDatesTask.PARAM_APPLY_PAY_GROUPS, "1");

        params.put(PayrollGroupLockDatesTask.PARAM_CLIENT_ID, "1");
        // *** lock emps
        PayGroupData pgd = pga.load(1);
        pgd.setPgcId(null);
        pgd.setPaygrpUdf1(DateHelper.convertDateString(
            DateHelper.addMinutes(taskDatetime , -60),
            PayrollGroupLockDatesTask.PGUDF_TIME_FORMAT));
        pga.update(pgd);

        task.execute(getConnection() , params);
        pgd = pga.load(1);
        assertEquals(DateHelper.DATE_3000 , pgd.getPaygrpSupervisorDate());
        assertEquals(PayrollGroupLockDatesTask.PAYGRP_STATUS_LOCKED, pgd.getPaygrpUdf4());
        // *** lock sups
        pgd.setPaygrpUdf2(DateHelper.convertDateString(
            DateHelper.addMinutes(taskDatetime , -30),
            PayrollGroupLockDatesTask.PGUDF_TIME_FORMAT));
        pgd.setPgcId(null);
        pgd.setPaygrpUdf4(PayrollGroupLockDatesTask.PAYGRP_STATUS_UNLOCKED);
        pga.update(pgd);
        task.execute(getConnection() , params);
        WorkbrainGroupData wbg = wga.load(3);
        assertEquals("N", wbg.getWbgLockdown() );
        // *** unlock
        params.put(PayrollGroupLockDatesTask.PARAM_PAYROLL_COORD_SEC_GROUPS , "OTHERS");
        params.put(PayrollGroupLockDatesTask.PARAM_SUPERVISOR_SEC_GROUPS, "SUPERVISORS");
        params.put(PayrollGroupLockDatesTask.PARAM_PAYROLL_DATES_LOCK_MODE,
                   PayrollGroupLockDatesTask.PARAM_VAL_MODE_UNLOCK);
        params.put(PayrollGroupLockDatesTask.PARAM_APPLY_PAY_GROUPS, "1");
        task.execute(getConnection() , params);

        wbg = wga.load(3);
        assertEquals("Y", wbg.getWbgLockdown() );
        pgd = pga.load(1);
        assertEquals(DateHelper.addDays(pgd.getPaygrpStartDate() , -1) , pgd.getPaygrpSupervisorDate());
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
