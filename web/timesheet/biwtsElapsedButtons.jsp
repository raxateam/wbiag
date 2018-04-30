<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="com.workbrain.app.jsp.action.timesheet.ElapsedTimeLine"%>
<%@ page import="com.workbrain.util.LocalizationHelper"%>
<%@ page import="com.workbrain.server.jsp.security.Permission"%>
<%@ page import="com.workbrain.server.jsp.taglib.util.LocalizableTag"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMWTShelper"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetConstants"%>
<%!
public static int MFRM_ID = -99;
%>
<%
MFRM_ID = CSMWTShelper.getMfrmId(request);
%>

<wb:page maintenanceFormId='<%=MFRM_ID%>'>
<wb:useBean id="timesheet" attribute="timesheet" type="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetPage">
  <wb:useBean id="helper" type="com.workbrain.app.jsp.action.timesheet.WTSPresentationHelper" property='helper'>
<%
LocalizationHelper lh = new LocalizationHelper(pageContext);
boolean ownsTimesheet = Integer.parseInt(JSPHelper.getWebLogin(request).getEmployeeId()) == timesheet.getEmployee().getEmpId();
boolean hasDistributedTime = false;

List elapsedTimeLines = timesheet.getElapsedTime().getElapsedTimeLines();
for (int index = 0; index < elapsedTimeLines.size(); index++) {
	if (((ElapsedTimeLine)elapsedTimeLines.get(index)).getDistributed()) {
		hasDistributedTime = true;
		break;
	}
}
%>
    <div id=pageactions> <!-- SUBMIT BUTTON -->
		<span class=action>
			<wba:button name="Submit" label= "Save for Now" labelLocalizeIndex="WTSSaveSubmit" onClick="submitTimesheet();" disabled='<%=timesheet.getReadOnly() && ownsTimesheet ?"true":"false"%>'/>
		</span>

		<wb:secureContent securityName='CSM_SAVE_VERBIAGE'>
			<wb:localize id='CSM_SAVE_VERBIAGE'><%=CSMBiWeeklyTimeSheetConstants.EMP_SAVE_VERBIAGE%></wb:localize>
		</wb:secureContent>
		<br><br><br>
		<span class=action>
			<wba:button name="SubmitForApproval" label="Submit for Approval" labelLocalizeIndex="WTStoSupervisor" onClick='<%=(timesheet.containsOverrideErrors() || timesheet.containsWrksErrors()) && helper.forceCleanSubmit() ? "errorPopup();return false;" : "submitToSupervisor();return false;"%>' disabled='<%=timesheet.getReadOnly() && ownsTimesheet ?"true":"false"%>'/>
		</span>
		<wb:secureContent securityName='CSM_SUBMIT_VERBIAGE'>
			<wb:localize id='CSM_SUBMIT_VERBIAGE' ><%=CSMBiWeeklyTimeSheetConstants.EMP_SUBMIT_VERBIAGE%></wb:localize>
		</wb:secureContent>
    </div><!-- id=pageactions -->
  </wb:useBean><!-- helper -->
</wb:useBean><!-- timesheet -->
</wb:page>