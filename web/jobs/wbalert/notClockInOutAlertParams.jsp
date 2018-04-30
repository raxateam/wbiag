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
<wb:define id="CHECK_NO_CLOCK_IN"><wb:get id="CHECK_NO_CLOCK_IN" scope="parameter" default=""/></wb:define>
<wb:define id="CHECK_NO_CLOCK_OUT"><wb:get id="CHECK_NO_CLOCK_OUT" scope="parameter" default=""/></wb:define>
<wb:define id="CLOCK_IN_TYPES"><wb:get id="CLOCK_IN_TYPES" scope="parameter" default=""/></wb:define>
<wb:define id="CLOCK_OUT_TYPES"><wb:get id="CLOCK_OUT_TYPES" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%
  String clockTypeSQL = "sourceType=SQL source=\"SELECT CLKTRANTYPE_ID, CLKTRANTYPE_NAME Name FROM CLOCK_TRAN_TYPE\" ";
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int alertID = Integer.valueOf((String) param.get( WBAlertTask.PARAM_ALERTID )).intValue();
  Boolean checkNoClockIn = "Y".equals((String)param.get( NotClockInOutAlertSource.PARAM_CHECK_NO_CLOCK_IN)) ? Boolean.TRUE : Boolean.FALSE;
  Boolean checkNoClockOut = "Y".equals((String)param.get( NotClockInOutAlertSource.PARAM_CHECK_NO_CLOCK_OUT )) ? Boolean.TRUE : Boolean.FALSE;
  String clockInTypes = (String)param.get( NotClockInOutAlertSource.PARAM_CLOCK_IN_TYPES);
  String clockOutTypes = (String)param.get( NotClockInOutAlertSource.PARAM_CLOCK_OUT_TYPES);
  //String clockInTypes = (String)param.get( "clockInTypes" );
  //String clockOutTypes = (String)param.get( "clockOutTypes" );

if ("SUBMIT".equals(OPERATION.toString())) {
  HashMap newParam = new HashMap();
  newParam.put( WBAlertTask.PARAM_ALERTID, String.valueOf( alertID ) );
  newParam.put( NotClockInOutAlertSource.PARAM_CHECK_NO_CLOCK_IN, CHECK_NO_CLOCK_IN == null ? "" : CHECK_NO_CLOCK_IN.toString() );
  newParam.put( NotClockInOutAlertSource.PARAM_CHECK_NO_CLOCK_OUT, CHECK_NO_CLOCK_OUT == null ? "" : CHECK_NO_CLOCK_OUT.toString() );
  newParam.put( NotClockInOutAlertSource.PARAM_CLOCK_IN_TYPES, CLOCK_IN_TYPES == null ? "1" : CLOCK_IN_TYPES.toString() );
  newParam.put( NotClockInOutAlertSource.PARAM_CLOCK_OUT_TYPES, CLOCK_OUT_TYPES == null ? "2" : CLOCK_OUT_TYPES.toString() );

  scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
  %>
  <Span>Type updated successfully</Span>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  <%
} else {
%>
<wb:set id="CLOCK_IN_TYPES"><%=clockInTypes%></wb:set> 
<wb:set id="CLOCK_OUT_TYPES"><%=clockOutTypes%></wb:set> 
<wb:set id="CHECK_NO_CLOCK_IN"><%=checkNoClockIn%></wb:set> 
<wb:set id="CHECK_NO_CLOCK_OUT"><%=checkNoClockOut%></wb:set> 


<wba:table caption="Not Clock In or Out Alert Parameters" captionLocalizeIndex="Not Clock In Out Alert Parameters" width="200">
  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="CHECK_NO_CLOCK_IN">Check No Clock In</wb:localize>
    </wba:th>
    <wba:td width='60%'>
      <wb:controlField cssClass="inputField" submitName="CHECK_NO_CLOCK_IN" ui="CheckboxUI"  uiParameter=""><%=checkNoClockIn%></wb:controlField>

    </wba:td>
  </wba:tr>

   <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="CHECK_NO_CLOCK_OUT">Check No Clock Out</wb:localize>
    </wba:th>
    <wba:td width='60%'>
      <wb:controlField cssClass="inputField" submitName="CHECK_NO_CLOCK_OUT" ui="CheckboxUI"  uiParameter=""><%=checkNoClockOut%></wb:controlField>
    </wba:td>
  </wba:tr> 

  <wba:tr>
<wba:th width='40%'>
  <wb:localize id="CLOCK_IN_TYPES">Clock In Types</wb:localize>
</wba:th>
<wba:td width='60%'> 
<wb:controlField cssClass="inputField" submitName="CLOCK_IN_TYPES" ui="DBLookupUI" uiParameter='<%=clockTypeSQL + " multiChoice=true"%>'><%=clockInTypes%></wb:controlField> 
</wba:td>
  </wba:tr>
 <wba:tr>
<wba:th width='40%'>
  <wb:localize id="CLOCK_OUT_TYPES">Clock Out Types</wb:localize>
</wba:th>
<wba:td width='60%'> 
<wb:controlField cssClass="inputField" submitName="CLOCK_OUT_TYPES" ui="DBLookupUI" uiParameter='<%=clockTypeSQL + " multiChoice=true"%>'><%=clockOutTypes%></wb:controlField> 
</wba:td>
  </wba:tr> 

</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
