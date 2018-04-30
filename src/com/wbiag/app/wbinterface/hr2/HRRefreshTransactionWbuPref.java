package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.model.RecordData;
import com.wbiag.app.ta.model.WorkbrainUserPreferenceData;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.app.ta.db.WorkbrainUserAccess;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.StringHelper;
import com.workbrain.util.NameValue;

/**
 * Inserts updates workbrain_user_preference based on format PREF_NAME=val~PREF_NAME1=val1
 * in a user defined columns
 * i.e TOC_VIEW_STATE=false~ETM_STARTING_PAGE=/etm/etmMenu.jsp
 *
 **/
public class HRRefreshTransactionWbuPref extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionWbuPref.class);

    public static final String TABLE_WORKBRAIN_USER_PREFERENCE = "WORKBRAIN_USER_PREFERENCE";
    private static final int USER_COL = 90;
    private int[] custColInds = new int[] {USER_COL};
    private DBConnection conn = null;

    /**
     * Sets custom column indexes to be used by the customization. Indexes start at 0
     * @param inds
     */
    public void setCustomColInds(int[] inds) {
        custColInds = inds;
    }

    /**
     * Override this class to customize after process batch events.
     * At this time, all HRRefreshTransactionData data has been processed.
     * All processed is available through <code>HRRefreshCache</code>.
     *
     * @param conn                          DBConnection
     * @param hrRefreshTransactionDataList  List of <code>HRRefreshTransactionData</code>
     * @param process                       HRRefreshProcessor
     * @throws Exception
     */
    public void postProcessBatch(DBConnection conn,
                                    List hrRefreshTransactionDataList,
                                    HRRefreshProcessor process) throws Exception {
        if (hrRefreshTransactionDataList == null || hrRefreshTransactionDataList.size() == 0) {
                return;
        }
        if (custColInds == null || custColInds.length != 1) {
            throw new WBInterfaceException ("Custom column index not supplied or too many, must be 1");
        }
        this.conn = conn;
        Map wbPrefs = loadWbPrefs(conn);
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            try {
                processWbuPref(data.getImportData().getField(custColInds[0]),
                              data, conn, wbPrefs);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionWbuPref." , ex);}
                data.error("Error in HRRefreshTransactionWbuPref." + ex.getMessage() );
            }
        }
    }

    private Map loadWbPrefs(DBConnection conn) throws SQLException{
        Map ret = new HashMap();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT wbpref_id, wbpref_name FROM workbrain_preference");
            ps = conn.prepareStatement(sb.toString());

            rs = ps.executeQuery();
            while (rs.next()) {
                ret.put(rs.getString(2).toUpperCase(), new Integer(rs.getInt(1))  ) ;
            }
        }
        finally {
            SQLHelper.cleanUp(ps, rs);
        }
        return ret;
    }

    protected void processWbuPref(String val,
                               HRRefreshTransactionData data,
                               DBConnection c, Map wbPrefs) throws SQLException, WBInterfaceException {
        // *** only do this if not blank in the file
        if (StringHelper.isEmpty(val)) {
            if (logger.isDebugEnabled()) logger.debug("No values supplied for user preferences, empId : " + data.getEmpId());
            return;
        }
        List vals = StringHelper.detokenizeStringAsNameValueList(val, "~", "=", true);
        String sWbuId = WorkbrainUserAccess.retrieveWbuIdByEmpId(conn, data.getEmpId());
        if (StringHelper.isEmpty(sWbuId)) {
            throw new WBInterfaceException ("Workbrain user not found for empId :" + data.getEmpId());
        }
        if (data.isNewEmployee() || data.isNewWorkbrainUser()) {
            insertWbuPrefs(conn , Integer.parseInt(sWbuId)  , vals, wbPrefs);
        }
        else {
            updateWbuPrefs(conn , Integer.parseInt(sWbuId) , vals, wbPrefs);
        }

    }

    protected void insertWbuPrefs(DBConnection conn, int wbuId,
                                List vals, Map wbPrefs) throws WBInterfaceException{
        List insList = new ArrayList();
        Iterator iter = vals.iterator();
        while (iter.hasNext()) {
            NameValue item = (NameValue)iter.next();
            if (!wbPrefs.containsKey(item.getName().toUpperCase())) {
                throw new WBInterfaceException("User Preference not found :" +  item.getName());
            }
            Integer wbPrefId =  (Integer )wbPrefs.get(item.getName().toUpperCase());
            WorkbrainUserPreferenceData wupd = new WorkbrainUserPreferenceData();
            wupd.setWbprefId(wbPrefId.intValue() );
            wupd.setWbuprefValue(item.getValue());
            wupd.setWbuId(wbuId);
            wupd.setGeneratesPrimaryKeyValue(true);
            insList.add(wupd);
        }
        if (insList.size() > 0) {
            new RecordAccess(conn).insertRecordData(insList,
                TABLE_WORKBRAIN_USER_PREFERENCE);
        }
        if (logger.isDebugEnabled()) logger.debug("Inserted : " + insList.size() + " preferences for wbuId : " + wbuId);
    }

    protected void updateWbuPrefs(DBConnection conn, int wbuId,
                                List vals, Map wbPrefs) throws WBInterfaceException{
       List insList = new ArrayList();
       List updList = new ArrayList();
       List existingWbuPrefs = new RecordAccess(conn).loadRecordData(
            new WorkbrainUserPreferenceData() , TABLE_WORKBRAIN_USER_PREFERENCE,
            "wbu_id", wbuId);
       Iterator iter = vals.iterator();
       while (iter.hasNext()) {
           NameValue item = (NameValue)iter.next();
           if (!wbPrefs.containsKey(item.getName().toUpperCase())) {
               throw new WBInterfaceException("User Preference not found :" +  item.getName());
           }
           Integer wbPrefId =  (Integer )wbPrefs.get(item.getName().toUpperCase());
           WorkbrainUserPreferenceData existingWbuPref = findWBUPref(existingWbuPrefs, wbPrefId.intValue()) ;
           if (existingWbuPref != null) {
               if (!StringHelper.equals(existingWbuPref.getWbuprefValue(), item.getValue())) {
                   existingWbuPref.setWbuprefValue(item.getValue());
                   updList.add(existingWbuPref);
               }
           }
           else {
               WorkbrainUserPreferenceData wupd = new
                   WorkbrainUserPreferenceData();
               wupd.setWbprefId(wbPrefId.intValue());
               wupd.setWbuprefValue(item.getValue());
               wupd.setWbuId(wbuId);
               wupd.setGeneratesPrimaryKeyValue(true);
               insList.add(wupd);
           }
       }
       if (insList.size() > 0) {
           new RecordAccess(conn).insertRecordData(insList,
               TABLE_WORKBRAIN_USER_PREFERENCE);
       }
       if (updList.size() > 0) {
           new RecordAccess(conn).updateRecordData(updList,
               TABLE_WORKBRAIN_USER_PREFERENCE, "wbupref_id");
       }
        if (logger.isDebugEnabled()) logger.debug("Inserted : " + insList.size() + " preferences for wbuId : " + wbuId);
        if (logger.isDebugEnabled()) logger.debug("Updated : " + updList.size() + " preferences for wbuId : " + wbuId);
   }

   private WorkbrainUserPreferenceData findWBUPref(List existingWbuPrefs, int wbprefId){
       Iterator iter = existingWbuPrefs.iterator();
       while (iter.hasNext()) {
           WorkbrainUserPreferenceData item = (WorkbrainUserPreferenceData)iter.next();
           if (item.getWbprefId() == wbprefId) {
               return item;
           }
       }
       return null;
   }
}
