package com.wbiag.app.export.process;

import java.sql.*;
import java.util.*;

import com.workbrain.app.export.process.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.mapping_rowsource.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 *
 * Empty transmitter that just sets transaction to applied for
 * export that leave data in staging table
 */

public class EmptyTransmitter implements Transmitter {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmptyTransmitter.class);
    String nameFields = null;

    public void execute( DBConnection conn, Map params )  throws WBInterfaceException {

        int wbitranId = ((Integer) params.get( RowSourceExportProcessor.PARAM_TRANSACTION_ID)).intValue();
        // *** if it needs to be applied. Not recommended
        try {
            new ExportAccess(conn).updateExportByTransactionId(wbitranId,
                ExportData.STATUS_APPLIED,
                null,
                ExportData.STATUS_PENDING);
        }
        catch (SQLException ex) {
            throw new NestedRuntimeException("Error in updating transaction to APPLIED", ex);
        }

        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("EmptyTransmitter completed processing sucessfully!");}
     }

     /**
      * Returns transmitterUI jsp path.
      *
      * @return path
      */
     public String getTransmitterUI() {
        return null;
     }
}
