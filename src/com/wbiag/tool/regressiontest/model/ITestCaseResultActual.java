package com.wbiag.tool.regressiontest.model;


/**
 * @author bviveiros
 *
 * Used by TestSuiteEngine.  Each TestSuiteType must have 
 * a xxxTestCaseActualResult class that implents this interface.
 * 
 * This cannot be an abstract class since the implementing class will
 * probably already extend a superclass.
 * 
 */
public interface ITestCaseResultActual {

	public void setExceptionThrown(Exception exceptionThrown);
	public Exception getExceptionThrown();
	
	public void setExecutionTime(long executionTimeSeconds);
	public long getExecutionTime();
}
