package com.wbiag.app.modules.leaverequest;


import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.util.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.workflow.*;
import com.workbrain.server.jsp.locale.LocalizationDictionary;
import com.workbrain.server.jsp.locale.ErrorMessageLocalizationException;
import com.workbrain.tool.locale.DataLocException;

public class LeaveProcessCommon {

  private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(Process.class);

  public static final String OVERRIDE_TABLE = "OVERRIDE";
  public static final String EMPLOYEE_SCHEDULE = "EMPLOYEE_SCHEDULE";

  private DBConnection con = null;
  private java.util.Date startDate = null,endDate = null;
  private java.util.Date startTime = null,endTime = null;
  private java.util.Date startDateTime = null,endDateTime = null;
  private int empId = -1,userId = -1;
  private String error = "";
  private String empFullName = "";
  protected int leaveTimeCodeId = -1;
  //private boolean payCurrentPeriod = false;
  //protected int unpaidLeaveTimeCodeId = -1;
  protected String leaveTimeCodeName = "";
  //protected String unpaidLeaveTimeCodeName = "";
  private int currentPaidNotAffectsBalanceTCodeId = -1,
    futureUnpaidAffectsBalanceTCodeId = -1;
  private String currentPaidNotAffectsBalanceTCodeName = "",
    futureUnpaidAffectsBalanceTCodeName = "";
  private String timeCode = "", comments = "";
  private boolean payCurrent = false;
  private EmployeeScheduleAccess esa = null;
  private LeaveMsgHelper messageLocalizer = null;
  private WorkbrainUserData userData = null;
  private List empSdkList = null;
  private CodeMapper codeMapper = null;
  private int payCurrentOverrideTypeId = -1;
  private OverrideList empHols = null;
  private int maxDays = 9999;

  public LeaveProcessCommon(DBConnection con) throws Exception {
    this.con = con;
  }

  public DBConnection getConnection(){
    return this.con;
  }

  protected void log(String str){
    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
      logger.debug(str);
    }
  }

  public void setError(String er){
    this.error = er;
  }

  public String getError(){
    return this.error;
  }

  public void setTimeCodeId(int id) {
    //this.leaveTimeCodeName = paidVacTimeCodeName.trim();
    this.leaveTimeCodeId = id;
  }

  public void setTimeCodeCurrentPaidNotAffectsBalance(
      String currentPaidNotAffectsBalanceTCodeName)
      throws Exception {
    try {
      this.currentPaidNotAffectsBalanceTCodeName =
        currentPaidNotAffectsBalanceTCodeName.trim();
      this.currentPaidNotAffectsBalanceTCodeId =
        getTimeCodeIdByName(currentPaidNotAffectsBalanceTCodeName);
      // *** make sure unpaid time code affects balances
      if (affectsBalances(currentPaidNotAffectsBalanceTCodeId)) {
        String err =
          this.localize(LeaveMsgHelper.PROCESS_PAID_AFFECTS_BALANCES);
        throw new Exception (err);
      }

    } catch(Exception e){
      if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
        logger.error(e);}
      String err = this.localize(LeaveMsgHelper.PROCESS_FAILED_TIME_CODE) +
      "\n" + e.getMessage();
      throw new Exception(err);
    }
  }


  public void setTimeCodeFutureUnpaidAffectsBalance(
      String futureUnpaidAffectsBalanceTCodeName)
      throws Exception {
    try {
      this.futureUnpaidAffectsBalanceTCodeName =
        futureUnpaidAffectsBalanceTCodeName.trim();
      this.futureUnpaidAffectsBalanceTCodeId =
        getTimeCodeIdByName(futureUnpaidAffectsBalanceTCodeName);
      // *** make sure unpaid time code affects balances
      if (!affectsBalances(futureUnpaidAffectsBalanceTCodeId)) {
        String err = this.localize(
            LeaveMsgHelper.PROCESS_UNPAID_DOESNOT_AFFECT_BALANCES);
        throw new Exception (err);
      }
    } catch(Exception e){
      if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
        logger.error(e);
      }
      String err = this.localize(LeaveMsgHelper.PROCESS_FAILED_TIME_CODE) +
      "\n" + e.getMessage();
      throw new Exception(err);
    }
  }

  public String getTimeCodeFutureUnpaidAffectsBalance() {
    return this.futureUnpaidAffectsBalanceTCodeName;
  }

  protected java.util.List getEmployeeSchedule()
  throws Exception{
    createEmployeeSchedule();
    return empSdkList;
  }

  protected EmployeeScheduleData getEmployeeSchedule(java.util.Date workDate)
  throws Exception{
    createEmployeeSchedule();
    Iterator it = empSdkList.iterator();
    while (it.hasNext()) {
      EmployeeScheduleData esd = (EmployeeScheduleData) it.next() ;
      if (DateHelper.equals(esd.getWorkDate() ,
            DateHelper.truncateToDays(workDate))) {
        return esd;
      }
    }
    return null;
  }



  protected java.util.Date truncDate(java.util.Date date){
    return DateHelper.truncateToDays(date);
  }

  protected int getOverrideTimeCodeId(){
    return this.leaveTimeCodeId;
  }

  public int getMaxDays() {
    return maxDays;
  }

  public void setMaxDays(int maxDays) {
    this.maxDays = maxDays;
  }

  public void setComments(String com){
    this.comments = com;
  }

  protected String getComments(){
    return this.comments;
  }

  public void setTimeCodeName(String name) {
    this.leaveTimeCodeName = name;
  }

  /** Will return the future timecode name (e.g. VAC_ADV) if this
   * is a pay-current-period type of leave request. Otherwise the
   * regular time code name (e.g. VAC) will be returned.
   *
   * @return time code name
   */
  protected String getOverrideTimeCodeName() {
    if ( getPayInAdvance() )
      return this.futureUnpaidAffectsBalanceTCodeName;
    else
      return this.leaveTimeCodeName;
  }

  protected String getOverrideTimeCodeNameCurrentPaid(){
    return this.currentPaidNotAffectsBalanceTCodeName;
  }

  public void setPayInAdvance(boolean vt){
    this.payCurrent = vt;
  }

  public boolean getPayInAdvance() {
    return this.payCurrent;
  }

  public void setEmployeeInfo(int empId , WorkbrainUserData userData,
      String empFullName){
    this.empId = empId;
    this.userId = userData.getWbuId();
    this.userData = userData;
    this.empFullName = empFullName;
    if (this.empFullName == null)
      this.empFullName = "";
  }

  protected String getEmployeeFullName(){
    return this.empFullName;
  }

  protected int getEmpId(){
    return this.empId;
  }

  protected String getUserName(){
    return this.userData.getWbuName();
  }

  protected WorkbrainUserData getUserData(){
    return this.userData;
  }

  public void setDates(java.util.Date start,java.util.Date end){
    this.startDate = start;
    this.endDate = end;
    setDateTimes();
  }

  public java.util.Date getStartDate(){
    return this.startDate;
  }

  public java.util.Date getEndDate(){
    return this.endDate;
  }

  public void setTimes(java.util.Date start , java.util.Date end){
    this.startTime = start;
    this.endTime = end;
    setDateTimes();
  }

  public java.util.Date getStartTime(){
    return this.startTime;
  }

  public java.util.Date getEndTime(){
    return this.endTime;
  }

  /**
   * Sets the dateTime values which keeps the combined date and times for
   * convenience
   */
  private void setDateTimes(){
    this.startDateTime = (startTime != null)
    ? DateHelper.setTimeValues(startDate , startTime)
        : startDate;
    this.endDateTime = (endTime != null)
    ? DateHelper.setTimeValues(endDate , endTime)
        : endDate;
  }

  public java.util.Date getStartDateTime(){
    return this.startDateTime;
  }

  public java.util.Date getEndDateTime(){
    return this.endDateTime;
  }


  /**
   * If end date has no time component, return skd end where necessary
   */
  public java.util.Date getStartDateTimeAdjusted() throws Exception{
    if (this.startTime == null) {
      EmployeeScheduleData esd = getEmployeeSchedule(this.startDate);
      if (esd.isEmployeeScheduledActual()) {
        return esd.getEmpskdActStartTime();
      }
    }
    return this.startDateTime;
  }

  /**
   * If end date has no time component, return skd end where necessary
   */
  public java.util.Date getEndDateTimeAdjusted() throws Exception{
    if (this.endTime == null) {
      EmployeeScheduleData esd = getEmployeeSchedule(this.endDate);
      if (esd.isEmployeeScheduledActual()) {
        return esd.getEmpskdActEndTime();
      }
    }
    return this.endDateTime;
  }

  protected String localize(String wbmldtCode) throws SQLException{
    if (this.messageLocalizer == null){
      messageLocalizer =
        new LeaveMsgHelper(this.getConnection(),
            this.userData.getWbllId());
    }
    return messageLocalizer.localize(wbmldtCode);
  }

  protected String localizeErrorMessage(String message)
      throws DataLocException, ErrorMessageLocalizationException{
    return LocalizationDictionary.localizeErrorMessage(
        this.getConnection(),message,this.userData.getWbllId());

  }

  protected void calculateScheduleForRequest() throws Exception {
    RuleEngine.runCalcGroup(this.con , this.empId , this.startDate,
        this.endDate , false);
    if (this.getPayInAdvance()) {
      RuleEngine.runCalcGroup(this.con , this.empId ,
          DateHelper.getCurrentDate() ,
          DateHelper.getCurrentDate() , false);
    }
  }

  protected void calculateScheduleForCancel(List cancelOvrList)
      throws Exception {
    int size = cancelOvrList.size();
    for (int i = 0; i < size; i++) {
      OverrideData ovrData = (OverrideData)cancelOvrList.get(i);
      RuleEngine.runCalcGroup(this.con , this.empId ,
          ovrData.getOvrStartDate() , ovrData.getOvrEndDate());
    }
  }

  protected String getCondition(java.util.Date workDate){
    String condition =
      "emp_id=" + String.valueOf(this.getEmpId()) +
      " and work_date between " +
      this.getConnection().encodeTimestamp(this.getStartDate()) +
      " and " +
      this.getConnection().encodeTimestamp(this.getEndDate());
    if (workDate != null){
      condition =
        "emp_id=" + String.valueOf(this.getEmpId()) +
        " and work_date=" +
        this.getConnection().encodeTimestamp(workDate);
    }
    return condition;
  }

  protected boolean scheduledAtLeastOneDay(java.util.List schedList){
    boolean scheduledToWorkInPeriod = false;
    EmployeeScheduleData schedData = null;
    for (int i = 0,size = schedList.size(); i < size; i++) {
      schedData = (EmployeeScheduleData)schedList.get(i);
      if (schedData.isEmployeeScheduledActual()){
        scheduledToWorkInPeriod = true;
        break;
      }
    }
    schedData = null;
    return scheduledToWorkInPeriod;
  }

  protected void createDefaultRecords() throws Exception {
    int[] employees = {this.empId};
    com.workbrain.app.ta.ruleengine.CreateDefaultRecords cdr =
      new com.workbrain.app.ta.ruleengine.CreateDefaultRecords(
          this.getConnection(),employees,this.startDate,this.endDate);
    cdr.execute();
    cdr.executeCalculate(true);
  }

  public int getTimeCodeIdByName(String tcodeName) throws SQLException{
    TimeCodeData tcd = getCodeMapper().getTimeCodeByName(tcodeName);
    if (tcd != null) {
      return tcd.getTcodeId();
    } else {
      throw new SQLException("Time code : " + tcodeName + " not found");
    }
  }

  public void setPayCurrentOverrideTypeId(int ovrtypId)  {
    this.payCurrentOverrideTypeId = ovrtypId;
  }

  public int getPayCurrentOverrideTypeId()  {
    return this.payCurrentOverrideTypeId ;
  }

  protected void createEmployeeSchedule() throws Exception {
    // *** checks whether the records in employee_schedule have been generated
    // *** this also creates the schedules if they  do not exist
    if (empSdkList != null) {
      return;
    }
    empSdkList = getEmployeeScheduleAccess().loadByDateRange(this.empId ,
        this.getStartDate() , this.getEndDate() );

    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
      logger.debug("empSdkList.size()" + empSdkList.size() +
          "- days between : " +
          (DateHelper.getDifferenceInDays(this.getEndDate() ,
              this.getStartDate()) + 1));}
    if (empSdkList == null
        || empSdkList.size() == 0
        || (empSdkList.size() != (DateHelper.getDifferenceInDays(
            this.getEndDate() , this.getStartDate()) + 1)) ){
      this.setError(
          this.localize(LeaveMsgHelper.COMMON_FAILED_CHECK_SCHEDULE));
      throw new Exception(this.getError());
    }
  }

  protected  EmployeeScheduleAccess getEmployeeScheduleAccess()
      throws SQLException{
    if (this.esa == null) {
      this.esa = new EmployeeScheduleAccess(this.getConnection(),
          getCodeMapper());
    }
    return this.esa;
  }

  protected  CodeMapper getCodeMapper() throws SQLException{
    if (this.codeMapper == null) {
      this.codeMapper =CodeMapper.createCodeMapper(this.getConnection());
    }
    return this.codeMapper;
  }

  private boolean affectsBalances(int tcodeId) throws SQLException{
    TimeCodeData tcd = getCodeMapper().getTimeCodeById(tcodeId);
    List balancesToCheck = getCodeMapper().getTCBByTimeCodeId(
        tcodeId);
    return (tcd.getTcodeAffectsBalances() != null &&
        tcd.getTcodeAffectsBalances().equals("Y")
        && balancesToCheck.size() > 0);
  }

  protected boolean isHoliday(EmployeeScheduleData empSched)
  throws SQLException
  {
    // Retrieve all holiday overrides for the employee, if necessary
    if (empHols == null) {
      OverrideAccess oa = new OverrideAccess(getConnection());
      empHols = oa.loadAffectingByRangeAndType(empId, getStartDate(),
          getEndDate(),
          OverrideData.HOLIDAY_TYPE_START, OverrideData.HOLIDAY_TYPE_END);
    }

    // Check whether the scheduled day is actually a holiday
    OverrideList overrides = empHols.filter(empSched.retrieveStartDate(),
        empSched.retrieveEndDate());
    return !overrides.isEmpty();
  }



}
