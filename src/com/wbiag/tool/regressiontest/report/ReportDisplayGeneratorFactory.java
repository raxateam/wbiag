package com.wbiag.tool.regressiontest.report;

import com.wbiag.tool.regressiontest.testengine.TestSuiteTypes;


/**
 * @author bviveiros
 *
 * For the given test suite type, return an
 * instance of the corresponding IReportDisplayGenerator.  
 */
public class ReportDisplayGeneratorFactory {

	public static IReportDisplayGenerator getInstance(String testSuiteType) 
	{
		IReportDisplayGenerator displayGenerator = null;
		
		if (TestSuiteTypes.PAY_RULES.equals(testSuiteType)) {
			displayGenerator = new RulesReportDisplayGenerator();
		}
		
		return displayGenerator;
	}
}
