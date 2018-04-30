
package com.wbiag.app.ta.holiday;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import junit.framework.TestSuite;

import com.wbiag.server.cleanup.CleanupProcess;
import com.workbrain.app.ta.db.EmployeeAccess;
import com.workbrain.app.ta.db.HolidayAccess;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.model.HolidayData;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.OverrideList;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.app.wbinterface.hr2.engine.HRRefreshData;
import com.workbrain.app.wbinterface.hr2.engine.HRRefreshProcessor;
import com.workbrain.sql.DBConnection;
import com.workbrain.test.TestCaseHW;
import com.workbrain.test.TestUtil;
import com.workbrain.tool.overrides.DeleteOverride;
import com.workbrain.tool.overrides.InsertEmployeeHolidayOverride;
import com.workbrain.tool.overrides.InsertEmployeeOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

public class HolidayAssignmentTaskExtTest extends TestCaseHW
{

	public HolidayAssignmentTaskExtTest(String arg0)
	{
		super(arg0);
	}

	public static TestSuite suite()
	{
		TestSuite result = new TestSuite();
		result.addTestSuite(HolidayAssignmentTaskExtTest.class);
		return result;
	}

	/**
	 * - Creates test holiday
	 * - Asserts override is created for 20 days ahead
	 * @throws Exception
	 */
	public void testAssign() throws Exception {

		TestUtil.getInstance().setVarTemp("/system/WORKBRAIN_PARAMETERS/HOLIDAY/HOLIDAY_ASSIGNMENT_USE_HOLIDAY" ,
										  "true");

		final int empId = 15;
		final int daysAhead = 20;
		final String holName = "TEST";
		final Date start = DateHelper.getCurrentDate();
		HolidayData hd = null;

        OverrideAccess oa = new OverrideAccess(getConnection());

        try {
            Date holDate = DateHelper.addDays(start, daysAhead);
            hd = createOrUpdateHoliday(holName, holDate);

            HolidayAssignmentTaskExt hasgn = new HolidayAssignmentTaskExt() {
                protected void sendMessage(String message) {
                }
                protected void sendMessage( String message , String userName) {
                }
            };
            HashMap params = new HashMap();
            params.put(HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM,
                       String.valueOf(daysAhead));
            params.put(HolidayAssignmentTaskExt.EMPLOYEE_IDS_PARAM,
                       String.valueOf(empId));
            params.put(HolidayAssignmentTaskExt.HOLIDAY_IDS_PARAM,
                       String.valueOf(hd.getHolId()));
            params.put(HolidayAssignmentTaskExt.USERNAME_PARAM, "WORKBRAIN");
            hasgn.run( -1, params);

            OverrideList ovrs = oa.loadAffectingOverrides(empId, holDate,
                OverrideData.HOLIDAY_TYPE_START, OverrideData.HOLIDAY_TYPE_END);
            assertEquals(1, ovrs.size());
        }
        finally {
            //clean up
            if (hd != null) {
                oa.deleteOverridesByHoliday(empId, hd , DateHelper.DATE_1900);
                new HolidayAccess(getConnection()).deleteRecordData("HOLIDAY", "HOL_ID", hd.getHolId());
                getConnection().commit();
            }
        }
	}

	/**
	 * - Creates test holiday
	 * - Asserts override is NOT created for 20 days ahead
	 * @throws Exception
	 */
	public void testNotAssign() throws Exception {

		TestUtil.getInstance().setVarTemp("/system/WORKBRAIN_PARAMETERS/HOLIDAY/HOLIDAY_ASSIGNMENT_USE_HOLIDAY" ,
										  "true");

		final int empId = 16;
		final int daysAhead = 20;
		final String holName = "TEST";
		final Date start = DateHelper.getCurrentDate();
		HolidayData hd = null;
		OverrideAccess oa = new OverrideAccess(getConnection());
        try {
            Date holDate = DateHelper.addDays(start, daysAhead + 5);
            hd = createOrUpdateHoliday(holName, holDate);

            HolidayAssignmentTaskExt hasgn = new HolidayAssignmentTaskExt() {
                protected void sendMessage(String message) {
                }
                protected void sendMessage( String message , String userName) {
                }

            };
            HashMap params = new HashMap();
            params.put(HolidayAssignmentTaskExt.EXACT_DAYS_TO_PROCESS_PARAM,
                       String.valueOf(daysAhead + 1));
            params.put(HolidayAssignmentTaskExt.EMPLOYEE_IDS_PARAM,
                       String.valueOf(empId));
            params.put(HolidayAssignmentTaskExt.HOLIDAY_IDS_PARAM,
                       String.valueOf(hd.getHolId()));
            params.put(HolidayAssignmentTaskExt.USERNAME_PARAM, "WORKBRAIN");
            hasgn.run( -1, params);

            OverrideList ovrs = oa.loadAffectingOverrides(empId, holDate,
                OverrideData.HOLIDAY_TYPE_START, OverrideData.HOLIDAY_TYPE_END);
            assertEquals(0, ovrs.size());
        }
        finally {
            //clean up
            if (hd != null) {
                oa.deleteOverridesByHoliday(empId, hd ,DateHelper.DATE_1900);
                new HolidayAccess(getConnection()).deleteRecordData("HOLIDAY", "HOL_ID", hd.getHolId());
                getConnection().commit();
            }
        }
}

	/**
	 * - Creates test holiday
	 * - Create employee override
     * - Create holiday override for end of year
	 * - Asserts holiday override is created for the override created employee
     * - Assert holiday override for end of year is cancelled
	 * @throws Exception
	 */
	public void testChangedEmps() throws Exception
	{
		final int empId = 15;
		final int daysBehind = 2;
		final boolean shouldCalc = true;
		final boolean shouldCommit = true;
		final int daysAhead = 20;
		final String holName = "TEST";
		int ovrId = 0;
		int prevOvrId = 0;
		OverrideAccess oa = new OverrideAccess(getConnection());
		OverrideBuilder ob = new OverrideBuilder(getConnection());
        ob.setCreatesDefaultRecords(true);
        HolidayData hd = null;
        HolidayData prevHd = null;

        try {
            Date endDate = DateHelper.getCurrentDate();
            Date startDate = DateHelper.addDays(endDate, -daysBehind);
            DBConnection conn = getConnection();

            Date holDate = DateHelper.addDays(endDate, daysAhead);
            hd = createOrUpdateHoliday(holName, holDate);

            // assign a holiday for end of year, thsi should get deleted by process
            Date prevHolDate = DateHelper.getUnitYear(DateHelper.
                APPLY_ON_LAST_DAY, false, endDate);
            String prevHolName = "TEST PREV";
            prevHd = createOrUpdateHoliday(prevHolName, prevHolDate);
            InsertEmployeeHolidayOverride ovrh = new InsertEmployeeHolidayOverride(conn);
            prevOvrId = getConnection().getDBSequence(OverrideAccess.OVERRIDE_SEQ).getNextValue();
            ovrh.setOverrideId(prevOvrId);
            ovrh.setWbuName("junit test");
            ovrh.setEmpId(empId);
            ovrh.setStartDate(prevHolDate);
            ovrh.setEndDate(prevHolDate);
            ovrh.setHolName(prevHolName);
            ob.add(ovrh);

            InsertEmployeeOverride ovr = new InsertEmployeeOverride(conn);
            ovrId = getConnection().getDBSequence(OverrideAccess.OVERRIDE_SEQ).getNextValue();
            ovr.setOverrideId(ovrId);
            ovr.setWbuName("junit test");
            ovr.setEmpId(empId);
            ovr.setStartDate(startDate);
            ovr.setEndDate(endDate);
            ovr.setEmpFlag2("Y");
            ovr.setOvrComment("TEST WD");
            ob.add(ovr);
            ob.execute(shouldCalc, shouldCommit);

            HolidayAssignmentTaskExt hasgn = new HolidayAssignmentTaskExt() {
                protected void sendMessage(String message) {
                }
                protected void sendMessage( String message , String userName) {
                }
            };
            HashMap params = new HashMap();
            params.put(HolidayAssignmentTaskExt.
                       RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM, "EMP_FLAG2");
            params.put(HolidayAssignmentTaskExt.CHECK_EMPLOYEES_DAYS_BACK_PARAM,
                       String.valueOf(daysBehind));
            params.put(HolidayAssignmentTaskExt.HOLIDAY_IDS_PARAM,
                       String.valueOf(hd.getHolId()));
            params.put(HolidayAssignmentTaskExt.USERNAME_PARAM, "WORKBRAIN");
            hasgn.run( -1, params);

            OverrideList ovrs = oa.loadAffectingOverrides(empId, holDate,
                OverrideData.HOLIDAY_TYPE_START, OverrideData.HOLIDAY_TYPE_END);
            assertEquals(1, ovrs.size());
            OverrideList ovrsPrev = oa.loadOverrides(empId, prevHolDate,
                                             OverrideData.HOLIDAY_TYPE_START,
                                             OverrideData.HOLIDAY_TYPE_END,
                                             null,
                                             new String[]{OverrideData.CANCELLED} );
            assertEquals(1, ovrsPrev.size());
        }
        catch (Exception e) {
            getConnection().rollback();
        }
        finally {
            //clean up
            if (hd != null) {
                oa.deleteOverridesByHoliday(empId, hd ,DateHelper.DATE_1900);
                new HolidayAccess(getConnection()).deleteRecordData(HolidayAccess.HOLIDAY_TABLE,
                                    HolidayAccess.HOLIDAY_PRI_KEY, hd.getHolId());
            }
            if (ovrId != 0) {
                ob.clear();
                // first cancel to rollback the effect
                DeleteOverride dov = new DeleteOverride();
                dov.setOverrideId(ovrId);
                ob.add(dov);
                ob.execute(true, true);
                oa.deleteRecordData(OverrideAccess.OVERRIDE_TABLE,
                                    OverrideAccess.OVERRIDE_PRI_KEY, ovrId);
                getConnection().commit();
            }
            if (prevOvrId != 0) {
                ob.clear();
                // first cancel to rollback the effect
                DeleteOverride dov = new DeleteOverride();
                dov.setOverrideId(prevOvrId);
                ob.add(dov);
                ob.execute(true, true);
                oa.deleteRecordData(OverrideAccess.OVERRIDE_TABLE, OverrideAccess.OVERRIDE_PRI_KEY, prevOvrId);
                getConnection().commit();
            }
            if (prevHd != null) {
                new HolidayAccess(getConnection()).deleteRecordData(HolidayAccess.HOLIDAY_TABLE,
                                    HolidayAccess.HOLIDAY_PRI_KEY,
                                    prevHd.getHolId());
            }
            getConnection().commit();
        }
	}

	 /**
     * If optional trigger TRG_EMP_AIUD has not been run
     * manuall update the CHANGE_HISTORY table.
     * @param dbconnection, empId
     * @return
     * @throws SQLException
     */
	protected void changeHistory(DBConnection dbConnection, int empId, Date createDate)
 	throws SQLException
 	{

		 PreparedStatement ps = null;
		 ResultSet rs = null;
		 int clientId = 1;
		 String changeType = "'I'";
		 String changedTable = "'EMPLOYEE'";
		 String value= "'"+(new Integer(empId)).toString()+"'";

		 SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
		 String dateString = format.format(createDate);
	 try
		 {
			 /*INSERT INTO CHANGE_HISTORY Values (30202, to_date('2005-10-25 00:00:00', 'yyyy-mm-dd hh24:mi:ss'), 'EMPLOYEE', 'I', 30202, '30202', 1)*/
			 String sql = "INSERT INTO CHANGE_HISTORY "
				 		+ "Values ("+empId+", to_date('"+dateString+"', 'yyyy-mm-dd hh24:mi:ss'), "+changedTable+", "+changeType+", "+
				 		empId+", "+value+", "+clientId+")";

			 ps = getConnection().prepareStatementAsIs(sql);
       	 	 rs = ps.executeQuery();
		 }
		 finally
		 {
			 if (rs != null)
			 {
				 rs.close();
			 }
			 if (ps != null)
			 {
				 ps.close();
			 }
     }

 }
	 /**
     * If optional trigger TRG_EMP_AIUD has not been run
     * cleanup manual update of the CHANGE_HISTORY table.
     * @param dbconnection, empId
     * @return
     * @throws SQLException
     */
	 protected void deleteChangeHistory(DBConnection dbConnection, int empId, Date createDate)
	 	throws SQLException
	 	{

			 PreparedStatement ps = null;
			 ResultSet rs = null;

			 try
			 {
				 String sql = "DELETE"
	    				+ " FROM change_history "
	        			+ " WHERE chnghist_table_name = ? "
	        			+ " AND chnghist_change_date >= ? "
	        			+ " AND change_history.chnghist_change_type = ?";

					 ps = getConnection().prepareStatement(sql);
					 ps.setString(1 , EmployeeAccess.EMPLOYEE_TABLE.toUpperCase());
					 ps.setTimestamp(2 , (Timestamp) createDate);
					 ps.setString(3 , "I");
				// ps = getConnection().prepareStatementAsIs(sql);
	       	 	 rs = ps.executeQuery();
			 }
			 finally
			 {
				 if (rs != null)
				 {
					 rs.close();
				 }
				 if (ps != null)
				 {
					 ps.close();
				 }
	     }

	 }

	 /**
	  * Check if optional trigger TRG_EMP_AIUD has not been run
	  * If it has been run, the change history table would have
	  * been updated
	  * @param dbconnection, empId
	  * @return true or false
	  * @throws SQLException
	  */
	 protected boolean updatedChangeHistory(DBConnection dbConnection, int empId, Date createDate)
	 	throws SQLException
	 	{
	  		 boolean tableUpdated = false;
			 PreparedStatement ps = null;
			 ResultSet rs = null;

			 try
			 {
				 String sql = "SELECT *"
    				+ " FROM change_history "
        			+ " WHERE chnghist_table_name = ? "
        			+ " AND chnghist_change_date >= ? "
        			+ " AND change_history.chnghist_change_type = ?";

				 ps = getConnection().prepareStatement(sql);
				 ps.setString(1 , EmployeeAccess.EMPLOYEE_TABLE.toUpperCase());
				 ps.setTimestamp(2 , (Timestamp) createDate);
				 ps.setString(3 , "I");
				// ps = getConnection().prepareStatementAsIs(sql);
	       	 	 rs = ps.executeQuery();
			 }
			 finally
			 {

				 if (rs != null)
				 {
					 if (rs.next())
						 tableUpdated = true;
					 rs.close();
				 }
				 if (ps != null)
				 {
					 ps.close();
				 }
			 }
			 return tableUpdated;
	 }


    /**
     * TRG_EMP_AIUD must be in DB schema for this to work
     * - Creates test holiday
     * - Create employee
     * - Create holiday override for end of year
     * - Asserts holiday override is created for the override created employee
     * - Assert holiday override for end of year is cancelled
     * @throws Exception
     */
	 public void testNewEmps() throws Exception
    {
        int empId = 0;
        final int daysBehind = 2;
        final boolean shouldCalc = true;
        final boolean shouldCommit = true;
        final int daysAhead = 20;
        final String holName = "TEST";
        OverrideAccess oa = new OverrideAccess(getConnection());
        OverrideBuilder ob = new OverrideBuilder(getConnection());
        ob.setCreatesDefaultRecords(true);
        HolidayData hd = null;
        EmployeeData ed = null;
        Date endDate = DateHelper.getCurrentDate();

        try {

            Date startDate = DateHelper.addDays(endDate, -daysBehind);
            DBConnection conn = getConnection();

            Date holDate = DateHelper.addDays(endDate, daysAhead);
            hd = createOrUpdateHoliday(holName, holDate);
            // create emp
            String empName = "TEST_" + System.currentTimeMillis();
            HRRefreshProcessor pr = new HRRefreshProcessor(getConnection());
            HRRefreshData data = new HRRefreshData(getConnection() , empName);
            data.setEmpFirstname(empName);
            data.setEmpLastname(empName);
            data.setEmpSin(empName);
            pr.addHRRefreshData(data) ;
            pr.process(true);
            ed = new EmployeeAccess(getConnection() , getCodeMapper()).loadByName(empName , DateHelper.getCurrentDate());
            assertNotNull(ed);
            empId = ed.getEmpId();

            //if trg_amp_auid not set, then we will update the change history table
            if (!updatedChangeHistory(getConnection(),empId, endDate))
            {
            	changeHistory(getConnection(), empId, endDate);
            	getConnection().commit();
            }
            HolidayAssignmentTaskExt hasgn = new HolidayAssignmentTaskExt() {
                protected void sendMessage(String message) {
                }
                protected void sendMessage( String message , String userName) {
                }
            };
            HashMap params = new HashMap();
            params.put(HolidayAssignmentTaskExt.
                       RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM, "Y");
            params.put(HolidayAssignmentTaskExt.CHECK_EMPLOYEES_DAYS_BACK_PARAM,
                       String.valueOf(daysBehind));
            params.put(HolidayAssignmentTaskExt.HOLIDAY_IDS_PARAM,
                       String.valueOf(hd.getHolId()));
            params.put(HolidayAssignmentTaskExt.USERNAME_PARAM, "WORKBRAIN");
            hasgn.run( -1, params);

            OverrideList ovrs = oa.loadAffectingOverrides(empId, holDate,
                OverrideData.HOLIDAY_TYPE_START, OverrideData.HOLIDAY_TYPE_END);
            assertEquals(1, ovrs.size());
        }
        catch (Exception e) {
            getConnection().rollback();
        }
        finally {
            //clean up created employee
            if (ed != null) {
                CleanupProcess.CleanupProcessContext ct = new CleanupProcess.CleanupProcessContext();
                ct.setDBConnection(getConnection() );
                ct.setClientId("1");
                ct.setDeleteEmployeeWhereClause("emp_id = " + empId);
                ct.setShouldCommit(true);
                // *** load default from CleanupProcess.java.net
                java.net.URL propsFile = ct.getClass().getResource("CleanupProcess.properties");
                Properties props = new Properties();
                props.load(propsFile.openStream());
                ct.setDeleteEmployeeTables(StringHelper.detokenizeStringAsList(
                    props.getProperty(CleanupProcess.PROP_DELETE_EMPLOYEE_TABLES) , ","));
                CleanupProcess cleanup = new CleanupProcess(ct);
                cleanup.execute();
            }
            if (hd != null) {
                oa.deleteOverridesByHoliday(empId, hd ,DateHelper.DATE_1900);
                new HolidayAccess(getConnection()).deleteRecordData(HolidayAccess.HOLIDAY_TABLE,
                                    HolidayAccess.HOLIDAY_PRI_KEY, hd.getHolId());
            }

            getConnection().commit();
            //clean up change history table
           if (updatedChangeHistory(getConnection(),empId, endDate))
            deleteChangeHistory(getConnection(), empId, endDate);
            getConnection().commit();
        }
    }

    /**
     * TRG_EMP_AIUD must be in DB schema for this to work
     * - Creates test holiday
     * - Create employee
     * - Create holiday override for end of year
     * - Asserts holiday override is created for the override created employee
     * - Assert holiday override for end of year is cancelled
     * @throws Exception
     */
    public void testNewChangedEmps() throws Exception
    {
        int empId = 0;
        final int daysBehind = 2;
        final boolean shouldCalc = true;
        final boolean shouldCommit = true;
        final int daysAhead = 20;
        final String holName = "TEST";
        OverrideAccess oa = new OverrideAccess(getConnection());
        OverrideBuilder ob = new OverrideBuilder(getConnection());
        ob.setCreatesDefaultRecords(true);
        HolidayData hd = null;
        EmployeeData ed = null;

        try {
            Date endDate = DateHelper.getCurrentDate();
            Date startDate = DateHelper.addDays(endDate, -daysBehind);
            DBConnection conn = getConnection();

            Date holDate = DateHelper.addDays(endDate, daysAhead);
            hd = createOrUpdateHoliday(holName, holDate);
            // create emp
            String empName = "TEST_" + System.currentTimeMillis();
            HRRefreshProcessor pr = new HRRefreshProcessor(getConnection());
            HRRefreshData data = new HRRefreshData(getConnection() , empName);
            data.setEmpFirstname(empName);
            data.setEmpLastname(empName);
            data.setEmpSin(empName);
            pr.addHRRefreshData(data) ;
            pr.process(true);
            ed = new EmployeeAccess(getConnection() , getCodeMapper()).loadByName(empName , DateHelper.getCurrentDate());
            assertNotNull(ed);
            empId = ed.getEmpId();
            // create override
            InsertEmployeeOverride ovr = new InsertEmployeeOverride(conn);
            ovr.setWbuName("junit test");
            ovr.setEmpId(empId);
            ovr.setStartDate(startDate);
            ovr.setEndDate(endDate);
            ovr.setEmpFlag2("Y");
            ovr.setOvrComment("TEST WD");
            ob.add(ovr);
            ob.execute(shouldCalc, shouldCommit);

            HolidayAssignmentTaskExt hasgn = new HolidayAssignmentTaskExt() {
                protected void sendMessage(String message) {
                }
                protected void sendMessage( String message , String userName) {
                }
            };
            HashMap params = new HashMap();
            params.put(HolidayAssignmentTaskExt.
                       RUN_FOR_NEW_EMPLOYEES_ONLY_PARAM, "Y");
            params.put(HolidayAssignmentTaskExt.
                       RUN_FOR_CHANGED_EMPLOYEES_ATTRIBUTES_PARAM, "EMP_FLAG2");
            params.put(HolidayAssignmentTaskExt.CHECK_EMPLOYEES_DAYS_BACK_PARAM,
                       String.valueOf(daysBehind));
            params.put(HolidayAssignmentTaskExt.HOLIDAY_IDS_PARAM,
                       String.valueOf(hd.getHolId()));
            params.put(HolidayAssignmentTaskExt.USERNAME_PARAM, "WORKBRAIN");
            hasgn.run( -1, params);

            OverrideList ovrs = oa.loadAffectingOverrides(empId, holDate,
                OverrideData.HOLIDAY_TYPE_START, OverrideData.HOLIDAY_TYPE_END);
            assertEquals(1, ovrs.size());
        }
        catch (Exception e) {
            getConnection().rollback();
        }
        finally {
            //clean up created employee
            if (ed != null) {
                CleanupProcess.CleanupProcessContext ct = new CleanupProcess.CleanupProcessContext();
                ct.setDBConnection(getConnection() );
                ct.setClientId("1");
                ct.setDeleteEmployeeWhereClause("emp_id = " + empId);
                ct.setShouldCommit(true);
                // *** load default from CleanupProcess.java.net
                java.net.URL propsFile = ct.getClass().getResource("CleanupProcess.properties");
                Properties props = new Properties();
                props.load(propsFile.openStream());
                ct.setDeleteEmployeeTables(StringHelper.detokenizeStringAsList(
                    props.getProperty(CleanupProcess.PROP_DELETE_EMPLOYEE_TABLES) , ","));
                CleanupProcess cleanup = new CleanupProcess(ct);
                cleanup.execute();
            }
            if (hd != null) {
                oa.deleteOverridesByHoliday(empId, hd ,DateHelper.DATE_1900);
                new HolidayAccess(getConnection()).deleteRecordData(HolidayAccess.HOLIDAY_TABLE,
                                    HolidayAccess.HOLIDAY_PRI_KEY, hd.getHolId());
            }
            getConnection().commit();
        }
    }

    private HolidayData createOrUpdateHoliday(String holName, Date holDate) throws Exception {
        HolidayData hd = null;
        HolidayAccess ha = new HolidayAccess(getConnection());
        List l = ha.loadRecordData(new HolidayData(),
                                   HolidayAccess.HOLIDAY_TABLE, "hol_name",
                                   holName);

        if (l.size() == 0) {
            hd = new HolidayData();
            int holId = getConnection().getDBSequence(HolidayAccess.HOLIDAY_SEQ).
                getNextValue();
            hd.setHolId(holId);
            hd.setHolName(holName);
            hd.setHolUseCalendar("N");
            hd.setHolDate(holDate);
            hd.setLmsId(RuleHelper.getDefaultLmsId(getCodeMapper()));
            ha.insertRecordData(hd, HolidayAccess.HOLIDAY_TABLE);
            getConnection().commit();
        }
        else {
            hd = (HolidayData) l.get(0);
            hd.setHolDate(holDate);
            ha.updateRecordData(hd, HolidayAccess.HOLIDAY_TABLE,
                                HolidayAccess.HOLIDAY_PRI_KEY);
            getConnection().commit();
        }
        return hd;
    }

	public static void main(String[] args) throws Exception {
			junit.textui.TestRunner.run(suite());
	}

}

