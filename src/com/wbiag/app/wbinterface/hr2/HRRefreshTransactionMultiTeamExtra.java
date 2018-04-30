package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.tool.security.*;

/**
 * Customization for extra team Attributes with multiple team assignment
 *
 **/
public class HRRefreshTransactionMultiTeamExtra
    extends HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(HRRefreshTransactionMultiTeamExtra.class);

    public static final String TEAM_SEPARATOR = "~";
    public static final String TEAM_TUPLE_SEPARATOR = "|";
    public static final String WBUT_ACTION_ADD = "ADD";
    public static final String WBUT_ACTION_UPDATE = "UPDATE";
    public static final String WBUT_ACTION_DELETE = "DELETE";

    private static final int TEAM_EXTRA_COL = 91;
    private static final int TEAM_DESC_COL = 92;
    private static final int TEAM_TOKEN_COL = 0;
    private static final int ROLE_TOKEN_COL = 1;
    private static final int ACTION_TOKEN_COL = 2;
    private static final String WBT_DESC_TOKEN = "WBT_DESC";
    private static final String WBROLE_NAME_TOKEN = "ROLE";
    private static final String WBTEAM_NAME_TOKEN = "TEAM";
    private static final String WBACTION_NAME_TOKEN = "ACTION";
    private static final String REGISTRY_OVERRIDE_EXISTING_EMPLOYEE_TEAM =
        "OVERRIDE_EXISTING_EMPLOYEE_TEAM";

    private int[] custColInds = new int[] {
        TEAM_EXTRA_COL, TEAM_DESC_COL};
    private DBConnection conn = null;
    protected Number wbuId;
    protected java.sql.Date wbutStartDate = null;
    protected java.sql.Date wbutEndDate = null;
    protected WorkbrainTeamAccess workbrainTeamAccess = null;
    protected boolean overridesExistingUserTeam = false;
    protected boolean isUserTeamRecursive = false;

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
        if (hrRefreshTransactionDataList == null ||
            hrRefreshTransactionDataList.size() == 0) {
            return;
        }
        if (custColInds == null || custColInds.length == 0) {
            throw new WBInterfaceException ("Custom column index not supplied");
        }
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.
                next();
            if (data.isError()) {
                continue;
            }
            WorkbrainRegistryAccess wra = new WorkbrainRegistryAccess(conn);
            WorkbrainRegistryData wrd = wra.loadParameterByName(
                REGISTRY_OVERRIDE_EXISTING_EMPLOYEE_TEAM);
            if (! (wrd == null)) {
                if (wrd.getWbregValue().equalsIgnoreCase("true")) {
                    overridesExistingUserTeam = true;
                }
            }
            try {
                processMultiTeamExtra(data, conn);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionMultiTeamExtra." , ex);}
                data.error("Error in HRRefreshTransactionMultiTeamExtra." + ex.getMessage() );
            }
        }
    }

    protected void processMultiTeamExtra(HRRefreshTransactionData data,
                                         DBConnection c) throws SQLException,
        WBInterfaceException {

        String val = null;
        String desc = null;
        Map decVals = null;
        Map[] nameVals = null;

        CodeMapper cm = CodeMapper.createCodeMapper(c);
        wbutStartDate = new java.sql.Date(data.getOvrStartDate().getTime());
        wbutEndDate = new java.sql.Date(data.getOvrEndDate().getTime());

        val = data.getImportData().getField(custColInds[0]);
        desc = data.getImportData().getField(custColInds[1]);
        if (StringHelper.isEmpty(val) && StringHelper.isEmpty(desc)
            && StringHelper.isEmpty(data.getImportData().getField(EMPT_NAME_COL))) {
            if (logger.isDebugEnabled()) logger.debug("Extra columns and EMPT_NAME_COL was empty");
            return;
        }
        /**
         * Using an Map array to store team tuple information
         */
        if (!StringHelper.isEmpty(val)) {
            nameVals = detokenizeStringAsNameValueMapArray(val,
                TEAM_SEPARATOR, TEAM_TUPLE_SEPARATOR);
            setValidateFieldValues(nameVals, cm);

            String strWbuId = WorkbrainUserAccess.retrieveWbuIdByEmpId(c,
                data.getEmpId());
            if (strWbuId == null) {
                data.error("User record does not exist for emp: " + data.getEmpName());
                return;
            }

            if (!StringHelper.isEmpty(val)) {
                for (int i = 0; i < nameVals.length; i++) {
                    processUserTeam(cm, Integer.parseInt(strWbuId),
                                    (String) nameVals[i].get(WBTEAM_NAME_TOKEN),
                                    (String) nameVals[i].get(WBROLE_NAME_TOKEN),
                                    (String) nameVals[i].get(
                        WBACTION_NAME_TOKEN));
                }
            }
        }
        /**
         * Set team description
         */
        if (!StringHelper.isEmpty(desc)) {
            processTeamDesc( desc, data, c);
        }
    }

    private void setValidateFieldValues(Map[] nameVals, CodeMapper cm) throws
        SQLException, WBInterfaceException {

        String teamName = null;
        String roleName = null;
        String action = null;

        for (int i = 0; i < nameVals.length; i++) {

            //Check Team
            teamName = nameVals[i].get(WBTEAM_NAME_TOKEN).toString();
            WorkbrainTeamData wbtd = cm.getWBTeamByName(teamName);
            if (wbtd == null) {
                throw new WBInterfaceException("Workbrain team not found : " +
                                               teamName);
            }

            //Check Role
            roleName = nameVals[i].get(WBROLE_NAME_TOKEN).toString();
            WorkbrainRoleData wbrd = cm.getWBRoleByName(roleName);
            if (wbrd == null) {
                throw new WBInterfaceException("Workbrain role not found : " +
                                               roleName);
            }

            //Check Action
            action = nameVals[i].get(WBACTION_NAME_TOKEN).toString();
            if (! (action.equalsIgnoreCase(WBUT_ACTION_ADD) ||
                   action.equalsIgnoreCase(WBUT_ACTION_UPDATE) ||
                   action.equalsIgnoreCase(WBUT_ACTION_DELETE))) {
                throw new WBInterfaceException("Invalid Action : " +
                                               roleName);
            }
        }
    }

    protected void processUserTeam(CodeMapper cm, int intWbuId, String teamName, String roleName,
                                   String wbutAction) throws SQLException,
        WBInterfaceException {

        WorkbrainTeamData wbTeam = cm.getWBTeamByName(teamName);
        WorkbrainRoleData wrd = cm.getWBRoleByName(roleName);
        Number wbroleId = Integer.valueOf(String.valueOf(wrd.getWbroleId()));
        Number wbuId = Integer.valueOf(String.valueOf(intWbuId));

        if (wbuId == null || wbTeam == null) {
            throw new WBInterfaceException(
                "User or Team error, no action preformed");
        }
        if (wbutAction.equals(WBUT_ACTION_ADD)) {
            if (wbroleId == null) {
                return;
            }
            SecurityUser.addTeam(conn,
                                 Integer.valueOf(String.valueOf(wbTeam.
                getWbtId())),
                                 wbuId,
                                 wbroleId,
                                 null, isUserTeamRecursive,
                                 overridesExistingUserTeam,
                                 wbutStartDate, wbutEndDate,
                                 null, null, null, null, null,
                                 null, null, null, null, null);
            if (logger.isInfoEnabled()) {
                logger.info("Added 1 user team record. \n");
            }
            // **** update the roles for the team with given user
        }
        else if (wbutAction.equals(WBUT_ACTION_UPDATE)) {
            if (wbroleId == null) {
                return;
            }
            ResultSet rs = null;
            PreparedStatement ps = null;

            String sql = "SELECT count(WBUT_ID) FROM workbrain_user_team " +
                " WHERE wbt_id =  ? " +
                " AND wbrole_id = ? " +
                " AND ? between wbut_start_date and wbut_end_date " +
                " OR ? between wbut_start_date and wbut_end_date ";
            try {
                ps = conn.prepareStatement(sql);
                ps.setInt(1, wbTeam.getWbtId());
                ps.setInt(2, wbroleId.intValue());
                ps.setTimestamp(3, DateHelper.toTimestamp(wbutStartDate));
                ps.setTimestamp(4, DateHelper.toTimestamp(wbutEndDate));
                rs = ps.executeQuery();

                rs.next();
                // *** if there are no roles betweem given dates, no updates
                if (rs.getInt(1) != 0) {

                    SecurityUser.addTeam(conn,
                                         Integer.valueOf(String.valueOf(
                        wbTeam.
                        getWbtId())),
                                         wbuId,
                                         wbroleId,
                                         null,
                                         isUserTeamRecursive,
                                         overridesExistingUserTeam,
                                         wbutStartDate, wbutEndDate,
                                         null, null, null, null, null,
                                         null, null, null, null, null);
                }
            }
            finally {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            }
        }
        else if (wbutAction.equals(WBUT_ACTION_DELETE)) {
            ResultSet rs = null;
            PreparedStatement ps = null;
            int deleteCnt = 0;
            String sql = "SELECT wbut_id FROM workbrain_user_team " +
                " WHERE wbt_id =  " + wbTeam.getWbtId() +
                " AND wbu_id = " + wbuId +
                ( (wbroleId != null) ? " AND wbrole_id = " + wbroleId.longValue() :
                 "");
            try {
                ps = conn.prepareStatement(sql);
                rs = ps.executeQuery();
                while (rs.next()) {
                    SecurityUser.deleteTeam(conn,
                                            Integer.valueOf(rs.getString(1)));
                    deleteCnt++;
                }
            }
            finally {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            }
            if (logger.isInfoEnabled()) {
                logger.info("Deleted " + deleteCnt + " user team records. \n");
            }
        }
        else {
            throw new WBInterfaceException("Unknown user team action : " +
                                           wbutAction);
        }
    }

    protected void processTeamDesc(String teamDesc,
                                   HRRefreshTransactionData data,
                                   DBConnection c) throws SQLException,
        WBInterfaceException {
        String teamStr;
        WorkbrainTeamData leafWbtData;

        // *** only do this if TEAM_EXTRA_COL is not blank in the file
        if (StringHelper.isEmpty(teamDesc)
            || StringHelper.isEmpty(data.getImportData().getField(EMPT_NAME_COL))
            ) {
            return;
        }
        teamStr = data.getImportData().getField(EMPT_NAME_COL);
        leafWbtData = getLeafWbtData(teamStr, c);
        if (leafWbtData == null) {
            throw new RuntimeException("Workbrain team not found : " + teamStr);
        }
        leafWbtData.setWbtDesc(teamDesc);
        new WorkbrainTeamAccess(c).update(leafWbtData);
    }

    private WorkbrainTeamData getLeafWbtData(String teamStr, DBConnection c) throws
        SQLException {

        WorkbrainTeamData ret = null;
        String[] teams;
        String leafWbtName;

        if (teamStr.startsWith(String.valueOf(SEPARATOR_CHAR) + SEPARATOR_CHAR)) {
            teamStr = teamStr.substring(2);
        }
        else if (teamStr.charAt(0) == SEPARATOR_CHAR) {
            teamStr = teamStr.substring(1);
        }
        teams = StringHelper.detokenizeString(teamStr,
                                              String.valueOf(SEPARATOR_CHAR));
        leafWbtName = teams[teams.length - 1];
        ret =
            CodeMapper.createCodeMapper(c).getWBTeamByName(leafWbtName);
        return ret;
    }

    private Map[] detokenizeStringAsNameValueMapArray(String val,
        String valueSeparator, String tupleSeparator) throws
        WBInterfaceException {

        String[] tuples;
        String[] values;
        HashMap[] arrayMapping;
        HashMap valueMap;

        tuples = StringHelper.detokenizeString(val, tupleSeparator);
        arrayMapping = new HashMap[tuples.length];

        for (int i = 0; i < tuples.length; i++) {
            values = StringHelper.detokenizeString(tuples[i],
                valueSeparator);
            if (! (values.length == 3)) {
                throw new WBInterfaceException(
                    "Invalid Multiple User Team String format");
            }
            valueMap = new HashMap();
            valueMap.put(WBTEAM_NAME_TOKEN, values[TEAM_TOKEN_COL]);
            valueMap.put(WBROLE_NAME_TOKEN, values[ROLE_TOKEN_COL]);
            valueMap.put(WBACTION_NAME_TOKEN, values[ACTION_TOKEN_COL]);
            arrayMapping[i] = valueMap;
        }
        return arrayMapping;
    }

}
