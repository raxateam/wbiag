package com.wbiag.app.modules.blackout;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.sql.*;
import com.workbrain.tool.locale.DataLocUtil;
import com.workbrain.util.*;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.*;
import com.workbrain.tool.security.*;

/**
 */
public class WbiagBlkoutAccess extends RecordAccess {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WbiagBlkoutAccess.class);

    public final static String WBIAG_BLKOUT_DAT_TABLE  = "WBIAG_BLKOUT_DAT";
    public final static String WBIAG_BLKOUT_DAT_SEQ    = "SEQ_WIBLKD_ID";
    public final static String WBIAG_BLKOUT_DAT_PRIKEY = "WIBLKD_ID";
    public final static String WBIAG_BLKOUT_DT_GRP_TABLE  = "WBIAG_BLKOUT_DT_GRP";
    public final static String WBIAG_BLKOUT_DT_GRP_SEQ    = "SEQ_WIBLKDG_ID";
    public final static String WBIAG_BLKOUT_DT_GRP_PRIKEY = "WIBLKDG_ID";

    public final static String IS_EMP_HOME = "HOME";
    public final static String IS_EMP_TEMP = "TEMP";
    public final static String IS_EMP_ALL = "ALL";

    public WbiagBlkoutAccess(DBConnection conn) {
        super(conn);
    }

    /**
     * Returns WbiagBlkoutDtGrpData for a given group name
     * @param grpName grpName
     * @return WbiagBlkoutDtGrpData
     */
    public WbiagBlkoutDtGrpData loadBlackoutDateGroup(String grpName) {
        List list = loadRecordData(new WbiagBlkoutDtGrpData(),
                                   WBIAG_BLKOUT_DT_GRP_TABLE,
                                   "wiblkdg_name", grpName);
        if (list.size() > 0) {
            return (WbiagBlkoutDtGrpData)list.get(0);
        }
        return null;
    }

    /**
     * Returns list of WbiagBlkoutDatData for a given group
     * @param grpName grpName
     * @return List
     */
    public List loadBlackoutDatesByGroup(String grpName) {
        return loadRecordData(new WbiagBlkoutDatData(),
                                   WBIAG_BLKOUT_DAT_TABLE,
                                   "wiblkdg_id IN (SELECT wiblkdg_id FROM "
                                   + WBIAG_BLKOUT_DT_GRP_TABLE +
                                   " WHERE wiblkdg_name = ?)",
                          new String[] {grpName});
    }

    /**
     * Delete WbiagBlkoutDtGrpData for a given group name
     * @param grpName grpName
     * @return WbiagBlkoutDtGrpData
     */
    public boolean deleteBlackoutDateGroup(String grpName) throws SQLException{
        int upd = 0;
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM " + WBIAG_BLKOUT_DT_GRP_TABLE + " WHERE wiblkdg_name = ?");
            ps = getDBConnection().prepareStatement(sb.toString());
            ps.setString(1  , grpName);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd > 0;
    }

    /**
     * Delete WbiagBlkoutDtGrpData for a given group id
     * @param grpName grpName
     * @return WbiagBlkoutDtGrpData
     */
    public boolean deleteBlackoutDateGroup(int grpId) throws SQLException{
        int upd = 0;
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM " + WBIAG_BLKOUT_DT_GRP_TABLE + " WHERE wiblkdg_id = ?");
            ps = getDBConnection().prepareStatement(sb.toString());
            ps.setInt(1  , grpId);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        return upd > 0;
    }

    /**
     * Creates blackout date group and black out date records for related sub teams
     * @param grpData WbiagBlkoutDtGrpData
     * @throws Exception
     */
    public boolean insertBlackoutDateGroup(WbiagBlkoutDtGrpData grpData) throws Exception{
        return insertBlackoutDateGroup(grpData, null, true);
    }

    /**
     * Creates blackout date group and black out date records for related sub teams
     * @param grpData WbiagBlkoutDtGrpData
     * @param subWbtIds team ids that will be in/exclusive for which WbiagBlkoutData records will be created
     * @param subWbtIdsInclusive in/exclusive per subWbtIds
     * @throws Exception
     */
    public boolean insertBlackoutDateGroup(WbiagBlkoutDtGrpData grpData,
                                           int[] subWbtIds,
                                           boolean subWbtIdsInclusive) throws Exception{
        if (grpData.getWbtId() == null) {
            throw new RuntimeException ("Team can't be null");
        }
        if (grpData.getWiblkdgStartDate() == null) {
            throw new RuntimeException("Blackout group start date can't be null");
        }
        if (StringHelper.isEmpty(grpData.getWiblkdgName())) {
            throw new RuntimeException("Blackout group name can't be null");
        }
        if (StringHelper.isEmpty(grpData.getWiblkdgDesc())) {
            throw new RuntimeException("Blackout group desc can't be null");
        }

        if (grpData.getWiblkdgEndDate() == null) {
            grpData.setWiblkdgEndDate(DateHelper.DATE_3000);
        }
        int blkdtGrpId = getDBConnection().getDBSequence(WBIAG_BLKOUT_DT_GRP_SEQ).getNextValue();
        if (grpData.getWiblkdgId() == null) {
            grpData.setWiblkdgId(new Integer(blkdtGrpId));
        }
        // *** insert group data
        insertRecordData(grpData, WBIAG_BLKOUT_DT_GRP_TABLE);
        IntegerList subTeams = getSubTeams(grpData, subWbtIds, subWbtIdsInclusive);
        for (int i=0, k=subTeams.size() ; i < k ; i++) {
            int wbtId = subTeams.getInt(i);
            WbiagBlkoutDatData blkoutData = new WbiagBlkoutDatData();
            blkoutData.setWiblkdgId(new Integer(blkdtGrpId));
            blkoutData.setWiblkdStartDate(grpData.getWiblkdgStartDate());
            blkoutData.setWiblkdEndDate(grpData.getWiblkdgEndDate());
            blkoutData.setWiblkdName(grpData.getWiblkdgName());
            blkoutData.setWiblkdDesc(grpData.getWiblkdgDesc());
            blkoutData.setJobId(grpData.getJobId());
            blkoutData.setWbtId(new Integer(wbtId));
            int blkdtId = getDBConnection().getDBSequence(WBIAG_BLKOUT_DAT_SEQ).getNextValue();
            blkoutData.setWiblkdId(new Integer(blkdtId));
            insertRecordData(blkoutData, WBIAG_BLKOUT_DAT_TABLE);
        }
        return true;
    }

    private IntegerList getSubTeams(WbiagBlkoutDtGrpData data,
                                           int[] subWbtIds,
                                           boolean subWbtIdsInclusive)
        throws SQLException {
        IntegerList ret = new IntegerList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT child_wbt_id FROM sec_wb_team_child_parent ");
            sb.append(" WHERE parent_wbt_id = ? ");
            if (data.getWbttId() != null) {
                sb.append(" AND child_wbtt_id = ?");
            }
            if (subWbtIds != null && subWbtIds.length > 0) {
                sb.append(" AND child_wbt_id ");
                sb.append(subWbtIdsInclusive ? " IN ( " : " NOT IN ( ");
                for (int i = 0; i < subWbtIds.length; i++) {
                    sb.append(i>0 ? ",?" : "?");
                }
                sb.append(" ) ");
            }

            ps = getDBConnection().prepareStatement(sb.toString());
            int cnt = 1;
            ps.setInt(cnt++ , data.getWbtId().intValue() );
            if (data.getWbttId() != null) {
                ps.setInt(cnt++ , data.getWbttId().intValue());
            }
            if (subWbtIds != null && subWbtIds.length > 0) {
                for (int i = 0; i < subWbtIds.length; i++) {
                    ps.setInt(cnt++ , subWbtIds[i]);
                }
            }

            rs = ps.executeQuery();
            while (rs.next()) {
                ret.add(rs.getInt(1) );
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return ret;
    }

    /**
     * Returns list of Dates employee is on blackout for a given date range
     * @param empId empId
     * @param startDate startDate
     * @param endDate endDate
     * @param homeTempTeam  HOME, TEMP or ALL
     * @return List of dates employee is on blackout
     * @throws Exception
     */
    public List getEmployeeBlackoutDates (int empId, Date startDate,
                                          Date endDate,
                                          String homeTempTeam) throws Exception{
       List ret = new ArrayList();
       for (Date date = startDate; date.compareTo(endDate) <= 0;
            date = DateHelper.addDays(date, 1)) {
           if (isEmployeeOnBlackout(empId , date, homeTempTeam)) {
               ret.add(date);
           }
       }
       return ret;
    }


    /**
     * Returns if an employee is on blackout for a given date based on emp's home team
     * @param empId
     * @param dat
     * @return
     * @throws Exception
     */
    public boolean isEmployeeOnBlackout (int empId, Date dat) throws Exception{
        return isEmployeeOnBlackout(empId, dat, IS_EMP_HOME);
    }

    /**
     * Returns if an employee is on blackout for a given date
     * @param empId
     * @param dat
     * @param homeTempTeam  HOME, TEMP or ALL
     * @return
     * @throws Exception
     */
    public boolean isEmployeeOnBlackout (int empId, Date dat,
                                         String homeTempTeam) throws Exception{
        boolean ret = false;
        // *** find teams
        IntegerList teams = new IntegerList();
        if (IS_EMP_HOME.equals(homeTempTeam) || IS_EMP_ALL.equals(homeTempTeam)) {
            teams.add(SecurityEmployee.getHomeTeamId(getDBConnection(),
                empId, new java.sql.Date(dat.getTime())));
        }
        else if (IS_EMP_TEMP.equals(homeTempTeam) || IS_EMP_ALL.equals(homeTempTeam)) {
            teams.add(getTempTeams(empId, dat));
        }
        else {
            throw new RuntimeException ("homeTempTeam must be " + IS_EMP_HOME + ", " + IS_EMP_TEMP + "or " + IS_EMP_ALL);
        }
        if (teams.size() == 0) {
            return ret;
        }
        // *** find jobs
        EmployeeJobAccess eja = new EmployeeJobAccess(getDBConnection());
        List empJobs = eja.loadByEmpDate(empId, dat);

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT wbt_id FROM ").append(WBIAG_BLKOUT_DAT_TABLE) ;
            sb.append(" WHERE wbt_id IN (");
            int[] teamsArr = teams.toIntArray();
            for (int i = 0; i < teamsArr.length; i++) {
                sb.append( i > 0 ? ",?" : "?");
            }
            sb.append(" )");
            if (empJobs.size() > 0) {
                sb.append(" AND (job_id is null OR job_id IN (");
                for (int i = 0, k = empJobs.size(); i < k; i++) {
                    sb.append(i > 0 ? ",?" : "?");
                }
                sb.append(" ))");
            }
            sb.append(" AND ? BETWEEN wiblkd_start_date AND  wiblkd_end_date ");
            ps = getDBConnection().prepareStatement(sb.toString());
            int cnt = 1;
            for (int i = 0; i < teamsArr.length; i++) {
                ps.setInt(cnt++, teamsArr[i]);
            }
            if (empJobs.size() > 0) {
                for (int i = 0, k = empJobs.size(); i < k; i++) {
                     ps.setInt(cnt++, ((EmployeeJobData)empJobs.get(i)).getJobId());
                }
            }

            ps.setTimestamp(cnt++, new java.sql.Timestamp(dat.getTime())) ;
            rs = ps.executeQuery();
            ret = rs.next();
        }
        finally {
            SQLHelper.cleanUp(ps , rs);
        }
        return ret;
    }

    /*
     * Returns all temparary team ids of the given emp as of effective date.
     *
     * @param c             DBConnection
     * @param empId         empId
     * @param effectiveDate effectiveDate
     * @throws SQLException
    */
    private List getTempTeams(int empId , Date effectiveDate ) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        IntegerList tempTeams = new IntegerList();
        String sql = "SELECT wbt_id " +
                     " FROM employee_team, workbrain_team " +
                     " WHERE workbrain_team.wbt_id = employee_team.wbt_id " +
                     " AND empt_home_team = 'N' " +
                     " AND emp_id = ? " +
                     " AND ? BETWEEN  empt_start_date AND empt_end_date";
        try {
            stmt = getDBConnection().prepareStatement(sql);
            stmt.setInt(1, empId);
            stmt.setTimestamp(2 , new Timestamp(DateHelper.truncateToDays(effectiveDate).getTime()));
            rs = stmt.executeQuery();
            while (rs.next()) {
                tempTeams.add(rs.getInt(1));
            }
        } finally {
            SQLHelper.cleanUp( stmt, rs );
        }
        return tempTeams;
    }

}

