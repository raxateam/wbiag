package com.wbiag.app.wbinterface.hr2;

import java.util.*;

import com.workbrain.app.modules.retailSchedule.model.*;
import com.wbiag.app.wbinterface.*;
import com.wbiag.app.wbinterface.hr2.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import junit.framework.*;
import com.workbrain.test.*;
import com.workbrain.app.modules.*;
import java.sql.*;
/**
 * Unit test for sample transaction
 *
 */
public class WBIAGHRRefreshTransactionTest extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBIAGHRRefreshTransactionTest.class);

    private final long index = System.currentTimeMillis();
    private final String OVR_START_DATE_COL       = "";
    private final String OVR_END_DATE_COL         = "";
    private String EMP_NAME_COL              ;
    private String EMP_LASTNAME_COL         ;
    private String EMP_FIRSTNAME_COL        ;
    private final String EMP_DAY_START_TIME_COL   = "00:00";
    private final String SHFTPAT_ID_COL           = "ALL DAYS";
    private final String CALCGRP_ID_COL           = "MANAGEMENT" ;
    private final String EMP_BASE_RATE_COL        = "7.77";
    private final String PAYGRP_ID_COL            = "1";
    private final String EMP_HIRE_DATE_COL        = "01/01/1999";
    private final String EMP_SENIORITY_DATE_COL   = "02/02/1999";
    private final String EMP_BIRTH_DATE_COL       = "02/02/1969";
    private final String EMP_TERMINATION_DATE_COL = "01/01/3000";
    private final String EMP_STATUS_COL           = "A";
    private final String EMP_SIN_COL              = "098765432";
    private final String EMP_SHFTPAT_OFFSET_COL   = "0";
    private final String EMP_FLAG_COL             = "";
    private final String EMP_VAL1_COL             = "XXX";
    private final String EMP_VAL2_COL             = "";
    private final String EMP_VAL3_COL             = "";
    private final String EMP_VAL4_COL             = "";
    private final String EMP_VAL5_COL             = "";
    private final String EMP_VAL6_COL             = "";
    private final String EMP_VAL7_COL             = "";
    private final String EMP_VAL8_COL             = "";
    private final String EMP_VAL9_COL             = "";
    private final String EMP_VAL10_COL            = "";
    private final String EMP_VAL11_COL            = "";
    private final String EMP_VAL12_COL            = "";
    private final String EMP_VAL13_COL            = "";
    private final String EMP_VAL14_COL            = "";
    private final String EMP_VAL15_COL            = "";
    private final String EMP_VAL16_COL            = "";
    private final String EMP_VAL17_COL            = "";
    private final String EMP_VAL18_COL            = "";
    private final String EMP_VAL19_COL            = "";
    private final String EMP_VAL20_COL            = "";
    private final String EMP_DEF_MINUTES_COL      = "";
    private final String EMPBDG_BADGE_NUMBER_COL  = "";
    private final String EMP_UDF_DATA_COL1        = "";
    private final String EMP_UDF_DATA_COL2        = "";
    private final String EMP_UDF_DATA_COL3        = "";
    private final String EMP_UDF_DATA_COL4        = "";
    private final String EMP_UDF_DATA_COL5        = "";
    private final String EMP_UDF_DATA_COL6        = "";
    private final String EMP_UDF_DATA_COL7        = "";
    private final String EMP_UDF_DATA_COL8        = "";
    private final String EMP_UDF_DATA_COL9        = "";
    private final String EMP_UDF_DATA_COL10        = "";
    private final String EMP_UDF_DATA_COL11        = "";
    private final String EMP_UDF_DATA_COL12        = "";
    private final String EMP_UDF_DATA_COL13        = "";
    private final String EMP_UDF_DATA_COL14        = "";
    private final String EMP_UDF_DATA_COL15        = "";
    private final String EMP_UDF_DATA_COL16        = "";
    private final String EMP_UDF_DATA_COL17        = "";
    private final String EMP_UDF_DATA_COL18        = "";
    private final String EMP_UDF_DATA_COL19        = "";
    private final String EMP_UDF_DATA_COL20        = "";
    private final String EMP_DEF_LAB1_COL         = "";
    private final String EMP_DEF_LAB2_COL         = "";
    private final String EMP_DEF_LAB3_COL         = "";
    private final String EMP_DEF_LAB4_COL         = "";
    private final String EMP_JOBS_COL             = "JANITOR";
    private final String EMP_JOB_RATE_INDEX_COL   = "";
    private final String EMP_JOB_RANK_COL         = "";
    private final String EMPT_NAME_COL            = "";
    private final String ENTPOL_ID_COL            = "";
    private final String WBU_NAME_COL             = "";
    private final String WBU_PASSWORD_COL         = "";
    private final String WBU_EMAIL_COL            = "";
    private final String WBLL_ID_COL              = "";
    private final String WBG_ID_COL               = "";
    private final String WBU_PWD_CHANGED_DATE_COL = "";
    private final String EMBAL_DELTA_COL1         = "";
    private final String EMBAL_DELTA_COL2         = "";
    private final String EMBAL_DELTA_COL3         = "";
    private final String EMBAL_DELTA_COL4         = "";
    private final String EMBAL_DELTA_COL5         = "";
    private final String EMBAL_DELTA_COL6         = "";
    private final String EMBAL_DELTA_COL7         = "";
    private final String EMBAL_DELTA_COL8         = "";
    private final String EMBAL_DELTA_COL9         = "";
    private final String EMBAL_DELTA_COL10         = "";
    private final String TS_USER_COL         = "1";

    char sep = HRRefreshTransaction.SEPARATOR_CHAR;
    char eq = HRRefreshTransaction.EQUALS_CHAR;

    private final int SKDGRP_ID = 1;
    private final int SEMP_SKDMAX_HRS = 1;
    private final double SEMP_DAYMAX_HRS = 1;
    private final String SEMP_EFF_DATE = "01/01/1900";
    private final int SEMP_MAXSHFTDAY = 2;
    private final int SEMP_ONFIXED_SKD = 1;
    private final String SEMP_X_IN_DATE = "01/01/1900";
    private final String SEMP_X_OUT_DATE = "01/01/2000";
    private final String PREFFERED_EMPJOB_NAME = EMP_JOBS_COL;

    private final String SOEMP_EXTRA_COL =
            HRRefreshTransactionSOExtra.SKDGRP_ID + eq + SKDGRP_ID + sep +
            HRRefreshTransactionSOExtra.SEMP_SKDMAX_HRS + eq + SEMP_SKDMAX_HRS + sep +
            HRRefreshTransactionSOExtra.SEMP_DAYMAX_HRS + eq + SEMP_DAYMAX_HRS + sep +
            HRRefreshTransactionSOExtra.SEMP_EFF_DATE + eq + SEMP_EFF_DATE + sep +
            HRRefreshTransactionSOExtra.SEMP_MAXSHFTDAY + eq + SEMP_MAXSHFTDAY + sep +
            HRRefreshTransactionSOExtra.SEMP_ONFIXED_SKD + eq + SEMP_ONFIXED_SKD + sep +
            HRRefreshTransactionSOExtra.SEMP_X_IN_DATE + eq + SEMP_X_IN_DATE + sep +
            HRRefreshTransactionSOExtra.SEMP_X_OUT_DATE + eq + SEMP_X_OUT_DATE  + sep +
            HRRefreshTransactionSOExtra.PREFERRED_EMPJOB_NAME + eq + PREFFERED_EMPJOB_NAME;

    private final String typName = "HR REFRESH";
    private final int typId = 1;


    public WBIAGHRRefreshTransactionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WBIAGHRRefreshTransactionTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testInsertMulti() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.WBIAGHRRefreshTransaction");
        TestUtil.getInstance().setVarTemp(ModuleHelper.REG_SCHOPT_ENABLED , "true");

        String path = createFile(createData());

        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        HashMap params = createDefaultHRTransactionParams();
        params.put(WBIAGHRRefreshTransaction.PARAM_WBIAG_EXTENSIONS ,
                  "HRRefreshTransactionTSUser~90 HRRefreshTransactionSOExtra~91");
        runWBInterfaceTaskByTransactionId(params , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        // *** validate data
        DBConnection c = getConnection();
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        EmployeeAccess ea = new EmployeeAccess(c,cm);
        EmployeeData ed = ea.loadByName(EMP_NAME_COL, DateHelper.getCurrentDate());
        assertNotNull(ed);
        // *** ts_user
        assertEquals("Timesheet flag", Integer.parseInt(TS_USER_COL) , getTSUserTimeSheetFlag(ed.getEmpId()));

        // *** SO_EMPLOYEE extra
        Employee soEmp = loadSOEmployee(getConnection() , ed.getEmpId());
        assertNotNull(soEmp);
        assertEquals(SEMP_SKDMAX_HRS ,  soEmp.getSempSkdmaxHrs() , 0);
        assertEquals(SEMP_DAYMAX_HRS ,  soEmp.getSempDaymaxHrs() , 0);
        assertEquals(DateHelper.parseDate(SEMP_X_IN_DATE , HRRefreshTransaction.HR_REFRESH_DATE_FMT) ,
                                          soEmp.getSempXInDate() );
        assertEquals(DateHelper.parseDate(SEMP_X_OUT_DATE , HRRefreshTransaction.HR_REFRESH_DATE_FMT) ,
                                       soEmp.getSempXOutDate() );
        assertEquals(SEMP_ONFIXED_SKD ,  soEmp.getSempOnfixedSkd() );

        List empjobs = new EmployeeJobAccess(getConnection()).loadByEmpId(soEmp.getEmpId());
        assertTrue(empjobs.size() == 1 );
        EmployeeJobData ejb = (EmployeeJobData)empjobs.get(0);
        assertEquals("Y", ejb.getEmpjobPreferred());

    }

    private Employee loadSOEmployee(DBConnection c,int empId) {
        List list = new com.workbrain.app.modules.retailSchedule.db.EmployeeAccess(c).
            loadRecordData( new Employee(),Employee.TABLE_NAME, "emp_id" , empId);
        if (list.size() > 0) {
            return (Employee) list.get(0);
        } else {
            return null;
        }
    }

    /**
     * @throws Exception
     */
    public void testHeaderFooter() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.WBIAGHRRefreshTransaction");

        String path = createFile(createHeaderData());

        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        HashMap params = createDefaultHRTransactionParams();
        params.put(WBIAGHRRefreshTransaction.PARAM_WBIAG_EXTENSIONS ,
                  "");
       params.put(WBIAGHRRefreshTransaction.PARAM_HEADER_PREFIX ,
                  "HEADER");
       params.put(WBIAGHRRefreshTransaction.PARAM_HEADER_CHECKSUM ,
                  "true");
        runWBInterfaceTaskByTransactionId(params , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());



    }

    /**
     * @throws Exception
     */
    public void testExtraMapping() throws Exception{

        final String xml =
            "<mapping>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"A\" />"
            + "  <output-row><output-field name=\"1\" value=\"THIS\"/></output-row>"
            + "</row-match>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"XXX,YYY,ZZZ\" operator=\"in\" />"
            + "  <output-row><output-field name=\"1\" value=\"TESTING_IN\"/></output-row>"
            + "</row-match>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"BB\"/>"
            + "  <output-row>"
            + "    <output-field name=\"1\" value=\"1\"/>"
            + "    <output-field name=\"2\" value=\"2\"/>"
            + "    <output-field name=\"3\" value=\"3\"/>"
            + "  </output-row>"
            + "</row-match>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"B\"/>"
            + "  <field-match fieldname=\"2\" matchvalue=\"WORKS\"/>"
            + "  <output-row><output-field name=\"1\" copyfield=\"2\"/></output-row>"
            + "</row-match>"
            //  catch-all
            + "<row-match>"
            + "  <output-row><output-field name=\"1\" value=\"NOT_FOUND\"/></output-row>"
            + "</row-match>"
            + "</mapping>";
        final String tempMapName = "TEST";

        int wbimap_id = getConnection().getDBSequence("seq_wbimap_id").getNextValue();
        String sql =
            "insert into wbint_mapping(wbimap_id, wbimap_name, wbimap_in_table, wbimap_out_table, wbimap_xml) values(?,?,?,?,?)";
        PreparedStatement ps = null;
        try {
            ps = getConnection().prepareStatement(sql);
            ps.setInt(1, wbimap_id);
            ps.setString(2, tempMapName);
            ps.setString(3, "WBINT_IMPORT");
            ps.setString(4, "WBINT_EXPORT");
            ps.setNull(5, Types.CLOB);
            ps.executeUpdate();

        }
        finally {
            if (ps != null)
                ps.close();
        }

       getConnection().updateClob(xml, "WBINT_MAPPING",
                                     "wbimap_xml",
                                     "wbimap_id",
                                     String.valueOf(wbimap_id));

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.WBIAGHRRefreshTransaction");

        String path = createFile(createData());

        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        HashMap params = createDefaultHRTransactionParams();
        params.put(WBIAGHRRefreshTransaction.PARAM_EXTRA_MAPPINGS ,"18,TEST");
        runWBInterfaceTaskByTransactionId(params , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());


        DBConnection c = getConnection();
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        EmployeeAccess ea = new EmployeeAccess(c,cm);
        EmployeeData ed = ea.loadByName(EMP_NAME_COL, DateHelper.getCurrentDate());
        assertNotNull(ed);
        // *** ts_user
        assertEquals("EmpVal1", "TESTING_IN" , ed.getEmpVal1());


    }

    private String createData(){

        final long index = System.currentTimeMillis();
        EMP_NAME_COL             = "HRTEST_" + index;
        EMP_LASTNAME_COL         = "HRTEST_" + + index + "_LASTNAME";
        EMP_FIRSTNAME_COL        = "HRTEST_" + + index + "_FIRSTNAME" ;

        String stm = OVR_START_DATE_COL;
        stm += "," + OVR_END_DATE_COL;
        stm += "," + EMP_NAME_COL;
        stm += "," + EMP_LASTNAME_COL;
        stm += "," + EMP_FIRSTNAME_COL;
        stm += "," + EMP_DAY_START_TIME_COL;
        stm += "," + SHFTPAT_ID_COL;
        stm += "," + CALCGRP_ID_COL;
        stm += "," + EMP_BASE_RATE_COL;
        stm += "," + PAYGRP_ID_COL;
        stm += "," + EMP_HIRE_DATE_COL;
        stm += "," + EMP_SENIORITY_DATE_COL;
        stm += "," + EMP_BIRTH_DATE_COL;
        stm += "," + EMP_TERMINATION_DATE_COL;
        stm += "," + EMP_STATUS_COL;
        stm += "," + EMP_SIN_COL;
        stm += "," + EMP_SHFTPAT_OFFSET_COL;
        stm += "," + EMP_FLAG_COL;
        stm += "," + EMP_VAL1_COL;
        stm += "," + EMP_VAL2_COL;
        stm += "," + EMP_VAL3_COL;
        stm += "," + EMP_VAL4_COL;
        stm += "," + EMP_VAL5_COL;
        stm += "," + EMP_VAL6_COL;
        stm += "," + EMP_VAL7_COL;
        stm += "," + EMP_VAL8_COL;
        stm += "," + EMP_VAL9_COL;
        stm += "," + EMP_VAL10_COL;
        stm += "," + EMP_VAL11_COL;
        stm += "," + EMP_VAL12_COL;
        stm += "," + EMP_VAL13_COL;
        stm += "," + EMP_VAL14_COL;
        stm += "," + EMP_VAL15_COL;
        stm += "," + EMP_VAL16_COL;
        stm += "," + EMP_VAL17_COL;
        stm += "," + EMP_VAL18_COL;
        stm += "," + EMP_VAL19_COL;
        stm += "," + EMP_VAL20_COL;
        stm += "," + EMP_DEF_MINUTES_COL;
        stm += "," + EMPBDG_BADGE_NUMBER_COL;
        stm += "," + EMP_UDF_DATA_COL1;
        stm += "," + EMP_UDF_DATA_COL2;
        stm += "," + EMP_UDF_DATA_COL3;
        stm += "," + EMP_UDF_DATA_COL4;
        stm += "," + EMP_UDF_DATA_COL5;
        stm += "," + EMP_UDF_DATA_COL6;
        stm += "," + EMP_UDF_DATA_COL7;
        stm += "," + EMP_UDF_DATA_COL8;
        stm += "," + EMP_UDF_DATA_COL9;
        stm += "," + EMP_UDF_DATA_COL10;
        stm += "," + EMP_UDF_DATA_COL11;
        stm += "," + EMP_UDF_DATA_COL12;
        stm += "," + EMP_UDF_DATA_COL13;
        stm += "," + EMP_UDF_DATA_COL14;
        stm += "," + EMP_UDF_DATA_COL15;
        stm += "," + EMP_UDF_DATA_COL16;
        stm += "," + EMP_UDF_DATA_COL17;
        stm += "," + EMP_UDF_DATA_COL18;
        stm += "," + EMP_UDF_DATA_COL19;
        stm += "," + EMP_UDF_DATA_COL20;
        stm += "," + EMP_DEF_LAB1_COL;
        stm += "," + EMP_DEF_LAB2_COL;
        stm += "," + EMP_DEF_LAB3_COL;
        stm += "," + EMP_DEF_LAB4_COL;
        stm += "," + EMP_JOBS_COL;
        stm += "," + EMP_JOB_RATE_INDEX_COL;
        stm += "," + EMP_JOB_RANK_COL;
        stm += "," + EMPT_NAME_COL;
        stm += "," + ENTPOL_ID_COL;
        stm += "," + WBU_NAME_COL;
        stm += "," + WBU_PASSWORD_COL;
        stm += "," + WBU_EMAIL_COL;
        stm += "," + WBLL_ID_COL;
        stm += "," + WBG_ID_COL;
        stm += "," + WBU_PWD_CHANGED_DATE_COL;
        stm += "," + EMBAL_DELTA_COL1;
        stm += "," + EMBAL_DELTA_COL2;
        stm += "," + EMBAL_DELTA_COL3;
        stm += "," + EMBAL_DELTA_COL4;
        stm += "," + EMBAL_DELTA_COL5;
        stm += "," + EMBAL_DELTA_COL6;
        stm += "," + EMBAL_DELTA_COL7;
        stm += "," + EMBAL_DELTA_COL8;
        stm += "," + EMBAL_DELTA_COL9;
        stm += "," + EMBAL_DELTA_COL10;
        stm += ",";  // 85
        stm += ",";
        stm += ",";
        stm += ",";
        stm += ",";
        stm += "," + TS_USER_COL;
        stm += "," + SOEMP_EXTRA_COL;

        return stm;
    }

    private String createHeaderData(){
        return "HEADER,0";
    }

    private int getTSUserTimeSheetFlag(int empId) throws SQLException{
        int ret = -1;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT tsu_timesheet_flag FROM ts_user WHERE emp_id = ? ";
            ps = getConnection().prepareStatement(sql);
            ps.setInt(1 , empId);
            rs = ps.executeQuery();
            if(rs.next()) {
                ret = rs.getInt(1);
            }
        }
        finally {
            if (rs != null)
                rs.close();
            if (ps != null)
                ps.close();
        }
        return ret;
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        updateWbintTypeClass(typId , "com.workbrain.app.wbinterface.hr2.HRRefreshTransaction");
        getConnection().commit();
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
