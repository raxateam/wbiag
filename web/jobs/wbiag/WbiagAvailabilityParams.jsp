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
<%@ page import="com.wbiag.app.modules.availability.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="org.apache.log4j.*"%>
<%!

    private static final String CLIENT_UI_PARAM =
      "sourceType=SQL source='SELECT client_id, client_name FROM workbrain_client' multiChoice=false";
    private static final String WBTEAM_PARAM =
      "sourceType=SQL source='SELECT wbt_id, wbt_name FROM workbrain_team' multiChoice=true";
    private static final String CALCGRP_PARAM =
      "sourceType=SQL source='SELECT calcgrp_id, calcgrp_name FROM calc_group' multiChoice=true";
    private static final String PAYGRP_PARAM =
      "sourceType=SQL source='SELECT paygrp_id, paygrp_name FROM pay_group' multiChoice=true";
    private static final String EMP_PARAM =
      "sourceType=SQL source='SELECT emp_id, emp_name FROM employee' multiChoice=true";

%>
<%-- ********************** --%>
<wb:page login="true">
<script language="JavaScript">
  function checkSubmit(){
      if(document.page_form.clientId.value  == "") {
          alert("Client id has to be specified");
          return;
      }
      if(document.page_form.rangeType[0].checked == true) {
        if (document.page_form.daysBefore.value == ""
            || document.page_form.daysAfter.value == "") {
          alert("Both days before and days after have to be specified when relative is selected");
          return;
        }
      }

      if(document.page_form.rangeType[1].checked == true) {
        if (document.page_form.absStartDate.value == ""
            || document.page_form.absEndDate.value == "") {
          alert("Both start and end date have to be specified when absolute is selected");
          return;
        }
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
<wb:define id="wbTeams"><wb:get id="wbTeams" scope="parameter" default=""/></wb:define>
<wb:define id="subTeams"><wb:get id="subTeams" scope="parameter" default=""/></wb:define>
<wb:define id="calcGrps"><wb:get id="calcGrps" scope="parameter" default=""/></wb:define>
<wb:define id="payGrps"><wb:get id="payGrps" scope="parameter" default=""/></wb:define>
<wb:define id="employees"><wb:get id="employees" scope="parameter" default=""/></wb:define>
<wb:define id="clientId"><wb:get id="clientId" scope="parameter" default=""/></wb:define>
<wb:define id="rangeType"><wb:get id="rangeType" scope="parameter" default=""/></wb:define>
<wb:define id="absStartDate"><wb:get id="absStartDate" scope="parameter" default=""/></wb:define>
<wb:define id="absEndDate"><wb:get id="absEndDate" scope="parameter" default=""/></wb:define>
<wb:define id="daysBefore"><wb:get id="daysBefore" scope="parameter" default=""/></wb:define>
<wb:define id="daysAfter"><wb:get id="daysAfter" scope="parameter" default=""/></wb:define>
<wb:define id="recreatesRecords"><wb:get id="recreatesRecords" scope="parameter" default=""/></wb:define>


<%
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    if(recparams.isEmpty()) {
        recparams.put( WbiagAvailabilityTask.PARAM_WBTEAMS, "" );
        recparams.put( WbiagAvailabilityTask.PARAM_SUBTEAMS, "N" );
        recparams.put( WbiagAvailabilityTask.PARAM_CALCGRPS, "" );
        recparams.put( WbiagAvailabilityTask.PARAM_PAYGRPS, "" );
        recparams.put( WbiagAvailabilityTask.PARAM_EMPLOYEES, "" );
        recparams.put( WbiagAvailabilityTask.PARAM_CLIENT_ID, "" );
        recparams.put( WbiagAvailabilityTask.PARAM_RANGE_TYPE, WbiagAvailabilityTask.PARAM_VAL_RANGE_TYPE_RELATIVE );
        recparams.put( WbiagAvailabilityTask.PARAM_ABS_START_DATE, "");
        recparams.put( WbiagAvailabilityTask.PARAM_ABS_END_DATE, "");
        recparams.put( WbiagAvailabilityTask.PARAM_DAYS_BEFORE, "");
        recparams.put( WbiagAvailabilityTask.PARAM_DAYS_AFTER, "");
        recparams.put( WbiagAvailabilityTask.PARAM_RECREATES_RECORDS, "N" );
    }
    %>
    <wb:switch>
    <wb:case expression="#TYPE#" compareToExpression="">
      <input type="hidden" name="TYPE" value="scheduleTask">
      <wb:set id="wbTeams"><%=recparams.get( WbiagAvailabilityTask.PARAM_WBTEAMS )%></wb:set>
      <wb:set id="subTeams"><%=recparams.get( WbiagAvailabilityTask.PARAM_SUBTEAMS )%></wb:set>
      <wb:set id="calcGrps"><%=recparams.get( WbiagAvailabilityTask.PARAM_CALCGRPS )%></wb:set>
      <wb:set id="payGrps"><%=recparams.get( WbiagAvailabilityTask.PARAM_PAYGRPS )%></wb:set>
      <wb:set id="employees"><%=recparams.get( WbiagAvailabilityTask.PARAM_EMPLOYEES )%></wb:set>
      <wb:set id="clientId"><%=recparams.get( WbiagAvailabilityTask.PARAM_CLIENT_ID )%></wb:set>
      <wb:set id="rangeType"><%=recparams.get( WbiagAvailabilityTask.PARAM_RANGE_TYPE )%></wb:set>
      <wb:set id="absStartDate"><%=recparams.get( WbiagAvailabilityTask.PARAM_ABS_START_DATE )%></wb:set>
      <wb:set id="absEndDate"><%=recparams.get( WbiagAvailabilityTask.PARAM_ABS_END_DATE )%></wb:set>
      <wb:set id="daysBefore"><%=recparams.get( WbiagAvailabilityTask.PARAM_DAYS_BEFORE )%></wb:set>
      <wb:set id="daysAfter"><%=recparams.get( WbiagAvailabilityTask.PARAM_DAYS_AFTER )%></wb:set>
      <wb:set id="recreatesRecords"><%=recparams.get( WbiagAvailabilityTask.PARAM_RECREATES_RECORDS )%></wb:set>

      <wba:table caption="Availability Task" captionLocalizeIndex="Availability_Task" width="600">

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_WBTEAMS">PARAM_WBTEAMS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="wbTeams" ui='DBLookupUI' uiParameter='<%=WBTEAM_PARAM%>'><%=wbTeams%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_SUBTEAMS">PARAM_SUBTEAMS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="subTeams" ui='CheckboxUI' uiParameter=''><%=subTeams%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_CALCGRPS">PARAM_CALCGRPS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="calcGrps" ui='DBLookupUI' uiParameter='<%=CALCGRP_PARAM%>'><%=calcGrps%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PAYGRPS">PARAM_PAYGRPS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="payGrps" ui='DBLookupUI' uiParameter='<%=PAYGRP_PARAM%>'><%=payGrps%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_EMPLOYEES">PARAM_EMPLOYEES</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="employees" ui='DBLookupUI' uiParameter='<%=EMP_PARAM%>'><%=employees%></wb:controlField>
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

	    <wba:tr>
		  <wba:td>
			<wb:localize id="DATES">Dates to run availablity task for:</wb:localize>
		  </wba:td>
	    </wba:tr>
	    <wba:tr>
		  <wba:td>
			<wb:localize id="RELATIVE">Relative (x days before or after today)</wb:localize>
		  </wba:td>
		  <wba:td>
            <wba:table>
	         <wba:tr><wba:td>
			  <input type='radio' name='rangeType' value='<%=WbiagAvailabilityTask.PARAM_VAL_RANGE_TYPE_RELATIVE%>' <wb:if expression='#rangeType#' compareToExpression='<%=WbiagAvailabilityTask.PARAM_VAL_RANGE_TYPE_RELATIVE%>'>checked</wb:if>>
             </wba:td></wba:tr>
             <wba:tr><wba:td>
               Days Before Current Date: <wb:controlField cssClass='inputField' submitName="daysBefore" ui='StringUI' uiParameter=''><%=daysBefore%></wb:controlField>
             </wba:td></wba:tr>
             <wba:tr><wba:td>
              Days After Current Date:   <wb:controlField cssClass='inputField' submitName="daysAfter" ui='StringUI' uiParameter=''><%=daysAfter%></wb:controlField>
             </wba:td></wba:tr>
            </wba:table>
		  </wba:td>
	    </wba:tr>
	    <wba:tr>
		 <wba:td>
			<wb:localize id="ABSOLUTE">Absolute (for these dates)</wb:localize>
		 </wba:td>
		  <wba:td>
            <wba:table>
	         <wba:tr><wba:td>
			  <input type='radio' name='rangeType' value='<%=WbiagAvailabilityTask.PARAM_VAL_RANGE_TYPE_ABSOLUTE%>' <wb:if expression='#rangeType#' compareToExpression='<%=WbiagAvailabilityTask.PARAM_VAL_RANGE_TYPE_ABSOLUTE%>'>checked</wb:if>>
             </wba:td><wba:td></wba:td></wba:tr>
             <wba:tr><wba:td>
              Start: <wb:controlField cssClass='inputField' submitName="absStartDate" ui='DatePickerUI' uiParameter=''><%=absStartDate%></wb:controlField>
             </wba:td><wba:td>
              End:   <wb:controlField cssClass='inputField' submitName="absEndDate" ui='DatePickerUI' uiParameter=''><%=absEndDate%></wb:controlField>
             </wba:td></wba:tr>
            </wba:table>
		  </wba:td>
	    </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_RECREATES_RECORDS">PARAM_RECREATES_RECORDS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="recreatesRecords" ui='CheckboxUI' uiParameter=''><%=recreatesRecords%></wb:controlField>
          </wba:td>
        </wba:tr>

      </wba:table>

      <div class="separatorLarge"></div>

      <wba:button label="Submit" labelLocalizeIndex="Submit" onClick="checkSubmit();"/>&nbsp;
      <wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="#onClickCancel#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%
            recparams.put( WbiagAvailabilityTask.PARAM_WBTEAMS , wbTeams.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_SUBTEAMS , subTeams.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_CALCGRPS , calcGrps.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_PAYGRPS , payGrps.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_EMPLOYEES , employees.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_CLIENT_ID , clientId.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_RANGE_TYPE , rangeType.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_ABS_START_DATE , absStartDate.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_ABS_END_DATE , absEndDate.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_DAYS_BEFORE , daysBefore.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_DAYS_AFTER , daysAfter.toString() );
            recparams.put( WbiagAvailabilityTask.PARAM_RECREATES_RECORDS , recreatesRecords.toString() );
            scheduler.setTaskParams( Integer.parseInt( TASK_ID.toString() ), recparams );

        %>
        <Span>Availability Task Successfully Scheduled</Span>
        <br>
        <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
    </wb:case>
    </wb:switch>
</wb:if>
</wb:page>
