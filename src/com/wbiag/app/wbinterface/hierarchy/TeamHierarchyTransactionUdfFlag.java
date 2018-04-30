package com.wbiag.app.wbinterface.hierarchy;

import java.sql.SQLException;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.hierarchy.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Transaction type updating team usf and flags
 *
 **/
public class TeamHierarchyTransactionUdfFlag extends TeamHierarchyTransaction{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TeamHierarchyTransactionUdfFlag.class);

    public final static int WBT_UDF1 = 8;
    public final static int WBT_UDF2 = 9;
    public final static int WBT_UDF3 = 10;
    public final static int WBT_UDF4 = 11;
    public final static int WBT_UDF5 = 12;
    public final static int WBT_FLAGS = 13;

    public static final String FLAG_BLANK = "&";
    public static final String WBT_PREFIX = "wbt";
    public static final String WBT_UDF_PREFIX = "wbt_udf";
    public static final String WBT_FLAG_PREFIX = "wbt_flag";

    /**
     * Postprocesses import data. <p>
     * processedData is the manipulated import data and implementors
     * can make use of it to retrieve calculated/formatted values.
     *
     *@param  data          raw import data
     *@param  processedData processed import data
     *@param  conn          conn
     *@throws Exception
     */
    public void postProcess(ImportData data , Object processedData ,
            DBConnection conn) throws Exception{
        if (data.isError()) {
            if (logger.isDebugEnabled()) logger.debug("Import data error, no post process");
            return;
        }
        if (!isAnyUdfFlagSet(data)) {
            if (logger.isDebugEnabled()) logger.debug("No udf/flag set, no post process");
            return;
        }
        processWbtString(data.getField(WBT_STRING));
    }

    protected void processWbtString(String wbtString) throws Exception{
        String finalWbtString = wbtString;

        if( wbtString.startsWith( TEAM_SEPARATOR_CHAR + TEAM_SEPARATOR_CHAR ) ) {
            finalWbtString = wbtString.substring( 2 );
        } else if( wbtString.startsWith (TEAM_SEPARATOR_CHAR)  ) {
            finalWbtString = wbtString.substring( 1 );
        }
        if (StringHelper.isEmpty(finalWbtString)) {
            if (logger.isDebugEnabled()) logger.debug("No team string, no post process");
            return;
        }
        String[] teams = StringHelper.detokenizeString(finalWbtString, TEAM_SEPARATOR_CHAR);
        String leafTeamNameWithType = teams[teams.length - 1];
        String[] leafTeamWithTypeParts = StringHelper.detokenizeString(leafTeamNameWithType,TEAM_TYPE_SEPARATOR_CHAR);
        String leafTeamName = leafTeamWithTypeParts[0].toUpperCase();
        
        WorkbrainTeamData wbtOriginal = CodeMapper.createCodeMapper(conn).getWBTeamByName(leafTeamName);
        if (wbtOriginal == null) {
            if (logger.isDebugEnabled()) logger.debug("Leaf team : " + leafTeamName + " not found, no post process");
            return;
        }
        WorkbrainTeamData wbtUpdate = (WorkbrainTeamData)wbtOriginal.clone();
        for (int k=1 ; k <= 5 ; k++) {
            String wbtUdf = data.getField(WBT_UDF1 + k - 1);
            if (!StringHelper.isEmpty(wbtUdf)) {
                wbtUpdate.setField(WBT_UDF_PREFIX + k, resolveBlank(wbtUdf));
                wbtUpdate.addToAssignedFields(WBT_UDF_PREFIX + k);
            }
        }

        String wbtFlags = data.getField(WBT_FLAGS);
        if (!StringHelper.isEmpty(wbtFlags)) {
            for (int k=1 ; k <= wbtFlags.length() ; k++) {
                String thisFlag = wbtFlags.substring(k-1 , k);
                if (!StringHelper.isEmpty(thisFlag)) {
                    wbtUpdate.setField(WBT_FLAG_PREFIX + k , resolveBlank(thisFlag));
                    wbtUpdate.addToAssignedFields(WBT_FLAG_PREFIX + k);
                }
            }
        }

        if (!wbtUpdate.equalsForAssignedFields(wbtOriginal) ) {
            new WorkbrainTeamAccess(conn).update(wbtUpdate);
        }
        else {
            if (logger.isDebugEnabled()) logger.debug("No udf/flag change not found, no update performed");
            return;
        }        
    }
    
    protected boolean isAnyUdfFlagSet(ImportData data) {
        boolean ret = false;
        for (int k=1 ; k <= 5 ; k++) {
            String wbtUdf = data.getField(WBT_UDF1 + k - 1);
            ret |= !StringHelper.isEmpty(wbtUdf) ;
        }

         ret |= !StringHelper.isEmpty(data.getField(WBT_FLAGS));
         return ret;
    }


    protected String resolveBlank(String val) {
        return StringHelper.isEmpty(val)
               ? null
               : (val.equals(FLAG_BLANK) ? null : val);
    }


}