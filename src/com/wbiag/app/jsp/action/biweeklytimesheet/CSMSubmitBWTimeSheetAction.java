package com.wbiag.app.jsp.action.biweeklytimesheet;

import java.util.ArrayList;
import java.util.List;

import com.workbrain.app.jsp.ActionContext;
import com.workbrain.app.jsp.action.timesheet.OverridePage;
import com.workbrain.app.jsp.action.timesheet.WorkSummaryModel;
import com.workbrain.server.WebSession;
import com.workbrain.sql.DBConnection;
import com.workbrain.tool.overrides.AbstractOverrideBuilder;
import com.workbrain.tool.overrides.OverrideBuilder;

public class CSMSubmitBWTimeSheetAction extends CSMSaveBWTimeSheetAction implements CSMBiWeeklyTimeSheetConstants {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CSMSubmitBWTimeSheetAction.class);

    public final String process( DBConnection conn, ActionContext context,
                           Object requestObj )
            throws Exception {
                logger.debug("process(...)   start");

        CSMBiWeeklyTimeSheetPage model = (CSMBiWeeklyTimeSheetPage)context.getAttribute(
                "timesheet" );

        context.setAttribute( "timesheet", model );
        AbstractOverrideBuilder ovrBuilder = getOverrideBuilder( conn );
        ovrBuilder = super.doProcess( conn, context, requestObj, ovrBuilder );

        model = (CSMBiWeeklyTimeSheetPage)context.getAttribute(
                "timesheet" );
        invalidateSummaries( model );

        CSMBiWeeklyTimeSheetPage request = (CSMBiWeeklyTimeSheetPage)requestObj;
        if (context.getAttribute("WBG_SESSION").equals("Y")){
            model.setIsLockDownPriv(true);
        } else {
            model.setIsLockDownPriv(false);
        }
        processOverrides( conn, model.getOverride(), request.getOverride(), model, ovrBuilder);

        context.setAttribute( "timesheet", model );

        WebSession ws = (WebSession) context.getAttribute(com.workbrain.server.WebConstants.WEB_SESSION_ATTRIBUTE_NAME);
        ws.setActionStatus("Changes to your timesheet have been saved.", "ACTION_STATUS_TIMESHEET_SUBMITTED");

        logger.debug("process(...)   return");
        //return "/action/timesheet/ViewTimeSheet";
        return CSMWTShelper.getActionURL(ACTION_VIEW_TIMESHEET);
        //return ("/action/timesheet?action=" + AbstractActionFactory.VIEW_TIMESHEET_ACTION);
    }

    protected void processOverrides( DBConnection conn, OverridePage modelOverridePage,
                                   OverridePage overridePage, CSMBiWeeklyTimeSheetPage parent, AbstractOverrideBuilder ovrBuilder)
            throws Exception {
        //Date date = parent.getWeekStartDate();

        int empId = parent.getEmployee().getEmpId();
        /*
        // *** Creating list of dates and employees to copy the overrides to ***
        List applyEmps = new ArrayList();
        List applyDates = new ArrayList();
        for( int i = 0; i < DAYS_ON_TIMESHEET; i++ ) {
            DayOverrideModel dayOverride = (DayOverrideModel)overridePage.
                     getOverrideDays().get(i);
            if (dayOverride.getApply()) {
                applyDates.add(DateHelper.addDays(date, i));
                applyEmps.add(String.valueOf(empId));
            }
        }

        for( int i = 0; i < DAYS_ON_TIMESHEET; i++ ) {
            DayOverrideModel dayOverride = (DayOverrideModel)overridePage.
                     getOverrideDays().get(i);
            WorkSummaryModel summary = (WorkSummaryModel)parent.
                                       getSummaries().get(i);

            Iterator j = dayOverride.getOverrides().iterator();
            while( j.hasNext() ) {
                OverrideModel ovr = (OverrideModel)j.next();
                if (ovr.getCp()) {
                    CopyOverride copyOvr = new CopyOverride();
                    copyOvr.setOverrideId(ovr.getId());
                    copyOvr.setEmpList(applyEmps);
                    copyOvr.setDateList(applyDates);
                    copyOvr.setWbuNameBoth( parent.getWbuName(), parent.getWbuNameActual() );
                    ovrBuilder.add(copyOvr);
                }
                if( ovr.getDelete() ) {
                    AbstractOverrideOperation del = getDeleteOverride();
                    del.setOverrideId( ovr.getId() );
                    ovrBuilder.add( del );
                    summary.setInvalid( true );
                    logger.debug("CSMSubmitBWTimeSheetAction.processOverrideForADay() added override.");
                }
            }
            date = DateHelper.addDays( date, 1 );
        }*/

        ovrBuilder.setDirection("TS");
        List empIdList = new ArrayList();
        empIdList.add( String.valueOf( empId ) );
        ovrBuilder.setEmpIdList( empIdList );
        ovrBuilder.setFirstStartDate( parent.getWeekStartDate() );
        ovrBuilder.setLastEndDate( parent.getWeekEndDate() );
        if (ovrBuilder instanceof OverrideBuilder) {
            ((OverrideBuilder)ovrBuilder).setRuleEngineAutoRecalc( true );
        }
        ovrBuilder.execute( true );
    }

    protected final void invalidateSummaries( CSMBiWeeklyTimeSheetPage model ) {
        List summaries = model.getSummaries();
        if( summaries != null ) {
            for( int i = 0; i < DAYS_ON_TIMESHEET; i++ ) {
                WorkSummaryModel summary = (WorkSummaryModel)summaries.get(i);
                summary.setInvalid( true );
            }
        }
    }
}
