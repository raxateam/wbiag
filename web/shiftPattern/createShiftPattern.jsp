<%@ include file="/system/wbheader.jsp"%>

<%@ page import="com.wbiag.app.jsp.shiftpatternresolver.ShiftPatternResolverExt" %>
<%@ page import="com.workbrain.app.jsp.shiftpatternresolver.ShiftPatternConstants" %>

<wb:page maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>

<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>
<wb:define id="paint1"><%=ShiftPatternConstants.ACTION_PAINT%></wb:define>
<wb:define id="dateFormat">MM/dd/yyyy</wb:define>

<%
    String mfrmIdStr = mfrm_id.toString();
    boolean toProcess = new Boolean(request.getParameter("toProcess")).booleanValue();
    String newShiftPatternName = request.getParameter("newShiftPatternName");
    boolean enableSetDays = new Boolean(request.getParameter("enableSetDays")).booleanValue();
    int numberOfDays = Integer.parseInt(request.getParameter("numberOfDays"));
    String maxNumDays = request.getParameter("maxNumDays");
    String newShiftPatternDesc = request.getParameter("newShiftPatternDesc");
    String effectiveDate = request.getParameter("effectiveDate");    
    String dayStartTime = request.getParameter("dayStartTime");    
    String newShiftPatternFlag1 = request.getParameter("newShiftPatternFlag1");
    String newShiftPatternFlag2 = request.getParameter("newShiftPatternFlag2");
    String newShiftPatternFlag3 = request.getParameter("newShiftPatternFlag3");
    String newShiftPatternFlag4 = request.getParameter("newShiftPatternFlag4");
    String newShiftPatternFlag5 = request.getParameter("newShiftPatternFlag5");
    String newShiftPatternUdf1 = request.getParameter("newShiftPatternUdf1");
    String newShiftPatternUdf2 = request.getParameter("newShiftPatternUdf2");
    String newShiftPatternUdf3 = request.getParameter("newShiftPatternUdf3");
    String newShiftPatternUdf4 = request.getParameter("newShiftPatternUdf4");
    String newShiftPatternUdf5 = request.getParameter("newShiftPatternUdf5");
    String newShiftPatternVal1 = request.getParameter("newShiftPatternVal1");
    String newShiftPatternVal2 = request.getParameter("newShiftPatternVal2");
    String newShiftPatternVal3 = request.getParameter("newShiftPatternVal3");
    String newShiftPatternVal4 = request.getParameter("newShiftPatternVal4");
    String newShiftPatternVal5 = request.getParameter("newShiftPatternVal5");
    boolean showShiftPattern_Name = new Boolean(request.getParameter("showShiftPattern_Name")).booleanValue();    
    boolean showShiftPattern_Desc = new Boolean(request.getParameter("showShiftPattern_Desc")).booleanValue();    
    boolean showEffective_Date = new Boolean(request.getParameter("showEffective_Date")).booleanValue();    
    boolean showDay_Start_Time = new Boolean(request.getParameter("showDay_Start_Time")).booleanValue();    
    boolean showShiftPattern_Flag1 = new Boolean(request.getParameter("showShiftPattern_Flag1")).booleanValue();    
    boolean showShiftPattern_Flag2 = new Boolean(request.getParameter("showShiftPattern_Flag2")).booleanValue();    
    boolean showShiftPattern_Flag3 = new Boolean(request.getParameter("showShiftPattern_Flag3")).booleanValue();    
    boolean showShiftPattern_Flag4 = new Boolean(request.getParameter("showShiftPattern_Flag4")).booleanValue();    
    boolean showShiftPattern_Flag5 = new Boolean(request.getParameter("showShiftPattern_Flag5")).booleanValue();    
    boolean showShiftPattern_Udf1 = new Boolean(request.getParameter("showShiftPattern_Udf1")).booleanValue();    
    boolean showShiftPattern_Udf2 = new Boolean(request.getParameter("showShiftPattern_Udf2")).booleanValue();    
    boolean showShiftPattern_Udf3 = new Boolean(request.getParameter("showShiftPattern_Udf3")).booleanValue();    
    boolean showShiftPattern_Udf4 = new Boolean(request.getParameter("showShiftPattern_Udf4")).booleanValue();    
    boolean showShiftPattern_Udf5 = new Boolean(request.getParameter("showShiftPattern_Udf5")).booleanValue();  
    boolean showShiftPattern_Val1 = new Boolean(request.getParameter("showShiftPattern_Val1")).booleanValue();      
    boolean showShiftPattern_Val2 = new Boolean(request.getParameter("showShiftPattern_Val2")).booleanValue();      
    boolean showShiftPattern_Val3 = new Boolean(request.getParameter("showShiftPattern_Val3")).booleanValue();      
    boolean showShiftPattern_Val4 = new Boolean(request.getParameter("showShiftPattern_Val4")).booleanValue();      
    boolean showShiftPattern_Val5 = new Boolean(request.getParameter("showShiftPattern_Val5")).booleanValue();      
%>

<tr><td bgcolor="#D4DCF5"><font color="navy"><wb:localize id='SPR_NewShiftPattern_Desc' overrideId="<%=mfrmIdStr%>"> To create a NEW Shift Pattern, complete the section below </wb:localize></font></td></tr>
<tr><td>
    <table class="contentTable" bgcolor="#ffffff" border="1">
    <tr>
 		<%if (showShiftPattern_Name) {%>
        <td><font color="navy"><wb:localize id='ShiftPattern_Name' overrideId="<%=mfrmIdStr%>" type="field"> Shift Pattern Name </wb:localize></font></td>
        <td><wb:controlField cssClass="inputField" id='ShiftPattern_Name' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternName"><%=newShiftPatternName %></wb:controlField></td>
        <% } %>
        <td>
            <table class="contentTable">
            <tr><td><font color="navy"><wb:localize id='SPR_DaysNo' overrideId="<%=mfrmIdStr%>" type="field"> Number of Days: </wb:localize></font></td>
                <% if (enableSetDays) { %>
                
                <td><input type="text" name="numberOfDays" size="2" maxlength="2" value="<%=numberOfDays %>" onBlur='limitValues(this, "<wb:localize id="SPR_daysLimitMessage" ignoreConfig="true" escapeForJavascript="true">The value for Number of Days has to be an integer between 1 and <%=maxNumDays%></wb:localize>");'><input type="hidden" name="numberOfDaysSet" value="<%=numberOfDays %>"><wb:config id="SPR_daysLimitMessage"/></td>
                <td>
                    <% if(toProcess) { %>
                        <wba:button label="Set Days" disabled="true" labelLocalizeIndex="SPR_SetDays" onClick=""/> 
                    <% } else { %>
                        <wba:button label="Set Days" labelLocalizeIndex="SPR_SetDays" onClick="setDays('#paint1#');"/>
                    <% } %>
                </td>
                <% } else { %>
                    <td>
                    <input type="text" name="numberOfDays" readonly='true' size="2" maxlength="2" value="<%=numberOfDays %>"> 
                    <input type="hidden" name="numberOfDaysSet" value="<%=numberOfDays %>">
                    </td>
                <% } %>
            </tr>
            </table>
        </td>
    </tr>
    <tr>
    	<%if (showShiftPattern_Desc) {%>
        <td><font color="navy"><wb:localize id='ShiftPattern_Desc' overrideId="<%=mfrmIdStr%>" type="field"> Shift Pattern Description </wb:localize></font></td>
        <td><wb:controlField cssClass="inputField" id='ShiftPattern_Desc' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternDesc"><%=newShiftPatternDesc %></wb:controlField></td>                
        <% } %>
    </tr>
    <tr>
    	<%if (showEffective_Date) {%>
        <td><font color="navy"><wb:localize id='Effective_Date' overrideId="<%=mfrmIdStr%>" type="field"> Effective Date </wb:localize></font></td>
        <td><wb:controlField cssClass="inputField" id='Effective_Date' overrideId="<%=mfrmIdStr%>" submitName="effectiveDate"><%=effectiveDate %></wb:controlField></td>
        <td></td>
        <% } %>        
    </tr>
    <tr>
    	<%if (showDay_Start_Time) {%>
        <td><font color="navy"><wb:localize id='Day_Start_Time' overrideId="<%=mfrmIdStr%>" type="field"> Day Start Time </wb:localize></font></td>
        <td><wb:controlField cssClass="inputField" id='Day_Start_Time' overrideId="<%=mfrmIdStr%>" submitName="dayStartTime"><%=dayStartTime %></wb:controlField></td>        
        <td></td>
        <% } %>
    </tr>
    <table>
    	<tr>
    		<%if (showShiftPattern_Udf1) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Udf1' overrideId="<%=mfrmIdStr%>" type="field"> Udf 1 </wb:localize></font></td>
    		<%} if (showShiftPattern_Udf2) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Udf2' overrideId="<%=mfrmIdStr%>" type="field"> Udf 2 </wb:localize></font></td>
    		<%} if (showShiftPattern_Udf3) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Udf3' overrideId="<%=mfrmIdStr%>" type="field"> Udf 3 </wb:localize></font></td>
    		<%} if (showShiftPattern_Udf4) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Udf4' overrideId="<%=mfrmIdStr%>" type="field"> Udf 4 </wb:localize></font></td>
    		<%} if (showShiftPattern_Udf5) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Udf5' overrideId="<%=mfrmIdStr%>" type="field"> Udf 5 </wb:localize></font></td>
    		<% } %>
    	</tr>
    	<tr>    
    		<%if (showShiftPattern_Udf1) {%>
			<td><wb:controlField cssClass="inputField" id='ShiftPattern_Udf1' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternUdf1"><%=newShiftPatternUdf1 %></wb:controlField></td>    	
			<%} if (showShiftPattern_Udf2) {%>
			<td><wb:controlField cssClass="inputField" id='ShiftPattern_Udf2' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternUdf2"><%=newShiftPatternUdf2 %></wb:controlField></td>    	
			<%} if (showShiftPattern_Udf3) {%>
			<td><wb:controlField cssClass="inputField" id='ShiftPattern_Udf3' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternUdf3"><%=newShiftPatternUdf3 %></wb:controlField></td>    	
			<%} if (showShiftPattern_Udf4) {%>
			<td><wb:controlField cssClass="inputField" id='ShiftPattern_Udf4' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternUdf4"><%=newShiftPatternUdf4 %></wb:controlField></td>    	
			<%} if (showShiftPattern_Udf5) {%>
			<td><wb:controlField cssClass="inputField" id='ShiftPattern_Udf5' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternUdf5"><%=newShiftPatternUdf5 %></wb:controlField></td>    	
			<% } %>
    	</tr>
    	<tr>
    		<%if (showShiftPattern_Val1) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Val1' overrideId="<%=mfrmIdStr%>" type="field"> Val 1 </wb:localize></font></td>
    		<%} if (showShiftPattern_Val2) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Val2' overrideId="<%=mfrmIdStr%>" type="field"> Val 2 </wb:localize></font></td>
    		<%} if (showShiftPattern_Val3) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Val3' overrideId="<%=mfrmIdStr%>" type="field"> Val 3 </wb:localize></font></td>
    		<%} if (showShiftPattern_Val4) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Val4' overrideId="<%=mfrmIdStr%>" type="field"> Val 4 </wb:localize></font></td>
    		<%} if (showShiftPattern_Val5) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Val5' overrideId="<%=mfrmIdStr%>" type="field"> Val 5 </wb:localize></font></td>
    		<% } %>
    	</tr>
    	<tr>
    		<%if (showShiftPattern_Val1) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Val1' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternVal1"><%=newShiftPatternVal1 %></wb:controlField></td>    	
    		<%} if (showShiftPattern_Val2) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Val2' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternVal2"><%=newShiftPatternVal2 %></wb:controlField></td>    	
    		<%} if (showShiftPattern_Val3) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Val3' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternVal3"><%=newShiftPatternVal3 %></wb:controlField></td>    	
    		<%} if (showShiftPattern_Val4) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Val4' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternVal4"><%=newShiftPatternVal4 %></wb:controlField></td>    	
    		<%} if (showShiftPattern_Val5) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Val5' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternVal5"><%=newShiftPatternVal5 %></wb:controlField></td>    	
    		<% } %>
    	</tr>
    	<tr>
    		<%if (showShiftPattern_Flag1) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Flag1' overrideId="<%=mfrmIdStr%>" type="field"> Flag 1</wb:localize></font></td>
    		<%} if (showShiftPattern_Flag2) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Flag2' overrideId="<%=mfrmIdStr%>" type="field"> Flag 2</wb:localize></font></td>
    		<%} if (showShiftPattern_Flag3) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Flag3' overrideId="<%=mfrmIdStr%>" type="field"> Flag 3</wb:localize></font></td>
    		<%} if (showShiftPattern_Flag4) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Flag4' overrideId="<%=mfrmIdStr%>" type="field"> Flag 4</wb:localize></font></td>
    		<%} if (showShiftPattern_Flag5) {%>
    		<td><font color="navy"><wb:localize id='ShiftPattern_Flag5' overrideId="<%=mfrmIdStr%>" type="field"> Flag 5</wb:localize></font></td>
    		<% } %>
    	</tr>
    	<tr>
    		<%if (showShiftPattern_Flag1) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Flag1' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternFlag1"><%=newShiftPatternFlag1 %></wb:controlField></td>    	
    		<%} if (showShiftPattern_Flag2) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Flag2' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternFlag2"><%=newShiftPatternFlag2 %></wb:controlField></td>    	
    		<%} if (showShiftPattern_Flag3) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Flag3' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternFlag3"><%=newShiftPatternFlag3 %></wb:controlField></td>    	
    		<%} if (showShiftPattern_Flag4) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Flag4' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternFlag4"><%=newShiftPatternFlag4 %></wb:controlField></td>    	
    		<%} if (showShiftPattern_Flag5) {%>
    		<td><wb:controlField cssClass="inputField" id='ShiftPattern_Flag5' overrideId="<%=mfrmIdStr%>" submitName="newShiftPatternFlag5"><%=newShiftPatternFlag5 %></wb:controlField></td>    	
    		<% } %>
    	</tr>
    </table>
    </table>
</td></tr>

</wb:page>