<%@ include file="/system/wbheader.jsp"%>

<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.app.wbinterface.mapping_rowsource.*"%>
<%@ page import="com.workbrain.security.SecurityService"%>
<%@ page import="com.workbrain.app.scheduler.enterprise.*"%>
<%@ page import="com.workbrain.app.export.process.*"%>
<%@ page import="com.wbiag.app.export.scheduleout.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>

<wb:page login="true" showUIPath="true" uiPathName='Export Parameters' uiPathNameId='UIPATHNAME_ID_WBEXPORT_PARAMS'>
<wb:config id='UIPATHNAME_ID_WBEXPORT_PARAMS'/>
<script type='text/javascript'>
  function checkSubmit(){
      if(document.page_form.mapping.value  == "") {
          alert('<wb:localize ignoreConfig="true" id="MAPPING_HAS_TO_BE_SPECIFIED">Mapping has to be specified</wb:localize>');
          return;
      }
      if(document.page_form.exportPath.value  == "") {
          alert('<wb:localize ignoreConfig="true" id="EXPORT_PATH_HAS_TO_BE_SPECIFIED">Export file name has to be specified</wb:localize>');
          return;
      }
      if(document.page_form.transmitterClass.value  == "") {
          alert('<wb:localize ignoreConfig="true" id="TRANSMITTER_CLASS_HAS_TO_BE_SPECIFIED">Transmitter class has to be specified</wb:localize>');
          return;
      }
       if(document.page_form.brkCodeList.value  == "") {
          alert('<wb:localize ignoreConfig="true" id="BRK_CODE_LIST_HAS_TO_BE_SPECIFIED">Break code list has to be specified</wb:localize>');
          return;
      }
      disableAllButtons();
      document.page_form.submit();
  }
</script> 
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/jobs/ent/jobs/schedules.jsp"/></wb:define>
<wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>#GO_TO#';</wb:define>
<wb:define id="mapping"><wb:get id="mapping" scope="parameter" default="SCHEDULE EXPORT"/></wb:define>
<wb:define id="exportPath"><wb:get id="exportPath" scope="parameter" default=""/></wb:define>
<wb:define id="clientId"><wb:get id="clientId" scope="parameter" default=""/></wb:define>
<wb:define id="transmitterClass"><wb:get id="transmitterClass" scope="parameter" default=""/></wb:define>
<wb:define id="brkCodeList"><wb:get id="brkCodeList" scope="parameter" default=""/></wb:define>

<%
    Scheduler scheduler = SchedulerObjectFactory.getScheduler();
    if (TASK_ID==null) throw new JspException("No task id has been passed to the page");
    int taskId = Integer.parseInt( TASK_ID.toString());    

%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    DBConnection connection = JSPHelper.getConnection(request);
    StringBuffer clientIds=new StringBuffer();
    StringBuffer clientNames=new StringBuffer();  
    int clientCount = 0;
    Statement s = null;
    ResultSet rs = null;

    try {
      s = connection.createStatement();
      rs = s.executeQuery("SELECT client_id, client_name FROM workbrain_client ORDER BY client_name");
      while (rs.next()) {
        if (clientCount > 0) {
          clientIds.append(',');
          clientNames.append(',');
        } 
        clientIds.append(rs.getInt(1));
        clientNames.append(rs.getString(2));
        clientCount++;      
      }
    } finally {
      if (rs!=null) rs.close();
      if (s!=null) s.close();
    }

    if (clientCount == 0) { 
    %> <p> At least one client must exist in the system , please contact your System Administrator. <wb:stop/> 
    <%  } %> 

    <wb:define id="ClientIds"><%=clientIds.toString()%></wb:define> 
    <wb:define id="ClientNames"><%=clientNames.toString()%></wb:define> 

    <%
    Map recparams = scheduler.getTaskParams(taskId);
    if(recparams.isEmpty()) {
        recparams.put( "mappingName", "SCHEDULE EXPORT" );
        recparams.put( "exportPath", "" );
        recparams.put( "clientId", SecurityService.getCurrentClientId());
        recparams.put( "transmitterClass", "com.wbiag.app.export.process.XMLFileTransmitter" );
        recparams.put( "brkCodeList", "BRK");
    }
    %>
    <wb:switch>    
    <wb:case expression="#TYPE#" compareToExpression="">
    <input type="hidden" name="TYPE" value="scheduleTask">
    <wb:set id="mapping"><%= recparams.get( "mappingName" ) == null ? "" : recparams.get( "mappingName" ) %></wb:set>
    <wb:set id="exportPath"><%= recparams.get( "exportPath" ) == null ? "" : recparams.get( "exportPath" ) %></wb:set>
    <wb:set id="clientId"><%= recparams.get("clientId") == null ? SecurityService.getCurrentClientId() : recparams.get("clientId")%></wb:set>
    <wb:set id="transmitterClass"><%= recparams.get( "transmitterClass" ) == null ? "" : recparams.get( "transmitterClass" ) %></wb:set>
    <wb:set id="brkCodeList"><%= recparams.get( "brkCodeList" ) == null ? "" : recparams.get( "brkCodeList" ) %></wb:set>

       <wba:table caption="Schedule Export Task Parameters" captionLocalizeIndex="Schedule_Export_Task_Parameters">
            <tr>
                <th>
                    <wb:localize id="SEE_Mapping_Name">Mapping</wb:localize>
                </th>
                <td>
                    <wb:controlField cssClass='inputField' submitName="mapping" ui='StringUI' uiParameter='width=60'><%=(null == mapping ? "" : mapping.toString())%></wb:controlField>
                </td>
            </tr>
            <tr>
                <th>
                    <wb:localize id="File_Name">File Name</wb:localize>
                </th>
                <td>
                    <wb:controlField cssClass='inputField' submitName="exportPath" ui='StringUI' uiParameter='width=60'><%=(null == exportPath ? "" : exportPath.toString())%></wb:controlField>
                </td>
            </tr>
            <tr>
              <th>
                <wb:localize id="Client_Name">Client Name</wb:localize>
              </th>
              <td>
                <wb:controlField cssClass="inputField" submitName="clientId"
                       nullable="false"
                       ui="ComboBoxUI"
                       uiParameter="labelList='#ClientNames#' valueList='#ClientIds#'"><%=clientId.toString()%></wb:controlField>
    
              </td>
            </tr>
            <tr>
                <th>
                    <wb:localize id="SEE_Transmitter_Class">Transmitter Class</wb:localize>
                </th>
                <td>
                    <wb:controlField cssClass='inputField' submitName="transmitterClass" ui='StringUI' uiParameter='width=60'><%=(null == transmitterClass ? "" : transmitterClass.toString())%></wb:controlField>
                </td>
            </tr>
              <tr>
                <th>
                    <wb:localize id="BRK_CODE_LIST">Break Code List</wb:localize>
                </th>
                <td>
                    <wb:controlField cssClass='inputField' submitName="brkCodeList" ui='StringUI' uiParameter='width=60'><%=(null == brkCodeList ? "" : brkCodeList.toString())%></wb:controlField>
                </td>
            </tr>
        </wba:table>       

        <div class="separatorLarge"></div>
        
        <wba:button label="Next" labelLocalizeIndex="Next" onClick="checkSubmit();"/>&nbsp;
        <wba:button label="cancel" labelLocalizeIndex="Cancel" onClick="disableAllButtons(); #okOnClickUrl#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%--Actually Schedulling the Export --%>
        <%
        try {
            String param0 = mapping.toString();
            String param1 = exportPath.toString();
		String param2 = clientId.toString();
            String param3 = transmitterClass.toString();
            String param4 = brkCodeList.toString();
            recparams.put( "mappingName", param0 );
            recparams.put( "exportPath", param1 );
            recparams.put( "clientId", param2 );
            recparams.put( "transmitterClass", param3 );
            recparams.put( "brkCodeList", param4);
            scheduler.setTaskParams(taskId,recparams);

            String forwardPage = null;
            if(!(StringHelper.isEmpty(transmitterClass))) {
                Transmitter transmitter = (Transmitter)Class.forName(transmitterClass.toString()).newInstance();
                forwardPage = transmitter.getTransmitterUI();
            }
            if( StringHelper.isEmpty(forwardPage) ) {
            %> 
                <wb:localize id="Schedule_Export_Successfully_Scheduled">Schedule Export Successfully Scheduled</wb:localize>            
                <br>
                <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
            <%} else { %> 
                <wb:forward page="<%=forwardPage%>" />
                <wba:button type='submit' label='Next' labelLocalizeIndex="Next" onClick='disableAllButtons(); document.page_form.submit();'/>
                <wb:define id="onClickCancel">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>
                <wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="#onClickCancel#"/>
            <%}
        } catch (Exception e) {
            e.printStackTrace();
            %>
            <wb:localize id="Error_Scheduling_HR_Export">Error Scheduling HR Export</wb:localize>            
            <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="disableAllButtons(); #okOnClickUrl#"/>
            <%
        }
        %>
    </wb:case>    
    </wb:switch>
</wb:if>
</wb:page>