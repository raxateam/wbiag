package com.wbiag.app.ta.ruleengine;

import java.util.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.security.team.*;
import com.workbrain.test.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.tool.security.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Test for CDataEventTempTeamTest.
 * @deprecated Core as of 5.0, wbt_id is now in work_Detail table and this is handled by core rule engine
 */
public class CDataEventTempTeamTest extends DataEventTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CDataEventTempTeamTest.class);

    public CDataEventTempTeamTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CDataEventTempTeamTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void xTempTeamDept() throws Exception {

        TestUtil.getInstance().setVarTemp("/" + OverrideAccess.REG_TEMP_TEAM_ASSIGN_BY_WORK_DETAIL_FIELD ,
                                          "WRKD_DEPT_NAME");
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventTempTeam");

        final int empId = 10;
        Date start = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;

        String teamName = "TEST_" + System.currentTimeMillis();
        WorkbrainTeamManager wtm = new WorkbrainTeamManager(getConnection());
        wtm.addTeam(teamName, teamName,
                    RuleHelper.getRootWorkbrainTeamId(getCodeMapper()) , 0);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkSummaryOverride ins = new InsertWorkSummaryOverride(getConnection());
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);
        ins.setEndDate(start);
        List clocks = new ArrayList();
        Clock clk = new Clock();
        clk.setClockDate(DateHelper.addMinutes(start, 9 * 60));
        clk.setClockType(Clock.TYPE_DEPT);
        clk.setClockData(Clock.CLOCKDATA_DEPARTMENT + "=" + teamName);
        clocks.add(clk);
        ins.setWrksClocks(Clock.createStringFromClockList(clocks));
        ovrBuilder.add(ins);

        ovrBuilder.execute(true, false);

        List tempTeams = SecurityEmployee.getTempTeams(getConnection() , empId,
            new java.sql.Date(start.getTime()) );
        assertTrue(tempTeams.size() > 0);
        assertTrue(tempTeams.contains(teamName) );

    }

    /**
     * @throws Exception
     */
    public void xTempTeamUdf() throws Exception {

        TestUtil.getInstance().setVarTemp("/" + OverrideAccess.REG_TEMP_TEAM_ASSIGN_BY_WORK_DETAIL_FIELD ,
                                          "WRKD_UDF1");
        setDataEventClassPath("com.wbiag.app.ta.ruleengine.CDataEventTempTeam");

        final int empId = 15;
        Date day1 = DateHelper.nextDay(DateHelper.getCurrentDate() , "Mon") ;
        Date day2 = DateHelper.addDays(day1 , 1);

        String teamName = "TEST_" + System.currentTimeMillis();
        WorkbrainTeamManager wtm = new WorkbrainTeamManager(getConnection());
        wtm.addTeam(teamName, teamName,
                    RuleHelper.getRootWorkbrainTeamId(getCodeMapper()) , 0);

        String teamName2 = "TEST2_" + System.currentTimeMillis();
        wtm.addTeam(teamName2, teamName2,
                    RuleHelper.getRootWorkbrainTeamId(getCodeMapper()) , 0);

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        InsertWorkDetailOverride ins = new InsertWorkDetailOverride(getConnection());
        ins.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(day1);
        ins.setEndDate(day1);
        ins.setWrkdUdf1(teamName);
        ovrBuilder.add(ins);

        InsertWorkDetailOverride ins2 = new InsertWorkDetailOverride(getConnection());
        ins2.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins2.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins2.setEmpId(empId);
        ins2.setStartDate(day2);
        ins2.setEndDate(day2);
        ins2.setStartTime(DateHelper.addMinutes(day2 , 10*60));
        ins2.setStartTime(DateHelper.addMinutes(day2 , 11*60));
        ins2.setWrkdUdf1(teamName2);
        ovrBuilder.add(ins2);

        InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride(getConnection());
        ins3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START);
        ins3.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins3.setEmpId(empId);
        ins3.setStartDate(day2);
        ins3.setEndDate(day2);
        ins3.setStartTime(DateHelper.addMinutes(day2 , 14*60));
        ins3.setStartTime(DateHelper.addMinutes(day2 , 15*60));
        ins3.setWrkdUdf1(teamName);
        ovrBuilder.add(ins3);

        ovrBuilder.execute(true, false);

        List tempTeams = SecurityEmployee.getTempTeams(getConnection() , empId,
            new java.sql.Date(day1.getTime()) );
        assertTrue(tempTeams.size() > 0);
        assertTrue(tempTeams.contains(teamName) );

        tempTeams = SecurityEmployee.getTempTeams(getConnection() , empId,
                    new java.sql.Date(day2.getTime()) );
        assertTrue(tempTeams.size() > 0);
        assertTrue(tempTeams.contains(teamName) );
        assertTrue(tempTeams.contains(teamName2) );

    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
