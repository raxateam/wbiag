<%@ include file="/system/wbheader.jsp"%> <%@ page import="java.io.*"%> <%@ page import="java.util.*"%> <%@ page import="javax.naming.*"%> <%@ page import="com.workbrain.sql.*"%> <%@ page import="com.workbrain.util.*"%> <%@ page import="com.workbrain.server.jsp.*"%> <%@ page import="com.workbrain.server.registry.*"%> <%@ page import="com.workbrain.app.scheduler.*"%> <%@ page import="com.workbrain.app.scheduler.enterprise.*"%> <%@ page import="com.workbrain.app.wbinterface.*"%> <%@ page import="com.workbrain.app.wbinterface.hr.*"%> <%@ page import="java.lang.reflect.*"%> <%@ page import="java.sql.*"%> <%@ page import="javax.servlet.jsp.JspException" %> <%@ page import="com.workbrain.app.wbalert.source.*"%> <%@ page import="com.workbrain.app.wbalert.*"%> <%@ page import="com.wbiag.app.wbalert.source.*"%> <wb:page login='true'> <wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define> <wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit> <wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define> <wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit> <wb:define id="ALERT_ID"><wb:get id="ALERT_ID" scope="parameter" default=""/></wb:define> <wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define> <wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define> <wb:define id="SHIFT_PATTERN_LIST"><wb:get id="SHIFT_PATTERN_LIST" scope="parameter" default=""/></wb:define> <wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define> <%
  String shiftPatternSQL = "sourceType=SQL source=\"SELECT SHFTPAT_ID, SHFTPAT_NAME Name FROM SHIFT_PATTERN\" ";

  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int alertID = Integer.valueOf((String) param.get( "alertId" )).intValue();
  String shiftPatternList = (String)param.get(ClockAlertSource.PARAM_SHIFT_PATTERNS);

if ("SUBMIT".equals(OPERATION.toString())) {
  HashMap newParam = new HashMap();
  newParam.put( WBAlertTask.PARAM_ALERTID, String.valueOf( alertID ) );
  newParam.put( ClockAlertSource.PARAM_SHIFT_PATTERNS, SHIFT_PATTERN_LIST == null ? "" : SHIFT_PATTERN_LIST.toString() );
  scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
  %> <Span>Parameters updated successfully</Span> <BR> <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/> <%
} else {
%> <wba:table caption="Clocking Alert Task Parameters" captionLocalizeIndex="Clocking Alert Task Parameters" width="200"> <wba:tr> <wba:th width='20%'> <wb:localize id="SHIFT_PATTERN_LIST">Shift Patterns</wb:localize> </wba:th> <wba:td width='80%'> <wb:controlField cssClass="inputField" submitName="SHIFT_PATTERN_LIST" ui="DBLookupUI" uiParameter='<%=shiftPatternSQL + " multiChoice=true"%>'><%=shiftPatternList%></wb:controlField> </wba:td> </wba:tr> </wba:table><BR> <wb:submit id="OPERATION">SUBMIT</wb:submit> <wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp; <wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/> <%}%> </wb:page> 
