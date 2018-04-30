/**
 * Created on May 19, 2005
 *
 * Title: NotClockInOutAlertSourceBuilder
 * Description: Builder class for NotClockInOutAlertSource
 * <p>Copyright:  Copyright (c) 2004</p>
 * <p>Company:    Workbrain Inc.</p>
 */
package com.wbiag.app.wbalert.source;

import java.util.HashMap;

import com.workbrain.app.wbalert.*;
import com.workbrain.server.data.RowSource;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;

/**
 * @author BLi
 *
 * @version  1.0
 */
public class NotClockInOutAlertSourceBuilder extends
        AbstractWBAlertSourceBuilder {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(NotClockInOutAlertSourceBuilder.class);

    public NotClockInOutAlertSourceBuilder()  {
        super();    
    }


    public RowSource getRowSource(java.io.Serializable param, DBConnection conn) {
        HashMap exportParam = (HashMap)param;

        try {
            return new NotClockInOutAlertSource(conn, exportParam);
        } catch (Exception ex) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ex.getMessage() , ex);}
            throw new NestedRuntimeException(ex);
        }
    }

    public String getTaskParametersUI() {
        return "/jobs/wbalert/notClockInOutAlertParams.jsp";
    }
}

