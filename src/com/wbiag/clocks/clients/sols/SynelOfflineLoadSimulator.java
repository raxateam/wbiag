package com.wbiag.clocks.clients.sols;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.wbiag.clocks.clients.sols.lib.InitializationException;
import com.wbiag.clocks.clients.sols.lib.Settings;

public class SynelOfflineLoadSimulator {
    
    /** log config file location. */
    public static String LOG_LOCATION = "conf/log.conf";
    public static String SETTINGS_LOCATION = "conf/sols.conf";
    private static Logger logger = Logger.getLogger(SynelOfflineLoadSimulator.class);

    public static String VERSION = "1.0";
    
    public static Settings settings;
    public static HashMap readerList = new HashMap();
    public static boolean noGUI = false;
    
    /**
     * @param arg0
     */
    public static void main(String[] arg0) {
        // TODO Auto-generated method stub
        
        processCmdArguments(arg0);
        
        try {
            init();

            loadReaders(settings.get("inbound.xml.reader"));
            logger.info("Number of reader addresses loaded: "+readerList.size());

            loadTransactions(settings.get("inbound.xml.transaction"));
            logger.info("Transactions XML loaded.");

            
            Iterator i = readerList.values().iterator();
            while(i.hasNext()){
                SynelReader currentReader =(SynelReader)i.next();
                logger.info("Reader "+currentReader.ipAddress+"/"+currentReader.readerName+" queue counter: "+currentReader.queueCounter());
            }
            
            if (noGUI){
                SynelReader.active = true;
                
                i = readerList.values().iterator();
                SynelReader currentReader;
                while(i.hasNext()){
                    currentReader =(SynelReader)i.next();
                    while (currentReader.queueCounter()>0){
                        Thread.sleep(SynelReader.pollPeriod*1000);
                    }
                    logger.debug("Stopping reader threads for "+currentReader.ipAddress+"/"+currentReader.readerName+" queue counter: "+currentReader.queueCounter());
                    currentReader.interactiveThread.interrupt();
                }
            } else {
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        SolsGUI.createAndShowGUI();
                    }
                });
            }
 
        }catch (Exception e){
            SynelReader.active = false;
            logger.fatal("Fatal exception! Quiting Synel Offline Load Simulator. ",e);
            Iterator i = readerList.values().iterator();
            while(i.hasNext()){
                SynelReader currentReader =(SynelReader)i.next();
                logger.debug("Stopping reader threads for "+currentReader.ipAddress+"/"+currentReader.readerName+" queue counter: "+currentReader.queueCounter());
                currentReader.interactiveThread.interrupt();
            }
        } finally {
            if (noGUI){
                try{
                    Thread.sleep(SynelReader.pollPeriod*1000);
                }catch(InterruptedException ie){
                    logger.info("Main thread received interrupted exception: ",ie);
                }
                logger.info(" Exiting Synel Offline Load Simulator. ");                
            }
        }
    }
    
    protected static void init() throws InitializationException {
        PropertyConfigurator.configure(LOG_LOCATION);
        logger.info("Starting Synel Offline Load Simulator version: "+VERSION);
        
            Settings.init(SETTINGS_LOCATION);
            settings = Settings.instance();
            SynelReader.init(settings);
            if (settings.get("inbound.xml.reader")==null||settings.get("inbound.xml.reader")=="")
            {
                logger.error("inbound.xml.reader XML file has not been specified in sols.conf !");
                throw new InitializationException();    
            }
            if (settings.get("inbound.xml.transaction")==null||settings.get("inbound.xml.transaction")=="")
            {
                logger.error("inbound.xml.transaction XML file has not been specified in sols.conf !");
                throw new InitializationException();    
            }
            if (settings.getBooleanSetting("noGUI", false)&&!noGUI)
            {
                logger.info("Setting noGUI is set to true. Initiating non-GUI mode.");
                noGUI = true;
            }

            
    }
    
    
    protected static void loadReaders  (String xmlFileName)throws Exception{
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new InputStreamReader(new FileInputStream(xmlFileName), settings.getAppDataEncoding())));
        Document xmlDoc = parser.getDocument(); 

        //Create an ArrayList of Reader objects
        String address = null;
        String name = null;
        
        NodeList readerNodeList = xmlDoc.getElementsByTagName("Rdr");
        for (int i = 0; i < readerNodeList.getLength(); i++) {
            Node readerNode = readerNodeList.item(i);

            //Create and fill a Reader object
            NamedNodeMap readerAttributes = readerNode.getAttributes();
            
            name = readerAttributes.getNamedItem("name") != null ? readerAttributes.getNamedItem("name").getNodeValue() : "";
            if (name.trim().equals("")) {
                logger.warn("Error parsing readers XML file!\nCould not find reader.name");
                continue;
            }
            address = readerAttributes.getNamedItem("addr") != null ? readerAttributes.getNamedItem("addr").getNodeValue() : "";
            if (address.trim().equals("")) {
                logger.warn("Error parsing readers XML file!\nCould not find reader address :"+ readerAttributes);
                continue;
            } else {
                try{
                    readerList.put(name, new SynelReader(address,name));
                    if (logger.isDebugEnabled()){
                        logger.debug("Added Reader: " + address + "/" + name);
                    }
                } catch (InitializationException ie){
                    logger.error("Error creating a reader: "+ address + "/" + name);
                }
                
                    
            }
        } //end for
    }
    
    protected static void loadTransactions  (String xmlFileName)throws Exception{
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(new InputStreamReader(new FileInputStream(xmlFileName), settings.getAppDataEncoding())));
        Document xmlDoc = parser.getDocument(); 

        //Create an ArrayList of Reader objects
        logger.info("Loading XML Transactions from: " + xmlFileName);

        NodeList clockNodeList = xmlDoc.getElementsByTagName("Clock");


        for (int i = 0; i < clockNodeList.getLength(); i++) {

            Node nodeType = null;
            Node nodeExtraInfo = null;
            Node nodeDst = null;
            Node nodeRdrName = null;
            Node badgeNo = null;
            Node nodeTime = null;
            Swipe currentSwipe = new Swipe();
            
            Node OneClockNode = clockNodeList.item(i);
            NamedNodeMap clockAttributes = OneClockNode.getAttributes();
            NodeList childElementsList =OneClockNode.getChildNodes();
            nodeType = clockAttributes.getNamedItem("type");
            nodeRdrName = clockAttributes.getNamedItem("rdrName");
            badgeNo = clockAttributes.getNamedItem("badgeNo");
            nodeTime = clockAttributes.getNamedItem("time");
            for (int j = 0; j < childElementsList.getLength(); j++) {
                if (childElementsList.item(j).getNodeName().equals("NValPair")){
                    NamedNodeMap NValPairAttributes = childElementsList.item(j).getAttributes();
                    String NValPairName = NValPairAttributes.getNamedItem("vName").getNodeValue();
                    if (NValPairName.equalsIgnoreCase("TCode")) {
                        nodeExtraInfo = NValPairAttributes.getNamedItem("value");
                    }else if (NValPairName.equalsIgnoreCase("Dst")) {
                        nodeDst = NValPairAttributes.getNamedItem("value");
                    }
                }
            }
            
            if ( null==nodeType || null==nodeRdrName || null==badgeNo || null==nodeTime || nodeTime.getNodeValue().length()!=14 ||(nodeType.getNodeValue() == Swipe.TYPE_TCODE && null==nodeExtraInfo.getNodeValue())){
                logger.warn("Cannot parse swipe! RdrName: " + nodeRdrName.getNodeValue() +" Badge: " + badgeNo.getNodeValue()+ " Type: " + nodeType.getNodeValue()+ " ExtraData: " + nodeExtraInfo.getNodeValue()+ " TimeStamp: " + nodeTime.getNodeValue());
            } else { 
                currentSwipe.type = nodeType.getNodeValue();
                currentSwipe.badge = badgeNo.getNodeValue();
                if ( null != nodeExtraInfo){
                    currentSwipe.extraData = nodeExtraInfo.getNodeValue();
                }
                currentSwipe.readerName = nodeRdrName.getNodeValue();
                currentSwipe.dateStamp = nodeTime.getNodeValue().substring(0, 8);
                currentSwipe.timeStamp = nodeTime.getNodeValue().substring(8, 14);
                if(null!=nodeDst&&nodeDst.getNodeValue().equalsIgnoreCase("T")){
                    currentSwipe.dst = true;
                }
                if ( currentSwipe.badge.length()>SynelReader.badgeLength){
                    logger.warn("Incorrect badge length! Swipe ignored: " + currentSwipe.toString());
                    continue;
                }
                if(null!=readerList.get(currentSwipe.readerName)){
                    ((SynelReader)readerList.get(currentSwipe.readerName)).addToQBuffer(currentSwipe);
                } else {
                    logger.warn("Cannot find the reader:"+currentSwipe.readerName);
                }
            }

        }

    }    
    
   
    protected static void processCmdArguments(String[] arg0){
        for (int i = 0; i < arg0.length; i++ ) {
            if (arg0[i].startsWith("-s:")) {
                //settings
                SETTINGS_LOCATION = arg0[i].substring(3);
            }
            if (arg0[i].startsWith("-l:")) {
                //log settings
                LOG_LOCATION = arg0[i].substring(3);
            }
            if (arg0[i].startsWith("-noGUI")) {
                //log settings
                logger.info("Command line switch -noGUI detected. Initiating non-GUI mode.");
                noGUI = true;
            }
        }      
    }
}
