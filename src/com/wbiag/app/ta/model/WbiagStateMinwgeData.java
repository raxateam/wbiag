package com.wbiag.app.ta.model;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;

public class WbiagStateMinwgeData extends RecordData {
    private int wistmId;
    private int wistId;
    private java.util.Date wistmEffDate;
    private double wistmMinWage;
    private String wistmFlag1;
    private String wistmFlag2;
    private String wistmFlag3;
    private String wistmFlag4;
    private String wistmFlag5;
    private String wistmUdf1;
    private String wistmUdf2;
    private String wistmUdf3;
    private String wistmUdf4;
    private String wistmUdf5;
    private String wistmFlag6;
    private String wistmFlag7;
    private String wistmFlag8;
    private String wistmFlag9;
    private String wistmFlag10;
    private String wistmUdf6;
    private String wistmUdf7;
    private String wistmUdf8;
    private String wistmUdf9;
    private String wistmUdf10;

    public RecordData newInstance() {
        return new WbiagStateMinwgeData ();
    }
    public int getWistmId(){
        return wistmId;
    }

    public void setWistmId(int v){
        wistmId=v;
    }

    public int getWistId(){
        return wistId;
    }

    public void setWistId(int v){
        wistId=v;
    }

    public java.util.Date getWistmEffDate(){
        return wistmEffDate;
    }

    public void setWistmEffDate(java.util.Date v){
        wistmEffDate=v;
    }

    public double getWistmMinWage(){
        return wistmMinWage;
    }

    public void setWistmMinWage(double v){
        wistmMinWage=v;
    }

    public String getWistmFlag1(){
        return wistmFlag1;
    }

    public void setWistmFlag1(String v){
        wistmFlag1=v;
    }

    public String getWistmFlag2(){
        return wistmFlag2;
    }

    public void setWistmFlag2(String v){
        wistmFlag2=v;
    }

    public String getWistmFlag3(){
        return wistmFlag3;
    }

    public void setWistmFlag3(String v){
        wistmFlag3=v;
    }

    public String getWistmFlag4(){
        return wistmFlag4;
    }

    public void setWistmFlag4(String v){
        wistmFlag4=v;
    }

    public String getWistmFlag5(){
        return wistmFlag5;
    }

    public void setWistmFlag5(String v){
        wistmFlag5=v;
    }

    public String getWistmUdf1(){
        return wistmUdf1;
    }

    public void setWistmUdf1(String v){
        wistmUdf1=v;
    }

    public String getWistmUdf2(){
        return wistmUdf2;
    }

    public void setWistmUdf2(String v){
        wistmUdf2=v;
    }

    public String getWistmUdf3(){
        return wistmUdf3;
    }

    public void setWistmUdf3(String v){
        wistmUdf3=v;
    }

    public String getWistmUdf4(){
        return wistmUdf4;
    }

    public void setWistmUdf4(String v){
        wistmUdf4=v;
    }

    public String getWistmUdf5(){
        return wistmUdf5;
    }

    public void setWistmUdf5(String v){
        wistmUdf5=v;
    }

    public String getWistmFlag6(){
        return wistmFlag6;
    }

    public void setWistmFlag6(String v){
        wistmFlag6=v;
    }

    public String getWistmFlag7(){
        return wistmFlag7;
    }

    public void setWistmFlag7(String v){
        wistmFlag7=v;
    }

    public String getWistmFlag8(){
        return wistmFlag8;
    }

    public void setWistmFlag8(String v){
        wistmFlag8=v;
    }

    public String getWistmFlag9(){
        return wistmFlag9;
    }

    public void setWistmFlag9(String v){
        wistmFlag9=v;
    }

    public String getWistmFlag10(){
        return wistmFlag10;
    }

    public void setWistmFlag10(String v){
        wistmFlag10=v;
    }

    public String getWistmUdf6(){
        return wistmUdf6;
    }

    public void setWistmUdf6(String v){
        wistmUdf6=v;
    }

    public String getWistmUdf7(){
        return wistmUdf7;
    }

    public void setWistmUdf7(String v){
        wistmUdf7=v;
    }

    public String getWistmUdf8(){
        return wistmUdf8;
    }

    public void setWistmUdf8(String v){
        wistmUdf8=v;
    }

    public String getWistmUdf9(){
        return wistmUdf9;
    }

    public void setWistmUdf9(String v){
        wistmUdf9=v;
    }

    public String getWistmUdf10(){
        return wistmUdf10;
    }

    public void setWistmUdf10(String v){
        wistmUdf10=v;
    }


    public String toString() {
        String s = "WbiagStateMinwgeData:\n" +
            "  wistmId = " + wistmId + "\n" +
            "  wistId = " + wistId + "\n" +
            "  wistmEffDate = " + wistmEffDate + "\n" +
            "  wistmMinWage = " + wistmMinWage + "\n" +
            "  wistmFlag1 = " + wistmFlag1 + "\n" +
            "  wistmFlag2 = " + wistmFlag2 + "\n" +
            "  wistmFlag3 = " + wistmFlag3 + "\n" +
            "  wistmFlag4 = " + wistmFlag4 + "\n" +
            "  wistmFlag5 = " + wistmFlag5 + "\n" +
            "  wistmUdf1 = " + wistmUdf1 + "\n" +
            "  wistmUdf2 = " + wistmUdf2 + "\n" +
            "  wistmUdf3 = " + wistmUdf3 + "\n" +
            "  wistmUdf4 = " + wistmUdf4 + "\n" +
            "  wistmUdf5 = " + wistmUdf5 + "\n" +
            "  wistmFlag6 = " + wistmFlag6 + "\n" +
            "  wistmFlag7 = " + wistmFlag7 + "\n" +
            "  wistmFlag8 = " + wistmFlag8 + "\n" +
            "  wistmFlag9 = " + wistmFlag9 + "\n" +
            "  wistmFlag10 = " + wistmFlag10 + "\n" +
            "  wistmUdf6 = " + wistmUdf6 + "\n" +
            "  wistmUdf7 = " + wistmUdf7 + "\n" +
            "  wistmUdf8 = " + wistmUdf8 + "\n" +
            "  wistmUdf9 = " + wistmUdf9 + "\n" +
            "  wistmUdf10 = " + wistmUdf10 + "\n" ;
        return s;
    }
}
