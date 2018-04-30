package com.wbiag.app.modules.leaverequest.source;

import com.workbrain.server.data.*;

import java.util.*;
import java.sql.*;
import com.workbrain.sql.*;
import com.workbrain.app.modules.vacationrequest.source.VacationSource;;

public class LeaveReqSourceBuilder implements RowSourceBuilder{
  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveReqSourceBuilder.class);

  public static String EMPID = "empId";
  public static String CONNECTION = "connection";
  public static String LTATCODENAMES = "ltaTcodeNames";
  public static String USERLOCALE = "userLocale";

  public LeaveReqSourceBuilder() {}

  public String getName(){
    return "LEAVE REQUEST SOURCE BUILDER";
  }

  public ParameterList getParameters() {
    // *** build the parameter list
    ParameterListImpl params = new ParameterListImpl();
    ParameterImpl param = new ParameterImpl (
        CONNECTION, DBConnection.class.getName(),
        true, true, "Database Connection");
    params.add (param);
    param = new ParameterImpl (EMPID, String.class.getName(),
        true, false, "Employee ID");
    params.add (param);
    param = new ParameterImpl (USERLOCALE, String.class.getName(),
        true, false, "User locale", "#page.property.defaultLocale#");
    params.add (param);

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

    // Here we build a list of requestable timecodes to give to
    // VacationSource
    StringBuffer ltaString = new StringBuffer("");
    ParameterImpl tcodeParam =
      new ParameterImpl (LTATCODENAMES, String.class.getName(),
          false, false, "LTA Time Code Names");
    ParameterListImpl listImpl = (ParameterListImpl) list;
    try {
      logger.debug("Loading list of LTA timecodes");
      Vector tcodes =
        LeaveSourceBuilder.getLeaveOverrideTcodes(
            (DBConnection) conn.getValue());

      if ( !tcodes.isEmpty()) {
        Iterator tcodeIter = tcodes.iterator();
        ltaString.append( (String) tcodeIter.next() );

        while ( tcodeIter.hasNext()) {
          ltaString.append( "," + (String) tcodeIter.next() );
        }
      }
      //else {
        // Added this else clause because the VacationSourc constructor
        // does not handle an empty ltaTcodeNames value well.
        //logger.debug("No LTA codes defined in leave request types. " +
        //    "Using default VAC");
        //tcodeParam.setStrValue("VAC");
      //}

      logger.debug("List of LTA timecodes = " + ltaString.toString());
      tcodeParam.setStrValue(ltaString.toString());
      listImpl.add(tcodeParam);

    } catch (Exception e) {
      logger.error("Problem loading list of LTA timecodes");
      throw new InstantiationException(e.toString());
    }

    try {
      rs = new VacationSource ( (DBConnection) conn.getValue(), listImpl);
    } catch (AccessException e) {
      logger.error("Problem instantiating VacationSource");
      throw new InstantiationException (e.toString());
    }
    return rs;
  }
}