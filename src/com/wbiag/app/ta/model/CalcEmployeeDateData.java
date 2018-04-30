package com.wbiag.app.ta.model;

import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;
import java.util.*;

public class CalcEmployeeDateData extends RecordData{

    private int cedId;
    private int empId;
    private Date cedWorkDate;
    private String cedMessage;

    public RecordData newInstance() {
        return new CalcEmployeeDateData();
    }

    public int getCedId(){
        return cedId;
    }

    public void setCedId(int v){
        cedId=v;
    }

    public int getEmpId(){
        return empId;
    }

    public void setEmpId(int v){
        empId=v;
    }

    public Date getCedWorkDate(){
        return cedWorkDate;
    }

    public void setCedWorkDate(Date v){
        cedWorkDate=v;
    }

    public String getCedMessage(){
        return cedMessage;
    }

    public void setCedMessage(String v){
        if (!StringHelper.isEmpty(v) && v.length() > 40) {
            v = v.substring(0 , 40);
        }
        cedMessage=v;
    }

    public String toString(){
          return
            "cedId=" + cedId + "\n" +
            "empId=" + empId + "\n" +
            "cedWorkDate=" + cedWorkDate + "\n" +
            "cedMessage=" + cedMessage + "\n" ;
    }
}
