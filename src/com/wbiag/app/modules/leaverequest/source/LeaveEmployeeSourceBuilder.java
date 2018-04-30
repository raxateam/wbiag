package com.wbiag.app.modules.leaverequest.source;


import com.workbrain.server.data.*;
import java.util.*;
import java.sql.*;
import com.workbrain.sql.*;

public class LeaveEmployeeSourceBuilder implements RowSourceBuilder{
  public LeaveEmployeeSourceBuilder() {}

  public String getName(){
    return "VACATION EMPLOYEE SOURCE BUILDER";
  }

  public ParameterList getParameters() {
    // *** build the parameter list
    ParameterListImpl params = new ParameterListImpl();
    params.add (new ParameterImpl ("connection", DBConnection.class.getName(),
        true, true, "Database Connection"));
    params.add (new ParameterImpl ("empId", String.class.getName(),
        true, false, "Employee Name"));
    return params;
  }

  public RowSource newInstance (ParameterList list)
  throws InstantiationException, IllegalArgumentException {
    RowSource rs = null;
    Parameter conn = list.findParam ("connection");
    // *** set the connection if it hasn't been set
    if (conn == null)
      throw new IllegalArgumentException ("Connection parameter not set");
    // *** check all parameters
    list.validateParams();
    try {
      rs = new LeaveEmployeeSource ((DBConnection)conn.getValue(),list);
    } catch (AccessException e) {
      throw new InstantiationException (e.toString());
    }
    return rs;
  }
}