package com.wbiag.app.wbinterface;

import java.util.*;

import com.wbiag.app.wbinterface.*;
import com.wbiag.util.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.hr2.*;
import junit.framework.*;
/**
 *
 */
public class GenericDelimitedImportTaskTest extends WBInterfaceCustomTestCase{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GenericDelimitedImportTaskTest.class);

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


    public GenericDelimitedImportTaskTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(GenericDelimitedImportTaskTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testDelim() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.GenericDelimitedImportTask");

        String path = createFile(createData(false));
        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

    }

    /**
     * Override with crypt one
     * @param filePath
     * @param transformer
     * @param wbitypName
     * @return
     * @throws Exception
     */
    public TransactionData importCSVFile(String filePath ,
                              ImportTransformer transformer,
                              String wbitypName) throws Exception{

        GenericDelimitedImportTask task = new GenericDelimitedImportTask();
        task.setCheckForInterrupt(false);
        List trList = task.importFileOneClient(
                getConnection(),
                transformer,
                wbitypName,
                filePath,
                false,
                1,
                GenericDelimitedImportTask.PARAM_DEL_VAL_TAB,
				null
                );
        assertEquals("Must be 1 transaction" , 1 , trList.size() );
        TransactionData trData = (TransactionData)trList.get(0);
        assertTrue("Must be PENDING" , trData.isPending());
        List imports = getImportsByTransactionId(trData.getWbitranId() );
        ImportData data = (ImportData )imports.get(0);
        assertEquals(null , data.getField(0) );
        assertEquals(null , data.getField(1) );
        assertEquals(EMP_NAME_COL , data.getField(2) );
        return trData;
    }

    private String createData(boolean addSODOData){

        final long index = System.currentTimeMillis();
        EMP_NAME_COL             = "HRTEST_" + index;
        EMP_LASTNAME_COL         = "HRTEST_" + + index + "_LASTNAME";
        EMP_FIRSTNAME_COL        = "HRTEST_" + + index + "_FIRSTNAME" ;

        String stm = "\"" + OVR_START_DATE_COL + "\"";
        stm += "\t\"" + OVR_END_DATE_COL + "\"";
        stm += "\t" + EMP_NAME_COL;
        stm += "\t" + EMP_LASTNAME_COL;
        stm += "\t" + EMP_FIRSTNAME_COL;
        stm += "\t" + EMP_DAY_START_TIME_COL;
        stm += "\t" + SHFTPAT_ID_COL;
        stm += "\t" + CALCGRP_ID_COL;
        stm += "\t" + EMP_BASE_RATE_COL;
        stm += "\t" + PAYGRP_ID_COL;
        stm += "\t" + EMP_HIRE_DATE_COL;
        stm += "\t" + EMP_SENIORITY_DATE_COL;
        stm += "\t" + EMP_BIRTH_DATE_COL;
        stm += "\t" + EMP_TERMINATION_DATE_COL;
        stm += "\t" + EMP_STATUS_COL;
        stm += "\t" + EMP_SIN_COL;
        stm += "\t" + EMP_SHFTPAT_OFFSET_COL;
        stm += "\t" + EMP_FLAG_COL;
        stm += "\t" + EMP_VAL1_COL;
        stm += "\t" + EMP_VAL2_COL;
        stm += "\t" + EMP_VAL3_COL;
        stm += "\t" + EMP_VAL4_COL;
        stm += "\t" + EMP_VAL5_COL;
        stm += "\t" + EMP_VAL6_COL;
        stm += "\t" + EMP_VAL7_COL;
        stm += "\t" + EMP_VAL8_COL;
        stm += "\t" + EMP_VAL9_COL;
        stm += "\t" + EMP_VAL10_COL;
        stm += "\t" + EMP_VAL11_COL;
        stm += "\t" + EMP_VAL12_COL;
        stm += "\t" + EMP_VAL13_COL;
        stm += "\t" + EMP_VAL14_COL;
        stm += "\t" + EMP_VAL15_COL;
        stm += "\t" + EMP_VAL16_COL;
        stm += "\t" + EMP_VAL17_COL;
        stm += "\t" + EMP_VAL18_COL;
        stm += "\t" + EMP_VAL19_COL;
        stm += "\t" + EMP_VAL20_COL;
        stm += "\t" + EMP_DEF_MINUTES_COL;
        stm += "\t" + EMPBDG_BADGE_NUMBER_COL;
        stm += "\t" + EMP_UDF_DATA_COL1;
        stm += "\t" + EMP_UDF_DATA_COL2;
        stm += "\t" + EMP_UDF_DATA_COL3;
        stm += "\t" + EMP_UDF_DATA_COL4;
        stm += "\t" + EMP_UDF_DATA_COL5;
        stm += "\t" + EMP_UDF_DATA_COL6;
        stm += "\t" + EMP_UDF_DATA_COL7;
        stm += "\t" + EMP_UDF_DATA_COL8;
        stm += "\t" + EMP_UDF_DATA_COL9;
        stm += "\t" + EMP_UDF_DATA_COL10;
        stm += "\t" + EMP_UDF_DATA_COL11;
        stm += "\t" + EMP_UDF_DATA_COL12;
        stm += "\t" + EMP_UDF_DATA_COL13;
        stm += "\t" + EMP_UDF_DATA_COL14;
        stm += "\t" + EMP_UDF_DATA_COL15;
        stm += "\t" + EMP_UDF_DATA_COL16;
        stm += "\t" + EMP_UDF_DATA_COL17;
        stm += "\t" + EMP_UDF_DATA_COL18;
        stm += "\t" + EMP_UDF_DATA_COL19;
        stm += "\t" + EMP_UDF_DATA_COL20;
        stm += "\t" + EMP_DEF_LAB1_COL;
        stm += "\t" + EMP_DEF_LAB2_COL;
        stm += "\t" + EMP_DEF_LAB3_COL;
        stm += "\t" + EMP_DEF_LAB4_COL;
        stm += "\t" + EMP_JOBS_COL;
        stm += "\t" + EMP_JOB_RATE_INDEX_COL;
        stm += "\t" + EMP_JOB_RANK_COL;
        stm += "\t" + EMPT_NAME_COL;
        stm += "\t" + ENTPOL_ID_COL;
        stm += "\t" + WBU_NAME_COL;
        stm += "\t" + WBU_PASSWORD_COL;
        stm += "\t" + WBU_EMAIL_COL;
        stm += "\t" + WBLL_ID_COL;
        stm += "\t" + WBG_ID_COL;
        stm += "\t" + WBU_PWD_CHANGED_DATE_COL;
        stm += "\t" + EMBAL_DELTA_COL1;
        stm += "\t" + EMBAL_DELTA_COL2;
        stm += "\t" + EMBAL_DELTA_COL3;
        stm += "\t" + EMBAL_DELTA_COL4;
        stm += "\t" + EMBAL_DELTA_COL5;
        stm += "\t" + EMBAL_DELTA_COL6;
        stm += "\t" + EMBAL_DELTA_COL7;
        stm += "\t" + EMBAL_DELTA_COL8;
        stm += "\t" + EMBAL_DELTA_COL9;
        stm += "\t" + EMBAL_DELTA_COL10;

        return stm;
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
