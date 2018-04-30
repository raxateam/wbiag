package com.wbiag.app.modules.availability;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;
/**
 * @deprecated As of 5.0, use core Make Availability Reportable Task.
 */
public class WbiagAvalDetailData extends RecordData {
    private int wavdId;
    private int wavsId;
    private java.util.Date wavdActStTime;
    private java.util.Date wavdActEndTime;
    private int wavdActMinutes;


    public RecordData newInstance() {
        return new WbiagAvalDetailData ();
    }
    public int getWavdId(){
        return wavdId;
    }

    public void setWavdId(int v){
        wavdId=v;
    }

    public int getWavsId(){
        return wavsId;
    }

    public void setWavsId(int v){
        wavsId=v;
    }

    public java.util.Date getWavdActStTime(){
        return wavdActStTime;
    }

    public void setWavdActStTime(java.util.Date v){
        wavdActStTime=v;
    }

    public java.util.Date getWavdActEndTime(){
        return wavdActEndTime;
    }

    public void setWavdActEndTime(java.util.Date v){
        wavdActEndTime=v;
    }

    public int getWavdActMinutes(){
        return wavdActMinutes;
    }

    public void setWavdActMinutes(int v){
        wavdActMinutes=v;
    }

    public String toString() {
        String s = "WbiagAvalDetailData:\n" +
            "  wavdId = " + wavdId + "\n" +
            "  wavsId = " + wavsId + "\n" +
            "  wavdActStTime = " + wavdActStTime + "\n" +
            "  wavdActEndTime = " + wavdActEndTime + "\n" +
            "  wavdActMinutes = " + wavdActMinutes;
        return s;
    }

    public String getPrimaryKeyName() {
        return WbiagAvailabilityAccess.WBIAG_AVAL_DETAIL_PRIKEY;
    }

    public String getSequenceName() {
        return WbiagAvailabilityAccess.WBIAG_AVAL_DETAIL_SEQ;
    }

}
