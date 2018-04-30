package com.wbiag.app.modules.leaverequest.action;

import com.workbrain.app.bo.*;
import com.workbrain.app.bo.ejb.actions.WorkflowUtil;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import java.sql.*;

import javax.naming.*;
import javax.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.OverrideList;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.modules.vacationrequest.common.MessageLocalizationManager;
import com.wbiag.app.modules.leaverequest.*;
import com.wbiag.app.modules.leaverequest.source.LeaveSourceBuilder;
import com.wbiag.app.ta.model.LeaveData;

public class LeaveProcessAction extends AbstractLeaveAction {

  private static String PAYCURRENT_OVERRIDE_TYPE_ID =
    "PAYCURRENT_OVERRIDE_TYPE_ID";

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveProcessAction.class);

  public ActionResponse processObject(Action data, WBObject object,
      Branch[] outputs, ActionResponse previous)
      throws WorkflowEngineException {

    logger.debug("Beginning processing of leave request");

    BOFormInstance boFormInstance = (BOFormInstance)object;
    DBConnection con = null;
    boolean cancelFailed = false;
    boolean requestFailed = false;
    LeaveProcessor leaveProc = null;
    LeaveCanceller canceller = null;
    boolean errorExists = false;

    try {
      this.setWBObject(object);
      con = this.getConnection();
      LeaveReqFieldsLoader form = new LeaveReqFieldsLoader(object);

      // we only allow the user to do either cancel or create a request, not
      // both. This is enforced in the validation node, so we won't check
      // explicitly for it here (although we'll only execute one option)

      if (form.leaveCancelled()){
        logger.debug("Processing leave request cancellation");
        canceller = new LeaveCanceller(con , form.getOvrIdsList());
        canceller.setEmployeeInfo(this.getEmployeeId() , this.getUserData(),
            this.getEmployeeFullName());
        try {
          canceller.process();
        } catch(Exception e){
          if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
            logger.error(e);
          }
          cancelFailed = true;
          canceller.setError("failed to process the cancellation \n " +
              canceller.getError());
          throw new WorkflowEngineException(e);
        }
      }

      else if (form.requestDetected()){
        logger.debug("Processing new leave request");
        leaveProc = new LeaveProcessor(con);
        leaveProc.setDates(form.getFormStartDate(),form.getFormEndDate());

        //This following statement is related to TT 29876
        leaveProc.setTimes(form.getFormStartTime(),form.getFormEndTime());

        leaveProc.setEmployeeInfo(this.getEmployeeId() , this.getUserData(),
            this.getEmployeeFullName());
        leaveProc.setPayInAdvance(form.getPayInAdvance());
        leaveProc.setComments(form.getComments());

        int leaveId = form.getLeaveId();
        logger.debug("Leave ID = " + leaveId);

        LeaveData leaveData = LeaveSourceBuilder.getLeaveDataById(con, leaveId);
        leaveProc.setTimeCodeId(leaveData.getTcodeId());

        CodeMapper mapper = CodeMapper.createCodeMapper(con);

        String tcodeName = leaveData.getTcodeName(con, mapper);
          //LeaveSourceBuilder.getTcodeNameForLeaveId(con, leaveId);
        leaveProc.setTimeCodeName(tcodeName);
        logger.debug("Leave tcode/ID = " + leaveData.getTcodeId() +
            "/" + tcodeName);

        if (leaveProc.getPayInAdvance()) {
          logger.debug("Processing leave with pay-current-period option");

          leaveProc.setTimeCodeCurrentPaidNotAffectsBalance(
              leaveData.getCurrentTcodeName(con, mapper));
          leaveProc.setTimeCodeFutureUnpaidAffectsBalance(
              leaveData.getFutureTcodeName(con, mapper));

          String sOvrTyp =
            (String) data.getProperty(PAYCURRENT_OVERRIDE_TYPE_ID);
          if (StringHelper.isEmpty(sOvrTyp)) {
            throw new WorkflowEngineException(
              localize(LeaveMsgHelper.OVERRIDE_ID_NULL));
          } else {
            leaveProc.setPayCurrentOverrideTypeId(Integer.parseInt(sOvrTyp));
          }

        }

        try {
          leaveProc.process();
          OverrideList procList = leaveProc.getOverridesProcessed();
          OverrideAccess oa = new OverrideAccess(con);
          for (int i = 0; i < procList.size(); i++){
            OverrideData od = procList.getOverrideData(i);
            int odInt = od.getOvrId();
            OverrideData odNow = oa.load(odInt);
            String status = odNow.getOvrStatus();
            if (!status.equals(OverrideData.APPLIED)){
              errorExists = true;
              break;
            }
          }
        } catch(Exception e){
          requestFailed = true;
          leaveProc.setError("Failed to process the vacation request \n" +
              leaveProc.getError());
          throw new WorkflowEngineException(e);
        }
      }
      if (!errorExists){
        this.commit(con);
      }
      else{
        this.rollback(con);
      }
    } catch(Exception e){
      if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
        logger.error(e);}
      this.rollback(con);
      String err = "";
      if (requestFailed == true){
        err = leaveProc.getError();
      } else if (cancelFailed == true){
        err = canceller.getError();
      } else {
        err = e.getMessage();
      }
      return WorkflowUtil.createActionResponseException(e.getMessage());
    } finally {
      this.close(con);
    }
    if (errorExists){
      return WorkflowUtil.createActionResponse(outputs , FAILURE,
          localize(LeaveMsgHelper.ERROR_PROCESSING_OVERRIDES));
    }
    else{
      return WorkflowUtil.createActionResponse(outputs, SUCCESS) ;
    }
  }

}

