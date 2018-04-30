<%@ include file="/system/wbheader.jsp"%>
<%@ taglib uri="/wbsys" prefix="wb" %>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="javax.servlet.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.wbiag.app.modules.employeeborrow.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="org.apache.log4j.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%!
    private static final String CLIENT_UI_PARAM =
      "sourceType=SQL source='SELECT client_id, client_name FROM workbrain_client' multiChoice=false";

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
<wb:define id="sendsMessages"><wb:get id="sendsMessages" scope="parameter" default=""/></wb:define>
<wb:define id="clientId"><wb:get id="clientId" scope="parameter" default=""/></wb:define>
<wb:define id="batchSize"><wb:get id="batchSize" scope="parameter" default=""/></wb:define>


<%
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    if(recparams.isEmpty()) {
        recparams.put( EmployeeBorrowEmpGroupTask.PARAM_SENDS_MESSAGE, "N" );
        recparams.put( EmployeeBorrowEmpGroupTask.PARAM_CLIENT_ID, "" );
        recparams.put( EmployeeBorrowEmpGroupTask.PARAM_BATCH_SIZE, String.valueOf(EmployeeBorrowEmpGroupTask.DEFAULT_BATCH_SIZE) );
    }
    %>
    <wb:switch>
    <wb:case expression="#TYPE#" compareToExpression="">
      <input type="hidden" name="TYPE" value="scheduleTask">
      <wb:set id="sendsMessages"><%=recparams.get( EmployeeBorrowEmpGroupTask.PARAM_SENDS_MESSAGE )%></wb:set>
      <wb:set id="clientId"><%=recparams.get( EmployeeBorrowEmpGroupTask.PARAM_CLIENT_ID )%></wb:set>
      <wb:set id="batchSize"><%=recparams.get( EmployeeBorrowEmpGroupTask.PARAM_BATCH_SIZE )%></wb:set>

      <wba:table caption="Employee Borrow Staff Group Assignment Task" captionLocalizeIndex="Employee Borrow Staff Group Assignment Task" width="600">

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_SENDS_MESSAGES">PARAM_SENDS_MESSAGES</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="sendsMessages" ui='CheckboxUI' uiParameter=''><%=sendsMessages%></wb:controlField>
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

        <wba:tr>
          <wba:th>
            <wb:localize id="PARAM_BATCH_SIZE">PARAM_BATCH_SIZE</wb:localize>
          </wba:th>
          <wba:td>
            <wb:controlField cssClass='inputField' submitName="batchSize" ui='NumberUI'><%=batchSize%></wb:controlField>
          </wba:td>
        </wba:tr>
      </wba:table>

      <div class="separatorLarge"></div>

      <wba:button label="Submit" labelLocalizeIndex="Submit" onClick="checkSubmit();"/>&nbsp;
      <wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="#onClickCancel#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%
            recparams.put( EmployeeBorrowEmpGroupTask.PARAM_SENDS_MESSAGE , sendsMessages.toString() );
            recparams.put( EmployeeBorrowEmpGroupTask.PARAM_CLIENT_ID , clientId.toString() );
            recparams.put( EmployeeBorrowEmpGroupTask.PARAM_BATCH_SIZE , batchSize.toString() );
            scheduler.setTaskParams( Integer.parseInt( TASK_ID.toString() ), recparams );

        %>
        <Span>Employee Borrow Staff Group Assignment Task Successfully Scheduled</Span>
        <br>
        <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
    </wb:case>
    </wb:switch>
</wb:if>
</wb:page>
