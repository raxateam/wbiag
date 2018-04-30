package com.wbiag.app.modules.entitlements;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Calendar;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.app.modules.entitlements.*;
import com.workbrain.util.DateHelper;

/**
 * Custom date unit to make sure leap years are considered when calculating YEAR periods
 */
public class CentUnitDateYearWithLeapYear implements CustomUnitDate {
    /**
     * Returnes the day when the entitlement applies for .
     *
     * @param applyOnUnit   applyOnUnit
     * @param applyOnValue  applyOnValue
     * @param previous      if previous date required
     * @param wbData        WBData
     * @return
     * @throws EntitlementException
     */
    public Date getUnitDate( String applyOnUnit, String applyOnValue,
        boolean previous,  WBData wbData) throws EntitlementException {

        Date ret = DateHelper.getUnitYear(applyOnValue , previous , wbData.getWrksWorkDate());
        GregorianCalendar retCal = (GregorianCalendar)DateHelper.getCalendarTruncatedToDay(ret);
        boolean isRetDateAfterFeb28 = retCal.get(Calendar.MONTH) >3
            || DateHelper.isLeapDay(ret);
        if (retCal.isLeapYear(retCal.get(Calendar.YEAR)) && isRetDateAfterFeb28) {
            ret = DateHelper.addDays(ret , 1);
        }

        return ret;
    }

}
