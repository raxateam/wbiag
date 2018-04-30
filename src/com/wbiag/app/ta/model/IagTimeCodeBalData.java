package com.wbiag.app.ta.model;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;

public class IagTimeCodeBalData extends RecordData {
    private int itcbId;
    private int balId;
    private int tcodeId;
    private int calcgrpId;
    private int tcbtId;
    private String itcbFlag1;
    private String itcbFlag2;
    private String itcbFlag3;
    private String itcbFlag4;
    private String itcbFlag5;
    private String itcbUdf1;
    private String itcbUdf2;
    private String itcbUdf3;
    private String itcbUdf4;
    private String itcbUdf5;

    public RecordData newInstance() {
        return new IagTimeCodeBalData ();
    }
    public int getItcbId(){
        return itcbId;
    }
    public void setItcbId(int v){
        itcbId=v;
    }

    public int getBalId(){
        return balId;
    }
    public void setBalId(int v){
        balId=v;
    }

    public int getTcodeId(){
        return tcodeId;
    }
    public void setTcodeId(int v){
        tcodeId=v;
    }

    public int getCalcgrpId(){
        return calcgrpId;
    }
    public void setCalcgrpId(int v){
        calcgrpId=v;
    }

    public int getTcbtId(){
        return tcbtId;
    }
    public void setTcbtId(int v){
        tcbtId=v;
    }

    public String getItcbFlag1(){
        return itcbFlag1;
    }
    public void setItcbFlag1(String v){
        itcbFlag1=v;
    }

    public String getItcbFlag2(){
        return itcbFlag2;
    }
    public void setItcbFlag2(String v){
        itcbFlag2=v;
    }

    public String getItcbFlag3(){
        return itcbFlag3;
    }
    public void setItcbFlag3(String v){
        itcbFlag3=v;
    }

    public String getItcbFlag4(){
        return itcbFlag4;
    }
    public void setItcbFlag4(String v){
        itcbFlag4=v;
    }

    public String getItcbFlag5(){
        return itcbFlag5;
    }
    public void setItcbFlag5(String v){
        itcbFlag5=v;
    }

    public String getItcbUdf1(){
        return itcbUdf1;
    }
    public void setItcbUdf1(String v){
        itcbUdf1=v;
    }

    public String getItcbUdf2(){
        return itcbUdf2;
    }
    public void setItcbUdf2(String v){
        itcbUdf2=v;
    }

    public String getItcbUdf3(){
        return itcbUdf3;
    }
    public void setItcbUdf3(String v){
        itcbUdf3=v;
    }

    public String getItcbUdf4(){
        return itcbUdf4;
    }
    public void setItcbUdf4(String v){
        itcbUdf4=v;
    }

    public String getItcbUdf5(){
        return itcbUdf5;
    }
    public void setItcbUdf5(String v){
        itcbUdf5=v;
    }


    public String toString() {
        String s = "IagTimeCodeBalData:\n" +
            "  itcbId = " + itcbId + "\n" +
            "  balId = " + balId + "\n" +
            "  tcodeId = " + tcodeId + "\n" +
            "  calcgrpId = " + calcgrpId + "\n" +
            "  tcbtId = " + tcbtId + "\n" +
            "  itcbFlag1 = " + itcbFlag1 + "\n" +
            "  itcbFlag2 = " + itcbFlag2 + "\n" +
            "  itcbFlag3 = " + itcbFlag3 + "\n" +
            "  itcbFlag4 = " + itcbFlag4 + "\n" +
            "  itcbFlag5 = " + itcbFlag5 + "\n" +
            "  itcbUdf1 = " + itcbUdf1 + "\n" +
            "  itcbUdf2 = " + itcbUdf2 + "\n" +
            "  itcbUdf3 = " + itcbUdf3 + "\n" +
            "  itcbUdf4 = " + itcbUdf4 + "\n" +
            "  itcbUdf5 = " + itcbUdf5 + "\n" ;
        return s;
    }
}
