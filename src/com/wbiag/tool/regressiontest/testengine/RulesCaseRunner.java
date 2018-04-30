package com.wbiag.tool.regressiontest.testengine;

import java.util.Date;

import com.wbiag.tool.regressiontest.model.*;
import com.workbrain.app.ta.ruleengine.RuleEngine;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;

/**
 * @author bviveiros
 *
 * Pay Rules implemenation of an ITestCaseRunner.
 * 
 * Calculates a timesheet for an employee for a date, or range of dates.
 * 
 */
public class RulesCaseRunner implements ITestCaseRunner {

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.testengine.TestCaseRunner#run(com.wbiag.tool.regressiontest.testengine.TestCase)
	 */
	public long run(ITestCaseData testCase, DBConnection conn) throws Exception {
		
		RulesCaseData ruleTestCase = (RulesCaseData) testCase;
		long numProcessed = 0;
		
		Date startDate = ruleTestCase.getCaseStartDate();
		Date endDate = ruleTestCase.getCaseEndDate() == null ? startDate : ruleTestCase.getCaseEndDate();
		
		try
        {
			// Run the calculation for the empid, and date range.
            RuleEngine.runCalcGroup(conn, ruleTestCase.getEmpId(), startDate, endDate);
            
            // Number of records processed is the the number of days calculated.
            numProcessed = DateHelper.getDifferenceInDays(endDate, startDate);
            
        } catch (Exception e) {
            throw new NestedRuntimeException("Error in calculating day.", e);
        }

        return numProcessed;
	}

}
