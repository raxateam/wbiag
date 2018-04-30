package com.wbiag.clocks.clients.sols;

public class Swipe {

    public static final String TYPE_IN = "01";
    public static final String TYPE_OUT = "02";
    public static final String TYPE_LOGICAL = "10";
    public static final String TYPE_TCODE = "06";

    public String type = "00";
    public String badge = "000000";
    public String extraData = "";
    public String readerName = "";
    public String dateStamp = "YYYYMMDD";
    public String timeStamp = "HHMMSS";
    public boolean dst = false;

    public String toString(){
        return " ReaderName:"+this.readerName+" badge:"+this.badge+" type:"+this.type+" extraData:"
                +this.extraData+" timeStamp:"+this.timeStamp+" dateStamp:"+this.dateStamp+" dst:"+this.dst;
        
    }
}
