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
    private static final String WBTEAM_PARAM =
      "sourceType=SQL source='SELECT wbt_id, wbt_name FROM workbrain_team' multiChoice=true all=true";
    private static final String CALCGRP_PARAM =
      "sourceType=SQL source=\"SELECT calcgrp_id, calcgrp_name FROM calc_group WHERE calcgrp_name <> 'ALL CALC GROUPS'\" multiChoice=true all=true";
    private static final String PAYGRP_PARAM =
      "sourceType=SQL source='SELECT paygrp_id, paygrp_name FROM pay_group' multiChoice=true all=true";
    private static final String EMP_PARAM =
      "sourceType=SQL source='SELECT emp_id, emp_name FROM employee' multiChoice=true all=true";

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
<wb:define id="batchSize"><wb:get id="batchSize" scope="parameter" default=""/></wb:define>
<wb:define id="threadCount"><wb:get id="threadCount" scope="parameter" default=""/></wb:define>
<wb:define id="autoRecalc"><wb:get id="autoRecalc" scope="parameter" default=""/></wb:define>
<wb:define id="futureBalanceRecalc"><wb:get id="futureBalanceRecalc" scope="parameter" default=""/></wb:define>


<%
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new javax.servlet.jsp.JspException("No task id has been passed to the page");

  Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    if(recparams.isEmpty()) {
        recparams.put( FullRecalculateTask.PARAM_APPLY_TO_TEAMS, "" );
        recparams.put( FullRecalculateTask.PARAM_APPLY_TO_SUBTEAMS, "N" );
        recparams.put( FullRecalculateTask.PARAM_APPLY_TO_CALCGRPS, "" );
        recparams.put( FullRecalculateTask.PARAM_APPLY_TO_PAYGRPS, "" );
        recparams.put( FullRecalculateTask.PARAM_APPLY_TO_EMPS, "" );
        recparams.put( FullRecalculateTask.PARAM_CLIENT_ID, "" );
        recparams.put( FullRecalculateTask.PARAM_TASK_TYPE, FullRecalculateTask.PARAM_TASK_TYPE_RELATIVE );
        recparams.put( FullRecalculateTask.PARAM_ABS_START_DATE, "");
        recparams.put( FullRecalculateTask.PARAM_ABS_END_DATE, "");
        recparams.put( FullRecalculateTask.PARAM_DAYS_BEFORE, "");
        recparams.put( FullRecalculateTask.PARAM_DAYS_AFTER, "");
        recparams.put( FullRecalculateTask.PARAM_CALCULATION_THREAD_COUNT, "1" );
        recparams.put( FullRecalculateTask.PARAM_BATCH_SIZE, String.valueOf(com.wbiag.app.ta.ruleengine.RecalculateRecords.CALC_BATCH_SIZE_MAX) );
        recparams.put( RecalculateTask.PARAM_AUTO_RECALC, "True" );
        recparams.put( RecalculateTask.PARAM_FUTURE_BALANCE_RECALC, "" );
    }
    %>
    <wb:switch>
    <wb:case expression="#TYPE#" compareToExpression="">
      <input type="hidden" name="TYPE" value="scheduleTask">
      <wb:set id="wbTeams"><%=recparams.get( FullRecalculateTask.PARAM_APPLY_TO_TEAMS )%></wb:set>
      <wb:set id="subTeams"><%=recparams.get( FullRecalculateTask.PARAM_APPLY_TO_SUBTEAMS )%></wb:set>
      <wb:set id="calcGrps"><%=recparams.get( FullRecalculateTask.PARAM_APPLY_TO_CALCGRPS )%></wb:set>
      <wb:set id="payGrps"><%=recparams.get( FullRecalculateTask.PARAM_APPLY_TO_PAYGRPS )%></wb:set>
      <wb:set id="employees"><%=recparams.get( FullRecalculateTask.PARAM_APPLY_TO_EMPS )%></wb:set>
      <wb:set id="clientId"><%=recparams.get( FullRecalculateTask.PARAM_CLIENT_ID )%></wb:set>
      <wb:set id="rangeType"><%=recparams.get( FullRecalculateTask.PARAM_TASK_TYPE )%></wb:set>
      <wb:set id="absStartDate"><%=recparams.get( FullRecalculateTask.PARAM_ABS_START_DATE )%></wb:set>
      <wb:set id="absEndDate"><%=recparams.get( FullRecalculateTask.PARAM_ABS_END_DATE )%></wb:set>
      <wb:set id="daysBefore"><%=recparams.get( FullRecalculateTask.PARAM_DAYS_BEFORE )%></wb:set>
      <wb:set id="daysAfter"><%=recparams.get( FullRecalculateTask.PARAM_DAYS_AFTER )%></wb:set>
      <wb:set id="batchSize"><%=recparams.get( FullRecalculateTask.PARAM_BATCH_SIZE )%></wb:set>
      <wb:set id="threadCount"><%=recparams.get( FullRecalculateTask.PARAM_CALCULATION_THREAD_COUNT )%></wb:set>
      <wb:set id="autoRecalc"><%=recparams.get( RecalculateTask.PARAM_AUTO_RECALC )%></wb:set>
      <wb:set id="futureBalanceRecalc"><%=recparams.get( RecalculateTask.PARAM_FUTURE_BALANCE_RECALC )%></wb:set>

      <wba:table caption="Full Recalculate Task" captionLocalizeIndex="Full_Recalculate_Task" width="600">

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_APPLY_TO_TEAMS">PARAM_APPLY_TO_TEAMS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="wbTeams" ui='DBLookupUI' uiParameter='<%=WBTEAM_PARAM%>'><%=wbTeams%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_APPLY_TO_SUBTEAMS">PARAM_APPLY_TO_SUBTEAMS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="subTeams" ui='CheckboxUI' uiParameter=''><%=subTeams%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_APPLY_TO_CALCGRPS">PARAM_APPLY_TO_CALCGRPS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="calcGrps" ui='DBLookupUI' uiParameter='<%=CALCGRP_PARAM%>'><%=calcGrps%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_APPLY_TO_PAYGRPS">PARAM_APPLY_TO_PAYGRPS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="payGrps" ui='DBLookupUI' uiParameter='<%=PAYGRP_PARAM%>'><%=payGrps%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_APPLY_TO_EMPLOYEES">PARAM_APPLY_TO_EMPLOYEES</wb:localize>
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
		  <wba:th>
			<wb:localize id="DATES">Dates to run full recalculate task for:</wb:localize>
		  </wba:th>
	    </wba:tr>
	    <wba:tr>
		  <wba:td>
			<wb:localize id="RELATIVE">Relative (x days before or after today)</wb:localize>
		  </wba:td>
		  <wba:td>
            <wba:table>
	         <wba:tr><wba:td>
			  <input type='radio' name='rangeType' value='<%=FullRecalculateTask.PARAM_TASK_TYPE_RELATIVE%>' <wb:if expression='#rangeType#' compareToExpression='<%=FullRecalculateTask.PARAM_TASK_TYPE_RELATIVE%>'>checked</wb:if>>
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
			  <input type='radio' name='rangeType' value='<%=FullRecalculateTask.PARAM_TASK_TYPE_ABSOLUTE%>' <wb:if expression='#rangeType#' compareToExpression='<%=FullRecalculateTask.PARAM_TASK_TYPE_ABSOLUTE%>'>checked</wb:if>>
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
            <wb:localize id="PARAM_BATCH_SIZE">PARAM_BATCH_SIZE</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="batchSize" ui='NumberUI' uiParameter=''><%=batchSize%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_CALCULATION_THREAD_COUNT">PARAM_CALCULATION_THREAD_COUNT</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="threadCount" ui='NumberUI' uiParameter=''><%=threadCount%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_AUTO_RECALC">PARAM_AUTO_RECALC</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="autoRecalc" ui='ComboBoxUI' uiParameter="valueList=',FALSE,TRUE' labelList='Default,False,True'"><%=autoRecalc%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_FUTURE_BALANCE_RECALC">PARAM_FUTURE_BALANCE_RECALC</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="futureBalanceRecalc" ui='ComboBoxUI' uiParameter="valueList=',FALSE,TRUE' labelList='Default,False,True'"><%=futureBalanceRecalc%></wb:controlField>
          </wba:td>
        </wba:tr>
      </wba:table>

      <div class="separatorLarge"></div>

      <wba:button label="Submit" labelLocalizeIndex="Submit" onClick="checkSubmit();"/>&nbsp;
      <wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="#onClickCancel#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%
            recparams.put( FullRecalculateTask.PARAM_APPLY_TO_TEAMS , wbTeams.toString() );
            recparams.put( FullRecalculateTask.PARAM_APPLY_TO_SUBTEAMS , subTeams.toString() );
            recparams.put( FullRecalculateTask.PARAM_APPLY_TO_CALCGRPS , calcGrps.toString() );
            recparams.put( FullRecalculateTask.PARAM_APPLY_TO_PAYGRPS , payGrps.toString() );
            recparams.put( FullRecalculateTask.PARAM_APPLY_TO_EMPS , employees.toString() );
            recparams.put( FullRecalculateTask.PARAM_CLIENT_ID , clientId.toString() );
            recparams.put( FullRecalculateTask.PARAM_TASK_TYPE , rangeType.toString() );
            recparams.put( FullRecalculateTask.PARAM_ABS_START_DATE , absStartDate.toString() );
            recparams.put( FullRecalculateTask.PARAM_ABS_END_DATE , absEndDate.toString() );
            recparams.put( FullRecalculateTask.PARAM_DAYS_BEFORE , daysBefore.toString() );
            recparams.put( FullRecalculateTask.PARAM_DAYS_AFTER , daysAfter.toString() );
            recparams.put( FullRecalculateTask.PARAM_BATCH_SIZE , batchSize.toString() );
            recparams.put( FullRecalculateTask.PARAM_CALCULATION_THREAD_COUNT , threadCount.toString() );
            recparams.put( RecalculateTask.PARAM_AUTO_RECALC , autoRecalc.toString() );
            recparams.put( RecalculateTask.PARAM_FUTURE_BALANCE_RECALC , futureBalanceRecalc.toString() );

            scheduler.setTaskParams( Integer.parseInt( TASK_ID.toString() ), recparams );

        %>
        <Span>Full Recalculate Task Successfully Scheduled</Span>
        <br>
        <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
    </wb:case>
    </wb:switch>
</wb:if>
</wb:page>
