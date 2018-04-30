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

/**
 * Unit test for ShiftPatternTransactionSOTests.
 */
public class WorkSummaryTransactionTest
    extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(WorkSummaryTransactionTest.class);

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




    final String typName = "WORK SUMMARY IMPORT";
    final int typId = 10041;

    public WorkSummaryTransactionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WorkSummaryTransactionTest.class);
        return result;
    }

    public void testWorkSummaryOverride() throws Exception {


        updateWbintTypeClass(typId,
                             "com.wbiag.app.wbinterface.schedulein.WorkSummaryTransaction");


        String path = createFile(createData());


        TransactionData trans = this.importCSVFile(path,
            new WorkSummaryTransformer(),
            typName);

        HashMap param = new HashMap();
        runWBInterfaceTaskByTransactionId(param, trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());


    }

    protected void setUp() throws Exception {
        super.setUp();
        java.sql.PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("INSERT INTO wbint_type (wbityp_id, wbityp_name, wbityp_type, wbityp_del_older_than) ");
            sb.append(" VALUES (?,?, ?, ?)");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setInt(1  , typId);
            ps.setString(2  , typName);
            ps.setString(3  , "IMPORT");
            ps.setInt(4  , 9999999);
            int upd = ps.executeUpdate();
            getConnection().commit();
        }
        catch (Exception e) {
            logger.error("Error in creating wbintType, it probably exists" , e);
        }
        finally {
            if (ps != null) ps.close();
        }

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        updateWbintTypeClass(typId,
                             "com.workbrain.app.wbinterface.schedulein.WorkSummaryTransaction");
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
