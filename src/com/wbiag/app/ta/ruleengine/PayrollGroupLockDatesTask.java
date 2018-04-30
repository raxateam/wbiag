package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.workbrain.app.scheduler.enterprise.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.security.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * This task goes through payroll_group table definitions and
 * locks employees, supervisors. <p>
 *
 * <p>Copyright: Copyright (c) 2003 Workbrain Inc.</p>
 */

public class PayrollGroupLockDatesTask extends AbstractScheduledJob  {
    private java.util.Date taskDateTime = null;
    private StringBuffer taskLogMessage = new StringBuffer("Scheduled OK.");

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PayrollGroupLockDatesTask.class);

    public static final String PGUDF_TIME_FORMAT = "yyyyMMdd HHmmss";

    public static final String PARAM_PAYROLL_DATES_LOCK_MODE = "PAYROLL_DATES_LOCK_MODE";
    public static final String PARAM_VAL_MODE_LOCK = "LOCK";
    public static final String PARAM_VAL_MODE_UNLOCK = "UNLOCK";
    public static final String PARAM_SUPERVISOR_SEC_GROUPS = "SUPERVISOR_SEC_GROUPS";
    public static final String PARAM_PAYROLL_COORD_SEC_GROUPS = "PAYROLL_COORD_SEC_GROUPS";
    public static final String PARAM_APPLY_PAY_GROUPS = "APPLY_PAY_GROUPS";
    public static final String PARAM_CLIENT_ID = "CLIENT_ID ";
    public static final String PAYGRP_STATUS_UNLOCKED = "UNLOCKED";
    public static final String PAYGRP_STATUS_LOCKED = "LOCKED";

    public PayrollGroupLockDatesTask() {
    }

    public Status run(int taskID, Map param) throws Exception {
        if (logger.isDebugEnabled()) logger.debug("PayrollGroupLockDatesTask.run(" + taskID + ", " + param + ")");

        DBConnection conn = null;

        try {
            conn = getConnection();

            execute(conn ,  param);

            if (!isInterrupted())
                conn.commit();
            else
                conn.rollback();
        } catch (Exception e) {
            if (getLogger().isEnabledFor(org.apache.log4j.Level.ERROR)) { getLogger().error("com.workbrain.app.wbinterfacePayrollGroupLockDatesTask.class", e);}
            if( conn != null ) conn.rollback();
            throw e;
        } finally {
            releaseConnection();
        }
        if (!isInterrupted())
            return jobOk( taskLogMessage.toString() );
        else
            return jobInterrupted("PayrollGroupLockDatesTask task has been interrupted.");
    }


    public String getTaskUI() {
        return "/jobs/wbiag/payrollGroupLockDatesTaskParams.jsp";
    }


    public void execute(DBConnection c , Map param) throws Exception {
        taskDateTime = new java.util.Date();
        if (logger.isDebugEnabled()) logger.debug("PayrollGroupLockDatesTask.execute");

        // *** store current client id
        String currentClientId = null;
        try {
            currentClientId = SecurityService.getCurrentClientId();
            String sClientId = (String) param.get( PARAM_CLIENT_ID );
            if (StringHelper.isEmpty(sClientId)) {
                throw new RuntimeException("Client id has to be supplied");
            }
            int clientId = Integer.parseInt(sClientId);

            SecurityService.setCurrentClientId(String.valueOf(clientId));

            Params params = new Params();
            String sLockMode = (String) param.get( PARAM_PAYROLL_DATES_LOCK_MODE );
            if (!PARAM_VAL_MODE_LOCK.equals(sLockMode)
                && !PARAM_VAL_MODE_UNLOCK.equals(sLockMode)) {
                throw new RuntimeException(PARAM_PAYROLL_DATES_LOCK_MODE + " must be " + PARAM_VAL_MODE_LOCK + " or " + PARAM_VAL_MODE_UNLOCK);
            }
            String sSupGrps = (String) param.get( PARAM_SUPERVISOR_SEC_GROUPS );
            if (StringHelper.isEmpty(sSupGrps)) {
                throw new RuntimeException ("Supervisor Security Groups must be defined") ;
            }
            params.supGrps = StringHelper.detokenizeString(sSupGrps , ",");

            String sPCGrps = (String) param.get( PARAM_PAYROLL_COORD_SEC_GROUPS );
            if (StringHelper.isEmpty(sPCGrps)) {
                throw new RuntimeException ("Payroll Coordinator Security Groups must be defined") ;
            }
            params.pcGrps = StringHelper.detokenizeString(sPCGrps , ",");

            String sPaygrps = (String) param.get( PARAM_APPLY_PAY_GROUPS );
            params.applyPayGroups = StringHelper.detokenizeString(sPaygrps , ",");

            params.taskDatetime = new java.util.Date();

            if (PARAM_VAL_MODE_LOCK.equals(sLockMode)) {
                executeLock(c, params);
            }
            else if (PARAM_VAL_MODE_UNLOCK.equals(sLockMode)) {
                executeUnlock(c, params);
            }

        } finally {
            SecurityService.setCurrentClientId(currentClientId);
        }
    }

    protected static class Params {
        String[] supGrps;
        String[] pcGrps;
        String[] applyPayGroups;
        Date taskDatetime;
    }

    private boolean executeLock(DBConnection c, Params params) throws Exception{
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        PayGroupAccess pga = new PayGroupAccess(c);

        List pgdList = new ArrayList();
        if (params.applyPayGroups == null ||  params.applyPayGroups.length == 0) {
            pgdList = pga.loadRecordData(new PayGroupData(),
                               PayGroupAccess.PAY_GROUP_TABLE, "paygrp_id <> " + RuleHelper.getDefaultPaygrpId(cm));
        }
        else {
            pgdList = pga.loadRecordDataINClause(new PayGroupData(),
                               PayGroupAccess.PAY_GROUP_TABLE,
                               "paygrp_name", params.applyPayGroups, true);
        }
        Iterator iter = pgdList.iterator();
        while (iter.hasNext()) {
            PayGroupData pgd = (PayGroupData) iter.next();
            // *** this is a bug, bean comes as 0
            pgd.setPgcId(null);
            if (logger.isDebugEnabled()) logger.debug("Processing paygroup for lock :" + pgd.getPaygrpName());
            if (StringHelper.isEmpty(pgd.getPaygrpUdf4())
                || PAYGRP_STATUS_UNLOCKED.equals(pgd.getPaygrpUdf4()) ) {
                if (logger.isDebugEnabled()) logger.debug("Pay group :" + pgd.getPaygrpName() + " is unlocked, checking for locks at :" + params.taskDatetime);
                Date empLockTime = null, supLockTime = null, pcLockTime = null;
                boolean updated = false;
                if (!StringHelper.isEmpty(pgd.getPaygrpUdf1())) {
                    empLockTime = DateHelper.
                        convertStringToDate(pgd.getPaygrpUdf1(),  PGUDF_TIME_FORMAT);
                }
                if (!StringHelper.isEmpty(pgd.getPaygrpUdf2())) {
                    supLockTime = DateHelper.
                        convertStringToDate(pgd.getPaygrpUdf2(), PGUDF_TIME_FORMAT);
                }
                if (!StringHelper.isEmpty(pgd.getPaygrpUdf3())) {
                    pcLockTime = DateHelper.
                        convertStringToDate(pgd.getPaygrpUdf3() , PGUDF_TIME_FORMAT);
                }
                if (logger.isDebugEnabled()) logger.debug("empLockTime=" + empLockTime + ",supLockTime" + supLockTime + ",pcLockTime=" + pcLockTime);
                if (pcLockTime != null && params.taskDatetime.after(pcLockTime)) {
                    if (logger.isDebugEnabled()) logger.debug("Locking payroll coordinators");;
                    setWbgLockdown(c, params.pcGrps, "N");
                    updated = true;
                }
                else  if (supLockTime != null && params.taskDatetime.after(supLockTime)) {
                    if (logger.isDebugEnabled()) logger.debug("Locking supervisors");
                    setWbgLockdown(c, params.supGrps, "N");
                    updated = true;
                }
                else  if (empLockTime != null && params.taskDatetime.after(empLockTime )) {
                    if (logger.isDebugEnabled()) logger.debug("Locking employees");
                    pgd.setPaygrpSupervisorDate(DateHelper.DATE_3000 );
                    updated = true;
                }
                if (updated){
                    pgd.setPaygrpUdf4(PAYGRP_STATUS_LOCKED);
                    pga.update(pgd);
                    appendToTaskLogMessage("Processed LOCK for paygroup :" +
                                           pgd.getPaygrpName());
                }
            }
        }
        return true;
    }

    private boolean executeUnlock(DBConnection c, Params params) throws Exception{
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        PayGroupAccess pga = new PayGroupAccess(c);

        List pgdList = pga.loadAll();
        if (params.applyPayGroups == null ||  params.applyPayGroups.length == 0) {
            pgdList = pga.loadRecordData(new PayGroupData(),
                               PayGroupAccess.PAY_GROUP_TABLE, "paygrp_id <> " + RuleHelper.getDefaultPaygrpId(cm));
        }
        else {
            pgdList = pga.loadRecordDataINClause(new PayGroupData(),
                               PayGroupAccess.PAY_GROUP_TABLE,
                               "paygrp_name", params.applyPayGroups, true);
        }

        Iterator iter = pgdList.iterator();
        while (iter.hasNext()) {
            PayGroupData pgd = (PayGroupData) iter.next();
            // *** this is a bug, bean comes as 0
            pgd.setPgcId(null);
            if (logger.isDebugEnabled()) logger.debug("Processing paygroup for unlock:" + pgd.getPaygrpName());
            if (PAYGRP_STATUS_LOCKED.equals(pgd.getPaygrpUdf4()) ) {
                if (logger.isDebugEnabled()) logger.debug("Pay group :" + pgd.getPaygrpName() + " is locked");
                boolean updated = false;
                if (!StringHelper.isEmpty(pgd.getPaygrpUdf3())) {
                    if (logger.isDebugEnabled()) logger.debug("Unlocked payroll coordinators");
                    setWbgLockdown(c, params.pcGrps, "Y");
                    updated = true;
                }
                if (!StringHelper.isEmpty(pgd.getPaygrpUdf2())) {
                    if (logger.isDebugEnabled()) logger.debug("Unlocked supervisors");
                    setWbgLockdown(c, params.supGrps, "Y");
                    updated = true;
                }
                if (!StringHelper.isEmpty(pgd.getPaygrpUdf1())) {
                    if (logger.isDebugEnabled()) logger.debug("Rolled back supervisor date to unlock employees");
                    pgd.setPaygrpSupervisorDate(DateHelper.addDays(pgd.getPaygrpStartDate() , -1 )  );
                    updated = true;
                }
                if (updated){
                    pgd.setPaygrpUdf4(PAYGRP_STATUS_UNLOCKED);
                    pga.update(pgd);
                    appendToTaskLogMessage("Processed UNLOCK for paygroup :" +
                                           pgd.getPaygrpName());
                }
            }
        }
        return true;
    }

    private boolean setWbgLockdown(DBConnection c, String[] wbgs,
                                   String wbgLockdown) throws Exception {
        WorkbrainGroupAccess wga = new WorkbrainGroupAccess(c);
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        for (int i = 0; i < wbgs.length; i++) {
            WorkbrainGroupData wbgData = cm.getWBGroupByName(wbgs[i]);
            wbgData.setWbgLockdown(wbgLockdown);
            wga.updateRecordData(wbgData,
                                 WorkbrainGroupAccess.WORKBRAIN_GROUP_TABLE,
                                 WorkbrainGroupAccess.WORKBRAIN_GROUP_PRI_KEY  ) ;
        }
        return true;
    }

    protected void appendToTaskLogMessage(String s) {
        taskLogMessage.append("<br>" + s);
    }


}
