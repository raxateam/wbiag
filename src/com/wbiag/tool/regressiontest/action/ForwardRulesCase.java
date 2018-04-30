package com.wbiag.tool.regressiontest.action;


/**
 * @author bviveiros
 *
 * Strings returned by Action classes, and used by the
 * Controller servlet (action.jsp) to determine the URL to forward to.
 */
public class ForwardRulesCase {

	public static final String ADD_TEST_CASE_SUCCEED = "addTestCaseSucceed";
	public static final String ADD_TEST_CASE_FAILED = "addTestCaseFailed";
	public static final String DELETE_TEST_CASE_SUCCEED = "deleteTestCaseSucceed";
	public static final String DELETE_TEST_CASE_FAILED = "deleteTestCaseFailed";
	public static final String COPY_TEST_CASE_SUCCEED = "copyTestCaseSucceed";
	public static final String COPY_TEST_CASE_FAILED = "copyTestCaseFailed";
	public static final String RECREATE_TEST_CASE_SUCCEED = "reCreateTestCaseSucceed";
	public static final String RECREATE_TEST_CASE_FAILED = "reCreateTestCaseFailed";
	public static final String SAVE_CHANGES_TEST_CASE_SUCCEED = "saveChangesTestCaseSucceed";
	public static final String SAVE_CHANGES_TEST_CASE_FAILED = "saveChangesTestCaseFailed";
}
