package com.wbiag.app.wbinterface.hr2;


import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import com.wbiag.app.wbinterface.*;
import com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSCEmpSchool.ScEmpSchoolData;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

import junit.framework.*;

/**
 * Test class for HRRefreshTransactionSCEmpSchool
 * 
 * author:      Ali Ajellu
 * date:        Jan 11 2006
 * Copytright:  Workbrain Inc. (2006)
 */
public class HRRefreshTransactionSCEmpSchoolTest extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = 
        org.apache.log4j.Logger.getLogger(HRRefreshTransactionSCEmpSchool.class);
    DBConnection conn = null;
    CodeMapper cm = null;

    private int EMP_ID = -1;
    private final String OVR_START_DATE_COL       = "";
    private final String OVR_END_DATE_COL         = "";
    private String EMP_NAME_COL                   = "C";
    private String EMP_LASTNAME_COL               = "C";
    private String EMP_FIRSTNAME_COL              = "C";
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
    
    private final String SCHOOL1_NAME       = "DUMMY_SCHOOL_1";
    private final String SCHOOL2_NAME       = "DUMMY_SCHOOL_2";
    private final String SCHOOL3_NAME       = "DUMMY_SCHOOL_3";
    
    private int school1_id, school2_id, school3_id;
    
    private final String typName = "HR REFRESH";
    private final int typId = 1;
    
    IntegerList schoolsToDelete = new IntegerList();
    IntegerList empSchoolsToDelete = new IntegerList();

    protected void setUp() throws Exception {
        super.setUp();
        
        //create schools
        school1_id = createSchool(SCHOOL1_NAME, DateHelper.createDate(2005, 1,1), DateHelper.createDate(2007,1,1));
        school2_id = createSchool(SCHOOL2_NAME, DateHelper.createDate(2005, 1,1), DateHelper.createDate(2007,1,1));
        school3_id = createSchool(SCHOOL3_NAME, DateHelper.createDate(2005, 1,1), DateHelper.createDate(2007,1,1));
        if (logger.isDebugEnabled()){
            logger.debug("setting up: school1_id = " + school1_id);
        }
        
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        
        //get rid of ALL employee schools. 
        String empSchoolDelSQL = "DELETE FROM SC_EMP_SCHOOL WHERE SCESCH_ID LIKE '%'";
        String schoolDelSQL    = "DELETE FROM SC_SCHOOL WHERE SCSCH_ID LIKE '%'";
        PreparedStatement prepStmnt = null;
        
        try{
            prepStmnt = conn.prepareStatement(empSchoolDelSQL);
            prepStmnt.executeUpdate();
            prepStmnt = conn.prepareStatement(schoolDelSQL);
            prepStmnt.executeUpdate();
            conn.commit();
        }finally{
            SQLHelper.cleanUp(prepStmnt);
        }
    }
    
    /*[start]********************* TEST CASES ************************/
    
    public void test_EmptyList() throws Exception {

        updateWbintTypeClass(typId,
                "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSCEmpSchool");

        String path = createFile(createData());

        TransactionData trans = importCSVFile(  path, 
                                                new HRFileTransformer(),
                                                typName);

        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());
        
        List empSchools = HRRefreshTransactionSCEmpSchool.getExistingEmpSchools(EMP_ID, conn);
        assertTrue(empSchools.size() == 0);
    }
    
    public void test_1Item_NoExistingEmpSchools() throws Exception {

        updateWbintTypeClass(typId,
                "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSCEmpSchool");

        String path = createFile(createData(SCHOOL1_NAME));

        TransactionData trans = importCSVFile(  path, 
                                                new HRFileTransformer(),
                                                typName);

        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());
        
        //get all employee schools
        List empSchools = HRRefreshTransactionSCEmpSchool.getExistingEmpSchools(EMP_ID, conn);
        assertTrue(empSchools.size() == 1);
        
        ScEmpSchoolData empSchool = (ScEmpSchoolData)empSchools.get(0);
        assertTrue(empSchool.scsch_id == school1_id);
    }
    
    public void test_1Item_1ExistingEmpSchool() throws Exception {

        updateWbintTypeClass(typId,
                "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSCEmpSchool");

        String path = createFile(createData(SCHOOL1_NAME));

        TransactionData trans = importCSVFile(  path, 
                                                new HRFileTransformer(),
                                                typName);

        Date startDate = DateHelper.createDate(2005,1,1);
        Date endDate = DateHelper.createDate(2005,2,1);
        
        createEmpSchool(school1_id, startDate, endDate);
        
        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());
        
        //get all employee schools
        List empSchools = HRRefreshTransactionSCEmpSchool.getExistingEmpSchools(EMP_ID, conn);
        assertTrue(empSchools.size() == 1);
        
        ScEmpSchoolData empSchool = (ScEmpSchoolData)empSchools.get(0);
        assertTrue(empSchool.scsch_id == school1_id);
        
//        assertTrue(empSchool.scesch_attend_st.equals(nowSql));
//        assertTrue(empSchool.scesch_attend_end.equals(DateHelper.createDate(3000,0,1)));
    }
    
    public void test_1Item_ManyExistingEmpSchools() throws Exception {

        updateWbintTypeClass(typId,
                "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSCEmpSchool");

        String path = createFile(createData(SCHOOL1_NAME));

        TransactionData trans = importCSVFile(  path, 
                                                new HRFileTransformer(),
                                                typName);

        Date startDate = DateHelper.createDate(2005,1,1);
        Date endDate = DateHelper.createDate(2005,2,1);
        
        createEmpSchool(school1_id, startDate, endDate);
        createEmpSchool(school2_id, startDate, endDate);
        
        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());
        
        //get all employee schools
        List empSchools = HRRefreshTransactionSCEmpSchool.getExistingEmpSchools(EMP_ID, conn);
        assertTrue(empSchools.size() == 2);
    }
    
    public void test_1Item_1existing_absolute_deleteOriginals() throws Exception {

        updateWbintTypeClass(typId,
                "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSCEmpSchool");

        String path = createFile(createData("~~"+SCHOOL1_NAME));

        TransactionData trans = importCSVFile(  path, 
                                                new HRFileTransformer(),
                                                typName);

        Date startDate = DateHelper.createDate(2005,1,1);
        Date endDate = DateHelper.createDate(2005,2,1);
        
        createEmpSchool(school2_id, startDate, endDate);
        createEmpSchool(school3_id, startDate, endDate);
        
        
        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());
        
        //get all employee schools
        List empSchools = HRRefreshTransactionSCEmpSchool.getExistingEmpSchools(EMP_ID, conn);
        assertTrue(empSchools.size() == 1);
        
        ScEmpSchoolData empSchool = (ScEmpSchoolData)empSchools.get(0);
        assertTrue(empSchool.scsch_id == school1_id);

    } 
    
    public void test_1Item_1existing_absolute_keepOriginal() throws Exception {

        updateWbintTypeClass(typId,
                "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionSCEmpSchool");

        String path = createFile(createData("~~"+SCHOOL1_NAME));

        TransactionData trans = importCSVFile(  path, 
                                                new HRFileTransformer(),
                                                typName);

        Date startDate = DateHelper.createDate(2005,1,1);
        Date endDate = DateHelper.createDate(2005,2,1);
        
        createEmpSchool(school1_id, startDate, endDate);
        
        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());
        
        //get all employee schools
        List empSchools = HRRefreshTransactionSCEmpSchool.getExistingEmpSchools(EMP_ID, conn);
        assertTrue(empSchools.size() == 1);
        
        ScEmpSchoolData empSchool = (ScEmpSchoolData)empSchools.get(0);
        assertTrue(empSchool.scsch_id == school1_id);
    } 
    
    /********************** TEST CASES [end]************************[end]*/
    
    public HRRefreshTransactionSCEmpSchoolTest(String testName) throws Exception {
        super(testName);
        conn = getConnection();
        cm = CodeMapper.createBrandNewCodeMapper(conn);
        EmployeeAccess empAcc = new EmployeeAccess(conn, cm);
        EMP_ID = empAcc.loadByName(EMP_NAME_COL, new java.util.Date()).getEmpId();
        tearDown();
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HRRefreshTransactionSCEmpSchoolTest.class);
        return result;
    }

    private String createData(){
        return createData("");
    }
    private String createData(String schools){

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
        stm += ",";  // 90
        stm += "," + schools;

        return stm;
    }

    

    protected int createSchool(String schoolName, Date startDate, Date endDate) throws SQLException{
        String schoolDesc = schoolName;
        
        DBSequence schoolSeq = conn.getDBSequence("SEQ_SCSCH_ID");
        int newSchoolId = schoolSeq.getNextValue();
        
        String insertSchoolSQL = "INSERT INTO SC_SCHOOL VALUES " + 
                                 "( " + newSchoolId + ", '" + schoolName + "', '" + schoolDesc + "', " +
                                 conn.encodeDate(startDate) + ", " + conn.encodeDate(endDate) + ")";
        
        PreparedStatement insertSchoolStmnt = null;
        
        try{
            insertSchoolStmnt = conn.prepareStatement(insertSchoolSQL);
            insertSchoolStmnt.executeUpdate();
            conn.commit();
        }finally{
            SQLHelper.cleanUp(insertSchoolStmnt);
        }
        
        schoolsToDelete.add(newSchoolId);
        return newSchoolId;
    }
    
    protected int createEmpSchool(int schoolId, Date attendStartDate, Date attendEndDate) throws SQLException{
               
        DBSequence empSchoolSeq = conn.getDBSequence("SEQ_SCESCH_ID");
        int newEmpSchoolId = empSchoolSeq.getNextValue();
        
        String insertSchoolSQL = "INSERT INTO SC_EMP_SCHOOL VALUES " + 
                                 "( " + newEmpSchoolId + ", " + EMP_ID + ", " + schoolId + ", " +
                                 conn.encodeDate(attendStartDate) + ", " + conn.encodeDate(attendEndDate) + " )";
        
        PreparedStatement insertEmpSchoolStmnt = null;
        
        try{
            insertEmpSchoolStmnt = conn.prepareStatement(insertSchoolSQL);
            insertEmpSchoolStmnt.executeUpdate();
            conn.commit();
        }finally{
            SQLHelper.cleanUp(insertEmpSchoolStmnt);
        }
        
        empSchoolsToDelete.add(newEmpSchoolId);
        return newEmpSchoolId;
    }
    
    protected void deleteEmpSchool(int empSchoolId) throws SQLException{
        deleteRecord("SC_EMP_SCHOOL", "SCESCH_ID", empSchoolId);
    }
    
    protected void deleteSchool(int schoolId) throws SQLException{
        deleteRecord("SC_SCHOOL", "SCSCH_ID", schoolId);
    }
    
    protected void deleteRecord(String tableName, String pkFieldName, int recordId) throws SQLException{
        String deletionSQL = "DELETE FROM " + tableName + " WHERE " + pkFieldName + " = " + recordId;
        
        PreparedStatement deleteStmnt = null;
        try{
            deleteStmnt = conn.prepareStatement(deletionSQL);
            deleteStmnt.executeUpdate();
            conn.commit();
        }finally{
            SQLHelper.cleanUp(deleteStmnt);
        }
    }
    
    protected java.util.Date getStrippedDate (java.util.Date date){
        date.setHours(0);
        date.setMinutes(0);
        date.setSeconds(0);
        return date;
    }

    
    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
