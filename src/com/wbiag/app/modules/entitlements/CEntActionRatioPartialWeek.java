package com.wbiag.app.modules.entitlements;

import java.util.*;

import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
/**
 * 39249	ENTITLEMENT PARTIAL RATIO
 */
public class CEntActionRatioPartialWeek extends DefaultEntAction {

    public CEntActionRatioPartialWeek() {
    }


    /**
     * Implements partial ratios for WEEK entitlements. <p>
     * The ratio only kicks for WEEK entitlements that kick on last day.
     * The code can be changed to apply the same logic for any entitlement unit like MONTH, QUARTER.
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

        boolean shouldCalcPartial =
            DateHelper.APPLY_ON_UNIT_WEEK.equals(ent.getEntApplyOnUnit())
            && DateHelper.APPLY_ON_LAST_DAY.equals(ent.getEntApplyOnValue());

        if (!shouldCalcPartial) {
            return ret;
        }
        Date weekStart = DateHelper.getUnitWeek(DateHelper.APPLY_ON_FIRST_DAY, false,
                                           wbData.getWrksWorkDate());
        Date weekEnd = DateHelper.getUnitWeek(DateHelper.APPLY_ON_LAST_DAY, false,
                                         wbData.getWrksWorkDate());
        if (DateHelper.isBetween(wbData.getEmpHireDate() , weekStart, weekEnd)) {
            int daysWrked = DateHelper.getDifferenceInDays(
                wbData.getEmpHireDate() ,
                weekEnd) + 1;
            ret = daysWrked / 7;
        }
        return ret;
    }

}
