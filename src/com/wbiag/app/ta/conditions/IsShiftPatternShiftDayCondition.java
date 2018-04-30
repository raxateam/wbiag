package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.sql.*;
/**
 * IsShiftPatternShiftDay condition
 */
public class IsShiftPatternShiftDayCondition extends Condition {
    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsShiftPatternShiftDayCondition.class);

    public static final String PARAM_SHFTPATSHFT_DAY = "ShftpatshftDay";

    public boolean evaluate(WBData wbData, Parameters parameters) throws Exception {
        int  spDay = parameters.getIntegerParameter(PARAM_SHFTPATSHFT_DAY);

        boolean ret = false;

        ShiftPatternShiftsAccess spsa = new ShiftPatternShiftsAccess(
            wbData.getDBconnection());
        List spsList = wbData.getCodeMapper().
            getShiftPatternShiftsByShftpatId(wbData.getShftpatId());
        if (spsList.size() > 0) {
            ShiftPatternShiftsData spsd =
                spsa.loadByShiftPatDatOffset(wbData.getShftpatId(),
                                             wbData.getWrksWorkDate(),
                                             wbData.getEmpShftpatOffset());
            ret = spsd.getShftpatshftDay() == spDay;
        }

        return ret ;
    }

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_SHFTPATSHFT_DAY, RuleParameterInfo.INT_TYPE, false));
        return result;
    }

    public String getComponentName() {
        return "WBIAG: Is Shift Pattern Shift Day";
    }

}
