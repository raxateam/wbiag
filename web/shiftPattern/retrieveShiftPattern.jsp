<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.wbiag.app.jsp.shiftpatternresolver.ShiftPatternResolverExt" %>
<%@ page import="com.workbrain.app.jsp.shiftpatternresolver.ShiftPatternConstants" %>


<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>

<wb:define id="shftPatRetrieve"><%=ShiftPatternResolverExt.ACTION_SHFT_PAT_RETRIEVE%></wb:define>
<wb:define id="assign1"><%=ShiftPatternConstants.ACTION_ASSIGN%></wb:define>
<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>

<%
    String mfrmIdStr = mfrm_id.toString();
    boolean toProcess = new Boolean(request.getParameter("toProcess")).booleanValue();

if(JSPHelper.getWebSession(pageContext.getRequest()).isConfigOn()) {%>
	<tr><td><wb:secureContent securityName='DAE_SEC_SELECT_EXISTING_PATTERN'></wb:secureContent></td></tr>
<% } %>
<wb:secureContent securityName='DAE_SEC_SELECT_EXISTING_PATTERN'>
    <tr><td bgcolor="#D4DCF5"><font color="navy"><wb:localize id='SPR_OldShiftPattern_Desc' overrideId="<%=mfrmIdStr%>"> To assign an EXISTING Shift Pattern, select a Shift Pattern </wb:localize></font></td></tr>
    <tr><td>
        <table class="contentTable" bgcolor="#ffffff" border="1">
        <tr>
            <td><font color="navy"><wb:localize id='Shift_Pattern' overrideId="<%=mfrmIdStr%>" type="field"> Shift Pattern </wb:localize></font></td>
            <td>
                <wb:controlField cssClass="inputField" id='Shift_Pattern'  overrideId="<%=mfrmIdStr%>" submitName="existShiftPatternName"><%= request.getParameter("existShiftPatternName") %></wb:controlField></td>
        </tr>
        <tr>
            <td>
            <% if(toProcess) { %>
                <wba:button label="Assign" disabled="true" labelLocalizeIndex="SPR_Assign" onClick=""/> 
            <% } else { %>
                <wba:button label="Assign" labelLocalizeIndex="SPR_Assign" onClick="startProcess('#assign1#')"/>
            <% } %>
            </td>
            
            <td>
            <% if(toProcess) { %>
                <wba:button label="Retrieve" disabled="true" labelLocalizeIndex="SPR_ShftPatRetrieve" onClick=""/> 
            <% } else { %>
                <wba:button label="Retrieve" labelLocalizeIndex="SPR_ShftPatRetrieve" onClick="startRetrieve('#shftPatRetrieve#')"/>
            <% } %>
            </td>
            
            <td></td>
        </tr>
        </table>
    </td></tr>
</wb:secureContent>

</wb:page>