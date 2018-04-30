package com.wbiag.app.bo.ejb.actions;

import java.lang.String;
import java.util.Date;

import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.model.PayGroupData;
import com.workbrain.app.ta.db.PayGroupAccess;

import com.wbiag.app.export.payroll.PayrollReadiness;

public class ReadinessPayrollAction
    extends AbstractActionProcess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(ReadinessPayrollAction.class);

    private static final String PAYGRP_FLAG = "PAYGRP_FLAG1";
    private static final String PAYGRP_UDF = "PAYGRP_UDF1";

    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd";
    
    private static final String DEFAULT = "default=";
    
    private static final String PARAM_PAYGRP_ID = "paygrp_ids";
    private static final String PARAM_USE_PP	= "use_pay_period";
    private static final String PARAM_START_DATE = "start_date";
    private static final String PARAM_END_DATE = "end_date";
    
    private static final String PARAM_ACTION_CKUNAUTH = "CHK_UNAUTH";
    private static final String PARAM_ACTION_LOOK_BACK = "LOOK_BACK_DAYS";
    private static final String PARAM_ACTION_LOCK_RECORDS = "LOCK_RECORDS";
    

    public ActionResponse processObject(Action data, WBObject object,
                                        Branch[] outputs,
                                        ActionResponse previous) throws
        WorkflowEngineException {
        DBConnection conn = null;
        try {
            conn = this.getConnection();
            if (WorkflowUtil.isFieldEmpty(object, PARAM_PAYGRP_ID)){
                String comment = "No pay group provided";
                return WorkflowUtil.createActionResponse(outputs, "Failure", comment);
            }
            int paygrpId = Integer.valueOf(WorkflowUtil.getFieldValueId(object,
                PARAM_PAYGRP_ID)).intValue();

            Date startDate = null;
            Date endDate = null;

            boolean usesPayPeriod = WorkflowUtil.isCheckBoxSelected(object,
                PARAM_USE_PP);

            if (usesPayPeriod) {
                //Get Pay Period Dates
                PayGroupAccess pga = new PayGroupAccess(conn);
                PayGroupData pgd = pga.load(paygrpId);

                startDate = pgd.getPaygrpStartDate();
                endDate = pgd.getPaygrpEndDate();

                if (logger.isDebugEnabled()) logger.debug("paygrpid : " + paygrpId);
                if (logger.isDebugEnabled()) logger.debug(startDate + " - " + endDate);

            }
            else {
                startDate = WorkflowUtil.getFieldValueAsDate(object, PARAM_START_DATE);
                endDate = WorkflowUtil.getFieldValueAsDate(object, PARAM_END_DATE);

            }

            int LBDays = 0;
            if (data.propertyExists(PARAM_ACTION_LOOK_BACK)) {
                String LBDaysString = WorkflowUtil.getDataPropertyAsString(
                        data, PARAM_ACTION_LOOK_BACK);
            	if (LBDaysString.indexOf(DEFAULT) != -1) {
            	    String SubS = LBDaysString.substring(LBDaysString.
            	            indexOf(DEFAULT) + 8);
            	    if (SubS.indexOf('~') != -1) {
            	        LBDaysString = SubS.substring(0, SubS.indexOf('~'));
            	    }
            	    else {
            	        LBDaysString = SubS;
            	    }
            	}
            	LBDays = Integer.valueOf(LBDaysString).intValue();
            }
            
            String checkUnauthString = "true";
            if (data.propertyExists(PARAM_ACTION_CKUNAUTH)) {
                checkUnauthString = WorkflowUtil.getDataPropertyAsString(
                        data, PARAM_ACTION_CKUNAUTH);
                
                if (checkUnauthString.indexOf(DEFAULT) != -1) {
                    String SubS = checkUnauthString.substring(checkUnauthString.
                            indexOf(DEFAULT) + 8);
                    if (SubS.indexOf('~') != -1) {
                        checkUnauthString = SubS.substring(0, SubS.indexOf('~'));
                    }
                    else {
                        checkUnauthString = SubS;
                    }
                }
            }
            boolean checkUnauth;
            if (checkUnauthString.equalsIgnoreCase("true")) {
                checkUnauth = true;
            }
            else {
                checkUnauth = false;
            }
            
            PayrollReadiness pr = new PayrollReadiness(conn);
            
            if (checkUnauth) {
                //Check that all records are authorized
                int [] pgIds = new int[1];
                pgIds[0] = paygrpId;
                
                if (!pr.checkAllUnauth( pgIds, startDate,
                                                  endDate, LBDays)) {
                    String comment = "Some records are unauthorized";
                    return WorkflowUtil.createActionResponse(outputs, "Failure", comment);
                }
            }
            String lockRecordsString = "false";
            if (data.propertyExists(PARAM_ACTION_LOCK_RECORDS)) {
                lockRecordsString = WorkflowUtil.getDataPropertyAsString(
                        data, PARAM_ACTION_LOCK_RECORDS);
                
                if (lockRecordsString.indexOf(DEFAULT) != -1) {
                    String SubS = lockRecordsString.substring(lockRecordsString.
                            indexOf(DEFAULT) + 8);
                    if (SubS.indexOf('~') != -1) {
                        lockRecordsString = SubS.substring(0, SubS.indexOf('~'));
                    }
                    else {
                        lockRecordsString = SubS;
                    }
                }
            }
            if (lockRecordsString.equalsIgnoreCase("true")) {
                pr.setLockRecords(true);
            }
            
            //Set PAYGRP FLAG as ready to export
            pr.setFlag(paygrpId);

            this.commit(conn);

        }
        catch (Throwable t) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) {
                logger.error(
                    "com.workbrain.app.bo.ejb.actions.ReadinessPayrollAction.class",
                    t);
            }
            this.rollback(conn);
            throw new WorkflowEngineException(t);
        }
        finally {
            this.close(conn);
        }

        return WorkflowUtil.createActionResponse(outputs, "Success");
    }
}
