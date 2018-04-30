package com.wbiag.app.ta.quickrules;

import com.workbrain.app.ta.quickrules.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.*;
import java.sql.SQLException;
import java.util.*;
/**
 */
public class PremiumWithLabor {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PremiumWithLabor.class);

    public final static String PARAM_PLM_VAL_DEFAULT_METHOD = "Default";
    public final static String PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD = "Hour-for-Hour/Proportional";
    public final static String PARAM_PLM_VAL_LAST_METHOD = "Last";
    public final static String PARAM_PLM_VAL_FIRST_METHOD = "First";
    public final static String PARAM_PLM_VAL_PRORATED_METHOD = "Prorated";
    public final static String PARAM_PLM_VAL_FLAT_RATE_THRESHOLD_METHOD =  "Flat_Rate_Threshold";
    protected List tabWrkdDates = new ArrayList();

    protected void insertWorkPremiumRecords(String  methodLaborMetric,
                                           WBData wbData,
                                           int tcodeId, int htypeId,
                                           int minutes, double rate,
                                           boolean useMinutesSuppliedForDefault,
                                           boolean isDefaultIndependent){
       insertWorkPremiumRecords(methodLaborMetric, null,
                                wbData, tcodeId, htypeId,
                                minutes, rate, useMinutesSuppliedForDefault,
                                isDefaultIndependent);
    }

    protected void insertWorkPremiumRecords(String  methodLaborMetric,
                                           String  methodLaborValue,
                                           WBData wbData,
                                           int tcodeId, int htypeId,
                                           int minutes, double rate,
                                           boolean useMinutesSuppliedForDefault,
                                           boolean isDefaultIndependent){
       int size = tabWrkdDates.size();
       if (size == 0) {
           return;
       }
       int triggeredIndex = -1;
       if (PARAM_PLM_VAL_FIRST_METHOD.equals(methodLaborMetric)) {
           if (isDefaultIndependent) {
               Iterator iter = tabWrkdDates.iterator();
               while (iter.hasNext()) {
                   WrkdDates item = (WrkdDates)iter.next();
                   insertOneWorkPremiumRecord(wbData, (WrkdDates) tabWrkdDates.get(0),
                                      tcodeId, htypeId, item.minutes, rate);
               }
           }
           else {
               insertOneWorkPremiumRecord(wbData, (WrkdDates) tabWrkdDates.get(0),
                                      tcodeId, htypeId, minutes, rate);
           }


       }
       else if (PARAM_PLM_VAL_LAST_METHOD.equals(methodLaborMetric)) {
           if (isDefaultIndependent) {
               Iterator iter = tabWrkdDates.iterator();
               while (iter.hasNext()) {
                   WrkdDates item = (WrkdDates)iter.next();
                   insertOneWorkPremiumRecord(wbData, (WrkdDates) tabWrkdDates.get(size - 1),
                                      tcodeId, htypeId, item.minutes, rate);
               }
           }
           else {
               insertOneWorkPremiumRecord(wbData, (WrkdDates) tabWrkdDates.get(size - 1),
                                          tcodeId, htypeId, minutes, rate);
           }
       }
       else if (PARAM_PLM_VAL_HOUR_FOR_HOUR_METHOD.equals(methodLaborMetric)) {

           Iterator iter = tabWrkdDates.iterator();
           while (iter.hasNext()) {
               WrkdDates item = (WrkdDates)iter.next();
               insertOneWorkPremiumRecord(wbData, item, tcodeId, htypeId,
                                          item.minutes, rate);
           }
       }
       else if (PARAM_PLM_VAL_PRORATED_METHOD.equals(methodLaborMetric)) {
           int durTotal = getWrkdDatesMinutes();
           int totMins = 0;
           for (int i=0, k=tabWrkdDates.size() ; i < k ; i++) {
               WrkdDates item = (WrkdDates)tabWrkdDates.get(i);
               // watch out for rounding problems
               if (i == k - 1) {
                   item.minutes = durTotal - totMins;
               }
               else {
                   double d1 = (double) item.minutes /
                       durTotal;
                   double mins = d1 * durTotal;
                   item.minutes = (int) mins;
                   totMins += item.minutes;
               }
               insertOneWorkPremiumRecord(wbData, item, tcodeId, htypeId,
                                          item.minutes, rate);
           }

       }
       else if (PARAM_PLM_VAL_DEFAULT_METHOD.equals(methodLaborMetric)) {

           if (isDefaultIndependent) {
               Iterator iter = tabWrkdDates.iterator();
               while (iter.hasNext()) {
                   WrkdDates item = (WrkdDates)iter.next();
                   wbData.insertWorkPremiumRecord(item.minutes , tcodeId,
                                                  htypeId,
                                                  rate);
               }
           }
           else {
               int mins = useMinutesSuppliedForDefault ? minutes : getWrkdDatesMinutes();
               wbData.insertWorkPremiumRecord(mins, tcodeId,
                                              htypeId,
                                              rate);
           }
       }
       else if (PARAM_PLM_VAL_FLAT_RATE_THRESHOLD_METHOD.equals(methodLaborMetric)) {
           int mins = 0;
           if (StringHelper.isEmpty(methodLaborValue)) {
               throw new RuntimeException ("Labormetric value must be supplied if method is :" + PARAM_PLM_VAL_FLAT_RATE_THRESHOLD_METHOD);
           }
           ArrayList thresholds = new ArrayList(StringHelper
               .detokenizeStringAsList(methodLaborValue , ",")) ;
           Iterator iter = tabWrkdDates.iterator();
           while (iter.hasNext()) {
               WrkdDates item = (WrkdDates)iter.next();
               mins += item.minutes;
               Iterator iterT = thresholds.iterator();
               while (iterT.hasNext()) {
                   int threshold =  Integer.parseInt( (String) iterT.next());
                   if (mins >= threshold) {
                       insertOneWorkPremiumRecord(wbData, item, tcodeId,
                           htypeId,
                           60, rate);
                       iterT.remove();
                   }
               }
           }
       }
       else {
           throw new RuntimeException ("Premium Labor method not supported:" +  methodLaborMetric);
       }
    }

    protected  void insertOneWorkPremiumRecord(WBData wbData,
                                               WrkdDates wrkDates,
                                               int tcodeId, int htypeId, int minutes, double rate){
        if(minutes <= 0){
            return;
        }
        WorkDetailData wd1 = new WorkDetailData();
        wd1.setCodeMapper(wbData.getCodeMapper());
        wd1.setWrksId(wbData.getRuleData().getWorkSummary().getWrksId());
        wd1.setEmpId(wbData.getEmpId());
        wd1.setWrkdWorkDate(wbData.getWrksWorkDate());
        WorkDetailData wd = wrkDates.wd;
        if (wd == null) {
            wd = wbData.getRuleData().getWorkPremiums().add(DateHelper.DATE_1900 ,
                DateHelper.DATE_1900, wbData.getRuleData().getEmpDefaultLabor(0));
        }
        wd1.setJobId(wd.getJobId());
        wd1.setProjId(wd.getProjId());
        wd1.setDeptId(wd.getDeptId());
        wd1.setDockId(wd.getDockId());
        wd1.setTcodeId(tcodeId);
        wd1.setHtypeId(htypeId);
        wd1.setWrkdMinutes(minutes);
        if (rate == 0) {
            HourTypeData htd = wbData.getCodeMapper().getHourTypeById(htypeId);
            wd.setWrkdRate(htd.getHtypeMultiple() * wbData.getEmpBaseRate());
        }
        else {
            wd1.setWrkdRate(rate);
        }
        wbData.insertWorkPremiumRecord(wd1);
   }

    protected void addWrkdDates(Date startDate, Date endDate, int minutes,
                                WorkDetailData wd) {
        WrkdDates wDates = new WrkdDates();
        wDates.startDate = startDate;
        wDates.endDate = endDate;
        wDates.minutes = minutes ;
        wDates.wd = wd;
        tabWrkdDates.add(wDates);
    }

    protected int getWrkdDatesMinutes() {
        int ret = 0;
        Iterator iter = tabWrkdDates.iterator();
        while (iter.hasNext()) {
            WrkdDates item = (WrkdDates)iter.next();
            ret += item.minutes;
        }

        return ret;
    }

    protected void clearWrkdDatesMinutes() {
        tabWrkdDates.clear();
    }

    class WrkdDates {
        public Date startDate;
        public Date endDate;
        public int minutes;
        public WorkDetailData wd;
        public boolean independent = false;


        public String toString() {
            return "Start Date : " + startDate+ "/n"
                + "End Date : " + endDate + "/n"
                + "Minutes : " + minutes;

        }
    }


}