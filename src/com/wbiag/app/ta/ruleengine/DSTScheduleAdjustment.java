package com.wbiag.app.ta.ruleengine ;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
/**
 * DSTScheduleAdjustment for DayLight Saving Days
 */
public class DSTScheduleAdjustment  {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(DSTScheduleAdjustment.class);

    public static final String PROP_DATE_FORMAT = "yyyyMMdd HHmm";

    public static final String OVR_WBU_NAME = "DST Schedule Adjustment";

    public static final String PROP_DST_FALLBACK_CHANGE_DATETIME = "DST_FALLBACK_CHANGE_DATETIME";
    public static final String PROP_DST_SPRINGFORWARD_CHANGE_DATETIME = "DST_SPRINGFORWARD_CHANGE_DATETIME";
    public static final String PROP_BASE_DS_LOGIN = "BASE_DS_LOGIN";
    public static final String PROP_BASE_DS_PASSWORD = "BASE_DS_PASSWORD";
    public static final String PROP_BASE_DS_DRIVER = "BASE_DS_DRIVER";
    public static final String PROP_BASE_DS_URL = "BASE_DS_URL";
    public static final String PROP_CLIENT_ID = "CLIENT_ID";
    public static final String PROP_EMPLOYEE_WHERE_CLAUSE = "EMPLOYEE_WHERE_CLAUSE";
    public static final String PROP_EMPLOYEE_OVR_WBU_NAME = "OVR_WBU_NAME";
    public static final String PROP_EMPLOYEE_OVR_TYPE_ID = "OVR_TYPE_ID";
    public static final String PROP_CANCEL_DST_ADJUSTMENT_OVERRIDES = "CANCEL_DST_ADJUSTMENT_OVERRIDES";


    protected DSTScheduleAdjustmentContext context;
    private long startMs;
    private long stepMs;
    private List logs;

    protected DSTScheduleAdjustment() {}

    /**
     * Loads context from property file
     * @param propFilePath propFilePath
     * @throws Exception
     */
    public DSTScheduleAdjustment(String propFilePath) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream(propFilePath));
        context = createContextFromPropFile(props);
        this.context.validate();
    }

    /**
     * Loads given context
     * @param context DSTScheduleAdjustmentContext
     * @throws Exception
     */
    public DSTScheduleAdjustment(DSTScheduleAdjustmentContext context) throws Exception {
        this.context = context;
        this.context.validate();
    }

    /**
     * Runs ScheduleAdjustment based on loaded DSTScheduleAdjustmentContext
     * @throws Exception
     */
    public void execute() throws Exception {
        startMs = System.currentTimeMillis();
        log("Parameters :\n" + context);

        DBConnection conn = null;
        try {
            conn = context.getDBConnection();
            // *** connection is null, check driver params
            if (conn == null) {
                conn = getDBConnection(context.getBaseDsLogin(),
                                       context.getBaseDsPassword(),
                                       context.getBaseDsUrl(),
                                       context.getBaseDsDriver());
            }
            if (conn == null) {
                throw new RuntimeException("Connection was not established");
            }
            processFallBack(conn);
            processSpringForward(conn);
            conn.commit();
        }
        finally {
            if (conn != null) conn.close();
        }
        meterTime("ALL PROCESS" , startMs);
    }

    /**
     * Returns run logs
     * @return
     */
    public List getRunLogs() {
        return logs;
    }

    protected void processFallBack(DBConnection conn) throws Exception{
        if (context.getDstFallbackChangeDatetime() != null) {
            if (context.deletesDstAdjustmentOverrides()) {
                int del = deleteDstOverrides(conn ,
                                             context.getDstFallbackChangeDatetime());
                log("Deleted " + del + " Fall Back DST adjustment overrides");
            }

            List empskds = findEmployeeSkdsOverlapping(conn,
                context.getDstFallbackChangeDatetime());
            log("Found " + empskds.size() + " overlapping schedules for FallBack");
            int ovrCnt = createOverrides(conn ,
                            context.getDstFallbackChangeDatetime(),
                            empskds,
                            -60);
            log("Created " + ovrCnt + " overrides for FallBack");
        }
    }

    protected void processSpringForward(DBConnection conn) throws Exception{
        if (context.getDstSpringforwardChangeDatetime() != null) {
            if (context.deletesDstAdjustmentOverrides()) {
                int del = deleteDstOverrides(conn , context.getDstSpringforwardChangeDatetime());
                log("Deleted " + del + " Spring Forward DST adjustment overrides");
            }

            List empskds = findEmployeeSkdsOverlapping(conn,
                context.getDstSpringforwardChangeDatetime());
            log("Found " + empskds.size() + " overlapping schedules for Spring Forward");
            int ovrCnt = createOverrides(conn ,
                            context.getDstSpringforwardChangeDatetime(),
                            empskds,
                            60);
            log("Created " + ovrCnt + " overrides for FallBack");
        }
    }

    protected List findEmployeeSkdsOverlapping(DBConnection conn , Date date)
        throws SQLException {

        List ret = new ArrayList();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT * ");
            sb.append(" FROM employee_schedule ");
            sb.append(" WHERE (? BETWEEN empskd_act_start_time AND empskd_act_end_time");
            sb.append(" OR ? BETWEEN empskd_act_start_time2 AND empskd_act_end_time2 ");
            sb.append(" OR ? BETWEEN empskd_act_start_time3 AND empskd_act_end_time3 ");
            sb.append(" OR ? BETWEEN empskd_act_start_time4 AND empskd_act_end_time4 ");
            sb.append(" OR ? BETWEEN empskd_act_start_time5 AND empskd_act_end_time5) ");
            if (!StringHelper.isEmpty(context.getEmployeeWhereClause())) {
                sb.append(" AND ").append(context.getEmployeeWhereClause());
            }

            ps = conn.prepareStatement(sb.toString());
            ps.setTimestamp(1 , new Timestamp(date.getTime()));
            ps.setTimestamp(2 , new Timestamp(date.getTime()));
            ps.setTimestamp(3 , new Timestamp(date.getTime()));
            ps.setTimestamp(4 , new Timestamp(date.getTime()));
            ps.setTimestamp(5 , new Timestamp(date.getTime()));
            rs = ps.executeQuery();
            while (rs.next()) {
                EmployeeScheduleData esd = new EmployeeScheduleData();
                esd.assignByName(rs);
                esd.setCodeMapper(CodeMapper.createCodeMapper(conn));
                ret.add(esd);
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        return ret;
    }

    protected int deleteDstOverrides(DBConnection conn , Date date)
        throws OverrideException , SQLException {
        int cnt = 0;
        PreparedStatement ps = null;
        ResultSet rs = null;
        OverrideBuilder ob = new OverrideBuilder(conn);
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT ovr_id FROM override");
            sb.append(" WHERE ovrtyp_id BETWEEN ? AND ? ");
            sb.append(" AND ovr_status = ? ");
            sb.append(" AND ovr_start_date BETWEEN ? and ? ");
            if (!StringHelper.isEmpty(context.getEmployeeWhereClause())) {
                sb.append(" AND ").append(context.getEmployeeWhereClause());
            }
            sb.append(" AND wbu_name = ? ");
            ps = conn.prepareStatement(sb.toString());
            ps.setInt(1 , OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
            ps.setInt(2 , OverrideData.SCHEDULE_SCHEDTIMES_TYPE + 4);
            ps.setString(3 , OverrideData.APPLIED);
            ps.setTimestamp(4 , DateHelper.addDays(DateHelper.truncateToDays(date) , -1));
            ps.setTimestamp(5 , DateHelper.addDays(DateHelper.truncateToDays(date) , 1));
            ps.setString(6 , context.getOvrWbuName());
            rs = ps.executeQuery();
            while (rs.next()) {
                DeleteOverride dov = new DeleteOverride ();
                dov.setOverrideId(rs.getInt(1));
                ob.add(dov);
                cnt++;
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

        ob.execute(true);
        return cnt;
    }

    protected int createOverrides(DBConnection conn, Date changeDatetime,
                                   List empskds,
                                   int offsetMins) throws Exception {
        if (empskds == null || empskds.size() == 0) {
            return 0;
        }
        OverrideBuilder ob = new OverrideBuilder(conn);

        IntegerList empIds = new IntegerList();
        Date minDate = DateHelper.DATE_3000; Date maxDate =  DateHelper.DATE_1900;
        int ovrCnt = 0;
        Iterator iter = empskds.iterator();
        while (iter.hasNext()) {
            EmployeeScheduleData esd = (EmployeeScheduleData)iter.next();
            int changedShiftInd = -1;
            for (int i = 0 ; i <= 4 ; i++ ) {
                if (esd.retrieveShiftScheduled(i)
                    && DateHelper.isBetween(changeDatetime , esd.retrieveShiftStartTime(i) , esd.retrieveShiftEndTime(i)) ) {
                    changedShiftInd = i;
                    break;
                }
            }
            if (changedShiftInd != -1) {
                empIds.add(esd.getEmpId());
                if (esd.getWorkDate().compareTo(minDate) < 0) {
                    minDate = esd.getWorkDate();
                }
                if (esd.getWorkDate().compareTo(maxDate) > 0) {
                    maxDate = esd.getWorkDate();
                }

                InsertOverride io = new InsertOverride();
                io.setEmpId(esd.getEmpId());
                io.setWbuName(context.getOvrWbuName());
                io.setWbuNameActual(context.getOvrWbuName());
                io.setStartDate(esd.getWorkDate());
                io.setEndDate(esd.getWorkDate());
                io.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE + changedShiftInd);
                StringBuffer sb = new StringBuffer(200);
                sb.append("\"").append(esd.EMPSKD_ACT_START_TIME).append(changedShiftInd == 0 ? "" : String.valueOf(changedShiftInd + 1));
                sb.append("=");
                sb.append(convertDateString(esd.retrieveShiftStartTime(changedShiftInd) , OverrideData.OVERRIDE_TIME_FORMAT_STR));
                sb.append("\",");
                sb.append("\"").append(esd.EMPSKD_ACT_END_TIME).append(changedShiftInd == 0 ? "" : String.valueOf(changedShiftInd + 1));
                sb.append("=");
                Date newEndTime = DateHelper.addMinutes(esd.retrieveShiftEndTime(changedShiftInd), offsetMins);
                String sNewEndTime = convertDateString(newEndTime , OverrideData.OVERRIDE_TIME_FORMAT_STR);
                // *** hack for DateHelper.addMinutes issue. offsetMins does not work work hours between i.e 1:00 and 1:59
                Date chk = convertStringToDate(sNewEndTime , OverrideData.OVERRIDE_TIME_FORMAT_STR);
                if (chk.getTime() == esd.retrieveShiftEndTime(changedShiftInd).getTime()) {
                    newEndTime = DateHelper.addMinutes(esd.retrieveShiftEndTime(changedShiftInd), 2*offsetMins);
                    sNewEndTime = convertDateString(newEndTime , OverrideData.OVERRIDE_TIME_FORMAT_STR);
                }
                sb.append(sNewEndTime);
                sb.append("\",");
                io.setOvrNewValue(sb.toString());
                ob.add(io);
                log("Added override for empId : " + esd.getEmpId() + " with new value : " + sb.toString());
                ovrCnt++;
            }
        }

        ob.setEmpIdList(empIds);
        ob.setFirstStartDate(minDate);
        ob.setLastEndDate(maxDate);
        // *** this commits
        ob.execute(true);
        return ovrCnt;
    }

    private DSTScheduleAdjustmentContext createContextFromPropFile(Properties props) throws ParseException{
        DSTScheduleAdjustmentContext ret = new DSTScheduleAdjustmentContext();

        ret.setBaseDsLogin(props.getProperty(PROP_BASE_DS_LOGIN));
        ret.setBaseDsPassword(props.getProperty(PROP_BASE_DS_PASSWORD));
        ret.setBaseDsDriver(props.getProperty(PROP_BASE_DS_DRIVER));
        ret.setBaseDsUrl(props.getProperty(PROP_BASE_DS_URL));

        ret.setClientId(props.getProperty(PROP_CLIENT_ID));

        ret.setEmployeeWhereClause(props.getProperty(PROP_EMPLOYEE_WHERE_CLAUSE));
        if (!StringHelper.isEmpty(props.getProperty(PROP_DST_FALLBACK_CHANGE_DATETIME))) {
            ret.setDstFallbackChangeDatetime(convertStringToDate(
                props.getProperty(PROP_DST_FALLBACK_CHANGE_DATETIME), PROP_DATE_FORMAT));
        }
        if (!StringHelper.isEmpty(props.getProperty(PROP_DST_SPRINGFORWARD_CHANGE_DATETIME))) {
            ret.setDstSpringforwardChangeDatetime(convertStringToDate(
                props.getProperty(PROP_DST_SPRINGFORWARD_CHANGE_DATETIME), PROP_DATE_FORMAT));
        }
        ret.setOvrWbuName(props.getProperty(PROP_EMPLOYEE_OVR_WBU_NAME, OVR_WBU_NAME));

        ret.setDeletesDstAdjustmentOverrides(Boolean.valueOf(props.getProperty(PROP_CANCEL_DST_ADJUSTMENT_OVERRIDES , "false")).booleanValue());
        return ret;
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

    private int[] detokenizeStringAsIntArray(String input,
                                             String separator) {
        String[] st = StringHelper.detokenizeString(input , separator);
        if (st == null || st.length == 0) {
            return null;
        }
        int[] stArray = new int[st.length];
        for (int i = 0; i < st.length; i++) {
            stArray[i] = Integer.parseInt(st[i]);
        }
        return stArray;
    }

    public static Date convertStringToDate(String str,
            String inputDateFormat) throws ParseException{
        DateFormat from = new SimpleDateFormat( inputDateFormat );
        return from.parse(str);
    }

    /**
     * Converts a date to String based on given format.
     *
     * @param date             date
     * @param inputDateFormat  format
     * @return                 formatted date string
     */
    public static String convertDateString(Date date,
            String inputDateFormat) {

        DateFormat from = new SimpleDateFormat( inputDateFormat );
        synchronized( from ) {
            return from.format(date);
        }
    }

    protected long meterTime(String what, long start){
        long l = System.currentTimeMillis();
        log(what+" took: "+(l-start)+" millis");
        return l;
    }

    protected void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
        if (logs == null) {
            logs = new ArrayList();
        }
        logs.add(msg);

    }

    public static void main( String[] args ) throws Exception {
        // *** context can be initialized as below
        /*DSTScheduleAdjustmentContext ctx = new DSTScheduleAdjustmentContext();
        ctx.setDstFallbackChangeDatetime(DateHelper.parseDate("20041031 0200" , PROP_DATE_FORMAT));
        ctx.setBaseDsLogin("workbrain");
        ctx.setBaseDsPassword("workbrain");
        ctx.setBaseDsDriver("oracle.jdbc.driver.OracleDriver");
        ctx.setBaseDsUrl("jdbc:oracle:oci8:@ROWBSP4");
        ctx.setClientId("1");
        ctx.setEmployeeWhereClause("");
        ctx.setDeletesDstAdjustmentOverrides(true);

        DSTScheduleAdjustment dst = new DSTScheduleAdjustment(ctx);
        dst.execute();*/
        if (args.length == 0) {
            throw new RuntimeException("Property file path must be supplied");
        }
        if (!FileUtil.fileExists(args[0])) {
            throw new RuntimeException("Property file not found");
        }
        DSTScheduleAdjustment dst = new DSTScheduleAdjustment(args[0]);
        //DSTScheduleAdjustment dst = new DSTScheduleAdjustment("C:\\source\\4.0_SP4\\Java\\src\\com\\wbiag\\app\\ta\\ruleengine\\DSTScheduleAdjustment.properties");
        dst.execute();
    }

    public static class DSTScheduleAdjustmentContext {
        private DBConnection dbConnection;
        private Date dstFallbackChangeDatetime;
        private Date dstSpringforwardChangeDatetime;
        private String baseDsLogin;
        private String baseDsPassword;
        private String baseDsDriver;
        private String baseDsUrl;
        private String clientId;
        private String employeeWhereClause;
        private String ovrWbuName;
        private boolean deletesDstAdjustmentOverrides;

        public DBConnection getDBConnection(){
            return dbConnection;
        }

        public void setDBConnection(DBConnection v){
            dbConnection=v;
        }

        public Date getDstFallbackChangeDatetime(){
            return dstFallbackChangeDatetime;
        }

        public void setDstFallbackChangeDatetime(Date v){
            dstFallbackChangeDatetime=v;
        }

        public Date getDstSpringforwardChangeDatetime(){
            return dstSpringforwardChangeDatetime;
        }

        public void setDstSpringforwardChangeDatetime(Date v){
            dstSpringforwardChangeDatetime=v;
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

        public String getEmployeeWhereClause(){
            return employeeWhereClause;
        }

        public void setEmployeeWhereClause(String v){
            employeeWhereClause=v;
        }

        public String getOvrWbuName(){
            return ovrWbuName;
        }

        public void setOvrWbuName(String v){
            ovrWbuName=v;
        }

        public boolean deletesDstAdjustmentOverrides(){
            return deletesDstAdjustmentOverrides;
        }

        public void setDeletesDstAdjustmentOverrides(boolean v){
            deletesDstAdjustmentOverrides=v;
        }

        public String toString(){
            return
            "dstFallbackChangeDatetime=" + dstFallbackChangeDatetime + "\n" +
            "dstSpringforwardChangeDatetime=" + dstSpringforwardChangeDatetime + "\n" +
            "baseDsLogin=" + baseDsLogin + "\n" +
            "baseDsPassword=" + baseDsPassword + "\n" +
            "baseDsDriver=" + baseDsDriver + "\n" +
            "baseDsUrl=" + baseDsUrl + "\n" +
            "clientId=" + clientId + "\n" +
            "employeeWhereClause=" + employeeWhereClause + "\n" +
            "ovrWbuName=" + ovrWbuName + "\n" +
            "deletesDstAdjustmentOverrides=" + deletesDstAdjustmentOverrides;
        }

        public void validate() {
            if (StringHelper.isEmpty(getDstFallbackChangeDatetime())
                && StringHelper.isEmpty(getDstSpringforwardChangeDatetime())) {
                throw new RuntimeException("Either DstFallbackChangeDatetime or DstFallbackChangeDatetime must be supplied");
            }
            assertNotEmpty(getClientId() , "ClientId");
            if (getDBConnection() == null) {
                assertNotEmpty(getBaseDsLogin(), "BaseDsLogin");
                assertNotEmpty(getBaseDsPassword(), "BaseDsPassword");
                assertNotEmpty(getBaseDsDriver(), "BaseDsDriver");
                assertNotEmpty(getBaseDsUrl(), "BaseDsUrl");
            }
            if (StringHelper.isEmpty(getOvrWbuName())) {
                setOvrWbuName(OVR_WBU_NAME);
            }
            setDstFallbackChangeDatetime(DateHelper.addMinutes(getDstFallbackChangeDatetime() , -60));
            setDstSpringforwardChangeDatetime(DateHelper.addMinutes(getDstSpringforwardChangeDatetime() , 60));
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


