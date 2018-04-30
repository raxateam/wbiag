package com.wbiag.tool.regressiontest.action;

/**
 * @author bviveiros
 *
 * Strings submitted in the 'actionType' parameter of a form, and 
 * used by the Controller servlet (action.jsp) to determine 
 * which Action class to call.
 * 
 * Warning!  These Strings are hardcoded in the .js for each page.
 * Changing these values will need to be propogated to the .js
 */
public class ActionTypeTestSuite {

	public static final String INIT_TEST_SUITE = "InitTestSuite";
	public static final String CREATE_TEST_SUITE = "CreateTestSuite";
	public static final String SAVE_TEST_SUITE = "SaveTestSuite";
	public static final String GET_TEST_SUITE = "GetTestSuite";
	public static final String DELETE_TEST_SUITE = "DeleteTestSuite";
	public static final String RUN_TEST_SUITE = "RunTestSuite";
	
}
