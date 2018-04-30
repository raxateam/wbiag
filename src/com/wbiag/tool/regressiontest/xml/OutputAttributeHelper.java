package com.wbiag.tool.regressiontest.xml;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.wbiag.util.StringHelper;
import com.wbiag.util.XMLHelper;

/**
 * @author bviveiros
 * 
 * Functions related to the Output Attributes XML.  
 * 
 * XML DTD is:
 * 
 * <output_attributes>
 * 
 * 		<work_summary>
 * 			<column_names>CSV of db column names</column_names>
 * 		</work_summary>
 * 
 * 		<work_detail>
 * 			<column_names>CSV of db column names</column_names>
 * 		</work_detail>
 * 
 * 		<work_premium>
 * 			<column_names>CSV of db column names</column_names>
 * 		</work_premium>
 * 
 * 		<employee_balance_list>
 * 			<balance_names>CSV of employee balance names</balance_names>
 * 		</employee_balance_list>
 * 
 * </output_attributes>
 * 
 */
public class OutputAttributeHelper {

	// Parameters from the request.
	private static final String PARAM_EMPLOYEE_BALANCES = "employeeBalancesInclude";
	private static final String PARAM_WORK_PREMIUM_FIELDS = "workPremiumFieldsInclude";
	private static final String PARAM_WORK_DETAIL_FIELDS = "workDetailFieldsInclude";
	private static final String PARAM_WORK_SUMMARY_FIELDS = "workSummaryFieldsInclude";
	
	// XML Element Names.
	private static final String ELEMENT_OUTPUT_ATTRIBUTES = "output_attributes";
	private static final String ELEMENT_WORK_SUMMARY = "work_summary";
	private static final String ELEMENT_WORK_DETAIL = "work_detail";
	private static final String ELEMENT_WORK_PREMIUM = "work_premium";
	private static final String ELEMENT_EMPLOYEE_BALANCE_LIST = "employee_balance_list";
	private static final String ELEMENT_COLUMN_NAMES = "column_names";
	private static final String ELEMENT_BALANCE_NAMES = "balance_names";
	
	
	/**
	 * Return an XML representation of the output attributes.
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public static String getOutputAttribXML(ServletRequest request) throws Exception {

		String[] params = null;
		
        Document doc = XMLHelper.getNewDocument();
		Element element = null;
		Element subElement = null;
		Text valuesNode = null;
		
		Node rootNode = doc.createElement(ELEMENT_OUTPUT_ATTRIBUTES);
		
        // -- Work Summary
		params = request.getParameterValues(PARAM_WORK_SUMMARY_FIELDS);

		if (params != null && params.length > 0) {
			element = doc.createElement(ELEMENT_WORK_SUMMARY);
			
			subElement = doc.createElement(ELEMENT_COLUMN_NAMES);
			valuesNode = doc.createTextNode(StringHelper.createCSVForArray(params, false));
			
			subElement.appendChild(valuesNode);
			element.appendChild(subElement);
			rootNode.appendChild(element);
		}
		
		// -- Work Details
		params = request.getParameterValues(PARAM_WORK_DETAIL_FIELDS);

		if (params != null && params.length > 0) {
			element = doc.createElement(ELEMENT_WORK_DETAIL);
			
			subElement = doc.createElement(ELEMENT_COLUMN_NAMES);
			valuesNode = doc.createTextNode(StringHelper.createCSVForArray(params, false));
			
			subElement.appendChild(valuesNode);
			element.appendChild(subElement);
	        rootNode.appendChild(element);
		}
		
		// -- Work Premiums
		params = request.getParameterValues(PARAM_WORK_PREMIUM_FIELDS);

		if (params != null && params.length > 0) {
			element = doc.createElement(ELEMENT_WORK_PREMIUM);
			
			subElement = doc.createElement(ELEMENT_COLUMN_NAMES);
			valuesNode = doc.createTextNode(StringHelper.createCSVForArray(params, false));
			
			subElement.appendChild(valuesNode);
			element.appendChild(subElement);
	        rootNode.appendChild(element);
		}
		
		// -- Employee Balances
		params = request.getParameterValues(PARAM_EMPLOYEE_BALANCES);

		if (params != null && params.length > 0) {
			element = doc.createElement(ELEMENT_EMPLOYEE_BALANCE_LIST);
			
			subElement = doc.createElement(ELEMENT_BALANCE_NAMES);

			valuesNode = doc.createTextNode(StringHelper.createCSVForArray(params, false));

			subElement.appendChild(valuesNode);
			element.appendChild(subElement);
	        rootNode.appendChild(element);
		}
		
		return com.workbrain.util.XMLHelper.convertToText(rootNode);	
	}
	
	
	/**
	 * Get the column list for the Work Summary from the given output attributes XML.
	 * 
	 * @param outputAttribXML
	 * @return
	 */
	public static String getWorkSummaryColumnList(String outputAttribXML) throws Exception {
		return getColumnList(ELEMENT_WORK_SUMMARY, outputAttribXML);
	}
	
	/**
	 * Get the column list for the Work Detail from the given output attributes XML.
	 * 
	 * @param outputAttribXML
	 * @return
	 */
	public static String getWorkDetailColumnList(String outputAttribXML) throws Exception {
		return getColumnList(ELEMENT_WORK_DETAIL, outputAttribXML);
	}
	
	/**
	 * Get the column list for the Work Premium from the given output attributes XML.
	 * 
	 * @param outputAttribXML
	 * @return
	 */
	public static String getWorkPremiumColumnList(String outputAttribXML) throws Exception {
		return getColumnList(ELEMENT_WORK_PREMIUM, outputAttribXML);
	}
	
	/**
	 * Returns a List of String with the balance names.
	 * 
	 * @param outputAttribXML
	 * @return
	 */
	public static List getEmployeeBalanceList(String outputAttribXML) throws Exception {

		List balanceNameList = null; 

		// Should only be 1 entry for balances, with a csv of balance names.
		String strBalanceNameList = getEmployeeBalanceListString(outputAttribXML);

		if (strBalanceNameList != null) {

			// Do not use the detokenizeStringAsList since it is not available in WB4.0
			String[] stArray = com.workbrain.util.StringHelper.detokenizeString(strBalanceNameList, ","); 
			balanceNameList = Arrays.asList(stArray);
		}
		
		return balanceNameList;
	}

	/**
	 * Returns a comma seperated list of balance names.
	 * 
	 * @param outputAttribXML
	 * @return
	 * @throws Exception
	 */
	public static String getEmployeeBalanceListString(String outputAttribXML) throws Exception {

		Document doc = XMLHelper.createDocument(outputAttribXML);
		NodeList nodeList = doc.getElementsByTagName(ELEMENT_EMPLOYEE_BALANCE_LIST);

		// If there is no entry for employee balances then return null.
		if (nodeList == null || nodeList.getLength() == 0) {
			return null;
		}
		
		// Should only be 1 entry for employee balance list.
		Node tableNode = nodeList.item(0);
		
		NodeList balanceNode = tableNode.getChildNodes();
		
		// If there is no entry for the balance names return null.
		if (balanceNode == null || balanceNode.getLength() == 0) {
			return null;
		}
		
		// Should only be 1 entry for balances, with a csv of balance names.
		String strBalanceNameList = balanceNode.item(0).getFirstChild().getNodeValue();
		
		return strBalanceNameList;
	}
	
	/*
	 * Given a table name and a output attributes XML, return the value of the 
	 * column names node. 
	 * 
	 * @param tableName
	 * @param outputAttribXML
	 * @return
	 * @throws Exception
	 */
	private static String getColumnList(String tableName, String outputAttribXML) throws Exception {
		
		Document doc = XMLHelper.createDocument(outputAttribXML);
		NodeList tableList = doc.getElementsByTagName(tableName);
		
		// If there is no entry for the table, then return null.
		if (tableList == null || tableList.getLength() == 0) {
			return null;
		}
		
		// Should only be 1 entry with the table name.
		NodeList columnsList = tableList.item(0).getChildNodes();
		
		// If there is no entry for the table, then return null.
		if (columnsList == null || columnsList.getLength() == 0) {
			return null;
		}
		
		// Should only be 1 entry for column, with a csv of column names.
		Node columnNode = columnsList.item(0);

		String columnNames = columnNode.getFirstChild().getNodeValue();
		
		return columnNames;
	}
}
