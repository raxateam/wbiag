package com.wbiag.app.jsp.action.biweeklytimesheet;

import com.workbrain.app.jsp.action.timesheet.WTSPresentationHelper;
import com.workbrain.server.WorkbrainParametersRetriever;

public class CSMWTSPresentationHelper implements java.io.Serializable {
    private static CSMWTSPresentationHelper instance = null;
    private static WTSPresentationHelper _coreinstance = null;

    private CSMWTSPresentationHelper() {
    	_coreinstance = WTSPresentationHelper.instance();
    }

    public static CSMWTSPresentationHelper instance() {
        if( instance == null ) {
            instance = new CSMWTSPresentationHelper();
        }
        return instance;
    }

    public boolean useDistributedTime() {
        return _coreinstance.useDistributedTime(); //WorkbrainParametersRetriever.getBoolean("USE_WTS_DISTRIBUTED_TIME", false);
    }

    public boolean forceCleanSubmit() {
        return _coreinstance.forceCleanSubmit();//WorkbrainParametersRetriever.getBoolean("WTS_FORCE_CLEAN_SUBMIT", false);
    }

    public boolean useCustomButton() {
        return _coreinstance.useCustomButton();//WorkbrainParametersRetriever.getBoolean("USE_WTS_CUSTOM_BUTTON", false);
    }

    public boolean useWorkStartStop(CSMBiWeeklyTimeSheetPage model) {
        String useWorkStartStop = CSMWTShelper.getWTSExtraParamValue(
            model.getExtraWtsParameters(),
            "WTS_USE_WORKSTARTSTOP");
        if (useWorkStartStop == null) {
            return WorkbrainParametersRetriever.getBoolean("WTS_USE_WORKSTARTSTOP", false);
        }
        return Boolean.valueOf(useWorkStartStop).booleanValue();
    }

    public boolean showNonWorkTimePage(CSMBiWeeklyTimeSheetPage model) {
        String showComponent = CSMWTShelper.getWTSExtraParamValue(
            model.getExtraWtsParameters(),
            "WTS_SHOW_COMPONENT");
        if (showComponent == null) {
            showComponent = WorkbrainParametersRetriever.getString("WTS_SHOW_COMPONENT", "");
        }
        return showComponent.indexOf("NONWORKTIME") > -1;
    }

    public boolean showFullDayAbsencePage(CSMBiWeeklyTimeSheetPage model) {
        String showComponent = CSMWTShelper.getWTSExtraParamValue(
            model.getExtraWtsParameters(),
            "WTS_SHOW_COMPONENT");
        if (showComponent == null) {
            showComponent = WorkbrainParametersRetriever.getString("WTS_SHOW_COMPONENT", "");
        }
        return showComponent.indexOf("FULLDAYABSENCE") > -1;
    }
}