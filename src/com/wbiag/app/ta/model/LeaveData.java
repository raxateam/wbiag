package com.wbiag.app.ta.model;

import com.wbiag.app.modules.leaverequest.source.LeaveSource;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.app.ta.model.BalanceData;
import com.workbrain.app.ta.model.TimeCodeData;
import com.workbrain.app.ta.model.RecordData;
import com.workbrain.sql.DBConnection;
import java.sql.SQLException;

public class LeaveData extends RecordData {

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveData.class);

  public static String TABLE_NAME = "WBIAG_LEAVE";

  public static String WGLVE_ID = "WGWGLVE_ID";
  private int wglveId;
  public static String WGLVE_NAME = "WGWGLVE_NAME";
  private String wglveName;
  public static String WGLVE_DESC = "WGWGLVE_DESC";
  private String wglveDesc;
  public static String WGLVE_FULL_DAY = "WGWGLVE_FULL_DAY";
  private String wglveFullDay;
  public static String WGLVE_PAY_CURRENT = "WGWGLVE_PAY_CURRENT";
  private String wglvePayCurrent;
  public static String WGLVE_RESTRICT_POL = "WGWGLVE_RESTRICT_POL";
  private String wglveRestrictPol;
  //public static String WGLVE_RESTRICT_CG = "WGWGLVE_RESTRICT_CG";
  //private String wglveRestrictCg;
  public static String WGLVE_CUR_TCODE_ID = "WGWGLVE_CUR_TCODE_ID";
  private int wglveCurTcodeId = -999;
  public static String WGLVE_FUT_TCODE_ID = "WGWGLVE_FUT_TCODE_ID";
  private int wglveFutTcodeId = -999;
  public static String TCODE_ID = "TCODE_ID";
  private int tcodeId;
  public static String WGLVE_REASON_REQD = "WGWGLVE_REASON_REQD";
  private String wglveReasonReqd;
  public static String WGLVE_MAX_DAYS = "WGWGLVE_MAX_DAYS";
  private int wglveMaxDays;
  public static String WGLVE_COMMENT_REQD = "WGWGLVE_COMMENT_REQD";
  private String wglveCommentReqd;
  public static String WGLVE_BAD_TCODES = "WGWGLVE_BAD_TCODES";
  private String wglveBadTcodes;
  public static String WGLVE_MIN_DAYS_EMP = "WGWGLVE_MIN_DAYS_EMP";
  private int wglveMinDaysEmp;
  public static String WGLVE_EMP_TYPE = "WGWGLVE_EMP_TYPE";
  private String wglveEmpType;


  public RecordData newInstance() {
    return new LeaveData();
  }

  public String getTcodeName(DBConnection conn, CodeMapper mapper)
      throws SQLException {
    logger.debug("Retrieving timecode name");
    TimeCodeData tcd = mapper.getTimeCodeById(tcodeId);
    return tcd.getTcodeName();
  }

  public String getCurrentTcodeName(DBConnection conn, CodeMapper mapper)
      throws SQLException {
    logger.debug("Retrieving current timecode name");
    TimeCodeData tcd = mapper.getTimeCodeById(wglveCurTcodeId);
    return tcd.getTcodeName();
  }

  public String getFutureTcodeName(DBConnection conn, CodeMapper mapper)
      throws SQLException {
    logger.debug("Retrieving future timecode name");
    TimeCodeData tcd = mapper.getTimeCodeById(wglveFutTcodeId);
    return tcd.getTcodeName();
  }

//  public String getBalName(DBConnection conn) throws SQLException {
//    logger.debug("Retrieving balance name");
//    CodeMapper mapper = CodeMapper.createCodeMapper(conn);
//    BalanceData tcd = mapper.getBalanceById(balId);
//    return tcd.getBalName();
//  }


  public int getWglveCurTcodeId() {
    return wglveCurTcodeId;
  }



  public void setWglveCurTcodeId(int wglveCurTcodeId) {
    this.wglveCurTcodeId = wglveCurTcodeId;
  }



  public String getWglveDesc() {
    return wglveDesc;
  }



  public void setWglveDesc(String wglveDesc) {
    this.wglveDesc = wglveDesc;
  }



  public String getWglveFullDay() {
    return wglveFullDay;
  }



  public void setWglveFullDay(String wglveFullDay) {
    this.wglveFullDay = wglveFullDay;
  }



  public int getWglveFutTcodeId() {
    return wglveFutTcodeId;
  }



  public void setWglveFutTcodeId(int wglveFutTcodeId) {
    this.wglveFutTcodeId = wglveFutTcodeId;
  }



  public int getWglveId() {
    return wglveId;
  }



  public void setWglveId(int wglveId) {
    this.wglveId = wglveId;
  }



  public String getWglveName() {
    return wglveName;
  }



  public void setWglveName(String wglveName) {
    this.wglveName = wglveName;
  }



  public String getWglvePayCurrent() {
    return wglvePayCurrent;
  }



  public void setWglvePayCurrent(String wglvePayCurrent) {
    this.wglvePayCurrent = wglvePayCurrent;
  }



  //public String getWglveRestrictCg() {
  //  return wglveRestrictCg;
  //}



  //public void setWglveRestrictCg(String wglveRestrictCg) {
  //  this.wglveRestrictCg = wglveRestrictCg;
  //}



  public String getWglveRestrictPol() {
    return wglveRestrictPol;
  }



  public void setWglveRestrictPol(String wglveRestrictPol) {
    this.wglveRestrictPol = wglveRestrictPol;
  }



  public int getTcodeId() {
    return tcodeId;
  }



  public void setTcodeId(int tcodeId) {
    this.tcodeId = tcodeId;
  }

  public int getWglveMaxDays() {
    return wglveMaxDays;
  }

  public void setWglveMaxDays(int wglveMaxDays) {
    this.wglveMaxDays = wglveMaxDays;
  }

  public String getWglveReasonReqd() {
    return wglveReasonReqd;
  }

  public void setWglveReasonReqd(String wglveReasonReqd) {
    this.wglveReasonReqd = wglveReasonReqd;
  }

  public String getWglveCommentReqd() {
    return wglveCommentReqd;
  }

  public void setWglveCommentReqd(String wglveCommentReqd) {
    this.wglveCommentReqd = wglveCommentReqd;
  }

  public String getWglveBadTcodes() {
    return wglveBadTcodes;
  }

  public void setWglveBadTcodes(String wglveBadTcodes) {
    this.wglveBadTcodes = wglveBadTcodes;
  }

  public int getWglveMinDaysEmp() {
    return wglveMinDaysEmp;
  }

  public void setWglveMinDaysEmp(int wglveMinDaysEmp) {
    this.wglveMinDaysEmp = wglveMinDaysEmp;
  }

  public String getWglveEmpType() {
    return wglveEmpType;
  }

  public void setWglveEmpType(String wglveEmpType) {
    this.wglveEmpType = wglveEmpType;
  }



}
