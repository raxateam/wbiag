package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * Test for IsHolidayExtendedConditionTest.
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class IsWeekendDayExtendedTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsWeekendDayExtendedTest.class);

    public IsWeekendDayExtendedTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsWeekendDayExtendedTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testWeekendDayCondition() throws Exception {
        
    	final int empId = 10001;
    	
    	//the 12th of March is a Saturday
        Date isWeekendDay = DateHelper.parseDate("03/12/2006", "MM/dd/yyyy");
        
        //the 10th of March is a Friday
        Date isNotWeekendDay = DateHelper.parseDate("03/10/2006", "MM/dd/yyyy");
        
        Condition cond = new IsWeekendDayExtended();
        Parameters params = new Parameters();
        
        //test true case -Today (Date: March 12, 2006)
        params.addParameter(IsWeekendDayExtended.DAY_TO_CHECK,"Today");
        assertConditionTrue(empId, isWeekendDay, cond, params);
        
        //test true case -Yesterday (Date: March 12th, 2006)
        params.removeAllParameters();
        params.addParameter(IsWeekendDayExtended.DAY_TO_CHECK,"Yesterday");
        assertConditionTrue(empId, isWeekendDay, cond, params);
        
        //test true case -Tomorrow (Date: March 10th, 2006)
        params.removeAllParameters();
        params.addParameter(IsWeekendDayExtended.DAY_TO_CHECK,"Tomorrow");
        assertConditionTrue(empId, isNotWeekendDay, cond, params);
        
         
        //test false case - Today (Date: March 10th, 2006)
        params.removeAllParameters();
        params.addParameter(IsWeekendDayExtended.DAY_TO_CHECK,"Today");
        assertConditionFalse(empId, isNotWeekendDay, cond, params);
        
        //test false case - Yesterday (Date: March 10th, 2006)
        params.removeAllParameters();
        params.addParameter(IsWeekendDayExtended.DAY_TO_CHECK,"Yesterday");
        assertConditionFalse(empId, isNotWeekendDay, cond, params);

        //test true case -Tomorrow (Date: March 12th, 2006)
        params.removeAllParameters();
        params.addParameter(IsWeekendDayExtended.DAY_TO_CHECK,"Tomorrow");
        assertConditionFalse(empId, isWeekendDay, cond, params);
        
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

