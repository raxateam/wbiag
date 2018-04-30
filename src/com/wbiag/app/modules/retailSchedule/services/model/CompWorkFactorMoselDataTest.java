/*---------------------------------------------------------------------------
  (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.app.modules.retailSchedule.services.model;

import com.workbrain.app.modules.retailSchedule.SOTestCase;

/**
 * Unit test for <code>CompWorkFactorMoselData</code> class.
 *
 * @author James Tam
 */
public class CompWorkFactorMoselDataTest extends SOTestCase {

    /**
     * Run this entire suite of tests.
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(CompWorkFactorMoselDataTest.class);
    }

    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test the <code>toString()</code> method, which generates a single entry
     * for the compress work factor Mosel temp input file.
     */
    public void testToString() {
        CompWorkFactorMoselData data = new CompWorkFactorMoselData("7185*7001*7005*7001", 70.25d);
        assertEquals("'7185*7001*7005*7001',70.25\n", data.toString());
    }

}