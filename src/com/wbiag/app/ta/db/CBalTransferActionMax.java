package com.wbiag.app.ta.db ;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.util.*;
import java.sql.*;

/**
 * Balance Transfer Actions up to a maximum stored in baltr_udf1
 */
public class CBalTransferActionMax implements BalanceTransferAction{

    public static final String BALFROM_MSG = "Balance Transfer From";
    public static final String BALTO_MSG = "Balance Transfer To";

    /**
     * Transfer up to max
     *
     * @param balTranData
     * @param wbData
     * @return
     * @throws SQLException
     */
    public boolean applyTransfer( BalanceTransferData balTranData, WBData wbData)
        throws SQLException {

            double val = wbData.getEmployeeBalanceValue(balTranData.getBalId());
            // *** max should be stored in a udf for ease of maintenance
            double max = 0.0;
            if (!StringHelper.isEmpty(balTranData.getBaltrUdf1())) {
                max = Double.parseDouble(balTranData.getBaltrUdf1());
            }
            else {
                throw new RuntimeException("Maximum value for transfer was not supplied");
            }
            val = Math.min(max, val);

            // *** subtract from
            wbData.addEmployeeBalanceValue(balTranData.getBalId(), -1 * val, BALFROM_MSG);
            // *** add to
            wbData.addEmployeeBalanceValue(balTranData.getBaltrBalId(), val, BALTO_MSG);
            return true;
       }

}
