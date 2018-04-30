package com.wbiag.app.export.process;

import java.util.*;

import com.wbiag.app.wbinterface.*;
import com.workbrain.app.export.process.*;
import com.workbrain.util.*;
import junit.framework.*;
/**
 * Unit test for XMLFileTransmitterTest.
 */
public class XMLFileTransmitterTest extends WBInterfaceCustomTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XMLFileTransmitterTest.class);

    public XMLFileTransmitterTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(XMLFileTransmitterTest.class);
        return result;
    }

    /**
     * @throws Exception
     */
    public void testTransmitter() throws Exception{
        RowSourceExportProcessor proc = new RowSourceExportProcessor();
        proc.setCheckForInterrupt(false);

        HashMap param = new HashMap();
        param.put(RowSourceExportProcessor.PARAM_TYPE_NAME, "HR EXPORT");
        param.put(RowSourceExportProcessor.PARAM_MAPPING_NAME, "HR EXPORT");
        param.put(com.workbrain.app.export.hr.HRExportRowSource.PARAM_WHERE_CLAUSE ,
                  "emp_id in (14,15)");
        param.put(RowSourceExportProcessor.PARAM_PERFORM_DIFF,Boolean.FALSE );
        param.put(com.workbrain.app.export.hr.HRExportRowSource.PARAM_EMPUDF_DEFS , "");
        param.put(com.workbrain.app.export.hr.HRExportRowSource.PARAM_EMP_BALANCES , "");
        param.put(RowSourceExportProcessor.PARAM_EXPORT_PATH,"c:\\test.xml");
        param.put(RowSourceExportProcessor.PARAM_CREATES_TRANSACTION_FOR_NO_RECS , Boolean.FALSE );
        param.put(RowSourceExportProcessor.PARAM_CLIENT_ID, "1");
        param.put(RowSourceExportProcessor.PARAM_TRANSMITTER_CLASS, "com.wbiag.app.export.process.XMLFileTransmitter" );
        param.put(XMLFileTransmitter.PARAM_XML_ROOT_TAG ,  "employees");
        param.put(XMLFileTransmitter.PARAM_XML_RECORD_TAG ,  "employee");
        //param.put(XMLFileTransmitter.PARAM_FILE_TIMESTAMP_FORMAT ,  "MMddyyyyhhmm");
        proc.export(getConnection() , 0 , param, true);

        assertTrue(FileUtil.fileExists("c:\\test.xml")) ;
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
