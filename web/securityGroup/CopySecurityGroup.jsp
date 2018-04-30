<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.io.*"%>
<%@ page import="java.util.*"%>
<%@ page import="java.sql.*"%>
<%@ page import="javax.naming.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="java.lang.reflect.*"%>
<%@ page import="javax.servlet.jsp.JspException" %>
<%@ page import="com.wbiag.app.ta.db.CopySecurityGroup"%>
<%-- ********************** --%>
<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>


    <wb:define id="GO_TO"><wb:get id="GO_TO" scope="parameter" default="/securityGroup/CopySecurityGroup.jsp"/></wb:define>
    <wb:submit id="GO_TO"><wb:get id="GO_TO"/></wb:submit>

    <wb:define id="OPERATION"><wb:get id="OPERATION" scope="parameter" default=""/></wb:define>
    <wb:submit id="OPERATION">SUBMIT</wb:submit>

    <wb:define id="OLDSECURITYGROUP"><wb:get id="OLDSECURITYGROUP" scope="parameter" default=""/></wb:define>
    <wb:define id="NEWSECURITYGROUP"><wb:get id="NEWSECURITYGROUP" scope="parameter" default=""/></wb:define>
    <wb:define id="OVERWRITE"><wb:get id="OVERWRITE" scope="parameter" default=""/></wb:define>

    <wb:define id="okOnClickUrl">window.location='#GO_TO#';</wb:define>
    <wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>
    <%
    if("SUBMIT".equals(OPERATION.toString()))
    {
        boolean flag = true;
        CopySecurityGroup csg = new CopySecurityGroup();
            try
            {
                if(OVERWRITE.toString().equals("Y"))
                {                   
                    csg.Overwrite(OLDSECURITYGROUP.toString(), NEWSECURITYGROUP.toString(), JSPHelper.getConnection(request));
                }
                else
                {
                    csg.Copy(OLDSECURITYGROUP.toString(), NEWSECURITYGROUP.toString(), JSPHelper.getConnection(request));
                }
            }
            catch(SQLException e)
            {
                flag = false;
                %>
                <span>Unable to create security group.  The following error occured:</span>
                <P><%=e%></P>
                <wba:button label="Try again" intensity="low" onClick="#okOnClickUrl#"/>

            <%}
        if(flag)
        {
        %>
            <span>Security group <I><%=NEWSECURITYGROUP.toString().toUpperCase()%></I> created.</span>
            <BR><wba:button label="OK" size="small" intensity="low" onClick="#okOnClickUrl#"/>

        <%
        }
    }
    else
    {
    %>

        <%
        String mfrmIdStr = mfrm_id.toString();
        //If the maintenance form ID is null, retrieve it from the database.
        if (mfrmIdStr.equals(""))
        {
            DBConnection dbc = JSPHelper.getConnection(request);
            PreparedStatement ps = null;
            ResultSet rs = null;
            ps = dbc.prepareStatement( "SELECT * " +
                                     "FROM maintenance_form " +
                                     "WHERE mfrm_jsp = ? ");
            ps.setString(1,"/securityGroup/CopySecurityGroup.jsp");
            rs = ps.executeQuery();
            rs.next();
            mfrmIdStr = rs.getString("mfrm_id");
            rs.close();
            rs = null;
            ps.close();
            ps = null;        
        }
        %>
    <wba:table caption="Copy Security Group <%=mfrmIdStr%>" captionLocalizeIndex="CopySecurityGroup" >
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="Old_Security_Group" overrideId="<%=mfrmIdStr%>" type="field">Existing Security Group Name </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField cssClass="inputField" id='Old_Security_Group' overrideId="<%=mfrmIdStr%>" submitName="OLDSECURITYGROUP"><%= request.getParameter("OLDSECURITYGROUP") %></wb:controlField>
<%/* Hardcoded DBLookup
                <wb:controlField submitName="OLDSECURITYGROUP" ui="DBLookupUI" cssClass="inputField" uiParameter="width=50 sourceType=SQL source='SELECT WBG_ID, WBG_NAME FROM WORKBRAIN_GROUP' sourceKeyField=wbg_name sourceLabelField=wbg_name"><wb:get id="OLDSECURITYGROUP"/></wb:controlField>
*/%>               
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="New_Security_Group" overrideId="<%=mfrmIdStr%>" type="field">New Security Group Name </wb:localize>
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField cssClass="inputField" id='New_Security_Group' overrideId="<%=mfrmIdStr%>" submitName="NEWSECURITYGROUP"><%= request.getParameter("NEWSECURITYGROUP") %></wb:controlField>
            </wba:td>
        </wba:tr>
        <wba:tr>
            <wba:th width='40%'> 
                <wb:localize id="Overwrite_Security_Group" type="field">Overwrite Existing Security Group </wb:localize>	
            </wba:th>
            <wba:td width='60%'>
                <wb:controlField ui='CheckboxUI' id='Overwrite' submitName="OVERWRITE"/>
            </wba:td>
        </wba:tr>

    </wba:table>


    <wba:button type='submit' label='Submit' labelLocalizeIndex='Submit' onClick=''/>

    <%}%>
</wb:page>