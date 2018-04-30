package com.wbiag.app.ta.quickrules;


import com.workbrain.app.ta.ruleengine.*;
import junit.framework.*;
import com.workbrain.util.*;
import com.workbrain.tool.overrides.*;
import java.util.*;

/**
 * Test for MinimumWageRule
 *
 *@deprecated As of 5.0.2.0, use core classes 
 * @author     Shelley Lee
 * @version    1.0
 */
public class MinimumWageRuleTest extends RuleTestCase {
	
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MinimumWageRuleTest.class);
		
	public static void main(String[] args) throws Exception{
		junit.textui.TestRunner.run(suite());
	}

	public MinimumWageRuleTest(String testName) throws Exception {
		super(testName);
	}
	
	//create TestSuite object
	public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(MinimumWageRuleTest.class);
        return result;
    }

	
	/*
	 * Test method for 'com.wbiag.app.ta.quickrules.MinimumWageRule.execute(WBData, Parameters)'
	 */
	public void testMinimumWageRule() throws Exception {
		//assume employee state minimum wage is defined in emp_val1
		//assume employee 11 exist
		//assume FED exist in WBIAG_STATE table
		
		//RegistryHelper regHelper = new RegistryHelper();
		//regHelper.setVar("system/wbiag/EMPLOYEE_STATE_COLUMN","emp_val1");
		
		int emp_id = 11;        //make sure this employee exist
        //Date today = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon");
		Date today = (Date) DateHelper.getCurrentDate();
	    
        Rule rule = new MinimumWageRule();
        Parameters params = new Parameters();        

    	//Setup Override	  		
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
    	ovrBuilder.setCreatesDefaultRecords(true);

        //Setup Employee Detail Override
        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmp.setEmpId(emp_id);      
        insEmp.setStartDate(today);      
        insEmp.setEndDate(today);
                  	
        //case1: pay rate meets minimum wage requirements
   	
	        params.removeAllParameters();
	        params.addParameter("FederalMinWageCode","FED");	
	        params.addParameter("IgnoreZeroPayRate", "TRUE");       
	        clearAndAddRule(emp_id, today, rule, params); //add rule to calc group 		
	        
	        insEmp.setEmpBaseRate(8.00);  //set base rate to $8 which is more than FED $5.15
	   		insEmp.setEmpVal1("CA");      //CA $6.75 is higher than FED $5.15
	        ovrBuilder.add(insEmp);       //apply employee detail override
	   		ovrBuilder.execute(true, false);  //apply override, but don't commit            
	   		ovrBuilder.clear();	   		
	   		assertRuleApplied(emp_id, today, rule);

        //case2: don't ignore zero pay rate, exception raised            
   			params.removeAllParameters();
            params.addParameter("FederalMinWageCode","FED");
            params.addParameter("IgnoreZeroPayRate", "FALSE");
            clearAndAddRule(emp_id, today, rule, params); //add rule to calc group 		
       	
            insEmp.setEmpBaseRate(0);     //set base rate to $0
            ovrBuilder.add(insEmp);       //apply employee detail override       		                        
        	ovrBuilder.execute(true , false);  //apply override, but don't commit            
       		ovrBuilder.clear();      	 
       		assertRuleApplied(emp_id, today, rule);
       		
       	//case3: ignore zero pay rate, no exception
            params.removeAllParameters();
            params.addParameter("FederalMinWageCode","FED");
            params.addParameter("IgnoreZeroPayRate", "TRUE");
            clearAndAddRule(emp_id, today, rule, params); //add rule to calc group 		
       	
            insEmp.setEmpBaseRate(0);     //set base rate to $0
            ovrBuilder.add(insEmp);       //apply employee detail override                   
         	ovrBuilder.execute(true , false);  //apply override, but don't commit            
       		ovrBuilder.clear();
       		assertRuleApplied(emp_id, today, rule);
       		
       	//case4: emp_val1 not defined, exception raised
       		params.removeAllParameters();
            params.addParameter("FederalMinWageCode","FED");
            params.addParameter("IgnoreZeroPayRate", "TRUE");
            clearAndAddRule(emp_id, today, rule, params); //add rule to calc group
      
            insEmp.setEmpBaseRate(8.00);     //set base rate to $0
       		insEmp.setEmpVal1("");           //empty out emp_val1
            ovrBuilder.add(insEmp);       //apply employee detail override            
       		ovrBuilder.execute(true , false);  //apply override, but don't commit
            ovrBuilder.clear();           
            assertRuleApplied(emp_id, today, rule);
            
        //case5: pay rate < federal rate, exception raised
       		params.removeAllParameters();
            params.addParameter("FederalMinWageCode","FED");
            params.addParameter("IgnoreZeroPayRate", "TRUE");
            clearAndAddRule(emp_id, today, rule, params); //add rule to calc group 		
       
            insEmp.setEmpBaseRate(4.75);  //set base rate to $4.75 which is less than FED $5.15
       		insEmp.setEmpVal1("OH");      //OH $4.25 is less than FED $5.15
            ovrBuilder.add(insEmp);       //apply employee detail override            
       		ovrBuilder.execute(true , false);  //apply override, but don't commit            
       		ovrBuilder.clear();      
       		assertRuleApplied(emp_id, today, rule);
       		
       	//case6: pay rate < state rate, exception raised
       		params.removeAllParameters();
            params.addParameter("FederalMinWageCode","FED");
            params.addParameter("IgnoreZeroPayRate", "TRUE");
            clearAndAddRule(emp_id, today, rule, params); //add rule to calc group 		

            insEmp.setEmpBaseRate(6.00);  //set base rate to be less than CA $6.75
       		insEmp.setEmpVal1("CA");      //CA $6.75 is higher than FED $5.15
            ovrBuilder.add(insEmp);       //apply employee detail override
            
       		ovrBuilder.execute(true, false);  //apply override, but don't commit            
       		ovrBuilder.clear();
       		assertRuleApplied(emp_id, today, rule);
       		
         //case7: pay rate < state rate, exception raised
       		params.removeAllParameters();
            params.addParameter("FederalMinWageCode","FED");
            params.addParameter("IgnoreZeroPayRate", "TRUE");
            clearAndAddRule(emp_id, today, rule, params); //add rule to calc group 		

            insEmp.setEmpBaseRate(6.00);  //set base rate to be less than CA $6.75
       		insEmp.setEmpVal1("CA");      //CA $6.75 is higher than FED $5.15
            ovrBuilder.add(insEmp);       //apply employee detail override
            
       		ovrBuilder.execute(true, false);  //apply override, but don't commit            
       		ovrBuilder.clear();       		
       		assertRuleApplied(emp_id, today, rule);
       		
         // case8: empty FederalMinWageCode, default to FED, no exception raised
         // : emp_val1 = AL which has no state min wage, assume min wage = 0, no exception raised
       		params.removeAllParameters();
            params.addParameter("FederalMinWageCode",""); //user does not enter value, assume FED
            params.addParameter("IgnoreZeroPayRate", "TRUE");
            clearAndAddRule(emp_id, today, rule, params); //add rule to calc group 		

            insEmp.setEmpBaseRate(6.00);  //base rate higher than FED $5.15
       		insEmp.setEmpVal1("AL");      //XX has no min wage record, rule assume $0
            ovrBuilder.add(insEmp);       //apply employee detail override
            
       		ovrBuilder.execute(true, false);  //apply override, but don't commit            
       		ovrBuilder.clear();      		
       		assertRuleApplied(emp_id, today, rule);
       	
	}

}
