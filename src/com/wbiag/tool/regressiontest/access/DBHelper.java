package com.wbiag.tool.regressiontest.access;

import com.workbrain.util.StringHelper;

/**
 * @author bviveiros
 *
 * Compatable with Workbrain 4.1
 * 
 * Database specific definitions such as SQL queries, table names,
 * column names are stored in this class.
 * 
 * There will be a copy of this class for any Workbrain versions where the
 * database definitions have changed.  Each copy will be in a different VSS stream.
 * Compile with the copy of this file required for your WB version. 
 */
public class DBHelper {

	/**
	 * Return an SQL Select statement for the Work Summary colums to be captured.  The complete list
	 * of results is grouped by WRKS_WORK_DATE so it is a mandatory column.
	 * 
	 * @param columnList
	 * @return
	 */
	public static String getWorkSummarySelect(String columnList) {

		StringBuffer sql = new StringBuffer();
		
		sql.append("select WRKS_WORK_DATE");
		
		if (!StringHelper.isEmpty(columnList)) {
			sql.append("," + columnList);
		}
		
		sql.append(" from WORK_SUMMARY ws, EMPLOYEE e, SHIFT s, CALC_GROUP cg, PAY_GROUP pg"
					+ " where"
					+ " ws.emp_id = e.emp_id"
					+ " and ws.shft_id = s.shft_id"
					+ " and ws.calcgrp_id = cg.calcgrp_id"
					+ " and ws.paygrp_id = pg.paygrp_id"
					+ " and ws.EMP_ID=? and ws.WRKS_WORK_DATE=?"
					);
		
		return sql.toString();
	}
	
	/**
	 * Return an SQL Select statement for the Work Detail colums to be captured.
	 * 
	 * @param columnList
	 * @return
	 */
	public static String getWorkDetailSelect(String columnList) {

		StringBuffer sql = new StringBuffer();
			
		if (StringHelper.isEmpty(columnList)) {
			return null;
		}
			
		sql.append("select ");
		sql.append(columnList);
		sql.append(" from WORK_DETAIL wd, WORK_SUMMARY ws, DEPARTMENT dept"
					+ ", DOCKET dock, HOUR_TYPE ht, JOB j, PROJECT p, TIME_CODE tc"
					+ " where"
					+ " wd.WRKS_ID = ws.WRKS_ID"
					+ " and wd.DEPT_ID = dept.DEPT_ID"
					+ " and wd.DOCK_ID = dock.DOCK_ID"
					+ " and wd.HTYPE_ID = ht.HTYPE_ID"
					+ " and wd.JOB_ID = j.JOB_ID"
					+ " and wd.PROJ_ID = p.PROJ_ID"
					+ " and wd.TCODE_ID = tc.TCODE_ID"
					+ " and wd.WRKD_TYPE='D' and ws.emp_id=? and ws.WRKS_WORK_DATE=?"
					);
		
		if (columnList.indexOf("WRKD_START_TIME") >= 0) {
			sql.append(" order by wd.WRKD_START_TIME");
		}
		
		return sql.toString();
	}
	
	/**
	 * Return an SQL Select statement for the Work Premium colums to be captured.
	 * 
	 * @param columnList
	 * @return
	 */
	public static String getWorkPremiumSelect(String columnList) {

		StringBuffer sql = new StringBuffer();
			
		if (StringHelper.isEmpty(columnList)) {
			return null;
		}
			
		sql.append("select ");
		sql.append(columnList);
		sql.append(" from WORK_DETAIL wd, WORK_SUMMARY ws, DEPARTMENT dept"
					+ ", DOCKET dock, HOUR_TYPE ht, JOB j, PROJECT p, TIME_CODE tc"
					+ " where"
					+ " wd.WRKS_ID = ws.WRKS_ID"
					+ " and wd.DEPT_ID = dept.DEPT_ID"
					+ " and wd.DOCK_ID = dock.DOCK_ID"
					+ " and wd.HTYPE_ID = ht.HTYPE_ID"
					+ " and wd.JOB_ID = j.JOB_ID"
					+ " and wd.PROJ_ID = p.PROJ_ID"
					+ " and wd.TCODE_ID = tc.TCODE_ID"
					+ " and wd.WRKD_TYPE='P' and ws.emp_id=? and ws.WRKS_WORK_DATE=?"
					);
		
		return sql.toString();
	}

	// Work Summary DB Columns.
	public static String ALL_WORK_SUMMARY_COLUMNS = 
							"WRKS_START_TIME,WRKS_END_TIME,WRKS_WRKD_AUTH"
							+ ",WRKS_MANUAL_CALC,CALCGRP_NAME,WRKS_AUTHORIZED"
							+ ",WRKS_AUTH_BY,WRKS_AUTH_DATE,WRKS_ERROR_STATUS"
							+ ",WRKS_FLAG_BRK,WRKS_FLAG_RECALL,WRKS_FLAG1"
							+ ",WRKS_FLAG2,WRKS_FLAG3,WRKS_FLAG4,WRKS_FLAG5"
							+ ",WRKS_UDF1,WRKS_UDF2,WRKS_UDF3,WRKS_UDF4,WRKS_UDF5"
							+ ",WRKS_UDF6,WRKS_UDF7,WRKS_UDF8,WRKS_UDF9,WRKS_UDF10"
							+ ",WRKS_DESC,WRKS_COMMENTS,WRKS_CLOCKS,WRKS_ERROR"
							+ ",WRKS_RULES_APPLIED,PAYGRP_NAME,WRKS_TCODE_SUM"
							+ ",WRKS_HTYPE_SUM,WRKS_ORIG_CLOCKS,WRKS_MESSAGES"
							+ ",WRKS_IN_CODE,WRKS_OUT_CODE,WRKS_FULL_DAY_CODE"
							+ ",WRKS_FULL_DAY_MINUTES,WRKS_SUBMITTED,WRKS_USE_DEF_SETTINGS";

	public static String ALL_WORK_SUMMARY_COLUMN_LABELS = ALL_WORK_SUMMARY_COLUMNS;
	
	// Work Detail DB Columns
	public static String ALL_WORK_DETAIL_COLUMNS = 
							"WRKD_START_TIME,WRKD_END_TIME,WRKD_MINUTES,TCODE_NAME,HTYPE_NAME"
							+ ",WRKD_WORK_DATE,DOCK_NAME,JOB_NAME"
							+ ",PROJ_NAME,WRKD_QUANTITY,WRKD_RATE,WRKD_FLAG1"
							+ ",WRKD_FLAG2,WRKD_FLAG3,WRKD_FLAG4,WRKD_FLAG5"
							+ ",WRKD_FLAG6,WRKD_FLAG7,WRKD_FLAG8,WRKD_FLAG9"
							+ ",WRKD_FLAG10,WRKD_UDF1,WRKD_UDF2,WRKD_UDF3,WRKD_UDF4"
							+ ",WRKD_UDF5,WRKD_UDF6,WRKD_UDF7,WRKD_UDF8,WRKD_UDF9"
							+ ",WRKD_UDF10,DEPT_NAME,WRKD_MESSAGES,WRKD_COMMENTS"
							+ ",WRKD_OVERRIDDEN,WRKD_AUTH,WRKD_AUTH_BY,WRKD_AUTH_DATE";

	public static String ALL_WORK_DETAIL_COLUMN_LABELS = ALL_WORK_DETAIL_COLUMNS;
	
	// Work Premium DB Columns.
	public static String ALL_WORK_PREMIUM_COLUMNS = 
							ALL_WORK_DETAIL_COLUMNS;

	public static String ALL_WORK_PREMIUM_COLUMN_LABELS = 
							"WRKP_START_TIME,WRKP_END_TIME,WRKP_MINUTES,TCODE_NAME,HTYPE_NAME"
							+ ",WRKP_WORK_DATE,DOCK_NAME,JOB_NAME"
							+ ",PROJ_NAME,WRKP_QUANTITY,WRKP_RATE,WRKP_FLAG1"
							+ ",WRKP_FLAG2,WRKP_FLAG3,WRKP_FLAG4,WRKP_FLAG5"
							+ ",WRKP_FLAG6,WRKP_FLAG7,WRKP_FLAG8,WRKP_FLAG9"
							+ ",WRKP_FLAG10,WRKP_UDF1,WRKP_UDF2,WRKP_UDF3,WRKP_UDF4"
							+ ",WRKP_UDF5,WRKP_UDF6,WRKP_UDF7,WRKP_UDF8,WRKP_UDF9"
							+ ",WRKP_UDF10,DEPT_NAME,WRKP_MESSAGES,WRKP_COMMENTS"
							+ ",WRKP_OVERRIDDEN,WRKP_AUTH,WRKP_AUTH_BY,WRKP_AUTH_DATE";
	
	// Default DB columns for Pay Rules Test Cases.
	public static String DEFAULT_RULES_WORK_SUMMARY_COLUMNS = "";
	public static String DEFAULT_RULES_WORK_SUMMARY_COLUMN_LABELS = DEFAULT_RULES_WORK_SUMMARY_COLUMNS;

	public static String DEFAULT_RULES_WORK_DETAIL_COLUMNS = 
							"WRKD_START_TIME,WRKD_END_TIME,WRKD_MINUTES,TCODE_NAME,HTYPE_NAME";
	public static String DEFAULT_RULES_WORK_DETAIL_COLUMN_LABELS = 
							DEFAULT_RULES_WORK_DETAIL_COLUMNS;

	public static String DEFAULT_RULES_WORK_PREMIUM_COLUMNS = 
							"WRKD_MINUTES,TCODE_NAME,HTYPE_NAME";
	public static String DEFAULT_RULES_WORK_PREMIUM_COLUMN_LABELS = 
							"WRKP_MINUTES,TCODE_NAME,HTYPE_NAME";
}
