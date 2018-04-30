package com.wbiag.app.ta.ruleengine;

import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.OverrideList;
import com.workbrain.app.ta.ruleengine.CalcDataCache;
import com.workbrain.app.ta.ruleengine.DataEvent;
import com.workbrain.app.ta.ruleengine.RuleEngineException;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;
/**
 * 
 * Data events for Itella
 *
 */
public class CDataEventAdjustSkdAfterLTA extends DataEvent {
	private static Logger logger = Logger.getLogger(CDataEventAdjustSkdAfterLTA.class);

    /**
     * Data event to remove schedule detail and recreate unassigned shift after inserting an LTA
     * 
     */
    public void afterLongTermAbsenceOverrideInsert(OverrideData od, DBConnection c) throws RuleEngineException {
        try {
            // For full day LTA
            if( od.getOvrStartTime() == null && od.getOvrEndTime() == null ) {
                CDEAdjustSkdAfterLTA.process( od, c );
            }
        } catch ( SQLException sqle ) {
            throw new RuleEngineException(sqle.getMessage());            
        }
    }

}
