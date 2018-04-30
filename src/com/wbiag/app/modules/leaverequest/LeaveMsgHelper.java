package com.wbiag.app.modules.leaverequest;


import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.util.*;
import java.sql.*;

public class LeaveMsgHelper {

  public static final String CANCEL_LVE_FAILED_DELETE_OVERRIDES = "CANCEL_LVE_FAILED_DELETE_OVERRIDES";
  public static final String CANCEL_LVE_FAILED_CALCULATE_SCHEDULE = "CANCEL_LVE_FAILED_CALCULATE_SCHEDULE";
  public static final String CANCEL_LVE_NO_LEAVE_OVRS = "CANCEL_LVE_NO_LEAVE_OVRS";
  public static final String PROCESS_LVE_NO_START_DATE = "PROCESS_LVE_NO_START_DATE";
  public static final String PROCESS_LVE_NO_END_DATE = "PROCESS_LVE_NO_END_DATE";
  public static final String COMMON_FAILED_CHECK_SCHEDULE = "COMMON_FAILED_CHECK_SCHEDULE";
  public static final String BALANCE_NOT_SCHEDULED = "BALANCE_NOT_SCHEDULED";
  public static final String PROCESS_NO_TIME_CODE_FOUND = "PROCESS_NO_TIME_CODE_FOUND";
  public static final String PROCESS_START_TIME_OUT_SCHEDULE = "PROCESS_START_TIME_OUT_SCHEDULE";
  public static final String PROCESS_END_TIME_OUT_SCHEDULE = "PROCESS_END_TIME_OUT_SCHEDULE";
  public static final String PROCESS_SCHEDULE_NOT_FOUND = "PROCESS_SCHEDULE_NOT_FOUND";
  public static final String BALANCE_START_DATE_GREATER_THAN_END_DATE = "BALANCE_START_DATE_GREATER_THAN_END_DATE";
  public static final String BALANCE_START_DATE_GREATER_THAN_TODAY = "BALANCE_START_DATE_GREATER_THAN_TODAY";
  public static final String BALANCE_FAILED_TO_PARSE_START_DATE = "BALANCE_FAILED_TO_PARSE_START_DATE";
  public static final String BALANCE_START_TIME_OUTSIDE_INTERVAL = "BALANCE_START_TIME_OUTSIDE_INTERVAL";
  public static final String BALANCE_END_TIME_OUTSIDE_INTERVAL = "BALANCE_END_TIME_OUTSIDE_INTERVAL";
  public static final String LEAVE_ALREADY_EXISTS = "LEAVE_ALREADY_EXISTS";
  public static final String PROCESS_FAILED_TIME_CODE = "PROCESS_FAILED_TIME_CODE";
  public static final String PROCESS_CALC_SCHEDULE_DATE_ERROR = "PROCESS_CALC_SCHEDULE_DATE_ERROR";
  public static final String BALANCE_SPAN_TWO_YEARS = "BALANCE_SPAN_TWO_YEARS";
  public static final String COMMON_PAID = "COMMON_PAID";
  public static final String COMMON_UNPAID = "COMMON_UNPAID";
  public static final String BALANCE_UNPAID = "BALANCE_UNPAID";
  public static final String BALANCE_ERROR_CHECK_BALANCE = "BALANCE_ERROR_CHECK_BALANCE";
  public static final String PROCESS_UNPAID_AFFECTS_BALANCES = "PROCESS_UNPAID_AFFECTS_BALANCES";
  public static final String PROCESS_UNPAID_DOESNOT_AFFECT_BALANCES = "PROCESS_UNPAID_DOESNOT_AFFECT_BALANCES";
  public static final String PROCESS_PAID_AFFECTS_BALANCES = "PROCESS_PAID_AFFECTS_BALANCES";

  public static final String ERROR_PROCESSING_OVERRIDES = "ERROR_PROCESSING_OVERRIDES";
  public static final String OVERRIDE_ID_NULL = "OVERRIDE_ID_NULL";
  public static final String START_STOP_TIMES_NOT_ALLOWED = "START_STOP_TIMES_NOT_ALLOWED";
  public static final String NO_ENT_POLICY = "NO_ENT_POLICY";
  public static final String MAX_DAYS_VIOLATED = "MAX_DAYS_VIOLATED";
  public static final String BAD_TCODE_FOUND = "BAD_TCODE_FOUND";
  public static final String ONE_OPP_ALLOWED = "ONE_OPP_ALLOWED";

  private static Map defCodes = null;
  static
  {
    defCodes = new HashMap ();
    defCodes.put(CANCEL_LVE_FAILED_DELETE_OVERRIDES , "Failed to delete leave overrides for ");
    defCodes.put(CANCEL_LVE_FAILED_CALCULATE_SCHEDULE , "Failed to calculate the schedule for ");
    defCodes.put(CANCEL_LVE_NO_LEAVE_OVRS , "No leave overrides found.");
    defCodes.put(PROCESS_LVE_NO_START_DATE , "Start date null.  Process cancelled.");
    defCodes.put(PROCESS_LVE_NO_END_DATE ,  "End date null.  Process cancelled.");
    defCodes.put(COMMON_FAILED_CHECK_SCHEDULE , "Failed to verify the employee's schedule for given dates.");
    defCodes.put(BALANCE_NOT_SCHEDULED , "You are not scheduled to work within the period : ");
    defCodes.put(PROCESS_NO_TIME_CODE_FOUND ,
    "No Time Code found in the database for the tcode_id provided : ");
    defCodes.put(PROCESS_START_TIME_OUT_SCHEDULE ,
    "The start time entered is outside the scheduled interval for date : ");
    defCodes.put(PROCESS_END_TIME_OUT_SCHEDULE , "The end time entered is outside the scheduled for date : ");
    defCodes.put(PROCESS_SCHEDULE_NOT_FOUND , "Schedule not found.");
    defCodes.put(BALANCE_START_DATE_GREATER_THAN_END_DATE ,
      "The End Date provided must be greater than the Start Date.");
    defCodes.put(BALANCE_START_DATE_GREATER_THAN_TODAY ,
    "The Start Date provided has already elapsed.  The Start Date must be greater than the current date.");
    defCodes.put(BALANCE_FAILED_TO_PARSE_START_DATE , "Failed to parse start date.");
    defCodes.put(BALANCE_START_TIME_OUTSIDE_INTERVAL ,
    "Start time provided is outside the scheduled interval for date : ");
    defCodes.put(BALANCE_END_TIME_OUTSIDE_INTERVAL ,
    "End time provided is outside the scheduled interval for date : ");
    defCodes.put(LEAVE_ALREADY_EXISTS , "A leave request already exists in the requested date range. ");
    defCodes.put(PROCESS_FAILED_TIME_CODE , "Failed to load the time codes indices.");
    defCodes.put(PROCESS_CALC_SCHEDULE_DATE_ERROR , "Failed to calculate the number of minutes");
    defCodes.put(BALANCE_SPAN_TWO_YEARS ,
        "You cannot request leave that spans more than one year.<br>" +
        "Please limit this request to the current year and do a separate request for the following year."
    );
    defCodes.put(COMMON_PAID , "Paid");
    defCodes.put(COMMON_UNPAID , "Unpaid");
    defCodes.put(BALANCE_ERROR_CHECK_BALANCE , "Error when checking leave balance result : ");
    defCodes.put(PROCESS_UNPAID_AFFECTS_BALANCES, "Unpaid time code affects balances which will " +
    "affect its unpaid behavior. Please contact system administrator");
    defCodes.put(PROCESS_UNPAID_DOESNOT_AFFECT_BALANCES, "Unpaid time code does not affect balances which will " +
    "affect its unpaid behavior. Please contact system administrator");
    defCodes.put(PROCESS_PAID_AFFECTS_BALANCES, "Paid time code affects balances which will " +
    "affect its unpaid behavior. Please contact system administrator");

    defCodes.put(ERROR_PROCESSING_OVERRIDES, "Failed to process the leave request due to a problem processing overrides.");
    defCodes.put(OVERRIDE_ID_NULL, "The override type ID for the current period leave override cannot be null. Please make sure it is set to valid value in the workflow processing node.");
    defCodes.put(START_STOP_TIMES_NOT_ALLOWED, "This leave type is full-day only. Start and stop times are not allowed.");
    defCodes.put(NO_ENT_POLICY, "Employee is not part of an entitlment policy "+
        "that is associated with this type of leave and in effect for the " +
        "duration of the requested period.");
    defCodes.put(MAX_DAYS_VIOLATED, "The request exceeds this leave type's the maximum number of consecutive days, which is ");
    defCodes.put(BAD_TCODE_FOUND, "This request cannot be processed because one of the following timecodes exists in the requested date range: ");
    defCodes.put(ONE_OPP_ALLOWED, "Leave requests cannot be submitted and cancelled at the same time. Please submit a separate form for each request.");

  }

  private DBConnection con = null;
  private int localeId = 1;

  public LeaveMsgHelper(DBConnection con,int wbllId) {
    this.con = con;
    this.localeId = wbllId;
  }

  public static String st_localize(String code, DBConnection dbCon,
      int locale ) throws SQLException{
    // TODO: check that the code is in the defCodes hash and if not then
    // return some generic message
    PreparedStatement ps = null;
    ResultSet rs = null;
    String ret = null;
    try {
      String sql = "SELECT wbmldt_text FROM workbrain_msg_locale_data " +
      " WHERE wbmldt_name= ? " +
      " AND wbll_id= ?";
      ps = dbCon.prepareStatement(sql);
      ps.setString(1 , code);
      ps.setInt(2 , locale);
      rs = ps.executeQuery();
      while(rs.next()){
        ret = rs.getString("wbmldt_text");
      }
    } finally {
      if (rs != null) {
        rs.close();
      }
      if (ps != null) {
        ps.close();
      }
    }
    return StringHelper.isEmpty(ret)
    ? getDefaultLocalization(code)
        : ret;
  }

  public String localize(String wbmldtCode) throws SQLException {
    return st_localize(wbmldtCode, this.con, this.localeId);
  }

  private static String getDefaultLocalization(String wbmldtCode){
    String def = (String)defCodes.get(wbmldtCode);
    return StringHelper.isEmpty(def)
    ? wbmldtCode
        : def;
  }
}