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
/**
 * Unit test for HRRefreshTransactionEndEntPolsTest
 *
 */
public class HRRefreshTransactionEndEntPolsTest extends WBInterfaceCustomTestCase{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionEndEntPolsTest.class);

    private final long index = System.currentTimeMillis();
    private final String OVR_START_DATE_COL       = "";
    private final String OVR_END_DATE_COL         = "01/01/2005";
    private String EMP_NAME_COL              ;
    private String EMP_LASTNAME_COL         ;
    private String EMP_FIRSTNAME_COL        ;
    private final String END_EMP_ENT_POLS_COL             = "TEST";
    private final String typName = "HR REFRESH";
    private final int typId = 1;
    final int empId = 14;

    public HRRefreshTransactionEndEntPolsTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HRRefreshTransactionEndEntPolsTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testEndEmpEntPol() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionEndEntPols");

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

        EntEmpPolicyData eepd = new EntEmpPolicyAccess(getConnection()).loadByEmpId(ed.getEmpId());
        assertTrue(eepd != null);
        assertEquals(DateHelper.convertStringToDate(OVR_END_DATE_COL, "MM/dd/yyyy"),
                   eepd.getEntemppolEndDate());
    }



    private void cleanEmpPolicies(int empId) throws Exception{
        PreparedStatement ps = null;
        try {
            String sql = "DELETE FROM ent_emp_policy WHERE emp_id = ?";
            ps = getConnection().prepareStatement(sql);
            ps.setInt(1 , empId);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

    }
    private int createEntPolicy(String polName , Date start, Date end) throws Exception {
        EntPolicyData entPolData = new EntPolicyData();
        int polId = getConnection().getDBSequence("seq_entpol_id").getNextValue();
        entPolData.setEntpolId(polId);
        entPolData.setEntpolName(polName);
        entPolData.setEntpolStartDate(start);
        entPolData.setEntpolEndDate(end);
        new RecordAccess(getConnection()).insertRecordData(entPolData,
            "ENT_POLICY");
        return polId;
    }

    private void delEntPolicy(String entpolName) throws Exception{
        PreparedStatement ps = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("DELETE FROM ENT_POLICY WHERE entpol_name = ?");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setString(1  , entpolName);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

    }

    private void associatePolicyWithEmployee(int empId, int polId) throws Exception{
        EntEmpPolicyData pol = new EntEmpPolicyData();
        pol.setEmpId(empId);
        pol.setEntemppolStartDate(DateHelper.DATE_1900);
        pol.setEntemppolEndDate(DateHelper.DATE_3000);
        pol.setEntemppolEnabled("Y");
        pol.setEntemppolPriority(1);
        pol.setEntpolId(polId);
        new EntEmpPolicyAccess(getConnection()).insert(pol);

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
        stm += "," + END_EMP_ENT_POLS_COL ;
        return stm;
    }

    protected void setUp() throws Exception  {
        super.setUp();
        cleanEmpPolicies(empId);
        int emppolId = createEntPolicy(END_EMP_ENT_POLS_COL, DateHelper.DATE_1900 , DateHelper.DATE_3000);
        associatePolicyWithEmployee(empId, emppolId);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        updateWbintTypeClass(typId , "com.workbrain.app.wbinterface.hr2.HRRefreshTransaction");
        cleanEmpPolicies(empId);
        delEntPolicy(END_EMP_ENT_POLS_COL);

        getConnection().commit();
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
