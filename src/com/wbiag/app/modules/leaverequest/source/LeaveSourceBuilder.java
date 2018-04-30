package com.wbiag.app.modules.leaverequest.source;

import com.wbiag.app.ta.model.LeaveData;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmployeeBalanceAccess;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.app.ta.model.BalanceData;
import com.workbrain.server.data.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.model.EmployeeBalanceData;
import java.util.*;

public class LeaveSourceBuilder implements RowSourceBuilder{
  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveSourceBuilder.class);


  public LeaveSourceBuilder() {}

  public String getName(){
    return "LEAVE SOURCE BUILDER";
  }

  public ParameterList getParameters() {
    // *** build the parameter list
    ParameterListImpl params = new ParameterListImpl();
    params.add (new ParameterImpl (
        LeaveSource.CONNECTION, DBConnection.class.getName(),
        true, true, "Database Connection"));
    params.add (new ParameterImpl (
        LeaveSource.EMPID, String.class.getName(),
        true, false, "Employee ID"));

    return params;
  }

  public RowSource newInstance (ParameterList list)
      throws InstantiationException, IllegalArgumentException {
    RowSource rs = null;
    Parameter conn = list.findParam (LeaveSource.CONNECTION);
    // *** set the connection if it hasn't been set
    if (conn == null)
      throw new IllegalArgumentException ("Connection parameter not set");
    // *** check all parameters
    list.validateParams();
    try {
      rs = new LeaveSource ((DBConnection)conn.getValue(),list);
    } catch (AccessException e) {
      throw new InstantiationException (e.toString());
    }
    return rs;
  }



  /**
   * Fetches a hashtable of tcode name/balance ID pairs for all the
   * timecodes that can be requested in the leave request form. The balance
   * ID is stored as a string in the hashtable.
   *
   * @param conn
   * @return -  a hashtable of tcode name/balance ID pairs
   * @throws Exception
   */
/*  public static Hashtable getLeaveTcodesBalIdHash(DBConnection conn)
  throws Exception {
    Hashtable result = new Hashtable();

    RecordAccess ra = new RecordAccess(conn);
    Iterator leaveList = ra.loadRecordData(new LeaveData(),
        LeaveData.TABLE_NAME, "1=1").iterator();

    while( leaveList.hasNext() ) {
      LeaveData ld = (LeaveData) leaveList.next();
      result.put(ld.getTcodeName(conn), Integer.toString(ld.getBalId()) );
    }
    return result;
  }*/

  /** Returns a vector of strings of all the valid leave timecodes. It will
   * include the main leave timecodes (e.g. VAC), as well as the future
   * unpaid timecodes (e.g. VAC-ADV, for leave types that allow the
   * pay-current-period option).
   *
   * @param conn
   * @return a Vector of strings
   * @throws Exception
   */
  public static Vector getLeaveOverrideTcodes( DBConnection conn)
      throws Exception {
    Vector result = new Vector();

    RecordAccess ra = new RecordAccess(conn);
    Iterator leaveList = ra.loadRecordData(new LeaveData(),
        LeaveData.TABLE_NAME, "1=1").iterator();
    CodeMapper mapper = CodeMapper.createCodeMapper(conn);

    while( leaveList.hasNext() ) {
      LeaveData ld = (LeaveData) leaveList.next();
      result.add(ld.getTcodeName(conn, mapper));

      // If the leave allows the pay-current-period option we need to also
      // add the future tcode (e.g. VAC-ADV)
      if ( "Y".equalsIgnoreCase(ld.getWglvePayCurrent()) ) {
        result.add(ld.getFutureTcodeName(conn, mapper));
      }
    }
    return result;
  }

  public static LeaveData getLeaveDataById(DBConnection conn, int id)
  throws Exception {

    RecordAccess ra = new RecordAccess(conn);
    Iterator leaveList = ra.loadRecordData(new LeaveData(),
        LeaveData.TABLE_NAME, LeaveData.WGLVE_ID, id).iterator();

    LeaveData ld = (LeaveData) leaveList.next();

    return ld;
  }

/*  public static Vector getLeaveBalanceIds( DBConnection conn)
      throws Exception {
    Vector result = new Vector();

    RecordAccess ra = new RecordAccess(conn);
    Iterator leaveList = ra.loadRecordData(new LeaveData(),
        LeaveData.TABLE_NAME, "1=1").iterator();


    while( leaveList.hasNext() ) {
      LeaveData ld = (LeaveData) leaveList.next();
      result.add(Integer.toString(ld.getBalId()));
    }

    return result;
  }*/

  public static int getTcodeIdForLeaveId( DBConnection conn, int leaveId)
      throws Exception {

    RecordAccess ra = new RecordAccess(conn);
    Iterator leaveList = ra.loadRecordData(new LeaveData(),
        LeaveData.TABLE_NAME, LeaveData.WGLVE_ID, leaveId).iterator();

    LeaveData ld = (LeaveData) leaveList.next();
    return ld.getTcodeId();
  }

  public static String getTcodeNameForLeaveId( DBConnection conn,
      int leaveId)  throws Exception {
    RecordAccess ra = new RecordAccess(conn);
    Iterator leaveList = ra.loadRecordData(new LeaveData(),
        LeaveData.TABLE_NAME, LeaveData.WGLVE_ID, leaveId).iterator();

    LeaveData ld = (LeaveData) leaveList.next();
    CodeMapper mapper = CodeMapper.createCodeMapper(conn);
    return ld.getTcodeName(conn, mapper);
  }

/*  public static Vector getLeaveTcodesForEmp( DBConnection conn, int empId)
      throws Exception {
    logger.debug("Retrieving leave tcodes for empId " + empId);

    Vector result = new Vector();
    Hashtable tcodeBalIdHash =
      LeaveSourceBuilder.getLeaveTcodesBalIdHash(conn);
    List leaveBalIds = (List) tcodeBalIdHash.values();
    List leaveTcodeNames = (List) tcodeBalIdHash.keys();
    //Vector leaveBalIds = LeaveSourceBuilder.getLeaveBalanceIds(conn);

    EmployeeBalanceAccess eba = new EmployeeBalanceAccess(conn);
    List empBalList = eba.loadRawData(empId);
    // we only need to load the raw data because we are not interested
    // in the actually numeric balances, only the list of balances that
    // this employee has

    if (empBalList.isEmpty() ) {
      logger.debug("Employee balance list is empty");
      return result;
    }

    // we only want to add balances that a) match a the list of
    // requestable balances/timecodes and b) are given to this
    // employee
    Iterator balIter = empBalList.iterator();
    EmployeeBalanceData ebd;
    int index = -1;
    while ( balIter.hasNext() ) {
      ebd = (EmployeeBalanceData) balIter.next();
      logger.debug("Processing balance " + ebd.getBalId());
      index = leaveBalIds.indexOf(Integer.toString(ebd.getBalId()));

      if ( index >= 0 ) { // then the employee has this balances
        logger.debug("Adding tcode " + leaveTcodeNames.get(index) );
        result.add(leaveTcodeNames.get(index));
      }
    }

    return result;
  }
 */
}