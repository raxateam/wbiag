package com.wbiag.app.wbinterface.pts;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestSuite;

import com.wbiag.app.wbinterface.WBInterfaceCustomTestCase;
import com.workbrain.server.data.RowSource;

/** 
 * Title:			PTSExportProcessorTest
 * Description:		Junit test for PTSExportProcessor
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Jun 29, 2005
 * @author         	Kevin Tsoi
 */
public class PTSExportProcessorTest extends WBInterfaceCustomTestCase
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
    	getLogger(PTSExportProcessorTest.class);

	public PTSExportProcessorTest(String testName) 
		throws Exception 
	{
	    super(testName);
	}

	public static TestSuite suite() 
	{
	    TestSuite result = new TestSuite();
	    result.addTestSuite(PTSExportProcessorTest.class);
	    return result;
	}

	public void testPTSExportProcessor() 
		throws Exception 
	{
	    PTSExportProcessor ptsExport = new PTSExportProcessor();
	
	    Map params = new HashMap();
	    params.put(PTSExportProcessor.PARAM_NUM_OF_DAYS, "0");
	    params.put(PTSExportProcessor.PARAM_OFFSET, "0");
	
	    RowSource rs = ptsExport.getRowSource(params, getConnection());
	    	
	    assertNotNull(rs);
	}

	public static void main(String[] args) 
		throws Exception 
	{
	    junit.textui.TestRunner.run(suite());
	}
}
