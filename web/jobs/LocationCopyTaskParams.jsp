<%@ include file="/system/wbheader.jsp"%>

<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.wbinterface.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>


<wb:page login='true' showUIPath='true'>
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>

<wb:define id="copyLocationID"><wb:get id="copyLocationID" scope="parameter" default=""/></wb:define>
<wb:define id="includeSubLocationsFlag"><wb:get id="includeSubLocationsFlag" scope="parameter" default=""/></wb:define>
<wb:define id="copyDistDataFlag"><wb:get id="copyDistDataFlag" scope="parameter" default=""/></wb:define>
<wb:define id="copyHistDataFlag"><wb:get id="copyHistDataFlag" scope="parameter" default=""/></wb:define>

<%
  Scheduler scheduler = SchedulerObjectFactory.getScheduler();
  ScheduledTaskRecord rec = null;
%>

<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
<%
  rec = scheduler.getTask( Integer.parseInt( TASK_ID.toString() ) );
  Map recparams = scheduler.getTaskParams( Integer.parseInt(TASK_ID.toString()) );

  if(recparams.isEmpty()) {
    recparams.put( "copyLocationID", "" );
    //recparams.put( "newLocationName", "" );    
    recparams.put( "includeSubLocationsFlag", "" );
    //recparams.put( "newPrefix", "" );        
    //recparams.put( "oldPrefix", "" );        
    recparams.put( "copyDistDataFlag", "" );        
    recparams.put( "copyHistDataFlag", "" );        
    //recparams.put( "createReaderFlag", "" );        
  }
%>
<wb:switch> 
  <wb:case expression="#TYPE#" compareToExpression="">
    <input type="hidden" name="TYPE" value="scheduleTask">
    <wb:set id="copyLocationID"><%=recparams.get( "copyLocationID" )%></wb:set>           
    <wb:set id="includeSubLocationsFlag"><%=recparams.get( "includeSubLocationsFlag" )%></wb:set>       
    <wb:set id="copyDistDataFlag"><%=recparams.get( "copyDistDataFlag" )%></wb:set>
    <wb:set id="copyHistDataFlag"><%=recparams.get( "copyHistDataFlag" )%></wb:set>
    <wba:table caption="Location Copy Task" captionLocalizeIndex="Location_Copy" width="600">
      <wba:tr>
        <wba:th width='40%'>
          <wb:localize id="Location Template">Location Template</wb:localize>
        </wba:th>
        <wba:td width='60%'>
          <wb:controlField cssClass="inputField" submitName="copyLocationID"
                           nullable="true"
                           ui="LocationTreeUI"
                           uiParameter="width='40'"><%=copyLocationID%></wb:controlField>
        </wba:td>
      </wba:tr>      
      <wba:tr>
        <wba:th width='40%'>
          <wb:localize id="Include Sub Locations">Include Sub Locations</wb:localize>
        </wba:th>
        <wba:td width='60%'>
          <wb:controlField cssClass="inputField" submitName="includeSubLocationsFlag"
                           nullable="true"
                           ui="CheckBoxUI"
                           uiParameter='alternateField=true'><%=includeSubLocationsFlag%></wb:controlField>
        </wba:td>
      </wba:tr>
      <wba:tr>
        <wba:th width='40%'>
          <wb:localize id="Copy Distributions:">Copy Distributions:</wb:localize>
        </wba:th>
        <wba:td width='60%'>
          <wb:controlField cssClass="inputField" submitName="copyDistDataFlag"
                           nullable="true"
                           ui="CheckBoxUI"
                           uiParameter='alternateField=true'><%=copyDistDataFlag%></wb:controlField>
        </wba:td>
      </wba:tr>
      <wba:tr>
        <wba:th width='40%'>
          <wb:localize id="Copy Historical Data:">Copy Historical Data:</wb:localize>
        </wba:th>
        <wba:td width='60%'>
          <wb:controlField cssClass="inputField" submitName="copyHistDataFlag"
                           nullable="true"
                           ui="CheckBoxUI"
                           uiParameter='alternateField=true'><%=copyHistDataFlag%></wb:controlField>
        </wba:td>
      </wba:tr>
    </wba:table><BR>
    <wba:button type='submit' label='Save' labelLocalizeIndex="Save" onClick='disableAllButtons(); document.page_form.submit();'/>&nbsp;
    <!--tt #7828 vrusu 10/04/2002-->
    <wb:define id="onClickCancel">window.location= contextPath + '/jobs/ent/jobs/schedules.jsp'</wb:define>
    <wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #onClickCancel#"/>
  </wb:case>
  <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
    <%
      String param0 = copyLocationID.toString();      
      String param3 = includeSubLocationsFlag.toString();
      String param6 = copyDistDataFlag.toString();
      String param7 = copyHistDataFlag.toString();

      recparams.put( "copyLocationID", param0 );      
      recparams.put( "includeSubLocationsFlag", param3 );        
      recparams.put( "copyDistDataFlag", param6 );        
      recparams.put( "copyHistDataFlag", param7 );        
      scheduler.setTaskParams( Integer.parseInt( TASK_ID.toString() ), recparams );
%>
    <Span>Parameters updated successfully</Span>
    <BR>
    <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
  </wb:case>
</wb:switch>
</wb:if>
</wb:page>


