package com.wbiag.tool.regressiontest.access;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.wbiag.tool.regressiontest.model.ITestCaseData;

/**
 * @author bviveiros
 * 
 * All XXXTestCaseAccess classes must implement this interface.  It
 * is used by TestCaseAccessFactory, TestSuiteEngine, and TestSuiteGetAction.
 *
 */
public interface ITestCaseAccess {
	
	/**
	 * Return the Test Case with the given Id.
	 * 
	 * @param testCaseId
	 * @return
	 * @throws SQLException
	 */
	public ITestCaseData getTestCase(int testCaseId) throws SQLException;

	/**
	 * Create the given Test Case.
	 * 
	 * @param testCase
	 * @throws SQLException
	 */
	public void addTestCase(ITestCaseData testCase) throws SQLException, IOException;

	/**
	 * Update the given Test Case.
	 * 
	 * @param testCase
	 * @throws SQLException
	 */
	public void updateTestCase(ITestCaseData testCase) throws SQLException, IOException;
	
	/**
	 * Return a List of TestCaseData for the given Test Suite Id.
	 * 
	 * @param testSuiteId
	 * @return
	 * @throws SQLException
	 */
	public List getTestCaseList(int testSuiteId) throws SQLException;

	/**
	 * Delete the Test Case with the given Test Case Id.
	 * 
	 * @param testCaseId
	 * @throws SQLException
	 */
	public void deleteTestCase(int testCaseId) throws SQLException;

	/**
	 * Delete the Test Cases with the Ids in the comma seperated list.
	 * 
	 * @param idList - CSV of Test Case Ids.
	 * @throws SQLException
	 */
	public void deleteTestCaseList(String idList) throws SQLException;
}