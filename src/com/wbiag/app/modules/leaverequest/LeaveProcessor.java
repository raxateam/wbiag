package com.wbiag.app.modules.leaverequest;

import com.workbrain.server.registry.Registry;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import java.text.SimpleDateFormat;
import java.util.*;

import com.workbrain.app.ta.model.TimeCodeData;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;

/** The code in this class was taken from the core VacationProcess class.
 * The functionality remains the same, although now this class extends
 * the LeaveProcessCommon class, which allows us to handle leave types other
 * than vacation.
 *
 * @author crector
 *
 */
public class LeaveProcessor extends LeaveProcessCommon {

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveProcessor.class);

  private static final String REG_SKIP_PUBLIC_HOLIDAYS =
    "/system/modules/vacation_request/SKIP_PUBLIC_HOLIDAYS";

  private EmployeeScheduleData schedData = null;
  private OverrideBuilder ob = null;
  private boolean skipPublicHolidays;
  private SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");

  public LeaveProcessor(DBConnection con) throws Exception {
    super(con);
    this.skipPublicHolidays =
      Registry.getVarBoolean(REG_SKIP_PUBLIC_HOLIDAYS, false);
  }

  public void process() throws Exception {
    ob =  new OverrideBuilder(this.getConnection());
    this.processOverrides();
  }

  /**
   * Returns overrides created by <code>process</code>. Must be called
   * after <code>process</code>.
   * @return
   */
  public OverrideList getOverridesProcessed() {
    return ob == null ? null : ob.getOverridesProcessed();
  }

  private void processOverrides() throws Exception {

    try {
      this.createOverrides(this.getEmployeeSchedule());
      ob.setCreatesDefaultRecords(true);
      ob.execute(true , false);
      if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
        logger.debug("Successfully INSERTED " + ob.getUpdateCount() +
        " override(s)");}
    } catch(Exception e){
      if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
        logger.error(e);
      }
      this.setError("Failed to insert LTA overrides: \n" + e.getMessage());
      throw new Exception(this.getError());
    }

  }

  private void createOverrides(java.util.List schedList) throws Exception {

    Date ovrStartTime = null;
    Date ovrEndTime = null;

    for (int i = 0,size = schedList.size(); i < size; i++) {

      schedData = (EmployeeScheduleData)schedList.get(i);
      schedData.setCodeMapper(getCodeMapper());
      Date ovrStartDate = schedData.getWorkDate();
      Date ovrEndDate = schedData.getWorkDate();

      if ( !schedData.isEmployeeScheduledActual() ) {
        // We do check that the employee is scheduled when they submit their
        // request, but they do not have to be scheduled for every day
        // in the range they requested. We'll log a warning at least, since
        // I think the usual scenario is to have the employee scheduled for
        // each day in the range.
        logger.warn("Employee is not scheduled for date " +
          schedData.getWorkDate() + ". No leave override will be inserted.");
        continue;
      }

      // if it's not a holiday, then create the override
      if( !(skipPublicHolidays && isHoliday(schedData)) ) {


        if (this.getStartTime() != null && this.getEndTime() != null) {
          ovrStartTime = RuleHelper.max(schedData.getEmpskdActStartTime() ,
              DateHelper.setTimeValues(schedData.getWorkDate() ,
                  this.getStartTime()));
          ovrEndTime = RuleHelper.min(schedData.getEmpskdActEndTime(),
              DateHelper.setTimeValues(schedData.getWorkDate() ,
                  this.getEndTime()));
          // if the end time is before the start time, we assume the end
          // time is the next day
//          if ( this.getEndTime().before(this.getStartTime()) ) {
//            Calendar tmpCal = DateHelper.toCalendar(ovrStartDate);
//            tmpCal.set(Calendar.DAY_OF_YEAR,
//                tmpCal.get(Calendar.DAY_OF_YEAR)+1);
//            ovrEndDate = tmpCal.getTime();
//            logger.debug("Override end date moved one day ahead to " +
//                sdf.format(ovrEndDate));
//          }
        }
        else {
          ovrStartTime = RuleHelper.max(schedData.getEmpskdActStartTime(),
              this.getStartDateTimeAdjusted());
          ovrEndTime = RuleHelper.min(schedData.getEmpskdActEndTime(),
              this.getEndDateTimeAdjusted());
        }
        int premOvrId = -1;
        if (getPayInAdvance()) {
          premOvrId = getConnection().getDBSequence(OverrideAccess.
              OVERRIDE_SEQ).getNextValue();
          InsertWorkPremiumOverride ioPrem =
            new InsertWorkPremiumOverride(getConnection());
          ioPrem.setEmpId(this.getEmpId());
          ioPrem.setStartDate(DateHelper.getCurrentDate());
          ioPrem.setEndDate(DateHelper.getCurrentDate());
          ioPrem.setOvrComment(this.getComments());
          ioPrem.setOvrType(getPayCurrentOverrideTypeId());
          ioPrem.setWbuName(this.getUserName());
          // *** do not remove same code premiums
          ioPrem.setRemovesSameTimeCodePremiums(Boolean.FALSE);
          int skdMinutes = schedData.
          retrieveScheduleDuration(getConnection(),
              ovrStartTime,
              ovrEndTime, true);
          log("Calculated scheduled minutes : " + skdMinutes +
              " for pay current");
          ioPrem.setWrkdMinutes(skdMinutes);
          ioPrem.setWrkdTcodeName(this.getOverrideTimeCodeNameCurrentPaid());
          ioPrem.setOverrideId(premOvrId);
          ob.add(ioPrem);
        }
        InsertWorkDetailOverride io =
          new InsertWorkDetailOverride(getConnection());
        io.setEmpId(this.getEmpId());
        io.setStartDate(ovrStartDate);
        io.setEndDate(ovrEndDate);
        io.setStartTime(ovrStartTime);
        io.setEndTime(ovrEndTime);
        io.setOvrComment(this.getComments());
        io.setOvrType(OverrideData.LTA_TYPE_START);
        io.setWbuName(this.getUserName());
        io.setWrkdTcodeName(this.getOverrideTimeCodeName());
        /* ovr udf1 holds the related prem override id, if created */
        if (premOvrId != -1) {
          io.setOvrUdf1(String.valueOf(premOvrId));
        }
        ob.add(io);
      }
    }
  }

  public static void main (String[] args) throws Exception {
    java.util.Date start = DateHelper.parseSQLDate("2004-01-16 00:00:00");
    java.util.Date end = DateHelper.parseSQLDate("2004-01-16 00:00:00");
    java.util.Date startT = DateHelper.parseSQLDate("1900-00-00 10:00:00");
    java.util.Date endT = DateHelper.parseSQLDate("1900-00-00 16:00:00");

    final DBConnection c = com.workbrain.sql.SQLHelper.connectToDevl();
    c.setAutoCommit( false );
    long s = System.currentTimeMillis();
    com.workbrain.security.SecurityService.setCurrentClientId("1");
    WorkbrainSystem.bindDefault(
        new com.workbrain.sql.SQLSource () {
          public java.sql.Connection getConnection()
              throws java.sql.SQLException {
            return c;
          }
        }
    );

    WorkbrainUserAccess wua = new WorkbrainUserAccess(c);

    if (1==1) {
      LeaveCanceller can = new LeaveCanceller(c, "426232");
      can.setEmployeeInfo(3, wua.loadByEmpId(3), "FULL NAME");
      can.process();
    }
    if (1==2) {
      LeaveProcessor vacProc = new LeaveProcessor(c);
      vacProc.setEmployeeInfo(3, wua.loadByEmpId(3), "FULL NAME");
      vacProc.setTimeCodeId(3); //TODO: set as needed
      vacProc.setTimeCodeCurrentPaidNotAffectsBalance("VAC-PAY");
      vacProc.setTimeCodeFutureUnpaidAffectsBalance("VAC-ADV");
      vacProc.setDates(start, end);
      vacProc.setTimes(null, null);
      vacProc.setPayInAdvance(false); //TODO: set as needed
      vacProc.setPayCurrentOverrideTypeId(201);
      vacProc.setComments("Testing");
      vacProc.process();
    }
    c.commit();
  }
}
