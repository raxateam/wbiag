package com.wbiag.app.modules.leaverequest.action;

import com.workbrain.app.bo.*;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import com.workbrain.server.sql.*;
import java.sql.*;
import javax.naming.*;
import javax.sql.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.wbiag.app.modules.leaverequest.LeaveMsgHelper;
import com.workbrain.app.bo.ejb.actions.WorkflowUtil;
import com.workbrain.util.*;

public abstract class AbstractLeaveAction extends AbstractActionProcess {

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(AbstractLeaveAction.class);

  public static final String SUCCESS = "Success";
  public static final String FAILURE = "Failure";
  public static final String SUCCESS_NOT_FOUND = "Branch Success not found";
  public static final String FAILURE_NOT_FOUND = "Branch Failure not found";
  public static final String FAILED_BO = "Failed to set BOFormInstance object";
  public static final String FAILED_CONNECTION = "Failed to get a database connection";

  private DBConnection connection = null;
  private BOFormInstance boFormInstance = null;
  private WBObject object = null;
  private int userId = -1,empId = -1;
  private LeaveMsgHelper locManager = null;
  private WorkbrainUserData creatorUserData = null;
  private EmployeeData creatorEmpData = null;
  private String empFullName = null;

  protected void setWBObject(WBObject objectIn)  throws Exception {
    this.object = objectIn;
    this.boFormInstance = (BOFormInstance) object;
    WorkbrainUserAccess wua = new WorkbrainUserAccess(this.getConnection());
    this.creatorUserData =
      WorkflowUtil.getCreatorUserData(this.object , this.getConnection());
    this.userId = creatorUserData.getWbuId();
    this.empId = creatorUserData.getEmpId();
    this.creatorEmpData = WorkflowUtil.getCreatorEmployeeByDate(this.object ,
        this.getConnection() , DateHelper.getCurrentDate());
  }

  protected String localize(String code) {
    String ret = "";

    if (this.locManager == null){
      if (this.connection != null){
        this.locManager =
          new LeaveMsgHelper(this.connection,
              creatorUserData.getWbllId());
      }
    }

    if (locManager == null) {
      throw new RuntimeException ("Could not initialize localization");
    }

    try {
      ret = locManager.localize(code);
    } catch (SQLException e) {
      logger.warn("Could not retrieve localized message for code "+code);
    }

    return ret;
  }

  protected int getEmployeeId(){
    return this.empId;
  }

  protected int getUserId(){
    return this.userId;
  }

  protected WorkbrainUserData getUserData(){
    return this.creatorUserData;
  }

  protected String getEmployeeFullName(){
    return this.creatorEmpData.getEmpLastname() + ", " +
      creatorEmpData.getEmpFirstname();
  }


  protected void log(String msg){
  }

}