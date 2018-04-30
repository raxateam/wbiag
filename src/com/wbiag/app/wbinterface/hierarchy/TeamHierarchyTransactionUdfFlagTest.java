package com.wbiag.app.wbinterface.hierarchy;

import java.io.*;
import java.util.*;

import com.wbiag.app.wbinterface.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.hierarchy.*;
import com.workbrain.sql.*;
import com.workbrain.test.*;
import com.workbrain.util.*;
import junit.framework.*;
import com.workbrain.tool.security.*;
/**
 * Unit test for TeamHierarchyTransactionUdfFlagTests.
 */
public class TeamHierarchyTransactionUdfFlagTest extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(TeamHierarchyTransactionUdfFlagTest.class);

    private final static String WBT_LEAF_NAME = "TEAM_TEST1.1.1";
	private final static String WBT_LEAF_TYPE_NAME = "CREWS";
    public final static String WBT_STRING = "TEAM_TEST~TEAM_TEST1~TEAM_TEST1.1~" + WBT_LEAF_NAME + "|" + WBT_LEAF_TYPE_NAME;
    public final static String WBT_ACTION = "ADD";
    public final static String WBROLE_NAME = "";
    public final static String WBU_NAME = "";
    public final static String WBUT_ACTION = "";
    public final static String WBUT_START_DATE = "";
    public final static String WBUT_END_DATE = "";
    public final static String WBT_DESC = "TEAM_TEST1.1.1 DESC";
    public final static String WBT_UDF1 = "UDF1";
    public final static String WBT_UDF2 = "&";
    public final static String WBT_FLAGS = "&Y";

    final String typName = "TEAM HIERARCHY IMPORT";
    final int typId = 40;

    public TeamHierarchyTransactionUdfFlagTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(TeamHierarchyTransactionUdfFlagTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testInsertTeam() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hierarchy.TeamHierarchyTransactionUdfFlag");

        String path = createFile(createData());

        TransactionData trans = importCSVFile(path ,
                                              new TeamHierarchyTransformer() ,
                                              typName);

        HashMap param = new HashMap();
        param.put("OverridesExistingUserTeam" , "Y");
        runWBInterfaceTaskByTransactionId(param , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        // *** validate team data
        WorkbrainTeamData data = getCodeMapper().getWBTeamByName(WBT_LEAF_NAME);
		WorkbrainTeamTypeData wbttData = getCodeMapper().getWBTeamTypeById(data.getWbttId());

        assertNotNull(data);
        assertEquals(WBT_LEAF_NAME , data.getWbtName());

		assertEquals(WBT_LEAF_TYPE_NAME, wbttData.getWbttName());
        assertEquals(WBT_DESC , data.getWbtDesc());

        assertEquals(WBT_UDF1 , data.getWbtUdf1());
        assertTrue(StringHelper.isEmpty(data.getWbtUdf2()));
        assertTrue(StringHelper.isEmpty(data.getWbtFlag1()));
        assertEquals("Y" , data.getWbtFlag2());
    }


    private String createData(){

        String stm = WBT_STRING;
        stm += "," + WBT_ACTION;
        stm += "," + WBROLE_NAME;
        stm += "," + WBU_NAME;
        stm += "," + WBUT_ACTION;
        stm += "," + WBUT_START_DATE;
        stm += "," + WBUT_END_DATE;
        stm += "," + WBT_DESC;
        stm += "," + WBT_UDF1;
        stm += "," + WBT_UDF2;
        stm += ",";
        stm += ",";
        stm += ",";
        stm += "," + WBT_FLAGS;
        return stm;
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }


}
