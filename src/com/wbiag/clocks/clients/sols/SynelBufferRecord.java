package com.wbiag.clocks.clients.sols;

import com.wbiag.clocks.clients.sols.lib.SynelProtocolStrings;
import com.wbiag.clocks.clients.sols.lib.Utilities;


public class SynelBufferRecord {
    protected String dateStamp;
    protected String[] transaction;
    private int transactionCounter;
    private int bufferLength;
    
    SynelBufferRecord(){
        dateStamp=null;
        transaction = new String[12];
        transactionCounter = 0;
        bufferLength = 16;
    }
    
    public int bufferLength(){
        return bufferLength;
    }
    
    public int getTransactionCounter(){
        return transactionCounter;
    }

    public void setDateStamp(String dStamp){
        this.dateStamp=dStamp;
    }
    
    public void resetTransactionCounter(){
        this.transactionCounter=0;
        this.bufferLength=16;
    }
    
    public boolean addSwipe(Swipe s){
        String bSwipe = bufferedSwipe(s);
        if (null == s || bufferLength+bSwipe.length() > 123 || bSwipe.equals("")){
            return false;
        } else {
            transaction[transactionCounter] = bSwipe;
            bufferLength=bufferLength+bSwipe.length();
            if (transactionCounter == 0) {
                setDateStamp(s.dateStamp);
            }
            transactionCounter ++;
            return true;
        }
    }
    
    public String bufferedSwipe(Swipe s) {
        String result = "";
        if (s.type.equals(Swipe.TYPE_IN)) {
            result = SynelProtocolStrings.DELAYED_X_SWIPE_TYPE + SynelProtocolStrings.ON_SWIPE_TYPE + Utilities.formatSynelTimeToString(s.timeStamp,s.dst) 
                            + Utilities.formatString(s.badge, SynelReader.badgeLength, Utilities.PAD_LEFT, '0')+"1";
        } else if (s.type.equals(Swipe.TYPE_OUT)) {
            result = SynelProtocolStrings.DELAYED_X_SWIPE_TYPE + SynelProtocolStrings.OFF_SWIPE_TYPE + Utilities.formatSynelTimeToString(s.timeStamp,s.dst) 
                            + Utilities.formatString(s.badge, SynelReader.badgeLength, Utilities.PAD_LEFT, '0')+"2";
        } else if (s.type.equals(Swipe.TYPE_TCODE)) {
            result = SynelProtocolStrings.DELAYED_X_SWIPE_TYPE + SynelProtocolStrings.TIMECODE_SWIPE_TYPE + Utilities.formatSynelTimeToString(s.timeStamp,s.dst)
                            + Utilities.formatString(s.badge, SynelReader.badgeLength, Utilities.PAD_LEFT, '0')
                            + Utilities.formatString(s.extraData, SynelProtocolStrings.LABOUR_METRIC_LENGTH, Utilities.PAD_RIGHT, ' ');
        }else if (s.type.equals(Swipe.TYPE_LOGICAL)) {
            result = Utilities.formatSynelTimeToString(s.timeStamp,s.dst)
            + Utilities.formatString(s.badge, SynelReader.badgeLength, Utilities.PAD_LEFT, '0');
            }
        
        return result;
    }
    
    public String toString(){
        String result = SynelProtocolStrings.DELAYED_STRING + SynelProtocolStrings.EPROM_VERSION +dateStamp;
        for(int i=0; i<transactionCounter; i++){
            result = result + transaction[i];
        }
        return result;
    }

}
