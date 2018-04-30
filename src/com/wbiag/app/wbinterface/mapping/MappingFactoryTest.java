package com.wbiag.app.wbinterface.mapping;

import com.workbrain.test.*;
import java.util.*;
import junit.framework.*;

/**
 */
public class MappingFactoryTest extends TestCaseLW {

    /**
     * @param arg0
     */
    public MappingFactoryTest(String arg0) {
        super(arg0);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(MappingFactoryTest.class);
        return result;
    }

    public void testMap() throws Exception	{
        final String xml =
            "<mapping>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"A\" />"
            + "  <output-row><output-field name=\"1\" value=\"THIS\"/></output-row>"
            + "</row-match>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"XX,YY,ZZ\" operator=\"in\" />"
            + "  <output-row><output-field name=\"1\" value=\"TESTING_IN\"/></output-row>"
            + "</row-match>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"BB\"/>"
            + "  <output-row>"
            + "    <output-field name=\"1\" value=\"1\"/>"
            + "    <output-field name=\"2\" value=\"2\"/>"
            + "    <output-field name=\"3\" value=\"3\"/>"
            + "  </output-row>"
            + "</row-match>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"B\"/>"
            + "  <field-match fieldname=\"2\" matchvalue=\"WORKS\"/>"
            + "  <output-row><output-field name=\"1\" copyfield=\"2\"/></output-row>"
            + "</row-match>"
            //  catch-all
            + "<row-match>"
            + "  <output-row><output-field name=\"1\" value=\"NOT_FOUND\"/></output-row>"
            + "</row-match>"
            + "</mapping>";

        MappingFactory mf = new MappingFactory(xml);
        assertEquals("THIS" , mf.mapForOneOutputValue("A"));
        assertEquals("WORKS" ,mf.mapForOneOutputValue("B~WORKS"));
        assertEquals("TESTING_IN" ,mf.mapForOneOutputValue("XX"));
        StringBuffer sb = new StringBuffer(200);
        List outs = mf.map("BB");
        Iterator iter = outs.iterator();
        while (iter.hasNext()) {
            MappingFactory.OutputFieldResolved item = (MappingFactory.OutputFieldResolved)iter.next();
            sb.append(item.value);
        }
        assertEquals("123" , sb.toString() );
    }


    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
