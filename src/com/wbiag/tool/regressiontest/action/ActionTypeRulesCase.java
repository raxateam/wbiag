package com.wbiag.tool.regressiontest.action;

/**
 * @author bviveiros
 *
 * Strings submitted in the 'actionType' parameter of a form, and 
 * used by the Controller servlet (action.jsp) to determine 
 * which Action class to call.
 */
public class ActionTypeRulesCase {

	public static final String ADD_TEST_CASE = "AddTestCase";
	public static final String DELETE_TEST_CASE = "DeleteTestCase";
	public static final String COPY_TEST_CASE = "CopyTestCase";
	public static final String RECREATE_TEST_CASE = "ReCreateTestCase";
	public static final String SAVE_CHANGES_TEST_CASE = "SaveChangesTestCase";
}
