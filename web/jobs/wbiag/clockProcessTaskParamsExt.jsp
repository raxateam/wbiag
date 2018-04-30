<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%@ page import="com.workbrain.app.clockInterface.processing.WBClockProcessTask"%>
<%@ page import="com.wbiag.app.clockInterface.processing.WBClockProcessTaskExt"%>
<%!

    private static final String RDRGRP_UI_PARAM =
      "sourceType=SQL source='SELECT rdrgrp_name, rdrgrp_desc FROM reader_group' multiChoice=true";

%>
<wb:page login='true'>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<!--<wb:define id="AUTO_RECALC"><wb:get id="AUTO_RECALC" scope="parameter" default=""/></wb:define>-->
<wb:define id="PARAM_RDRGRP_NAMES"><wb:get id="PARAM_RDRGRP_NAMES" scope="parameter" default=""/></wb:define>
<wb:define id="PARAM_RDRGRP_NAMES_INCL"><wb:get id="PARAM_RDRGRP_NAMES_INCL" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>
<%

Scheduler scheduler = SchedulerObjectFactory.getScheduler();
if (TASK_ID==null) throw new JspException("No task id has been passed to the page");
int taskId = Integer.parseInt( TASK_ID.toString());
Map params = scheduler.getTaskParams(taskId);
if (params==null) {
   params = new HashMap();
}
String autoRecalc = null;
//String autoRecalc = params.get(WBClockProcessTask.AUTO_RECALC) != null ? (String) params.get(WBClockProcessTask.AUTO_RECALC) : "";
String rdrgrpNames = params.get(WBClockProcessTaskExt.PARAM_RDRGRP_NAMES) != null ? (String) params.get(WBClockProcessTaskExt.PARAM_RDRGRP_NAMES) : "";
String rdrgrpNamesInc = params.get(WBClockProcessTaskExt.PARAM_RDRGRP_NAMES_INCL) != null ? (String) params.get(WBClockProcessTaskExt.PARAM_RDRGRP_NAMES_INCL) : "";

if ("SUBMIT".equals(OPERATION.toString())) {
    //params.put(WBClockProcessTask.AUTO_RECALC, AUTO_RECALC == null ? "" : AUTO_RECALC.toString());
    params.put(WBClockProcessTaskExt.PARAM_RDRGRP_NAMES, PARAM_RDRGRP_NAMES == null ? "" : PARAM_RDRGRP_NAMES.toString());
    params.put(WBClockProcessTaskExt.PARAM_RDRGRP_NAMES_INCL, PARAM_RDRGRP_NAMES_INCL == null ? "" : PARAM_RDRGRP_NAMES_INCL.toString());
    scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), params);
%>
    <Span><wb:localize id="Task_parameters_updated_successfully">Task parameters updated successfully</wb:localize></Span>
    <BR>
    <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%
} else {
%>
<wba:table caption="Process Clocks Task Settings" captionLocalizeIndex="PCT_Settings" width="400">
    <!--
    <wba:tr>
    <wba:th width='60%'>
        <wb:localize id="AUTO_RECALC">Auto Recalc: </wb:localize>
    </wba:th>
    <wba:td>
        <wb:controlField cssClass="inputField" submitName="AUTO_RECALC"
                       nullable="false"
                       ui="ComboBoxUI"
                       uiParameter="valueList='FALSE,TRUE' lableList='FALSE,TRUE'"><%=autoRecalc%></wb:controlField>
    </wba:td>
    </wba:tr>
    -->
    <wba:tr>

      <wba:th>
        <wb:localize id="PARAM_RDRGRP_NAMES">PARAM_RDRGRP_NAMES</wb:localize>
      </wba:th>
      <wba:td>
        <wb:controlField cssClass='inputField' submitName="PARAM_RDRGRP_NAMES" ui='DBLookupUI' uiParameter='<%=RDRGRP_UI_PARAM%>'><%=rdrgrpNames%></wb:controlField>
      </wba:td>
    </wba:tr>
    <wba:tr>
      <wba:th>
        <wb:localize id="PARAM_RDRGRP_NAMES_INCL">PARAM_RDRGRP_NAMES_INCL</wb:localize>
      </wba:th>
      <wba:td>
        <wb:controlField cssClass='inputField' submitName="PARAM_RDRGRP_NAMES_INCL" ui='CheckboxUI' uiParameter=''><%=rdrgrpNamesInc%></wb:controlField>
      </wba:td>
    </wba:tr>
</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
