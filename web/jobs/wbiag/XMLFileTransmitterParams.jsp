<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.wbinterface.*"%>
<%@ page import="com.wbiag.app.export.process.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>

<wb:page login='true' showUIPath='true'>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="XMLDeclaration"><wb:get id="XMLDeclaration" scope="parameter" default=""/></wb:define>
<wb:define id="XMLRootTag"><wb:get id="XMLRootTag" scope="parameter" default=""/></wb:define>
<wb:define id="XMLRecordTag"><wb:get id="XMLRecordTag" scope="parameter" default=""/></wb:define>
<wb:define id="FileTimestampFormat"><wb:get id="FileTimestampFormat" scope="parameter" default=""/></wb:define>
<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>

<%
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");
  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  
  if (param.isEmpty())
  {
	param.put(XMLFileTransmitter.PARAM_XML_DECLARATION, XMLFileTransmitter.PARAM_XML_DECLARATION_DEFAULT);
      param.put(XMLFileTransmitter.PARAM_XML_ROOT_TAG, XMLFileTransmitter.PARAM_XML_ROOT_TAG_DEFAULT);
	param.put(XMLFileTransmitter.PARAM_XML_RECORD_TAG, XMLFileTransmitter.PARAM_XML_RECORD_TAG_DEFAULT);
      param.put(XMLFileTransmitter.PARAM_FILE_TIMESTAMP_FORMAT, ""); 
  }
if (!"SUBMIT".equals(OPERATION.toString())){
%>
<wb:set id="XMLDeclaration"><%= param.get( XMLFileTransmitter.PARAM_XML_DECLARATION) == null ? "" : param.get( XMLFileTransmitter.PARAM_XML_DECLARATION ) %></wb:set>
<wb:set id="XMLRootTag"><%= param.get( XMLFileTransmitter.PARAM_XML_ROOT_TAG ) == null ? "" : param.get(XMLFileTransmitter.PARAM_XML_ROOT_TAG) %></wb:set>
<wb:set id="XMLRecordTag"><%= param.get(XMLFileTransmitter.PARAM_XML_RECORD_TAG) == null ? "" : param.get(XMLFileTransmitter.PARAM_XML_RECORD_TAG)%></wb:set>
<wb:set id="FileTimestampFormat"><%= param.get( XMLFileTransmitter.PARAM_FILE_TIMESTAMP_FORMAT ) == null ? "" : param.get( XMLFileTransmitter.PARAM_FILE_TIMESTAMP_FORMAT ) %></wb:set>
<wba:table caption="XML File Transmitter Parameters" captionLocalizeIndex="XML File Transmitter Parameters" width="300" >

<wba:tr>
    <wba:th width='40%'>
      <wb:localize id="XMLDeclaration">XML Declaration</wb:localize>
    </wba:th>
    <wba:td width='60%'>
          <wb:controlField cssClass="inputField" submitName="XMLDeclaration" ui="TextAreaUI" uiParameter="width=50 heigh=5"><%=(null == XMLDeclaration  ? "" : XMLDeclaration.toString())%></wb:controlField>
    </wba:td>
  </wba:tr>

  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="XMLRootTag">XML Root Tag</wb:localize>
    </wba:th>
    <wba:td width='60%'>
          <wb:controlField cssClass="inputField" submitName="XMLRootTag" ui="StringUI" uiParameter="width=20"><%=(null == XMLRootTag  ? "" : XMLRootTag.toString())%></wb:controlField>
    </wba:td>
  </wba:tr>

  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="XMLRecordTag">XML Record Tag</wb:localize>
    </wba:th>
    <wba:td width='60%'>
          <wb:controlField cssClass="inputField" submitName="XMLRecordTag" ui="StringUI" uiParameter="width=20"><%=(null == XMLRecordTag  ? "" : XMLRecordTag.toString())%></wb:controlField>
    </wba:td>
  </wba:tr>

  <wba:tr>
    <wba:th width='40%'>
      <wb:localize id="FileTimestampFormat">File Timestamp Format (blank means no timestamp format)</wb:localize>
    </wba:th>
    <wba:td width='60%'>
          <wb:controlField cssClass="inputField" submitName="FileTimestampFormat" ui="StringUI" uiParameter="width=20"><%=(null == FileTimestampFormat  ? "" : FileTimestampFormat.toString())%></wb:controlField>
    </wba:td>
  </wba:tr>

</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}
else{


 param.put( XMLFileTransmitter.PARAM_XML_DECLARATION, XMLDeclaration.toString() );
 param.put( XMLFileTransmitter.PARAM_XML_ROOT_TAG, XMLRootTag.toString() );
 param.put( XMLFileTransmitter.PARAM_XML_RECORD_TAG, XMLRecordTag.toString() );
 param.put( XMLFileTransmitter.PARAM_FILE_TIMESTAMP_FORMAT, FileTimestampFormat.toString() );
 scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), param);

  %>
  <Span>Type updated successfully</Span>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  <%
}%>

</wb:page>


