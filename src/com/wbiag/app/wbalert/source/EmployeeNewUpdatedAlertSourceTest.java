package com.wbiag.app.wbalert.source ;

import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbalert.*;
import com.workbrain.app.wbalert.db.*;
import com.workbrain.app.wbalert.model.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import com.workbrain.test.*;
import junit.framework.*;

public class EmployeeNewUpdatedAlertSourceTest extends TestCaseHW {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(EmployeeNewUpdatedAlertSourceTest.class);

    public EmployeeNewUpdatedAlertSourceTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(EmployeeNewUpdatedAlertSourceTest.class);
        return result;
    }

    /**
     */
    public void testAlert() throws Exception {
        final int alertId = 10021;
        CodeMapper codeMapper = CodeMapper.createCodeMapper(getConnection());
        // *** create emp
        String empName = "TEST_" + System.currentTimeMillis();
        HRRefreshProcessor pr = new HRRefreshProcessor(getConnection());
        HRRefreshData hrdata = new HRRefreshData(getConnection() , empName);
        hrdata.setEmpFirstname(empName);
        hrdata.setEmpLastname(empName);
        hrdata.setEmpSin(empName);
        pr.addHRRefreshData(hrdata) ;
        pr.process(true);
        EmployeeAccess ea = new EmployeeAccess(getConnection() , codeMapper);
        EmployeeData ed = ea.loadByName(empName , DateHelper.getCurrentDate());
        assertNotNull(ed);
        int empId = ed.getEmpId();
        // *** update emp
		OverrideBuilder ob = new OverrideBuilder(getConnection());
        InsertEmployeeOverride ovr = new InsertEmployeeOverride(getConnection());
        ovr.setWbuNameBoth("TEST" , "TEST");
        ovr.setEmpId(empId);
        ovr.setStartDate(DateHelper.DATE_1900);
        ovr.setEndDate(DateHelper.DATE_3000);
        ovr.setEmpFlag2("Y");
        ob.add(ovr);
        ob.execute(false, false);
        assertEquals(1 , ob.getOverridesProcessed().size()  );

        WBAlertAccess access = new WBAlertAccess(getConnection());
        WBAlertData data = access.load(alertId);
        HashMap params = new HashMap();
        params.put(EmployeeNewUpdatedAlertSource.PARAM_CHECK_NEW_EMPS, "Y");
        params.put(EmployeeNewUpdatedAlertSource.PARAM_CHECK_UPDATED_EMPS, "Y");
        params.put(EmployeeNewUpdatedAlertSource.PARAM_LOOK_BACK_DAYS, "1");
        params.put(EmployeeNewUpdatedAlertSource.PARAM_EMPLOYEE_COLUMNS, EmployeeNewUpdatedAlertSource.getAllEmployeeColumnString());
        WBAlertProcess apr = new WBAlertProcess(getConnection() , alertId, params);
        apr.execute();
        // *** must be only sent to workbrain
        assertEquals(1 , apr.getSentUserCount());
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

