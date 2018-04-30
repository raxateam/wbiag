package com.wbiag.app.modules.leaverequest.source;

import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import java.util.*;

import com.workbrain.app.modules.entitlements.EntitlementData;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.data.ParameterList;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/**
 *
 * @author crector
 */
public class LeaveEntSource extends AbstractRowSource {

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveEntSource.class);

  static String EMPID = "empId";
  static String CONNECTION = "connection";

  static String ENT_ENTITLEMENT = "ENT_ENTITLEMENT";
  static String ENT_NAME = "ENT_NAME";
  static String ENT_DESC = "ENT_DESC";
  static String ENT_ID = "ENT_ID";
  static String ENT_START_DATE = "ENT_START_DATE";
  static String ENT_END_DATE = "ENT_END_DATE";

  private RowDefinition rowDefinition;
  private int empId;
  private Date todaysDate;
  //private String pBalName;
  private DBConnection connection = null;
  private java.util.List rows = new ArrayList();
  private final int COLUMNS_COUNT = 5;

  {
    RowStructure rs = new RowStructure(COLUMNS_COUNT);
    rs.add(ENT_NAME, CharType.get(100));
    rs.add(ENT_DESC, CharType.get(100));
    rs.add(ENT_ID, CharType.get(100));
    rs.add(ENT_START_DATE, CharType.get(100));
    rs.add(ENT_END_DATE, CharType.get(100));
    rowDefinition = new RowDefinition(-1,rs);
  }


  /**
   *
   * @param connection
   * @param list - expected to contain an employee ID
   * @throws AccessException
   */
  public LeaveEntSource(DBConnection connection, ParameterList list)
      throws AccessException {

    String empIdParam = (String)
      list.findParam (EMPID).getValue();

    // *** do not attempt if params are null or strings like #request.TextBox#
    if ((empIdParam == null) || (empIdParam.indexOf("#") != -1)) {
      logger.debug("Employee ID is malformed or null");
      return;
    }
    this.empId = Integer.parseInt(empIdParam.trim()) ;

    this.todaysDate = new Date();

    this.connection = connection;
    try {
      loadRows();
    } catch (Exception e) {
      e.printStackTrace();
      logger.error("Error loading rows");
      throw new AccessException("Error loading rows", e);
    }
  }

  private void loadRows() throws Exception{

    logger.debug("Loading entitlements for emp ID = " + empId);

    rows.clear();

    if (!EmployeeAccess.existsEmployee(this.connection , this.empId)) {
      throw new AccessException ("Employee Id not found : " + this.empId);
    }

    RecordAccess ra = new RecordAccess(this.connection);

    List entList = ra.loadRecordDataBetweenDates(
        new EntitlementData(), ENT_ENTITLEMENT, "1", 1,
        ENT_START_DATE, ENT_END_DATE, this.todaysDate);

    EntitlementData eed;
    Iterator entIter = entList.iterator();
    while ( entIter.hasNext() ) {
      eed = (EntitlementData) entIter.next();
      Row r = new BasicRow(getRowDefinition());
      r.setValue(ENT_NAME, eed.getEntName());
      r.setValue(ENT_ID, Integer.toString(eed.getEntId()));
      r.setValue(ENT_DESC, eed.getEntDesc());
      r.setValue(ENT_START_DATE, eed.getEntStartDate());
      r.setValue(ENT_END_DATE, eed.getEntEndDate());

      rows.add(r);
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
}

