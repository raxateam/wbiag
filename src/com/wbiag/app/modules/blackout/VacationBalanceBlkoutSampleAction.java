package com.wbiag.app.modules.blackout;

import com.workbrain.app.bo.*;
import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import java.util.*;
import javax.naming.*;
import javax.sql.*;
import com.workbrain.app.modules.vacationrequest.common.*;
import com.workbrain.app.modules.vacationrequest.actions.*;
import com.workbrain.util.*;
/**
 * Example on how blackout dates can be utilized in Vacation Request
 */
public class VacationBalanceBlkoutSampleAction extends AbstractVacationAction {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(VacationBalanceBlkoutSampleAction.class);

    public ActionResponse processObject(Action data, WBObject object, Branch[] outputs,
        ActionResponse previous) throws WorkflowEngineException {
        DBConnection con = null;
        VacationBalance vacBalance = null;
        try {
            con = this.getConnection();
            this.setWBObject(object);
            FormFieldsLoader form = new FormFieldsLoader(object);
            vacBalance = new VacationBalance(con);
            vacBalance.setEmployeeInfo(this.getEmployeeId() , this.getUserData(),
                this.getEmployeeFullName());
            vacBalance.setTimeCodes((String)data.getProperty("PAID_VACATION"),
                (String)data.getProperty("UNPAID_VACATION"));
            String vacBalCalc = WorkflowUtil.getDataPropertyAsString(data , "VACATION_BALANCE_CALC");
            if (StringHelper.isEmpty(vacBalCalc)) {
                throw new WorkflowEngineException("VACATION_BALANCE_CALC parameter cannot be null");
            }
            vacBalance.setVacationBalanceCalcValue(vacBalCalc);
            String sEstEnt = WorkflowUtil.getDataPropertyAsString(data ,"ESTIMATE_ENTITLEMENTS");
            boolean estEnt = sEstEnt != null && sEstEnt.equals("TRUE") ? true : false;
            vacBalance.setEstimatesEntitlements(estEnt);
            String sBalCasc = WorkflowUtil.getDataPropertyAsString(data ,"ESTIMATE_BALANCE_CASCADES");
            boolean estBalCasc = sBalCasc != null && sBalCasc.equals("TRUE") ? true : false;
            vacBalance.setEstimatesBalanceCascades(estBalCasc);
            String sSimulateCalc = WorkflowUtil.getDataPropertyAsString(data, "SIMULATE_CALCULATION");
				boolean simCalc = sSimulateCalc != null && sSimulateCalc.equalsIgnoreCase("TRUE") ? true : false;
				vacBalance.setSimulateCalc(simCalc);

            if (form.requestDetected()){
                vacBalance.setDates(form.getFormStartDate(),form.getFormEndDate());
                // *** check blackout date
                checkBlackoutDates(this.getEmployeeId(), form.getFormStartDate(),form.getFormEndDate());
                vacBalance.setTimes(form.getFormStartTime(),form.getFormEndTime());
                vacBalance.setVacationType(form.getVacationType());
                if (vacBalance.isVacationTypePayCurrent()) {
                    vacBalance.setTimeCodeFutureUnpaidAffectsBalance(
                        (String)data.getProperty("FUTURE_UNPAID_AFFECTS_BALANCE_CODE"));
                }
            }
            if (form.vacationCancelled()){
                vacBalance.setOverridesIdsList(form.getOvrIdsList());
            }
            vacBalance.process();
            this.commit(con);
        } catch(Exception e){
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("com.workbrain.app.modules.vacationrequest.actions.VacationBalanceBlkoutSampleAction.class" , e);}
            this.rollback(con);
            return WorkflowUtil.createActionResponseException(e.getMessage());
        } finally {
            this.close(con);
        }
        return WorkflowUtil.createActionResponse(outputs , this.SUCCESS) ;
    }

    private void checkBlackoutDates(int empId, Date startDate, Date endDate)
        throws Exception  {
        WbiagBlkoutAccess acc = new WbiagBlkoutAccess(getConnection());
        List blDates = acc.getEmployeeBlackoutDates(empId, startDate, endDate,
            acc.IS_EMP_HOME);
        if (blDates.size() > 0) {
            throw new WorkflowEngineException ("Employee is subject to blackout dates for the requested dates");
        }
    }

}