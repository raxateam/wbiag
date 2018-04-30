package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.quickrules.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.util.ArrayList;
import java.util.List;
import com.wbiag.app.ta.db.*;
import com.wbiag.app.ta.model.*;
/**
 * CascadeTimeCodeRule that checks balance min/max from IAG_BALANCE_CALCGRP table
 * and time_code_balance definitions from IAG_TIME_CODE_BAL table
 */
public class CascadeTimeCodeRuleBalCalcgrp extends Rule{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CascadeTimeCodeRuleBalCalcgrp.class);


    public CascadeTimeCodeRuleBalCalcgrp() {
    }

    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {
        CodeMapper codeMapper = wbData.getRuleData().getCodeMapper();

        String cascadeFromTimeCode = parameters.getParameter(CascadeTimeCodeRule.PARAM_CASCADE_FROM_TIMECODE);
        String cascadeToTimeCode = parameters.getParameter(CascadeTimeCodeRule.PARAM_CASCADE_TO_TIMECODE);
        String cascadeForBalance = parameters.getParameter(CascadeTimeCodeRule.PARAM_CASCADE_FOR_BALANCE);
        String cascadeType = parameters.getParameter(CascadeTimeCodeRule.PARAM_CASCADE_TYPE);

        TimeCodeData tcdFrom = codeMapper.getTimeCodeByName(cascadeFromTimeCode);
        BalanceData bd = codeMapper.getBalanceByName(cascadeForBalance);
        if (tcdFrom == null) {
            throw new NestedRuntimeException ("CascadeFromTimeCode not found : " + cascadeFromTimeCode);
        }
        // *** no proccessing if timecode does not affect balances
        if ("Y".equals(tcdFrom.getTcodeAffectsBalances()) == false) {
            return;
        }
        // *** no proccessing if timecode sum = 0
        int sumFromMinutes = wbData.getMinutesWorkDetail(cascadeFromTimeCode,
                true,  null,  true);
        if (sumFromMinutes == 0) {
            return;
        }
        double asofDataBalValue = wbData.getEmployeeBalanceValue(bd.getBalId());
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("balValue as of date: " +  asofDataBalValue);}

        // *** calculate the violating minutes i.e if this timecode affected the balances
        // *** how many minutes of sumFromMinutes would cause it to to be below min/above max
        int minutesViolating = calculateViolatingMinutes(tcdFrom.getTcodeId() ,
                bd,
                sumFromMinutes,
                asofDataBalValue,
                cascadeType, wbData);
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("minutesViolating: " +  minutesViolating);}

        if (minutesViolating != 0) {
            // *** and change the violating minutes to given cascadeToTimeCode starting from reverse
            wbData.setWorkDetailTcodeNameDuration (cascadeToTimeCode,
                    minutesViolating,
                    cascadeFromTimeCode,
                    false,
                    true,
                    true);
        }

    }

    public List getParameterInfo(DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(CascadeTimeCodeRule.PARAM_CASCADE_FROM_TIMECODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(CascadeTimeCodeRule.PARAM_CASCADE_TO_TIMECODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(CascadeTimeCodeRule.PARAM_CASCADE_FOR_BALANCE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(CascadeTimeCodeRule.PARAM_CASCADE_TYPE, RuleParameterInfo.STRING_TYPE, false));

        return result;
    }


    /**
     *  Calculates the violating minutes for the given time code when it
     *  affects the given balance for given minutes.
     * Checks balance min/max from IAG_BALANCE_CALCGRP table and time_code_balance definitions
     * from IAG_TIME_CODE_BAL table
     *
     *@param  tcodeId      tcodeId
     *@param  bd           balance Data
     *@param  minutes      minutes to be consumed
     *@param  balValue     as of date value
     *@param  cascadeType  type of cascade
     *@return              violating minutes
     */
    protected int calculateViolatingMinutes(int tcodeId,
                                      BalanceData bd,
                                      int minutes,
                                      double balValue,
                                      String cascadeType,
                                      WBData data) {
        TimeCodeBalanceTypeData tCodeBalTypeData = null;
        int violatedMinutes = 0;

        // *** find the affecting TimeCodeBalanceTypeData
        BalanceCalcgrpCache cache = BalanceCalcgrpCache.getInstance();
        List balancesToUpdate = cache.getIagTimeCodeBalData(data.getDBconnection() ,
                tcodeId ,
                data.getCalcgrpId());
        for (int currBal = 0; currBal < balancesToUpdate.size(); currBal++) {
            IagTimeCodeBalData tcb = (IagTimeCodeBalData) balancesToUpdate.get(currBal);
            if (tcb.getBalId() == bd.getBalId()) {
                tCodeBalTypeData = data.getCodeMapper().getTimeCodeBalanceTypeById(tcb.getTcbtId());
                break;
            }
        }

        // *** if time code balance exists for this balance, no violating minutes
        if (tCodeBalTypeData == null) {
            return violatedMinutes;
        }

        int interval = tCodeBalTypeData.getTcbtMinuteInterval();
        double changeBy = tCodeBalTypeData.getTcbtChangeBy();
        int split = tCodeBalTypeData.getTcbtSplitMinute();
        boolean violates =  false;

        // calculate the balance
        balValue += ((double)minutes / interval) * changeBy;

        double balMax = bd.getBalMax() , balMin = bd.getBalMin();
        IagBalanceCalcgrpData balCg = cache.getIagBalanceCalcgrpData(data.getDBconnection() ,
            data.getCalcgrpId() , bd.getBalId());
        if (balCg != null) {
            balMax = balCg.getIbcgBalMax();
            balMin = balCg.getIbcgBalMin();
        }

         if (cascadeType.equals(CascadeTimeCodeRule.BELOW_MINIMUM)) {
            violates = balValue < balMin;
        } else if (cascadeType.equals(CascadeTimeCodeRule.ABOVE_MAXIMUM)) {
            violates = balValue > balMax;
        }

        if (violates) {
            // The balance min or max value has been violated.
            // set the violatedMinutes to the exceeded value.
            violatedMinutes = Math.abs((int)(balValue * interval));
        }

        return violatedMinutes;
    }


    public String getComponentName() {
        return "WBIAG: Cascade Time Code Rule Calcgrp";
    }


}
