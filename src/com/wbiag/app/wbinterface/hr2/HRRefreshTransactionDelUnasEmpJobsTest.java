package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
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
 * Unit test for HRRefreshTransactionDelUnasEmpJobsTest
 *
 */
public class HRRefreshTransactionDelUnasEmpJobsTest extends WBInterfaceCustomTestCase{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionDelUnasEmpJobsTest.class);

    private final long index = System.currentTimeMillis();
    private final String OVR_START_DATE_COL       = "";
    private final String OVR_END_DATE_COL         = "";
    private String EMP_NAME_COL              ;
    private String EMP_LASTNAME_COL         ;
    private String EMP_FIRSTNAME_COL        ;
    private final String EMP_JOBS_COL             = "JANITOR";
    private final String typName = "HR REFRESH";
    private final int typId = 1;


    public HRRefreshTransactionDelUnasEmpJobsTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HRRefreshTransactionDelUnasEmpJobsTest.class);
        return result;
    }

    /**
     * - Adds a CASHIER job to employee job
     * - Runs the interface with JANITOR and asserts only JANITOR  is there
     * @throws Exception
     */
    public void testDelUnassignedEmpJob() throws Exception{

        updateWbintTypeClass(typId , "com.wbiag.app.wbinterface.hr2.HRRefreshTransactionDelUnasEmpJobs");

        final String empName = "2015";
        final int empId = 14;
        final String firstJob = "CASHIER";
        // *** clean all emp jobs
        PreparedStatement ps = null;
        try {
            String sql = "DELETE FROM employee_job WHERE emp_id = ?";
            ps = getConnection().prepareStatement(sql);
            ps.setInt(1 , empId);
            int upd = ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }
        
        //if the Cashier job does not exist in the database, create it first
        createNewJob(firstJob);

        EmployeeJobAccess eja = new EmployeeJobAccess(getConnection());
        EmployeeJobData ejd = new EmployeeJobData();
        ejd.setJobId(getCodeMapper().getJobByName(firstJob).getJobId() );
        ejd.setEmpId(empId);
        ejd.setEmpjobStartDate(DateHelper.DATE_1900);
        ejd.setEmpjobEndDate(DateHelper.DATE_3000);
        ejd.setEmpjobRateIndex(0);
        eja.insert(ejd);

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

        List empjobs = eja.loadByEmpId(ed.getEmpId());
        assertEquals(1 , empjobs.size());
        ejd = (EmployeeJobData)empjobs.get(0);
        assertEquals(getCodeMapper().getJobByName(EMP_JOBS_COL).getJobId() , ejd.getJobId());
    }
    
    /**
     * Method checks to see if a job exists, and if it does not, it creates the job
     * */
    protected void createNewJob(String newJobName) throws Exception
    {
    	JobAccess ja = new JobAccess(getConnection());
        List currentJobs = ja.loadAll();
        
        if (currentJobs.size()>0)
        {
        	for (int i=0; i<currentJobs.size(); i++)
        	{
        		JobData jd = (JobData)currentJobs.get(i);
        		if (jd.getJobName().equalsIgnoreCase(newJobName))
        			return;
        	}	
        }
        
        ja.insertDefault(newJobName, newJobName, DateHelper.addDays(DateHelper.getCurrentDate(),-1), DateHelper.addDays(DateHelper.getCurrentDate(),1));
    }


    private String createUpdateData(String empName){

        EMP_NAME_COL             = empName;

        String stm = OVR_START_DATE_COL;
        stm += "," + OVR_END_DATE_COL;
        stm += "," + EMP_NAME_COL;
        stm += "," + "";
        stm += "," + "";
        for (int i = 0 ; i < 59 ; i++ ) {
            stm += ",";
        }
        stm += "," + EMP_JOBS_COL ;
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
