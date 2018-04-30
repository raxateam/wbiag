package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;

import com.wbiag.app.wbinterface.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Unit test for updating ST_EMP_SKILL table
 *
 */
public class HRRefreshTransactionSTEmpSkillTest extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionSTEmpSkillTest.class);

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
    private final String EMP_VAL1_COL             = "";
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
    private final String EMP_JOBS_COL             = "";
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
    private final String SKILL_CASHIERING = "CASHIERING";
    private final String SKILL_CUSTOMER_SERVICE = "CUSTOMER SERVICE";
    private final String ST_EMP_SKILL_COL         = SKILL_CASHIERING + "~0|" + SKILL_CUSTOMER_SERVICE + "~0";
    private final String ST_EMP_SKILL_COL_UPDATE = SKILL_CASHIERING + "~10|" + SKILL_CUSTOMER_SERVICE + "~10";
    private final String ST_EMP_SKILL_COL_ABSOLUTE = "~~" + SKILL_CUSTOMER_SERVICE + "~10";

    private final String typName = "HR REFRESH";
    private final int typId = 1;


    public HRRefreshTransactionSTEmpSkillTest(String testName) throws Exception {
        super(testName);
    }


    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HRRefreshTransactionSTEmpSkillTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testInsertEmpSkill() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSTEmpSkill");

        String path = createFile(createData());

        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        // *** validate data
        EmployeeAccess ea = new EmployeeAccess(getConnection() , getCodeMapper());
        EmployeeData ed = ea.loadByName(EMP_NAME_COL, DateHelper.getCurrentDate());
        assertNotNull(ed);

        List skills = new RecordAccess(getConnection()).loadRecordData(new StEmpSkillData() , HRRefreshTransactionSTEmpSkill.ST_EMP_SKILL_TABLE , "emp_id" , ed.getEmpId());
        assertEquals(2, skills.size());
        StEmpSkillData data = (StEmpSkillData )skills.get(0);
        assertEquals(0 , data.getStempsklWeight().intValue());
    }

    /**
     * @throws Exception
     */
    public void testUpdateEmpSkill() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSTEmpSkill");

        final String empName = "2015";
        final int empId = 14;
        String path = createFile(createUpdateData(empName));
        // *** delete all skills
        PreparedStatement ps = null;
        try {
            String sql = "DELETE FROM " + HRRefreshTransactionSTEmpSkill.ST_EMP_SKILL_TABLE + " WHERE emp_id = ?";
            ps = getConnection().prepareStatement(sql);
            ps.setInt(1, empId);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        // *** validate data
        EmployeeAccess ea = new EmployeeAccess(getConnection() , getCodeMapper());
        EmployeeData ed = ea.loadByName(EMP_NAME_COL, DateHelper.getCurrentDate());
        assertNotNull(ed);

        List skills = new RecordAccess(getConnection()).loadRecordData(new StEmpSkillData() ,
            HRRefreshTransactionSTEmpSkill.ST_EMP_SKILL_TABLE , "emp_id" , ed.getEmpId());
        assertEquals(2, skills.size());
        StEmpSkillData data = (StEmpSkillData )skills.get(0);
        assertEquals(10 , data.getStempsklWeight().intValue());

    }

    /**
     * Depends on testUpdateEmpSkill
     * @throws Exception
     */
    public void testAbsolueEmpSkill() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSTEmpSkill");

        final String empName = "2015";
        final int empId = 14;
        String path = createFile(createAbsoluteData(empName));
        // *** delete all skills

        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        // *** validate data
        EmployeeAccess ea = new EmployeeAccess(getConnection() , getCodeMapper());
        EmployeeData ed = ea.loadByName(EMP_NAME_COL, DateHelper.getCurrentDate());
        assertNotNull(ed);

        List skills = new RecordAccess(getConnection()).loadRecordData(new StEmpSkillData() ,
            HRRefreshTransactionSTEmpSkill.ST_EMP_SKILL_TABLE , "emp_id" , ed.getEmpId());
        assertEquals(1, skills.size());
        //StEmpSkillData data = (StEmpSkillData )skills.get(0);
        //assertEquals(DateHelper.getCurrentDate() , data.getStempsklEndDate());

    }


    private String createData(){

        final long index = System.currentTimeMillis();
        EMP_NAME_COL             = "HRTEST_" + index;
        EMP_LASTNAME_COL         = "HRTEST_" + index + "_LASTNAME";
        EMP_FIRSTNAME_COL        = "HRTEST_" + index + "_FIRSTNAME" ;

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
        stm += "," + ST_EMP_SKILL_COL ;

        return stm;
    }

    private String createUpdateData(String empName){

        EMP_NAME_COL             = empName;

        String stm = OVR_START_DATE_COL;
        stm += "," + OVR_END_DATE_COL;
        stm += "," + EMP_NAME_COL;
        stm += "," + "";
        stm += "," + "";
        for (int i = 0 ; i < 85 ; i++ ) {
            stm += ",";
        }
        stm += "," + ST_EMP_SKILL_COL_UPDATE ;
        return stm;
    }

    private String createAbsoluteData(String empName){

        EMP_NAME_COL             = empName;

        String stm = OVR_START_DATE_COL;
        stm += "," + OVR_END_DATE_COL;
        stm += "," + EMP_NAME_COL;
        stm += "," + "";
        stm += "," + "";
        for (int i = 0 ; i < 85 ; i++ ) {
            stm += ",";
        }
        stm += "," + ST_EMP_SKILL_COL_ABSOLUTE ;
        return stm;
    }

    protected void setUp() throws Exception {
        super.setUp();
        // *** set up SKILL data
        PreparedStatement ps = null;
        int skltypId = getConnection().getDBSequence("seq_stskltyp_id").getNextValue();
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO st_skill_type (stskltyp_id, stskltyp_name, stskltyp_desc) VALUES (?,?,?)");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1  , skltypId );
            ps.setString(2, "DUMMY");
            ps.setString(3, "DUMMY");
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO st_skill (stskl_id, stskl_name, stskl_desc, stskltyp_id) VALUES (?,?,?,?)");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1, getConnection().getDBSequence("seq_stskl_id").getNextValue());
            ps.setString(2, SKILL_CASHIERING);
            ps.setString(3, SKILL_CASHIERING);
            ps.setInt(4, skltypId);
            ps.addBatch();
            ps.setInt(1, getConnection().getDBSequence("seq_stskl_id").getNextValue());
            ps.setString(2, SKILL_CUSTOMER_SERVICE);
            ps.setString(3, SKILL_CUSTOMER_SERVICE);
            ps.setInt(4, skltypId);
            ps.addBatch();

            ps.executeBatch();
        }
        finally {
            if (ps != null)
                ps.close();
}

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // *** remove SKILL data
        updateWbintTypeClass(typId , "com.workbrain.app.wbinterface.hr2.HRRefreshTransaction");
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM st_emp_skill WHERE stskl_id in (SELECT stskl_id FROM st_skill WHERE stskl_name IN (?,?))");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setString(1  , SKILL_CASHIERING);
            ps.setString(2  , SKILL_CUSTOMER_SERVICE);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM st_skill WHERE stskl_name IN (?,?)");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setString(1  , SKILL_CASHIERING);
            ps.setString(2  , SKILL_CUSTOMER_SERVICE);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM st_skill_type WHERE stskltyp_name = ?");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setString(1  , "DUMMY");
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

        getConnection().commit();
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
