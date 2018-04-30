package com.wbiag.server.wbiagprocess;

import java.util.*;
import com.workbrain.util.*;
import com.workbrain.app.ta.model.*;

public class WbiagSolutionData extends RecordData {

    public static final String WIS_STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String WIS_STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String WIS_STATUS_RELEASED_TO_TESTING = "RELEASED_TO_TESTING";
    public static final String WIS_STATUS_COMPLETE = "COMPLETE";
    public static final String WIS_STATUS_FORCE_CLOSED = "FORCE_CLOSED";
    public static final String WIS_TYPE_BUILD = "BUILD";
    public static final String WIS_TYPE_REVIEW = "REVIEW";
    public static final String WIS_TYPE_LIBRARY = "LIBRARY";

    private int wisId;
    private java.util.Date wisCreateDate;
    private int wisTtNumber;
    private String wisTtSummary;
    private String wisSolutionType;
    private int wisaId;
    private String wisaLevel;
    private int wicId;
    private String wisStatus;
    private String wisFuncSpec;
    private String wisTechSpec;
    private java.util.Date wisEstDlvrDate;
    private int wisEstManDays;
    private String wisCodeReviewBy;
    private java.util.Date wisReleaseDate;
    private int wisFinalManDays;
    private String wisAssgnTo;
    private String wisComments;

    public RecordData newInstance() {
        return new WbiagSolutionData ();
    }
    public int getWisId(){
        return wisId;
    }

    public void setWisId(int v){
        wisId=v;
    }

    public java.util.Date getWisCreateDate(){
        return wisCreateDate;
    }

    public void setWisCreateDate(java.util.Date v){
        wisCreateDate=v;
    }

    public int getWisTtNumber(){
        return wisTtNumber;
    }

    public void setWisTtNumber(int v){
        wisTtNumber=v;
    }

    public String getWisTtSummary(){
        return wisTtSummary;
    }

    public void setWisTtSummary(String v){
        wisTtSummary=v;
    }

    public String getWisSolutionType(){
        return wisSolutionType;
    }

    public void setWisSolutionType(String v){
        wisSolutionType=v;
    }

    public int getWisaId(){
        return wisaId;
    }

    public void setWisaId(int v){
        wisaId=v;
    }

    public String getWisaLevel(){
        return wisaLevel;
    }

    public void setWisaLevel(String v){
        wisaLevel=v;
    }

    public int getWicId(){
        return wicId;
    }

    public void setWicId(int v){
        wicId=v;
    }

    public String getWisStatus(){
        return wisStatus;
    }

    public void setWisStatus(String v){
        wisStatus=v;
    }

    public String getWisFuncSpec(){
        return wisFuncSpec;
    }

    public void setWisFuncSpec(String v){
        wisFuncSpec=v;
    }

    public String getWisTechSpec(){
        return wisTechSpec;
    }

    public void setWisTechSpec(String v){
        wisTechSpec=v;
    }

    public java.util.Date getWisEstDlvrDate(){
        return wisEstDlvrDate;
    }

    public void setWisEstDlvrDate(java.util.Date v){
        wisEstDlvrDate=v;
    }

    public int getWisEstManDays(){
        return wisEstManDays;
    }

    public void setWisEstManDays(int v){
        wisEstManDays=v;
    }

    public String getWisCodeReviewBy(){
        return wisCodeReviewBy;
    }

    public void setWisCodeReviewBy(String v){
        wisCodeReviewBy=v;
    }

    public java.util.Date getWisReleaseDate(){
        return wisReleaseDate;
    }

    public void setWisReleaseDate(java.util.Date v){
        wisReleaseDate=v;
    }

    public int getWisFinalManDays(){
        return wisFinalManDays;
    }

    public void setWisFinalManDays(int v){
        wisFinalManDays=v;
    }

    public String getWisAssgnTo(){
        return wisAssgnTo;
    }

    public void setWisAssgnTo(String v){
        wisAssgnTo=v;
    }

    public String getWisComments(){
        return wisComments;
    }

    public void setWisComments(String v){
        wisComments=v;
    }


    public String toString() {
        String s = "WbiagSolutionData:\n" +
            "  wisId = " + wisId + "\n" +
            "  wisCreateDate = " + wisCreateDate + "\n" +
            "  wisTtNumber = " + wisTtNumber + "\n" +
            "  wisTtSummary = " + wisTtSummary + "\n" +
            "  wisSolutionType = " + wisSolutionType + "\n" +
            "  wisaId = " + wisaId + "\n" +
            "  wisaLevel = " + wisaLevel + "\n" +
            "  wicId = " + wicId + "\n" +
            "  wisStatus = " + wisStatus + "\n" +
            "  wisFuncSpec = " + wisFuncSpec + "\n" +
            "  wisTechSpec = " + wisTechSpec + "\n" +
            "  wisEstDlvrDate = " + wisEstDlvrDate + "\n" +
            "  wisEstManDays = " + wisEstManDays + "\n" +
            "  wisCodeReviewBy = " + wisCodeReviewBy + "\n" +
            "  wisReleaseDate = " + wisReleaseDate + "\n" +
            "  wisFinalManDays = " + wisFinalManDays + "\n" +
            "  wisAssgnTo = " + wisAssgnTo + "\n" +
            "  wisComments = " + wisComments + "\n" ;
        return s;
    }
}

