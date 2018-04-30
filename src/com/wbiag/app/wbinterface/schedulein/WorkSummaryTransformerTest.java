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
import com.wbiag.app.wbinterface.schedulein.*;
import com.workbrain.app.scheduler.enterprise.implementation.TaskLock;


/**
 * Unit test for ShiftPatternTransactionSOTests.
 */
public class WorkSummaryTransformerTest
    extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkSummaryTransformerTest.class);


    private final static String EMP_NAME = "2015";
    private final static String OVR_ID = "300";
    private final static String WRK_DATE = "20050107";
    private final static String CLOCKS = "~XX2005010709000001TCODE=BRK~XX2005010718000002~";
    private final static String CALC_GRP = "";
    private final static String PAY_GRP = "";
    private final static String WRKS_AUTH = "Y";
    private final static String WRKS_FLAG_BRK = "N";
    private final static String WRKS_FLAG_RECALL = "N";
    private final static String WRKS_IN_CODE = "WRK";
    private final static String WRKS_OUT_CODE = "WRK";




    public WorkSummaryTransformerTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkSummaryTransformerTest.class);
        return result;
    }

    public void testWorkSummaryTransformer() throws Exception {
        DBConnection c = null;
        int taskId = 10104;
        c = this.getConnection();


        /**
         * This is same as workdetial transformer, and doesn't warrent nightly
         * testing.
         *
         * To enable nightly testing a transformer test case needs to be created
         * that can lock task. Like runWBInterfaceTaskByTransactionId for
         * transactions.
         *
        String path = createFile(createData());
        WorkSummaryTransformer xform = new WorkSummaryTransformer();
        CSVImportTask importTask = new CSVImportTask();
        importTask.setTaskId(taskId);
        List rowList = importTask.importCSVOneClient(c, xform,
            "WORK SUMMARY IMPORT", path, false, 1);
        ImportData id = (ImportData) rowList.get(0);

        this.assertEquals("Status Check", ImportData.STATUS_PENDING, id.getStatus());
*/
    }

    private String createData() {

      String stm = EMP_NAME;
      stm += "," +   OVR_ID;
      stm += "," +   WRK_DATE;
      stm += "," +   CLOCKS;
      stm += "," +   CALC_GRP;
      stm += "," +   PAY_GRP;
      stm += "," +   WRKS_AUTH;
      stm += "," +   WRKS_FLAG_BRK;
      stm += "," +   WRKS_FLAG_RECALL;
      stm += "," +   WRKS_IN_CODE;
      stm += "," +   WRKS_OUT_CODE;

      return stm;
  }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
