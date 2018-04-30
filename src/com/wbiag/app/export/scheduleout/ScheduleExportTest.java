package com.wbiag.app.export.scheduleout;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestSuite;

import com.workbrain.app.export.process.RowSourceExportProcessor;
import com.workbrain.app.wbinterface.WBInterfaceTestCase;
import com.workbrain.util.FileUtil;
import com.wbiag.app.export.process.*;

public class ScheduleExportTest extends WBInterfaceTestCase 
{
	 public ScheduleExportTest(String testName) throws Exception 
	 {
	        super(testName);
	 }
	 
	 public static TestSuite suite() 
	 {
	      TestSuite result = new TestSuite();
	      result.addTestSuite(ScheduleExportTest.class);
	      return result;
	 }
	 
	 public void testScheduleExport() throws Exception{

	        RowSourceExportProcessor proc = new RowSourceExportProcessor();

	        proc.setCheckForInterrupt(false);
	        final String exportPath = "C:\\test.xml";
	        HashMap param = new HashMap();
	        param.put(RowSourceExportProcessor.PARAM_TYPE_NAME, "SCHEDULE EXPORT");
	        param.put(RowSourceExportProcessor.PARAM_MAPPING_NAME, "SCHEDULE EXPORT");
	        param.put(RowSourceExportProcessor.PARAM_EXPORT_PATH , exportPath);
	        param.put(RowSourceExportProcessor.PARAM_CLIENT_ID, "1");
	        param.put(RowSourceExportProcessor.PARAM_TRANSMITTER_CLASS, "com.wbiag.app.export.process.XMLFileTransmitter" );
	        param.put(ScheduleDataSource.PARAM_BRK_CODE_LIST, "BRK,MEAL");
	        param.put(XMLFileTransmitter.PARAM_FILE_TIMESTAMP_FORMAT, "");
	        proc.setTaskId(0);
	        proc.export(getConnection() , proc.getTaskId() , (Map)param, true);


	     
	        // *** verify if file created
	        assertEquals(FileUtil.fileExists(exportPath), true);
	        // *** verify if exports are as many as emps
	    
	    }



	    public static void main(String[] args) throws Exception {
	        junit.textui.TestRunner.run(suite());
	    }


}
