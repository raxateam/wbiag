package com.wbiag.app.ta.conditions;

import java.util.*;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import junit.framework.*;

/**
 * Test for TerminationDateConditionTest.
 *@deprecated As of 5.0.2.0, use core classes 
 */
public class IsTerminationDateConditionTest extends RuleTestCase {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(IsTerminationDateConditionTest.class);

    public IsTerminationDateConditionTest(String testName) throws Exception {
        super(testName);
    }
    
    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(IsTerminationDateConditionTest.class);
        return result;
    }

    /**
     * Test when the termination date is equal to the 'current' date
	 * @throws Exception
     */
    public void testTerminationDateEqualsCurrentDate() throws Exception {
        DBConnection conn = this.getConnection();

        Date start = DateHelper.getCurrentDate();
        
        final int empId = 15;
        EmployeeAccess ea = new EmployeeAccess(conn, this.getCodeMapper());
        EmployeeData ed = ea.load(empId, start);

        Date curDate = ed.getEmpTerminationDate();

        Condition condition = new IsTerminationDateCondition();
        Parameters condParams = new Parameters();

        // Check that curDate = termination date - Today
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsTerminationDateCondition.PARAM_DATES, IsTerminationDateCondition.PARAM_DATES_TODAY);
        assertConditionTrue(empId, curDate, condition, condParams);

        // Check that termination date != Yesterday relative to curDate
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsTerminationDateCondition.PARAM_DATES, IsTerminationDateCondition.PARAM_DATES_YESTERDAY);
        assertConditionFalse(empId, curDate, condition, condParams);

        // Check that termination date != Tomorrow relative to curDate
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsTerminationDateCondition.PARAM_DATES, IsTerminationDateCondition.PARAM_DATES_TOMORROW);
        assertConditionFalse(empId, curDate, condition, condParams);
    }    
    
    /**
     * Test when the termination date is the day AFTER the 'current' date
	 * @throws Exception
     */
    public void testTerminationDateAfterCurrentDate() throws Exception {
        DBConnection conn = this.getConnection();
        
        Date start = DateHelper.getCurrentDate();

        final int empId = 15;
        EmployeeAccess ea = new EmployeeAccess(conn, this.getCodeMapper());
        EmployeeData ed = ea.load(empId, start);

        Date curDate = DateHelper.addDays(ed.getEmpTerminationDate(), 1);

        Condition condition = new IsTerminationDateCondition();
        Parameters condParams = new Parameters();

        // Check that curDate != termination date
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsTerminationDateCondition.PARAM_DATES, IsTerminationDateCondition.PARAM_DATES_TODAY);
        assertConditionFalse(empId, curDate, condition, condParams);

        // Check that termination date = Yesterday relative to curDate
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsTerminationDateCondition.PARAM_DATES, IsTerminationDateCondition.PARAM_DATES_YESTERDAY);
        assertConditionTrue(empId, curDate, condition, condParams);

		// Check that termination date != Tomorrow relative to curDate
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsTerminationDateCondition.PARAM_DATES, IsTerminationDateCondition.PARAM_DATES_TOMORROW);
        assertConditionFalse(empId, curDate, condition, condParams);
    }

    /**
     * Test when the termination date is the day BEFORE the 'current' date
	 * @throws Exception
     */
    public void testTerminationDateBeforeCurrentDate() throws Exception {
        DBConnection conn = this.getConnection();

        Date start = DateHelper.getCurrentDate();
        
        final int empId = 15;
        EmployeeAccess ea = new EmployeeAccess(conn, this.getCodeMapper());
        EmployeeData ed = ea.load(empId, start);

        Date curDate = DateHelper.addDays(ed.getEmpTerminationDate(), -1);

        Condition condition = new IsTerminationDateCondition();
        Parameters condParams = new Parameters();

        // Check that curDate != termination date - Today
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsTerminationDateCondition.PARAM_DATES, IsTerminationDateCondition.PARAM_DATES_TODAY);
        assertConditionFalse(empId, curDate, condition, condParams);

        // Check that termination date != Yesterday relative to curDate
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsTerminationDateCondition.PARAM_DATES, IsTerminationDateCondition.PARAM_DATES_YESTERDAY);
        assertConditionFalse(empId, curDate, condition, condParams);

        // Check that termination date = Tomorrow relative to curDate
    	condParams.removeAllParameters();        
    	condParams.addParameter(IsTerminationDateCondition.PARAM_DATES, IsTerminationDateCondition.PARAM_DATES_TOMORROW);
        assertConditionTrue(empId, curDate, condition, condParams);
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }
}

