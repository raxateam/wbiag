package com.wbiag.app.ta.quickrules;

//standard imports for rules
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.sql.DBConnection;
import java.util.List;

//extra imports needed for this rule
import com.workbrain.app.ta.quickrules.WeeklyOvertimeRule;
import com.workbrain.util.DateHelper;
import java.util.Date;

import com.workbrain.app.ta.model.*;
//import com.workbrain.app.ta.ruleengine.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Enumeration;

/**
*  Title: WeeklyOvertimeAllocationRule
*  Description: Allocates weekly OT hours by labor metrics distribution
*  Copyright: Copyright (c) 2005, 2006, 2007
*  Company: Workbrain Inc.
*
* @author gtam@workbrain.com
*/

public class WeeklyOvertimeAllocationRule extends WeeklyOvertimeRule {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WeeklyOvertimeAllocationRule.class);
    
    
    // OT Allocation Specific
    public final static String PARAM_ALLOCATE_TIME_CODE = "Allocate by Time Code";    
    public final static String PARAM_ALLOCATE_HOUR_TYPE = "Allocate by Hour Type";
    public final static String PARAM_ALLOCATE_PROJECT = "Allocate by Project";
    public final static String PARAM_ALLOCATE_JOB = "Allocate by Job";
    public final static String PARAM_ALLOCATE_DOCKET = "Allocate by Docket";
    public final static String PARAM_ALLOCATE_DEPT = "Allocate by Dept";
    public final static String PARAM_PREMIUM_HOUR_TYPE_INSERTED = "Premium Hour Type Inserted";

    /** @return the parameters of this custom rule */
    public List getParameterInfo(DBConnection parm1) {
        List result = new ArrayList();
    
        // Weekly OT Parameters
        result.add(new RuleParameterInfo(PARAM_HOURSET_DESCRIPTION, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_HOURTYPES, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODES_COUNTED, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_DISCOUNT_TIMECODES, RuleParameterInfo.STRING_TYPE, true));
        
        RuleParameterInfo rpiDayWeekStartsChoice = new RuleParameterInfo(PARAM_DAY_WEEK_STARTS, RuleParameterInfo.CHOICE_TYPE, false);
        rpiDayWeekStartsChoice.addChoice("Sunday");
        rpiDayWeekStartsChoice.addChoice("Monday");
        rpiDayWeekStartsChoice.addChoice("Tuesday");
        rpiDayWeekStartsChoice.addChoice("Wednesday");
        rpiDayWeekStartsChoice.addChoice("Thursday");
        rpiDayWeekStartsChoice.addChoice("Friday");
        rpiDayWeekStartsChoice.addChoice("Saturday");
        result.add(rpiDayWeekStartsChoice);
        
        RuleParameterInfo rpiApplyBasedOnScheduleChoice = new RuleParameterInfo(PARAM_APPLY_BASED_ON_SCHEDULE, RuleParameterInfo.CHOICE_TYPE, true);
        rpiApplyBasedOnScheduleChoice.addChoice("true");
        rpiApplyBasedOnScheduleChoice.addChoice("false");
        result.add(rpiApplyBasedOnScheduleChoice);
        
        result.add(new RuleParameterInfo(PARAM_PREMIUM_TIMECODE_INSERTED, RuleParameterInfo.STRING_TYPE, true));    
        result.add(new RuleParameterInfo(PARAM_PREMIUM_HOUR_TYPE_INSERTED, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_HOURTYPE_FOR_OVERTIME_WORKDETAILS, RuleParameterInfo.STRING_TYPE, true));
    
        // OT Allocation Specific Parameters
        RuleParameterInfo rpiAllocateTimeCode = new RuleParameterInfo(PARAM_ALLOCATE_TIME_CODE, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAllocateTimeCode.addChoice("true");
        rpiAllocateTimeCode.addChoice("false");
        result.add(rpiAllocateTimeCode);
        
        RuleParameterInfo rpiAllocateHourType = new RuleParameterInfo(PARAM_ALLOCATE_HOUR_TYPE, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAllocateHourType.addChoice("true");
        rpiAllocateHourType.addChoice("false");
        result.add(rpiAllocateHourType);
    
        RuleParameterInfo rpiAllocateProject = new RuleParameterInfo(PARAM_ALLOCATE_PROJECT, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAllocateProject.addChoice("true");
        rpiAllocateProject.addChoice("false");
        result.add(rpiAllocateProject);
        
        RuleParameterInfo rpiAllocateJob = new RuleParameterInfo(PARAM_ALLOCATE_JOB, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAllocateJob.addChoice("true");
        rpiAllocateJob.addChoice("false");
        result.add(rpiAllocateJob);
        
        RuleParameterInfo rpiAllocateDocket = new RuleParameterInfo(PARAM_ALLOCATE_DOCKET, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAllocateDocket.addChoice("true");
        rpiAllocateDocket.addChoice("false");
        result.add(rpiAllocateDocket);
        
        RuleParameterInfo rpiAllocateDept = new RuleParameterInfo(PARAM_ALLOCATE_DEPT, RuleParameterInfo.CHOICE_TYPE, true);
        rpiAllocateDept.addChoice("true");
        rpiAllocateDept.addChoice("false");
        result.add(rpiAllocateDept);
            
        return result;
    }

    /**
    * Executes the weekly OT rule, then runs the OT allocation logic
    * @throws Exception
    */
    public void execute(WBData wbData, Parameters parameters) throws java.lang.Exception {
        if(logger.isDebugEnabled()) { logger.debug("Executing Weekly OT Rule for empId: " + wbData.getEmpId()); }
        // Run the Weekly OT Rule   
        parameters.addParameter(PARAM_WORKDETAIL_TIMECODES, null);
        parameters.addParameter(PARAM_ASSIGN_BETTERRATE, "True");
        super.execute(wbData, parameters);
        
        // Finished Core Weekly OT Rule Logic: wbData object should be updated with OT premiums (if any)
        // Now run the OT Allocation Logic
        String premiumTimeCodeInserted = parameters.getParameter(super.PARAM_PREMIUM_TIMECODE_INSERTED);
        String premiumHourTypeInserted = parameters.getParameter(PARAM_PREMIUM_HOUR_TYPE_INSERTED);
        String eligibleHourTypes = parameters.getParameter(super.PARAM_ELIGIBLE_HOURTYPES);
        boolean allocateTimeCode = parameters.getParameter(PARAM_ALLOCATE_TIME_CODE).equals("true");
        boolean allocateHourType = parameters.getParameter(PARAM_ALLOCATE_HOUR_TYPE).equals("true");
        boolean allocateProject = parameters.getParameter(PARAM_ALLOCATE_PROJECT).equals("true");
        boolean allocateJob = parameters.getParameter(PARAM_ALLOCATE_JOB).equals("true");
        boolean allocateDocket = parameters.getParameter(PARAM_ALLOCATE_DOCKET).equals("true");
        boolean allocateDept = parameters.getParameter(PARAM_ALLOCATE_DEPT).equals("true");
        ruleOvertimeAllocation(wbData, premiumTimeCodeInserted, premiumHourTypeInserted, eligibleHourTypes,
                allocateTimeCode, allocateHourType, allocateProject, allocateJob,
                allocateDocket, allocateDept);
        if(logger.isDebugEnabled()) { logger.debug("Done Executing OT Allocation Rule for empId: " + wbData.getEmpId()); }
    }

    /**
    * The WBIAG OT Allocation logic
    * @param WBData wbData
    * @param String premiumTimeCodeInserted
    * @param String premiumHourTypeInserted
    * @param String eligibleHourTypes
    * @param boolean allocateTimeCode
    * @param boolean allocateHourType
    * @param boolean allocateProject
    * @param boolean allocateJob
    * @param boolean allocateDocket
    * @param boolean allocateDept
    * @throws Exception
    */
    protected void ruleOvertimeAllocation(WBData wbData, String premiumTimeCodeInserted, String premiumHourTypeInserted,
            String eligibleHourTypes, boolean allocateTimeCode, boolean allocateHourType, boolean allocateProject,
            boolean allocateJob, boolean allocateDocket, boolean allocateDept) throws Exception {
        if(logger.isDebugEnabled()) { logger.debug("Allocating Overtime Hours for empId: " + wbData.getEmpId()); }
           
        int totalMinutes = 0;
        int totalOTMinutes = 0;
        int todaysOTMinutes = 0;
        Hashtable MetricsMinutes = new Hashtable();
        WorkDetailData workPremiumTemplate = null;
        
        Date dateWeekStarts = DateHelper.getUnitWeek(DateHelper.APPLY_ON_FIRST_DAY, false, wbData.getWrksWorkDate());
        Date dateWeekEnds = DateHelper.addDays(dateWeekStarts, 6);
           
        WorkDetailList wdl = wbData.getWorkDetails(dateWeekStarts, dateWeekEnds, null, 0);
        for (int i = 0; i < wdl.size(); i++) {
            WorkDetailData wd = (WorkDetailData)wdl.get(i);
            
            // Eligible Hour Type
            if (RuleHelper.isCodeInList(eligibleHourTypes, wd.getWrkdHtypeName()) && wd.isTypeDetail()){
                //Build the key for the hash table
                StringBuffer keyBuffer = new StringBuffer();
                if (allocateTimeCode) {
                    keyBuffer.append(wd.getTcodeId()).append(',');               
                }
                if (allocateHourType) {
                    keyBuffer.append(wd.getHtypeId()).append(',');               
                }
                if (allocateProject) {
                    keyBuffer.append(wd.getProjId()).append(',');               
                }
                if (allocateJob) {
                    keyBuffer.append(wd.getJobId()).append(',');               
                }
                if (allocateDocket){
                    keyBuffer.append(wd.getDockId()).append(',');               
                }
                if (allocateDept){
                    keyBuffer.append(wd.getDeptId());
                }
                String key = keyBuffer.toString();
                   
                if (MetricsMinutes.get(key) == null) {
                    MetricsMinutes.put(key, new Integer(wd.getWrkdMinutes()));
                    totalMinutes += wd.getWrkdMinutes();
                       
                } else {
                    int workedMinutes = ((Integer)MetricsMinutes.get(key)).intValue();
                    MetricsMinutes.put(key, new Integer(wd.getWrkdMinutes() + workedMinutes));
                    totalMinutes += wd.getWrkdMinutes();                
                }
               
            // OT Premium
            } else if (RuleHelper.isCodeInList(premiumTimeCodeInserted, 
                    wd.getWrkdTcodeName()) && wd.isTypePremium()){
                totalOTMinutes += wd.getWrkdMinutes();
                if (wd.getWrkdWorkDate() == wbData.getWrksWorkDate()){
                    workPremiumTemplate = wd;  // Save a work detail with the correct Docket, Job, Project, etc.
                    todaysOTMinutes += wd.getWrkdMinutes();
                }
            }
        }
           
        // Remove all OT Premiums inserted by weekly OT rule
        wbData.removeWorkPremiums(premiumTimeCodeInserted, true, null, true, null);
        
        int duration = 0;
    
        // Only allocate hours if the current day had existing OT premiums from the weekly OT calculation
        if (todaysOTMinutes > 0) {
            // Calculate the ratio of daily OT minutes / total OT minutes
            double dailyPercentage = (double)todaysOTMinutes / (double)totalOTMinutes;
               
            // Allocate Hours
            Enumeration keys = MetricsMinutes.keys();
            while (keys.hasMoreElements()){
                String key = (String)keys.nextElement();
                Integer metricMinutes = (Integer)MetricsMinutes.get(key);
                double timePercentage = (double)metricMinutes.intValue() / (double)totalMinutes;
                duration = (int)Math.round(dailyPercentage * timePercentage * (double)totalOTMinutes);
                //Insert Work Premium with selected Metrics and duration
                WorkDetailData wrkDetail = workPremiumTemplate;
                if (allocateTimeCode) {
                    wrkDetail.setTcodeId(Integer.parseInt(key.substring(0,key.indexOf(','))));
                    key = key.substring(key.indexOf(',') + 1);
                }
                if (allocateHourType) {
                    wrkDetail.setHtypeId(Integer.parseInt(key.substring(0,key.indexOf(','))));
                    key = key.substring(key.indexOf(',') + 1);
                }
                if (allocateProject) {
                    wrkDetail.setProjId(Integer.parseInt(key.substring(0,key.indexOf(','))));
                    key = key.substring(key.indexOf(',') + 1);
                }
                if (allocateJob) {
                    wrkDetail.setJobId(Integer.parseInt(key.substring(0,key.indexOf(','))));
                    key = key.substring(key.indexOf(',') + 1);
                }
                if (allocateDocket){
                    wrkDetail.setDockId(Integer.parseInt(key.substring(0,key.indexOf(','))));
                    key = key.substring(key.indexOf(',') + 1);
                }
                if (allocateDept){
                    wrkDetail.setDeptId(Integer.parseInt(key));
                }
                wbData.insertWorkPremiumRecord(duration, premiumTimeCodeInserted, 
                        premiumHourTypeInserted, wrkDetail);             
                if(logger.isDebugEnabled()) { logger.debug("Inserting Overtime premium with duration: " + duration); }
            }       
        }
    }

    /** @return the name of this custom rule */
    public String getComponentName() {
        return "WBIAG Weekly Overtime Allocation Rule";
    }
    
    /** @return information indicating how to use this rule */
    public String getDescription() {
        return "Prorates overtime hours to different job metrics that an employee works in during the week.";
    }

}
