<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.File"%>
<%@ page import="java.text.NumberFormat"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.Date"%>
<%@ page import="java.sql.PreparedStatement"%>
<%@ page import="java.sql.ResultSet"%>
<%@ page import="javax.servlet.*"%>

<%@ page import="com.workbrain.sql.SQLHelper"%>
<%@ page import="com.workbrain.sql.DBConnection"%>
<%@ page import="com.workbrain.util.DateHelper"%>
<%@ page import="com.workbrain.server.jsp.JSPHelper"%>
<%@ page import="com.wbiag.tool.autotest.chart.*"%>

<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>
<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>

<wba:table caption="Auto Test">
    <wba:tr>
        <wba:th> 
            Test Name
        </wba:th>
        <wba:th>
            Test Desc
        </wba:th>
        <wba:th>
            Details
        </wba:th>
    </wba:tr>
    
<%  
        DBConnection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuffer sql = null;
        Date testDateTime = null;        
        String atName = null;
        String atDesc = null;
        String graphURL = "graph.jsp";
        double testDuration = 0;
        int atStep = 0;
        int atId = 0;                
        
        //clean up old img files        
        ChartUtil.deleteFiles(application.getRealPath("/autotest"), ".jpeg");
                
        //list test info
        conn = JSPHelper.getConnection(request);
        sql = new StringBuffer();
        
        sql.append(" SELECT "); 
        sql.append(" A1.WAT_ID, "); 
        sql.append(" A1.WAT_NAME, "); 
        sql.append(" A1.WAT_DESC, "); 
        sql.append(" A1.WAT_STEP "); 
        sql.append(" FROM "); 
        sql.append(" WBIAG_AUTOTEST A1, ");
        sql.append(" WBIAG_AUTOTEST A2 ");
        sql.append(" WHERE "); 
        sql.append(" A1.WAT_NAME != 'PARENT' ");
        sql.append(" AND "); 
        sql.append(" A1.WAT_PARENT_ID = A2.WAT_ID ");
        sql.append(" AND ");
        sql.append(" A2.WAT_NAME = 'PARENT' "); 
        sql.append(" ORDER BY "); 
        sql.append(" WAT_ID ");     
        
        try
        {
            ps = conn.prepareStatement(sql.toString());                 
            
            rs = ps.executeQuery();
            while(rs.next())
            {
                atId = rs.getInt(1);
                atName = rs.getString(2);
                atDesc = rs.getString(3);
                atStep = rs.getInt(4);                                              
%>
    <wba:tr>
        <wba:td> 
            <%=atName%>
        </wba:td>
        <wba:td>
            <%=atDesc%>
        </wba:td>
        <wba:td>
            <A href=<%=graphURL + "?atId="+atId%>>Show Details</A>
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

</wb:page>