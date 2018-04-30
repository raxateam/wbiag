package com.wbiag.app.modules.entitlements;

import java.util.*;

import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
/**
 * Partial Ratio that kicks on HireDate.
 */
public class CEntActionRatioPartialHireDateMonth extends DefaultEntAction {

    public CEntActionRatioPartialHireDateMonth() {
    }


    /**
     * Implements partial ratios for APPLY_ON_UNIT_HIREDATE entitlements. <p>
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

        boolean shouldCalcPartial = EntitlementData.APPLY_ON_UNIT_HIREDATE.equals(ent.getEntApplyOnValue());

        if (!shouldCalcPartial) {
            return ret;
        }
        Date yearEnd = DateHelper.getUnitYear(DateHelper.APPLY_ON_LAST_DAY,
                                              false,
                                              wbData.getWrksWorkDate());
        int mnthsBetween = (int)DateHelper.getMonthsBetween(wbData.getWrksWorkDate(), yearEnd);
        ret = (mnthsBetween + 1) / 12;
        return ret;
    }

}
