<%@ include file="/system/wbheader.jsp"%> <%@ page import="java.io.*"%> 
<%@ page import="java.sql.*"%> 
<%@ page import="java.util.*"%> 
<%@ page import="javax.naming.*"%> 
<%@ page import="com.workbrain.sql.*"%> 
<%@ page import="com.workbrain.util.*"%> 
<%@ page import="com.workbrain.server.jsp.*"%> 
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%> 
<%@ page import="com.workbrain.security.SecurityService"%> 
<%@ page import="com.wbiag.app.wbinterface.SQLExportTask"%> 
<%@ page import="java.lang.reflect.*"%> <%@ page import="javax.servlet.jsp.JspException" %> <%@ page import="org.apache.log4j.*"%> <%!
    private static final Logger csvExportParamsLogger = Logger.getLogger("jsp.jobs.ent.jobs.csvExportParams");
%> <wb:page login='true' showUIPath='true' uiPathName='CSV Query Export Parameters' uiPathNameId='UIPATHNAME_ID_CSVEXPORT_PARAMS'> <wb:config id='UIPATHNAME_ID_CSVEXPORT_PARAMS'/> 
   <wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define> 
   <wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define> 
   <wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit> 
   <wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/>
   </wb:define> <wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit> 
   <wb:define id="inputQuery"><wb:get id="inputQuery" scope="parameter" default=""/></wb:define> 
   <wb:define id="filename"><wb:get id="filename" scope="parameter" default=""/></wb:define> 
   <wb:define id="tostamp"><wb:get id="tostamp" scope="parameter" default="N"/></wb:define> 
   <wb:define id="performDifference"><wb:get id="performDifference" scope="parameter" default="N"/></wb:define> 
   <wb:define id="enclose"><wb:get id="enclose" scope="parameter" default=""/></wb:define>
   <wb:define id="delimiter"><wb:get id="delimiter" scope="parameter" default=","/></wb:define>
   <wb:define id="clientId"><wb:get id="clientId" scope="parameter" default=""/></wb:define> 
   <wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define> <%
    Scheduler scheduler = SchedulerObjectFactory.getScheduler();
    if (TASK_ID==null) throw new JspException("No task id has been passed to the page");
    int taskId = Integer.parseInt( TASK_ID.toString());    
    DBConnection connection = JSPHelper.getConnection(request);
    Statement s = null;
    ResultSet rs = null;
    StringBuffer clientIds=new StringBuffer();
    StringBuffer clientNames=new StringBuffer();
    int clientCount = 0;

    try {
      s = connection.createStatement();
      rs = s.executeQuery("SELECT client_id, client_name FROM workbrain_client ORDER BY client_name");
      while (rs.next()) {
        if (clientCount > 0) {
          clientIds.append(',');
          clientNames.append(',');
        }
        clientNames.append(rs.getInt(1));
        clientIds.append(rs.getString(2));
        clientCount++;
      }
    } finally {
      if (rs!=null) rs.close();
      if (s!=null) s.close();
    }

    if (clientCount == 0) {
    %> <p> At least one client must exist in the system , please contact your System Administrator. <wb:stop/> <%  } %> <wb:define id="ClientIds"><%=clientIds.toString()%></wb:define> <wb:define id="ClientNames"><%=clientNames.toString()%></wb:define> <wb:if expression="#TASK_ID#" compareToExpression="" operator="<>"> <%
    Map recparams = scheduler.getTaskParams(taskId);

    if(recparams.isEmpty()) {
        recparams.put(SQLExportTask.SQL_PARAM,"");
        recparams.put(SQLExportTask.FILE_NAME_PARAM,"");
        recparams.put(SQLExportTask.TOSTAMP_PARAM,"N");
        recparams.put(SQLExportTask.PERF_DIFF_PARAM,"N");
        recparams.put(SQLExportTask.DELIMITER_PARAM,",");        
        recparams.put(SQLExportTask.ENCLOSE_PARAM,"");        
        recparams.put(SQLExportTask.CLIENT_ID_PARAM, SecurityService.getCurrentClientId());
    }
    %> <wb:switch> <wb:case expression="#TYPE#" compareToExpression=""> <input type="hidden" name="TYPE" value="scheduleTask"> 
       <wb:set id="inputQuery"><%=(String)recparams.get(SQLExportTask.SQL_PARAM)%></wb:set> 
       <wb:set id="filename"><%=(String)recparams.get(SQLExportTask.FILE_NAME_PARAM)%></wb:set> 
       <wb:set id="tostamp"><%=recparams.get(SQLExportTask.TOSTAMP_PARAM)%></wb:set> 
       <wb:set id="performDifference"><%=recparams.get(SQLExportTask.PERF_DIFF_PARAM)%></wb:set> 
       <wb:set id="delimiter"><%=recparams.get(SQLExportTask.DELIMITER_PARAM)%></wb:set>
       <wb:set id="enclose"><%=recparams.get(SQLExportTask.ENCLOSE_PARAM)%></wb:set>
       <wb:set id="clientId"><%=recparams.get(SQLExportTask.CLIENT_ID_PARAM)%></wb:set> 
       <wba:table caption="Export Task Parameters" captionLocalizeIndex="TPS_Export_Task_Parameters"> 
       <tr> <th> <wb:localize id="Sql_Query">SQL Query</wb:localize> </th> <td> <wb:controlField cssClass='inputField' submitName="inputQuery" ui='StringUI' uiParameter='width=60'><%=inputQuery%></wb:controlField> </td> </tr> 
       <tr> <th> <wb:localize id="File_Name">File Name</wb:localize> </th> <td> <wb:controlField cssClass='inputField' submitName="filename" ui='StringUI' uiParameter='width=60'><%=filename%></wb:controlField> </td> </tr> 
       <tr> <th> <wb:localize id="date_and_time_stamp">Add Date and Time to Each File Name</wb:localize> </th> <td> <wb:controlField submitName="tostamp" ui="CheckboxUI" cssClass="inputField" uiParameter=""><wb:get id="tostamp"/></wb:controlField> </td> </tr> 
       <tr> <th> <wb:localize id="CSVEXP_Pereform_Difference">Performs Difference</wb:localize> </th> <td> <wb:controlField submitName="performDifference" ui="CheckboxUI" cssClass="inputField" uiParameter=""><wb:get id="performDifference"/></wb:controlField> </td> </tr>
	   <tr> <th> <wb:localize id="CSVEXP_Delimiter">Delimiter</wb:localize> </th> <td> <wb:controlField cssClass='inputField' submitName="inputQuery" ui='StringUI' uiParameter=''><%=delimiter%></wb:controlField> </td> </tr>        
	   <tr> <th> <wb:localize id="CSVEXP_Enclose">Enclose</wb:localize> </th> <td> <wb:controlField cssClass='inputField' submitName="inputQuery" ui='StringUI' uiParameter=''><%=enclose%></wb:controlField> </td> </tr>	   
       <tr> <th> <wb:localize id="Client_Name">Client Name</wb:localize> </th> <td> <wb:controlField cssClass="inputField" submitName="clientId" nullable="false" ui="ComboBoxUI" uiParameter="labelList='#ClientIds#' valueList='#ClientNames#'"><%=clientId.toString()%></wb:controlField> </td> </tr> </wba:table> 
       <div class="separatorLarge"></div> <wba:button type="submit" label="Submit" labelLocalizeIndex="Submit" onClick="disableAllButtons(); document.page_form.submit();"/>&nbsp; <wba:button label="cancel" labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/> </wb:case> <wb:case expression="#TYPE#" compareToExpression="scheduleTask"> <%--Actually Schedulling the Export --%> <%
        try {
            String param0 = inputQuery.toString();
            String param1 = filename.toString();
            String param2 = tostamp.toString();
            String param3 = performDifference.toString();
            recparams.put(SQLExportTask.SQL_PARAM,param0);
            recparams.put(SQLExportTask.FILE_NAME_PARAM,param1);
            recparams.put(SQLExportTask.TOSTAMP_PARAM,param2);
            recparams.put(SQLExportTask.PERF_DIFF_PARAM,param3);
            recparams.put(SQLExportTask.DELIMITER_PARAM,delimiter.toString());
            recparams.put(SQLExportTask.ENCLOSE_PARAM,enclose.toString());            
            recparams.put(SQLExportTask.CLIENT_ID_PARAM,clientId.toString());
            scheduler.setTaskParams(taskId,recparams);
            %> <wb:localize id="Export_Successfully_Scheduled">Export Successfully Scheduled</wb:localize> <%
        } catch (Exception e) {
            csvExportParamsLogger.error(e);
            %> <wb:localize id="Error_Scheduling_Export">Error_Scheduling_Export</wb:localize> <%
        }
        %> <br> <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/> </wb:case> </wb:switch> </wb:if> </wb:page> 