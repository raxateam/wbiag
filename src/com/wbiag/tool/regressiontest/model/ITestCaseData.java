package com.wbiag.tool.regressiontest.model;

/**
 * @author bviveiros
 *
 * Used by the ITestCaseAccess interface.
 *
 * Why is this empty?
 * 
 * There are common attributes to all types of test cases.  However, 
 * in order to be used by a RecordAccess class, a subclass must 
 * name it's properties to match the columns in it's database table.  
 * Since the column names will be different for different test case 
 * types, the properties will have different names so therefore 
 * cannot be declared in a base class.
 * 
 * Eg.  The PayRulesTestCaseData class must have a getRulesCaseName() method 
 * for the RULES_CASE_NAME db column but ScheduleOptimizationData must have
 * a getSchedOpCaseName() method for the SCHED_OP_CASE_NAME db column.
 * 
 * Therefore, declaring testCaseName in the base class would not help us.
 *   
 */
public interface ITestCaseData {


}
