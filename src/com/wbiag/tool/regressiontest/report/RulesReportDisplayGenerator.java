package com.wbiag.tool.regressiontest.report;

import com.wbiag.tool.regressiontest.access.TestReportAccess;
import com.wbiag.tool.regressiontest.model.TestReportData;
import com.wbiag.tool.regressiontest.xml.RulesReportHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;


/**
 * @author bviveiros
 * 
 * This class handles creating an instance of a ReportDisplayRulesSuite class
 * from a report XML string.  It handles the generation at a higher level, while
 * the RulesReportHelper handles the XML parsing.
 * 
 * The ReportDisplayRulesSuite class is used by a JSP for rendering the report.
 * 
 */
public class RulesReportDisplayGenerator implements IReportDisplayGenerator {

	/* (non-Javadoc)
	 * @see com.wbiag.tool.regressiontest.testengine.IReportDisplayGenerator#generateReportDisplay()
	 */
	public ReportDisplaySuite generateReportDisplay(int reportId, DBConnection conn) throws Exception {

		// Get the XML representing the report.
		String reportXML = getReportXML(reportId, conn);
		
		// Create the report display object.
		RulesReportHelper reportHelper = new RulesReportHelper(reportXML);
		ReportDisplaySuite reportDisplay = reportHelper.getReportDisplay();
		
		// Return the report object.
		return reportDisplay;
	}

	/*
	 * Given a report Id, get the report XML from the database.
	 * 
	 * @param reportId
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	private String getReportXML(int reportId, DBConnection conn) throws Exception {
		
		TestReportAccess reportAccess = new TestReportAccess(conn);
		TestReportData reportData = null;
		
		try {
			reportData = reportAccess.getReport(reportId);
		} catch (Exception e) {
			throw new NestedRuntimeException("Exception retrieving report.", e);
		}
		
		return reportData.getReportOutput();
	}
	
}
