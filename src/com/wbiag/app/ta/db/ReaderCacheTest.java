package com.wbiag.app.ta.db;

import com.workbrain.app.ta.model.*;
import com.workbrain.test.*;
import junit.framework.*;

public class ReaderCacheTest extends TestCaseHW{

    public ReaderCacheTest(String arg0) {
        super(arg0);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(ReaderCacheTest.class);
        return result;
    }

    /**
     * Tests cache methods.
     * @throws Exception
     */
    public void testGet() throws Exception {
        ReaderCache cache = ReaderCache.getInstance();
        ReaderData rd = cache.getReaderData(getConnection(), "VIRTUAL READER");
        assertNotNull(rd);

        Integer rgId = cache.getReaderGroupId(getConnection(), "VIRTUAL READER GROUP");
        assertNotNull(rgId);
    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
}

}
