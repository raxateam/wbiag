<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.ArrayList, java.util.Iterator,
                 com.workbrain.server.jsp.locale.LocalizationDictionary" %>
<%@ page import="com.workbrain.app.jsp.shiftpatternresolver.ShiftPatternConstants" %>
<%@ page import="com.workbrain.app.jsp.shiftpatternresolver.ShiftPatternDay" %>
<%@ page import="com.wbiag.app.jsp.shiftpatternresolver.ShiftPatternResolverExt" %>
<%@ page import="com.workbrain.security.*" %>
<%@ page import="com.workbrain.app.ta.model.*" %>
<%@ page import="com.workbrain.util.DateHelper" %>
<%@ page import="com.workbrain.server.data.sql.FieldDescription" %>
<%@ page import="com.workbrain.server.data.sql.SQLDataDictionary" %>

<wb:page showUIPath="true" uiPathNameId="SPR_Page_Title" uiPathName="Shift Pattern Definition and Assignment" maintenanceFormId='<%=request.getParameter("mfrm_id")%>'>

<wb:define id="contextPath"><%= request.getContextPath() %></wb:define>
<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>

<script src="<%= request.getContextPath() %>/shiftPattern/definitionAssignmentExt.js"></script>
<script src="<%= request.getContextPath() %>/shiftPattern/definitionAssignmentExtCustom.js"></script>

<jsp:useBean id="spre" scope="request" class="com.wbiag.app.jsp.shiftpatternresolver.ShiftPatternResolverExt"/>

<%
    spre.init(pageContext);
    int mfrmIdInt = Integer.parseInt(mfrm_id.toString());
    String mfrmIdStr = mfrm_id.toString();
    System.out.println("maintenance form id string: "+mfrmIdStr);
    String fieldId;
    int preferredJob;
    String onChangeParam;
    boolean toProcess = false;
    boolean doRetrieve = false;
    boolean doGetInfo = false;
    ArrayList allDays = (ArrayList)spre.getAllDays();
    ShiftPatternDay day = new ShiftPatternDay();
    ArrayList shiftsList = spre.getShiftsList();
    ArrayList shiftLaborsList = spre.getShiftLaborsList();
    ArrayList shiftBreaksList = spre.getShiftBreaksList();
%>

<%!
    public String getDefaultUI(String id, PageContext pc, int formId) throws Exception{

        FieldDescription fd = SQLDataDictionary.get(JSPHelper.getWebContext(pc).getConnection()).getFieldDescription(id, formId);

        if (fd == null || fd.getFieldUI() == null) {
          return "StringUI";
        } else {
          return fd.getFieldUI().getName();
        }
      }


	public boolean showField(String fieldId, PageContext pageContext, int formId) throws Exception {

		boolean showField = false;
		String controlType = getDefaultUI(fieldId, pageContext, formId);

        if (!"HiddenUI".equals(controlType) || JSPHelper.getWebSession(pageContext.getRequest()).isConfigOn()) {
        	showField = true;
        }

		return showField;
	}
%>

<wb:define id="assign1"><%=ShiftPatternConstants.ACTION_ASSIGN%></wb:define>
<wb:define id="create1"><%=ShiftPatternConstants.ACTION_CREATE%></wb:define>
<wb:define id="create_assign1"><%=ShiftPatternConstants.ACTION_CREATE_ASSIGN%></wb:define>
<wb:define id="paint1"><%=ShiftPatternConstants.ACTION_PAINT%></wb:define>
<wb:define id="retrieve"><%=ShiftPatternResolverExt.ACTION_RETRIEVE%></wb:define>
<wb:define id="shftPatRetrieve"><%=ShiftPatternResolverExt.ACTION_SHFT_PAT_RETRIEVE%></wb:define>
<wb:define id="addRow"><%=ShiftPatternResolverExt.ACTION_ADDROW%></wb:define>
<wb:define id="getInfo"><%=ShiftPatternResolverExt.ACTION_GETINFO%></wb:define>
<wb:define id="dateFormat">MM/dd/yyyy</wb:define>
<wb:define id="timeFormat">HH:mm</wb:define>
<wb:define id="maxNumDays"><%=spre.getMaxNumDays()%></wb:define>

<wb:pageOnLoad id="123">if(<%=spre.getOvrIsPermanent() %> && eval('window.document.page_form.patternEndDate_dummy')){window.document.page_form.patternEndDate_dummy.disabled=true; }</wb:pageOnLoad>

<% if (!JSPHelper.getWebSession(pageContext.getRequest()).isConfigOn()) { %>
<wb:pageOnLoad id="hideDefaults">showHideAll()</wb:pageOnLoad>
<% } %>

<input type=HIDDEN name="actionType" value="<%=ShiftPatternConstants.ACTION_PAINT %>">
<input type=HIDDEN name="mfrm_id" value="<%=mfrmIdStr%>">
<input type=HIDDEN name="checkGaps" value="<%=spre.getCheckGaps()%>">
<input type=HIDDEN name="maxNumDays" value="<%=maxNumDays%>">
<input type=HIDDEN name="numberOfShifts" value="<%=spre.getNumOfShifts()%>">
<input type=HIDDEN name="enableCustomValidation" value="<%=spre.getEnableCustomValidation()%>">

<jsp:include page="definitionAssignmentExtConstants.jsp" />

<!--checks the parameters extracted from context -->
<%
    if (spre.hasErrors())
    {
%>
    <table class="contentTable" cellpadding='0' cellspacing='0' border='0' width="80%">
    <tr><th><font color="red"><b><wb:localize id="SPR_ERROR_HEADER">ERROR</wb:localize></b></font></th></tr>
    <tr><td><font size=2 color="red"><%= LocalizationDictionary.get().localizeErrorMessage(JSPHelper.getConnection(request), spre.getErrorMessage(), JSPHelper.getWebLocale(request).getLanguageId()) %></font></td></tr>
    </table><br>
<%
    }
    else if(ShiftPatternResolverExt.ACTION_GETINFO.equals(spre.getActionType()))
    {
        spre.doActionGetInfo();
        preferredJob = spre.getPreferredJobId();
        doGetInfo = true;
    }
    else if(ShiftPatternResolverExt.ACTION_SHFT_PAT_RETRIEVE.equals(spre.getActionType()))
    {
        spre.processRetrieve(Integer.parseInt(request.getParameter("existShiftPatternName")));
        spre.setNumberOfDays(shiftsList.size());
        doRetrieve = true;
    }
    else if(ShiftPatternResolverExt.ACTION_RETRIEVE.equals(spre.getActionType()))
    {
        spre.processRetrieve(-1);
        spre.setNumberOfDays(shiftsList.size());
        doRetrieve = true;
    }
    else if(!(ShiftPatternConstants.ACTION_PAINT.equals(spre.getActionType())) && !(ShiftPatternResolverExt.ACTION_ADDROW.equals(spre.getActionType())))
    {
        toProcess=true;
    }
%>

<%if (toProcess) {
        java.util.Date startTime = new  java.util.Date(); %>

    <br><table width="50%" class="contentTable" cellpadding='0' cellspacing='0' border='0'>
    <tr><th>Status</th></tr>
    <tr><td>
      <font size=2 color="navy">
        <wb:localize id='SPR_StartProcess' overrideId="<%=mfrmIdStr%>"> Shift Pattern Resolver Process started ...</wb:localize>
      </font>
    </td></tr>
    <tr><td>
      <font size=2 color="red">
        <wb:localize id='SPR_PleaseWait' overrideId="<%=mfrmIdStr%>"> Please wait while the process is done.</wb:localize>
      </font>
    </td></tr>
    </table>
<%
      out.flush();
      spre.processRequest();

%>
    <table class="contentTable" cellpadding='0' cellspacing='0' border='0'>
    <tr><td><font size=2 color="navy">
        <%=spre.getStatusMessage()%></font>
     </td></tr>
    </table><br>
    <wba:button label="OK" labelLocalizeIndex="OK" onClick="window.location='#contextPath#/shiftPattern/definitionAssignmentExt.jsp?mfrm_id=#mfrm_id#'"/>
<br><br>

<% } %>



<table bgcolor="#ffffff" border="0">
<tr><td><b><font color="navy" size=3><wb:localize id='SPR_Page_Title'> Shift Pattern Definition and Assignment </wb:localize></b></font></td></tr>
<tr><td>&nbsp;</td></tr>
</table>


<table class="contentTable" border="1"  width="80%">
<tr><td bgcolor="#D4DCF5"><font color="navy"><wb:localize id='SPR_Assign_Desc'> To ASSIGN a Shift Pattern, select an employee or group of employees and specify override dates </wb:localize></font></td></tr>
<tr><td>
    <table class="contentTable" bgcolor="#ffffff" border="1">
    <tr>
        <td><font color="navy"><wb:localize id='Employee' overrideId="<%=mfrmIdStr%>" type="field"> Employee </wb:localize></font></td>
        <td><wb:controlField cssClass="inputField" id='Employee' overrideId="<%=mfrmIdStr%>" submitName="empName" onChange="populateRetrieve();" ><%=spre.getEmployeesNames() %></wb:controlField></td>

        <%if(spre.getEnableGetInfo()){%>
            <td>
            <% if (toProcess) { %>
                <wba:button label="Get Info" labelLocalizeIndex="SPR_Get_Info" onClick="" disabled="true"/>
            <% } else { %>
                <wba:button label="Get Info" labelLocalizeIndex="SPR_Get_Info" onClick="getInfo('#getInfo#')"/>
            <% } %>
            </td>
        <%}%>
    </tr>

    <%  if (showField("Team", pageContext, mfrmIdInt)) { %>
        <tr>
            <td><font color="navy"><wb:localize id='Team' overrideId="<%=mfrmIdStr%>" type="field"> Team </wb:localize></font></td>
            <td><wb:controlField cssClass="inputField" id='Team' overrideId="<%=mfrmIdStr%>" submitName="teamName"><%=spre.getTeamName() %></wb:controlField></td>
        </tr>
    <% } %>

    <%  if (showField("Calc_Group", pageContext, mfrmIdInt)) { %>
        <tr>
            <td><font color="navy"><wb:localize id='Calc_Group' overrideId="<%=mfrmIdStr%>" type="field"> Calc Group </wb:localize></font></td>
            <td><wb:controlField cssClass="inputField" id='Calc_Group' overrideId="<%=mfrmIdStr%>" submitName="calcGroupName"><%=spre.getCalcGroupName() %></wb:controlField></td>
        </tr>
    <% } %>

    <%  if (showField("Pay_Group", pageContext, mfrmIdInt)) {  %>
        <tr>
            <td><font color="navy"><wb:localize id='Pay_Group' overrideId="<%=mfrmIdStr%>" type="field"> Pay Group </wb:localize></font></td>
            <td><wb:controlField cssClass="inputField" id='Pay_Group' overrideId="<%=mfrmIdStr%>" submitName="payGroupName"><%=spre.getPayGroupName() %></wb:controlField></td>
        </tr>
    <% } %>

    </table>
</td></tr>
<tr><td>
    <table class="contentTable" bgcolor="#ffffff" border="1">
    <tr>
        <td><font color="navy"><wb:localize id='Permanent'> Permanent </wb:localize></font></td>
        <td><font color="navy"><wb:localize id='Start_Date' overrideId="<%=mfrmIdStr%>" type="field"> Start Date </wb:localize></font></td>
        <td><font color="navy"><wb:localize id='End_Date' overrideId="<%=mfrmIdStr%>" type="field"> End Date </wb:localize></font></td>
    </tr>
    <tr>
        <td align="center">
        <% if(spre.getOvrIsPermanent()) {%>
            <wb:controlField submitName="isPermanent" ui="CheckboxUI" uiParameter="checked=true" onChange="javascript:activateEndDate();"></wb:controlField>
        <% } else { %>
            <wb:controlField submitName="isPermanent" ui="CheckboxUI" uiParameter="checked=false"></wb:controlField>
        <% } %>
        </td>
        <td><wb:controlField cssClass="inputField" id='Start_Date' overrideId="<%=mfrmIdStr%>" submitName="patternStartDate"><%=spre.getOvrStartDate()  %></wb:controlField></td>
        <td><wb:controlField cssClass="inputField" id='End_Date' overrideId="<%=mfrmIdStr%>" submitName="patternEndDate"><%=spre.getOvrEndDate() %></wb:controlField></td>
    </tr>
    </table>
</td></tr>


<%  if (showField("Shift_Pattern", pageContext, mfrmIdInt)) { %>
<jsp:include page="retrieveShiftPattern.jsp">
    <jsp:param name="toProcess" value="<%= String.valueOf(toProcess) %>" />
</jsp:include>
<% } %>

<%  if (showField("Retrieve_Employee", pageContext, mfrmIdInt)) { %>
<jsp:include page="retrieveShiftPatternEmployee.jsp">
    <jsp:param name="toProcess" value="<%= String.valueOf(toProcess) %>" />
</jsp:include>
<% } %>

<%
	boolean showShiftPattern_Name = false;
	boolean showShiftPattern_Desc = false;
	boolean showEffective_Date = false;
	boolean showDay_Start_Time = false;
	boolean showShiftPattern_Udf1 = false;
	boolean showShiftPattern_Udf2 = false;
	boolean showShiftPattern_Udf3 = false;
	boolean showShiftPattern_Udf4 = false;
	boolean showShiftPattern_Udf5 = false;
	boolean showShiftPattern_Val1 = false;
	boolean showShiftPattern_Val2 = false;
	boolean showShiftPattern_Val3 = false;
	boolean showShiftPattern_Val4 = false;
	boolean showShiftPattern_Val5 = false;
	boolean showShiftPattern_Flag1 = false;
	boolean showShiftPattern_Flag2 = false;
	boolean showShiftPattern_Flag3 = false;
	boolean showShiftPattern_Flag4 = false;
	boolean showShiftPattern_Flag5 = false;

	if (showField("ShiftPattern_Name", pageContext, mfrmIdInt))
	{
		showShiftPattern_Name = true;
	}
	if (showField("ShiftPattern_Desc", pageContext, mfrmIdInt))
	{
		showShiftPattern_Desc = true;
	}
	if (showField("Effective_Date", pageContext, mfrmIdInt))
	{
		showEffective_Date = true;
	}
	if (showField("Day_Start_Time", pageContext, mfrmIdInt))
	{
		showDay_Start_Time = true;
	}
	if (showField("ShiftPattern_Udf1", pageContext, mfrmIdInt))
	{
		showShiftPattern_Udf1 = true;
	}
	if (showField("ShiftPattern_Udf2", pageContext, mfrmIdInt))
	{
		showShiftPattern_Udf2 = true;
	}
	if (showField("ShiftPattern_Udf3", pageContext, mfrmIdInt))
	{
		showShiftPattern_Udf3 = true;
	}
	if (showField("ShiftPattern_Udf4", pageContext, mfrmIdInt))
	{
		showShiftPattern_Udf4 = true;
	}
	if (showField("ShiftPattern_Udf5", pageContext, mfrmIdInt))
	{
		showShiftPattern_Udf5 = true;
	}

	if (showField("ShiftPattern_Val1", pageContext, mfrmIdInt))
	{
		showShiftPattern_Val1 = true;
	}
	if (showField("ShiftPattern_Val2", pageContext, mfrmIdInt))
	{
		showShiftPattern_Val2 = true;
	}
	if (showField("ShiftPattern_Val3", pageContext, mfrmIdInt))
	{
		showShiftPattern_Val3 = true;
	}
	if (showField("ShiftPattern_Val4", pageContext, mfrmIdInt))
	{
		showShiftPattern_Val4 = true;
	}
	if (showField("ShiftPattern_Val5", pageContext, mfrmIdInt))
	{
		showShiftPattern_Val5 = true;
	}

	if (showField("ShiftPattern_Flag1", pageContext, mfrmIdInt))
	{
		showShiftPattern_Flag1 = true;
	}
	if (showField("ShiftPattern_Flag2", pageContext, mfrmIdInt))
	{
		showShiftPattern_Flag2 = true;
	}
	if (showField("ShiftPattern_Flag3", pageContext, mfrmIdInt))
	{
		showShiftPattern_Flag3 = true;
	}
	if (showField("ShiftPattern_Flag4", pageContext, mfrmIdInt))
	{
		showShiftPattern_Flag4 = true;
	}
	if (showField("ShiftPattern_Flag5", pageContext, mfrmIdInt))
	{
		showShiftPattern_Flag5 = true;
	}
%>

<jsp:include page="createShiftPattern.jsp">
    <jsp:param name="toProcess" value="<%= String.valueOf(toProcess) %>" />
    <jsp:param name="newShiftPatternName" value="<%= spre.getNewShiftPatternName() %>" />
    <jsp:param name="enableSetDays" value="<%= String.valueOf(spre.getEnableSetDays()) %>" />
    <jsp:param name="numberOfDays" value="<%= String.valueOf(spre.getNumberOfDays()) %>" />
    <jsp:param name="maxNumDays" value="<%= maxNumDays.toString() %>" />
    <jsp:param name="newShiftPatternDesc" value="<%= spre.getNewShiftPatternDesc() %>" />
    <jsp:param name="effectiveDate" value="<%= spre.getEffectiveDate() %>" />
    <jsp:param name="dayStartTime" value="<%= spre.getDayStartTime() %>" />
    <jsp:param name="newShiftPatternFlag1" value="<%= spre.getNewShiftPatternFlag1() %>" />
    <jsp:param name="newShiftPatternFlag2" value="<%= spre.getNewShiftPatternFlag2() %>" />
    <jsp:param name="newShiftPatternFlag3" value="<%= spre.getNewShiftPatternFlag3() %>" />
    <jsp:param name="newShiftPatternFlag4" value="<%= spre.getNewShiftPatternFlag4() %>" />
    <jsp:param name="newShiftPatternFlag5" value="<%= spre.getNewShiftPatternFlag5() %>" />
    <jsp:param name="newShiftPatternUdf1" value="<%= spre.getNewShiftPatternUdf1() %>" />
    <jsp:param name="newShiftPatternUdf2" value="<%= spre.getNewShiftPatternUdf2() %>" />
    <jsp:param name="newShiftPatternUdf3" value="<%= spre.getNewShiftPatternUdf3() %>" />
    <jsp:param name="newShiftPatternUdf4" value="<%= spre.getNewShiftPatternUdf4() %>" />
    <jsp:param name="newShiftPatternUdf5" value="<%= spre.getNewShiftPatternUdf5() %>" />
    <jsp:param name="newShiftPatternVal1" value="<%= spre.getNewShiftPatternVal1() %>" />
    <jsp:param name="newShiftPatternVal2" value="<%= spre.getNewShiftPatternVal2() %>" />
    <jsp:param name="newShiftPatternVal3" value="<%= spre.getNewShiftPatternVal3() %>" />
    <jsp:param name="newShiftPatternVal4" value="<%= spre.getNewShiftPatternVal4() %>" />
    <jsp:param name="newShiftPatternVal5" value="<%= spre.getNewShiftPatternVal5() %>" />
    <jsp:param name="showShiftPattern_Name" value="<%= String.valueOf(showShiftPattern_Name) %>" />
    <jsp:param name="showShiftPattern_Desc" value="<%= String.valueOf(showShiftPattern_Desc) %>" />
    <jsp:param name="showEffective_Date" value="<%= String.valueOf(showEffective_Date) %>" />
    <jsp:param name="showDay_Start_Time" value="<%= String.valueOf(showDay_Start_Time) %>" />
    <jsp:param name="showShiftPattern_Flag1" value="<%= String.valueOf(showShiftPattern_Flag1) %>" />
    <jsp:param name="showShiftPattern_Flag2" value="<%= String.valueOf(showShiftPattern_Flag2) %>" />
    <jsp:param name="showShiftPattern_Flag3" value="<%= String.valueOf(showShiftPattern_Flag3) %>" />
    <jsp:param name="showShiftPattern_Flag4" value="<%= String.valueOf(showShiftPattern_Flag4) %>" />
    <jsp:param name="showShiftPattern_Flag5" value="<%= String.valueOf(showShiftPattern_Flag5) %>" />
    <jsp:param name="showShiftPattern_Udf1" value="<%= String.valueOf(showShiftPattern_Udf1) %>" />
    <jsp:param name="showShiftPattern_Udf2" value="<%= String.valueOf(showShiftPattern_Udf2) %>" />
    <jsp:param name="showShiftPattern_Udf3" value="<%= String.valueOf(showShiftPattern_Udf3) %>" />
    <jsp:param name="showShiftPattern_Udf4" value="<%= String.valueOf(showShiftPattern_Udf4) %>" />
    <jsp:param name="showShiftPattern_Udf5" value="<%= String.valueOf(showShiftPattern_Udf5) %>" />
    <jsp:param name="showShiftPattern_Val1" value="<%= String.valueOf(showShiftPattern_Val1) %>" />
    <jsp:param name="showShiftPattern_Val2" value="<%= String.valueOf(showShiftPattern_Val2) %>" />
    <jsp:param name="showShiftPattern_Val3" value="<%= String.valueOf(showShiftPattern_Val3) %>" />
    <jsp:param name="showShiftPattern_Val4" value="<%= String.valueOf(showShiftPattern_Val4) %>" />
    <jsp:param name="showShiftPattern_Val5" value="<%= String.valueOf(showShiftPattern_Val5) %>" />
</jsp:include>

<tr><td>
    <table class="contentTable" bgcolor="#ffffff" border="1">
    <tr><td><font color="navy"><wb:localize id='SPR_ShiftTimes'> Shift Times: </wb:localize></font></td></tr>
    <tr><td>
        <!--div id="days"-->
        <table class="contentTable" border=1>

            <%
                String fieldName = "";
                String fieldValue = "";
                int iterations;
                ShiftData sd = new ShiftData();
                ShiftBreakData sbd = new ShiftBreakData();
                ArrayList laborList = new ArrayList();
                ArrayList shiftDataList = new ArrayList();
                ArrayList shiftBreakDataList = new ArrayList();

                if(doRetrieve)
                {
                    iterations = shiftsList.size();
                }
                else
                {
                    iterations = spre.getNumberOfDays();
                }

                for(int k=0;k<iterations; k++)
                {
                    if(doRetrieve)
                    {
                    	shiftDataList = (ArrayList)shiftsList.get(k);
                        shiftBreakDataList = (ArrayList)shiftBreaksList.get(k);
                        //sd = (ShiftData)shiftsList.get(k);
                        //sbd = (ShiftBreakData)shiftBreaksList.get(k);
                        laborList = (ArrayList)shiftLaborsList.get(k);
                    }
             %>

            <!-- Shift -->
            <tr>
		<td><%if(spre.getEnableWeekLabel()){%><font color='navy'><wb:localize id='SPR_Week'> Week </wb:localize> <%= (k/7)+1 %> - <%}%><% if(spre.getDayOfWeekLocalization()){fieldId = "SPR_Day"+(k%7+1);}else{fieldId = "SPR_Day";}%><font color='navy'><wb:localize id='<%=fieldId%>'> Day </wb:localize> <%if(!spre.getDayOfWeekLocalization()){%><%=k+1%><%}%></td>
			</tr>
            <tr>
                <td>
                    <table class="contentTable" border=1>
                        <tr>
                            <td>&nbsp;</td>
                            <td><font color="navy"><wb:localize id='SPR_sStartTime' overrideId="<%=mfrmIdStr%>" type="field"> Shift Start Date </wb:localize></td>
                            <td><font color="navy"><wb:localize id='SPR_sEndTime' overrideId="<%=mfrmIdStr%>" type="field"> Shift End Date </wb:localize></td>

                            <td><span id="SHIFT_GROUP_SPAN_TITLE_<%=(k+1)%>">
                            <%  if (showField("Shift_Group", pageContext, mfrmIdInt)) { %>
                            	<font color="navy"><wb:localize id='Shift_Group' overrideId="<%=mfrmIdStr%>" type="field"></wb:localize>
                            <%}%>
                            	</span></td>

                            <td><span id="SHIFT_YAG_SPAN_TITLE_<%=(k+1)%>">
                            <%  if (showField("Include_In_YAG", pageContext, mfrmIdInt)) { %>
                            	<font color="navy"><wb:localize id='Include_In_YAG' overrideId="<%=mfrmIdStr%>" type="field"></wb:localize>
                            <%}%>
                            	</span></td>

                            <td><span id="SHIFT_COLOR_SPAN_TITLE_<%=(k+1)%>">
                            <%  if (showField("Shift_Color", pageContext, mfrmIdInt)) { %>
                            	<font color="navy"><wb:localize id='Shift_Color' overrideId="<%=mfrmIdStr%>" type="field"></wb:localize>
                            <%}%>
                            	</span></td>

                            <%  if (showField("Break_Start_Time", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Break_Start_Time' overrideId="<%=mfrmIdStr%>" type="field"></wb:localize></td>
                            <%}%>

                            <% if (showField("Break_End_Time", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Break_End_Time' overrideId="<%=mfrmIdStr%>" type="field"></wb:localize></td>
                            <%}%>

                            <td></td>

                            <td><span id="BREAK_HT_SPAN_TITLE_<%=(k+1)%>">
                            <%  if (showField("Break_Default_Hour_Type", pageContext, mfrmIdInt)) { %>
                            	<font color="navy"><wb:localize id='Break_Default_Hour_Type' overrideId="<%=mfrmIdStr%>" type="field"></wb:localize>
                            <%}%>
                            	</span></td>

                            <td><span id="BREAK_TC_SPAN_TITLE_<%=(k+1)%>">
                            <%  if (showField("Break_Time_Code", pageContext, mfrmIdInt)) { %>
                            	<font color="navy"><wb:localize id='Break_Time_Code' overrideId="<%=mfrmIdStr%>" type="field"></wb:localize>
                            <%}%>
                            	</span></td>
                        </tr>

                    	<%
                    		String shiftNumPosfix = null;
                    		int numOfShifts = spre.getNumOfShifts();

                    		for(int shiftNum = 1 ; shiftNum <= numOfShifts ; shiftNum++)
                    		{
                    			shiftNumPosfix = "_shift_" + shiftNum;
                    			if(doRetrieve)
                    			{
                    				sd = (ShiftData)shiftDataList.get(shiftNum-1);
                    				sbd = (ShiftBreakData)shiftBreakDataList.get(shiftNum-1);
                    			}
						%>


                        <tr>
                        	<td><font color="navy"><wb:localize id='SPR_shiftNum'> Shift <%=shiftNum%> </wb:localize></td>
                        	<%
                        	boolean autoPopulate = false;
							if(shiftNum==1 &&spre.getAutoPopulateLabor())
							{
								autoPopulate = true;
                        	}
                        	%>
                            <td>
                            <%
                            fieldName = "shiftStartTime_" + (k+1) + shiftNumPosfix;
                            if (!spre.getDisableLaborMetrics())
                            {
								onChangeParam = "prePopulate("+(k+1)+","+"\'start\'"+","+autoPopulate+");";
							}
							else
							{
								onChangeParam = "";
							}
							%>
                            <wb:controlField id='SPR_sStartTime' overrideId="<%=mfrmIdStr%>" cssClass="inputField" nullable="true" submitName="<%= fieldName%>" onChange="<%= onChangeParam %>"><% if(doRetrieve && sd != null && sd.getShftStartTime() != null) { %><%= DateHelper.convertDateString(sd.getShftStartTime(), ShiftPatternConstants.FULL_TIME_FMT) %><% } else if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <td>
                            <%
                            fieldName = "shiftEndTime_" + (k+1) + shiftNumPosfix;
                            if (!spre.getDisableLaborMetrics())
                            {
                            	onChangeParam = "prePopulate("+(k+1)+","+"\'end\'"+","+autoPopulate+");";
                            }
							else
							{
								onChangeParam = "";
							}
                            %>
                            <wb:controlField  id='SPR_sEndTime' overrideId="<%=mfrmIdStr%>" cssClass="inputField" nullable="true" submitName="<%= fieldName%>" onChange="<%= onChangeParam %>"><% if(doRetrieve && sd != null && sd.getShftEndTime() != null) { %><%= DateHelper.convertDateString(sd.getShftEndTime(), ShiftPatternConstants.FULL_TIME_FMT) %><% } else if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></span></td>
                            <td><span id="SHIFT_GROUP_SPAN_<%=(k+1) + shiftNumPosfix%>">
                            	<% String sgStr = "sourceType=SQL source=\"SELECT SHFTGRP_ID, SHFTGRP_NAME, SHFTGRP_DESC FROM SHIFT_GROUP\" width=10"; %>
                            	<% fieldName = "shiftGroup_" + (k+1) + shiftNumPosfix; %>
                            <%  if (showField("Shift_Group", pageContext, mfrmIdInt)) { %>
                            	<wb:controlField cssClass="inputField" id="Shift_Group" submitName="<%= fieldName%>" ><% if(doRetrieve) { %><%= spre.getNotNull(String.valueOf(sd.getShftgrpId()), "" + spre.DEFAULT_SHIFT_GROUP) %><% } else { %><%= spre.getNotNull(request.getParameter(fieldName), "" + spre.DEFAULT_SHIFT_GROUP) %><% } %></wb:controlField>
                            <% } else { %>
                            	<wb:controlField cssClass="inputField" submitName="<%= fieldName%>" ui="HiddenUI"><% if(doRetrieve) { %><%= spre.getNotNull(String.valueOf(sd.getShftgrpId()), "" + spre.DEFAULT_SHIFT_GROUP) %><% } else { %><%= spre.getNotNull(request.getParameter(fieldName), "" + spre.DEFAULT_SHIFT_GROUP) %><% } %></wb:controlField>
                            <% } %>
                            	</span></td>

                            <td><span id="SHIFT_YAG_SPAN_<%=(k+1) + shiftNumPosfix%>">
	                            <% fieldName = "shiftYAG_" + (k+1) + shiftNumPosfix;
	                            	String isChecked;
	                            	if(doRetrieve) {
	                            		isChecked = sd.getShftYag();

	                            	} else {
	                            		isChecked = request.getParameter(fieldName + "_dummy");
	                            }%>
                            <%  if (showField("Include_In_YAG", pageContext, mfrmIdInt)) { %>
	                            <% if("Y".equalsIgnoreCase(isChecked)) { %>
	                            	<wb:controlField cssClass="inputField" submitName="<%= fieldName%>" ui="CheckboxUI" uiParameter="checked=true alternateField=true"></wb:controlField>
	                            <% } else { %>
	                            	<wb:controlField cssClass="inputField" submitName="<%= fieldName%>" ui="CheckboxUI" uiParameter="alternateField=true"></wb:controlField>
	                            <% } %>
                            <% } else {
                                fieldName = fieldName + "_dummy";
                            %>
	                            <% if("Y".equalsIgnoreCase(isChecked)) { %>
	                            	<wb:controlField submitName="<%= fieldName %>" ui="HiddenUI">Y</wb:controlField>
	                            <% } else { %>
	                            	<wb:controlField submitName="<%= fieldName %>" ui="HiddenUI">N</wb:controlField>
	                            <% } %>
                            <% } %>
	                            </span></td>

                            <td><span id="SHIFT_COLOR_SPAN_<%=(k+1) + shiftNumPosfix%>">
	                            <% String colorStr = "sourceType=SQL source=\"SELECT COLR_ID, COLR_NAME FROM COLOUR\" width=10"; %>
	                            <% fieldName = "shiftColor_" + (k+1) + shiftNumPosfix; %>
                            <%  if (showField("Shift_Color", pageContext, mfrmIdInt)) { %>
	                            <wb:controlField cssClass="inputField" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter='<%= colorStr %>'><% if(doRetrieve) { %><%= spre.getNotNull(String.valueOf(sd.getColrId()), "" + spre.DEFAULT_COLOR) %><% } else { %><%= spre.getNotNull(request.getParameter(fieldName), "" + spre.DEFAULT_COLOR) %><% } %></wb:controlField>
                            <% } else { %>
	                            <wb:controlField cssClass="inputField" submitName="<%= fieldName%>" ui="HiddenUI"><% if(doRetrieve) { %><%= spre.getNotNull(String.valueOf(sd.getColrId()), "" + spre.DEFAULT_COLOR) %><% } else { %><%= spre.getNotNull(request.getParameter(fieldName), "" + spre.DEFAULT_COLOR) %><% } %></wb:controlField>
                            <% } %>
	                            </span></td>

                            <%  if (showField("Break_Start_Time", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "breakStartTime_" + (k+1) + shiftNumPosfix; %><wb:controlField cssClass="inputField" id='Break_Start_Time' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(doRetrieve) { %><% if(sbd.getShftbrkStartTime() != null) { %><%= DateHelper.convertDateString(sbd.getShftbrkStartTime(), ShiftPatternConstants.FULL_TIME_FMT) %><% } %><% } else if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Break_End_Time", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "breakEndTime_" + (k+1) + shiftNumPosfix; %><wb:controlField cssClass="inputField" id='Break_End_Time' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(doRetrieve) { %><% if(sbd.getShftbrkEndTime() != null) { %><%= DateHelper.convertDateString(sbd.getShftbrkEndTime(), ShiftPatternConstants.FULL_TIME_FMT) %><% } %><% } else if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <td><% fieldName = "breakDuration_" + (k+1) + shiftNumPosfix; %><wb:controlField cssClass="inputField" submitName="<%= fieldName%>" ui="HiddenUI" uiParameter="Number=3"><%= request.getParameter(fieldName) %></wb:controlField></td>

                            <td><span id="BREAK_HT_SPAN_<%=(k+1) + shiftNumPosfix%>">
	                            <% String htStr = "sourceType=SQL source=\"SELECT HTYPE_ID, HTYPE_NAME, HTYPE_DESC, HTYPE_MULTIPLE FROM HOUR_TYPE\" width=7"; %>
	                            <% fieldName = "breakDefaultHourType_" + (k+1) + shiftNumPosfix; %>
                            <%  if (showField("Break_Default_Hour_Type", pageContext, mfrmIdInt)) { %>
	                            <wb:controlField cssClass="inputField" id='Break_Default_Hour_Type' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(doRetrieve) { %><%=  spre.getNotNull(String.valueOf(sbd.getHtypeId()), "" + spre.getDefaultHourType()) %><% } else { %><%=  spre.getNotNull(request.getParameter(fieldName), "" + spre.getDefaultHourType()) %><% } %></wb:controlField>
                            <% } %>
                            	</span></td>

                            <td><span id="BREAK_TC_SPAN_<%=(k+1) + shiftNumPosfix%>">
                            	<% String tcStr = "sourceType=SQL source=\"SELECT TCODE_ID, TCODE_NAME, TCODE_DESC FROM TIME_CODE\" width=5"; %>
                            	<% fieldName = "breakTimeCode_" + (k+1) + shiftNumPosfix; %>
                            <%  if (showField("Break_Time_Code", pageContext, mfrmIdInt)) { %>
                            	<wb:controlField cssClass="inputField" id='Break_Time_Code' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(doRetrieve) { %><%=  spre.getNotNull(String.valueOf(sbd.getTcodeId()), "" + spre.getDefaultTimeCode()) %><% } else { %><%= spre.getNotNull(request.getParameter(fieldName), "" + spre.getDefaultTimeCode()) %><% } %></wb:controlField>
                            <% } %>
                            	</span></td>
                        </tr>
                        <% } %>
                    </table>
                </td>
            </tr>

            <!-- Shift Labour -->
			<% if (!spre.getDisableLaborMetrics()){ %>
            <tr>
                <td>
                    <table class="contentTable" border=1>

                        <!-- Column Labels. -->
                        <tr><td>&nbsp;</td>
                            <td><font color="navy"><wb:localize id='labStartTime' overrideId="<%=mfrmIdStr%>" type="field"> Start Time </wb:localize></td>
                            <td><font color="navy"><wb:localize id='labEndTime' overrideId="<%=mfrmIdStr%>" type="field"> End Time </wb:localize></td>

                            <% if (showField("Docket", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Docket' overrideId="<%=mfrmIdStr%>" type="field"> Docket </wb:localize></td>
                            <% } %>

                            <% if (showField("Hour_Type", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Hour_Type' overrideId="<%=mfrmIdStr%>" type="field">Default Hour Type</wb:localize></td>
                            <% } %>

                            <% if (showField("Job", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Job' overrideId="<%=mfrmIdStr%>" type="field">Job</wb:localize></td>
                            <% } %>

                            <% if (showField("Time_Code", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Time_Code' overrideId="<%=mfrmIdStr%>" type="field">Time Code</wb:localize></td>
                            <% } %>

                            <% if (showField("Project", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Project' overrideId="<%=mfrmIdStr%>" type="field">Project</wb:localize></td>
                            <% } %>

                            <% if (showField("Dept", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Dept' overrideId="<%=mfrmIdStr%>" type="field">Department</wb:localize></td>
                            <% } %>

                            <% if (showField("Act", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Act' overrideId="<%=mfrmIdStr%>" type="field">Activity</wb:localize></td>
                            <% } %>

                            <% if (showField("Wbt", pageContext, mfrmIdInt)) { %>
                                <td><font color="navy"><wb:localize id='Wbt' overrideId="<%=mfrmIdStr%>" type="field">Team</wb:localize></td>

                            <% } %>

                            <!--   <td><font color="navy"><wb:localize id='bDuration'> Break Duration </wb:localize></td>-->
                            <%
                                for (int i=1 ; i<=10 ; i++)
                                {
                                    fieldId = "Flag"+String.valueOf(i);
                                    if (showField(fieldId, pageContext, mfrmIdInt))
                                    {
                            %>
                            <td><font color="navy"><wb:localize id='<%=fieldId%>' overrideId="<%=mfrmIdStr%>" type="field"> Lab Flag<%=i%> </wb:localize></td>
                            <%
                                    }
                                }
                            %>
                            <%
                                for (int i=1 ; i<=10 ; i++)
                                {
                                    fieldId = "Udf"+String.valueOf(i);
                                    if (showField(fieldId, pageContext, mfrmIdInt))
                                    {
                            %>
                                    <td><font color="navy"><wb:localize id='<%=fieldId%>' overrideId="<%=mfrmIdStr%>" type="field"></wb:localize></td>
                            <%
                                    }
                                }
                            %>
                            <td></td>
                            <td></td>
                        </tr>
                        <!-- End column labels. -->

                        <%
                            ShiftPatternShiftLaborData spsld;
                            int count = 0;
                            Iterator it = laborList.iterator();
                            if(doRetrieve)
                            {
                                while(it.hasNext())
                                {
                                    count++;
                                    spsld = (ShiftPatternShiftLaborData)it.next();
                         %>

                         <!-- Labour rows retrieved. -->
                        <tr>
                            <td><font color='navy'><wb:localize id='SPR_Labor'>Labor</wb:localize></td>
                            <td><% fieldName = "LabStartTime_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='labStartTime' overrideId="<%=mfrmIdStr%>" nullable="true" submitName="<%= fieldName%>"><% if(spsld.getSpslabStartTime() != null) { %><%= DateHelper.convertDateString(spsld.getSpslabStartTime(), ShiftPatternConstants.FULL_TIME_FMT) %><% } %></wb:controlField></td>
                            <td><% fieldName = "LabEndTime_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='labEndTime' overrideId="<%=mfrmIdStr%>" nullable="true" submitName="<%= fieldName%>"><% if(spsld.getSpslabEndTime() != null) { %><%= DateHelper.convertDateString(spsld.getSpslabEndTime(), ShiftPatternConstants.FULL_TIME_FMT) %><% } %></wb:controlField></td>

                            <%  if (showField("Docket", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabDocket_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT DOCK_ID, DOCK_NAME FROM DOCKET\" width=10"><% if(spsld.getDockId() != null) { %><%= spsld.getDockId() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Hour_Type", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabHType_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT HTYPE_ID, HTYPE_NAME FROM HOUR_TYPE\" width=10"><% if(spsld.getHtypeId() != null) { %><%= spsld.getHtypeId() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Job", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabJob_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT JOB_ID, JOB_NAME FROM JOB\" width=10"><% if(spsld.getJobId() != null) { %><%= spsld.getJobId() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Time_Code", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabTCode_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT TCODE_ID, TCODE_NAME FROM TIME_CODE\" width=10"><% if(spsld.getTcodeId() != null) { %><%= spsld.getTcodeId() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Project", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabProj_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT PROJ_ID, PROJ_NAME FROM PROJECT\" width=10"><% if(spsld.getProjId() != null) { %><%= spsld.getProjId() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Dept", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabDept_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" id='Dept' overrideId="<%=mfrmIdStr%>"><% if(spsld.getDeptId() != null) { %><%= spsld.getDeptId() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Act", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabAct_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" id='Act' overrideId="<%=mfrmIdStr%>"><% if(spsld.getActId() != null) { %><%= spsld.getActId() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Wbt", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabWbt_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" id='Wbt' overrideId="<%=mfrmIdStr%>"><% if(spsld.getActId() != null) { %><%= spsld.getActId() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag1", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag1_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag1' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag1() != null) { %><%= spsld.getSpslabFlag1() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag2", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag2_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag2' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag2() != null) { %><%= spsld.getSpslabFlag2() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag3", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag3_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag3' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag3() != null) { %><%= spsld.getSpslabFlag3() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag4", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag4_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag4' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag4() != null) { %><%= spsld.getSpslabFlag4() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag5", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag5_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag5' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag5() != null) { %><%= spsld.getSpslabFlag5() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag6", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag6_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag6' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag6() != null) { %><%= spsld.getSpslabFlag6() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag7", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag7_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag7' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag7() != null) { %><%= spsld.getSpslabFlag7() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag8", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag8_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag8' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag8() != null) { %><%= spsld.getSpslabFlag8() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag9", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag9_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag9' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag9() != null) { %><%= spsld.getSpslabFlag9() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Flag10", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabFlag10_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Flag10' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabFlag10() != null) { %><%= spsld.getSpslabFlag10() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf1", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf1_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf1' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf1() != null) { %><%= spsld.getSpslabUdf1() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf2", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf2_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf2' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf2() != null) { %><%= spsld.getSpslabUdf2() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf3", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf3_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf3' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf3() != null) { %><%= spsld.getSpslabUdf3() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf4", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf4_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf4' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf4() != null) { %><%= spsld.getSpslabUdf4() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf5", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf5_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf5' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf5() != null) { %><%= spsld.getSpslabUdf5() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf6", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf6_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf6' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf6() != null) { %><%= spsld.getSpslabUdf6() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf7", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf7_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf7' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf7() != null) { %><%= spsld.getSpslabUdf7() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf8", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf8_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf8' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf8() != null) { %><%= spsld.getSpslabUdf8() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf9", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf9_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf9' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf9() != null) { %><%= spsld.getSpslabUdf9() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Udf10", pageContext, mfrmIdInt)) { %>
                                <td><% fieldName = "LabUdf10_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='Udf10' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(spsld.getSpslabUdf10() != null) { %><%= spsld.getSpslabUdf10() %><% } %></wb:controlField></td>
                            <%}%>
                        </tr>

                            <% }  // end while retrieve each shift pattern labor %>

                        <%  } // end if(doRetrieve)

                            else
                            {
                                // Edited rows if the form is being refreshed due to a Add row etc.
                                while(request.getParameter("LabStartTime_" + String.valueOf(k+1) + "_" + String.valueOf(count+1)) != null)
                                {
                                    count++;
                        %>
                        <tr>
                            <td><font color='navy'><wb:localize id='SPR_Labor'>Labor</wb:localize></td>
                            <td><% fieldName = "LabStartTime_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='labStartTime' overrideId="<%=mfrmIdStr%>" nullable="true" submitName="<%= fieldName%>"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <td><% fieldName = "LabEndTime_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" id='labEndTime' overrideId="<%=mfrmIdStr%>" nullable="true" submitName="<%= fieldName%>"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>

                            <%  if (showField("Docket", pageContext, mfrmIdInt)) { %>
                            <td><% fieldName = "LabDocket_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT DOCK_ID, DOCK_NAME FROM DOCKET\" width=10"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Hour_Type", pageContext, mfrmIdInt)) { %>
                            <td><% fieldName = "LabHType_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT HTYPE_ID, HTYPE_NAME FROM HOUR_TYPE\" width=10"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Job", pageContext, mfrmIdInt)) { %>
                            <td><% fieldName = "LabJob_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT JOB_ID, JOB_NAME FROM JOB\" width=10"><% if(doGetInfo) { %><%= spre.getPreferredJobId() %><% } else if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Time_Code", pageContext, mfrmIdInt)) { %>
                            <td><% fieldName = "LabTCode_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT TCODE_ID, TCODE_NAME FROM TIME_CODE\" width=10"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Project", pageContext, mfrmIdInt)) { %>
                            <td><% fieldName = "LabProj_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT PROJ_ID, PROJ_NAME FROM PROJECT\" width=10"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Dept", pageContext, mfrmIdInt)) { %>
                            <td><% fieldName = "LabDept_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" id='Dept' overrideId="<%=mfrmIdStr%>"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Act", pageContext, mfrmIdInt)) { %>
                            <td><% fieldName = "LabAct_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" id='Act' overrideId="<%=mfrmIdStr%>"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Wbt", pageContext, mfrmIdInt)) { %>
                            <td><% fieldName = "LabWbt_" + String.valueOf(k+1) + "_" + String.valueOf(count); %><wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" id='Wbt' overrideId="<%=mfrmIdStr%>"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%}%>

                            <%
                                for (int i=1 ; i<=10 ; i++)
                                {
                                    fieldId = "Flag"+String.valueOf(i);
                                    fieldName = "LabFlag"+String.valueOf(i)+"_" + String.valueOf(k+1) + "_" + String.valueOf(count);

                                    if (showField(fieldId, pageContext, mfrmIdInt))
                                    {
                            %>
                            <td><wb:controlField cssClass="inputField" id='<%=fieldId%>' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%
                                    }
                                }
                            %>
                            <%
                                for (int i=1 ; i<=10 ; i++)
                                {
                                    fieldId = "Udf"+String.valueOf(i);
                                    fieldName = "LabUdf"+String.valueOf(i)+"_" + String.valueOf(k+1) + "_" + String.valueOf(count);

                                    if (showField(fieldId, pageContext, mfrmIdInt))
                                    {
                            %>
                            <td><wb:controlField cssClass="inputField" id='<%=fieldId%>' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"><% if(request.getParameter(fieldName)!=null) { %><%= request.getParameter(fieldName) %><% } %></wb:controlField></td>
                            <%
                                    }
                                }
                            %>
                        </tr>
                            <%
                                } // end while request.getParameter("LabStartTime_xxx) != null

                            } // end else.


                            // Blank row
                            if((String.valueOf(k+1)).equals(request.getParameter("whichRow")) || count == 0)
                            {
                                count++;
                            %>
                        <tr>
                            <td><font color='navy'><wb:localize id='SPR_Labor'>Labor</wb:localize></td>
                            <td>
                                <% fieldName = "LabStartTime_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                <wb:controlField cssClass="inputField" id='labStartTime' overrideId="<%=mfrmIdStr%>" nullable="true" submitName="<%= fieldName%>"></wb:controlField></td>

                            <td>
                                <% fieldName = "LabEndTime_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                <wb:controlField cssClass="inputField" id='labEndTime' overrideId="<%=mfrmIdStr%>" nullable="true" submitName="<%= fieldName%>"></wb:controlField></td>

                            <%  if (showField("Docket", pageContext, mfrmIdInt)) { %>
                                <td>
                                <% fieldName = "LabDocket_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                <wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT DOCK_ID, DOCK_NAME FROM DOCKET\" width=10"></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Hour_Type", pageContext, mfrmIdInt)) { %>
                                <td>
                                <% fieldName = "LabHType_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                <wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT HTYPE_ID, HTYPE_NAME FROM HOUR_TYPE\" width=10"></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Job", pageContext, mfrmIdInt)) { %>
                                <td>
                                    <% fieldName = "LabJob_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                    <wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT JOB_ID, JOB_NAME FROM JOB\" width=10"><% if(doGetInfo) { %><%= spre.getPreferredJobId() %><% } %></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Time_Code", pageContext, mfrmIdInt)) { %>
                                <td>
                                <% fieldName = "LabTCode_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                <wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT TCODE_ID, TCODE_NAME FROM TIME_CODE\" width=10"></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Project", pageContext, mfrmIdInt)) { %>
                                <td>
                                <% fieldName = "LabProj_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                <wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" ui="DBLookupUI" uiParameter="sourceType=SQL source=\"SELECT PROJ_ID, PROJ_NAME FROM PROJECT\" width=10"></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Dept", pageContext, mfrmIdInt)) { %>
                                <td>
                                <% fieldName = "LabDept_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                    <wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" id='Dept' overrideId="<%=mfrmIdStr%>"></wb:controlField></td>
                            <%}%>

                             <%  if (showField("Act", pageContext, mfrmIdInt)) { %>
                                <td>
                                <% fieldName = "LabAct_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                    <wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" id='Act' overrideId="<%=mfrmIdStr%>"></wb:controlField></td>
                            <%}%>

                            <%  if (showField("Wbt", pageContext, mfrmIdInt)) { %>
                                <td>
                                <% fieldName = "LabWbt_" + String.valueOf(k+1) + "_" + String.valueOf(count); %>
                                    <wb:controlField cssClass="inputField" nullable="true" submitName="<%= fieldName%>" id='Wbt' overrideId="<%=mfrmIdStr%>"></wb:controlField></td>
                            <%}%>


                            <%
                                for (int i=1 ; i<=10 ; i++)
                                {
                                    fieldId = "Flag"+String.valueOf(i);
                                    fieldName = "LabFlag"+String.valueOf(i)+"_" + String.valueOf(k+1) + "_" + String.valueOf(count);

                                    if (showField(fieldId, pageContext, mfrmIdInt))
                                    {
                            %>
                            <td>
                                <wb:controlField cssClass="inputField" id='<%=fieldId%>' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"></wb:controlField></td>
                            <%
                                    }
                                }
                            %>
                            <%
                                for (int i=1 ; i<=10 ; i++)
                                {
                                    fieldId = "Udf"+String.valueOf(i);
                                    fieldName = "LabUdf"+String.valueOf(i)+"_" + String.valueOf(k+1) + "_" + String.valueOf(count);

                                    if (showField(fieldId, pageContext, mfrmIdInt))
                                    {
                            %>
                            <td>
                                <wb:controlField cssClass="inputField" id='<%=fieldId%>' overrideId="<%=mfrmIdStr%>" submitName="<%= fieldName%>"></wb:controlField></td>
                            <%
                                    }
                                }
                            %>
                        </tr>
                        <% } // end if %>

                            <% fieldName = "rowCount_" + (k+1); %>
                            <input type=HIDDEN name="<%= fieldName%>" value="<%= String.valueOf(count)%>">

                        <% if (spre.getEnableAddRows()) { %>
                        <tr>
                        <td>
                            <% if (toProcess) { %>
                                <wba:button label="Add Row" labelLocalizeIndex="Add_Row" onClick="" disabled="true" />
                            <% } else { %>
                                <% fieldValue = "addRow('#addRow#', '"+(k+1)+"');"; %>
                                <wba:button label="Add Row" labelLocalizeIndex="Add_Row" onClick="<%= fieldValue%>"/>
                            <% } %>
                        </td>
                        </tr>
                        <% } %>

                    </table>
                </td>
            </tr>
            <% } %>
        </table>
        <!--/div -->
    </td></tr>
    <% } // !spre.getDisableLaborMetrics() %>
<% if (spre.getEnableShowHideDefaults()) { %>
    <tr>
    <td align="left">
        <a href="javascript:showHideAll()">
            <wb:localize id="SPR_ShowHideDefaults">Show/Hide Default Values</wb:localize>
        </a>
    </td>
    </tr>
<% } %>

    </table>
</td></tr>
<a name="process_anchor">
<jsp:include page="process.jsp">
    <jsp:param name="toProcess" value="<%= String.valueOf(toProcess) %>" />
</jsp:include>
</a>

</wb:page>