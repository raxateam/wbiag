package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.wbiag.app.ta.quickrules.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
/**
 * Custom event for Saving calclogs to the DB. This is done in AfterSave because
 *   <ul>
 *   <li> as of 4.1, work summary is not saved to DB when creating def records, therefore trying
 *      to create records when no work summary is present creates errorr
 *   <li> optimized insert process by batching it through RecordAccess
 *   </ul>
 */
public class CDataEventCalcLog extends DataEvent {

    public void afterSave(WBData data, DBConnection c)   {
        processCalcLog(data , c);
    }

    protected void processCalcLog(WBData data, DBConnection c)   {
        TreeMap calcLogsMap = (TreeMap)data.getRuleData().getCalcDataCache()
            .getEntityCache().get(DebugRule.CALC_LOG_CACHE_NAME);
        if (calcLogsMap == null) {
            return;
        }
        List allCalcLogs = new ArrayList();
        Iterator iter = calcLogsMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            Integer wrksId = (Integer)entry.getKey();
            List calcLogs = (List)entry.getValue();
            allCalcLogs.addAll(calcLogs) ;
        }

        if (allCalcLogs.size() > 0) {
            new RecordAccess(data.getDBconnection()).insertRecordData(allCalcLogs ,
                "CALC_LOG");
        }
    }

}
