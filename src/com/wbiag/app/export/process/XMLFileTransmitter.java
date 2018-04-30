package com.wbiag.app.export.process;

import com.workbrain.sql.*;
import com.workbrain.app.export.process.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.mapping_rowsource.*;
import com.workbrain.app.scheduler.enterprise.AbstractScheduledJob;
import com.workbrain.util.*;
import java.util.*;
import java.sql.*;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Export XML Transmitter that will transmit the values in wbint_export based on mapping
 *  (i.e copyfields as element names).
 * i.e for mapping
 *            <mapping>
              <row-match>
               <output-row>
                <output-field name=\"A\" copyfield=\"EMP_NAME\"/>
                <output-field name=\"B\" copyfield=\"EMP_MINUTES\"/>
               </output-row>
              </row-match>
             </mapping>"
 *    it will export as
 *    <records>
 *      <record>
 *       <emp_name> ? </emp_name>
 *       <emp_minutes> ? </emp_minutes>
 *      </record>
 *    </records>

 */

public class XMLFileTransmitter implements Transmitter {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XMLFileTransmitter.class);

    public static final String PARAM_XML_DECLARATION_DEFAULT = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?> ";
    public static final String PARAM_XML_ROOT_TAG_DEFAULT = "records";
    public static final String PARAM_XML_RECORD_TAG_DEFAULT = "record";

    public static final String PARAM_XML_DECLARATION = "XMLDeclaration";
    public static final String PARAM_XML_ROOT_TAG = "XMLRootTag";
    public static final String PARAM_XML_RECORD_TAG = "XMLRecordTag";
    public static final String PARAM_FILE_TIMESTAMP_FORMAT = "FileTimestampFormat";


    public void execute( DBConnection conn, Map params )  throws WBInterfaceException {

        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {
            logger.debug( "XMLFileTransmitter.execute(" + params + ")" );
        }

        String mappingName = (String) params.get( RowSourceExportProcessor.PARAM_MAPPING_NAME );
        String wbitypName = (String) params.get( RowSourceExportProcessor.PARAM_TYPE_NAME );
        String exportPath = (String) params.get( RowSourceExportProcessor.PARAM_EXPORT_PATH );
        if (StringHelper.isEmpty(exportPath)) {
            throw new RuntimeException ("Export file name must be specified");
        }
        // **** get the select fields supplied by RowSourceExportProcessor, if none use all
        InterfaceMapping mapping = (InterfaceMapping)params.get( RowSourceExportProcessor.PARAM_MAPPING_OBJECT );

        int clientId = Integer.parseInt((String) params.get( RowSourceExportProcessor.PARAM_CLIENT_ID ));
        int wbitranId = ((Integer) params.get( RowSourceExportProcessor.PARAM_TRANSACTION_ID)).intValue();

        try {
            writeToFile(conn, params, mapping, mappingName , wbitranId, exportPath);
            new ExportAccess(conn).updateExportByTransactionId(wbitranId,
                ExportData.STATUS_APPLIED,
                null,
                ExportData.STATUS_PENDING);
        }
        catch (Exception ex) {
            throw new WBInterfaceException ("Error during XMLFileTransmitter" , ex);
        }

        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("TestEmployeeTransmitter completed processing sucessfully!");}
     }

     /**
      * Returns select statement that will be used in transmitting
      *
      * @param wbiTranId
      * @return
      */
     protected String getQueryString( String nameFields, int wbiTranId ) {
         return ExportAccess.getPendingSelectSqlForFields(nameFields , wbiTranId);
     }

     /**
      * Writes to xml file based on ammping copyfile definitions
      * @param conn
      * @param mapping
      * @param wbiTranId
      * @param exportPath
      * @return
      * @throws Exception
      */
     protected boolean writeToFile(DBConnection conn,
                                   Map params,
                                   InterfaceMapping mapping ,
                                   String mappingName,
                                   int wbiTranId,
                                   String exportPath) throws Exception{
         String xmlDeclaration = (String) params.get( PARAM_XML_DECLARATION );
         xmlDeclaration = StringHelper.isEmpty(xmlDeclaration)
             ?  PARAM_XML_DECLARATION_DEFAULT
             : xmlDeclaration;
         String xmlRootTag = (String) params.get( PARAM_XML_ROOT_TAG );
         xmlRootTag = StringHelper.isEmpty(xmlRootTag)
             ?  PARAM_XML_ROOT_TAG
             : xmlRootTag;
         String xmlRecordTag = (String) params.get( PARAM_XML_RECORD_TAG );
         xmlRecordTag = StringHelper.isEmpty(xmlRecordTag)
             ?  PARAM_XML_RECORD_TAG
             : xmlRecordTag;
         String filestampFormat = (String) params.get( PARAM_FILE_TIMESTAMP_FORMAT );

         // *** for wbint_export query
         String nameFields = mapping.getOrderedOutputFieldNamesFormatted(conn) ;
         nameFields  = (nameFields == null ) ? "*" : nameFields;

         String sql = getQueryString(nameFields , wbiTranId);
         // *** for xml tags, to find out what field goes with copy fields
         List copyFields = getOrderedFields(conn, mappingName );

         PreparedStatement ps = null;
         ResultSet rs = null;
         PrintWriter writer = null;
         try {
             File file = FileUtil.createFileWithDir(exportPath);
             writer = new PrintWriter(new FileOutputStream(file));
             writer.println(xmlDeclaration);
             writer.println("<" + xmlRootTag + ">");
             StringBuffer sb = new StringBuffer(200);
             ps = conn.prepareStatement(sql);
             rs = ps.executeQuery();
             ResultSetMetaData md = rs.getMetaData();
             int cnt = 0;
             while (rs.next()) {
                 writer.println(" " + "<" + xmlRecordTag + ">");
                 Iterator iter = copyFields.iterator();
                 while (iter.hasNext()) {
                     Field item = (Field)iter.next();
                     String fieldValue = XMLHelper.escapeXMLValue(rs.getString(item.fldName));
                     String elementName = item.cpyField;
                     fieldValue = "  <" + elementName + ">" +  fieldValue + "</" + elementName + ">" ;
                     writer.println(fieldValue);
                 }
                 writer.println(" " + "</" + xmlRecordTag + ">");
                 writer.println();
                 cnt++;
             }
             writer.println("</" + xmlRootTag + ">");
             if (logger.isDebugEnabled()) logger.debug("Exported :" + cnt + " records to file:" + exportPath);
         }
         finally {
             if (writer != null) {
                 writer.flush();
                 writer.close();
                 if (!StringHelper.isEmpty(filestampFormat)) {
                     FileUtil.timeStampFile(exportPath , filestampFormat);
                 }
             }
             if (rs != null) rs.close();
             if (ps != null) ps.close();
         }

         return true;
     }

     private class Field {
         String fldName;
         String cpyField;

         public Field(String fldName,  String cpyField) {
             this.fldName = fldName;
             this.cpyField = cpyField;
         }
     }

     private List getOrderedFields(DBConnection conn, String mappingName) throws Exception {
         List ret = new ArrayList();
         MappingData data = new MappingAccess(conn).loadByName(mappingName);
         if (data ==  null) {
             throw new RuntimeException ("Mapping not found:" +  mappingName);
         }
         Document document = null;
         DocumentBuilder docBuilder = getDocBuilder();

         InputSource isource = new InputSource( new StringReader(data.getXml()) );
         document = docBuilder.parse( isource );
         Element mapElem = (Element) document.getElementsByTagName(
             InterfaceMapping.MAPPING_TAG ).item( 0 );

          NamedNodeMap attrs = mapElem.getAttributes();
          NodeList nl = mapElem.getElementsByTagName( RowMatch.ROW_MATCH_TAG );
          for( int ii = 0; ii < 1; ii++ ) {
              NodeList nl1 = ((Element) nl.item( ii )).getElementsByTagName( OutputField.OUTPUT_FIELD_TAG );
              for( int iii = 0; iii < nl1.getLength(); iii++ ) {
                  OutputField field = OutputField.createOutputFieldFromXML( (Element) nl1.item( iii ) );
                  ret.add(new Field(field.getName(), field.getCopyField().toLowerCase()));
              }
          }
          return ret;

     }

     private static DocumentBuilder getDocBuilder() {
         DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
         dbf.setValidating( false );

         try {
             return dbf.newDocumentBuilder();
         } catch( ParserConfigurationException exc ) {
             // rethrow unchecked exception
             if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Wrapping exception with unchecked exception:" );}
             if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug(exc);}

             throw new RuntimeException( exc.toString() );
         }
     }

     /**
      * Returns transmitterUI jsp path.
      *
      * @return path
      */
     public String getTransmitterUI() {
        return "/jobs/wbiag/XMLFileTransmitterParams.jsp";
     }
}
