package com.wbiag.app.ta.model;

import com.workbrain.app.ta.model.RecordData;

public class WbiagSpzoneDateEffData extends RecordData {
    private int spdeId;
    private int spzoneId;
    private java.util.Date spdeEffDate;
    private String spdeValue;

    public RecordData newInstance() {
        return new WbiagSpzoneDateEffData ();
    }
    public int getSpdeId(){
        return spdeId;
    }

    public void setSpdeId(int v){
        spdeId=v;
    }

    public int getSpzoneId(){
        return spzoneId;
    }

    public void setSpzoneId(int v){
        spzoneId=v;
    }

    public java.util.Date getSpdeEffDate(){
        return spdeEffDate;
    }

    public void setSpdeEffDate(java.util.Date v){
        spdeEffDate=v;
    }

    public String getSpdeValue(){
        return spdeValue;
    }

    public void setSpdeValue(String v){
        spdeValue=v;
    }


    public String toString() {
        String s = "WbiagSpzoneDateEffData:\n" +
            "  spdeId = " + spdeId + "\n" +
            "  spzoneId = " + spzoneId + "\n" +
            "  spdeEffDate = " + spdeEffDate + "\n" +
            "  spdeValue = " + spdeValue;
        return s;
    }
}
