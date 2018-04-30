package com.wbiag.tool.regressiontest.xml;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.wbiag.tool.regressiontest.model.RulesCaseResultActual;
import com.wbiag.tool.regressiontest.model.RulesCaseResultExpected;
import com.wbiag.tool.regressiontest.report.ReportDisplayRulesDay;
import com.wbiag.tool.regressiontest.report.TestCaseCompareResult;
import com.wbiag.tool.regressiontest.report.TestCaseResult;
import com.wbiag.util.XMLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;

/**
 * @author bviveiros
 *
 * Functional related to the Pay Rules calc result XML.
 * 
 * XML DTD is:
 * 
 * <pay_rules_calc_result>
 * 
 * 	<calc_result emp_id="" wrks_work_date="">
 * 		
 * 		<work_summary>
 * 			<wrks_work_date></works_work_date>
 * 			<column name>column value</column name>
 * 		</work_summary>
 * 
 * 		<work_detail_list>
 * 			<work_detail>
 * 				<column name>column value</column name>
 * 			</work_detail>
 * 			<work_detail>
 * 				<column name>column value</column name>
 * 			</work_detail>
 * 		</work_detail_list>
 * 
 * 		<work_premium_list>
 * 			<work_premium>
 * 				<column name>column value</column name>
 * 			</work_premium>
 * 			<work_premium>
 * 				<column name>column value</column name>
 * 			</work_premium>
 * 		</work_premium_list>
 * 
 * 		<employee_balance_list>
 * 			<balance name>balance value</balance name>
 * 		</employee_balance_list>
 * 
 * 	</calc_result>
 * 
 * </pay_rules_calc_result>
 * 
 */
public class RulesResultsHelper {

	private static final String MESSAGE_ACTUAL_RESULTS_EMPTY = "Actual results are empty.";
	private static final String MESSAGE_EXTRA_RECORDS_IN_ACTUAL = "Extra records appear in actual results.";
	private static final String MESSAGE_ACTUAL_DATA_DIFFERS = "Actual Data differs from the Expected Data.";
	public static final String DATE_TIME_FORMAT = "dd-MM-yyyy_HH:mm";
	
	private static final String ELEMENT_EMPLOYEE_BALANCE_LIST = "employee_balance_list";
	private static final String ELEMENT_WORK_PREMIUM = "work_premium";
	private static final String ELEMENT_WORK_PREMIUM_LIST = "work_premium_list";
	private static final String ELEMENT_WORK_DETAIL = "work_detail";
	private static final String ELEMENT_WORK_DETAIL_LIST = "work_detail_list";
	private static final String ELEMENT_WORK_SUMMARY = "work_summary";
	private static final String ATTRIBUTE_WRKS_WORK_DATE = "wrks_work_date";
	private static final String ATTRIBUTE_EMP_NAME = "emp_name";
	private static final String ELEMENT_CALC_RESULT = "calc_result";
	private static final String ELEMENT_PAY_RULES_CALC_RESULT = "pay_rules_calc_result";
	private static final String DATE_FORMAT = "dd/MM/yyyy";
	
	Document doc = null;
	
	/**
	 * Default constructor. 
	 * 
	 * @throws Exception - Thrown if an XML Documument cannot be created.
	 */
	public RulesResultsHelper() throws Exception {
		doc = XMLHelper.getNewDocument();
	}
	
	/**
	 * Generates an XML represenation of the data.
	 * 
	 * @param empId
	 * @param workSummaries - key is wrksDate, value is List of Element
	 * @param workDetails - key is wrksDate, value is List of Element
	 * @param workPremiums - key is wrksDate, value is List of Element
	 * @param employeeBalances - key is wrksDate, value is List of Element
	 * @return
	 * @throws Exception
	 */
	public String getResultsXML(String empName,
								Map workSummaries,
								Map workDetails,
								Map workPremiums,
								Map employeeBalances) throws Exception {
		
		Element rootNode = null;
		Element calcResultNode = null;
		Element resultNode = null;
		Long dateKey = null;
		
		rootNode = doc.createElement(ELEMENT_PAY_RULES_CALC_RESULT);
		
		try {
			// For each wrks_work_date, create a calc_result node.
			Date wrksDate = new Date();
			Iterator i = workSummaries.keySet().iterator();
			while (i.hasNext()) {
				
				dateKey = (Long) i.next();
				wrksDate.setTime(dateKey.longValue());
				
				// Create a Calc Result node.
				calcResultNode = doc.createElement(ELEMENT_CALC_RESULT);
				calcResultNode.setAttribute(ATTRIBUTE_EMP_NAME, empName);
				calcResultNode.setAttribute(ATTRIBUTE_WRKS_WORK_DATE, DateHelper.convertDateString(wrksDate, DATE_FORMAT));
				
				// Work Summary.
				resultNode = getWorkSummaryResult(workSummaries, wrksDate);
				if (resultNode != null) {
					calcResultNode.appendChild(resultNode);
				}
				
				// Work Details
				resultNode = getWorkDetailResult(workDetails, wrksDate);
				if (resultNode != null) {
					calcResultNode.appendChild(resultNode);
				}
				
				// Work Premiums
				resultNode = getWorkPremiumResult(workPremiums, wrksDate);
				if (resultNode != null) {
					calcResultNode.appendChild(resultNode);
				}
				
				// Employee Balances.
				resultNode = getEmployeeBalanceResult(employeeBalances, wrksDate);
				if (resultNode != null) {
					calcResultNode.appendChild(resultNode);
				}
	
				rootNode.appendChild(calcResultNode);
			}
			
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception generating pay rules results XML", e);
		}
		
		return com.workbrain.util.XMLHelper.convertToText(rootNode);
	}

	/**
	 * Create an XML element representing a Balance Name and Value.
	 * 
	 * @param balanceName
	 * @param balanceValue
	 * @return
	 */
	public Element createEmployeeBalanceData(String balanceName, double balanceValue) {

		Element rowElement = null;
		rowElement = doc.createElement(balanceName);
		
		Text valueNode = doc.createTextNode(String.valueOf(balanceValue));
		rowElement.appendChild(valueNode);
		
		return rowElement;
	}
	
	/**
	 * From a ResultSet, create an XML Element using the table name, the columns
	 * and data from the ResultSet.
	 * 
	 * @param tableName
	 * @param resultSet
	 * @return
	 * @throws Exception
	 */
	public Element createColumnData(String tableName, ResultSet resultSet) throws Exception {

		Element columnDataNode = null;
		ResultSetMetaData metaData = null;
		int index = 0;
		String columnName = null;
		String columnValue = null;
		int colType = 0;
		
		// Create a node for the table name.  It will be the parent node
		// for all the data elements.
		Element parentNode = doc.createElement(tableName);

		metaData = resultSet.getMetaData();
		
		// For each column name in the ResultSet MetaData.
		for (index=1; index <= metaData.getColumnCount(); index++) {
			
			columnName = metaData.getColumnName(index);
			
	        colType = metaData.getColumnType(index);
	        
	        // Get the column value based on the column type.
	        switch (colType) {
		        case Types.CHAR:
		        case Types.VARCHAR:
		        case Types.LONGVARCHAR:
		            columnValue = resultSet.getString(index);
		            break;
		        case Types.NUMERIC:
		            columnValue = String.valueOf(resultSet.getBigDecimal(index));
		            break;
		        case Types.BIT:
		            columnValue = String.valueOf(resultSet.getBoolean(index));
		            break;
		        case Types.TINYINT:
		            columnValue = String.valueOf(resultSet.getByte(index));
		            break;
		        case Types.DATE:
		            columnValue = resultSet.getTimestamp(index) == null ? null : DateHelper.convertDateString(resultSet.getTimestamp(index), DATE_TIME_FORMAT);
		            break;
		        case Types.DOUBLE:
		        case Types.DECIMAL:
		            columnValue = String.valueOf(resultSet.getDouble(index));
		            break;
		        case Types.FLOAT:
		            columnValue = String.valueOf(resultSet.getFloat(index));
		            break;
		        case Types.INTEGER:
		            columnValue = String.valueOf(resultSet.getInt(index));
		            break;
		        case Types.BIGINT:
		            columnValue = String.valueOf(resultSet.getLong(index));
		            break;
		        case Types.SMALLINT:
		            columnValue = String.valueOf(resultSet.getShort(index));
		            break;
		        case Types.TIME:
		            columnValue = String.valueOf(resultSet.getTime(index));
		            break;
		        case Types.TIMESTAMP:
		            columnValue = resultSet.getTimestamp(index) == null ? null : DateHelper.convertDateString(resultSet.getTimestamp(index), DATE_TIME_FORMAT);
		            break;
		        default:
		            throw new SQLException("Unsupported SQL Type: " + colType);
	        }
			
	        // Inserting null will result in a null pointer exception
	        // from the XMLHelper.convertToText() method so insert blank string instead.
	        if (columnValue == null) {
	        	columnValue = "";
	        }
	        
	        // Create the XML node. <column name>column value</column name>
	        columnDataNode = doc.createElement(columnName);
	        columnDataNode.appendChild(doc.createTextNode(columnValue));

	        // Add the new node to the parent node.
	        parentNode.appendChild(columnDataNode);
		}
		
		return parentNode;
	}
	
	/**
	 * Compares two Pay Rules Test Results.  Returns a message if the compare failed.  If
	 * nothing failed, return null.
	 * 
	 * Each result is associated with a test case.  A test case is for one empId, for a start
	 * date and end date (optional).
	 * 
	 * @param expectedResult
	 * @param actualResult
	 * @return
	 * @throws Exception
	 */
	public void compare(TestCaseResult caseResult, 
								RulesCaseResultExpected expectedResult, 
								RulesCaseResultActual actualResult) throws Exception {

		Document expectedDoc = XMLHelper.createDocument(expectedResult.getExpectedResult());
		Document actualDoc = XMLHelper.createDocument(actualResult.getActualResult());
		List dailyFails = new ArrayList();
		TestCaseCompareResult compareResult = null;
		
		// There will be calc_result for an empId for each wrksDate.
		NodeList expectedCalcResultList = expectedDoc.getElementsByTagName(ELEMENT_CALC_RESULT);
		NodeList actualCalcResultList = actualDoc.getElementsByTagName(ELEMENT_CALC_RESULT);
		Element expectedCalcResult = null;
		Element actualCalcResult = null;

		String empNameAttr = null;
		String wrksDateAttr = null;
		Date workSummaryDate = null;
		
		try {
			// For each expected result in the list.
			for (int i=0; i < expectedCalcResultList.getLength(); i++) {
			
				expectedCalcResult = (Element) expectedCalcResultList.item(i);
				
				// Look for an actual calc result with the same empId and wrksDate.
				empNameAttr = expectedCalcResult.getAttributes().getNamedItem(ATTRIBUTE_EMP_NAME).getNodeValue();
				wrksDateAttr = expectedCalcResult.getAttributes().getNamedItem(ATTRIBUTE_WRKS_WORK_DATE).getNodeValue();
				workSummaryDate = DateHelper.parseDate(wrksDateAttr, DATE_FORMAT);
				
				actualCalcResult = findCalcResult(actualCalcResultList, empNameAttr, wrksDateAttr);
				
				// If a calc result was not found, return a fail message.
				if (actualCalcResult == null) {
					compareResult = new TestCaseCompareResult();
					compareResult.setWorkSummaryDate(workSummaryDate);
					compareResult.setErrorMessage("Calc Result not found for empName: " + empNameAttr + ", wrksDate: " + wrksDateAttr);
					dailyFails.add(compareResult);
				}
				
				// Work Summaries.
				compareResult = compareWorkSummary(expectedCalcResult, actualCalcResult, workSummaryDate);
				if (compareResult != null && compareResult.getErrorMessage() != null) {
					dailyFails.add(compareResult);
				}
				
				// Work Details.
				compareResult = compareWorkDetails(expectedCalcResult, actualCalcResult, workSummaryDate);
				if (compareResult != null && compareResult.getErrorMessage() != null) {
					dailyFails.add(compareResult);
				}
				
				// Work Premiums.
				compareResult = compareWorkPremiums(expectedCalcResult, actualCalcResult, workSummaryDate);
				if (compareResult != null && compareResult.getErrorMessage() != null) {
					dailyFails.add(compareResult);
				}
				
				// Employee Balances.
				compareResult = compareEmployeeBalances(expectedCalcResult, actualCalcResult, workSummaryDate);
				if (compareResult != null && compareResult.getErrorMessage() != null) {
					dailyFails.add(compareResult);
				}
			}
		
		} catch (Exception e) {
			compareResult = new TestCaseCompareResult();
			compareResult.setErrorMessage("Exception while comparing results: \n" + e.toString());
			compareResult.setWorkSummaryDate(workSummaryDate);
			dailyFails.add(compareResult);
		}

		// Store the list of daily compares.
		caseResult.setDailyCompareResults(dailyFails);
		
		// Flag to indicate that all days passed.
		if (dailyFails.size() == 0) {
			caseResult.setAllPassed(true);
		} else {
			caseResult.setAllPassed(false);
		}
	}

	/*
	 * From a NodeList of calc_result nodes, return the Node with matching empId and wrksDate,
	 * or null if not found.
	 * 
	 * @param calcResultList
	 * @param empId
	 * @param wrksDate
	 * @return
	 */
	private Element findCalcResult(NodeList calcResultList, String empName, String wrksDate) {

		Node calcResult = null;
		
		for (int i=0; i < calcResultList.getLength(); i++) {
			calcResult = calcResultList.item(i);
			if (calcResult.getAttributes().getNamedItem(ATTRIBUTE_EMP_NAME).getNodeValue().equals(empName)
					&& 	calcResult.getAttributes().getNamedItem(ATTRIBUTE_WRKS_WORK_DATE).getNodeValue().equals(wrksDate)
					) {
				return (Element) calcResult;
			}
		}
		
		// Node was not found so return null.
		return null;
	}
	
	private TestCaseCompareResult compareWorkSummary(Element expectedCalcResult, Element actualCalcResult, Date workSummaryDate) {
		return compareSingleResult(expectedCalcResult, actualCalcResult, ELEMENT_WORK_SUMMARY, workSummaryDate);
	}
	
	private TestCaseCompareResult compareWorkDetails(Element expectedCalcResult, Element actualCalcResult, Date workSummaryDate) {
		return compareListResult(expectedCalcResult, actualCalcResult, ELEMENT_WORK_DETAIL_LIST, ELEMENT_WORK_DETAIL, workSummaryDate);
	}
	
	private TestCaseCompareResult compareWorkPremiums(Element expectedCalcResult, Element actualCalcResult, Date workSummaryDate) {
		return compareListResult(expectedCalcResult, actualCalcResult, ELEMENT_WORK_PREMIUM_LIST, ELEMENT_WORK_PREMIUM, workSummaryDate);
	}
	
	private TestCaseCompareResult compareEmployeeBalances(Element expectedCalcResult, Element actualCalcResult, Date workSummaryDate) {
		return compareSingleResult(expectedCalcResult, actualCalcResult, ELEMENT_EMPLOYEE_BALANCE_LIST, workSummaryDate);
	}
	
	/*
	 * Compares 2 XML Elements that should have only 1 child node and returns a message
	 * if the compare fails.
	 * 
	 * If there are more results in the actual, then a message is returned.
	 * 
	 * If there is a record in the expected that is not in the actual, a message is returned.
	 * 
	 * @param expectedCalcResult
	 * @param actualCalcResult
	 * @param listName
	 * @return
	 */
	private TestCaseCompareResult compareSingleResult(Element expectedCalcResult, Element actualCalcResult,
										String listName, Date workSummaryDate) {

		TestCaseCompareResult compareResult = new TestCaseCompareResult();
		compareResult.setWorkSummaryDate(workSummaryDate);

		// There will only be one list.
		NodeList expectedList = expectedCalcResult.getElementsByTagName(listName);
		NodeList actualList = actualCalcResult.getElementsByTagName(listName);

		// If there are expected records, but the actual records is empty.
		if (expectedList != null && expectedList.getLength() > 0
					&& (actualList == null || actualList.getLength() == 0)) {
			
			compareResult.setStatus(TestCaseCompareResult.STATUS_FAILED);
			compareResult.setErrorMessage(MESSAGE_ACTUAL_RESULTS_EMPTY);
			if (expectedList != null && expectedList.getLength() > 0) {
				compareResult.setExpectedData(formatResult(expectedList.item(0)));
			}
			
			return compareResult;
		}
		
		// If there are no element to compare.
		if (expectedList == null || expectedList.getLength() == 0) {
			return null;
		}

		// There will only be one item in the list.
		Element expectedElement = (Element) expectedCalcResult.getElementsByTagName(listName).item(0);
		Element actualElement = (Element) actualCalcResult.getElementsByTagName(listName).item(0);

		// There will one entry.
		NodeList actualRecordList = actualCalcResult.getElementsByTagName(listName);
		
		if (!XMLHelper.existsElementMatchingChildNodes(expectedElement, actualRecordList)) {
			compareResult.setStatus(TestCaseCompareResult.STATUS_FAILED);
			compareResult.setErrorMessage(MESSAGE_ACTUAL_DATA_DIFFERS);
			compareResult.setExpectedData(formatResult(expectedElement));
			compareResult.setActualData(formatResult(actualElement));
		}
		
		return compareResult;
	}
	
	/*
	 * Compares 2 XML Elements that could have multiple child nodes and returns a message
	 * if the compare fails.
	 * 
	 * If there are more results in the actual, then a message is returned.
	 * 
	 * If there is a record in the expected that is not in the actual, a message is returned.
	 * 
	 * @param expectedCalcResult
	 * @param actualCalcResult
	 * @param listName
	 * @param recordName
	 * @return
	 */
	private TestCaseCompareResult compareListResult(Element expectedCalcResult, Element actualCalcResult,
										String listName, String recordName, Date workSummaryDate) {
		
		TestCaseCompareResult compareResult = new TestCaseCompareResult();
		compareResult.setWorkSummaryDate(workSummaryDate);

		// There will only be one list.
		NodeList expectedList = expectedCalcResult.getElementsByTagName(listName);
		NodeList actualList = actualCalcResult.getElementsByTagName(listName);
		
		// There will only be one list.
		// There can be multiple records.
		NodeList expectedRecordList = null;
		NodeList actualRecordList = null;
		Element expectedElement = null;
		Element actualElement = null;
		
		if (expectedList != null && expectedList.getLength() > 0) {
			expectedElement = (Element) expectedList.item(0);
			expectedRecordList = expectedElement.getElementsByTagName(recordName);
		}
		if (actualList != null && actualList.getLength() > 0) {
			actualElement = (Element) actualList.item(0);
			actualRecordList = actualElement.getElementsByTagName(recordName);
		}

		// Ensure that no extra records exist in the actual results.
		if ((expectedRecordList == null && actualRecordList != null)
				|| (expectedRecordList != null && actualRecordList != null
						&& actualRecordList.getLength() > expectedRecordList.getLength())) {
			
			compareResult.setStatus(TestCaseCompareResult.STATUS_FAILED);
			compareResult.setErrorMessage(MESSAGE_EXTRA_RECORDS_IN_ACTUAL);
			if (expectedList != null && expectedList.getLength() > 0) {
				compareResult.setExpectedData(formatResult(expectedList.item(0)));
			}
			compareResult.setActualData(formatResult(actualList.item(0)));
			
			return compareResult;
		
		// If there are expected records, but the actual records is empty.
		} else if (expectedRecordList != null 
					&& (actualRecordList == null || actualRecordList.getLength() == 0)) {
			
			compareResult.setStatus(TestCaseCompareResult.STATUS_FAILED);
			compareResult.setErrorMessage(MESSAGE_ACTUAL_RESULTS_EMPTY);
			if (expectedList != null && expectedList.getLength() > 0) {
				compareResult.setExpectedData(formatResult(expectedList.item(0)));
			}
			
			return compareResult;
		}
		
		// If there are no element to compare.
		if (expectedList == null || expectedList.getLength() == 0) {
			return null;
		}
				
		for (int i=0; i < expectedRecordList.getLength(); i++) {
			
			if (!XMLHelper.existsElementMatchingChildNodes((Element)expectedRecordList.item(i), actualRecordList)) {
				
				// An expected result was not found in the actual results.  
				// Return an error message.
				compareResult.setStatus(TestCaseCompareResult.STATUS_FAILED);
				compareResult.setErrorMessage(MESSAGE_ACTUAL_DATA_DIFFERS);
				compareResult.setExpectedData(formatResult(expectedElement));
				compareResult.setActualData(formatResult(actualElement));
				return compareResult;
			}
		}
		
		// All expected were found in actual.  No error to return.
		return null;
	}
	
	/*
	 * Create an XML Element representing a Work Summary Result.  Returns null
	 * if there is no data.
	 * 
	 * @param results
	 * @param wrksDate
	 * @return
	 * @throws Exception
	 */
	private Element getWorkSummaryResult(Map results, Date wrksDate) throws Exception {
		
		Long key = new Long(wrksDate.getTime());
		Element data = null;
		
		if (results != null) {
			data = (Element) results.get(key);
		}
		
		return data;
	}
	
	/*
	 * Create an XML Element representing the Work Detail List.  Returns null
	 * if there is no data.
	 *  
	 * @param results
	 * @param wrksDate
	 * @return
	 * @throws Exception
	 */
	private Element getWorkDetailResult(Map results, Date wrksDate) throws Exception {
		return createListElement(ELEMENT_WORK_DETAIL_LIST, results, wrksDate);
	}

	/*
	 * Create an XML Element representing the Work Premium List.  Returns null
	 * if there is no data.
	 * 
	 * @param results
	 * @param wrksDate
	 * @return
	 * @throws Exception
	 */
	private Element getWorkPremiumResult(Map results, Date wrksDate) throws Exception {
		return createListElement(ELEMENT_WORK_PREMIUM_LIST, results, wrksDate);
	}

	/*
	 * Create an XML Element representing the Employee Balance List.  Returns null
	 * if there is no data.
	 * 
	 * @param results
	 * @param wrksDate
	 * @return
	 * @throws Exception
	 */
	private Element getEmployeeBalanceResult(Map results, Date wrksDate) throws Exception {
		return createListElement(ELEMENT_EMPLOYEE_BALANCE_LIST, results, wrksDate);
	}
	
	/*
	 * Create an XML Element representing a list named listName, with the data in results Map,
	 * with the key wrksDate.
	 * 
	 * @param listName
	 * @param results
	 * @param wrksDate
	 * @return
	 * @throws Exception
	 */
	private Element createListElement(String listName, Map results, Date wrksDate) throws Exception {
		
		Long key = new Long(wrksDate.getTime());

		if (results == null || results.get(key) == null || ((List) results.get(key)).size() == 0) {
			return null;
		}
		
		Element listElement = doc.createElement(listName);

		Element rowElement = null;
		boolean dataExists = false;
		List resultList = null;
		Iterator i = null;
			
		try {
			resultList = (List) results.get(key);
			i = resultList.iterator();
			
			while (i.hasNext()) {
				rowElement = (Element) i.next();
				listElement.appendChild(rowElement);
				dataExists = true;
			}
			
			if (!dataExists) {
				listElement = null;
			}
		} catch (Exception e) {
			throw e;
		}
		
		return listElement;
	}

	/*
	 * Formats the actual or expected result data to be more legible.
	 *  
	 * @param resultData
	 * @return
	 */
	private String formatResult(Node resultData) {
		return com.workbrain.util.XMLHelper.makePrettyText(resultData);
	}
	
	
	/**
	 * Given an Element that contains a pay_rules_calc_result representing
	 * an expected result, parse it into the ReportDisplayRulesDay object. 
	 * 
	 * @param resultElement
	 * @param displayTestDay
	 */
	public static void parseExpectedResult(Element resultElement, ReportDisplayRulesDay displayTestDay) {
		
		List columnList = null;
		List valueList = null;
		
		// Work Summary results.  There will be only one per calc_result.
		columnList = new ArrayList();
		valueList = new ArrayList();
		parseWorkSummaryResult(resultElement, columnList, valueList);
		displayTestDay.setWorkSummaryColumns(columnList);
		displayTestDay.setWorkSummaryExpectedData(valueList);
		
		// Work Detail results.
		columnList = new ArrayList();
		valueList = new ArrayList();
		parseWorkDetailListResult(resultElement, columnList, valueList);
		displayTestDay.setWorkDetailColumns(columnList);
		displayTestDay.setWorkDetailExpectedData(valueList);
		
		// Work Premium results.
		columnList = new ArrayList();
		valueList = new ArrayList();
		parseWorkPremiumListResult(resultElement, columnList, valueList);
		displayTestDay.setWorkPremiumColumns(columnList);
		displayTestDay.setWorkPremiumExpectedData(valueList);
		
		// Employee Balance results.
		columnList = new ArrayList();
		valueList = new ArrayList();
		parseEmployeeBalanceResult(resultElement, columnList, valueList);
		displayTestDay.setEmployeeBalanceColumns(columnList);
		displayTestDay.setEmployeeBalanceExpectedData(valueList);
	}

	
	/**
	 * Given an Element that contains a pay_rules_calc_result representing
	 * an actual result, parse it into the ReportDisplayRulesDay object. 
	 * 
	 * @param resultElement
	 * @param displayTestDay
	 */
	public static void parseActualResult(Element resultElement, ReportDisplayRulesDay displayTestDay) {
		
		List columnList = null;
		List valueList = null;
		
		// Work Summary results.  There will be only one per calc_result.
		columnList = new ArrayList();
		valueList = new ArrayList();
		parseWorkSummaryResult(resultElement, columnList, valueList);
		displayTestDay.setWorkSummaryColumns(columnList);
		displayTestDay.setWorkSummaryActualData(valueList);

		// Work Detail results.
		// Need to parse the work_detail_list
		columnList = new ArrayList();
		valueList = new ArrayList();
		parseWorkDetailListResult(resultElement, columnList, valueList);
		displayTestDay.setWorkDetailColumns(columnList);
		displayTestDay.setWorkDetailActualData(valueList);
	
		// Work Premium results.
		// Need to parse the work_premium_list
		columnList = new ArrayList();
		valueList = new ArrayList();
		parseWorkPremiumListResult(resultElement, columnList, valueList);
		displayTestDay.setWorkPremiumColumns(columnList);
		displayTestDay.setWorkPremiumActualData(valueList);

		columnList = new ArrayList();
		valueList = new ArrayList();
		parseEmployeeBalanceResult(resultElement, columnList, valueList);
		displayTestDay.setEmployeeBalanceColumns(columnList);
		displayTestDay.setEmployeeBalanceActualData(valueList);
	}

	private static void parseWorkSummaryResult(Element resultDataNode, List columnList, List valueList) {
		
		NodeList tableResultList = null;
		Element tableResultNode = null;

		tableResultList = resultDataNode.getElementsByTagName(ELEMENT_WORK_SUMMARY);
		if (tableResultList != null && tableResultList.getLength() > 0) {
			tableResultNode = (Element) tableResultList.item(0);
			columnList.addAll(getColumnList(tableResultNode));
			valueList.add(getValues(tableResultNode));
		}
	}
	
	
	private static void parseWorkDetailListResult(Element resultDataNode, List columnList, List valueList) {

		NodeList tableResultList = null;

		tableResultList = resultDataNode.getElementsByTagName(ELEMENT_WORK_DETAIL_LIST);
		if (tableResultList != null && tableResultList.getLength() > 0) {
			parseWorkDetailResult((Element) tableResultList.item(0), columnList, valueList);
		}
	}
	
	private static void parseWorkDetailResult(Element resultDataNode, List columnList, List valueList) {
		
		NodeList tableResultList = null;
		Element tableResultNode = null;
			
		tableResultList = resultDataNode.getElementsByTagName(ELEMENT_WORK_DETAIL);
		if (tableResultList != null && tableResultList.getLength() > 0) {

			columnList.addAll(getColumnList((Element)tableResultList.item(0)));
			
			for (int i=0; i < tableResultList.getLength(); i++) {
				tableResultNode = (Element) tableResultList.item(i);
				valueList.add(getValues(tableResultNode));
			}
		}
	}
	
	private static void parseWorkPremiumListResult(Element resultDataNode, List columnList, List valueList) {

		NodeList tableResultList = null;
	
		tableResultList = resultDataNode.getElementsByTagName(ELEMENT_WORK_PREMIUM_LIST);
		if (tableResultList != null && tableResultList.getLength() > 0) {
			parseWorkPremiumResult((Element) tableResultList.item(0), columnList, valueList);
		}
	}
	
	
	private static void parseWorkPremiumResult(Element resultDataNode, List columnList, List valueList) {

		NodeList tableResultList = null;
		Element tableResultNode = null;

		tableResultList = resultDataNode.getElementsByTagName(ELEMENT_WORK_PREMIUM);
		if (tableResultList != null && tableResultList.getLength() > 0) {

			columnList.addAll(getColumnList((Element)tableResultList.item(0)));
			
			for (int i=0; i < tableResultList.getLength(); i++) {
				tableResultNode = (Element) tableResultList.item(i);
				valueList.add(getValues(tableResultNode));
			}
		}
	}
	
	
	private static void parseEmployeeBalanceResult(Element resultDataNode, List columnList, List valueList) {
		
		NodeList tableResultList = null;
		Element tableResultNode = null;

		tableResultList = resultDataNode.getElementsByTagName(ELEMENT_EMPLOYEE_BALANCE_LIST);
		if (tableResultList != null && tableResultList.getLength() > 0) {
			tableResultNode = (Element) tableResultList.item(0);
			columnList.addAll(getColumnList(tableResultNode));
			valueList.add(getValues(tableResultNode));
		}
	}
	
	
	/**
	 * nameValueTags is XML tags in the format <COLUMN_NAME>value</COLUMN_NAME>.
	 * Retuns a List of column names.
	 * 
	 * @param nameValueTags
	 * @return
	 */
	private static List getColumnList(Element nameValueTags) {
		
		List columnNames = new ArrayList();
		NodeList nameValueTagList = nameValueTags.getChildNodes();
		Node currentTag = null;
		
		for (int i=0; i < nameValueTagList.getLength(); i++) {
			
			currentTag = nameValueTagList.item(i);
			
			if (currentTag.getNodeType() == Node.ELEMENT_NODE) {
				columnNames.add(currentTag.getNodeName());
			}
		}
		
		return columnNames;
	}
	
	
	/**
	 * nameValueTags is XML tags in the format <COLUMN_NAME>value</COLUMN_NAME>.
	 * Returns a List of value.
	 * 
	 * @param nameValueTags
	 * @return
	 */
	private static List getValues(Element nameValueTags) {

		List columnValues = new ArrayList();
		NodeList nameValueTagList = nameValueTags.getChildNodes();
		Node currentTag = null;
		
		for (int i=0; i < nameValueTagList.getLength(); i++) {
			
			currentTag = nameValueTagList.item(i);
			
			if (currentTag.getNodeType() == Node.ELEMENT_NODE) {
				
				if (currentTag.getFirstChild() != null) {
					columnValues.add(currentTag.getFirstChild().getNodeValue()); 
				} else {
					columnValues.add("");
				}
			}
		}
		
		return columnValues;
	}
}
