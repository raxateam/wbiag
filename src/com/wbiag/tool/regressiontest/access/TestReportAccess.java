package com.wbiag.tool.regressiontest.access;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import com.wbiag.tool.regressiontest.model.TestReportData;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.sql.DBConnection;

/**
 * @author bviveiros
 *
 * Access class for the WBIAG_TST_REPORT table.
 */
public class TestReportAccess extends RecordAccess {

	private static Logger logger = Logger.getLogger(TestReportAccess.class);

	
	public static final String TEST_REPORT_TABLE = "WBIAG_TST_REPORT";
    public static final String TEST_REPORT_PRI_KEY = "REPORT_ID";
    public static final String TEST_REPORT_SEQ = "SEQ_TST_REPORT_ID";
    public static final String COL_REPORT_OUTPUT = "REPORT_OUTPUT";
    public static final String TEST_SUITE_FOR_KEY = "SUITE_ID";

	/**
	 * @param c
	 */
	public TestReportAccess(DBConnection c) {
		super(c);
	}

	/**
	 * Create the given Test Suite Report.
	 * 
	 * @param report
	 * @throws SQLException
	 */
	public void addReport(TestReportData report) throws SQLException, IOException {
		
		report.setReportId( getDBConnection().getDBSequence( TEST_REPORT_SEQ ).getNextValue() );
        
		// Need to update any CLOB columns seperately.  If it is larger than 32K, it will get truncated
		// by the PreparedStatement.setString() method.  Use the DBConnection.updateClob() method instead.
		String clobValue = report.getReportOutput();
		report.setReportOutput("TEMP");
		
        // Insert the record.
        insertRecordData( report, TEST_REPORT_TABLE );
        
        // Update the CLOB columns.
		report.setReportOutput(clobValue);
		
		try {
			getDBConnection().updateClob(clobValue, TEST_REPORT_TABLE, COL_REPORT_OUTPUT,
        								TEST_REPORT_PRI_KEY, String.valueOf(report.getReportId()));
		} catch (SQLException e) {
        	logger.error("ReportId: " + report.getReportId() + ", CLOB chars: " + clobValue.length());
			throw e;
		}
	}
	
	/**
	 * Return the Test Suite Report with the given Id.
	 * 
	 * @param reportId
	 * @return
	 */
	public TestReportData getReport(int reportId) {

		TestReportData suiteReport = new TestReportData();
        
        List records = loadRecordData( suiteReport, TEST_REPORT_TABLE, TEST_REPORT_PRI_KEY + " = " + reportId );
        
        if( records.size() > 0 ) {
            return (TestReportData) records.get( 0 );
        }

        return null;
	}
	public List getAllReports(int suiteId) {

		TestReportData suiteReport = new TestReportData();
        
        List records = loadRecordData( suiteReport, TEST_REPORT_TABLE, TEST_SUITE_FOR_KEY + " = " + suiteId );
        
        if( records.size() > 0 ) {
            return records;
        }

        return null;
	}
	
}
