<%@ include file="/system/wbheader.jsp"%> 
<%@ page import="java.io.*"%> 
<%@ page import="java.util.*"%> 
<%@ page import="javax.naming.*"%> 
<%@ page import="com.workbrain.sql.*"%> 
<%@ page import="com.workbrain.util.*"%> 
<%@ page import="com.workbrain.server.jsp.*"%> 
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%> 
<%@ page import="com.workbrain.app.wbinterface.*"%> 
<%@ page import="com.wbiag.app.wbinterface.hbm.*"%> 
<%@ page import="java.lang.reflect.*"%> 
<%@ page import="java.sql.*"%> 
<%@ page import="javax.servlet.jsp.JspException" %> 
<wb:page login='true' showUIPath='true'> 
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define> 
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit> 
<wb:define id="TYPE_ID"><wb:get id="TYPE_ID" scope="parameter" default=""/></wb:define> 
<wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define> 
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>  
<wb:define id="BATCH_PROCESS_SIZE"><wb:get id="BATCH_PROCESS_SIZE" scope="parameter" default=""/></wb:define> 
<wb:define id="DEF_START_DATE"><wb:get id="DEF_START_DATE" scope="parameter" default=""/></wb:define> 
<wb:define id="DEF_END_DATE"><wb:get id="DEF_END_DATE" scope="parameter" default=""/></wb:define> 
<wb:define id="okOnClickUrl">window.location = contextPath + '/jobs/ent/jobs/schedules.jsp';</wb:define> 
<%
  Scheduler scheduler = SchedulerObjectFactory.getScheduler();
  if (TASK_ID==null) throw new JspException("No task id has been passed to the page");
  int taskId = Integer.parseInt( TASK_ID.toString());    

  Map param = scheduler.getTaskParams(taskId);
  int typeID = Integer.parseInt( String.valueOf( param.get( "transactionType" ) ) );
  String batchProcessSize = param.get( HBMImportTransaction.PARAM_BATCH_PROCESS_SIZE ) == null 
      ? String.valueOf(HBMImportTransaction.DEFAULT_BATCH_PROCESS_SIZE)
      : (String)param.get( HBMImportTransaction.PARAM_BATCH_PROCESS_SIZE );  
  String defaultStartDate = param.get( HBMImportTransaction.PARAM_DEFAULT_START_DATE ) == null 
      ? String.valueOf(HBMImportTransaction.DEFAULT_START_DATE)
      : (String)param.get( HBMImportTransaction.PARAM_DEFAULT_START_DATE );   
  String defaultEndDate = param.get( HBMImportTransaction.PARAM_DEFAULT_END_DATE ) == null 
      ? String.valueOf(HBMImportTransaction.DEFAULT_END_DATE)
      : (String)param.get( HBMImportTransaction.PARAM_DEFAULT_END_DATE );   
  
if ("SUBMIT".equals(OPERATION.toString())) {
  Map newParam = new HashMap();
  newParam.put( "transactionType", new Integer( typeID ) );
  newParam.put( HBMImportTransaction.PARAM_BATCH_PROCESS_SIZE, BATCH_PROCESS_SIZE == null ? "" : BATCH_PROCESS_SIZE.toString() );  
  newParam.put( HBMImportTransaction.PARAM_DEFAULT_START_DATE, DEF_START_DATE == null ? "" : DEF_START_DATE.toString() );
  newParam.put( HBMImportTransaction.PARAM_DEFAULT_END_DATE, DEF_END_DATE == null ? "" : DEF_END_DATE.toString() );  
  scheduler.setTaskParams(taskId,newParam);
%> 
<Span>Type updated successfully</Span> <BR> <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/> 
<%
} else {
%> 
<wba:table caption="HBM Import Parameter" captionLocalizeIndex="HBM_Import_Parameter" width="150"> 
	<wba:tr> 
		<wba:th width='20%'> 
			<wb:localize id="Batch_Process_Size">Batch Process Size</wb:localize> 
		</wba:th> 
		<wba:td width='80%'> 
			<wb:controlField cssClass='inputField' submitName="BATCH_PROCESS_SIZE" ui='StringUI' uiParameter='width=5'><%=batchProcessSize%></wb:controlField> 
		</wba:td> 
	</wba:tr> 
	<wba:tr> 
		<wba:th width='20%'> 
			<wb:localize id="Def_Start_Date">Default Start Date</wb:localize> 
		</wba:th> 
		<wba:td width='80%'> 
			<wb:controlField cssClass='inputField' submitName="DEF_START_DATE" ui='DatePickerUI' uiParameter='width=10'><%=defaultStartDate%></wb:controlField> 
		</wba:td> 
	</wba:tr> 	
	<wba:tr> 
		<wba:th width='20%'> 
			<wb:localize id="Def_End_Date">Default End Date</wb:localize> 
		</wba:th> 
		<wba:td width='80%'> 
			<wb:controlField cssClass='inputField' submitName="DEF_END_DATE" ui='DatePickerUI' uiParameter='width=10'><%=defaultEndDate%></wb:controlField> 
		</wba:td> 
	</wba:tr> 	
	
</wba:table>
<BR> <wb:submit id="OPERATION">SUBMIT</wb:submit> <wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp; <wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/> <%}%> </wb:page> 