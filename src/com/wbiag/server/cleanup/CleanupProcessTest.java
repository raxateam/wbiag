package com.wbiag.server.cleanup ;

import java.util.*;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.modules.retailSchedule.db.ScheduleGroupAccess;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.wbiag.server.cleanup.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.security.team.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import com.wbiag.app.ta.quickrules.*;
import junit.framework.*;
import java.net.*;
import java.io.*;
/**
 * Test for CleanupProcessTest.
 */
public class CleanupProcessTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CleanupProcessTest.class);

    public CleanupProcessTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CleanupProcessTest.class);
        return result;
    }


    /**
     * @throws Exception
     */
    public void testEmployee() throws Exception {
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

        assertNotNull(EmployeeAccess.retrieveEmpIdByName(getConnection() , empName));

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setDeleteEmployeeWhereClause("emp_name = '" + empName + "'");
        ctx.setDeleteTeamWhereClause("1=2");
        ctx.setDeleteDeptWhereClause("1=2");
        ctx.setDeleteJobWhereClause("1=2");

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();

        assertNull(EmployeeAccess.retrieveEmpIdByName(getConnection() , empName));
    }

    /**
     * @throws Exception
     */
    public void testTeam() throws Exception {
        getConnection().setCodeMapper(getCodeMapper());
        String name = "TEST_" + System.currentTimeMillis();
        new WorkbrainTeamManager(getConnection()).addTeam(name , name, RuleHelper.getRootWorkbrainTeamId(getCodeMapper()) , 0);

        assertTrue(getCodeMapper().getWBTeamByName(name) != null);

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setDeleteEmployeeWhereClause("1=2");
        ctx.setDeleteTeamWhereClause("wbt_name ='" + name + "'");
        ctx.setDeleteDeptWhereClause("1=2");
        ctx.setDeleteJobWhereClause("1=2");

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();

        getCodeMapper().invalidateWorkbrainTeam(name);
        assertTrue(getCodeMapper().getWBTeamByName(name) == null);
    }

    /**
     * @throws Exception
     */
    public void testJob() throws Exception {
        JobAccess ja = new JobAccess(getConnection());
        getConnection().setCodeMapper(getCodeMapper());
        String jobName = "TEST_" + System.currentTimeMillis();
        ja.insertDefault(jobName , jobName, DateHelper.DATE_1900, DateHelper.DATE_3000);

        assertTrue(ja.loadRecordData(new JobData(), ja.JOB_TABLE , "job_name", jobName).size() == 1);

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setDeleteEmployeeWhereClause("1=2");
        ctx.setDeleteTeamWhereClause("1=2");
        ctx.setDeleteDeptWhereClause("1=2");
        ctx.setDeleteJobWhereClause("job_name = '" + jobName + "'");

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();

        assertTrue(ja.loadRecordData(new JobData(), ja.JOB_TABLE , "job_name", jobName).size() == 0);
    }

    /**
     * @throws Exception
     */
    public void testDept() throws Exception {
        DepartmentAccess da = new DepartmentAccess(getConnection());
        getConnection().setCodeMapper(getCodeMapper());
        String deptName = "TEST_" + System.currentTimeMillis();
        da.insertDefault(deptName , deptName, DateHelper.DATE_1900, DateHelper.DATE_3000);

        assertTrue(da.loadRecordData(new JobData(), da.DEPT_TABLE , "dept_name", deptName).size() == 1);

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setDeleteEmployeeWhereClause("1=2");
        ctx.setDeleteTeamWhereClause("1=2");
        ctx.setDeleteDeptWhereClause("dept_name = '" + deptName + "'");
        ctx.setDeleteJobWhereClause("1=2");

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();

        assertTrue(da.loadRecordData(new JobData(), da.DEPT_TABLE , "dept_name", deptName).size() == 0);
    }

    /**
     * @throws Exception
     */
    public void testCalcgrp() throws Exception {
        CalcGroupAccess access = new CalcGroupAccess(getConnection());
        String cgName = "TEST_" + System.currentTimeMillis();
        access.insertDefault(cgName , cgName);

        assertTrue(access.loadRecordData(new CalcGroupData(), access.CALC_GROUP_TABLE , "calcgrp_name", cgName).size() == 1);

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setDeleteEmployeeWhereClause("1=2");
        ctx.setDeleteTeamWhereClause("1=2");
        ctx.setDeleteDeptWhereClause("1=2");
        ctx.setDeleteJobWhereClause("1=2");
        ctx.setDeleteCalcgrpWhereClause("calcgrp_name = '" + cgName + "'");

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();

        assertTrue(access.loadRecordData(new CalcGroupData(), access.CALC_GROUP_TABLE , "calcgrp_name", cgName).size() == 0);
    }

    /**
     * @throws Exception
     */
    public void testWBGroup() throws Exception {
        RecordAccess  access = new RecordAccess(getConnection());
        String name = "TEST_" + System.currentTimeMillis();
        WorkbrainGroupData data = new WorkbrainGroupData();
        data.setWbgId(getConnection().getDBSequence("seq_wbg_id").getNextValue());
        data.setWbgName(name);
        access.insertRecordData(data , "WORKBRAIN_GROUP");

        assertTrue(access.loadRecordData(new CalcGroupData(), "WORKBRAIN_GROUP" , "wbg_name", name).size() == 1);

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setDeleteEmployeeWhereClause("1=2");
        ctx.setDeleteTeamWhereClause("1=2");
        ctx.setDeleteDeptWhereClause("1=2");
        ctx.setDeleteJobWhereClause("1=2");
        ctx.setDeleteWBGroupWhereClause("wbg_name = '" + name + "'");

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();

        assertTrue(access.loadRecordData(new CalcGroupData(), "WORKBRAIN_GROUP" , "wbg_name", name).size() == 0);
    }

    /**
     * @throws Exception
     */
    public void testGenericTable() throws Exception {
        ProjectAccess access = new ProjectAccess(getConnection());
        getConnection().setCodeMapper(getCodeMapper());
        String name = "TEST_" + System.currentTimeMillis();
        access.insertDefault(name , name, DateHelper.DATE_1900, DateHelper.DATE_3000);

        assertTrue(access.loadRecordData(new ProjectData(), access.PROJECT_TABLE , "proj_name", name).size() == 1);

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setDeleteEmployeeWhereClause("1=2");
        ctx.setDeleteTeamWhereClause("1=2");
        ctx.setDeleteDeptWhereClause("1=2");
        ctx.setDeleteJobWhereClause("1=2");
        CleanupProcess.GenericTableDelete gtd = new CleanupProcess.GenericTableDelete();
        gtd.tableName = "PROJECT";
        gtd.idCol = "proj_id";
        gtd.nameCol = "proj_name";
        gtd.whereClause = "proj_name = '" + name + "'";
        gtd.deleteTables =  StringHelper.detokenizeStringAsList(
            "docket,emp_def_lab,project_rdr_grp,shift_pattern_shift_labor,work_detail_adjust,work_detail,project" ,
            ",");
        ctx.addToGenericTableDeletes(gtd);

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();

        assertTrue(access.loadRecordData(new ProjectData(), access.PROJECT_TABLE , "proj_name", name).size() == 0);
    }

    /**
     * @throws Exception
     */
    public void testConfirmationOnly() throws Exception {

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setConfirmationOnly(true);
        ctx.setDeleteEmployeeWhereClause("emp_id > 10000");
        ctx.setDeleteTeamWhereClause("wbt_id > 10000");
        ctx.setDeleteDeptWhereClause("dept_id > 10000");
        ctx.setDeleteJobWhereClause("job_id > 10000");
        ctx.setDeleteCalcgrpWhereClause("calcgrp_id > 10000");
        ctx.setDeleteWBGroupWhereClause("wbg_id > 10000");
        ctx.setDeleteOverrideWhereClause("ovr_id > 10000");

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();

        assertEquals(0 , dst.getTotalDeletedCount());
    }

    /**
     * @throws Exception
     */
    public void xScheduleGroup() throws Exception {

        ScheduleGroupAccess access = new ScheduleGroupAccess(getConnection());
        ScheduleGroupData data = new ScheduleGroupData();
        int id = getConnection().getDBSequence(ScheduleGroupData.SCHEDULE_GROUP_PRI_KEY_SEQ).getNextValue();
        String name = "TEST_GROUP";
        String desc = "TEST_GROUP";
        data.assignIsNew(true);
        data.setSkdgrpId(new Integer(id));
        data.setSkdgrpName(name);
        data.setSkdgrpDesc(desc);
        data.setClnttypId(1);
        data.setSkdgrpParentId(new Integer(1));
        data.setVoltypId(new Integer(1000));
        data.setSkdgrpStaffStd(1);
        data.setSkdgrpFcastMtd(1);
        data.setSkdgrpBudgTyp(1);
        data.setSkdgrpFcastInt(1);
        data.setSkdgrpStartdow(1);
        data.setSkdgrpPctCallTg(0);
        data.setSkdgrpAnsTimeTg(0);
        data.setSkdgrpAnsTimeUn(0);
        data.setSkdgrpAnsSpdTg(0);
        data.setSkdgrpAnsSpdUn(0);
        data.setSkdgrpOwnsAttrib(0);
        data.setSkdgrpCalcvarstf(0);
        data.setSkdgrpAhtUnits(0);
        data.setSkdgrpFcstInhtyp(0);
        data.setSkdgrpFcstInhpct(0);
        data.setSkdgrpBudgVal1(0);
        data.setSkdgrpBudgVal2(0);
        data.setSkdgrpOptchkskd(0);
        data.setSkdgrpVolsub(0);
        data.setSkdgrpScriptName("shift_REI.mos");
        data.setSkdgrpFcstSubloc(0);
        data.setSkdgrpDefEngine(0);
        data.setSkdgrpHistdatSub(0);
        data.setSkdgrpInlevmxId(null);
        data.setSkdgrpOutlevmxId(null);
        data.setSkdgrpFcastWeeks(0);
        data.setSkdgrpHrsOpSrc(0);
        data.setSkdgrpAdjFcast(0);
        data.setSkdgrpSortOrder(0);
        data.setSkdgrpSalesBased(0);
        data.setSkdgrpCustHelp(null);
        data.setSkdgrpCalType(0);
        data.setFcastgrpId(null);
        data.setSkdgrpIntrnlType(0);
        data.setWbtId(1);
        data.setSkdgrpHropstrOfs(2);
        data.setSkdgrpHropendOfs(2);
        data.setSkdgrpStfstrOfs(2);
        data.setSkdgrpStfendOfs(2);
        data.setSkdgrpClientkey("1234567890");
        //data.setStfrrlId(rr.getStfrrlId());
        access.insert(data);
        List l = access.loadRecordData(new ScheduleGroupData(), "SO_SCHEDULE_GROUP", "skdgrp_id = " + id);
        assertEquals(1, l.size());

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setDeleteScheduleGroupWhereClause("skdgrp_id = " + id);

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();
        l = access.loadRecordData(new ScheduleGroupData(), "SO_SCHEDULE_GROUP", "skdgrp_id = " + id);
        assertEquals(0, l.size());
    }

    private CleanupProcess.CleanupProcessContext createDefaultContext() throws Exception {
        CleanupProcess.CleanupProcessContext ctx = new CleanupProcess.CleanupProcessContext();
        ctx.setDBConnection(getConnection());
        ctx.setClientId("1");
        ctx.setShouldCommit(false);
        // *** load default from CleanupProcess.properties
        URL propsFile = this.getClass().getResource("CleanupProcess.properties");
        Properties props = new Properties();
        props.load(propsFile.openStream());
        ctx.setDeleteEmployeeTables(StringHelper.detokenizeStringAsList(
            props.getProperty(CleanupProcess.PROP_DELETE_EMPLOYEE_TABLES) , ","));
        ctx.setDeleteTeamTables(StringHelper.detokenizeStringAsList(
            props.getProperty(CleanupProcess.PROP_DELETE_TEAM_TABLES) , ","));
        ctx.setDeleteDeptTables(StringHelper.detokenizeStringAsList(
            props.getProperty(CleanupProcess.PROP_DELETE_DEPT_TABLES) , ","));
        ctx.setDeleteJobTables(StringHelper.detokenizeStringAsList(
            props.getProperty(CleanupProcess.PROP_DELETE_JOB_TABLES) , ","));
        ctx.setDeleteCalcgrpTables(StringHelper.detokenizeStringAsList(
            props.getProperty(CleanupProcess.PROP_DELETE_CALCGRP_TABLES) , ","));
        ctx.setDeleteWBGroupTables(StringHelper.detokenizeStringAsList(
            props.getProperty(CleanupProcess.PROP_DELETE_WBGROUP_TABLES) , ","));
        ctx.setDeleteScheduleGroupTables(StringHelper.detokenizeStringAsList(
            props.getProperty(CleanupProcess.PROP_DELETE_SCHEDULE_GROUP_TABLES) , ","));
        return ctx;
    }

    /**
     * @throws Exception
     */
    public void testOverride() throws Exception {
        final int empId = 20;
        final Date start = DateHelper.getCurrentDate();
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        InsertEmployeeOverride ins = new InsertEmployeeOverride(getConnection());
        int ovrId = getConnection().getDBSequence("seq_ovr_id").getNextValue();
        ins.setOverrideId(ovrId);
        ins.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins.setEmpId(empId);
        ins.setStartDate(start);      ins.setEndDate(start);
        ins.setEmpVal17("Y");

        ovrBuilder.add(ins);
        ovrBuilder.execute(false , false);

        OverrideAccess oa = new OverrideAccess(getConnection());
        OverrideData od = oa.load(ovrId);
        assertTrue(od != null);

        CleanupProcess.CleanupProcessContext ctx = createDefaultContext();
        ctx.setDeleteEmployeeWhereClause("1=2");
        ctx.setDeleteTeamWhereClause("1=2");
        ctx.setDeleteDeptWhereClause("1=2");
        ctx.setDeleteJobWhereClause("1=2");
        ctx.setDeleteOverrideWhereClause("ovr_id = " + ovrId);

        CleanupProcess dst = new CleanupProcess(ctx);
        dst.execute();

        od = oa.load(ovrId);
        assertTrue(od == null);
    }

    private String readInputStream(InputStream is){
        try {
            Reader fr = new InputStreamReader(is);
            StringWriter sw = new StringWriter();
            char[] ca = new char[500];
            while(true){
                int i = fr.read(ca);
                if(i==-1) break;
                sw.write(ca,0,i);
            }
            String s = sw.getBuffer().toString();
            fr.close();
            return s;
        } catch (IOException e) {
            throw new NestedRuntimeException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
