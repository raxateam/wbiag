package com.wbiag.server.wbiagprocess;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * see http://wbiag/internal/IAG_Internal_Guide.htm
 *   'Synchronization of TestTrack to IAGSolutionTrack' section for details
 */
public class SynchTestTrackTask  extends AbstractScheduledJob{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(SynchTestTrackTask.class);

    public static final String PARAM_RELEASE_NOTES_FILE_PATH = "RELEASE_NOTES_FILE_PATH";

    private static final String TESTTRACK_DB_DRVR = "oracle.jdbc.driver.OracleDriver";
    private static final String TESTTRACK_DB_URL = "jdbc:oracle:oci8:@devhevn";
    private static final String TESTTRACK_DB_USR = "workbrain";
    private static final String TESTTRACK_DB_PWD = "workbrain";
    private static final int DEF_WISA_ID = 0;

    private static final String TESTTRACK_CLIENT_WBIAG = "WORKBRAIN, IMPLEMENTATION ARCHITECTURE GROUP";
    private static final String SOLTRACK_CLIENT_WBIAG = "WBIAG";

    private long startMs;
    private Map wbiagCust = new HashMap();
    private List createdSols = new ArrayList();
    private List updatedSols = new ArrayList();
    private List deletedSols = new ArrayList();

    public Status run( int taskID, Map parameters ) throws Exception    {
        DBConnection connThis = null;
        try {
            connThis = getConnection();
            connThis.setAutoCommit(false);
            execute(connThis , parameters);
            connThis.commit();
        } catch ( Exception e ) {
            getLogger().error( e, e );
            connThis.rollback();
            return jobFailed( "Testtrack sync task failed." );
        } finally {
            releaseConnection();
        }

        return jobOk("Finished successfully."
                     + getCreatedWbiagSolutions().size() + " testtracks have been created, "
                     + getUpdatedWbiagSolutions().size() + " testtracks have been updated, "
                     + getDeletedWbiagSolutions().size() + " testtracks have been deleted"
                     );
    }

    public String getTaskUI() {
        return "/jobs/synchTestTrackParams.jsp";
    }

    /**
     * Runs SynchTestTrackTask based on loaded SynchTestTrackTaskContext
     * @throws Exception
     */
    public void execute(DBConnection connThis , Map parameters) throws Exception {
        createdSols.clear(); updatedSols.clear();  deletedSols.clear();
        startMs = System.currentTimeMillis();
        Connection connTT = null;
        try {
            connTT = getTTConnection();
            loadWbiagCustomers(connThis);
            List ttDefects = getExistingTTDefects(connTT);
            IntegerList solIds = getWBIAGSolutions(connThis);
            Iterator iter = ttDefects.iterator();
            while (iter.hasNext()) {
                TTDefect item = (TTDefect) iter.next();
                if (!solIds.contains(item.ttNumber)) {
                    if (!WbiagSolutionData.WIS_STATUS_FORCE_CLOSED.equals(item.status)) {
                        createdSols.add(item);
                    }
                }
                else {
                    if (WbiagSolutionData.WIS_STATUS_FORCE_CLOSED.equals(item.status)) {
                        if (logger.isDebugEnabled()) logger.debug("TT : " + item.ttNumber + " was force closed");
                        deletedSols.add(item);
                    }
                    else {
                        if (!StringHelper.isEmpty(item.assignedTo)) {
                            updatedSols.add(item);
                        }
                    }
                }
            }

            deleteWbiagSolutions(connThis , deletedSols);
            insertWbiagSolutions(connThis , createdSols);
            updateWbiagSolutions(connThis , updatedSols);
            String relPath = (String)parameters.get(PARAM_RELEASE_NOTES_FILE_PATH);
            if (!StringHelper.isEmpty(relPath)) {
                try {
                    CreateReleaseNotesFile crn = new CreateReleaseNotesFile();
                    crn.execute(connThis, relPath);
                    if (logger.isDebugEnabled()) logger.debug("Release notes file created successfully");
                }
                catch (Exception ex) {
                    logger.error("Error in creating release notes file", ex);
                    if (logger.isDebugEnabled()) logger.debug("Release notes file creation failed");
                }
            }
        }
        finally {
            if (connTT != null) connTT.close();
        }
        meterTime("ALL PROCESS" , startMs);
    }

    public List getCreatedWbiagSolutions() {
        return createdSols;
    }

    public List getUpdatedWbiagSolutions() {
        return updatedSols;
    }

    public List getDeletedWbiagSolutions() {
        return deletedSols;
    }

    private static final int IDCUSTREC_CUSTOMER = 104;
    private static final int IDCUSTREC_CUSTOMER_IAG = 191;
    private static final int IDCUSTREC_ARCHITECT = 113;
    private static final int IDCUSTREC_ARCHITECT_IAG_TYPE_BUILD = 198;
    private static final int IDCUSTREC_ARCHITECT_IAG_TYPE_REVIEW = 203;
    private static final int IDTYPE_FEATURE_REQUEST = 7;
    private static final int IDTYPE_CLIENT_CUSTOMIZATION = 9;

    private List getExistingTTDefects(Connection conn) throws SQLException{
        List ttDefects = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT distinct tt_defectnum, tt_summary , tt_defects.tt_idrecord "
                + " FROM tt_defects, tt_custmval "
                + " WHERE tt_defects.tt_idrecord =  tt_custmval.tt_iddefrec AND "
                + " tt_idtype in (?,?) AND "  // not a bug. Feature Req or CLient Customization
                + " ((tt_idcustrec=? and tt_custvalue = ?) OR " // customer = WBIAG
                + "  (tt_idcustrec=? and tt_custvalue = ?) OR"   // architect = WBIAG Type A
                + "  (tt_idcustrec=? and tt_custvalue = ?))"   // architect = WBIAG Type B
                + " ORDER BY tt_defectnum";

            ps = conn.prepareStatement(sql);
            int cnt = 1;
            ps.setInt(cnt++, IDTYPE_FEATURE_REQUEST);
            ps.setInt(cnt++, IDTYPE_CLIENT_CUSTOMIZATION);
            ps.setInt(cnt++, IDCUSTREC_CUSTOMER);
            ps.setInt(cnt++, IDCUSTREC_CUSTOMER_IAG);
            ps.setInt(cnt++, IDCUSTREC_ARCHITECT);
            ps.setInt(cnt++, IDCUSTREC_ARCHITECT_IAG_TYPE_BUILD);
            ps.setInt(cnt++, IDCUSTREC_ARCHITECT);
            ps.setInt(cnt++, IDCUSTREC_ARCHITECT_IAG_TYPE_REVIEW);
            rs = ps.executeQuery();
            while (rs.next()) {
                TTDefect tt = new TTDefect ();
                tt.ttNumber = rs.getInt(1);
                tt.ttDesc = rs.getString(2);
                tt.idRecord = rs.getInt(3);
                assignTTExtra(conn , tt);
                ttDefects.add(tt);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return ttDefects;
    }

    private static final String WBIAG_CUSTOMER_SQL = "SELECT wic_id, wic_name FROM wbiag_customer";

    private void loadWbiagCustomers(DBConnection conn) throws SQLException{
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(WBIAG_CUSTOMER_SQL);
            rs = ps.executeQuery();
            while (rs.next()) {
                wbiagCust.put(rs.getString(2) , rs.getString(1));
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

    }

    private static final String WBIAG_CUSTOMER_INSERT_SQL = "INSERT INTO wbiag_customer(wic_id, wic_name ) VALUES (?,?)";

    private int getWbiagCustomerId(DBConnection conn , TTDefect item) throws SQLException{
        int ret = -1;
        if (wbiagCust.containsKey(item.customerName)) {
            ret = Integer.parseInt((String)wbiagCust.get(item.customerName));
        }
        else {
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(WBIAG_CUSTOMER_INSERT_SQL);
                ret = conn.getDBSequence("seq_wic_id").getNextValue();
                ps.setInt(1 , ret);
                ps.setString(2 , item.customerName);
                ps.executeUpdate();
                if (logger.isDebugEnabled()) logger.debug("Created customer : " + item.customerName);
            }
            finally {
                if (ps != null) ps.close();
            }
        }
        return ret;
    }

    private static final int EVENTDEF_ID_ASSIGN = 1;
    private static final int EVENTDEF_ID_FIX = 3;
    private static final int EVENTDEF_ID_RELEASE_TO_TESTING = 4;
    private static final int EVENTDEF_ID_VERIFY = 5;
    private static final int EVENTDEF_FORCE_CLOSE = 8;
    private static final int EVENTDEF_COMMENT = 11;

    private void assignTTExtra(Connection conn, TTDefect defect) throws SQLException {
        assignTTAssignedToStatus(conn, defect);
        assignCustomerType(conn , defect);
    }

    private static final String TT_EVENTS_SQL =
        "SELECT tt_evtdefid, tt_asgndusers, tt_dateevent " +
        "     FROM  tt_defectevts " +
        "     WHERE tt_parentid = ? " +
        "     ORDER BY tt_ordernum";

    private void assignTTAssignedToStatus(Connection conn, TTDefect defect) throws SQLException {
        String asgnUsersIdFinal = null;
        int defEvtFinal = -1;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(TT_EVENTS_SQL);
            ps.setInt(1, defect.idRecord);
            rs = ps.executeQuery();
            while (rs.next()) {
                String tt_asgndusers = rs.getString(2);
                int tt_evtdef_id = rs.getInt(1);
                Date eventDate = rs.getDate(3);
                if (!StringHelper.isEmpty(tt_asgndusers)) {
                    asgnUsersIdFinal = tt_asgndusers;
                }
                if (defEvtFinal == EVENTDEF_ID_RELEASE_TO_TESTING
                    && tt_evtdef_id == EVENTDEF_ID_ASSIGN) {
                    // *** assignments after release to testing are ignored for status
                    if (logger.isDebugEnabled()) logger.debug("Do nothing");
                }
                else {
                    if (tt_evtdef_id != EVENTDEF_COMMENT) {
                        defEvtFinal = tt_evtdef_id;
                        // *** fix date is release date
                        if (tt_evtdef_id == EVENTDEF_ID_FIX) {
                            defect.releaseDate = eventDate;
                        }
                    }
                }
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        if (!StringHelper.isEmpty(asgnUsersIdFinal)) {
            try {
                defect.assignedTo = getAssignedUser(conn,
                    Integer.parseInt(asgnUsersIdFinal));
            }
            catch (Exception ex) {
                defect.assignedTo = null;
            }
        }
        switch (defEvtFinal) {
            case EVENTDEF_ID_ASSIGN :
                defect.status = WbiagSolutionData.WIS_STATUS_IN_PROGRESS;
                break;
            case EVENTDEF_ID_RELEASE_TO_TESTING :
                defect.status = WbiagSolutionData.WIS_STATUS_RELEASED_TO_TESTING;
                break;
            case EVENTDEF_ID_FIX :
                defect.status = WbiagSolutionData.WIS_STATUS_IN_PROGRESS;
                break;
            case EVENTDEF_ID_VERIFY:
                defect.status =WbiagSolutionData. WIS_STATUS_COMPLETE;
                break;
            case EVENTDEF_FORCE_CLOSE:
                defect.status = WbiagSolutionData.WIS_STATUS_FORCE_CLOSED;
                break;
            default:
                defect.status = WbiagSolutionData.WIS_STATUS_NOT_STARTED;
                break;
        }
    }

    private static final String ASSIGNED_TO_SQL =
        "SELECT tt_users.tt_firstname, tt_users.tt_lastname FROM tt_users " +
        "WHERE tt_idrecord = ?";

    private String getAssignedUser(Connection conn, int idRecord) throws SQLException {
        String ret = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(ASSIGNED_TO_SQL);
            ps.setInt(1 , idRecord);
            rs = ps.executeQuery();
            if (rs.next()) {
                String fName = rs.getString(1);
                String lName = rs.getString(2);
                if (!StringHelper.isEmpty(fName)
                    && !StringHelper.isEmpty(lName) ) {
                    lName = StringHelper.searchReplace(lName , "(AX", "(X");
                    int extInd = lName.indexOf("(X");
                    if ( extInd > -1) {
                        lName = lName.substring(0 , extInd);
                    }
                    ret = fName.substring(0, 1).toLowerCase() + lName.toLowerCase();
                }
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return ret;
    }
    private static final  String TT_CUSTOMER_TYPE_SQL =
        "SELECT tt_custmval.tt_idcustrec, tt_custvalue,tt_descriptor  FROM tt_custmval,tt_fldcustm "
        + "WHERE tt_fldcustm.tt_idrecord = tt_custmval.TT_CUSTVALUE "
        + "AND tt_iddefrec =? and tt_custmval.tt_idcustrec IN (?,?)";

    /**
     * If customer is IAG, type=C, otw infer it from Arhitect attribute
     * @param conn
     * @param defect
     * @throws SQLException
     */
    private void assignCustomerType(Connection conn, TTDefect defect) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = conn.prepareStatement(TT_CUSTOMER_TYPE_SQL);
            ps.setInt(1 , defect.idRecord);
            ps.setInt(2 , IDCUSTREC_CUSTOMER);
            ps.setInt(3 , IDCUSTREC_ARCHITECT);
            rs = ps.executeQuery();
            while (rs.next()) {
                int custrec = rs.getInt(1);
                int custVal = rs.getInt(2);
                if (IDCUSTREC_CUSTOMER == custrec) {
                    defect.customerName = rs.getString(3);
                    if (IDCUSTREC_CUSTOMER_IAG == custVal) {
                        defect.type = WbiagSolutionData.WIS_TYPE_LIBRARY ;
                    }
                }
                else if (IDCUSTREC_ARCHITECT == custrec) {
                    if (StringHelper.isEmpty(defect.type)) {
                        if (IDCUSTREC_ARCHITECT_IAG_TYPE_BUILD == custVal) {
                            defect.type = WbiagSolutionData.WIS_TYPE_BUILD;
                        }
                        else if (IDCUSTREC_ARCHITECT_IAG_TYPE_REVIEW == custVal) {
                            defect.type = WbiagSolutionData.WIS_TYPE_REVIEW;
                        }
                    }

                }
                // *** map long TT client WBIAG name
                if (TESTTRACK_CLIENT_WBIAG.equals(defect.customerName)) {
                    defect.customerName = SOLTRACK_CLIENT_WBIAG;
                }

            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        // *** if nothing assigned default LIBRARY
        if (StringHelper.isEmpty(defect.type)) {
            defect.type = WbiagSolutionData.WIS_TYPE_LIBRARY;
        }

    }

    private static final  String INSERT_SOL_SQL =
        "INSERT INTO wbiag_solution (wis_id,wis_tt_number,wis_tt_summary,wis_solution_type,wisa_id,wis_assgn_to,wis_status,wic_id)"
        + "VALUES (seq_wis_id.nextval,?,?,?,?,?,?,?)";

    private int insertWbiagSolutions(DBConnection conn , List list) throws SQLException {
        if (list == null || list.size() == 0) {
            return 0;
        }

        int cnt = 0;
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            TTDefect item = (TTDefect) iter.next();
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(INSERT_SOL_SQL);
                ps.setInt(1 , item.ttNumber);
                ps.setString(2 , item.ttDesc);
                ps.setString(3 , item.type);
                ps.setInt(4 , DEF_WISA_ID);
                ps.setString(5 , item.assignedTo);
                ps.setString(6 , item.status);
                ps.setInt(7 , getWbiagCustomerId(conn , item));
                ps.executeUpdate();
            }
            finally {
                if (ps != null) ps.close();
            }

            if (logger.isDebugEnabled()) logger.debug("(" + item + ") did not exist in WBIAG_SOLUTION and was created");
            cnt++;
        }
        return cnt;
    }

    private static final  String UPDATE_SOL_SQL =
        "UPDATE wbiag_solution SET wis_assgn_to = ? , wis_status =?, wis_release_date = ? "
        + "WHERE wis_tt_number=? ";

    private int updateWbiagSolutions(DBConnection conn , List list) throws SQLException {
        if (list == null || list.size() == 0) {
            return 0;
        }

        int cnt = 0;
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            TTDefect item = (TTDefect) iter.next();
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(UPDATE_SOL_SQL);
                ps.setString(1 , item.assignedTo);
                ps.setString(2 , item.status);
                if (item.releaseDate != null) {
                    ps.setTimestamp(3, new Timestamp(item.releaseDate.getTime()));
                }
                else {
                    ps.setNull(3 , Types.TIMESTAMP);
                }
                ps.setInt(4 , item.ttNumber);
                ps.executeUpdate();
            }
            finally {
                if (ps != null) ps.close();
            }

            if (logger.isDebugEnabled()) logger.debug("(" + item.ttNumber + ") assigned_to updated with assignedTo: " + item.assignedTo + ", status : " + item.status + ", releaseDate:" + item.releaseDate);
            cnt++;
        }
        return cnt;
    }

    private static final  String DELETE_SOL_SQL =
        "DELETE FROM wbiag_solution WHERE wis_tt_number=? ";

    private int deleteWbiagSolutions(DBConnection conn , List list) throws SQLException {
        if (list == null || list.size() == 0) {
            return 0;
        }

        int cnt = 0;
        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            TTDefect item = (TTDefect) iter.next();
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(DELETE_SOL_SQL);
                ps.setInt(1 , item.ttNumber);
                ps.executeUpdate();
            }
            finally {
                if (ps != null) ps.close();
            }

            if (logger.isDebugEnabled()) logger.debug("(" + item.ttNumber + ") deleted from WBIAG_SOLUTION");
            cnt++;
        }
        return cnt;
    }

    private IntegerList getWBIAGSolutions(DBConnection conn) throws SQLException{
        IntegerList sols = new IntegerList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT wis_tt_number FROM wbiag_solution ";
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                sols.add(rs.getInt(1));
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return sols;
    }

    private Connection getTTConnection() throws Exception{
        Class.forName( TESTTRACK_DB_DRVR );
        return DriverManager.getConnection(TESTTRACK_DB_URL, TESTTRACK_DB_USR, TESTTRACK_DB_PWD);
    }

    protected long meterTime(String what, long start){
        long l = System.currentTimeMillis();
        if (logger.isDebugEnabled()) logger.debug(what+" took: "+(l-start)+" millis");
        return l;
    }

    private class TTDefect {
        int ttNumber;
        String ttDesc;
        int idRecord;
        String assignedTo;
        String status;
        String customerName;
        String type;
        Date releaseDate;

        public String toString() {
            return  "TT No : " + ttNumber
                + "\n desc : " + ttDesc
                + "\n assignedTo : " + assignedTo
                + "\n status : " + status
                + "\n customerName : " + customerName
                + "\n type : " + type
                + "\n releaseDate : " + releaseDate
                ;
        }
    }

    public static void main( String[] args ) throws Exception {
        System.setProperty("junit.db.username" , "workbrain");
        System.setProperty("junit.db.password" , "workbrain");
        System.setProperty("junit.db.url" , "jdbc:oracle:oci8:@IAG01SIT");
        //System.setProperty("junit.db.url" , "jdbc:oracle:oci8:@IAG41DV");
        System.setProperty("junit.db.driver" , "oracle.jdbc.driver.OracleDriver");


        SynchTestTrackTask dst = new SynchTestTrackTask();
        Map params = new HashMap();
        params.put(PARAM_RELEASE_NOTES_FILE_PATH , "\\\\Toriag\\\\wbiag\\\\IAGReleaseNotes.txt");
        dst.execute(SQLHelper.connectTo() , params);
        System.out.println("Created " + dst.getCreatedWbiagSolutions().size() + " items");
        System.out.println("Updated " + dst.getUpdatedWbiagSolutions().size() + " items");
        System.out.println("Deleted " + dst.getDeletedWbiagSolutions().size() + " items");
    }

}


