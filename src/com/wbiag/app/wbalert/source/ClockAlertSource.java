package com.wbiag.app.wbalert.source;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;


/**
 * Returns a list of all employees that are scheduled for the current day
 * that have issues with their clocks defined by the parameters.
 *
 * TODO:
 * - develop the .jsp front for all of the parameters
 * - develop the functionality for: first tcodes, last tcodes, clock seqs, tcode seqs
 *
*/
public class ClockAlertSource extends AbstractRowSource {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ClockAlertSource.class);

	// calculation groups filter on employees' timesheets to be evaluated
	public static final String PARAM_CALC_GROUPS = "paramCalcGroups";
	public static final String PARAM_CALC_GROUPS_DEFAULT = "-1"; // All

	// shift pattern filter on employees' timesheets to be evaluated
	public static final String PARAM_SHIFT_PATTERNS = "paramShftGroups";
	public static final String PARAM_SHIFT_PATTERNS_DEFAULT = "-1"; // All

	// pay group filter on employees' timesheets to be evaluated
	public static final String PARAM_PAY_GROUPS = "paramPayGroups";
	public static final String PARAM_PAY_GROUPS_DEFAULT = "-1"; // All

	// whether the alert should check WRKS_CLOCKS or WRKS_ORIG_CLOCKS
	public static final String PARAM_WRKS_CLOCK_SEL = "paramWrksClockSel";
	public static final String PARAM_WRKS_CLOCK_SEL_ORIG = "WRKS_ORIG_CLOCKS";
	public static final String PARAM_WRKS_CLOCK_SEL_CALC = "WRKS_CLOCKS";
	public static final String PARAM_WRKS_CLOCK_SEL_BOTH = "BOTH";
	public static final String PARAM_WRKS_CLOCK_SEL_DEFAULT = PARAM_WRKS_CLOCK_SEL_CALC;

	// whether to add to the alert days with no clocks
	public static final String PARAM_CHECK_NO_CLOCKS = "paramCheckNoClocks";
	public static final String PARAM_CHECK_NO_CLOCKS_DEFAULT = "YES";

	// whether to add to the alert days with uneven clocks
	public static final String PARAM_CHECK_UNEVEN_CLOCKS = "paramCheckUnevenClocks";
	public static final String PARAM_CHECK_UNEVEN_CLOCKS_DEFAULT = "YES";

	// add to the alert if any of the following clock types are first
	public static final String PARAM_INVALID_FIRST_CLOCKS = "paramInvalidFirstClocks";
	public static final String PARAM_INVALID_FIRST_CLOCKS_DEFAULT = ""; // none specified

	// add to the alert if any of the following clock types are last
	public static final String PARAM_INVALID_LAST_CLOCKS = "paramInvalidLastClocks";
	public static final String PARAM_INVALID_LAST_CLOCKS_DEFAULT = ""; // none specified

	// add to the alert if any of the following clock time codes are first
	public static final String PARAM_INVALID_FIRST_TCODES = "paramInvalidFirstTcodes";
	public static final String PARAM_INVALID_FIRST_TCODES_DEFAULT = ""; // none specified

	// add to the alert if any of the following clock time codes are last
	public static final String PARAM_INVALID_LAST_TCODES = "paramInvalidLastTcodes";
	public static final String PARAM_INVALID_LAST_TCODES_DEFAULT = ""; // none specified

	// add to the alert if any of the following sequences of clock types are present
	public static final String PARAM_INVALID_CLOCK_SEQS = "paramInvalidClockSeq";
	public static final String PARAM_INVALID_CLOCK_SEQS_DEFAULT = ""; // none specified

	// add to the alert if any of the following sequences of time codes are present
	public static final String PARAM_INVALID_TCODE_SEQS = "paramInvalidTcodeSeq";
	public static final String PARAM_INVALID_TCODE_SEQS_DEFAULT = ""; // none specified

	// Formica defaults:
	public static final String FORM_CALC_GROUPS_DEFAULT = "-1"; // All
	public static final String FORM_SHIFT_PATTERNS_DEFAULT = "-1"; // All
	public static final String FORM_PAY_GROUPS_DEFAULT = "-1"; // All
	public static final String FORM_WRKS_CLOCK_SEL_DEFAULT = PARAM_WRKS_CLOCK_SEL_CALC;
	public static final String FORM_CHECK_NO_CLOCKS_DEFAULT = "YES";
	public static final String FORM_CHECK_UNEVEN_CLOCKS_DEFAULT = "YES";
	public static final String FORM_INVALID_FIRST_CLOCKS_DEFAULT = "06,02";
	public static final String FORM_INVALID_LAST_CLOCKS_DEFAULT = "06,01";
	public static final String FORM_INVALID_FIRST_TCODES_DEFAULT = "";
	public static final String FORM_INVALID_LAST_TCODES_DEFAULT = "";
	public static final String FORM_INVALID_CLOCK_SEQS_DEFAULT = "";
	public static final String FORM_INVALID_TCODE_SEQS_DEFAULT = "";

    private RowDefinition rowDefinition;
    private java.util.List rows = new ArrayList();

    public static final String COL_EMP_ID = "EMP_ID";
    public static final String COL_EMP_NAME = "EMP_NAME" ;
    public static final String COL_EMP_FULLNAME = "EMP_FULLNAME";
    public static final String COL_WORK_DATE = "WORK_DATE";
    public static final String COL_SHIFT_NAME = "SHIFT_NAME";
    public static final String COL_START_TIME = "START_TIME";
    public static final String COL_END_TIME = "END_TIME";

	{
		RowStructure rs = new RowStructure(7);
		rs.add(COL_EMP_ID,CharType.get(100));
		rs.add(COL_EMP_NAME,CharType.get(100));
		rs.add(COL_EMP_FULLNAME,CharType.get(100));
		rs.add(COL_WORK_DATE,CharType.get(100));
		rs.add(COL_SHIFT_NAME,CharType.get(100));
		rs.add(COL_START_TIME,CharType.get(100));
		rs.add(COL_END_TIME,CharType.get(100));
		rowDefinition = new RowDefinition(-1,rs);
	}

	private DBConnection dbConnection = null;

	protected String calcGroups = null;
	protected String shiftPatterns = null;
	protected String payGroups = null;
	protected String wrksClocksSel = null;
  	protected boolean checkNoClocks = true;
  	protected boolean checkUnevenClocks = true;
  	protected Set invalidFirstClocks = new HashSet();
  	protected Set invalidLastClocks = new HashSet();

    /**
     * Retains a reference to the passed connection only;
     * calls prepareParameters(...), then loadRows();
     * and removes the local reference to the passed connection.
    */
    public ClockAlertSource(DBConnection dbConnection, HashMap alertParams) throws AccessException  {
		this.dbConnection = dbConnection;
        try {
	        prepareParameters(alertParams);
	        loadRows();
		} catch(Exception ex) {
			throw new NestedRuntimeException(ex);
		} finally {
			this.dbConnection = null;
		}
    }

    /**
     * @throws IllegalArgumentException for invalid parameters
    */
	protected void prepareParameters(HashMap alertParams) {
		// TODO: handle: first tcodes, last tcodes, clock seqs, tcode seqs

		String calcGroupsParam = (String)alertParams.get(PARAM_CALC_GROUPS);
		String shiftPatternsParam = (String)alertParams.get(PARAM_SHIFT_PATTERNS);
		String payGroupsParam = (String)alertParams.get(PARAM_PAY_GROUPS);
		String wrksClocksSelParam = (String)alertParams.get(PARAM_WRKS_CLOCK_SEL);
		String checkNoClocksParam = (String)alertParams.get(PARAM_CHECK_NO_CLOCKS);
		String checkUnevenClocksParam = (String)alertParams.get(PARAM_CHECK_UNEVEN_CLOCKS);
		String invalidFirstClocksParam = (String)alertParams.get(PARAM_INVALID_FIRST_CLOCKS);
		String invalidLastClocksParam = (String)alertParams.get(PARAM_INVALID_LAST_CLOCKS);

		// TODO: remove hard-coded parameters for Formica
		calcGroupsParam = FORM_CALC_GROUPS_DEFAULT;
		payGroupsParam = FORM_PAY_GROUPS_DEFAULT;
		wrksClocksSelParam = FORM_WRKS_CLOCK_SEL_DEFAULT;
		checkNoClocksParam = FORM_CHECK_NO_CLOCKS_DEFAULT;
		checkUnevenClocksParam = FORM_CHECK_UNEVEN_CLOCKS_DEFAULT;
		invalidFirstClocksParam = FORM_INVALID_FIRST_CLOCKS_DEFAULT;
		invalidLastClocksParam = FORM_INVALID_LAST_CLOCKS_DEFAULT;

		if(calcGroupsParam == null) {
			throw new IllegalArgumentException("No calculation group specified");
		} else if(calcGroupsParam.equals("-1")) {
			calcGroups = null;
		} else {
			calcGroups = calcGroupsParam;
		}
		if(logger.isDebugEnabled()) { logger.debug("Parameter: calcGroups=[" + calcGroups + "]"); }

		if(shiftPatternsParam == null) {
			throw new IllegalArgumentException("No shift patterns specified");
		} else if(shiftPatternsParam.equals("-1")) {
			shiftPatterns = null;
		} else {
			shiftPatterns = shiftPatternsParam;
		}
		if(logger.isDebugEnabled()) { logger.debug("Parameter: shiftPatterns=[" + shiftPatterns + "]"); }

		if(payGroupsParam == null) {
			throw new IllegalArgumentException("No pay group specified");
		} else if(payGroupsParam.equals("-1")) {
			payGroups = null;
		} else {
			payGroups = payGroupsParam;
		}
		if(logger.isDebugEnabled()) { logger.debug("Parameter: payGroups=[" + payGroups + "]"); }

		if(wrksClocksSelParam == null) {
			throw new IllegalArgumentException("No works clocks decision specified");
		} else if(		!wrksClocksSelParam.equalsIgnoreCase(PARAM_WRKS_CLOCK_SEL_ORIG)
		          &&	!wrksClocksSelParam.equalsIgnoreCase(PARAM_WRKS_CLOCK_SEL_CALC)
		          &&	!wrksClocksSelParam.equalsIgnoreCase(PARAM_WRKS_CLOCK_SEL_BOTH)
		          ) {
			throw new IllegalArgumentException("Invalid works clocks decision specified");
		} else {
			wrksClocksSel = wrksClocksSelParam.toUpperCase();
		}
		if(logger.isDebugEnabled()) { logger.debug("Parameter: wrksClocksSel=[" + wrksClocksSel + "]"); }

		if(checkNoClocksParam == null) {
			throw new IllegalArgumentException("No checking of no clocks decision specified");
		} else if(		checkNoClocksParam.startsWith("T")
		          ||	checkNoClocksParam.startsWith("Y")
		          ) {
			checkNoClocks = true;
		} else if(		checkNoClocksParam.startsWith("F")
		          ||	checkNoClocksParam.startsWith("N")
		          ) {
			checkNoClocks = false;
		} else {
			throw new IllegalArgumentException("Invalid checking of no clocks decision specified");
		}
		if(logger.isDebugEnabled()) { logger.debug("Parameter: checkNoClocks=[" + checkNoClocks + "]"); }

		if(checkUnevenClocksParam == null) {
			throw new IllegalArgumentException("No checking of uneven clocks decision specified");
		} else if(		checkUnevenClocksParam.startsWith("T")
		          ||	checkUnevenClocksParam.startsWith("Y")
		          ) {
			checkUnevenClocks = true;
		} else if(		checkUnevenClocksParam.startsWith("F")
		          ||	checkUnevenClocksParam.startsWith("N")
		          ) {
			checkUnevenClocks = false;
		} else {
			throw new IllegalArgumentException("Invalid checking of uneven clocks decision specified");
		}
		if(logger.isDebugEnabled()) { logger.debug("Parameter: checkUnevenClocks=[" + checkUnevenClocks + "]"); }

		if(invalidFirstClocksParam == null || invalidFirstClocksParam.trim().length() == 0) {
			invalidFirstClocks = new HashSet();
		} else {
			String[] clockTypes = StringHelper.detokenizeString(invalidFirstClocksParam, ",", true);
			for(int clockTypeIndex = 0 ; clockTypeIndex < clockTypes.length ; clockTypeIndex++) {
				invalidFirstClocks.add(clockTypes[clockTypeIndex]);
				if(logger.isDebugEnabled()) { logger.debug("Parameter: invalidFirstClock=[" + clockTypes[clockTypeIndex] + "]"); }
			}
		}

		if(invalidLastClocksParam == null || invalidLastClocksParam.trim().length() == 0) {
			invalidLastClocks = new HashSet();
		} else {
			String[] clockTypes = StringHelper.detokenizeString(invalidLastClocksParam, ",", true);
			for(int clockTypeIndex = 0 ; clockTypeIndex < clockTypes.length ; clockTypeIndex++) {
				invalidLastClocks.add(clockTypes[clockTypeIndex]);
				if(logger.isDebugEnabled()) { logger.debug("Parameter: invalidLastClock=[" + clockTypes[clockTypeIndex] + "]"); }
			}
		}
	}

    /**
     * Executes a query to retrieve all employees and their clocks for
     * employees who are scheduled on the same day as the alert run,
     * and who fit into the selection criteria (pay group, calc. group,
     * shift pattern), and are still active and not yet terminated.
    */
	protected void loadRows() throws AccessException, SQLException {
		rows.clear();

		StringBuffer sql = new StringBuffer();
		sql.append(" SELECT eh.emp_id");
		sql.append("      , eh.emp_name");
// TODO: SQL Server uses +, other DBs use || -> find a generic workaround
		sql.append("      , eh.emp_firstname + ' ' + eh.emp_lastname empFullName");
		sql.append("      , es.work_date");
		sql.append("      , sh.shft_name");
		sql.append("      , es.empskd_act_start_time");
		sql.append("      , es.empskd_act_end_time");
// TODO: future timesheet link - when alerts are allowed more characters
		sql.append("      , ws.wrks_clocks");
		sql.append("      , ws.wrks_orig_clocks");
		sql.append(" FROM employee_history eh");
		sql.append("    , employee_schedule es");
		sql.append("    , shift sh");
		sql.append("    , work_summary ws");
		sql.append(" WHERE es.work_date = ?"); // alertRuntimeDay
		sql.append("   AND (es.empskd_act_start_time <> es.empskd_act_end_time)");
		sql.append("   AND sh.shft_id = es.empskd_act_shift_id");
		sql.append("   AND es.emp_id = eh.emp_id");
		sql.append("   AND eh.emphist_start_date <= ?"); // alertRuntimeDay
		sql.append("   AND eh.emphist_end_date >= ?"); // alertRuntimeDay
		sql.append("   AND eh.emp_status = 'A'");
		sql.append("   AND eh.emp_termination_date > ?"); // alertRuntimeDay
// TODO: implement: pay group, calc group
		if(shiftPatterns != null && !shiftPatterns.equals("-1")) {
			sql.append("   AND eh.shftpat_id IN("); // alertRuntimeDay
			String[] shiftPatternIds = StringHelper.detokenizeString(shiftPatterns, ",");
			for(int shftPatIndex = 0 ; shftPatIndex < shiftPatternIds.length ; shftPatIndex++) {
				if(shftPatIndex > 0) {
					sql.append(",");
				}
				sql.append(shiftPatternIds[shftPatIndex]);
			}
			sql.append(")");
		}
		sql.append("   AND eh.emp_id = ws.emp_id");
		sql.append("   AND es.work_date = ws.wrks_work_date");
		sql.append(" ORDER BY es.work_date, sh.shft_name, empFullName");
		if(logger.isDebugEnabled()) { logger.debug("sql:\n" + sql); }

		PreparedStatement preparedStatement = null;
		ResultSet resultSet = null;

		// we gather dates here to wait as long as possible before executing the query
		// to ensure that we get as close as possible to the shift starting/ending
		Date currentDate = new Date();
		long currentTime = currentDate.getTime();
		Timestamp alertRuntime = new Timestamp(currentTime);
		long currentDay = DateHelper.truncateToDays(alertRuntime).getTime();
		Timestamp alertRuntimeDay = new Timestamp(currentDay);
		if(logger.isDebugEnabled()) { logger.debug("alertRuntime: " + alertRuntime); }
		if(logger.isDebugEnabled()) { logger.debug("alertRuntimeDay:" + alertRuntimeDay); }

		try {
			preparedStatement = dbConnection.prepareStatement(sql.toString());
			preparedStatement.setTimestamp(1, alertRuntimeDay);
			preparedStatement.setTimestamp(2, alertRuntimeDay);
			preparedStatement.setTimestamp(3, alertRuntimeDay);
			preparedStatement.setTimestamp(4, alertRuntimeDay);
			resultSet = preparedStatement.executeQuery();

			while(resultSet.next()) {
				int employeeId = resultSet.getInt(1);
				if(logger.isDebugEnabled()) { logger.debug("employeeId: " + employeeId); }
				String wrksClocks = resultSet.getString(8);
				if(logger.isDebugEnabled()) { logger.debug("wrksClocks: " + wrksClocks); }
				String wrksOrigClocks = resultSet.getString(9);
				if(logger.isDebugEnabled()) { logger.debug("wrksOrigClocks: " + wrksOrigClocks); }
				Date startTime = new Date(resultSet.getDate(6).getTime());
				Date endTime = new Date(resultSet.getDate(7).getTime());

				boolean clockingIssueExists = false;
				if(wrksClocksSel.equals(PARAM_WRKS_CLOCK_SEL_ORIG)) {
					clockingIssueExists = clockIssueExists(employeeId, wrksOrigClocks, startTime, endTime, currentDate);
				} else if(wrksClocksSel.equals(PARAM_WRKS_CLOCK_SEL_CALC)) {
					clockingIssueExists = clockIssueExists(employeeId, wrksClocks, startTime, endTime, currentDate);
				} else if(wrksClocksSel.equals(PARAM_WRKS_CLOCK_SEL_BOTH)) {
					clockingIssueExists =
					(	 clockIssueExists(employeeId, wrksOrigClocks, startTime, endTime, currentDate)
					  || clockIssueExists(employeeId, wrksClocks, startTime, endTime, currentDate)
					);
				}

				if(clockingIssueExists) {
				   Row row = new BasicRow(getRowDefinition());
				   row.setValue(COL_EMP_ID , resultSet.getString(1));
				   row.setValue(COL_EMP_NAME , resultSet.getString(2));
				   row.setValue(COL_EMP_FULLNAME , resultSet.getString(3));
				   row.setValue(COL_WORK_DATE , DateHelper.convertDateString(resultSet.getDate(4), "EEE MM/dd/yyyy"));
				   row.setValue(COL_SHIFT_NAME , resultSet.getString(5));
				   row.setValue(COL_START_TIME , DateHelper.convertDateString(startTime, "HH:mm:ss"));
				   row.setValue(COL_END_TIME , DateHelper.convertDateString(endTime, "HH:mm:ss"));
				   rows.add(row);
				}
			}
		} finally {
			if (logger.isDebugEnabled()) logger.debug("Loaded " + rows.size() + " rows.");
			SQLHelper.cleanUp(preparedStatement, resultSet);
		}
	}

	/** @return true iff a clocking issue exists as defined by the alert parameters; false otherwise. */
	protected boolean clockIssueExists(int employeeId, String clockString, Date shiftStartTime, Date shiftEndTime, Date alertRuntime) {

		if(logger.isDebugEnabled()) { logger.debug("Checking employeeId=[" + employeeId + "]");}

		List clockList = null;
		try {
			if(logger.isDebugEnabled()) { logger.debug("Clocks string=[" + clockString + "]");}
			if(clockString == null || clockString.trim().length() == 0) {
				clockList = new ArrayList();
			} else {
				clockList = Clock.createClockListFromString(clockString);
			}
		} catch (Exception ex) {
			logger.error("Can not create clock list", ex);
			throw new NestedRuntimeException(ex);
		}

		int clockCount = clockList.size();
		if(logger.isDebugEnabled()) { logger.debug("clockCount=[" + clockCount + "]"); }

 		// if the shift has started, check for the presence of clocks, uneven clocks and correct starting clocks
		if(shiftStartTime.compareTo(alertRuntime) <= 0) {
			if(logger.isDebugEnabled()) { logger.debug("Shift has started"); }
			if(checkNoClocks && clockCount == 0) {
				if(logger.isDebugEnabled()) { logger.debug("checkNoClocks failure"); }
				return true;
			}
			if(invalidFirstClocks.size() > 0 && clockCount > 0) {
				Clock firstClock = (Clock)clockList.get(0);
				int firstClockType = firstClock.getClockType();
				if(logger.isDebugEnabled()) { logger.debug("firstClockType=[" + firstClockType + "]"); }
				String firstClockTypeString = String.valueOf(firstClockType);
				if(firstClockTypeString.length() == 1) {
					firstClockTypeString = "0" + firstClockTypeString;
				}
				if(logger.isDebugEnabled()) { logger.debug("firstClockTypeString=[" + firstClockTypeString + "]"); }
				if(invalidFirstClocks.contains(firstClockTypeString)) {
					if(logger.isDebugEnabled()) { logger.debug("invalidFirstClocks failure"); }
					return true;
				}
			}
		}
 		// iff the shift has ended, check for the presence of correct ending clocks
		if(shiftEndTime.compareTo(alertRuntime) <= 0) {
			if(logger.isDebugEnabled()) { logger.debug("Shift has ended"); }
			if(checkUnevenClocks && (clockCount%2 != 0)) {
				if(logger.isDebugEnabled()) { logger.debug("checkUnevenClocks failure"); }
				return true;
			}
			if(invalidLastClocks.size() > 0 && clockCount > 0) {
				Clock lastClock = (Clock)clockList.get((clockList.size() - 1));
				int lastClockType = lastClock.getClockType();
				if(logger.isDebugEnabled()) { logger.debug("lastClockType=[" + lastClockType + "]"); }
				String lastClockTypeString = String.valueOf(lastClockType);
				if(lastClockTypeString.length() == 1) {
					lastClockTypeString = "0" + lastClockTypeString;
				}
				if(logger.isDebugEnabled()) { logger.debug("lastClockTypeString=[" + lastClockTypeString + "]"); }
				if(invalidLastClocks.contains(lastClockTypeString)) {
					if(logger.isDebugEnabled()) { logger.debug("invalidLastClocks failure"); }
					return true;
				}
			}
		}
		// TODO: handle: first tcodes, last tcodes, clock seqs, tcode seqs

		return false;
	}

	/** @see AbstractRowSource. */
	public RowDefinition getRowDefinition() throws AccessException {
		return rowDefinition;
	}

	/** @see AbstractRowSource. */
	public RowCursor query(String queryString) throws AccessException{
		return queryAll();
	}

	/** @see AbstractRowSource. */
	public RowCursor query(String queryString, String orderByString) throws AccessException{
		return queryAll();
	}

	/** @see AbstractRowSource. */
	public RowCursor query(List keys) throws AccessException{
		return queryAll();
	}

	/** @see AbstractRowSource. */
	public RowCursor query(String[] fields, Object[] values) throws AccessException {
		return queryAll();
	}

	/** @see AbstractRowSource. */
	public RowCursor queryAll()  throws AccessException{
		return new AbstractRowCursor(getRowDefinition()){
			private int counter = -1;
			protected Row getCurrentRowInternal(){
				return counter >= 0 && counter < rows.size() ? (BasicRow)rows.get(counter) : null;
			}
			protected boolean fetchRowInternal() throws AccessException{
				return ++counter < rows.size();
			}
			public void close(){}
		};
	}

	/** @see AbstractRowSource. */
	public boolean isReadOnly(){
		return true;
	}

	/** @see AbstractRowSource. */
	public int count() {
		return rows.size();
	}

	/** @see AbstractRowSource. */
	public int count(String where) {
		return rows.size();
	}
}
