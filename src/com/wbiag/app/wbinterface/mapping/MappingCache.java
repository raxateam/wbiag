package com.wbiag.app.wbinterface.mapping;

import java.sql.*;
import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.mapping.*;
import com.workbrain.server.cache.*;
import com.workbrain.sql.*;
/**
 * Subclass of WorkbrainCache used for long-term caching of WBINT_MAPPING.
 * Only applicable to mappings that comform general mapping schema
 */
public class MappingCache extends WorkbrainCache {

    private static Logger logger = Logger.getLogger(MappingCache.class);

    public static final String CACHE_NAME = "MappingCache";
    public static final String WBINT_MAPPING_TABLE = "WBINT_MAPPING";
    public static final int ACCESS_INTERVAL = 10000;

    private static final MappingCache instance = new MappingCache(CACHE_NAME);

    public static final MappingCache getInstance() {
        instance.setAccessInterval(ACCESS_INTERVAL);
        return instance;
    }

    private MappingCache(String name) {
        super(name);
    }

    public void updateCacheContents(Connection conn) {
        logger.debug("Reloading data");

        put(WBINT_MAPPING_TABLE, new HashMap());
    }

    public InterfaceMapping getWBIntMapping(DBConnection conn,
        String mappingName) throws Exception{
        InterfaceMapping ret = null;

        HashMap mappings = (HashMap)get(WBINT_MAPPING_TABLE , conn);
        String key = mappingName;
        if (!mappings.containsKey(key)) {
            MappingAccess mapAccess = new MappingAccess( conn );
            // *** load mapping
            MappingData map = mapAccess.loadByName( mappingName );
            if (map == null) {
                throw new RuntimeException ("Mapping not found : " + mappingName);
            }
            ret = InterfaceMapping.createMappingFromXML( map.getXml());

            mappings.put(key , ret);
        }
        else {
            ret = (InterfaceMapping)mappings.get(key);
        }

        return ret;
    }


}
