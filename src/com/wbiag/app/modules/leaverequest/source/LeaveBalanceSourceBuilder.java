package com.wbiag.app.modules.leaverequest.source;

import com.workbrain.server.data.*;
import com.workbrain.sql.*;

public class LeaveBalanceSourceBuilder implements RowSourceBuilder{
  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveBalanceSourceBuilder.class);


  public LeaveBalanceSourceBuilder() {}

  public String getName(){
    return "LEAVE BALANCE SOURCE BUILDER";
  }

  public ParameterList getParameters() {
    // *** build the parameter list
    ParameterListImpl params = new ParameterListImpl();
    params.add (new ParameterImpl (
        LeaveBalanceSource.CONNECTION, DBConnection.class.getName(),
        true, true, "Database Connection"));
    params.add (new ParameterImpl (
        LeaveBalanceSource.EMPID, String.class.getName(),
        true, false, "Employee ID"));
    params.add (new ParameterImpl (
        LeaveBalanceSource.END_OF_PERIOD_MONTH, String.class.getName(),
        true, false, "End-of-period month (MM)"));
    params.add (new ParameterImpl (
        LeaveBalanceSource.END_OF_PERIOD_DAY, String.class.getName(),
        true, false, "End-of-period day (DD)"));


    return params;
  }

  public RowSource newInstance (ParameterList list)
      throws InstantiationException, IllegalArgumentException {
    RowSource rs = null;
    Parameter conn = list.findParam (LeaveBalanceSource.CONNECTION);
    // *** set the connection if it hasn't been set
    if (conn == null)
      throw new IllegalArgumentException ("Connection parameter not set");
    // *** check all parameters
    list.validateParams();
    try {
      rs = new LeaveBalanceSource ((DBConnection)conn.getValue(),list);
    } catch (AccessException e) {
      throw new InstantiationException (e.toString());
    }
    return rs;
  }
}