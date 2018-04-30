package com.wbiag.tool.regressiontest.model;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Priority;

import com.wbiag.tool.regressiontest.access.DBHelper;
import com.wbiag.tool.regressiontest.xml.OutputAttributeHelper;
import com.wbiag.tool.regressiontest.xml.RulesResultsHelper;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmployeeBalanceLogAccess;
import com.workbrain.app.ta.ruleengine.CreateDefaultRecords;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 * 
 * This class is used to retrieve the Work Summary, Work Details,
 * Work Premiums, and Employee Balances for a given Test Case.
 *
 */
public class RulesCaseResult implements Serializable {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RulesCaseResult.class);

    protected RulesCaseData testCase = null;
	protected DBConnection conn = null;
	
	private RulesResultsHelper resultsHelper = null;
	private EmployeeBalanceLogAccess balAccess = null;
	private CodeMapper codeMapper = null;
	
	public RulesCaseResult() throws Exception {
		resultsHelper = new RulesResultsHelper();
	}
	

	public RulesCaseData getTestCase() {
		return testCase;
	}


	/**
	 * Retrieve the data from the result of a Pay Rules Test Case.  The data
	 * is then returned as an XML String.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected String retrieveResult() throws Exception {
		
		// If there is no test case, or the list of attributes to capture is empty
		// then there is no result.
		if (testCase == null || StringHelper.isEmpty(testCase.getCaseOutputAttrib())) {
			return null;
		}
		
		Date endDate = null;
		Date queryDate = null;
		Timestamp sqlQueryDate = new Timestamp(0);
		boolean dataExists = false;
		boolean attemptedCreateDefaultRecords = false;
		boolean createDefaultsFailed = false;
		
		Map workSummariesMap = null;
		Map workDetailsMap = null;
		Map workPremiumsMap = null;
		Map employeeBalancesMap = null;
		
		List detailList = null;
		List balanceNameList = null;
		
		String sql = null;
		PreparedStatement wsStatement = null;
		PreparedStatement wdStatement = null;
		PreparedStatement wpStatement = null;
		ResultSet rs = null;
		
		String resultsXML = null;
		
		try {
			// Prepare the SQL statements.
			sql = getWorkSummarySQL();
			wsStatement = conn.prepareStatement(sql);
			wsStatement.setInt(1, this.testCase.getEmpId());
			workSummariesMap = new HashMap();
			
			sql = getWorkDetailSQL();
			if (sql != null) {
				wdStatement = conn.prepareStatement(sql);
				wdStatement.setInt(1, this.testCase.getEmpId());
				workDetailsMap = new HashMap();
			}
			
			sql = getWorkPremiumSQL();
			if (sql != null) {
				wpStatement = conn.prepareStatement(sql);
				wpStatement.setInt(1, this.testCase.getEmpId());
				workPremiumsMap = new HashMap();
			}
			
			// Get the list of Employee Balance Names to be queried.
			balanceNameList = getEmployeeBalanceNames();
			if (balanceNameList != null) {
				employeeBalancesMap = new HashMap();
				balAccess = new EmployeeBalanceLogAccess(conn);
				codeMapper = CodeMapper.createCodeMapper(conn);
			}

			// Set the date range.
			if (this.testCase.getCaseEndDate() == null) {
				endDate = this.testCase.getCaseStartDate();
			} else {
				endDate = this.testCase.getCaseEndDate();
			}
			
			// Query each table for each date in the range.
			queryDate = this.testCase.getCaseStartDate();
			while (queryDate.getTime() <= endDate.getTime()) {
				
				sqlQueryDate.setTime(queryDate.getTime());
				
				// Work Summary
				attemptedCreateDefaultRecords = false;
				dataExists = false;
				while (!dataExists && !createDefaultsFailed) {
					wsStatement.setTimestamp(2, sqlQueryDate);
					
					if (rs != null) rs.close();
					rs = wsStatement.executeQuery();
					
					// there should always be only 1 work summary.
					if (rs.next()) {
						dataExists = true;
						
						workSummariesMap.put(new Long(queryDate.getTime()), 
								resultsHelper.createColumnData("work_summary", rs));
					} else {
						// If no work summary then attemt to create default records.

						// If an attempt was made but failed, then log an info message.
						if (attemptedCreateDefaultRecords) {

							createDefaultsFailed = true;
							
							if (logger.isEnabledFor(Priority.INFO)) {
								StringBuffer msg = new StringBuffer();
								msg.append("Could not create default records for empId: ");
								msg.append(this.testCase.getEmpId());
								msg.append(", date: ");
								msg.append(DateHelper.convertDateString(queryDate, "dd/MM/yyyy"));
								msg.append("\nTest case '");
								msg.append(this.testCase.getCaseName());
								msg.append("' should be removed.");
								
								// Log a message.
								logger.info(msg);
							}
							
						} else {
							attemptedCreateDefaultRecords = true;
							
							generateExpectedResultData();
						}
					}
				}
				
				// Work Detail
				if (wdStatement != null) {
					wdStatement.setTimestamp(2, sqlQueryDate);

					if (rs != null) rs.close();
					rs = wdStatement.executeQuery();
					
					detailList = new ArrayList();
					while (rs.next()) {
						detailList.add(resultsHelper.createColumnData("work_detail", rs));
					}
					workDetailsMap.put(new Long(queryDate.getTime()), detailList);
				}
				
				// Work Premium
				if (wpStatement != null) {
					wpStatement.setTimestamp(2, sqlQueryDate);

					if (rs != null) rs.close();
					rs = wpStatement.executeQuery();
					
					detailList = new ArrayList();
					while (rs.next()) {
						detailList.add(resultsHelper.createColumnData("work_premium", rs));
					}
					workPremiumsMap.put(new Long(queryDate.getTime()), detailList);
				}
				
				// Employee Balance
				if (balanceNameList != null) {
					detailList = getEmployeeBalances(balanceNameList, queryDate);
					employeeBalancesMap.put(new Long(queryDate.getTime()), detailList);
				}
				
				// Increment to the next day.
				queryDate = DateHelper.addDays(queryDate, 1);
			}
			
			// Generate an XML representation of the result data.
			resultsXML = resultsHelper.getResultsXML(
												testCase.empName(),
												workSummariesMap,
												workDetailsMap,
												workPremiumsMap,
												employeeBalancesMap);
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception retrieving Pay Rules results", e);
			
		} finally {
			if (rs != null) {rs.close();}
			if (wsStatement != null) {wsStatement.close();}
			if (wdStatement != null) {wdStatement.close();}
			if (wpStatement != null) {wpStatement.close();}
		}
		
		return resultsXML;
	}

	
	/*
	 * Return an SQL Select statement for the Work Summary colums to be captured.  The complete list
	 * of results is grouped by WRKS_WORK_DATE so it is a mandatory column.
	 * 
	 * @return
	 * @throws Exception
	 */
	private String getWorkSummarySQL() throws Exception {
		return DBHelper.getWorkSummarySelect(
					OutputAttributeHelper.getWorkSummaryColumnList(
							this.testCase.getCaseOutputAttrib())
							);
	}

	/*
	 * Return an SQL Select statement for the Work Detail colums to be captured.
	 * 
	 * @return
	 * @throws Exception
	 */
	private String getWorkDetailSQL() throws Exception {
		return DBHelper.getWorkDetailSelect(
					OutputAttributeHelper.getWorkDetailColumnList(
							this.testCase.getCaseOutputAttrib())
							);
	}


	/*
	 * Return an SQL Select statement for the Work Premium colums to be captured.
	 * 
	 * @return
	 * @throws Exception
	 */
	private String getWorkPremiumSQL() throws Exception {
		return DBHelper.getWorkPremiumSelect(
					OutputAttributeHelper.getWorkPremiumColumnList(
							this.testCase.getCaseOutputAttrib())
							);
	}
	
	/*
	 *  Returns a List of String containing the employee balance Name to be queried.
	 * 
	 * @return
	 * @throws Exception
	 */
	private List getEmployeeBalanceNames() throws Exception {
		return OutputAttributeHelper.getEmployeeBalanceList(this.testCase.getCaseOutputAttrib());
	}
	

	/*
	 * Returns a List of String.  The String is an XML representation 
	 * of Balance Name and Value.
	 * 
	 * @param balanceNameList
	 * @param balanceDate
	 * @return
	 * @throws Exception
	 */
	private List getEmployeeBalances(List balanceNameList, 
										Date balanceDate) throws Exception {
		
		List empBalanceList = new ArrayList();
		
		Iterator i = null;
		int balanceId = 0;
		String balanceName = null;
		double balanceValue = 0;
		
		java.sql.Date date1900 = new java.sql.Date(DateHelper.DATE_1900.getTime());
		try {
			
			// For each Employee Balance. 
			i = balanceNameList.iterator();
			while (i.hasNext()) {
			
				balanceName = (String) i.next();
				balanceId = codeMapper.getBalanceByName(balanceName).getBalId();

				// Get the balance as of the given date.
				balanceValue = balAccess.getSumBalanceLogForDateRange(this.testCase.getEmpId(),
													balanceId,
													date1900,
													new java.sql.Date(balanceDate.getTime()));
		
				// Get the XML representation of the balance and add it to the list.
				empBalanceList.add(resultsHelper.createEmployeeBalanceData(balanceName, balanceValue));
			}
			
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception loading Employee Balances: ", e);
		}
		
		return empBalanceList;
	}
	
	
	/*
	 * Generates the data from a calc result. I.e. Forces a calculate of the day.
	 *
	 */
	private void generateExpectedResultData() throws Exception {
		
		int[] empIds = {testCase.getEmpId()};
		Date startDate = testCase.getCaseStartDate();
		Date endDate = testCase.getCaseEndDate() == null ? startDate : testCase.getCaseEndDate();
		
		CreateDefaultRecords defRecords = 
					new CreateDefaultRecords(conn, empIds, startDate, endDate);
		
		defRecords.execute(true);
	}
}
