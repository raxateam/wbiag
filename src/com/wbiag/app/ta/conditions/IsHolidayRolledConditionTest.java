package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for IsHolidayRolledCondition.
 */
public class IsHolidayRolledConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsHolidayRolledConditionTest.class);

    public IsHolidayRolledConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsHolidayRolledConditionTest.class);
        return result;
    }


    /**
     * Tests if today is holiday by inserting an emp holiday override
     * @throws Exception
     */
    public void testHolidayYesterday() throws Exception {

        final int empId = 15;
        final String holName = "TEST HOLIDAY";
        HolidayData hd = new HolidayData();
        hd.setHolName(holName);
        hd.setHolDate(DateHelper.getCurrentDate());
        hd.setHolId(getConnection().getDBSequence(HolidayAccess.HOLIDAY_SEQ).getNextValue() );
        hd.setHolUseCalendar("N");
        hd.setLmsId(1);
        new HolidayAccess(getConnection()).insertRecordData(hd, HolidayAccess.HOLIDAY_TABLE )   ;
        // *** assign it to a day different than defined
        Date today = DateHelper.addDays(hd.getHolDate() , 300);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeHolidayOverride ins = new InsertEmployeeHolidayOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(today);
        ins.setEndDate(today);
        ins.setHolName(holName);

        ovrBuilder.add(ins);
        ovrBuilder.execute(true , false);

        assertOverrideAppliedOne(ovrBuilder);

        // *** create condition to evaluate TRUE
        Condition condition = new IsHolidayRolledCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(IsHolidayRolledCondition.PARAM_HOLIDAY_NAME , holName + ",OTHER_HOLIDAY");
        assertConditionTrue(empId, today, condition, condParams);

        // *** create condition to evaluate FALSE
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayRolledCondition.PARAM_HOLIDAY_NAME , holName);
        assertConditionFalse(empId, DateHelper.addDays(today , -1), condition, condParams);


    }



    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
