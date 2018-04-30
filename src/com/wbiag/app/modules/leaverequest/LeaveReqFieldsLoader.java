package com.wbiag.app.modules.leaverequest;


import com.workbrain.app.bo.*;
import com.workbrain.app.bo.ejb.actions.WorkflowUtil;
import com.workbrain.app.workflow.*;
import java.text.SimpleDateFormat;
import java.util.*;
import com.workbrain.util.*;

public class LeaveReqFieldsLoader {

  private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(LeaveReqFieldsLoader.class);

  public static final String START_DATE = "Start_Date";
  public static final String END_DATE = "End_Date";
  public static final String START_TIME = "StartTime";
  public static final String END_TIME = "EndTime";
  public static final String CANCEL_REQUEST = "CancelRequest";
  public static final String HI_CANCEL_OVR_LIST = "hiCancelOvrList";
  //public static final String PAY_TYPE = "payType";
  public static final String COMMENTS = "comments";
  public static final String LEAVE_ID = "leaveId";
  public static final String REASON = "reason";
  public static final String PAY_IN_ADVANCE = "payInAdvance";

  private BOFormInstance boFormInstance = null;
  private WBObject object = null;
  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
  private SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm");
  private SimpleDateFormat datetimeFormat = new SimpleDateFormat("yyyyMMdd HHmm");
  private java.util.Date startDate = null,endDate = null;
  private java.util.Date startTime = null,endTime = null;

  public LeaveReqFieldsLoader(WBObject object) throws WorkflowEngineException{
    this.object = object;
    this.boFormInstance = (BOFormInstance)object;
    setFormStartEndTimes();
  }

  private void setFormStartEndTimes() throws WorkflowEngineException{
    if (!StringHelper.isEmpty(
        (String) this.getField(this.START_DATE))) {
      this.startDate = WorkflowUtil.getFieldValueAsDate(this.object , this.START_DATE);
    }
    if (!StringHelper.isEmpty(
        (String) this.getField(this.END_DATE))) {
      this.endDate = WorkflowUtil.getFieldValueAsDate(this.object , this.END_DATE);
    }
    if (!StringHelper.isEmpty(
        (String) this.getField(this.START_TIME))) {
      this.startTime = WorkflowUtil.getFieldValueAsDatetime(this.object , this.START_TIME);
    }
    if (!StringHelper.isEmpty(
        (String) this.getField(this.END_TIME))) {
      this.endTime  = WorkflowUtil.getFieldValueAsDatetime(this.object , this.END_TIME);
    }
  }

  public boolean leaveCancelled() throws WorkflowEngineException{
    String ret = this.getField(this.CANCEL_REQUEST);
    return ((ret != null) && ret.equalsIgnoreCase("true"));
  }

  public boolean requestDetected() throws WorkflowEngineException{
    this.startDate = this.getFormStartDate();
    this.endDate = this.getFormEndDate();
    return ((this.startDate != null) && (this.endDate != null));
  }

  public String getOvrIdsList() throws WorkflowEngineException{
    return this.getField(this.HI_CANCEL_OVR_LIST);
  }


  public int getLeaveId() throws WorkflowEngineException{
    String ret = this.getField(this.LEAVE_ID);
    return Integer.parseInt(ret);
  }

  public String getReason() throws WorkflowEngineException{
    String ret = this.getField(this.REASON);
    return ret;
  }

  public boolean getPayInAdvance() throws WorkflowEngineException{
    String ret = this.getField(this.PAY_IN_ADVANCE);
    return ((ret != null) && ret.equalsIgnoreCase("true"));
  }

  public String getComments() throws WorkflowEngineException{
    return this.getField(this.COMMENTS);
  }

  public java.util.Date getFormStartDate() throws WorkflowEngineException{
    return this.startDate;
  }

  public java.util.Date getFormEndDate() throws WorkflowEngineException{
    return this.endDate;
  }

  public java.util.Date getFormStartTime() throws WorkflowEngineException{
    return this.startTime;
  }

  public java.util.Date getFormEndTime() throws WorkflowEngineException{
    return this.endTime;
  }

  private String getField(String field) throws WorkflowEngineException{
    String ret = (String)this.boFormInstance.getValue(field);
    return ret;
  }

  private void log(String msg){
  }

}
