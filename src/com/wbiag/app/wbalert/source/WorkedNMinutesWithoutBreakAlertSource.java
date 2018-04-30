package com.wbiag.app.wbalert.source;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.workbrain.app.ta.db.WorkDetailAccess;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.server.data.AbstractRowCursor;
import com.workbrain.server.data.AbstractRowSource;
import com.workbrain.server.data.AccessException;
import com.workbrain.server.data.BasicRow;
import com.workbrain.server.data.Row;
import com.workbrain.server.data.RowCursor;
import com.workbrain.server.data.RowDefinition;
import com.workbrain.server.data.RowStructure;
import com.workbrain.server.data.type.StringType;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

public class WorkedNMinutesWithoutBreakAlertSource extends AbstractRowSource {

	public static final String PARAM_N_MINUTES = "nMinutes";

	public static final String PARAM_BRK_TCODE = "brkCode";

	public static final String PARAM_WRK_TCODE = "wrkCode";

	public static final String PARAM_WRK_TCODE_INCLUSIVE = "wrkCodeInclusive";

	public static final String PARAM_HTYPE = "hType";

	public static final String PARAM_HTYPE_INCLUSIVE = "hTypeInclusive";

	public static final String PARAM_EMPLOYEE = "employee";

	public static final String PARAM_CALCGROUP = "calcGroup";

	public static final String PARAM_PAYGROUP = "payGroup";

	public static final String PARAM_TEAM = "team";

	private RowDefinition rowDefinition;

	private DBConnection conn;

	private int nMinutes;

	private String brkCode;

	private String wrkCode;

	private boolean wrkCodeInclusive;

	private String hType;

	private boolean hTypeInclusive;

	private String employee;

	private String calcGroup;

	private String payGroup;

	private String team;

	private int oriSqlLength;

	private java.util.List rows = new ArrayList();

	private String all = "ALL";

	private String empTable = "EMPLOYEE";

	private String empTeamTable = "EMPLOYEE_TEAM";

	/**Method initializes the row source*/
	private void initRowSource() {
		RowStructure rs = new RowStructure(6);
		rs.add("EMP_ID", StringType.get());
		rs.add("EMP_FULLNAME", StringType.get());
		rs.add("Work Date", StringType.get());
		rs.add("Start Time		", StringType.get());
		rs.add("End Time		", StringType.get());
		rs.add("Min Wrked W/Out Break", StringType.get());
		rowDefinition = new RowDefinition(-1, rs);
	}
    
	/**Constructor creates a row source structure using the load all employees method*/
	public WorkedNMinutesWithoutBreakAlertSource(DBConnection c, HashMap params)
			throws AccessException, SQLException {
		this.conn = c;

		initRowSource();
		try {
			loadParameters(params);
			loadAllEmployees();
		} catch (SQLException e) {
			throw new SQLException();
		}
	}

	private void loadParameters(HashMap params) {
		nMinutes = new Integer((String) params.get(PARAM_N_MINUTES)).intValue();
		brkCode = (String) params.get(PARAM_BRK_TCODE);
		
		wrkCode = (String) params.get(PARAM_WRK_TCODE);
		wrkCodeInclusive = new Boolean((String) params.get(PARAM_WRK_TCODE_INCLUSIVE)).booleanValue();
		hType = (String) params.get(PARAM_HTYPE);
		hTypeInclusive = new Boolean((String) params.get(PARAM_HTYPE_INCLUSIVE)).booleanValue();
		
		employee = (String) params.get(PARAM_EMPLOYEE);
		calcGroup = (String) params.get(PARAM_CALCGROUP);
		payGroup = (String) params.get(PARAM_PAYGROUP);
		team = (String) params.get(PARAM_TEAM);
	}

	/** loads all employees selected by client
	 *  Method is not meant to be run for large
	 *  numbers of employees at a time.  Therefore
	 *  the entire row source shall be returned.*/
	private void loadAllEmployees() throws AccessException, SQLException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		String sql = constructSQL();
		int[] empID = new int[1];
		try {
			ps = conn.prepareStatement(sql);
			rs = ps.executeQuery();
			while (rs.next()) {
				//for each employee, retrieve work details for the day before
				//and process accordingly.
				empID[0] = new Integer(rs.getString(1)).intValue();
				WorkDetailAccess wrkDetAccess = new WorkDetailAccess(conn);
				WorkDetailList wdl = null;
				wdl = wrkDetAccess.loadByEmpIdsAndDateRange(empID, DateHelper
						.addDays(DateHelper.getCurrentDate(), -1), DateHelper
						.addDays(DateHelper.getCurrentDate(), -1), "D", null);
				Iterator wdlIter = wdl.iterator();
				while (wdlIter.hasNext()) {
					processWorkDetail((WorkDetailData) wdlIter.next(), rs
							.getString(2));
				}

			}

		} catch (SQLException e) {
			throw new SQLException();
		} finally {
			if (rs != null)
				rs.close();

			if (ps != null)
				ps.close();
		}

	}
	
	/**Method constructs the sql statement according to the parameters"
	 * employee, calc group, pay group, team
	 * */
	private String constructSQL() {
		String sql = "SELECT E.EMP_ID, E.EMP_FULLNAME" + " FROM " + empTable
				+ " E";
		oriSqlLength = sql.length();

		StringTokenizer empTok = new StringTokenizer(employee, ",");
		StringTokenizer calcTok = new StringTokenizer(calcGroup, ",");
		StringTokenizer payTok = new StringTokenizer(payGroup, ",");
		StringTokenizer teamTok = new StringTokenizer(team, ",");

		// check if any of the selects have been used.

		if (empTok.hasMoreTokens() && !empTok.nextToken().equalsIgnoreCase(all))
			sql = groupSelect("employee", sql);
		if (calcTok.hasMoreTokens()
				&& !calcTok.nextToken().equalsIgnoreCase(all))
			sql = groupSelect("calcGroup", sql);
		if (payTok.hasMoreTokens() && !payTok.nextToken().equalsIgnoreCase(all))
			sql = groupSelect("payGroup", sql);
		if (teamTok.hasMoreTokens()
				&& !teamTok.nextToken().equalsIgnoreCase(all))
			sql = groupSelect("team", sql);

		return sql;
	}

	// This method adds to the original select statement in accordance with the
	// select statement
	// Use OR FOR multiple values of the same parameter
	// Use AND for different parameters
	private String groupSelect(String selectType, String sql) {
		String condition = null;
		StringTokenizer tok = null;
		boolean isFirstToken = true;
		String idType = null;

		if (selectType.equalsIgnoreCase("employee")) {
			idType = "E.EMP_ID";
			tok = new StringTokenizer(employee, ",");
		} else if (selectType.equalsIgnoreCase("calcGroup")) {
			idType = "E.CALCGRP_ID";
			tok = new StringTokenizer(calcGroup, ",");
		} else if (selectType.equalsIgnoreCase("payGroup")) {
			idType = "E.PAYGRP_ID";
			tok = new StringTokenizer(payGroup, ",");
		} else {
			idType = "T.WBT_ID";
			tok = new StringTokenizer(team, ",");
		}
		// specifically for the team case, we must access the employee team
		// table
		if (idType.equalsIgnoreCase("T.WBT_ID")) {
			if (sql.length() > oriSqlLength)
				sql = sql.substring(0, oriSqlLength) + ", " + empTeamTable
						+ " T" + sql.substring(oriSqlLength);
			else
				sql = sql + ", " + empTeamTable + " T";
			condition = "T.EMP_ID= E.EMP_ID AND";
		}

		while (tok.hasMoreTokens()) {
			String id = tok.nextToken();
			if (isFirstToken) {
				if (condition == null)
					condition = "(" + idType + "= " + id;
				else
					condition = condition + " (" + idType + "= " + id;
				isFirstToken = false;
			} else
				condition = condition + " OR " + idType + "= " + id;
		}
		condition = condition + ")";

		boolean noConditions = (sql.indexOf("WHERE") == -1);
		// if we have not added any conditions
		if (noConditions)
			sql = sql + " WHERE " + condition;
		// otherwise
		else
			sql = sql + " AND " + condition;
		return sql;
	}

	private void processWorkDetail(WorkDetailData wdd, String fullName)
			throws AccessException {
		// If it is not a break detail
		if (!(RuleHelper.isCodeInList(brkCode, wdd.getWrkdTcodeName()))) {
			// If not including all time codes, and this time code is not in the
			// included list, stop processing
			if (wrkCode != null
					&& ((wrkCodeInclusive== true && RuleHelper.isCodeInList(wrkCode, wdd.getWrkdTcodeName())!=true)  
						 || (wrkCodeInclusive== false && RuleHelper.isCodeInList(wrkCode, wdd.getWrkdTcodeName())==true) 	))
				return;
			// If not including all hour types, and this hour type is not in the
			// included list, stop processing
			if (hType != null 
					&& ((hTypeInclusive == true && RuleHelper.isCodeInList(hType, wdd.getWrkdTcodeName())!=true) || 
						(hTypeInclusive == false && RuleHelper.isCodeInList(hType, wdd.getWrkdTcodeName())==true)))
				return;
			if (wdd.getWrkdMinutes() < new Integer(nMinutes).intValue())
				return;
			Row r = new BasicRow(getRowDefinition());
			r.setValue("EMP_ID", new Integer(wdd.getEmpId()).toString());
			r.setValue("EMP_FULLNAME", fullName);
			r.setValue("Work Date", getDate(wdd.getWrkdWorkDate()));
			r.setValue("Start Time		", getTime(wdd.getWrkdStartTime()));
			r.setValue("End Time		", getTime(wdd.getWrkdEndTime()));
			r.setValue("Min Wrked W/Out Break", new Integer(wdd
					.getWrkdMinutes()).toString());
			rows.add(r);
		}
	}

	private String getDate(Date date) {
		String dateS = date.toString();
		int space = dateS.indexOf(" ");
		return dateS.substring(0, space);
	}
	
	private String getTime(Date time) {
		String dateS = time.toString();
		int dot = dateS.indexOf(".");
		return dateS.substring(0, dot-3);
	}

	public RowDefinition getRowDefinition() throws AccessException {
		return rowDefinition;
	}

	public RowCursor query(String queryString) throws AccessException {
		return queryAll();
	}

	public RowCursor query(String queryString, String orderByString)
			throws AccessException {
		return queryAll();
	}

	public RowCursor query(List keys) throws AccessException {
		return queryAll();
	}

	public RowCursor query(String[] fields, Object[] values)
			throws AccessException {
		return queryAll();
	}

	public RowCursor queryAll() throws AccessException {

		RowCursor rc = new AbstractRowCursor(this.getRowDefinition()) {
			private int counter = -1;

			protected Row getCurrentRowInternal() {
				return counter >= 0 && counter < rows.size() ? (BasicRow) rows
						.get(counter) : null;
			}

			protected boolean fetchRowInternal() throws AccessException {
				return ++counter < rows.size();
			}

			public void close() {
			}

		};
		return rc;

	}

	public boolean isReadOnly() {
		return true;
	}

	public int count() {
		return rows.size();
	}

	public int count(String where) {
		return rows.size();
	}

}
