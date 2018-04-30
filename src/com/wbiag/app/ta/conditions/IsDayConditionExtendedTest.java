package com.wbiag.app.ta.conditions;

import java.util.*;


import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for IsDayConditionExtended.
 *@deprecated As of 5.0.2.0, use core classes 
 * @author     Shelley Lee
 * @version    1.0
 */
 
public class IsDayConditionExtendedTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsDayConditionExtendedTest.class);

    public IsDayConditionExtendedTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsDayConditionExtendedTest.class);
        return result;
    }
    
    /**
     * @throws Exception
     */    
    public void testConditionTrue() throws Exception {
    	final int empId = 11;
    	//March 13 is a Monday
        Date start = DateHelper.parseDate("03/13/2006", "MM/dd/yyyy");        
        Condition condition = new IsDayConditionExtended();
        Parameters condParams = new Parameters();
    	
        //*** create condition to evaluate TRUE
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsDayConditionExtended.PARAM_DAY, "MONDAY");
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Today");
        assertConditionTrue(empId, start, condition, condParams);
                
        condParams.addParameter(IsDayConditionExtended.PARAM_DAY, "SUNDAY");
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Yesterday");
        assertConditionTrue(empId, start, condition, condParams);

        condParams.removeAllParameters();
        condParams.addParameter(IsDayConditionExtended.PARAM_DAY, "TUESDAY");
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Tomorrow");
        assertConditionTrue(empId, start, condition, condParams);

    	
    }
    public void testConditionFalse() throws Exception {
    	final int empId = 11;
    	//March 13 is a Monday
        Date start = DateHelper.parseDate("03/13/2006", "MM/dd/yyyy");        
        Condition condition = new IsDayConditionExtended();
        Parameters condParams = new Parameters();
    	
        //*** create condition to evaluate FALSE
        condParams.removeAllParameters();
        condParams.addParameter(IsDayConditionExtended.PARAM_DAY, "FRIDAY");
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Today");
        assertConditionFalse(empId, start, condition, condParams);
        
        condParams.removeAllParameters();
        condParams.addParameter(IsDayConditionExtended.PARAM_DAY, "FRIDAY");
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Yesterday");
        assertConditionFalse(empId, start, condition, condParams);

        condParams.removeAllParameters();
        condParams.addParameter(IsDayConditionExtended.PARAM_DAY, "FRIDAY");
        condParams.addParameter(IsDayConditionExtended.DAY_TO_CHECK, "Tomorrow");
        assertConditionFalse(empId, start, condition, condParams);
        
           
    }
    public void testInvalidParam() throws Exception {
    	final int empId = 11;
    	//March 13 is a Monday
        Date start = DateHelper.parseDate("03/13/2006", "MM/dd/yyyy");        
        Condition condition = new IsDayConditionExtended();
        Parameters condParams = new Parameters();
  
        //no value for DAY_TO_CHECK should default to Today        
        condParams.removeAllParameters();
        condParams.addParameter(IsDayConditionExtended.PARAM_DAY, "MONDAY");
        assertConditionTrue(empId, start, condition, condParams);
        
        //exception should occur here when we don't specify DAY
        condParams.removeAllParameters();
        assertConditionTrue(empId, start, condition, condParams);

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}


