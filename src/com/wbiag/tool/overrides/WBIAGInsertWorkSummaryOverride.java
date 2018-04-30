package com.wbiag.tool.overrides;

import com.workbrain.tool.overrides.*;
import com.workbrain.sql.*;
import com.workbrain.app.ta.model.*;

public class WBIAGInsertWorkSummaryOverride
    extends InsertWorkSummaryOverride {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WBIAGInsertWorkSummaryOverride.class);

    private String wrksInCode;
    private String wrksOutCode;

    public WBIAGInsertWorkSummaryOverride(DBConnection conn) {
        super(conn);
    }

    public String getOvrNewValue() {
        if (!isBaseType(OverrideData.WORK_SUMMARY_TYPE_START)) {
            throw new RuntimeException("Override type : " + getOvrType()
                                       + " is not a work summary override type");
        }
        WorkSummaryData wsSuper = super.getWorkSummaryData();
        wsSuper.setWrksInCode(wrksInCode);
        wsSuper.setWrksOutCode(wrksOutCode);
        if (logger.isInfoEnabled()) {
            logger.info("WBIAG IWSO AUTH BY: " + wsSuper.getWrksAuthBy());
        }
        return wsSuper.createOverrideNewValue();
    }

    public void setWrksInCode(String v) {
        wrksInCode = v;
    }

    public void setWrksOutCode(String v) {
        wrksOutCode = v;
    }

}
