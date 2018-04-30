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
<%@ page import="com.workbrain.app.export.process.*"%>
<%@ page import="com.workbrain.app.wbinterface.mapping_rowsource.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="org.apache.log4j.*"%>
<%@ page import="com.workbrain.app.wbinterface.AbstractImportTask"%>
<%@ page import="com.workbrain.security.SecurityService"%>
<%@ page import="com.wbiag.app.wbinterface.pts.PTSExportProcessor"%>


<wb:page login="true">
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>
<wb:define id="offset"><wb:get id="offset" scope="parameter" default=""/></wb:define>
<wb:define id="numberOfDays"><wb:get id="numberOfDays" scope="parameter" default=""/></wb:define>
<wb:define id="mapping"><wb:get id="mapping" scope="parameter" default=""/></wb:define>
<wb:define id="inputRate"><wb:get id="inputRate" scope="parameter" default=""/></wb:define>
<wb:define id="exportPath"><wb:get id="exportPath" scope="parameter" default=""/></wb:define>
<wb:define id="transmitterClass"><wb:get id="transmitterClass" scope="parameter" default=""/></wb:define>
<wb:define id="CLIENT_ID"><wb:get id="CLIENT_ID" scope="parameter" default=""/></wb:define>

<%
Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
ScheduledTaskRecord rec = null;
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
    Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
    if(recparams.isEmpty()) {
    	recparams.put( PTSExportProcessor.PARAM_OFFSET, "0" );
    	recparams.put( PTSExportProcessor.PARAM_NUM_OF_DAYS, "0" );
        recparams.put( "mappingName", "" );        
        recparams.put( "exportPath", "" );        
        recparams.put( "transmitterClass", "com.workbrain.app.export.process.CSVTransmitter" );        
    }
    %>
    <wb:switch> 
    <wb:case expression="#TYPE#" compareToExpression="">
    <input type="hidden" name="TYPE" value="scheduleTask">
    <wb:set id="offset"><%=recparams.get( PTSExportProcessor.PARAM_OFFSET )%></wb:set>           
    <wb:set id="numberOfDays"><%=recparams.get( PTSExportProcessor.PARAM_NUM_OF_DAYS )%></wb:set>           
    <wb:set id="mapping"><%=recparams.get( "mappingName" )%></wb:set>           
    <wb:set id="exportPath"><%=recparams.get( "exportPath" )%></wb:set>       
    <wb:set id="transmitterClass"><%=recparams.get( "transmitterClass" )%></wb:set>
    
       <wba:table caption="PTS Export Task Parameters" captionLocalizeIndex="PTS_Export_Task_Parameters">
            <wba:tr>
                <wba:th>
                    <wb:localize id="Offset">Offset</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="offset" ui='NumberUI' uiParameter='width=4'><%=offset%></wb:controlField>
                </wba:td>
            </wba:tr>
            
            <wba:tr>
                <wba:th>
                    <wb:localize id="Number_Of_Days">Number Of Days</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="numberOfDays" ui='NumberUI' uiParameter='width=4'><%=numberOfDays%></wb:controlField>
                </wba:td>
            </wba:tr>
            
            <wba:tr>
                <wba:th>
                    <wb:localize id="HRE_Mapping_Name">Mapping</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="mapping" ui='StringUI' uiParameter='width=60'><%=mapping%></wb:controlField>
                </wba:td>
            </wba:tr>

            <wba:tr>
                <wba:th>
                    <wb:localize id="Export_Path">Export Path</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="exportPath" ui='StringUI' uiParameter='width=60'><%=exportPath%></wb:controlField>
                </wba:td>
            </wba:tr>
            
            <wba:tr>
                <wba:th>
                    <wb:localize id="Transmitter_Class">Transmitter Class</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="transmitterClass" ui='StringUI' uiParameter='width=60'><%=transmitterClass%></wb:controlField>
                </wba:td>
            </wba:tr>
            
        </wba:table>       

        <div class="separatorLarge"/>
        
        <wba:button type="submit" label="Submit" labelLocalizeIndex="Submit" onClick=""/>&nbsp;
        <wba:button label="cancel" labelLocalizeIndex="Cancel" onClick="#okOnClickUrl#"/>
    </wb:case>
    <wb:case expression="#TYPE#" compareToExpression="scheduleTask">
        <%--Actually Schedulling the Export --%>
        <%
            String param0 = mapping.toString();            
            String param1 = exportPath.toString();
            String param2 = offset.toString();
            String param3 = numberOfDays.toString();
            recparams.put( PTSExportProcessor.PARAM_OFFSET, param2 ); 
            recparams.put( PTSExportProcessor.PARAM_NUM_OF_DAYS, param3 ); 
            recparams.put( "mappingName", param0 );            
            recparams.put( "exportPath", param1 );
            recparams.put( "transmitterClass", transmitterClass.toString() );      
            recparams.put(AbstractImportTask.CLIENT_ID_PARAM_NAME, SecurityService.getCurrentClientId());           
            scheduler.setTaskParams(Integer.parseInt(TASK_ID.toString()), recparams);
            String forwardPage = null;
            if(transmitterClass!=null &&  "".equals(transmitterClass.toString())) {
                Transmitter transmitter = (Transmitter)Class.forName(transmitterClass.toString()).newInstance();
                forwardPage = transmitter.getTransmitterUI();
            }
            if( forwardPage==null || "".equals(forwardPage) ) {
            %>
                <Span>PTS Export Successfully Scheduled</Span>                
                <br>
                <wba:button label="OK" labelLocalizeIndex="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>
            <%
            } else {
            %>
                <wb:forward page="<%=forwardPage%>" />
                <wba:button type='submit' label='Next' labelLocalizeIndex="Next" onClick=''/>&nbsp;                
                 <wba:button label="Cancel" labelLocalizeIndex="Cancel" onClick="#onClickCancel#"/>
            <%}%>
    </wb:case>  
    </wb:switch>
</wb:if>
</wb:page>