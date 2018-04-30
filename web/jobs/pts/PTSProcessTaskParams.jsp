<%@ include file="/system/wbheader.jsp"%>
<%@ taglib uri="/wbsys" prefix="wb" %>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="javax.naming.*"%>
<%@ page import="javax.servlet.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.export.process.*"%>
<%@ page import="com.wbiag.app.ta.ruleengine.*"%>
<%@ page import="com.wbiag.app.scheduler.tasks.PTSProcessTask"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="org.apache.log4j.*"%>

<%-- ********************** --%>
<wb:page login="true">
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>

<wb:define id="skdGrpId"><wb:get id="skdGrpId" scope="parameter" default="1"/></wb:define>
<wb:define id="offset"><wb:get id="offset" scope="parameter" default=""/></wb:define>
<wb:define id="dailyOffset"><wb:get id="dailyOffset" scope="parameter" default=""/></wb:define>
<wb:define id="dayOrWeek"><wb:get id="dayOrWeek" scope="parameter" default=""/></wb:define>
<wb:define id="actualCost"><wb:get id="actualCost" scope="parameter" default=""/></wb:define>
<wb:define id="budgetCost"><wb:get id="budgetCost" scope="parameter" default=""/></wb:define>
<wb:define id="actualEarnings"><wb:get id="actualEarnings" scope="parameter" default=""/></wb:define>
<wb:define id="budgetEarnings"><wb:get id="budgetEarnings" scope="parameter" default=""/></wb:define>
<wb:define id="timeCodes"><wb:get id="timeCodes" scope="parameter" default=""/></wb:define>
<wb:define id="hourTypes"><wb:get id="hourTypes" scope="parameter" default=""/></wb:define>
<wb:define id="timeCodesInclusive"><wb:get id="timeCodesInclusive" scope="parameter" default=""/></wb:define>
<wb:define id="hourTypesInclusive"><wb:get id="hourTypesInclusive" scope="parameter" default=""/></wb:define>

<%
  DBConnection conn = null;
  PreparedStatement ps = null;
  ResultSet rs = null;
  String mfrmIdStr = null;
 
  try
  {
  	conn = JSPHelper.getConnection( request );
  	ps = conn.prepareStatement("select mfrm_id from maintenance_form where mfrm_name = ?");
  	ps.setString(1, "PTS REPORT - FORECAST");
  	rs = ps.executeQuery();
  	if(rs.next())
  	{
  		mfrmIdStr = String.valueOf(rs.getInt("mfrm_id"));
  	}
  }
  finally
  {
	SQLHelper.cleanUp(ps, rs);
  }

  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    if(recparams.isEmpty()) {
        recparams.put( PTSProcessTask.PARAM_SKDGRP_ID, "1" );
        recparams.put( PTSProcessTask.PARAM_OFFSET, "0" );
        recparams.put( PTSProcessTask.PARAM_DAILY_OFFSET, "0" );
        recparams.put( PTSProcessTask.PARAM_DAY_OR_WEEK, "1" );
        recparams.put( PTSProcessTask.PARAM_ACTUAL_COST, "N" );
        recparams.put( PTSProcessTask.PARAM_BUDGET_COST, "N" );
        recparams.put( PTSProcessTask.PARAM_ACTUAL_EARNINGS, "N" );
        recparams.put( PTSProcessTask.PARAM_BUDGET_EARNINGS, "N" );
        recparams.put( PTSProcessTask.PARAM_TIMECODES, "WRK" );
        recparams.put( PTSProcessTask.PARAM_HOURTYPES, "REG" );
        recparams.put( PTSProcessTask.PARAM_TIMECODESINCLUSIVE, "N" );
        recparams.put( PTSProcessTask.PARAM_HOURTYPESINCLUSIVE, "N" );
    }
    %>
    <wb:switch>
    <wb:case expression="#TYPE#" compareToExpression="">
      <input type="hidden" name="TYPE" value="scheduleTask">

      <wb:set id="skdGrpId"><%=recparams.get( PTSProcessTask.PARAM_SKDGRP_ID )%></wb:set>
      <wb:set id="offset"><%=recparams.get( PTSProcessTask.PARAM_OFFSET )%></wb:set>
      <wb:set id="dailyOffset"><%=recparams.get( PTSProcessTask.PARAM_DAILY_OFFSET )%></wb:set>
      <wb:set id="dayOrWeek"><%=recparams.get( PTSProcessTask.PARAM_DAY_OR_WEEK )%></wb:set>
      <wb:set id="actualCost"><%=recparams.get( PTSProcessTask.PARAM_ACTUAL_COST )%></wb:set>
      <wb:set id="budgetCost"><%=recparams.get( PTSProcessTask.PARAM_BUDGET_COST )%></wb:set>
      <wb:set id="actualEarnings"><%=recparams.get( PTSProcessTask.PARAM_ACTUAL_EARNINGS )%></wb:set>
      <wb:set id="budgetEarnings"><%=recparams.get( PTSProcessTask.PARAM_BUDGET_EARNINGS )%></wb:set>
      <wb:set id="timeCodes"><%=recparams.get( PTSProcessTask.PARAM_TIMECODES )%></wb:set>
      <wb:set id="hourTypes"><%=recparams.get( PTSProcessTask.PARAM_HOURTYPES )%></wb:set>
      <wb:set id="timeCodesInclusive"><%=recparams.get( PTSProcessTask.PARAM_TIMECODESINCLUSIVE )%></wb:set>
      <wb:set id="hourTypesInclusive"><%=recparams.get( PTSProcessTask.PARAM_HOURTYPESINCLUSIVE )%></wb:set>

      <wba:table caption="PTS Process Task" captionLocalizeIndex="PTS_Process_Task" width="600">
        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_Schedule_Group" overrideId="<%=mfrmIdStr%>">Schedule Group</wb:localize>
          </wba:th>
          <wba:td>
          	<wb:controlField cssClass='inputField' id='PTS_SKDGRP_ID' submitName="skdGrpId" ui='LocationTreeUI' uiParameter="width=50 explicit=10"><%=skdGrpId%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_Offset" overrideId="<%=mfrmIdStr%>">Offset</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="offset" ui='NumberUI' uiParameter='width=5'><%=offset%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_Daily_Offset" overrideId="<%=mfrmIdStr%>">Daily Offset</wb:localize>
          </wba:th>
          <wba:td>
          	<wb:controlField cssClass='inputField' submitName="dailyOffset" ui='NumberUI' uiParameter='width=5'><%=dailyOffset%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_Day_Or_Week" overrideId="<%=mfrmIdStr%>">Day Or Week</wb:localize>
          </wba:th>
          <wba:td>
          	<wb:controlField cssClass='inputField' submitName="dayOrWeek" ui='ComboboxUI' uiParameter='valueList=1,2 labelList=DAY,WEEK'><%=dayOrWeek%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_Actual_Cost" overrideId="<%=mfrmIdStr%>">Actual Cost</wb:localize>
          </wba:th>
          <wba:td>
          	<wb:controlField cssClass='inputField' submitName="actualCost" ui='CheckboxUI' uiParameter=''><%=actualCost%></wb:controlField>
          </wba:td>
        </wba:tr>


        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_Actual_Earnings" overrideId="<%=mfrmIdStr%>">Actual Earnings</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="actualEarnings" ui='CheckboxUI' uiParameter=''><%=actualEarnings%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_Actual_Budget_Cost" overrideId="<%=mfrmIdStr%>">Actual and Budget Cost</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="budgetCost" ui='CheckboxUI' uiParameter=''><%=budgetCost%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_Actual_Budget_Earnings" overrideId="<%=mfrmIdStr%>">Actual and Budget Earnings</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="budgetEarnings" ui='CheckboxUI' uiParameter=''><%=budgetEarnings%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_TimeCodes" overrideId="<%=mfrmIdStr%>">Time Codes</wb:localize>
          </wba:th>
          <wba:td>
          	<wb:controlField cssClass='inputField' submitName="timeCodes" ui='StringUI' uiParameter='width=25'><%=timeCodes%></wb:controlField>
          </wba:td>
        </wba:tr>

		<wba:tr>
          <wba:th>
            <wb:localize id="PTS_TimeCodes_Inclusive" overrideId="<%=mfrmIdStr%>">Inclusive</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="timeCodesInclusive" ui='CheckboxUI' uiParameter=''><%=timeCodesInclusive%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PTS_HourTypes" overrideId="<%=mfrmIdStr%>">Hour Types</wb:localize>
          </wba:th>
          <wba:td>
          	<wb:controlField cssClass='inputField' submitName="hourTypes" ui='StringUI' uiParameter='width=25'><%=hourTypes%></wb:controlField>
          </wba:td>
        </wba:tr>

		<wba:tr>
          <wba:th>
            <wb:localize id="PTS_HourTypes_Inclusive" overrideId="<%=mfrmIdStr%>">Inclusive</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="hourTypesInclusive" ui='CheckboxUI' uiParameter=''><%=hourTypesInclusive%></wb:controlField>
          </wba:td>
        </wba:tr>

      </wba:table>

      <div class="separatorLarge"></div>

      <wba:button type="submit" label="Submit" labelLocalizeIndex="Submit" onClick=""/>&nbsp;
      <wba:button label="cancel" labelLocalizeIndex="Cancel" onClick="#okOnClickUrl#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%
            recparams.put( PTSProcessTask.PARAM_SKDGRP_ID , skdGrpId.toString() );
            recparams.put( PTSProcessTask.PARAM_OFFSET , offset.toString() );
            recparams.put( PTSProcessTask.PARAM_DAILY_OFFSET , dailyOffset.toString() );
            recparams.put( PTSProcessTask.PARAM_DAY_OR_WEEK , dayOrWeek.toString() );
            recparams.put( PTSProcessTask.PARAM_ACTUAL_COST , actualCost.toString() );
            recparams.put( PTSProcessTask.PARAM_BUDGET_COST , budgetCost.toString() );
            recparams.put( PTSProcessTask.PARAM_ACTUAL_EARNINGS , actualEarnings.toString() );
            recparams.put( PTSProcessTask.PARAM_BUDGET_EARNINGS , budgetEarnings.toString() );
            recparams.put( PTSProcessTask.PARAM_TIMECODES , timeCodes.toString() );
            recparams.put( PTSProcessTask.PARAM_HOURTYPES , hourTypes.toString() );
            recparams.put( PTSProcessTask.PARAM_TIMECODESINCLUSIVE , timeCodesInclusive.toString() );
            recparams.put( PTSProcessTask.PARAM_HOURTYPESINCLUSIVE , hourTypesInclusive.toString() );
            scheduler.setTaskParams( Integer.parseInt( TASK_ID.toString() ), recparams );
        %>
        <Span>PTS Process Task Successfully Scheduled</Span>
        <br>
        <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
    </wb:case>
    </wb:switch>
</wb:if>
</wb:page>