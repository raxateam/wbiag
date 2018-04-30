package com.wbiag.app.export.hr;

import com.workbrain.sql.*;
import com.workbrain.server.data.*;
import com.workbrain.server.data.sql.*;
import com.workbrain.server.data.type.*;

import com.workbrain.tool.renderer.bo.*;
import com.workbrain.util.*;
import com.workbrain.tool.security.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;

import java.math.*;
import java.util.*;
import java.sql.*;
import java.text.*;
import com.workbrain.app.export.hr.*;
import com.workbrain.app.modules.entitlements.*;
import com.workbrain.app.wbinterface.mapping_rowsource.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.export.process.*;

public class HRExportRowSourceExt extends HRExportRowSource{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRExportRowSourceExt.class);

    // *** parameter to export as of records i.e employee balances as of 11/01/2006 format= MM/dd/yyyy
    // *** default is current date. The parameter has no affect of how records are selected when
    // *** perform difference is selected
    public  static final String PARAM_EXPORT_AS_OF_DATE = "exportAsOfDate";


    protected HRExportRowSourceExt() {
    }

    /**
     * @deprecated as of June 16, 2005.
     * @see #HRExportRowSourceExt(DBConnection, Map)
     */
    public HRExportRowSourceExt(DBConnection dbConn,  HashMap exportParam) throws AccessException {
        // Deprecated as a result of TT42979.
        super(dbConn, (Map)exportParam);
    }

    public HRExportRowSourceExt(DBConnection dbConn,  Map exportParam) throws AccessException {
        super(dbConn , exportParam);
        if (exportParam.containsKey(PARAM_EXPORT_AS_OF_DATE)
            && !StringHelper.isEmpty(exportParam.get(PARAM_EXPORT_AS_OF_DATE))) {
            super.exportDate = DateHelper.convertStringToDate((String)exportParam.get(PARAM_EXPORT_AS_OF_DATE),
                "MM/dd/yyyy");
        }
    }

}


