package com.wbiag.app.ta.model;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;

/**
 *  Title: WbiagCalcgrpParamOvrData
 *  Description: Data object which holds one row from WbiagCalcgrpParamOvr table
 *  Copyright: Copyright (c) 2005, 2006, 200nc.
 *
 * @author gtam@workbrain.com
*/
public class WbiagCalcgrpParamOvrData extends RecordData {
    private int wcpoId;
    private int calcgrpId;
    private Date wcpoStartDate;
    private Date wcpoEndDate;
    private String wcpoAllowPartialBreaks;
    private String wcpoNoSwipeForBreaks;
    private String wcpoFlag1;
    private String wcpoFlag2;
    private String wcpoFlag3;
    private String wcpoFlag4;
    private String wcpoFlag5;
    private String wcpoUdf1;
    private String wcpoUdf2;
    private String wcpoUdf3;
    private String wcpoUdf4;
    private String wcpoUdf5;
    
    public RecordData newInstance() {
        return new WbiagCalcgrpParamOvrData();
    }
    public int getWcpoId(){
        return wcpoId;
    }

    public void setWcpoId(int v){
        wcpoId=v;
    }

    public int getCalcgrpId(){
        return calcgrpId;
    }

    public void setCalcgrpId(int v){
        calcgrpId=v;
    }

    public Date getWcpoStartDate(){
        return wcpoStartDate;
    }

    public void setWcpoStartDate(Date v){
        wcpoStartDate=v;
    }

    public Date getWcpoEndDate(){
        return wcpoEndDate;
    }

    public void setWcpoEndDate(Date v){
        wcpoEndDate=v;
    }
    
    public String getWcpoAllowPartialBreaks(){
        return wcpoAllowPartialBreaks;
    }
    
    public void setWcpoAllowPartialBreaks(String v){
        wcpoAllowPartialBreaks = v;
    }
    
    public String getWcpoNoSwipeForBreaks(){
        return wcpoNoSwipeForBreaks;
    }
    
    public void setWcpoNoSwipeForBreaks(String v){
        wcpoNoSwipeForBreaks = v;
    }
    
    public String getWcpoFlag1(){
        return wcpoFlag1;
    }

    public void setWcpoFlag1(String v){
        wcpoFlag1=v;
    }

    public String getWcpoFlag2(){
        return wcpoFlag2;
    }

    public void setWcpoFlag2(String v){
        wcpoFlag2=v;
    }

    public String getWcpoFlag3(){
        return wcpoFlag3;
    }

    public void setWcpoFlag3(String v){
        wcpoFlag3=v;
    }

    public String getWcpoFlag4(){
        return wcpoFlag4;
    }

    public void setWcpoFlag4(String v){
        wcpoFlag4=v;
    }

    public String getWcpoFlag5(){
        return wcpoFlag5;
    }

    public void setWcpoFlag5(String v){
        wcpoFlag5=v;
    }

    public String getWcpoUdf1(){
        return wcpoUdf1;
    }

    public void setWcpoUdf1(String v){
        wcpoUdf1=v;
    }

    public String getWcpoUdf2(){
        return wcpoUdf2;
    }

    public void setWcpoUdf2(String v){
        wcpoUdf2=v;
    }

    public String getWcpoUdf3(){
        return wcpoUdf3;
    }

    public void setWcpoUdf3(String v){
        wcpoUdf3=v;
    }

    public String getWcpoUdf4(){
        return wcpoUdf4;
    }

    public void setWcpoUdf4(String v){
        wcpoUdf4=v;
    }

    public String getWcpoUdf5(){
        return wcpoUdf5;
    }

    public void setWcpoUdf5(String v){
        wcpoUdf5=v;
    }

    public String toString() {
        String s = "CaclgrpParamOvrData:\n" +
            "  wcpoId = " + wcpoId + "\n" +
            "  calcgrpId = " + calcgrpId + "\n" +
            "  wcpoStartDate = " + wcpoStartDate + "\n" +
            "  wcpoEndDate = " + wcpoEndDate + "\n" +
            "  wcpoAllowPartialBreaks = " + wcpoAllowPartialBreaks + "\n" +
            "  wcpoNoSwipeForBreaks = " + wcpoNoSwipeForBreaks + "\n" +
            "  wcpoFlag1 = " + wcpoFlag1 + "\n" +
            "  wcpoFlag2 = " + wcpoFlag2 + "\n" +
            "  wcpoFlag3 = " + wcpoFlag3 + "\n" +
            "  wcpoFlag4 = " + wcpoFlag4 + "\n" +
            "  wcpoFlag5 = " + wcpoFlag5 + "\n" +
            "  wcpoUdf1 = " + wcpoUdf1 + "\n" +
            "  wcpoUdf2 = " + wcpoUdf2 + "\n" +
            "  wcpoUdf3 = " + wcpoUdf3 + "\n" +
            "  wcpoUdf4 = " + wcpoUdf4 + "\n" +
            "  wcpoUdf5 = " + wcpoUdf5 + "\n" ;
        return s;
    }
}
