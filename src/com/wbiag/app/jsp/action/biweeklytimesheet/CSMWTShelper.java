package com.wbiag.app.jsp.action.biweeklytimesheet;

//import com.workbrain.app.ta.model.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;

import com.workbrain.app.jsp.action.dailytimesheet.TimesheetHelper;
import com.workbrain.app.jsp.action.timesheet.DayScheduleModel;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimeView;
import com.workbrain.app.jsp.action.timesheet.OverrideModel;
import com.workbrain.app.jsp.action.timesheet.SchedulePage;
import com.workbrain.app.jsp.action.timesheet.WTShelper;
import com.workbrain.app.ta.model.EmployeeIdStartEndDate;
import com.workbrain.app.ta.model.EmployeeIdStartEndDateSet;
import com.workbrain.server.data.AccessException;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;

public final class CSMWTShelper implements CSMBiWeeklyTimeSheetConstants {

    private static final String WTS_URL_PREFIX = ACTION_URL + "?action=";
    private static final Logger logger = Logger.getLogger("com.wbiag.app.jsp.action.biweeklytimesheet.CSMWTShelper");

	private static final String MFRM_NAME = "BIWEEKLY TIMESHEET";
    private static final int DEFAULT_MFRM_ID = -99;
    private static int _mfrmId = DEFAULT_MFRM_ID;

    public static String getActionURL( String actionName ) {
        return WTS_URL_PREFIX + actionName;
    }

    /**
     * @param dayInPayPeriod - any day that falls in the a pay period
     * @param payGroupStartDate - the pay group's start date
     * @return
     * Example:
     * WeekStart is Sunday
     * PayGroup Start: Aug 20
     * PayGroup End: Sept 1
     * DayInPeriod: Aug 16
     * This method returns Aug 6, which is the start day of the previous pay period(Aug 6 to Aug 19)
     */
    public static Date getAlignedPeriodStartDate(Date dayInPayPeriod, Date payGroupStartDate ){

    	Date periodStartDate = TimesheetHelper.getWeekStartDate(dayInPayPeriod);
    	int diff = DateHelper.getDifferenceInDays(payGroupStartDate,periodStartDate);
    	if(diff%CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET!=0)
    		periodStartDate = DateHelper.addDays(periodStartDate, -1*CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET/2);

    	return periodStartDate;
    }

    // returns 1 if daily, 2 if weekly and 0 if default timesheet not set
    public static int dailyOrWeekly(DBConnection conn, String empId) throws Exception{
    	return WTShelper.dailyOrWeekly(conn, empId);
/*        PreparedStatement s1 = conn.prepareStatement(
                "select * from TS_USER where EMP_ID = ?");
        ResultSet rs1 = null;
        try {
            s1.setInt(1, Integer.parseInt(empId));
            rs1 = s1.executeQuery();
            if (rs1.next()){
                return rs1.getInt("TSU_TIMESHEET_FLAG");
            }else{
                return 0;
            }
        }finally {
            if (rs1 != null) {
                rs1.close();
            }
            if (s1 != null) {
                s1.close();
            }
        }*/
    }

    // returns extra parameters value for WTS
    public static String retrieveWTSExtraParams(DBConnection conn, String empId)
        throws SQLException {
    	return WTShelper.retrieveWTSExtraParams(conn, empId);
/*        ResultSet rs = null;
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement("SELECT TSU_EXTRA_PARAMS FROM TS_USER WHERE EMP_ID = ?");
            pst.setString(1, empId);
            rs = pst.executeQuery();
            if (rs.next()){
                return rs.getString(1);
            } else {
                return null;
            }
        } finally {
            SQLHelper.cleanUp(pst, rs);
        }*/
    }

    public static String getWTSExtraParamValue(String extraParams, String paramName) {
    	return WTShelper.getWTSExtraParamValue(extraParams, paramName);
/*        if (StringHelper.isEmpty(extraParams) ||
            StringHelper.isEmpty(paramName)) {
            return null;
        }
        String [] parameters = StringHelper.detokenizeString(extraParams, "&", true);
        for (int i=0; i<parameters.length; i++) {
            int index = parameters[i].indexOf(paramName + "=");
            if (index > -1) {
                return parameters[i].substring(index + paramName.length() + 1);
            }
        }
        return null;*/
    }

    /**
     *  this method assumes that a valid statement is initialized and passed in
     *  It is the caller's responsibility to close the statement
     */
    public static String overrideViewMode(String userName, int ovrId, Statement s) throws SQLException
    {
    	return WTShelper.overrideViewMode(userName, ovrId, s);
/*        String result = "NONE";
        ResultSet rs = null;
        try {
            rs = s.executeQuery("select WBP_ID from OVERRIDE_TYPE_GRP, WORKBRAIN_USER where " +
                                "OVERRIDE_TYPE_GRP.WBG_ID = WORKBRAIN_USER.WBG_ID " +
                                "and OVRTYP_ID = " + ovrId + " and WBU_NAME = '" + userName +"'");
            if (rs.next()){
                int permission = rs.getInt("WBP_ID");

                if( permission == 1 ){
                    result = "EDIT";
                } else if( permission == 0 ){
                    result = "VIEW";
                }
            }
            return result;
        } finally {
            SQLHelper.cleanUp( rs );
        }*/
    }

    public static String overrideViewMode(String userName, int ovrId, DBConnection conn)
            throws SQLException{
    	return WTShelper.overrideViewMode(userName, ovrId, conn);
/*        PreparedStatement pStmt = null;
        ResultSet rs = null;
        try {
            pStmt = conn.prepareStatement("select WBP_ID from OVERRIDE_TYPE_GRP, " +
                    "WORKBRAIN_USER where OVERRIDE_TYPE_GRP.WBG_ID = WORKBRAIN_USER.WBG_ID " +
                    "and OVRTYP_ID = ? and WBU_NAME = ?");
            pStmt.setInt(1, ovrId);
            pStmt.setString(2, userName);
            rs= pStmt.executeQuery();

            if (rs.next()){
                if (rs.getInt("WBP_ID")== 1){
                    return "EDIT";
                }else if (rs.getInt("WBP_ID")== 0){
                    return "VIEW";
                }else{
                    return "NONE";
                }
            }else{
                 return "NONE";
            }
        } finally {
            SQLHelper.cleanUp(pStmt, rs);
        }*/
    }

    public static String displayOverrides(PageContext pc, OverrideModel od, int intBlankRec, String empDetails, int empId, int ovrCount, int i)
        throws AccessException, Exception {
    	return WTShelper.displayOverrides(pc, od, intBlankRec, empDetails, empId, ovrCount, i);

/*        String contextPath = JSPHelper.getWebContext(pc).getContextPath();
        int localeId = JSPHelper.getWebContext(pc).getWebLocale().getId();
        String localizedOvrName;
        String ovrNewValue;
        StringBuffer ovrDisplay = new StringBuffer();
        LocalizationHelper lh = new LocalizationHelper(pc);
        SimpleDateFormat df = new SimpleDateFormat("");
        Integer ovrtypId = new Integer(od.getTypeId());

        TimesheetBean tsb = (TimesheetBean) pc.getSession().getAttribute(TimesheetConstants.TIMESHEET_BEAN_ATTRIBUTE_NAME);
        HashMap otp = tsb.getOverrideTypePermissions();
        Integer otPerm = (Integer) otp.get(ovrtypId);
        DBConnection conn = JSPHelper.getConnection(pc.getRequest());
        CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
        OverrideTypeData otd = codeMapper.getOverrideTypeById(ovrtypId.intValue());

        //
        	String delText = JSPHelper.encodeHTML(lh.getText("DELETE_TOOLTIP", LocalizableTag.CONFIG_MESSAGE, "Delete"));
        	String copyText = JSPHelper.encodeHTML(lh.getText("COPY_TOOLTIP", LocalizableTag.CONFIG_MESSAGE, "Copy"));

            ovrDisplay.append("<tr class='evenRow'><td></td><td nowrap width ='250'><nobr>");

            if ("EDIT".equalsIgnoreCase(od.getMode())) {
                ovrDisplay.append("&nbsp;<span class=textDark><nobr><img src='" + contextPath + "/COMMON-IMG/Remove.gif' border='0' alt='" + delText + "' title='" + delText + "'></span>");
                ovrDisplay.append("<input type=checkbox name='DELETE_OVR_" + ovrCount + "_" + i +
                                  "' value='true'/>");
            }

            if ("EDIT".equalsIgnoreCase(od.getMode()) || "ERROR".equalsIgnoreCase(od.getStatus())) {
                ovrDisplay.append("&nbsp;<span class=textDark><nobr>" +
                                 "<img src='" + contextPath + "/images/Copy.gif' border='0' alt='" + copyText + "' title='" + copyText + "'></span>" +
                                 "<input type=checkbox name='COPY_OVR_" + ovrCount +
                                 "' value='" + od.getId() + "'>");
            } else {
                ovrDisplay.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
            }

            if(OverrideData.ERROR.equalsIgnoreCase(od.getStatus())) {
                ovrDisplay.append("<font class='txtOvrError'>!</font>&nbsp;");
            } else if (OverrideData.APPLIED.equalsIgnoreCase(od.getStatus())) {
                ovrDisplay.append("<font class='txtOvrApplied'>+</font>&nbsp;");
            } else if (OverrideData.PENDING.equalsIgnoreCase(od.getStatus())) {
                ovrDisplay.append("<font class='txtOvrPending'>?</font>&nbsp;");
            } else if (od.getStatus().equalsIgnoreCase("HOLDING")){
                ovrDisplay.append("&nbsp;");
            }

            localizedOvrName = otd.getLocalizedOvrName(localeId);

            if(!StringHelper.isEmpty(otd.getOvrtypLink()) && otd.getOvrtypLink().equalsIgnoreCase("Y") &&
               otPerm.intValue() == TimesheetConstants.EDIT && od.getMode().equalsIgnoreCase("edit") &&
               !(od.getTypeId() >= OverrideData.WORK_SUMMARY_TYPE_START &&
                od.getTypeId() <= OverrideData.WORK_SUMMARY_TYPE_END)){

                ovrDisplay.append("<a href=\"#\" onClick=\"newOverrideWindow(" + intBlankRec + ", this, '' , " + od.getType()+ ", '" + empDetails + "', " + empId + ", false)\">" +
                                  lh.getCaption("Edit_Override",
                                                LocalizableTag.CONFIG_MESSAGE,
                                                TimesheetConstants.TIMESHEET_MFRM_ID,
                                                "Edit") +
                                  "&nbsp;" + localizedOvrName + "</a>");
            } else {
                ovrDisplay.append(localizedOvrName);
            }

            ovrDisplay.append("</nobr></td>");

            //display start/end times of the override if applicable
            ovrDisplay.append("<td>");
            df.applyPattern("HH:mm");
            if(od.getStartTime() != null){
                ovrDisplay.append(df.format(od.getStartTime()) + "&nbsp;");

                if(od.getEndTime() == null){
                    ovrDisplay.append("- End&nbsp;&nbsp;");
                }
            }

            if(od.getEndTime() != null){
                if (od.getStartTime() != null){
                    ovrDisplay.append("- ");
                }
                else {
                    ovrDisplay.append("Start - ");
                }
                ovrDisplay.append(df.format(od.getEndTime()) + "&nbsp;");
            }


            ovrNewValue = FormatClockOvr.get().getClockFormat(od.getNewOvrValue()==null?"":od.getNewOvrValue(),
                                                              od.getOldOvrValue()==null?"":od.getOldOvrValue(),
                                                              "; ");
            ovrNewValue = JSPHelper.encodeHTML(ovrNewValue.substring(1,ovrNewValue.length()-1));
            ovrDisplay.append(StringHelper.searchReplace(OverrideFunctions.parseOverride(ovrNewValue,"&#34;,&#34;",pc,od.getTypeId()), "&#34;,&#34;", ",") +
                              "&nbsp;");
            ovrDisplay.append("( " + od.getCreatorUser() + ", ");

            ovrDisplay.append(lh.getConfigLink("OVR_DATE_TIME",
                              LocalizableTag.CONFIG_FIELD,
                              TimesheetConstants.TIMESHEET_MFRM_ID));
            df.applyPattern(TimesheetConstants.TIMESHEET_DATE_FORMAT);
            ovrDisplay.append(lh.getUI("OVR_DATE_TIME",
                                       "OVR_DATE_TIME",
                                       df.format(od.getCreateDate()),
                                       "",
                                       "view",
                                       TimesheetConstants.TIMESHEET_MFRM_ID,
                                       ""));
            ovrDisplay.append(")");

            if(od.getStatus().equals(OverrideData.ERROR)  && !StringHelper.isEmpty(od.getMessage())){
                ovrDisplay.append("<br>Error:&nbsp; " + od.getMessage());
            }

            if (!StringHelper.isEmpty(od.getComments()) &&
                !(od.getTypeId() >= OverrideData.WORK_SUMMARY_TYPE_START &&
                od.getTypeId() <= OverrideData.WORK_SUMMARY_TYPE_END &&
                lh.isHidden("OVR_COMMENT", TimesheetConstants.TIMESHEET_MFRM_ID))) {

                ovrDisplay.append("<br>").append(StringHelper.searchReplace(JSPHelper.encodeHTML(od.getComments()),
                                                                           "\"",
                                                                           "&#34;"));
            }
            ovrDisplay.append("</td></tr>");

            ovrCount++;
        //}
        return ovrDisplay.toString();*/
    }


    public static void copyElapsedTimes(int empId,
            ElapsedTimeView elapsedTimeView,
            CSMBiWeeklyTimeSheetPage fromWTS,
            CSMBiWeeklyTimeSheetPage toWTS) throws Exception {

        copyElapsedTimes(empId,
                elapsedTimeView,
                fromWTS.getWeekStartDate(),
                toWTS.getWeekStartDate(),
                fromWTS.getVisibleTeamDateSet(),
                toWTS.getVisibleTeamDateSet(),
                (CSMElapsedTimePage) fromWTS.getElapsedTime(),
                (CSMElapsedTimePage) toWTS.getElapsedTime());
    }

    /**
     * @param toWTS
     * @param empId
     * @param elapsedTimeView
     * @param fromWTS
     * @throws Exception
     */


    public static void copyElapsedTimes(int empId,
            ElapsedTimeView elapsedTimeView,
            java.util.Date fromWeekStartDate,
            java.util.Date toWeekStartDate,
            EmployeeIdStartEndDateSet fromVisibleSet,
            EmployeeIdStartEndDateSet toVisibleSet,
            CSMElapsedTimePage fromETPage,
            CSMElapsedTimePage toETPage) throws Exception {

        java.util.Date prevWeekWrkDate = fromWeekStartDate;
        java.util.Date currWeekWrkDate = toWeekStartDate;
        //clear the current week's elapsed time section appropriately
        Integer zero = new Integer(0);
        for (int line=0; line < toETPage.getElapsedTimeLines().size(); line++) {
            CSMElapsedTimeLine elapsedTimeLine = (CSMElapsedTimeLine) toETPage.getElapsedTimeLine(line);
            prevWeekWrkDate = fromWeekStartDate;
            currWeekWrkDate = toWeekStartDate;
            for (int day=0; day < DAYS_ON_TIMESHEET; day++) {
                if (fromVisibleSet.includes(empId, prevWeekWrkDate)
                        && toVisibleSet.includes(empId, currWeekWrkDate) ) {
                    elapsedTimeLine.getHours().set(day, zero);
                }
                prevWeekWrkDate = DateHelper.addDays(prevWeekWrkDate, 1);
                currWeekWrkDate = DateHelper.addDays(currWeekWrkDate, 1);
            }
        }

        // now append the qualified elapsed time entries from prev to current week. (with not visible days appropriately zero'ed)
        for (int line=0; line < fromETPage.getElapsedTimeLines().size(); line++) {
            CSMElapsedTimeLine elapsedTimeLine = (CSMElapsedTimeLine) fromETPage.getElapsedTimeLine(line);
            prevWeekWrkDate = fromWeekStartDate;
            currWeekWrkDate = toWeekStartDate;
            for (int day=0; day < DAYS_ON_TIMESHEET; day++) {
                if (!fromVisibleSet.includes(empId, prevWeekWrkDate)
                        || !toVisibleSet.includes(empId, currWeekWrkDate) ) {
                    elapsedTimeLine.getHours().set(day, zero);
                }
                prevWeekWrkDate = DateHelper.addDays(prevWeekWrkDate, 1);
                currWeekWrkDate = DateHelper.addDays(currWeekWrkDate, 1);
            }
            for (int j = 0; j < elapsedTimeLine.getOvrIds().size(); j++) {
                elapsedTimeLine.getOvrIds().set(j, new Integer(-1));
            }
            toETPage.getElapsedTimeLines().add(elapsedTimeLine);
        }


        // now adjust totals & remove any elapsedLines that has zero totals
        int weeklyElapsedTimeTotal = 0;
        int dayTotals[] = new int[DAYS_ON_TIMESHEET];
        for (Iterator lineIter = toETPage.getElapsedTimeLines().iterator(); lineIter.hasNext() ;) {
            CSMElapsedTimeLine elapsedTimeLine = (CSMElapsedTimeLine) lineIter.next();
            int lineTotal = 0;
            for (int day=0; day < DAYS_ON_TIMESHEET; day++) {
                lineTotal += elapsedTimeLine.getHour(day);
                dayTotals[day] += elapsedTimeLine.getHour(day);
            }
            weeklyElapsedTimeTotal += lineTotal;
            if (lineTotal == 0) {
                lineIter.remove();
            } else {
                elapsedTimeLine.setLineTotal(lineTotal);
            }
        }
        toETPage.setTotal(weeklyElapsedTimeTotal);
        for (int day=0; day < dayTotals.length; day++) {
            toETPage.getDayTotals().set(day, new Integer(dayTotals[day]));
        }
        // add 3 blank lines.
        for (int i = 0; i < 3; i++) {
            toETPage.getElapsedTimeLines().add(createBlankLine(elapsedTimeView));
        }
    }



    public static SchedulePage incrementScheduleByOneWeek(SchedulePage schedPage) {
        Iterator schedDaysIter = schedPage.getScheduleDays().iterator();

        while (schedDaysIter.hasNext()) {
            DayScheduleModel daySchedModel = (DayScheduleModel)schedDaysIter.next();
            if (! (((java.util.Date)DateHelper.truncateToDays(daySchedModel.getDefStartTime())).compareTo(daySchedModel.getDefStartTime() ) == 0) && ((daySchedModel.getEndTime()).equals(daySchedModel.getDefStartTime()))) {
                daySchedModel.setDefStartTime(DateHelper.addDays(daySchedModel.getDefStartTime(), DAYS_ON_TIMESHEET));
                daySchedModel.setEndTime(DateHelper.addDays(daySchedModel.getEndTime(), DAYS_ON_TIMESHEET));
                daySchedModel.setActStartTime(DateHelper.addDays(daySchedModel.getActStartTime(), DAYS_ON_TIMESHEET));
                daySchedModel.setWrkStartTime(DateHelper.addDays(daySchedModel.getWrkStartTime(), DAYS_ON_TIMESHEET));
                daySchedModel.setWrkEndTime(DateHelper.addDays(daySchedModel.getWrkEndTime(), DAYS_ON_TIMESHEET));
            }
        }

        return schedPage;
    }

    public static int getMinsInSchedule( DayScheduleModel day ) {
    	return WTShelper.getMinsInSchedule(day);
/*        int minutes = 0;
        if( day.getActStartTime() != null && day.getEndTime() != null ) {
            minutes
                += DateHelper.getMinutesBetween(
                        day.getEndTime(),
                        day.getActStartTime());
        }
        return minutes;
*/    }


    public static CSMElapsedTimeLine createBlankLine(
            ElapsedTimeView elapsedTimeView) throws Exception {
        CSMElapsedTimeLine line = new CSMElapsedTimeLine();
        List hours = new ArrayList();
        for (int j = 0; j < DAYS_ON_TIMESHEET; j++) {
            hours.add(new Integer(0));
        }
        List ovrIds = new ArrayList();
        for (int j = 0; j < DAYS_ON_TIMESHEET; j++) {
            ovrIds.add(new Integer(-1));
        }
        line = new CSMElapsedTimeLine();
        List values = new ArrayList();
        for (int j = 0; j < elapsedTimeView.getFieldNames().size(); j++) {
            values.add("");
        }
        line.setFields(values);
        line.setHours(hours);
        line.setOvrIds(ovrIds);

        return line;
    }

    public static Date getHandsOffDateForEmp(CSMEmployeeModel emp, Date currentDate ) {
        HashMap paygroupHandsoffDates = emp.getPaygroupHandsoffDates();
        if (paygroupHandsoffDates != null) {
	        Iterator paygrpIte = paygroupHandsoffDates.keySet().iterator();
	        while(paygrpIte.hasNext()) {
	            EmployeeIdStartEndDate key = (EmployeeIdStartEndDate)paygrpIte.next();
	            if (DateHelper.isBetween(currentDate,key.getStartDate(), key.getEndDate())) {
	                return (Date) paygroupHandsoffDates.get(key);
	            }
	        }
        }
        return emp.getHandsOffDate();
    }

    public static Date getSupervisorDateForEmp(CSMEmployeeModel emp, Date currentDate ) {
        HashMap paygroupSupervisorDates = emp.getPaygroupSupervisorDates();
        if (paygroupSupervisorDates != null) {
	        Iterator paygrpIte = paygroupSupervisorDates.keySet().iterator();
	        while(paygrpIte.hasNext()) {
	            EmployeeIdStartEndDate key = (EmployeeIdStartEndDate)paygrpIte.next();
	            if (DateHelper.isBetween(currentDate,key.getStartDate(), key.getEndDate())) {
	                return (Date) paygroupSupervisorDates.get(key);
	            }
	        }
        }
        return emp.getSupervisorDate();
    }

    /**
     * Returns a list of all ranges of dates between startDate and endDate, that are not editable
     * either because its locked or within the hands Off period
     *
     * @param conn      - Database connection
     * @param empId     - employee id
     * @param startDate - start date for the range to look at
     * @param endDate   - end date for the range to look at
     * @param isSupervisor - specifies if the user has lockdown privileges
     * @return list of TimePeriod objects, each corresponding to a
     *         date range of locked dates
     *         empty list, if NO date between startDate and endDate are blocked by hands off date or timesheet locks
     */
    public static List getLockedRange(DBConnection conn, int empId, Date startDate, Date endDate, boolean isSupervisor) {
    	return WTShelper.getLockedRange(conn, empId, startDate, endDate, isSupervisor);
        /*List subtractList = new ArrayList();

        subtractList.addAll(TimesheetHelper.getHandsOffDateRangesFromDB(conn, empId, startDate, endDate, isSupervisor));

        //get ranges locked by timesheet locks
        subtractList.addAll(TimesheetHelper.getLockedDatesRanges(conn, empId, startDate, endDate));

        try {
            subtractList = TimePeriodHelper.union(subtractList);
        } catch (Exception e) {
            //need to do nothing
            //unable to unionize the subtractList..is not an issue.
        }

        return subtractList;
*/
    }

    public static int getMfrmId(ServletRequest request) {
    	if (_mfrmId == DEFAULT_MFRM_ID) {
    		String sMfrmId = request.getParameter("mfrm_id");
    		if (sMfrmId != null) {
    			try {
    				_mfrmId = Integer.parseInt(sMfrmId);
    			} catch (Exception e) {
    				logger.error("Invalid mfrm_id argument: " + sMfrmId);
    			}
    		}
    	}
/*		if (_mfrmId == DEFAULT_MFRM_ID) {
			PreparedStatement ps = null;
			ResultSet rs = null;
			DBConnection conn = DBConnection.;
			try {
				ps = conn.prepareStatement("select MFRM_ID from MAINTENANCE_FORM where MFRM_NAME = '" + MFRM_NAME + "'");
				rs = ps.executeQuery();
				if (rs.next()) {
					_mfrmId = rs.getInt("MFRM_ID");
				}
			} catch (Exception e) {
				logger.error("Please set the MFRM_ID", e);
			} finally {
				SQLHelper.cleanUp(ps,rs);
			}
		}*/
    	return _mfrmId;
    }

    public static Vector getPremiumTimeCodes(DBConnection conn) throws SQLException{

    	Vector premTCodeList = new Vector();

    	PreparedStatement pStmt = null;
		ResultSet rs = null;
		String sql = "SELECT TCODE_NAME"
				+ " FROM TIME_CODE WHERE TCODE_FLAG6='Y' ";

		try {
			pStmt = conn.prepareStatement(sql);
			rs = pStmt.executeQuery();
			while (rs.next()) {
				premTCodeList.add(rs.getString("TCODE_NAME"));
			}
		} finally {
			SQLHelper.cleanUp(rs);
			SQLHelper.cleanUp(pStmt);
		}


    	return premTCodeList;
    }

    public static void markPremiumLines(DBConnection conn,List lines) {
		try {
			Vector premiumTimeCodes = CSMWTShelper.getPremiumTimeCodes(conn);

			Iterator itr = lines.iterator();
			while (itr.hasNext()) {
				CSMElapsedTimeLine line = (CSMElapsedTimeLine) itr.next();
				if (line!=null && line.hasData()) {
					List fields =line.getFields();
					if(fields!=null){
						String tcode = (String)fields.get(0);
						if(premiumTimeCodes.contains(tcode))
							line.setPremium(true);
					}
				}
			}

		} catch (SQLException e) {
			logger.error(e);
		}
	}

}
