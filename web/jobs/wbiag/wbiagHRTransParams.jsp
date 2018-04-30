<%@ include file="/system/wbheader.jsp"%>

<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.wbinterface.*"%>
<%@ page import="com.workbrain.app.wbinterface.hr2.*"%>
<%@ page import="com.wbiag.app.wbinterface.hr2.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>

<wb:page login='true' showUIPath='true'>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="TYPE_ID"><wb:get id="TYPE_ID" scope="parameter" default=""/></wb:define>
<wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="CALC_GRP_MAP_NAME"><wb:get id="CALC_GRP_MAP_NAME" scope="parameter" default=""/></wb:define>
<wb:define id="PAY_GRP_MAP_NAME"><wb:get id="PAY_GRP_MAP_NAME" scope="parameter" default=""/></wb:define>
<wb:define id="SEC_GRP_MAP_NAME"><wb:get id="SEC_GRP_MAP_NAME" scope="parameter" default=""/></wb:define>
<wb:define id="SHIFT_PATTERN_MAP_NAME"><wb:get id="SHIFT_PATTERN_MAP_NAME" scope="parameter" default=""/></wb:define>
<wb:define id="FUTURE_BALANCE_DATES"><wb:get id="FUTURE_BALANCE_DATES" scope="parameter" default=""/></wb:define>
<wb:define id="AUTO_RECALC"><wb:get id="AUTO_RECALC" scope="parameter" default=""/></wb:define>
<wb:define id="BATCH_PROCESS_SIZE"><wb:get id="BATCH_PROCESS_SIZE" scope="parameter" default=""/></wb:define>
<wb:define id="PARAM_WBIAG_EXTENSIONS"><wb:get id="PARAM_WBIAG_EXTENSIONS" scope="parameter" default=""/></wb:define>
<wb:define id="PARAM_HEADER_PREFIX"><wb:get id="PARAM_HEADER_PREFIX" scope="parameter" default=""/></wb:define>
<wb:define id="PARAM_FOOTER_PREFIX"><wb:get id="PARAM_FOOTER_PREFIX" scope="parameter" default=""/></wb:define>
<wb:define id="PARAM_HEADER_CHECKSUM"><wb:get id="PARAM_HEADER_CHECKSUM" scope="parameter" default=""/></wb:define>
<wb:define id="PARAM_FOOTER_CHECKSUM"><wb:get id="PARAM_FOOTER_CHECKSUM" scope="parameter" default=""/></wb:define>
<wb:define id="PARAM_EXTRA_MAPPINGS"><wb:get id="PARAM_EXTRA_MAPPINGS" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location = contextPath + '/jobs/ent/jobs/schedules.jsp';</wb:define>
<%
  Scheduler scheduler = SchedulerObjectFactory.getScheduler();
  if (TASK_ID==null) throw new JspException("No task id has been passed to the page");
  int taskId = Integer.parseInt( TASK_ID.toString());

  Map param = scheduler.getTaskParams(taskId);
  int typeID = Integer.parseInt( String.valueOf( param.get( "transactionType" ) ) );
  String calcGrpMapName = param.get( HRRefreshTransaction.PARAM_CALCGRP_MAP_NAME ) == null
      ? HRRefreshTransaction.DEFAULT_CALCGRP_MAP_NAME
      : (String)param.get( HRRefreshTransaction.PARAM_CALCGRP_MAP_NAME );
  String payGrpMapName = param.get( HRRefreshTransaction.PARAM_PAYGRP_MAP_NAME ) == null
      ? HRRefreshTransaction.DEFAULT_PAYGRP_MAP_NAME
      : (String)param.get( HRRefreshTransaction.PARAM_PAYGRP_MAP_NAME );
  String secGrpMapName = param.get( HRRefreshTransaction.PARAM_SECGRP_MAP_NAME ) == null
      ? HRRefreshTransaction.DEFAULT_SECGRP_MAP_NAME
      : (String)param.get( HRRefreshTransaction.PARAM_SECGRP_MAP_NAME );
  String shiftPatternMapName = param.get( HRRefreshTransaction.PARAM_SHFPAT_MAP_NAME ) == null
      ? HRRefreshTransaction.DEFAULT_SHFPAT_MAP_NAME
      : (String)param.get( HRRefreshTransaction.PARAM_SHFPAT_MAP_NAME );
  String futureBalanceDates = param.get( "FUTURE_BALANCE_DATES" ) == null
      ? null : (String)param.get( "FUTURE_BALANCE_DATES" );
  String autoRecalc = param.get( "AUTO_RECALC" ) == null
      ? null : (String)param.get( "AUTO_RECALC" );
  String batchProcessSize = param.get( HRRefreshTransaction.PARAM_BATCH_PROCESS_SIZE ) == null
      ? String.valueOf(HRRefreshTransaction.DEFAULT_BATCH_PROCESS_SIZE)
      : (String)param.get( HRRefreshTransaction.PARAM_BATCH_PROCESS_SIZE );
  String wbiagExtension = param.get( WBIAGHRRefreshTransaction.PARAM_WBIAG_EXTENSIONS ) == null
      ? ""
      : (String)param.get( WBIAGHRRefreshTransaction.PARAM_WBIAG_EXTENSIONS );
  String headerPrefix = param.get( WBIAGHRRefreshTransaction.PARAM_HEADER_PREFIX ) == null
      ? ""
      : (String)param.get( WBIAGHRRefreshTransaction.PARAM_HEADER_PREFIX );
  String headerCheckSum = param.get( WBIAGHRRefreshTransaction.PARAM_HEADER_CHECKSUM ) == null
      ? ""
      : (String)param.get( WBIAGHRRefreshTransaction.PARAM_HEADER_CHECKSUM );

  String footerPrefix = param.get( WBIAGHRRefreshTransaction.PARAM_FOOTER_PREFIX ) == null
      ? ""
      : (String)param.get( WBIAGHRRefreshTransaction.PARAM_FOOTER_PREFIX );
  String footerCheckSum = param.get( WBIAGHRRefreshTransaction.PARAM_FOOTER_CHECKSUM ) == null
      ? ""
      : (String)param.get( WBIAGHRRefreshTransaction.PARAM_FOOTER_CHECKSUM );
  String extraMappings = param.get( WBIAGHRRefreshTransaction.PARAM_EXTRA_MAPPINGS ) == null
      ? ""
      : (String)param.get( WBIAGHRRefreshTransaction.PARAM_EXTRA_MAPPINGS );

if ("SUBMIT".equals(OPERATION.toString())) {
  param.put( "transactionType", new Integer( typeID ) );
  param.put( HRRefreshTransaction.PARAM_CALCGRP_MAP_NAME, CALC_GRP_MAP_NAME == null ? "" : CALC_GRP_MAP_NAME.toString() );
  param.put( HRRefreshTransaction.PARAM_PAYGRP_MAP_NAME, PAY_GRP_MAP_NAME == null ? "" : PAY_GRP_MAP_NAME.toString() );
  param.put( HRRefreshTransaction.PARAM_SECGRP_MAP_NAME, SEC_GRP_MAP_NAME == null ? "" : SEC_GRP_MAP_NAME.toString() );
  param.put( HRRefreshTransaction.PARAM_SHFPAT_MAP_NAME, SHIFT_PATTERN_MAP_NAME == null ? "" : SHIFT_PATTERN_MAP_NAME.toString() );
  param.put( HRRefreshTransaction.PARAM_BATCH_PROCESS_SIZE, BATCH_PROCESS_SIZE == null ? "" : BATCH_PROCESS_SIZE.toString() );
  param.put( "FUTURE_BALANCE_DATES", FUTURE_BALANCE_DATES == null ? "" : FUTURE_BALANCE_DATES.toString() );
  param.put( "AUTO_RECALC", AUTO_RECALC == null ? "" : AUTO_RECALC.toString() );
  param.put( WBIAGHRRefreshTransaction.PARAM_WBIAG_EXTENSIONS, PARAM_WBIAG_EXTENSIONS == null ? "" : PARAM_WBIAG_EXTENSIONS.toString() );
  param.put( WBIAGHRRefreshTransaction.PARAM_HEADER_PREFIX, PARAM_HEADER_PREFIX == null ? "" : PARAM_HEADER_PREFIX.toString() );
  param.put( WBIAGHRRefreshTransaction.PARAM_HEADER_CHECKSUM, PARAM_HEADER_CHECKSUM == null ? "" : PARAM_HEADER_CHECKSUM.toString() );
  param.put( WBIAGHRRefreshTransaction.PARAM_FOOTER_PREFIX, PARAM_FOOTER_PREFIX == null ? "" : PARAM_FOOTER_PREFIX.toString() );
  param.put( WBIAGHRRefreshTransaction.PARAM_FOOTER_CHECKSUM, PARAM_FOOTER_CHECKSUM == null ? "" : PARAM_FOOTER_CHECKSUM.toString() );
  param.put( WBIAGHRRefreshTransaction.PARAM_EXTRA_MAPPINGS, PARAM_EXTRA_MAPPINGS == null ? "" : PARAM_EXTRA_MAPPINGS.toString() );

  scheduler.setTaskParams(taskId,param);
  %>
  <Span>Type updated successfully</Span>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  <%
} else {
%>
<wba:table caption="HR Refresh Parameter" captionLocalizeIndex="HR_Refresh_Parameter">
  <tr>
    <th>
      <wb:localize id="Calc_Grp_Map_Name">Calculation Group Map Name</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="CALC_GRP_MAP_NAME" ui='StringUI' uiParameter='width=60'><%=calcGrpMapName%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th>
      <wb:localize id="Pay_Grp_Map_Name">Pay Group Map Name</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="PAY_GRP_MAP_NAME" ui='StringUI' uiParameter='width=60'><%=payGrpMapName%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th>
      <wb:localize id="Sec_Grp_Map_Name">Security Group Map Name</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="SEC_GRP_MAP_NAME" ui='StringUI' uiParameter='width=60'><%=secGrpMapName%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th>
      <wb:localize id="Shift_Pattern_Map_Name">Shift Pattern Map Name</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="SHIFT_PATTERN_MAP_NAME" ui='StringUI' uiParameter='width=60'><%=shiftPatternMapName%></wb:controlField>
    </td>
  </tr>
  <tr>
    <th><wb:localize id='AUTO_RECALC' type='field'>Process Auto-Recalc Period</wb:localize></th>
    <td><wb:controlField submitName='AUTO_RECALC' id='AUTO_RECALC' cssClass="inputField"><%= autoRecalc %></wb:controlField></td>
  </tr>
  <tr>
    <th><wb:localize id='FUTURE_BALANCE_DATES' type='field'>Process Future Balance Dates</wb:localize></th>
    <td><wb:controlField submitName='FUTURE_BALANCE_DATES' id='FUTURE_BALANCE_DATES' cssClass="inputField"><%= futureBalanceDates %></wb:controlField></td>
  </tr>
  <tr>
    <th>
      <wb:localize id="Batch_Process_Size">Batch Process Size</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="BATCH_PROCESS_SIZE" ui='StringUI' uiParameter='width=60'><%=batchProcessSize%></wb:controlField>
    </td>
  </tr>

  <tr>
    <th>
      <wb:localize id="PARAM_WBIAG_EXTENSIONS">Solution Center Extensions (i.e format HRRefreshTransactionTSUser~90 HRRefreshTransactionSOExtra~91)</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="PARAM_WBIAG_EXTENSIONS" ui='TextAreaUI' uiParameter='width=60 height=10'><%=wbiagExtension%></wb:controlField>
    </td>
  </tr>

  <tr>
    <th>
      <wb:localize id="PARAM_HEADER_PREFIX">Header Prefix</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="PARAM_HEADER_PREFIX" ui='StringUI' uiParameter=''><%=headerPrefix%></wb:controlField>
    </td>
  </tr>

  <tr>
    <th>
      <wb:localize id="PARAM_HEADER_CHECKSUM">Header Checksum</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="PARAM_HEADER_CHECKSUM" ui='CheckBoxUI' uiParameter=''><%=headerCheckSum%></wb:controlField>
    </td>
  </tr>

  <tr>
    <th>
      <wb:localize id="PARAM_FOOTER_PREFIX">Footer Prefix</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="PARAM_FOOTER_PREFIX" ui='StringUI' uiParameter=''><%=footerPrefix%></wb:controlField>
    </td>
  </tr>

  <tr>
    <th>
      <wb:localize id="PARAM_FOOTER_CHECKSUM">Footer Checksum</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="PARAM_FOOTER_CHECKSUM" ui='CheckBoxUI' uiParameter=''><%=footerCheckSum%></wb:controlField>
    </td>
  </tr>

  <tr>
    <th>
      <wb:localize id="PARAM_EXTRA_MAPPINGS">Extra Mapping Definition (i.e format 8,MAPPING1~75,MAPPING2)</wb:localize>
    </th>
    <td>
          <wb:controlField cssClass='inputField' submitName="PARAM_EXTRA_MAPPINGS" ui='TextAreaUI' uiParameter='width=60 height=10'><%=extraMappings%></wb:controlField>
    </td>
  </tr>


</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
