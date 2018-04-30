package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.*;
import com.wbiag.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * Unit test for updating emp_fulltime
 *
 */
public class HRRefreshTransactionDelEmpTest extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionDelEmpTest.class);

    private final long index = System.currentTimeMillis();
    private final String OVR_START_DATE_COL       = "";
    private final String OVR_END_DATE_COL         = "";
    private String EMP_NAME_COL              ;
    private String EMP_LASTNAME_COL         ;
    private String EMP_FIRSTNAME_COL        ;

    private final String typName = "HR REFRESH";
    private final int typId = 1;


    public HRRefreshTransactionDelEmpTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HRRefreshTransactionDelEmpTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testExistingEmp() throws Exception{
        getConnection().setCodeMapper(getCodeMapper());
        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionDelEmp");

        // *** create HRRefreshProcessor
        HRRefreshProcessor pr = new HRRefreshProcessor(getConnection());
        // *** add employeee job change for 2015
        String empName = "TEST_" + System.currentTimeMillis();
        HRRefreshData data = new HRRefreshData(getConnection() , empName);
        data.setEmpFirstname(empName);
        data.setEmpLastname(empName);
        data.setEmpSin(empName);
        pr.addHRRefreshData(data) ;
        pr.process(true);
        DBConnection c = getConnection();
        CodeMapper cm = CodeMapper.createCodeMapper(c);
        EmployeeAccess ea = new EmployeeAccess(c,cm);
        EmployeeData ed = ea.loadByName(empName, DateHelper.getCurrentDate());
        assertNotNull(ed);
        int empId = ed.getEmpId();

        String path = createFile(createUpdateStatusData(empName));

        TransactionData trans = importCSVFile(path ,
                                              new HRFileTransformer() ,
                                              typName);

        runWBInterfaceTaskByTransactionId(createDefaultHRTransactionParams() , trans.getWbitranId());
        assertTransactionSuccess(trans.getWbitranId());

        // *** validate data
        // *** assert emp is deleted
        ed = ea.loadByName(EMP_NAME_COL, DateHelper.getCurrentDate());
        assertNull(ed);

        /*PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            StringBuffer sb = new StringBuffer(200);
            sb.append("SELECT count(1) FROM audit_log WHERE audlog_change_date > ? AND audlog_key_id = ? AND audlog_tablename='EMPLOYEE'");
            ps = getConnection().prepareStatement(sb.toString());
            ps.setTimestamp(1 , new Timestamp (DateHelper.addMinutes(new java.util.Date(), -10).getTime()));
            ps.setInt(2, empId);

            rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getInt(1));
                assertTrue("Audit must be created" , rs.getInt(1) > 0 ) ;
            }
        }
        finally {
            SQLHelper.cleanUp(ps, rs);
        }*/


    }


    private String createUpdateStatusData(String empName){

        EMP_NAME_COL             = empName;

        String stm = OVR_START_DATE_COL;
        stm += "," + OVR_END_DATE_COL;
        stm += "," + EMP_NAME_COL;
        for (int i = 1 ; i <= 11 ; i++ ) {
            stm += ",";
        }
        stm += ",D";
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
