package com.wbiag.app.modules.availability;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;
/**
 * @deprecated As of 5.0, use core Make Availability Reportable Task.
 */
public class WbiagAvalSummaryData extends RecordData {
    private int wavsId;
    private int empId;
    private java.util.Date wavsDate;
    private int wavsActMinutes;
    private Integer wbtId;

    public RecordData newInstance() {
        return new WbiagAvalSummaryData ();
    }
    public int getWavsId(){
        return wavsId;
    }

    public void setWavsId(int v){
        wavsId=v;
    }

    public int getEmpId(){
        return empId;
    }

    public void setEmpId(int v){
        empId=v;
    }

    public java.util.Date getWavsDate(){
        return wavsDate;
    }

    public void setWavsDate(java.util.Date v){
        wavsDate=v;
    }

    public int getWavsActMinutes(){
        return wavsActMinutes;
    }

    public void setWavsActMinutes(int v){
        wavsActMinutes=v;
    }

    public Integer getWbtId(){
        return wbtId;
    }

    public void setWbtId(Integer v){
        wbtId=v;
    }

    public String toString() {
        String s = "WbiagAvalSummaryData:\n" +
            "  wavsId = " + wavsId + "\n" +
            "  empId = " + empId + "\n" +
            "  wavsDate = " + wavsDate + "\n" +
            "  wavsActMinutes = " + wavsActMinutes + "\n" +
            "  wbtId = " + wbtId ;
        return s;
    }
}
