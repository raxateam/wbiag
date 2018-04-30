/*
 * Created on Feb 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.wbiag.server.data.source;

import com.workbrain.server.data.*;
import com.workbrain.sql.*;

import java.sql.*;

/**
 * @author wwoo
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class SOPlannedVsActualRowSourceBuilder implements RowSourceBuilder {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( SOPlannedVsActualRowSourceBuilder.class );

    public SOPlannedVsActualRowSourceBuilder()
    {
    }
    
    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSourceBuilder#getName()
     */
    public String getName() {
        // TODO Auto-generated method stub
        return "SO PLANNED VS ACTUAL";
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSourceBuilder#getParameters()
     */
    public ParameterList getParameters() {
        
        // TODO Auto-generated method stub
        // build the parameter list
        ParameterListImpl params = new ParameterListImpl();

        params.add( new ParameterImpl( "connection", Connection.class.getName(),
                true, true, "Database Connection" ) );
        
        params.add( new ParameterImpl( "startDate", String.class.getName(),
                false, false, "Start Date" ) );
        
        params.add( new ParameterImpl( "endDate", String.class.getName(),
                false, false, "End Date" ) );
        
        params.add( new ParameterImpl( "locationID", String.class.getName(),
                false, false, "Store Location" ) );

        return params;
    }

    /* (non-Javadoc)
     * @see com.workbrain.server.data.RowSourceBuilder#newInstance(com.workbrain.server.data.ParameterList)
     */
    public RowSource newInstance( ParameterList list ) throws InstantiationException, IllegalArgumentException
    {
        RowSource rs = null;
        // TODO Auto-generated method stub
        // find our params
        Parameter conn = list.findParam( "connection" );
        Parameter startDateParam = list.findParam( "startDate" );
        Parameter endDateParam = list.findParam( "endDate" );
        Parameter locationID = list.findParam( "locationID" );

        // set the connection if it hasn't been set
        if( conn == null )
            throw new IllegalArgumentException( "Connection parameter not set" );
        
        if( startDateParam == null)
            throw new IllegalArgumentException( "Start Date parameter not set" );

        if( endDateParam == null)
            throw new IllegalArgumentException( "End Date parameter not set" );

        if( locationID == null )
            throw new IllegalArgumentException( "Location Store parameter not set" );
        
        // check all parameters
        list.validateParams();
        
        if( logger.isDebugEnabled() )
        {
            logger.debug( "==== Parameter List, size of (" + list.size() + "): ====" );
            for( int i = 0; i < list.size(); i ++ )
                logger.debug( list.get( i ) );
        }

        try
        {
            rs = new SOPlannedVsActualRowSource (
                    ( DBConnection )conn.getValue(),
                    list );
        }
        catch( AccessException ae )
        {
             throw new InstantiationException( ae.toString() );
        }

        return rs;
    }
}
