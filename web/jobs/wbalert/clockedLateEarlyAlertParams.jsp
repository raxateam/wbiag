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
<wb:define id="xMinutes"><wb:get id="xMinutes" scope="parameter" default=""/></wb:define>
<wb:define id="earlyLate"><wb:get id="earlyLate" scope="parameter" default=""/></wb:define>
<wb:define id="clockType"><wb:get id="clockType" scope="parameter" default=""/></wb:define>
<wb:define id="maxAge"><wb:get id="maxAge" scope="parameter" default=""/></wb:define>
<wb:define id="employee"><wb:get id="employee" scope="parameter" default=""/></wb:define>
<wb:define id="calcGroup"><wb:get id="calcGroup" scope="parameter" default=""/></wb:define>
<wb:define id="payGroup"><wb:get id="payGroup" scope="parameter" default=""/></wb:define>
<wb:define id="team"><wb:get id="team" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%
  String singleQuote = "'";
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int alertID = Integer.valueOf((String) param.get( WBAlertTask.PARAM_ALERTID )).intValue();
  String xMin = (String)param.get( ClockedLateEarlyAlertSource.PARAM_MINUTES );
  String earLate = (String)param.get( ClockedLateEarlyAlertSource.PARAM_EARLY_LATE );
  String clkType = (String)param.get( ClockedLateEarlyAlertSource.PARAM_CLOCK_TYPE ); 
  String mAge = (String)param.get( ClockedLateEarlyAlertSource.PARAM_MAXIMUM_AGE); 
  String emp = (String)param.get( ClockedLateEarlyAlertSource.PARAM_EMPLOYEE );
  String calc = (String)param.get( ClockedLateEarlyAlertSource.PARAM_CALCGROUP );
  String pay = (String)param.get( ClockedLateEarlyAlertSource.PARAM_PAYGROUP  );
  String empTeam = (String)param.get( ClockedLateEarlyAlertSource.PARAM_TEAM );

if ("SUBMIT".equals(OPERATION.toString())) {
	HashMap newParam = new HashMap();
	newParam.put( WBAlertTask.PARAM_ALERTID, String.valueOf( alertID ) );
	newParam.put( ClockedLateEarlyAlertSource.PARAM_MINUTES, xMinutes == null ? "" : xMinutes.toString() );
    newParam.put( ClockedLateEarlyAlertSource.PARAM_EARLY_LATE, earlyLate== null ? "" : earlyLate.toString() );
	newParam.put( ClockedLateEarlyAlertSource.PARAM_CLOCK_TYPE, clockType== null ? "" : clockType.toString() );
    newParam.put( ClockedLateEarlyAlertSource.PARAM_MAXIMUM_AGE, maxAge == null ? "" : maxAge.toString() );
	newParam.put( WorkedNMinutesWithoutBreakAlertSource.PARAM_EMPLOYEE, employee== null ? "" : employee.toString() );
	newParam.put( WorkedNMinutesWithoutBreakAlertSource.PARAM_CALCGROUP, calcGroup== null ? "" : calcGroup.toString() );
	newParam.put( WorkedNMinutesWithoutBreakAlertSource.PARAM_PAYGROUP, payGroup== null ? "" : payGroup.toString() );
	newParam.put( WorkedNMinutesWithoutBreakAlertSource.PARAM_TEAM, team== null ? "" : team.toString() );
	scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
%>
<Span>Type updated successfully</Span><BR>
<wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
<% } else { %>
<wba:table caption="Clocked Late Early Alert Parameters" captionLocalizeIndex="Clocked_Late_Early_Alert_Parameters">
	<wba:tr>
    		<wba:th width='20%'>
      		<wb:localize id="xMinutes">X Minutes *</wb:localize>
    		</wba:th>
    		<wba:td width='80%'>
      	<wb:controlField cssClass="inputField" submitName="xMinutes"
                       nullable="false"
                       ui="NumberUI"
                       uiParameter='precision=0'><%=(xMinutes==null?"":xMin)%></wb:controlField>

    		</wba:td>
  	</wba:tr>
	
	<wba:tr>
    		<wba:th width='20%'>
      		<wb:localize id="earlyLate">Early/late *</wb:localize>
    		</wba:th>
    		<wba:td width='80%'>
      	       <wb:controlField cssClass="inputField" submitName="earlyLate"
                       nullable="false"
                       ui="ListBoxUI"
                       uiParameter="labelList='Early,Late' ValueList='EARLY,LATE'"><%=(earLate==null?"":earLate)%></wb:controlField>
    		</wba:td>
  	</wba:tr>
	<wba:tr>
    		<wba:th width='20%'>
      		<wb:localize id="clockType">Out/in *</wb:localize>
    		</wba:th>
    		<wba:td width='80%'>
      	       <wb:controlField cssClass="inputField" submitName="clockType"
                       nullable="false"
                       ui="ListBoxUI"
                       uiParameter="labelList='Out,In' ValueList='OUT,IN'"><%=(clkType==null?"":clkType)%></wb:controlField>
    		</wba:td>
  	</wba:tr>
	<wba:tr>
    		<wba:th width='20%'>
      		<wb:localize id="maxAge">Max Employee Age</wb:localize>
    		</wba:th>
    		<wba:td width='80%'>
      	       <wb:controlField cssClass="inputField" submitName="maxAge"
                       nullable="true"
                       ui="NumberUI"
                       uiParameter='precision=0'><%=(maxAge==null?"":mAge)%></wb:controlField>

    		</wba:td>
  	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="employee">Employee</wb:localize>
		</wba:th>
		<wba:td width='80%'>
	  <%  String strQuery = "width=50 multiChoice='true' pageSize='12' title='Employee' sourceType='SQL' source=\"SELECT EMP_ID, EMP_NAME, EMP_FULLNAME FROM EMPLOYEE ORDER BY EMP_ID\" sourceKeyField=EMP_ID sourceLabelField=EMP_ID"; %>
		<wb:controlField cssClass="inputField" submitName="employee" ui="DBLookupUI" uiParameter='<%=strQuery%>' ><%=emp%></wb:controlField>
	</wba:td>
	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="calcGroup">Calc Group</wb:localize>
		</wba:th>
		<wba:td width='80%'>
	  <%  String strQuery = "width=50 multiChoice='true' pageSize='12' title='Calc Group' sourceType='SQL' source=\"SELECT CALCGRP_ID, CALCGRP_NAME FROM CALC_GROUP ORDER BY CALCGRP_ID\" sourceKeyField=CALCGRP_ID sourceLabelField=CALCGRP_ID"; %>
		<wb:controlField cssClass="inputField" submitName="calcGroup" ui="DBLookupUI" uiParameter='<%=strQuery%>' ><%=calc%></wb:controlField>
	</wba:td>
	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="payGroup">Pay Group</wb:localize>
		</wba:th>
		<wba:td width='80%'>
	  <%  String strQuery = "width=50 multiChoice='true' pageSize='12' title='Pay Group' sourceType='SQL' source=\"SELECT PAYGRP_ID, PAYGRP_NAME FROM PAY_GROUP ORDER BY PAYGRP_ID\" sourceKeyField=PAYGRP_ID sourceLabelField=PAYGRP_ID"; %>
		<wb:controlField cssClass="inputField" submitName="payGroup" ui="DBLookupUI" uiParameter='<%=strQuery%>' ><%=pay%></wb:controlField>
	</wba:td>
	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="team">Team</wb:localize>
		</wba:th>
		<wba:td width='80%'>
	  <%  String strQuery = "width=50 multiChoice='true' pageSize='12' title='Team' sourceType='SQL' source=\"SELECT WBT_ID, WBT_NAME FROM WORKBRAIN_TEAM ORDER BY WBT_ID\" sourceKeyField=WBT_ID sourceLabelField=WBT_ID"; %>
		<wb:controlField cssClass="inputField" submitName="team" ui="DBLookupUI" uiParameter='<%=strQuery%>' ><%=empTeam%></wb:controlField>
	</wba:td>
	</wba:tr>
</wba:table>

<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
