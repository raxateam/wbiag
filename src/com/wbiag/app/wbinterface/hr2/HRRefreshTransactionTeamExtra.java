package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.tool.security.*;
/**
 * Customization for extra team Attributes
 *
 **/
public class HRRefreshTransactionTeamExtra extends  HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionTeamExtra.class);

    private static final int TEAM_EXTRA_COL = 90;
    private static final String WBROLE_NAME_TOKEN = "WBROLE_NAME";
    private static final String WBT_DESC_TOKEN = "WBT_DESC";
    private int[] custColInds = new int[] {TEAM_EXTRA_COL};
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
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            try {
                processTeamExtra(data,   conn);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionTeamExtra." , ex);}
                data.error("Error in HRRefreshTransactionTeamExtra." + ex.getMessage() );
            }
        }
    }


    protected void processTeamExtra(HRRefreshTransactionData data,
                               DBConnection c) throws SQLException, WBInterfaceException {
        String val = data.getImportData().getField(custColInds[0]);
        if (StringHelper.isEmpty(val)
            || StringHelper.isEmpty(data.getImportData().getField(EMPT_NAME_COL))) {
            return;
        }
        Map nameVals = StringHelper.detokenizeStringAsNameValueMap(val ,
            String.valueOf(SEPARATOR_CHAR) , String.valueOf(EQUALS_CHAR)  , true);
        if (nameVals.containsKey(WBROLE_NAME_TOKEN)) {
            processUserTeam((String)nameVals.get(WBROLE_NAME_TOKEN) , data, c);
        }
        if (nameVals.containsKey(WBT_DESC_TOKEN)) {
            processTeamDesc((String)nameVals.get(WBT_DESC_TOKEN) , data, c);
        }
    }

    protected void processUserTeam(String roleName,
                                   HRRefreshTransactionData data,
                                   DBConnection c) throws SQLException, WBInterfaceException {
        // *** only do this if TEAM_EXTRA_COL is not blank in the file
        if (StringHelper.isEmpty(roleName)
            || StringHelper.isEmpty(data.getImportData().getField(EMPT_NAME_COL))
            || StringHelper.isEmpty(data.getImportData().getField(WBU_NAME_COL))
            )  {
            return;
        }
        String teamStr = data.getImportData().getField( EMPT_NAME_COL );
        WorkbrainTeamData leafWbtData = getLeafWbtData(teamStr , c);
        if (leafWbtData == null) {
            throw new RuntimeException ("Workbrain team not found : " +  teamStr);
        }

        WorkbrainRoleData newWBRoleData =
            CodeMapper.createCodeMapper(c).getWBRoleByName(roleName);
        if (newWBRoleData == null) {
            throw new RuntimeException ("Workbrain role not found : " +  roleName);
        }
        int newWBRoleId = newWBRoleData.getWbroleId();

        String wbuId = WorkbrainUserAccess.retrieveWbuIdByWbuName(c ,
            data.getImportData().getField( WBU_NAME_COL));
        if (StringHelper.isEmpty(wbuId)) {
            throw new RuntimeException ("Workbrain user not found : "
                                        +  data.getImportData().getField( WBU_NAME_COL));
        }

        SecurityUser.addTeam(conn, new Integer(leafWbtData.getWbtId()),
                 new Integer(wbuId),
                 new Integer(newWBRoleId),
                 null ,
                 true,
                 true,
                 new java.sql.Date(data.getOvrStartDate().getTime()) ,
                 new java.sql.Date(data.getOvrEndDate().getTime()),
                 null, null, null, null, null,
                 null, null, null, null, null);
    }

    protected void processTeamDesc(String teamDesc,
                                   HRRefreshTransactionData data,
                                   DBConnection c) throws SQLException, WBInterfaceException {
        // *** only do this if TEAM_EXTRA_COL is not blank in the file
        if (StringHelper.isEmpty(teamDesc)
            || StringHelper.isEmpty(data.getImportData().getField(EMPT_NAME_COL))
            )  {
            return;
        }
        String teamStr = data.getImportData().getField( EMPT_NAME_COL );
        WorkbrainTeamData leafWbtData = getLeafWbtData(teamStr , c);
        if (leafWbtData == null) {
            throw new RuntimeException ("Workbrain team not found : " +  teamStr);
        }
        leafWbtData.setWbtDesc(teamDesc);
        new WorkbrainTeamAccess(c).update(leafWbtData);
    }

    private WorkbrainTeamData getLeafWbtData(String teamStr , DBConnection c) throws SQLException{
        WorkbrainTeamData ret = null;
        if( teamStr.startsWith( String.valueOf( SEPARATOR_CHAR ) + SEPARATOR_CHAR ) ) {
            teamStr = teamStr.substring( 2 );
        } else if( teamStr.charAt( 0 ) == SEPARATOR_CHAR ) {
            teamStr = teamStr.substring( 1 );
        }
        String[] teams = StringHelper.detokenizeString(teamStr , String.valueOf(SEPARATOR_CHAR));
        String leafWbtName = teams[teams.length -1];
        ret =
            CodeMapper.createCodeMapper(c).getWBTeamByName(leafWbtName);
        return ret;
    }

}