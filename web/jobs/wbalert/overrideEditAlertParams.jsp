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
<%@ page import="com.workbrain.app.wbinterface.hr.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%@ page import="com.workbrain.app.wbalert.*"%>
<%@ page import="com.wbiag.app.wbalert.source.*"%>
<%!
    private String OVRTYP_UI_PARAM;
%>
<wb:page login='true'>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
<wb:define id="ALERT_ID"><wb:get id="ALERT_ID" scope="parameter" default=""/></wb:define>
<wb:define id="none"><wb:localize id="None" ignoreConfig="true">None</wb:localize></wb:define>
<wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
<wb:define id="PARAM_OVR_TYPES"><wb:get id="PARAM_OVR_TYPES" scope="parameter" default=""/></wb:define>
<wb:define id="LOOK_BACK_MINUTES"><wb:get id="LOOK_BACK_MINUTES" scope="parameter" default=""/></wb:define>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<%
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  DBConnection conn = JSPHelper.getConnection(request);;
  DBServer dbs = DBServer.getServer(conn);

  if (dbs.isOracle()){
    OVRTYP_UI_PARAM =
    "sourceType=SQL source='SELECT ovrtyp_id, ovrtyp_name FROM override_type WHERE mod(ovrtyp_id, 100) = 0' multiChoice=true";
  }
  else if (dbs.isDB2()){
    OVRTYP_UI_PARAM =
    "sourceType=SQL source='SELECT ovrtyp_id, ovrtyp_name FROM override_type WHERE mod(ovrtyp_id, 100) = 0' multiChoice=true";
  }
  else if (dbs.isMSSQL()){
    OVRTYP_UI_PARAM =
      "sourceType=SQL source='SELECT ovrtyp_id, ovrtyp_name FROM override_type WHERE (ovrtyp_id % 100) = 0' multiChoice=true";
  }

  Map param = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
  int alertID = Integer.valueOf((String) param.get( WBAlertTask.PARAM_ALERTID )).intValue();
  String ovrTypes = (String)param.get( OverrideEditAlertSource.PARAM_OVR_TYPES);
  String lookBackDays = (String)param.get( OverrideEditAlertSource.PARAM_LOOK_BACK_MINUTES);
  String ovrCols = (String)param.get( OverrideEditAlertSource.PARAM_OVR_COLUMNS);

if ("SUBMIT".equals(OPERATION.toString())) {
  HashMap newParam = new HashMap();
  newParam.put( WBAlertTask.PARAM_ALERTID, String.valueOf( alertID ) );
  newParam.put( OverrideEditAlertSource.PARAM_OVR_TYPES, PARAM_OVR_TYPES == null ? "" : PARAM_OVR_TYPES.toString() );
  newParam.put( OverrideEditAlertSource.PARAM_LOOK_BACK_MINUTES, LOOK_BACK_MINUTES == null ? "" : LOOK_BACK_MINUTES.toString() );
  String[] ovrColsArr = request.getParameterValues("OVR_COLUMNS");
  StringBuffer sb = new StringBuffer(200);
  if (ovrColsArr != null && ovrColsArr.length > 0) {
      for (int i = 0; i < ovrColsArr.length; i++) {
          sb.append(i > 0 ? "," : "").append(ovrColsArr[i]);
      }
  }
  newParam.put( OverrideEditAlertSource.PARAM_OVR_COLUMNS, sb.toString());

  scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), newParam);
  %>
  <Span>Type updated successfully</Span>
  <BR>
  <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  <%
} else {
%>
<wba:table caption="Override Edit Alert Parameters" captionLocalizeIndex="Override Edit Alert Parameters" width="200">
  <wba:tr>
    <wba:th width='20%'>
      <wb:localize id="PARAM_OVR_TYPES">Override Types (Blank for ALL)</wb:localize>
    </wba:th>
    <wba:td width='80%'>
      <wb:controlField cssClass="inputField" submitName="PARAM_OVR_TYPES" ui="DBLookupUI"  uiParameter="<%=OVRTYP_UI_PARAM%>"><%=ovrTypes%></wb:controlField>

    </wba:td>
  </wba:tr>

  <wba:tr>
    <wba:th width='20%'>
      <wb:localize id="LOOK_BACK_MINUTES">Look Back Minutes</wb:localize>
    </wba:th>
    <wba:td width='80%'>
      <wb:controlField cssClass="inputField" submitName="LOOK_BACK_MINUTES"
                       nullable="false"
                       ui="NumberUI"
                       uiParameter='precision=0'><%=(lookBackDays==null?"":lookBackDays)%></wb:controlField>

    </wba:td>
  </wba:tr>

  <wba:tr>
    <wba:th width='20%'>
      <wb:localize id="OVR_COLUMNS">Override Columns</wb:localize>
    </wba:th>
    <wba:td width='80%'>
      <%
      List ovrColsAll = OverrideEditAlertSource.getAllOverrideColumns();
      List ovrColsSelected = new ArrayList();
      String[] ovrColsArr = StringHelper.detokenizeString(ovrCols , ",");
      if (ovrColsArr != null && ovrColsArr.length > 0) {
          ovrColsSelected = Arrays.asList(ovrColsArr);
      }
      StringBuffer ovrColsHtml = new StringBuffer(200);
      ovrColsHtml.append("<select class='inputField' name='OVR_COLUMNS' size=10 MULTIPLE>");
      for (int i=0 , k=ovrColsAll.size() ; i<k ; i++) {
          String item = (String)ovrColsAll.get(i);
          ovrColsHtml.append("<option value='").append(item).append("'");
          if (ovrColsSelected.size() > 0 && ovrColsSelected.contains(item)) {
              ovrColsHtml.append(" selected");
          }
          ovrColsHtml.append(">").append(item).append("</option>");
      }
      ovrColsHtml.append("</select>");
      out.println(ovrColsHtml.toString());
      %>
    </wba:td>
  </wba:tr>

</wba:table><BR>
<wb:submit id="OPERATION">SUBMIT</wb:submit>
<wba:button type='submit' label='Submit' labelLocalizeIndex="Submit" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
<wba:button label='Cancel' labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
<%}%>
</wb:page>
