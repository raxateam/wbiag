package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletRequest;

import com.workbrain.app.jsp.ActionContext;
import com.workbrain.app.jsp.action.dateselection.DateRange;
import com.workbrain.app.jsp.action.timesheet.DayOverrideModel;
import com.workbrain.app.jsp.action.timesheet.DayScheduleModel;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimeView;
import com.workbrain.app.jsp.action.timesheet.OverrideModel;
import com.workbrain.app.jsp.action.timesheet.OverridePage;
import com.workbrain.app.jsp.action.timesheet.OverrideString;
import com.workbrain.app.jsp.action.timesheet.SchedulePage;
import com.workbrain.app.jsp.action.timesheet.UpdateLockedCmd;
import com.workbrain.app.jsp.action.timesheet.WorkDetailModel;
import com.workbrain.app.jsp.action.timesheet.WorkSummaryModel;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.app.ta.model.EmployeeIdStartEndDateSet;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.util.AuthorizationRightsHelper;
import com.workbrain.server.WorkbrainParametersRetriever;
import com.workbrain.server.data.type.BooleanType;
import com.workbrain.server.data.type.DatetimeType;
import com.workbrain.server.jsp.taglib.sys.AbstractFieldTag;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.tool.overrides.AbstractOverrideBuilder;
import com.workbrain.tool.overrides.AbstractOverrideOperation;
import com.workbrain.tool.overrides.InsertWorkDetailOverride;
import com.workbrain.tool.overrides.OverrideException;
import com.workbrain.util.DateHelper;
import com.workbrain.util.LocalizationHelper;
import com.workbrain.util.StringHelper;

public abstract class CSMAbstractSaveBWTimeSheetAction extends
		CSMViewBiWeeklyTimeSheetAction {

	protected final static DateFormat DATE_OVR_FORMAT = new SimpleDateFormat(
			"MM/dd/yyyy HH:mm");

	protected final static int SCHEDULE_SCHEDTIMES_TYPE = 401;

	protected final static int TIMESHEET_TYPE_START = 600;

	protected final static int TIMESHEET_TYPE_END = 699;

	protected final static int PREMIUM_TYPE_START = 200;

	protected static Vector premTCodeList;

	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(CSMAbstractSaveBWTimeSheetAction.class);

	private int recno;

	protected abstract AbstractOverrideBuilder getOverrideBuilder(
			DBConnection conn);

	protected abstract AbstractOverrideOperation getInsertOverride();

	protected abstract AbstractOverrideOperation getDeleteOverride();

	public Object createRequest(ServletRequest request, DBConnection conn)
			throws Exception {
		logger.debug("createRequest()  start");
		recno = 0;
		CSMBiWeeklyTimeSheetPage model = new CSMBiWeeklyTimeSheetPage();

		model.setRecalculate(true);

		boolean submitted = false;
		if (request.getParameter("SUBMITTED") != null) {
			submitted = true;
		}
		model.setSubmitted(submitted);

		boolean locked = false;
		if (request.getParameter("LOCKED") != null) {
			locked = true;
		}
		model.setLocked(locked);

		// model.setSchedule(createScheduleRequest(request, conn));
		model.setElapsedTime(createElapsedTimeRequest(request, conn));
		model.setNonWorkTimePage(createNonWorkTimeRequest(request, conn));
		model.setFullDayAbsencePage(createFullDayAbsenceRequest(request, conn));

		// model.setOverride(createOverrideRequest(request, conn));
		model.setSummaries(createWorkSummaryRequest(request, conn));

		logger.debug("createRequest()   return");
		return model;
	}

	public abstract String process(DBConnection conn, ActionContext context,
			Object requestObj) throws Exception;

	protected boolean applyChangedFields(DBConnection conn,
			ElapsedTimeView elapsedTimeView, OverrideString ovrString,
			CSMElapsedTimeLine newLine, CSMElapsedTimeLine oldLine)
			throws Exception {
		logger.debug("applyChangedFields(...)   start");

		boolean changed = false;

		boolean isPremium = false;

		for (int j = 0; j < newLine.getFields().size(); j++) {

			String newField = (String) newLine.getFields().get(j);
			String oldField = (String) oldLine.getFields().get(j);
			String fieldName = elapsedTimeView.getFieldName(j);

			//CSM CR27:
			//If we're looking at the first field, we may be looking at the timecode
			//Some timecodes are marked as premiums and should use WRKP instead of WRKD
			//for the fieldName
			if(j==0 && fieldName.equals("WRKD_TCODE_NAME")){
				if(premTCodeList!=null && premTCodeList.contains(newField))
					isPremium=true;
			}

			if(isPremium){
				fieldName = fieldName.replaceAll("WRKD", "WRKP");
			}

			ovrString.set(fieldName, newField);
			if (oldField != null) {
				if (!oldField.equals(newField)) {
					changed = true;
				}
			}
		}

		logger.debug("applyChangedFields(...)   return");
		return changed;
	}

	protected int getAllFields(DBConnection conn,
			ElapsedTimeView elapsedTimeView, CSMElapsedTimeLine newLine,
			OverrideString ovrString) throws Exception {
		logger.debug("getAllFields(...)   start");

		for (int j = 0; j < newLine.getFields().size(); j++) {
			String newField = (String) newLine.getFields().get(j);
			if (!"".equals(newField)) {
				String fieldName = elapsedTimeView.getFieldName(j);
				ovrString.set(fieldName, newField);
			}
		}

		logger.debug("getAllFields(...)   return");
		return ovrString.length();
	}

	public boolean processFields(CSMBiWeeklyTimeSheetPage wtsRequest,
			DBConnection conn, ElapsedTimeView elapsedTimeView,
			CSMElapsedTimeLine oldLine, CSMElapsedTimeLine newLine, int day,
			OverrideString ovrString) throws Exception {
		logger.debug("processFields(...)   start");

		boolean doOverride = false;

		boolean changed = applyChangedFields(conn, elapsedTimeView, ovrString,
				newLine, oldLine);

		int newHour = 0;
		int oldHour = 0;
		if (newLine.getHours().size() > day) {
			newHour = ((Integer) newLine.getHours().get(day)).intValue();
		}
		if (oldLine.getHours().size() > day) {
			oldHour = ((Integer) oldLine.getHours().get(day)).intValue();
		}
		if (newHour == 0) {
			ovrString.reset("");
			if (newHour != oldHour) {
				doOverride = true;
			}
		} else {
			if (ovrString.length() > 0) {
				if(ovrString.get("WRKP_TCODE_NAME")!=null){
					ovrString.set("WRKP_MINUTES", String.valueOf(newHour));
					ovrString.set("WRKP_REMOVE_SAME_TIMECODE", "N");
				}
				else
					ovrString.set("WRKD_MINUTES", String.valueOf(newHour));
			}
			if (newHour != oldHour || changed || oldLine.getDistributed()) {
				doOverride = true;
			}
		}

		logger.debug("processFields(...)   return");
		return doOverride;
	}

	protected boolean buildOverride(DBConnection conn, List fieldNames,
			CSMElapsedTimeLine oldLine, CSMElapsedTimeLine newLine,
			int oldHour, int newHour, StringBuffer ovrString) throws Exception {
		logger.debug("buildOverride(...)   start");

		boolean changed = false;
		StringBuffer fields = new StringBuffer();
		for (int i = 0; i < fieldNames.size(); i++) {
			String oldValue = (String) oldLine.getFields().get(i);
			String newValue = (String) newLine.getFields().get(i);
			if (newHour == 0 && oldHour != 0) {
				changed = true;
			} else if (!(oldHour == 0 && newHour == 0)) {
				changed = changed
						|| addOverride(oldValue, newValue, (String) fieldNames
								.get(i), fields);
			}
		}
		if (fields.toString().length() > 0) {
			ovrString.append("\"WRKD_MINUTES=" + newHour + "\""
					+ fields.toString());
		}

		logger.debug("buildOverride(...)   return");
		return changed || oldHour != newHour;
	}

	protected boolean addOverride(String oldValue, String newValue,
			String ovrField, StringBuffer ovrString) {
		logger.debug("addOverride(...)   start");

		if (newValue.trim().length() > 0) {
			ovrString.append(",\"" + ovrField + "=" + newValue + "\"");
		}
		logger.debug("addOverride(...)   return");

		return oldValue.compareTo(newValue) != 0;
	}

	public List createWorkSummaryRequest(ServletRequest request,
			DBConnection conn) throws Exception {
		logger.debug("createWorkSummaryRequest(...)   start");

		boolean isAllChecked = (request.getParameter("AUTHORIZED") != null && "Y"
				.equals(request.getParameter("AUTHORIZED").toString()));

		List summaries = new ArrayList();
		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			WorkSummaryModel summary = new WorkSummaryModel();
			boolean authorized = false;
			if ((request.getParameter("AUTHORIZED_" + i) != null && "Y"
					.equals(request.getParameter("AUTHORIZED_" + i).toString()))
					|| isAllChecked) {
				authorized = true;
			}
			summary.setAuthorized(authorized);
			boolean locked = false;
			if (request.getParameter("LOCKED_" + i) != null
					&& "Y".equals(request.getParameter("LOCKED_" + i)
							.toString())) {
				locked = true;
			}
			boolean weekLocked = false;
			if (request.getParameter("LOCKED") != null) {
				weekLocked = true;
			}
			summary.setLocked(locked || weekLocked);

			String comments = request.getParameter("COMMENTS_" + i);
			summary.setComments(comments == null ? "" : comments);

			List udfs = new ArrayList();
			for (int j = 0; j < 10; j++) {
				String udf = request.getParameter("UDFS_" + i + "_" + j);
				udfs.add(udf == null ? "" : udf);
			}
			summary.setUdfs(udfs);

			List flags = new ArrayList();
			for (int j = 0; j < 5; j++) {
				String flag = request.getParameter("FLAGS_" + i + "_" + j);
				flags.add(flag == null ? "" : flag);
			}
			summary.setFlags(flags);
			String flag = request.getParameter("FLAG_BRK_" + i);
			summary.setFlagBrk(flag == null ? "" : flag);
			flag = request.getParameter("FLAG_RECALL_" + i);
			summary.setFlagRecall(flag == null ? "" : flag);

			summaries.add(summary);
		}
		logger.debug("createWorkSummaryRequest(...)   return");
		return summaries;
	}

	public SchedulePage createScheduleRequest(ServletRequest request,
			DBConnection conn) throws Exception {
		logger.debug("createScheduleRequest(...)   start");

		SchedulePage form = new SchedulePage();
		List schedules = new ArrayList();
		String weekStart = (String) request.getParameter("WEEK_START_DATE");
		boolean startFlag = false;
		Date currDate;
		String currDateS;
		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {

			currDate = DateHelper.addDays(DatetimeType.FORMAT.parse(weekStart),
					i);
			currDateS = DatetimeType.FORMAT.format(currDate);

			DayScheduleModel schedule = new DayScheduleModel();
			Date date = null;
			String actTime = (String) request.getParameter("ACT_START_TIME_"
					+ i);
			if (actTime != null && !"".equals(actTime.trim())) {
				actTime = currDateS.substring(0, 8) + actTime.substring(8, 15);
				date = DatetimeType.FORMAT.parse(actTime);
			} else {
				date = DateHelper.addDays(DatetimeType.FORMAT.parse(weekStart),
						i);
			}
			schedule.setActStartTime(date);

			String startTime = (String) request.getParameter("SCH_START_TIME_"
					+ i);
			date = null;
			if (startTime != null && !"".equals(startTime.trim())) {
				startTime = currDateS.substring(0, 8)
						+ startTime.substring(8, 15);
				date = DatetimeType.FORMAT.parse(startTime);
			} else {
				date = DateHelper.addDays(DatetimeType.FORMAT.parse(weekStart),
						i);
				startFlag = true;
			}
			schedule.setDefStartTime(date);

			String endTime = (String) request.getParameter("SCH_END_TIME_" + i);
			date = null;
			if (endTime != null && !"".equals(endTime.trim())) {
				endTime = currDateS.substring(0, 8) + endTime.substring(8, 15);
				date = DatetimeType.FORMAT.parse(endTime);
			} else {
				if (startFlag) {
					date = DateHelper.addDays(DatetimeType.FORMAT
							.parse(weekStart), i);
					startFlag = false;
				} else {
					date = DateHelper.addDays(DatetimeType.FORMAT
							.parse(weekStart), i + 1);
				}
			}
			schedule.setEndTime(date);
			if (schedule.getDefStartTime().compareTo(schedule.getEndTime()) > 0) {
				schedule.setEndTime(DateHelper
						.addDays(schedule.getEndTime(), 1));
			}

			date = null;
			String wrkStartTimeStr = (String) request
					.getParameter("WRK_START_TIME_" + i);
			if (wrkStartTimeStr != null && !"".equals(wrkStartTimeStr.trim())) {
				wrkStartTimeStr = currDateS.substring(0, 8)
						+ wrkStartTimeStr.substring(8, 15);
				date = DatetimeType.FORMAT.parse(wrkStartTimeStr);
			} else {
				date = DateHelper.DATE_1900;
				// DateHelper.addDays(DatetimeType.FORMAT.parse(weekStart), i);
			}
			schedule.setWrkStartTime(date);

			date = null;
			String wrkEndTimeStr = (String) request
					.getParameter("WRK_END_TIME_" + i);
			if (wrkEndTimeStr != null && !"".equals(wrkEndTimeStr.trim())) {
				wrkEndTimeStr = currDateS.substring(0, 8)
						+ wrkEndTimeStr.substring(8, 15);
				date = DatetimeType.FORMAT.parse(wrkEndTimeStr);
			} else {
				date = DateHelper.DATE_1900;
			}
			schedule.setWrkEndTime(date);

			date = null;
			String brkStartTimeStr = (String) request
					.getParameter("BRK_START_TIME_" + i);
			if (brkStartTimeStr != null && !"".equals(brkStartTimeStr.trim())) {
				brkStartTimeStr = currDateS.substring(0, 8)
						+ brkStartTimeStr.substring(8, 15);
				date = DatetimeType.FORMAT.parse(brkStartTimeStr);
			} else {
				date = DateHelper.DATE_1900;
			}
			schedule.setBrkStartTime(date);

			date = null;
			String brkEndTimeStr = (String) request
					.getParameter("BRK_END_TIME_" + i);
			if (brkEndTimeStr != null && !"".equals(brkEndTimeStr.trim())) {
				brkEndTimeStr = currDateS.substring(0, 8)
						+ brkEndTimeStr.substring(8, 15);
				date = DatetimeType.FORMAT.parse(brkEndTimeStr);
			} else {
				date = DateHelper.DATE_1900;
			}
			schedule.setBrkEndTime(date);

			String noMealBreakStr = (String) request
					.getParameter("NO_MEAL_BREAK_" + i);
			schedule.setNoMealBreak(noMealBreakStr != null);
			if (schedule.getNoMealBreak()) {
				schedule.setBrkStartTime(DateHelper.DATE_1900);
				schedule.setBrkEndTime(DateHelper.DATE_1900);
			}

			schedules.add(schedule);
		}
		form.setScheduleDays(schedules);

		List schOvrIdsToDelete = new ArrayList();
		int schOvrLineCnt = 0;
		try {
			schOvrLineCnt = Integer.parseInt(request
					.getParameter("SCH_OVR_LINE_CNT"));
		} catch (Exception e) {
			logger.debug(e);
		}
		for (int i = 0; i < schOvrLineCnt; i++) {
			String param = request.getParameter("DELETE_SCH_OVR_" + i);
			if (param != null) {
				Integer ovrId = new Integer(param);
				schOvrIdsToDelete.add(ovrId);
			}
		}
		form.setSchOvrIdsToDelete(schOvrIdsToDelete);

		logger.debug("createScheduleRequest(...)   return");
		return form;
	}

	public CSMElapsedTimePage createElapsedTimeRequest(ServletRequest request,
			DBConnection conn) throws Exception {
		logger.debug("createElapsedTimeRequest(...)   start");
		CSMElapsedTimePage form = new CSMElapsedTimePage();

		int i = 0;
		int numFields = 0;
		try{
			numFields = Integer.parseInt(StringHelper.isEmpty(request
				.getParameter("ELAPSED_NUM_FIELDS")) ? "-1" : request
				.getParameter("ELAPSED_NUM_FIELDS"));
		}catch(Exception e){
			logger.debug(e);
		}
		String field = request.getParameter("FIELDS0X_" + i);
		while (field != null) {
			CSMElapsedTimeLine line = new CSMElapsedTimeLine();
			String del = request.getParameter("WTSDELET_" + i);

			if (del != null) {
				line.setDel(true);
			}

			List fields = new ArrayList();
			int j = 0;
			while (field != null) {
				fields.add(field);
				j++;
				field = request.getParameter("FIELDS" + j + "X_" + i);
				if (field == null && j < numFields) {
					field = "";
				}
			}
			line.setFields(fields);

			List hours = new ArrayList();
			String hourStr;
			for (int k = 0; k < DAYS_ON_TIMESHEET;) {
				hourStr = request.getParameter("HOUR_" + i + "_" + k);
				// WBLogger.debug("com.workbrain.app.jsp.action.timesheetAbstractSaveTimeSheetAction.class",
				// "HOUR_" + i + "_" + k + " : " + hourStr);
				int hour = 0;
				if (!StringHelper.isEmpty(hourStr)) {
					try {
						hour = new Integer(hourStr).intValue();
					} catch (Exception e) {
						logger.debug(e);
						hour = 0;  // Conversation with Bona on Aug 28,2006 ****
					}
					hours.add(new Integer(hour));
				} else {
					hours.add(new Integer(hour));
				}
				k++;
			}
			line.setHours(hours);
			form.getElapsedTimeLines().add(line);

			i++;
			field = request.getParameter("FIELDS0X_" + i);
		}
		logger.debug("createElapsedTimeRequest(...)   return");
		return form;
	}

	public CSMLTAPage createFullDayAbsenceRequest(ServletRequest request,
			DBConnection conn) throws Exception {
		logger.debug("createFullDayAbsenceRequest(...)   start");
		CSMLTAPage form = new CSMLTAPage();

		int i = 0;
		String field = request.getParameter("WTSLTANAME_" + i);
		while (field != null) {
			CSMLTAPage.LTALine line = new CSMLTAPage.LTALine(field);
			String all = request.getParameter("WTSLTAALL_" + i);

			if (all != null) {
				line.setIsAll(true);
			}

			for (int day = 0; day < DAYS_ON_TIMESHEET; day++) {
				boolean checked = request.getParameter("WTSLTA" + day + "X_"
						+ i) != null;
				int id = -1;
				try {
					id = Integer.parseInt(request.getParameter("WTSLTAOVR"
							+ day + "X_" + i));
				} catch (Exception pe) {
					logger.debug(pe);
				}
				CSMLTAPage.Override ovr = new CSMLTAPage.Override();
				ovr.setIsChecked(checked);
				ovr.setId(id);
				line.getDays().add(ovr);
			}
			form.getLines().add(line);

			field = request.getParameter("WTSLTANAME_" + ++i);
		}
		logger.debug("createFullDayAbsenceRequest(...)   return");
		return form;
	}

	public CSMLTAPage createNonWorkTimeRequest(ServletRequest request,
			DBConnection conn) throws Exception {
		logger.debug("createNonWorkTimeRequest(...)   start");
		CSMLTAPage form = new CSMLTAPage();

		int i = 0;
		String field = request.getParameter("WTSNWTNAME_" + i);
		while (field != null) {
			CSMLTAPage.LTALine line = new CSMLTAPage.LTALine(field);

			String del = request.getParameter("WTSNWTDEL_" + i);
			if (del != null) {
				line.setDel(true);
			}

			for (int day = 0; day < DAYS_ON_TIMESHEET; day++) {
				CSMLTAPage.Override ovr = new CSMLTAPage.Override();
				String str = request.getParameter("WTSNWTSTART" + day + "X_"
						+ i);
				if (str != null) {
					try {
						ovr.setStart(DatetimeType.FORMAT.parse(str));
					} catch (Exception e) {
						logger.debug("", e);
					}
				}
				str = request.getParameter("WTSNWTSTOP" + day + "X_" + i);
				if (str != null) {
					try {
						ovr.setStop(DatetimeType.FORMAT.parse(str));
					} catch (Exception e) {
						logger.debug("", e);
					}
				}

				int id = -1;
				try {
					id = Integer.parseInt(request.getParameter("WTSNWTOVR"
							+ day + "X_" + i));
				} catch (Exception pe) {
					logger.debug(pe);
				}
				ovr.setId(id);
				line.getDays().add(ovr);
			}
			form.getLines().add(line);

			field = request.getParameter("WTSNWTNAME_" + ++i);
		}
		logger.debug("createNonWorkTimeRequest(...)   return");

		return form;
	}

	public OverridePage createOverrideRequest(ServletRequest request,
			DBConnection conn) throws Exception {
		logger.debug("createOverrideRequest(...)   start");

		OverridePage form = new OverridePage();
		List dayOverrides = new ArrayList();

		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			recno = recno + DAYS_ON_TIMESHEET;
			DayOverrideModel dayOverride = createDayOverride(request, conn, i);
			dayOverrides.add(dayOverride);
		}
		form.setOverrideDays(dayOverrides);
		logger.debug("createOverrideRequest(...)   return");

		return form;
	}

	protected DayOverrideModel createDayOverride(ServletRequest request,
			DBConnection conn, int i) throws Exception {
		logger.debug("createDayOverride(...)   start");

		DayOverrideModel dayOverride = new DayOverrideModel();
		boolean apply = false;
		String applyStr = (String) request.getParameter("APPLY_OVR_" + i);
		if (applyStr != null) {
			apply = ((Boolean) BooleanType.get().convert(applyStr))
					.booleanValue();
			dayOverride.setApply(apply);
		}

		List ovrs = new ArrayList();
		int j = 0;
		String ovrId = (String) request.getParameter("OVR_" + i + "_" + j);
		while (ovrId != null) {
			ovrs.add(createOverride(request, ovrId, i, j, recno));
			j++;
			recno = recno + 1;
			ovrId = (String) request.getParameter("OVR_" + i + "_" + j);
		}
		dayOverride.setOverrides(ovrs);
		logger.debug("createDayOverride(...)   return");
		return dayOverride;
	}

	protected OverrideModel createOverride(ServletRequest request,
			String ovrId, int i, int j, int recno) throws Exception {
		logger.debug("createOverride(...)   start");

		String delete = (String) request.getParameter("DELETE_OVR_" + recno);
		String copy = (String) request.getParameter("COPY_OVR_" + i + "_" + j);
		OverrideModel override = new OverrideModel();
		int id = 0;
		try {
			id = Integer.parseInt(ovrId);
		} catch (Exception e) {
			logger.debug(e);
		}
		override.setId(id);
		if (delete != null) {
			override.setDelete(((Boolean) BooleanType.get().convert(delete))
					.booleanValue());
		}
		if (copy != null) {
			override.setCp(((Boolean) BooleanType.get().convert(copy))
					.booleanValue());
		}
		logger.debug("createOverride(...)   return");
		return override;
	}

	protected void processSubmitted(CSMBiWeeklyTimeSheetPage model,
			CSMBiWeeklyTimeSheetPage request, AbstractOverrideBuilder ovrBuilder)
			throws SQLException, OverrideException {
		logger.debug("processSubmitted(...)   start");

		if (!model.getSubmitted() && request.getSubmitted()) {
			setSubmitted(model, ovrBuilder, true);
		} else if (model.getSubmitted() && !request.getSubmitted()) {
			setSubmitted(model, ovrBuilder, false);
		}
		logger.debug("processSubmitted(...)   return");
	}

	protected void processDailyLocks(DBConnection conn,
			CSMBiWeeklyTimeSheetPage model, CSMBiWeeklyTimeSheetPage request,
			LocalizationHelper lh) throws SQLException {
		logger.debug("processDailyLocks(...)   start");

		// find locks to insert/delete
		List summariesModel = model.getSummaries();
		List summariesRequest = request.getSummaries();
		ArrayList newLocks = new ArrayList();
		ArrayList newUnlocks = new ArrayList();
		ArrayList lockDateRanges, unlockDateRanges;
		int empId = model.getEmployee().getEmpId();

		if ((lh != null)
				&& lh.determineMode("LOCKED", "edit") == AbstractFieldTag.EDIT_MODE) {
			for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
				WorkSummaryModel wsmModel = (WorkSummaryModel) summariesModel
						.get(i);
				WorkSummaryModel wsmRequest = (WorkSummaryModel) summariesRequest
						.get(i);
				java.util.Date workDate = (java.util.Date) DateHelper
						.truncateToDays(wsmModel.getWorkDate());
				if (model.getVisibleTeamDateSet().includes(empId, workDate)) {
					if (wsmRequest.getLocked() && !wsmModel.getLocked()) {
						newLocks.add(workDate);
					} else if (!wsmRequest.getLocked() && wsmModel.getLocked()) {
						newUnlocks.add(workDate);
					}
				}
			}

			lockDateRanges = dateRangesFromDates(newLocks);
			unlockDateRanges = dateRangesFromDates(newUnlocks);
			updateDailyLocks(conn, model, lockDateRanges, unlockDateRanges);
		}

		logger.debug("processDailyLocks(...)   return");
	}

	private void updateDailyLocks(DBConnection conn,
			CSMBiWeeklyTimeSheetPage model, ArrayList locks, ArrayList unlocks)
			throws SQLException {
		// check that there are locks tobe processed
		if (locks == null && unlocks == null) {
			return;
		}
		if (locks.isEmpty() && unlocks.isEmpty()) {
			return;
		}
		PreparedStatement pStmt = null;
		ResultSet rs = null;
		String sql = "SELECT TSL_ID, TSL_START_DATE, TSL_END_DATE"
				+ " FROM TIMESHEET_LOCK WHERE EMP_ID = ? "
				+ " and tsl_end_date >= ?" + // lock enddate not before
												// week's startdate
				" and tsl_start_date <= ? " + // lock startdate not beyond
												// week's enddate
				" ORDER BY TSL_START_DATE";

		Integer empId = new Integer(model.getEmployee().getEmpId());
		EmployeeIdStartEndDateSet empTSLcks = new EmployeeIdStartEndDateSet();
		try {
			Timestamp weekStart = new Timestamp(model.getWeekStartDate()
					.getTime());
			Timestamp weekEnd = new Timestamp(model.getWeekEndDate().getTime());
			pStmt = conn.prepareStatement(sql);
			pStmt.setString(1, empId.toString());
			pStmt.setTimestamp(2, weekStart);
			pStmt.setTimestamp(3, weekEnd);

			rs = pStmt.executeQuery();
			while (rs.next()) {
				empTSLcks.add(empId.intValue(), rs
						.getTimestamp("TSL_START_DATE"), rs
						.getTimestamp("TSL_END_DATE"));
			}
		} finally {
			SQLHelper.cleanUp(rs);
			SQLHelper.cleanUp(pStmt);
		}

		// add new locks to the set
		Iterator newLckIter = locks.iterator();
		while (newLckIter.hasNext()) {
			DateRange dateRange = (DateRange) newLckIter.next();
			empTSLcks.add(empId.intValue(), dateRange.startDate,
					dateRange.endDate);
		}

		EmployeeIdStartEndDateSet delLcks = new EmployeeIdStartEndDateSet();
		// delete locks from the set as per user request
		Iterator delLckIter = unlocks.iterator();
		while (delLckIter.hasNext()) {
			DateRange dateRange = (DateRange) delLckIter.next();
			delLcks.add(empId.intValue(), dateRange.startDate,
					dateRange.endDate);
		}

		try {
			empTSLcks.delete(delLcks);
		} catch (Exception e) {
			logger.error("Problem encountered while removing timesheet locks.",
					e);
			throw new SQLException(e.getMessage());
		}

		saveDailyLocks(conn, model, empTSLcks);
	}

	private void saveDailyLocks(DBConnection conn,
			CSMBiWeeklyTimeSheetPage model,
			EmployeeIdStartEndDateSet lockSEDateSet) throws SQLException {
		int batchSize = WorkbrainParametersRetriever.getInt(
				RecordAccess.REG_WBPARM_RECORD_ACCESS_INSERTUPDATE_BATCH_SIZE,
				20);
		if (batchSize <= 0) {
			// sanity check so more appropriate msg in included in the
			// exception.
			throw new IllegalArgumentException(
					WorkbrainParametersRetriever.WB_PARAM_PATH
							+ RecordAccess.REG_WBPARM_RECORD_ACCESS_INSERTUPDATE_BATCH_SIZE
							+ " must be greater than zero.");
		}

		Integer empId = new Integer(model.getEmployee().getEmpId());
		try {
			// clear all related existing locks before putting the new
			// consolidated locks in.
			UpdateLockedCmd delExistingLckCmd = new UpdateLockedCmd(conn,
					model, false);
			delExistingLckCmd.addEmp(empId.intValue(), DateHelper
					.getEarliestEmployeeDate(), DateHelper
					.getLatestEmployeeDate());
			EmployeeIdStartEndDateSet currEmpWeek = new EmployeeIdStartEndDateSet();
			currEmpWeek.add(empId.intValue(), model.getWeekStartDate(), model
					.getWeekEndDate());
			currEmpWeek.runCommand(delExistingLckCmd);

			// now add newly consolidated locks
			UpdateLockedCmd addNewLckCmd = new UpdateLockedCmd(conn, model,
					true);
			addNewLckCmd.addEmp(empId.intValue(), DateHelper
					.getEarliestEmployeeDate(), DateHelper
					.getLatestEmployeeDate());
			lockSEDateSet.runCommand(addNewLckCmd);
		} catch (Exception e) {
			logger
					.error("Problem encountered while saving timesheet locks.",
							e);
			throw new SQLException(e.getMessage());
		}
	}

	/**
	 * @param dates
	 *            A list of sorted java.util.Date objects.
	 * @return A sorted list of DateRange objects where each range holds the
	 *         consecutive days from the dates List.
	 */
	private ArrayList dateRangesFromDates(List dates) {
		Iterator i = dates.iterator();
		ArrayList dateRanges = new ArrayList();
		Date old = null;

		if (i.hasNext()) {
			old = (java.util.Date) i.next();
			dateRanges.add(new DateRange(old, old));
		}
		while (i.hasNext()) {
			Date current = (Date) i.next();
			if (((Timestamp) DateHelper.truncateToDays(current))
					.equals(DateHelper.addDays(DateHelper.truncateToDays(old),
							1))) {
				((DateRange) dateRanges.get(dateRanges.size() - 1)).endDate = current;
			} else {
				dateRanges.add(new DateRange(current, current));
			}
			old = current;
		}
		return dateRanges;
	}

	// dateranges MUST have Date objects that have been truncated to Days.
	/*
	 * private ArrayList excludeRange(DateRange a, DateRange cut) { ArrayList
	 * newRanges = new ArrayList(); if (cut.overlaps(a)) { // case 1: the cut
	 * completely covers a, no ranges returned. if (cut.startDate.getTime() <=
	 * a.startDate.getTime() && cut.endDate.getTime() >= a.endDate.getTime()) {
	 * return newRanges; } // case 2: left side of range is cut if
	 * (cut.startDate.getTime() <= a.startDate.getTime()) { newRanges.add(new
	 * DateRange((java.util.Date)DateHelper.addDays(cut.endDate, 1),
	 * a.endDate)); return newRanges; } // case 3: right side of range is cut if
	 * (cut.endDate.getTime() >= a.endDate.getTime()) { newRanges.add(new
	 * DateRange(a.startDate, (java.util.Date)DateHelper.addDays(cut.startDate,
	 * -1))); return newRanges; } // case 4: the cut splits the range in two
	 * DateRange first = new DateRange(a.startDate,
	 * (java.util.Date)DateHelper.addDays(cut.startDate,-1)); DateRange second =
	 * new DateRange((java.util.Date)DateHelper.addDays(cut.endDate,1),
	 * a.endDate); newRanges.add(first); newRanges.add(second); return
	 * newRanges; } return newRanges; }
	 */

	protected boolean processAuthorized(DBConnection conn,
			CSMBiWeeklyTimeSheetPage model, CSMBiWeeklyTimeSheetPage request,
			AbstractOverrideBuilder ovrBuilder) throws SQLException,
			OverrideException {
		logger.debug("processAuthorized(...)   start");

		boolean modified = false;
		if (!model.getReadOnly()) {
			int empId = model.getEmployee().getEmpId();
			for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
				WorkSummaryModel summaryModel = (WorkSummaryModel) model
						.getSummaries().get(i);
				WorkSummaryModel summaryRequest = (WorkSummaryModel) request
						.getSummaries().get(i);
				if (!model.getVisibleTeamDateSet().includes(empId,
						summaryModel.getWorkDate())) {
					continue;
				}
				if (summaryModel.getAuthorized() != summaryRequest
						.getAuthorized()
						&& !summaryModel.getLocked()) {
					if (model.getAuthSelf()) {
						setAuthorized(conn, model, summaryModel, ovrBuilder,
								summaryRequest.getAuthorized());
					}
					modified = true;
				}
			} // end for
		} else {
			for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
				WorkSummaryModel summary = (WorkSummaryModel) model
						.getSummaries().get(i);
				if (summary.getInvalid()) {
					modified = true;
				}
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("modified : " + modified);
			logger.debug("processAuthorized(...)   return");
		}
		return modified;
	}

	protected void setSubmitted(CSMBiWeeklyTimeSheetPage model,
			AbstractOverrideBuilder ovrBuilder, boolean submitted)
			throws SQLException {
		logger.debug("setSubmitted(...)   start");

		// Date startDate = model.getWeekStartDate();
		int empId = model.getEmployee().getEmpId();
		Iterator summaries = model.getSummaries().iterator();

		while (summaries.hasNext()) {
			WorkSummaryModel summary = (WorkSummaryModel) summaries.next();
			if (!model.getVisibleTeamDateSet().includes(empId,
					summary.getWorkDate())) {
				continue;
			}
			AbstractOverrideOperation ins = getInsertOverride();
			ins.setWbuNameBoth(model.getWbuName(), model.getWbuNameActual());
			ins.setEmpId(empId);
			ins.setStartDate(summary.getWorkDate());
			ins.setEndDate(summary.getWorkDate());
			ins.setOvrType(300);
			ins.setOvrComment("");

			String ovrValue = "\"WRKS_SUBMITTED=" + (submitted ? "Y\"" : "N\"");
			ins.setOvrNewValue(ovrValue);

			ovrBuilder.add(ins);
			summary.setInvalid(true);
		}

		logger.debug("setSubmitted(...)   return");
	}

	protected void setAuthorized(DBConnection conn,
			CSMBiWeeklyTimeSheetPage model, WorkSummaryModel summary,
			AbstractOverrideBuilder ovrBuilder, boolean authorized)
			throws SQLException, OverrideException {
		logger.debug("setAuthorized(...)   start");

		int empId = model.getEmployee().getEmpId();

		if (AuthorizationRightsHelper.getUseWorkDetailApproval()) {
			List wrkdModels = summary.getWorkDetailModels();
			for (int i = 0; i < wrkdModels.size(); i++) {
				WorkDetailModel wdm = (WorkDetailModel) wrkdModels.get(i);
				if (wdm.getWrkdAuth() == null || "N".equals(wdm.getWrkdAuth())
						&& authorized && summary.getCanAuthorize()
						|| "Y".equals(wdm.getWrkdAuth()) && !authorized
						&& summary.getCanUnauthorize()) {
					InsertWorkDetailOverride ins = new InsertWorkDetailOverride(
							conn);
					ins.setWbuNameBoth(model.getWbuName(), model
							.getWbuNameActual());
					ins.setEmpId(empId);
					ins.setStartDate(summary.getWorkDate());
					ins.setEndDate(summary.getWorkDate());
					ins.setStartTime(wdm.getWrkdStartTime());
					ins.setEndTime(wdm.getWrkdEndTime());
					ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
					if (authorized) {
						ins.setWrkdAuth("Y");
					} else {
						ins.setWrkdAuth("N");
					}
					ins.setWrkdAuthBy(model.getWbuName());
					ins.setWrkdAuthDate(new Date(System.currentTimeMillis()));
					ovrBuilder.add(ins);
				}
			}
		} else {
			AbstractOverrideOperation ins = getInsertOverride();
			ins.setWbuNameBoth(model.getWbuName(), model.getWbuNameActual());
			ins.setEmpId(empId);
			ins.setStartDate(summary.getWorkDate());
			ins.setEndDate(summary.getWorkDate());
			ins.setOvrType(300);
			ins.setOvrComment("");

			String ovrValue = "\"WRKS_AUTHORIZED="
					+ (authorized ? "Y\"" : "N\"");
			ins.setOvrNewValue(ovrValue);
			ovrBuilder.add(ins);
		}
		summary.setInvalid(true);
		// WBLogger.debug("com.workbrain.app.jsp.action.timesheetAbstractSaveTimeSheetAction.class",
		// "SaveTimeSheetAction.setAuthorized() added override.");
		logger.debug("setAuthorized(...)   return");
	}

	protected Date combineDateAndTime(Date date, Date time) {
		logger.debug("combineDateAndTime(...)   start");

		Calendar c = DateHelper.getCalendar();
		c.setTime(date);
		Calendar timeC = DateHelper.getCalendar();
		timeC.setTime(time);

		c.set(Calendar.HOUR, timeC.get(Calendar.HOUR_OF_DAY));
		c.set(Calendar.MINUTE, timeC.get(Calendar.MINUTE));
		c.set(Calendar.SECOND, timeC.get(Calendar.SECOND));
		c.set(Calendar.MILLISECOND, timeC.get(Calendar.MILLISECOND));
		logger.debug("combineDateAndTime(...)   return");
		return c.getTime();
	}

	protected void processScheduleOverrides(DBConnection conn,
			SchedulePage model, CSMBiWeeklyTimeSheetPage wtsRequest,
			CSMBiWeeklyTimeSheetPage parent, AbstractOverrideBuilder ovrBuilder)
			throws Exception {
		logger.debug("processScheduleOverrides(...)   start");

		Iterator schOvrToDel = wtsRequest.getSchedule().getSchOvrIdsToDelete()
				.iterator();
		while (schOvrToDel.hasNext()) {
			AbstractOverrideOperation del = getDeleteOverride();
			del.setOverrideId(((Integer) schOvrToDel.next()).intValue());
			ovrBuilder.add(del);
		}

		// SchedulePage request = wtsRequest.getSchedule();
		int empId = parent.getEmployee().getEmpId();

		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			WorkSummaryModel summary = (WorkSummaryModel) parent.getSummaries()
					.get(i);
			DayScheduleModel oldSchedule = (DayScheduleModel) model
					.getScheduleDays().get(i);
			// DayScheduleModel newSchedule =
			// (DayScheduleModel)wtsRequest.getSchedule().getScheduleDays().get(i);
			if (parent.getVisibleTeamDateSet().includes(empId,
					summary.getWorkDate())) {
				processScheduleOverride(conn, summary, oldSchedule, wtsRequest,
						empId, parent.getWbuName(), parent.getWbuNameActual(),
						ovrBuilder, i, parent.getShiftIndex());
			}
		}
		logger.debug("processScheduleOverrides(...)   return");
	}

	protected void processScheduleOverride(DBConnection conn,
			WorkSummaryModel summary, DayScheduleModel oldSchedule,
			CSMBiWeeklyTimeSheetPage wtsRequest, int empId, String wbuName,
			String wbuNameActual, AbstractOverrideBuilder ovrBuilder, int i,
			int shiftIndex) throws ParseException, SQLException {
		logger.debug("processScheduleOverride(...)   start");

		DayScheduleModel newSchedule = (DayScheduleModel) wtsRequest
				.getSchedule().getScheduleDays().get(i);
		Date date = summary.getWorkDate();
		Date newStartTime = newSchedule.getDefStartTime();
		Date newEndTime = newSchedule.getEndTime();
		if (!(((java.util.Date) DateHelper.truncateToDays(newStartTime))
				.compareTo(newStartTime) == 0 && newEndTime
				.equals(newStartTime))) {
			if (newStartTime.compareTo(oldSchedule.getDefStartTime()) != 0
					|| newEndTime.compareTo(oldSchedule.getEndTime()) != 0) {
				if (newStartTime.compareTo(newEndTime) > 0) {
					newEndTime = DateHelper.addDays(newEndTime, 1);
				}
				addScheduleDelOvr(conn, empId, date, ovrBuilder, wbuName,
						wbuNameActual);
				addScheduleInsOvr(empId, date, wbuName, wbuNameActual,
						newStartTime, newEndTime, shiftIndex, ovrBuilder);
				summary.setInvalid(true);
			}
			// } else {
			// OFF override
		}
		logger.debug("processScheduleOverride(...)   return");
	}

	protected void addScheduleInsOvr(int empId, Date date, String wbuName,
			String wbuNameActual, Date startTime, Date endTime, int shiftIndex,
			AbstractOverrideBuilder ovrBuilder) {
		/*
		 * logger.debug("addScheduleInsOvr(...) start");
		 *
		 * AbstractOverrideOperation ins = getInsertOverride();
		 * ins.setWbuNameBoth(wbuName, wbuNameActual); ins.setEmpId(empId);
		 * ins.setStartDate(date); ins.setEndDate(date);
		 * ins.setOvrType(SCHEDULE_SCHEDTIMES_TYPE); ins.setOvrComment("");
		 *
		 * OverrideString ovrString = new OverrideString(); synchronized (
		 * DATE_OVR_FORMAT ) { ovrString.set( shiftIndex > 1 ?
		 * "EMPSKD_ACT_START_TIME"+shiftIndex : "EMPSKD_ACT_START_TIME",
		 * DATE_OVR_FORMAT.format(startTime)); ovrString.set( shiftIndex > 1 ?
		 * "EMPSKD_ACT_END_TIME"+shiftIndex : "EMPSKD_ACT_END_TIME",
		 * DATE_OVR_FORMAT.format(endTime)); }
		 * ins.setOvrNewValue(ovrString.toString());
		 *
		 * ovrBuilder.add(ins); logger.debug("addScheduleInsOvr(...) return");
		 */
	}

	protected void deletePriorOverride(int ovrId,
			AbstractOverrideBuilder ovrBuilder, WorkSummaryModel summary) {
		logger.debug("deletePriorOverride(...)   start");
		if (ovrId != -1) {
			AbstractOverrideOperation del = getDeleteOverride();
			del.setOverrideId(ovrId);
			ovrBuilder.add(del);
			summary.setInvalid(true);
			if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
				logger
						.debug("SaveTimeSheetAction.applyOverride() deleted override.");
			}
		}
		logger.debug("deletePriorOverride(...)   return");
	}

	protected boolean applyOverride(String defaultTime,
			CSMBiWeeklyTimeSheetPage parent, int ovrId, String ovrString,
			WorkSummaryModel summary, AbstractOverrideBuilder ovrBuilder,
			int ovrOrder, Date startTime, boolean delete) {
		logger.debug("applyOverride(...)   start");

		deletePriorOverride(ovrId, ovrBuilder, summary);
		if (ovrString.length() > 0 && !delete) {
			AbstractOverrideOperation ins = getInsertOverride();
			ins.setWbuNameBoth(parent.getWbuName(), parent.getWbuNameActual());
			ins.setEmpId(parent.getEmployee().getEmpId());
			ins.setStartDate(summary.getWorkDate());
			ins.setEndDate(summary.getWorkDate());
			if (parent.getShiftIndex() > 1) {
				OverrideString ovrStr = new OverrideString(ovrString.toString());
				ovrStr.set("SHIFT_INDEX", String
						.valueOf(parent.getShiftIndex()));
				ovrString = ovrStr.toString();
			}
			ins.setOvrNewValue(ovrString.toString());

			//CSM CR27: Since CSM wants premiums to be entered in the elapsed time section,
			//the ovrType is no longer just 600. It's 600 for all elapsed time overrides, and
			//200 for all premiums.
			if(ovrString.indexOf("WRKP_")!=-1)
				ins.setOvrType(PREMIUM_TYPE_START);
			else
				ins.setOvrType(TIMESHEET_TYPE_START);

			ins.setOvrComment("");
			ins.setOvrOrder(ovrOrder);
			if (ovrOrder == 0) {
				if(startTime==null){
					Calendar cal = Calendar.getInstance();
					cal.setTime(summary.getWorkDate());

					int hour = 0;
					int min = 0;

					String dayStartTime = ((CSMEmployeeModel)parent.getEmployee()).getDayStartTime();

					if(!StringHelper.isEmpty(dayStartTime)){
						int separator = dayStartTime.indexOf(":");
						try{
							if (dayStartTime.charAt(0) == '0')
								hour = Integer.parseInt(dayStartTime.substring(1,
										separator));
							else
								hour = Integer.parseInt(dayStartTime.substring(0,
										separator));
						}catch(Exception e){
							logger.error(e);
						}

						try{
							if (dayStartTime.charAt(separator + 1) == '0')
								min = Integer.parseInt(dayStartTime.substring(
										separator + 2));
							else
								min = Integer.parseInt(dayStartTime.substring(
										separator + 1));
						}catch(Exception e){
							logger.error(e);
						}
					}

					cal.set(Calendar.HOUR, hour);
					cal.set(Calendar.MINUTE, min);
					ins.setStartTime(cal.getTime());

				}else
					ins.setStartTime(startTime);
			}

			ovrBuilder.add(ins);
			summary.setInvalid(true);
			// WBLogger.debug("com.workbrain.app.jsp.action.timesheetAbstractSaveTimeSheetAction.class",
			// "SaveTimeSheetAction.applyOverride() added override," +
			// ins.getOvrNewValue() + " on " + summary.getWorkDate());
			return true;
		}

		logger.debug("applyOverride(...)   return");
		return false;
	}

	protected void addAuthorizedOverride(String wbuName, String wbuNameActual,
			int empId, Date date, boolean authorize,
			AbstractOverrideBuilder ovrBuilder) {
		logger.debug("addAuthorizedOverride(...)   start");
		AbstractOverrideOperation ins = getInsertOverride();
		ins.setWbuNameBoth(wbuName, wbuNameActual);
		ins.setEmpId(empId);
		ins.setStartDate(date);
		ins.setEndDate(date);
		ins.setOvrType(300);
		ins.setOvrComment("");

		String ovrValue = "\"WRKS_AUTHORIZED=";
		if (authorize) {
			ovrValue += "Y\"";
		} else {
			ovrValue += "N\"";
		}
		ins.setOvrNewValue(ovrValue);

		ovrBuilder.add(ins);
		logger.debug("addAuthorizedOverride(...)   return");
	}

	public void processComments(DBConnection conn, List oldSummaries,
			List newSummaries, Date weekStartDate, String wbuName,
			String wbuNameActual, int empId,
			EmployeeIdStartEndDateSet teamVisibleDateSet,
			AbstractOverrideBuilder ovrBuilder) throws Exception {
		logger.debug("processComments(...)   start");

		Date date = weekStartDate;
		for (int i = 0; i < newSummaries.size(); i++) {
			if (teamVisibleDateSet.includes(empId, date)) {
				String oldComment = ((WorkSummaryModel) oldSummaries.get(i))
						.getComments();
				String newComment = ((WorkSummaryModel) newSummaries.get(i))
						.getComments();
				if (!newComment.equals(oldComment)) {
					AbstractOverrideOperation ins = getInsertOverride();
					ins.setWbuNameBoth(wbuName, wbuNameActual);
					ins.setEmpId(empId);
					ins.setStartDate(date);
					ins.setEndDate(date);
					// IC.11568 - Must escape single quote for DB.
					ins.setOvrNewValue("\"WRKS_COMMENTS=" + newComment + "\"");
					ins.setOvrType(300);
					ins.setOvrComment("");

					ovrBuilder.add(ins);
					if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
						logger
								.debug("SaveTimeSheetAction.processComments() added override.");
					}
				}
			}
			date = DateHelper.addDays(date, 1);
		}
		logger.debug("processComments(...)   return");
	}

	public void processFlags(DBConnection conn, List oldSummaries,
			List newSummaries, Date weekStartDate, String wbuName,
			String wbuNameActual, int empId,
			EmployeeIdStartEndDateSet teamVisibleDateSet,
			AbstractOverrideBuilder ovrBuilder) throws Exception {
		logger.debug("processFlags(...)   start");

		Date date = weekStartDate;
		for (int i = 0; i < newSummaries.size(); i++) {
			if (teamVisibleDateSet.includes(empId, date)) {
				WorkSummaryModel oldSummary = (WorkSummaryModel) oldSummaries
						.get(i);
				WorkSummaryModel newSummary = (WorkSummaryModel) newSummaries
						.get(i);
				List oldUdfs = oldSummary.getUdfs();
				List newUdfs = newSummary.getUdfs();
				for (int j = 0; j < newUdfs.size(); j++) {
					String oldUdf = (String) oldUdfs.get(j);
					String newUdf = (String) newUdfs.get(j);
					if (!newUdf.equals(oldUdf)) {
						AbstractOverrideOperation ins = getInsertOverride();
						ins.setWbuNameBoth(wbuName, wbuNameActual);
						ins.setEmpId(empId);
						ins.setStartDate(date);
						ins.setEndDate(date);
						ins.setOvrNewValue("\"WRKS_UDF" + (j + 1) + "="
								+ newUdf + "\"");
						ins.setOvrType(300);
						ins.setOvrComment("");

						ovrBuilder.add(ins);
						oldSummary.setInvalid(true);
						if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
							logger
									.debug("SaveTimeSheetAction.processFlags() added override.");
						}
					}
				}
				List oldFlags = oldSummary.getFlags();
				List newFlags = newSummary.getFlags();
				for (int j = 0; j < newFlags.size(); j++) {
					String oldFlag = (String) oldFlags.get(j);
					String newFlag = (String) newFlags.get(j);
					if (!newFlag.equals(oldFlag)) {
						insertFlagOvr(wbuName, wbuNameActual, empId, date,
								"\"WRKS_FLAG" + (j + 1) + "=" + newFlag + "\"",
								ovrBuilder);
						oldSummary.setInvalid(true);
						if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
							logger
									.debug("SaveTimeSheetAction.processFlags() added override.");
						}
					}
				}
				String oldFlag = oldSummary.getFlagBrk();
				String newFlag = newSummary.getFlagBrk();
				if (!newFlag.equals(oldFlag)) {
					insertFlagOvr(wbuName, wbuNameActual, empId, date,
							"\"WRKS_FLAG_BRK="
									+ ("Y".equalsIgnoreCase(newFlag) ? "Y"
											: "N") + "\"", ovrBuilder);
					oldSummary.setInvalid(true);
					if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
						logger
								.debug("SaveTimeSheetAction.processFlags() added override.");
					}
				}
				oldFlag = oldSummary.getFlagRecall();
				newFlag = newSummary.getFlagRecall();
				if (!newFlag.equals(oldFlag)) {
					insertFlagOvr(wbuName, wbuNameActual, empId, date,
							"\"WRKS_FLAG_RECALL="
									+ ("Y".equalsIgnoreCase(newFlag) ? "Y"
											: "N") + "\"", ovrBuilder);
					oldSummary.setInvalid(true);
					if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
						logger
								.debug("SaveTimeSheetAction.processFlags() added override.");
					}
				}
			} // if visible
			date = DateHelper.addDays(date, 1);
		}
		logger.debug("processFlags(...)   return");
	}

	protected void insertFlagOvr(String wbuName, String wbuNameActual,
			int empId, Date date, String newValue,
			AbstractOverrideBuilder ovrBuilder) {
		logger.debug("insertFlagOvr(...)   start");

		AbstractOverrideOperation ins = getInsertOverride();
		ins.setWbuNameBoth(wbuName, wbuNameActual);
		ins.setEmpId(empId);
		ins.setStartDate(date);
		ins.setEndDate(date);
		ins.setOvrNewValue(newValue);
		ins.setOvrType(300);
		ins.setOvrComment("");

		ovrBuilder.add(ins);
		logger.debug("insertFlagOvr(...)   start");
	}

	protected abstract void addScheduleDelOvr(DBConnection conn, int empId,
			Date date, AbstractOverrideBuilder ovrBuilder, String wbuName,
			String wbuNameActual) throws SQLException;
}
