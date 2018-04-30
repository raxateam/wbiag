package com.wbiag.tool.regressiontest.testengine;

import java.util.Map;

import com.wbiag.tool.regressiontest.model.*;
import com.wbiag.tool.regressiontest.report.IReportXMLGenerator;
import com.wbiag.tool.regressiontest.report.RulesReportXMLGenerator;
import com.workbrain.sql.DBConnection;

/**
 * @author bviveiros
 *
 * PayRules implementation of the TestSuiteEngine.  Implements all the abstract
 * methods required by the TestSuiteEngine.
 */
public class RulesTestSuiteEngine extends TestSuiteEngine {

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.testengine.TestSuiteEngine#getTestCaseRunner()
	 */
	public ITestCaseRunner getTestCaseRunner() {
		return new RulesCaseRunner();
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.testengine.TestSuiteEngine#getTestCaseActualResult(com.wbiag.tool.regressiontest.model.ITestCaseData, com.workbrain.sql.DBConnection)
	 */
	protected ITestCaseResultActual getTestCaseActualResult(ITestCaseData testCase, DBConnection conn) throws Exception {
		return new RulesCaseResultActual(testCase, conn);
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.testengine.TestSuiteEngine#getTestCaseExpectedResult(com.wbiag.tool.regressiontest.model.ITestCaseData, com.workbrain.sql.DBConnection)
	 */
	protected ITestCaseResultExpected getTestCaseExpectedResult(ITestCaseData testCase, DBConnection conn) throws Exception {
		return new RulesCaseResultExpected((RulesCaseData)testCase, conn);
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.testengine.TestSuiteEngine#getTestCaseId(com.wbiag.tool.regressiontest.model.ITestCaseData)
	 */
	protected Integer getTestCaseId(ITestCaseData testCase) {
		return new Integer( ((RulesCaseData) testCase).getCaseId());
	}

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.testengine.TestSuiteEngine#getReportXMLGenerator(com.wbiag.tool.regressiontest.model.TestSuiteData, java.util.Map, java.util.Map, long, com.workbrain.sql.DBConnection)
	 */
	protected IReportXMLGenerator getReportXMLGenerator(TestSuiteData testSuite, Map actualResultList, Map expectedResultList, long executionTimeSeconds, DBConnection conn) throws Exception {
		return new RulesReportXMLGenerator(testSuite, actualResultList, expectedResultList, executionTimeSeconds);
	}

}
