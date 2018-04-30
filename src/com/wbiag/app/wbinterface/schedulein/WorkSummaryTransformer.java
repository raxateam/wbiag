package com.wbiag.app.wbinterface.schedulein;

import com.workbrain.util.*;
import com.workbrain.sql.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.ImportData;
import com.workbrain.sql.DBConnection;
import java.text.*;

/**
 * Transformer for Work Summary data processing.
 *
 * <p>Copyright: Copyright (c) 2002 Workbrain Inc.</p>
 *
 **/
public class WorkSummaryTransformer
    extends AbstractImportTransformer {
    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkSummaryTransformer.class);

    public void transform(DBConnection conn, ImportData data,
                          DelimitedInputStream is) throws
        ImportTransformerException {
        for (int ii = 1; ii < is.getColumnCount() + 1; ii++) {
            /* do not trim flags field */
            if (ii != 28) {
                addString(ii, is, data, true);
            }
            else if (ii == 28) {
                addString(ii, is, data, false);
            }
        }
        data.setRecNum(is.getColumnCount());

    }
}
