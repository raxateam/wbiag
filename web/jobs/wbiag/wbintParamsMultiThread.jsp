<%@ include file="/system/wbheader.jsp"%>

<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.wbinterface.*"%>
<%@ page import="com.wbiag.app.wbinterface.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%@ page import="org.apache.log4j.*"%>
<%!
    private static final Logger wbintParamsLogger = Logger.getLogger("jsp.wbinterface.wbintParams");
%>

<wb:page login='true' showUIPath='true' uiPathName='Interface Task Parameters' uiPathNameId='UIPATHNAME_ID_WBINT_PARAMS'>
<wb:config id='UIPATHNAME_ID_WBINT_PARAMS'/>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
<wb:define id="TYPE_ID"><wb:get id="TYPE_ID" scope="parameter" default=""/></wb:define>
<wb:define id="THREAD_COUNT"><wb:get id="THREAD_COUNT" scope="parameter" default=""/></wb:define>
<wb:define id="PARTITION_COLUMN_NAME"><wb:get id="PARTITION_COLUMN_NAME" scope="parameter" default=""/></wb:define>

<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define>
<%
  Scheduler scheduler = SchedulerObjectFactory.getScheduler();
  if (TASK_ID==null) throw new JspException("No task id has been passed to the page");
  int taskId = Integer.parseInt( TASK_ID.toString());

  Map param = scheduler.getTaskParams(taskId);
  int typeID = (!param.containsKey(WBInterfaceTask.TRANSACTION_TYPE_PARAM_NAME)) ? -1 : Integer.parseInt( String.valueOf(param.get( WBInterfaceTask.TRANSACTION_TYPE_PARAM_NAME )) );
  String threadCount = !param.containsKey(WBInterfaceTaskMultiThread.PARAM_THREAD_COUNT)
                       ? String.valueOf(WBInterfaceTaskMultiThread.THREAD_COUNT_DEFAULT)
                       : (String)param.get(WBInterfaceTaskMultiThread.PARAM_THREAD_COUNT);
  String partitionColumnName = (String)param.get(WBInterfaceTaskMultiThread.PARAM_PARTITION_COLUMN_NAME);

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
    if (s!=null) s.close();
    if (rs!=null) rs.close();
  }

%>
<wb:define id="TaskNames"><%=names.toString()%></wb:define>
<wb:define id="TaskValues"><%=values.toString()%></wb:define>

<%
if (!StringHelper.isEmpty(TYPE_ID.toString())) {
  typeID = Integer.parseInt( TYPE_ID.toString() );
  String forwardPage = null;
  connection = JSPHelper.getConnection(request);
  s = null;
  rs = null;
  try {
    s = connection.createStatement();
    rs = s.executeQuery("SELECT WBITYP_JAVACLASS FROM WBINT_TYPE WHERE WBITYP_ID=" + typeID);
    if (rs.next()) {
      String className = rs.getString(1);
      if(className!=null) {
        TransactionType transType = (TransactionType)Class.forName(className).newInstance();
        forwardPage = transType.getTaskUI();
      }
    }
  } catch( Throwable t ) {
    wbintParamsLogger.error(t);
  } finally {
    if (s!=null) s.close();
    if (rs!=null) rs.close();
  }

  param.put( WBInterfaceTask.TRANSACTION_TYPE_PARAM_NAME, new Integer( typeID ) );
  param.put( WBInterfaceTaskMultiThread.PARAM_THREAD_COUNT, THREAD_COUNT.toString() );
  param.put( WBInterfaceTaskMultiThread.PARAM_PARTITION_COLUMN_NAME, PARTITION_COLUMN_NAME.toString() );
  scheduler.setTaskParams(taskId,param);
  if( forwardPage==null || "".equals(forwardPage) ) {
%>
  <BR>
  <Span><wb:localize id="Type_Updated_Successfully">Type updated successfully</wb:localize></Span>
  <BR>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%
  } else {
%>
  <wb:forward page="<%=forwardPage%>" />
<%
  }
} else {
%>
<wba:table caption="Multi Thread Interface Task" captionLocalizeIndex="Multi Thread Interface_Task" width="200">
  <tr>
    <th width='20%'>
      <wb:localize id="Interface_Type">Interface Type</wb:localize>
    </th>
    <td width='80%'>
      <wb:controlField cssClass="inputField" submitName="TYPE_ID"
                       nullable="false"
                       ui="ComboBoxUI"
                       uiParameter="labelList='#TaskNames#' valueList='#TaskValues#'"><%=String.valueOf(typeID)%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th width='20%'>
      <wb:localize id="Thread_Count">Thread_Count</wb:localize>
    </th>
    <td width='80%'>
      <wb:controlField cssClass="inputField" submitName="THREAD_COUNT" ui="NumberUI"><%=threadCount%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th width='20%'>
      <wb:localize id="Partition_Column_Name">Partition_Column_Name</wb:localize>
    </th>
    <td width='80%'>
      <wb:controlField cssClass="inputField" submitName="PARTITION_COLUMN_NAME" ui="StringUI"><%=partitionColumnName%></wb:controlField>
    </td>
  </tr>
</wba:table><BR>
<wba:button type='submit' label='Next' labelLocalizeIndex="Next" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
