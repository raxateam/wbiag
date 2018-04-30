package com.wbiag.app.ta.ruleTrace.model ;


import com.workbrain.util.XMLHelper;
import com.workbrain.sql.*;
import com.workbrain.server.data.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.util.*;
import java.io.*;
import java.sql.*;
import javax.sql.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *  @deprecated Core as of 5.0.3.0
 */
public class RuleTraceConfigMapping implements MappingFactory {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RuleTraceConfigMapping.class);

    private String xml;


    /**
     * Default constructor.
     */
    public RuleTraceConfigMapping() {
    }


    /**
     * Create an RuleTraceConfigMapping object from mappingData.
     *
     * @param conn   DBConnection
     * @param mappingData   mappingData
     * @return              new RuleTraceConfigMapping object
     * @throws SAXException
     * @throws IOException
     */
    public MappingObject createMappingObject( DBConnection conn , MappingData mappingData )
            throws SAXException, IOException {
        if (mappingData == null) {
            throw new RuntimeException ("Mapping Data cannot be null");
        }
        xml = mappingData.getXml();
        RuleTraceConfig config = new RuleTraceConfig();

        if (StringHelper.isEmpty(xml)) {
            return config;
        }

        createMappingFromXML( config, new StringReader( xml ) );

        return config;
    }

    /**
     * Static helper method to create an object from an XML string.
     *
     * @param mapping           RuleTraceConfigMapping object to populate
     * @param xmlReader         Reader object containing the XML data
     * @throws SAXException
     * @throws IOException
     */
    private void createMappingFromXML( RuleTraceConfig config,
            Reader xmlReader ) throws SAXException, IOException {
        Document document = null;
        DocumentBuilder docBuilder = getDocBuilder();

        InputSource isource = new InputSource( xmlReader );
        document = docBuilder.parse( isource );
        Element configE = (Element) document.getElementsByTagName(
            "ruleTraceConfig" ).item( 0 );
        if (configE != null) {
            config.setEnabled(Boolean.valueOf(configE.getAttribute("enabled")).booleanValue()) ;
            String cgs = configE.getAttribute("applyToCalcGroups");
            if (!StringHelper.isEmpty(cgs)) {
                config.setApplyToCalcGroups(cgs);
            }
            NodeList dataNL = configE.getElementsByTagName("data");
            if (dataNL.getLength() > 0) {
                Element dataE = (Element)dataNL.item(0);
                NodeList empE = dataE.getElementsByTagName("employee");
                if (empE.getLength() > 0) {
                    String fields = empE.item(0).getAttributes().getNamedItem("fields").getNodeValue();
                    config.setEmployeeFields(StringHelper.
                                             detokenizeStringAsList(fields, ",", true));
                }

                NodeList wrks = dataE.getElementsByTagName("workSummary");
                if (wrks.getLength() > 0) {
                    String fields = wrks.item(0).getAttributes().getNamedItem("fields").getNodeValue();
                    config.setWorkSummaryFields(StringHelper.
                                             detokenizeStringAsList(fields, ",", true));
                }

                NodeList wrkd = dataE.getElementsByTagName("workDetails");
                if (wrkd.getLength() > 0) {
                    String fields = wrkd.item(0).getAttributes().getNamedItem("fields").getNodeValue();
                    config.setWorkDetailFields(StringHelper.
                                             detokenizeStringAsList(fields, ",", true));
                }

                NodeList wrkp = dataE.getElementsByTagName("workPremiums");
                if (wrkp.getLength() > 0) {
                    String fields = wrkp.item(0).getAttributes().getNamedItem("fields").getNodeValue();
                    config.setWorkPremiumFields(StringHelper.
                                             detokenizeStringAsList(fields, ",", true));
                }

                NodeList empBals = dataE.getElementsByTagName("employeeBalances");
                if (empBals.getLength() > 0) {
                    String fields = empBals.item(0).getAttributes().getNamedItem("balances").getNodeValue();
                    config.setEmployeBalances(StringHelper.
                                             detokenizeStringAsList(fields, ",", true));
                }

            }
        }

    }


    private DocumentBuilder getDocBuilder() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating( false );

        try {
            return dbf.newDocumentBuilder();
        } catch( ParserConfigurationException exc ) {
            // rethrow unchecked exception
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Wrapping exception with unchecked exception:" );}
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(exc);}

            throw new NestedRuntimeException( "Error in parsing Rule Trace config xml", exc);
        }
    }


}

