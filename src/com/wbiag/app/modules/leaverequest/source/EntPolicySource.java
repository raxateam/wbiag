package com.wbiag.app.modules.leaverequest.source;

import com.workbrain.server.data.*;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;

import java.text.SimpleDateFormat;
import java.util.*;

import com.workbrain.app.modules.entitlements.EntDetailData;
import com.workbrain.app.modules.entitlements.EntitlementData;
import com.workbrain.app.modules.entitlements.EntitlementCodeMapper;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.data.ParameterList;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/** This rowsource retrieves all balance policies associated with a given
 * employee.
 *
 * @author crector
 */
public class EntPolicySource extends AbstractRowSource {

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(EntPolicySource.class);

  static String EMPID = "empId";
  static String CONNECTION = "connection";

  // data returned by this rowsource
  static String POL_ID = "POL_ID";
  static String POL_NAME = "POL_NAME";
  static String POL_DESC = "POL_DESC";
  static String ENT_NAME = "ENT_NAME";
  static String START_DATE = "START_DATE";
  static String END_DATE = "END_DATE";
  static String BAL_NAME = "BAL_NAME";

  // ENT_EMP_POLICY related definitions
  static String ENT_EMP_POLICY = "ENT_EMP_POLICY"; //table name
  static String ENTEMPPOL_ID = "ENTEMPPOL_ID"; // primary key
  static String EMP_ID = "EMP_ID"; // employee key
  static String ENTEMPPOL_START_DATE = "ENTEMPPOL_START_DATE"; // start date
  static String ENTEMPPOL_END_DATE = "ENTEMPPOL_END_DATE"; // end date
  static String ENTEMPPOL_ENABLED = "ENTEMPPOL_ENABLED";

  // ENT_POLICY definitions
  static String ENT_POLICY = "ENT_POLICY";
  static String ENTPOL_ID = "ENTPOL_ID";

  // ENT_ENTITLEMENT definitions
  static String ENT_ENTITLEMENT = "ENT_ENTITLEMENT";
  static String ENT_DESC = "ENT_DESC";
  static String ENT_ID = "ENT_ID";
  static String ENT_START_DATE = "ENT_START_DATE";
  static String ENT_END_DATE = "ENT_END_DATE";

  // ENT_DETAIL definitions
  static String ENT_DETAIL = "ENT_DETAIL";

  private final String DATE_FORMAT =  "MM/dd/yyyy";
  private SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
  private RowDefinition rowDefinition;
  private int empId;
  private Date todaysDate;
  //private String pBalName;
  private DBConnection connection = null;
  private java.util.List rows = new ArrayList();
  private final int COLUMNS_COUNT = 5;

  {
    RowStructure rs = new RowStructure(COLUMNS_COUNT);
    rs.add(POL_ID, CharType.get(100));
    rs.add(POL_NAME, CharType.get(100));
    rs.add(POL_DESC, CharType.get(100));
    //rs.add(ENT_NAME, CharType.get(100));
    rs.add(START_DATE, CharType.get(100));
    rs.add(END_DATE, CharType.get(100));
    //rs.add(BAL_NAME, CharType.get(100));
    //rs.add(POL_DATE,CharType.get(100));
    rowDefinition = new RowDefinition(-1,rs);
  }


  /**
   *
   * @param connection
   * @param list - expected to contain an employee ID
   * @throws AccessException
   */
  public EntPolicySource(DBConnection connection, ParameterList list)
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

    logger.debug("Loading entitlement policies for emp ID = " + empId);

    rows.clear();

    if (!EmployeeAccess.existsEmployee(this.connection , this.empId)) {
      throw new AccessException ("Employee Id not found : " + this.empId);
    }

    // Before accessing the ENT_POLICY table we query the ENT_EMP_POLICY,
    // which links the employee to a policy (or multiple policies).
    EntEmpPolicyAccess entEmpPolAccess =
      new EntEmpPolicyAccess(this.connection);
    EntEmpPolicyData entEmpPolData = new EntEmpPolicyData();
    // we display all current and future policies
    List entEmpPolList = entEmpPolAccess.loadRecordDataBetweenDates(
        entEmpPolData, ENT_EMP_POLICY,
        EMP_ID, this.empId,
        ENTEMPPOL_END_DATE, this.todaysDate, DateHelper.DATE_3000);

    if (entEmpPolList.isEmpty()) {
      logger.debug("Employee (id=" + this.empId + ") has no entitlement" +
          " policies");
      return;
    }

    Iterator iter = entEmpPolList.iterator();
    EntEmpPolicyData currData;
    EntPolicyData currPolData;
    RecordAccess ra = new RecordAccess(this.connection);


    while (iter.hasNext()) {
      currData = (EntEmpPolicyData) iter.next();

      // In the future we may want to show disabled policies. For now,
      // do not display them.
      if ( !StringHelper.equals("Y", currData.getEntemppolEnabled()) ){
        logger.debug("Will not load disabled entitlement policy (" +
            ENTEMPPOL_ID + "=" + currData.getEntemppolId() + ")");
        continue;
      }

      //currPolData = (EntPolicyData) ra.loadRecordDataByPrimaryKey(
      //    new EntPolicyData(), currData.getEmpId());
      List entPolList = ra.loadRecordData(
          new EntPolicyData(), ENT_POLICY, ENTPOL_ID,
          currData.getEntpolId());
      if ( entPolList.isEmpty() ) {
        // this should never happen
        throw new AccessException("No entitlement policy with ID = " +
          currData.getEntemppolId() + " found");
      }
      // Since we are querying by the primary key, we will only get one
      // result. We would have used loadRecordDataByPrimaryKey but it is
      // currently not implemented by EntPolicyData.
      currPolData = (EntPolicyData) entPolList.get(0);

      logger.debug("Loading entitlement policy " +
          currPolData.getEntpolName());

      Row r = new BasicRow(getRowDefinition());
      r.setValue(POL_ID, Integer.toString(currPolData.getEntpolId()));
      r.setValue(POL_NAME, currPolData.getEntpolName());
      r.setValue(POL_DESC, currPolData.getEntpolDesc());
      // the start and end dates are from the ent emp pol, not the policy
      r.setValue(START_DATE, sdf.format(currData.getEntemppolStartDate()));
      r.setValue(END_DATE, sdf.format(currData.getEntemppolEndDate()));
      rows.add(r);

      //addEntitlements(ra, currPolData.getEntpolId());

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

