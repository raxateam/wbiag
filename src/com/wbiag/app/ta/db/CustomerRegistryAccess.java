package com.wbiag.app.ta.db;

import com.workbrain.sql.*;
import com.workbrain.app.ta.db.*;
import com.wbiag.app.ta.model.*;

import java.util.*;
import java.sql.SQLException;

/**
 * Provides database access for CustomerRegistry table
 */
public class CustomerRegistryAccess extends RecordAccess {
    /**
     * Sets logger for class
     */
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CustomerRegistryAccess.class);
    /**
     * name of table
     */
    public static final String REGISTRY_TABLE = "WORKBRAIN_REGISTRY";
    /**
     * Name of primary key
     */
    public static final String REGISTRY_KEY = "WBREG_ID";
    /**
     * Sequence name
     */
    public static final String REGISTRY_SEQ = "SEQ_WBREG_ID";

    /**
     * Constructor for class
     * Creates new object with supplied database connection
     * 
     * @param c 
     */
    public CustomerRegistryAccess( DBConnection c ) {
        super( c );
    }

    /**
     * Loads registry entry from table by ID
     * 
     * @param wbregId ID of registry entry
     * @return Requested record
     */
    public CustomerRegistryData load( int wbregId ) {
        CustomerRegistryData data = new CustomerRegistryData();
        List records = loadRecordData( data, REGISTRY_TABLE,
                                       REGISTRY_KEY + " = " + wbregId );
        if( records.size() > 0 ) {
            return (CustomerRegistryData) records.get( 0 );
        }

        return null;
    }

    /**
     * Gets ID from Registry table for Workbrain Parameters
     * 
     * @return WorkbrainParameterId
     */
    public int getWorkbrainParameterId() {
        String where = "WBREG_NAME = 'customer'";

        CustomerRegistryData data = new CustomerRegistryData();
        List records = loadRecordData( data, REGISTRY_TABLE, where );
        if( records.size() > 0 ) {
            return ((CustomerRegistryData) records.get( 0 )).getWbregId().intValue();
        }

        return -1;
    }

    /**
     * Loads parameters by given name
     * 
     * @param wbregName Name of registry entry to return
     * @return Requested record
     */
    public CustomerRegistryData loadParameterByName( String wbregName ) {
        String where = "WBREG_NAME = '" + wbregName + "' AND WBREG_PARENT_ID IN " +
                       "(SELECT WBREG_ID FROM WORKBRAIN_REGISTRY " +
                       " WHERE WBREG_NAME = 'customer')";

        CustomerRegistryData data = new CustomerRegistryData();
        List records = loadRecordData( data, REGISTRY_TABLE, where );
        if( records.size() > 0 ) {
            return (CustomerRegistryData) records.get( 0 );
        }

        return null;
    }

    /**
     * Loads all records from registry table
     * 
     * @return List of all CustomerRegistryData items
     */
    public List loadAll() {
        return loadRecordData( new CustomerRegistryData(), REGISTRY_TABLE, "" );
    }

    /**
     * Inserts new registry entry into database
     * 
     * @param data New data record to insert
     * @throws SQLException 
     */
    public void insert( CustomerRegistryData data ) throws SQLException {
        data.setWbregId( new Integer( getDBConnection().getDBSequence( REGISTRY_SEQ ).getNextValue() ) );
        insertRecordData( data, REGISTRY_TABLE );
    }

    /**
     * Updates registry record
     * 
     * @param data Record to update
     * @throws SQLException 
     */
    public void update( CustomerRegistryData data ) throws SQLException {
        updateRecordData( data, REGISTRY_TABLE, REGISTRY_KEY );
    }
}
