package com.wbiag.server.hr ;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.tool.security.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for WbiagTermEmpProcessTest.
 */
public class WbiagTermEmpProcessTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WbiagTermEmpProcessTest.class);
    private static final String TIME_PREFIX_START = "amxavStime";
    private static final String TIME_PREFIX_END = "amxavEtime";

    public WbiagTermEmpProcessTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WbiagTermEmpProcessTest.class);
        return result;
    }


    /**
     * Creates ent, empjob, override, team and payroll data and verifies they are gone after process
     * @throws Exception
     */
    public void testTermEmp() throws Exception {
        final int empId = 15;
        final Date termDate = DateHelper.nextDay(DateHelper.getCurrentDate() , "SUN");
        final Date dateAfterTermDate = DateHelper.addDays(termDate , 7);
        final Date dateBeforeTermDate = DateHelper.addDays(termDate , -7);

        EmployeeData ed = getEmployeeData(empId , termDate);
        // *** shouldnt be a terminated emp
        assertNotNull(ed);
        assertTrue(DateHelper.getCalendarTruncatedToDay(ed.getEmpTerminationDate()).get(Calendar.YEAR) == 3000);
        // *** create test data
        // *** create emp job
        EmployeeJobData ejd = new EmployeeJobData ();
        ejd.setGeneratesPrimaryKeyValue(true);
        ejd.setEmpId(empId);
        ejd.setJobId(0);
        ejd.setEmpjobStartDate(DateHelper.DATE_1900);
        ejd.setEmpjobEndDate(DateHelper.DATE_3000);
        EmployeeJobAccess eja = new EmployeeJobAccess (getConnection());
        eja.insert(ejd);
        // *** create ent pol
        EntEmpPolicyData epd = new EntEmpPolicyData ();
        epd.setGeneratesPrimaryKeyValue(true);
        epd.setEmpId(empId);
        epd.setEntpolId(createEntPolicy("TEST", DateHelper.DATE_1900, DateHelper.DATE_3000));
        epd.setEntemppolStartDate(DateHelper.DATE_1900);
        epd.setEntemppolEndDate(DateHelper.DATE_3000);
        EntEmpPolicyAccess epa = new EntEmpPolicyAccess (getConnection());
        epa.insert(epd);
        // *** create future override
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride insWrks = new InsertWorkSummaryOverride(getConnection());
        insWrks.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insWrks.setEmpId(empId);
        insWrks.setStartDate(dateAfterTermDate);      insWrks.setEndDate(dateAfterTermDate);
        insWrks.setWrksFlag1("N");
        ovrBuilder.add(insWrks);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        InsertEmployeeBalanceOverride insEmpBal = new InsertEmployeeBalanceOverride(getConnection());
        insEmpBal.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmpBal.setEmpId(empId);
        insEmpBal.setStartDate(dateAfterTermDate);      insEmpBal.setEndDate(dateAfterTermDate);
        insEmpBal.setEmpbalActionSET();
        insEmpBal.setEmpbalValue(10);
        insEmpBal.setBalName("VACATION");
        ovrBuilder.add(insEmpBal);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        InsertEmployeeBalanceOverride insEmpBal2 = new InsertEmployeeBalanceOverride(getConnection());
        insEmpBal2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmpBal2.setEmpId(empId);
        insEmpBal2.setStartDate(dateBeforeTermDate);      insEmpBal2.setEndDate(dateBeforeTermDate);
        insEmpBal2.setEmpbalActionSET();
        insEmpBal2.setEmpbalValue(10);
        insEmpBal2.setBalName("SICK");
        ovrBuilder.add(insEmpBal2);
        ovrBuilder.execute(true , false); ovrBuilder.clear();

        WorkbrainUserAccess wba = new WorkbrainUserAccess (getConnection());
        WorkbrainUserData wud = wba.loadByEmpId(empId);
        assertNotNull(wud);

        // *** add user team privilege
        SecurityUser.addTeam(getConnection() , new Integer(0), new Integer(wud.getWbuId())
                             , new Integer(0), null, true, true,
                             SQLHelper.getMinDate(), SQLHelper.getMaxDate(), "","","","","","","","","","");
        // *** create some skd and default payroll data
        new CreateDefaultRecords(getConnection(), new int[] {empId}, termDate, dateAfterTermDate).execute(false);
        // *** terminate the guy
        InsertEmployeeOverride insEmp = new InsertEmployeeOverride(getConnection());
        insEmp.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        insEmp.setEmpId(empId);
        insEmp.setStartDate(termDate);      insEmp.setEndDate(DateHelper.DATE_3000);
        insEmp.setEmpTerminationDate(termDate);
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(false , false); ovrBuilder.clear();

        // *** run cleanup now
        WbiagTermEmpProcess.WbiagTermEmpProcessContext cont =
            new WbiagTermEmpProcess.WbiagTermEmpProcessContext();
        cont.conn = getConnection();
        cont.processEmpSkds = true;
        cont.processEntEmpPols = true;
        cont.processEmployeeJobs = true;
        cont.processOverrideTypes = new int[] {OverrideData.EMP_BAL_OVERRIDE_TYPE, OverrideData.WORK_SUMMARY_TYPE_START};
        cont.empBalsReset = new int[] {getCodeMapper().getBalanceByName("SICK").getBalId()};
        cont.processPayrollData = true;
        cont.processTeamHierarchy = true;
        cont.processWorkbrainUser = true;
        cont.processEmpStatus = true;
        cont.lastRun = DateHelper.addDays(DateHelper.getCurrentDate() , -1);
        cont.shouldCommit = false;

        WbiagTermEmpProcess pr = new WbiagTermEmpProcess();
        pr.process(cont);

        List jobs = eja.loadByEmpDate(empId , dateAfterTermDate);
        assertTrue("Should be no empjobs" , jobs.size() == 0);
        List pols = epa.loadRecordDataBetweenDates(new EntEmpPolicyData(), epa.ENT_EMP_POLICY_TABLE,
                                     "emp_id", empId, "ENTEMPPOL_START_DATE",
                                     dateAfterTermDate,dateAfterTermDate);
        assertTrue("Should be no entemppols" , pols.size() == 0);

        OverrideAccess oa = new OverrideAccess(getConnection());
        OverrideList ovrs = oa.loadOverridesByEmpIdsAndDateRange(new int[] {empId},
            dateAfterTermDate, dateAfterTermDate, new String[]{OverrideData.CANCELLED});
        assertTrue("Should be no affecting ovrs" , ovrs.size() == 0);

        EmployeeBalanceAccess eba = new EmployeeBalanceAccess(getConnection());
        List empBalVals = eba.loadByEmployeeAsOfDate(empId , termDate);
        double balVal = eba.getBalanceValueFromList(empBalVals , getCodeMapper().getBalanceByName("SICK").getBalId());
        assertTrue("Balance must be zero", balVal == 0);

        wud = wba.loadByEmpId(empId);
        assertTrue("User must be inactive", "N".equals(wud.getWbuActive()) );

        int homeTeamId = SecurityEmployee.getHomeTeamId(getConnection(),
            empId, new java.sql.Date( termDate.getTime())  ) ;
        assertTrue("Must be on terminated team",
                   homeTeamId == RuleHelper.getTerminatedWorkbrainTeamId(getCodeMapper())  );
        List wbuts = new WorkbrainUserTeamAccess(getConnection()).
            loadRecordDataBetweenDates(new WorkbrainUserTeamData(),
                                       WorkbrainUserTeamAccess.WBUT_TABLE,
                                     "emp_id", empId, "WBUT_START_DATE",
                                     dateAfterTermDate,dateAfterTermDate);
        assertTrue("Should be no wbuts" , wbuts.size() == 0);

        WorkSummaryAccess wsa = new WorkSummaryAccess (getConnection());
        List wrks = wsa.loadByEmpIdAndDateRange(empId, termDate, DateHelper.DATE_3000);
        assertTrue("Should have no work summaries" , wrks.size() == 0);

        EmployeeData edAfter = getEmployeeData(empId, dateAfterTermDate) ;
        assertTrue("Employee should be inactive", "I".equals(edAfter.getEmpStatus()) );

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


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
