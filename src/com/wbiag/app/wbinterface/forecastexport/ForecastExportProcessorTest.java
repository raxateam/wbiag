package com.wbiag.app.wbinterface.forecastexport;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestSuite;

import com.workbrain.server.data.RowSource;
import com.workbrain.test.TestCaseHW;

/** 
 * Title:			Forecast Export Processor Test
 * Description:		JUnit test for ForecastExportProcessor
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Mar 24, 2006
 * @author         	Kevin Tsoi
 */
public class ForecastExportProcessorTest extends TestCaseHW
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ForecastExportProcessorTest.class);

	public ForecastExportProcessorTest(String testName) 
		throws Exception 
	{
	    super(testName);
	}
	
	public static TestSuite suite() 
	{
	    TestSuite result = new TestSuite();
	    result.addTestSuite(ForecastExportProcessorTest.class);
	    return result;
	}
	
	public void testForecastExportProcessor() 
		throws Exception 
	{
	    ForecastExportProcessor forecastExport = new ForecastExportProcessor();
	
	    Map params = new HashMap();
	    
	    params.put(ForecastExportDataSource.PARAM_VOLUME_TYPE, "1000");
	    params.put(ForecastExportDataSource.PARAM_WEEK_OFFSET, "0");
	    params.put(ForecastExportDataSource.PARAM_NUM_OF_WEEKS, "1");
	    params.put(ForecastExportDataSource.PARAM_AGGREGATE_LEVEL, ForecastExportDataSource.AGG_LEVEL_WEEK);
	    
	    RowSource rs = forecastExport.getRowSource(params, getConnection());
	    	
	    assertNotNull(rs);
	}
	
	public static void main(String[] args) 
		throws Exception 
	{
	    junit.textui.TestRunner.run(suite());
	}
}
