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
<wb:define id="WBITYP_NAMES"><wb:get id="WBITYP_NAMES" scope="parameter" default=""/></wb:define>
<wb:define id="WBITRAN_STATUS"><wb:get id="WBITRAN_STATUS" scope="parameter" default=""/></wb:define>
<wb:define id="LOOK_BACK_MINUTES"><wb:get id="LOOK_BACK_MINUTES" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%
  String singleQuote = "'";
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int alertID = Integer.valueOf((String) param.get( WBAlertTask.PARAM_ALERTID )).intValue();
  String lookBackMinutes = (String)param.get( WBIntTransactionAlertSource.PARAM_LOOK_BACK_MINUTES );
  String wbitypNames = (String)param.get( WBIntTransactionAlertSource.PARAM_WBINT_TYPE_NAMES );
  String wbitranStatus = (String)param.get( WBIntTransactionAlertSource.PARAM_WBITRAN_STATUS );

if ("SUBMIT".equals(OPERATION.toString())) {
	HashMap newParam = new HashMap();
	newParam.put( WBAlertTask.PARAM_ALERTID, String.valueOf( alertID ) );
	newParam.put( WBIntTransactionAlertSource.PARAM_WBINT_TYPE_NAMES, WBITYP_NAMES == null ? "" : WBITYP_NAMES.toString() );
	newParam.put( WBIntTransactionAlertSource.PARAM_WBITRAN_STATUS, WBITRAN_STATUS == null ? "" : WBITRAN_STATUS.toString() );
    newParam.put( WBIntTransactionAlertSource.PARAM_LOOK_BACK_MINUTES, LOOK_BACK_MINUTES == null ? "" : LOOK_BACK_MINUTES.toString() );
	scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
%>
<Span>Type updated successfully</Span><BR>
<wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
<% } else { %>
<wba:table caption="Interface Transaction Alert Parameters" captionLocalizeIndex="Interface Transaction Alert Parameters">
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="WBITYP_NAMES">Interface Types:</wb:localize>
	</wba:th>
	<wba:td width='80%'>
	  <%  String strQuery = "width=50 multiChoice='true' pageSize='12' title='Interface Types' sourceType='SQL' source=\"SELECT wbityp_id, wbityp_name FROM wbint_type ORDER BY 2\" sourceKeyField=wbityp_name"; %>
		<wb:controlField cssClass="inputField" submitName="WBITYP_NAMES" ui="DBLookupUI" uiParameter='<%=strQuery%>' ><%=wbitypNames%></wb:controlField>
	</wba:td>
	</wba:tr>

	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="PARAM_WBITRAN_STATUS">Interface Status:</wb:localize>
	</wba:th>
	<wba:td width='80%'>
	  <%  String strQuery = "valueList='SUCCESS, FAILURE, IGNORED, PENDING, INTERRUPTED, IN PROGRESS'"; %>
		<wb:controlField cssClass="inputField" submitName="WBITRAN_STATUS" ui="ComboBoxUI" uiParameter='<%=strQuery%>' ><%=wbitranStatus%></wb:controlField>
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
</wba:table>

<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
