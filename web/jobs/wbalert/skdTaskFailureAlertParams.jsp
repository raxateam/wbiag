<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.wbalert.*"%>
<%@ page import="com.wbiag.app.wbalert.source.*"%>
<wb:page login='true'>
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
<wb:define id="ALERT_ID"><wb:get id="ALERT_ID" scope="parameter" default=""/></wb:define>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="SKD_TASK_IDS"><wb:get id="SKD_TASK_IDS" scope="parameter" default=""/></wb:define>
<wb:define id="LOOK_BACK_MINUTES"><wb:get id="LOOK_BACK_MINUTES" scope="parameter" default=""/></wb:define>
<wb:define id="LOG_STATUS"><wb:get id="LOG_STATUS" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%
  String singleQuote = "'";
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int alertID = Integer.valueOf((String) param.get( WBAlertTask.PARAM_ALERTID )).intValue();
  String lookBackMinutes = (String)param.get( ScheduledTaskFailureAlertSource.PARAM_LOOK_BACK_MINUTES );
  String skdTaskIds = (String)param.get( ScheduledTaskFailureAlertSource.PARAM_SKD_TASK_IDS );
  String logStatus = (String)param.get( ScheduledTaskFailureAlertSource.PARAM_LOG_STATUS );
  if (StringHelper.isEmpty(logStatus)) {
    logStatus = ScheduledTaskFailureAlertSource.PARAM_DEFLT_LOG_STATUS;
  }
if ("SUBMIT".equals(OPERATION.toString())) {
	HashMap newParam = new HashMap();
	newParam.put( WBAlertTask.PARAM_ALERTID, String.valueOf( alertID ) );
	newParam.put( ScheduledTaskFailureAlertSource.PARAM_SKD_TASK_IDS, SKD_TASK_IDS == null ? "" : SKD_TASK_IDS.toString() );
    newParam.put( ScheduledTaskFailureAlertSource.PARAM_LOOK_BACK_MINUTES, LOOK_BACK_MINUTES == null ? "" : LOOK_BACK_MINUTES.toString() );
    newParam.put( ScheduledTaskFailureAlertSource.PARAM_LOG_STATUS, LOG_STATUS == null ? "" : LOG_STATUS.toString() );
	scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
%>
<Span>Type updated successfully</Span><BR>
<wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
<% } else { %>
<wba:table caption="Scheduled Task Failures Alert Parameters" captionLocalizeIndex="Scheduled_Task_Failures_Alert_Parameters">
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="SKD_TASK_IDS">Scheduled Task Names:</wb:localize>
	</wba:th>
	<wba:td width='80%'>
	  <%  String strQuery = "width=50 multiChoice='true' pageSize='12' title='Balances' sourceType='SQL' source=\"SELECT JSTSK_ID, JSTSK_DESC FROM JOBSKD_TASK WHERE JSTSK_DELETED <>\'Y\' ORDER BY jstsk_desc\" sourceKeyField=JSTSK_ID"; %>
		<wb:controlField cssClass="inputField" submitName="SKD_TASK_IDS" ui="DBLookupUI" uiParameter='<%=strQuery%>' ><%=skdTaskIds%></wb:controlField>
	</wba:td>
	</wba:tr>

  <wba:tr>
    <wba:th width='20%'>
      <wb:localize id="LOOK_BACK_MINUTES">Look Back Minutes:</wb:localize>
    </wba:th>
    <wba:td width='80%'>
      <wb:controlField cssClass="inputField" submitName="LOOK_BACK_MINUTES"
                       nullable="false"
                       ui="NumberUI"
                       uiParameter='precision=0'><%=(lookBackMinutes==null?"":lookBackMinutes)%></wb:controlField>

    </wba:td>
  </wba:tr>

  <wba:tr>
    <wba:th width='20%'>
      <wb:localize id="LOG_STATUS">Log Status:</wb:localize>
    </wba:th>
    <wba:td width='80%'>
      <wb:controlField cssClass="inputField" submitName="LOG_STATUS"
                       ui="StringUI"
                       uiParameter='width=40'><%=logStatus%></wb:controlField>

    </wba:td>
  </wba:tr>
</wba:table>

<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
