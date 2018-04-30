package com.wbiag.app.wbinterface.hr2;

import java.sql.PreparedStatement;
import java.util.*;

import com.wbiag.app.wbinterface.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import junit.framework.*;
import com.workbrain.tool.security.*;
/**
 * Unit test for HRRefreshTransactionTempTeamTest
 *
 */
public class HRRefreshTransactionTempTeamTest extends WBInterfaceCustomTestCase{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionTempTeamTest.class);

    private final long index = System.currentTimeMillis();
    private final String OVR_START_DATE_COL       = "";
    private final String OVR_END_DATE_COL         = "";
    private String EMP_NAME_COL              ;
    private String EMP_LASTNAME_COL         ;
    private String EMP_FIRSTNAME_COL        ;
    private final String TEMP_TEAM_COL             = "~~OFFICE|~~TEST3";
    private final String typName = "HR REFRESH";
    private final int typId = 1;
    final int empId = 14;

    public HRRefreshTransactionTempTeamTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HRRefreshTransactionTempTeamTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testTempTeam() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionTempTeam");

        final String empName = "2015";

        String path = createFile(createUpdateData(empName));

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

        List empTeams = SecurityEmployee.getTempTeams(getConnection() , empId, SQLHelper.getCurrDate()  );
        assertTrue(empTeams.contains("OFFICE"));
        assertTrue(empTeams.contains("TEST3"));

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
        stm += "," + TEMP_TEAM_COL ;
        return stm;
    }

    protected void setUp() throws Exception  {
        super.setUp();
        SecurityEmployee.deleteTempTeamsForEmployee(getConnection() , empId);
        getConnection().commit();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        updateWbintTypeClass(typId , "com.workbrain.app.wbinterface.hr2.HRRefreshTransaction");

        SecurityEmployee.deleteTempTeamsForEmployee(getConnection() , empId);

        getConnection().commit();
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
