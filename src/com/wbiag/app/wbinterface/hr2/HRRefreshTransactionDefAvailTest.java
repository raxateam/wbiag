package com.wbiag.app.wbinterface.hr2;

import java.io.*;
import java.util.*;

import java.sql.*;

import com.workbrain.app.modules.*;
import com.workbrain.app.modules.retailSchedule.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.wbiag.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.sql.*;
import com.workbrain.test.*;
import com.workbrain.util.*;
import junit.framework.*;
import com.workbrain.app.modules.availability.model.*;
import com.workbrain.app.modules.availability.db.*;
/**
 * Unit test for updating default AMX_AVAILABILITY
 *
 */
public class HRRefreshTransactionDefAvailTest extends WBInterfaceCustomTestCase{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionDefAvailTest.class);

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

    private final String typName = "HR REFRESH";
    private final int typId = 1;


    public HRRefreshTransactionDefAvailTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HRRefreshTransactionDefAvailTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testInsertEmp() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionDefAvail");
        TestUtil.getInstance().setVarTemp("/" + HRRefreshTransactionDefAvail.REG_AMX_LEN , "7");
        TestUtil.getInstance().setVarTemp("/" + HRRefreshTransactionDefAvail.REG_AMX_AMX_DEF_AVL ,
                                          HRRefreshTransactionDefAvail.REG_VAL_ALL_ON);

        String path = createFile(createData(false));

        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        // *** validate data
        DBConnection c = getConnection();
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        EmployeeAccess ea = new EmployeeAccess(c,cm);
        EmployeeData ed = ea.loadByName(EMP_NAME_COL, DateHelper.getCurrentDate());
        assertNotNull(ed);

        AvailabilityData data = getAvailabilityData(getConnection() , ed.getEmpId());
        assertNotNull(data);
        assertEquals("Start time 1", data.DEF_START_DATE , data.getAmxavStime1());
    }

    /**
     * @throws Exception
     */
    public void testUpdateEmp() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionDefAvail");
        TestUtil.getInstance().setVarTemp("/" + HRRefreshTransactionDefAvail.REG_AMX_LEN , "7");
        TestUtil.getInstance().setVarTemp("/" + HRRefreshTransactionDefAvail.REG_AMX_AMX_DEF_AVL ,
                                          "DAYS=1,10,17~2,10,17~3,10,17~4,10,20~5,10,20~6,OFF,OFF~7,OFF,OFF");
        final String empName = "2015";
        final int empId = 14;
        String path = createFile(createUpdateData(empName));

        DBConnection c = getConnection();
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        // *** delete exising for test
        AvailabilityData existData = getAvailabilityData(getConnection() , empId);
        if (existData != null) {
            new AvailabilityAccess(c, cm).delete(existData);
        }
        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        // *** validate data

        EmployeeAccess ea = new EmployeeAccess(c,cm);
        EmployeeData ed = ea.loadByName(EMP_NAME_COL, DateHelper.getCurrentDate());
        assertNotNull(ed);

        AvailabilityData data = getAvailabilityData(getConnection() , ed.getEmpId());
        assertNotNull(data);
        assertEquals("Start 1", 600,
                     DateHelper.getDayFraction(data.getAmxavStime1()) / DateHelper.MINUTE_MILLISECODS);
        assertEquals("End 1", 1020,
                     DateHelper.getDayFraction(data.getAmxavEtime1())  / DateHelper.MINUTE_MILLISECODS);
        assertNull("Start 6", data.getAmxavStime6());
        assertNull("End 6", data.getAmxavEtime6());
    }


    /**
     * @throws Exception
     */
    public void testUpdateEmpError() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionDefAvail");
        TestUtil.getInstance().setVarTemp("/" + HRRefreshTransactionDefAvail.REG_AMX_LEN , "14");
        TestUtil.getInstance().setVarTemp("/" + HRRefreshTransactionDefAvail.REG_AMX_AMX_DEF_AVL ,
                                          "DAYS=1,10,17~2,10,17~3,10,17~4,10,20~5,10,20~6,OFF,OFF~7,OFF,OFF");
        final String empName = "2015";
        final int empId = 14;
        String path = createFile(createUpdateData(empName));

        DBConnection c = getConnection();
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        // *** delete exising for test
        AvailabilityData existData = getAvailabilityData(getConnection() , empId);
        if (existData != null) {
            new AvailabilityAccess(c, cm).delete(existData);
        }
        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionFailure(trans.getWbitranId());
    }

    private String createData(boolean addSODOData){

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
        return stm;
    }

    private AvailabilityData getAvailabilityData( DBConnection c, int empId) throws SQLException{
       List l = new RecordAccess(c).
           loadRecordData(new AvailabilityData() , AvailabilityData.EMPLOYEE_AVAILABILITY_TABLE, "emp_id", empId);
       if (l.size() > 0) {
           return (AvailabilityData)l.get(0);
       }
       return null;
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
