package com.wbiag.tool.regressiontest.testengine;


/**
 * @author bviveiros
 *
 * A Test Suite can contain only 1 type of Test Case and is stored
 * as a Test Suite Type.  For the given test suite type, return an
 * instance of the corresponding Test Suite Engine.  
 */
public class TestSuiteEngineFactory {

	public static TestSuiteEngine getInstance(String testSuiteType) 
	{
		TestSuiteEngine engine = null;
		
		if (TestSuiteTypes.PAY_RULES.equals(testSuiteType)) {
			engine = new RulesTestSuiteEngine();
		}
		
		return engine;
	}
}
