package com.wbiag.app.ta.model;

import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;
import java.util.*;

public class EmployeeDateEffRdrgrpData extends RecordData{

    private int wdrgId;
    private int empId;
    private int wdrgRdrgrpId;
    private Date wdrgStartDate;
    private Date wdrgEndDate;
    private String wdrgComments;
    

    public RecordData newInstance() {
        return new EmployeeDateEffRdrgrpData();
    }

    public int getWdrgId(){
        return wdrgId;
    }

    public void setWdrgId(int v){
        wdrgId=v;
    }

    public int getEmpId(){
        return empId;
    }

    public void setEmpId(int v){
        empId=v;
    }
    
    public int getWdrgRdrgrpId(){
        return wdrgRdrgrpId;
    }

    public void setWdrgRdrgrpId(int v){
        wdrgRdrgrpId=v;
    }

    public Date getWdrgStartDate(){
        return wdrgStartDate;
    }

    public void setWdrgStartDate(Date v){
        wdrgStartDate=v;
    }
    
    public Date getWdrgEndDate(){
        return wdrgEndDate;
    }

    public void setWdrgEndDate(Date v){
        wdrgEndDate=v;
    }

    public String getWdrgComments(){
        return wdrgComments;
    }

    public void setWdrgComments(String v){
        if (!StringHelper.isEmpty(v) && v.length() > 40) {
            v = v.substring(0 , 40);
        }
        wdrgComments=v;
    }

    public String toString(){
          return
            "wdrgId=" + wdrgId + "\n" +
            "empId=" + empId + "\n" +
            "startDate=" + wdrgStartDate + "\n" +
            "endDate=" + wdrgEndDate + "\n" +
            "comments=" + wdrgComments + "\n" ;
    }
}
