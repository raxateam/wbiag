package com.wbiag.app.ta.ruleTrace.model;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import com.wbiag.util.*;
import java.util.*;

/**
 *  @deprecated Core as of 5.0.3.0
 */
public class RuleTraceContent {


    private NameValueList employeeData;
    private List workDetails;
    private List workPremiums;
    private NameValueList workSummaryData;
    private NameValueList empBals;
    private boolean isChangedFromPrevious = false;

    public NameValueList getEmployeeData() {
        return employeeData;
    }

    public void setEmployeeData(NameValueList v) {
        employeeData = v;
    }

    public NameValueList getWorkSummaryData() {
        return workSummaryData;
    }

    public void setWorkSummaryData(NameValueList v) {
        workSummaryData = v;
    }

    public List getWorkDetails() {
        return workDetails;
    }

    public void setWorkDetails(List v) {
        workDetails = v;
    }

    public List getWorkPremiums() {
        return workPremiums;
    }

    public void setWorkPremiums(List v) {
        workPremiums = v;
    }

    public NameValueList getEmployeeBalances() {
        return empBals;
    }

    public void setEmployeeBalances(NameValueList v) {
        empBals = v;
    }

    public boolean isChangedFromPrevious() {
        return isChangedFromPrevious;
    }

    public void setChangedFromPrevious(boolean v) {
        isChangedFromPrevious = v;
    }

    public String toString() {
        return
            "employeeData=" + employeeData + "\n" +
            "workSummaryData=" + workSummaryData + "\n" +
            "workDetails=" + workDetails + "\n" +
            "workPremiums=" + workPremiums + "\n" +
            "empBals=" + empBals + "\n" +
            "isChangedFromPrevious=" + isChangedFromPrevious + "\n"
            ;
    }

    public String toDescription() {
        return
            employeeData + "\n" +
            workSummaryData +  "\n" +
            workDetails + "\n" +
            workPremiums + "\n"
            ;
    }

    public String toXMLData(RuleTraceConfig config){
        StringBuffer sb = new StringBuffer(200);
        sb.append("   <data>").append("\n") ;
        sb.append(toXML(employeeData , config.getEmployeeFields(),"employee")) ;
        sb.append(toXML(workSummaryData , config.getWorkSummaryFields(),"workSummary")) ;
        sb.append(toXML(workDetails , config.getWorkDetailFields(), true)) ;
        sb.append(toXML(workPremiums , config.getWorkPremiumFields(), false)) ;
        sb.append(toXML(empBals , config.getEmployeeBalances(), "employeeBalances" , "balance")) ;
        sb.append("   </data>").append("\n") ;
        return sb.toString();
    }

    private String toXML(List list, List fields, boolean isDetail) {
        if (fields == null || fields.size() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append(isDetail ? "    <workDetails>" : "    <workPremiums>").append("\n") ;
        for (int i = 0; i < list.size(); i++) {
            NameValueList wd = (NameValueList)list.get(i);
            sb.append(toXML(wd, fields, isDetail ? "workDetail" : "workPremium"));
        }

        sb.append(isDetail ? "    </workDetails>" : "    </workPremiums>").append("\n");
        return sb.toString();
    }

    private String toXML(NameValueList data, List fields, String tag) {
        if (fields == null || fields.size() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("    <").append(tag).append(" ") ;
        Iterator iter = fields.iterator();
        while (iter.hasNext()) {
            String fld = (String)iter.next();

            NameValue obj = data.getByName(fld);
            if (obj == null) {
                throw new RuntimeException ("Error in getting value for field :" + fld + " in RuleTrace");
            }
            String val = obj.getValue();
            sb.append(fld).append("='").append(val).append("' ");
        }
        sb.append("/>").append("\n");

        return sb.toString();
    }

    private String toXML(NameValueList data, List fields, String tag, String oneTag) {
        if (fields == null || fields.size() == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("    <").append(tag).append(">").append("\n");
        Iterator iter = fields.iterator();
        while (iter.hasNext()) {
            String fld = (String)iter.next();

            NameValue obj = data.getByName(fld);
            if (obj == null) {
                throw new RuntimeException ("Error in getting value for field :" + fld + " in RuleTrace");
            }
            String val = obj.getValue();
            sb.append("     <").append(oneTag).append(" name='").append(fld).append("' ")
                .append(" value='").append(val).append("'/>").append("\n");
        }
        sb.append("    </").append(tag).append(">").append("\n");

        return sb.toString();
    }


    public boolean equalsForContent(RuleTraceContent rtc){
        boolean ret = true;
        if (rtc == null) {
            return false;
        }
        ret &= employeeData != null && employeeData.equals(rtc.getEmployeeData());
        if (ret) {
            ret &= workSummaryData != null &&  workSummaryData.equals(rtc.getWorkSummaryData());
        }
        if (ret) {
            ret &= equalsForList(workDetails, rtc.getWorkDetails());
        }
        if (ret) {
            ret &= equalsForList(workPremiums, rtc.getWorkPremiums());
        }
        if (ret) {
            ret &= empBals != null && empBals.equals(rtc.getEmployeeBalances());
        }
        return ret;
    }

    private boolean equalsForList(List compTo, List compFrom) {
        boolean ret = true;
        if (compTo.size() == compFrom.size()) {
            for (int i=0; i < compTo.size() ; i++) {
                NameValueList compToNV = (NameValueList)compTo.get(i);
                NameValueList compFromNV = (NameValueList)compFrom.get(i);
                ret &= compToNV.equals(compFromNV);
                if (!ret) {
                    break;
                }
            }
        }
        else {
            ret = false;
        }
        return ret;
    }

    private boolean equalsNameValue(NameValueList nvl, List fields) {
        boolean ret = false;
        return ret;
    }

}






