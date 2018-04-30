package com.wbiag.app.ta.model;

import com.workbrain.app.ta.model.RecordData;

public class ReasonData extends RecordData {

  public static String TABLE_NAME = "WBIAG_REASON";

  public static String WGRSN_ID = "WGRSN_ID";
  private int wgrsnId;
  public static String WGRSN_NAME = "WGRSN_NAME";
  private String wgrsnName;
  public static String WGRSN_DESC = "WGRSN_DESC";
  private String wgrsnDesc;
  public static String WGLVE_ID = "WGLVE_ID";
  private int wglveId;


  public int getWglveId() {
    return wglveId;
  }


  public void setWglveId(int wglveId) {
    this.wglveId = wglveId;
  }


  public String getWgrsnDesc() {
    return wgrsnDesc;
  }


  public void setWgrsnDesc(String wgrsnDesc) {
    this.wgrsnDesc = wgrsnDesc;
  }


  public int getWgrsnId() {
    return wgrsnId;
  }


  public void setWgrsnId(int wgrsnId) {
    this.wgrsnId = wgrsnId;
  }


  public String getWgrsnName() {
    return wgrsnName;
  }


  public void setWgrsnName(String wgrsnName) {
    this.wgrsnName = wgrsnName;
  }


  public RecordData newInstance() {
    return new ReasonData();
  }

}
