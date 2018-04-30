package com.wbiag.app.modules.leaverequest.action;


import com.workbrain.app.bo.ejb.actions.WorkflowUtil;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import com.wbiag.app.modules.leaverequest.LeaveBalanceHandler;
import com.wbiag.app.modules.leaverequest.LeaveMsgHelper;
import com.wbiag.app.modules.leaverequest.LeaveReqFieldsLoader;
import com.wbiag.app.modules.leaverequest.source.LeaveSourceBuilder;
import com.wbiag.app.ta.model.LeaveData;
import com.workbrain.util.*;


/** The code in this class is based on the code in the core
 * VacationBalanceAction class but refactored to allow requests other
 * than just vacation. This class is responsible for validating the leave
 * request and is used by the first node in the leave request workflow.
 *
 * @author crector
 *
 */
public class LeaveValidationAction extends AbstractLeaveAction {

  // Properties of the workflow node:
  private static String VACATION_BALANCE_CALC = "VACATION_BALANCE_CALC";
  private static String ESTIMATE_ENTITLEMENTS = "ESTIMATE_ENTITLEMENTS";
  private static String SIMULATE_CALCULATION = "SIMULATE_CALCULATION";
  private static String ESTIMATE_BALANCE_CASCADES =
    "ESTIMATE_BALANCE_CASCADES";

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveValidationAction.class);

  public ActionResponse processObject(Action data, WBObject object,
      Branch[] outputs,
      ActionResponse previous) throws WorkflowEngineException {

    logger.debug("Beginning leave validation");

    DBConnection con = null;
    LeaveBalanceHandler balHandler = null;

    try {
      con = this.getConnection();
      this.setWBObject(object);
      LeaveReqFieldsLoader form = new LeaveReqFieldsLoader(object);

      // We don't allow the user to created and cancel a request at the
      // same time. Ideally this would be enforced in the form (i.e.
      // via javascript)
      if (form.leaveCancelled() && form.requestDetected() ) {
        throw new WorkflowEngineException(localize(
            LeaveMsgHelper.ONE_OPP_ALLOWED));
      }

      balHandler = new LeaveBalanceHandler(con);
      balHandler.setEmployeeInfo(this.getEmployeeId() ,
        this.getUserData(), this.getEmployeeFullName());

      if (form.leaveCancelled()){
        balHandler.setOverridesIdsList(form.getOvrIdsList());
      }
      else {

        int leaveId = form.getLeaveId();
        logger.debug("Leave ID = " + leaveId);

        LeaveData leaveData = LeaveSourceBuilder.getLeaveDataById(con,
            leaveId);
        balHandler.setTimeCodeId(leaveData.getTcodeId());

        String vacBalCalc = WorkflowUtil.getDataPropertyAsString(data ,
            VACATION_BALANCE_CALC);
        if (StringHelper.isEmpty(vacBalCalc)) {
          throw new WorkflowEngineException(VACATION_BALANCE_CALC +
          " parameter cannot be null");
        }
        balHandler.setVacationBalanceCalcValue(vacBalCalc);

        String estEntField = WorkflowUtil.getDataPropertyAsString(data ,
            ESTIMATE_ENTITLEMENTS);
        boolean estEnt = (estEntField != null &&
            estEntField.equalsIgnoreCase("TRUE") );
        balHandler.setEstimatesEntitlements(estEnt);

        String sBalCasc = WorkflowUtil.getDataPropertyAsString(data ,
            ESTIMATE_BALANCE_CASCADES);
        boolean estBalCasc = ( sBalCasc != null &&
            sBalCasc.equalsIgnoreCase("TRUE") );
        balHandler.setEstimatesBalanceCascades(estBalCasc);

        String sSimulateCalc = WorkflowUtil.getDataPropertyAsString(data,
            SIMULATE_CALCULATION);
        boolean simCalc = ( sSimulateCalc != null &&
            sSimulateCalc.equalsIgnoreCase("TRUE") );
        balHandler.setSimulateCalc(simCalc);

        if (form.requestDetected()){
          balHandler.setDates(form.getFormStartDate(),form.getFormEndDate());
          balHandler.setTimes(form.getFormStartTime(),form.getFormEndTime());
          balHandler.setPayInAdvance(form.getPayInAdvance());
        }

        balHandler.setMaxDays(leaveData.getWglveMaxDays());

        balHandler.setBadTimeCodes(leaveData.getWglveBadTcodes());
      }

      balHandler.process();
      this.commit(con);
    } catch(Exception e){
      if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
        logger.error(e);
      }
      this.rollback(con);
      return WorkflowUtil.createActionResponseException(e.getMessage());
    } finally {
      this.close(con);
    }
    return WorkflowUtil.createActionResponse(outputs , this.SUCCESS) ;
  }

}