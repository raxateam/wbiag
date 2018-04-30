package com.wbiag.app.modules.retailSchedule.db;

import java.util.List;
import com.wbiag.app.modules.retailSchedule.model.ForecastGroupExtTypeData;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.sql.DBConnection;

/**
 * @author bchan
 * 
 * Provides Record Access to ForecastGroupExtTypeData
 *
 */
public class ForecastGroupExtTypeAccess extends RecordAccess {

    public static final String WBIAG_FCAST_GROUP_TYPE_TABLE = "WBIAG_FCAST_GROUP_TYPE";
    public static final String WBIAG_FCAST_GROUP_TYPE_PRI_KEY = "FCASTGRPTYP_ID";
    public static final String WBIAG_FCAST_GROUP_TYPE_NAME = "FCASTGRPTYP_NAME";
    public static final String WBIAG_FCAST_GROUP_TYPE_DESC = "FCASTGRPTYP_DESC";
    public static final String WBIAG_FCAST_GROUP_TYPE_FORMULA = "FCASTGRPTYP_FORMULA";
    public static final String WBIAG_FCAST_GROUP_TYPE_SEQ = "SEQ_FCASTGRPTYP_ID";

    /**
     * Constructor for ForecastGroupExtTypeAccess
     * Creates new object using specified database connection
     * 
     * @param c DBConnection
     */
    public ForecastGroupExtTypeAccess( DBConnection c ) {
        super( c );
    }

    /**
     * Loads extended forecast group data by FCASTGRPTYP_ID id
     * 
     * @param FCASTGRPTYP_ID Id of WBIAG_FCAST_GROUP_EXT to load
     * @return WowFCastExtData
     */
    public ForecastGroupExtTypeData load( int forecastgrptypId ) {
    	ForecastGroupExtTypeData data = new ForecastGroupExtTypeData();
        List records = loadRecordData( data, WBIAG_FCAST_GROUP_TYPE_TABLE, WBIAG_FCAST_GROUP_TYPE_PRI_KEY + " = " + forecastgrptypId );
        if( records.size() > 0 ) {
            return (ForecastGroupExtTypeData) records.get( 0 );
        }
        return null;
    }
    
    /**
     * Loads extended forecast group type data by name
     * 
     * @param fcastgrpId Id of WBIAG_FCAST_GROUP_TYPE to load
     * @return ForecastGroupExtTypeData
     */
    public ForecastGroupExtTypeData loadByFCastGrpName( int fcastgrptypName ) {
    	ForecastGroupExtTypeData data = new ForecastGroupExtTypeData();
        List records = loadRecordData( data, WBIAG_FCAST_GROUP_TYPE_TABLE, WBIAG_FCAST_GROUP_TYPE_NAME + " = " + fcastgrptypName );
        if( records.size() > 0 ) {
            return (ForecastGroupExtTypeData) records.get( 0 );
        }
        return null;
    }

    /**
     * Loads all extended forecast group types from database that have the given type
     * in the formula field
     * 
     * @return List of all extended forecasts types in database
     */
    public List loadTypeInFormula(String fcastgrptypName) {
    	ForecastGroupExtTypeData data = new ForecastGroupExtTypeData();
        return loadRecordData( data, WBIAG_FCAST_GROUP_TYPE_TABLE, WBIAG_FCAST_GROUP_TYPE_FORMULA + " LIKE '%" + fcastgrptypName + "%'");
    }
    
    /**
     * Loads extended forecast group type data for all locations that are children
     * of given location, including the given location
     * @param skdgrpId
     * @return List of ForecastGroupExtTypeData
     */
    public List loadByChildSkdgrpId( int skdgrpId ) {
    	String whereClause = "FCASTGRPTYP_ID IN "
    		+ "(SELECT FCASTGRPTYP_ID "
    		+ "FROM SO_SCHEDULE_GROUP A, WBIAG_FCAST_GROUP_EXT B "
    		+ "WHERE A.FCASTGRP_ID = B.FCASTGRP_ID "
    		+ "AND A.SKDGRP_ID IN "  
    		+ "     ("
			+ "      SELECT SKDGRP_ID"
			+ "      FROM SO_SCHEDULE_GROUP, WORKBRAIN_TEAM"
			+ "      WHERE WORKBRAIN_TEAM.WBT_ID = SO_SCHEDULE_GROUP.WBT_ID"
			+ "      AND EXISTS"
			+ "      (SELECT CHILD_WBT_ID FROM SEC_WB_TEAM_CHILD_PARENT"
			+ "       WHERE PARENT_WBT_ID = (SELECT WBT_ID FROM SO_SCHEDULE_GROUP"
			+ "                              WHERE SKDGRP_ID = " + skdgrpId + ")"
			+ "       AND SEC_WB_TEAM_CHILD_PARENT.CHILD_WBT_ID = SO_SCHEDULE_GROUP.WBT_ID)"
    		+ "     )"
    		+ ")";

    	ForecastGroupExtTypeData data = new ForecastGroupExtTypeData();
        return loadRecordData( data, WBIAG_FCAST_GROUP_TYPE_TABLE, whereClause);

    }
    
    /**
     * Loads extended forecast group type data by location
     * 
     * @param skdgrpId Id of SO_SCHEDULE_GROUP to load
     * @return ForecastGroupExtTypeData
     */
    public ForecastGroupExtTypeData loadBySkdgrpId( int skdgrpId ) {
    	String whereClause = "FCASTGRPTYP_ID IN "
    		+ "(SELECT FCASTGRPTYP_ID "
    		+ "FROM SO_SCHEDULE_GROUP A, WBIAG_FCAST_GROUP_EXT B "
    		+ "WHERE A.FCASTGRP_ID = B.FCASTGRP_ID "
    		+ "AND A.SKDGRP_ID = " + skdgrpId + ")";
    	
    	ForecastGroupExtTypeData data = new ForecastGroupExtTypeData();
        List records = loadRecordData( data, WBIAG_FCAST_GROUP_TYPE_TABLE, whereClause);
        if( records.size() > 0 ) {
            return (ForecastGroupExtTypeData) records.get( 0 );
        }
        return null;
    }
    /**
     * Loads all extended forecasts types from database
     * 
     * @return List of all extended forecasts in database
     */
    public List loadAll() {
        return loadRecordData( new ForecastGroupExtTypeData(), WBIAG_FCAST_GROUP_TYPE_TABLE, "" );
    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}

