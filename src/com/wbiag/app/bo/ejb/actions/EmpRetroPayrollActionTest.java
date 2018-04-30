package com.wbiag.app.bo.ejb.actions;

import java.util.*;

import org.apache.log4j.*;
import com.workbrain.app.workflow.*;
import com.wbiag.app.bo.*;
import com.workbrain.app.bo.*;
import junit.framework.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.ruleengine.*;

/**
 * ExportPayrollActionTest
 */
public class EmpRetroPayrollActionTest
    extends com.workbrain.app.bo.InteractionTestCase {
    private static Logger logger = Logger.getLogger(EmpRetroPayrollActionTest.class);
    private transient AbstractActionProcess node = null;

    private static final String TEST_USER1 = "WORKBRAIN";
    private BOFormInstance form = null;
    private Action action = null;

    private static final String DATE_FORMAT ="yyyy-MM-dd hh:mm:ss.sss";

    public EmpRetroPayrollActionTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(EmpRetroPayrollActionTest.class);
        return result;
    }

    public void setUp() throws Exception {
        super.setUp();
        node = new EmpRetroPayrollAction();
        // *** load workflow and form from DB
        form = createInteraction(
            TEST_USER1, "Employee Retro Adjust", "Employee Retro Adjust",
            false, false, new Date(), null);
        // *** set node parameters
        Map params = new Hashtable();
        action = super.getAction(params);
        action = new Action();

    }

    public void tearDown() throws Exception {
        super.tearDown();
        node = null;
        form = null;
        action = null;
    }

    /**
     * This test case runs Export payroll for one paygroup accross current pay period
     *
     * Expected result: branch "Success".
     */
    public void testEmpRetroPayrollOneRetro() throws Exception {
        ActionResponse result = null;
        this.createRetro(10038);
        // *** set action values


        // *** set form values
        Map pgValueMap = new HashMap();
        pgValueMap.put("value","10067");
        pgValueMap.put("valueId","10038");
        form.setFieldValue("emp_ids", pgValueMap);

        Map psysValueMap = new HashMap();
        psysValueMap.put("value","PEOPLESOFT");
        psysValueMap.put("valueId","1");
        form.setFieldValue("paysys_id", psysValueMap);

        form.setValue("use_pay_period", "true");
        form.setValue("not_final", "true");

        form.setValue("start_date", DateHelper.convertDateString(DateHelper.getCurrentDate(), DATE_FORMAT));


        form.setValue("write_to_file", "false");
        form.setValue("merge_files", "false");
        form.setValue("write_to_table", "false");


        // *** set output branch names
        Branch[] branches = super.getBranches(new String[] {"Success",
                                              "Failure"});
        // *** run test
        result = node.processObject(action, form, branches, null);
        System.out.println("Checking results...");
        assertNotNull("There was a result", result);
        assertEquals("Branch ",ActionResponse.BRANCH, result.getType());
        assertEquals("Success", ( (Branch) result.getValue()).getLabel());

        this.cancelRetro(10038);
    }

    public void createRetro(int empID){
        try{
            DBConnection c = getConnection();
            PayGroupAccess pga = new PayGroupAccess(c);
            PayGroupData pgd = pga.load(1);

            Date adjDate = pgd.getPaygrpAdjustDate();
            Date start = DateHelper.addDays(adjDate, -3);

            OverrideAccess oa = new OverrideAccess(c);
            OverrideData od = new OverrideData();
            od.setWbuName("WORKBRAIN");
            od.setOvrtypId(OverrideData.WORK_SUMMARY_TYPE_START);
            od.setEmpId(empID);
            od.setOvrStartDate(start);
            od.setOvrEndDate(start);
            od.setOvrStatus(OverrideData.APPLIED);
            od.setOvrNewValue("\"WRKS_FLAG2=Y");

            oa.insert(od);

            RuleEngine.runCalcGroup(c, empID, start,start);
        }
        catch (java.sql.SQLException e) {
            String message = "Retro adjustment not made...";
        }

    }

    public void cancelRetro(int empID){
        try{
            DBConnection c = getConnection();
            PayGroupAccess pga = new PayGroupAccess(c);
            PayGroupData pgd = pga.load(1);

            Date adjDate = pgd.getPaygrpAdjustDate();
            Date start = DateHelper.addDays(adjDate, -3);

            OverrideAccess oa = new OverrideAccess(c);

            oa.cancelByRangeAndType(empID, start, start, OverrideData.WORK_SUMMARY_TYPE_START,OverrideData.WORK_SUMMARY_TYPE_START, null);
        }
        catch (java.sql.SQLException e) {
            String message = "Retro adjustment not made...";
        }
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
