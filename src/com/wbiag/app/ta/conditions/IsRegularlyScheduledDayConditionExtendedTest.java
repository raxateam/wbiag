package com.wbiag.app.ta.conditions;

import java.util.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * Test for IsRegularlyScheduledDayConditionExtended.
 * @author     Shelley Lee
 * @version    1.0
 */
public class IsRegularlyScheduledDayConditionExtendedTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsRegularlyScheduledDayConditionExtendedTest.class);

    public IsRegularlyScheduledDayConditionExtendedTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsRegularlyScheduledDayConditionExtendedTest.class);
        return result;
    }

    /**
     * Tests IsRegularlyScheduledDayConditionExtended. Emp is Mon-Fri ALL DAYS shift pattern
     * @throws Exception
     */
    public void testConditionTrue() throws Exception {
    	final int empId = 11;
        Date sun = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "SUN");
        Date sat = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "SAT");
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        
        Condition condition = new IsRegularlyScheduledDayConditionExtended();
        Parameters condParams = new Parameters();
        
        //*** create condition to evaluate TRUE
        
        //Today is Monday, Is Monday Scheduled?
        condParams.removeAllParameters();
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Today");        
        assertConditionTrue(empId, mon, condition, condParams);
        
        //Yesterday is Friday, Is Friday scheduled?
        condParams.removeAllParameters();
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Yesterday");        
        assertConditionTrue(empId, sat, condition, condParams);
       
        //Tomorrow is Monday, Is Monday scheduled?
        condParams.removeAllParameters();        
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Tomorrow");        
        assertConditionTrue(empId, sun, condition, condParams);
        
        //DAY_TO_CHECK empty, default should be today
        condParams.removeAllParameters();
        assertConditionTrue(empId, mon, condition, condParams); 	
    }
    
    /**
     * Tests IsRegularlyScheduledDayConditionExtended. Emp is Mon-Fri ALL DAYS shift pattern
     * @throws Exception
     */
    public void testConditionFalse() throws Exception {
    	final int empId = 11;
        Date sun = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "SUN");
        Date fri = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "FRI");        
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        
        Condition condition = new IsRegularlyScheduledDayConditionExtended();
        Parameters condParams = new Parameters();
        
    	// *** create condition to evaluate FALSE
        
        //      Today is Sunday, Is Sunday Scheduled?
        condParams.removeAllParameters();
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Today");
        assertConditionFalse(empId, sun, condition, condParams);
        
        //      Yesterday is Sunday, Is Sunday Scheduled?
        condParams.removeAllParameters();        
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Yesterday");
        assertConditionFalse(empId, mon, condition, condParams);
        
        //      Tomorrow is Saturday, Is Saturday Scheduled?
        condParams.removeAllParameters();
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Tomorrow");
        assertConditionFalse(empId, fri, condition, condParams);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}


