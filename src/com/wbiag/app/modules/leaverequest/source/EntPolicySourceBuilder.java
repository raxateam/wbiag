package com.wbiag.app.modules.leaverequest.source;

import com.workbrain.server.data.*;
import com.workbrain.sql.*;

public class EntPolicySourceBuilder implements RowSourceBuilder{
  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(EntPolicySourceBuilder.class);


  public EntPolicySourceBuilder() {}

  public String getName(){
    return "LEAVE ENT POLICY SOURCE BUILDER";
  }

  public ParameterList getParameters() {
    // *** build the parameter list
    ParameterListImpl params = new ParameterListImpl();
    params.add (new ParameterImpl (
        EntPolicySource.CONNECTION, DBConnection.class.getName(),
        true, true, "Database Connection"));
    params.add (new ParameterImpl (
        EntPolicySource.EMPID, String.class.getName(),
        true, false, "Employee ID"));

    return params;
  }

  public RowSource newInstance (ParameterList list)
  throws InstantiationException, IllegalArgumentException {
    RowSource rs = null;
    Parameter conn = list.findParam (EntPolicySource.CONNECTION);
    // *** set the connection if it hasn't been set
    if (conn == null)
      throw new IllegalArgumentException ("Connection parameter not set");
    // *** check all parameters
    list.validateParams();
    try {
      rs = new EntPolicySource ((DBConnection)conn.getValue(),list);
    } catch (AccessException e) {
      throw new InstantiationException (e.toString());
    }
    return rs;
  }
}