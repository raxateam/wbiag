package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;
import com.workbrain.security.team.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.tool.security.*;

/**
 * Customization for temp team assignment
 * Format is ~team1~team11|~~team2|
 *
 **/
public class HRRefreshTransactionTempTeam
    extends HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(HRRefreshTransactionTempTeam.class);

    public static final char TEAM_HIER_SEPARATOR = '~';
    public static final String TEAM_SEPARATOR = "|";

    private static final int TEMP_TEAM_COL = 90;
    private int[] custColInds = new int[] {TEMP_TEAM_COL};

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
        if (custColInds == null || custColInds.length != 1) {
            throw new WBInterfaceException ("Custom column index not supplied or too many, must be 1");
        }
        this.conn = conn;
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.
                next();
            if (data.isError()) {
                continue;
            }
            try {
                processTempTeam(data, conn);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionTempTeam." , ex);}
                data.error("Error in HRRefreshTransactionTempTeam." + ex.getMessage() );
            }

        }
    }

    protected void processTempTeam(HRRefreshTransactionData data,
                                   DBConnection c) throws SQLException,
        WBInterfaceException {

        String val = data.getImportData().getField(custColInds[0]);
        if (StringHelper.isEmpty(val)) {
            if (logger.isDebugEnabled()) logger.debug("Temp team column was empty");
            return;
        }

        String[] teamHier = StringHelper.detokenizeString(val,
                TEAM_SEPARATOR);
        createTempTeams(teamHier , data);

    }

    private void createTempTeams(String[] teamHier,
                                 HRRefreshTransactionData data) throws
        SQLException, WBInterfaceException {

        String teamName = null;
        String roleName = null;
        String action = null;

        for (int i = 0; i < teamHier.length; i++) {

            String teamStr = teamHier[i];
            if (StringHelper.isEmpty(teamStr)) {
                continue;
            }

            boolean isFullPath = false;

            if( teamStr.startsWith( String.valueOf( TEAM_HIER_SEPARATOR ) + TEAM_HIER_SEPARATOR ) ) {
                isFullPath = true;
                teamStr = teamStr.substring( 2 );
            } else if( teamStr.charAt( 0 ) == TEAM_HIER_SEPARATOR ) {
                teamStr = teamStr.substring( 1 );
            }

            List teamsHieararchy = StringHelper.detokenizeStringAsList(teamStr, String.valueOf(TEAM_HIER_SEPARATOR));

            if (teamsHieararchy.size() > 0) {
                int rootTeamId = updateCreateTeams(teamsHieararchy, isFullPath);
                if (rootTeamId != Integer.MIN_VALUE) {
                    SecurityEmployee.addTempTeam(conn, data.getEmpId(),
                                                 rootTeamId,
                                                 new java.sql.Date(data.
                        getOvrStartDate().getTime()),
                                                 new java.sql.Date(data.
                        getOvrEndDate().getTime()));
                }
            }


        }
    }

    private int updateCreateTeams(List teamHierarchy ,
                                   boolean isFullPath) throws SQLException{

        int ret = Integer.MIN_VALUE;
        if (teamHierarchy == null || teamHierarchy.size() == 0) {
            return ret;
        }

        WorkbrainTeamData rootTeam = getCodeMapper().getWBTeamById( RuleHelper.getRootWorkbrainTeamId(codeMapper) );
        WorkbrainTeamData parentTeam = rootTeam;

        for (int i=0, k=teamHierarchy.size() ; i < k ; i++) {
            String teamName = (String) teamHierarchy.get(i);
            teamName = teamName.toUpperCase();
            WorkbrainTeamData wbTeam = null;
            if (!StringHelper.isEmpty(teamName)) {
                wbTeam = codeMapper.getWBTeamByName(teamName);
            }
            if( wbTeam == null ) {
                wbTeam = new WorkbrainTeamData();
                wbTeam.setWbtDesc(DEFAULT_DESC);
                wbTeam.setWbtName(teamName);
                wbTeam.setWbtParentId(parentTeam.getWbtId());
                WorkbrainTeamManager wtm = new WorkbrainTeamManager(conn);
                try {
                    int addedTeamId = wtm.addTeam(wbTeam.getWbtName(), wbTeam.getWbtDesc(),
                                wbTeam.getWbtParentId(),
                                RuleHelper.getNullTeamTypeId(codeMapper)
                                , false, false);
                    wbTeam.setWbtId(addedTeamId);
                }
                catch (Exception ex) {
                    throw new NestedRuntimeException("Error when adding team : " + teamName , ex);
                }
                //codeMapper.invalidateWorkbrainTeams();
                String msg = "Warning: Team " + teamName + " did not exist and was created.";
                appendWarning(msg);
                if (logger.isDebugEnabled()) logger.debug(msg);
            }
            else {
                if( isFullPath
                    && wbTeam.getWbtParentId() != parentTeam.getWbtId() ) {
                    String msg = "Warning: Team structure changed.  Team " + teamName +
                        " has been moved from parent ID " + wbTeam.getWbtParentId() +
                        " to " + parentTeam.getWbtId() + ".  Please confirm." ;
                    if (logger.isDebugEnabled()) logger.debug(msg);
                    appendWarning(msg);
                    WorkbrainTeamManager wtm = new WorkbrainTeamManager(conn);
                    try {
                        wtm.moveTeam(wbTeam.getWbtId(), parentTeam.getWbtId(), false, false);
                    }
                    catch (Exception ex1) {
                        throw new NestedRuntimeException("Error when moving team : " + teamName , ex1);
                    }
                    // *** team updated, invalidate read-through cache
                    codeMapper.invalidateWorkbrainTeam(teamName);
                }
                else {
                }
            }
            if (i == teamHierarchy.size() - 1) {
                ret = wbTeam.getWbtId();
            }

            parentTeam = wbTeam;
        }

        return ret;
    }


}
