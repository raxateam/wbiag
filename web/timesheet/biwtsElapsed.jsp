<%@ include file="/system/wbheader.jsp"%>
<%@ page import="java.util.*"%>
<%@ page import="java.util.Date"%>
<%@ page import="java.sql.*"%>
<%@ page import="java.net.*"%>
<%@ page import="java.text.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.sql.*"%>
<%@ page import="com.workbrain.server.WorkbrainParametersRetriever"%>
<%@ page import="com.workbrain.app.jsp.*"%>
<%@ page import="com.workbrain.server.jsp.*"%>
<%@ page import="com.workbrain.server.registry.*"%>
<%@ page import="com.workbrain.app.jsp.action.timesheet.*"%>
<%@ page import="com.workbrain.app.jsp.workbrain.overrides.*"%>
<%@ page import="com.workbrain.server.data.type.*"%>
<%@ page import="com.workbrain.server.jsp.taglib.sys.*"%>
<%@ page import="com.workbrain.server.jsp.taglib.util.*"%>
<%@ page import="com.workbrain.server.jsp.security.*"%>
<%@ page import="com.workbrain.util.*"%>
<%@ page import="com.workbrain.server.jsp.taglib.util.LocalizableTag"%>
<%@ page import="com.workbrain.server.jsp.locale.LocalizationDictionary" %>
<%@ page import="com.workbrain.app.ta.util.AuthorizationRightsHelper" %>
<%@ page import="org.apache.log4j.*"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetConstants"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMWTShelper"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetPage"%>
<!--We do not use CSMWTSPresentationHelper specific methods in this jsp, so just use original presentation helper-->
<!--%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMWTSPresentationHelper"%-->
<%@ page import="com.workbrain.app.jsp.action.timesheet.WTSPresentationHelper"%>
<%@ page import="com.wbiag.app.jsp.action.biweeklytimesheet.CSMBiWeeklyTimeSheetPage"%>


<%!
private static Logger logger = Logger.getLogger("jsp.weeklyTimesheet.wtsElapsed");
private static int MFRM_ID = -99;
private static String MFRM_ID_STR = "-99";
%>
<%
MFRM_ID = CSMWTShelper.getMfrmId(request);
MFRM_ID_STR = MFRM_ID + "";

%> <wb:page maintenanceFormId='<%=MFRM_ID%>'> <%
CSMBiWeeklyTimeSheetPage timesheetBackup = null;
CSMBiWeeklyTimeSheetPage currWeekTSPage = null; // usually same as timesheet except when copied,
										   // which it would point to the current week
										   // while the timesheet actually points to prevWeek
LocalizationHelper lh = new LocalizationHelper(pageContext);
%>
<wb:define id="contextPath"><%= request.getContextPath() %></wb:define>
<wb:define id="hourFieldName,weekDay,authorizedFieldName,lockedFieldName"/>

<wb:useBean id="timesheet" attribute="timesheet" type="CSMBiWeeklyTimeSheetPage">
<%
currWeekTSPage = timesheet;
%>
<wb:define id="EMP_ID"><%=timesheet.getEmployee().getEmpId()%></wb:define>
<!-- for labour metric security, don't remove -->
<wb:useBean id="helper" type="WTSPresentationHelper" property='helper'>
<wb:useBean id="elapsedTime" type="com.workbrain.app.jsp.action.timesheet.ElapsedTimePage" property="elapsedTime">
<wb:useBean id="copiedtimesheet" attribute="COPIED_PREVIOUS_WEEK_TIMESHEET_JSP" type="CSMBiWeeklyTimeSheetPage">
<%
timesheetBackup = timesheet;
timesheet = (CSMBiWeeklyTimeSheetPage)copiedtimesheet;
elapsedTime = timesheet.getElapsedTime();
%>
</wb:useBean>
<%
boolean cellEntryDisabled[] = new boolean[CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET];
boolean ownsTimesheet = Integer.parseInt(JSPHelper.getWebLogin(request).getEmployeeId()) == timesheet.getEmployee().getEmpId();
boolean isOff[]     = new boolean[CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET];
boolean isHoliday[] = new boolean[CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET];
boolean isError[]   = new boolean[CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET];
boolean isAbsent[]  = new boolean[CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET];
boolean isLocked[]  = new boolean[CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET];
boolean isVisible[] = new boolean[CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET]; // based on team assignment.
boolean isReadOnly[] = new boolean[CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET];
boolean isAnyReadOnly = false;
Date earliest[] = new Date[CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET];

int nonWorkTimePageLines = 0;
int elapsedTimePageLines = timesheet.getElapsedTime().getElapsedTimeLines().size();
if (timesheet.getNonWorkTimePage() != null) {
	nonWorkTimePageLines = timesheet.getNonWorkTimePage().getLines().size();
}

EmployeeModel em = timesheet.getEmployee();
List summaries = timesheet.getSummaries();
SchedulePage schedule = timesheet.getSchedule();
Date hireDate = em.getHireDate();
Date currentDate = null;
Date handsOffDate = null;
Date supervisorDate = null;
for (int day = 0; day < CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET; day++) {
    WorkSummaryModel wsm = (WorkSummaryModel)summaries.get(day);
    DayScheduleModel dsm = (DayScheduleModel)schedule.getScheduleDay(day);
    isAbsent[day]   = wsm.getAbsent();
    isOff[day]      = (((Date)DateHelper.truncateToDays(dsm.getDefStartTime())).compareTo(dsm.getDefStartTime() ) == 0) && ((dsm.getEndTime()).equals(dsm.getDefStartTime()));
    isHoliday[day]  = wsm.getIsHoliday();
    isError[day]    = timesheet.getScheduleOverrideErrors(day) != null || ((WorkSummaryModel)timesheet.getSummaries().get(day)).getWrksError() != null;
    isLocked[day]   = wsm.getLocked();
	isVisible[day] = currWeekTSPage.getVisibleTeamDateSet().includes(em.getEmpId(), ((WorkSummaryModel)currWeekTSPage.getSummaries().get(day)).getWorkDate());
	isReadOnly[day] = wsm.getReadOnly();
	isAnyReadOnly = isAnyReadOnly || isReadOnly[day];
	earliest[day] = hireDate;
	currentDate = DateHelper.addDays(timesheet.getWeekStartDate(), day);
	supervisorDate = DateHelper.addDays(WTShelper.getSupervisorDateForEmp(em,currentDate), 1);
	// issue # 54 sri: handsOffDate = DateHelper.addDays(WTShelper.getHandsOffDateForEmp(em,currentDate), 1);
	handsOffDate = WTShelper.getHandsOffDateForEmp(em,currentDate);

	if (!ownsTimesheet || timesheet.getIsLockDownPriv()) { // supervisor
		if (earliest[day].compareTo(handsOffDate) < 0) earliest[day] = handsOffDate;
	} else { // employee
    	if (earliest[day].compareTo(handsOffDate) < 0) earliest[day] = handsOffDate;
		if (earliest[day].compareTo(supervisorDate) < 0) earliest[day] = supervisorDate;
	}
	if (logger.isDebugEnabled()) {
		logger.debug( ((isVisible[day])?"Visible":"NOT visible") + " date : " + wsm.getWorkDate());
	}
}

for (int day = 0; day < CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET; day++) {
    cellEntryDisabled[day] = isLocked[day] || isAbsent[day];
    // disable today if it is before the earliest editable day.
    if ( (earliest[day].compareTo(DateHelper.addDays(timesheet.getWeekStartDate(), day)) > 0) || !isVisible[day] || isReadOnly[day]) {
    	cellEntryDisabled[day] = true;
    }
}
%>
<wb:secureContent securityName='WTS_SEC_ELAPSED_TIME'>
  <div class=sectionTitle>
    <h3>
      <wb:config id="WTS_Enter_Elapsed_Time"/>
      <wb:localize id="WTS_Enter_Elapsed_Time" ignoreConfig="true">Enter Elapsed Time</wb:localize>
    </h3>
  </div>
  <div id=sectionElapsed>
	<table class=wtsTable>
	  <wb:useBean property='containsWrksErrors' methodPrefix=''>
	    <tr>
	      <td colspan="18" class=warning><wb:localize id="Highlighted_day_columns_have_one_or_more_error_entries">Highlighted days have one or more errors</wb:localize></td>
	    </tr>
	    <wb:display whenConfig='on'>
	      <tr>
	        <td colspan="18">
	          <wb:localize id="DATE1_ID" type="field" overrideId='<%=MFRM_ID_STR%>'></wb:localize>
	          <wb:localize id="DATE2_ID" type="field" overrideId='<%=MFRM_ID_STR%>'></wb:localize>
	        </td>
	      </tr>
	    </wb:display>
	    <wb:iterate indexId="i" property="summaries">
	      <wb:useBean id="error" type="java.lang.String" property='wrksError'>
	        <tr>
	          <td colspan="18" class=error>
	            <wb:controlField id="DATE1_ID" overrideId='<%=MFRM_ID_STR%>' mode='view'><%=timesheet.getWeekDate(i.intValue())%></wb:controlField>
	            <wb:controlField id="DATE2_ID" overrideId='<%=MFRM_ID_STR%>' mode='view'><%=timesheet.getWeekDate(i.intValue())%></wb:controlField>
	            <%=LocalizationDictionary.localizeErrorMessage(JSPHelper.getConnection(request), error,
                        JSPHelper.getWebLocale(request).getLanguageId())%>
              </td>
            </tr>
          </wb:useBean>
        </wb:iterate>
      </wb:useBean>
      <tr>
        <th>
          <img src="<%=request.getContextPath()%>/images/pixel.gif" width=45 height=1 alt=""><br>
          <img src="<%=request.getContextPath()%>/images/interface/trashcan.gif" width=9 height=11 alt='<wb:localize id="DELETE_TOOLTIP" overrideId="<%=MFRM_ID_STR%>" ignoreConfig="true" encodeHTML="true">Del</wb:localize>' title='<wb:localize id="DELETE_TOOLTIP" overrideId="<%=MFRM_ID_STR%>" ignoreConfig="true" encodeHTML="true">Del</wb:localize>'>
        </th>
        <th>
          <img src="<%=request.getContextPath()%>/images/pixel.gif" width=70 height=1 alt=""><br>
          <wb:secureContent securityName="<%=elapsedTime.getFieldNames().get(0).toString()%>" />
<%
if (!lh.isDenied(elapsedTime.getFieldNames().get(0).toString()) || lh.isConfigOn()){
%>
		  <wb:localize id="<%=elapsedTime.getFieldNames().get(0).toString()%>" type="field" overrideId='<%=MFRM_ID_STR%>'><%=elapsedTime.getFieldNames().get(0).toString()%></wb:localize>
		</th>
<%
}
	String fieldName = elapsedTime.getFieldNames().get(1).toString();
  	if (!lh.isHidden(fieldName, MFRM_ID)) {
		out.print("<th>");
		out.print(lh.getCaption(fieldName, LocalizableTag.CONFIG_FIELD, MFRM_ID, fieldName));
		out.print("</th>");
	} else {
		out.print("<th style='display:none'></th>");
	}
%>
        <wb:iterate id="summary" indexId="day" type="com.workbrain.app.jsp.action.timesheet.WorkSummaryModel" property="summaries">
<%
int i = day.intValue();
%>
          <th class='<%=isError[i] ? "error " : ""%><%=isHoliday[i] ? "holiday " : ""%><%=isOff[i] ? "off " : "on "%>'>
            <img src="<%=request.getContextPath()%>/images/pixel.gif" width=50 height=1 alt=""><br>
            <wb:display whenConfig='on'>
              <wb:localize id="WWDATE1_ID" type="field" overrideId='<%=MFRM_ID_STR%>'></wb:localize>
              <wb:localize id="WWDATE2_ID" type="field" overrideId='<%=MFRM_ID_STR%>'></wb:localize> <br>
              <wb:config id='<%=DateHelper.getWeekDayLocalizeId(summary.getWorkDate(),true)%>'/>
            </wb:display>
            <wb:set id = "weekDay"><wb:localize ignoreConfig="true" id='<%=DateHelper.getWeekDayLocalizeId(summary.getWorkDate(),true)%>'><wb:formatDate date='<%=DatetimeType.FORMAT.format(summary.getWorkDate())%>' inFormat="yyyyMMdd" outFormat="EEE"/></wb:localize></wb:set>
            <wb:secureContent securityName="WTS_SEC_ELAPSED_DATE_LINKS" />
<%
Permission p = lh.getPermission("WTS_SEC_ELAPSED_DATE_LINKS");
if (p.allowAct() && isVisible[i]  && summary.getWrksId() != -1 ) {
	StringBuffer sb = new StringBuffer();
	sb.append("<a href=# onclick=\"openPopup('/dailytimesheet/dailyWorkDetails.jsp?FROM=WTS&REC_NO=0&WRKS_ID=");
	sb.append(summary.getWrksId());
	sb.append("',700,400,'popUpWorkDetails'); return false; \">");
	out.print(sb.toString());
}
if (p.allowView()) {
%>
			<div class=date>
			  <span class=day><%=weekDay%></span>
			  <span class=date><wb:controlField id="WWDATE2_ID" overrideId='<%=MFRM_ID_STR%>' property="workDate" mode='view'></wb:controlField></span>
			</div>
<%
}
if (p.allowAct() && isVisible[i] && summary.getWrksId() != -1) {
	out.print("</a>");
}
%>
          </th>
        </wb:iterate>
<%
if( helper.useDistributedTime() ) {
%>
		<th>
		  <img src="<%=request.getContextPath()%>/images/pixel.gif" width=60 height=1 alt=""><br>
		  <wb:define id="totalDist"/>
	      <wb:set id="totalDist"><wb:localize id="TOTAL_DIST" ignoreConfig="true" escapeForJavascript="true">Total/Dist</wb:localize></wb:set>
		  <wb:config id="TOTAL_DIST"/>
		  <wba:button label="#totalDist#" onClick="distributeTime()" disabled='<%=timesheet.getReadOnly()?"true":"false"%>'/>
		</th>
<%
} else {
%>
		<th>
		  <img src="<%=request.getContextPath()%>/images/pixel.gif" width=60 height=1 alt=""><br>
		  <wb:localize id="WWLINETOTAL_ID" type="field" overrideId='<%=MFRM_ID_STR%>'>Total</wb:localize>
		</th>
<%
}
// Display labor metric field headers.
for(int fieldIdx = 2; fieldIdx < elapsedTime.getFieldNames().size(); fieldIdx++) {
	fieldName = elapsedTime.getFieldNames().get(fieldIdx).toString();
  	if (!lh.isHidden(fieldName, MFRM_ID)) {
		out.print("<th>");
		out.print(lh.getCaption(fieldName, LocalizableTag.CONFIG_FIELD, MFRM_ID, fieldName));
		out.print("</th>");
	} else {
		out.print("<th style='display:none'></th>");
	}
}
if (elapsedTime.getFieldNames().size() == 1){
	out.print("<th>");
	out.print(" ");
	out.print("</th>");
}
%>
        </tr>
        <wb:display whenConfig='on'>
          <tr>
            <td colspan='1'/>
            <td><wb:localize id="WWHOUR_ID_T" type="field" overrideId='<%=MFRM_ID_STR%>'>Total Hours</wb:localize></td>
            <td></td>
            <wb:iterate property="summaries" indexId="h">
<%
int hourIdx = h.intValue();
%>
              <td>
                <wb:localize id="WWHOUR_ID" type="field" overrideId='<%=MFRM_ID_STR%>'/>
                <wb:secureContent securityName='<%="WTS_HOUR_" + hourIdx%>' />
              </td>
            </wb:iterate>
            <td>
              <wb:localize id="WWDIST_HOUR_ID" type="field" overrideId='<%=MFRM_ID_STR%>'/>
            </td>
          </tr>
        </wb:display>

<%
// timeLineIdx = index = i
List elapsedTimeLines = elapsedTime.getElapsedTimeLines();
for (int timeLineIdx = 0; timeLineIdx < elapsedTimeLines.size(); timeLineIdx++) {
	ElapsedTimeLine elapsedTimeLine = (ElapsedTimeLine)elapsedTimeLines.get(timeLineIdx);
	Integer timeLineIdxInt = new Integer(timeLineIdx);
%>
		<tr>

<%
	boolean readOnly = false;
	for( int j = 0; j < CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET; j++ ) {
//    Integer hour = (Integer)elapsedTimeLine.getHours().get(j);
//    String hourStr = hour.intValue()==0.0?"":hour.toString();
		WorkSummaryModel summary = (WorkSummaryModel)timesheet.getSummaries().get(j);
		if( summary.getReadOnly() ) {
		readOnly = true;
	}
}
%>
          <td align=center>
            <wb:secureContent securityName='WTSDELET_ID'>
<%
	if (lh.isAct("WTSDELET_ID")) {
%>
		      <INPUT type='checkbox' Class='inputField checkbox' Name='<%="WTSDELET_" + timeLineIdx%>' <%=readOnly && !timesheet.getSupervisor()?"disabled":(lh.isAct("WTS_SEC_ELAPSED_TIME")?"":"disabled")%>>
<%
	}
%>
            </wb:secureContent>
          </td>
          <td class=preceedsTimeEntry>
<%
	Permission codePerm = lh.getPermission(elapsedTime.getFieldNames().get(0).toString());
	if (codePerm.allowAct()) {
%>
			<wb:controlField submitName='<%="FIELDS0X_" + timeLineIdx%>' id='<%=elapsedTime.getFieldNames().get(0).toString()%>' mode='<%=lh.isAct("WTS_SEC_ELAPSED_TIME")?"edit":"view"%>' cssClass="inputField" overrideId='<%=MFRM_ID_STR%>'><%=elapsedTimeLine.getFields().get(0)%></wb:controlField>
<%
	} else if (codePerm.allowView()) {
		out.print(elapsedTimeLine.getFields().get(0));
	}
%>
		  </td>
<%
	int fieldIdx = 1;
	if (!lh.isHidden(elapsedTime.getFieldNames().get(fieldIdx).toString(), MFRM_ID)) {
		String fieldMode = "edit";
		if (!lh.isAct(elapsedTime.getFieldNames().get(fieldIdx).toString()) && !lh.isDenied(elapsedTime.getFieldNames().get(fieldIdx).toString())){
			fieldMode = "view"; %> <input type="hidden" name='<%="FIELDS" + fieldIdx + "X_" + timeLineIdx%>' value='<%=elapsedTimeLine.getFields().get(fieldIdx)%>' /> <%
		}
		out.print("<td>");
       	%><wb:controlField submitName='<%="FIELDS" + fieldIdx + "X_" + timeLineIdx%>' id='<%=elapsedTime.getFieldNames().get(fieldIdx).toString()%>' cssClass="inputField" overrideId='<%=MFRM_ID_STR%>' mode='<%=lh.isAct("WTS_SEC_ELAPSED_TIME")?fieldMode:"view"%>' onChange="documentChanged()"><%=elapsedTimeLine.getFields().get(fieldIdx)%></wb:controlField><%
		out.print("</td>");
	} else {
		out.print("<td style='display:none'><input type='hidden' name='FIELDS");
		out.print(fieldIdx);
		out.print("X_");
		out.print(timeLineIdx);
		out.print("' value='");
		out.print(elapsedTimeLine.getFields().get(fieldIdx));
		out.print("' /></td>");
	}
	String errorMsgs = "";
	Calendar currCal = Calendar.getInstance();
	currCal.setTime( timesheet.getWeekStartDate() );
	currCal.set( Calendar.AM_PM, Calendar.AM );
	currCal.set( Calendar.HOUR, 0 );
	currCal.set( Calendar.MINUTE, 0 );
	currCal.set( Calendar.SECOND, 0 );
	currCal.set( Calendar.MILLISECOND, 0 );
	// sumIdx = k = j
	for (int sumIdx = 0; sumIdx < timesheet.getSummaries().size(); sumIdx++) {
		WorkSummaryModel summary = (WorkSummaryModel)timesheet.getSummaries().get(sumIdx);
		Integer sumIdxInt = new Integer(sumIdx);
		Integer hours = (Integer)elapsedTimeLine.getHours().get(sumIdx);
%> <%
  String mode = summary.getWrksId()==-1 || summary.getReadOnly()?"view":(lh.isAct("WTS_SEC_ELAPSED_TIME")?"edit":"view");

  if ( cellEntryDisabled[sumIdx] ) {
    mode = "view"; // manually locked, set mode to view
  } else {
    mode = (lh.isAct("WTS_SEC_ELAPSED_TIME")?"edit":"view");
  }
  String cssClass = "class=timeCell";
  if( summary.getIsHoliday() ) {
      cssClass = "class=holiday";
  }
  String wrksErrorMsg = summary.getWrksError()==null?"":summary.getWrksError();
  wrksErrorMsg        = LocalizationDictionary.localizeErrorMessage(
                        JSPHelper.getConnection(request), wrksErrorMsg,
                        JSPHelper.getWebLocale(request).getLanguageId());
  if(!"".equals(wrksErrorMsg)){
    if(timeLineIdx == 0){
      //if this is the first iteration append this error message
      errorMsgs += "<p align='left' style='margin: 0 0 0 0; text-align: left; vertical-align: top; color: Red;'>"+wrksErrorMsg + "</p>";
    }
    cssClass = "class=error";
  }
  if (isOff[sumIdx]) cssClass = "class=off";
%> <wb:set id="hourFieldName"><%="HOUR_" + timeLineIdx + "_" + sumIdx%></wb:set> <td class='<%=isError[sumIdx] ? "error " : ""%><%=isHoliday[sumIdx] ? "holiday " : ""%><%=isOff[sumIdx] ? "off " : "on "%>'> <% Permission hourPerm = lh.getPermission("WTS_HOUR_"+ sumIdx);
        if (hourPerm.allowView() && isVisible[sumIdx]) {
			String hourMode = "edit";
			String strCssClass = "inputField number";
			if (!hourPerm.allowAct() || "view".equals(mode) || !lh.isAct("HOUR_" + timeLineIdx + "_" + sumIdx)){
				hourMode = "view";
				strCssClass = "inputField number label";
				%> <input type=hidden name=<%=hourFieldName%> value=<%=hours%>> <%
			}
		%> <wb:controlField submitName='#hourFieldName#' id='WWHOUR_ID' overrideId='<%=MFRM_ID_STR%>' cssClass='<%=strCssClass%>' mode='<%=hourMode%>' onChange="documentChanged()"><%=hours%></wb:controlField> <% } %> </td> <%
  if ( mode.equals("view") ) {   // generate a hidden input field if the mode is "view" so the value is not lost.
%> <input type=hidden name=<%=hourFieldName%> value=<%=hours%>> <%
  }
  currCal.add( Calendar.DATE, 1 );
%> <%
	} //for (int sumIdx = 0; sumIdx
%> <%
          if( helper.useDistributedTime() ) {
        %> <td align='right' class=followsTimeEntry> <wb:controlField submitName='<%="TOTAL_HOUR_" + timeLineIdx%>' id='WWDIST_HOUR_ID' overrideId='<%=MFRM_ID_STR%>' cssClass="inputField" mode='<%=lh.isAct("WTS_SEC_ELAPSED_TIME")?"edit":"view"%>' onChange="documentChanged()"><%=elapsedTimeLine.getLineTotal()==0.0?"":String.valueOf(elapsedTimeLine.getLineTotal())%></wb:controlField> </td> <%
          } else {
        %> <td align='right' class=followsTimeEntry><wb:controlField id='WWLINETOTAL_ID' overrideId='<%=MFRM_ID_STR%>' mode='view'><%=elapsedTimeLine.getLineTotal()==0.0?"":String.valueOf(elapsedTimeLine.getLineTotal())%></wb:controlField></td> <%
          }
        %> <input type="hidden" name='ELAPSED_NUM_FIELDS' value=<%=elapsedTime.getFieldNames().size()%> /> <%
      	// Display labor metric field data.
		for(fieldIdx = 2; fieldIdx < elapsedTime.getFieldNames().size(); fieldIdx++) {
			if (!lh.isHidden(elapsedTime.getFieldNames().get(fieldIdx).toString(), MFRM_ID)) {
				String fieldMode = "edit";
				if (!lh.isAct(elapsedTime.getFieldNames().get(fieldIdx).toString()) && !lh.isDenied(elapsedTime.getFieldNames().get(fieldIdx).toString())){
					fieldMode = "view"; %> <input type="hidden" name='<%="FIELDS" + fieldIdx + "X_" + timeLineIdx%>' value='<%=elapsedTimeLine.getFields().get(fieldIdx)%>' /> <%
				}
				out.print("<td>");
	        	%><wb:controlField submitName='<%="FIELDS" + fieldIdx + "X_" + timeLineIdx%>' id='<%=elapsedTime.getFieldNames().get(fieldIdx).toString()%>' cssClass="inputField" overrideId='<%=MFRM_ID_STR%>' mode='<%=lh.isAct("WTS_SEC_ELAPSED_TIME")?fieldMode:"view"%>' onChange="documentChanged()"><%=elapsedTimeLine.getFields().get(fieldIdx)%></wb:controlField><%
				out.print("</td>");
			} else {
				out.print("<td style='display:none'><input type='hidden' name='FIELDS");
				out.print(fieldIdx);
				out.print("X_");
				out.print(timeLineIdx);
				out.print("' value='");
				out.print(elapsedTimeLine.getFields().get(fieldIdx));
				out.print("' /></td>");
			}
		}
		if (elapsedTime.getFieldNames().size() == 1){
			out.print("<td>");
			out.print("</td>");
		}%> </tr> <%
	}
%> <tr class=totals>
<td colspan=3 class=title><wb:localize id="WWLINETOTAL_ID" type="field" overrideId='<%=MFRM_ID_STR%>'>Total</wb:localize><wb:config id="WTS_HOUR_SUFFIX"/></td>
<% for (int day = 0; day < elapsedTime.getDayTotals().size(); day++) { %> <td class='<%=isError[day] ? "error " : ""%><%=isHoliday[day] ? "holiday " : ""%><%=isOff[day] ? "off " : "on "%>'> <% if (isVisible[day]) {  %> <wb:controlField id='WWLINETOTAL_ID' overrideId='<%=MFRM_ID_STR%>' mode='view'><%=elapsedTime.getDayTotals().get(day)%></wb:controlField> <% } %> </td> <% } %> <td class=grandTotal><wb:controlField id='WWLINETOTAL_ID' overrideId='<%=MFRM_ID_STR%>' mode='view'><%=elapsedTime.getTotal()%></wb:controlField></td> <td colspan='10'></td> </tr> </table> <wb:useBean id="copiedtimesheet" attribute="COPIED_PREVIOUS_WEEK_TIMESHEET_JSP" type="CSMBiWeeklyTimeSheetPage"> <% timesheet = timesheetBackup;	%> </wb:useBean> <% request.getSession().removeAttribute("COPIED_PREVIOUS_WEEK_TIMESHEET_JSP"); %> </div> </wb:secureContent>
<%--------------------------------------- NonWorkTimePage ------------------------------- --%> <wb:secureContent securityName='WTS_SEC_NON_WORK_TIME' showKey='false'> <wb:useBean property="nonWorkTimePage"> <%
  int lineCount = Integer.parseInt(request.getParameter("totalLineCount") == null?"0":request.getParameter("totalLineCount"));
  String link = "'" + request.getContextPath() + "/action/timesheet.action?action=ExpandComponentAction&NWT=" + (timesheet.getIsNonWorkTimePageExpanded()?"collapse":"expand") + "'";
  String image = request.getContextPath() + (timesheet.getIsNonWorkTimePageExpanded()?"/images/collapse.gif":"/images/expand.gif");
%> <%
  // generate hidden input fields when collapsed.
  LTAPage nwtp = timesheet.getNonWorkTimePage();
  if (nwtp != null && !timesheet.getIsNonWorkTimePageExpanded()) {
    List lines = nwtp.getLines();
    lineCount = lines.size()-1;
    for (int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
      LTAPage.LTALine line = (LTAPage.LTALine)lines.get(lineIdx);
      %><input type="hidden" name="WTSNWTNAME_<%=lineIdx%>" value="<%=(line.getType()!=null)?line.getType():""%>"/><%
      if (line.getDel()) {
	    %><input type="hidden" name="WTSNWTDEL_<%=lineIdx%>" value="true"/><%
	  }

	  List days = line.getDays();
      for (int day = 0; day < days.size(); day++) {
        LTAPage.Override ltaOvr = (LTAPage.Override)days.get(day);
        %><input type="hidden" name="WTSNWTSTART<%=day%>X_<%=lineIdx%>" value="<%=(ltaOvr.getStart()!=null)?DatetimeType.FORMAT.format(ltaOvr.getStart(), new StringBuffer(), new FieldPosition(0)).toString():""%>"/><%
        %><input type="hidden" name="WTSNWTSTOP<%=day%>X_<%=lineIdx%>" value="<%=(ltaOvr.getStop()!=null)?DatetimeType.FORMAT.format(ltaOvr.getStop(), new StringBuffer(), new FieldPosition(0)).toString():""%>"/><%
        %><input type="hidden" name="WTSNWTOVR<%=day%>X_<%=lineIdx%>" value="<%=ltaOvr.getId()%>"/><%
      }
    }
  }
%>
<div class=sectionTitle>
	<wb:secureContent securityName='WTS_SEC_NON_WORK_TIME' />
		<h3><a href="#" onClick="confirmedLeaveWithSubmit(<%=link%>)"><img src='<%=image%>' width='11' height='11' border='0' hspace=5 align=bottom><wb:localize id="Enter_Non_Work_Time">Enter Non-Work Time</wb:localize></a>
		</h3>
</div>
<div class=sectionData<%=timesheet.getIsNonWorkTimePageExpanded()?"":"Collapsed"%>>
	<table class=wtsTable>
		<wb:useBean property="isNonWorkTimePageExpanded">
			<tr>
				<th><img src="<%=request.getContextPath()%>/images/pixel.gif" width=45 height=1 alt=""><br><wb:localize ignoreConfig="true" id="Del" encodeHTML="true">Del</wb:localize>
				</th>
				<th><img src="<%=request.getContextPath()%>/images/pixel.gif" width=70 height=1 alt=""><br><wb:localize id="WTSNWTTYP_ID" type="field" overrideId='<%=MFRM_ID_STR%>'>Type</wb:localize>
				</th>
				<wb:useBean id="summaryPage" type="com.workbrain.app.jsp.action.timesheet.SummaryPage" property="summary">
					<wb:iterate id="summary" indexId="day" type="com.workbrain.app.jsp.action.timesheet.WorkSummaryModel" property="summaries"> <% int dayIdx = day.intValue(); %> <th class='<%=isError[dayIdx] ? "error " : ""%><%=isHoliday[dayIdx] ? "holiday " : ""%><%=isOff[dayIdx] ? "off " : "on "%>'> <img src="<%=request.getContextPath()%>/images/pixel.gif" width=50 height=1 alt=""><br> <wb:localize ignoreConfig="true" id='<%=DateHelper.getWeekDayLocalizeId(summary.getWorkDate(),true)%>'><wb:formatDate date='<%=DatetimeType.FORMAT.format(summary.getWorkDate())%>' inFormat="yyyyMMdd" outFormat="EEE"/></wb:localize> </th> </wb:iterate> </wb:useBean> <th style="text-align:center"> <img src="<%=request.getContextPath()%>/images/pixel.gif" width=60 height=1 alt=""><br> <wb:localize id="WTSNWTTOTAL_ID" type="field" overrideId='<%=MFRM_ID_STR%>'>Total</wb:localize> </th> </tr> <wb:iterate indexId="line" id="ltaLine" property="lines"> <% lineCount = line.intValue(); %> <wb:display whenConfig='on'> <tr> <td colspan='2'></td> <wb:iterate indexId="day" property="days"> <td><wb:localize id="WWNWTTIME_ID" type="field" overrideId='<%=MFRM_ID_STR%>'/></td> </wb:iterate> <td><wb:localize id="WWHOUR_ID_T" type="field" overrideId='<%=MFRM_ID_STR%>'>Total Hours</wb:localize></td> </tr> </wb:display> <tr> <td align=center> <wb:controlField cssClass='inputField checkbox' submitName='<%="WTSNWTDEL_"+line%>' ui='CheckboxUI' uiParameter='hideTable=true' mode='<%=(lh.isAct("WTS_SEC_NON_WORK_TIME")?"edit":"view")%>'><%=((LTAPage.LTALine)ltaLine).getDel()%></wb:controlField> </td> <td> <wb:controlField cssClass='inputField' id='WTSNWTTYP_ID' overrideId='<%=MFRM_ID_STR%>' submitName='<%="WTSNWTNAME_"+line%>' property='type' mode='<%=(lh.isAct("WTS_SEC_NON_WORK_TIME")?"edit":"view")%>'></wb:controlField> </td> <wb:iterate indexId="day" property="days"> <% int dayIdx = day.intValue(); %> <td class='<%=isError[dayIdx] ? "error " : ""%><%=isHoliday[dayIdx] ? "holiday " : ""%><%=isOff[dayIdx] ? "off " : "on "%>'> <wb:submit id='<%="WTSNWTOVR"+dayIdx+"X_"+line%>' property='id'/> <wb:controlField cssClass='inputField start' id="WWNWTTIME_ID" overrideId='<%=MFRM_ID_STR%>' submitName='<%="WTSNWTSTART"+dayIdx+"X_"+line%>' onChange="documentChanged()" mode='<%=cellEntryDisabled[dayIdx]?"view":(lh.isAct("WTS_SEC_NON_WORK_TIME")?"edit":"view")%>' property='start'></wb:controlField><%=cellEntryDisabled[dayIdx]?"<br>":""%> <wb:controlField cssClass='inputField stop' id="WWNWTTIME_ID" overrideId='<%=MFRM_ID_STR%>' submitName='<%="WTSNWTSTOP"+dayIdx+"X_"+line%>' onChange="documentChanged()" mode='<%=cellEntryDisabled[dayIdx]?"view":(lh.isAct("WTS_SEC_NON_WORK_TIME")?"edit":"view")%>' property='stop'></wb:controlField> </td> </wb:iterate> <td> <wb:controlField cssClass='inputField' id='WTSNWTTOTAL_ID' overrideId='<%=MFRM_ID_STR%>' submitName='<%="WTSNWTTOTAL_"+line%>' property='total' mode='view'></wb:controlField> </td> </tr> </wb:iterate> <tr class=totals> <td colspan=2 class=title><wb:localize id="WTSNWTLINETOTAL_ID" type="field" overrideId='<%=MFRM_ID_STR%>'>Total</wb:localize></td> <wb:iterate indexId="day" property="hours"> <% int dayIdx = day.intValue(); %> <td class='value <%=isError[dayIdx] ? "error " : ""%><%=isHoliday[dayIdx] ? "holiday " : ""%><%=isOff[dayIdx] ? "off " : "on "%>'> <wb:controlField id='WTSNWTLINETOTAL_ID' overrideId='<%=MFRM_ID_STR%>' mode='view' property=""/> </td> </wb:iterate> <td class=grandTotal><wb:controlField id='WTSNWTLINETOTAL_ID' overrideId='<%=MFRM_ID_STR%>' mode='view' property='total'/></td> </tr> </wb:useBean> </table> </div> <input type="hidden" name="totalLineCount" value=<%=lineCount %> /> </wb:useBean> </wb:secureContent>  <wb:include page="biwtsElapsedButtons.jsp"/> </wb:useBean> </wb:useBean> </wb:useBean> </wb:page>