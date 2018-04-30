package com.wbiag.app.wbinterface.labor;

import java.io.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.wbinterface.*;
import com.wbiag.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.labor.*;
import com.workbrain.sql.*;
import com.workbrain.test.*;
import com.workbrain.util.*;
import junit.framework.*;
import com.workbrain.tool.security.*;
/**
 * Unit test for JobImportTransactionSOExtraTests.
 */
public class JobImportTransactionSOExtraTest extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(JobImportTransactionSOExtraTest.class);

    public final static String TYPE_J = JobImportTransactionSOExtra.TYPE_JOB;
    public final static String JOB_NAME = "TEST_JOB_" + System.currentTimeMillis();
    public final static String JOB_DESC = "TEST_JOB_" + System.currentTimeMillis();
    public final static String JOB_START_DATE = "19000101";
    public final static String JOB_END_DATE = "30000101";
    public final static String JOB_FLAGS = "";
    public final static String JOB_REFKEY1 = "";
    public final static String JOB_REFKEY2 = "";
    public final static String JOB_REFKEY3 = "";
    public final static String JOB_REFKEY4 = "";
    public final static String JOB_REFKEY5 = "";
    public final static String JOB_VAL1 = "";
    public final static String JOB_VAL2 = "";
    public final static String JOB_VAL3 = "";
    public final static String JOB_VAL4 = "";
    public final static String JOB_VAL5 = "";
    public final static String JOB_VAL6 = "";
    public final static String JOB_VAL7 = "";
    public final static String JOB_VAL8 = "";
    public final static String JOB_VAL9 = "";
    public final static String JOB_VAL10 = "";

    public final static String TYPE_R = JobImportTransactionSOExtra.TYPE_RATE;
    public final static String JOB_RATE_JOB_NAME = JOB_NAME;
    public final static String JOB_RATE_INDEX = "1";
    public final static String JOB_RATE_RATE = "20";
    public final static String JOB_RATE_EFF_DATE = "19000101";
    public final static String JOB_RATE_FLAGS = "";
    public final static String JOB_RATE_VAL1 = "";
    public final static String JOB_RATE_VAL2 = "";
    public final static String JOB_RATE_VAL3 = "";
    public final static String JOB_RATE_VAL4 = "";
    public final static String JOB_RATE_VAL5 = "";

    public final static String TYPE_T = JobImportTransactionSOExtra.TYPE_TEAM;
    public final static String JOBTEAM_JOB_NAME = JOB_NAME;
    public final static String JOBTEAM_WBT_NAME = "OFFICE";
    public final static String JOBTEAM_DOT_THRESH = "3";
    public final static String JOBTEAM_WOT_THRESH = "4";
    public final static String JOBTEAM_AV_HR_RATE = "5";
    public final static String JOBTEAM_OT_MULT = "6";
    public final static String JOBTEAM_OT_INC = "7";

    public JobImportTransactionSOExtraTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(JobImportTransactionSOExtraTest.class);
        return result;
    }

    /**
     * Tests insert job and rate
     * @throws Exception
     */
    public void testInsertJobAndRate() throws Exception{

        final String typName = "JOB IMPORT";
        final int typId = 90;

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.labor.JobImportTransactionSOExtra");

        String path = createFile(createData());

        TransactionData trans = importCSVFile(path ,
                                              new JobImportTransformer() ,
                                              typName);

        HashMap param = new HashMap();
        runWBInterfaceTaskByTransactionId(param , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        CodeMapper cm = CodeMapper.createCodeMapper(getConnection());
        // *** validate job data
        JobData job = cm.getJobByName(JOB_NAME);
        assertNotNull(job);
        assertEquals(JOB_DESC , job.getJobDesc());

        JobRateData jrd = cm.getJobRate(job.getJobId() ,
            Integer.parseInt(JOB_RATE_INDEX) ,
            DateHelper.convertStringToDate(JOB_RATE_EFF_DATE , JobImportTransactionSOExtra.DATE_FMT)) ;
        assertNotNull(jrd);
        assertEquals(JOB_RATE_RATE , String.valueOf(jrd.getJobrateRate()) );

        JobImportTransactionSOExtra.JobTeamData jtd = JobImportTransactionSOExtra.loadJobTeamDataByJobId(
            getConnection(),
            job.getJobId(),
            cm.getWBTeamByName(JOBTEAM_WBT_NAME).getWbtId());
        assertNotNull(jtd);
        assertEquals(Double.parseDouble(JOBTEAM_AV_HR_RATE) , jtd.getJobteamAvHrRate() , 0);
        assertEquals(Double.parseDouble(JOBTEAM_DOT_THRESH) , jtd.getJobteamDotThresh() ,0);
        assertEquals(Double.parseDouble(JOBTEAM_OT_INC) , jtd.getJobteamOtInc() ,0);
        assertEquals(Double.parseDouble(JOBTEAM_OT_MULT) , jtd.getJobteamOtMult() ,0);
        assertEquals(Double.parseDouble(JOBTEAM_WOT_THRESH) , jtd.getJobteamWotThresh() ,0);
    }


    private String createData(){

        String job = TYPE_J;
        job += "," + JOB_NAME;
        job += "," + JOB_DESC;
        job += "," + JOB_START_DATE;
        job += "," + JOB_END_DATE;
        String jobRate = TYPE_R;
        jobRate += "," + JOB_RATE_JOB_NAME;
        jobRate += "," + JOB_RATE_INDEX;
        jobRate += "," + JOB_RATE_RATE;
        jobRate += "," + JOB_RATE_EFF_DATE;
        String jobTeam = TYPE_T;
        jobTeam += "," + JOBTEAM_JOB_NAME;
        jobTeam += "," + JOBTEAM_WBT_NAME;
        jobTeam += "," + JOBTEAM_DOT_THRESH;
        jobTeam += "," + JOBTEAM_WOT_THRESH;
        jobTeam += "," + JOBTEAM_AV_HR_RATE;
        jobTeam += "," + JOBTEAM_OT_MULT;
        jobTeam += "," + JOBTEAM_OT_INC;

        return job + "\n" + jobRate + "\n" + jobTeam;
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }


}
