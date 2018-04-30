package com.wbiag.app.ta.db;

import java.sql.*;
import java.util.*;
import java.util.Date;

import org.apache.log4j.*;
import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.cache.*;
import com.workbrain.server.registry.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Subclass of WorkbrainCache used for long-term caching of WBIAG_SPZONE_DATE_EFF tables
 */
public class WbiagSpzoneDateEffCache extends WorkbrainCache {

    private static final Logger logger = Logger.getLogger(WbiagSpzoneDateEffCache.class);

    public static final String CACHE_NAME = "WbiagSpzoneDateEffCache";
    public static final String WBIAG_SPZONE_DATE_EFF_TABLE = "WBIAG_SPZONE_DATE_EFF";
    public static final int ACCESS_INTERVAL = 10000;
    public final static String EFF_DATETIME_FORMAT_STR = "MM/dd/yyyy HH:mm";

    private static final WbiagSpzoneDateEffCache instance = new WbiagSpzoneDateEffCache(CACHE_NAME);

    public static final WbiagSpzoneDateEffCache getInstance() {
        instance.setAccessInterval(ACCESS_INTERVAL);
        return instance;
    }

    private WbiagSpzoneDateEffCache(String name) {
        super(name);
    }

    public void updateCacheContents(Connection conn) {
        logger.debug("Reloading data");
        put(WBIAG_SPZONE_DATE_EFF_TABLE, new HashMap());
    }

    /**
     * Returns ShiftPremiumZoneData for given spzoneId, checks if there are any overrides in WBIAG_SPZONE_DATE_EFF table
     * @param conn
     * @param spzoneId
     * @param effDate
     * @return
     * @throws Exception
     */
    public ShiftPremiumZoneData getSpzoneByDate(DBConnection conn,
        int spzoneId, Date effDate) throws Exception {

        CodeMapper cm = CodeMapper.createCodeMapper(conn);
        ShiftPremiumZoneData spz = cm.getShiftPremiumZoneById(spzoneId);
        if (spz == null) {
            throw new RuntimeException ("Shift premium zone not found :" +  spzoneId);
        }
        WbiagSpzoneDateEffData eff = getSpzoneBySpzoneIdDate(conn, spzoneId, effDate);
        // *** no eff date, return original record
        if (eff == null) {
            return spz;
        }
        if (logger.isDebugEnabled()) logger.debug("Premium zone :" + spz.getSpzoneName() + " has an effective date override as of " + eff.getSpdeEffDate());

        ShiftPremiumZoneData ret = spz;
        String val = eff.getSpdeValue();
        List nameVals = StringHelper.detokenizeStringAsNameValueList(val, ",", "=", true);
        Iterator iter = nameVals.iterator();
        while (iter.hasNext()) {
            NameValue item = (NameValue)iter.next();
            String name = ret.getFieldPropertyName(item.getName());
            Object value = item.getValue();

            Class paramType = ret.getPropertyDescriptor(name).getWriteMethod().getParameterTypes()[0];
            if (paramType.isAssignableFrom(Date.class)) {
                if (StringHelper.isEmpty(value.toString())) {
                    value = null;
                } else {
                    boolean dateParsed = false;
                    Date dateValue = null;
                    boolean valueIsTimestamp = value.toString().length() > 10;
                    if (valueIsTimestamp) {
                        try {
                            dateValue = DateHelper.convertStringToDate(value.
                                toString(), EFF_DATETIME_FORMAT_STR);
                        }
                        catch (Exception pe) {
                            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("com.wbiag.app.ta.db.WbiagSpzoneDateEffCache: Parse exception when parsing override data", pe);
                            throw new RuntimeException("Error when parsing :" +   value, pe);
                        }
                        value = dateValue;
                    }
                }
            }
            spz.setProperty(name, value, true);
            if (logger.isDebugEnabled()) logger.debug("Changing " + name + " to " + value );
        }
        return spz;
    }

    /**
     * Returns WbiagSpzoneDateEffData for given spzoneId by EffData. Loads full cache if empty
     * @param conn
     * @param stateName state abbreviation i.e ON
     * @return
     */
    public WbiagSpzoneDateEffData getSpzoneBySpzoneIdDate(DBConnection conn,
        int spzoneId, Date effDate) {
        WbiagSpzoneDateEffData ret = null;

        List zones  = getSpzoneBySpzoneId (conn , spzoneId);

        if (zones != null && zones.size() > 0) {
            for ( int i = 0; i < zones.size(); i++ ) {
                WbiagSpzoneDateEffData data = (WbiagSpzoneDateEffData)zones.get( i );
                // *** find the first  prior to effectiveDate
                if ( DateHelper.compare( data.getSpdeEffDate(), effDate ) <= 0 ) {
                    try {
                        ret = (WbiagSpzoneDateEffData) data.clone();
                        break;
                    }
                    catch (CloneNotSupportedException ex) {
                        throw new NestedRuntimeException(ex);
                    }
                }
            }

        }

        return ret;
    }

    /**
     * Returns WbiagSpzoneDateEffData for given spzoneId. Loads full cache if empty
     * @param conn
     * @param spzoneId spzoneId
     * @return
     */
    public List getSpzoneBySpzoneId(DBConnection conn,
        int spzoneId) {
        List ret = null;
        HashMap spzoneEffDates = (HashMap)get(WBIAG_SPZONE_DATE_EFF_TABLE , conn);
        if (spzoneEffDates.size() == 0) {
            RecordAccess ra = new RecordAccess(conn);
            List recs = ra.loadRecordDataOrderBy(new WbiagSpzoneDateEffData(),
                              WBIAG_SPZONE_DATE_EFF_TABLE,
                              "",
                              "spzone_id, spde_eff_date DESC");
            Iterator iter = recs.iterator();
            while (iter.hasNext()) {
                WbiagSpzoneDateEffData item = (WbiagSpzoneDateEffData)iter.next();
                Integer key = new Integer(item.getSpzoneId());
                List spzones = (List)spzoneEffDates.get(key);
                if (spzones == null) {
                    spzones = new ArrayList();
                    spzones.add(item);
                }
                spzoneEffDates.put(key , spzones);
            }
        }
        Integer key = new Integer(spzoneId);
        if (spzoneEffDates.containsKey(key)) {
            ret = (List)spzoneEffDates.get(key);
        }
        return ret;
    }


}
