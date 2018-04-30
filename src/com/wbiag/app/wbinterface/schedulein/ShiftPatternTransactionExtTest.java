package com.wbiag.app.wbinterface.schedulein;

import java.io.*;
import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.wbiag.app.wbinterface.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.schedulein.*;
import com.workbrain.sql.*;
import com.workbrain.test.*;
import com.workbrain.util.*;
import com.workbrain.tool.overrides.*;
import junit.framework.*;
import com.workbrain.tool.security.*;
/**
 * Unit test for ShiftPatternTransactionExtTests.
 */
public class ShiftPatternTransactionExtTest extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPatternTransactionExtTest.class);

    private final static String EMP_NAME = "2010";
    private final static String OVR_START_DATE = "";
    private final static String OVR_END_DATE = "";
    private final static String SHFTPAT_NAME = "";
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
    private final static String SCH_IDENTIFIER_8 = "8~1";
    private final static String SCH_START_END_TIME_8 = "1000~1800";
    private final static String SCH_BREAK_STRING_8 = "";
    private final static String SHFTPAT_LABOR_STRING_8 = "";
    private final static String SCH_IDENTIFIER_9 = "9~1";
    private final static String SCH_START_END_TIME_9 = "0800~1800";
    private final static String SCH_BREAK_STRING_9 = "";
    private final static String SHFTPAT_LABOR_STRING_9 = "";
    private final static String SCH_IDENTIFIER_10 = "10~1";
    private final static String SCH_START_END_TIME_10 = "0800~1800";
    private final static String SCH_BREAK_STRING_10 = "";
    private final static String SHFTPAT_LABOR_STRING_10 = "";
    private final static String SCH_IDENTIFIER_11 = "11~1";
    private final static String SCH_START_END_TIME_11 = "0800~1800";
    private final static String SCH_BREAK_STRING_11 = "";
    private final static String SHFTPAT_LABOR_STRING_11 = "";
    private final static String SCH_IDENTIFIER_12 = "12~1";
    private final static String SCH_START_END_TIME_12 = "0800~1800";
    private final static String SCH_BREAK_STRING_12 = "";
    private final static String SHFTPAT_LABOR_STRING_12 = "";
    private final static String SCH_IDENTIFIER_13 = "13~1";
    private final static String SCH_START_END_TIME_13 = "OFF";
    private final static String SCH_BREAK_STRING_13 = "";
    private final static String SHFTPAT_LABOR_STRING_13 = "";
    private final static String SCH_IDENTIFIER_14 = "14~1";
    private final static String SCH_START_END_TIME_14 = "OFF";
    private final static String SCH_BREAK_STRING_14 = "";
    private final static String SHFTPAT_LABOR_STRING_14 = "";

    final String typName = "SHIFT PATTERN IMPORT";
    final int typId = 100;

    private final String TIME_FMT = "HHmm";

    public ShiftPatternTransactionExtTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ShiftPatternTransactionExtTest.class);
        return result;
    }

    /**
     * Test creating WorkDetailOverride.
     * @throws Exception
     */
    public void testShiftPattern() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.schedulein.ShiftPatternTransactionExt");
        String path = createFile(createData());
        final int empId = 9;

        try {
            // *** cleanup
            cancelEmpOverrides(empId);
            TransactionData trans = importCSVFile(path,
                                                  new ShiftPatternTransformer(),
                                                  typName);

            HashMap param = new HashMap();
            param.put("CreatesEmployeeOverride", "Y");
            runWBInterfaceTaskByTransactionId(param, trans.getWbitranId());
            assertTransactionSuccess(trans.getWbitranId());

            Date start = DateHelper.nextDay(DateHelper.addDays(DateHelper.
                getCurrentDate(), 7), "Mon");
            EmployeeScheduleAccess esa = new EmployeeScheduleAccess(
                getConnection(), getCodeMapper());
            EmployeeData ed = new EmployeeAccess(getConnection(), getCodeMapper()).
                loadByName(EMP_NAME, start);
            EmployeeScheduleData esd = esa.load(ed, start);
            Date st1 = DateHelper.convertStringToDate(
                SCH_START_END_TIME_1.substring(0,
                                               SCH_START_END_TIME_1.indexOf("~")),
                TIME_FMT);
            Date end1 = DateHelper.convertStringToDate(
                SCH_START_END_TIME_1.substring(SCH_START_END_TIME_1.indexOf("~") +
                                               1),
                TIME_FMT);
            assertEquals(DateHelper.getDayFraction(st1),
                         DateHelper.getDayFraction(esd.getEmpskdActStartTime()));
            assertEquals(DateHelper.getDayFraction(end1),
                         DateHelper.getDayFraction(esd.getEmpskdActEndTime()));
        }
        finally {
            // *** cleanup
            cancelEmpOverrides(empId);
        }
    }

    private void cancelEmpOverrides(int empId) throws Exception{
        OverrideAccess oa = new OverrideAccess(getConnection());
        OverrideList ol = oa.loadAffectingOverrides(empId, DateHelper.getCurrentDate() ,
                                  OverrideData.EMPLOYEE_TYPE_START,
                                  OverrideData.EMPLOYEE_TYPE_END);
        OverrideBuilder ob = new OverrideBuilder(getConnection());
        Iterator iter = ol.iterator();
        while (iter.hasNext()) {
            OverrideData item = (OverrideData)iter.next();
            DeleteOverride dov = new DeleteOverride();
            dov.setOverrideId(item.getOvrId());
            ob.add(dov);
            ob.execute(true, true);
            //oa.deleteRecordData(OverrideAccess.OVERRIDE_TABLE, OverrideAccess.OVERRIDE_PRI_KEY, item.getOvrId());
        }
        //getConnection().commit();
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        updateWbintTypeClass(typId,
                             "com.workbrain.app.wbinterface.schedulein.ShiftPatternTransaction");
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
        stm += "," + "*" ;
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

        String stm2 = EMP_NAME;
        stm2 += "," + OVR_START_DATE ;
        stm2 += "," + OVR_END_DATE ;
        stm2 += "," + SHFTPAT_NAME ;
        stm2 += "," + SHFTPAT_DESC ;
        stm2 += "," + SHFTPAT_START_DATE ;
        stm2 += "," + SHFTPAT_DAY_START_TIME ;
        stm2 += "," + SHFTGRP_NAME ;
        stm2 += "," + SHFTPAT_UDF1 ;
        stm2 += "," + SHFTPAT_UDF2 ;
        stm2 += "," + SHFTPAT_UDF3 ;
        stm2 += "," + SHFTPAT_UDF4 ;
        stm2 += "," + SHFTPAT_UDF5 ;
        stm2 += "," + SHFTPAT_FLAGS ;
        stm2 += "," + "" ;
        stm2 += "," + SCH_IDENTIFIER_8 ;
        stm2 += "," + SCH_START_END_TIME_8 ;
        stm2 += "," + SCH_BREAK_STRING_8;
        stm2 += "," + SHFTPAT_LABOR_STRING_8 ;
        stm2 += "," + SCH_IDENTIFIER_9 ;
        stm2 += "," + SCH_START_END_TIME_9 ;
        stm2 += "," + SCH_BREAK_STRING_9 ;
        stm2 += "," + SHFTPAT_LABOR_STRING_9 ;
        stm2 += "," + SCH_IDENTIFIER_10 ;
        stm2 += "," + SCH_START_END_TIME_10 ;
        stm2 += "," + SCH_BREAK_STRING_10 ;
        stm2 += "," + SHFTPAT_LABOR_STRING_10 ;
        stm2 += "," + SCH_IDENTIFIER_11 ;
        stm2 += "," + SCH_START_END_TIME_11 ;
        stm2 += "," + SCH_BREAK_STRING_11 ;
        stm2 += "," + SHFTPAT_LABOR_STRING_11 ;
        stm2 += "," + SCH_IDENTIFIER_12 ;
        stm2 += "," + SCH_START_END_TIME_12 ;
        stm2 += "," + SCH_BREAK_STRING_12 ;
        stm2 += "," + SHFTPAT_LABOR_STRING_12 ;
        stm2 += "," + SCH_IDENTIFIER_13 ;
        stm2 += "," + SCH_START_END_TIME_13 ;
        stm2 += "," + SCH_BREAK_STRING_13 ;
        stm2 += "," + SHFTPAT_LABOR_STRING_13 ;
        stm2 += "," + SCH_IDENTIFIER_14 ;
        stm2 += "," + SCH_START_END_TIME_14 ;
        stm2 += "," + SCH_BREAK_STRING_14 ;
        stm2 += "," + SHFTPAT_LABOR_STRING_14 ;

        return stm + "\n" + stm2;
    }
}
