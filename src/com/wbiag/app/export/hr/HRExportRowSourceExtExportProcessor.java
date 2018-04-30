package com.wbiag.app.export.hr;

import java.util.*;

import com.workbrain.sql.*;
import com.workbrain.server.data.*;
import com.workbrain.app.export.hr.*;
import com.workbrain.app.export.process.*;
import com.workbrain.server.data.source.*;
import com.workbrain.server.data.sql.*;
import com.workbrain.util.NestedRuntimeException;
import com.workbrain.server.registry.*;

public class HRExportRowSourceExtExportProcessor implements ExportTransactionType {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRExportRowSourceExtExportProcessor.class);
    public HRExportRowSourceExtExportProcessor() {
    }

    public RowSource getRowSource(Map param, DBConnection conn) {
        try {
            return new HRExportRowSourceExt(conn, (Map)param);
        } catch (Exception ex) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ex.getMessage() , ex);}
            throw new NestedRuntimeException(ex);
        }
    }


    public String getTaskUI() {
        return "/jobs/wbiag/wbtransExtExportParams.jsp";
    }

    public void initializeTransaction(DBConnection conn) throws Exception {
    }

    public void finalizeTransaction(DBConnection conn) throws Exception {
    }

}

