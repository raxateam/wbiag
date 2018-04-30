package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.model.EmployeeScheduleData;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 * Test for IsEmpScheduleDtlPropertyGenericTest.
 */
public class IsEmpScheduleDtlPropertyGenericTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(IsEmpScheduleDtlPropertyGenericTest.class);

    public IsEmpScheduleDtlPropertyGenericTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsEmpScheduleDtlPropertyGenericTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void test1() throws Exception {

        Date start = DateHelper.getCurrentDate();
        final int empId = 11;
        final int actId = 10001; final String actName="WORK";

        // Insert an override.
        OverrideBuilder ovrBuilder = new OverrideBuilder(getConnection());
        ovrBuilder.setCreatesDefaultRecords(true);

        EmployeeScheduleData esd = getEmployeeScheduleData(empId, start);
        esd.setCodeMapper(getCodeMapper());
        
        InsertScheduleDetailOverride ins0 = new InsertScheduleDetailOverride();
        ins0.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
        ins0.setEmpId(empId);
        ins0.setStartDate(start);
        ins0.setEndDate(start);
        WorkDetailData wd = new WorkDetailData();
        wd.setEmpId(empId);
        wd.setCodeMapper(getCodeMapper() );
        wd.setActId(actId );
        wd.setWrkdWorkDate(start) ;
        wd.setWrkdStartTime(esd.getEmpskdActStartTime());
        wd.setWrkdEndTime(esd.getEmpskdActEndTime());
        ins0.setWorkDetailData(wd);
        EmployeeScheduleData esd0 = new EmployeeScheduleData();
        esd0.setEmpId(empId);
        esd0.setWorkDate(esd.getWorkDate() );
        esd0.setEmpskdActStartTime(esd.getEmpskdActStartTime() );
        esd0.setEmpskdActEndTime(esd.getEmpskdActEndTime() );
        ins0.setEmployeeScheduleData(esd0);
        ovrBuilder.add(ins0);

        ovrBuilder.add(ins0);
        ovrBuilder.execute(true , false);


        // *** create the condition
        Condition cond = new IsEmpScheduleDtlPropertyGeneric();

        Parameters params = new Parameters();
        params.addParameter(IsEmpScheduleDtlPropertyGeneric.PARAM_EXPRESSION_STRING, "eschdActName[IN]WORK");

        assertConditionTrue(empId, start, cond, params);


    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}
