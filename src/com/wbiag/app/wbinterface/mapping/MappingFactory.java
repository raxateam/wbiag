package com.wbiag.app.wbinterface.mapping;

import java.sql.*;
import java.util.*;

import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.mapping.*;
import com.workbrain.sql.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.util.*;
/**
 *  Simple Mapping Factory for general mapping schema.
 **/
public class MappingFactory  {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(MappingFactory.class);

    private InterfaceMapping mapping = null;

    /**
     * Constructor with a mapping name for wbint_mapping table
     * @param conn
     * @param mappingName
     * @throws Exception
     */
    public MappingFactory(DBConnection conn , String mappingName) throws Exception{
        mapping = MappingCache.getInstance().getWBIntMapping(conn , mappingName);
    }

    /**
     * Constructor with xml
     * @param mappingXml
     * @throws Exception
     */
    public MappingFactory(String mappingXml) throws Exception{
        mapping = InterfaceMapping.createMappingFromXML( mappingXml);
    }

    public List map(String input) throws Exception {
        List ret = new ArrayList();
        ResultSet rs = new com.workbrain.app.wbinterface.hr2.FactorResultSet( input );
        // *** go through all matchings
        Iterator iter = mapping.getRowMatchings();
        while (iter.hasNext()) {
            RowMatch row = (RowMatch) iter.next();
            // ** for matching item
            boolean matches = false;
            try {
                matches = matches(row , rs);
            }
            catch (Exception ex) {
                // *** if matches fails
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) logger.error("", ex);

                matches = false;
            }
            if( matches ) {
                Iterator output = row.getOutputRows();
                OutputRow out = (OutputRow) output.next();
                Iterator iterOut = out.getOutputValues();
                // *** print all matching name-values
                while (iterOut.hasNext()) {
                    OutputField outField = (OutputField)iterOut.next();
                    OutputFieldResolved outFieldResolved = new OutputFieldResolved ();
                    outFieldResolved.name = outField.getName();
                    outFieldResolved.value = outField.getOutputValue(rs);
                    ret.add(outFieldResolved) ;
                }
                break;
            }
        }
        // *** output rows are in wrong order
        Collections.reverse(ret);
        return ret;
    }

    /**
     * Overriding core matches logic
     * @param row
     * @param rs
     * @return
     * @throws Exception
     */
    private boolean matches( RowMatch row , ResultSet rs ) throws Exception {
        java.lang.reflect.Field fldTemp = row.getClass().getDeclaredField("fieldMatchings");
        fldTemp.setAccessible(true);
        List fieldMatchings = (List) fldTemp.get(row);

        Iterator fields = fieldMatchings.iterator();
        while( fields.hasNext() ) {
            FieldMatch match = (FieldMatch) fields.next();
            if( !matches( match , rs ) ) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines if the corresponding field in the input row matches the value.
     * Only exact match is supported at this time.
     *
     * @param rs            input row data
     * @return              true if the column matches
     * @throws SQLException
     * @throws WBInterfaceException
     */
    private boolean matches( FieldMatch match , ResultSet rs ) throws Exception {
        java.lang.reflect.Field fldTemp = match.getClass().getDeclaredField("name");
        fldTemp.setAccessible(true);
        String name = (String) fldTemp.get(match);
        fldTemp = match.getClass().getDeclaredField("value");
        fldTemp.setAccessible(true);
        String value = (String) fldTemp.get(match);
        fldTemp = match.getClass().getDeclaredField("operator");
        fldTemp.setAccessible(true);
        String operator = (String) fldTemp.get(match);

        ResultSetMetaData metaData = rs.getMetaData();
        int colCount = metaData.getColumnCount();
        for( int ii = 1; ii <= colCount; ii++ ) {
            if( name.equals( metaData.getColumnName( ii ) ) ) {
                int type = metaData.getColumnType( ii );
                Object obj = rs.getObject( ii );
                String dbVal = obj == null ? "" : obj.toString();

                return evaluate( dbVal, operator , value );
            }
        }
        return false;
    }

    private boolean evaluate( String dbVal, String operator , String value) {
        if (StringHelper.isEmpty(operator)
            ||StringHelper.isItemInList("eq,=,le,ge,gt,lt",operator)) {
            int intResult = dbVal.compareTo( value );
            return evaluate(intResult, operator );
        }
        else {
            if ("in".equals(operator)) {
                return StringHelper.isItemInList(value , dbVal);
            }
        }
        return false;
    }

    private boolean evaluate( int intResult, String operator ) {

        boolean result = false;
        if( intResult == 0 ) {
            if( operator == null || "".equals( operator ) ||
                "=".equals( operator ) || "eq".equalsIgnoreCase( operator ) ||
                "ge".equalsIgnoreCase( operator ) ||
                "le".equalsIgnoreCase( operator ) ) {
                result = true;
            }
        } else if( intResult > 0 ) {
            if( "gt".equalsIgnoreCase( operator ) ||
                "ge".equalsIgnoreCase( operator ) ) {
                result = true;
            }
        } else if( intResult < 0 ) {
            if( "lt".equalsIgnoreCase( operator ) ||
                "le".equalsIgnoreCase( operator ) ) {
                result = true;
            }
        }

        return result;
    }

    public String mapForOneOutputValue(String input) throws Exception {
        String ret = null;
        List outputs = map(input);
        if (outputs.size() > 0) {
            OutputFieldResolved outFieldResolved = (OutputFieldResolved) outputs.get(0);
            ret = outFieldResolved.value;
        }
        return ret;
    }

    public static class OutputFieldResolved {
        String name;
        String value;
    }

    public static void main( String[] args ) throws Exception {
        String xml =
            "<mapping>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"A\"/>"
            + "  <output-row><output-field name=\"1\" value=\"THIS\"/></output-row>"
            + "</row-match>"
            + "<row-match>"
            + "  <field-match fieldname=\"1\" matchvalue=\"BB\"/>"
            + "  <output-row>"
            + "    <output-field name=\"1\" value=\"JUST\"/>"
            + "    <output-field name=\"2\" value=\"FINE\"/>"
            + "    <output-field name=\"3\" value=\"...\"/>"
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
        System.out.println(mf.mapForOneOutputValue("A"));
        System.out.println(mf.mapForOneOutputValue("B~WORKS"));
        List outs = mf.map("BB");
        Iterator iter = outs.iterator();
        while (iter.hasNext()) {
            OutputFieldResolved item = (OutputFieldResolved)iter.next();
            System.out.println(item.value);
        }
    }


}
