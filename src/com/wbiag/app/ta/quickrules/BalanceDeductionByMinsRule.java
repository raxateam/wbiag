package com.wbiag.app.ta.quickrules;

import java.sql.*;
import java.util.*;
import java.text.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * BalanceDeductionByMinsRule - TT646
 */
public class BalanceDeductionByMinsRule extends Rule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(BalanceDeductionByMinsRule.class);

    public static final String PARAM_TIMECODE_LIST = "TimeCodeList";
    public static final String PARAM_AFFECTS_BALANCE_LIST = "BalanceTransferTo";
    public static final String PARAM_AFFECTS_BALANCE_RATIO = "BalanceTransferRatio";
    public static final String PARAM_BALANCE_LOG_MESSAGE = "BalanceTransferMessage";
    public static final String PARAM_VAL_DFLT_MSG = "BalanceDeductionByMinsRule";
    public static final String ERRMSG_NOT_SAME_TOKENS = "TimeCodeList, AffectsBalanceList and AffectsBalanceRatio do not have same number of tokens";
    public static final String ERRMSG_NO_VALUES = "No TimeCodeList, AffectsBalanceList or AffectsBalanceRatio was supplied";
    public static final MessageFormat ERRMSG_TCODE_NOT_FOUND
        = new MessageFormat("Time code not found : ?");
    public static final MessageFormat ERRMSG_BAL_NOT_FOUND
        = new MessageFormat("Balance not found : ?");
    public static final MessageFormat ERRMSG_RATIO
        = new MessageFormat("Ratio : ? is not a valid double number" );
    public static final MessageFormat ERRMSG_DBL_AFFECT
        = new MessageFormat("Time code : ? has tcode_affects_balances checked and same balance can’t be defined in both time_code_balance and Balance Deduction by Minutes rule");

    public static List ELIGIBLE_EMP_VALS = new ArrayList();
    public static String EMP_VAL_PREFIX = "EMP_VAL";
    public static String RATIO_MULT = "*";
    public static String RATIO_DIV = "/";

    static {
        for (int i = 1; i <= 20; i++) {
            ELIGIBLE_EMP_VALS.add(EMP_VAL_PREFIX + i);
        }

    }

    public List getParameterInfo (DBConnection conn) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_TIMECODE_LIST, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_AFFECTS_BALANCE_LIST, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_AFFECTS_BALANCE_RATIO, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BALANCE_LOG_MESSAGE, RuleParameterInfo.STRING_TYPE, true));
        return result;
    }

    public void execute(WBData wbData, Parameters parameters) throws SQLException, Exception {
        DeductionDef def = getDeductionDefiniton ( wbData, parameters);
        for (int i = 0; i < def.tcodeNames.length; i++) {
            int mins = wbData.getMinutesWorkDetail(def.tcodeNames[i], true, null, true);
            mins += wbData.getMinutesWorkPremium(def.tcodeNames[i], true, null, true);
            double delta = (mins  / 60.0) * def.ratios[i];
            if (logger.isDebugEnabled()) {
                logger.debug("Found :" + mins + " minutes for time code : " + def.tcodeNames[i]);
                logger.debug("Delta calculated as : " + delta);
            }
            wbData.addEmployeeBalanceValue(def.balNames[i], delta , def.msg);
        }

    }

    public String getComponentName() {
        return "WBIAG: Balance Deduction by Minutes Rule";
    }

    private DeductionDef getDeductionDefiniton(WBData wbData,
                                               Parameters parameters) throws Exception {
        DeductionDef ret = new DeductionDef();

        String[] tcodes = StringHelper.detokenizeString(
            parameters.getParameter(PARAM_TIMECODE_LIST, ""), "," );
        String[] balNames = StringHelper.detokenizeString(
            parameters.getParameter(PARAM_AFFECTS_BALANCE_LIST, ""), "," );
        String[] ratios = StringHelper.detokenizeString(
            parameters.getParameter(PARAM_AFFECTS_BALANCE_RATIO, ""), "," );
        if (tcodes.length != balNames.length
            || tcodes.length != ratios.length
            || balNames.length != ratios.length) {
            throw new RuleEngineException (ERRMSG_NOT_SAME_TOKENS);
        }
        if (tcodes.length == 0) {
            throw new RuleEngineException (ERRMSG_NO_VALUES);
        }

        for (int i = 0; i < balNames.length; i++) {
            BalanceData bcd = wbData.getCodeMapper().getBalanceByName(balNames[i]);
            if (bcd == null) {
                 throw new RuleEngineException (ERRMSG_BAL_NOT_FOUND.format(new String[] {balNames[i]}));
            }
        }
        ret.balNames = balNames;


        for (int i = 0; i < tcodes.length; i++) {
            TimeCodeData tcd = wbData.getCodeMapper().getTimeCodeByName(tcodes[i]);
            if (tcd == null) {
                 throw new RuleEngineException (ERRMSG_TCODE_NOT_FOUND.format(new String[] {tcodes[i]}));
            }
            else {
                if ("Y".equals(tcd.getTcodeAffectsBalances())) {
                    List tcb = wbData.getCodeMapper().getTCBByTimeCodeId(tcd.
                        getTcodeId());
                    Iterator iter = tcb.iterator();
                    while (iter.hasNext()) {
                        TimeCodeBalanceData item = (TimeCodeBalanceData) iter.next();
                        if (item.getBalId() == (wbData.getCodeMapper().getBalanceByName(balNames[i]).getBalId())) {
                            throw new RuleEngineException (ERRMSG_DBL_AFFECT.format(new String[] {tcodes[i]}));
                        }
                    }
                }

            }
        }
        ret.tcodeNames = tcodes;


        ret.ratios = new double[ratios.length];
        for (int i = 0; i < ratios.length; i++) {
            try {
                ret.ratios[i] = resolveRatio(ratios[i], wbData);
            }
            catch (NumberFormatException ex) {
                throw new RuleEngineException (ERRMSG_RATIO.format(new String[] {ratios[i]}));
            }
        }
        ret.msg = parameters.getParameter(PARAM_AFFECTS_BALANCE_RATIO, PARAM_VAL_DFLT_MSG);
        return ret;
    }

    /**
     * see if ratio is like -1 * emp_val1 or a number
     * @param ratio
     * @param wbData
     * @return
     */
    protected double resolveRatio(String ratio, WBData wbData) {
        double ret = 1.0;
        int multInd = ratio.indexOf(RATIO_MULT);
        int multDiv= ratio.indexOf(RATIO_DIV);
        if (multInd > -1 && multDiv > -1) {
            throw new RuntimeException ("Ratio can only have one of / or *");
        }
        if (multInd > -1 || multDiv > -1) {
            int ind = multInd > -1 ? multInd : multDiv;
            String sOpr1 = ratio.substring(0, ind).trim();
            String sOpr2 = ratio.substring(ind + 1).trim();
            if (sOpr1.toUpperCase().indexOf(EMP_VAL_PREFIX)  > -1) {
                sOpr1 = (String)wbData.getRuleData().getEmployeeData().getField(sOpr1);
            }
            if (sOpr2.toUpperCase().indexOf(EMP_VAL_PREFIX)  > -1) {
                sOpr2 = (String)wbData.getRuleData().getEmployeeData().getField(sOpr2);
            }
            double opr1 = Double.parseDouble(sOpr1);
            double opr2= Double.parseDouble(sOpr2 );
            ret = (multInd > -1) ? (opr1 * opr2) : (opr1 / opr2);
        }
        else {
            ret = Double.parseDouble(ratio);
        }
        return ret;
    }

    private class DeductionDef {
        String[] tcodeNames;
        String[] balNames;
        double[] ratios;
        String msg;
    }

}
