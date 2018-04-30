<%@ include file="/system/wbheader.jsp"%>
<%@ taglib uri="/wbsys" prefix="wb" %>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="javax.naming.*"%>
<%@ page import="javax.servlet.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.scheduler.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.export.process.*"%>
<%@ page import="com.wbiag.app.ta.ruleengine.*"%>
<%@ page import="com.wbiag.app.scheduler.tasks.SyncTeamsAndLocationsTask"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="org.apache.log4j.*"%>

<%-- ********************** --%>
<wb:page login="true">
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>

<wb:define id="teamType"><wb:get id="teamType" scope="parameter" default=""/></wb:define>
<wb:define id="template"><wb:get id="template" scope="parameter" default=""/></wb:define>

<%  
  Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
  ScheduledTaskRecord rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
  if (rec==null) throw new JspException("No task id has been passed to the page");

  Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    if(recparams.isEmpty()) 
    {
        recparams.put( SyncTeamsAndLocationsTask.PARAM_TEAM_TYPE, "1" );
        recparams.put( SyncTeamsAndLocationsTask.PARAM_TEMPLATE, "1" );
    }
    %>
    <wb:switch>
    <wb:case expression="#TYPE#" compareToExpression="">
      <input type="hidden" name="TYPE" value="scheduleTask">        
      
      <wb:set id="teamType"><%=recparams.get( SyncTeamsAndLocationsTask.PARAM_TEAM_TYPE )%></wb:set>
      <wb:set id="template"><%=recparams.get( SyncTeamsAndLocationsTask.PARAM_TEMPLATE )%></wb:set>      

      <wba:table caption="Synchronize Teams And Locations Task" captionLocalizeIndex="Sync_Teams_And_Locations_Task" width="600">
        
        <wba:tr>
          <wba:th>
            <wb:localize id="syncTeamLocationTeamType">Team Type(s):</wb:localize>
          </wba:th>
          <wba:td>
          	<wb:controlField cssClass='inputField' submitName="teamType" ui='DBLookupUI' uiParameter='sourceType=SQL multiChoice=True source=\"SELECT WBTT_ID, WBTT_NAME FROM WORKBRAIN_TEAM_TYPE\" width=30'><%=teamType%></wb:controlField>
          </wba:td>
        </wba:tr>
        
        <wba:tr>
          <wba:th>
            <wb:localize id="syncTeamLocationTemplate">Template:</wb:localize>
          </wba:th>
          <wba:td>
          	<wb:controlField cssClass='inputField' submitName="template" ui='LocationTreeUI' uiParameter="width=30"><%=template%></wb:controlField>
          </wba:td>
        </wba:tr>
        
      </wba:table>

      <div class="separatorLarge"></div>

      <wba:button type="submit" label="Submit" labelLocalizeIndex="Submit" onClick=""/>&nbsp;
      <wba:button label="cancel" labelLocalizeIndex="Cancel" onClick="#okOnClickUrl#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%
            recparams.put( SyncTeamsAndLocationsTask.PARAM_TEAM_TYPE , teamType.toString() );
            recparams.put( SyncTeamsAndLocationsTask.PARAM_TEMPLATE , template.toString() );
            scheduler.setTaskParams( Integer.parseInt( TASK_ID.toString() ), recparams );
        %>
        <Span>Synchronize Teams and Locations Task Successfully Scheduled</Span>
        <br>
        <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
    </wb:case>
    </wb:switch>
</wb:if>
</wb:page>