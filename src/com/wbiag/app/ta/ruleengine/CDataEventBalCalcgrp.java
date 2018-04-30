package com.wbiag.app.ta.ruleengine;
 
import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
import com.wbiag.app.ta.ruleengine.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.wbinterface.db.*;
import java.util.*;
/**
 * Custom event for BalCalcgrp min/max checks and time code affects
 */
public class CDataEventBalCalcgrp extends DataEvent {

    public void beforeSave(WBData data, DBConnection c)   {
        processWorkDetailBalanceChanges(data , c);
        checkBalCalcgrpMinMax(data , c);
    }

    protected void processWorkDetailBalanceChanges(WBData data, DBConnection c)   {
        WorkDetailList details = null;
        for (int k = 0; k < 2; k++) {
            details = (k == 0 ? data.getRuleData().getWorkDetails() :
                       data.getRuleData().getWorkPremiums());
            processWorkDetailBalanceChanges(data , details, c);
        }
    }

    protected void processWorkDetailBalanceChanges(WBData data, WorkDetailList details,
        DBConnection c)   {
        HashMap timeCodes = new HashMap();
        for (int i = 0; i < details.size(); i++) {
            WorkDetailData wd = details.getWorkDetail(i);
            TimeCodeData tcd = data.getCodeMapper().getTimeCodeById(wd.getTcodeId());
            if ("Y".equalsIgnoreCase(tcd.getTcodeAffectsBalances())) {
                int wdTime = 0;
                if ("D".equals(wd.getWrkdType())) {
                    wdTime = (int) ((wd.getWrkdEndTime().getTime() - wd.getWrkdStartTime().getTime()) /
                            DateHelper.MINUTE_MILLISECODS);
                } else {
                    // *** Premiums don't have start and end times set ***
                    wdTime = wd.getWrkdMinutes();
                }

                Integer timeInTimeCode = (Integer) timeCodes.get(tcd.getTcodeName());
                if(timeInTimeCode==null) {
                    timeInTimeCode=new Integer(wdTime);
                } else {
                    timeInTimeCode = new Integer(timeInTimeCode.intValue() + wdTime);
                    timeCodes.remove(tcd.getTcodeName());
                }
                timeCodes.put(tcd.getTcodeName(), timeInTimeCode);
            }
        }
        BalanceCalcgrpCache cache = BalanceCalcgrpCache.getInstance();
        Iterator tcodesIterator = timeCodes.keySet().iterator();
        while (tcodesIterator.hasNext()) {
            String tcodeName = (String) tcodesIterator.next();
            TimeCodeData tcd = data.getCodeMapper().getTimeCodeByName(tcodeName);
            int tcodeTime = ((Integer) timeCodes.get(tcodeName)).intValue();
            List balancesToUpdate = cache.getIagTimeCodeBalData(data.getDBconnection() ,
                tcd.getTcodeId() ,
                data.getCalcgrpId());
            for (int currBal = 0; currBal < balancesToUpdate.size(); currBal++) {
                IagTimeCodeBalData tcb = (IagTimeCodeBalData) balancesToUpdate.get(currBal);
                TimeCodeBalanceTypeData tCodeBalTypeData = data.getCodeMapper().
                    getTimeCodeBalanceTypeById(tcb.getTcbtId());

                int interval = tCodeBalTypeData.getTcbtMinuteInterval();
                double changeBy = tCodeBalTypeData.getTcbtChangeBy();
                int split = tCodeBalTypeData.getTcbtSplitMinute();

                double delta = (tcodeTime / interval) * changeBy;
                if ((tcodeTime % interval) >= split) {
                    delta += changeBy;
                }
                // **** balance min/max moved to ruleAccess due to cascading balances
                EmployeeBalanceLogData empBalLog = RuleHelper.makeEmpBalLog(
                     data.getEmpId(),
                     tcb.getBalId(),
                     data.getRuleData().getWorkSummary().getWrksId(),
                     delta,
                     "processWorkDetailBalanceChanges/TCODE_NAME:" + tcodeName ,
                     data.getWrksWorkDate());
                data.getRuleData().addEmployeeBalanceLog(empBalLog);
            }
        }
    }

    protected void checkBalCalcgrpMinMax(WBData data , DBConnection c) {
        BalanceCalcgrpCache cache = BalanceCalcgrpCache.getInstance();
        List balances = data.getRuleData().getEmployeeBalancesAsofDateWithLogs();
        Iterator it = balances.iterator();
        while (it.hasNext()) {
            EmployeeBalanceData ebd = (EmployeeBalanceData) it.next();
            int balId = ebd.getBalId();
            BalanceData bd = data.getCodeMapper().getBalanceById(balId);
            double balMax = bd.getBalMax() , balMin = bd.getBalMin();
            IagBalanceCalcgrpData balCg = cache.getIagBalanceCalcgrpData(c , data.getCalcgrpId() , balId);
            if (balCg != null) {
                balMax = balCg.getIbcgBalMax();
                balMin = balCg.getIbcgBalMin();
            }
            if (ebd.getEmpbalValue() > balMax) {
                String msg = "Balance max violated:" + bd.getBalName()
                    + " (Max:" + balMax + ")(Value:" + ebd.getEmpbalValue() + ")";
                reflectBalanceViolation(data , msg);
            }
            else if (ebd.getEmpbalValue() < balMin) {
                String msg = "Balance min violated: " + bd.getBalName()
                    + " (Min:" + balMin + ")(Value:" + ebd.getEmpbalValue() + ")";
                reflectBalanceViolation(data , msg);
            }
        }
    }

    /**
     * Reflects balance violations
     *
     * @param wrksErrorMessage Error message as a string
     * @throws SQLException
     */
    protected void reflectBalanceViolation(WBData data, String wrksErrorMessage) {
        Iterator iter = data.getRuleData().getOverrides().iterator();
        while (iter.hasNext()) {
            OverrideData od = (OverrideData) iter.next();
            if (!
                (od.isBaseType(OverrideData.EMP_BAL_TYPE_START) ||
                 od.isBaseType(OverrideData.SCHEDULE_TYPE_START) ||
                 od.isBaseType(OverrideData.EMP_DLA_TYPE_START) ||
                 od.isBaseType(OverrideData.EMPLOYEE_TYPE_START) ||
                 OverrideData.CANCEL.equals(od.getOvrStatus()) ||
                 OverrideData.CANCELLED.equals(od.getOvrStatus())
                 )) {
                od.setOvrStatus(OverrideData.PENDING);
                data.getRuleData().getCalcDataCache().addBatchUpdate(od);
            }
        }
        data.getRuleData().getWorkSummary().unauthorize();
        throw new NestedRuntimeException(wrksErrorMessage);
    }
}
