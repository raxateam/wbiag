<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.wbiag.app.jsp.shiftpatternresolver.ShiftPatternResolverExt" %>
<%@ page import="com.workbrain.app.jsp.shiftpatternresolver.ShiftPatternConstants" %>


<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>

<wb:define id="retrieve"><%=ShiftPatternResolverExt.ACTION_RETRIEVE%></wb:define>
<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>
<wb:define id="dateFormat">MM/dd/yyyy</wb:define>

<%
    String mfrmIdStr = mfrm_id.toString();
    boolean toProcess = new Boolean(request.getParameter("toProcess")).booleanValue();

if(JSPHelper.getWebSession(pageContext.getRequest()).isConfigOn()) {%>
	<tr><td><wb:secureContent securityName='DAE_SEC_LOOKUP_PATTERN_FOR_EMPLOYEE'></wb:secureContent></td></tr>
<% } %>
<wb:secureContent securityName='DAE_SEC_LOOKUP_PATTERN_FOR_EMPLOYEE'>
<tr><td bgcolor="#D4DCF5"><font color="navy"><wb:localize id='SPR_LookupShiftPattern_Desc' overrideId="<%=mfrmIdStr%>"> To LOOKUP a Shift Pattern for an employee, select an employee and specify the date </wb:localize></font></td></tr>
<tr><td>
    <table class="contentTable" bgcolor="#ffffff" border="1">
        <tr>
            <td><font color="navy"><wb:localize id='Retrieve_Employee' overrideId="<%=mfrmIdStr%>" type="field"> Employee </wb:localize></font></td>
            <td><wb:controlField cssClass="inputField" id='Retrieve_Employee' overrideId="<%=mfrmIdStr%>" submitName="retEmpName"><%= request.getParameter("retEmpName") %></wb:controlField></td>
        </tr>       
        <tr>            
            <td><font color="navy"><wb:localize id="Retrieve_Date" overrideId="<%=mfrmIdStr%>" type="field"> Date </wb:localize></font></td>           
            <td><wb:controlField submitName="retStartDate" id='Retrieve_Date' cssClass="inputField" overrideId="<%=mfrmIdStr%>"><%= request.getParameter("retStartDate") %></wb:controlField></td>
        </tr>           
        <tr>
        <td>
        <% if(toProcess) { %>
            <wba:button label="Retrieve" disabled="true" labelLocalizeIndex="SPR_Retrieve" onClick=""/> 
        <% } else { %>
            <wba:button label="Retrieve" labelLocalizeIndex="SPR_Retrieve" onClick="startRetrieve('#retrieve#')"/>
        <% } %>
        </td>        
        </tr>
    </table>
</td></tr>  
</wb:secureContent>
</td></tr>

</wb:page>