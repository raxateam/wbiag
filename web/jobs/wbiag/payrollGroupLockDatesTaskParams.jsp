<%@ include file="/system/wbheader.jsp"%>
<%@ taglib uri="/wbsys" prefix="wb" %>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="javax.servlet.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.wbiag.app.ta.ruleengine.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="org.apache.log4j.*"%>
<%!

    private static final String CLIENT_UI_PARAM =
      "sourceType=SQL source='SELECT client_id, client_name FROM workbrain_client' multiChoice=false";
    private static final String WBGROUP_PARAM =
      "sourceType=SQL source='SELECT wbg_id, wbg_name FROM workbrain_group' multiChoice=true all=false";
    private static final String PAYGRP_PARAM =
      "sourceType=SQL source='SELECT paygrp_id, paygrp_name FROM pay_group' multiChoice=true all=false";

%>
<%-- ********************** --%>
<wb:page login="true">
<script language="JavaScript">
  function checkSubmit(){
      if(document.page_form.clientId.value  == "") {
          alert("Client id has to be specified");
          return;
      }
      if(document.page_form.lockMode.value  == "") {
          alert("Lock Mode has to be specified");
          return;
      }
      disableAllButtons();
      document.page_form.submit();
  }
</script>
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>
<wb:define id="onClickCancel">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>
<wb:define id="wbGroupsSup"><wb:get id="wbGroupsSup" scope="parameter" default=""/></wb:define>
<wb:define id="wbGroupsPC"><wb:get id="wbGroupsPC" scope="parameter" default=""/></wb:define>
<wb:define id="payGrps"><wb:get id="payGrps" scope="parameter" default=""/></wb:define>
<wb:define id="clientId"><wb:get id="clientId" scope="parameter" default=""/></wb:define>
<wb:define id="lockMode"><wb:get id="lockMode" scope="parameter" default=""/></wb:define>

<%
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new javax.servlet.jsp.JspException("No task id has been passed to the page");

  Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    if(recparams.isEmpty()) {
        recparams.put( PayrollGroupLockDatesTask.PARAM_APPLY_PAY_GROUPS, "" );
        recparams.put( PayrollGroupLockDatesTask.PARAM_PAYROLL_COORD_SEC_GROUPS, "" );
        recparams.put( PayrollGroupLockDatesTask.PARAM_SUPERVISOR_SEC_GROUPS, "" );
        recparams.put( PayrollGroupLockDatesTask.PARAM_PAYROLL_DATES_LOCK_MODE, "" );
        recparams.put( PayrollGroupLockDatesTask.PARAM_CLIENT_ID, "" );
    }
    %>
    <wb:switch>
    <wb:case expression="#TYPE#" compareToExpression="">
      <input type="hidden" name="TYPE" value="scheduleTask">
      <wb:set id="wbGroupsSup"><%=recparams.get( PayrollGroupLockDatesTask.PARAM_SUPERVISOR_SEC_GROUPS )%></wb:set>
      <wb:set id="wbGroupsPC"><%=recparams.get( PayrollGroupLockDatesTask.PARAM_PAYROLL_COORD_SEC_GROUPS )%></wb:set>
      <wb:set id="payGrps"><%=recparams.get( PayrollGroupLockDatesTask.PARAM_APPLY_PAY_GROUPS )%></wb:set>
      <wb:set id="clientId"><%=recparams.get( PayrollGroupLockDatesTask.PARAM_CLIENT_ID )%></wb:set>
      <wb:set id="lockMode"><%=recparams.get( PayrollGroupLockDatesTask.PARAM_PAYROLL_DATES_LOCK_MODE )%></wb:set>

      <wba:table caption="Payroll Group Lock Dates Task" captionLocalizeIndex="Payroll Group Lock Dates Task" width="600">

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_SUPERVISOR_SEC_GROUPS">PARAM_SUPERVISOR_SEC_GROUPS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="wbGroupsSup" ui='DBLookupUI' uiParameter='<%=WBGROUP_PARAM%>'><%=wbGroupsSup%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PAYROLL_COORD_SEC_GROUPS">PARAM_PAYROLL_COORD_SEC_GROUPS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="wbGroupsPC" ui='DBLookupUI' uiParameter='<%=WBGROUP_PARAM%>'><%=wbGroupsPC%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_APPLY_PAY_GROUPS">PARAM_APPLY_PAY_GROUPS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="payGrps" ui='DBLookupUI' uiParameter='<%=PAYGRP_PARAM%>'><%=payGrps%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PAYROLL_DATES_LOCK_MODE">PARAM_PAYROLL_DATES_LOCK_MODE</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="lockMode" ui='ComboBoxUI' uiParameter="valueList='LOCK,UNLOCK' labelList='LOCK,UNLOCK'"><%=lockMode%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_CLIENT_ID">PARAM_CLIENT_ID</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="clientId" ui='DBLookupUI' uiParameter='<%=CLIENT_UI_PARAM%>'><%=clientId%></wb:controlField>
          </wba:td>
        </wba:tr>


      </wba:table>

      <div class="separatorLarge"></div>

      <wba:button label="Submit" labelLocalizeIndex="Submit" onClick="checkSubmit();"/>&nbsp;
      <wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="#onClickCancel#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%
            recparams.put( PayrollGroupLockDatesTask.PARAM_SUPERVISOR_SEC_GROUPS , wbGroupsSup.toString() );
            recparams.put( PayrollGroupLockDatesTask.PARAM_PAYROLL_COORD_SEC_GROUPS , wbGroupsPC.toString() );
            recparams.put( PayrollGroupLockDatesTask.PARAM_APPLY_PAY_GROUPS , payGrps.toString() );
            recparams.put( PayrollGroupLockDatesTask.PARAM_CLIENT_ID , clientId.toString() );
            recparams.put( PayrollGroupLockDatesTask.PARAM_PAYROLL_DATES_LOCK_MODE , lockMode.toString() );
            scheduler.setTaskParams( Integer.parseInt( TASK_ID.toString() ), recparams );

        %>
        <Span>Payroll Group Lock Dates Task Successfully Scheduled</Span>
        <br>
        <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
    </wb:case>
    </wb:switch>
</wb:if>
</wb:page>
