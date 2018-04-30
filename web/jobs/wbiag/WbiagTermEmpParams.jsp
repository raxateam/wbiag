<%@ include file="/system/wbheader.jsp"%>
<%@ taglib uri="/wbsys" prefix="wb" %>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="javax.servlet.*"%>
<%@ page import="javax.servlet.jsp.JspException"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.wbiag.server.hr.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="org.apache.log4j.*"%>
<%!

    private static final String CLIENT_UI_PARAM =
      "sourceType=SQL source='SELECT client_id, client_name FROM workbrain_client' multiChoice=false";
    private String OVRTYP_UI_PARAM;
    private static final String BAL_UI_PARAM =
      "sourceType=SQL source='SELECT bal_id, bal_name FROM balance' multiChoice=true";

%>
<%-- ********************** --%>
<wb:page login="true">
<script language="JavaScript">
  function checkSubmit(){
      if(document.page_form.clientId.value  == "") {
          alert("Client id has to be specified");
          return;
      }
      disableAllButtons();
      document.page_form.submit();
  }
</script>
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>
<wb:define id="onClickCancel">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>
<wb:define id="processEmpJobs"><wb:get id="processEmpJobs" scope="parameter" default=""/></wb:define>
<wb:define id="processEntPols"><wb:get id="processEntPols" scope="parameter" default=""/></wb:define>
<wb:define id="processOvrs"><wb:get id="processOvrs" scope="parameter" default=""/></wb:define>
<wb:define id="empBalReset"><wb:get id="empBalReset" scope="parameter" default=""/></wb:define>
<wb:define id="processWbu"><wb:get id="processWbu" scope="parameter" default=""/></wb:define>
<wb:define id="processTeamH"><wb:get id="processTeamH" scope="parameter" default=""/></wb:define>
<wb:define id="clientId"><wb:get id="clientId" scope="parameter" default=""/></wb:define>
<wb:define id="processEmpSkds"><wb:get id="processEmpSkds" scope="parameter" default=""/></wb:define>
<wb:define id="processPayrollData"><wb:get id="processPayrollData" scope="parameter" default=""/></wb:define>
<wb:define id="processEmpStatus"><wb:get id="processEmpStatus" scope="parameter" default=""/></wb:define>
<wb:define id="processReaderGroups"><wb:get id="processReaderGroups" scope="parameter" default=""/></wb:define>

<%
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new javax.servlet.jsp.JspException("No task id has been passed to the page");

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
  Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    if(recparams.isEmpty()) {
        recparams.put( WbiagTermEmpTask.PARAM_PROCESS_EMPJOBS, "N" );
        recparams.put( WbiagTermEmpTask.PARAM_PROCESS_ENTEMPPOLS, "N" );
        recparams.put( WbiagTermEmpTask.PARAM_PROCESS_WBUSER, "N" );
        recparams.put( WbiagTermEmpTask.PARAM_PROCESS_TEAM_HIERARCHY, "N" );
        recparams.put( WbiagTermEmpTask.PARAM_PROCESS_EMPSKDS, "N");
        recparams.put( WbiagTermEmpTask.PARAM_PROCESS_PAYROLL_DATA, "N");
        recparams.put( WbiagTermEmpTask.PARAM_CLIENT_ID, "" );
        recparams.put( WbiagTermEmpTask.PARAM_PROCESS_EMPSTATUS, "N" );
        recparams.put( WbiagTermEmpTask.PARAM_PROCESS_READERGROUPS, "N" );
    }
    %>
    <wb:switch>
    <wb:case expression="#TYPE#" compareToExpression="">
      <input type="hidden" name="TYPE" value="scheduleTask">
      <wb:set id="processEmpJobs"><%=recparams.get( WbiagTermEmpTask.PARAM_PROCESS_EMPJOBS )%></wb:set>
      <wb:set id="processEntPols"><%=recparams.get( WbiagTermEmpTask.PARAM_PROCESS_ENTEMPPOLS )%></wb:set>
      <wb:set id="processOvrs"><%=recparams.get( WbiagTermEmpTask.PARAM_PROCESS_OVRS )%></wb:set>
      <wb:set id="empBalReset"><%=recparams.get( WbiagTermEmpTask.PARAM_EMP_BALS_RESET )%></wb:set>
      <wb:set id="processWbu"><%=recparams.get( WbiagTermEmpTask.PARAM_PROCESS_WBUSER )%></wb:set>
      <wb:set id="processTeamH"><%=recparams.get( WbiagTermEmpTask.PARAM_PROCESS_TEAM_HIERARCHY )%></wb:set>
      <wb:set id="processEmpSkds"><%=recparams.get( WbiagTermEmpTask.PARAM_PROCESS_EMPSKDS )%></wb:set>
      <wb:set id="processPayrollData"><%=recparams.get( WbiagTermEmpTask.PARAM_PROCESS_PAYROLL_DATA )%></wb:set>
      <wb:set id="processEmpStatus"><%=recparams.get( WbiagTermEmpTask.PARAM_PROCESS_EMPSTATUS )%></wb:set>
      <wb:set id="processReaderGroups"><%=recparams.get( WbiagTermEmpTask.PARAM_PROCESS_READERGROUPS )%></wb:set>
      <wb:set id="clientId"><%=recparams.get( WbiagTermEmpTask.PARAM_CLIENT_ID )%></wb:set>

      <wba:table caption="Terminated Employee Task" captionLocalizeIndex="Terminated_Employee_Task" width="600">

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_EMPJOBS">PARAM_PROCESS_EMPJOBS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processEmpJobs" ui='CheckboxUI' uiParameter=''><%=processEmpJobs%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_ENTEMPPOLS">PARAM_PROCESS_ENTEMPPOLS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processEntPols" ui='CheckboxUI' uiParameter=''><%=processEntPols%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_OVRS">PARAM_PROCESS_OVRS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processOvrs" ui='DBLookupUI' uiParameter='<%=OVRTYP_UI_PARAM%>'><%=processOvrs%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_EMP_BALS_RESET">PARAM_EMP_BALS_RESET</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="empBalReset" ui='DBLookupUI' uiParameter='<%=BAL_UI_PARAM%>'><%=empBalReset%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_WBUSER">PARAM_PROCESS_WBUSER</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processWbu" ui='CheckboxUI' uiParameter=''><%=processWbu%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_TEAM_HIERARCHY">PARAM_PROCESS_TEAM_HIERARCHY</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processTeamH" ui='CheckboxUI' uiParameter=''><%=processTeamH%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_EMPSKDS">PARAM_PROCESS_EMPSKDS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processEmpSkds" ui='CheckboxUI' uiParameter=''><%=processEmpSkds%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_PAYROLL_DATA">PARAM_PROCESS_PAYROLL_DATA</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processPayrollData" ui='CheckboxUI' uiParameter=''><%=processPayrollData%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_EMPSTATUS">PARAM_PROCESS_EMPSTATUS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processEmpStatus" ui='CheckboxUI' uiParameter=''><%=processEmpStatus%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_PROCESS_READERGROUPS">PARAM_PROCESS_READERGROUPS</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="processReaderGroups" ui='CheckboxUI' uiParameter=''><%=processReaderGroups%></wb:controlField>
          </wba:td>
        </wba:tr>

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_CLIENT_ID">PARAM_CLIENT_ID</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="clientId" ui='DBLookupUI' uiParameter='<%=CLIENT_UI_PARAM%>'><%=clientId%></wb:controlField>
          </wba:td>
        </wba:tr>

      </wba:table>

      <div class="separatorLarge"></div>

      <wba:button label="Submit" labelLocalizeIndex="Submit" onClick="checkSubmit();"/>&nbsp;
      <wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="#onClickCancel#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%
            recparams.put( WbiagTermEmpTask.PARAM_PROCESS_EMPJOBS , processEmpJobs.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_PROCESS_ENTEMPPOLS , processEntPols.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_PROCESS_OVRS , processOvrs.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_EMP_BALS_RESET , empBalReset.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_PROCESS_WBUSER , processWbu.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_PROCESS_TEAM_HIERARCHY , processTeamH.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_CLIENT_ID , clientId.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_PROCESS_EMPSKDS , processEmpSkds.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_PROCESS_PAYROLL_DATA , processPayrollData.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_PROCESS_EMPSTATUS , processEmpStatus.toString() );
            recparams.put( WbiagTermEmpTask.PARAM_PROCESS_READERGROUPS , processReaderGroups.toString() );
            scheduler.setTaskParams( Integer.parseInt( TASK_ID.toString() ), recparams );

        %>
        <Span>Terminated Employee Task Successfully Scheduled</Span>
        <br>
        <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
    </wb:case>
    </wb:switch>
</wb:if>
</wb:page>
