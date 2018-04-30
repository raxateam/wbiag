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
<%@ page import="com.workbrain.app.wbinterface.hr.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%@ page import="com.workbrain.app.wbalert.*"%>
<%@ page import="com.wbiag.app.wbalert.source.*"%>

<wb:page login='true'>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
<wb:define id="ALERT_ID"><wb:get id="ALERT_ID" scope="parameter" default=""/></wb:define>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="EMPLOYEES"><wb:get id="EMPLOYEES" scope="parameter" default=""/></wb:define>
<wb:define id="CALCGROUPS"><wb:get id="CALCGROUPS" scope="parameter" default=""/></wb:define>
<wb:define id="PAYGROUPS"><wb:get id="PAYGROUPS" scope="parameter" default=""/></wb:define>
<wb:define id="TEAMS"><wb:get id="TEAMS" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%  
  String employeeSQL = "sourceType=SQL source=\"select emp_id, emp_name from employee\" multiChoice=true";
  String calgroupSQL = "sourceType=SQL source=\"select CALCGRP_ID, CALCGRP_NAME from calc_group\" multiChoice=true";
  String paygroupSQL = "sourceType=SQL source=\"select PAYGRP_ID, PAYGRP_NAME from pay_group\" multiChoice=true";
  String teamSQL = "sourceType=SQL source=\"select WBT_ID, WBT_NAME from workbrain_team\" multiChoice=true";

  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int alertID = Integer.valueOf((String) param.get( WBAlertTask.PARAM_ALERTID )).intValue();
      
  String employees = (String)param.get( UnauthorizedTimesheetAlertSource.PARAM_EMPLOYEES);
  String calcgroups = (String)param.get( UnauthorizedTimesheetAlertSource.PARAM_CALCGROUPS);
  String paygroups = (String)param.get( UnauthorizedTimesheetAlertSource.PARAM_PAYGROUPS);
  String teams = (String)param.get( UnauthorizedTimesheetAlertSource.PARAM_TEAMS);

if ("SUBMIT".equals(OPERATION.toString())) {
  HashMap newParam = new HashMap();
  newParam.put( WBAlertTask.PARAM_ALERTID, String.valueOf( alertID ) );
  newParam.put( UnauthorizedTimesheetAlertSource.PARAM_EMPLOYEES, EMPLOYEES == null ? "" : EMPLOYEES.toString() );
  newParam.put( UnauthorizedTimesheetAlertSource.PARAM_CALCGROUPS, CALCGROUPS == null ? "" : CALCGROUPS.toString() );
  newParam.put( UnauthorizedTimesheetAlertSource.PARAM_PAYGROUPS, PAYGROUPS == null ? "" : PAYGROUPS.toString() );
  newParam.put( UnauthorizedTimesheetAlertSource.PARAM_TEAMS, TEAMS == null ? "" : TEAMS.toString() );

  scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
  %>
  <Span>Type updated successfully</Span>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  <%
} else {
%>
<wb:set id="EMPLOYEES"><%=employees%></wb:set> 
<wb:set id="CALCGROUPS"><%=calcgroups%></wb:set> 
<wb:set id="PAYGROUPS"><%=paygroups%></wb:set> 
<wb:set id="TEAMS"><%=teams%></wb:set> 


<wba:table caption="Unauthorized Timesheet Alert Parameters" captionLocalizeIndex="Unauthorized Timesheet Alert Parameters" width="200">
  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="EMPLOYEES">Employees</wb:localize>
    </wba:th>
    <wba:td width='60%'>
      <wb:controlField cssClass="inputField" submitName="EMPLOYEES" ui="DBLookupUI" uiParameter='<%=employeeSQL%>'><%=employees%></wb:controlField> 
    </wba:td>
  </wba:tr>
  
  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="CALCGROUPS">Calgroups</wb:localize>
    </wba:th>
    <wba:td width='60%'>
      <wb:controlField cssClass="inputField" submitName="CALCGROUPS" ui="DBLookupUI" uiParameter='<%=calgroupSQL%>'><%=calcgroups%></wb:controlField> 
    </wba:td>
  </wba:tr>
  
  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="PAYGROUPS">Paygroups</wb:localize>
    </wba:th>
    <wba:td width='60%'>
      <wb:controlField cssClass="inputField" submitName="PAYGROUPS" ui="DBLookupUI" uiParameter='<%=paygroupSQL%>'><%=paygroups%></wb:controlField> 
    </wba:td>
  </wba:tr>
  
  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="TEAMS">Teams</wb:localize>
    </wba:th>
    <wba:td width='60%'>
      <wb:controlField cssClass="inputField" submitName="TEAMS" ui="DBLookupUI" uiParameter='<%=teamSQL%>'><%=teams%></wb:controlField> 
    </wba:td>
  </wba:tr>



</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
