package com.wbiag.tool.regressiontest.report;

import com.wbiag.tool.regressiontest.model.TestReportData;

/**
 * @author bviveiros
 *
 * Each module will have a ReportXMLGenerator that implements this
 * interface.
 * 
 * The class will do the comparason of the expected results with the 
 * actual results and generates a report in XML format.
 * 
 * It also generates a TestReportData object to be used by the 
 * TestSuiteEngine.
 * 
 */
public interface IReportXMLGenerator {

	public String generateXML() throws Exception;
	
	public TestReportData getReportData() throws Exception;
}
