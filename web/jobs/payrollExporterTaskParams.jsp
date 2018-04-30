<%@ include file="/system/wbheader.jsp"%>
<%@ page import="com.workbrain.app.scheduler.*" %>
<%@ page import="com.workbrain.server.jsp.*" %>
<%@ page import="com.workbrain.sql.*" %>
<%@ page import="com.workbrain.util.*" %>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>

<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.sql.*"%>
<%@ page import="org.apache.log4j.*"%>

<%@ page import="com.wbiag.app.export.payroll.*"%>
<%!
    private static final Logger logger = Logger.getLogger("jsp.payrollExport.payroll.export");

    private static final String CLIENT_UI_PARAM =
      "sourceType=SQL source='SELECT client_id, client_name FROM workbrain_client' multiChoice=false";
%>


<wb:page login="true" showUIPath="true">
    <INPUT TYPE=HIDDEN NAME='COMMAND' VALUE='CANCEL'>
    <wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
    <wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
    <wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define>
    <wb:define id="Error_1"><wb:localize id="Provide_Export_Type">Provide Export Type</wb:localize></wb:define>
    <wb:define id="Error_2"><wb:localize id="Provide_Paygroup">Provide Paygroup</wb:localize></wb:define>
    <wb:define id="Error_3"><wb:localize id="Provide_Interface_Type">Provide Interface Type</wb:localize></wb:define>
    <wb:define id="Error_4"><wb:localize id="Provide_Cycle_Type">Provide Cycle Type</wb:localize></wb:define>
	<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>


    <%-- LOGIC ------------------------------------------------------ --%>
    <%
        DateFormat df = new SimpleDateFormat(PayrollExporterTask.UDF_DATE_FORMAT);
        boolean isPostBack          = request.getParameter("COMMAND")!=null;
        String cycle                = request.getParameter("CYCLE");
        String taskId               = request.getParameter("TASK_ID");
        String payGroupIdString     = request.getParameter("PAYGRP_ID");
        String startDateString      = request.getParameter("START_DATE");
        String endDateString        = request.getParameter("END_DATE");
        String error                = null;
        String petIdString          = request.getParameter("PET_ID");
        String lookBackDays         = request.getParameter("LOOK_BACK_DAYS");
        String termAddDays          = request.getParameter("TERM_ADD_DAYS");
        String allReadyWhere		= request.getParameter("ALL_READY_WHERE");
        boolean allReadyPaygroups   = request.getParameter("ALL_READY_PAYGRPS")!=null;
        boolean resetReadiness      = request.getParameter("RESET_READINESS")!=null;
        boolean usePayPeriod        = request.getParameter("USE_PAY_PERIOD")!=null;
        boolean writeToFile         = request.getParameter("WRITE_TO_FILE")!=null;
        boolean mergeFiles          = request.getParameter("MERGE_FILES")!=null;
        boolean writeToStagingTable = request.getParameter("WRITE_TO_STAGING_TABLE")!=null;
        boolean adjustDates         = request.getParameter("ADJUST_DATES")!=null;
        boolean chkUnauth           = request.getParameter("CHK_UNAUTH")!=null;
        boolean contUnauth          = request.getParameter("CONTINUE_ON_UNAUTH")!=null;
        String clientId             = request.getParameter("CLIENT_ID");

        if(!isPostBack){
            Scheduler scheduler = SchedulerObjectFactory.getScheduler();
            Map params = scheduler.getTaskParams(Integer.parseInt(taskId));

            try{
                petIdString         = (String) params.get(PayrollExporterTask.PARAM_PET_ID);
                payGroupIdString    = (String) params.get(PayrollExporterTask.PARAM_PAYGRP_ID);
                cycle               = (String) params.get(PayrollExporterTask.PARAM_CYCLE);
                startDateString     = (String) params.get(PayrollExporterTask.PARAM_START_DATE);
                endDateString       = (String) params.get(PayrollExporterTask.PARAM_END_DATE);
                clientId            = (String) params.get(PayrollExporterTask.PARAM_CLIENT_ID);
                lookBackDays        = (String) params.get(PayrollExporterTask.PARAM_LOOK_BACK_DAYS);
                termAddDays         = (String) params.get(PayrollExporterTask.PARAM_TERM_ADD_DAYS);
				allReadyWhere		= (String) params.get(PayrollExporterTask.PARAM_ALL_READY_WHERE);
				
                allReadyPaygroups   = Boolean.valueOf((String) params.get(
                        PayrollExporterTask.PARAM_ALL_READY_PAYGRPS)).booleanValue();
                resetReadiness      = Boolean.valueOf((String) params.get(
                        PayrollExporterTask.PARAM_RESET_READINESS)).booleanValue();
                usePayPeriod        = Boolean.valueOf((String) params.get(
                        PayrollExporterTask.PARAM_USE_PAY_PERIOD)).booleanValue();
                writeToStagingTable = Boolean.valueOf((String) params.get(
                        PayrollExporterTask.PARAM_WRITE_TO_TABLE)).booleanValue();
                writeToFile         = Boolean.valueOf((String) params.get(
                        PayrollExporterTask.PARAM_WRITE_TO_FILE)).booleanValue();
                mergeFiles          = Boolean.valueOf((String) params.get(
                        PayrollExporterTask.PARAM_MERGE_FILES)).booleanValue();
                adjustDates         = Boolean.valueOf((String) params.get(
                        PayrollExporterTask.PARAM_ADJUST_DATES)).booleanValue();
                chkUnauth         = Boolean.valueOf((String) params.get(
                        PayrollExporterTask.PARAM_CHECK_UNAUTH)).booleanValue();
                contUnauth         = Boolean.valueOf((String) params.get(
                        PayrollExporterTask.PARAM_CONTINUE_ON_UNAUTH)).booleanValue();
            } catch(Exception e){
                petIdString=payGroupIdString=startDateString=endDateString= "";
                writeToStagingTable = writeToFile = adjustDates = false;
            }
        } else {
            if("SUBMIT".equals(request.getParameter("COMMAND"))){
                Scheduler scheduler = SchedulerObjectFactory.getScheduler();

                try{
                    if(StringHelper.isEmpty(petIdString))      throw new Exception(Error_1.toString());
                    if(StringHelper.isEmpty(cycle)) throw new Exception(Error_4.toString());

                    Map params = new HashMap();
                    params.put(PayrollExporterTask.PARAM_PET_ID, petIdString);
                    params.put(PayrollExporterTask.PARAM_PAYGRP_ID, payGroupIdString);
                    params.put(PayrollExporterTask.PARAM_CYCLE, cycle);
                    params.put(PayrollExporterTask.PARAM_LOOK_BACK_DAYS, lookBackDays);
                    params.put(PayrollExporterTask.PARAM_TERM_ADD_DAYS, termAddDays);
                    params.put(PayrollExporterTask.PARAM_CLIENT_ID, clientId);
					params.put(PayrollExporterTask.PARAM_ALL_READY_WHERE, allReadyWhere);
					
                    if(!StringHelper.isEmpty(startDateString)){
                        df.parse(startDateString);
                        params.put(PayrollExporterTask.PARAM_START_DATE, startDateString);
                    }

                    if(!StringHelper.isEmpty(endDateString)){
                        df.parse(endDateString);
                        params.put(PayrollExporterTask.PARAM_END_DATE, endDateString);
                    }

                    params.put(PayrollExporterTask.PARAM_ALL_READY_PAYGRPS, allReadyPaygroups ? "true" : "false");
                    params.put(PayrollExporterTask.PARAM_RESET_READINESS, resetReadiness ? "true" : "false");
                    params.put(PayrollExporterTask.PARAM_USE_PAY_PERIOD, usePayPeriod ? "true" : "false");
                    params.put(PayrollExporterTask.PARAM_WRITE_TO_TABLE, writeToStagingTable ? "true" : "false");
                    params.put(PayrollExporterTask.PARAM_WRITE_TO_FILE, writeToFile ? "true" : "false");
                    params.put(PayrollExporterTask.PARAM_MERGE_FILES, mergeFiles ? "true" : "false");
                    params.put(PayrollExporterTask.PARAM_ADJUST_DATES, adjustDates ? "true" : "false");
                    params.put(PayrollExporterTask.PARAM_CHECK_UNAUTH, chkUnauth ? "true" : "false");
                    params.put(PayrollExporterTask.PARAM_CONTINUE_ON_UNAUTH, contUnauth ? "true" : "false");
                    scheduler.setTaskParams(Integer.parseInt(taskId),params);
                }catch(Exception e){
                    logger.error("payrollExporterTaskParams.jsp",e);
                    error = e.getMessage();
                }
            }
            if(error==null){
                %>
                <Span>Parameters updated successfully</Span>
			    <BR>
			    <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
                <%
            }
        }
	if(!"SUBMIT".equals(request.getParameter("COMMAND"))){
%>

    <INPUT TYPE=HIDDEN NAME='TASK_ID' VALUE='<%=taskId%>'>
    <%-- RENDERING ------------------------------------------------------ --%>

    <wb:define id="uiParms,startDateValue,endDateValue,defaultValue"/>

    <wba:table caption="Payroll Export" >

    <tr>
        <wba:th><wb:localize id="EXPORT_TYPE" >Export Type</wb:localize></wba:th>
        <td>
            <wb:controlField submitName='PET_ID' id='PET_ID'
                cssClass="inputField"
                ui='DBLookupUI'
                uiParameter="multiChoice=false sourceType=SQL source='SELECT pet_id, pet_name, pet_desc FROM payroll_export_tsk' all=false labelFieldStatus=edit"
            ><%=petIdString%></wb:controlField>
        </td>
    </tr>
        <tr>
        <wba:th><wb:localize id="CYCLE" >Cycle Type</wb:localize></wba:th>
        <td>
            <wb:controlField submitName='CYCLE' id='CYCLE'
                cssClass="inputField"
                ui='ComboBoxUI'
               uiParameter="labelList='Regular, New Employee, Terminated Employee'"><%=cycle%></wb:controlField>
        </td>
    </tr>
    <tr>
        <wba:th><wb:localize id="LOOK_BACK_DAYS" >Look Back Days</wb:localize></wba:th>
        <td>
            <wb:controlField submitName='LOOK_BACK_DAYS' id='LOOK_BACK_DAYS'
                cssClass="inputField"
                ui='StringUI'
            ><%=lookBackDays%></wb:controlField>
        </td>
    </tr>
        <tr>
        <wba:th><wb:localize id="TERM_ADD_DAYS" >Termination Add Days</wb:localize></wba:th>
        <td>
            <wb:controlField submitName='TERM_ADD_DAYS' id='TERM_ADD_DAYS'
                cssClass="inputField"
                ui='StringUI'
            ><%=termAddDays%></wb:controlField>
        </td>
    </tr>
    <tr>
        <wba:th><wb:localize id="PAYGRP_ID" type="field" overrideId='205'>Pay Group</wb:localize></wba:th>
        <td>
            <wb:controlField
                submitName='PAYGRP_ID'
                id='PAYGRP_ID'
                overrideId="205"
                cssClass="inputField"
                ui='DBLookupUI'
                uiParameter="multiChoice=true sourceType=SQL source='SELECT PAYGRP_ID, PAYGRP_NAME, PAYGRP_DESC FROM PAY_GROUP' all=false labelFieldStatus=edit"
             ><%=payGroupIdString%></wb:controlField>
        </td>
    </tr>
    <tr>
        <wba:th><wb:localize id="Start_Date" type="field" overrideId="205">Start Date</wb:localize></wba:th>
        <td>
            <wb:controlField id="Start_Date"    overrideId="205" submitName='START_DATE'
            onChange="" cssClass="inputField" ><%=StringHelper.isEmpty(startDateString) ? "" : startDateString%></wb:controlField>
        </td>
    </tr>
    <tr>
        <wba:th><wb:localize id="End_Date" overrideId='205' type='field'>End Date</wb:localize></wba:th>
        <td>
            <wb:controlField id='End_Date' submitName='END_DATE'
            onChange="" cssClass="inputField" overrideId='205'><%=StringHelper.isEmpty(endDateString) ? "" : endDateString%></wb:controlField>
        </td>
    </tr>
        </tr>
        <tr>
        <wba:th><wb:localize id="ALL_READY_WHERE" >All Ready Where</wb:localize></wba:th>
        <td>
            <wb:controlField submitName='ALL_READY_WHERE' id='ALL_READY_WHERE'
                cssClass="inputField"
                ui='StringUI'
            ><%=allReadyWhere%></wb:controlField>
        </td>
    </tr>
            <tr>
    <wba:th>&nbsp;</wba:th>
        <td>
        <wb:secureContent securityName='CHK_UNAUTH'>
        <input type=checkbox name='CHK_UNAUTH'  <%=chkUnauth?"checked":""%> >
            <wb:localize id="chk_unauth" >Check for Unauthorized Records</wb:localize>
        </wb:secureContent>
        </td>
    </tr>
            <tr>
    <wba:th>&nbsp;</wba:th>
        <td>
        <wb:secureContent securityName='CONTINUE_ON_UNAUTH'>
        <input type=checkbox name='CONTINUE_ON_UNAUTH'  <%=contUnauth?"checked":""%> >
            <wb:localize id="vontinue_on_unauth" >Continue on Unauthorized Records</wb:localize>
        </wb:secureContent>
        </td>
    </tr>
        <tr>
    <wba:th>&nbsp;</wba:th>
        <td>
        <wb:secureContent securityName='USE_PAY_PERIOD'>
        <input type=checkbox name='USE_PAY_PERIOD'  <%=usePayPeriod?"checked":""%> >
            <wb:localize id="use_pay_period" >Use Pay Period Dates</wb:localize>
        </wb:secureContent>
        </td>
    </tr>
        <tr>
    <wba:th>&nbsp;</wba:th>
        <td>
        <wb:secureContent securityName='ALL_READY_PAYGRPS'>
        <input type=checkbox name='ALL_READY_PAYGRPS'  <%=allReadyPaygroups?"checked":""%> >
            <wb:localize id="all_ready_paygroups" >All Ready Pay Groups</wb:localize>
        </wb:secureContent>
        </td>
    </tr>
        <tr>
    <wba:th>&nbsp;</wba:th>
        <td>
        <wb:secureContent securityName='RESET_READINESS'>
        <input type=checkbox name='RESET_READINESS'  <%=resetReadiness?"checked":""%> >
            <wb:localize id="reset_readiness" >Reset Readiness</wb:localize>
        </wb:secureContent>
        </td>
    </tr>
    <tr>
    <wba:th>&nbsp;</wba:th>
        <td>
        <wb:secureContent securityName='WRITE_TO_STAGING_TABLE'>
        <input type=checkbox name='WRITE_TO_STAGING_TABLE'  <%=writeToStagingTable?"checked":""%> >
            <wb:localize id="write_to_staging_table" >Write to Staging Table</wb:localize>
        </wb:secureContent>
        </td>
    </tr>
    <tr>
    <wba:th>&nbsp;</wba:th>
    <td>
        <wb:secureContent securityName='WRITE_TO_FILE'>
        <input type=checkbox name='WRITE_TO_FILE'  <%=writeToFile?"checked":""%> >
            <wb:localize id="write_to_file" >Write to File</wb:localize>
        </wb:secureContent>
    </td>
    </tr>
    <tr>
    <wba:th>&nbsp;</wba:th>
    <td>
        <wb:secureContent securityName='MERGE_FILES'>
        <input type=checkbox name='MERGE_FILES'  <%=mergeFiles?"checked":""%> >
            <wb:localize id="merge_files" >Merge Files</wb:localize>
        </wb:secureContent>
    </td>
    </tr>
    <tr>
    <wba:th>&nbsp;</wba:th>
    <td>
        <wb:secureContent securityName='ADJUST_DATES'>
        <input type=checkbox name='ADJUST_DATES' <%=adjustDates?"checked":""%> >
            <wb:localize id="auto_adjust_payroll_dates" >
                 Automatically adjust payroll payroll dates to the next pay period
        </wb:localize>
        </wb:secureContent>
    </td>
    </tr>
        <tr>
        <wba:th><wb:localize id="CLIENT_ID" type="field" overrideId='205'>Client ID</wb:localize></wba:th>
        <td>
            <wb:controlField
                submitName='CLIENT_ID'
                id='CLIENT_ID'
                overrideId="205"
                cssClass="inputField"
                ui='DBLookupUI'
                uiParameter="multiChoice=false sourceType=SQL source='SELECT client_id, client_name FROM workbrain_client' multiChoice=false"
             ><%=clientId%></wb:controlField>
        </td>
    </tr>


  </wba:table>

        <wba:button label="Go" labelLocalizeIndex="Submit"
             onClick="disableAllButtons(); document.forms[0].COMMAND.value='SUBMIT'; document.forms[0].submit(); "
        />

        <wba:button label="Cancel" labelLocalizeIndex="Cancel"
             onClick="disableAllButtons(); document.forms[0].submit(); "
        />


    <BR>
    <BR>
    <div color=red>
       <%=error==null?"":error%>
    </div>
    <%}%>
</wb:page>