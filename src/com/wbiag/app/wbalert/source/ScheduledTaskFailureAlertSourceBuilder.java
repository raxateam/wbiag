
package com.wbiag.app.wbalert.source;

import java.util.HashMap;

import com.workbrain.app.wbalert.AbstractWBAlertSourceBuilder;
import com.workbrain.server.data.RowSource;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;
/**
 * Created on Mar 7, 2005
 *
 * Title: ScheduledTaskFailureAlertSourceBuilder
 * Description: The builder of the RowSource of ScheduledTaskFailureAlertSource
 * <p>Copyright:  Copyright (c) 2004</p>
 * <p>Company:    Workbrain Inc.</p>
 */

public class ScheduledTaskFailureAlertSourceBuilder extends AbstractWBAlertSourceBuilder {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScheduledTaskFailureAlertSourceBuilder.class);
    /**
     *
     */
    public ScheduledTaskFailureAlertSourceBuilder() {
        super();
        // TODO Auto-generated constructor stub
    }

    public RowSource getRowSource(java.io.Serializable param, DBConnection conn) {
        HashMap exportParam = (HashMap)param;

        try {
            return new ScheduledTaskFailureAlertSource(conn, exportParam);
        } catch (Exception ex) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ex.getMessage() , ex);}
            throw new NestedRuntimeException(ex);
        }
    }

    public String getTaskParametersUI() {
        return "/jobs/wbalert/skdTaskFailureAlertParams.jsp";
    }
}

