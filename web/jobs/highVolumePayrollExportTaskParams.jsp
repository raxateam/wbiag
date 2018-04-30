<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.workbrain.util.*" %>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.log4j.*"%>

<%@ page import="com.wbiag.app.export.payroll.*"%>
<%!
    private static final Logger logger = Logger.getLogger("jsp.highVolumePayrollExport");

    private static final String CLIENT_UI_PARAM =
      "sourceType=SQL source='SELECT client_id, client_name FROM workbrain_client' multiChoice=false";
%>


<wb:page login="true" showUIPath="true">
    <INPUT TYPE=HIDDEN NAME='COMMAND' VALUE='CANCEL'>
    <wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
    <wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
    <wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define>
    <wb:define id="Error_1"><wb:localize id="ProvideMaxInProgress">Provide Max In Progress</wb:localize></wb:define>
    <wb:define id="Error_2"><wb:localize id="ProvideClientID">Provide Client ID</wb:localize></wb:define>


    <%-- LOGIC ------------------------------------------------------ --%>
    <%
        
        boolean isPostBack          = request.getParameter("COMMAND")!=null;
        String maxInProgress        = request.getParameter("MAX_IN_PROGRESS");
        String taskId               = request.getParameter("TASK_ID");
        String clientId             = request.getParameter("CLIENT_ID");
        String error                = null;

        if(!isPostBack){
            Scheduler scheduler = SchedulerObjectFactory.getScheduler();
            Map params = scheduler.getTaskParams(Integer.parseInt(taskId));

            try{
                maxInProgress       = (String) params.get(HighVolumeExportTask.PARAM_MAX_IN_PROGRESS);
                clientId            = (String) params.get(HighVolumeExportTask.PARAM_CLIENT_ID);
            } catch(Exception e){
                maxInProgress = "0";
                clientId = "1";
                if (logger.isEnabledFor(Level.ERROR)){
                	logger.error("Could not get params. ", e);
                }
            }
        } else {
            if("SUBMIT".equals(request.getParameter("COMMAND"))){
                Scheduler scheduler = SchedulerObjectFactory.getScheduler();

                try{
                    if(StringHelper.isEmpty(clientId))      throw new Exception(Error_2.toString());
                    if(StringHelper.isEmpty(maxInProgress)) throw new Exception(Error_1.toString());

                    Map params = new HashMap();
                    params.put(HighVolumeExportTask.PARAM_MAX_IN_PROGRESS, maxInProgress);
                    params.put(HighVolumeExportTask.PARAM_CLIENT_ID, clientId);
                    scheduler.setTaskParams(Integer.parseInt(taskId),params);
                }catch(Exception e){
                    if (logger.isEnabledFor(Level.ERROR)){
                    	logger.error("highVolumePayrollExporterTaskParams.jsp",e);
                    }
                    error = e.getMessage();
                }
            }
            if(error==null){
                %>
                <wb:forward page='/jobs/ent/jobs/schedules.jsp' />
                <%
            }
        }

%>

    <INPUT TYPE=HIDDEN NAME='TASK_ID' VALUE='<%=taskId%>'>
    <%-- RENDERING ------------------------------------------------------ --%>

    <wb:define id="uiParms,startDateValue,endDateValue,defaultValue"/>

    <wba:table caption="High Volume Payroll Export" >

    <tr>
        <wba:th><wb:localize id="MAX_IN_PROGRESS" >Max In Progress</wb:localize></wba:th>
        <td>
            <wb:controlField submitName='MAX_IN_PROGRESS' id='MAX_IN_PROGRESS'
                cssClass="inputField"
                ui='StringUI'
               ><%=maxInProgress%></wb:controlField>
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
</wb:page>