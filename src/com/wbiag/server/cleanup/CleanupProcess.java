package com.wbiag.server.cleanup;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import com.workbrain.tool.overrides.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * CleanupProcess for employee,team and labor data
 */
public class CleanupProcess  {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(CleanupProcess.class);

    public static final String PROP_DELETE_EMPLOYEE_WHERE_CLAUSE = "DELETE_EMPLOYEE_WHERE_CLAUSE";
    public static final String PROP_DELETE_TEAM_WHERE_CLAUSE = "DELETE_TEAM_WHERE_CLAUSE";
    public static final String PROP_DELETE_DEPT_WHERE_CLAUSE = "DELETE_DEPT_WHERE_CLAUSE";
    public static final String PROP_DELETE_JOB_WHERE_CLAUSE = "DELETE_JOB_WHERE_CLAUSE";
    public static final String PROP_DELETE_CALCGRP_WHERE_CLAUSE = "DELETE_CALCGRP_WHERE_CLAUSE";
    public static final String PROP_DELETE_WBGROUP_WHERE_CLAUSE = "DELETE_WBGROUP_WHERE_CLAUSE";
    public static final String PROP_DELETE_GENERIC_TABLE_PREFIX = "DELETE_GENERIC_TABLE";
    public static final String PROP_DELETE_EMPLOYEE_TABLES = "DELETE_EMPLOYEE_TABLES";
    public static final String PROP_DELETE_TEAM_TABLES = "DELETE_TEAM_TABLES";
    public static final String PROP_DELETE_DEPT_TABLES = "DELETE_DEPT_TABLES";
    public static final String PROP_DELETE_JOB_TABLES = "DELETE_JOB_TABLES";
    public static final String PROP_DELETE_CALCGRP_TABLES = "DELETE_CALCGRP_TABLES";
    public static final String PROP_DELETE_WBGROUP_TABLES = "DELETE_WBGROUP_TABLES";
    public static final String PROP_BASE_DS_LOGIN = "BASE_DS_LOGIN";
    public static final String PROP_BASE_DS_PASSWORD = "BASE_DS_PASSWORD";
    public static final String PROP_BASE_DS_DRIVER = "BASE_DS_DRIVER";
    public static final String PROP_BASE_DS_URL = "BASE_DS_URL";
    public static final String PROP_CLIENT_ID = "CLIENT_ID";
    public static final String PROP_CONFIRMATION_ONLY = "CONFIRMATION_ONLY";
    public static final String PROP_DELETE_OVERRIDE_WHERE_CLAUSE = "DELETE_OVERRIDE_WHERE_CLAUSE";
    public static final String PROP_DELETE_OVERRIDE_CALCULATES = "DELETE_OVERRIDE_CALCULATES";
    public static final String PROP_DELETE_SCHEDULE_GROUP_WHERE_CLAUSE = "DELETE_SCHEDULE_GROUP_WHERE_CLAUSE";
    public static final String PROP_DELETE_SCHEDULE_GROUP_TABLES = "DELETE_SCHEDULE_GROUP_TABLES";

    private static final String ID_BIND = "?id";
    private static final String NAME_BIND = "?name";
    private static final String INSTRCHK = "INSTRCHK";
    protected CleanupProcessContext context;
    private long startMs;
    private long stepMs;
    private int totalDeletedCnt = 0;
    private List logMessages = new ArrayList() ;

    private CleanupProcess() {}

    /**
     * Loads context from property file
     * @param propFilePath propFilePath
     * @throws Exception
     */
    public CleanupProcess(String propFilePath) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream(propFilePath));
        context = createContextFromPropFile(props);
        this.context.validate();
    }

    /**
     * Loads given context
     * @param context CleanupProcessContext
     * @throws Exception
     */
    public CleanupProcess(CleanupProcessContext context) throws Exception {
        this.context = context;
        this.context.validate();
    }

    /**
     * Loads context from property file
     * @param propFilePath propFilePath
     * @throws Exception
     */
    public CleanupProcess(DBConnection conn, String propFilePath) throws Exception {

        Properties props = new Properties();
        props.load(new FileInputStream(propFilePath));
        context = createContextFromPropFile(props);
        context.conn = conn;
        this.context.validate();
    }

    /**
     * Runs CleanupProcess based on loaded CleanupProcessContext
     * @throws Exception
     */
    public CleanupProcessContext getCleanupProcessContext() {
        return this.context;
    }

    /**
     * Runs CleanupProcess based on loaded CleanupProcessContext
     * @throws Exception
     */
    public void execute() throws Exception {
        logMessages.clear();
        startMs = System.currentTimeMillis();
        //log("Parameters :\n" + context);

        DBConnection conn = null;
        try {
            if (context.getDBConnection() == null) {
                conn = getDBConnection(context.getBaseDsLogin(),
                                       context.getBaseDsPassword(),
                                       context.getBaseDsUrl(),
                                       context.getBaseDsDriver());
            }
            else {
                conn = context.getDBConnection();
            }
            totalDeletedCnt += processEmployee(conn);
            totalDeletedCnt += processLabor(conn);
            totalDeletedCnt += processCalcgrp(conn);
            totalDeletedCnt += processWBGroup(conn);
            totalDeletedCnt += processGenericTableDeletes(conn);
            totalDeletedCnt += processOverride(conn);
            totalDeletedCnt += processScheduleGroup(conn);
            totalDeletedCnt += processTeam(conn);
            if (context.shouldCommit()) conn.commit();
        }
        catch (Exception e){
            logError("Error in execute" , e);
            if (context.shouldCommit() && conn != null) {
                conn.rollback();
            }
            throw e;
        }
        finally {
            if (context.getDBConnection() == null
                && conn != null) {
                conn.close();
            }
        }
        meterTime("ALL PROCESS" , startMs);
    }

    /**
     * Returns the number of total records deleted after <code>execute</code>
     * @return
     */
    public int getTotalDeletedCount() {
        return totalDeletedCnt;
    }

    /**
     * Returns all log messages
     * @return
     */
    public List getLogMessages() {
        return logMessages;
    }

    protected int processEmployee(DBConnection conn) throws Exception{
        return processTable(conn , "EMPLOYEE" , "emp_id" , "emp_name",
                            context.deleteEmployeeWhereClause,
                            context.getDeleteEmployeeTables(), "Employee");
    }

    protected int processTeam(DBConnection conn) throws Exception{
        if (StringHelper.isEmpty(context.getDeleteTeamWhereClause())) {
            log("No team where clause defined to delete");
            return 0;
        }
        List teams = getTeamsToDelete(conn);
        if (context.isConfirmationOnly()) {
            log(teams.size() + " teams would be deleted, NONE deleted since confirmation only is true");
            return 0;
        }

        List tabs = null;
        if (teams.size() > 0) {
            tabs = getExecStatements(conn , context.getDeleteTeamTables(), "wbt_id");
        }
        int cnt = 0;
        for (int i=0 , k=teams.size() ; i<k ; i++) {
            IdName idName = (IdName)teams.get(i);
            int wbtID = idName .id;
            try {
                int del = deleteOneTeamWithSubteams(conn, wbtID, tabs);
                if (del > 0) {
                    log("Deleted wbtId :" + idName.id + " with subteams");
                    cnt++;
                    if (context.shouldCommit()) conn.commit();
                }
            }
            catch (SQLException ex) {
                if (context.shouldCommit()) conn.rollback();
                logError("Error in deleting wbtId : " + idName.id , ex);
            }
        }
        log("Deleted " + cnt + " teams");
        return cnt;
    }

    protected List getTeamsToDelete(DBConnection conn) throws SQLException {
        return getIdNamesToDelete(conn, "WORKBRAIN_TEAM", "wbt_id",
                                  "wbt_name",
                                  context.getDeleteTeamWhereClause());
    }

    protected int deleteOneTeamWithSubteams(DBConnection conn , int wbtId, List tabs) throws SQLException {
        List subTeams = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT child_wbt_id, child_wbt_name");
            sb.append(" FROM sec_wb_team_child_parent, sec_wb_team_level ");
            sb.append(" WHERE sec_wb_team_level.wbt_id = sec_wb_team_child_parent.child_wbt_id ");
            sb.append(" AND parent_wbt_id = ? ");
            sb.append(" ORDER BY wbt_level DESC ");

            ps = conn.prepareStatement(sb.toString());
            ps.setInt(1 , wbtId);
            rs = ps.executeQuery();
            while (rs.next()) {
                IdName idName = new IdName ();
                idName.id = rs.getInt(1);
                idName.name = rs.getString(2);
                subTeams.add(idName);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

        Iterator iter = subTeams.iterator();
        while (iter.hasNext()) {
            IdName item = (IdName)iter.next();
            deleteOneTeam(conn , item , tabs);
        }

        return subTeams.size();
    }

    protected int deleteOneTeam(DBConnection conn , IdName idName, List tabs) throws SQLException {
        return deleteOneIdName(conn , idName, tabs);
    }

    protected int processScheduleGroup(DBConnection conn) throws Exception{
        if (StringHelper.isEmpty(context.getDeleteScheduleGroupWhereClause())) {
            log("No schedule group where clause defined to delete");
            return 0;
        }
        List skdGrps = getScheduleGroupsToDelete(conn);
        if (context.isConfirmationOnly()) {
            log(skdGrps.size() + " schedule groups would be deleted, NONE deleted since confirmation only is true");
            return 0;
        }

        List tabs = null;
        if (skdGrps.size() > 0) {
            tabs = getExecStatements(conn , context.getDeleteScheduleGroupTables(), "skdgrp_id");
        }
        int cnt = 0;
        for (int i=0 , k=skdGrps.size() ; i<k ; i++) {
            IdName idName = (IdName)skdGrps.get(i);
            int id = idName .id;
            try {
                int del = deleteOneScheduleGroupWithSubs(conn, id, tabs);
                if (del > 0) {
                    log("Deleted skdgrp_id :" + idName.id + " with sub groups");
                    cnt++;
                    if (context.shouldCommit()) conn.commit();
                }
            }
            catch (SQLException ex) {
                if (context.shouldCommit()) conn.rollback();
                logError("Error in deleting skdgrp_id : " + idName.id , ex);
            }
        }
        log("Deleted " + cnt + " schedule groups");
        return cnt;
    }

    protected List getScheduleGroupsToDelete(DBConnection conn) throws SQLException {
        return getIdNamesToDelete(conn, "SO_SCHEDULE_GROUP", "skdgrp_id",
                                  "skdgrp_name",
                                  context.getDeleteScheduleGroupWhereClause());
    }

    protected int deleteOneScheduleGroupWithSubs(DBConnection conn , int skdgrpId, List tabs) throws SQLException {
        List subGrps = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT skdgrp_id, skdgrp_name ");
            sb.append(" FROM so_schedule_group, workbrain_team");
            sb.append(" WHERE workbrain_team.wbt_id = so_schedule_group.wbt_id");
            sb.append(" AND so_schedule_group.wbt_id IN (");
            sb.append(" SELECT child_wbt_id FROM sec_wb_team_child_parent WHERE parent_wbt_id = ");
            sb.append("       (SELECT wbt_id FROM so_schedule_group WHERE skdgrp_id = ?))");
            sb.append(" ORDER BY wbt_level DESC");

            ps = conn.prepareStatement(sb.toString());
            ps.setInt(1 , skdgrpId);
            rs = ps.executeQuery();
            while (rs.next()) {
                IdName idName = new IdName ();
                idName.id = rs.getInt(1);
                idName.name = rs.getString(2);
                subGrps.add(idName);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

        Iterator iter = subGrps.iterator();
        while (iter.hasNext()) {
            IdName item = (IdName)iter.next();
            deleteOneScheduleGroup(conn , item , tabs);
        }

        return subGrps.size();
    }

    protected int deleteOneScheduleGroup(DBConnection conn , IdName idName, List tabs) throws SQLException {
        return deleteOneIdName(conn , idName, tabs);
    }

    protected int processLabor(DBConnection conn) throws Exception{
        int ret = 0;
        ret += processDept(conn);
        ret += processJob(conn);
        return ret;
    }

    protected int processDept(DBConnection conn) throws Exception{
        return processTable(conn , "DEPARTMENT" , "dept_id" , "dept_name",
                            context.getDeleteDeptWhereClause(),
                            context.getDeleteDeptTables(), "Department");
    }

    protected int processJob(DBConnection conn) throws Exception{
        return processTable(conn , "JOB" , "job_id" , "job_name",
                            context.getDeleteJobWhereClause(),
                            context.getDeleteJobTables(), "Job");
    }

    protected int processCalcgrp(DBConnection conn) throws Exception{
        return processTable(conn , "CALC_GROUP" , "calcgrp_id" , "calcgrp_name",
                            context.getDeleteCalcgrpWhereClause(),
                            context.getDeleteCalcgrpTables(), "CalcGroup");
    }

    protected int processWBGroup(DBConnection conn) throws Exception{
        return processTable(conn , "WORKBRAIN_GROUP" , "wbg_id" , "wbg_name",
                            context.getDeleteWBGroupWhereClause(),
                            context.getDeleteWBGroupTables(), "Workbrain Group");
    }

    protected int processOverride(DBConnection conn) throws Exception{
        int ret = 0;
        if (StringHelper.isEmpty(context.getDeleteOverrideWhereClause())) {
            log("No override where clause defined to delete");
            return ret;
        }
        if (context.isConfirmationOnly()) {
            int cnt = 0;
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                String sql = "SELECT count(*) FROM override WHERE " + context.getDeleteOverrideWhereClause();
                ps = conn.prepareStatement(sql);
                rs = ps.executeQuery();
                if (rs.next()) {
                    cnt = rs.getInt(1);
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }

            log(cnt + " override(s) would be deleted, NONE deleted since confirmation only is true");
            return ret;
        }

        // *** if not calculate, batch delete them
        if (!context.getDeleteOverrideCalculates()) {
            PreparedStatement ps = null;
            try {
                String sql = "DELETE FROM override WHERE " + context.getDeleteOverrideWhereClause();
                ps = conn.prepareStatement(sql);
                ret = ps.executeUpdate();
            }
            catch (Exception ex) {
                if (context.shouldCommit()) conn.rollback();
                logError("Error in deleting ovrIds without calculating", ex);
            }
            finally {
                if (ps != null) ps.close();
            }
        }
        else {
            // *** if calculate, first cancel, calculate and then delete
            IntegerList ovrIds = new IntegerList();
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                String sql = "SELECT ovr_id FROM override WHERE " + context.getDeleteOverrideWhereClause();
                ps = conn.prepareStatement(sql);
                rs = ps.executeQuery();
                while (rs.next()) {
                    ovrIds.add(rs.getInt(1));
                }
            }
            finally {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
            }
            int delCnt = 0;
            OverrideBuilder ob = new OverrideBuilder(conn);
            for (int i=0 , k=ovrIds.size() ; i<k ; i++) {
                int ovrId = ovrIds.getInt(i);
                DeleteOverride dov = new DeleteOverride();
                dov.setOverrideId(ovrId);
                ob.add(dov);
                ob.execute(true, false);
                // *** delete override
                PreparedStatement ps2 = null;
                try {
                    String sql = "DELETE FROM override WHERE ovr_id = ?";
                    ps2 = conn.prepareStatement(sql);
                    ps2.setInt(1, ovrId);
                    int upd = ps2.executeUpdate();
                }
                catch (Exception ex) {
                    if (context.shouldCommit()) conn.rollback();
                    logError("Error in deleting ovrId : " + ovrId, ex);
                }
                finally {
                    if (ps2 != null) ps2.close();
                }
                log("Deleted by calculating ovrId : " + ovrId);
                delCnt++;
                if (context.shouldCommit()) conn.commit();
            }
            ret = delCnt;
        }
        return ret;
    }

    protected int processGenericTableDeletes(DBConnection conn) throws Exception{
        if (context.getGenericTableDeletes() == null
            || context.getGenericTableDeletes().size() == 0) {
            log("No generic table entries for found");
            return 0;
        }
        int cnt = 0;
        Iterator iter = context.getGenericTableDeletes().iterator();
        while (iter.hasNext()) {
            GenericTableDelete item = (GenericTableDelete)iter.next();
            cnt += processTable(conn , item.tableName ,
                                item.idCol ,
                                item.nameCol,
                                item.whereClause,
                                item.deleteTables ,
                                item.tableName);
        }
        return cnt;
    }

    protected int processTable(DBConnection conn, String tableName,
                               String idCol,
                               String nameCol,
                               String whereClause,
                               List tableNames,
                               String msg) throws Exception{
        if (StringHelper.isEmpty(whereClause)) {
            log("No " + msg + " where clause defined to delete");
            return 0;
        }
        List idNames = getIdNamesToDelete(conn , tableName , idCol, nameCol, whereClause);
        if (context.isConfirmationOnly()) {
            log(idNames.size() + " " + msg + "(s) would be deleted, NONE deleted since confirmation only is true");
            return 0;
        }
        List tabs = null;
        if (idNames.size() > 0) {
            tabs = getExecStatements(conn , tableNames , idCol);
        }
        if (tabs == null || tabs.size() == 0) {
            log("No tables were defined for " + msg);
        }
        int cnt=0;
        for (int i=0 , k=idNames.size() ; i<k ; i++) {
            IdName idName = (IdName)idNames.get(i);
            String name = idName.name;
            try {
                int del = deleteOneIdName(conn, idName, tabs);
                if (del > 0) {
                    log("Deleted " + msg + " : " + name);
                    cnt++;
                    if (context.shouldCommit()) conn.commit();
                }
            }
            catch (SQLException ex) {
                 if (context.shouldCommit()) conn.rollback();
                 logError("Error in deleting " + msg + " : " + name , ex);
            }
        }
        log("Deleted " + cnt + " " + msg + "(s)");
        return cnt;
    }

    protected List getIdNamesToDelete(DBConnection conn,
                                      String tableName,
                                      String idCol,
                                      String nameCol,
                                      String whereClause) throws SQLException {
        List ret = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT ").append(idCol).append(",").append(nameCol);
            sb.append(" FROM ").append(tableName) ;
            sb.append(" WHERE ").append(whereClause);
            ps = conn.prepareStatement(sb.toString());
            rs = ps.executeQuery();
            while (rs.next()) {
                IdName idName = new IdName ();
                idName.id = rs.getInt(1);
                idName.name = rs.getString(2);
                ret.add(idName);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return ret;
    }

    protected int deleteOneIdName(DBConnection conn , IdName idName, List tabs) throws SQLException {
        int ret = 0;

        String empName = idName.name;
        int empId = idName.id;
        Iterator iter = tabs.iterator();
        while (iter.hasNext()) {
            ExecStatement item = (ExecStatement)iter.next();
            if (item.isId()) {
                ret = execStatement(conn , empId, item.statement);
            }
            else if (item.isName()) {
                ret = execStatement(conn , empName, item.statement);
            }
        }
        return ret;
    }

    private List getExecStatements(DBConnection conn ,List tables, String idCol) {
        List ret = new ArrayList();
        Iterator iter = tables.iterator();
        while (iter.hasNext()) {
            String item = (String)iter.next();
            if (StringHelper.isEmpty(item)) continue;

            int ind = item.indexOf("~");
            if ( ind == -1) {
                ExecStatement es = new ExecStatement();
                es.setId();
                es.statement = "DELETE FROM " + item + " WHERE " + idCol + " = ? ";
                ret.add(es);
            }
            else {
                String tableName = item.substring(0, ind);
                String where = item.substring(ind + 1);
                int bindInd = where.indexOf("?");
                if (bindInd > -1) {
                    int bindIndId = where.indexOf(ID_BIND);
                    int bindIndName = where.indexOf(NAME_BIND);
                    if (bindIndId > -1) {
                        where = StringHelper.searchReplace(where, ID_BIND, "?");
                        ExecStatement es = new ExecStatement();
                        es.setId();
                        if (!where.startsWith("SET")) {
                            es.statement = "DELETE FROM " + tableName + " " +
                                formatInstrChk(conn, where);
                        }
                        else {
                            es.statement = "UPDATE " + tableName + " " +
                                formatInstrChk(conn, where);
                        }
                        ret.add(es);
                    }
                    else if (bindIndName > -1) {
                        where = StringHelper.searchReplace(where, NAME_BIND, "?");
                        ExecStatement es = new ExecStatement();
                        es.setName();
                        if (!where.startsWith("SET")) {
                            es.statement = "DELETE FROM " + tableName + " " +
                                formatInstrChk(conn, where);
                        }
                        else {
                            es.statement = "UPDATE " + tableName + " " +
                                formatInstrChk(conn, where);
                        }
                        ret.add(es);
                    }
                    else {
                        log("No bind ?id or ?name found although ? exists. Table = " + tableName + " and where = " + where + " will not be processed");
                    }
                }
                else {
                    log("No bind ? found although ~ exists. Table = " + tableName + " and where = " + where + " will not be processed");
                }
            }
        }
        return ret;
    }

    /**
     * Foramt INSTRCHK based on DB
     * @param where
     * @return
     */
    private String formatInstrChk(DBConnection conn , String where) {
        String ret = where;
        if (StringHelper.isEmpty(where)) {
            return ret;
        }
        int instrInd = where.indexOf(INSTRCHK);
        if (instrInd == -1) {
            return ret;
        }
        int parOpen = where.indexOf("(", instrInd);
        if (parOpen > -1) {
            int parClose = where.indexOf(")", parOpen);
            String whereBeg = where.substring(0, instrInd);
            String whereEnd = where.substring(parClose + 1);
            int comma = where.indexOf("," , parOpen);
            if (comma > -1) {
                String srchIn = where.substring(parOpen + 1, comma);
                String srchFor = where.substring(comma + 1 , parClose);
                String concat = conn.getDBServer().getConcatOperator();
                srchIn = "','" +  concat + srchIn + concat + "','";
                srchFor = "','" +  concat + srchFor + concat + "','";
                StringBuffer sb = new StringBuffer(200);
                sb.append(whereBeg).append(" ");
                if (conn.getDBServer().isOracle()) {
                    sb.append("INSTR(");
                    sb.append(srchIn).append(",").append(srchFor);
                    sb.append(") > 0 ");
                } else if (conn.getDBServer().isMSSQL()) {
                    sb.append("CHARINDEX(");
                    sb.append(srchFor).append(",").append(srchIn);
                    sb.append(") > 0 ");
                } else if (conn.getDBServer().isDB2()) {
                    sb.append("LOCATE(");
                    sb.append(srchFor).append(",").append(srchIn);
                    sb.append(") > 0 ");
                }
                sb.append(whereEnd);
                ret =  sb.toString();
            }
        }

        return ret;
    }

    protected CleanupProcessContext createContextFromPropFile(Properties props) throws ParseException{
        CleanupProcessContext ret = new CleanupProcessContext();

        ret.setBaseDsLogin(props.getProperty(PROP_BASE_DS_LOGIN));
        ret.setBaseDsPassword(props.getProperty(PROP_BASE_DS_PASSWORD));
        ret.setBaseDsDriver(props.getProperty(PROP_BASE_DS_DRIVER));
        ret.setBaseDsUrl(props.getProperty(PROP_BASE_DS_URL));

        ret.setClientId(props.getProperty(PROP_CLIENT_ID));

        ret.setDeleteEmployeeWhereClause(props.getProperty(PROP_DELETE_EMPLOYEE_WHERE_CLAUSE));
        ret.setDeleteTeamWhereClause(props.getProperty(PROP_DELETE_TEAM_WHERE_CLAUSE));
        ret.setDeleteDeptWhereClause(props.getProperty(PROP_DELETE_DEPT_WHERE_CLAUSE));
        ret.setDeleteJobWhereClause(props.getProperty(PROP_DELETE_JOB_WHERE_CLAUSE));
        ret.setDeleteCalcgrpWhereClause(props.getProperty(PROP_DELETE_CALCGRP_WHERE_CLAUSE));
        ret.setDeleteWBGroupWhereClause(props.getProperty(PROP_DELETE_WBGROUP_WHERE_CLAUSE));
        ret.setDeleteOverrideWhereClause(props.getProperty(PROP_DELETE_OVERRIDE_WHERE_CLAUSE));
        String sOvrCalcs = props.getProperty(PROP_DELETE_OVERRIDE_CALCULATES);
        ret.setConfirmationOnly(StringHelper.isEmpty(sOvrCalcs)
            ? false
            : Boolean.valueOf(sOvrCalcs).booleanValue());
        ret.setDeleteScheduleGroupWhereClause(props.getProperty(PROP_DELETE_SCHEDULE_GROUP_WHERE_CLAUSE));

        String sConf = props.getProperty(PROP_CONFIRMATION_ONLY);
        ret.setConfirmationOnly(StringHelper.isEmpty(sConf)
            ? false
            : Boolean.valueOf(sConf).booleanValue());
        ret.setDeleteEmployeeTables(detokenizeStringAsList(
            props.getProperty(PROP_DELETE_EMPLOYEE_TABLES) , ","));
        ret.setDeleteTeamTables(detokenizeStringAsList(
            props.getProperty(PROP_DELETE_TEAM_TABLES) , ","));
        ret.setDeleteJobTables(detokenizeStringAsList(
            props.getProperty(PROP_DELETE_JOB_TABLES) , ","));
        ret.setDeleteDeptTables(detokenizeStringAsList(
            props.getProperty(PROP_DELETE_DEPT_TABLES) , ","));
        ret.setDeleteCalcgrpTables(detokenizeStringAsList(
            props.getProperty(PROP_DELETE_CALCGRP_TABLES) , ","));
        ret.setDeleteWBGroupTables(detokenizeStringAsList(
            props.getProperty(PROP_DELETE_WBGROUP_TABLES) , ","));
        ret.setDeleteScheduleGroupTables(detokenizeStringAsList(
            props.getProperty(PROP_DELETE_SCHEDULE_GROUP_TABLES) , ","));

        boolean genericExists = true; int cnt = 1;
        while (genericExists) {
            String propName = PROP_DELETE_GENERIC_TABLE_PREFIX + "_" + cnt;
            genericExists = props.getProperty(propName) != null;
            if (genericExists) {
                cnt++;
                List entries = detokenizeStringAsList(props.getProperty(propName), ",");
                if (entries != null && entries.size() > 0) {
                    String tabInfo = (String) entries.get(0);
                    String[] tabInfoArr = StringHelper.detokenizeString(tabInfo, "~");
                    if (tabInfoArr != null && tabInfoArr.length == 4) {
                        GenericTableDelete gtd = new GenericTableDelete();
                        gtd.tableName = tabInfoArr[0];
                        gtd.idCol = tabInfoArr[1];
                        gtd.nameCol = tabInfoArr[2];
                        gtd.whereClause = tabInfoArr[3];
                        // *** entries after first entry are tables
                        gtd.deleteTables = entries.subList(1, entries.size()) ;
                        ret.addToGenericTableDeletes(gtd);
                    }
                    else {
                        log("Table entries were not correct for :" + propName);
                    }
                }
                else {
                    log("No entries found for property :" + propName);
                }

            }
        }
        return ret;
    }

    private List detokenizeStringAsList(String input,
                                              String separator) {
        String[] st = StringHelper.detokenizeString(input, separator);
        return st == null ? null : Arrays.asList(st);
    }

    private DBConnection getDBConnection(String user, String pwd,
                                         String url , String driver) throws Exception{
        System.setProperty("junit.db.username" , user);
        System.setProperty("junit.db.password" , pwd);
        System.setProperty("junit.db.url" , url);
        System.setProperty("junit.db.driver" , driver);

        final DBConnection c = com.workbrain.sql.SQLHelper.connectTo();
        c.setAutoCommit( false );
        com.workbrain.security.SecurityService.setCurrentClientId(context.getClientId());
        WorkbrainSystem.bindDefault(
            new com.workbrain.sql.SQLSource () {
                public java.sql.Connection getConnection() throws SQLException {
                    return c;
                }
            }
        );
        return c;
    }

    protected int execStatement(DBConnection conn ,
                              int id,
                              String sql) throws SQLException{
        int upd = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setInt(1 , id);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        return upd;
    }

    protected int execStatement(DBConnection conn ,
                              String name,
                              String sql) throws SQLException{
        int upd = 0;
        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sql);
            ps.setString(1 , name);
            upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        return upd;
    }

    protected long meterTime(String what, long start){
        long l = System.currentTimeMillis();
        log(what+" took: "+(l-start)+" millis");
        return l;
    }

    private void log(String msg  ) {
        logMessages.add(msg);
        if (logger.isDebugEnabled()) logger.debug(msg);
    }

    private void logError(String msg , Exception e ) {
        logMessages.add("ERROR \n" + msg + "\n" + StringHelper.getStackTrace(e));
        logger.error(msg , e);
    }

    public static void main( String[] args ) throws Exception {
        // *** context can be initialized as below

        //CleanupProcess dst = new CleanupProcess("C:\\source\\4.1\\Java\\src\\com\\wbiag\\server\\cleanup\\CleanupProcess.properties");
        //dst.execute();
        if (args.length == 0) {
            throw new RuntimeException("Property file path must be supplied");
        }
        if (!FileUtil.fileExists(args[0])) {
            throw new RuntimeException("Property file not found");
        }
        CleanupProcess dst = new CleanupProcess(args[0]);
        //CleanupProcess dst = new CleanupProcess("C:\\source\\4.1\\Java\\src\\com\\wbiag\\server\\cleanup\\CleanupProcess.properties");
        dst.execute();
    }

    private class IdName {
        int id;
        String name;
    }

    public static class GenericTableDelete {
        String tableName;
        String idCol;
        String nameCol;
        String whereClause;
        List deleteTables;
    }

    private class ExecStatement {

        public static final int ID = 0;
        public static final int NAME = 1;
        int idName;
        String statement;

        public void setId() {
            idName = 0;
        }

        public void setName() {
            idName = 1;
        }

        public boolean isId() {
            return idName == 0;
        }

        public boolean isName() {
            return idName == 1;
        }

    }

    public static class CleanupProcessContext {
        private boolean shouldCommit = true;
        private DBConnection conn;
        private String deleteEmployeeWhereClause;
        private String deleteTeamWhereClause;
        private String deleteDeptWhereClause;
        private String deleteJobWhereClause;
        private String deleteCalcgrpWhereClause;
        private String deleteOverrideWhereClause;
        private String deleteScheduleGroupWhereClause;
        private boolean deleteOverrideCalculates = true;
        private String baseDsLogin;
        private String baseDsPassword;
        private String baseDsDriver;
        private String baseDsUrl;
        private String clientId;
        private boolean isConfirmationOnly = false;
        private List deleteEmployeeTables;
        private List deleteTeamTables;
        private List deleteJobTables;
        private List deleteDeptTables;
        private List deleteCalcgrpTables;
        private String deleteWBGroupWhereClause;
        private List deleteWBGroupTables;
        private List genericTableDeletes;
        private List deleteScheduleGroupTables;

        public void addToGenericTableDeletes(GenericTableDelete tab) {
            if (genericTableDeletes == null) {
                genericTableDeletes = new ArrayList();
            }
            if (tab != null) {
                genericTableDeletes.add(tab);
            }
        }

        public List getGenericTableDeletes() {
            return genericTableDeletes;
        }

        public boolean shouldCommit(){
            return shouldCommit;
        }

        public void setShouldCommit(boolean v){
            shouldCommit=v;
        }

        public boolean isConfirmationOnly(){
            return isConfirmationOnly;
        }

        public void setConfirmationOnly(boolean v){
            isConfirmationOnly=v;
        }

        public DBConnection getDBConnection(){
            return conn;
        }

        public void setDBConnection(DBConnection v){
            conn=v;
        }

        public String getDeleteEmployeeWhereClause(){
            return deleteEmployeeWhereClause;
        }

        public void setDeleteEmployeeWhereClause(String v){
            deleteEmployeeWhereClause=v;
        }

        public List getDeleteEmployeeTables(){
            return deleteEmployeeTables;
        }

        public void setDeleteEmployeeTables(List v){
            deleteEmployeeTables=v;
        }

        public List getDeleteTeamTables(){
            return deleteTeamTables;
        }

        public void setDeleteTeamTables(List v){
            deleteTeamTables=v;
        }

        public List getDeleteDeptTables(){
            return deleteDeptTables;
        }

        public void setDeleteDeptTables(List v){
            deleteDeptTables=v;
        }

        public List getDeleteJobTables(){
            return deleteJobTables;
        }

        public void setDeleteJobTables(List v){
            deleteJobTables=v;
        }

        public List getDeleteCalcgrpTables(){
            return deleteCalcgrpTables;
        }

        public void setDeleteCalcgrpTables(List v){
            deleteCalcgrpTables=v;
        }

        public String getDeleteTeamWhereClause(){
            return deleteTeamWhereClause;
        }

        public void setDeleteTeamWhereClause(String v){
            deleteTeamWhereClause=v;
        }

        public String getDeleteDeptWhereClause(){
            return deleteDeptWhereClause;
        }

        public void setDeleteDeptWhereClause(String v){
            deleteDeptWhereClause=v;
        }

        public String getDeleteJobWhereClause(){
            return deleteJobWhereClause;
        }

        public void setDeleteJobWhereClause(String v){
            deleteJobWhereClause=v;
        }

        public String getDeleteCalcgrpWhereClause(){
            return deleteCalcgrpWhereClause;
        }

        public void setDeleteCalcgrpWhereClause(String v){
            deleteCalcgrpWhereClause=v;
        }

        public String getDeleteWBGroupWhereClause(){
            return deleteWBGroupWhereClause;
        }

        public void setDeleteWBGroupWhereClause(String v){
            deleteWBGroupWhereClause=v;
        }

        public List getDeleteWBGroupTables(){
            return deleteWBGroupTables;
        }

        public void setDeleteOverrideWhereClause(String v){
            deleteOverrideWhereClause=v;
        }

        public String getDeleteOverrideWhereClause(){
            return deleteOverrideWhereClause;
        }

        public void setDeleteScheduleGroupWhereClause(String v){
            deleteScheduleGroupWhereClause=v;
        }

        public String getDeleteScheduleGroupWhereClause(){
            return deleteScheduleGroupWhereClause;
        }

        public List getDeleteScheduleGroupTables(){
            return deleteScheduleGroupTables;
        }

        public void setDeleteScheduleGroupTables(List v){
            deleteScheduleGroupTables=v;
        }

        public boolean getDeleteOverrideCalculates(){
            return deleteOverrideCalculates;
        }

        public void setDeleteOverrideCalculates(boolean v){
            deleteOverrideCalculates=v;
        }

        public void setDeleteWBGroupTables(List v){
            deleteWBGroupTables=v;
        }

        public String getBaseDsLogin(){
            return baseDsLogin;
        }

        public void setBaseDsLogin(String v){
            baseDsLogin=v;
        }

        public String getBaseDsPassword(){
            return baseDsPassword;
        }

        public void setBaseDsPassword(String v){
            baseDsPassword=v;
        }

        public String getBaseDsDriver(){
            return baseDsDriver;
        }

        public void setBaseDsDriver(String v){
            baseDsDriver=v;
        }

        public String getBaseDsUrl(){
            return baseDsUrl;
        }

        public void setBaseDsUrl(String v){
            baseDsUrl=v;
        }

        public String getClientId(){
            return clientId;
        }

        public void setClientId(String v){
            clientId=v;
        }

        public String toString(){
              return
                "deleteEmployeeWhereClause=" + deleteEmployeeWhereClause + "\n" +
                "deleteTeamWhereClause=" + deleteTeamWhereClause + "\n" +
                "deleteDeptWhereClause=" + deleteDeptWhereClause + "\n" +
                "deleteJobWhereClause=" + deleteJobWhereClause + "\n" +
                "deleteCalcgrpWhereClause=" + deleteCalcgrpWhereClause + "\n" +
                "deleteEmployeeWhereClause=" + deleteEmployeeTables + "\n" +
                "deleteTeamTables=" + deleteTeamTables + "\n" +
                "deleteDeptTables=" + deleteDeptTables + "\n" +
                "deleteJobTables=" + deleteJobTables + "\n" +
                "deleteCalcgrpTables=" + deleteCalcgrpTables + "\n" +
                "baseDsLogin=" + baseDsLogin + "\n" +
                "baseDsPassword=" + baseDsPassword + "\n" +
                "baseDsDriver=" + baseDsDriver + "\n" +
                "baseDsUrl=" + baseDsUrl + "\n" +
                "clientId=" + clientId + "\n" ;
        }

        public void validate() {
            assertNotEmpty(getClientId() , "ClientId");
            assertTable(getDeleteEmployeeWhereClause() , getDeleteEmployeeTables() , "DeleteEmployeeTables");
            assertTable(getDeleteTeamWhereClause() , getDeleteTeamTables() , "DeleteTeamTables");
            assertTable(getDeleteJobWhereClause() , getDeleteJobTables() , "DeleteJobTables");
            assertTable(getDeleteDeptWhereClause() , getDeleteDeptTables() , "DeleteDeptTables");
            assertTable(getDeleteCalcgrpWhereClause() , getDeleteCalcgrpTables() , "DeleteCalcgrpTables");
            assertTable(getDeleteWBGroupWhereClause() , getDeleteWBGroupTables() , "DeleteWBGroupTables");
            if (conn == null) {
                assertNotEmpty(getBaseDsLogin(), "BaseDsLogin");
                assertNotEmpty(getBaseDsPassword(), "BaseDsPassword");
                assertNotEmpty(getBaseDsDriver(), "BaseDsDriver");
                assertNotEmpty(getBaseDsUrl(), "BaseDsUrl");
            }
        }

        private void assertTable(String whereClause, List lst, String msg) {
            if (!StringHelper.isEmpty(whereClause)
                && (lst == null
                    || lst.size() == 0)) {
                throw new RuntimeException(msg + " list cannot be empty when where is supplied");
            }
        }

        private void assertNotEmpty(Object obj, String msg) {
            if (StringHelper.isEmpty(obj)) {
                throw new RuntimeException(msg + " cannot be empty");
            }
        }

        private void assertNotEmpty(int obj, String msg) {
            if (obj == Integer.MIN_VALUE ) {
                throw new RuntimeException(msg + " cannot be empty");
            }
        }

    }


}



