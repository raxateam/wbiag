<%@ include file="/system/wbheader.jsp"%>

<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.security.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.app.wbinterface.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.wbiag.app.wbinterface.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>

<wb:page login='true' showUIPath='true' uiPathName='Import Parameters' uiPathNameId='UIPATHNAME_ID_WBIMPORT_PARAMS'>
<wb:config id='UIPATHNAME_ID_WBIMPORT_PARAMS'/>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="TYPE_ID"><wb:get id="TYPE_ID" scope="parameter" default=""/></wb:define>
<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
<wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="FILE_NAME"><wb:get id="FILE_NAME" scope="parameter" default=""/></wb:define>
<wb:define id="PERFORM_DIFFERENCE"><wb:get id="PERFORM_DIFFERENCE" scope="parameter" default="false"/></wb:define>
<wb:define id="TRANSFORMER_CLASS"><wb:get id="TRANSFORMER_CLASS" scope="parameter" default=""/></wb:define>
<wb:define id="CLIENT_ID"><wb:get id="CLIENT_ID" scope="parameter" default=""/></wb:define>
<wb:define id="DELIMITER"><wb:get id="DELIMITER" scope="parameter" default=""/></wb:define>
<wb:define id="SORT_FILES_BY"><wb:get id="SORT_FILES_BY" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%
  Scheduler scheduler = SchedulerObjectFactory.getScheduler();
  if (TASK_ID.toString()==null) throw new JspException("No task id has been passed to the page");
  int taskId = Integer.parseInt( TASK_ID.toString());

  StringBuffer names=new StringBuffer();
  StringBuffer values=new StringBuffer();
  names.append(none.toString());
  values.append("-1");
  DBConnection connection = JSPHelper.getConnection(request);
  Statement s = null;
  ResultSet rs = null;
  try {
    s = connection.createStatement();
    rs = s.executeQuery("SELECT WBITYP_ID,WBITYP_NAME FROM WBINT_TYPE ORDER BY WBITYP_NAME");
    while (rs.next()) {
      if (names.length()>0) {
        names.append(',');
        values.append(',');
      }
      values.append(rs.getInt(1));
      names.append(rs.getString(2));
    }
  } finally {
    if (rs!=null) rs.close();
    if (s!=null) s.close();
  }

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
%>
    <p> At least one client must exist in the system , please contact your System Administrator.
    <wb:stop/>
<% } %>
<wb:define id="TaskNames"><%=names.toString()%></wb:define>
<wb:define id="TaskValues"><%=values.toString()%></wb:define>
<wb:define id="ClientIds"><%=clientIds.toString()%></wb:define>
<wb:define id="ClientNames"><%=clientNames.toString()%></wb:define>
<%

  Map params = scheduler.getTaskParams(taskId);

  Integer typeID = params != null && params.get(AbstractImportTask.TYPE_ID_PARAM_NAME) != null ?
    new Integer(String.valueOf(params.get(AbstractImportTask.TYPE_ID_PARAM_NAME))) : new Integer(-1);

  String transformerClass = params != null && params.get(AbstractImportTask.TRANSFORMER_CLASS_PARAM_NAME) != null ?
    String.valueOf(params.get(AbstractImportTask.TRANSFORMER_CLASS_PARAM_NAME)) : "";
  String fileName = params != null && params.get(AbstractImportTask.FILENAME_PARAM_NAME) != null ?
    String.valueOf(params.get(AbstractImportTask.FILENAME_PARAM_NAME)) : "";
  Boolean performDifference = params != null && params.get(AbstractImportTask.PERFORMANCE_DIFFERENCE_PARAM_NAME) != null ?
    new Boolean(String.valueOf(params.get(AbstractImportTask.PERFORMANCE_DIFFERENCE_PARAM_NAME))) : Boolean.FALSE;
  Integer clientId = params != null && params.containsKey(AbstractImportTask.CLIENT_ID_PARAM_NAME)
                     ? Integer.valueOf(String.valueOf(params.get(AbstractImportTask.CLIENT_ID_PARAM_NAME)))
                     : Integer.valueOf(String.valueOf(SecurityService.getCurrentClientId()));
  String delimiter = params != null && params.get(GenericDelimitedImportTask.PARAM_DELIMITER) != null ?
    String.valueOf(params.get(GenericDelimitedImportTask.PARAM_DELIMITER)) : "";
  String sortFilesBy = params != null && params.get(GenericDelimitedImportTask.PARAM_SORT_FILES_BY) != null ?
            String.valueOf(params.get(GenericDelimitedImportTask.PARAM_SORT_FILES_BY)) : "";

if ("SUBMIT".equals(OPERATION.toString())) {
  Map newParams = new HashMap();
  newParams.put(AbstractImportTask.TYPE_ID_PARAM_NAME,new Integer(TYPE_ID.toString()));
  newParams.put(AbstractImportTask.TRANSFORMER_CLASS_PARAM_NAME,TRANSFORMER_CLASS == null ? "" : TRANSFORMER_CLASS.toString());
  newParams.put(AbstractImportTask.FILENAME_PARAM_NAME,FILE_NAME == null ? "" : FILE_NAME.toString());
  newParams.put(AbstractImportTask.CLIENT_ID_PARAM_NAME, CLIENT_ID == null ? SecurityService.getCurrentClientId() : CLIENT_ID.toString());
  newParams.put(AbstractImportTask.PERFORMANCE_DIFFERENCE_PARAM_NAME,"Y".equals(PERFORM_DIFFERENCE.toString()) ? Boolean.TRUE : Boolean.FALSE);
  newParams.put(GenericDelimitedImportTask.PARAM_DELIMITER ,DELIMITER == null ? "" : DELIMITER.toString());
  newParams.put(GenericDelimitedImportTask.PARAM_SORT_FILES_BY ,SORT_FILES_BY == null ? "" : SORT_FILES_BY.toString());  
  scheduler.setTaskParams(taskId,newParams);
  %>
  <Span><wb:localize id="Type_updated_successfully">Type updated successfully</wb:localize></Span>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  <%
} else {
%>
<wba:table caption="Generic Delimited Import Task" captionLocalizeIndex="Generic Delimited  Import_Task" width="200">
  <tr>
    <th nowrap width='20%'>
      <wb:localize id="Interface_Type">Interface Type</wb:localize>
    </th>
    <td width='80%'>
      <wb:controlField cssClass="inputField" submitName="TYPE_ID"
                       nullable="false"
                       ui="ComboBoxUI"
                       uiParameter="labelList='#TaskNames#' valueList='#TaskValues#'"><%=typeID.toString()%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th nowrap width='20%'>
      <wb:localize id="Transformer_Class_Name">Transformer Class Name</wb:localize>
    </th>
    <td width='80%'>
          <wb:controlField cssClass="inputField" submitName="TRANSFORMER_CLASS" ui="StringUI" uiParameter="width=60"><%=transformerClass%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th nowrap width='20%'>
      <wb:localize id="File_Name">File Name</wb:localize>
    </th>
    <td width='80%'>
          <wb:controlField cssClass="inputField" submitName="FILE_NAME" ui="StringUI" uiParameter="width=60"><%=fileName%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th nowrap width='20%'>
      <wb:localize id="IPPerform_Difference">Performs Difference</wb:localize>
    </th>
    <td width='80%'>
          <wb:controlField cssClass="inputField" submitName="PERFORM_DIFFERENCE" ui="CheckboxUI" uiParameter=""><%=performDifference%></wb:controlField>
    </td>
  </tr>
  <tr>
  <th nowrap width='20%'>
    <wb:localize id="Client_Name">Client Name</wb:localize>
  </th>
  <td width='80%'>
      <wb:controlField cssClass="inputField" submitName="CLIENT_ID"
                       nullable="false"
                       ui="ComboBoxUI"
                       uiParameter="labelList='#ClientIds#' valueList='#ClientNames#'"><%=clientId.toString()%></wb:controlField>

  </td>
  <tr>
    <th nowrap width='20%'>
      <wb:localize id="Delimiter">Delimiter</wb:localize>
    </th>
    <td width='80%'>
          <wb:controlField cssClass="inputField" submitName="DELIMITER" ui="StringUI" uiParameter=""><%=delimiter%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th nowrap width='20%'>
      <wb:localize id="SortFilesBy">Sort Files By</wb:localize>
    </th>
    <td width='80%'>
          <wb:controlField cssClass="inputField" submitName="SORT_FILES_BY" ui="ComboBoxUI" 
          uiParameter="labelList='DATE,NAME_ASC,NAME_DESC' valueList='DATE,NAME_ASC,NAME_DESC'"><%=sortFilesBy%></wb:controlField>
    </td>  
  </tr>
</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
