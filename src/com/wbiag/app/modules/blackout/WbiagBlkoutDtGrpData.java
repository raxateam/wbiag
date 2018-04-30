package com.wbiag.app.modules.blackout;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;

public class WbiagBlkoutDtGrpData extends RecordData {
    private Integer wiblkdgId;
    private String wiblkdgName;
    private String wiblkdgDesc;
    private java.util.Date wiblkdgStartDate;
    private java.util.Date wiblkdgEndDate;
    private Integer wbtId;
    private Integer wbttId;
    private Integer jobId;
    private String wiblkdgFlag1;
    private String wiblkdgFlag2;
    private String wiblkdgFlag3;
    private String wiblkdgFlag4;
    private String wiblkdgFlag5;
    private String wiblkdgUdf1;
    private String wiblkdgUdf2;
    private String wiblkdgUdf3;
    private String wiblkdgUdf4;
    private String wiblkdgUdf5;

    public RecordData newInstance() {
        return new WbiagBlkoutDtGrpData ();
    }
    public Integer getWiblkdgId(){
        return wiblkdgId;
    }

    public void setWiblkdgId(Integer v){
        wiblkdgId=v;
    }

    public String getWiblkdgName(){
        return wiblkdgName;
    }

    public void setWiblkdgName(String v){
        wiblkdgName=v;
    }

    public String getWiblkdgDesc(){
        return wiblkdgDesc;
    }

    public void setWiblkdgDesc(String v){
        wiblkdgDesc=v;
    }

    public java.util.Date getWiblkdgStartDate(){
        return wiblkdgStartDate;
    }

    public void setWiblkdgStartDate(java.util.Date v){
        wiblkdgStartDate=v;
    }

    public java.util.Date getWiblkdgEndDate(){
        return wiblkdgEndDate;
    }

    public void setWiblkdgEndDate(java.util.Date v){
        wiblkdgEndDate=v;
    }

    public Integer getWbtId(){
        return wbtId;
    }

    public void setWbtId(Integer v){
        wbtId=v;
    }

    public Integer getWbttId(){
        return wbttId;
    }

    public void setWbttId(Integer v){
        wbttId=v;
    }

    public Integer getJobId(){
        return jobId;
    }

    public void setJobId(Integer v){
        jobId=v;
    }

    public String getWiblkdgFlag1(){
        return wiblkdgFlag1;
    }

    public void setWiblkdgFlag1(String v){
        wiblkdgFlag1=v;
    }

    public String getWiblkdgFlag2(){
        return wiblkdgFlag2;
    }

    public void setWiblkdgFlag2(String v){
        wiblkdgFlag2=v;
    }

    public String getWiblkdgFlag3(){
        return wiblkdgFlag3;
    }

    public void setWiblkdgFlag3(String v){
        wiblkdgFlag3=v;
    }

    public String getWiblkdgFlag4(){
        return wiblkdgFlag4;
    }

    public void setWiblkdgFlag4(String v){
        wiblkdgFlag4=v;
    }

    public String getWiblkdgFlag5(){
        return wiblkdgFlag5;
    }

    public void setWiblkdgFlag5(String v){
        wiblkdgFlag5=v;
    }

    public String getWiblkdgUdf1(){
        return wiblkdgUdf1;
    }

    public void setWiblkdgUdf1(String v){
        wiblkdgUdf1=v;
    }

    public String getWiblkdgUdf2(){
        return wiblkdgUdf2;
    }

    public void setWiblkdgUdf2(String v){
        wiblkdgUdf2=v;
    }

    public String getWiblkdgUdf3(){
        return wiblkdgUdf3;
    }

    public void setWiblkdgUdf3(String v){
        wiblkdgUdf3=v;
    }

    public String getWiblkdgUdf4(){
        return wiblkdgUdf4;
    }

    public void setWiblkdgUdf4(String v){
        wiblkdgUdf4=v;
    }

    public String getWiblkdgUdf5(){
        return wiblkdgUdf5;
    }

    public void setWiblkdgUdf5(String v){
        wiblkdgUdf5=v;
    }


    public String toString() {
        String s = "WbiagBlkoutDtGrpData:\n" +
            "  wiblkdgId = " + wiblkdgId + "\n" +
            "  wiblkdgName = " + wiblkdgName + "\n" +
            "  wiblkdgDesc = " + wiblkdgDesc + "\n" +
            "  wiblkdgStartDate = " + wiblkdgStartDate + "\n" +
            "  wiblkdgEndDate = " + wiblkdgEndDate + "\n" +
            "  wbtId = " + wbtId + "\n" +
            "  jobId = " + jobId + "\n" +
            "  wiblkdgFlag1 = " + wiblkdgFlag1 + "\n" +
            "  wiblkdgFlag2 = " + wiblkdgFlag2 + "\n" +
            "  wiblkdgFlag3 = " + wiblkdgFlag3 + "\n" +
            "  wiblkdgFlag4 = " + wiblkdgFlag4 + "\n" +
            "  wiblkdgFlag5 = " + wiblkdgFlag5 + "\n" +
            "  wiblkdgUdf1 = " + wiblkdgUdf1 + "\n" +
            "  wiblkdgUdf2 = " + wiblkdgUdf2 + "\n" +
            "  wiblkdgUdf3 = " + wiblkdgUdf3 + "\n" +
            "  wiblkdgUdf4 = " + wiblkdgUdf4 + "\n" +
            "  wiblkdgUdf5 = " + wiblkdgUdf5;
        return s;
    }
}


