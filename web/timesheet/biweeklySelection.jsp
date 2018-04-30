<%@ include file="/system/wbheader.jsp"%> <%@ page import="java.util.*"%> <%@ page import="java.text.*"%> <%@ page import="com.workbrain.util.*"%> <%@ page import="com.workbrain.server.jsp.*" %> <%@ page import="com.workbrain.server.jsp.JSPHelper"%> <%@ page import="com.workbrain.server.data.type.*"%> <%@ page import="com.workbrain.server.data.sql.*" %> <%@ page import="com.workbrain.app.jsp.action.timesheet.*"%> <%@ page import="com.workbrain.app.jsp.action.dailytimesheet.TimesheetHelper"%> <%@ page import="com.workbrain.app.jsp.action.AbstractActionFactory"%> <%@ page import="com.workbrain.sql.*" %>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetConstants" %>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMWTShelper"%>

<script type='text/javascript'>

    function onSubmit( url ) {
		document.forms[0].Go.disabled=true;

		if ( url && url!='' && url.charAt(0) == '/' )
			url = '<%= request.getContextPath() %>' + url;

		document.forms[0].action=url;
    	document.forms[0].submit();
    }

    function onALL( elem ) {
		document.forms[0].elements[elem].value='ALL';
		document.forms[0].elements[elem+'_label'].value='ALL';
    }
</script>
<%! private static int MFRM_ID = -99;
	private static String MFRM_ID_STR = "-99";
	private String timesheetMfrmId = "-99";
%>
<% 	/*MFRM_ID = CSMWTShelper.getMfrmId(request);
	MFRM_ID_STR = MFRM_ID + "";*/
	MFRM_ID_STR = request.getParameter("mfrm_id");
	try{
	MFRM_ID = Integer.parseInt(MFRM_ID_STR);
	}
	catch(Exception e){MFRM_ID = -99;}

	timesheetMfrmId= request.getParameter("timesheetMfrmId");
	if(timesheetMfrmId == null) timesheetMfrmId= "-99";

%>

<wb:page maintenanceFormId='<%=MFRM_ID%>'> <span class=headingPageMedium><wb:localize id='WEEKLY_TIMESHEET' >Bi-Weekly Timesheet</wb:localize></span> <br><br> <%
LocalizationHelper lh = new LocalizationHelper(pageContext);
class Params {
  String getDefaultUI(String id, PageContext pc, int formId) throws Exception{
    FieldDescription fd = SQLDataDictionary.get(JSPHelper.getWebContext(pc).getConnection()).getFieldDescription(id, formId);

    if (fd == null) {
	  return "StringUI";
    } else {
      return fd.getFieldUI().getName();
    }
  }
}

Params objParams = new Params();
String controlType;
%> <wb:useBean id="select" type="com.workbrain.app.jsp.action.timesheet.SelectionPage" attribute="timesheet.select"> <%
String bypassWTSSelect = request.getParameter("BYPASS_WTS_SELECT");
String currEmp = request.getParameter("CURR_EMP");
if (null != bypassWTSSelect && bypassWTSSelect.equalsIgnoreCase("y")) {
	if (null != currEmp && currEmp.equalsIgnoreCase("y")){
		String url = "/timesheet/CSMbiwtsAction.jsp?action=CSMLoadEmployeeAction&WEEK_START_DATE=-99&AUTH_SELECT=0&FLAG=M&INC_EMPS=" + select.getWbuId();
		%><wb:forward page='<%=url%>' /> <%
	}else{
		%><wb:forward page='<%="/timesheet/CSMbiwtsAction.jsp?action=CSMLoadEmployeeAction&WEEK_START_DATE=-99&AUTH_SELECT=0&FLAG=M"%>' /> <%
	}
}else{
%> <wba:table caption="Selection Parameters" captionLocalizeIndex="Selection_Parameters">
<tr> <%
	boolean canSeeSelfOnly = false;
	if ( lh.isControlVisible("INC_EMPS", MFRM_ID) ) {
	        canSeeSelfOnly = TimesheetHelper.isOnlyAllowedToSeeSelf(
	             JSPHelper.getWebLogin(request).getUserId(),
	             JSPHelper.getConnection(request));
	%> <wb:define id="currentEmpId">#page.property.employeeId#</wb:define>

	<th><wb:localize id="Employee:">Employee:</wb:localize></th>
	<td>
		<wb:localize id="WWE_INC_ID" type="field" overrideId='<%=MFRM_ID_STR%>'>Employee
		</wb:localize><br>
		<%if (canSeeSelfOnly) {
	        out.print(lh.getUI("WWE_INC_ID", "INC_EMPS", currentEmpId.toString(), null, "view", MFRM_ID)); %>
		<input type = 'hidden' name='INC_EMPS' value = '<wb:get id="currentEmpId"/>'>
		<input type = 'hidden' name='SELECTED_EMP' value= 'AAA'>
		<%} else {%>
			<wb:controlField submitName="INC_EMPS" id='WWE_INC_ID' cssClass="inputField" overrideId='<%=MFRM_ID_STR%>' >
			<wb:get id="INC_EMPS_S" scope="session" default=""/>
			</wb:controlField>
		<%}%>
	</td>
	<%} %>
	<%if (lh.isControlVisible("INC_TEAMS", MFRM_ID) && !canSeeSelfOnly) { %>
	<td>
		<wb:localize id="WBT_INC_ID" type="field" overrideId='<%=MFRM_ID_STR%>'>Team</wb:localize>
		<br>
		<wb:controlField submitName='INC_TEAMS' id='WBT_INC_ID' cssClass="inputField" overrideId='<%=MFRM_ID_STR%>'>
			<wb:get id="INC_TEAMS_S" scope="session" default=""/>
		</wb:controlField>
	</td>
	<%}%>
	<%if (lh.isControlVisible("INC_SUB_TEAMS", MFRM_ID) && !canSeeSelfOnly) { %>
	<td>
		<wb:localize id="SUB_TEAM_ID" type="field" overrideId='<%=MFRM_ID_STR%>'>Include Sub Team</wb:localize>
		<wb:controlField submitName='INC_SUB_TEAMS' id='SUB_TEAM_ID' cssClass="inputField" overrideId='<%=MFRM_ID_STR%>'><wb:get id="INC_SUB_TEAMS_S" scope="session" default=""/>
		</wb:controlField>
	</td>
	<%}%>
</tr>

<%if ( lh.isControlVisible("WTSWEEKSTARTDATE_ID", MFRM_ID) ) { %>
<th><wb:localize id="Dates:">Dates:</wb:localize></th>
<td><wb:localize id="WEEKSTARTDATE_ID" type="field" overrideId="<%=MFRM_ID_STR%>">Week Start Date</wb:localize><br> <wb:define id="uiParameter"/>
<% if( JSPHelper.getWebSession(request).isConfigOn() ) { %>
	<wb:localize id="Period" overrideId='<%=MFRM_ID_STR%>'>Period</wb:localize>
	<wb:localize id="Manual_Date_Range" overrideId='<%=MFRM_ID_STR%>'>Manual Date Range</wb:localize>
	<wb:localize id="Current_PayPeriod" overrideId='<%=MFRM_ID_STR%>'>Current Pay Period</wb:localize> <br>
<% } %>
<wb:secureContent securityName='WTSWEEKSTARTDATE_ID'>
<select name="WEEK_START_DATE" class="inputField" >
<%
	Date date = select.getWeekStartDate();

	    for( int i = -12; i <= 12; i++ ) {
            Date start = DateHelper.addDays( date, i * CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET );
            Date end = DateHelper.addDays( start, CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET -1 );
%> <option value=<%=i%><%=i==0?" selected":""%>>
		<wb:controlField id='WEEKSTARTDATE_ID' overrideId='<%=MFRM_ID_STR%>' mode='view'><%=DatetimeType.FORMAT.format(start)%></wb:controlField> - <wb:controlField id='WEEKSTARTDATE_ID' overrideId='<%=MFRM_ID_STR%>' mode='view'><%=DatetimeType.FORMAT.format(end)%></wb:controlField>&nbsp; <%
            if( i == 0 ) {
%> <wb:localize id="Current_PayPeriod" overrideId='<%=MFRM_ID_STR%>'>Current Pay Period</wb:localize> <%
            } else if( i > 0 ) {
%> <wb:localize id="Period" overrideId='<%=MFRM_ID_STR%>'>Period</wb:localize> +<%=i%> <%
            } else {
%> <wb:localize id="Period" overrideId='<%=MFRM_ID_STR%>'>Period</wb:localize> <%=i%> <%
            }
	    out.print( "</option>" );
	}
%> </select> </wb:secureContent> </td> <%}%> </tr> </wba:table><br> <%}%> <%--<wba:button type="button" onClick="onSubmit('/timesheet/weeklyModValidation.jsp')" label="Go" labelLocalizeIndex="Go"/> --%> <%out.print(lh.getButton("Go","Go","Go","validateFormFields('onSubmit(\\'/timesheet/CSMbiwtsAction.jsp?action=CSMLoadEmployeeAction&PAYGROUP_START_DATE="+ select.getWeekStartDate().getTime() +"&mfrm_id=" + timesheetMfrmId + "&sel_mfrm_Id="+ MFRM_ID_STR + "\\')'); return false;",null,-1,false,false,"","","",""));%> </wb:useBean> </wb:page>