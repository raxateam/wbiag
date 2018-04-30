package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;

import com.workbrain.app.jsp.ActionContext;
import com.workbrain.app.jsp.action.timesheet.ElapsedTimeView;
import com.workbrain.app.jsp.action.timesheet.ExpandComponentAction;
import com.workbrain.app.jsp.action.timesheet.OverrideString;
import com.workbrain.app.jsp.action.timesheet.WorkSummaryModel;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.ta.model.EmployeeIdStartEndDate;
import com.workbrain.app.ta.model.EmployeeIdStartEndDateSet;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.server.WebContext;
import com.workbrain.server.jsp.JSPHelper;
import com.workbrain.server.jsp.taglib.sys.PageTag;
import com.workbrain.server.jsp.taglib.util.WebPageRequest;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.tool.overrides.AbstractOverrideBuilder;
import com.workbrain.tool.overrides.AbstractOverrideOperation;
import com.workbrain.tool.overrides.DeleteOverride;
import com.workbrain.tool.overrides.InsertOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.DateHelper;
import com.workbrain.util.LocalizationHelper;
public abstract class CSMSaveBWTimeSheetAction extends CSMAbstractSaveBWTimeSheetAction implements CSMBiWeeklyTimeSheetConstants{
	private String pageMfrmId = "2263"; //NOT VALID UNLESS REPLACING CORE COMPLETELY


	class ClearVisibleTimeSheetOverrideCmd extends EmployeeIdStartEndDateSet.BasicAbstractCommand {
		OverrideAccess oa;
		public ClearVisibleTimeSheetOverrideCmd(DBConnection conn) {
			this.oa = new OverrideAccess(conn);
		}
		public void execute(EmployeeIdStartEndDate empIdSEDate) throws Exception {
			this.oa.cancelByRangeAndType(empIdSEDate.getEmpId(),
					empIdSEDate.getStartDate(),
					empIdSEDate.getEndDate(),
					OverrideData.TIMESHEET_TYPE_START,
					OverrideData.TIMESHEET_TYPE_END,
					DateHelper.DATE_1900);
		}
	}

	public abstract String process( DBConnection conn, ActionContext context,
			Object requestObj ) throws Exception;

	public AbstractOverrideBuilder doProcess( DBConnection conn, ActionContext context,
			Object requestObj, AbstractOverrideBuilder ovrBuilder )
	throws Exception {
		CSMBiWeeklyTimeSheetPage model = (CSMBiWeeklyTimeSheetPage) context.getAttribute(
		"timesheet");

		CSMBiWeeklyTimeSheetPage reqCached = (CSMBiWeeklyTimeSheetPage) context.
		getAttribute("timesheet.request");
		CSMBiWeeklyTimeSheetPage loaded = (CSMBiWeeklyTimeSheetPage) context.getAttribute(
		"timesheet.loaded");
		if (loaded != null && model.equals(loaded)) {
			// multi-shift submit
			ovrBuilder = submitMultiShift(conn, context, model, loaded,
					reqCached,
					(CSMBiWeeklyTimeSheetPage) requestObj,
					ovrBuilder);
		} else {
			ovrBuilder = processShift(conn, context, requestObj, model,
					true, ovrBuilder);
		}

		// Clear loaded and request cache only when the request is processed
		// successfully.
		context.setAttribute("timesheet.loaded", null);
		context.setAttribute("timesheet.request", null);
		return ovrBuilder;
	}

	protected AbstractOverrideBuilder submitMultiShift(DBConnection conn,
			ActionContext context,
			CSMBiWeeklyTimeSheetPage model,
			CSMBiWeeklyTimeSheetPage loaded,
			CSMBiWeeklyTimeSheetPage reqCached,
			CSMBiWeeklyTimeSheetPage newRequest,
			AbstractOverrideBuilder ovrBuilder) throws Exception {
		// process shifts other than the current one
		for( int i = 1; i < 6; i++ ) { // TODO: Should this be DAYS_ON_TIMESHEET - 1?
			if( loaded.isShiftLoaded(i) && model.getShiftIndex() != i ) {
				//bhacko: remove schedule portion
				//reqCached.setSchedule(reqCached.getSchedule(i));
				reqCached.setElapsedTime(reqCached.getElapsedTime(i));
				reqCached.setShiftIndex(i);
				//bhacko: remove schedule portion
				//loaded.setSchedule(loaded.getSchedule(i));
				loaded.setElapsedTime(loaded.getElapsedTime(i));
				loaded.setShiftIndex(i);
				ovrBuilder = processShift(conn, context,
						reqCached, loaded, false,
						ovrBuilder);
			}
		}

		model.setSummaries(loaded.getSummaries());
		if (loaded.isShiftLoaded(model.getShiftIndex())) {
			//bhacko: remove schedule portion
			//model.setSchedule(loaded.getSchedule(model.getShiftIndex()));
			model.setElapsedTime(loaded.getElapsedTime(model.getShiftIndex()));
			ovrBuilder = processShift(conn, context, newRequest, model,
					true, ovrBuilder);
		} else {
			ovrBuilder = processShift(conn, context, newRequest, model,
					true, ovrBuilder);
		}

		return ovrBuilder;
	}

	protected AbstractOverrideBuilder processShift(DBConnection conn,
			ActionContext context,
			Object requestObj, CSMBiWeeklyTimeSheetPage model,
			boolean processAllSections,
			AbstractOverrideBuilder ovrBuilder) throws Exception {
		CodeMapper codeMapper = CodeMapper.createCodeMapper( conn );
		ElapsedTimeView elapsedTimeView = CSMRequestCache.getElapsedTimeView( conn, context, true );

		if (! validate( model, requestObj, conn) ) {
			return ovrBuilder;
		}

		CSMBiWeeklyTimeSheetPage request = (CSMBiWeeklyTimeSheetPage)requestObj;

		updateHandsOffDates(conn, model);

		boolean copied = model.getCopied();
		boolean distributed = model.getElapsedTime().getDistributed();
		int noOfElapsedTimeLinesCopied = 0;

		if (copied) {
			ClearVisibleTimeSheetOverrideCmd cmd = new ClearVisibleTimeSheetOverrideCmd(conn);
			cmd.addEmp(
					model.getEmployee().getEmpId(),
					model.getWeekStartDate(),
					model.getWeekEndDate() );
			model.getVisibleTeamDateSet().runCommand(cmd);
			noOfElapsedTimeLinesCopied = request.getElapsedTime().getElapsedTimeLines().size();
		}

		if( model == null ) {
			model = new CSMBiWeeklyTimeSheetPage();
		}
		//bhacko: Don't need schedules
		/*if( model.getSchedule() == null ) {
		 model.setSchedule( createSchedulePage( conn, codeMapper, model ) );
		 }*/
		if( distributed || model.getElapsedTime() == null ) {
			model.setElapsedTime( createElapsedTimePage( conn, codeMapper,
					elapsedTimeView, model, false, null ) );
		}
		if (copied || distributed) {
			int noOfElapsedTimeLines = model.getElapsedTime().getElapsedTimeLines().size();
			if (noOfElapsedTimeLinesCopied > noOfElapsedTimeLines) {
				for (int i = 0; i < noOfElapsedTimeLinesCopied - noOfElapsedTimeLines; i++) {
					model.getElapsedTime().getElapsedTimeLines().add(CSMWTShelper.createBlankLine(elapsedTimeView));
				}
			}
		}
		//bhacko: Don't need override page
		/*
		 if( model.getOverride() == null ) {
		 model.setOverride( createOverridePage( conn, model,context.getAttribute("WBG_SESSION").equals("Y")) );
		 }*/
		if( model.getSummary() == null ) {
			model.setSummary( createSummaryPage( conn, model ) );
		}
		//bhacko: Don't need to show schedule
		/*
		 if (model.getShowSchedule() && !CSMWTSPresentationHelper.instance().useWorkStartStop(model)){
		 processScheduleOverrides( conn, model.getSchedule(),
		 request, model, ovrBuilder );
		 }*/
		//bhacko: Don't need start/stop times
		/*
		 if( CSMWTSPresentationHelper.instance().useWorkStartStop(model) ) {
		 processStartStopTimeOverrides(context, conn, elapsedTimeView,
		 request,
		 model, ovrBuilder);
		 }*/

		if( processAllSections ) {
			processComments(conn, model.getSummaries(), request.getSummaries(),
					model.getWeekStartDate(), model.getWbuName(),
					model.getWbuNameActual(),
					model.getEmployee().getEmpId(),
					model.getVisibleTeamDateSet(), ovrBuilder);

			processFlags(conn, model.getSummaries(), request.getSummaries(),
					model.getWeekStartDate(), model.getWbuName(),
					model.getWbuNameActual(),
					model.getEmployee().getEmpId(), model.getVisibleTeamDateSet(), ovrBuilder);

			processSubmitted(model, request, ovrBuilder);

			WebContext wc = JSPHelper.getWebContext(context.getRequest());
			PageTag pt = (PageTag)((WebPageRequest) wc).getWebPage();
			pt.setMaintenanceFormId(pageMfrmId); //weekly timesheet
			PageContext pc = pt.getPageContext();

			processDailyLocks(conn, model, request, new LocalizationHelper(pc));
		}

		processElapsedTimeOverrides( context, conn, elapsedTimeView,
				(CSMElapsedTimePage) model.getElapsedTime(),
				request,
				model, ovrBuilder );

		// The original NWT page is restored if sections have been expanded or collapsed,
		// since the models NWT page is changed under ExpandComponentAction
		CSMLTAPage nwtp = (CSMLTAPage)context.getAttribute(ExpandComponentAction.ORIGINAL_NON_WORK_TIME_PAGE);
		if (nwtp != null) model.setNonWorkTimePage(nwtp);
		context.setAttribute(ExpandComponentAction.ORIGINAL_NON_WORK_TIME_PAGE, null);
		processNonWorkTimes(conn, ovrBuilder, model.getEmployee().getEmpId(),
				(CSMLTAPage) request.getNonWorkTimePage(), model);

		// The original FDA page is restored if sections have been expanded or collapsed,
		// since the models FDA page is changed under ExpandComponentAction
		CSMLTAPage fdap = (CSMLTAPage)context.getAttribute(ExpandComponentAction.ORIGINAL_FULL_DAY_ABSENCE_PAGE);
		if (fdap != null) model.setFullDayAbsencePage(fdap);
		context.setAttribute(ExpandComponentAction.ORIGINAL_FULL_DAY_ABSENCE_PAGE, null);
		processFullDayAbsences(conn, ovrBuilder, model.getEmployee().getEmpId(),
				(CSMLTAPage)request.getFullDayAbsencePage(), model);

		if( processAllSections ) {
			boolean modified = processAuthorized(conn, model, request,
					ovrBuilder);

			if (modified) {
				ovrBuilder.setDirection("TS");
				List empIdList = new ArrayList();
				empIdList.add(String.valueOf(model.getEmployee().getEmpId()));
				ovrBuilder.setEmpIdList(empIdList);
				ovrBuilder.setFirstStartDate(model.getWeekStartDate());
				ovrBuilder.setLastEndDate(model.getWeekEndDate());
				//ovrBuilder.execute(true);
				//TT 10692
				if (model.getAuthorized() == request.getAuthorized()) {
					HttpSession session = context.getRequest().getSession(false);
					session.setAttribute("WTS_MODIFIED", "Y");
				}
				//end TT 10692
			}
		}

		model.setCopied(false);
		context.setAttribute( "timesheet", model );

		return ovrBuilder;
		//return CSMWTShelper.getActionURL(AbstractActionFactory.VIEW_TIMESHEET_ACTION);
		//return ("/action/timesheet?action=" + AbstractActionFactory.VIEW_TIMESHEET_ACTION);
		//return "/action/timesheet/ViewTimeSheet";
	}

	private boolean validate( CSMBiWeeklyTimeSheetPage timesheet, Object request, DBConnection conn ) {
		boolean valid = true;

//CSM CR27: Remove this validation altogether,
//the same validation is going to be done in a rule, so that the days can be properly highlighted.
//		CSMElapsedTimePage etPage = (CSMElapsedTimePage)((CSMBiWeeklyTimeSheetPage)request).getElapsedTime();
//
//		int dayHours[] = new int[DAYS_ON_TIMESHEET];
//		for (int i=0; i<DAYS_ON_TIMESHEET; i++) {
//			dayHours[i] = 0;
//		}
//
//		int totalHours = 0;
//
//		//CSM CR27: Mark the premium lines first before processing validation
//		//Iterator lines = etPage.getElapsedTimeLines().iterator();
//		List lines = etPage.getElapsedTimeLines();
//		CSMWTShelper.markPremiumLines(conn,lines);
//
//		Iterator it = lines.iterator();
//		while( it.hasNext() ) {
//			CSMElapsedTimeLine line = (CSMElapsedTimeLine)it.next();
//			//CSM CR27: Only validate the 24 limit with elapsed time overrides.
//			//Always skip premium lines
//			if(!line.isPremium()){
//				Iterator hours = line.getHours().iterator();
//				int day = 0;
//				while( hours.hasNext() ) {
//					int minutes = ((Integer)hours.next()).intValue();
//					dayHours[day] += minutes;
//					totalHours += minutes;
//					day++;
//				}
//			}
//		}
//		for( int i = 0; i < DAYS_ON_TIMESHEET; i++ ) {
//			WorkSummaryModel summary = (WorkSummaryModel)timesheet.getSummaries().get(i);
//
//			int minutes = dayHours[i];
//			if( minutes > 24 * 60 ) {
//				summary.setWrksError( "The total hours for the date exceeded 24. Please type in again." );
//				valid = false;
//			}
//		}

		return valid;
	}

	protected void processElapsedTimeOverrides( ActionContext context, DBConnection conn,
			ElapsedTimeView elapsedTimeView,
			CSMElapsedTimePage model,
			CSMBiWeeklyTimeSheetPage wtsRequest,
			CSMBiWeeklyTimeSheetPage parent, AbstractOverrideBuilder ovrBuilder )
	throws Exception {
		//WBLogger.debug("com.workbrain.app.jsp.action.timesheetAbstractSaveTimeSheetAction.class", "ElapsedTime.processOverrides()");
		premTCodeList = CSMWTShelper.getPremiumTimeCodes(conn);

		CSMElapsedTimePage request = (CSMElapsedTimePage) wtsRequest.getElapsedTime();
		//SchedulePage scheduleRequest = wtsRequest.getSchedule();
		HttpSession session = context.getRequest().getSession(false);
		int lines = request.getElapsedTimeLines().size();
		int cols = DAYS_ON_TIMESHEET;

		String ovrStrings[][] = new String[lines][cols];
		int ovrIds[][] = new int[lines][cols];
		boolean doOverrides[] = new boolean[cols];
		boolean doElapsedTimeOverride[][] = new boolean[lines][cols];
		boolean isSummaryReadonly[] = new boolean[cols];

		boolean flagLine = true;

		for( int i = 0; i < lines; i++ ) {

			CSMElapsedTimeLine newLine = (CSMElapsedTimeLine)request.getElapsedTimeLines().get( i );

			//CSMElapsedTimeLine oldLine = (CSMElapsedTimeLine)model.getElapsedTimeLines().get( i );
			CSMElapsedTimeLine oldLine;
			if (i < model.getElapsedTimeLines().size()){
				oldLine = (CSMElapsedTimeLine)model.getElapsedTimeLines().get( i );
			}else{
				flagLine = false;
				oldLine = new CSMElapsedTimeLine();
			}

			for( int j = 0; j < cols; j++ ) {
				if (flagLine){
					int ovrId = ((Integer)oldLine.getOvrIds().get(j)).intValue();

					ovrIds[i][j] = ovrId;
					OverrideString ovrString = new OverrideString();
					boolean doOverride = processFields( wtsRequest,
							conn, elapsedTimeView,
							oldLine, newLine, j, ovrString );
					doOverride = doOverride || (ovrId < 0 && ovrString.length() > 0); // part of TT23356, making sure the new items from defaultTimesheets are saved
					ovrStrings[i][j] = ovrString.toString();
					doOverrides[j] = doOverrides[j] || doOverride;
					doElapsedTimeOverride[i][j] = doOverride;
				}
			}
			oldLine.setDistributed(false);
		}

		boolean del[] = new boolean[lines];
		for( int j = 0; j < lines; j++ ) {
			CSMElapsedTimeLine line = (CSMElapsedTimeLine)request.getElapsedTimeLines().get(j);
			del[j] = line.getDel();
		}

		if (parent != null){
			List summaries = parent.getSummaries();
			for (int i=0; i< summaries.size(); i++) {
				WorkSummaryModel sum = null;
				if (summaries.size() > i) {
					sum = (WorkSummaryModel) summaries.get(i);
					isSummaryReadonly[i] = sum.getReadOnly();
				}
			}
		}
		for( int i = 0; i < cols && lines != 0; i++ ) {
			//bhacko: Not using start/stop times
			/*DayScheduleModel req = (DayScheduleModel)scheduleRequest.
			getScheduleDays().get(i);
			DayScheduleModel mod = (DayScheduleModel)parent.getSchedule().
			getScheduleDays().get(i);
			Date newStartTime = req.getActStartTime();

			if (!CSMWTSPresentationHelper.instance().useWorkStartStop(parent)) {
				if( newStartTime.compareTo( mod.getActStartTime() ) != 0 ) {
					CSMElapsedTimeLine oldLine;
					for (int k = 0; k < model.getElapsedTimeLines().size(); k++) {
						oldLine = (CSMElapsedTimeLine)model.getElapsedTimeLines().get(k);
						if (oldLine.getHours().size() > i) {
							Integer hour = (Integer)oldLine.getHours().get(i);
							if (null != hour && hour.intValue() != 0) {
								doOverrides[i] = true;
								doElapsedTimeOverride[k][i] = true;
							}
						}
					}
				}
			}*/
			for( int j = 0; j < lines; j++ ) {
				CSMElapsedTimeLine line = (CSMElapsedTimeLine)request.getElapsedTimeLines().get(j);
				if( line.getDel() && ovrStrings[j][i].length() > 0 ) {
					doOverrides[i] = true;
					doElapsedTimeOverride[j][i] = true;
				}
			}
		}
		OverrideAccess ovrAccess = new OverrideAccess( conn );

		String defaultTime = "";
		if (session.getAttribute("APPLY_DEFAULT")!=null){
			defaultTime = session.getAttribute("APPLY_DEFAULT").toString();
		}

		Date date = parent.getWeekStartDate();
		int empId = parent.getEmployee().getEmpId();
		for( int i = 0; i < cols; i++ ) {
			if ( ! parent.getVisibleTeamDateSet().includes(empId, date) ) {
				date = DateHelper.addDays( date, 1 );
				continue;
			}
			if( doOverrides[i] || "Y".equals(defaultTime) ) {
				if ("Y".equals(defaultTime)){
					Iterator ovrsDel = ovrAccess.loadByRangeAndType(parent.getEmployee().getEmpId(),
							parent.getWeekStartDate(),
							parent.getWeekEndDate(),
							600,699 ).iterator();

					while( ovrsDel.hasNext() ) {
						OverrideData ovr = (OverrideData)ovrsDel.next();
						if( ovr.getShiftIndex() == parent.getShiftIndex() ) {
							int ovrId = ovr.getOvrId();
							DeleteOverride delOvr = new DeleteOverride();
							delOvr.setOverrideId(ovrId);
							delOvr.setWbuNameBoth(wtsRequest.getWbuName() , wtsRequest.getWbuNameActual());
							ovrBuilder.add(delOvr);
						}
					}
				}

				int ovrOrder = 0;

				//CSM: calculate default start time in the applyOverride method
				Date startTime = null;
				//getDefaultScheduleTime();
				/*DayScheduleModel schedule = (DayScheduleModel)scheduleRequest.
				getScheduleDays().get(i);
				if( !CSMWTSPresentationHelper.instance().useWorkStartStop(parent) ) {
					startTime = schedule.getActStartTime();
					if (startTime == null ||
							new Date(0).compareTo(startTime) == 0) {
						startTime = schedule.getDefStartTime();
					}
				}*/

				for( int j = 0; j < lines; j++ ) {
					if(flagLine){
						if ( doElapsedTimeOverride[j][i] && !isSummaryReadonly[i]) {
							if (applyOverride(defaultTime, parent, ovrIds[j][i],
									ovrStrings[j][i],
									(WorkSummaryModel) parent.
									getSummaries().get(i),
									ovrBuilder, ovrOrder, startTime,
									del[j]))
								ovrOrder++;
						}
					}
				}

			}
			date = DateHelper.addDays( date, 1 );
		}
		session.setAttribute("APPLY_DEFAULT", "N");
	}

	/*protected void processStartStopTimeOverrides(ActionContext context, DBConnection conn,
	 ElapsedTimeView elapsedTimeView, CSMBiWeeklyTimeSheetPage request, CSMBiWeeklyTimeSheetPage loaded,
	 AbstractOverrideBuilder ovrBuilder) throws Exception {

	 SchedulePage requestSchedule = request.getSchedule();
	 SchedulePage loadedSchedule = loaded.getSchedule();

	 setElapsedTimeDayTotals(request.getElapsedTime());
	 Calendar cal = Calendar.getInstance();
	 cal.setTime(request.getWeekStartDate());
	 int empId = loaded.getEmployee().getEmpId();
	 for (int i = 0; i < DAYS_ON_TIMESHEET; i++) {
	 if ( ! loaded.getVisibleTeamDateSet().includes(empId, cal.getTime()) ) {
	 cal.add(Calendar.DAY_OF_YEAR, 1);
	 continue;
	 }
	 DayScheduleModel requestDay = requestSchedule.getScheduleDay(i);
	 DayScheduleModel loadedDay = loadedSchedule.getScheduleDay(i);

	 if (userClearedStartStopBoundaries(requestDay)) continue;

	 adjustStartStopTimes(requestDay);
	 pushScheduleToFitElapsedTimeDayTotals(requestSchedule, request.getElapsedTime(), i);

	 if( doProcessStartStopTimes(loadedDay, requestDay) ) {
	 processStartStopTimes(conn, ovrBuilder, loaded.getEmployee().getEmpId(),
	 i, requestDay, loaded);
	 }

	 if (loadedDay.getNoMealBreak() != requestDay.getNoMealBreak()) {
	 processNoMealBreak(conn, ovrBuilder, loaded, i, requestDay.getNoMealBreak());
	 }
	 cal.add(Calendar.DAY_OF_YEAR, 1);
	 }
	 }*/

	/*private void setElapsedTimeDayTotals(CSMElapsedTimePage etp) {
	 int numLines = etp.getElapsedTimeLines().size();
	 List dayTotals = new ArrayList();
	 for (int day = 0; day < DAYS_ON_TIMESHEET; day++) {
	 int dayTotal = 0;
	 for (int line = 0; line < numLines; line++) {
	 CSMElapsedTimeLine etl = (CSMElapsedTimeLine)etp.getElapsedTimeLine(line);
	 if (! etl.getDel()) {
	 dayTotal += ((Integer)etl.getHours().get(day)).intValue();
	 }
	 }
	 dayTotals.add(new Integer(dayTotal));
	 }
	 etp.setDayTotals(dayTotals);
	 }*/

	// dayTotals must be set in etp
	// sp must have correct dates
	/*private void pushScheduleToFitElapsedTimeDayTotals(SchedulePage sp, CSMElapsedTimePage etp, int day) {
	 DayScheduleModel dsm = sp.getScheduleDay(day);
	 long breakLength = dsm.hasBreak() ? (dsm.getBrkEndTime().getTime() - dsm.getBrkStartTime().getTime()) / 60000 : 0;
	 long shiftLength = (dsm.getWrkEndTime().getTime() - dsm.getWrkStartTime().getTime()) / 60000  - breakLength;
	 long dayElapsedTime = ((Integer)etp.getDayTotals().get(day)).intValue();

	 if (dayElapsedTime > shiftLength) {
	 dsm.setWrkEndTime(DateHelper.addMinutes(dsm.getWrkEndTime(), (int)(dayElapsedTime - shiftLength)));
	 }
	 }*/

	/*private boolean userClearedStartStopBoundaries(DayScheduleModel requestDay) {
	 Date wrkStart = requestDay.getWrkStartTime();
	 Date wrkEnd = requestDay.getWrkEndTime();
	 return wrkStart == null || wrkStart.compareTo(DateHelper.DATE_1900) == 0 ||
	 wrkEnd == null   || wrkEnd.compareTo(DateHelper.DATE_1900)   == 0;
	 }*/

	/*private boolean doProcessStartStopTimes(DayScheduleModel loaded, DayScheduleModel request) {
	 return loaded.getWrkStartTime().compareTo(request.getWrkStartTime()) != 0 ||
	 loaded.getWrkEndTime().compareTo(request.getWrkEndTime()) != 0 ||
	 loaded.getBrkStartTime().compareTo(request.getBrkStartTime()) != 0 ||
	 loaded.getBrkEndTime().compareTo(request.getBrkEndTime()) != 0;
	 }*/

	/*private void processNoMealBreak(DBConnection conn,
	 AbstractOverrideBuilder ovrBuilder,
	 CSMBiWeeklyTimeSheetPage model,
	 int dayIndex, boolean noMealBreak) {
	 WorkSummaryModel summary = (WorkSummaryModel) model.getSummaries().get(dayIndex);
	 if (noMealBreak) {
	 InsertWorkSummaryOverride iwso = new InsertWorkSummaryOverride(conn);
	 iwso.setEmpId(model.getEmployee().getEmpId());
	 iwso.setWbuName(model.getWbuName());
	 iwso.setWrksFlagBrk("Y");
	 iwso.setStartDate(summary.getWorkDate());
	 iwso.setEndDate(summary.getWorkDate());
	 ovrBuilder.add(iwso);
	 } else {
	 InsertWorkSummaryOverride iwso = new InsertWorkSummaryOverride(conn);
	 iwso.setEmpId(model.getEmployee().getEmpId());
	 iwso.setWbuName(model.getWbuName());
	 iwso.setWrksFlagBrk("N");
	 iwso.setStartDate(summary.getWorkDate());
	 iwso.setEndDate(summary.getWorkDate());
	 ovrBuilder.add(iwso);
	 }
	 }*/

	/*private void processStartStopTimes(DBConnection conn,
	 AbstractOverrideBuilder ovrBuilder,
	 int empId, int dayIndex,
	 DayScheduleModel scheduleReq,
	 CSMBiWeeklyTimeSheetPage model
	 ) throws
	 SQLException {
	 boolean hasBreak = scheduleReq.hasBreak();
	 Date wrkStart = scheduleReq.getWrkStartTime();
	 Date wrkEnd = scheduleReq.getWrkEndTime();
	 Date brkStart = scheduleReq.getBrkStartTime();
	 Date brkEnd = scheduleReq.getBrkEndTime();

	 Iterator ovrsToDelete = null;
	 WorkSummaryModel summary = (WorkSummaryModel) model.getSummaries().get(
	 dayIndex);
	 OverrideAccess ovrAccess = new OverrideAccess(conn);

	 ovrsToDelete = ovrAccess.loadAffectingOverrides(empId,
	 summary.getWorkDate(),
	 600, 699).iterator();
	 while (ovrsToDelete.hasNext()) {
	 OverrideData ovr = (OverrideData) ovrsToDelete.next();
	 if (ovr.retrieveIsTimeSheetStartStop() &&
	 ovr.getShiftIndex() == model.getShiftIndex()) {
	 AbstractOverrideOperation del = getDeleteOverride();
	 del.setOverrideId(ovr.getOvrId());
	 del.setWbuNameBoth(model.getWbuName() , model.getWbuNameActual());
	 ovrBuilder.add(del);
	 }
	 }

	 if( hasBreak ) {
	 ovrBuilder.add(createStartStopInsOvr(conn,
	 model.getWbuName(),
	 model.getWbuNameActual(),
	 model.getEmployee().getEmpId(),
	 summary.getWorkDate(),
	 model.getShiftIndex(),
	 wrkStart, brkStart,
	 false));
	 ovrBuilder.add(createStartStopInsOvr(conn,
	 model.getWbuName(),
	 model.getWbuNameActual(),
	 model.getEmployee().getEmpId(),
	 summary.getWorkDate(),
	 model.getShiftIndex(),
	 brkStart, brkEnd, true));
	 ovrBuilder.add(createStartStopInsOvr(conn,
	 model.getWbuName(),
	 model.getWbuNameActual(),
	 model.getEmployee().getEmpId(),
	 summary.getWorkDate(),
	 model.getShiftIndex(),
	 brkEnd, wrkEnd, false));
	 } else {
	 ovrBuilder.add(createStartStopInsOvr(conn,
	 model.getWbuName(),
	 model.getWbuNameActual(),
	 model.getEmployee().getEmpId(),
	 summary.getWorkDate(),
	 model.getShiftIndex(),
	 wrkStart, wrkEnd,
	 false));
	 }
	 summary.setInvalid(true);
	 }*/

	/*protected AbstractOverrideOperation createStartStopInsOvr(
	 DBConnection conn, String wbuName, String wbuNameActual,
	 int empId, Date date, int shiftIndex,
	 Date startTime, Date endTime, boolean isBrk
	 ) throws SQLException {
	 AbstractOverrideOperation ins = getInsertOverride();
	 ins.setWbuNameBoth(wbuName, wbuNameActual);
	 ins.setEmpId(empId);
	 ins.setStartDate(date);
	 ins.setEndDate(date);
	 ins.setStartTime(startTime);
	 ( (InsertOverride) ins).setEndTime(endTime);
	 OverrideString ovrString = new OverrideString(
	 isBrk ? StartStopTimeView.getBrkOverrideValue() :
	 StartStopTimeView.getWrkOverrideValue());

	 TimeCodeAccess tcodeAccess = new TimeCodeAccess(conn);
	 String tcode = ovrString.get(WorkDetailData.WRKD_TCODE_NAME);
	 List lstTimeCode = tcodeAccess.loadByNames("'" + tcode + "'");
	 TimeCodeData timeCode = (TimeCodeData) lstTimeCode.get(0);
	 if (timeCode != null) {
	 HourTypeAccess hTypeAccess = new HourTypeAccess(conn);
	 HourTypeData htd = hTypeAccess.load(timeCode.getHtypeId());
	 if (htd != null) {
	 ovrString.set(WorkDetailData.WRKD_HTYPE_NAME, htd.getHtypeName());

	 CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
	 EmployeeAccess empAccess = new EmployeeAccess(conn, codeMapper);
	 EmployeeData empData = empAccess.load(empId, date);
	 if (empData != null) {
	 double rate = empData.getEmpBaseRate()
	 * htd.getHtypeMultiple();
	 ovrString.set(WorkDetailData.WRKD_RATE, Double
	 .toString(rate));
	 }
	 }
	 }

	 ovrString.set(OverrideData.TIMESHEET_START_STOP, "TRUE");
	 if (shiftIndex > 1) {
	 ovrString.set("SHIFT_INDEX",
	 String.valueOf(shiftIndex));
	 }
	 ins.setOvrNewValue(ovrString.toString());
	 ins.setOvrType(TIMESHEET_TYPE_START);
	 ins.setOvrComment("");
	 ins.setOvrOrder(0);

	 return ins;
	 }*/

	protected void processNonWorkTimes(DBConnection conn,
			AbstractOverrideBuilder ovrBuilder,
			int empId,
			CSMLTAPage nonWorkTimeReq,
			CSMBiWeeklyTimeSheetPage model) throws
			SQLException {
		boolean doInvalidateSummaries[] = new boolean[DAYS_ON_TIMESHEET];
		for( int i = 0; i < nonWorkTimeReq.getLines().size() &&
		i < model.getNonWorkTimePage().getLines().size(); i++ ) {
			CSMLTAPage.LTALine loaded = (CSMLTAPage.LTALine)
			model.getNonWorkTimePage().getLines().get(i);
			CSMLTAPage.LTALine req = (CSMLTAPage.LTALine)
			nonWorkTimeReq.getLines().get(i);
			if( req.getDel() ) {
				for (int j = 0; j < loaded.getDays().size(); j++) {
					int oldOvrId = ((CSMLTAPage.Override)loaded.getDays().get(j)).getId();
					if( oldOvrId != -1) {
						cancelLTAOverride(conn, oldOvrId, ovrBuilder , model.getWbuName() , model.getWbuNameActual() );
					}
				}
			} else {
				// Do nothing if there is no timecode for this line 37804
				if (req.getType().equals("")) continue;
				//TODO: Find out where the LTALine _days property gets set
				for (int j = 0; j < req.getDays().size() && j < loaded.getDays().size(); j++) {
					CSMLTAPage.Override loadedOvr = (CSMLTAPage.Override) loaded.getDays().get(j);
					CSMLTAPage.Override reqOvr = (CSMLTAPage.Override) req.getDays().get(j);

					if (norm(loadedOvr.getStart()).compareTo(norm(reqOvr.getStart())) != 0 ||
							norm(loadedOvr.getStop()).compareTo(norm(reqOvr.getStop())) != 0 ||
							// Allow the user to change the type of an existing line. 37812
							(!req.getType().equals(loaded.getType()) &&
									norm(loadedOvr.getStart()).compareTo(norm(null)) != 0 &&
									norm(loadedOvr.getStop()).compareTo(norm(null)) != 0)) {

						Date workDate = DateHelper.addDays(model.getWeekStartDate(), j);
						if( reqOvr.getStart() != null && reqOvr.getStop() != null) {
							reqOvr.setStart(DateHelper.setTimeValues((Date)workDate.clone(), reqOvr.getStart()));
							reqOvr.setStop(DateHelper.setTimeValues((Date)workDate.clone(), reqOvr.getStop()));
						} else {
							// Allow partial time entries to the NWT by completing the missing
							// time entry with an appropriate schedule boundary. 37825
							if (reqOvr.getStart() == null && reqOvr.getStop() != null) {
								reqOvr.setStart(model.getSchedule().getScheduleDay(j).getActStartTime());
								reqOvr.setStop(DateHelper.setTimeValues((Date)workDate.clone(), reqOvr.getStop()));
							} else if (reqOvr.getStart() != null && reqOvr.getStop() == null) {
								reqOvr.setStart(DateHelper.setTimeValues((Date)workDate.clone(), reqOvr.getStart()));
								reqOvr.setStop(model.getSchedule().getScheduleDay(j).getEndTime());
							}
						}

						int oldOvrId = ( (CSMLTAPage.Override) loaded.getDays().
								get(j)).getId();
						if (oldOvrId != -1) {
							cancelLTAOverride(conn, oldOvrId, ovrBuilder , model.getWbuName() , model.getWbuNameActual() );
						}
						// Skip adding an NWT override if it has no time. 37839
						if (norm(reqOvr.getStart()).compareTo(norm(null)) != 0 || norm(reqOvr.getStop()).compareTo(norm(null)) != 0) {

							// Handle over-midnight entries by bumping the stop time ahead one day. 37840
							if (reqOvr.getStart().compareTo(reqOvr.getStop()) > 0) {
								reqOvr.setStop(DateHelper.addDays(reqOvr.getStop(), 1));
							}

							addLTAOvr(conn, model.getWbuName(),
									model.getWbuNameActual(), empId,
									workDate,
									req.getType(),
									reqOvr.getStart(), reqOvr.getStop(),
									ovrBuilder);
						}
						doInvalidateSummaries[j] = true;
					}
				}
			}
		}

		for( int j = 0; j < DAYS_ON_TIMESHEET; j++ ) {
			if( doInvalidateSummaries[j] ) {
				WorkSummaryModel summary = (WorkSummaryModel) model.
				getSummaries().
				get(j);
				summary.setInvalid(true);
			}
		}
	}

	private Date norm( Date date ) {
		if( date == null ) {
			return DateHelper.DATE_1900;
		} else {
			return date;
		}
	}

	protected void processFullDayAbsences(DBConnection conn,
			AbstractOverrideBuilder ovrBuilder,
			int empId,
			CSMLTAPage fullDayAbsenceReq,
			CSMBiWeeklyTimeSheetPage model) throws
			SQLException {
		boolean doInvalidateSummaries[] = new boolean[DAYS_ON_TIMESHEET];
		for( int i = 0; i < fullDayAbsenceReq.getLines().size() &&
		i < model.getFullDayAbsencePage().getLines().size(); i++ ) {
			CSMLTAPage.LTALine loaded = (CSMLTAPage.LTALine)
			model.getFullDayAbsencePage().getLines().get(i);
			CSMLTAPage.LTALine req = (CSMLTAPage.LTALine)
			fullDayAbsenceReq.getLines().get(i);
			for( int j = 0; j < req.getDays().size() && j < loaded.getDays().size(); j++ ) {
				boolean loadedChecked = ((CSMLTAPage.Override)loaded.getDays().get(j)).getChecked();
				boolean reqChecked = ((CSMLTAPage.Override)req.getDays().get(j)).getChecked();
				if( loadedChecked && !reqChecked ) {
					cancelLTAOverride(conn,
							((CSMLTAPage.Override)loaded.getDays().get(j)).getId(),
							ovrBuilder ,
							model.getWbuName() , model.getWbuNameActual() );
					doInvalidateSummaries[j] = true;
				} else if( !loadedChecked && reqChecked ) {
					cancelAllETOverrides(conn, empId,
							DateHelper.addDays(model.getWeekStartDate(),j),
							ovrBuilder,
							model.getWbuName() , model.getWbuNameActual()
					);
					addLTAOvr(conn, model.getWbuName(),
							model.getWbuNameActual(), empId,
							DateHelper.addDays(model.getWeekStartDate(),j),
							req.getType(), null, null, ovrBuilder);
					doInvalidateSummaries[j] = true;
				}
			}
		}

		for( int j = 0; j < DAYS_ON_TIMESHEET; j++ ) {
			if( doInvalidateSummaries[j] ) {
				WorkSummaryModel summary = (WorkSummaryModel) model.
				getSummaries().
				get(j);
				summary.setInvalid(true);
			}
		}
	}

	protected void cancelLTAOverride(DBConnection conn, int ovrId,
			AbstractOverrideBuilder ovrBuilder,
			String wbuName, String wbuNameActual) throws
			SQLException {
		AbstractOverrideOperation del = getDeleteOverride();
		del.setOverrideId(ovrId);
		del.setWbuNameBoth(wbuName , wbuNameActual);
		ovrBuilder.add(del);
	}

	protected void cancelAllETOverrides(DBConnection conn, int empId, Date date,
			AbstractOverrideBuilder ovrBuilder,
			String wbuName, String wbuNameActual) throws   SQLException {
		OverrideAccess ovrAcc = new OverrideAccess(conn);
		Iterator ovrsToDelete = ovrAcc.loadAffectingOverrides(empId, date,
				OverrideData.TIMESHEET_TYPE_START,
				OverrideData.TIMESHEET_TYPE_END, new String[]
				                                            {OverrideData.HOLDING, OverrideData.UNPUBLISHED,
				OverrideData.CANCEL, OverrideData.CANCELLED}).iterator();
		while( ovrsToDelete.hasNext() ) {
			OverrideData ovr = (OverrideData)ovrsToDelete.next();
			AbstractOverrideOperation del = getDeleteOverride();
			del.setOverrideId( ovr.getOvrId() );
			del.setWbuNameBoth(wbuName , wbuNameActual);
			ovrBuilder.add( del );
		}
		ovrsToDelete = ovrAcc.loadAffectingOverrides(empId, date,
				OverrideData.LTA_TYPE_START,
				OverrideData.LTA_TYPE_END, new String[]
				                                      {OverrideData.HOLDING, OverrideData.UNPUBLISHED,
				OverrideData.CANCEL, OverrideData.CANCELLED}).iterator();
		while( ovrsToDelete.hasNext() ) {
			OverrideData ovr = (OverrideData)ovrsToDelete.next();
			AbstractOverrideOperation del = getDeleteOverride();
			del.setOverrideId( ovr.getOvrId() );
			del.setWbuNameBoth(wbuName , wbuNameActual);
			ovrBuilder.add( del );
		}
	}

	protected void addLTAOvr(DBConnection conn,
			String wbuName, String wbuNameActual,
			int empId, Date date,
			String tCodeName,
			Date startTime,
			Date stopTime,
			AbstractOverrideBuilder ovrBuilder) throws
			SQLException {
		AbstractOverrideOperation ins = getInsertOverride();
		ins.setWbuNameBoth(wbuName, wbuNameActual);
		ins.setEmpId(empId);
		ins.setStartDate(date);
		ins.setEndDate(date);
		if( startTime != null && stopTime != null ) {
			ins.setStartTime(startTime);
			((InsertOverride)ins).setEndTime(stopTime);
		}
		OverrideString ovrString = new OverrideString();
		ovrString.set(OverrideData.LTA_TCODE_OVERRIDE_FIELDNAME, tCodeName);
		ins.setOvrNewValue(ovrString.toString());
		ins.setOvrType(OverrideData.LTA_TYPE_START);
		ins.setOvrComment("");

		ovrBuilder.add(ins);
	}

	protected void addScheduleDelOvr( DBConnection conn, int empId, Date date,
			AbstractOverrideBuilder ovrBuilder,
			String wbuName, String wbuNameActual)
	throws SQLException {
		/*OverrideAccess ovrAccess = new OverrideAccess( conn );
		Iterator ovrsToDelete = ovrAccess.loadAffectingOverrides(empId, date,
				OverrideData.SCHEDULE_SCHEDTIMES_TYPE,
				OverrideData.SCHEDULE_SCHEDTIMES_TYPE, new String[]
				                                                  {OverrideData.CANCELLED, OverrideData.HOLDING, OverrideData.APPLIED,
				OverrideData.UNPUBLISHED, OverrideData.CANCEL, OverrideData.CANCELLED}).iterator();
		while( ovrsToDelete.hasNext() ) {
			OverrideData ovr = (OverrideData)ovrsToDelete.next();
			AbstractOverrideOperation del = getDeleteOverride();
			del.setOverrideId( ovr.getOvrId() );
			del.setWbuNameBoth(wbuName , wbuNameActual);
			ovrBuilder.add( del );
		}*/
	}

	/*    private void addETOverridesToDelete( DBConnection conn, int empId, Date date,
	 AbstractOverrideBuilder ovrBuilder,
	 String wbuName, String wbuNameActual )
	 throws SQLException {
	 OverrideAccess access = new OverrideAccess( conn );
	 Iterator ovrs = access.loadByRangeAndType( empId, date, date,
	 OverrideData.TIMESHEET_TYPE_START,
	 OverrideData.TIMESHEET_TYPE_START ).iterator();
	 while( ovrs.hasNext() ) {
	 OverrideData ovr = (OverrideData)ovrs.next();
	 AbstractOverrideOperation del = getDeleteOverride();
	 del.setOverrideId( ovr.getOvrId() );
	 del.setWbuNameBoth(wbuName , wbuNameActual);
	 ovrBuilder.add( del );
	 }
	 }*/

	protected AbstractOverrideBuilder getOverrideBuilder(DBConnection conn) {
		return new OverrideBuilder(conn);
	}

	protected AbstractOverrideOperation getDeleteOverride() {
		return new DeleteOverride();
	}

	protected AbstractOverrideOperation getInsertOverride() {
		return new InsertOverride();
	}

	private void updateHandsOffDates(DBConnection conn,
			CSMBiWeeklyTimeSheetPage model) throws
			SQLException {
		Date dateHandsOff = null;
		Date supervisorDate = null;
		Date emphistStartDate = null;
		Date emphistEndDate = null;
		HashMap paygroupHandsoffDates = new HashMap();
		HashMap paygroupSupervisorDates = new HashMap();

		//Get the Hands OFF Date
		String sqlQuery = "SELECT PAYGRP_SUPERVISOR_DATE SUPERVISOR_DATE," + // 'yyyyMMdd'
		" PAYGRP_HANDS_OFF_DATE HANDS_OFF_DATE," + // 'yyyyMMdd'
		" EMPLOYEE.EMPHIST_START_DATE, EMPLOYEE.EMPHIST_END_DATE" + // 'yyyyMMdd'
		" FROM EMPLOYEE_HISTORY EMPLOYEE, PAY_GROUP " +
		" WHERE EMPLOYEE.EMP_ID = ?" +
		" AND EMPLOYEE.PAYGRP_ID = PAY_GROUP.PAYGRP_ID";
		ResultSet rs = null;
		PreparedStatement stmt = null;
		try {
			stmt = conn.prepareStatement(sqlQuery);
			stmt.setInt(1, model.getEmployee().getEmpId());
			rs = stmt.executeQuery();
			while (rs.next()) {
				dateHandsOff = rs.getDate("HANDS_OFF_DATE");
				supervisorDate = rs.getDate("SUPERVISOR_DATE");
				emphistStartDate = rs.getDate("EMPHIST_START_DATE");
				emphistEndDate = rs.getDate("EMPHIST_END_DATE");
				EmployeeIdStartEndDate key = new EmployeeIdStartEndDate(
						model.getEmployee().getEmpId(),emphistStartDate,emphistEndDate);
				paygroupHandsoffDates.put(key,DateHelper.truncateToDays(dateHandsOff ));
				paygroupSupervisorDates.put(key,DateHelper.truncateToDays(supervisorDate ));
			}
		} finally {
			SQLHelper.cleanUp(stmt, rs);
		}
		model.getEmployee().setPaygroupSupervisorDates(paygroupSupervisorDates);
		model.getEmployee().setPaygroupHandsoffDates(paygroupHandsoffDates);
	}


	/*public static void adjustStartStopTimes(DayScheduleModel dsm) {
	 Date shiftStart = dsm.getWrkStartTime();
	 Date shiftEnd = dsm.getWrkEndTime();
	 Date breakStart = dsm.getBrkStartTime();
	 Date breakEnd = dsm.getBrkEndTime();
	 boolean hasBreak = dsm.hasBreak();
	 boolean rejectBreak = false;

	 if (hasBreak) {
	 int shiftStartEndCompare = shiftStart.compareTo(shiftEnd);
	 int breakStartEndCompare = breakStart.compareTo(breakEnd);

	 // Reject or modify the break times if necessary
	  if (breakStartEndCompare > 0) {
	  breakEnd = DateHelper.addDays(breakEnd, 1);
	  }

	  if (shiftStartEndCompare < 0) {
	  if (breakStart.compareTo(shiftStart) < 0 ||
	  breakEnd.compareTo(shiftEnd) > 0) {
	  rejectBreak = true;
	  }
	  } else if (shiftStartEndCompare > 0) {
	  shiftEnd = DateHelper.addDays(shiftEnd, 1);
	  if (breakStart.compareTo(shiftStart) < 0 ||
	  breakEnd.compareTo(shiftEnd) > 0) {
	  breakStart = DateHelper.addDays(breakStart, 1);
	  breakEnd = DateHelper.addDays(breakEnd, 1);
	  if (breakStart.compareTo(shiftStart) < 0 ||
	  breakEnd.compareTo(shiftEnd) > 0) {
	  rejectBreak = true;
	  }
	  }
	  } else {
	  rejectBreak = true;
	  }

	  // Reject 0-hour breaks.
	   if (breakStartEndCompare == 0) {
	   rejectBreak = true;
	   }
	   } else {
	   rejectBreak = true;
	   }

	   if (shiftEnd.compareTo(shiftStart) < 0) {
	   shiftEnd = DateHelper.addDays(shiftEnd, 1);
	   }

	   dsm.setWrkStartTime(shiftStart);
	   dsm.setWrkEndTime(shiftEnd);
	   if (rejectBreak) {
	   dsm.setBrkStartTime(DateHelper.DATE_1900);
	   dsm.setBrkEndTime(DateHelper.DATE_1900);
	   } else {
	   dsm.setBrkStartTime(breakStart);
	   dsm.setBrkEndTime(breakEnd);
	   }
	   }*/
}

