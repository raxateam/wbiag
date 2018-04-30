<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.wbinterface.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>

<wb:page login='true' showUIPath='true' uiPathName='Strategic Labor Schedule Import Task Parameters' uiPathNameId='UIPATHNAME_ID_SCHEDULE_PARAMS'>
<wb:config id='UIPATHNAME_ID_SCHEDULE_PARAMS'/>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="TYPE_ID"><wb:get id="TYPE_ID" scope="parameter" default=""/></wb:define>
<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>

<wb:define id="DAYS_TO_ADJUST_SCHED"><wb:get id="DAYS_TO_ADJUST_SCHED" scope="parameter" default="7"/></wb:define>

<%
  int taskId = Integer.parseInt( TASK_ID.toString()); 
  
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(taskId);
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param;
  if ( StringHelper.isEmpty(TASK_ID.toString()) )
    param = new HashMap();
  else
    param = scheduler.getTaskParams(taskId);
    
  int typeID = Integer.parseInt( String.valueOf(param.get("transactionType")) );
  String MaxDaysForScheduleAdjust = (String)param.get( "MaxDaysForScheduleAdjust" );


if ("SUBMIT".equals(OPERATION.toString())) {
  param.put( "transactionType", String.valueOf( typeID ) );
  param.put( "CreatesEmployeeOverride", "Y" );  
  param.put( "MaxDaysForScheduleAdjust", StringHelper.isEmpty(DAYS_TO_ADJUST_SCHED.toString()) ? "7" : DAYS_TO_ADJUST_SCHED.toString() );

  scheduler.setTaskParams(taskId, param);
  %>
  <BR>
  <Span><wb:localize id="Type_Updated_Successfully">Type updated successfully</wb:localize></Span>
  <BR>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  <%
} else {
%>
<wba:table caption="Schedule Import Parameters" captionLocalizeIndex="Schedule Import Parameters" width="800" >

  <tr> <td colspan=10> <wb:localize id="SL_INT_MAX_DAYS_ADJUST_FULL_TEXT">The task will automatically apply new schedule overrides to update employee's schedules. However for schedules that more than a few pay periods in the future, there may not be a need to update the schedule at the task runtime. Generally beyond the period processed by Start Of Day task, schedules are yet to be created, therefore the schedule override can stay pending until new employee's schedule is generated (e.g Start Of Day task, Timesheet access, etc...) .<br><br>Past schedules and current day schedules will automatically be updated<br><br>Please enter below the number of days after current date where employee's schedule WILL be refreshed by the task.</wb:localize> </td> </tr> 
  <tr><td>
  
  <wb:localize id="SL_INT_DAYS_TO_ADJUST_SCHED">Refresh schedules up to</wb:localize>&nbsp;
  <wb:controlField submitName="DAYS_TO_ADJUST_SCHED" cssClass="inputField"
			ui="NumberUI" uiParameter="width='2' scale='2' precision='0'" ><%=MaxDaysForScheduleAdjust%></wb:controlField> 
  &nbsp<wb:localize id="SL_INT_DAYS_TO_ADJUST_SCHED_CONT">days after current date</wb:localize> 
  </td></tr> 

</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>

</wb:page>
