package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.sql.*;

import junit.framework.*;


/**
 * Test for IsHolidayExtendedConditionTest.
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class IsHolidayExtendedConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsHolidayExtendedConditionTest.class);

    public IsHolidayExtendedConditionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsHolidayExtendedConditionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void xHolidayExtendedConditionTest() throws Exception {

    	//identity the employee you plan to test with by emp_id
    	final int empId = 11;

    	//set a variable to represent the test holiday name
        final String holName = "TEST HOLIDAY_" + System.currentTimeMillis()  ;

        //variable to hold today's date
        Date today = DateHelper.parseDate("03/08/2006", "MM/dd/yyyy");

        //create a HolidayData object, so that we can define the TEST HOLIDAY
        HolidayData hd = new HolidayData();

        //set name of Holiday
        hd.setHolName(holName);

        //set the Date (Date: March 14th, 2006)
        hd.setHolDate(DateHelper.parseDate("03/08/2006", "MM/dd/yyyy"));

        //connect to Database to get the next Holiday ID number
        hd.setHolId(getConnection().getDBSequence(HolidayAccess.HOLIDAY_SEQ).getNextValue() );

        //set if the holiday will be set based on calendar date
        hd.setHolUseCalendar("N");

        //set the Labor Metric for this Holiday 1 = Standard Data, 2 = RESTRICTED DATA
        hd.setLmsId(1);

        //add the new holiday to the Database
        new HolidayAccess(getConnection()).insertRecordData(hd, HolidayAccess.HOLIDAY_TABLE );

        //adding a Holiday Override on a particular day
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeHolidayOverride ins = new InsertEmployeeHolidayOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(today);
        ins.setEndDate(today);
        ins.setHolName(holName);


        ovrBuilder.add(ins);
        //Parameters (calculate Records, Commit Changes)
        //so we set it to calculate the records the records, but not to commit
        ovrBuilder.execute(true , false);

        //ensure the override has been added!
        assertOverrideAppliedOne(ovrBuilder);

        //create condition to evaluate TRUE - Today (Date: March 8th 2006)
        Condition condition = new IsHolidayExtendedCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , "Today");
        assertConditionTrue(empId, today, condition, condParams);

        //create condition to evaluate TRUE - Yesterday (Date: March 9th 2006)
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , "Yesterday");
        assertConditionTrue(empId, DateHelper.parseDate("03/09/2006", "MM/dd/yyyy"), condition, condParams);

        //create condition to evaluate TRUE - Tomorrow (Date: March 7th 2006)
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , "Tomorrow");
        assertConditionTrue(empId, DateHelper.parseDate("03/07/2006", "MM/dd/yyyy"), condition, condParams);

        //create condition to evaluate FALSE - Today (Date: March 7th 2006)
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , "Tomorrow");
        assertConditionTrue(empId, DateHelper.parseDate("03/07/2006", "MM/dd/yyyy"), condition, condParams);

        //create condition to evaluate FALSE - Today (Date: March 7th 2006)
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , "Today");
        assertConditionFalse(empId, DateHelper.parseDate("03/07/2006", "MM/dd/yyyy"), condition, condParams);

        //create condition to evaluate FALSE - Today (Date: March 7th 2006)
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , "Yesterday");
        assertConditionFalse(empId, DateHelper.parseDate("03/07/2006", "MM/dd/yyyy"), condition, condParams);

        //create condition to evaluate FALSE - Today (Date: March 9th 2006)
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , "Tomorrow");
        assertConditionFalse(empId, DateHelper.parseDate("03/09/2006", "MM/dd/yyyy"), condition, condParams);

    }

    /**
     * @throws Exception
     */
    public void testLastNext() throws Exception {

        //identity the employee you plan to test with by emp_id
        final int empId = 11;

        //set a variable to represent the test holiday name
        final String holName = "TEST HOLIDAY_" + System.currentTimeMillis()  ;

        //variable to hold today's date
        Date today = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon"); ;

        //create a HolidayData object, so that we can define the TEST HOLIDAY
        HolidayData hd = new HolidayData();
        hd.setHolName(holName);
        hd.setHolDate(DateHelper.getCurrentDate());
        hd.setHolId(getConnection().getDBSequence(HolidayAccess.HOLIDAY_SEQ).getNextValue() );
        hd.setHolUseCalendar("N");
        hd.setLmsId(1);

        new HolidayAccess(getConnection()).insertRecordData(hd, HolidayAccess.HOLIDAY_TABLE );

        //adding a Holiday Override on a particular day
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertEmployeeHolidayOverride ins = new InsertEmployeeHolidayOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(today);
        ins.setEndDate(today);
        ins.setHolName(holName);


        ovrBuilder.add(ins);
        //Parameters (calculate Records, Commit Changes)
        //so we set it to calculate the records the records, but not to commit
        ovrBuilder.execute(true , false);

        //ensure the override has been added!
        assertOverrideAppliedOne(ovrBuilder);

        //create condition to evaluate TRUE -
        Condition condition = new IsHolidayExtendedCondition();
        Parameters condParams = new Parameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , "Today");
        assertConditionTrue(empId, today, condition, condParams);


        //create condition to evaluate TRUE - For Tue, Last skd day is holiday
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , WBData.WORKED_LAST);
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAY_TYPE_APPLIEDTO , IsHolidayExtendedCondition.SKD_SCHEDULED);
        assertConditionTrue(empId, DateHelper.addDays(today , 1), condition, condParams);

        //create condition to evaluate TRUE - For previous Fri, Next skd day is holiday
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , WBData.WORKED_NEXT);
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAY_TYPE_APPLIEDTO , IsHolidayExtendedCondition.SKD_SCHEDULED);
        assertConditionTrue(empId, DateHelper.addDays(today , -3), condition, condParams);

        //create condition to evaluate FALSE - For previous Fri, Next unskd day is not a holiday
        condParams.removeAllParameters();
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAYS_TO_CHECK , WBData.WORKED_NEXT);
        condParams.addParameter(IsHolidayExtendedCondition.PARAM_DAY_TYPE_APPLIEDTO , IsHolidayExtendedCondition.SKD_UNSCHEDULED);
        assertConditionFalse(empId, DateHelper.addDays(today , -3), condition, condParams);


    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

