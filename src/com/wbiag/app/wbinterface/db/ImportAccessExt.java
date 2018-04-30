package com.wbiag.app.wbinterface.db;

import java.sql.*;
import java.util.*;
import java.io.IOException;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.server.registry.*;
import javax.naming.*;
/**
 * Provides access to the WBINT_IMPORT table.
 *
 * <p>Copyright: Copyright (c) 2005 Workbrain Inc.</p>
 *
 */



public class ImportAccessExt extends ImportAccess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ImportAccessExt.class);

    /**
     * Constructor.
     *
     * @param c     database connection
     */
    public ImportAccessExt( DBConnection c ) {
        super( c );
    }

    /**
     * Returns import data list for given <code>tranId</code> for a chunk size
     * of <code>fetchsize</code> and status <code>status</code>.
     *
     * @param tranId  tranId
     * @param fetchsize fetchsize
     * @param status import status
     * @return List of Import Data
     * @throws SQLException
     */
    public List loadByTransactionIdWithFetchSizeFiltered(int tranId ,
                                                 int fetchsize,
                                                 String status,
                                                 ImportDataFilter impDatFilter) throws SQLException{
        List ret = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            String sql = makeImportSQL(tranId , fetchsize);
            ImportData data = null;
            boolean done = false;
            while (!done) {
                ps = getDBConnection().prepareStatement(sql);
                ps.setInt(1 , tranId);
                ps.setString(2 , status);
                if (data!=null) {
                    ps.setInt(3, data.getId());
                }

                rs = ps.executeQuery();
                boolean hasData = false;
                while (rs.next()) {
                    if (!hasData)
                        hasData = true;
                    data = new ImportData();
                    mapToData(data , rs);
                    if (impDatFilter.accept(data)) {
                        ret.add(data);
                        if (ret.size()==fetchsize) {
                            done = true;
                            break;
                        }
                    }
                }
                rs.close();
                ps.close();
                if (!hasData) {
                    break; // no more data returned in the query.
                }
                sql = makeImportSQL(tranId, fetchsize, new Integer(data.getId()));
            }
        }
        finally {
            if (ps != null) ps.close();
            if (rs != null) rs.close();
        }
        return ret;
    }


 }
