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
<%@ page import="com.wbiag.app.wbinterface.schedulein.*" %>

<wb:page login='true' showUIPath='true'>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="TYPE_ID"><wb:get id="TYPE_ID" scope="parameter" default=""/></wb:define>
<wb:define id="CREATES_EMPLOYEE_OVERRIDE"><wb:get id="CREATES_EMPLOYEE_OVERRIDE" scope="parameter" default=""/></wb:define>
<wb:define id="FIXED_PARTIAL"><wb:get id="FIXED_PARTIAL" scope="parameter" default=""/></wb:define>
<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%

  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int typeID = Integer.parseInt( String.valueOf( param.get( "transactionType" ) ) );
  String createsEmployeeOverride = (String)param.get( "CreatesEmployeeOverride" );
  String fixedPartial = (String)param.get( ShiftPatternTransactionSO.PARAM_FIXED_SHIFT );

if ("SUBMIT".equals(OPERATION.toString())) {
  HashMap newParam = new HashMap();
  newParam.put( "transactionType", String.valueOf( typeID ) );
  newParam.put( "CreatesEmployeeOverride", CREATES_EMPLOYEE_OVERRIDE == null ? "" : CREATES_EMPLOYEE_OVERRIDE.toString() );
  newParam.put( ShiftPatternTransactionSO.PARAM_FIXED_SHIFT , FIXED_PARTIAL.toString() );
  scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
  %>
  <Span>Type updated successfully</Span>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  <%
} else {
%>
<wba:table caption="Shift Pattern Import Parameters" captionLocalizeIndex="Shift Pattern Import Parameters" width="300" >

  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="SPCreatesEmployeeOverride">Creates Employee Override</wb:localize>
    </wba:th>
    <wba:td width='60%'>
          <wb:controlField cssClass="inputField" submitName="CREATES_EMPLOYEE_OVERRIDE" ui="CheckboxUI" uiParameter=""><%=createsEmployeeOverride%></wb:controlField>
    </wba:td>
  </wba:tr>


  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="SPFixedPartial">Fixed Partial</wb:localize>
    </wba:th>
    <wba:td width='60%'>
      <wb:controlField cssClass="inputField" submitName="FIXED_PARTIAL"
                       nullable="false"
                       ui="ComboBoxUI"
                       uiParameter="valueList='1,2' labelList='Only,Partial'"><%=fixedPartial%></wb:controlField>

    </wba:td>
  </wba:tr>

</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
