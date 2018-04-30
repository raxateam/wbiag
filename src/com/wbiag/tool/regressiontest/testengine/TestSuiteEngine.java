package com.wbiag.tool.regressiontest.testengine;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.wbiag.tool.regressiontest.access.ITestCaseAccess;
import com.wbiag.tool.regressiontest.access.TestCaseAccessFactory;
import com.wbiag.tool.regressiontest.access.TestSuiteAccess;
import com.wbiag.tool.regressiontest.access.TestReportAccess;
import com.wbiag.tool.regressiontest.model.*;
import com.wbiag.tool.regressiontest.report.IReportXMLGenerator;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;

/**
 * @author bviveiros
 *
 * The class the runs a Test Suite.  
 * 
 * Each Workbrain module that is added to the test tool will have an Engine class
 * that extends this class.  The subclass will be very simple and only need to 
 * implement the abstact methods below.
 * 
 */
public abstract class TestSuiteEngine {
	
	private static Logger logger = Logger.getLogger(TestSuiteEngine.class);

	private static final int BATCH_SIZE_FOR_COMMIT = 100;
	
	/**
	 * Retrieve the test suite with the given Id, then execute it.
	 * See execute(TestSuiteData testSuite, DBConnection conn).
	 * 
	 * @param testSuiteId
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public int execute(int testSuiteId, DBConnection conn) throws Exception {
		
		TestSuiteData testSuite = null;
		TestSuiteAccess suiteAccess = new TestSuiteAccess(conn);
		
		// Retrieve the test suite.
		try {
			testSuite = suiteAccess.getTestSuite(testSuiteId);
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception retrieving Test Suite with id: " + testSuiteId, e);
		}
		
		// Execute it.
		return execute(testSuite, conn);
	}
	
    /**
     * Get the Test Case List for the Test Suite.  Then for each Test Case,
     * run the test case and retrieve the result data.  Use all expected data 
     * and actual data to generate a report.  The report is saved to the
     * database, and the Id of the report is returned. 
     * 
     * @param tsData
     * @return
     */
    public int execute(TestSuiteData testSuite, DBConnection conn) throws Exception
    {
    	TestReportData report = null;
		ITestCaseData testCase = null;
		ITestCaseResultActual actualResult = null;
		ITestCaseResultExpected expectedResult = null;
        Map actualResultList = new HashMap();
        Map expectedResultList = new HashMap();
        List testCaseList = null;
        Date runTime = new Date();
        Date suiteStartTime = null;
        Date suiteEndTime = null;
        Date caseStartTime = null;
        Date caseEndTime = null;
        long suiteExecutionTime = 0;
        long numProcessed = 0;
        
        if (testSuite == null) {
        	return 0;
        }

        suiteStartTime = new Date();
        
        // Get the test cases for the test suite.
        ITestCaseAccess caseAccess = getTestCaseAccess(testSuite, conn);
        
        try {
        	testCaseList = caseAccess.getTestCaseList(testSuite.getSuiteId());
        } catch (Exception e) {
        	throw new NestedRuntimeException("Exception retrieving Test Cases.", e);
        }
        
        if (testCaseList == null || testCaseList.size() == 0) {
        	throw new NestedRuntimeException("No Test Cases exist.");
        }
        
        long caseExecutionTime = 0;
        ITestCaseRunner runner = getTestCaseRunner();
        Iterator i = testCaseList.iterator();
        
        // Execute each test case.
        while (i.hasNext())
        {
            // Get the test case.
            testCase = (ITestCaseData) i.next();
	            
            try {
	            caseStartTime = new Date();
	            
	            // Get the expected results.
	            expectedResult = getTestCaseExpectedResult(testCase, conn);
	
            	// Run the test case.
	            numProcessed += runner.run(testCase, conn);
	            
	            // If we've reached the batch size, commit and reset the counter.
	            // This is to avoid database timeout of a commit has not occured in 60 seconds.
	            if (numProcessed >= getBatchSizeForCommit()) {
	            	conn.commit(); 
	            	numProcessed = 0;
	            }
	            
	            // Get the results of the execute.
	            actualResult = getTestCaseActualResult(testCase, conn);

	            caseEndTime = new Date();
	            
	            // Calculate the execution time of the test case.
	            caseExecutionTime = DateHelper.getSecondsBetween(caseEndTime, caseStartTime);
	            
	            actualResult.setExecutionTime(caseExecutionTime);
	            
	        } catch (Exception e) {
	        	if (logger.isEnabledFor(Priority.INFO)) {
	        		logger.info("Exception in testCaseId: " + getTestCaseId(testCase) + "\n");
	        		logger.info(e);
	        	}
	        	
	        	// If an exeption was thrown, save the exception 
	        	// as the result.
	        	if (actualResult == null) {
	        		actualResult = getTestCaseActualResult(testCase, conn);
	        	}
	        	actualResult.setExceptionThrown(e);
	        }

	        // Add to the list of test case results for our output.
            expectedResultList.put(getTestCaseId(testCase), expectedResult);
            actualResultList.put(getTestCaseId(testCase), actualResult);
            
            actualResult = null;
        }

        // Save the Last Executed time of the test suite.
        TestSuiteAccess suiteAccess = getTestSuiteAccess(conn);
        
        try {
        	testSuite.setSuiteExecuteDate(runTime);
        	suiteAccess.updateTestSuite(testSuite);
        } catch (Exception e) {
        	throw new NestedRuntimeException("Exception updating Last Executed Date.", e);
        }
        
        suiteEndTime = new Date();
        
        suiteExecutionTime = DateHelper.getSecondsBetween(suiteEndTime, suiteStartTime);

        // Generate a report for the entire test suite.
        try {
        	report = getReportXMLGenerator(testSuite, actualResultList, 
        								expectedResultList, suiteExecutionTime, conn).getReportData();
        } catch (Exception e) {
        	throw new NestedRuntimeException("Exception generating Test Suite Report.", e);
        }
        
        // Save the report to the database.
        TestReportAccess reportAccess = getTestSuiteReportAccess(conn);
        
        try {
        	reportAccess.addReport(report);
        } catch (Exception e) {
        	throw new NestedRuntimeException("Exception saving Test Suite Report.", e);
        }
        
        // Return the report so it can be sent to the browser.
        return report.getReportId();
    }


    /*
	 * Abstract methods that must be overritten by all Test Engines.
	 */
	protected abstract ITestCaseRunner getTestCaseRunner();
	protected abstract ITestCaseResultActual getTestCaseActualResult(ITestCaseData testCase, DBConnection conn) throws Exception;
	protected abstract ITestCaseResultExpected getTestCaseExpectedResult(ITestCaseData testCase, DBConnection conn) throws Exception;
	protected abstract Integer getTestCaseId(ITestCaseData testCase);
	protected abstract IReportXMLGenerator getReportXMLGenerator(TestSuiteData testSuite, 
														Map actualResultList,
														Map expectedResultList,
														long executionTimeSeconds,
														DBConnection conn) throws Exception;

	
	/*
	 * Protected methods that can be overriteen by any Test Engine for
	 * a specific implementation.
	 */
	protected TestSuiteAccess getTestSuiteAccess(DBConnection conn) {
		return new TestSuiteAccess(conn);
	}
	
	protected ITestCaseAccess getTestCaseAccess(TestSuiteData testSuite, DBConnection conn) {
		return TestCaseAccessFactory.getInstance(testSuite.getSuiteType(), conn);
	}
	
	protected TestReportAccess getTestSuiteReportAccess(DBConnection conn) {
		return new TestReportAccess(conn);
	}
	
	protected long getBatchSizeForCommit() {
		return BATCH_SIZE_FOR_COMMIT;
	}
	
}
