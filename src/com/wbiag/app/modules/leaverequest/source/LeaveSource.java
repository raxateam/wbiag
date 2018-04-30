package com.wbiag.app.modules.leaverequest.source;

import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.wbiag.app.ta.model.LeaveData;
import com.workbrain.app.ta.model.BalanceData;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.model.TimeCodeData;
import com.workbrain.server.data.ParameterList;

/**
 * This class retrieves the types of leave that are requestable through
 * the leave request form.
 *
 * @author crector
 *
 */
public class LeaveSource extends AbstractRowSource {

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveSource.class);

  public static String CONNECTION = "connection";
  public static String EMPID = "empId";
  private static String WGLVE_BAL_NAME = "WGLVE_BAL_NAME";
  private static String WGLVE_TCODE_NAME = "WGLVE_TCODE_NAME";
  private static String ALL = "All";
  private static String FULLTIME = "Full-time";
  private static String PARTTIME = "Part-time";

  private RowDefinition rowDefinition;
  //private String pBalName;
  private DBConnection connection = null;
  private int empId;
  private java.util.List rows = new ArrayList();
  private final int COLUMNS_COUNT = 6;

  {
    RowStructure rs = new RowStructure(COLUMNS_COUNT);
    rs.add(LeaveData.WGLVE_ID, CharType.get(100));
    rs.add(LeaveData.WGLVE_NAME, CharType.get(100));
    rs.add(LeaveData.TCODE_ID, CharType.get(100));
    rs.add(WGLVE_TCODE_NAME, CharType.get(100));
    rs.add(LeaveData.WGLVE_DESC, CharType.get(100));
    rs.add(WGLVE_BAL_NAME, CharType.get(100));
    rs.add(LeaveData.WGLVE_FULL_DAY, CharType.get(100));
    rs.add(LeaveData.WGLVE_PAY_CURRENT, CharType.get(100));
    rs.add(LeaveData.WGLVE_CUR_TCODE_ID, CharType.get(100));
    rs.add(LeaveData.WGLVE_FUT_TCODE_ID, CharType.get(100));
    //rs.add(LeaveData.WGLVE_RESTRICT_CG, CharType.get(100));
    rs.add(LeaveData.WGLVE_RESTRICT_POL, CharType.get(100));
    rs.add(LeaveData.WGLVE_MAX_DAYS, CharType.get(100));
    rs.add(LeaveData.WGLVE_REASON_REQD, CharType.get(100));
    rs.add(LeaveData.WGLVE_COMMENT_REQD, CharType.get(100));
    rowDefinition = new RowDefinition(-1,rs);
  }


  /**
   *
   * @param connection
   * @param list - expected to contain an employee ID
   * @throws AccessException
   */
  public LeaveSource(DBConnection connection, ParameterList list)
      throws AccessException {

    String empIdParam = (String) list.findParam(EMPID).getValue();

    if ((empIdParam == null) || (empIdParam.indexOf("#") != -1)) {
      logger.debug("Employee ID is malformed or null");
      return;
    }
    this.empId = Integer.parseInt(empIdParam.trim()) ;

    this.connection = connection;
    try {
      loadRows();
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Error loading rows");
      throw new AccessException("Error loading rows", e);
    }
  }

  /** Returns a list of LeaveData objects, taking into account the
   * employee. For leave entries that have the restrict-by-calcgroup
   * flag set this method will only return this leave if the employee
   * is part of a calc group that is linked to this type of leave.
   *
   * @param conn
   * @param empId
   * @return
   * @throws Exception
   */
  public static List loadLeaveForEmployee(DBConnection conn, int empId)
      throws Exception {

    Vector returnVector = new Vector();

    RecordAccess ra = new RecordAccess(conn);
    CodeMapper mapper = CodeMapper.createCodeMapper(conn);
    List leaveList = ra.loadRecordData(new LeaveData(),
        LeaveData.TABLE_NAME, "1=1");

    if (leaveList.isEmpty()) {
      logger.debug("No leave entries found");
      return returnVector;
    }

    Iterator iter = leaveList.iterator();

    while ( iter.hasNext() ) {

      LeaveData leaveData = (LeaveData) iter.next();
      logger.debug("found leave " + leaveData.getWglveName());

/*      if ( "Y".equals(leaveData.getWglveRestrictCg()) ) {
        // then we need to make sure that this employee is part of a
        // calc group that is associated with this leave. If they are
        // not, then we won't return the leave record.
        EmployeeAccess ea = new EmployeeAccess(conn, mapper);
        EmployeeData employee =
          ea.load(empId, DateHelper.getCurrentDate());
        List leaveCgList = ra.loadRecordData(new LeaveCalcGroupData(),
            LeaveCalcGroupData.TABLE_NAME,
            LeaveCalcGroupData.CALCGRP_ID, employee.getCalcgrpId(),
            LeaveCalcGroupData.WGLVE_ID, leaveData.getWglveId());
        logger.debug("found " + leaveCgList.size() + " " +
            LeaveCalcGroupData.TABLE_NAME + " entries ");
        if ( leaveCgList.size() == 0 ) {
          logger.debug("Will not return leave type " + leaveData.getWglveName() +
          " because of calc group restrictions");
          continue;
        }
      }*/

      returnVector.add(leaveData);
    }

    return returnVector;
  }

  private void loadRows() throws Exception{

    logger.debug("Loading leave codes rows from db");

    rows.clear();

    RecordAccess ra = new RecordAccess(connection);
    CodeMapper mapper = CodeMapper.createCodeMapper(this.connection);
    List leaveList = ra.loadRecordData(new LeaveData(),
        LeaveData.TABLE_NAME, "1=1");

    if (leaveList.isEmpty()) {
      logger.debug("No leave entries found");
      return;
    }

    Iterator iter = LeaveSource.loadLeaveForEmployee(
        this.connection, this.empId).iterator();

    while ( iter.hasNext() ) {
      LeaveData leaveData = (LeaveData) iter.next();
      Row row = new BasicRow(getRowDefinition());

      EmployeeAccess empAccess = new EmployeeAccess(this.connection,
          CodeMapper.createCodeMapper(this.connection));
      Date today = DateHelper.getCurrentDate();
      EmployeeData employee = empAccess.load(this.empId, today);

      if (leaveData.getWglveMinDaysEmp() > 0 &&
          !employeeWorkedMinDays(leaveData, employee))
        continue;

      if ( !employeeIsOfCorrectType(leaveData, employee) )
        continue;

      row.setValue(LeaveData.WGLVE_ID, Integer.toString(leaveData.getWglveId()));
      row.setValue(LeaveData.WGLVE_NAME, leaveData.getWglveName());
      row.setValue(LeaveData.TCODE_ID,
          Integer.toString(leaveData.getTcodeId()));
      row.setValue(LeaveData.WGLVE_DESC, leaveData.getWglveDesc());
      row.setValue(LeaveData.WGLVE_FULL_DAY, leaveData.getWglveFullDay());
      row.setValue(LeaveData.WGLVE_PAY_CURRENT, leaveData.getWglvePayCurrent());
      row.setValue(LeaveData.WGLVE_CUR_TCODE_ID,
          Integer.toString(leaveData.getWglveCurTcodeId()));
      row.setValue(LeaveData.WGLVE_FUT_TCODE_ID,
          Integer.toString(leaveData.getWglveFutTcodeId()));
      //row.setValue(LeaveData.WGLVE_RESTRICT_CG, leaveData.getWglveRestrictCg());
      row.setValue(LeaveData.WGLVE_RESTRICT_POL, leaveData.getWglveRestrictPol());
      row.setValue(LeaveData.WGLVE_MAX_DAYS,
          Integer.toString(leaveData.getWglveMaxDays()));
      row.setValue(LeaveData.WGLVE_REASON_REQD, leaveData.getWglveReasonReqd());
      row.setValue(LeaveData.WGLVE_COMMENT_REQD, leaveData.getWglveCommentReqd());

      // now we add the tcode name for convenience
      TimeCodeData tcd = mapper.getTimeCodeById(leaveData.getTcodeId());
      row.setValue(WGLVE_TCODE_NAME, tcd.getTcodeName());

      rows.add(row);
    }

  }


  public RowDefinition getRowDefinition() throws AccessException {
    return rowDefinition;
  }

  public RowCursor query(String queryString) throws AccessException{
    return queryAll();
  }

  public RowCursor query(String queryString, String orderByString) throws AccessException{
    return queryAll();
  }

  public RowCursor query(List keys) throws AccessException{
    return queryAll();
  }

  public RowCursor query(String[] fields, Object[] values) throws AccessException {
    return queryAll();
  }

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

  public boolean isReadOnly(){
    return true;
  }

  public int count() {
    return rows.size();
  }

  public int count(String where) {
    return rows.size();
  }

  /** Checks that the employee has worked a minimum number of days, as
   * specified in the leave table.
   *
   * @param leaveData - a LeaveData object representing the type of leave
   * @return - true if the employee has worked the minimum number of days
   * @throws Exception
   */
  private boolean employeeWorkedMinDays(LeaveData leaveData,
      EmployeeData employee) throws Exception {

    Date today = DateHelper.getCurrentDate();
    Date hireDate = employee.getEmpHireDate();

    int employedDays = DateHelper.dateDifferenceInDays(today, hireDate);
    logger.debug("Employee has worked for " + employedDays + " days");
    int minDays = leaveData.getWglveMinDaysEmp();
    logger.debug("Minimum req'd days for leave type = " + minDays);

    if ( employedDays < minDays )
      return false;
    else
      return true;
  }

  /** Checks that this leave type is available to the employee
   * based on whether he/she is full-time or part-time.
   *
   * @param leaveData - the leave that may or may not be available
   * @return
   * @throws Exception
   */
  private boolean employeeIsOfCorrectType(LeaveData leaveData,
      EmployeeData employee) throws Exception {

    logger.debug("Leave type " + leaveData.getWglveName() + " applies " +
        "to employees of type: " + leaveData.getWglveEmpType());

    if (ALL.equals(leaveData.getWglveEmpType()))
      return true;

    logger.debug("Employee (id="+this.empId+") fulltime status = " +
        employee.getEmpFulltime());

    if ( "Y".equals(employee.getEmpFulltime()) &&
        FULLTIME.equals(leaveData.getWglveEmpType()))
      return true;
    if ( "N".equals(employee.getEmpFulltime()) &&
        PARTTIME.equals(leaveData.getWglveEmpType()))
      return true;
    else
      return false;
  }

}

