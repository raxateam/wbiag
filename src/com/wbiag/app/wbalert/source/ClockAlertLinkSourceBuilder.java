package com.wbiag.app.wbalert.source;

import com.workbrain.app.wbalert.*;
import com.workbrain.server.data.*;
import com.workbrain.server.data.sql.AbstractSQLRowSource;
import com.workbrain.server.data.type.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import java.util.*;
import java.sql.*;

/**
 * Creates a new instance of ClockAlertSource which will return a list of rows
 * for all employees with clocking issues.
 * TODO: implement proper parameters .jsp page
*/
public class ClockAlertLinkSourceBuilder extends AbstractWBAlertSourceBuilder {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ClockAlertLinkSourceBuilder.class);

	/** Does nothing. */
    public ClockAlertLinkSourceBuilder()  {}

	/** Creates a new instance of ClockAlertSource, passing the parameters to the constructor. */
    public RowSource getRowSource(java.io.Serializable param, DBConnection conn) {
        HashMap exportParam = (HashMap)param;
        try {
            return new ClockAlertLinkSource(conn, exportParam);
        } catch (Exception ex) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ex.getMessage() , ex);}
            throw new NestedRuntimeException(ex);
        }
    }

	/** @return /jobs/wbalert/clockingAlertParams.jsp */
    public String getTaskParametersUI() {
        return "/jobs/wbalert/clockingAlertParams.jsp";
    }
}
