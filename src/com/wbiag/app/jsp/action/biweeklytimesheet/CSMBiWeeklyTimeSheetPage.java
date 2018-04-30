package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.util.Date;

import org.apache.log4j.Logger;

import com.workbrain.app.jsp.action.timesheet.WTSPresentationHelper;
import com.workbrain.app.jsp.action.timesheet.WeeklyTimeSheetPage;
import com.workbrain.util.DateHelper;

public class CSMBiWeeklyTimeSheetPage extends WeeklyTimeSheetPage implements CSMBiWeeklyTimeSheetConstants {
    private static final Logger logger = Logger.getLogger(CSMBiWeeklyTimeSheetPage.class);

    public CSMBiWeeklyTimeSheetPage(){
    	setNonWorkTimePage(new CSMLTAPage());
    	setFullDayAbsencePage(new CSMLTAPage());
    }

    public Date getWeekEndDate() {
        return DateHelper.addDays( super.getWeekStartDate(), DAYS_ON_TIMESHEET - 1 );
    }

    protected void setWbuName( String wbuName ) {
        super.setWbuName(wbuName);
    }

/*vlo: WARNING: Since WTSPresentationHelper has a private constructor,
CSMWTSPresentationHelper cannot extend WTSPresentationHelper.
Currently, no one uses the WeeklyTimeSheetPage.getHelper() method.
In the future, however, whoever wants to use the WeeklyTimeSheetPage.getHelper() should change
reference to use CSMBiWeeklyTimeSheetPage.getCSMHelper() instead for the biweekly timesheet solution
*/
//    public CSMWTSPresentationHelper getCSMHelper() {
//        return CSMWTSPresentationHelper.instance();
//    }

/*vlo: WARNING: WeeklyTimeSheetPage.clone() uses the WTSPresentationHelper class.
 * In the biweekly solution, it should use WTSPresentationHelper instead.
 * Currently, the clone() method is used by the following classes:
 * -AbstractScheduleSegement
 * -CopyPreviousWeekAction
 * -NewShiftAction
 *
 * CSMBiWeeklyTimeSheetPage has not overridden the clone method, since
 * the related classes above are not part of the customization.
 *
 */

}

