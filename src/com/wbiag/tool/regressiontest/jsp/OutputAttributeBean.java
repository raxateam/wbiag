 package com.wbiag.tool.regressiontest.jsp;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.workbrain.app.ta.db.BalanceAccess;
import com.workbrain.app.ta.model.BalanceData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;
import com.wbiag.tool.regressiontest.access.DBHelper;
import com.wbiag.tool.regressiontest.xml.OutputAttributeHelper;
import com.wbiag.util.StringHelper;

/**
 * @author bviveiros
 *
 */
public class OutputAttributeBean implements Serializable {

	private String workSummaryIncludedLabels = null;
	private String workSummaryIncludedIds = null;
	private String workSummaryExcludedLabels = null;
	private String workSummaryExcludedIds = null;
	
	private String workDetailIncludedLabels = null;
	private String workDetailIncludedIds = null;
	private String workDetailExcludedLabels = null;
	private String workDetailExcludedIds = null;

	private String workPremiumIncludedLabels = null;
	private String workPremiumIncludedIds = null;
	private String workPremiumExcludedLabels = null;
	private String workPremiumExcludedIds = null;

	private String employeeBalanceIncludedLabels = null;
	private String employeeBalanceIncludedIds = null;
	private String employeeBalanceExcludedLabels = null;
	private String employeeBalanceExcludedIds = null;
	
	/**
	 * Create the included and excluded lists using the selected 
	 * attributes xml.  If the XML is null or empty then restores
	 * defaults.
	 * 
	 * @param xmlSelectedAttributes
	 * @param conn
	 */
	public OutputAttributeBean(String xmlSelectedAttributes, DBConnection conn) throws Exception {

		// If there are no selected attributes, then set detaults.
		if (com.workbrain.util.StringHelper.isEmpty(xmlSelectedAttributes)) {
			restoreDefaults(conn);
			
		} else {
			initAttributes(xmlSelectedAttributes, conn);
		}
	}

	/**
	 * Restores the Include/Excluded fields to their defaults.
	 *
	 */
	public void restoreDefaults(DBConnection conn) throws Exception {

		workSummaryIncludedLabels = DEFAULT_WORK_SUMMARY_LABELS;
		workSummaryIncludedIds = DEFAULT_WORK_SUMMARY_IDS;
		workSummaryExcludedLabels = StringHelper.getSublist(ALL_WORK_SUMMARY_LABELS, DEFAULT_WORK_SUMMARY_LABELS, ",");
		workSummaryExcludedIds = StringHelper.getSublist(ALL_WORK_SUMMARY_IDS, DEFAULT_WORK_SUMMARY_IDS, ",");
		
		workDetailIncludedLabels = DEFAULT_WORK_DETAIL_LABELS;
		workDetailIncludedIds = DEFAULT_WORK_DETAIL_IDS;
		workDetailExcludedLabels = StringHelper.getSublist(ALL_WORK_DETAIL_LABELS, DEFAULT_WORK_DETAIL_LABELS, ",");
		workDetailExcludedIds = StringHelper.getSublist(ALL_WORK_DETAIL_IDS, DEFAULT_WORK_DETAIL_IDS, ",");

		workPremiumIncludedLabels = DEFAULT_WORK_PREMIUM_LABELS;
		workPremiumIncludedIds = DEFAULT_WORK_PREMIUM_IDS;
		workPremiumExcludedLabels = StringHelper.getSublist(ALL_WORK_PREMIUM_LABELS, DEFAULT_WORK_PREMIUM_LABELS, ",");
		workPremiumExcludedIds = StringHelper.getSublist(ALL_WORK_PREMIUM_IDS, DEFAULT_WORK_PREMIUM_IDS, ",");

		restoreEmployeeBalanceDefaults(conn);
	}
	
	/*
	 * Include list is empty, exclude list is the list of balances
	 * retrieved from the database.
	 * 
	 * @param conn
	 * @throws Exception
	 */
	private void restoreEmployeeBalanceDefaults(DBConnection conn) throws Exception {
		
		employeeBalanceIncludedLabels = "";
		employeeBalanceIncludedIds = "";
		employeeBalanceExcludedLabels = "";
		employeeBalanceExcludedIds = "";

		// Set the complete list in the Exclude list.
		employeeBalanceExcludedLabels = getEmployeeBalanceNames(conn);
		
		// The Ids are the same as the labels.
		employeeBalanceExcludedIds = employeeBalanceExcludedLabels;
	}

	/*
	 * Query the list of defined balances and return as
	 * a CSV of balance names.
	 * 
	 * @param conn
	 * @return
	 */
	private String getEmployeeBalanceNames(DBConnection conn) {
		
		BalanceAccess balanceAccess = new BalanceAccess(conn);
		List balanceList = null;
		StringBuffer balanceNames = new StringBuffer();
		
		try {
			balanceList = balanceAccess.loadAll();
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception retrieving balances.", e);
		}
		
		// If there are any balances defined, create a CSV of the names.
		if (balanceList != null) {
			
			// Get a CSV of balance names.
			BalanceData balanceData = null;
			
			Iterator i = balanceList.iterator();
			while (i.hasNext()) {
				balanceData = (BalanceData) i.next();
				
				balanceNames.append(balanceData.getBalName());
				balanceNames.append(",");
			}
			balanceNames.deleteCharAt(balanceNames.length()-1);
		}
		
		return balanceNames.toString();
	}
	
	/*
	 * Create the included and excluded lists using the selected attributes xml.
	 * 
	 * @param xmlSelectedAttrib - XML of selected attributes.  See OutputAttributeHelper for XML DTD.
	 */
	private void initAttributes(String xmlSelectedAttrib, DBConnection conn) throws Exception {

		String columnIds = null;
		
		// Work Summary
		columnIds = OutputAttributeHelper.getWorkSummaryColumnList(xmlSelectedAttrib);
		
		if (columnIds == null) {
			workSummaryIncludedLabels = "";
			workSummaryIncludedIds = "";
			workSummaryExcludedLabels = ALL_WORK_SUMMARY_LABELS;
			workSummaryExcludedIds = ALL_WORK_SUMMARY_IDS;
		} else {
			workSummaryIncludedLabels = getLabelsFromIds(ALL_WORK_SUMMARY_IDS, ALL_WORK_SUMMARY_LABELS, columnIds);
			workSummaryIncludedIds = columnIds;
			workSummaryExcludedLabels = removeLabelsWithIds(ALL_WORK_SUMMARY_IDS, ALL_WORK_SUMMARY_LABELS, columnIds);
			workSummaryExcludedIds = removeIds(ALL_WORK_SUMMARY_IDS, columnIds);
		}

		// Work Detail
		columnIds = OutputAttributeHelper.getWorkDetailColumnList(xmlSelectedAttrib);
		
		if (columnIds == null) {
			workDetailIncludedLabels = "";
			workDetailIncludedIds = "";
			workDetailExcludedLabels = ALL_WORK_DETAIL_LABELS;
			workDetailExcludedIds = ALL_WORK_DETAIL_IDS;
		} else {
			workDetailIncludedLabels = getLabelsFromIds(ALL_WORK_DETAIL_IDS, ALL_WORK_DETAIL_LABELS, columnIds);
			workDetailIncludedIds = columnIds;
			workDetailExcludedLabels = removeLabelsWithIds(ALL_WORK_DETAIL_IDS, ALL_WORK_DETAIL_LABELS, columnIds);
			workDetailExcludedIds = removeIds(ALL_WORK_DETAIL_IDS, columnIds);
		}
		
		// Work Premium
		columnIds = OutputAttributeHelper.getWorkPremiumColumnList(xmlSelectedAttrib);
		
		if (columnIds == null) {
			workPremiumIncludedLabels = "";
			workPremiumIncludedIds = "";
			workPremiumExcludedLabels = ALL_WORK_PREMIUM_LABELS;
			workPremiumExcludedIds = ALL_WORK_PREMIUM_IDS;
		} else {
			workPremiumIncludedLabels = getLabelsFromIds(ALL_WORK_PREMIUM_IDS, ALL_WORK_PREMIUM_LABELS, columnIds);
			workPremiumIncludedIds = columnIds;
			workPremiumExcludedLabels = removeLabelsWithIds(ALL_WORK_PREMIUM_IDS, ALL_WORK_PREMIUM_LABELS, columnIds);
			workPremiumExcludedIds = removeIds(ALL_WORK_PREMIUM_IDS, columnIds);
		}
		
		// Employee Balances
		columnIds = OutputAttributeHelper.getEmployeeBalanceListString(xmlSelectedAttrib);
		String balanceNames = getEmployeeBalanceNames(conn);
		
		// Balance Ids are the same as the labels.
		String balanceIds = balanceNames;
		
		if (columnIds == null) {
			employeeBalanceIncludedLabels = "";
			employeeBalanceIncludedIds = "";
			employeeBalanceExcludedLabels = balanceNames;
			employeeBalanceExcludedIds = balanceIds;
		} else {
			employeeBalanceIncludedLabels = getLabelsFromIds(balanceIds, balanceNames, columnIds);
			employeeBalanceIncludedIds = columnIds;
			employeeBalanceExcludedLabels = removeLabelsWithIds(balanceIds, balanceNames, columnIds);
			employeeBalanceExcludedIds = removeIds(balanceIds, columnIds);
		}
		
	}
	
	/*
	 * Takes a comma seperated list sourceList and creates a sublist by removing
	 * any elements that exist in the toRemove list.  The new sublist is returned.
	 * 
	 * @param allIds
	 * @param removeIds
	 * @return
	 */
	private String removeIds(String allIds, String removeIds) {
		return StringHelper.getSublist(allIds, removeIds, ",");
	}
	
	/*
	 * Returns a sublist of allLabelList excluding any names if their id is in excludeList.
	 * 
	 * @param allIdList
	 * @param allLabelList
	 * @param excludeList
	 * @return
	 */
	private String removeLabelsWithIds(String allIdList, String allLabelList, String excludeList) {
		return getSublist(allIdList, allLabelList, excludeList, false);
	}
	
	/*
	 * Returns a sublist of allLabelList with any names if their id is in findIdList.
	 * 
	 * @param allIdList
	 * @param allNameList
	 * @param findIdList
	 * @return
	 */
	private String getLabelsFromIds(String allIdList, String allLabelList, String findIdList) {
		return getSublist(allIdList, allLabelList, findIdList, true);
	}
	
	/*
	 * Returns a sublist of actualsList based on the parameters.  
	 * 
	 * @param referenceList
	 * @param actualsList
	 * @param findList
	 * @param inclusive
	 * @return
	 */
	private String getSublist(String referenceList, String actualsList, String findList, boolean inclusive) {
		
		StringBuffer subList = new StringBuffer();
		StringTokenizer refTokens = new StringTokenizer(referenceList, ",");
		StringTokenizer actualTokens = new StringTokenizer(actualsList, ",");
		String refToken = null;
		String actualToken = null;
		boolean isInList = false;
		
		while (refTokens.hasMoreTokens()) {
			
			refToken = refTokens.nextToken();
			actualToken = actualTokens.nextToken();
			
			isInList = com.workbrain.util.StringHelper.isItemInList(findList, refToken);
			
			if ((isInList && inclusive) || (!isInList && !inclusive)) {
				subList.append(actualToken);
				subList.append(",");
			}
		}
		
		// Delete the trailing comma.
		if (subList.length() > 0) {
			subList.deleteCharAt(subList.length()-1);
		}
		
		return subList.toString();
	}
	
	/*
	 * Constants for Defaults
	 */
	
	/*
	 * Work Summary.
	 */
	private static String ALL_WORK_SUMMARY_LABELS = DBHelper.ALL_WORK_SUMMARY_COLUMN_LABELS;
	private static String ALL_WORK_SUMMARY_IDS = DBHelper.ALL_WORK_SUMMARY_COLUMNS;
	
	private static String DEFAULT_WORK_SUMMARY_LABELS = DBHelper.DEFAULT_RULES_WORK_SUMMARY_COLUMN_LABELS;
	private static String DEFAULT_WORK_SUMMARY_IDS = DBHelper.DEFAULT_RULES_WORK_SUMMARY_COLUMNS;

	
	/*
	 * Work Detail.
	 */
	private static String ALL_WORK_DETAIL_LABELS = DBHelper.ALL_WORK_DETAIL_COLUMN_LABELS;
	private static String ALL_WORK_DETAIL_IDS = DBHelper.ALL_WORK_DETAIL_COLUMNS;

	private static String DEFAULT_WORK_DETAIL_LABELS = DBHelper.DEFAULT_RULES_WORK_DETAIL_COLUMN_LABELS;
	private static String DEFAULT_WORK_DETAIL_IDS = DBHelper.DEFAULT_RULES_WORK_DETAIL_COLUMNS;

	
	/*
	 * Work Premium.
	 */
	// The actual column names are WRKD_XXX but want to display WRKP_XXX
	private static String ALL_WORK_PREMIUM_LABELS = DBHelper.ALL_WORK_PREMIUM_COLUMN_LABELS;
	
	// The actual column names are WRKD_XXX but want to display WRKP_XXX
	private static String ALL_WORK_PREMIUM_IDS = DBHelper.ALL_WORK_PREMIUM_COLUMNS;

	private static String DEFAULT_WORK_PREMIUM_LABELS = DBHelper.DEFAULT_RULES_WORK_PREMIUM_COLUMN_LABELS;
	private static String DEFAULT_WORK_PREMIUM_IDS = DBHelper.DEFAULT_RULES_WORK_PREMIUM_COLUMNS;
	

	/*
	 * Public Getters.
	 */
	public String getEmployeeBalanceExcludedIds() {
		return employeeBalanceExcludedIds;
	}
	public String getEmployeeBalanceExcludedLabels() {
		return employeeBalanceExcludedLabels;
	}
	public String getEmployeeBalanceIncludedIds() {
		return employeeBalanceIncludedIds;
	}
	public String getEmployeeBalanceIncludedLabels() {
		return employeeBalanceIncludedLabels;
	}
	public String getWorkDetailExcludedIds() {
		return workDetailExcludedIds;
	}
	public String getWorkDetailExcludedLabels() {
		return workDetailExcludedLabels;
	}
	public String getWorkDetailIncludedIds() {
		return workDetailIncludedIds;
	}
	public String getWorkDetailIncludedLabels() {
		return workDetailIncludedLabels;
	}
	public String getWorkPremiumExcludedIds() {
		return workPremiumExcludedIds;
	}
	public String getWorkPremiumExcludedLabels() {
		return workPremiumExcludedLabels;
	}
	public String getWorkPremiumIncludedIds() {
		return workPremiumIncludedIds;
	}
	public String getWorkPremiumIncludedLabels() {
		return workPremiumIncludedLabels;
	}
	public String getWorkSummaryExcludedIds() {
		return workSummaryExcludedIds;
	}
	public String getWorkSummaryExcludedLabels() {
		return workSummaryExcludedLabels;
	}
	public String getWorkSummaryIncludedIds() {
		return workSummaryIncludedIds;
	}
	public String getWorkSummaryIncludedLabels() {
		return workSummaryIncludedLabels;
	}
}