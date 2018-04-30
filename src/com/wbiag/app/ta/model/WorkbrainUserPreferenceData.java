package com.wbiag.app.ta.model;

import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;
import java.util.*;

public class WorkbrainUserPreferenceData extends RecordData {
    private int wbuprefId;
    private int wbprefId;
    private int wbuId;
    private String wbuprefValue;

    public RecordData newInstance() {
        return new WorkbrainUserPreferenceData ();
    }
    public int getWbuprefId(){
        return wbuprefId;
    }

    public void setWbuprefId(int v){
        wbuprefId=v;
    }

    public int getWbprefId(){
        return wbprefId;
    }

    public void setWbprefId(int v){
        wbprefId=v;
    }

    public int getWbuId(){
        return wbuId;
    }

    public void setWbuId(int v){
        wbuId=v;
    }

    public String getWbuprefValue(){
        return wbuprefValue;
    }

    public void setWbuprefValue(String v){
        wbuprefValue=v;

    }

    public String getPrimaryKeyName() {
        return "wbupref_id";
    }

    public String getSequenceName() {
        return "seq_wbupref_id";
    }

    public String toString() {
        String s = "WorkbrainUserPreferenceData:\n" +
            "  wbuprefId = " + wbuprefId + "\n" +
            "  wbprefId = " + wbprefId + "\n" +
            "  wbuId = " + wbuId + "\n" +
            "  wbuprefValue = " + wbuprefValue ;
        return s;
    }
}
