package com.wbiag.app.ta.model;

import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;
import java.util.*;

public class CalcLogData extends RecordData {
    private int calclogId;
    private int wrksId;
    private java.util.Date calclogDate;
    private String calclogMessage;

    public RecordData newInstance() {
        return new CalcLogData ();
    }
    public int getCalclogId(){
        return calclogId;
    }

    public void setCalclogId(int v){
        calclogId=v;
    }

    public int getWrksId(){
        return wrksId;
    }

    public void setWrksId(int v){
        wrksId=v;
    }

    public java.util.Date getCalclogDate(){
        return calclogDate;
    }

    public void setCalclogDate(java.util.Date v){
        calclogDate=v;
    }

    public String getCalclogMessage(){
        return calclogMessage;
    }

    public void setCalclogMessage(String v){
        calclogMessage=v;
    }

    /**
     * For generatesPrimaryKeyValue stuff
     * @return
     */
    public String getPrimaryKeyName(){
        return "calclog_id";
    }

    public String getSequenceName(){
        return "seq_calclog_id";
    }


    public String toString() {
        String s = "CalcLogData:\n" +
            "  calclogId = " + calclogId + "\n" +
            "  wrksId = " + wrksId + "\n" +
            "  calclogDate = " + calclogDate + "\n" +
            "  calclogMessage = " + calclogMessage + "\n" ;
        return s;
    }
}
