package com.wbiag.app.modules.blackout;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;

public class WbiagBlkoutDatData extends RecordData {
    private Integer wiblkdId;
    private String wiblkdName;
    private String wiblkdDesc;
    private Integer wiblkdgId;
    private java.util.Date wiblkdStartDate;
    private java.util.Date wiblkdEndDate;
    private Integer wbtId;
    private Integer jobId;
    private String wiblkdFlag1;
    private String wiblkdFlag2;
    private String wiblkdFlag3;
    private String wiblkdFlag4;
    private String wiblkdFlag5;
    private String wiblkdUdf1;
    private String wiblkdUdf2;
    private String wiblkdUdf3;
    private String wiblkdUdf4;
    private String wiblkdUdf5;

    public RecordData newInstance() {
        return new WbiagBlkoutDatData ();
    }
    public Integer getWiblkdId(){
        return wiblkdId;
    }

    public void setWiblkdId(Integer v){
        wiblkdId=v;
    }

    public String getWiblkdName(){
        return wiblkdName;
    }

    public void setWiblkdName(String v){
        wiblkdName=v;
    }

    public String getWiblkdDesc(){
        return wiblkdDesc;
    }

    public void setWiblkdDesc(String v){
        wiblkdDesc=v;
    }

    public Integer getWiblkdgId(){
        return wiblkdgId;
    }

    public void setWiblkdgId(Integer v){
        wiblkdgId=v;
    }

    public java.util.Date getWiblkdStartDate(){
        return wiblkdStartDate;
    }

    public void setWiblkdStartDate(java.util.Date v){
        wiblkdStartDate=v;
    }

    public java.util.Date getWiblkdEndDate(){
        return wiblkdEndDate;
    }

    public void setWiblkdEndDate(java.util.Date v){
        wiblkdEndDate=v;
    }

    public Integer getWbtId(){
        return wbtId;
    }

    public void setWbtId(Integer v){
        wbtId=v;
    }

    public Integer getJobId(){
        return jobId;
    }

    public void setJobId(Integer v){
        jobId=v;
    }

    public String getWiblkdFlag1(){
        return wiblkdFlag1;
    }

    public void setWiblkdFlag1(String v){
        wiblkdFlag1=v;
    }

    public String getWiblkdFlag2(){
        return wiblkdFlag2;
    }

    public void setWiblkdFlag2(String v){
        wiblkdFlag2=v;
    }

    public String getWiblkdFlag3(){
        return wiblkdFlag3;
    }

    public void setWiblkdFlag3(String v){
        wiblkdFlag3=v;
    }

    public String getWiblkdFlag4(){
        return wiblkdFlag4;
    }

    public void setWiblkdFlag4(String v){
        wiblkdFlag4=v;
    }

    public String getWiblkdFlag5(){
        return wiblkdFlag5;
    }

    public void setWiblkdFlag5(String v){
        wiblkdFlag5=v;
    }

    public String getWiblkdUdf1(){
        return wiblkdUdf1;
    }

    public void setWiblkdUdf1(String v){
        wiblkdUdf1=v;
    }

    public String getWiblkdUdf2(){
        return wiblkdUdf2;
    }

    public void setWiblkdUdf2(String v){
        wiblkdUdf2=v;
    }

    public String getWiblkdUdf3(){
        return wiblkdUdf3;
    }

    public void setWiblkdUdf3(String v){
        wiblkdUdf3=v;
    }

    public String getWiblkdUdf4(){
        return wiblkdUdf4;
    }

    public void setWiblkdUdf4(String v){
        wiblkdUdf4=v;
    }

    public String getWiblkdUdf5(){
        return wiblkdUdf5;
    }

    public void setWiblkdUdf5(String v){
        wiblkdUdf5=v;
    }


    public String toString() {
        String s = "WbiagBlkoutDatData:\n" +
            "  wiblkdId = " + wiblkdId + "\n" +
            "  wiblkdName = " + wiblkdName + "\n" +
            "  wiblkdDesc = " + wiblkdDesc + "\n" +
            "  wiblkdgId = " + wiblkdgId + "\n" +
            "  wiblkdStartDate = " + wiblkdStartDate + "\n" +
            "  wiblkdEndDate = " + wiblkdEndDate + "\n" +
            "  wbtId = " + wbtId + "\n" +
            "  jobId = " + jobId + "\n" +
            "  wiblkdFlag1 = " + wiblkdFlag1 + "\n" +
            "  wiblkdFlag2 = " + wiblkdFlag2 + "\n" +
            "  wiblkdFlag3 = " + wiblkdFlag3 + "\n" +
            "  wiblkdFlag4 = " + wiblkdFlag4 + "\n" +
            "  wiblkdFlag5 = " + wiblkdFlag5 + "\n" +
            "  wiblkdUdf1 = " + wiblkdUdf1 + "\n" +
            "  wiblkdUdf2 = " + wiblkdUdf2 + "\n" +
            "  wiblkdUdf3 = " + wiblkdUdf3 + "\n" +
            "  wiblkdUdf4 = " + wiblkdUdf4 + "\n" +
            "  wiblkdUdf5 = " + wiblkdUdf5;
        return s;
    }
}

