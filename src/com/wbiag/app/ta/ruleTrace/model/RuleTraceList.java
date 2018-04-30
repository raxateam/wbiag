package com.wbiag.app.ta.ruleTrace.model;

import com.wbiag.util.NameValueList;
import com.wbiag.app.ta.ruleTrace.engine.*;
import com.wbiag.app.ta.ruleTrace.model.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import java.util.*;
import java.io.*;
import java.text.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.apache.xerces.parsers.*;
import org.apache.log4j.*;

/**
 *  @deprecated Core as of 5.0.3.0
 */
public class RuleTraceList extends TypedList{

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(RuleTraceList.class);

    public RuleTraceList() {
        super(RuleNodeExt.class);
    }


    public String toXML(RuleTraceConfig config){
        StringBuffer sb = new StringBuffer();
        //sb.append("<?xml version='1.0' encoding='UTF-8'?>").append("\n") ;
        sb.append("<ruleTraceList>").append("\n") ;
        Iterator iter = iterator();
        while (iter.hasNext()) {
            RuleNodeExt trc = (RuleNodeExt)iter.next();
            sb.append(trc.toXML(config));
        }
        sb.append("</ruleTraceList>").append("\n") ;
        return sb.toString();
    }

    public static RuleTraceList fromXML(String xml, RuleTraceConfig config){
        RuleTraceList ret = new RuleTraceList();
        Document d = loadXML(xml);

        Element ruleTraceList = (Element) d.getElementsByTagName("ruleTraceList").item(0);
        NodeList rules = ruleTraceList.getElementsByTagName("rule");
        for (int i = 0; i< rules.getLength(); i++) {

            Element rulTrc = (Element)rules.item(i);
            String ruleName = rulTrc.getAttribute("name");
            RuleNodeExt ruleNode = new RuleNodeExt((CalculationGroup)null);
            ret.add(ruleNode);
            ruleNode.setRuleName(ruleName);

            NodeList csetNodeList = rulTrc.getElementsByTagName("conditionset");
            for (int j = 0; j < csetNodeList.getLength(); j++) {
                Element condsetElement = (Element)csetNodeList.item(j);
                String name = condsetElement.getAttribute("name");
                ConditionSetNodeExt cset = new ConditionSetNodeExt(ruleNode);
                cset.setDescription(name);
                ConditionSetNodeExt csetPrev = ruleNode.getLastConditionSetNode();
                ruleNode.addConditionSetNode(cset);

                NodeList csetParamNodeList = condsetElement.getElementsByTagName("parameter");
                Parameters csetParams = new Parameters ();
                cset.setParameters(csetParams);
                for (int k = 0; k < csetParamNodeList.getLength(); k++) {
                    Element parmElement = (Element)csetParamNodeList.item(k);
                    if (!"conditionset".equals(parmElement.getParentNode().getNodeName())) {
                        continue;
                    }
                    String parmName = parmElement.getAttribute("name");
                    String parmVal = parmElement.getAttribute("value");
                    csetParams.addParameter(parmName ,  parmVal);
                }
                NodeList condNodeList = condsetElement.getElementsByTagName("condition");
                for (int k = 0; k < condNodeList.getLength(); k++) {
                    Element condElement = (Element)condNodeList.item(k);
                    String condName = condElement.getAttribute("name");
                    boolean evalC = Boolean.valueOf(condElement.getAttribute("eval")).booleanValue() ;
                    ConditionNodeExt condNode = new ConditionNodeExt (cset);
                    cset.addConditionNode(condNode);
                    condNode.setName(condName);
                    condNode.setEval(evalC);

                    NodeList condParamNodeList = condElement.getElementsByTagName("parameter");
                    Parameters condParams = new Parameters ();
                    condNode.setParameters(condParams);
                    for (int l = 0; l < condParamNodeList.getLength(); l++) {
                        Element parmElement = (Element)condParamNodeList.item(l);
                        String parmName = parmElement.getAttribute("name");
                        String parmVal = parmElement.getAttribute("value");
                        condParams.addParameter(parmName ,  parmVal);
                    }
                }

                NodeList dataNodeList = condsetElement.getElementsByTagName("data");
                if (dataNodeList.getLength() > 0) {
                    RuleTraceContent ruleTraceContent = new RuleTraceContent();
                    Element dataElement = (Element)dataNodeList.item(0);
                    cset.setRuleTraceContent(ruleTraceContent);
                    NodeList empNL = dataElement.getElementsByTagName("employee");
                    if (empNL.getLength() > 0) {
                        ruleTraceContent.setEmployeeData(getNameValueList(
                            empNL, config.getEmployeeFields()));
                    }
                    NodeList wrksNL = dataElement.getElementsByTagName("workSummary");
                    if (wrksNL.getLength() > 0) {
                        ruleTraceContent.setWorkSummaryData(
                            getNameValueList(wrksNL,
                                             config.getWorkSummaryFields()));
                    }
                    NodeList wrkdsNL = dataElement.getElementsByTagName( "workDetails");
                    if (wrkdsNL.getLength() > 0) {
                        List wds = new ArrayList();
                        NodeList wrkd = ( (Element) wrkdsNL.item(0)).
                            getElementsByTagName("workDetail");
                        for (int l = 0; l < wrkd.getLength(); l++) {
                            Element wd = (Element) wrkd.item(l);
                            wds.add(getNameValueList(wd,
                                config.getWorkDetailFields()));
                        }
                        ruleTraceContent.setWorkDetails(wds);
                    }
                    NodeList wrkpsNL = dataElement.getElementsByTagName("workPremiums");
                    if (wrkpsNL.getLength() > 0) {
                        List wps = new ArrayList();
                        NodeList wrkp = ( (Element) wrkpsNL.item(0)).
                            getElementsByTagName("workPremium");
                        for (int l = 0; l < wrkp.getLength(); l++) {
                            Element wd = (Element) wrkp.item(l);
                            wps.add(getNameValueList(wd,
                                config.getWorkDetailFields()));
                        }
                        ruleTraceContent.setWorkPremiums(wps);
                    }
                    NodeList empBalsNL = dataElement.getElementsByTagName("employeeBalances");
                    if (empBalsNL.getLength() > 0) {
                        NameValueList empBals = new NameValueList();
                        NodeList balNL = ( (Element) empBalsNL.item(0)).
                            getElementsByTagName("balance");
                        for (int l = 0; l < balNL.getLength(); l++) {
                            Element bal = (Element) balNL.item(l);
                            empBals.add(bal.getAttribute("name") , bal.getAttribute("value"));
                        }
                        ruleTraceContent.setEmployeeBalances(empBals);
                    }
                    // *** check if content changed compared to previous cset or previous rule's last cset
                    if (csetPrev != null) {
                        ruleTraceContent.setChangedFromPrevious(
                            !ruleTraceContent.equalsForContent(csetPrev.getRuleTraceContent()));
                    }
                    else {
                        if (ret.size() > 1) {
                            RuleNodeExt rextPrev = (RuleNodeExt) ret.get(ret.
                                size() - 2);
                            csetPrev = rextPrev.getLastConditionSetNode();
                            if (csetPrev != null) {
                                //if (logger.isDebugEnabled()) logger.debug("comparing \n" + ruleTraceContent);
                                //if (logger.isDebugEnabled()) logger.debug("to \n" + csetPrev.getRuleTraceContent());
                                ruleTraceContent.setChangedFromPrevious(
                                    !ruleTraceContent.equalsForContent(csetPrev.getRuleTraceContent()));
                            }
                        }
                    }
                }

            }
        }
        return ret;
    }


    private static NameValueList getNameValueList(NodeList nodeList, List fields) {
        NameValueList namVals = new NameValueList();
        NamedNodeMap empAttrs = nodeList.item(0).getAttributes() ;
        Iterator iter = fields.iterator();
        while (iter.hasNext()) {
            String fld = (String)iter.next();
            Node item = empAttrs.getNamedItem(fld);
            namVals.add(new NameValue(item.getNodeName() , item.getNodeValue())) ;
        }
        return namVals;
    }

    private static NameValueList getNameValueList(Element element, List fields) {
        NameValueList namVals = new NameValueList();
        Iterator iter = fields.iterator();
        while (iter.hasNext()) {
            String fld = (String)iter.next();
            namVals.add(new NameValue(fld , element.getAttribute(fld))) ;
        }
        return namVals;
    }

    private static Document loadXML(String xml) {
        try {
            InputSource isource = new InputSource();
            isource.setCharacterStream(new StringReader(xml));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            //dbf.setValidating(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new org.xml.sax.ErrorHandler(){
                public void error(SAXParseException e) throws SAXParseException{
                    throw e;
                }
                public void fatalError(SAXParseException e) throws SAXException{
                    throw e;
                }
                public void warning(SAXParseException e) throws SAXException{
                    throw e;
                }
            });
            Document d = db.parse(isource);
            return d;
        } catch (SAXException e) {
            throw new NestedRuntimeException("Return stream is not valid XML: "
                    + e.getMessage(), e);
        } catch (Exception e) {
            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(e);}
            throw new NestedRuntimeException(e);
        }
    }






}


