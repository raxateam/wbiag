<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="java.text.*"%>
<%@ page import="java.lang.*"%>
<%@ page import="java.util.Date"%>
<%@ page import="java.sql.*"%>
<%@ page import="com.workbrain.security.SecurityService"%>
<%@ page import="com.workbrain.app.ta.db.TimeCodeAccess"%>
<%@ page import="com.workbrain.app.ta.db.CodeMapper"%>
<%@ page import="com.workbrain.app.ta.model.TimeCodeData"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.app.jsp.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.jsp.action.timesheet.*"%>
<%@ page import="com.workbrain.app.jsp.workbrain.overrides.*"%>
<%@ page import="com.workbrain.server.data.type.*"%>
<%@ page import="java.text.*, com.workbrain.util.StringHelper"%>
<%@ page import="com.workbrain.server.jsp.taglib.util.LocalizableTag"%>
<%@ page import="com.workbrain.server.data.sql.FieldDescription,com.workbrain.server.data.sql.SQLDataDictionary,java.sql.Connection"%>
<%@ page import="org.apache.log4j.*"%>
<%@ page import="com.workbrain.server.jsp.security.Permission"%>
<%@ page import="com.workbrain.security.SecurityService"%>
<%@ page import="com.workbrain.server.jsp.locale.LocalizationDictionary"%>
<%@ page import="com.workbrain.tool.locale.DataLocException"%>
<%@ page import="com.workbrain.util.callouts.TimeSheetUICallout" %>
<%@ page import="com.workbrain.util.callouts.CalloutFactory" %>
<%@ page import="com.workbrain.app.jsp.action.TimesheetUtil" %>
<%@ page import="com.workbrain.app.jsp.action.dailytimesheet.TimesheetConstants"%>
<%@ page import="com.workbrain.server.WorkbrainParametersRetriever"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetConstants"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMWTShelper"%>

<%!
private static Logger logger = Logger.getLogger("jsp.biweeklyTimeSheet.CSMBiweeklyTimesheet");
private static int MFRM_ID = -99;
private static String MFRM_ID_STR = "-99";
%>
<%
	MFRM_ID = CSMWTShelper.getMfrmId(request);
	MFRM_ID_STR = MFRM_ID + "";
%>
<wb:page submitAction='<%=CSMWTShelper.getActionURL(CSMBiWeeklyTimeSheetConstants.ACTION_SUBMIT_TIMESHEET)%>' maintenanceFormId='<%=MFRM_ID%>' uiPathName='Weekly Timesheet' uiPathNameId='WEEKLY_TIMESHEET'>

<%!
    class VisibleFlagsOrUDFs {

        private Vector      visibleFlags        = new Vector();
        private Vector      visibleUDFs         = new Vector();
        private Vector      visibleFlagBrk      = new Vector();
        private Vector      visibleFlagRecall   = new Vector();
        private String      controlType         = "";
        private PageContext pc;

        public VisibleFlagsOrUDFs(PageContext myPc) throws Exception {
            this.pc = myPc;
            visibleFlags.setSize(5);
            visibleUDFs.setSize(10);
            visibleFlagBrk.setSize(1);
            visibleFlagRecall.setSize(1);
            for(int i=0; i < visibleFlags.size(); i++){
                controlType = getControlType("WTSFLAG_ID"+(i+1));
                if (controlType.equals("HiddenUI")) {
                    visibleFlags.set(i,"N");
                } else {
                    visibleFlags.set(i,"Y");
                }
            }

            for(int i=0; i < visibleUDFs.size(); i++){
                controlType = getControlType("WTSUDF_ID"+(i+1));
                if (controlType.equals("HiddenUI")) {
                    visibleUDFs.set(i,"N");
                } else {
                    visibleUDFs.set(i,"Y");
                }
            }

            controlType = getControlType("WTSFLAGBRK_ID");
            if (controlType.equals("HiddenUI")) {
                visibleFlagBrk.set(0,"N");
            } else {
                visibleFlagBrk.set(0,"Y");
            }

            controlType = getControlType("WTSFLAGRECALL_ID");
            if (controlType.equals("HiddenUI")) {
                visibleFlagRecall.set(0,"N");
            } else {
                visibleFlagRecall.set(0,"Y");
            }
        }

        private String getControlType(String sFieldName){
            String retVal;
          try{
              FieldDescription fldDescr = SQLDataDictionary.get(JSPHelper.getWebContext(pc).getConnection()).getFieldDescription(sFieldName,MFRM_ID);
              retVal = (fldDescr.getFieldUI().getName()==null?"StringUI":fldDescr.getFieldUI().getName());
          }catch(Exception e){
              retVal = "StringUI";
          }
          return retVal;
        }

        public String getVisibleFlags(){
            StringBuffer tempStr = new StringBuffer();
            for(int i=0; i < visibleFlags.size(); i++){
                tempStr.append(visibleFlags.get(i).toString());
            }
            return tempStr.toString();
        }

        public String getVisibleUDFs(){
            StringBuffer tempStr = new StringBuffer();
            for(int i=0; i < visibleUDFs.size(); i++){
                tempStr.append(visibleUDFs.get(i).toString());
            }
            return tempStr.toString();
        }

        public String getVisibleFlagBrk(){
            StringBuffer tempStr = new StringBuffer();
            for(int i=0; i < visibleFlagBrk.size(); i++){
                tempStr.append(visibleFlagBrk.get(i).toString());
            }
            return tempStr.toString();
        }

        public String getVisibleFlagRecall(){
            StringBuffer tempStr = new StringBuffer();
            for(int i=0; i < visibleFlagRecall.size(); i++){
                tempStr.append(visibleFlagRecall.get(i).toString());
            }
            return tempStr.toString();
        }
    }
%> <%
	DBConnection conn = JSPHelper.getConnection(request);
	LocalizationHelper lh = new LocalizationHelper(pageContext);
	LocalizationDictionary ld = LocalizationDictionary.get();
	int languageId = JSPHelper.getWebContext(pageContext).getWebLocale().getLanguageId();

    VisibleFlagsOrUDFs vfu = new VisibleFlagsOrUDFs(pageContext);

    request.setAttribute("WRKS_ID_LIST", "");
    //out.println("Flags = " + vfu.getVisibleFlags());
    //out.println("UDFs = " + vfu.getVisibleUDFs());
    String hideOverrides = WorkbrainParametersRetriever.getString("/system/timesheet/TS_HIDE_OVR_BY_STATUS", "", Boolean.FALSE);
    List hideOverridesList;
    if (!StringHelper.isEmpty(hideOverrides)) {
        hideOverrides = hideOverrides.toUpperCase();
        hideOverridesList = StringHelper.detokenizeStringAsList(hideOverrides, ",", true);
    }else{
        hideOverridesList = new ArrayList();
    }
%>
<wb:include page="biwtsJavaScript.jsp"/>

<wb:config id="SELECT_EMPLOYEES_TO_APPLY_OVERRIDE"/>
<wb:config id="SELECT_DATES_TO_APPLY_OVERRIDE"/>
<wb:config id="YOUR_TIMESHEET_HAS_BEEN_SUBMITTED"/>
<wb:config id="ERROR_ON_SUBMIT"/>
<wb:config id="ACTION_STATUS_TIMESHEET_SUBMITTED"/>
<wb:config id="ACTION_STATUS_SUBMIT_TO_SUPERVISOR"/>
<wb:config id="ACTION_STATUS_COPY_FROM_PREVIOUS"/>
<wb:config id="JAVASCRIPT_Please_fill_in_an_appropriate_Timecode_for_the_Elapsed_Time_line_"/>
<wb:config id="JAVASCRIPT_,_then_press_submit."/>
<wb:config id="YOU_CANNOT_ENTER_OVERLAPPING_BREAK_NONWORK"/>
<wb:config id="YOU_CANNOT_AUTHORIZE_ENTIRE_WEEK"/>
<wb:config id="WRKD_DELETE"/> <wb:config id="Y"/>

<script>
  var JSVAR_SELECT_EMPLOYEES_TO_APPLY_OVERRIDE = "<wb:localize id="SELECT_EMPLOYEES_TO_APPLY_OVERRIDE" ignoreConfig="true" escapeForJavascript="true">You must select employees to apply this override</wb:localize>";
  var JSVAR_SELECT_DATES_TO_APPLY_OVERRIDE     = "<wb:localize id="SELECT_DATES_TO_APPLY_OVERRIDE" ignoreConfig="true" escapeForJavascript="true">You must select dates to apply this override</wb:localize>";
</script>
<%
if( JSPHelper.getWebSession(request).isConfigOn() ) {
%>
<br> <wb:localize id='Confirmed_Leave'>Timesheet has been modified since it was last saved. All changes will be lost if you continue.</wb:localize>
<br> <wb:localize id='To_Supervisor'>You have not checked your timesheet as "Submitted". Are you sure you want to forward your timesheet? Press "OK" to send your timesheet otherwise press "CANCEL".</wb:localize>
<br> <wb:localize id='Sup_Comments'>If you want to send comments along with the Timesheet, enter them below and press "Ok". Press "CANCEL" if you want to send without comments.</wb:localize>
<br> <%
}
%>
<wb:define id="curEmpId"/>
<wb:define id="w_ovr_type_ID,w_ovr_type_showAsLink,toSupMsg,supComment,leaveMsg,weekDay,ovr_type_options,thisWorkDate,empListUIParameter"/>
<wb:set id="toSupMsg"><wb:localize id='To_Supervisor' escapeForJavascript='true'>You have not checked your timesheet as "Submitted".  Are you sure you want to forward your timesheet?  Press "OK" to send your timesheet otherwise press "CANCEL".</wb:localize></wb:set>
<wb:set id="supComment"><wb:localize id='Sup_Comments' escapeForJavascript='true'>If you want to send comments along with the Timesheet, enter them below and press "Ok".  Press "CANCEL" if you want to send without comments.</wb:localize></wb:set>
<wb:set id="leaveMsg"><wb:localize id='Confirmed_Leave' escapeForJavascript='true'>Timesheet has been modified since it was last saved.  All changes will be lost if you continue.</wb:localize></wb:set>

<script Language="JavaScript">
function supervisorComment(){
    var returnMessage = '';
<%  if( !JSPHelper.getWebSession(request).isConfigOn() ) {%>
        returnMessage = '<%=supComment.toString()%>';
<%  } %>
    return returnMessage;
}

function getToSupervisorMsg() {
    var returnMessage = '';
<%  if( !JSPHelper.getWebSession(request).isConfigOn() ) { %>
        returnMessage = '<%=toSupMsg.toString()%>';
<%  } %>
    return returnMessage;
}

function getConfirmedLeaveMsg() {
    var returnMessage = '';
<%  if( !JSPHelper.getWebSession(request).isConfigOn() ) { %>
        returnMessage = '<%=leaveMsg.toString()%>';
<%  } %>
    return returnMessage;
}
</script>

<wb:pageOnLoad id="commentsFlags">
if( hasAnyFlagsOrUDFs() ) {
    addOvrnMark( document.forms[0].FlagsBtn );
}
if( hasAnyComments() ) {
    addOvrnMark( document.forms[0].CommentsBtn );
}
</wb:pageOnLoad>

<script src="<%= request.getContextPath() %>/overrides/ovrCode.js"></script>

<input type=hidden name='DOC_CHANGED'/>
<input type=hidden name='VISIBILE_FLAGS' value='<%=vfu.getVisibleFlags()%>'/>
<input type=hidden name='VISIBILE_UDFS' value='<%=vfu.getVisibleUDFs()%>'/>
<input type=hidden name='VISIBILE_FLAG_BRK' value='<%=vfu.getVisibleFlagBrk()%>'/>
<input type=hidden name='VISIBILE_FLAG_RECALL' value='<%=vfu.getVisibleFlagRecall()%>'/>

<wb:sql createDataSource="WtsOvrTypes"> SELECT OVERRIDE_TYPE.OVRTYP_ID, OVRTYP_NAME, OVRTYP_LINK FROM OVERRIDE_TYPE WHERE OVRTYP_WKTIMESHEET = 'Y' AND (OVERRIDE_TYPE.OVRTYP_ID <= 300 OR OVERRIDE_TYPE.OVRTYP_ID >= 400) ORDER BY OVRTYP_ORDER, OVRTYP_NAME </wb:sql>
<wb:define id="userGroupId"><wb:getPageProperty id="userGroupId"/></wb:define>

<%
String locNewEdit = lh.getText("New_Edit", LocalizableTag.CONFIG_MESSAGE, "New Edit");
StringBuffer ovrTypeOptions = new StringBuffer("<option selected value='NEW'>");
StringBuffer ovrTypeOptionsSc = new StringBuffer("<option selected value='NEW'>New Edit");

PreparedStatement pstmt = null;
ResultSet rs = null;
StringBuffer querySB = new StringBuffer();
querySB.append("SELECT OVERRIDE_TYPE.OVRTYP_ID, OVRTYP_NAME FROM OVERRIDE_TYPE,");
querySB.append(" OVERRIDE_TYPE_GRP WHERE OVRTYP_ACTIVE = 'Y' AND OVERRIDE_TYPE_GRP.WBG_ID= ?");
querySB.append(" AND OVERRIDE_TYPE.OVRTYP_ID=OVERRIDE_TYPE_GRP.OVRTYP_ID");
querySB.append(" AND OVERRIDE_TYPE_GRP.WBP_ID=1 AND (OVERRIDE_TYPE.OVRTYP_ID < 300 ");
querySB.append(" OR OVERRIDE_TYPE.OVRTYP_ID >= 400) ");
querySB.append(" AND (OVERRIDE_TYPE.OVRTYP_WKTIMESHEET = 'Y')");
querySB.append(" ORDER BY OVRTYP_ORDER, OVRTYP_NAME");

ovrTypeOptions.append(locNewEdit);
ovrTypeOptions.append("</option>");

try {
    pstmt = JSPHelper.getConnection(request).prepareStatement(querySB.toString());
    pstmt.setInt(1, Integer.parseInt(userGroupId.toString()));
    rs = pstmt.executeQuery();
    while (rs.next()) {
		String ovrName = rs.getString("OVRTYP_NAME");
		String locOvrName = null;
		try {
			locOvrName = ld.localizeData(conn, ovrName, "OVERRIDE_TYPE", "OVRTYP_NAME", languageId);
		} catch (DataLocException e) {
			StringBuffer sb = new StringBuffer();
			sb.append("weeklyTimesheet.jsp: Could not localize ");
			sb.append(ovrName);
			sb.append(" from the OVRTYP_NAME column of the OVERRIDE_TYPE table.");
			logger.debug(sb.toString());
			locOvrName = ovrName;
		}
        ovrTypeOptions.append("<option value='");
        ovrTypeOptions.append(rs.getString("OVRTYP_ID"));
        ovrTypeOptions.append("'>");
        if (locOvrName.equals("")) {
			ovrTypeOptions.append(JSPHelper.encodeHTML(ovrName));
        } else {
        	ovrTypeOptions.append(JSPHelper.encodeHTML(locOvrName));
        }
        ovrTypeOptions.append("</option>");
    }
    session.setAttribute("SELECT_OVRTYP_ID_TS", ovrTypeOptions.toString());
    if (session.getAttribute("SELECT_OVRTYP_ID_SC") == null) {
        StringBuffer queryScSB = new StringBuffer();
        queryScSB.append("SELECT OVERRIDE_TYPE.OVRTYP_ID, OVRTYP_NAME FROM OVERRIDE_TYPE,");
        queryScSB.append(" OVERRIDE_TYPE_GRP WHERE OVRTYP_ACTIVE = 'Y' AND OVRTYP_SCHEDULE = 'Y'");
        queryScSB.append(" AND OVERRIDE_TYPE_GRP.WBG_ID= ?");
        queryScSB.append(" AND OVERRIDE_TYPE.OVRTYP_ID=OVERRIDE_TYPE_GRP.OVRTYP_ID");
        queryScSB.append(" AND OVERRIDE_TYPE_GRP.WBP_ID=1 AND (OVERRIDE_TYPE.OVRTYP_ID < 300");
        queryScSB.append(" OR OVERRIDE_TYPE.OVRTYP_ID >= 400) ORDER BY OVRTYP_ORDER, OVRTYP_NAME");
        SQLHelper.cleanUp(pstmt, rs);
        pstmt = JSPHelper.getConnection(request).prepareStatement(queryScSB.toString());
        pstmt.setInt(1, Integer.parseInt(userGroupId.toString()));
        rs = pstmt.executeQuery();
        while (rs.next()) {
            ovrTypeOptionsSc.append("<option value='");
            ovrTypeOptionsSc.append(rs.getString("OVRTYP_ID"));
            ovrTypeOptionsSc.append("'>");
            ovrTypeOptionsSc.append(rs.getString("OVRTYP_NAME"));
        }
        session.setAttribute("SELECT_OVRTYP_ID_SC", ovrTypeOptionsSc.toString());
    }
} catch (Exception e) {
    logger.error("weeklyTimesheet.jsp", e);
    throw new RuntimeException(e.getMessage());
} finally {
    SQLHelper.cleanUp(pstmt, rs);
}
%>

<style type="text/css"> @import URL(<%= request.getContextPath() %>/timesheet/timesheet.css); </style>

<wb:useBean id="timesheet" attribute="timesheet" type="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetPage">
<%
if (request.getParameter("WEEK_START_DATE") != null) session.setAttribute("WEEK_START_DATE_S", request.getParameter("WEEK_START_DATE"));
if (request.getParameter("INC_PGS") != null) session.setAttribute("INC_PGS_S", request.getParameter("INC_PGS"));
if (request.getParameter("START_DATE") != null) session.setAttribute("START_DATE_S", request.getParameter("START_DATE"));
if (request.getParameter("INC_CGS") != null) session.setAttribute("INC_CGS_S", request.getParameter("INC_CGS"));
if (request.getParameter("INC_SPS") != null) session.setAttribute("INC_SPS_S", request.getParameter("INC_SPS"));
if (request.getParameter("INC_SUB_TEAMS") != null) session.setAttribute("INC_SUB_TEAMS_S", request.getParameter("INC_SUB_TEAMS"));
if (request.getParameter("INC_TEAMS") != null) session.setAttribute("INC_TEAMS_S", request.getParameter("INC_TEAMS"));
if (request.getParameter("INC_EMPS") != null) session.setAttribute("INC_EMPS_S", request.getParameter("INC_EMPS"));
session.setAttribute("EXCLUDE_DAILY_S", (request.getParameter("EXCLUDE_DAILY") == null) ? "N" : request.getParameter("EXCLUDE_DAILY"));
boolean fromSupervisorTimesheet = false; //default values
boolean fromPaySummary = false; //Necessary to declare it here.

SimpleDateFormat df = new SimpleDateFormat("");
df.applyPattern(TimesheetConstants.TIMESHEET_DATE_FORMAT);
String strOneDayTeam = "F";
boolean isCopied = timesheet.getCopied();
int empId = timesheet.getEmployee().getEmpId();
session.setAttribute("L_EMP_ID", new Integer(timesheet.getEmployee().getEmpId()).toString());
session.setAttribute("L_WEEK_START_DATE", conn.encodeTimestamp(timesheet.getWeekStartDate()));
%>




<%// ###REMOVE### This bean is not neeeded %>

<wb:useBean id="empselect" type="com.workbrain.app.jsp.action.timesheet.SelectionPage" attribute="timesheet.loademployeselection.select">

<%
//variables need to be declared outside of the useBean context
fromSupervisorTimesheet = empselect.getPreviousPage().equals(SelectionPage.SUPERVISOR_SUMMARY);
fromPaySummary = empselect.getPreviousPage().equals(SelectionPage.PAY_SUMMARY);
%> </wb:useBean> <%-- selection page (timesheet.loademployeselection.select)--%>

<%// ###REMOVE### This bean is not neeeded %>




<wb:set id='curEmpId'><%=timesheet.getEmployee().getEmpId()%></wb:set>
<input type=hidden name='EMP_ID_TEST' value='<%=timesheet.getEmployee().getEmpId()%>'>
<input type=hidden name='COPY_OVR_DATES' value=','>
<input type=hidden name='START_DATE_WEEK' value=','>
<input type=hidden name='OVRTYP_ID_9999' value=''>
<input type=hidden name='SUMRECNO' value='7'>
<input type=hidden name='WS_WRKS_WORK_DATE_0' value='20021104 000000'>
<input type=hidden name='APPLY_DATES' value=','>
<input type=hidden name='COPY_OVR_IDS' value=','>
<input type=hidden name='COPY_OVR_EMPS' value=','>
<input type=hidden name='COPY_OVR_DATES' value=','>
<input type=hidden name='RECORD_LIST' value=''>
<input type=hidden name='RESET_RECORDS' value=''>
<input type=hidden name='SUM_RECNO' value=''>
<input type=hidden name='HIDE_OVERRIDES_ON_SUBMIT' value=''>
<input type=hidden name='SUBMIT_OVERRIDES' value=''>
<input type=hidden name='CALC_OVERRIDES' value=''>
<input type=hidden name='WS_DELETE_CLOCKS_9999' value='~'>
<input type=hidden name='WS_WRKS_ID_9999' value=''>
<input type=hidden name='ES_OVERRIDE_9999' value='||'>
<input type=hidden name='MULTIPLE_OVERRIDES' value=''>
<input type=hidden name='DAY_BEFORE_9999' value=''>
<input type=hidden name='TIMESHEET' value='T'>
<input type=hidden name='BLANK_REC_CHANGED' value='F'>
<input type='hidden' name='COPY_OVERRIDE' value='F'>
<input type='hidden' name='FROM_SUPERVISOR_SUMMARY' value='<%=fromSupervisorTimesheet?"T":"N"%>'>
<input type='hidden' name='FROM_PAY_SUMMARY' value='<%=fromPaySummary?"T":"N"%>'>

<%TimeSheetUICallout tcallout = CalloutFactory.getInstance().getTimesheetUICallout();%>
<%=tcallout.generateWeeklyTimesheetIncludes(request)%>
<%=tcallout.generateWeeklyTimesheetHiddenFields(request)%>

<wb:useBean id="employee" type="com.wbiag.app.jsp.action.biweeklytimesheet.CSMEmployeeModel" property="employee" >

<%
String defDateFormat = JSPConstants.DEFAULT_DATE_FORMAT;
String tempStringX = timesheet.getWeekStartDateString();
String tempLinkq = "document.location='" + request.getContextPath() + "/overrides/ovrEmployee.jsp?EMP_ID_0=" + employee.getEmpId() + "&OVR_DATE_0=" + tempStringX + "&SUBMIT_TYPE=WEEKLYTIMESHEET';return false;" ;
String sbLink = StringHelper.searchReplace(tempLinkq," ", "+");
String displayName = employee.getFullName() + " (" + employee.getEmpName() + ")";
//CSM specific employee data
String empName = employee.getEmpName();
String fullName = employee.getFullName();
String deptNo = employee.getDeptNo();
String employeePct = employee.getEmployeePct();
String workgroup = employee.getWorkgroup();
String type = employee.getType();
String jobClass = employee.getJobClass();
String jobClassName = employee.getJobClassName();
String schedule = employee.getSchedule();
String eValues = StringHelper.searchReplace(timesheet.getEmployeeValues(), "'", "\\'");
String eLabels = StringHelper.searchReplace(timesheet.getEmployeeLabels(), "'", "\\'");
%>

<wb:set id="empListUIParameter"><%="valueList='" + eValues + "' labelList='" + eLabels +"'"%></wb:set>

<div class="header">
	<table>
		<tr>
			<td>
			  <div class="heading">
				<H3><wb:localize id='WEEKLY_TIMESHEET' >Biweekly Timesheet</wb:localize></H3>
			  </div>
			</td>
		</tr>
		<tr>
			<td>
				<div class=dateRangeControl>
					<%
						boolean canActOnDateRangeControl = lh.getPermission("WTS_SEC_DATE_RANGE_SELECTION").allowAct();

					// Previous Week Button
					out.print(lh.getSecurityLink("WTS_SEC_DATE_RANGE_SELECTION"));
				   	out.print(lh.getConfigLink("CSM_WTSPreviousWeek", LocalizableTag.CONFIG_MESSAGE));
				   	if (canActOnDateRangeControl) {
				    %> <script Language="JavaScript">
				    var prevWeekButtonClicked = false;
				    function onClickPrevWeekButton( url, msg ) {
				      if( prevWeekButtonClicked ) {
				        //alert("Please don't click this link more than once.");
				        return;
				      }

				      prevWeekButtonClicked = true;
				      confirmedLeave( url, msg );
				    }
				    </script><%

						out.print("\n<div class=prevLink>");
						out.print("<a href=# onClick=\"onClickNextWeekButton('");
						out.print(CSMBiWeeklyTimeSheetConstants.ACTION_URL);
						out.print("?action=CSMLoadEmployeeAction&mfrm_id=");
						out.print(MFRM_ID_STR);
						out.print("&NEWWEEK=");
					    out.print(StringHelper.searchReplace(DatetimeType.FORMAT.format(timesheet.getPrevWeek()), " ", "%20"));
						out.print("&INC_EMPS=" + empId);
						out.print("');return false;\" title='");
						out.print(JSPHelper.encodeHTML(lh.getText("CSM_WTSPreviousWeek", LocalizableTag.CONFIG_MESSAGE, "Previous Period")));
						out.print("'></a></div>\n");
					}
					%>
					<div class=range><div class=start>
						<%
						if( JSPHelper.getWebSession(request).isConfigOn() ) {
						%>
							<wb:localize id="WWDATERANGE_ID" type="field" overrideId='<%=MFRM_ID_STR%>'></wb:localize>
						<%
						}
						String CSM_DATERANGE_ONCHANGE_STR = "confirmedLeave('"+CSMBiWeeklyTimeSheetConstants.ACTION_URL + "?action=CSMLoadEmployeeAction&mfrm_id=" + MFRM_ID_STR + "&NEWWEEK='+escape(getElement('NEW_WEEK_DATE').value));return false;";

						%>

						<span class=ui>
							<wb:controlField id="WWDATERANGE_ID" secure="false" cssClass="inputField" submitName='NEW_WEEK_DATE' onChange="<%=CSM_DATERANGE_ONCHANGE_STR%>" overrideId='<%=MFRM_ID_STR%>' property='weekStartDate' mode="view"/>
						</span>
					</div></div>
					<%
					// Next Week Button
					out.print(lh.getConfigLink("CSM_WTSNextWeek", LocalizableTag.CONFIG_MESSAGE));
					if (canActOnDateRangeControl) {
					%>

					  <script Language="JavaScript">
					    var nextWeekButtonClicked = false;
					    function onClickNextWeekButton( url, msg ) {
					      if( nextWeekButtonClicked ) {
					        //alert("Please don't click this link more than once.");
					        return;
					      }

					      nextWeekButtonClicked = true;
					      confirmedLeave( url, msg );
					    }
					  </script>
					<%
					   	out.print("\n<div class=nextLink>");
					   	out.print("<a href=# onClick=\"onClickNextWeekButton('");
						out.print(CSMBiWeeklyTimeSheetConstants.ACTION_URL);
						out.print("?action=CSMLoadEmployeeAction&mfrm_id=");
						out.print(MFRM_ID_STR);
						out.print("&NEWWEEK=");
					   	out.print(StringHelper.searchReplace(DatetimeType.FORMAT.format(timesheet.getNextWeek()), " ", "%20"));
						out.print("&INC_EMPS=" + empId);
					   	out.print("');return false;\" title='");
					   	out.print(JSPHelper.encodeHTML(lh.getText("CSM_WTSNextWeek", LocalizableTag.CONFIG_MESSAGE, "Next Period")));
					   	out.print("'></a></div>\n");
					}
					%>
				</div>
			</td>
			<td>
				<%
				boolean hasNextEmp = timesheet.getNextEmpId() != -1;
				boolean hasPrevEmp = timesheet.getPrevEmpId() != -1;
				String prevLink = "confirmedLeave('" + CSMBiWeeklyTimeSheetConstants.ACTION_URL + "?action=CSMNewEmployeeAction&EMP_ID=" + timesheet.getPrevEmpId() + "'); return false;";
				String nextLink = "confirmedLeave('" + CSMBiWeeklyTimeSheetConstants.ACTION_URL + "?action=CSMNewEmployeeAction&EMP_ID=" + timesheet.getNextEmpId() + "'); return false;";

				if (hasPrevEmp || hasNextEmp) { // more than one employee selected
				%>
				<div class=employeeSelect>
					<div class="prevLink">
						<wb:config id="WTSPreviousEmployee"/>
							<a href=# <%=hasPrevEmp?"":"class=disabled"%> onClick="<%=hasPrevEmp?prevLink:""%>" title="<wb:localize id="WTSPreviousEmployee" ignoreConfig="true">Previous Employee</wb:localize>"></a>
					</div>
					<div class="select">
						<wb:useBean property="employee">
							<wb:controlField nullable="false" onChange="confirmedLeave('/timesheet/CSMbiwtsAction.jsp?action=CSMNewEmployeeAction&EMP_ID=' + document.forms[0].EMP_ID.options[document.forms[0].EMP_ID.selectedIndex].value)" submitName="EMP_ID" ui="ComboboxUI" uiParameter="#empListUIParameter# size='10'" cssClass="inputField" mode='edit' property="empId">
							</wb:controlField>
						</wb:useBean>
					</div>
					<div class="nextLink">
						<wb:config id="WTSNextEmployee"/>
							<a href=# <%=hasNextEmp?"":"class=disabled"%> onClick="<%=hasNextEmp?nextLink:""%>" title="<wb:localize id="WTSNextEmployee" ignoreConfig="true">Next Employee</wb:localize>"></a>
					</div>
				</div>
				<%}%>
			</td>
		</tr>
	</table>
	<% 	if ( fromSupervisorTimesheet &&
			(lh.getPermission("WTS_SEC_BACK_TO_SUPERVISOR_SUMMARY").allowAct()
			|| lh.isConfigOn())) {
		   	out.print("<div class=link>");
			out.print(lh.getSecurityLink("WTS_SEC_BACK_TO_SUPERVISOR_SUMMARY"));
			out.print(lh.getConfigLink("WTSBackToSupervisorSummary", LocalizableTag.CONFIG_MESSAGE, MFRM_ID));
			out.print("<a href=# onclick='backToSupervisorSummary();return false;'>");
			out.print(lh.getText("WTSBackToSupervisorSummary", LocalizableTag.CONFIG_MESSAGE, "Supervisor Summary"));
			out.print("</a></div>");
		}
		if ( fromPaySummary && (lh.getPermission("WTS_SEC_BACK_TO_PAY_SUMMARY").allowAct() || lh.isConfigOn())) {
		   	out.print("<div class=link>");
			out.print(lh.getSecurityLink("WTS_SEC_BACK_TO_PAY_SUMMARY"));
			out.print(lh.getConfigLink("WTSBackToPaySummary", LocalizableTag.CONFIG_MESSAGE, MFRM_ID));
			out.print("<a href=# onclick='backToPaySummary();return false;'>");
			out.print(lh.getText("WTSBackToPaySummary", LocalizableTag.CONFIG_MESSAGE, "Pay Summary"));
			out.print("</a></div>");
		}
		tcallout.createWeeklyTimesheetCustomButton(request, lh);
		if (timesheet.getBeyondHandsOffDate()
				&& timesheet.getBeyondSupervisorDate()
				|| JSPHelper.getWebSession(request).isConfigOn()){
	%>
			<div class=timesheetMessage>
				<%--Consider adding a link to the supervisor/hands off date if user has access to it.--%>
				<wb:secureContent securityName="HANDS_OFF_SUPERVISOR_MSG_S">
					<wb:localize id='HANDS_OFF_SUPERVISOR_MSF_L' >Timesheet cannot be edited prior to the 'Hands-Off Date' / 'Supervisor Date'.</wb:localize>
				</wb:secureContent>
			</div>
	<%
		}
		if (timesheet.getBeyondHandsOffDate()
				&& !timesheet.getBeyondSupervisorDate()
				|| JSPHelper.getWebSession(request).isConfigOn()){
	%>
			<div class=timesheetMessage>
			<%--Consider adding a link to the supervisor/hands off date if user has access to it.--%>
				<wb:secureContent securityName="HANDS_OFF_MSG_S">
				<wb:localize id='HANDS_OFF_MSF_L' >Timesheet cannot be edited prior to the 'Hands-Off Date'.</wb:localize></wb:secureContent>
			</div>
		<%}
		if (timesheet.getBeyondSupervisorDate() && !timesheet.getBeyondHandsOffDate() || JSPHelper.getWebSession(request).isConfigOn()){%>

			<div class=timesheetMessage>
				<%--Consider adding a link to the supervisor/hands off date if user has access to it.--%>
				<wb:secureContent securityName="SUPERVISOR_DATE_MSG_S"><wb:localize id='SUPERVISOR_DATE_MSF_L' >Timesheet cannot be edited prior to the 'Supervisor Date'.</wb:localize></wb:secureContent>
			</div>
		<%}
		if (timesheet.getLocked() || JSPHelper.getWebSession(request).isConfigOn()){%>
			<div class=timesheetMessage>
			<%--Consider adding a link to the supervisor/hands off date if user has access to it.--%>
				<wb:secureContent securityName='LOCKED_MSG_S'><wb:localize id='LCOKED_MSF_L' >Timesheet cannot be edited as a result of a supervisor lock.</wb:localize></wb:secureContent>
			</div>
		<%}%>
	<br>
	<table>
		<tr>
			<td><h4>
				<wb:secureContent securityName='CSM_EMPID'>
					<wb:localize id='CSM_EMPID'>Employee ID</wb:localize>
				</wb:secureContent>
			</h4></td>
			<td><wb:secureContent securityName='CSM_EMPID' showKey='false'><%=empName%></wb:secureContent></td>

			<td><h4>
				<wb:secureContent securityName='CSM_EMPNAME'>
					<wb:localize id='CSM_EMPNAME' >Employee Name</wb:localize>
				</wb:secureContent>
			</h4></td>
			<td>
				<wb:secureContent securityName='CSM_EMPNAME' showKey='false'>
					<!--a href=# onclick="<%=sbLink%>" title="<wb:localize id='WTS_EMP_DETAILS' ignoreConfig='true'>Employee Details</wb:localize>"><%=fullName%></a--> <%=fullName%>
				</wb:secureContent>
			</td>

			<!--td><h4>
				<wb:secureContent securityName='CSM_SKD'>
					<wb:localize id='CSM_SKD' >Schedule</wb:localize>
				</wb:secureContent>
			</h4></td>
			<td>
				<wb:secureContent securityName='CSM_SKD' showKey='false'>
					<%=schedule%>
				</wb:secureContent>
			</td-->
		</tr>
		<tr>
			<td><h4>
				<wb:secureContent securityName='CSM_ENDDATE'>
					<wb:localize id='CSM_ENDDATE' >Period End Date</wb:localize>
				</wb:secureContent>
			</h4></td>
			<td>
				<wb:secureContent securityName='CSM_ENDDATE' showKey='false'>
				<div class=periodEndDate>
					<%if( JSPHelper.getWebSession(request).isConfigOn() ) {%>
						<wb:localize id="WWDATERANGE_ID" type="field" overrideId='<%=MFRM_ID_STR%>'></wb:localize>
					<%}%>
					<span class=ui>
						<wb:controlField id="WWDATERANGE_ID" secure="false" cssClass="inputField" overrideId='<%=MFRM_ID_STR%>' property='weekEndDate' mode="view"/>
					</span>
				</div>
				</wb:secureContent>
			</td>
		</tr>
	</table>
</div><!--close header div-->
</wb:useBean> <%-- employee --%>

<%
// Lock & Submitted checkbox.
Permission submittedPerm = lh.getPermission("SUBMITTED");
Permission lockPerm = lh.getPermission("WTS_SEC_TOP_LOCKED_CHECKBOX");
boolean configOn = lh.isConfigOn();
boolean ownsTimesheet = Integer.parseInt(JSPHelper.getWebLogin(request).getEmployeeId()) == timesheet.getEmployee().getEmpId();

if (submittedPerm.allowView() || lockPerm.allowView() || configOn) {
	StringBuffer lockSub = new StringBuffer("<div class='lockedSubmitted'>");
	if (submittedPerm.allowView() || configOn) {
		boolean submittedDisabled = !submittedPerm.allowAct() || timesheet.getReadOnly();

		lockSub.append("<div>");
		lockSub.append(lh.getSecurityLink("SUBMITTED"));
  		lockSub.append("<input type='checkbox' name='SUBMITTED'");
  		if (timesheet.getSubmitted()) {
			lockSub.append(" checked");
  		}
  		if (submittedDisabled) {
  			lockSub.append(" disabled");
		}
		lockSub.append("/>");
		if (submittedDisabled) {
			lockSub.append("<input type='hidden' name='SUBMITTED' value='");
			lockSub.append(timesheet.getSubmitted());
			lockSub.append("'/>");
		}
		lockSub.append("<span>");
		lockSub.append(HTMLStringHelper.escapeText(lh.getText("Submitted", LocalizableTag.CONFIG_MESSAGE, "Submitted")));
		lockSub.append("</span>");
		lockSub.append("</div>");
	}

    if (lockPerm.allowView() && (!lh.getPermission("LOCKED").allowView() || ownsTimesheet) || configOn) {
		lockSub.append("<div>");
		lockSub.append(lh.getSecurityLink("WTS_SEC_TOP_LOCKED_CHECKBOX"));
		lockSub.append("<input type='checkbox' name='LOCKED' value='Y'");
		if (timesheet.getLocked()) {
   			lockSub.append(" checked");
   		}
   		lockSub.append(" disabled/><span>");
   		lockSub.append(HTMLStringHelper.escapeText(lh.getText("Locked", LocalizableTag.CONFIG_MESSAGE, "Locked")));
   		lockSub.append("</span></div>");
	}
	lockSub.append("</div>");
	out.print(lockSub.toString());
}
%>
<div>
  <wb:iterate id="summary" indexId="i" type="com.workbrain.app.jsp.action.timesheet.WorkSummaryModel" property="summaries"> <%
    // IC - this variable not used
    //String comments = summary.getComments();
    List flags = summary.getFlags();
    List udfs = summary.getUdfs();
    %>
    <%-- IC.11568 - Handling of quotes in comments --%>
    <input type=hidden name='COMMENTS_<%=i.intValue()%>' value="<%=JSPHelper.encodeHTML(summary.getComments())%>">
    <%-- END IC.11568--%>
<%
for( int j = 0; j < 5; j++ ) {
%>
    <input type=hidden name='FLAGS_<%=i.intValue()%>_<%=j%>' value='<%=flags.get(j).toString()%>'>
<%
}
%>
    <input type=hidden name='FLAG_BRK_<%=i.intValue()%>' value='<%=summary.getFlagBrk()%>'>
    <input type=hidden name='FLAG_RECALL_<%=i.intValue()%>' value='<%=summary.getFlagRecall()%>'>
<%
for( int j = 0; j < 10; j++ ) {
%>
    <input type=hidden name='UDFS_<%=i.intValue()%>_<%=j%>' value='<%=udfs.get(j).toString()%>'>
<%
}
%>
  </wb:iterate>
</div>

<!--bhacko:don't need schedule wb:include page="wtsSchedule.jsp"/-->
<wb:include page="biwtsElapsed.jsp"/>
<wb:secureContent securityName='WTS_SEC_SUMMARY' showKey='false'>
<%
if (timesheet.getShowSummary()){
%>
<!-- ================================================================================================================== -->
<wb:useBean id="summaryPage" type="com.workbrain.app.jsp.action.timesheet.SummaryPage" property="summary">
  <div class=sectionTitle>
    <h3><wb:localize id="Week Summary">Week Summary</wb:localize></h3>
  </div>
  <div id=sectionSummary> <%-- Moved the security key below to stay inside the containing DIV
    	 so that the key will move with the relative positioning of the DIV it secures. --%>
    <wb:secureContent securityName='WTS_SEC_SUMMARY' />
    <table class=wtsTable>
      <tr>
        <th>
			<img src="<%=request.getContextPath()%>/images/pixel.gif" width=45 height=1 alt=""><br>
			<wb:localize id="WRKD_TCODE_NAME_SUM" type="field" overrideId='<%=MFRM_ID_STR%>'>
				HourType
			</wb:localize>
        </th>
        <!--th>
			<img src="<%=request.getContextPath()%>/images/pixel.gif" width=70 height=1 alt=""><br>
			<wb:localize id="<%=summaryPage.getFieldNames().get(1).toString()%>" type="field" overrideId='<%=MFRM_ID_STR%>'>
				Code
			</wb:localize>
        </th-->
        <wb:iterate id="summary" indexId="day" type="com.workbrain.app.jsp.action.timesheet.WorkSummaryModel" property="summaries">
<%
	DayScheduleModel dsm = timesheet.getSchedule().getScheduleDay(day.intValue());
	boolean isOff = (((Date)DateHelper.truncateToDays( dsm.getDefStartTime() )).compareTo( dsm.getDefStartTime() ) == 0) && ((dsm.getEndTime()).equals(dsm.getDefStartTime()));
	boolean isHoliday = summary.getIsHoliday();
%>
          <th class='dayheading <%= isHoliday ? "holiday " : ""%><%=isOff ? "off " : "on "%>'>
            <img src="<%=request.getContextPath()%>/images/pixel.gif" width=50 height=1 alt=""><br>
<%
	if( JSPHelper.getWebSession(request).isConfigOn() ) {
%>
		    <wb:localize id="WWDATE1_ID" type="field" overrideId='<%=MFRM_ID_STR%>'></wb:localize>
		    <wb:localize id="WWDATE2_ID" type="field" overrideId='<%=MFRM_ID_STR%>'></wb:localize> <br>
		    <wb:config id='<%=DateHelper.getWeekDayLocalizeId(summary.getWorkDate(),true)%>'/>
<%
	}
%>
		    <wb:set id = "weekDay">
		      <wb:localize ignoreConfig="true" id='<%=DateHelper.getWeekDayLocalizeId(summary.getWorkDate(),true)%>'>
		        <wb:formatDate date='<%=DatetimeType.FORMAT.format(summary.getWorkDate())%>' inFormat="yyyyMMdd" outFormat="EEE"/>
		      </wb:localize>
            </wb:set>
            <wb:secureContent securityName="WTS_SEC_SUMMARY_DATE_LINKS" />
<%
	Permission p = lh.getPermission("WTS_SEC_SUMMARY_DATE_LINKS");
	if ((p.allowAct() || lh.isConfigOn()) && summary.getWrksId() != -1) {
		out.print("<a href=# onclick=\"openPopup('/dailytimesheet/dailyWorkDetails.jsp?FROM=WTS&REC_NO=0&WRKS_ID=");
		out.print(summary.getWrksId());
		out.print("',700,400,'popUpWorkDetails'); return false;\" >");
	}
	if (p.allowView() || lh.isConfigOn()) {
%>
		    <div class=date>
		      <span class=day><%=weekDay%></span>
		      <span class=date><wb:controlField id="WWDATE2_ID" overrideId='<%=MFRM_ID_STR%>' property="workDate" mode='view'></wb:controlField></span>
		    </div>
<%
	}
	if ((p.allowAct() || lh.isConfigOn()) &&  summary.getWrksId() != -1) {
		out.print("</a>");
	}
%>
	      </th>
	    </wb:iterate>
	    <th>
	      <img src="<%=request.getContextPath()%>/images/pixel.gif" width=60 height=1 alt=""><br>
	      <wb:localize id="WWLINETOTAL_ID" type="field" overrideId='<%=MFRM_ID_STR%>'>Total</wb:localize>
	    </th>
<%
	for( int i = 2; i < summaryPage.getFieldNames().size(); i++ ) {
%>
		<th>
		  <img src="<%=request.getContextPath()%>/images/pixel.gif" width=60 height=1 alt=""><br>
		  <wb:localize id="<%=summaryPage.getFieldNames().get(i).toString()%>" type="field" overrideId='<%=MFRM_ID_STR%>'>Extensions</wb:localize>
		</th>
<%
	}
%>
      </tr>
<%
    if( JSPHelper.getWebSession(request).isConfigOn() ) {
%>
      <tr>
        <td colspan='2'/>
<%
        for( int j = 0; j < CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET; j++ ) {
%>
		<td><wb:localize id="WWSUMHOUR_ID" type="field" overrideId='<%=MFRM_ID_STR%>'/></td>
<%
        }
%>
	  </tr>
<%
    }
%>
      <wb:iterate id="elapsedTimeLine" indexId="row" type="com.wbiag.app.jsp.action.biweeklytimesheet.CSMElapsedTimeLine" property="elapsedTimeLines">
        <tr class="<%=row.intValue() % 2 == 0 ? "evenRow" : "oddRow"%>">
        <!-- TIME CODE -->
          <td><wb:controlField id="WRKD_TCODE_NAME_SUM" overrideId='<%=MFRM_ID_STR%>' mode="view"><%=elapsedTimeLine.getFields().get(0)%></wb:controlField></td>
        <!-- HOUR TYPE -->
          <!--td><wb:controlField id="<%=summaryPage.getFieldNames().get(1).toString()%>" overrideId='<%=MFRM_ID_STR%>' mode="view"><%=elapsedTimeLine.getFields().get(1)%></wb:controlField></td-->
        <!-- TOTALS FOR THE WEEK -->
<%
    for( int j = 0; j < CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET; j++ ) {
        Integer hour = (Integer)elapsedTimeLine.getHours().get(j);
        String hourStr = hour.intValue()==0.0?"":hour.toString();

        DayScheduleModel dsm = timesheet.getSchedule().getScheduleDay(j);
        WorkSummaryModel wsm = ((WorkSummaryModel)timesheet.getSummaries().get(j));
        Date wrkdate = wsm.getWorkDate();
        boolean isOff = (((Date)DateHelper.truncateToDays( dsm.getDefStartTime() )).compareTo( dsm.getDefStartTime() ) == 0) && ((dsm.getEndTime()).equals(dsm.getDefStartTime()));
        boolean isHoliday = wsm.getIsHoliday();
	    boolean isError = timesheet.getScheduleOverrideErrors(j) != null || ((WorkSummaryModel)timesheet.getSummaries().get(j)).getWrksError() != null;
%>
		  <td align='right' class='<%=isError ? "error ": ""%><%=isHoliday ? "holiday " : ""%><%=isOff ? "off " : "on "%>'>
<%
    	if (timesheet.getVisibleTeamDateSet().includes(empId, wrkdate)) {
%>
		    <wb:controlField id='WWSUMHOUR_ID' overrideId='<%=MFRM_ID_STR%>' mode='view'><%=hourStr%></wb:controlField>
<%
        }
%>
          </td>
        <!-- EXTENSIONS --> <%
	}
    for( int i = 2; i < summaryPage.getFieldNames().size(); i++ ) {
%>
		  <td><wb:controlField id="<%=summaryPage.getFieldNames().get(i).toString()%>" overrideId='<%=MFRM_ID_STR%>' mode="view"><%=elapsedTimeLine.getFields().get(i)%></wb:controlField></td>
<%
    }
%>
		<!-- LINE TOTAL -->
		  <td align='right'><wb:controlField id='WWSUMHOUR_ID' overrideId='<%=MFRM_ID_STR%>' mode='view'><%=elapsedTimeLine.getLineTotal()%></wb:controlField></td>
		</tr>
      </wb:iterate>
      <tr class=totals>
        <td colspan=1 class=title><wb:localize id="WWSUMHOUR_ID" type="field" overrideId='<%=MFRM_ID_STR%>'/></td>
        <wb:iterate id="summaryTotal" indexId="day" property="summaryTotals">
<%
    int dayIdx = day.intValue();
    DayScheduleModel dsm = timesheet.getSchedule().getScheduleDay(dayIdx);
    boolean isOff = (((Date)DateHelper.truncateToDays( dsm.getDefStartTime() )).compareTo( dsm.getDefStartTime() ) == 0) && ((dsm.getEndTime()).equals(dsm.getDefStartTime()));
    boolean isHoliday = ((WorkSummaryModel)timesheet.getSummaries().get(dayIdx)).getIsHoliday();
    boolean isError = timesheet.getScheduleOverrideErrors(dayIdx)==null;
    WorkSummaryModel wsm = ((WorkSummaryModel)timesheet.getSummaries().get(dayIdx));
    Date wrkdate = wsm.getWorkDate();
%>
		  <td class='<%=isError ? "" : "error "%><%= isHoliday ? "holiday " : ""%><%=isOff ? "off " : "on "%>'>
<%
	if (timesheet.getVisibleTeamDateSet().includes(empId, wrkdate)) {
%>
			<wb:controlField id='WWSUMHOUR_ID' overrideId='<%=MFRM_ID_STR%>' mode='view'><%=summaryTotal%></wb:controlField>
<%
    }
%>
	      </td>
	    </wb:iterate>
	    <td class=grandTotal><wb:controlField id='WWSUMHOUR_ID' overrideId='<%=MFRM_ID_STR%>' mode='view'><%=summaryPage.getSummaryTotal()%></wb:controlField></td>
	  </tr>
	</table>
  </div>
</wb:useBean>
<%-- summary page --%>
<%
}
%>
</wb:secureContent> <%
//	Do not delete this lines --- this releases memory occupied in the session, if we experience memory shortage, because of lots of
//	users accessing the web server concurrently, we can use this lines by sacrificing performance.
//	Uncommenting these lines will result in more DB accesses in the action classes by removing some data cached in the session.
//	timesheet.setSchedule( null );
//	timesheet.setElapsedTime( null );
//	timesheet.setOverride( null );
//	timesheet.setSummary( null );
//	timesheet.setMessage( null );
%>
</wb:useBean> <%-- timesheet --%>
<!--table border=0 class='relatedLinksTable'> <tr><td style="padding-left:20"><wb:relatedLink mfrm="<%=MFRM_ID_STR%>"/></td></tr> </table-->
</wb:page>