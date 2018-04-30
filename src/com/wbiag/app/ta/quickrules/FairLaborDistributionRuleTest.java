package com.wbiag.app.ta.quickrules;

import java.util.*;

import org.apache.log4j.BasicConfigurator;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;

import junit.framework.*;
/**
 * Test for FairLaborDistributionRuleTest.
 */
public class FairLaborDistributionRuleTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(FairLaborDistributionRuleTest.class);

    public FairLaborDistributionRuleTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
    	BasicConfigurator.configure();
        TestSuite result = new TestSuite();
        result.addTestSuite(FairLaborDistributionRuleTest.class);
        return result;
    }

    /**
     * tue in case 0001
     * @throws Exception
     */
    public void test1() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);
        final int projId1 = 0; final String projName1 = "0";
        final int projId2 = 1201; final String projName2 = "AUTOPROJECT";
        final int projId3 = 1202; final String projName3 = "AUTOPROJECT2";
        // *** create the rule
        Rule rule = new FairLaborDistributionRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_TIME_CODES_INCLUSIVE , "true");
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_HOUR_TYPES , "REG");
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_HOUR_TYPES_INCLUSIVE , "true");

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeDefaultLaborOverride insEmp = new InsertEmployeeDefaultLaborOverride (getConnection());
        insEmp.setWbuNameBoth("JUNIT", "JUNIT");;
        insEmp.setEmpId(empId);
        insEmp.setStartDate(DateHelper.DATE_1900);      insEmp.setEndDate(DateHelper.DATE_3000);
        List edlaList = new ArrayList();
        EmployeeDefaultLaborData edla = new EmployeeDefaultLaborData();
        edla.setProjId(projId1);
        edla.setTcodeId(1);
        edla.setHtypeId(1);
        edla.setDockId(0);
        edla.setDeptId(0);
        edla.setJobId(0);
        edla.setEdlaPercentage(25);
        edlaList.add(edla);
        for (int i = 1; i <= 10; i++) {
            edla.setProperty("edlaUdf" + i, "");
            edla.setProperty("edlaFlag" + i, "");
        }

        EmployeeDefaultLaborData edla2 = edla.duplicate();
        edla2.setProjId(projId2);
        edla2.setEdlaPercentage(25);
        edlaList.add(edla2);
        EmployeeDefaultLaborData edla3 = edla.duplicate();
        edla3.setProjId(projId3);
        edla3.setEdlaPercentage(50);
        edlaList.add(edla3);
        insEmp.setEmployeeDefaultLaborList(edlaList);
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(false , false); ovrBuilder.clear();
        RuleEngine.runCalcGroup(getConnection() , empId, mon, mon , false);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);      ins.setEndDate(mon);
        Datetime sh1Start = DateHelper.addMinutes(mon, 8*60);
        Datetime sh1End = DateHelper.addMinutes(mon, 17*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride ins2 = new InsertWorkSummaryOverride(getConnection());
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(mon);      ins2.setEndDate(mon);
        Datetime clk1On = DateHelper.addMinutes(mon, 6*60);
        Datetime clk1Off = DateHelper.addMinutes(mon, 17*60);
        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins2.setWrksClocks(clks);
        ovrBuilder.add(ins2);

        InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride (getConnection());
        ins3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins3.setWbuNameBoth("JUNIT", "JUNIT");
        ins3.setWrkdTcodeName("BRK");
        ins3.setEmpId(empId);
        ins3.setStartDate(mon);      ins3.setEndDate(mon);
        ins3.setStartTime(DateHelper.addMinutes(mon, 12*60));
        ins3.setEndTime(DateHelper.addMinutes(mon, 13*60));
        ovrBuilder.add(ins3);

        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertRuleApplied(empId, mon, rule);


        WorkDetailList wdl = getWorkDetailsForDate(empId , mon);
        //System.out.println(wdl.toDescription() );
        assertEquals(150 ,  wdl.getMinutes(null, null, "WRK", true, "REG", true, "wrkdProjName=" + projName1 , false));
        assertEquals(150 ,  wdl.getMinutes(null, null, "WRK", true, "REG", true, "wrkdProjName=" + projName2 , false));
        assertEquals(300 ,  wdl.getMinutes(null, null, "WRK", true, "REG", true, "wrkdProjName=" + projName3 , false)) ;

    }

    /**
     * tue in case 0002
     * @throws Exception
     */
    public void test2() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);
        final int projId1 = 0; final String projName1 = "0";
        final int projId2 = 1201; final String projName2 = "AUTOPROJECT";
        final int projId3 = 1202; final String projName3 = "AUTOPROJECT2";
        // *** create the rule
        Rule rule = new FairLaborDistributionRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_TIME_CODES_INCLUSIVE , "true");
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_HOUR_TYPES , "REG");
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_HOUR_TYPES_INCLUSIVE , "true");

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeDefaultLaborOverride insEmp = new InsertEmployeeDefaultLaborOverride (getConnection());
        insEmp.setWbuNameBoth("JUNIT", "JUNIT");;
        insEmp.setEmpId(empId);
        insEmp.setStartDate(DateHelper.DATE_1900);      insEmp.setEndDate(DateHelper.DATE_3000);
        List edlaList = new ArrayList();
        EmployeeDefaultLaborData edla = new EmployeeDefaultLaborData();
        edla.setProjId(projId1);
        edla.setTcodeId(1);
        edla.setHtypeId(1);
        edla.setDockId(0);
        edla.setDeptId(0);
        edla.setJobId(0);
        edla.setEdlaPercentage(40);
        edlaList.add(edla);
        for (int i = 1; i <= 10; i++) {
            edla.setProperty("edlaUdf" + i, "");
            edla.setProperty("edlaFlag" + i, "");
        }

        EmployeeDefaultLaborData edla2 = edla.duplicate();
        edla2.setProjId(projId2);
        edla2.setEdlaPercentage(40);
        edlaList.add(edla2);
        EmployeeDefaultLaborData edla3 = edla.duplicate();
        edla3.setProjId(projId3);
        edla3.setEdlaPercentage(20);
        edlaList.add(edla3);
        insEmp.setEmployeeDefaultLaborList(edlaList);
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(false , false); ovrBuilder.clear();
        RuleEngine.runCalcGroup(getConnection() , empId, mon, mon , false);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);      ins.setEndDate(mon);
        Datetime sh1Start = DateHelper.addMinutes(mon, 6*60);
        Datetime sh1End = DateHelper.addMinutes(mon, 18*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride ins2 = new InsertWorkSummaryOverride(getConnection());
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(mon);      ins2.setEndDate(mon);
        Datetime clk1On = DateHelper.addMinutes(mon, 2*60);
        Datetime clk1Off = DateHelper.addMinutes(mon, 4*60);
        Datetime clk2On = DateHelper.addMinutes(mon, 6*60);
        Datetime clk2Off = DateHelper.addMinutes(mon, 18*60);

        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off, clk2On , clk2Off);
        ins2.setWrksClocks(clks);
        ovrBuilder.add(ins2);

        /*InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride (getConnection());
        ins3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins3.setWbuNameBoth("JUNIT", "JUNIT");
        ins3.setWrkdTcodeName("BRK");
        ins3.setEmpId(empId);
        ins3.setStartDate(mon);      ins3.setEndDate(mon);
        ins3.setStartTime(DateHelper.addMinutes(mon, 12*60));
        ins3.setEndTime(DateHelper.addMinutes(mon, 13*60));
        ovrBuilder.add(ins3);*/

        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertRuleApplied(empId, mon, rule);


        WorkDetailList wdl = getWorkDetailsForDate(empId , mon);
        //System.out.println(wdl.toDescription() );
        assertEquals(336 ,  wdl.getMinutes(null, null, "WRK", true, "REG", true, "wrkdProjName=" + projName1 , false));
        assertEquals(336 ,  wdl.getMinutes(null, null, "WRK", true, "REG", true, "wrkdProjName=" + projName2 , false));
        assertEquals(168 ,  wdl.getMinutes(null, null, "WRK", true, "REG", true, "wrkdProjName=" + projName3 , false)) ;
    }

    /**
     * tue in case 0003
     * @throws Exception
     */
    public void test3() throws Exception {

        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        final int empId = 11;
        Date mon = DateHelper.nextDay(DateHelper.addDays(DateHelper.getCurrentDate(), -7), "MON");
        Date tue = DateHelper.addDays(mon , 1);
        final int projId1 = 0; final String projName1 = "0";
        final int projId2 = 1201; final String projName2 = "AUTOPROJECT";
        final int projId3 = 1202; final String projName3 = "AUTOPROJECT2";
        final int projId4 = 1203; final String projName4 = "AUTOPROJECT_REPORT";
        // *** create the rule
        Rule rule = new FairLaborDistributionRule();
        Parameters ruleparams = new Parameters();
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_TIME_CODES, "WRK");
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_TIME_CODES_INCLUSIVE , "true");
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_HOUR_TYPES , "REG");
        ruleparams.addParameter(FairLaborDistributionRule.PARAM_HOUR_TYPES_INCLUSIVE , "true");

        clearAndAddRule(empId , mon , rule , ruleparams);

        InsertEmployeeDefaultLaborOverride insEmp = new InsertEmployeeDefaultLaborOverride (getConnection());
        insEmp.setWbuNameBoth("JUNIT", "JUNIT");;
        insEmp.setEmpId(empId);
        insEmp.setStartDate(DateHelper.DATE_1900);      insEmp.setEndDate(DateHelper.DATE_3000);
        List edlaList = new ArrayList();
        EmployeeDefaultLaborData edla = new EmployeeDefaultLaborData();
        edla.setProjId(projId1);
        edla.setTcodeId(1);
        edla.setHtypeId(1);
        edla.setDockId(0);
        edla.setDeptId(0);
        edla.setJobId(0);
        edla.setEdlaPercentage(25);
        edlaList.add(edla);
        for (int i = 1; i <= 10; i++) {
            edla.setProperty("edlaUdf" + i, "");
            edla.setProperty("edlaFlag" + i, "");
        }

        EmployeeDefaultLaborData edla2 = edla.duplicate();
        edla2.setProjId(projId2);
        edla2.setEdlaPercentage(25);
        edlaList.add(edla2);
        EmployeeDefaultLaborData edla3 = edla.duplicate();
        edla3.setProjId(projId3);
        edla3.setEdlaPercentage(50);
        edlaList.add(edla3);
        insEmp.setEmployeeDefaultLaborList(edlaList);
        ovrBuilder.add(insEmp);
        ovrBuilder.execute(false , false); ovrBuilder.clear();
        RuleEngine.runCalcGroup(getConnection() , empId, mon, mon , false);

        InsertEmployeeScheduleOverride ins = new InsertEmployeeScheduleOverride(getConnection());
        ins.setWbuNameBoth("JUNIT", "JUNIT");;
        ins.setOvrType(OverrideData.SCHEDULE_SCHEDTIMES_TYPE);
        ins.setEmpId(empId);
        ins.setStartDate(mon);      ins.setEndDate(mon);
        Datetime sh1Start = DateHelper.addMinutes(mon, 8*60);
        Datetime sh1End = DateHelper.addMinutes(mon, 17*60);
        ins.setEmpskdActStartTime(sh1Start);
        ins.setEmpskdActEndTime(sh1End);
        ovrBuilder.add(ins);

        InsertWorkSummaryOverride ins2 = new InsertWorkSummaryOverride(getConnection());
        ins2.setWbuNameBoth("JUNIT", "JUNIT");
        ins2.setEmpId(empId);
        ins2.setStartDate(mon);      ins2.setEndDate(mon);
        Datetime clk1On = DateHelper.addMinutes(mon, 6*60);
        Datetime clk1Off = DateHelper.addMinutes(mon, 17*60);

        String clks = createWorkSummaryClockStringForOnOffs(clk1On , clk1Off);
        ins2.setWrksClocks(clks);
        ovrBuilder.add(ins2);

        InsertWorkDetailOverride ins3 = new InsertWorkDetailOverride (getConnection());
        ins3.setOvrType(OverrideData.WORK_DETAIL_TYPE_START );
        ins3.setWbuNameBoth("JUNIT", "JUNIT");
        ins3.setWrkdTcodeName("BRK");
        ins3.setEmpId(empId);
        ins3.setStartDate(mon);      ins3.setEndDate(mon);
        ins3.setStartTime(DateHelper.addMinutes(mon, 12*60));
        ins3.setEndTime(DateHelper.addMinutes(mon, 13*60));
        ovrBuilder.add(ins3);

        InsertWorkDetailOverride ins4 = new InsertWorkDetailOverride (getConnection());
        ins4.setOvrType(OverrideData.POSTCALC_WORKDETAIL_TYPE_START  );
        ins4.setWbuNameBoth("JUNIT", "JUNIT");
        ins4.setWrkdProjName(projName4 );
        ins4.setEmpId(empId);
        ins4.setStartDate(mon);      ins4.setEndDate(mon);
        ins4.setStartTime(DateHelper.addMinutes(mon, 9*60));
        ins4.setEndTime(DateHelper.addMinutes(mon, 11*60));
        ovrBuilder.add(ins4);


        ovrBuilder.execute(true , false); ovrBuilder.clear();

        assertRuleApplied(empId, mon, rule);


        WorkDetailList wdl = getWorkDetailsForDate(empId , mon);
        //System.out.println(wdl.toDescription() );
        assertEquals(150 ,  wdl.getMinutes(null, null, "WRK", true, "REG", true, "wrkdProjName=" + projName1 , false));
        assertEquals(30 ,  wdl.getMinutes(null, null, "WRK", true, "REG", true, "wrkdProjName=" + projName2 , false));
        assertEquals(300 ,  wdl.getMinutes(null, null, "WRK", true, "REG", true, "wrkdProjName=" + projName3 , false)) ;
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

