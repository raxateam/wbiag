package com.wbiag.tool.regressiontest.action;

/**
 * @author bviveiros
 *
 * Strings returned by Action classes, and used by the
 * Controller servlet (action.jsp) to determine the URL to forward to.
 */
public class ForwardTestSuite {

	public static final String INIT_TEST_SUITE_SUCCEED = "initTestSuiteSucceed";
	public static final String INIT_TEST_SUITE_FAILED = "initTestSuiteFailed";
	public static final String CREATE_TEST_SUITE_SUCCEED = "createTestSuiteSucceed";
	public static final String CREATE_TEST_SUITE_FAILED = "createTestSuiteFailed";
	public static final String GET_TEST_SUITE_SUCCEED = "getTestSuiteSucceed";
	public static final String GET_TEST_SUITE_FAILED = "getTestSuiteFailed";
	public static final String SAVE_TEST_SUITE_SUCCEED = "saveTestSuiteSucceed";
	public static final String SAVE_TEST_SUITE_FAILED = "saveTestSuiteFailed";
	public static final String DELETE_TEST_SUITE_SUCCEED = "deleteTestSuiteSucceed";
	public static final String DELETE_TEST_SUITE_FAILED = "deleteTestSuiteFailed";
	public static final String RUN_TEST_SUITE_SUCCEED = "runTestSuiteSucceed";
	public static final String RUN_TEST_SUITE_FAILED = "runTestSuiteFailed";
}
