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
<wb:define id="nMinutes"><wb:get id="nMinutes" scope="parameter" default=""/></wb:define>
<wb:define id="timePeriod"><wb:get id="timePeriod" scope="parameter" default=""/></wb:define>
<wb:define id="wrkCode"><wb:get id="wrkCode" scope="parameter" default=""/></wb:define>
<wb:define id="wrkCodeInclusive"><wb:get id="wrkCodeInclusive" scope="parameter" default="TRUE"/></wb:define>
<wb:define id="hType"><wb:get id="hType" scope="parameter" default=""/></wb:define>
<wb:define id="hTypeInclusive"><wb:get id="hTypeInclusive" scope="parameter" default=""/></wb:define>
<wb:define id="employee"><wb:get id="employee" scope="parameter" default=""/></wb:define>
<wb:define id="calcGroup"><wb:get id="calcGroup" scope="parameter" default=""/></wb:define>
<wb:define id="payGroup"><wb:get id="payGroup" scope="parameter" default=""/></wb:define>
<wb:define id="minimumAge"><wb:get id="minimumAge" scope="parameter" default=""/></wb:define>
<wb:define id="team"><wb:get id="team" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%
  String singleQuote = "'";
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int alertID = Integer.valueOf((String) param.get( WBAlertTask.PARAM_ALERTID )).intValue();
  String nMin = (String)param.get( WorkedNMinutesAlertSource.PARAM_N_MINUTES );
  String tPeriod = (String)param.get( WorkedNMinutesAlertSource.PARAM_TIME_PERIOD );
  String wrk = (String)param.get( WorkedNMinutesAlertSource.PARAM_WRK_TCODE ); 
  String wrkInc = (String)param.get( WorkedNMinutesAlertSource.PARAM_WRK_TCODE_INCLUSIVE );
  String hr = (String)param.get( WorkedNMinutesAlertSource.PARAM_HTYPE );
  String hrInc = (String)param.get( WorkedNMinutesAlertSource.PARAM_HTYPE_INCLUSIVE );
  String emp = (String)param.get( WorkedNMinutesAlertSource.PARAM_EMPLOYEE );
  String minAge = (String)param.get( WorkedNMinutesAlertSource.PARAM_MIN_EMP_AGE );
  String calc = (String)param.get( WorkedNMinutesAlertSource.PARAM_CALCGROUP );
  String pay = (String)param.get( WorkedNMinutesAlertSource.PARAM_PAYGROUP  );
  String empTeam = (String)param.get( WorkedNMinutesAlertSource.PARAM_TEAM );

if ("SUBMIT".equals(OPERATION.toString())) {
	HashMap newParam = new HashMap();
	newParam.put( WBAlertTask.PARAM_ALERTID, String.valueOf( alertID ) );
	newParam.put( WorkedNMinutesAlertSource.PARAM_N_MINUTES, nMinutes == null ? "" : nMinutes.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_TIME_PERIOD, timePeriod== null ? "" : timePeriod.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_MIN_EMP_AGE, minimumAge== null ? "" : minimumAge.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_WRK_TCODE, wrkCode== null ? "" : wrkCode.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_WRK_TCODE_INCLUSIVE, wrkCodeInclusive== null ? "" : wrkCodeInclusive.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_HTYPE, hType== null ? "" : hType.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_HTYPE_INCLUSIVE, hTypeInclusive== null ? "" : hTypeInclusive.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_EMPLOYEE, employee== null ? "" : employee.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_CALCGROUP, calcGroup== null ? "" : calcGroup.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_PAYGROUP, payGroup== null ? "" : payGroup.toString() );
	newParam.put( WorkedNMinutesAlertSource.PARAM_TEAM, team== null ? "" : team.toString() );
	scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
%>
<Span>Type updated successfully</Span><BR>
<wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
<% } else { %>
<wba:table caption="Worked N Minutes Alert Parameters" captionLocalizeIndex="Worked_N_Minutes_Without_Break_Alert_Parameters">
	<wba:tr>
    		<wba:th width='20%'>
      		<wb:localize id="nMinutes">N Minutes</wb:localize>
    		</wba:th>
    		<wba:td width='80%'>
      	<wb:controlField cssClass="inputField" submitName="nMinutes"
                       nullable="false"
                       ui="StringUI"
                       uiParameter='precision=0'><%=(nMinutes==null?"":nMin)%></wb:controlField>

    		</wba:td>
  	</wba:tr>
	<wba:tr>
    		<wba:th width='20%'>
      		<wb:localize id="wrkCode">Time Codes</wb:localize>
    		</wba:th>
    		<wba:td width='80%'>
      	<wb:controlField cssClass="inputField" submitName="wrkCode"
                       nullable="true"
                       ui="StringUI"
                       uiParameter='precision=0'><%=(wrkCode==null?"":wrk)%></wb:controlField>

    		</wba:td>
  	</wba:tr>
	<wba:tr>
    		<wba:th width='20%'>
      		<wb:localize id="wrkCodeInclusive">Time Codes Inclusive</wb:localize>
    		</wba:th>
    		<wba:td width='80%'>
      	<wb:controlField cssClass="inputField" submitName="wrkCodeInclusive"
                       nullable="false"
                       ui="StringUI"
                       uiParameter='precision=0'><%=(wrkCodeInclusive==null?"TRUE":wrkInc)%></wb:controlField>

    		</wba:td>
  	</wba:tr>
	<wba:tr>
    		<wba:th width='20%'>
      		<wb:localize id="hType">Hour Types</wb:localize>
    		</wba:th>
    		<wba:td width='80%'>
      	<wb:controlField cssClass="inputField" submitName="hType"
                       nullable="true"
                       ui="StringUI"
                       uiParameter='precision=0'><%=(hType==null?"":hr)%></wb:controlField>

    		</wba:td>
  	</wba:tr>
	<wba:tr>
    		<wba:th width='20%'>
      		<wb:localize id="hTypeInclusive">Hour Types Inclusive</wb:localize>
    		</wba:th>
    		<wba:td width='80%'>
      	<wb:controlField cssClass="inputField" submitName="hTypeInclusive"
                       nullable="true"
                       ui="StringUI"
                       uiParameter='precision=0'><%=(hTypeInclusive==null?"TRUE":hrInc)%></wb:controlField>

    		</wba:td>
  	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="timeFrame">Time Frame (daily or weekly)</wb:localize>
		</wba:th>
    	<wba:td width='80%'>
      		<wb:controlField cssClass="inputField" submitName="timePeriod"
                       nullable="false"
                       ui="StringUI"
                       uiParameter='precision=0'><%=(timePeriod==null?"daily":tPeriod)%></wb:controlField>

    	</wba:td>
	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="minAge">Minimum Employee Age</wb:localize>
		</wba:th>
    	<wba:td width='80%'>
      		<wb:controlField cssClass="inputField" submitName="minimumAge"
                       nullable="false"
                       ui="StringUI"
                       uiParameter='precision=0'><%=(minimumAge==null?"":minAge)%></wb:controlField>

    	</wba:td>
	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="employee">Employee:</wb:localize>
		</wba:th>
		<wba:td width='80%'>
	  <%  String strQuery = "width=50 multiChoice='true' pageSize='12' title='Employee' sourceType='SQL' source=\"SELECT EMP_ID, EMP_FULLNAME FROM EMPLOYEE ORDER BY EMP_ID\" sourceKeyField=EMP_ID sourceLabelField=EMP_ID"; %>
		<wb:controlField cssClass="inputField" submitName="employee" ui="DBLookupUI" uiParameter='<%=strQuery%>' ><%=emp%></wb:controlField>
	</wba:td>
	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="calcGroup">Calc Group:</wb:localize>
		</wba:th>
		<wba:td width='80%'>
	  <%  String strQuery = "width=50 multiChoice='true' pageSize='12' title='Calc Group' sourceType='SQL' source=\"SELECT CALCGRP_ID, CALCGRP_NAME FROM CALC_GROUP ORDER BY CALCGRP_ID\" sourceKeyField=CALCGRP_ID sourceLabelField=CALCGRP_ID"; %>
		<wb:controlField cssClass="inputField" submitName="calcGroup" ui="DBLookupUI" uiParameter='<%=strQuery%>' ><%=calc%></wb:controlField>
	</wba:td>
	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="payGroup">Pay Group:</wb:localize>
		</wba:th>
		<wba:td width='80%'>
	  <%  String strQuery = "width=50 multiChoice='true' pageSize='12' title='Pay Group' sourceType='SQL' source=\"SELECT PAYGRP_ID, PAYGRP_NAME FROM PAY_GROUP ORDER BY PAYGRP_ID\" sourceKeyField=PAYGRP_ID sourceLabelField=PAYGRP_ID"; %>
		<wb:controlField cssClass="inputField" submitName="payGroup" ui="DBLookupUI" uiParameter='<%=strQuery%>' ><%=pay%></wb:controlField>
	</wba:td>
	</wba:tr>
	<wba:tr>
	<wba:th width='20%'>
		<wb:localize id="team">Team:</wb:localize>
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
