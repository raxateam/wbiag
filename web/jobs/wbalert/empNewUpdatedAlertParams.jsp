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
<wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="CHECK_NEW_EMPS"><wb:get id="CHECK_NEW_EMPS" scope="parameter" default=""/></wb:define>
<wb:define id="LOOK_BACK_DAYS"><wb:get id="LOOK_BACK_DAYS" scope="parameter" default=""/></wb:define>
<wb:define id="CHECK_UPDATED_EMPS"><wb:get id="CHECK_UPDATED_EMPS" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int alertID = Integer.valueOf((String) param.get( WBAlertTask.PARAM_ALERTID )).intValue();
  Boolean checkNewEmps = "Y".equals((String)param.get( EmployeeNewUpdatedAlertSource.PARAM_CHECK_NEW_EMPS)) ? Boolean.TRUE : Boolean.FALSE;
  String lookBackDays = (String)param.get( EmployeeNewUpdatedAlertSource.PARAM_LOOK_BACK_DAYS);
  Boolean checkUpdatedEmps = "Y".equals((String)param.get( EmployeeNewUpdatedAlertSource.PARAM_CHECK_UPDATED_EMPS )) ? Boolean.TRUE : Boolean.FALSE;
  String empCols = (String)param.get( EmployeeNewUpdatedAlertSource.PARAM_EMPLOYEE_COLUMNS);

if ("SUBMIT".equals(OPERATION.toString())) {
  HashMap newParam = new HashMap();
  newParam.put( WBAlertTask.PARAM_ALERTID, String.valueOf( alertID ) );
  newParam.put( EmployeeNewUpdatedAlertSource.PARAM_CHECK_NEW_EMPS, CHECK_NEW_EMPS == null ? "" : CHECK_NEW_EMPS.toString() );
  newParam.put( EmployeeNewUpdatedAlertSource.PARAM_LOOK_BACK_DAYS, LOOK_BACK_DAYS == null ? "" : LOOK_BACK_DAYS.toString() );
  newParam.put( EmployeeNewUpdatedAlertSource.PARAM_CHECK_UPDATED_EMPS, CHECK_UPDATED_EMPS == null ? "" : CHECK_UPDATED_EMPS.toString() );
  String[] empColsArr = request.getParameterValues("EMPLOYEE_COLUMNS");
  StringBuffer sb = new StringBuffer(200);
  if (empColsArr != null && empColsArr.length > 0) {
      for (int i = 0; i < empColsArr.length; i++) {
          sb.append(i > 0 ? "," : "").append(empColsArr[i]);
      }
  }
  newParam.put( EmployeeNewUpdatedAlertSource.PARAM_EMPLOYEE_COLUMNS, sb.toString());

  scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
  %>
  <Span>Type updated successfully</Span>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  <%
} else {
%>
<wba:table caption="New Updated Employees Alert Parameters" captionLocalizeIndex="New Updated Employees Alert Parameters" width="200">
  <wba:tr>
    <wba:th width='20%'>
      <wb:localize id="CHECK_NEW_EMPS">Check New Employees</wb:localize>
    </wba:th>
    <wba:td width='80%'>
      <wb:controlField cssClass="inputField" submitName="CHECK_NEW_EMPS" ui="CheckboxUI"  uiParameter=""><%=checkNewEmps%></wb:controlField>

    </wba:td>
  </wba:tr>

  <wba:tr>
    <wba:th width='20%'>
      <wb:localize id="CHECK_UPDATED_EMPS">Check Updated Employees</wb:localize>
    </wba:th>
    <wba:td width='80%'>
      <wb:controlField cssClass="inputField" submitName="CHECK_UPDATED_EMPS" ui="CheckboxUI"  uiParameter=""><%=checkUpdatedEmps%></wb:controlField>
    </wba:td>
  </wba:tr>

  <wba:tr>
    <wba:th width='20%'>
      <wb:localize id="LOOK_BACK_DAYS">Look Back Days</wb:localize>
    </wba:th>
    <wba:td width='80%'>
      <wb:controlField cssClass="inputField" submitName="LOOK_BACK_DAYS"
                       nullable="false"
                       ui="NumberUI"
                       uiParameter='precision=0'><%=(lookBackDays==null?"":lookBackDays)%></wb:controlField>

    </wba:td>
  </wba:tr>

  <wba:tr>
    <wba:th width='20%'>
      <wb:localize id="EMPLOYEE_COLUMNS">Employee Columns</wb:localize>
    </wba:th>
    <wba:td width='80%'>
      <%
      // *** TT46623 ListBoxUI does not work for multi choice, doing it by code
      List empColsAll = EmployeeNewUpdatedAlertSource.getAllEmployeeColumns();
      List empColsSelected = new ArrayList();
      String[] empColsArr = StringHelper.detokenizeString(empCols , ",");
      if (empColsArr != null && empColsArr.length > 0) {
          empColsSelected = Arrays.asList(empColsArr);
      }
      StringBuffer empColsHtml = new StringBuffer(200);
      empColsHtml.append("<select class='inputField' name='EMPLOYEE_COLUMNS' size=10 MULTIPLE>");
      for (int i=0 , k=empColsAll.size() ; i<k ; i++) {
          String item = (String)empColsAll.get(i);
          empColsHtml.append("<option value='").append(item).append("'");
          if (empColsSelected.size() > 0 && empColsSelected.contains(item)) {
              empColsHtml.append(" selected");
          }
          empColsHtml.append(">").append(item).append("</option>");
      }
      empColsHtml.append("</select>");
      out.println(empColsHtml.toString());
      %>
    </wba:td>
  </wba:tr>

</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
