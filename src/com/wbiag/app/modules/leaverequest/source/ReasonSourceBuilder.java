package com.wbiag.app.modules.leaverequest.source;

import com.workbrain.server.data.*;
import com.workbrain.sql.*;

public class ReasonSourceBuilder implements RowSourceBuilder{
  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(ReasonSourceBuilder.class);


  public ReasonSourceBuilder() {}

  public String getName(){
    return "REASON SOURCE BUILDER";
  }

  public ParameterList getParameters() {
    // *** build the parameter list
    ParameterListImpl params = new ParameterListImpl();
    params.add (new ParameterImpl (
        ReasonSource.CONNECTION, DBConnection.class.getName(),
        true, true, "Database Connection"));

    return params;
  }

  public RowSource newInstance (ParameterList list)
      throws InstantiationException, IllegalArgumentException {
    RowSource rs = null;
    Parameter conn = list.findParam (ReasonSource.CONNECTION);
    // *** set the connection if it hasn't been set
    if (conn == null)
      throw new IllegalArgumentException ("Connection parameter not set");
    // *** check all parameters
    list.validateParams();
    try {
      rs = new ReasonSource ((DBConnection)conn.getValue(),list);
    } catch (AccessException e) {
      throw new InstantiationException (e.toString());
    }
    return rs;
  }
}