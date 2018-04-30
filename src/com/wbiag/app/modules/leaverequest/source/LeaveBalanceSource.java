package com.wbiag.app.modules.leaverequest.source;

import com.wbiag.app.ta.model.LeaveData;
import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import java.util.*;
import java.text.SimpleDateFormat;
import com.workbrain.app.modules.entitlements.EntitlementEstimator;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.data.ParameterList;


public class LeaveBalanceSource extends AbstractRowSource {

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveBalanceSource.class);

  private final String DATE_FORMAT =  "MM/dd/yyyy";
  SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

  public static String EMPID = "empId";
  public static String CONNECTION = "connection";
  public static String END_OF_PERIOD_MONTH = "eopMonth";
  public static String END_OF_PERIOD_DAY = "eopDay";

  public static String BAL_NAME = "BAL_NAME";
  public static String BAL_ID = "BAL_ID";
  public static String BAL_TODAY = "BAL_TODAY";
  public static String BAL_ENDOFPERIOD = "BAL_ENDOFPERIOD";
  public static String BAL_ENDOFYEAR = "BAL_ENDOFYEAR";
  public static String BAL_INFINITY = "BAL_INFINITY";
  public static String BAL_TYPE = "BAL_TYPE";
  public static String END_OF_PERIOD = "END_OF_PERIOD";

  private static String TIME_CODE_BALANCE = "TIME_CODE_BALANCE";
  private static String TCODE_ID = "TCODE_ID";

  public static String EMPLOYEE_BALANCE = "EMPLOYEE_BALANCE";
  public static String EMP_ID = "EMP_ID";

  private RowDefinition rowDefinition;
  private int empId;
  private int endOfPeriodMonth, endOfPeriodDay;
  //private String pBalName;
  private DBConnection connection = null;
  private java.util.List rows = new ArrayList();
  private final int COLUMNS_COUNT = 6;

  {
    RowStructure rs = new RowStructure(COLUMNS_COUNT);
    rs.add(BAL_NAME,CharType.get(100));
    rs.add(BAL_ID,CharType.get(100));
    rs.add(BAL_TODAY,CharType.get(100));
    rs.add(BAL_ENDOFPERIOD,CharType.get(100));
    rs.add(BAL_ENDOFYEAR,CharType.get(100));
    rs.add(BAL_INFINITY,CharType.get(100));
    rs.add(BAL_TYPE,CharType.get(100));
    rs.add(END_OF_PERIOD,CharType.get(100));

    rowDefinition = new RowDefinition(-1,rs);
  }


  /**
   *
   * @param connection
   * @param list - expected to contain an employee ID
   * @throws AccessException
   */
  public LeaveBalanceSource(DBConnection connection, ParameterList list)
      throws AccessException {

    String empIdParam = (String)
      list.findParam (LeaveBalanceSource.EMPID).getValue();

    // *** do not attempt if params are null or strings like #request.TextBox#
    if ((empIdParam == null) || (empIdParam.indexOf("#") != -1)) {
      logger.debug("Employee ID is malformed or null");
      return;
    }

    this.endOfPeriodMonth = Integer.parseInt( (String)
        list.findParam (LeaveBalanceSource.END_OF_PERIOD_MONTH).getValue());
    // java.util.Calendar uses 0 for the first month, so we adjust here
    this.endOfPeriodMonth -= 1;
    this.endOfPeriodDay = Integer.parseInt( (String)
        list.findParam (LeaveBalanceSource.END_OF_PERIOD_DAY).getValue());

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

  private void loadRows() throws Exception {

    rows.clear();

    CodeMapper mapper = CodeMapper.createCodeMapper(this.connection);
    EmployeeBalanceAccess eba = new EmployeeBalanceAccess (this.connection);

    // set up date-related objects...
    Datetime today = DateHelper.getCurrentDate();
    Date endOfYear = DateHelper.getUnitYear(DateHelper.APPLY_ON_LAST_DAY ,
        false, DateHelper.getCurrentDate());

    //  the end-of-period date is configurable in the form builder source
    logger.debug("end-of-period month/day = " + this.endOfPeriodMonth +
        "/" + this.endOfPeriodDay);
    Calendar eopCal = DateHelper.getCalendar();
    eopCal.set(Calendar.MONTH, this.endOfPeriodMonth);
    eopCal.set(Calendar.DAY_OF_MONTH, this.endOfPeriodDay);
    Calendar jan1 = DateHelper.getCalendar(); // of next year
    jan1.set(Calendar.MONTH, 0);
    jan1.set(Calendar.DAY_OF_MONTH, 1);
    jan1.set(Calendar.YEAR, jan1.get(Calendar.YEAR)+1);

    // advance the end-of-period year, if the specified day/month has
    // already past
    logger.debug("eop = " + sdf.format(eopCal.getTime()));
    logger.debug("today = " + sdf.format(today));
    logger.debug("jan 1 = " + sdf.format(jan1.getTime()));
    if ( !DateHelper.isBetween(eopCal.getTime(), today, jan1.getTime()) ) {
      eopCal.set(Calendar.YEAR, eopCal.get(Calendar.YEAR)+1);
    }

    String formattedEopDate = sdf.format(eopCal.getTime());
    logger.debug("end of period date = " + formattedEopDate);

    List leaveList = LeaveSource.loadLeaveForEmployee(
        this.connection, this.empId);
    Iterator leaveIter = leaveList.iterator();

    while (leaveIter.hasNext()) {
      LeaveData leaveData = (LeaveData) leaveIter.next();
      int tcodeId = leaveData.getTcodeId();

      ArrayList tcodeBalList = mapper.getTCBByTimeCodeId(tcodeId);
      Iterator tcodeBalIter = tcodeBalList.iterator();

      while (tcodeBalIter.hasNext()) {

        TimeCodeBalanceData tcbData =
          (TimeCodeBalanceData) tcodeBalIter.next();
        BalanceData bd = mapper.getBalanceById(tcbData.getBalId());
        if (bd == null) {
          logger.warn("Balance not found, ID=" + tcbData.getBalId());
          continue;
        }

        // There is a chance that the employee does not have the
        // particular balance,
        if ( !employeeHasBalance(bd.getBalId(), eba) ) {
          logger.debug("Employee does not have balance for bal ID " +
              bd.getBalId());
          continue;
        }
        logger.debug("loading balances for " + bd.getBalName());

        // retrieve the balance values for the various dates. We add a day to each
        // to get the actual value at the end of the particular day.
        String balName = bd.getBalName();
        int balId = bd.getBalId();
        double valToday = eba.getBalance(this.empId,
            new Date(today.getTime() + DateHelper.DAY_MILLISECODS),
            balId);
        double valEndOfYear = eba.getBalance(this.empId,
            new Date(endOfYear.getTime() + DateHelper.DAY_MILLISECODS),
            balId);
        double valInfin = eba.getBalance(this.empId,
            DateHelper.DATE_3000, balId);

        double valEop = eba.getBalance(this.empId,
            new Date(eopCal.getTime().getTime() + DateHelper.DAY_MILLISECODS),
            balId);
        logger.debug("end of period balance before accruals = " + valEop);

        // for the end-of-period value we also take into account future
        // accruals by using the entitlement estimator
        EntitlementEstimator estimator = new EntitlementEstimator(connection);
        estimator.setEmpId(empId);
        estimator.setBalId(balId);
        estimator.setStartDate(today);
        estimator.setEndDate(eopCal.getTime());
        valEop += estimator.estimateEntitlement();
        logger.debug("end of period balance after accruals = " + valEop);

        Row r = new BasicRow(getRowDefinition());
        r.setValue(BAL_NAME , balName);
        r.setValue(BAL_ID , Integer.toString(balId));
        r.setValue(BAL_TODAY , String.valueOf(valToday));
        r.setValue(BAL_ENDOFPERIOD , String.valueOf(valEop));
        r.setValue(BAL_ENDOFYEAR , String.valueOf(valEndOfYear));
        r.setValue(BAL_INFINITY , String.valueOf(valInfin));
        r.setValue(END_OF_PERIOD, formattedEopDate);
        BalanceTypeData btd = mapper.getBalanceTypeById(bd.getBaltypId());
        if (btd == null) {
          throw new AccessException ("Balance type not found. ID = " +
              bd.getBaltypId());
        }
        r.setValue(BAL_TYPE , btd.getBaltypName());

        rows.add(r);
      }

    }
  }

  private boolean employeeHasBalance(int balId, EmployeeBalanceAccess eba ) {

    List ebList = eba.loadRecordData(new EmployeeBalanceData(),
        EMPLOYEE_BALANCE, BAL_ID, balId, EMP_ID, this.empId);
    if (ebList.size() > 0 ) return true;

    return false;
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
}

