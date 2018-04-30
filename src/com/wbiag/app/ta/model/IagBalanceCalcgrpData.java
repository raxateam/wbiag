package com.wbiag.app.ta.model;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;

public class IagBalanceCalcgrpData extends RecordData {
    private int ibcgId;
    private int balId;
    private int baltypId;
    private int calcgrpId;
    private double ibcgBalMin;
    private double ibcgBalMax;
    private String ibcgFlag1;
    private String ibcgFlag2;
    private String ibcgFlag3;
    private String ibcgFlag4;
    private String ibcgFlag5;
    private String ibcgUdf1;
    private String ibcgUdf2;
    private String ibcgUdf3;
    private String ibcgUdf4;
    private String ibcgUdf5;
    private String ibcgDesc;
    private String ibcgFlag6;
    private String ibcgFlag7;
    private String ibcgFlag8;
    private String ibcgFlag9;
    private String ibcgFlag10;
    private String ibcgUdf6;
    private String ibcgUdf7;
    private String ibcgUdf8;
    private String ibcgUdf9;
    private String ibcgUdf10;

    public RecordData newInstance() {
        return new IagBalanceCalcgrpData ();
    }

    public int getIbcgId(){
        return ibcgId;
    }

    public void setIbcgId(int v){
        ibcgId=v;
    }

    public int getBalId(){
        return balId;
    }

    public void setBalId(int v){
        balId=v;
    }

    public int getBaltypId(){
        return baltypId;
    }

    public void setBaltypId(int v){
        baltypId=v;
    }

    public int getCalcgrpId(){
        return calcgrpId;
    }

    public void setCalcgrpId(int v){
        calcgrpId=v;
    }

    public double getIbcgBalMin(){
        return ibcgBalMin;
    }

    public void setIbcgBalMin(double v){
        ibcgBalMin=v;
    }

    public double getIbcgBalMax(){
        return ibcgBalMax;
    }

    public void setIbcgBalMax(double v){
        ibcgBalMax=v;
    }

    public String getIbcgFlag1(){
        return ibcgFlag1;
    }

    public void setIbcgFlag1(String v){
        ibcgFlag1=v;
    }

    public String getIbcgFlag2(){
        return ibcgFlag2;
    }

    public void setIbcgFlag2(String v){
        ibcgFlag2=v;
    }

    public String getIbcgFlag3(){
        return ibcgFlag3;
    }

    public void setIbcgFlag3(String v){
        ibcgFlag3=v;
    }

    public String getIbcgFlag4(){
        return ibcgFlag4;
    }

    public void setIbcgFlag4(String v){
        ibcgFlag4=v;
    }

    public String getIbcgFlag5(){
        return ibcgFlag5;
    }

    public void setIbcgFlag5(String v){
        ibcgFlag5=v;
    }

    public String getIbcgUdf1(){
        return ibcgUdf1;
    }

    public void setIbcgUdf1(String v){
        ibcgUdf1=v;
    }

    public String getIbcgUdf2(){
        return ibcgUdf2;
    }

    public void setIbcgUdf2(String v){
        ibcgUdf2=v;
    }

    public String getIbcgUdf3(){
        return ibcgUdf3;
    }

    public void setIbcgUdf3(String v){
        ibcgUdf3=v;
    }

    public String getIbcgUdf4(){
        return ibcgUdf4;
    }

    public void setIbcgUdf4(String v){
        ibcgUdf4=v;
    }

    public String getIbcgUdf5(){
        return ibcgUdf5;
    }

    public void setIbcgUdf5(String v){
        ibcgUdf5=v;
    }

    public String getIbcgDesc(){
        return ibcgDesc;
    }

    public void setIbcgDesc(String v){
        ibcgDesc=v;
    }

    public String getIbcgFlag6(){
        return ibcgFlag6;
    }

    public void setIbcgFlag6(String v){
        ibcgFlag6=v;
    }

    public String getIbcgFlag7(){
        return ibcgFlag7;
    }

    public void setIbcgFlag7(String v){
        ibcgFlag7=v;
    }

    public String getIbcgFlag8(){
        return ibcgFlag8;
    }

    public void setIbcgFlag8(String v){
        ibcgFlag8=v;
    }

    public String getIbcgFlag9(){
        return ibcgFlag9;
    }

    public void setIbcgFlag9(String v){
        ibcgFlag9=v;
    }

    public String getIbcgFlag10(){
        return ibcgFlag10;
    }

    public void setIbcgFlag10(String v){
        ibcgFlag10=v;
    }

    public String getIbcgUdf6(){
        return ibcgUdf6;
    }

    public void setIbcgUdf6(String v){
        ibcgUdf6=v;
    }

    public String getIbcgUdf7(){
        return ibcgUdf7;
    }

    public void setIbcgUdf7(String v){
        ibcgUdf7=v;
    }

    public String getIbcgUdf8(){
        return ibcgUdf8;
    }

    public void setIbcgUdf8(String v){
        ibcgUdf8=v;
    }

    public String getIbcgUdf9(){
        return ibcgUdf9;
    }

    public void setIbcgUdf9(String v){
        ibcgUdf9=v;
    }

    public String getIbcgUdf10(){
        return ibcgUdf10;
    }

    public void setIbcgUdf10(String v){
        ibcgUdf10=v;
    }

    public String toString() {
        String s = "IagBalanceCalcgrpData:\n" +
            "  ibcgId = " + ibcgId + "\n" +
            "  balId = " + balId + "\n" +
            "  baltypId = " + baltypId + "\n" +
            "  calcgrpId = " + calcgrpId + "\n" +
            "  ibcgBalMin = " + ibcgBalMin + "\n" +
            "  ibcgBalMax = " + ibcgBalMax + "\n" +
            "  ibcgFlag1 = " + ibcgFlag1 + "\n" +
            "  ibcgFlag2 = " + ibcgFlag2 + "\n" +
            "  ibcgFlag3 = " + ibcgFlag3 + "\n" +
            "  ibcgFlag4 = " + ibcgFlag4 + "\n" +
            "  ibcgFlag5 = " + ibcgFlag5 + "\n" +
            "  ibcgUdf1 = " + ibcgUdf1 + "\n" +
            "  ibcgUdf2 = " + ibcgUdf2 + "\n" +
            "  ibcgUdf3 = " + ibcgUdf3 + "\n" +
            "  ibcgUdf4 = " + ibcgUdf4 + "\n" +
            "  ibcgUdf5 = " + ibcgUdf5 + "\n" +
            "  ibcgDesc = " + ibcgDesc + "\n" +
            "  ibcgFlag6 = " + ibcgFlag6 + "\n" +
            "  ibcgFlag7 = " + ibcgFlag7 + "\n" +
            "  ibcgFlag8 = " + ibcgFlag8 + "\n" +
            "  ibcgFlag9 = " + ibcgFlag9 + "\n" +
            "  ibcgFlag10 = " + ibcgFlag10 + "\n" +
            "  ibcgUdf6 = " + ibcgUdf6 + "\n" +
            "  ibcgUdf7 = " + ibcgUdf7 + "\n" +
            "  ibcgUdf8 = " + ibcgUdf8 + "\n" +
            "  ibcgUdf9 = " + ibcgUdf9 + "\n" +
            "  ibcgUdf10 = " + ibcgUdf10 + "\n" ;
        return s;
    }
}
