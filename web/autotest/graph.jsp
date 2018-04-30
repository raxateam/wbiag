<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.File"%>
<%@ page import="java.text.NumberFormat"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.Date"%>
<%@ page import="java.sql.PreparedStatement"%>
<%@ page import="java.sql.ResultSet"%>

<%@ page import="com.workbrain.sql.SQLHelper"%>
<%@ page import="com.workbrain.sql.DBConnection"%>
<%@ page import="com.workbrain.util.DateHelper"%>
<%@ page import="com.workbrain.server.jsp.JSPHelper"%>
<%@ page import="com.wbiag.tool.autotest.chart.*"%>

<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>
<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>

<%  
    //params    
    String atParentName = null;
    String thresholdStr = null;
    String atName = null;
    String fromDateStr = null;
    String toDateStr = null;
    String startWindow = null;      
    String endWindow = null;
    String realPath = null; 
    String chartViewStr = null;
    String timePrecision = null;
    String timeUnit = null;
    boolean chartView = false;
    int atId = 0;   
    int atParentId = 0;
    int threshold = 0;  

    //get real path 
    realPath = application.getRealPath("/autotest");
    
    //get parameters from request
    atId = Integer.parseInt(request.getParameter("atId"));          

    thresholdStr = request.getParameter("AtThreshold");
    if(thresholdStr != null && !"".equals(thresholdStr))
    {
        threshold = Integer.parseInt(thresholdStr);
    }
    
    fromDateStr = request.getParameter("AtFromDate");
    toDateStr = request.getParameter("AtToDate");
    
    startWindow = request.getParameter("AtStartWindow");
    endWindow = request.getParameter("AtEndWindow");    
    
    chartViewStr = request.getParameter("AtView");
    if("2".equals(chartViewStr))
    {
        chartView = true;
    }
    
    //get time precision and unit from registry
    timePrecision = ChartUtil.getRegistryValue(ChartUtil.REG_TIME_PRECISION);
    timeUnit = ChartUtil.getRegistryValue(ChartUtil.REG_TIME_UNIT);
    
    //generate chart based on parameters
    DBConnection conn = JSPHelper.getConnection(request);       
    GenerateCharts genCharts = new GenerateCharts();        
    String filename = genCharts.GenerateXYChart(conn, realPath, atId, fromDateStr, toDateStr, startWindow, endWindow, timePrecision, timeUnit, threshold, chartView);        
    atParentId = genCharts.getAtParentId();
    atParentName = genCharts.getAtParentName();     
%>

<input type=HIDDEN name="atId" value="<%=atId%>">

    <wba:table caption="Auto Test Details"> 
        <wba:tr>
            <wba:th> 
                Test Name:
            </wba:th>
            <wba:td> 
                <%=genCharts.getTestName() %>
            </wba:td>                       
        </wba:tr>
        <wba:tr>
            <wba:th> 
                Last Run Date:
            </wba:th>
            <wba:td> 
                <%=genCharts.getLastRun() %>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th> 
                
<%
if("ms".equalsIgnoreCase(timeUnit))
{
%>
                Last Run Duration (ms):
<%
}
else
{
%>            
                Last Run Duration (s):
<%
}
%>
            </wba:th>
            <wba:td> 
                <%=genCharts.getLastRunDuration() %>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th> 
                Number of Runs:
            </wba:th>
            <wba:td> 
                <%=genCharts.getNumOfPoints() %>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th> 
<%
if("ms".equalsIgnoreCase(timeUnit))
{
%>
                Average Duration (ms):
<%
}
else
{
%>            
                Average Duration (s):
<%
}
%>
            </wba:th>
            <wba:td> 
                <%=genCharts.getDurationAvg() %>
            </wba:td>
        </wba:tr>
    </wba:table>

    <wba:table caption="Auto Test Graph">   
        <wba:tr>
            <wba:th> 
                <wb:localize id="AT_From_Date">From Date</wb:localize>
            </wba:th>
            <wba:th> 
                <wb:localize id="AT_To_Date">To Date</wb:localize>
            </wba:th>
            <wba:th> 
                <wb:localize id="AT_Start_Window">Start Window</wb:localize>
            </wba:th>
            <wba:th> 
                <wb:localize id="AT_End_Window">End Window</wb:localize>
            </wba:th>
            <wba:th> 
                <wb:localize id="AT_Threshold">Threshold</wb:localize>
            </wba:th>
            <wba:th> 
                <wb:localize id="AT_View">View</wb:localize>
            </wba:th>
            <wba:th>                 
            </wba:th>
        </wba:tr>
        <wba:tr>    
            <wba:td>
                <wb:controlField submitName="AtFromDate" ui="DatePickerUI"><%=request.getParameter("AtFromDate")%></wb:controlField>                  
            </wba:td>
            <wba:td>
                <wb:controlField submitName="AtToDate" ui="DatePickerUI"><%=request.getParameter("AtToDate")%></wb:controlField>
            </wba:td>
            <wba:td>
                <wb:controlField submitName="AtStartWindow" ui="TimeEditUI" uiParameter="look=box"><%if(request.getParameter("AtStartWindow") != null){%><%=request.getParameter("AtStartWindow")%><%}%></wb:controlField>                
            </wba:td>
            <wba:td>
                <wb:controlField submitName="AtEndWindow" ui="TimeEditUI" uiParameter="look=box"><%if(request.getParameter("AtEndWindow") != null){%><%=request.getParameter("AtEndWindow")%><%}%></wb:controlField>                
            </wba:td>
            <wba:td>
                <wb:controlField submitName="AtThreshold" ui="NumberUI" uiParameter="width=7"><%if(request.getParameter("AtThreshold") != null){%><%=request.getParameter("AtThreshold")%><%}%></wb:controlField>                                
            </wba:td>
            <wba:td>
                <wb:controlField submitName="AtView" ui="ComboBoxUI" uiParameter="labelList='Lines,Points' valueList='1,2'"><%=request.getParameter("AtView")%></wb:controlField>                                                
            </wba:td>
            <wba:td>
                <wba:button type='submit' label='Graph' labelLocalizeIndex='Graph' onClick=''/>
            </wba:td>
        </wba:tr>
        <wba:tr>  
            <img src="<%= filename %>" width=600 height=480 border=0>
        </wba:tr>                      
    </wba:table>
    
    <wba:table caption="Sub Tests">
        <wba:tr>
            <wba:th> 
                Test Name
            </wba:th>
            <wba:th>
                Test Desc
            </wba:th>
            <wba:th>
                Step #
            </wba:th>
            <wba:th>
                Details
            </wba:th>
        </wba:tr>

    <%              
            PreparedStatement ps = null;
            ResultSet rs = null;
            StringBuffer sql = null;
            Date testDateTime = null;
            int subAtId = 0;
            String subAtName = null;
            String atDesc = null;
            String graphURL = "graph.jsp";
            int atStep = 0;
            double testDuration = 0;
                        
            sql = new StringBuffer();
            
            sql.append(" SELECT ");
            sql.append(" WAT_ID, ");
            sql.append(" WAT_NAME, ");
            sql.append(" WAT_DESC, ");
            sql.append(" WAT_STEP ");
            sql.append(" FROM ");
            sql.append(" WBIAG_AUTOTEST ");  
            sql.append(" WHERE ");
            sql.append(" WAT_PARENT_ID = ? ");
            sql.append(" ORDER BY ");
            sql.append(" WAT_ID ");      
            
            try
            {
                ps = conn.prepareStatement(sql.toString());                 
                ps.setInt(1, atId);
                
                rs = ps.executeQuery();
                while(rs.next())
                {
                    subAtId = rs.getInt(1);
                    subAtName = rs.getString(2);
                    atDesc = rs.getString(3);
                    atStep = rs.getInt(4);                              

%>
    <wba:tr>
        <wba:td> 
            <%=subAtName%>
        </wba:td>
        <wba:td>
            <%=atDesc%>
        </wba:td>
        <wba:td>
            <%=atStep%>
        </wba:td>
        <wba:td>
            <A href=<%=graphURL + "?atId="+subAtId%>>Show Details</A>
        </wba:td>
    </wba:tr>
<%                             
                }                       
            }
            finally
            {
                SQLHelper.cleanUp(ps, rs);
            }   
    %>      

        
    </wba:table>
    
<table> 
<tr><td></td></tr>  
<%
if(!"PARENT".equals(atParentName))
{
%>
<tr><td><A href=<%="graph.jsp?atId="+atParentId%>>Back to Parent Test</A></td></tr>
<%
}
%>

<tr><td></td></tr>  
<tr><td><A href=<%="index.jsp"%>>Back to Main Page</A></td></tr>
</table>

</wb:page>