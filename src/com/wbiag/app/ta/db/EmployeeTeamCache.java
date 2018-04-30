package com.wbiag.app.ta.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * Cache API to be used for EmployeeTeam Access from RuleEngine calls like
 * business rules, conditions , entitlements etc.
 * @deprecated Core as of 5.0, wbt_id is now in work_Detail table and CalcDataCache
 */
public class EmployeeTeamCache {

    private WBData wbData;
    private static final String TEAM_ENTITY_CACHE_NAME = "EMP_TEAMS";

    public EmployeeTeamCache(WBData wbData)   {
        if (wbData == null) {
            throw new RuntimeException ("WBData can't be null");
        }
        this.wbData = wbData;
    }

    /**
     * Returns list of both home and temp <code>EmployeeTeam</code> objects for given empId from CalcDataCache.
     * If not in CalcDataCache, loads from DB
     *
     * @param empId
     * @return
     * @throws Exception
     */
    public List getEmpTeams(int empId) throws Exception  {

        Integer key = new Integer(empId);

        Map empTeams = getEmpTeamsMap();
        if (!empTeams.containsKey(key)) {
            List thisEmpTeams = loadEmpTeamsFromDB(wbData.getDBconnection(), empId);
            empTeams.put(key , thisEmpTeams);
        }

        return (List) empTeams.get(key);
    }

    /**
     * Returns list of both temp and home <code>EmployeeTeam</code> objects
     * for given empId/date from CalcDataCache.
     * If not in CalcDataCache, loads from DB
     *
     * @param empId
     * @return
     * @throws Exception
     */
    public List getEmpTeams(int empId, Date date) throws Exception  {
        return getEmpTeams(empId, date, null);
    }

    /**
     * List of Temp Teams <code>EmployeeTeam</code> objects for a date
     * @param empId
     * @param date
     * @return
     * @throws Exception
     */
    public List getEmpTempTeams(int empId, Date date) throws Exception  {
        return getEmpTeams(empId, date, "N");
    }

    /**
     * Returns set of temp team names for empId and date
     * @param empId
     * @param date
     * @return
     * @throws Exception
     */
    public Set getEmpTempTeamNames(int empId, Date date) throws Exception  {
        Set ret = new HashSet();

        List teams = getEmpTempTeams(empId, date);
        Iterator iter = teams.iterator();
        while (iter.hasNext()) {
            EmployeeTeam item = (EmployeeTeam)iter.next();
            ret.add(item.wbtName) ;
        }

        return ret;

    }

    /**
     * List of Home Teams <code>EmployeeTeam</code> objects for a date
     * @param empId
     * @param date
     * @return
     * @throws Exception
     */
    public EmployeeTeam getEmpHomeTeam(int empId, Date date) throws Exception  {
        List allTeams = getEmpTeams(empId);
        List homeTeam = getEmpTeams(empId, date, "Y");
        if (homeTeam.size() == 0) {
            throw new RuntimeException("Employee :" + empId + " has no home team for :" + date);
        }
        if (homeTeam.size() > 1) {
            throw new RuntimeException("Employee :" + empId + " has more than one home team for :" + date);
        }
        return (EmployeeTeam)homeTeam.get(0);
    }

    /**
     * Returns wbtName for home team for empId and date
     * @param empId
     * @param date
     * @return
     * @throws Exception
     */
    public String getEmpHomeTeamName(int empId, Date date) throws Exception  {
        return getEmpHomeTeam(empId, date).wbtName;
    }

    /**
     * Returns set of home and temp team names for empId and date
     * @param empId
     * @param date
     * @return
     * @throws Exception
     */
    public Set getEmpTeamNames(int empId, Date date) throws Exception  {
        Set ret = new HashSet();

        List teams = getEmpTeams(empId, date);
        Iterator iter = teams.iterator();
        while (iter.hasNext()) {
            EmployeeTeam item = (EmployeeTeam)iter.next();
            ret.add(item.wbtName) ;
        }

        return ret;

    }

    /**
     * Updates cache if an DB update is done
     * @param empId
     * @param date
     * @return
     * @throws Exception
     */
    public boolean updateCache(int empId, int wbtId,String wbtName,
                               Date start, Date end,
                               String homeTeam) throws Exception  {

        if (start == null || end ==  null
            || wbtName == null || homeTeam == null) {
            throw new RuntimeException ("Start, end dates, wbtName, homeTeam can't be null");
        }

        Integer key = new Integer(empId);

        Map empTeams = getEmpTeamsMap();
        List thisEmpTeams = null;
        if (!empTeams.containsKey(key)) {
            thisEmpTeams = new ArrayList();
        }
        else {
            thisEmpTeams = (List) empTeams.get(key);
        }

        EmployeeTeam et = new EmployeeTeam();
        et.empId = empId;
        et.wbtId = wbtId;
        et.wbtName = wbtName;
        et.start = start ;
        et.end = end;
        thisEmpTeams.add(et);

        empTeams.put(key , thisEmpTeams);
        return true;
    }

    /**
     * Invalidates all cache content for given empId.
     * @param empId
     * @param date
     * @return
     * @throws Exception
     */
    public boolean invalidateCache(int empId) throws Exception  {

        Integer key = new Integer(empId);

        Map empTeams = getEmpTeamsMap();
        empTeams.remove(key);
        return true;
    }

    /**
     * Returns list of <code>EmployeeTeam</code> objects for given empId from DB
     * @param conn
     * @param empId
     * @return
     * @throws SQLException
     */
    private List loadEmpTeamsFromDB(DBConnection conn , int empId)  throws SQLException {
        List ret = new ArrayList();

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT emp_id, employee_team.wbt_id, wbt_name, empt_start_date, empt_end_date, empt_home_team ");
            sb.append(" FROM employee_team , workbrain_team ");
            sb.append(" WHERE workbrain_team.wbt_id = employee_team.wbt_id ");
            sb.append(" AND emp_id = ? ");
            ps = conn.prepareStatement(sb.toString());
            ps.setInt(1 , empId);
            rs = ps.executeQuery();
            while (rs.next()) {
                EmployeeTeam et = new EmployeeTeam();
                et.empId = rs.getInt(1);
                et.wbtId = rs.getInt(2);
                et.wbtName = rs.getString(3);
                et.homeTeam = rs.getString(6);
                if (StringHelper.isEmpty(et.homeTeam)) {
                    et.homeTeam = "N";
                }
                et.start = new java.util.Date(rs.getDate(4).getTime()) ;
                et.end = new java.util.Date(rs.getDate(5).getTime());
                ret.add(et);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return ret;
    }

    /**
     * Returns <code>EmployeeTeam</code> objects for given date and homeTeam criteria
     * @param empId
     * @param date
     * @param homeTeam
     * @return
     * @throws Exception
     */
    public List getEmpTeams(int empId, Date date, String homeTeam) throws Exception  {
        List allTeams = getEmpTeams(empId);
        List ret = new ArrayList();

        Iterator iter = allTeams.iterator();
        while (iter.hasNext()) {
            EmployeeTeam item = (EmployeeTeam)iter.next();
            boolean homeTeamCheck = "Y".equalsIgnoreCase(homeTeam)
                ? "Y".equalsIgnoreCase(item.homeTeam)
                : homeTeam == null ? true : "N".equalsIgnoreCase(item.homeTeam);
            if (homeTeamCheck
                && (DateHelper.isBetween(date, item.start, item.end))) {
                ret.add(item);
            }
        }

        return ret;
    }

    private Map getEmpTeamsMap() {
        Hashtable entity = wbData.getRuleData().getCalcDataCache().getEntityCache();

        Map empTeams = (Map)entity.get(TEAM_ENTITY_CACHE_NAME);
        if (empTeams == null) {
            empTeams = new HashMap();
            entity.put(TEAM_ENTITY_CACHE_NAME, empTeams);
        }
        return empTeams;
    }

    public static class EmployeeTeam {
        int empId;
        int wbtId;
        String wbtName;
        Date start;
        Date end;
        String homeTeam;

    }
}