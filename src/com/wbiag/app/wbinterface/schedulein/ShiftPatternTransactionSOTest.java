package com.wbiag.app.wbinterface.schedulein;

import java.io.*;
import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.schedulein.*;
import com.workbrain.sql.*;
import com.workbrain.test.*;
import com.workbrain.util.*;
import junit.framework.*;
import com.workbrain.tool.security.*;
import com.workbrain.app.modules.retailSchedule.db.*;
import com.workbrain.app.modules.retailSchedule.model.*;
import com.wbiag.app.wbinterface.*;
/**
 * Unit test for ShiftPatternTransactionSOTests.
 */
public class ShiftPatternTransactionSOTest extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPatternTransactionSOTest.class);

    private final static String EMP_NAME = "2015";
    private final static String OVR_START_DATE = "";
    private final static String OVR_END_DATE = "";
    private final static String SHFTPAT_NAME = "TEST";
    private final static String SHFTPAT_DESC = "TEST";
    private final static String SHFTPAT_START_DATE = "";
    private final static String SHFTPAT_DAY_START_TIME = "";
    private final static String SHFTGRP_NAME = "";
    private final static String SHFTPAT_UDF1 = "UDF1";
    private final static String SHFTPAT_UDF2 = "";
    private final static String SHFTPAT_UDF3 = "";
    private final static String SHFTPAT_UDF4 = "";
    private final static String SHFTPAT_UDF5 = "";
    private final static String SHFTPAT_FLAGS = "";
    private final static String RESERVED = "";
    private final static String SCH_IDENTIFIER_1 = "1~1";
    private final static String SCH_START_END_TIME_1 = "1000~1800";
    private final static String SCH_BREAK_STRING_1 = "";
    private final static String SHFTPAT_LABOR_STRING_1 = "";
    private final static String SCH_IDENTIFIER_2 = "2~1";
    private final static String SCH_START_END_TIME_2 = "0800~1800";
    private final static String SCH_BREAK_STRING_2 = "";
    private final static String SHFTPAT_LABOR_STRING_2 = "";
    private final static String SCH_IDENTIFIER_3 = "3~1";
    private final static String SCH_START_END_TIME_3 = "0800~1800";
    private final static String SCH_BREAK_STRING_3 = "";
    private final static String SHFTPAT_LABOR_STRING_3 = "";
    private final static String SCH_IDENTIFIER_4 = "4~1";
    private final static String SCH_START_END_TIME_4 = "0800~1800";
    private final static String SCH_BREAK_STRING_4 = "";
    private final static String SHFTPAT_LABOR_STRING_4 = "";
    private final static String SCH_IDENTIFIER_5 = "5~1";
    private final static String SCH_START_END_TIME_5 = "0800~1800";
    private final static String SCH_BREAK_STRING_5 = "";
    private final static String SHFTPAT_LABOR_STRING_5 = "";
    private final static String SCH_IDENTIFIER_6 = "6~1";
    private final static String SCH_START_END_TIME_6 = "OFF";
    private final static String SCH_BREAK_STRING_6 = "";
    private final static String SHFTPAT_LABOR_STRING_6 = "";
    private final static String SCH_IDENTIFIER_7 = "7~1";
    private final static String SCH_START_END_TIME_7 = "OFF";
    private final static String SCH_BREAK_STRING_7 = "";
    private final static String SHFTPAT_LABOR_STRING_7 = "";

    final String typName = "SHIFT PATTERN IMPORT";
    final int typId = 100;


    public ShiftPatternTransactionSOTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ShiftPatternTransactionSOTest.class);
        return result;
    }

    /**
     * Test creating WorkDetailOverride.
     * @throws Exception
     */
    public void testUpdateSOEmp() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.schedulein.ShiftPatternTransactionSO");

        final int empId = 14;
        final int soFixPar = 1;
        String path = createFile(createData());

        // *** create soEmp if not there
        Employee emp = loadSOEmployee(empId);
        if (emp == null) {
            List l = new ArrayList();
            l.add(new com.workbrain.app.ta.db.EmployeeAccess(getConnection() ,
                getCodeMapper()).loadRawData(empId));
            new com.workbrain.app.modules.retailSchedule.db.
                EmployeeAccess(getConnection()).insertDefaultEmployee(l);
        }
        emp = loadSOEmployee(empId);
        assertNotNull(emp);

        TransactionData trans = importCSVFile(path ,
                                              new ShiftPatternTransformer() ,
                                              typName);

        HashMap param = new HashMap();
        param.put("CreatesEmployeeOverride", "Y");
        param.put(ShiftPatternTransactionSO.PARAM_FIXED_SHIFT, String.valueOf(soFixPar));
        runWBInterfaceTaskByTransactionId(param , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        // *** validate data
        ShiftPatternAccess spsa = new ShiftPatternAccess(getConnection());
        ShiftPatternData sps = getCodeMapper().getShiftPatternByName(SHFTPAT_NAME);
        assertNotNull(sps);

        emp = loadSOEmployee(empId);
        assertEquals(soFixPar , emp.getSempOnfixedSkd());
    }

    private Employee loadSOEmployee(int empId) {
        List list = new com.workbrain.app.modules.retailSchedule.db.EmployeeAccess(getConnection()).
            loadRecordData( new Employee(),Employee.TABLE_NAME, "emp_id" , empId);
        if (list.size() > 0) {
            return (Employee) list.get(0);
        } else {
            return null;
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        updateWbintTypeClass(typId , "com.workbrain.app.wbinterface.schedulein.ShiftPatternTransaction");
        getConnection().commit();
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    private String createData(){

        String stm = EMP_NAME;
        stm += "," + OVR_START_DATE ;
        stm += "," + OVR_END_DATE ;
        stm += "," + SHFTPAT_NAME ;
        stm += "," + SHFTPAT_DESC ;
        stm += "," + SHFTPAT_START_DATE ;
        stm += "," + SHFTPAT_DAY_START_TIME ;
        stm += "," + SHFTGRP_NAME ;
        stm += "," + SHFTPAT_UDF1 ;
        stm += "," + SHFTPAT_UDF2 ;
        stm += "," + SHFTPAT_UDF3 ;
        stm += "," + SHFTPAT_UDF4 ;
        stm += "," + SHFTPAT_UDF5 ;
        stm += "," + SHFTPAT_FLAGS ;
        stm += "," + RESERVED ;
        stm += "," + SCH_IDENTIFIER_1 ;
        stm += "," + SCH_START_END_TIME_1 ;
        stm += "," + SCH_BREAK_STRING_1 ;
        stm += "," + SHFTPAT_LABOR_STRING_1 ;
        stm += "," + SCH_IDENTIFIER_2 ;
        stm += "," + SCH_START_END_TIME_2 ;
        stm += "," + SCH_BREAK_STRING_2 ;
        stm += "," + SHFTPAT_LABOR_STRING_2 ;
        stm += "," + SCH_IDENTIFIER_3 ;
        stm += "," + SCH_START_END_TIME_3 ;
        stm += "," + SCH_BREAK_STRING_3 ;
        stm += "," + SHFTPAT_LABOR_STRING_3 ;
        stm += "," + SCH_IDENTIFIER_4 ;
        stm += "," + SCH_START_END_TIME_4 ;
        stm += "," + SCH_BREAK_STRING_4 ;
        stm += "," + SHFTPAT_LABOR_STRING_4 ;
        stm += "," + SCH_IDENTIFIER_5 ;
        stm += "," + SCH_START_END_TIME_5 ;
        stm += "," + SCH_BREAK_STRING_5 ;
        stm += "," + SHFTPAT_LABOR_STRING_5 ;
        stm += "," + SCH_IDENTIFIER_6 ;
        stm += "," + SCH_START_END_TIME_6 ;
        stm += "," + SCH_BREAK_STRING_6 ;
        stm += "," + SHFTPAT_LABOR_STRING_6 ;
        stm += "," + SCH_IDENTIFIER_7 ;
        stm += "," + SCH_START_END_TIME_7 ;
        stm += "," + SCH_BREAK_STRING_7 ;
        stm += "," + SHFTPAT_LABOR_STRING_7 ;

        return stm;
    }



}
