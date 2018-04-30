package com.wbiag.app.jsp.action.biweeklytimesheet;

public interface CSMBiWeeklyTimeSheetConstants {
	public static final int DAYS_ON_TIMESHEET = 14;	//14 days for bi-weekly
	public static final int ONE_WEEK = 7;
	public static final String ACTION_URL = "/timesheet/CSMbiwtsAction.jsp";
	public static final String SELECTION_JSP_URL = "/timesheet/biweeklySelection.jsp";
	public static final String ACTION_LOAD_EMPLOYEE = "CSMLoadEmployeeAction";
	public static final String ACTION_NEW_EMPLOYEE = "CSMNewEmployeeAction";
	public static final String ACTION_VIEW_TIMESHEET = "CSMViewBiWeeklyTimeSheetAction";
	public static final String ACTION_SAVE_TIMESHEET = "CSMSaveBWTimeSheetAction";
	public static final String ACTION_SUBMIT_TIMESHEET = "CSMSubmitBWTimeSheetAction";
	public static final String ACTION_SUBMIT_TO_SUPERVISOR = "CSMBWSubmitToSupervisorAction";

	public static final String JOB_DELIMITER = ",\n";
	public static final String WBREG_CHECK_FOR_EARLIEST_UNSUBMITTED = "/system/wbiag/biweeklytimesheet/CHECK_FOR_EARLIEST_UNSUBMITTED";
	public static final String DAY_START_TIME = "/system/WORKBRAIN_PARAMETERS/DAY_START_TIME";

	public static final String EMP_SUBMIT_VERBIAGE = "I hereby certify I am the employee of record for this time collection form ...";
	public static final String EMP_SAVE_VERBIAGE = "Clicking on the Save For Now button will save your current work so that you may continue to enter time for this pay period at it at a later time ...";
}
