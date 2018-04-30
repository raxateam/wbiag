package com.wbiag.app.modules.entitlements;

import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
/**
 * Entitlement Partial Ratio Based on Employee Attribute
 */
public class CEntActionRatioPartialEmployeeAttribute extends DefaultEntAction {

    private static final int RATIO_HRS = 40;

    public CEntActionRatioPartialEmployeeAttribute() {
    }


    /**
     * Implements partial ratios based on employee attribute.
     *
     * @param wbData wbData
     * @param ent EntitlementData
     * @param entDetail EntDetailData that satisfied the conditions
     * @return ratio
     * @throws EntitlementException
     */
    public double getRatio( WBData wbData, EntitlementData ent,
            EntDetailData entDetail ) throws EntitlementException {
        double ret = 1;
        // *** return 1 if no value
        if (StringHelper.isEmpty(wbData.getEmpVal10())) {
            return ret;
        }
        double weekHrs = 0;
        try {
            weekHrs = Double.parseDouble(wbData.getEmpVal10());
        }
        catch (NumberFormatException ex) {
            throw new EntitlementException(
                "Empval10 could not be parsed as number : " + wbData.getEmpVal10());
        }

        return weekHrs/RATIO_HRS;
    }

}
