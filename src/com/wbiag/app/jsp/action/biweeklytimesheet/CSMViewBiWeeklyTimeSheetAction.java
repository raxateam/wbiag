package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.ServletRequest;

import com.workbrain.app.jsp.ActionContext;
import com.workbrain.app.jsp.action.timesheet.AddDefaultTimeSheetAction;
import com.workbrain.app.jsp.action.timesheet.DayOverrideModel;
import com.workbrain.app.jsp.action.timesheet.DayScheduleModel;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimeLine;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimeModel;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimePage;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimeView;
import com.workbrain.app.jsp.action.timesheet.ExpandComponentAction;
import com.workbrain.app.jsp.action.timesheet.LTAPage;
import com.workbrain.app.jsp.action.timesheet.OverridePage;
import com.workbrain.app.jsp.action.timesheet.SchedulePage;
import com.workbrain.app.jsp.action.timesheet.StartStopTimeView;
import com.workbrain.app.jsp.action.timesheet.SummaryPage;
import com.workbrain.app.jsp.action.timesheet.ViewTimeSheetAction;
import com.workbrain.app.jsp.action.timesheet.WTShelper;
import com.workbrain.app.jsp.action.timesheet.WeeklyTimeSheetPage;
import com.workbrain.app.jsp.action.timesheet.WorkSummaryModel;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmployeeAccess;
import com.workbrain.app.ta.db.EmployeeScheduleAccess;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.ta.db.WorkbrainGroupAccess;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.model.EmployeeIdStartEndDateSet;
import com.workbrain.app.ta.model.EmployeeScheduleData;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.OverrideList;
import com.workbrain.app.ta.model.TimeCodeData;
import com.workbrain.app.ta.model.WorkbrainGroupData;
import com.workbrain.app.ta.ruleengine.CreateDefaultRecords;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.security.SecurityService;
import com.workbrain.server.WorkbrainParametersRetriever;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.tool.overrides.OverrideException;
import com.workbrain.tool.security.SecurityException;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.util.StringHelper;

public class CSMViewBiWeeklyTimeSheetAction extends ViewTimeSheetAction
		implements CSMBiWeeklyTimeSheetConstants {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger
			.getLogger(CSMViewBiWeeklyTimeSheetAction.class);

	protected class CSMWorkDetailLine extends WorkDetailLine {

		String hType;

		String tCode;

		int minutes;

		Date workDate;

	}

	public String process(DBConnection conn, ActionContext context,
			Object requestObj) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("process(...)   start");
		}

		boolean loadDefaultLaborsAnyway = false;
		if (requestObj instanceof ServletRequest) {
			ServletRequest request = (ServletRequest) requestObj;
			loadDefaultLaborsAnyway = request
					.getParameter("LOAD_DEFAULT_LABORS") != null;
			boolean fromExpandComponentAction = request
					.getParameter(ExpandComponentAction.FROM_EXPAND_COMPONENT_ACTION) != null;
			if (fromExpandComponentAction)
				return "/timesheet/weeklyTimeSheet.jsp";
		}

		CSMBiWeeklyTimeSheetPage model = (CSMBiWeeklyTimeSheetPage) context
				.getAttribute("timesheet");
		if (model == null) {
			model = new CSMBiWeeklyTimeSheetPage();
		}

		/**
		 * @todo refactor this to use from the request..
		 */
		model.setCopied(false);
		model.getElapsedTime().setDistributed(false);
		CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
		ElapsedTimeView elapsedTimeView = CSMRequestCache.getElapsedTimeView(conn,
				context, true);

		populateModel(conn, model);
		List summaries = model.getSummaries();
		if (summaries == null) {
			summaries = new ArrayList();
		}
		Date date = model.getWeekStartDate();
		Date endDate = DateHelper.addDays(date, DAYS_ON_TIMESHEET - 1);
		model.setReadOnly(true);
		boolean isHolidays[] = loadHolidays(conn, model.getEmployee()
				.getEmpId(), model.getWeekStartDate());
		List editableDateList = model.getEditableDateSet().getDates(
				model.getEmployee().getEmpId());

		// we need to make sure that all days that do not have work summaries
		// allocated
		// will be calculated through the CDR process, and all other days will
		// be skipped.
		// Also, any day that began without a work summary, which has a default
		// work summary made during
		// the calculation process, will be re-calculated - For example, if we
		// are processing a new empty week,
		// and in the middle of the week a rule fires which needs access to the
		// full weeks' data, then default or
		// temporary records will be made for the remainder of days - and when
		// those days are in review, we will
		// fully calculate them - we will not skip processing those days that
		// had temporary summaries made.
		CreateDefaultRecords cdr = new CreateDefaultRecords(conn,
				new int[] { model.getEmployee().getEmpId() }, date, endDate);
		cdr.execute();

		// create the work summary model structure based on the work summary
		// data object
		int numOfReadOnlyDays = 0;

		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			WorkSummaryModel sum = null;
			if (summaries.size() > i) {
				sum = (WorkSummaryModel) summaries.get(i);
			}

			if (sum == null || sum.getInvalid()) {
				sum = createSummaryModel(conn, codeMapper, model.getEmployee()
						.getEmpId(), date);
			}

			if (summaries.size() > i) {
				summaries.set(i, sum);
			} else {
				summaries.add(sum);
			}

			if ((date.compareTo(WTShelper.getHandsOffDateForEmp(model
					.getEmployee(), date)) >= 0)
					&& (context.getAttribute("WBG_SESSION").equals("Y") || date
							.compareTo(WTShelper.getSupervisorDateForEmp(model
									.getEmployee(), date)) > 0)
					&& (editableDateList.contains(date))) {
				sum.setReadOnly(false);
				model.setReadOnly(false);
			} else {
				sum.setReadOnly(true);
				if (context.getAttribute("WBG_SESSION").equals("N")
						&& date.compareTo(WTShelper.getSupervisorDateForEmp(
								model.getEmployee(), date)) <= 0) {
					model.setBeyondSupervisorDate(true);
				} else if (date.compareTo(WTShelper.getHandsOffDateForEmp(model
						.getEmployee(), date)) <= 0) {
					model.setBeyondHandsOffDate(true);
				}
			}

			sum.setIsHoliday(isHolidays[i]);

			try{
				//timesheet viewer's security group
				int viewerWbgId = Integer.parseInt(context.getUserWbgId());
				WorkbrainGroupAccess wbgAccess = new WorkbrainGroupAccess(conn);
				WorkbrainGroupData wbgData = wbgAccess.load(viewerWbgId);

				List wrks_flags = sum.getFlags();
				String wrks_flag1 = (String)wrks_flags.get(0);	//supervisor approval flag

				//CR-Sept12: If it's already submitted OR approved,
				//the timesheet should be read-only to all ppl security group w/ wbgFlag1 = 'Y'
				if(sum.getSubmitted()||(!StringHelper.isEmpty(wrks_flag1)&&wrks_flag1.equals("Y"))){
					if(!StringHelper.isEmpty(wbgData.getWbgFlag1()) && wbgData.getWbgFlag1().equals("Y"))
						sum.setReadOnly(true);
				}

				//CR-Sept22: If it's already approved
				//the timesheet should be read-only to all ppl with security group w/ wbFlag2 = 'Y'
				if(!StringHelper.isEmpty(wbgData.getWbgFlag2()) && wbgData.getWbgFlag2().equals("Y")
			                && !StringHelper.isEmpty(wrks_flag1)&&wrks_flag1.equals("Y"))
					sum.setReadOnly(true);

			}catch(NumberFormatException e){
					logger.error(e);
			}

			//CSM: count how many days are read-only
			if(sum.getReadOnly())
				numOfReadOnlyDays++;

			date = DateHelper.addDays(date, 1);
		}
		//CSM: If all the days on the timesheet are read-only, then set the entire model to read only
		if(numOfReadOnlyDays==DAYS_ON_TIMESHEET)
			model.setReadOnly(true);

		model.setSummaries(summaries);
		createDailyLocks(conn, model);

		if (model.getLocked()) {
			model.setReadOnly(true);
			for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
				WorkSummaryModel sum = (WorkSummaryModel) summaries.get(i);
				sum.setReadOnly(true);
			}
		}

		// if (!recordExist(summaries)) {
		// if (model.getNextEmpId() != -1) {
		// removeEmpValueAndLabel(model);
		// model.setEmployee( CSMNewEmployeeAction.createEmployeeModel( conn,
		// context, model.getWbuName(),
		// model.getNextEmpId(), model.getWeekStartDate()) );
		// return
		// CSMWTShelper.getActionURL(AbstractActionFactory.VIEW_TIMESHEET_ACTION);
		// }
		// return "/timesheet/noRecordsFound.jsp";
		// }
		model.setSchedule(createSchedulePage(conn, codeMapper, model));

		// If the user is loading a preset, load it here.
		Object o = context
				.getAttribute(AddDefaultTimeSheetAction.PRESET_ET_PAGE);
		ElapsedTimePage presetPage = (o != null) ? (ElapsedTimePage) o : null;
		context.setAttribute(AddDefaultTimeSheetAction.PRESET_ET_PAGE, null);

		model.setElapsedTime(createElapsedTimePage(conn, codeMapper,
				elapsedTimeView, model, loadDefaultLaborsAnyway, presetPage));
		List ltaOvrs = loadLTAOverrides(conn, model.getEmployee().getEmpId(),
				model.getWeekStartDate(), model.getWeekEndDate());
		if (CSMWTSPresentationHelper.instance().showNonWorkTimePage(model)) {
			model.setNonWorkTimePage(createNonWorkTimePage(conn, codeMapper,
					ltaOvrs, model.getWeekStartDate()));
			context.setAttribute(
					ExpandComponentAction.ORIGINAL_NON_WORK_TIME_PAGE, model
							.getNonWorkTimePage());
		} else {
			model.setNonWorkTimePage(null);
		}
		if (CSMWTSPresentationHelper.instance().showFullDayAbsencePage(model)) {
			model.setFullDayAbsencePage(createFullDayAbsencePage(conn,
					codeMapper, model.getWeekStartDate(), elapsedTimeView,
					ltaOvrs));
			processFullDayAbsences(model);
			context.setAttribute(
					ExpandComponentAction.ORIGINAL_FULL_DAY_ABSENCE_PAGE, model
							.getFullDayAbsencePage());
		} else {
			model.setFullDayAbsencePage(null);
		}
		model.setOverride(createOverridePage(conn, model, context.getAttribute(
				"WBG_SESSION").equals("Y")));
		model.setSummary(createSummaryPage(conn, model));
		setShowComponents(model);
		Iterator i = model.getSummaries().iterator();
		while (i.hasNext()) {
			WorkSummaryModel summary = (WorkSummaryModel) i.next();
			summary.setInvalid(false);
		}

		model
				.setIsLockDownPriv(context.getAttribute("WBG_SESSION").equals(
						"Y"));
		model.setAuthorized(isAuthorized(model.getEmployee().getEmpId(), model
				.getVisibleTeamDateSet(), summaries));
		model.setSubmitted(isSubmitted(model.getEmployee().getEmpId(), model
				.getVisibleTeamDateSet(), summaries));

		// TT 20595 USER_CAN_AUTHORIZE_SELF does not work for WTS
		model.setAuthSelf(true);
		if ((new Integer(context.getUserEmpId())).intValue() == model
				.getEmployee().getEmpId()
				|| SecurityService.getCurrentUser().getEmpIdActual() == model
						.getEmployee().getEmpId()) {
			model.setAuthSelf(WorkbrainParametersRetriever.getBoolean(
					"USER_CAN_AUTHORIZE_SELF", false));
		}

		// end TT 20595
		context.setAttribute("timesheet", model);
		updateLoadedTimesheet(context, model);

		initializeWTSForceTimecodeParameter(conn, context);

		model.setNumberOfShifts(Math.max(getNumberOfShifts(), 1));

		if (logger.isDebugEnabled()) {
			logger.debug("process(...)   return");
		}
		return "/timesheet/biweeklyTimeSheet.jsp";
	}

	private void updateLoadedTimesheet(ActionContext context,
			CSMBiWeeklyTimeSheetPage model) {
		CSMBiWeeklyTimeSheetPage loaded = (CSMBiWeeklyTimeSheetPage) context
				.getAttribute("timesheet.loaded");
		int shiftIndex = model.getShiftIndex();
		if (loaded != null && !loaded.isShiftLoaded(shiftIndex)) {
			try {
				loaded.setSchedule(shiftIndex, (SchedulePage) model
						.getSchedule().clone());
				loaded.setSchedule(loaded.getSchedule(shiftIndex));

				loaded.setElapsedTime(shiftIndex, (ElapsedTimePage) model
						.getElapsedTime().clone());
				loaded.setElapsedTime(loaded.getElapsedTime(shiftIndex));
				loaded.setShiftIndex(shiftIndex);
				loaded.setShiftLoaded(shiftIndex, true);
				context.setAttribute("timesheet.loaded", loaded);
			} catch (CloneNotSupportedException e) {
				throw new NestedRuntimeException();
			}
		}
	}

	private void removeEmpValueAndLabel(CSMBiWeeklyTimeSheetPage model) {
		int empId = model.getEmployee().getEmpId();
		String employeeValues = "";
		String employeeLabels = "";
		StringTokenizer valueToker = new StringTokenizer(model
				.getEmployeeValues(), ",");
		StringTokenizer labelToker = new StringTokenizer(model
				.getEmployeeLabels(), ",");

		while (valueToker.hasMoreElements()) {
			String value = valueToker.nextToken();
			String label = labelToker.nextToken();

			if (Integer.parseInt(value) != empId) {
				if (employeeValues.length() != 0) {
					employeeValues += ",";
					employeeLabels += ",";
				}
				employeeValues += value;
				employeeLabels += label;
			}
		}
		model.setEmployeeValues(employeeValues);
		model.setEmployeeLabels(employeeLabels);
	}

	private void processFullDayAbsences(CSMBiWeeklyTimeSheetPage model) {
		List summaries = model.getSummaries();
		LTAPage fdap = model.getFullDayAbsencePage();
		if (fdap == null)
			return;
		List wsmLines = fdap.getLines();

		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			WorkSummaryModel wsm = (WorkSummaryModel) summaries.get(i);
			for (int j = 0; j < wsmLines.size(); j++) {
				LTAPage.LTALine ltaLine = (LTAPage.LTALine) wsmLines.get(j);
				LTAPage.Override ltaOverride = (LTAPage.Override) ltaLine
						.getDays().get(i);
				if (ltaOverride.getChecked()) {
					wsm.setAbsent(true);
				}
			}
		}
	}

	private void setShowComponents(CSMBiWeeklyTimeSheetPage model) {
		String showComponent = WTShelper.getWTSExtraParamValue(model
				.getExtraWtsParameters(), "WTS_SHOW_COMPONENT");
		if (showComponent == null) {
			showComponent = WorkbrainParametersRetriever.getString(
					"WTS_SHOW_COMPONENT", "");
		}
		if (showComponent.indexOf("SCHEDULE") > -1) {
			model.setShowSchedule(true);
		}
		if (showComponent.indexOf("SUMMARY") > -1) {
			model.setShowSummary(true);
		}
	}

	protected void populateModel(DBConnection conn,
			CSMBiWeeklyTimeSheetPage model) throws SQLException,
			SecurityException {
		if (logger.isDebugEnabled()) {
			logger.debug("populateModel(...)   start");
		}

		// CSM: get oldest unsubmitted t/s here
		// Date weekStartDate =
		// DateHelper.truncateToDays(model.getWeekStartDate());
		Date weekStartDate = model.getWeekStartDate();

		int empId = model.getEmployee().getEmpId();
		model.setPrevEmpId(-1);
		model.setNextEmpId(-1);
		List empList = getList(model.getEmployeeValues());
		int index = empList.indexOf(String.valueOf(empId));
		if (index > 0) {
			String prev = (String) empList.get(index - 1);
			model.setPrevEmpId(Integer.parseInt(prev));
		}
		if (index + 1 < empList.size()) {
			String next = (String) empList.get(index + 1);
			model.setNextEmpId(Integer.parseInt(next));
		}
		model.setSupervisor(RuleHelper.isSupervisor(model.getWbuName(), model
				.getEmployee().getEmpId(), model.getWeekStartDate(), conn));

		model.setPrevWeek(DateHelper.addDays(weekStartDate, -1
				* DAYS_ON_TIMESHEET));
		model.setNextWeek(DateHelper.addDays(weekStartDate, DAYS_ON_TIMESHEET));
		if (logger.isDebugEnabled()) {
			logger.debug("populateModel(...)   return");
		}
	}

	protected SchedulePage createSchedulePage(DBConnection conn,
			CodeMapper codeMapper, CSMBiWeeklyTimeSheetPage parent)
			throws SQLException, ParseException, OverrideException {
		if (logger.isDebugEnabled()) {
			logger.debug("createSchedulePage(...)   start");
		}

		SchedulePage model = new SchedulePage();

		EmployeeAccess employeeAccess = new EmployeeAccess(conn, codeMapper);
		EmployeeScheduleAccess scheduleAccess = new EmployeeScheduleAccess(
				conn, codeMapper);

		int minutes = 0;
		List schedules = new ArrayList();

		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			WorkSummaryModel summary = (WorkSummaryModel) parent.getSummaries()
					.get(i);
			DayScheduleModel day = null;
			if (parent.getSchedule() == null || summary.getInvalid()) {
				int empId = parent.getEmployee().getEmpId();
				EmployeeData employee = employeeAccess.load(empId, summary
						.getWorkDate());
				EmployeeScheduleData schedule = scheduleAccess.load(employee,
						summary.getWorkDate());
				// load start/stop, break start/stop times from the employee
				// schedule record
				// this might be overriden by elapsed time override later.
				day = StartStopTimeView.loadSchedulePage(conn, parent,
						schedule, i);
			} else {
				day = (DayScheduleModel) parent.getSchedule().getScheduleDays()
						.get(i);
			}
			if (day.getActStartTime() != null && day.getEndTime() != null) {
				minutes += DateHelper.getMinutesBetween(day.getEndTime(), day
						.getActStartTime());
			}
			schedules.add(day);
		}
		model.setSchedWeekHours(minutes);
		model.setScheduleDays(schedules);

		if (logger.isDebugEnabled()) {
			logger.debug("createSchedulePage(...)   return");
		}
		return model;
	}

	protected ElapsedTimePage createElapsedTimePage(DBConnection conn,
			CodeMapper codeMapper, ElapsedTimeView elapsedTimeView,
			CSMBiWeeklyTimeSheetPage parent, boolean loadDefaultLaborsAnyway,
			ElapsedTimePage presetPage) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("CSMElapsedTimePage(...)   start");
		}
		int empId = parent.getEmployee().getEmpId();

		if (loadDefaultLaborsAnyway) {
			OverrideAccess oa = new OverrideAccess(conn);
			java.util.Date startDate = parent.getWeekStartDate();
			java.util.Date endDate = DateHelper.addDays(startDate,
					DAYS_ON_TIMESHEET);
			oa.cancelByRangeAndType(parent.getEmployee().getEmpId(), startDate,
					endDate, OverrideData.TIMESHEET_TYPE_START,
					OverrideData.TIMESHEET_TYPE_END, DateHelper.DATE_1900);
		}

		ElapsedTimePage newElapsedTimePage = null;
		List dayTotals = null;
		List lines = null;

		if (presetPage == null) {
			newElapsedTimePage = new CSMElapsedTimePage();
			dayTotals = new ArrayList();
			for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
				dayTotals.add(new Integer(0));
			}
			newElapsedTimePage.setTotal(0);
			lines = new ArrayList();
			newElapsedTimePage.setElapsedTimeLines(lines);
		} else {
			newElapsedTimePage = presetPage;
			lines = presetPage.getElapsedTimeLines();
			dayTotals = presetPage.getDayTotals();
		}

		HashMap labors = new HashMap();
		Date date = parent.getWeekStartDate();
		EmployeeIdStartEndDateSet visibleSet = parent.getVisibleTeamDateSet();
		for (int day = 0; day < DAYS_ON_TIMESHEET; day++) {
			if (visibleSet.includes(empId, date)) {
				loadElapsedTimesForADay(elapsedTimeView, day, parent,
						newElapsedTimePage, labors, lines, dayTotals,
						loadDefaultLaborsAnyway);
			}
			date = DateHelper.addDays(date, 1);
		}

		//CSM CR27:
		//CSMElapsedTimeLines now contains both ElapsedTime overrides and work premium overrides
		//We need to mark the Premium lines with a flag in the CSMElapsedTimeLine object.
		CSMWTShelper.markPremiumLines(conn,lines);

		newElapsedTimePage.setElapsedTimeLines(lines);
		List blankLines = new ArrayList();
		Iterator itr = lines.iterator();
		while (itr.hasNext()) {
			ElapsedTimeLine line = (ElapsedTimeLine) itr.next();
			if (!line.hasData()) {
				blankLines.add(line);
			}
		}

		newElapsedTimePage.setElapsedTimeLines(lines);
		lines.removeAll(blankLines);
		newElapsedTimePage.setElapsedTimeLines(lines);

		for (int i = 0; i < 3; i++) {
			lines.add(createBlankLine(elapsedTimeView));
		}

		newElapsedTimePage.setFieldNames(elapsedTimeView.getFieldNames());
		newElapsedTimePage.setElapsedTimeLines(lines);
		newElapsedTimePage.setDayTotals(dayTotals);

		if (logger.isDebugEnabled()) {
			logger.debug("createElapsedTimePage(...)   return");
		}
		return newElapsedTimePage;
	}

	private void dropLTAOverrideToBucket(LTAPage page, String type,
			OverrideData ovr, Date weekStart) {

		Date startDate = DateHelper.truncateToDays(ovr.getOvrStartDate());
		Date endDate = DateHelper.truncateToDays(ovr.getOvrEndDate());

		// Only use LTAPage.LTALines of the same type as the override (stored in
		// linesOfType)
		List lines = page.getLines();
		List linesOfType = new ArrayList();
		for (int i = 0; i < lines.size(); i++) {
			LTAPage.LTALine line = (LTAPage.LTALine) lines.get(i);
			if (type.equalsIgnoreCase(line.getType())) {
				linesOfType.add(line);
			}
		}

		for (int day = 0; day < DAYS_ON_TIMESHEET; day++) {
			LTAPage.Override foundOvr = null;
			LTAPage.LTALine line = null;
			Iterator itr = linesOfType.iterator();

			// Skip this date if the override doesn't apply to this date.
			if (!DateHelper.isBetween(DateHelper.addDays(weekStart, day),
					startDate, endDate))
				continue;

			// Find an unchecked LTAPage.Override from the lines (same type).
			while (itr.hasNext()) {
				line = (LTAPage.LTALine) itr.next();
				LTAPage.Override anOvr = (LTAPage.Override) line.getDays().get(
						day);
				if (!anOvr.getChecked()) {
					foundOvr = anOvr;
				}
			}

			// If no unchecked LTAPage.Override is found, then populate a new
			// line with LTAPage.Overrides
			// to apply the override there.
			if (foundOvr == null) {
				line = new LTAPage.LTALine(type);
				page.getLines().add(line);
				linesOfType.add(line);
				for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
					line.getDays().add(new LTAPage.Override());
				}
				foundOvr = (LTAPage.Override) line.getDays().get(day);
			}

			// Apply the override to the (new or found) LTAPage.Override and
			// check the latter Override.
			foundOvr.setIsChecked(true);
			foundOvr.setStart(ovr.getOvrStartTime());
			foundOvr.setStop(ovr.getOvrEndTime());
			foundOvr.setId(ovr.getOvrId());
		}
	}

	protected LTAPage createNonWorkTimePage(DBConnection conn,
			CodeMapper codeMapper, List ltaOvrs, Date weekStartDate)
			throws Exception {
		LTAPage page = new CSMLTAPage();

		Iterator ovrs = ltaOvrs.iterator();
		while (ovrs.hasNext()) {
			OverrideData ovr = (OverrideData) ovrs.next();
			if (ovr.getOvrStartTime() != null && ovr.getOvrEndTime() != null) {
				String timeCode = ovr.getNewOverrideByName(
						OverrideData.LTA_TCODE_OVERRIDE_FIELDNAME).getValue();
				dropLTAOverrideToBucket(page, timeCode, ovr, weekStartDate);
			}
		}

		// get TimeCodeData for each LTALine
		List tcodes = new ArrayList();
		for (int j = 0; j < page.getLines().size(); j++) {
			String type = ((LTAPage.LTALine) page.getLines().get(j)).getType();
			TimeCodeData tcode = codeMapper.getTimeCodeByName(type);
			if (tcode != null)
				tcodes.add(tcode);
		}

		// Filter out LTALines with non-LTA time codes from the non-work time
		// page.
		for (int k = 0; k < tcodes.size(); k++) {
			TimeCodeData tcd = (TimeCodeData) tcodes.get(k);
			if (tcd.getTcodeIsLta().equals("N")) {
				String nonLTAType = tcd.getTcodeName();
				int cursor = 0;
				while (cursor < page.getLines().size()) {
					LTAPage.LTALine ltaLine = (LTAPage.LTALine) page.getLines()
							.get(cursor);
					if (ltaLine.getType().equals(nonLTAType)) {
						page.getLines().remove(cursor);
					} else {
						cursor++;
					}
				}
			}
		}

		for (int i = 0; i < 2; i++) {
			LTAPage.LTALine line = new LTAPage.LTALine();
			for (int j = 0; j < DAYS_ON_TIMESHEET; j++) {
				line.getDays().add(new LTAPage.Override());
			}
			page.getLines().add(line);
		}

		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			int lineMinutes = 0;
			Iterator itr = page.getLines().iterator();
			while (itr.hasNext()) {
				LTAPage.LTALine line = (LTAPage.LTALine) itr.next();
				LTAPage.Override ovr = (LTAPage.Override) line.getDays().get(i);
				if (ovr.getStop() != null && ovr.getStart() != null) {
					int minutes = (int) DateHelper.getMinutesBetween(ovr
							.getStop(), ovr.getStart());
					lineMinutes += minutes;
					line.setTotal(line.getTotal() + minutes);
					page.setTotal(page.getTotal() + minutes);
				}
			}
			page.getHours()[i] = lineMinutes;
		}

		return page;
	}

	protected LTAPage createFullDayAbsencePage(DBConnection conn,
			CodeMapper codeMapper, Date weekStartDate,
			ElapsedTimeView elapsedTimeView, List ltaOvrs) throws Exception {
		LTAPage page = new CSMLTAPage();

		Iterator ltaTimeCodes = elapsedTimeView.getAllLTATimeCodes(conn)
				.iterator();
		while (ltaTimeCodes.hasNext()) {
			TimeCodeData code = (TimeCodeData) ltaTimeCodes.next();
			LTAPage.LTALine line = new LTAPage.LTALine(code.getTcodeName());
			for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
				line.getDays().add(new LTAPage.Override());
			}
			page.getLines().add(line);
		}

		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			Date date = DateHelper.addDays(weekStartDate, i);
			Iterator ovrs = ltaOvrs.iterator();
			while (ovrs.hasNext()) {
				OverrideData ovr = (OverrideData) ovrs.next();
				if (ovr.getOvrStartDate().compareTo(date) == 0
						&& ovr.getOvrStartTime() == null
						&& ovr.getOvrEndTime() == null) {
					String timeCode = ovr.getNewOverrideByName(
							OverrideData.LTA_TCODE_OVERRIDE_FIELDNAME)
							.getValue();
					for (int j = 0; j < page.getLines().size(); j++) {
						LTAPage.LTALine line = (LTAPage.LTALine) page
								.getLines().get(j);
						if (line.getType().equalsIgnoreCase(timeCode)) {
							LTAPage.Override pageOvr = ((LTAPage.Override) line
									.getDays().get(i));
							pageOvr.setId(ovr.getOvrId());
							pageOvr.setIsChecked(true);
						}
					}
				}
			}
		}

		return page;
	}

	private void initializeWTSForceTimecodeParameter(DBConnection conn,
			ActionContext context) throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("initializeWTSForceTimecodeParameter(...)   start");
		}

		String tCodeMissingFlag = "N";
		// String parameterName = "WTS_FORCE_TIMECODE_SUBMIT";
		boolean forceTimecodeSubmitParam = WorkbrainParametersRetriever
				.getBoolean("WTS_FORCE_TIMECODE_SUBMIT", false);
		if (forceTimecodeSubmitParam) {
			tCodeMissingFlag = "Y";
		}

		context.setAttribute("WTS_checkTCodeMissing", tCodeMissingFlag);
		if (logger.isDebugEnabled()) {
			logger.debug("initializeWTSForceTimecodeParameter(...)   return");
		}
	}

	private boolean lockWhenSubmitted() {
		if (logger.isDebugEnabled()) {
			logger.debug("lockWhenSubmitted(...)   start");
		}

		boolean lock = WorkbrainParametersRetriever.getBoolean(
				"WTS_LOCK_WHEN_SUBMITTED", false);
		if (logger.isDebugEnabled()) {
			logger.debug("lock = " + lock);
			logger.debug("lockWhenSubmitted(...)   return");
		}

		return lock;
	}

	private void createDailyLocks(DBConnection conn,
			CSMBiWeeklyTimeSheetPage model) throws SQLException {
		ResultSet rs = null;
		PreparedStatement ps = null;
		int empId = model.getEmployee().getEmpId();
		try {
			Timestamp weekStart = new Timestamp(model.getWeekStartDate()
					.getTime());
			Timestamp weekEnd = new Timestamp(model.getWeekEndDate().getTime());
			ps = conn.prepareStatement(LOAD_LOCKS_SQL);
			ps.setString(1, "" + model.getEmployee().getEmpId());
			ps.setTimestamp(2, weekStart);
			ps.setTimestamp(3, weekEnd);
			ps.setTimestamp(4, weekStart);
			ps.setTimestamp(5, weekStart);
			ps.setTimestamp(6, weekEnd);
			ps.setTimestamp(7, weekEnd);
			ps.setTimestamp(8, weekStart);
			ps.setTimestamp(9, weekEnd);

			rs = ps.executeQuery();
			List summaries = model.getSummaries();
			while (rs.next()) {
				Timestamp tsl_startDate = rs.getTimestamp("TSL_START_DATE");
				Timestamp tsl_endDate = rs.getTimestamp("TSL_END_DATE");
				for (int i = 0; i < summaries.size(); i++) {
					WorkSummaryModel wsm = (WorkSummaryModel) summaries.get(i);
					if (DateHelper.isBetween(wsm.getWorkDate(), tsl_startDate,
							tsl_endDate)) {
						wsm.setLocked(true);
					}
				}
			}
			int lockedDays = 0;
			int visibleDays = 0;
			for (int i = 0; i < summaries.size(); i++) {
				WorkSummaryModel wsm = (WorkSummaryModel) summaries.get(i);
				if (model.getVisibleTeamDateSet().includes(empId,
						wsm.getWorkDate())) {
					visibleDays++;
					if (wsm.getLocked())
						lockedDays++;
				}
			}
			model.setLocked(visibleDays == lockedDays);
		} finally {
			SQLHelper.cleanUp(ps, rs);
		}
	}

	// ~~~~~~~ redeclare with RANGE variable for
	// AbstractViewTimeSheetAction~~~~~~~~~~~~~~~~~~~~~~~~~~~````//
	protected boolean[] loadHolidays(DBConnection conn, int empId,
			Date startDate) throws SQLException {

		boolean[] holidays = new boolean[DAYS_ON_TIMESHEET];
		OverrideAccess ovrAcc = new OverrideAccess(conn);
		Date endDate = DateHelper.addDays(startDate, DAYS_ON_TIMESHEET - 1);
		OverrideList overrides = ovrAcc.loadByRangeAndType(empId, startDate,
				endDate, OverrideData.HOLIDAY_TYPE_START,
				OverrideData.HOLIDAY_TYPE_END);
		for (int i = 0; i < overrides.size(); i++) {
			OverrideData ovr = overrides.getOverrideData(i);
			holidays[DateHelper.getDifferenceInDays(ovr.getOvrStartDate(),
					startDate)] = true;
		}
		return holidays;
	}

	protected int processAnElapsedTime(int day, int detailIndex, Map labors,
			List lines, ElapsedTimeModel elapsedTime, int prevReturn) {
		if (logger.isDebugEnabled()) {
			logger.debug("processAnElapsedTime(...)   start");
		}

		String key = createKey(elapsedTime.getFields());
		ElapsedTimeLine line = findSuitableLine(lines, key, day);
		if (line == null) {
			// ********* Adding a new line ***
			labors.put(key, new Integer(labors.size()));
			List hours = new ArrayList();
			for (int j = 0; j < DAYS_ON_TIMESHEET; j++) {
				hours.add(new Integer(0));
			}
			List ovrIds = new ArrayList();
			for (int j = 0; j < DAYS_ON_TIMESHEET; j++) {
				ovrIds.add(new Integer(-1));
			}
			line = new CSMElapsedTimeLine();
			line.setFields(elapsedTime.getFields());
			line.setHours(hours);
			line.setOvrIds(ovrIds);
			lines.add(line);
		}
		line.getHours().set(
				day,
				new Integer(((Integer) line.getHours().get(day)).intValue()
						+ elapsedTime.getMinutes()));
		line.setLineTotal(line.getLineTotal() + elapsedTime.getMinutes());
		line.getOvrIds().set(day, new Integer(elapsedTime.getOvrId()));
		if (logger.isDebugEnabled()) {
			logger.debug("processAnElapsedTime(...)   return");
		}
		return -1;
	}

	private ElapsedTimeLine findSuitableLine(List lines, String key, int day) {
        if ( logger.isDebugEnabled() ) {
            logger.debug("findSuitableLine(...)   start");
        }

        for (int i = 0, j = lines.size(); i < j; i++) {
            ElapsedTimeLine line = (ElapsedTimeLine) lines.get(i);
            String lineKey = createKey(line.getFields());
            if (key.equals(lineKey)) {
                // *** Found a line with the same key ***
                Integer hrs = (Integer) line.getHours().get(day);
                if (hrs.intValue() == 0) {
                    // *** Hours are empty, so this line is good ***
                    return line;
                }
            }
        }
        // *** Could not find a line, need to add new one ***
        if ( logger.isDebugEnabled() ) {
            logger.debug("findSuitableLine(...)   return");
        }
        return null;
    }

    private String createKey(List fields) {
        String retVal = null;
        StringBuffer buffer = new StringBuffer();
        Iterator itr = fields.iterator();
        while (itr.hasNext()) {
            Object value = itr.next();
            buffer.append(value == null ? "" : value);
        }
        retVal = buffer.toString();
        if ( logger.isDebugEnabled() ) {
            logger.debug("createKey returns : " +  retVal);
        }
        return retVal;
    }

	protected List loadWorkDetailForDays(DBConnection conn, int empId, Date date)
			throws SQLException {
		if (logger.isDebugEnabled()) {
			logger.debug("loadWorkDetailForDays(...)   start");
		}

		List wholeDetails = new ArrayList();
		String sql = "SELECT HTYPE_NAME, TCODE_NAME, WRKD_MINUTES, WRKS_WORK_DATE "
				+ "FROM WORK_DETAIL, HOUR_TYPE, TIME_CODE, WORK_SUMMARY "
				+ "WHERE WORK_SUMMARY.WRKS_ID = WORK_DETAIL.WRKS_ID AND "
				+ "WORK_DETAIL.HTYPE_ID = HOUR_TYPE.HTYPE_ID AND "
				+ "WORK_DETAIL.TCODE_ID = TIME_CODE.TCODE_ID AND "
				+ "TIME_CODE.TCODE_SUMMARIZE = 'Y' AND "
				+ "HOUR_TYPE.HTYPE_SUMMARIZE = 'Y' AND "
				+ "EMP_ID = ? AND "
				+ "WRKS_WORK_DATE BETWEEN ? AND  ? "
				+ " ORDER BY WRKS_WORK_DATE";
		ResultSet rs = null;
		PreparedStatement s = null;
		try {
			s = conn.prepareStatement(sql);
			s.setInt(1, empId);
			s.setTimestamp(2, new Timestamp(date.getTime()));
			s.setTimestamp(3, new Timestamp(DateHelper.addDays(date, CSMBiWeeklyTimeSheetConstants.DAYS_ON_TIMESHEET-1)
					.getTime()));
			rs = s.executeQuery();
			while (rs.next()) {
				CSMWorkDetailLine detail = new CSMWorkDetailLine();
				detail.hType = rs.getString(1);
				detail.tCode = rs.getString(2);
				detail.minutes = rs.getInt(3);
				detail.workDate = rs.getDate(4);
				wholeDetails.add(detail);
			}
		} finally {
			SQLHelper.cleanUp(s, rs);
		}

		List days = new ArrayList();
		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			days.add(new ArrayList());
		}

		/**
		 * @todo optimize this loop
		 */
		Iterator itr = wholeDetails.iterator();
		while (itr.hasNext()) {
			CSMWorkDetailLine detail = (CSMWorkDetailLine) itr.next();
			Date day = date;
			for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
				if (day.compareTo(detail.workDate) == 0) {
					((List) days.get(i)).add(detail);
				}
				day = DateHelper.addDays(day, 1);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("loadWorkDetailForDays(...)   return");
		}
		return days;
	}

	protected SummaryPage createSummaryPage(DBConnection conn,
			CSMBiWeeklyTimeSheetPage parent) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("createSummaryPage(...)   start");
		}
		final CodeMapper cm = CodeMapper.createCodeMapper(conn);
		SummaryPage model = new SummaryPage();
		List dayTotals = new ArrayList();
		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			dayTotals.add(new Integer(0));
		}
		model.setSummaryTotal(0);
		List lines = new ArrayList();
		HashMap labors = new HashMap();

		Date date = parent.getWeekStartDate();
		int empId = parent.getEmployee().getEmpId();
		List days = loadWorkDetailForDays(conn,
				parent.getEmployee().getEmpId(), date);
		for (int day = 0; day < DAYS_ON_TIMESHEET; day++) {
			// Do not show summary information before hire and on or after
			// termination date.
			if ((date.compareTo(parent.getEmployee().getHireDate()) < 0)
					|| (date.compareTo(parent.getEmployee()
							.getTerminationDate()) >= 0)
					|| (!parent.getVisibleTeamDateSet().includes(empId, date))) {
				dayTotals.set(day, new Integer(0));
				date = DateHelper.addDays(date, 1);
				continue;
			}

			List details = (List) days.get(day);

			for (int i = 0; i < details.size(); i++) {
				CSMWorkDetailLine detail = (CSMWorkDetailLine) details.get(i);
				int minutes = processWorkDetail(detail, labors, lines, day);
				dayTotals.set(day, new Integer(((Integer) dayTotals.get(day))
						.intValue()
						+ minutes));
				model.setSummaryTotal(model.getSummaryTotal() + minutes);
			}
			date = DateHelper.addDays(date, 1);
		}

		Collections.sort(lines, new Comparator() {
			public int compare(Object o1, Object o2) {
				String tcodeName1 = (String) ((ElapsedTimeLine) o1).getFields()
						.get(0);
				String tcodeName2 = (String) ((ElapsedTimeLine) o2).getFields()
						.get(0);

				int sortOrder1 = ((TimeCodeData) cm
						.getTimeCodeByName(tcodeName1)).getTcodeSortorder();
				int sortOrder2 = ((TimeCodeData) cm
						.getTimeCodeByName(tcodeName2)).getTcodeSortorder();
				if (sortOrder1 < sortOrder2) {
					return -1;
				} else if (sortOrder1 == sortOrder2) {
					return 0;
				} else {
					return 1;
				}
			}
		});

		List fieldNames = new ArrayList();
		fieldNames.add("WRKD_TCODE_NAME");
		fieldNames.add("WRKD_HTYPE_NAME");
		model.setFieldNames(fieldNames);
		model.setElapsedTimeLines(lines);
		model.setSummaryTotals(dayTotals);

		if (logger.isDebugEnabled()) {
			logger.debug("createSummaryPage(...)   return");
		}
		return model;
	}

	protected int processWorkDetail(WorkDetailLine detail, Map labors,
			List lines, int day) {
		if (logger.isDebugEnabled()) {
			logger.debug("processWorkDetail(...)   start");
		}

		int minutes = ((CSMWorkDetailLine) detail).minutes;
		List fields = new ArrayList();
		fields.add(((CSMWorkDetailLine) detail).tCode);
		fields.add(((CSMWorkDetailLine) detail).hType);
		String key = null;
		Iterator itr = fields.iterator();
		while (itr.hasNext()) {
			Object value = itr.next();
			key += value + ":";
		}

		ElapsedTimeLine line = new CSMElapsedTimeLine();
		if (!labors.containsKey(key)) {
			labors.put(key, new Integer(labors.size()));
			List hours = new ArrayList();
			for (int j = 0; j < DAYS_ON_TIMESHEET; j++) {
				hours.add(new Integer(0));
			}
			line = new CSMElapsedTimeLine();
			line.setFields(fields);
			line.setHours(hours);
			lines.add(line);
		} else {
			int lineNo = ((Integer) labors.get(key)).intValue();
			line = (ElapsedTimeLine) lines.get(lineNo);
		}
		line.getHours().set(
				day,
				new Integer(((Integer) line.getHours().get(day)).intValue()
						+ minutes));
		line.setLineTotal(line.getLineTotal() + minutes);

		if (logger.isDebugEnabled()) {
			logger.debug("processWorkDetail(...)   return");
		}
		return minutes;
	}

	protected OverridePage createOverridePage(DBConnection conn,
			CSMBiWeeklyTimeSheetPage parent, boolean isSupervisor)
			throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("createOverridePage(...)   start");
		}

		OverridePage form = new OverridePage();

		List ovrDays = new ArrayList();
		Date date = parent.getWeekStartDate();
		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			DayOverrideModel dayOverride = null;
			if (parent.getOverride() == null
					|| ((WorkSummaryModel) parent.getSummaries().get(i))
							.getInvalid()) {
				dayOverride = reloadDayOverrideModel(conn, parent.getWbuName(),
						parent.getEmployee().getEmpId(), date, isSupervisor);
			} else {
				dayOverride = (DayOverrideModel) parent.getOverride()
						.getOverrideDays().get(i);
			}
			ovrDays.add(dayOverride);

			date = DateHelper.addDays(date, 1);
		}
		form.setOverrideDays(ovrDays);

		if (logger.isDebugEnabled()) {
			logger.debug("createOverridePage(...)   return");
		}
		return form;
	}

	// ~~~~~~replace CSMWTShelper createBlankLine with our own
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public static ElapsedTimeLine createBlankLine(
			ElapsedTimeView elapsedTimeView) throws Exception {
		ElapsedTimeLine line = new CSMElapsedTimeLine();
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

	protected SummaryPage createSummaryPage(
			DBConnection conn,
			WeeklyTimeSheetPage parent)
	throws Exception {
		//System.out.println("using custom createSummaryPage");
		if ( logger.isDebugEnabled() ) {
			logger.debug("createSummaryPage(...)   start");
		}
		final CodeMapper cm = CodeMapper.createCodeMapper(conn);
		SummaryPage model = new SummaryPage();
		List dayTotals = new ArrayList();
		for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
			dayTotals.add(new Integer(0));
		}
		model.setSummaryTotal(0);
		List lines = new ArrayList();
		HashMap labors = new HashMap();

		Date date = parent.getWeekStartDate();
		int empId = parent.getEmployee().getEmpId();
		List days =
			loadWorkDetailForDays(conn, parent.getEmployee().getEmpId(), date);
		for (int day = 0; day < DAYS_ON_TIMESHEET; day++) {
			// Do not show summary information before hire and on or after termination date.
			if ((date.compareTo(parent.getEmployee().getHireDate()) < 0) ||
					(date.compareTo(parent.getEmployee().getTerminationDate()) >= 0) ||
					(!parent.getVisibleTeamDateSet().includes(empId, date)) ) {
				dayTotals.set(day, new Integer(0));
				date = DateHelper.addDays(date, 1);
				continue;
			}

			List details = (List) days.get(day);

			for (int i = 0; i < details.size(); i++) {
				WorkDetailLine detail = (WorkDetailLine) details.get(i);
				int minutes = processWorkDetail(detail, labors, lines, day);
				dayTotals.set(
						day,
						new Integer(
								((Integer) dayTotals.get(day)).intValue() + minutes));
				model.setSummaryTotal(model.getSummaryTotal() + minutes);
			}
			date = DateHelper.addDays(date, 1);
		}

		Collections.sort(lines,
				new Comparator() {
			public int compare(Object o1, Object o2) {
				String tcodeName1 = (String)((ElapsedTimeLine)o1).getFields().get(0);
				String tcodeName2 = (String)((ElapsedTimeLine)o2).getFields().get(0);

				int sortOrder1 = ((TimeCodeData)cm.getTimeCodeByName(tcodeName1)).getTcodeSortorder();
				int sortOrder2 = ((TimeCodeData)cm.getTimeCodeByName(tcodeName2)).getTcodeSortorder();
				if (sortOrder1 < sortOrder2) {
					return -1;
				} else if (sortOrder1 == sortOrder2) {
					return 0;
				} else {
					return 1;
				}
			}
		});

		List fieldNames = new ArrayList();
		fieldNames.add("WRKD_TCODE_NAME");
		fieldNames.add("WRKD_HTYPE_NAME");
		model.setFieldNames(fieldNames);
		model.setElapsedTimeLines(lines);
		model.setSummaryTotals(dayTotals);

		if ( logger.isDebugEnabled() ) {
			logger.debug("createSummaryPage(...)   return");
		}
		return model;
	}

}
