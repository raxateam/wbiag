package com.wbiag.server.data.sql;

import java.sql.Connection;

import com.workbrain.server.WebLogin;
import com.workbrain.server.data.AccessException;
import com.workbrain.server.data.Parameter;
import com.workbrain.server.data.ParameterList;
import com.workbrain.server.data.RowSource;
import com.workbrain.server.data.sql.*;
import com.wbiag.server.data.source.TableEmployeeExt;
import com.workbrain.sql.DBConnection;
/**
 * Extension class to override core RowSources
 *
 */
public class TableRowSourceBuilderExt extends TableRowSourceBuilder {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TableRowSourceBuilderExt.class);
    public TableRowSourceBuilderExt() {
    }


    public RowSource newInstance (ParameterList list)
    throws InstantiationException, IllegalArgumentException {
        // find our params
        Parameter conn = list.findParam ("connection");
        Parameter table = list.findParam ("tableName");
        // set the connection if it hasn't been set
        if (conn == null)
            throw new IllegalArgumentException ("Connection parameter not set");

        // depending on the table name, create different row sources
        String physicalTableName = (String)table.getValue();
        Connection c = (Connection)conn.getValue();

        String logicalTableName = ((DBConnection)c).physicalToLogical(physicalTableName.toUpperCase()).toUpperCase();

        if (!logicalTableName.equals("EMPLOYEE")){
            return super.newInstance(list);
        }
        else {
            try {
                // check all required parameters
                Parameter login = list.findParam ("login");        
                WebLogin l = (WebLogin)login.getValue();                
                list.validateParams();
                return new TableEmployeeExt(c,l);
            }
            catch (AccessException e) {
                throw new InstantiationException (e.toString());
            }            
        }         
        //} catch (AccessException e) {
        //    throw new InstantiationException (e.toString());
        //}
    }
}