
package com.wbiag.app.bo.ejb.actions;

import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;

import junit.framework.TestSuite;

import com.wbiag.app.bo.InteractionTestCase;
import com.workbrain.app.bo.BOFormInstance;
import com.workbrain.app.workflow.AbstractActionProcess;
import com.workbrain.app.workflow.Action;
import com.workbrain.app.workflow.ActionResponse;
import com.workbrain.app.workflow.Branch;

/**
 * Title: HighVolumePayrollExportActionTest.java <br>
 * Description: <br>
 * 
 * Created: May 25, 2005
 * @author cleigh
 * <p>
 * Revision History <br>
 * May 25, 2005 -  <br>
 * <p>
 * */
public class HighVolumePayrollExportActionTest extends InteractionTestCase {
    
    private static Logger logger = Logger.getLogger(HighVolumePayrollExportActionTest.class);
    private AbstractActionProcess node = null;

    private static final String TEST_USER1 = "WORKBRAIN";
    private BOFormInstance form = null;
    private Action action = null;
    
    public HighVolumePayrollExportActionTest(String testName) throws Exception {
        super(testName);
    }
    
    public void setUp() throws Exception {
        super.setUp();
        node = new HighVolumePayrollExportAction();
        // *** load workflow and form from DB
        form = createInteraction(
            TEST_USER1, "HighVolume Payroll Export", "HighVolume Payroll Export",
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
    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(HighVolumePayrollExportActionTest.class);
        return result;
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(HighVolumePayrollExportActionTest.class);
    }

    /*
     * Class under test for ActionResponse processObject(Action, WBObject, Branch[], ActionResponse)
     */
    public void testProcessObjectActionByPayGroup() throws Exception {
        ActionResponse result = null;

        // *** set action values
        
        //Params not used in this test (defaut values used instead):
        //HighVolumePayrollExportAction.PARAM_EMP_ID
        //HighVolumePayrollExportAction.PARAM_ALL_READY_WHERE
        //HighVolumePayrollExportAction.PARAM_START_DATE
        //HighVolumePayrollExportAction.PARAM_END_DATE
        //HighVolumePayrollExportAction.PARAM_LOOK_BACK
        //HighVolumePayrollExportAction.PARAM_CONT_ON_UNAUTH
        //HighVolumePayrollExportAction.PARAM_TERM_ADD_DAYS


        // *** set form values
        Map pgValueMap = new HashMap();
        pgValueMap.put("value","2");
        pgValueMap.put("valueId","2");
        form.setFieldValue(HighVolumePayrollExportAction.PARAM_PAYGRP_ID, pgValueMap);
        
        Map psValueMap = new HashMap();
        psValueMap.put("value", "PEOPLESOFT");
        psValueMap.put("valueId", "1");
        form.setFieldValue(HighVolumePayrollExportAction.PARAM_PAYSYS_ID, psValueMap);
        
        form.setValue(HighVolumePayrollExportAction.PARAM_CYCLE, "Regular");

        form.setValue(HighVolumePayrollExportAction.PARAM_RESET, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_USE_PP, "true");
        form.setValue(HighVolumePayrollExportAction.PARAM_ALL_READY, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_ADJ_DATE, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_WRITE_FILE, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_MERGE_FILE, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_WRITE_TABLE, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_CKUNAUTH, "false");
        
        form.setValue(HighVolumePayrollExportAction.PARAM_SUBMITTED_BY, TEST_USER1);
        
//      *** set output branch names
        Branch[] branches = super.getBranches(new String[] {"Success",
                                              "Failure"});
        // *** run test

        result = node.processObject(action, form, branches, null);
        assertNotNull(result);
        assertEquals(ActionResponse.BRANCH, result.getType());
        assertEquals("Success", ( (Branch) result.getValue()).getLabel());

    }
    
    public void testProcessObjectActionByEmpId() throws Exception {
        ActionResponse result = null;

        // *** set action values
        
        //Params not used in this test (defaut values used instead):
        //HighVolumePayrollExportAction.PARAM_PAYGRP_ID
        //HighVolumePayrollExportAction.PARAM_ALL_READY_WHERE
        //HighVolumePayrollExportAction.PARAM_START_DATE
        //HighVolumePayrollExportAction.PARAM_END_DATE
        //HighVolumePayrollExportAction.PARAM_LOOK_BACK
        //HighVolumePayrollExportAction.PARAM_CONT_ON_UNAUTH
        //HighVolumePayrollExportAction.PARAM_TERM_ADD_DAYS


        // *** set form values
        Map pgValueMap = new HashMap();
        pgValueMap.put("value","2005");
        pgValueMap.put("valueId","4");
        form.setFieldValue(HighVolumePayrollExportAction.PARAM_EMP_ID, pgValueMap);
        
        Map psValueMap = new HashMap();
        psValueMap.put("value", "PEOPLESOFT");
        psValueMap.put("valueId", "1");
        form.setFieldValue(HighVolumePayrollExportAction.PARAM_PAYSYS_ID, psValueMap);
        
        form.setValue(HighVolumePayrollExportAction.PARAM_CYCLE, "Regular");

        form.setValue(HighVolumePayrollExportAction.PARAM_RESET, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_USE_PP, "true");
        form.setValue(HighVolumePayrollExportAction.PARAM_ALL_READY, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_ADJ_DATE, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_WRITE_FILE, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_MERGE_FILE, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_WRITE_TABLE, "false");
        form.setValue(HighVolumePayrollExportAction.PARAM_CKUNAUTH, "false");
        
        form.setValue(HighVolumePayrollExportAction.PARAM_SUBMITTED_BY, TEST_USER1);
        
//      *** set output branch names
        Branch[] branches = super.getBranches(new String[] {"Success",
                                              "Failure"});
        // *** run test

        result = node.processObject(action, form, branches, null);
        assertNotNull(result);
        assertEquals(ActionResponse.BRANCH, result.getType());
        assertEquals("Success", ( (Branch) result.getValue()).getLabel());

    }

}
