
package com.wbiag.app.wbalert.source;

import java.util.*;

import com.workbrain.app.wbalert.*;
import com.workbrain.server.data.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * Created on Mar 7, 2005
 *
 * Title: WBIntTransactionAlertSourceBuilder
 * Description: The builder of the RowSource of WBIntTransactionAlertSource
 * <p>Copyright:  Copyright (c) 2004</p>
 * <p>Company:    Workbrain Inc.</p>
 */

public class WBIntTransactionAlertSourceBuilder extends AbstractWBAlertSourceBuilder {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBIntTransactionAlertSourceBuilder.class);
    /**
     *
     */
    public WBIntTransactionAlertSourceBuilder() {
        super();
        // TODO Auto-generated constructor stub
    }

    public RowSource getRowSource(java.io.Serializable param, DBConnection conn) {
        HashMap exportParam = (HashMap)param;

        try {
            return new WBIntTransactionAlertSource(conn, exportParam);
        } catch (Exception ex) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ex.getMessage() , ex);}
            throw new NestedRuntimeException(ex);
        }
    }

    public String getTaskParametersUI() {
        return "/jobs/wbiag/wbIntTransactionAlertParams.jsp";
    }
}

