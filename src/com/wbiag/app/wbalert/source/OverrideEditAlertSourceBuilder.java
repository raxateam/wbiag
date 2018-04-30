package com.wbiag.app.wbalert.source ;

import java.util.*;

import com.workbrain.app.wbalert.*;
import com.workbrain.server.data.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Builder class for OverrideEditAlertSource
 *
 */
public class OverrideEditAlertSourceBuilder extends AbstractWBAlertSourceBuilder {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(OverrideEditAlertSourceBuilder.class);

    public OverrideEditAlertSourceBuilder()  {}

    public RowSource getRowSource(java.io.Serializable param, DBConnection conn) {
        HashMap exportParam = (HashMap)param;

        try {
            return new OverrideEditAlertSource(conn, exportParam);
        } catch (Exception ex) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ex.getMessage() , ex);}
            throw new NestedRuntimeException(ex);
        }
    }

    public String getTaskParametersUI() {
        return "/jobs/wbalert/overrideEditAlertParams.jsp";
    }


}






