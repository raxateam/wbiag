package com.wbiag.app.ta.ruleengine ;

import java.io.*;
import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.export.payroll.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * PayrollRegression that compares two payroll exports
 * <ul>
 * <li> from two databases
 * <li> to a given file
 * <li> to most recent file
 * </ul>
 */
public class PayrollRegression  {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PayrollRegression.class);

    public static final String FILE_EXT_BASE = "BASE";
    public static final String FILE_EXT_TARGET = "TARGET";
    public static final String FILE_EXT_DIFF = "DIFF";
    public static final String FILE_EXT_COMPARE = "COMPARE";

    public static final String PROP_DATE_FORMAT = "yyyyMMDdd";

    public static final String PROP_COMPARE_TYPE = "COMPARE_TYPE";
    public static final String PROPVAL_COMPARE_TYPE_COMPARE_2_DBS = "COMPARE_2_DBS";
    public static final String PROPVAL_COMPARE_TYPE_COMPARE_TO_GIVEN_FILE = "COMPARE_TO_GIVEN_FILE";
    public static final String PROPVAL_COMPARE_TYPE_COMPARE_TO_MOST_RECENT = "COMPARE_TO_MOST_RECENT";
    public static final String PROP_COMPARE_TO_GIVEN_FILE_PATH = "COMPARE_TO_GIVEN_FILE_PATH";
    public static final String PROP_BASE_DS_LOGIN = "BASE_DS_LOGIN";
    public static final String PROP_BASE_DS_PASSWORD = "BASE_DS_PASSWORD";
    public static final String PROP_BASE_DS_DRIVER = "BASE_DS_DRIVER";
    public static final String PROP_BASE_DS_URL = "BASE_DS_URL";
    public static final String PROP_TARGET_DS_LOGIN = "TARGET_DS_LOGIN";
    public static final String PROP_TARGET_DS_PASSWORD = "TARGET_DS_PASSWORD";
    public static final String PROP_TARGET_DS_DRIVER = "TARGET_DS_DRIVER";
    public static final String PROP_TARGET_DS_URL = "TARGET_DS_URL";
    public static final String PROP_CLIENT_NAME = "CLIENT_NAME";
    public static final String PROP_BASE_RECALCULATES = "BASE_RECALCULATES";
    public static final String PROP_TARGET_RECALCULATES = "TARGET_RECALCULATES";
    public static final String PROP_PAYROLL_OUTPUT_FILE_PATH = "PAYROLL_OUTPUT_FILE_PATH";
    public static final String PROP_PAYROLL_OUTPUT_FILE_NAME = "PAYROLL_OUTPUT_FILE_NAME";
    public static final String PROP_PAYGROUP_IDS = "PAYGROUP_IDS";
    public static final String PROP_PET_ID = "PET_ID";
    public static final String PROP_PAYROLL_START_DATE = "START_DATE";
    public static final String PROP_PAYROLL_END_DATE = "END_DATE";

    protected PayrollRegressionContext context;
    private long startMs;
    private long stepMs;

    protected PayrollRegression() {}

    /**
     * Loads context from proprty file
     * @param propFilePath propFilePath
     * @throws Exception
     */
    public PayrollRegression(String propFilePath) throws Exception {
        Properties props = new Properties();
        props.load(new FileInputStream(propFilePath));
        context = createContextFromPropFile(props);
        this.context.validate();
    }

    /**
     * Loads given context
     * @param context PayrollRegressionContext
     * @throws Exception
     */
    public PayrollRegression(PayrollRegressionContext context) throws Exception {
        this.context = context;
        this.context.validate();
    }

    /**
     * Runs regression based on loaded PayrollRegressionContext
     * @throws Exception
     */
    public void execute() throws Exception {
        startMs = System.currentTimeMillis();
        if (PROPVAL_COMPARE_TYPE_COMPARE_2_DBS.equals(context.getCompareType())){
            processCompare2DBs(context);
        }
        else if (PROPVAL_COMPARE_TYPE_COMPARE_TO_GIVEN_FILE.equals(context.getCompareType())){
            processCompareToGivenFile(context);
        }
        else if (PROPVAL_COMPARE_TYPE_COMPARE_TO_MOST_RECENT.equals(context.getCompareType())){
            processCompareToMostRecent(context);
        }
        meterTime("ALL PROCESS" , startMs);
    }

    protected void processCompare2DBs(PayrollRegressionContext context) throws Exception {
        DBConnection baseConn = null;
        DBConnection targetConn = null;
        try {
            baseConn = getDBConnection(context.getBaseDsLogin(),
                                       context.getBaseDsPassword(),
                                       context.getBaseDsUrl(),
                                       context.getBaseDsDriver());

            String fullBaseFilePath = runPayroll(baseConn, context.getClientName(),
                       context.getPetId(),
                       context.getPaygroupIds(),
                       context.getStartDate(),
                       context.getEndDate(),
                       context.getBaseRecalculates() ,
                       context.getPayrollOutputFilePath(),
                       context.getPayrollOutputFileName() ,
                       FILE_EXT_BASE);

            if (!FileUtil.fileExists(fullBaseFilePath)) {
                throw new RuntimeException(
                    "Base Payroll did not run successfully");
            }

            targetConn = getDBConnection(context.getBaseDsLogin(),
                                         context.getBaseDsPassword(),
                                         context.getBaseDsUrl(),
                                         context.getBaseDsDriver());

            String fullTargetFilePath = runPayroll(targetConn, context.getClientName(),
                       context.getPetId(),
                       context.getPaygroupIds(),
                       context.getStartDate(),
                       context.getEndDate(),
                       context.getTargetRecalculates(),
                       context.getPayrollOutputFilePath(),
                       context.getPayrollOutputFileName() ,
                       FILE_EXT_TARGET);

            if (!FileUtil.fileExists(fullTargetFilePath)) {
                throw new RuntimeException("Target Payroll did not run successfully");
            }
            String fullDiffFilePath = context.getPayrollOutputFilePath()
                + context.getClientName()
                + File.separator + context.getPayrollOutputFileName() + "." + FILE_EXT_DIFF;

            boolean existsDiff = existsDiffWhenCompareTwoFilesTwoWay(
                fullBaseFilePath,
                fullTargetFilePath,
                fullDiffFilePath);
            if (!existsDiff) {
                if (logger.isDebugEnabled()) logger.debug("Payroll Comparison Successful with NO DIFFERENCES");
            }
            else {
                throw new RuntimeException("DIFFERENCES in PAYROLL COMPARISON. See difference file under :" + fullDiffFilePath);
            }
        }
        finally {
            if (baseConn != null) {
                //baseConn.commit();
                baseConn.close();
            }
            if (targetConn != null) {
                //targetConn.commit();
                targetConn.close();
            }

        }
    }

    protected void processCompareToGivenFile(PayrollRegressionContext context) throws Exception {
        DBConnection baseConn = null;
        try {
            baseConn = getDBConnection(context.getBaseDsLogin(),
                                       context.getBaseDsPassword(),
                                       context.getBaseDsUrl(),
                                       context.getBaseDsDriver());

            String fullBaseFilePath = runPayroll(baseConn, context.getClientName(),
                       context.getPetId(),
                       context.getPaygroupIds(),
                       context.getStartDate(),
                       context.getEndDate(),
                       context.getBaseRecalculates(),
                       context.getPayrollOutputFilePath(),
                       context.getPayrollOutputFileName(),
                       FILE_EXT_BASE);
            if (!FileUtil.fileExists(fullBaseFilePath)) {
                throw new RuntimeException(
                    "Base Payroll did not run successfully");
            }

            String fullDiffFilePath = context.getPayrollOutputFilePath()
                + context.getClientName()
                + File.separator + context.getPayrollOutputFileName()  + "." + FILE_EXT_DIFF;

            boolean existsDiff = existsDiffWhenCompareTwoFilesTwoWay(
                fullBaseFilePath,
                context.getCompareToGivenFilePath(),
                fullDiffFilePath);
            if (!existsDiff) {
                if (logger.isDebugEnabled()) logger.debug("Payroll Comparison Successful with NO DIFFERENCES");
            }
            else {
                throw new RuntimeException("DIFFERENCES in PAYROLL COMPARISON. See difference file under :" + fullDiffFilePath);
            }
        }
        finally {
            if (baseConn != null) {
                //baseConn.commit();
                baseConn.close();
            }
        }
    }

    protected void processCompareToMostRecent(PayrollRegressionContext context) throws Exception {
        DBConnection baseConn = null;
        try {
            baseConn = getDBConnection(context.getBaseDsLogin(),
                                       context.getBaseDsPassword(),
                                       context.getBaseDsUrl(),
                                       context.getBaseDsDriver());

            String fullBaseFilePath = runPayroll(baseConn, context.getClientName(),
                       context.getPetId(),
                       context.getPaygroupIds(),
                       context.getStartDate(),
                       context.getEndDate(),
                       context.getBaseRecalculates(),
                       context.getPayrollOutputFilePath(),
                       context.getPayrollOutputFileName() ,
                       FILE_EXT_BASE);

            if (!FileUtil.fileExists(fullBaseFilePath)) {
                throw new RuntimeException(
                    "Base Payroll did not run successfully");
            }

            String fullCompareFilePath = context.getPayrollOutputFilePath()
                + context.getClientName()
                + File.separator + context.getPayrollOutputFileName() + "." + FILE_EXT_COMPARE;

            if (FileUtil.fileExists(fullCompareFilePath)) {
                String fullDiffFilePath = context.getPayrollOutputFilePath()
                    + context.getClientName()
                    + File.separator + context.getPayrollOutputFileName()
                    + "." + FILE_EXT_DIFF;

                boolean existsDiff = existsDiffWhenCompareTwoFilesTwoWay(
                    fullBaseFilePath,
                    fullCompareFilePath,
                    fullDiffFilePath);

                if (!existsDiff) {
                    if (logger.isDebugEnabled()) logger.debug("Payroll Comparison Successful with NO DIFFERENCES");
                }
                else {
                    throw new RuntimeException(
                        "DIFFERENCES in PAYROLL COMPARISON. See difference file under :" +
                        fullDiffFilePath);
                }
            }
            else {
                if (logger.isDebugEnabled()) logger.debug("No comparison performed, payroll ran successfully");
            }
            FileUtil.moveFile(fullBaseFilePath , fullCompareFilePath);
        }
        finally {
            if (baseConn != null) {
                //baseConn.commit();
                baseConn.close();
            }
        }
    }

    protected String runPayroll(DBConnection conn,
                              String clientName,
                              int petId,
                              int[] paygrpIds,
                              Date startDate,
                              Date endDate,
                              boolean recalcs,
                              String filePath,
                              String fileName,
                              String fileExt) throws Exception {

        stepMs = System.currentTimeMillis();
        if (recalcs) {
            if (logger.isDebugEnabled()) logger.debug("Started recalculating");
            RecalculateRecords recalc =
                new RecalculateRecords(conn);
            recalc.addEmployeeDate(null , null, null , paygrpIds,
                                   false, startDate , endDate);
            recalc.execute();
            if (logger.isDebugEnabled()) logger.debug("Recalculated " + recalc.getEmployeeDatesCount() + " employee dates");
            stepMs = meterTime("Recalculation" , stepMs);
        }
        if (logger.isDebugEnabled()) logger.debug("Started running payroll export");
        PayrollExportProcessor processor = new PayrollExportProcessor();

        processor.setPetId(petId);
        processor.setPayGrpIds(paygrpIds);
        if (startDate != null) {
            processor.setFromDate(startDate);
        }
        if (endDate != null) {
            processor.setToDate(endDate);
        }
        processor.setWriteToTable(false);
        processor.setWriteToFile(true);
        processor.setMergeFiles(true);
        processor.setAdjustDates(false);
        // petXML
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM payroll_export_tsk WHERE pet_id = ?";
            ps = conn.prepareStatement(sql);
            ps.setInt(1, petId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Clob clob = rs.getClob("pet_xml");
                String xml = clob.getSubString(1L, (int) clob.length());
                processor.setPetXml( xml );
            }
            else {
                throw new RuntimeException ("Payroll export task id not found : " + petId);
            }
        }
        finally {
            if (ps != null) ps.close();
            if (rs != null) rs.close();
        }

        processor.setPetPath(filePath);
        processor.setPetMask("'" + fileName + "'");
        processor.setPetExt(fileExt);
        processor.process(conn , null);

        stepMs = meterTime("Running Payroll Export" , stepMs);

        return filePath + clientName
            + File.separator + fileName + "." + fileExt;

    }

    private PayrollRegressionContext createContextFromPropFile(Properties props) throws ParseException{
        PayrollRegressionContext ret = new PayrollRegressionContext();
        ret.setCompareType(props.getProperty(PROP_COMPARE_TYPE));
        ret.setCompareToGivenFilePath(props.getProperty(PROP_COMPARE_TO_GIVEN_FILE_PATH));
        ret.setBaseDsLogin(props.getProperty(PROP_BASE_DS_LOGIN));
        ret.setBaseDsPassword(props.getProperty(PROP_BASE_DS_PASSWORD));
        ret.setBaseDsDriver(props.getProperty(PROP_BASE_DS_DRIVER));
        ret.setBaseDsUrl(props.getProperty(PROP_BASE_DS_URL));
        ret.setTargetDsLogin(props.getProperty(PROP_TARGET_DS_LOGIN));
        ret.setTargetDsPassword(props.getProperty(PROP_TARGET_DS_PASSWORD));
        ret.setTargetDsDriver(props.getProperty(PROP_TARGET_DS_DRIVER));
        ret.setTargetDsUrl(props.getProperty(PROP_TARGET_DS_URL));

        ret.setClientName(props.getProperty(PROP_CLIENT_NAME));

        ret.setBaseRecalculates(Boolean.valueOf(props.getProperty(PROP_BASE_RECALCULATES , "false")).booleanValue());
        ret.setTargetRecalculates(Boolean.valueOf(props.getProperty(PROP_TARGET_RECALCULATES , "false")).booleanValue());

        ret.setPayrollOutputFilePath(props.getProperty(PROP_PAYROLL_OUTPUT_FILE_PATH));
        ret.setPayrollOutputFileName(props.getProperty(PROP_PAYROLL_OUTPUT_FILE_NAME));

        String pgIds = props.getProperty(PROP_PAYGROUP_IDS);
        if (!StringHelper.isEmpty(pgIds)) {
            ret.setPaygroupIds(detokenizeStringAsIntArray(props.getProperty(PROP_PAYGROUP_IDS) , ","));
        }
        if (!StringHelper.isEmpty(props.getProperty(PROP_PET_ID))) {
            ret.setPetId(Integer.parseInt(props.getProperty(PROP_PET_ID)));
        }
        if (!StringHelper.isEmpty(props.getProperty(PROP_PAYROLL_START_DATE))) {
            ret.setStartDate(convertStringToDate(
            props.getProperty(PROP_PAYROLL_START_DATE), PROP_DATE_FORMAT));
        }
        if (!StringHelper.isEmpty(props.getProperty(PROP_PAYROLL_END_DATE))) {
            ret.setEndDate(convertStringToDate(
            props.getProperty(PROP_PAYROLL_END_DATE), PROP_DATE_FORMAT));
        }

        return ret;
    }

    /**
     * Compares two files two way
     * @param baseFile baseFile
     * @param targetFile targetFile
     * @param diffFilePath diffFilePath
     * @return boolean
     * @throws IOException
     */
    private boolean existsDiffWhenCompareTwoFilesTwoWay(
        String baseFile,
        String targetFile,
        String diffFilePath) throws IOException {

        if (FileUtil.areFilesIdentical(baseFile , targetFile)
            && FileUtil.areFilesIdentical(targetFile , baseFile)) {
            return false;
        }
        String temp1Path = diffFilePath + ".TEMP1";
        FileUtil.compareFilesWithOutputFile(baseFile,
                                            targetFile,
                                            temp1Path);
        String temp2Path = diffFilePath + ".TEMP2";
        FileUtil.compareFilesWithOutputFile(targetFile,
                                            baseFile,
                                            temp2Path);
        FileUtil.createFileWithDir(diffFilePath);
        if (FileUtil.fileExists(temp1Path)) {
            FileUtil.copyFile(temp1Path , diffFilePath, true);
            File temp1 = new File(temp1Path);
            temp1.delete();
        }
        if (FileUtil.fileExists(temp2Path)) {
            FileUtil.copyFile(temp2Path , diffFilePath, true);
            File temp2 = new File(temp2Path);
            temp2.delete();
        }
        return true;
    }

    private DBConnection getDBConnection(String user, String pwd,
                                         String url , String driver) throws Exception{
        System.setProperty("junit.db.username" , user);
        System.setProperty("junit.db.password" , pwd);
        System.setProperty("junit.db.url" , url);
        System.setProperty("junit.db.driver" , driver);

        final DBConnection c = com.workbrain.sql.SQLHelper.connectTo();
        c.setAutoCommit( false );
        com.workbrain.security.SecurityService.setCurrentClientId("1");
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

    private Date convertStringToDate(String str,
            String inputDateFormat) throws ParseException{
        DateFormat from = new SimpleDateFormat( inputDateFormat );
        return from.parse(str);
    }

    protected long meterTime(String what, long start){
        long l = System.currentTimeMillis();
        if (logger.isDebugEnabled()) logger.debug(what+" took: "+(l-start)+" millis");
        return l;
    }

    public static void main( String[] args ) throws Exception {
        /*
        // *** context can be initialized as below
        PayrollRegressionContext ctx = new PayrollRegressionContext();
        ctx.setCompareType(PROPVAL_COMPARE_TYPE_COMPARE_2_DBS);
        ctx.setBaseDsLogin("workbrain");
        ctx.setBaseDsPassword("workbrain");
        ctx.setBaseDsDriver("oracle.jdbc.driver.OracleDriver");
        ctx.setBaseDsUrl("jdbc:oracle:oci8:@ROWBSP4");
        ctx.setBaseRecalculates(false);
        ctx.setCompareToGivenFilePath("c:\\temp\\Sep2004\\DEFAULT\\TEST.TARGET");
        ctx.setTargetDsLogin("workbrain");
        ctx.setTargetDsPassword("workbrain");
        ctx.setTargetDsDriver("oracle.jdbc.driver.OracleDriver");
        ctx.setTargetDsUrl("jdbc:oracle:oci8:@ROWBSP4");
        ctx.setTargetRecalculates(false);
        ctx.setClientName("DEFAULT");
        ctx.setPaygroupIds(new int[] {0,1,2});
        ctx.setPetId(2);
        ctx.setStartDate(DateHelper.parseSQLDate("2004-09-29"));
        ctx.setEndDate(DateHelper.parseSQLDate("2004-09-29"));
        ctx.setPayrollOutputFileName("TEST");
        ctx.setPayrollOutputFilePath("c:\\temp\\Sep2004\\");

        PayrollRegression cmp = new PayrollRegression(ctx);
        cmp.execute();*/
        if (args.length == 0) {
            throw new RuntimeException("Regression Property file path must be supplied");
        }
        if (!FileUtil.fileExists(args[0])) {
            throw new RuntimeException("Regression Property file not found");
        }
        PayrollRegression preg = new PayrollRegression(args[0]);
        preg.execute();
    }

    public static class PayrollRegressionContext {
        private String compareType;
        private String baseDsLogin;
        private String baseDsPassword;
        private String baseDsDriver;
        private String baseDsUrl;
        private String targetDsLogin;
        private String targetDsPassword;
        private String targetDsDriver;
        private String targetDsUrl;
        private String compareToGivenFilePath;
        private boolean baseRecalculates;
        private boolean targetRecalculates;
        private String clientName;
        private String payrollOutputFilePath;
        private String payrollOutputFileName;
        private int[] paygroupIds;
        private int petId = Integer.MIN_VALUE ;
        private Date startDate;
        private Date endDate;

        public String getCompareType(){
            return compareType;
        }

        public void setCompareType(String v){
            compareType=v;
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

        public String getTargetDsLogin(){
            return targetDsLogin;
        }

        public void setTargetDsLogin(String v){
            targetDsLogin=v;
        }

        public String getTargetDsPassword(){
            return targetDsPassword;
        }

        public void setTargetDsPassword(String v){
            targetDsPassword=v;
        }

        public String getTargetDsDriver(){
            return targetDsDriver;
        }

        public void setTargetDsDriver(String v){
            targetDsDriver=v;
        }

        public String getTargetDsUrl(){
            return targetDsUrl;
        }

        public void setTargetDsUrl(String v){
            targetDsUrl=v;
        }

        public String getCompareToGivenFilePath(){
            return compareToGivenFilePath;
        }

        public void setCompareToGivenFilePath(String v){
            compareToGivenFilePath=v;
        }

        public boolean getBaseRecalculates(){
            return baseRecalculates;
        }

        public void setBaseRecalculates(boolean v){
            baseRecalculates=v;
        }

        public boolean getTargetRecalculates(){
            return targetRecalculates;
        }

        public void setTargetRecalculates(boolean v){
            targetRecalculates=v;
        }

        public String getClientName(){
            return clientName;
        }

        public void setClientName(String v){
            clientName=v;
        }

        public String getPayrollOutputFilePath(){
            return payrollOutputFilePath;
        }

        public void setPayrollOutputFilePath(String v){
            payrollOutputFilePath=v;
        }

        public String getPayrollOutputFileName(){
            return payrollOutputFileName;
        }

        public void setPayrollOutputFileName(String v){
            payrollOutputFileName=v;
        }

        public int[] getPaygroupIds(){
            return paygroupIds;
        }

        public void setPaygroupIds(int[] v){
            paygroupIds=v;
        }

        public int getPetId(){
            return petId;
        }

        public void setPetId(int v){
            petId=v;
        }

        public Date getStartDate(){
            return startDate;
        }

        public void setStartDate(Date v){
            startDate=v;
        }

        public Date getEndDate(){
            return endDate;
        }

        public void setEndDate(Date v){
            endDate=v;
        }

        public void validate() {
            if (PROPVAL_COMPARE_TYPE_COMPARE_2_DBS.equals(getCompareType())){
                asserCommon();
                assertNotEmpty(getTargetDsLogin() , "TargetDsLogin");
                assertNotEmpty(getTargetDsPassword() , "TargetDsPassword");
                assertNotEmpty(getTargetDsDriver() , "TargetDsDriver");
                assertNotEmpty(getTargetDsUrl() , "TargetDsUrl");
            }
            else if (PROPVAL_COMPARE_TYPE_COMPARE_TO_GIVEN_FILE.equals(getCompareType())){
                asserCommon();
                assertNotEmpty(getCompareToGivenFilePath() , "CompareToGivenFilePath");
            }
            else if (PROPVAL_COMPARE_TYPE_COMPARE_TO_MOST_RECENT.equals(getCompareType())){
                asserCommon();
            }
            else {
                throw new RuntimeException("Compare type not recognized, must be "
                    + PROPVAL_COMPARE_TYPE_COMPARE_2_DBS + " or "
                    + PROPVAL_COMPARE_TYPE_COMPARE_TO_GIVEN_FILE + " or "
                    + PROPVAL_COMPARE_TYPE_COMPARE_TO_MOST_RECENT);
            }
        }

        private void asserCommon() {
            assertNotEmpty(getBaseDsLogin(), "BaseDsLogin");
            assertNotEmpty(getBaseDsPassword(), "BaseDsPassword");
            assertNotEmpty(getBaseDsDriver(), "BaseDsDriver");
            assertNotEmpty(getBaseDsUrl(), "BaseDsUrl");
            assertNotEmpty(getPayrollOutputFilePath(), "PayrollOutputFilePath");
            assertNotEmpty(getPayrollOutputFileName(), "PayrollOutputFileName");
            assertNotEmpty(getPaygroupIds(), "PaygroupIds");
            assertNotEmpty(getPetId(), "BaseDsLogin");
            assertNotEmpty(getStartDate(), "StartDate");
            assertNotEmpty(getEndDate(), "EndDate");
            assertNotEmpty(getClientName() , "ClientName");
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


