package com.wbiag.tool.regressiontest.model;


/**
 * @author bviveiros
 *
 * Used by TestSuiteEngine.  Each TestSuiteType must have 
 * a xxxTestCaseExpectedResult class that implents this interface.
 * 
 * This cannot be an abstract class since the implementing class will
 * probably already extend a superclass.
 * 
 */
public interface ITestCaseResultExpected {

}
