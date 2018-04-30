package com.wbiag.app.export.payroll ;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;
import com.workbrain.app.export.payroll.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * PayrollExportEditor for PEEditor.jsp
 */
public class PayrollExportEditor  {

    public static final String[] GROUP_FUNCTIONS = new String[] {"" ,"first" , "last", "sum"};
    public static final String[] SUMMARY_FUNCTIONS = new String[] {"" ,"first" , "last", "sum", "count" , "average" , "min" , "max" , "list"};
    public static final String[] BOOLEANS = new  String[] {"true", "false"};

    private DBConnection conn;

    public PayrollExportEditor(DBConnection conn) {
        this.conn = conn;
    }

    public PayrollExportData savePayrollExportData(HttpServletRequest request, int petId) throws Exception {
        PayrollExportData data = createPayrollExportData(request);
        // *** verify XML
        String xml = data.createXML();
        savePayrollExportData(xml , petId);
        return data;
    }

    public boolean savePayrollExportData(String xml, int petId) throws Exception {
        validateXml(xml);
        conn.updateClob(xml, "PAYROLL_EXPORT_TSK",
                                     "PET_XML",
                                     "PET_ID",
                                     String.valueOf(petId));
        return true;
    }

    public PayrollExportData createPayrollExportData(HttpServletRequest request) throws Exception {
        // *** plugin
        PayrollExportData ret = new PayrollExportData();
        ret.setPlugin((String)request.getParameter("plugin"));
        // ** * data
        Data data = new Data();
        int dataCount = Integer.parseInt((String)request.getParameter("data_count"));
        for (int i = 1; i <= dataCount; i++) {
            String type = (String)request.getParameter("data_type_" + i);
            data.addDataField(request, type,
                               "data_",
                               "_" + i,
                               i);
        }
        ret.setData(data);
        // ***logic
        Logic logic = new Logic();
        int logicCount = Integer.parseInt((String)request.getParameter("logic_count"));
        for (int i = 1; i <= logicCount; i++) {
            int mtchCount = Integer.parseInt((String)request.getParameter("logic_match_count_" + i));
            int outputRowCount = Integer.parseInt((String)request.getParameter("logic_outputRow_count_" + i));

            MatchOutput matchOutput = new MatchOutput();
            for (int k = 1; k <= mtchCount; k++) {
                matchOutput.addMatchOutputField(request, "match",
                                                "logic_match_", "_" + i + "_" + k);
            }
            for (int k = 1; k <= outputRowCount; k++) {
                matchOutput.addMatchOutputField(request, "map",
                                                "logic_map_", "_" + i + "_" + k);
            }

            logic.addMatchOutput(matchOutput);
        }
        ret.setLogic(logic);
        // *** format
        Format fmt = new Format();
        for (int k = 1; k <=3 ; k++) {
            String part = "";
            switch (k) { case 1: part = "header"; break; case 2: part = "body"; break; case 3: part = "footer"; break; }

            int partCount = Integer.parseInt( (String) request.getParameter(
                "format_" + part + "_count"));
            for (int i = 1; i <= partCount; i++) {
                String type = (String) request.getParameter(part + "_type_" + i);
                Map attrs = new HashMap();
                fmt.addFormatField(request, type,
                                   "format_" + part + "_",
                                   "_" + i,
                                   k );
           }
        }

        ret.setFormat(fmt);

        return ret;
    }


    public PayrollExportData createPayrollExportData(int petId) throws Exception {
        PreparedStatement ps = null;
        ResultSet rs = null;
        String xml = null, name = null;
        try {
            final String petSql = "SELECT pet_xml, pet_name FROM payroll_export_tsk WHERE pet_id = ?";
            ps = conn.prepareStatement(petSql);
            ps.setInt(1, petId);
            rs = ps.executeQuery();
            if (rs.next()) {
                final Clob clob = rs.getClob("pet_xml");
                xml = clob.getSubString(1L, ((int) clob.length()));
                name = rs.getString("pet_name");
            }
        } finally {
            if ((ps != null)) {
               ps.close();
           }
           if ((rs != null)) {
               rs.close();
           }
        }
        return createExport(xml, name);
    }

    public PayrollExportData createPayrollExportDataEmpty() {
        PayrollExportData ret = new PayrollExportData();
        ret.setPlugin("");
        ret.setPetName("");
        for (int i = 0; i < 10; i++) {
            ret.getData().addDataFieldEmpty(DataField.TYPE_FIELD, i);
        }
        for (int i = 0; i < 3; i++) {
            ret.getLogic().addMatchOutputEmpty(i, 3, 1);
        }
        ret.setFormatEmpty();
        return ret;
    }

    private PayrollExportData createExport(String xml, String petName) throws Exception{
        PayrollExportData ret = new PayrollExportData();
        ret.setPetName(petName);

        Document d = validateXml(xml);
        Element e = d.getDocumentElement();
        String plugIn = e.getAttribute("plugin");
        ret.setPlugin(plugIn);

        processData(getSingleElement(e, "data") , ret);

        processLogic(getSingleElement(e, "logic") , ret);

        processFormat(getSingleElement(e, "format") , ret);

        return ret;
    }

    private Document validateXml(String xml) {
        Document d = null;
        try {
            URL url = new PayrollExportTask().getClass().getResource(
                "payroll_schema.properties");
            String schema = readInputStream(url.openStream());

            InputSource isource = new InputSource();
            isource.setCharacterStream(new StringReader(schema + "\n" + xml));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new org.xml.sax.ErrorHandler() {
                public void error(SAXParseException e) throws
                    SAXParseException {
                    throw e;
                }

                public void fatalError(SAXParseException e) throws
                    SAXException {
                    throw e;
                }

                public void warning(SAXParseException e) throws
                    SAXException {
                    throw e;
                }
            });
            d = db.parse(isource);

        }
        catch (SAXException e) {
            throw new NestedRuntimeException(
                "Return stream is not valid XML: " + e.getMessage() , e);
        }
        catch (Throwable e) {
            throw new NestedRuntimeException("Error in parsing payroll XML" + e.getMessage()  , e);
        }
        return d;

    }
    private void processData( Element dataElement, PayrollExportData ped) {
        NodeList nl = dataElement.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) continue;
            if (node.getNodeType() == Node.COMMENT_NODE) continue;
            Element item = (Element)node;

            ped.getData().addDataField(item);
        }
    }

    private void processLogic( Element dataElement, PayrollExportData ped) {
        NodeList nl =  dataElement.getElementsByTagName("match_output");
        for (int i = 0; i < nl.getLength(); i++) {
            MatchOutput matchOutput = new MatchOutput ();
            Element item = (Element)nl.item(i);
            NodeList nlMatch = item.getElementsByTagName("match");
            for (int k = 0; k < nlMatch.getLength(); k++) {
                 Element item2 = (Element)nlMatch.item(k);
                 matchOutput.addMatchOutputField(item2);
            }
            Element outputrow = getSingleElement(item, "output_row");
            NodeList nlOutputRow = item.getElementsByTagName("map");
            for (int k = 0; k < nlOutputRow.getLength(); k++) {
                 Element item2 = (Element)nlOutputRow.item(k);
                 matchOutput.addMatchOutputField(item2);
            }
            ped.getLogic().addMatchOutput(matchOutput);
        }

    }

    private void processFormat( Element element, PayrollExportData ped) {
        Format format = new Format();
        processFormatPart("header", Format.TYPE_HEADER ,
                          getSingleElement(element , "header"), format);
        processFormatPart("body", Format.TYPE_BODY ,
                          getSingleElement(element , "body"), format);
        processFormatPart("footer", Format.TYPE_FOOTER ,
                          getSingleElement(element , "footer"), format);
        ped.setFormat(format );
    }

    private void processFormatPart(String part , int partInd, Element element, Format format) {
        if (element == null) return;
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) continue;
            if (node.getNodeType() == Node.COMMENT_NODE) continue;            
            Element item = (Element)node;

            format.addFormatField(item , partInd);
        }

    }
    private Element getSingleElement(Element e, String s){
        NodeList nl = e.getElementsByTagName(s);
        if( nl.getLength() > 0 ) {
          return (Element)nl.item(0);
        }

        return null;
    }

    private String readInputStream(InputStream is) throws IOException {
        Reader fr = null;
        StringWriter sw = null;
        try {
            fr = new InputStreamReader(is);
            sw = new StringWriter();
            char[] ca = new char[500];
            while(true){
                int i = fr.read(ca);
                if(i==-1) break;
                sw.write(ca,0,i);
            }
            return sw.getBuffer().toString();
        } catch (IOException e) {
            throw new NestedRuntimeException(e);
        } finally {
            if (fr != null) fr.close();
            if (sw != null) sw.close();
        }
    }

    public static class PayrollExportData implements Serializable   {
        private String petName;
        private String plugin;

        private Data data = new Data();
        private Logic logic = new Logic();
        private Format format = new Format();

        public void setPetName(String petName) {
            this.petName  = petName;
        }

        public String getPetName() {
            return petName;
        }

        public void setPlugin(String plugin) {
            this.plugin  = plugin;
        }

        public String getPlugin() {
            return plugin;
        }

        public Logic getLogic() {
            return logic;
        }

        public void setLogic(Logic logic) {
            this.logic = logic;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data ) {
            this.data = data;
        }

        public void setFormat(Format format) {
            this.format  = format;
        }

        public Format getFormat() {
            return format;
        }


        public void addFormatField(HashMap attributes, String type, int hdrBdyFtr) {
            FormatField ff = new FormatField();
            ff.type = type;
            ff.attributes = attributes ;
            switch (hdrBdyFtr) {
                case Format.TYPE_HEADER:
                    format.header.add(ff);
                    break;
                case Format.TYPE_BODY:
                    format.body.add(ff);
                    break;
                case Format.TYPE_FOOTER:
                    format.footer.add(ff);
                    break;
            }
        }

        public void setFormatEmpty() {
            format = new Format();
        }

        public String toString() {
            return "data:" + data + "\n" +
                "logic:" + logic + "\n" +
                "format:" + format + "\n" +
                "plugin:" + plugin + "\n";
        }

        public String createXML() {
            StringBuffer sb = new StringBuffer(200);
            sb.append("<payroll_export plugin='").append(escXML(plugin)).append("'>").append("\n")   ;

            sb.append("<data>").append("\n");
            Iterator iter = getData().dataFields.iterator();
            while (iter.hasNext()) {
                DataField item = (DataField)iter.next();
                sb.append(createXMLForElementData(item.getType() , item , item.attributes));
            }
            sb.append("</data>").append("\n");

            sb.append("<logic>").append("\n");
            Iterator iterLogic = logic.matchOutputs.iterator();
            while (iterLogic.hasNext()) {
                sb.append("<match_output>").append("\n");
                MatchOutput item = (MatchOutput)iterLogic.next();
                List matches = item.matches;
                Iterator iterMatches = matches.iterator();
                while (iterMatches.hasNext()) {
                    MatchOutputField match = (MatchOutputField)iterMatches.next();
                    sb.append(createXMLForElementLogic(match.getType() , match , match.attributes));
                }
                List outputRows = item.outputRows;
                Iterator iterOutputRows = outputRows.iterator();
                sb.append("<output_row>").append("\n");
                while (iterOutputRows.hasNext()) {
                    MatchOutputField map = (MatchOutputField)iterOutputRows.next();
                    sb.append(createXMLForElementLogic(map.getType() , map , map.attributes));
                }
                sb.append("</output_row>").append("\n");
                sb.append("</match_output>").append("\n");
            }
            sb.append("</logic>");
            // *** no escape here yet
            sb.append("<format>").append("\n");
            sb.append("<header>").append("\n");
            sb.append(getFormatPart(Format.TYPE_HEADER));
            sb.append("</header>").append("\n");
            sb.append("<body>").append("\n");
            sb.append(getFormatPart(Format.TYPE_BODY));
            sb.append("</body>").append("\n");
            sb.append("<footer>").append("\n");
            sb.append(getFormatPart(Format.TYPE_FOOTER));
            sb.append("</footer>").append("\n");
            sb.append("</format>").append("\n");
            sb.append("</payroll_export>");
            return sb.toString();
        }

        private String getFormatPart(int part) {
            StringBuffer sb = new StringBuffer(200);
            List items = new ArrayList();
            switch (part) {
                case Format.TYPE_HEADER:  items = format.header;    break;
                case Format.TYPE_BODY:  items = format.body;    break;
                case Format.TYPE_FOOTER:  items = format.footer;    break;
            }
            Iterator iterF = items.iterator();
            while (iterF.hasNext()) {
                FormatField item = (FormatField)iterF.next();
                sb.append(createXMLForElementFormat(item.getType() , item , item.attributes));
            }
            return sb.toString();
        }

        private String createXMLForElementData(String name, DataField item , HashMap attrs) {
            StringBuffer sb = new StringBuffer(200);
            sb.append("<").append(name) ;
            Iterator iterA = item.getAttributeNames().iterator();
            while (iterA.hasNext()) {
                String attr = (String)iterA.next();
                String val = (String)attrs.get(attr);
                if (!StringHelper.isEmpty(val)) {
                    sb.append(" " + attr +
                        "='").append(escXML(val)).append("' ");
                }
            }
            sb.append(" />").append("\n");
            return sb.toString();
        }

        private String createXMLForElementLogic(String name, MatchOutputField item , HashMap attrs) {
            StringBuffer sb = new StringBuffer(200);
            sb.append("<").append(name) ;
            Iterator iterA = item.getAttributeNames().iterator();
            while (iterA.hasNext()) {
                String attr = (String)iterA.next();
                String val = (String)attrs.get(attr);
                if (!StringHelper.isEmpty(val)) {
                    sb.append(" " + attr +
                        "='").append(escXML(val)).append("' ");
                }
            }
            sb.append(" />").append("\n");
            return sb.toString();
        }

        private String createXMLForElementFormat(String name, FormatField item , HashMap attrs) {
            StringBuffer sb = new StringBuffer(200);
            sb.append("<").append(name) ;
            Iterator iterA = item.getAttributeNames().iterator();
            while (iterA.hasNext()) {
                String attr = (String)iterA.next();
                String val = (String)attrs.get(attr);
                if (!StringHelper.isEmpty(val)) {
                    sb.append(" " + attr +
                        "='").append(escXML(val)).append("' ");
                }
            }
            if (item.isTypePCData(name)) {
                sb.append(">").append(escXML(item.PCDataValue)).append("</").append(name).append(">");

            }
            else {
                sb.append(" />").append("\n");
            }
            return sb.toString();
        }

        private String escXML(String val) {
            return XMLHelper.escapeXMLValue(val);
        }


    }

    public static class Data{
        public List dataFields = new ArrayList();

        public void addDataField(DataField df , int index ) {
            dataFields.add(index , df);
        }

        public void addDataFieldEmpty(String type, int index) {
            DataField df = new DataField();
            df.type = type ;
            List attrNames = df.getAttributeNames();
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String attr = (String)iter.next();
                df.attributes.put(attr, "");
            }

            addDataField(df , index ) ;
        }

        public void addDataField(Element item) {
            DataField df = new DataField();
            df.type =item.getNodeName() ;
            List attrNames = df.getAttributeNames();
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String attr = (String)iter.next();
                String val = item.getAttribute(attr);
                if (!StringHelper.isEmpty(val)) {
                    df.attributes.put(attr, val);
                }
            }
            dataFields.add(df) ;
        }

        public void addDataField(HttpServletRequest request,
                                   String typeName,
                                   String attrPrefix,
                                   String attrSuffix,
                                   int hdrBdyFtr) {
            DataField df = new DataField();
            df.type = typeName ;
            List attrNames = df.getAttributeNames();
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String attr = (String)iter.next();
                df.attributes.put(attr, request.getParameter(attrPrefix + attr + attrSuffix));
            }
            dataFields.add(df) ;
        }

        public List getDataFieldNames() {
            List ret = new ArrayList();
            Iterator iter = dataFields.iterator();
            while (iter.hasNext()) {
                DataField item = (DataField)iter.next();
                ret.add((String)item.attributes.get("name"));
            }
            return ret;
        }

        public void removeField(int index) {
            dataFields.remove(index);
        }

    }

    public static class DataField {
        public static final String TYPE_FIELD = "field";
        public static final String TYPE_DUMMYFIELD = "dummy_field";
        public static final String TYPE_SUMMARYFIELD = "summary_field";

        public static List FIELD_ATTRS = new ArrayList();
        public static List DUMMY_FIELD_ATTRS = new ArrayList();
        public static List SUMMARY_FIELD_ATTRS = new ArrayList();

         static {
            FIELD_ATTRS.add("name");
            FIELD_ATTRS.add("group");
            FIELD_ATTRS.add("group_function");
            DUMMY_FIELD_ATTRS.add("name");
            DUMMY_FIELD_ATTRS.add("group");
            DUMMY_FIELD_ATTRS.add("group_function");
            SUMMARY_FIELD_ATTRS.add("name");
            SUMMARY_FIELD_ATTRS.add("field");
            SUMMARY_FIELD_ATTRS.add("summary_function");
        }

        public static Map FIELD_ATTRIBUTES = new HashMap();
        static {
             FIELD_ATTRIBUTES.put(TYPE_FIELD ,FIELD_ATTRS );
             FIELD_ATTRIBUTES.put(TYPE_DUMMYFIELD ,DUMMY_FIELD_ATTRS );
             FIELD_ATTRIBUTES.put(TYPE_SUMMARYFIELD ,SUMMARY_FIELD_ATTRS );
        }

        public static List TYPE_NAMES = new ArrayList();
        static {
            TYPE_NAMES.add(TYPE_FIELD);
            TYPE_NAMES.add(TYPE_DUMMYFIELD);
            TYPE_NAMES.add(TYPE_SUMMARYFIELD);
        }

        public String type;
        public HashMap attributes = new HashMap();

        public List getAttributeNames() {
            return (List)FIELD_ATTRIBUTES.get(type);
        }

        public String getType() {
            return type;
        }

        public String toString() {
            return "type:" + type + "\n" +
                 "attributes:" + attributes + "\n";
        }

    }

    public static class MatchOutput {
        public List matches = new ArrayList();
        public List outputRows = new ArrayList();

        public void addMatchOutputField(HttpServletRequest request,
                                   String typeName,
                                   String attrPrefix,
                                   String attrSuffix) {
            MatchOutputField mof = new MatchOutputField();
            mof.type = typeName ;
            List attrNames = mof.getAttributeNames();
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String attr = (String)iter.next();
                mof.attributes.put(attr, request.getParameter(attrPrefix + attr + attrSuffix));
            }
            if ("match".equals(typeName)) {
                matches.add(mof);
            }
            else if ("map".equals(typeName)) {
                outputRows.add(mof);
            }
        }

        public void addMatchOutputField(Element item) {
            MatchOutputField mof = new MatchOutputField();
            mof.type = item.getNodeName() ;
            List attrNames = mof.getAttributeNames();
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String attr = (String)iter.next();
                String val = item.getAttribute(attr);
                if (!StringHelper.isEmpty(val)) {
                    mof.attributes.put(attr, val);
                }
            }
            if ("match".equals(item.getNodeName())) {
                matches.add(mof);
            }
            else if ("map".equals(item.getNodeName())) {
                outputRows.add(mof);
            }

        }

        public String toString() {
            return "matches" + matches + "\n" +
                "outputRows" + outputRows + "\n";
        }
    }

    public static class Logic{
        public List matchOutputs = new ArrayList();

        public void addMatchOutput(MatchOutput mo , int index ) {
            matchOutputs.add(index , mo);
        }

        public void addMatchOutput(MatchOutput mo) {
            matchOutputs.add(mo);
        }

        public void addMatchOutputEmpty( int index, int matchCount, int outputRowCount) {
            MatchOutput matchOutput = new MatchOutput();
            for (int i = 0; i < matchCount; i++) {
                MatchOutputField match = new MatchOutputField();
                match.type = MatchOutputField.TYPE_MATCH;
                Iterator iterA = match.getAttributeNames().iterator();
                while (iterA.hasNext()) {
                    String attr = (String)iterA.next();
                    match.attributes.put(attr , "");
                }
                matchOutput.matches.add(match);
            }
            for (int i = 0; i < outputRowCount; i++) {
                MatchOutputField map = new MatchOutputField();
                map.type = MatchOutputField.TYPE_MAP;
                Iterator iterA = map.getAttributeNames().iterator();
                while (iterA.hasNext()) {
                    String attr = (String)iterA.next();
                    map.attributes.put(attr , "");
                }
                 matchOutput.outputRows.add(map);
            }
            matchOutputs.add(index, matchOutput);
        }

        public void removeMatchOutput(int index) {
            matchOutputs.remove(index);
        }

    }

    public static class MatchOutputField {
        public static final String TYPE_MATCH = "match";
        public static final String TYPE_MAP = "map";

        public static List MATCH_ATTRS = new ArrayList();
        public static List MAP_ATTRS = new ArrayList();

        static {
            MATCH_ATTRS.add("field");
            MATCH_ATTRS.add("value");
            MAP_ATTRS.add("field");
            MAP_ATTRS.add("value");
        }

        public static Map FIELD_ATTRIBUTES = new HashMap();
        static {
             FIELD_ATTRIBUTES.put(TYPE_MATCH ,MATCH_ATTRS );
             FIELD_ATTRIBUTES.put(TYPE_MAP ,MAP_ATTRS );
        }

        public static List TYPE_NAMES = new ArrayList();
        static {
             TYPE_NAMES.add(TYPE_MATCH);
             TYPE_NAMES.add(TYPE_MAP);
        }

        public String type;
        public HashMap attributes = new HashMap();

        public List getAttributeNames() {
            return (List)FIELD_ATTRIBUTES.get(type);
        }

        public String getType() {
            return type;
        }

        public String toString() {
            return "type:" + type + "\n" +
                 "attributes:" + attributes + "\n";
        }


    }

    public static class Format {
        public static final int TYPE_HEADER = 1;
        public static final int TYPE_BODY = 2;
        public static final int TYPE_FOOTER = 3;

        public List header = new ArrayList();
        public List body = new ArrayList();
        public List footer = new ArrayList();

        public static final List PART_NAMES  = new ArrayList();
        static {
            PART_NAMES.add("header");
            PART_NAMES.add("body");
            PART_NAMES.add("footer");
       }

        public static int resolvePart(String part) {
            return PART_NAMES.indexOf(part) + 1;
        }

        public void addFormatField(FormatField ff , int hdrBdyFtr) {
            switch (hdrBdyFtr) {
                case Format.TYPE_HEADER:
                    header.add(ff);
                    break;
                case Format.TYPE_BODY:
                    body.add(ff);
                    break;
                case Format.TYPE_FOOTER:
                    footer.add(ff);
                    break;
            }
        }

        public void addFormatField(FormatField ff , int hdrBdyFtr, int index) {
            switch (hdrBdyFtr) {
                case Format.TYPE_HEADER:
                    header.add(index , ff);
                    break;
                case Format.TYPE_BODY:
                    body.add(index , ff);
                    break;
                case Format.TYPE_FOOTER:
                    footer.add(index , ff);
                    break;
            }
        }

        public void removeFormatField(int hdrBdyFtr, int index) {
            switch (hdrBdyFtr) {
                case Format.TYPE_HEADER:
                    header.remove(index );
                    break;
                case Format.TYPE_BODY:
                    body.remove(index );
                    break;
                case Format.TYPE_FOOTER:
                    footer.remove(index );
                    break;
            }
        }

        public void addFormatField(HttpServletRequest request,
                                   String typeName,
                                   String attrPrefix,
                                   String attrSuffix,
                                   int hdrBdyFtr) {
            FormatField ff = new FormatField();
            ff.type = typeName ;
            List attrNames = ff.getAttributeNames();
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String attr = (String)iter.next();
                ff.attributes.put(attr, request.getParameter(attrPrefix + attr + attrSuffix));
            }
            if (FormatField.isTypePCData(typeName)) {
                ff.PCDataValue = request.getParameter(attrPrefix + "pcdata" + attrSuffix);
            }

            addFormatField(ff , hdrBdyFtr) ;
        }

        public void addFormatField(Element item, int hdrBdyFtr) {
            FormatField ff = new FormatField();
            ff.type = item.getNodeName() ;
            List attrNames = ff.getAttributeNames();
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String attr = (String)iter.next();
                String val = item.getAttribute(attr);
                if (!StringHelper.isEmpty(val)) {
                    ff.attributes.put(attr, val);
                }
            }
            if (FormatField.isTypePCData(item.getNodeName())) {
                ff.PCDataValue = XMLHelper.getText(item);
            }
            addFormatField(ff , hdrBdyFtr) ;
        }

        public void addFormatFieldEmpty(String type , int hdrBdyFtr, int index) {
            FormatField ff = new FormatField();
            ff.isTemp = true;
            ff.type = type ;
            List attrNames = ff.getAttributeNames();
            Iterator iter = attrNames.iterator();
            while (iter.hasNext()) {
                String attr = (String)iter.next();
                ff.attributes.put(attr, "");
            }
            ff.PCDataValue = "";

            addFormatField(ff , hdrBdyFtr , index ) ;
        }

        public void addFormatFieldEmpty(String type , String hdrBdyFtr, int index) {
            addFormatFieldEmpty(type ,
                                Format.resolvePart(hdrBdyFtr) ,
                                index ) ;
        }

        public void removeFormatField(String hdrBdyFtr, int index) {
            removeFormatField(Format.resolvePart(hdrBdyFtr) ,
                              index ) ;
        }

        public String toString() {
            return "header:" + header + "\n" +
                "body:" + body + "\n" +
                "footer:" + footer + "\n"
                ;
        }

    }

    public static class FormatField {
        public static final String TYPE_STRING = "string";
        public static final String TYPE_CONSTANT = "constant";
        public static final String TYPE_TAB = "tab";
        public static final String TYPE_DATETIME= "datetime";
        public static final String TYPE_NUMBER = "number";
        public static final String TYPE_NEWLINE = "new_line";

        public static List STRING_ATTRS = new ArrayList();
        public static List CONSTANT_ATTRS = new ArrayList();
        public static List DATETIME_ATTRS = new ArrayList();
        public static List NEW_LINE_ATTRS = new ArrayList();
        public static List NUMBER_ATTRS = new ArrayList();
        public static List TAB_ATTRS = new ArrayList();

        static {
            STRING_ATTRS.add("field");
            STRING_ATTRS.add("pad_left");
            STRING_ATTRS.add("pad_right");
            STRING_ATTRS.add("pad_value");
            STRING_ATTRS.add("null");
            CONSTANT_ATTRS.add("pad_left");
            CONSTANT_ATTRS.add("pad_right");
            CONSTANT_ATTRS.add("pad_value");
            NUMBER_ATTRS.add("field");
            NUMBER_ATTRS.add("format");
            NUMBER_ATTRS.add("divide");
            NUMBER_ATTRS.add("null");
            NUMBER_ATTRS.add("pad_left");
            NUMBER_ATTRS.add("pad_right");
            NUMBER_ATTRS.add("pad_value");
            NUMBER_ATTRS.add("percent");
            NUMBER_ATTRS.add("multiply");
            NUMBER_ATTRS.add("field_multiply");
            NEW_LINE_ATTRS.add("CR");
            DATETIME_ATTRS.add("field");
            DATETIME_ATTRS.add("format");
            DATETIME_ATTRS.add("pad_left");
            DATETIME_ATTRS.add("pad_right");
            DATETIME_ATTRS.add("pad_value");
            DATETIME_ATTRS.add("null");
        }
        public static List PCDATA_TYPES = new ArrayList();
        static {
            PCDATA_TYPES.add(TYPE_CONSTANT);
        }

        public static Map FIELD_ATTRIBUTES = new HashMap();
        static {
             FIELD_ATTRIBUTES.put(TYPE_STRING ,STRING_ATTRS );
             FIELD_ATTRIBUTES.put(TYPE_CONSTANT ,CONSTANT_ATTRS );
             FIELD_ATTRIBUTES.put(TYPE_TAB ,TAB_ATTRS );
             FIELD_ATTRIBUTES.put(TYPE_DATETIME ,DATETIME_ATTRS );
             FIELD_ATTRIBUTES.put(TYPE_NUMBER ,NUMBER_ATTRS );
             FIELD_ATTRIBUTES.put(TYPE_NEWLINE , NEW_LINE_ATTRS );
        }

        public static List TYPE_NAMES = new ArrayList();
        static {
             TYPE_NAMES.add(TYPE_STRING);
             TYPE_NAMES.add(TYPE_CONSTANT);
             TYPE_NAMES.add(TYPE_TAB);
             TYPE_NAMES.add(TYPE_DATETIME);
             TYPE_NAMES.add(TYPE_NUMBER);
             TYPE_NAMES.add(TYPE_NEWLINE);
        }

        public String PCDataValue;
        public String type;
        public boolean isTemp = false;
        public HashMap attributes = new HashMap();

        public List getAttributeNames() {
            return (List) FIELD_ATTRIBUTES.get(type);
        }

        public static boolean isTypePCData(String type) {
            return PCDATA_TYPES.indexOf(type) > -1;
        }

        public String getType() {
            return type;
        }

        public boolean isString() {
            return type == TYPE_STRING;
        }
        public boolean isConstant() {
            return type == TYPE_CONSTANT;
        }
        public boolean isTab() {
            return type == TYPE_TAB;
        }

        public boolean isDatetime() {
            return type == TYPE_DATETIME;
        }

        public boolean isNumber() {
            return type == TYPE_NUMBER;
        }

        public boolean isNewLine() {
            return type == TYPE_NEWLINE;
        }

        public String toString() {
            return "type:" + type + "\n" +
                   "attributes:" + attributes + "\n";
        }

    }

    public static void main( String[] args ) throws Exception {
        System.setProperty("junit.db.username" , "workbrain");
        System.setProperty("junit.db.password" , "workbrain");
        System.setProperty("junit.db.url" , "jdbc:oracle:oci8:@iag41dv");
        System.setProperty("junit.db.driver" , "oracle.jdbc.driver.OracleDriver");

        final DBConnection c = com.workbrain.sql.SQLHelper.connectTo();
        c.setAutoCommit( false );
        com.workbrain.security.SecurityService.getCurrentUser();
        WorkbrainSystem.bindDefault(
            new com.workbrain.sql.SQLSource () {
                public java.sql.Connection getConnection() throws SQLException {
                    return c;
                }
            }
        );


        com.workbrain.security.SecurityService.setCurrentClientId("1");
        com.workbrain.security.AuthenticatedUser u =
            new com.workbrain.security.WorkbrainAuthenticatedUser("WORKBRAIN" , 3 , 3 , "", true,"1" , false , 1);;
        u.setUserNameActual("WORKBRAIN");
        com.workbrain.security.SecurityService.setCurrentUser(u);

        final int psTskId = 2;
        PayrollExportEditor pee = new PayrollExportEditor(c);
        PayrollExportData ped = pee.createPayrollExportData(psTskId) ;
        //System.out.println("ped \n" + ped);

        System.out.println("before \n" + ped.createXML());
        pee.savePayrollExportData(ped.createXML() , psTskId);
        ped = pee.createPayrollExportData(psTskId) ;

        System.out.println("after \n" + ped.createXML());
        if (c!=null) c.rollback();
        //System.out.println("job " + j);
        //c.close();
    }


}


