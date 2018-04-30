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
<%@ page import="com.wbiag.app.wbinterface.forecastexport.ForecastExportDataSource"%>
<%@ page import="com.workbrain.util.StringHelper"%>

<wb:page login="true">
<wb:define id="TYPE"><wb:get id="TYPE" scope="parameter" default=""/></wb:define>
<wb:define id="TASK_ID"><wb:get id="TASK_ID" scope="parameter" default=""/></wb:define>
<wb:submit id="TASK_ID"><wb:get id="TASK_ID"/></wb:submit>
<wb:define id="okOnClickUrl">window.location='<%= request.getContextPath() %>/jobs/ent/jobs/schedules.jsp';</wb:define>
<wb:define id="mapping"><wb:get id="mapping" scope="parameter" default=""/></wb:define>
<wb:define id="transmitterClass"><wb:get id="transmitterClass" scope="parameter" default=""/></wb:define>
<wb:define id="CLIENT_ID"><wb:get id="CLIENT_ID" scope="parameter" default=""/></wb:define>

<wb:define id="startOfWeek"><wb:get id="startOfWeek" scope="parameter" default="Sunday"/></wb:define>
<wb:define id="volumeType"><wb:get id="volumeType" scope="parameter" default=""/></wb:define>
<wb:define id="weekOffset"><wb:get id="weekOffset" scope="parameter" default="0"/></wb:define>
<wb:define id="numOfWeeks"><wb:get id="numOfWeeks" scope="parameter" default="1"/></wb:define>
<wb:define id="aggregateLevel"><wb:get id="aggregateLevel" scope="parameter" default="Day"/></wb:define>

<%
Scheduler scheduler = (Scheduler)GOPObjectFactory.createScheduler();
ScheduledTaskRecord rec = null;
%>
<wb:if expression="#TASK_ID#" compareToExpression="" operator="<>">
    <%
    rec = scheduler.getTask(Integer.parseInt(TASK_ID.toString()));
    Map recparams = scheduler.getTaskParams(Integer.parseInt(TASK_ID.toString()));
    
    //set default values if empty
    String weekOffsetStr = (String)recparams.get( ForecastExportDataSource.PARAM_WEEK_OFFSET );
    String numOfWeeksStr = (String)recparams.get( ForecastExportDataSource.PARAM_NUM_OF_WEEKS );
    String startOfWeekStr = (String)recparams.get( ForecastExportDataSource.PARAM_START_OF_WEEK );
    String aggregateLevelStr = (String)recparams.get( ForecastExportDataSource.PARAM_AGGREGATE_LEVEL );
    String transmitterClassStr = (String)recparams.get( "transmitterClass" );    
    String mappingNameStr = (String)recparams.get( "mappingName" );
    
    if(StringHelper.isEmpty(weekOffsetStr))
    {
    	recparams.put( ForecastExportDataSource.PARAM_WEEK_OFFSET, "0" );
    }
    if(StringHelper.isEmpty(numOfWeeksStr))
    {
    	recparams.put( ForecastExportDataSource.PARAM_NUM_OF_WEEKS, "1" );
    }
    if(StringHelper.isEmpty(startOfWeekStr))
    {
    	recparams.put( ForecastExportDataSource.PARAM_START_OF_WEEK, "Sunday" );
    }
    if(StringHelper.isEmpty(aggregateLevelStr))
    {
    	recparams.put( ForecastExportDataSource.PARAM_AGGREGATE_LEVEL, "Day" );
    }
    if(StringHelper.isEmpty(mappingNameStr))
    {
    	recparams.put( "mappingName", "FORECAST EXPORT MAPPING" );
    }
    if(StringHelper.isEmpty(transmitterClassStr))
    {
    	recparams.put( "transmitterClass", "com.wbiag.app.wbinterface.forecastexport.ForecastExportTransmitter" );
    }
    
    if(recparams.isEmpty()) 
    {    	
    	recparams.put( ForecastExportDataSource.PARAM_WEEK_OFFSET, "0" );
    	recparams.put( ForecastExportDataSource.PARAM_NUM_OF_WEEKS, "1" );        
    	recparams.put( ForecastExportDataSource.PARAM_START_OF_WEEK, "Sunday" );
    	recparams.put( ForecastExportDataSource.PARAM_AGGREGATE_LEVEL, "Day" );        
        recparams.put( "mappingName", "FORECAST EXPORT MAPPING" );        
        recparams.put( "transmitterClass", "com.wbiag.app.wbinterface.forecastexport.ForecastExportTransmitter" );                        
    }
    %>
    <wb:switch> 
    <wb:case expression="#TYPE#" compareToExpression="">
    <input type="hidden" name="TYPE" value="scheduleTask">
    <wb:set id="volumeType"><%=recparams.get( ForecastExportDataSource.PARAM_VOLUME_TYPE )%></wb:set>           
    <wb:set id="weekOffset"><%=recparams.get( ForecastExportDataSource.PARAM_WEEK_OFFSET )%></wb:set>           
    <wb:set id="numOfWeeks"><%=recparams.get( ForecastExportDataSource.PARAM_NUM_OF_WEEKS )%></wb:set>                      
    <wb:set id="startOfWeek"><%=recparams.get( ForecastExportDataSource.PARAM_START_OF_WEEK )%></wb:set>                      
    <wb:set id="aggregateLevel"><%=recparams.get( ForecastExportDataSource.PARAM_AGGREGATE_LEVEL )%></wb:set>                          
    <wb:set id="mapping"><%=recparams.get( "mappingName" )%></wb:set>           
    <wb:set id="transmitterClass"><%=recparams.get( "transmitterClass" )%></wb:set>
    
       <wba:table caption="Forecast Export Parameters" captionLocalizeIndex="Forecast_Export_Parameters">
            <wba:tr>
                <wba:th>
                    <wb:localize id="VolumeType">Volume Type</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="volumeType" ui='DBLookupUI' uiParameter='sourceType=SQL source="select VOLTYP_ID, VOLTYP_NAME from so_volume_type" width=10'><%=volumeType%></wb:controlField>
                </wba:td>
            </wba:tr>
            
            <wba:tr>
                <wba:th>
                    <wb:localize id="WeekOffset">Export Week</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="weekOffset" ui='NumberUI' uiParameter='width=4'><%=weekOffset%></wb:controlField>
                </wba:td>
            </wba:tr>
            
            <wba:tr>
                <wba:th>
                    <wb:localize id="NumOfWeeks">Number of Weeks to Export</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="numOfWeeks" ui='NumberUI' uiParameter='width=4'><%=numOfWeeks%></wb:controlField>
                </wba:td>
            </wba:tr>           

			<wba:tr>
                <wba:th>
                    <wb:localize id="StartOfWeek">Start of Week</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="startOfWeek" ui='ComboBoxUI' uiParameter='valueList="Sunday,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday" labelList="Sunday,Monday,Tuesday,Wednesday,Thursday,Friday,Saturday"'><%=startOfWeek%></wb:controlField>
                </wba:td>
            </wba:tr>           

			<wba:tr>
                <wba:th>
                    <wb:localize id="AggregateLevel">Aggregate Level</wb:localize>
                </wba:th>
                <wba:td>
                    <wb:controlField cssClass='inputField' submitName="aggregateLevel" ui='ComboBoxUI' uiParameter='valueList="Day,Week" labelList="Day,Week"'><%=aggregateLevel%></wb:controlField>
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
            recparams.put( ForecastExportDataSource.PARAM_VOLUME_TYPE, volumeType.toString() ); 
            recparams.put( ForecastExportDataSource.PARAM_WEEK_OFFSET, weekOffset.toString() );      
            recparams.put( ForecastExportDataSource.PARAM_NUM_OF_WEEKS, numOfWeeks.toString() );      
            recparams.put( ForecastExportDataSource.PARAM_START_OF_WEEK, startOfWeek.toString() );      
            recparams.put( ForecastExportDataSource.PARAM_AGGREGATE_LEVEL, aggregateLevel.toString() );                  
            recparams.put( "mappingName", mapping.toString() );      
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
                <Span>Forecast Export Successfully Scheduled</Span>                
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