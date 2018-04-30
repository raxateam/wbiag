package com.wbiag.app.modules.leaverequest;

import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.util.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.*;
import com.workbrain.app.workflow.*;
import com.workbrain.app.ta.ruleengine.*;
import javax.naming.NamingException;
import com.workbrain.security.SecurityService;
import com.workbrain.server.registry.Registry;
import com.workbrain.app.modules.vacationrequest.common.Process;

/** This code in this class was taken from the VacationBalanceAction class
 * and refactored so that it could handle any type of leave, not just VAC.
 * This class' primary funtion is to help validate a leave request, e.g.
 * that the employee has sufficient balance, and that the start/end
 * date/times are valid.
 *
 * @author crector
 *
 */
public class LeaveBalanceHandler extends LeaveProcessCommon {

  public static String VACATION_BALANCE_CALC_INFINITE = "INFINITE";
  public static String VACATION_BALANCE_CALC_YEARLY = "YEARLY";
  public static String VACATION_BALANCE_CALC_REQUEST = "REQUEST";

  public static String WORK_DETAIL = "WORK_DETAIL";
  public static String WRKD_WORK_DATE = "WRKD_WORK_DATE";
  public static String EMP_ID = "EMP_ID";

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveBalanceHandler.class);

  private boolean cancelationDetected = false;
  private boolean requestDetected = false;
  private ArrayList vacationDaysFound = new ArrayList();
  private String vacCancelledIds = null;
  private java.util.Date checkBalanceAsOfDate = null;
  private List willBeCancelledOvrList = null;
  private String vacationBalanceCalcValue = null;
  private boolean estimatesEntitlements = false;
  private boolean estimatesBalanceCascades = false;
  private boolean simulatesCalculation = false;
  private String badTimeCodes = "";

  public LeaveBalanceHandler(DBConnection con) throws Exception {
    super(con);
  }

  public void process() throws Exception {
    if (this.cancelationDetected == true){
      LeaveCanceller cancelVacation = new LeaveCanceller(this.getConnection() ,
          this.vacCancelledIds);
      cancelVacation.setEmployeeInfo(this.getEmpId(), this.getUserData(),
          this.getEmployeeFullName());
      willBeCancelledOvrList = cancelVacation.getCancelOvrList();
      cancelVacation = null;
    }
    if (this.requestDetected == true){
      // *** set retDate to be end of year of 3000 date, depending on registry value
      this.checkBalanceAsOfDate = this.getCheckBalanceAsOfDate();
      // *** here check if startdate is after today
      this.checkStartDate();
      // *** here check if end date is before end of year and after start date
      this.checkEndDate();

      // *** Check that the employee is scheduled. We may want to remove this
      // check in the future, as is being done in the new retail leave request
      // in 5.0.
      this.checkSchedule();

      // *** if there is time check to see if its in between schedule time
      this.checkTimes();

      this.checkMaxDays();

      this.checkLtaDaysInInterval();

      this.checkForBadTimeCodes();

      this.checkEmployeBalance();

      if (simulatesCalculation){
        this.simulateRuleEngineCalculation();
      }
    }
  }


  public String getBadTimeCodes() {
    return badTimeCodes;
  }

  public void setBadTimeCodes(String badTimeCodes) {
    this.badTimeCodes = badTimeCodes;
  }

  public void setVacationBalanceCalcValue(String v){
    vacationBalanceCalcValue = v;
  }

  public String getVacationBalanceCalcValue(){
    return vacationBalanceCalcValue;
  }

  public void setEstimatesEntitlements(boolean b) {
    estimatesEntitlements = b;
  }

  public void setEstimatesBalanceCascades(boolean b) {
    estimatesBalanceCascades = b;
  }
  public void setSimulateCalc(boolean b){
    simulatesCalculation = b;
  }

  public void setDates(java.util.Date start,java.util.Date end){
    super.setDates(start,end);
    this.requestDetected = true;
  }

  public void setOverridesIdsList(String ovrIdsList){
    if ((ovrIdsList == null) || ovrIdsList.trim().equalsIgnoreCase("")){
      return;
    }
    this.vacCancelledIds = ovrIdsList;
    this.cancelationDetected = true;
  }

  void checkMaxDays() throws Exception {

    int requested = 1 + DateHelper.getDifferenceInDays(this.getEndDate(),
        this.getStartDate());
    logger.debug("Leave max days/requested days: " + this.getMaxDays() + "/" +
        requested);
    if (requested > this.getMaxDays()) {
      throw new WorkflowEngineException(
          this.localize(LeaveMsgHelper.MAX_DAYS_VIOLATED) +
          this.getMaxDays());
    }
  }

  /**
   * check whether the start date is greater than current date
   */
  protected void checkStartDate() throws Exception {
    if (this.getStartDateTime() == null) return;

    java.util.Date today = new java.util.Date();
    if (DateHelper.compare(this.getStartDateTime() , today) <= 0){
      this.setError(this.getStartDateTime() + " " +
          this.localize(LeaveMsgHelper.BALANCE_START_DATE_GREATER_THAN_TODAY)
      );
      throw new Exception(this.getError());
    }
  }

  /**
   * check whether the end date is greater than start date
   */
  protected void checkEndDate() throws Exception {
    if (this.getEndDateTime() == null) return;

    if (DateHelper.compare(this.getEndDate(), this.getStartDate()) < 0){
      this.setError(this.localize(LeaveMsgHelper.BALANCE_START_DATE_GREATER_THAN_END_DATE));
      throw new Exception(this.getError());
    }
    if (getVacationBalanceCalcValue().equals(VACATION_BALANCE_CALC_YEARLY)) {
      if (DateHelper.compare(this.getEndDateTime() , this.getEndOfTheYear()) > 0){
        String err =
          this.localize(LeaveMsgHelper.BALANCE_SPAN_TWO_YEARS);
        this.setError(err);
        throw new Exception(this.getError());
      }
    }
  }

  protected void checkTimes() throws Exception {
    // *** start time
    EmployeeScheduleData esd = this.getEmployeeSchedule(this.getStartDate());
    if (esd.isEmployeeScheduledActual()) {
      if (!RuleHelper.between(
          RuleHelper.min(this.getStartDateTimeAdjusted() , esd.getEmpskdActStartTime()),
          esd.getEmpskdActStartTime(),
          esd.getEmpskdActEndTime())) {
        String err =
          this.localize(LeaveMsgHelper.BALANCE_START_TIME_OUTSIDE_INTERVAL)
          + this.getStartDateTime();
        this.setError(err);
        throw new Exception(this.getError());
      }
    }
    // *** all days between start and end are assumed to be from skd start to end
    // *** end time
    esd = this.getEmployeeSchedule(this.getEndDate());
    if (esd.isEmployeeScheduledActual()) {
      if (!RuleHelper.between(
          RuleHelper.max(this.getEndDateTimeAdjusted() , esd.getEmpskdActEndTime()),
          esd.getEmpskdActStartTime(),
          esd.getEmpskdActEndTime())) {
        String err =
          this.localize(LeaveMsgHelper.BALANCE_END_TIME_OUTSIDE_INTERVAL)
          + this.getEndDateTime();
        this.setError(err);
        throw new Exception(this.getError());
      }
    }
  }

  /**
   * check the schedule for the interval start date - end date
   *
   */
  protected void checkSchedule() throws Exception {
    // *** checks whether the employee has at least one working day in the interval
    if (this.scheduledAtLeastOneDay(this.getEmployeeSchedule()) == false){
      this.setError(
          this.localize(LeaveMsgHelper.BALANCE_NOT_SCHEDULED) +
          this.getStartDate() + " - " +
          this.getEndDate());
      throw new Exception(this.getError());
    }
  }

  public java.util.Date getCheckBalanceAsOfDate() throws Exception{
    java.util.Date retDate = DateHelper.DATE_3000;

    String result = getVacationBalanceCalcValue();
    if (result != null) {
      if (result.equalsIgnoreCase(VACATION_BALANCE_CALC_YEARLY)){
        retDate = getEndOfTheYear();
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Balance calculated YEARLY");}
      } else if (result.equalsIgnoreCase(VACATION_BALANCE_CALC_INFINITE)){
        retDate = DateHelper.DATE_3000;
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Balance calculated INFINITE");}
      } else if (result.equalsIgnoreCase(VACATION_BALANCE_CALC_REQUEST)){
        retDate = super.getEndDate();
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Balance calculated REQUEST");}
      } else {
        throw new Exception ("VACATION_BALANCE_CALC value not recognized : " + result);
      }
    }
    return retDate;
  }

  public static java.util.Date getEndOfTheYear(){
    return  DateHelper.getUnitYear(DateHelper.APPLY_ON_LAST_DAY ,  false, DateHelper.getCurrentDate());
  }

  private String getOvrCondition() throws Exception {
    String condition =
      "emp_id = " + String.valueOf(this.getEmpId()) +
      " AND ovrtyp_id >= " + OverrideData.LTA_TYPE_START + " AND ovrtyp_id <= " + OverrideData.LTA_TYPE_END +
      " AND ovr_start_date between " +
      this.getConnection().encodeTimestamp(this.getStartDate()) +
      " AND " + this.getConnection().encodeTimestamp(this.getEndDate()) +
      " AND ovr_status not in ('" + OverrideData.ERROR + "','" +
      OverrideData.CANCELLED  + "') " +
      " ORDER BY ovr_start_date,ovr_order,ovr_create_date";
    return condition;
  }

  /**
   * Throws an exception if there is already an LTA override in the
   * requested interval.
   */
  protected void checkLtaDaysInInterval() throws Exception {
    OverrideAccess oa = new OverrideAccess(this.getConnection());
    OverrideData ovr = new OverrideData();
    java.util.List ovrList = null;
    ovrList = oa.loadRecordData(ovr,Process.OVERRIDE_TABLE,
        this.getOvrCondition());
    if (ovrList != null && ovrList.size() > 0){
      this.setError(localize(LeaveMsgHelper.LEAVE_ALREADY_EXISTS));
      throw new Exception (this.getError());
    }
    ovr = null;
    ovrList.clear();
    ovrList = null;
    oa = null;
  }

  /** Checks for particular "bad" timecodes in the requested date range
   * and throws an exception if any are found.
   *
   * @throws Exception
   */
  private void checkForBadTimeCodes() throws Exception {

    if (this.getBadTimeCodes() == null ||
        "".equals(this.getBadTimeCodes()))
      return;
    logger.debug("checking for bad timecodes: " + this.getBadTimeCodes());

    WorkDetailAccess wda = new WorkDetailAccess(this.getConnection());
    int[] idArray = {this.getEmpId()};
    WorkDetailList wdList = wda.loadByEmpIdsAndDateRange(
        idArray, this.getStartTime(), this.getEndTime(),
        null, null);
    logger.debug("examining " + wdList.size() + " work detail records");
    if ( wdList.size() <= 0 ) return;

    Vector badTcodeIds = buildTcodeIdVector(this.getBadTimeCodes(),
        this.getConnection());

    WorkDetailData wdd;
    Iterator wdIter = wdList.iterator();
    while ( wdIter.hasNext() ) {
      wdd = (WorkDetailData) wdIter.next();
      Integer id = new Integer(wdd.getTcodeId());

      if (badTcodeIds.contains(id)) {
        logger.debug("found bad code (id=" + id + ") in work detail " +
            " record with id " + wdd.getWrkdId());
        throw new Exception(this.localize(LeaveMsgHelper.BAD_TCODE_FOUND) +
            this.getBadTimeCodes());
      }
    }

    logger.debug("no bad timecodes found");
  }

  /** Returns a vector of ID's as Integers
   *
   * @param tcodeNameList - a comma-separated list of time code ID's
   * @param dbCon
   * @return Vector
   */
  private static Vector buildTcodeIdVector(String tcodeIdList,
      DBConnection dbCon) {
    Vector idVector = new Vector();

    String[] tcodes = tcodeIdList.split(",");
    for (int i=0; i < tcodes.length; i++ ) {

      try { idVector.add(new Integer(tcodes[i])); }
      catch (Exception e) {
        logger.warn("timecode ID (value=" + tcodes[i] + ") could not" +
            " be converted to an Integer and will be ignored");
      }
    }

    return idVector;
  }


  protected void checkEmployeBalance() throws Exception {

    EmployeeBalanceCheck empBalanceCheck =
      new EmployeeBalanceCheck(
          this.getConnection(),
          this.getEmpId(),
          this.getOverrideTimeCodeId(),
          this.getStartDateTimeAdjusted(),
          this.getEndDateTimeAdjusted());
    empBalanceCheck.setCheckBalanceAsOfDate(this.checkBalanceAsOfDate);
    empBalanceCheck.setWillBeCancelledOvrList(willBeCancelledOvrList);
    empBalanceCheck.setEstimatesEntitlements(this.estimatesEntitlements);
    empBalanceCheck.setEstimatesBalanceCascades(this.estimatesBalanceCascades);
    empBalanceCheck.execute();
    String err = empBalanceCheck.getErrorMessage();
    if (!StringHelper.isEmpty(err)){
      err = StringHelper.searchReplace(err , "\n" , "<br>");
      this.setError(
          this.localize(LeaveMsgHelper.BALANCE_ERROR_CHECK_BALANCE)
          + "<br> " + this.localizeErrorMessage(err));
      throw new Exception(this.getError());
    }
    empBalanceCheck = null;

  }

  /**
   * The validation performs the vacation override calculations through the
   * rule engine. If the vacation overrides result in issues during the
   * calculation process, the vacation request is rejected.
   * This is activated by the SIMULATE_CALCULATION parameter
   * on the VacationBalance workflow node.
   *
   * @throws Exception
   */
  protected void simulateRuleEngineCalculation() throws Exception {
    LeaveProcessor leaveProc = null;
    leaveProc = new LeaveProcessor(getConnection());
    leaveProc.setDates(this.getStartDate(), this.getEndDate());
    leaveProc.setTimes(this.getStartTime(), this.getEndTime());
    leaveProc.setEmployeeInfo(this.getEmpId() , this.getUserData(),
        this.getEmployeeFullName());
    leaveProc.setPayInAdvance(this.getPayInAdvance());
    leaveProc.setComments(this.getComments());
    leaveProc.setTimeCodeId(this.leaveTimeCodeId);
    //leaveProc.setTimeCodes(this.leaveTimeCodeName, this.unpaidLeaveTimeCodeName);

    if (leaveProc.getPayInAdvance()) {
      leaveProc.setTimeCodeCurrentPaidNotAffectsBalance(
          this.getOverrideTimeCodeNameCurrentPaid());
      leaveProc.setTimeCodeFutureUnpaidAffectsBalance(
          this.getTimeCodeFutureUnpaidAffectsBalance());
      leaveProc.setPayCurrentOverrideTypeId(
          this.getPayCurrentOverrideTypeId());
    }

    leaveProc.process();
    OverrideList procList = leaveProc.getOverridesProcessed();
    OverrideAccess oa = new OverrideAccess(getConnection());
    for (int i = 0; i < procList.size(); i++){
      OverrideData od = procList.getOverrideData(i);
      int odInt = od.getOvrId();
      OverrideData odNow = oa.load(odInt);
      String status = odNow.getOvrStatus();
      if (!status.equals(OverrideData.APPLIED)){
        rollback(this.getConnection());
        throw new Exception("Problems when simulating rule engine "+
            "calculation; not all overrides applied.");
      }
    }
    rollback(this.getConnection());
  }

  protected void rollback(DBConnection c) throws WorkflowEngineException {
    try {
      c.rollback();
    } catch (SQLException e) {
      throw new WorkflowEngineException(e);
    }
  }


}
