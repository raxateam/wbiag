<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.wbiag.app.jsp.shiftpatternresolver.ShiftPatternResolverExt" %>
<%@ page import="com.workbrain.app.jsp.shiftpatternresolver.ShiftPatternConstants" %>


<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>' subsidiaryPage='true'>

<wb:define id="contextPath"><%= request.getContextPath() %></wb:define>
<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>

<wb:define id="create1"><%=ShiftPatternConstants.ACTION_CREATE%></wb:define>
<wb:define id="create_assign1"><%=ShiftPatternConstants.ACTION_CREATE_ASSIGN%></wb:define>

<jsp:useBean id="spre" scope="request" type="com.wbiag.app.jsp.shiftpatternresolver.ShiftPatternResolverExt"/>

<wb:define id="disableLaborMetrics"><%=spre.getDisableLaborMetrics()%></wb:define>

<%
    String mfrmIdStr = mfrm_id.toString();
    boolean toProcess = new Boolean(request.getParameter("toProcess")).booleanValue();
%>

<tr><td>
    <a name="process_anchor">
    <table class="contentTable" bgcolor="#ffffff" border="1">
	<wb:secureContent securityName='DAE_SEC_TABLE_THIS_WILL_RESOLVE'>
	<% if(!spre.getDisableCreateButton()){ %>
    <tr><td colspan=2><hr color="navy"></td></tr>
    <tr>
        <td><% if(toProcess) { %>
            <wba:button label="Create" disabled="true" labelLocalizeIndex="SPR_Create" onClick=""/> 
        <% } else { %>
            <wba:button label="Create" labelLocalizeIndex="SPR_Create" onClick="startProcess('#create1#', '#disableLaborMetrics#')"/>
        <% } %>
        </td>
        <td><font color="navy"><wb:localize id='SPR_Create_Desc' overrideId="<%=mfrmIdStr%>"> This will resolve the shift pattern specified and creates a new one if another one with the same specifications doesn't exist.</wb:localize></font></td>
    </tr>
	<% } %>
</wb:secureContent>
	
	<% if(!spre.getDisableCreateAssignButton()){ %>
    <tr><td colspan=2><hr color="navy"></td></tr>
    <tr>
        <td><% if(toProcess) { %>
            <wba:button label="Create & Assign" disabled="true" labelLocalizeIndex="SPR_CreateAssign" onClick=""/> 
        <% } else { %>
            <wba:button label="Create & Assign" labelLocalizeIndex="SPR_CreateAssign" onClick="startProcess('#create_assign1#', '#disableLaborMetrics#')"/>
        <% } %>
        </td>
        <td><font color="navy"><wb:localize id='SPR_CreateAssign_Desc' overrideId="<%=mfrmIdStr%>"> This will create the shift pattern specified, if one doesn't already exist, and assign it to the employee(s) selected above</wb:localize></font></td>
    </tr>
	<% } %>
    </table>
    </a>
</td></tr>

</table>


</wb:page>