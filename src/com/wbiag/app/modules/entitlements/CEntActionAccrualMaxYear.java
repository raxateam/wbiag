package com.wbiag.app.modules.entitlements;

import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.util.*;
import java.util.*;
import java.sql.SQLException;
/**
 * Entitlement Accrual Max Year
 */
public class CEntActionAccrualMaxYear extends DefaultEntAction {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CEntActionAccrualMaxYear.class);


    public CEntActionAccrualMaxYear() {
    }


    /**
     * Implements OnMaxOut interface.
     *
     * @param wbData
     * @param ent
     * @param entDetail
     * @throws EntitlementException
     */
    public boolean execute( WBData wbData,
            EntitlementData ent, EntDetailData entDetail ) throws EntitlementException {
        super.execute(wbData , ent, entDetail);

        String yearMaxString = entDetail.getEntdetUdf1();
        if (StringHelper.isEmpty(yearMaxString)) {
            return false;
        }
        double yearMax = Double.parseDouble(yearMaxString);
        double accrualSoFar = getAccrualSinceYearStart(wbData , entDetail);
        if (logger.isDebugEnabled()) logger.debug("yearMax : " + yearMax + "-accrualSoFar :" + accrualSoFar);

        double value = yearMax - accrualSoFar;
        if (value < 0) {
            try {
                wbData.addEmployeeBalanceValue(entDetail.getBalId(), value,
                    "Created by Year Max Accrual for Entitlement : " +
                    ent.getEntName());
                if (logger.isDebugEnabled()) logger.debug("Created " + value + " log due to Year Max Accrual");
            }
            catch (SQLException ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("Error in Year Max Accrual " , ex);
                throw new NestedRuntimeException("Error in Year Max Accrual " , ex);
            }
        }
        return false;
    }

    protected double getAccrualSinceYearStart(WBData wbData , EntDetailData entDetail) {
        double ret = 0.0;
        Date yearStart = DateHelper.getUnitYear(DateHelper.APPLY_ON_FIRST_DAY,
                                                false,
                                                wbData.getWrksWorkDate());
        // *** first get balances since year start up to current date
        List balLogs = wbData.getRuleData().getCalcDataCache().
            getEmployeeBalanceLogsAfterDate(wbData.getEmpId() , yearStart);
        Iterator iter = balLogs.iterator();
        while (iter.hasNext()) {
            EmployeeBalanceLogData item = (EmployeeBalanceLogData)iter.next();
            if (item.getBalId() == entDetail.getBalId()
                && item.getWrksWorkDate().before(wbData.getWrksWorkDate())){
                ret += item.getEblogDelta();
            }
        }
        // *** add latest changes in current date
        List curBalLogs = wbData.getRuleData().getEmployeeBalanceLogs();
        Iterator iter2 = curBalLogs.iterator();
        while (iter2.hasNext()) {
            EmployeeBalanceLogData item = (EmployeeBalanceLogData)iter2.next();
            if (item.getBalId() == entDetail.getBalId()){
                ret += item.getEblogDelta();
            }
        }

        return ret;
    }
}
