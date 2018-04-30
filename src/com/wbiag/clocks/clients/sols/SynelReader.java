package com.wbiag.clocks.clients.sols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.wbiag.clocks.clients.sols.lib.CRCBuilder;
import com.wbiag.clocks.clients.sols.lib.InitializationException;
import com.wbiag.clocks.clients.sols.lib.Settings;
import com.wbiag.clocks.clients.sols.lib.SynelCRC;
import com.wbiag.clocks.clients.sols.lib.SynelProtocolStrings;
import com.wbiag.clocks.clients.sols.lib.Utilities;

public class SynelReader {
    
    public static boolean active = false;
    private static Logger logger = Logger.getLogger(SynelReader.class);
    
    public static int pollPeriod = 12;
    public static int disconnectTime = 7;
    public static int hbPeriod = 60;
    public static int badgeLength = 6;
    public static String destIP ="";
    public static int destPort = 3734;
    
    protected String ipAddress = null;
    protected String readerName = null;
    protected ArrayList qBuffer;
    private int qCounter;
    protected long initialSleep;
    /** Local port for communication with WBCS.*/
    protected int myPort = 0;
    /** Local IP address.*/
    protected InetAddress myIpAddress;
    /** Remote IP address.*/
    protected static InetAddress remoteIpAddress;
    /** Socket to communicate with Clock Server.*/    
    protected Socket clientSocket;
    /** Input stream for the socket.*/
    protected InputStream inStream;
    /** Output stream for the socket.*/
    protected OutputStream outStream;
    private CRCBuilder crc = new SynelCRC();
    

    
    public SynelShadowThread shadowThread;
    public SynelInteractiveThread interactiveThread;

    class SynelShadowThread extends Thread {
        SynelShadowThread (String threadName){
            super(threadName);
        }
        public void run() {
            
            try{
                logger.info("Initial sleep timout: "+initialSleep);
                sleep(initialSleep);               
            }catch (InterruptedException ie){
                logger.debug("Initial sleep interrupted. Received interrupted exception.");
                if (!interactiveThread.isAlive()||interactiveThread.isInterrupted()){
                    logger.debug("Interactive thread also has beed stoped or interrupted. Ending this thread.");
                    return;  
                }
            }
            
            while (true){
                try  {
                    sleep(pollPeriod*1000); 
                   if (!interactiveThread.isBusy && active && queueCounter()>0){
                       if (logger.isDebugEnabled()){
                           logger.debug("Start processing queue. Queue size is:" + queueCounter());
                       }
                       processQueue();                           
                       logger.info("Number of delayed swpes left in the buffer: " + queueCounter());
                   }
                } catch (InterruptedException ie){
                    logger.debug("Reseting the thread. Received interrupted exception.");
                    if (!interactiveThread.isAlive()||interactiveThread.isInterrupted()){
                        logger.debug("Interactive thread has beed stoped or interrupted as well. Ending this thread.");
                        break;  
                    } else {
                        continue;
                    }
                }
            }
        }
    }
    
    class SynelInteractiveThread extends Thread {
        SynelInteractiveThread (String threadName){
            super(threadName);
        }
        public boolean isBusy;
        public void run() {
            try{
                logger.info("Initial sleep timout: "+initialSleep);
                sleep(initialSleep);               
            }catch (InterruptedException ie){
                logger.debug("Initial sleep interrupted. Trying to stopping this and shadow threads.");
                shadowThread.interrupt();
                isBusy = false;
                return;  
            }
            
            while (true){
                try  {

                    if (active){
                        isBusy = true;
                        shadowThread.interrupt();
                        doHeartBeat();
                        isBusy = false;
                        shadowThread.interrupt();                        
                    }
                    sleep(hbPeriod*1000);  
                } catch (InterruptedException ie){
                    logger.debug("Received interrupted exception. Trying to stopping this and shadow threads.");
                    shadowThread.interrupt();
                    isBusy = false;
                    break;  
                }

            }
        }
    }
    
  
    SynelReader(String rdrAddress, String rdrName)throws InitializationException{
        this.ipAddress = rdrAddress;
        this.readerName = rdrName;
        qBuffer = new ArrayList();
        qCounter = 0;
        initialSleep = Math.round(Math.random()*hbPeriod)*1000;
        try{
            myIpAddress = InetAddress.getByName(ipAddress);
        }
        catch (IOException e) {
            logger.error("Exception resolving IP address: "+ipAddress +" : ",e);
            throw new InitializationException();
        }
        shadowThread = new SynelShadowThread("SynelShadowThread @ " + ipAddress);
        interactiveThread = new SynelInteractiveThread("SynelInteractiveThread @ " + ipAddress);
        
        shadowThread.start();
        interactiveThread.start();

    }
    
    
    public static void init(Settings settings)throws InitializationException{
        String key="";
        try {
            pollPeriod = Integer.parseInt(settings.get(key="reader.synel.pollPeriod"));
            disconnectTime = Integer.parseInt(settings.get(key="reader.synel.disconnectTime"));
            hbPeriod = Integer.parseInt(settings.get(key="reader.synel.hbPeriod"));
            badgeLength = Integer.parseInt(settings.get(key="reader.synel.badgeLength"));
            destPort = Integer.parseInt(settings.get(key="reader.synel.destPort"));
            destIP = settings.get(key="reader.synel.destIP");
            if (null==destIP || destIP.equalsIgnoreCase("")){
                throw new NumberFormatException("IP Address is missing");
            }
            
        } catch (NumberFormatException nfe) {
            logger.error("Cannot parse "+key+" ! Parameter file is incomplete or incorrectly formatted.");
            throw new InitializationException("Cannot initialize Synel Reader.");
        }

            try{
            remoteIpAddress = InetAddress.getByName(destIP);
        }
        catch (IOException e) {
            logger.error("Cannot resolve destination IP address (reader.synel.destIP): "+destIP);
            throw new InitializationException("Cannot initialize Synel Reader.");
        }
    }
     
    public String getIP(){
        return this.ipAddress;
    }
    
    public void addToQBuffer(Swipe s){
        SynelBufferRecord currentRecord = null;
        
        if (!qBuffer.isEmpty()){
            currentRecord = (SynelBufferRecord) qBuffer.get(qBuffer.size()-1);
            if (currentRecord.addSwipe(s)){
                qBuffer.set(qBuffer.size()-1, currentRecord);
                qCounter++;
                return;
            } else {
                if (logger.isDebugEnabled()){
                    logger.debug(readerName+"@" + ipAddress + ": Current buffer record is full adding to a new one.");
                }
            }
        }

        currentRecord = new SynelBufferRecord();
        if (currentRecord.addSwipe(s)){
            qBuffer.add(currentRecord);
            qCounter++;
            if (logger.isDebugEnabled()){
                logger.debug("Reader " + readerName +" has added to the buffer queue swipe: "+s);
            }
        } else {
            logger.warn("Warning, reader: "+readerName+"@" + ipAddress+" cannot store swipe: " +s);
        }
        
    }
    
    public int queueCounter(){
        if (qBuffer.size()==0){
            return 0;
        } else {
            return this.qCounter;
        }
    }
    
    void doHeartBeat(){
        String reply;

        try {
            
            if (clientSocket == null) {
                connect();
            }
                    sendStringCommand(syHeartbeatSwipe());
                    if (logger.isDebugEnabled()){
                        logger.debug("HeartBeat sent from port: " + myPort);
                    }
                    if (crcOK(reply = getReply()))
                    {
                        sendRawCommand(ackMessage());
                        if (reply.startsWith("E0")){
                            processTimeAdjustment(reply);
                        }else if ((reply.startsWith(SynelProtocolStrings.SERVER_PREAMBLE_LONG) && reply.substring(6,7).equals(SynelProtocolStrings.READER_RESPONSE_ERROR))
                                  || (reply.startsWith(SynelProtocolStrings.SERVER_PREAMBLE) && reply.substring(3,4).equals(SynelProtocolStrings.READER_RESPONSE_ERROR))
                                  ){
                            logger.warn("This reader is not registered on the server");
                        }else {
                            processHBRespond(reply);
                            if (crcOK(reply = getReply())){
                                sendRawCommand(ackMessage());
                                if (reply.startsWith("E0")){
                                    processTimeAdjustment(reply);
                                }
                            } else {
                                    if (!reply.equals("")){
                                        logger.warn("CRC Failed!");
                                    }
                            }
                        }
                    } else {
                            logger.warn("CRC Failed!");
                    }
                    disconnect();

        }
        catch (IOException e) {
                  try {
                          logger.warn("Cannot communicate to server from port: " + myPort + " ", e);
                      disconnect();
                  }
                  catch (IOException ex){
                          logger.warn("Cannot close connection from port: " + myPort + " ", ex);
                      clientSocket = null;
                  }
        }

    }
    
    synchronized void  sendStringCommand(String command) throws IOException{
        if (logger.isDebugEnabled()){
            logger.debug("Sending: " +crc.formatString(command));
        }
        outStream.write( crc.formatString(command));
    }

    
    synchronized void connect() throws IOException{
        if (clientSocket == null){
            if (myPort>=99999){
                myPort=0;
            }
            clientSocket = new Socket(remoteIpAddress, destPort, myIpAddress, ++myPort);
            clientSocket.setSoTimeout(disconnectTime*1000);
            inStream = clientSocket.getInputStream();
            outStream = clientSocket.getOutputStream();
            if (logger.isDebugEnabled()){
                logger.debug("Opened new socket from port: " +myPort);
            }
        } else {
            if (logger.isDebugEnabled()){
                logger.debug("Socket is still open on port: "+myPort);
            }
        }

    }

    synchronized void disconnect() throws IOException{
       if (clientSocket != null){
            outStream.close();
            inStream.close();
            clientSocket.close();
            clientSocket = null;
            if (logger.isDebugEnabled()){
                logger.debug("Closed socket on port: "+myPort);
            }
        } else{
            if (logger.isDebugEnabled()){
                logger.debug("Socket is already closed on port: " +myPort);
            }
            
        }
   }
   
   String syHeartbeatSwipe(){
       String hb = SynelProtocolStrings.QUERY_STRING + SynelProtocolStrings.EPROM_VERSION + SynelProtocolStrings.HEARTBEAT_TYPE +
       Utilities.currentSynelDateTimeToString();
       if (logger.isDebugEnabled()){
           logger.debug("Generated heartbeat: " + hb);
       }
       return hb;
   }
   
   String getReply() throws IOException{
       StringBuffer sb = new StringBuffer("");
       int inByte = 0;
       while ((inByte != -1) && (inByte != 4)) {
           inByte = inStream.read();
           if (inByte != -1 && inByte != 4) {
               sb.append((char)inByte);
           }
       }
       if (logger.isDebugEnabled()&& sb.length()!=0){
           logger.debug("Port: " +myPort + " Received reply: " + sb.toString());
       }
       return sb.toString();
   }

   boolean crcOK(String input){
       if(input.length()<5){
           return false;
       }
       else
           if ((new String(crc.getCRCBytes(input.substring(0,(input.length()-4))))).equals(input.substring((input.length()-4),input.length())))
               return true;
           else
               if (logger.isDebugEnabled()){
                   logger.debug("CRC should be: "+ new String(crc.getCRCBytes(input.substring(0,(input.length()-4)))));
               }
               return false;
   } 
   
   byte[] ackMessage(){
       byte[] ack = {0x06, 0x30};
       return crc.formatBytes( ack );
   }
   
   private void processTimeAdjustment(String input){

   }
   
   private void processHBRespond(String input){

   }
   
   synchronized void sendRawCommand(byte[] command) throws IOException{
       if (outStream != null) {
           if (logger.isDebugEnabled()){
               logger.debug("Sending: " +command);
           }
           outStream.write(command);
       } else {
           throw new IOException("Reader " + myIpAddress + ":" + myPort + " cannot communicate to: " + remoteIpAddress + ":" + destPort + " Output Stream is closed.");
       }
   }
   private void processQueue(){
       String Reply;

       try {
           if (clientSocket == null) {
               connect();
           }

           for (int i=0;!qBuffer.isEmpty();){
               if(logger.isDebugEnabled()){
                   logger.debug(("Sending buffer record: "+(SynelBufferRecord)qBuffer.get(i)).toString());   
               }
               sendStringCommand(((SynelBufferRecord)qBuffer.get(i)).toString());
               if (crcOK(Reply = getReply())){
                   if (Reply.startsWith(SynelProtocolStrings.DELAYED_ACK)){
                       sendRawCommand(ackMessage());
                       qCounter = qCounter - ((SynelBufferRecord)qBuffer.get(i)).getTransactionCounter();
                       qBuffer.remove(0);
                       if (crcOK(Reply = getReply())){
                           if (Reply.startsWith(SynelProtocolStrings.DELAYED_ASK_FOR_MORE_DATA)){
                               if(qBuffer.isEmpty()){
                                   sendStringCommand(SynelProtocolStrings.DELAYED_NO_MORE_DATA);
                               }
                               continue;
                           } else {
                               break;
                           }
                       } else {
                           break;                                   
                       }

                   } else if (Reply.startsWith(SynelProtocolStrings.DELAYED_ASK_FOR_MORE_DATA)){
                       if(++i>=qBuffer.size()){
                           sendStringCommand(SynelProtocolStrings.DELAYED_NO_MORE_DATA);
                           break;
                       }
                   }
               }
               else {
                 break;
               }  
           }

               disconnect();

       }
       catch (IOException e) {
                 try {
                     if(!(new ClosedByInterruptException()).equals(e.getCause())){
                         logger.warn("Cannot communicate to server from port: " + myPort + " ",e);
                         disconnect();
                     } else {
                         logger.debug("Communitation interrupted on port: " + myPort);
                     }
                 }
                 catch (IOException ex){
                     logger.warn("Cannot close connection from port: " + myPort + " ",ex);
                     clientSocket = null;
                 }
       }
   }


}
