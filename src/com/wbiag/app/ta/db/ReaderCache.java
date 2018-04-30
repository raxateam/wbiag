package com.wbiag.app.ta.db;

import java.sql.*;
import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.cache.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Subclass of WorkbrainCache used for long-term caching of READER and READER_GROUP tables
 */
public class ReaderCache extends WorkbrainCache {

    private static final Logger logger = Logger.getLogger(ReaderCache.class);

    public static final String CACHE_NAME = "ReaderCache";
    public static final String READER_TABLE = "READER";
    public static final String READER_TABLE_ID = "READER_ID";
    public static final String READER_TABLE_NAMES = "READER_NAMES";
    public static final String READER_GROUP_TABLE = "READER_GROUP";
    public static final String READER_GROUP_TABLE_ID = "READER_ID";
    public static final String READER_GROUP_TABLE_NAMES = "READER_NAMES";
    public static final int ACCESS_INTERVAL = 10000;

    private static final ReaderCache instance = new ReaderCache(CACHE_NAME);

    public static final ReaderCache getInstance() {
        instance.setAccessInterval(ACCESS_INTERVAL);
        return instance;
    }

    private ReaderCache(String name) {
        super(name);
    }

    public void updateCacheContents(Connection conn) {
        logger.debug("Reloading data");

        put(READER_TABLE, new HashMap());
        put(READER_GROUP_TABLE, new HashMap());
    }

    /**
     * Returns reader data for given reader name
     * @param conn
     * @param rdrName reader name
     * @return
     */
    public ReaderData getReaderData(DBConnection conn,
        String rdrName) {
        ReaderData ret = null;

        Map readersAll = loadReaders(conn);
        if (readersAll.containsKey(READER_TABLE_NAMES)) {
            Map readers = (Map)readersAll.get(READER_TABLE_NAMES);
            ReaderData item = (ReaderData)readers.get(rdrName);
            if (item != null) {
                try {
                    ret = (ReaderData) item.clone();
                }
                catch (CloneNotSupportedException ex) {
                    throw new NestedRuntimeException(ex);
                }
            }
        }
        return ret;
    }

    /**
     * Returns reader data for given reader id
     * @param conn
     * @param rdrName reader name
     * @return
     */
    public ReaderData getReaderData(DBConnection conn,
        int rdrId) {
        ReaderData ret = null;

        Map readersAll = loadReaders(conn);
        if (readersAll.containsKey(READER_TABLE_ID)) {
            Map readers = (Map)readersAll.get(READER_TABLE_ID);
            ReaderData item = (ReaderData)readers.get(new Integer(rdrId));
            if (item != null) {
                try {
                    ret = (ReaderData) item.clone();
                }
                catch (CloneNotSupportedException ex) {
                    throw new NestedRuntimeException(ex);
                }
            }
        }
        return ret;
    }

    /**
     * Returns reader group name for given reader groupid
     * @param conn
     * @param rdrgrpName reader group name
     * @return
     */
    public Integer getReaderGroupId(DBConnection conn,
        String rdrgrpName) {
        Integer ret = null;

        Map readerGrpsAll = null;
        try {
            readerGrpsAll = loadReaderGroups(conn);
        }
        catch (SQLException ex) {
            logger.error("Error when querying READER_GROUP" , ex);
            throw new NestedRuntimeException(ex);
        }
        if (readerGrpsAll.containsKey(READER_GROUP_TABLE_NAMES)) {
            Map readers = (Map)readerGrpsAll.get(READER_GROUP_TABLE_NAMES);
            Integer item = (Integer)readers.get(rdrgrpName);
            if (item != null) {
                ret = item;
            }
        }
        return ret;
    }

    /**
     * Returns reader group name for given reader group name
     * @param conn
     * @param rdrgrpId reader group id
     * @return
     */
    public String getReaderGroupName(DBConnection conn,
        int rdrgrpId) {
        String ret = null;

        Map readerGrpsAll = null;
        try {
            readerGrpsAll = loadReaderGroups(conn);
        }
        catch (SQLException ex) {
            logger.error("Error when querying READER_GROUP" , ex);
            throw new NestedRuntimeException(ex);
        }
        if (readerGrpsAll.containsKey(READER_GROUP_TABLE_ID)) {
            Map readers = (Map)readerGrpsAll.get(READER_GROUP_TABLE_ID);
            String item = (String)readers.get(new Integer(rdrgrpId));
            if (item != null) {
                ret = item;
            }
        }
        return ret;
    }

    private Map loadReaders(DBConnection conn) {
        HashMap readersAll = (HashMap)get(READER_TABLE , conn);
        if (readersAll.size() == 0) {
            Map readerIds = new HashMap();
            Map readerNames = new HashMap();
            readersAll.put(READER_TABLE_ID, readerIds);
            readersAll.put(READER_TABLE_NAMES, readerNames);
            RecordAccess ra = new RecordAccess(conn);
            List recs = ra.loadRecordData(new ReaderData(),
                              READER_TABLE,
                              "");
                System.out.println("loading r");
            Iterator iter = recs.iterator();
            while (iter.hasNext()) {
                ReaderData item = (ReaderData)iter.next();
                readerNames.put(item.getRdrName() , item);
                readerIds.put(new Integer(item.getRdrId()) , item);
            }
        }
        return readersAll;
    }

    private Map loadReaderGroups(DBConnection conn) throws SQLException {
        HashMap readerGrpsAll = (HashMap)get(READER_GROUP_TABLE , conn);
        if (readerGrpsAll.size() == 0) {
            Map readerGrpIds = new HashMap();
            Map readerGrpNames = new HashMap();
            readerGrpsAll.put(READER_GROUP_TABLE_ID, readerGrpIds);
            readerGrpsAll.put(READER_GROUP_TABLE_NAMES, readerGrpNames);

            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                System.out.println("loading rd");
                StringBuffer sb = new StringBuffer(200);
                sb.append("SELECT rdrgrp_id, rdrgrp_name FROM reader_group");
                ps = conn.prepareStatement(sb.toString());
                rs = ps.executeQuery();
                while (rs.next()) {
                    readerGrpNames.put(rs.getString(2)  , new Integer(rs.getInt(1)) );
                    readerGrpIds.put(new Integer(rs.getInt(1)) , rs.getString(2));
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }
        }
        return readerGrpsAll;
    }

}
