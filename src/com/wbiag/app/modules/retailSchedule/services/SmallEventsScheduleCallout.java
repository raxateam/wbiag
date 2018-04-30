package com.wbiag.app.modules.retailSchedule.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.modules.retailSchedule.model.EmployeeGroup;
import com.workbrain.app.modules.retailSchedule.model.RuleDefinition;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.model.ShiftActivity;
import com.workbrain.app.modules.retailSchedule.model.ShiftDetail;
import com.workbrain.app.modules.retailSchedule.model.ShiftRule;
import com.workbrain.app.modules.retailSchedule.services.FileManager;
import com.workbrain.app.modules.retailSchedule.services.PostProcessor;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.ShiftCover;
import com.workbrain.app.modules.retailSchedule.services.StaffRuleBehaviorDefinition;
import com.workbrain.app.modules.retailSchedule.services.model.CalloutHelper;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.app.modules.retailSchedule.services.rules.RulesFile;
import com.workbrain.app.modules.retailSchedule.utils.SODay;
import com.workbrain.app.modules.retailSchedule.utils.SODaySet;
import com.workbrain.server.registry.Registry;
import com.workbrain.util.DateHelper;


/**
 * This class contains the logic for the small events feature. This feature causes
 * employees who must attend mandatory events below a specified length to still be
 * eligible for other shifts on that day.
 */
public class SmallEventsScheduleCallout {
    
    private static Logger logger = Logger.getLogger(SmallEventsScheduleCallout.class);
    
    private static final Integer RULE_MAX_SHIFTS_PER_DAY = new Integer(1);
    private static final Integer RULE_ENFORCE_MAX_HOURS = new Integer(7);
    private static final Integer RULE_MAX_CONSECUTIVE_DAYS = new Integer(9);
    private static final Integer RULE_MAX_SHIFTS_PER_SCHEDULE = new Integer(33);
    private static final Integer RULE_MAX_HOURS_PER_DAY = new Integer(41);
    private static final Integer RULE_MAX_HOURS_PER_SCHEDULE = new Integer(40);
    
    private static final String SMALL_PFS_RULE_MAX_SHIFTS_PER_DAY = "1500";
    private static final String SMALL_PFS_RULE_ENFORCE_MAX_HOURS = "7003";
    private static final String SMALL_PFS_RULE_MAX_CONSECUTIVE_DAYS = "9002";
    private static final String SMALL_PFS_RULE_MAX_SHIFTS_PER_SCHEDULE = "3300";
    private static final String SMALL_PFS_RULE_MAX_HOURS_PER_DAY = "7004";
    private static final String SMALL_PFS_RULE_MAX_HOURS_PER_SCHEDULE = "7005";
    
    public static final String SMALL_PFS_LIST = "SmallPFSList";
    public static final String SMALL_PFS_FILE = "SmallPFSFile";
    public static final String SMALL_PFS_FILE_IDX = "smallEventsFile_fileIdx";
    
    private static final String SO_CONTEXT_KEY_STATUS = ScheduleCallout.INCLUDE_SHIFT_CHOICE + CalloutHelper.TYPE_BOOLEAN_ARRAY;
    private static final String SO_CONTEXT_KEY_EMPLOYEE = ScheduleCallout.INCLUDE_SHIFT_CHOICE + CalloutHelper.TYPE_EMPLOYEE;
    private static final String SO_CONTEXT_KEY_MIN_SHIFT_LENGTHS = ScheduleCallout.INCLUDE_SHIFT_CHOICE + "SmallEventsScheduleCallout::minShiftLengths";
    private static final String SO_CONTEXT_KEY_EMPGRP_SHIFT_RULES = ScheduleCallout.INCLUDE_SHIFT_CHOICE + "SmallEventsScheduleCallout::empgrpShiftRules";   
    
    private static final int WRITE_STATUS_IDX               = 0;
    private static final int FIXED_SHIFT_STATUS_IDX         = 1;
    private static final int MANDATORY_STATUS_IDX           = 2;
    private static final int PARTIAL_FIXED_SHIFT_STATUS_IDX = 3;

    private static final String ROW_KEY = ScheduleCallout.POPULATE_SCHEDULE + CalloutHelper.TYPE_STRING_ARRAY;
    private static final String SHIFT_DETAIL_KEY =ScheduleCallout.POPULATE_SCHEDULE+CalloutHelper.TYPE_SHIFT_DETAIL;
    private static final String SMALL_EVENT_SHIFTS = "SmallEventShifts";
    
    private static final String SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_SHIFTS_PER_DAY = "system/modules/scheduleOptimization/smallEvents/NOT_CONTRIBUTE_TO_MAX_SHIFTS_PER_DAY";
    private static final String SMALL_EVENTS_NOT_CONTRIBUTE_TO_EMPLOYEE_MAX_HOURS = "system/modules/scheduleOptimization/smallEvents/NOT_CONTRIBUTE_TO_EMPLOYEE_MAX_HOURS";
    private static final String SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_CONSECUTIVE_DAYS = "system/modules/scheduleOptimization/smallEvents/NOT_CONTRIBUTE_TO_MAX_CONSECUTIVE_DAYS";
    private static final String SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_SHIFTS_PER_SCHEDULE = "system/modules/scheduleOptimization/smallEvents/NOT_CONTRIBUTE_TO_MAX_SHIFTS_PER_SCHEDULE";
    private static final String SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_HOURS_PER_DAY = "system/modules/scheduleOptimization/smallEvents/NOT_CONTRIBUTE_TO_MAX_HOURS_PER_DAY";
    private static final String SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_HOURS_PER_SCHEDULE = "system/modules/scheduleOptimization/smallEvents/NOT_CONTRIBUTE_TO_MAX_HOURS_PER_SCHEDULE";

	/**
     * Creates file that will store short PFS. This should be called before the
     * PFS shifts are assessed. <code> buildViewRuleFilePostLoop </code> would be
     * a good choice since the ViewRules temp file is created early.
     *  
     * @param    soContext
     */
    public static void createSmallEventsFile(SOData soContext) throws RetailException {
        FileManager oFileManager = soContext.getLocalFileManager();
        Integer FileIndx = oFileManager.registerFile(SMALL_PFS_FILE);
        soContext.addCustomData(SMALL_PFS_FILE_IDX, FileIndx);
     }
	
    /**
     * Writes the rule ID and parameter information into the alpha rules file for the
     * corresponding small events feature in the mosel file if the max [x] shifts/day
     * rule is turned on. This should be called via the <code> buildViewRuleFilePreAction </code>
     * callout.
     * 
     * @param soContext
     * @return
     * @throws RetailException
     */
    public static boolean writeSmallEventsRuleID(SOData soContext) throws RetailException {
        RulesFile rulesFile = RulesFile.getRulesFile(soContext);
    	StaffRuleBehaviorDefinition oRule=(StaffRuleBehaviorDefinition)soContext.getLocalVariable(ScheduleCallout.BUILD_VIEW_RULE_FILE+CalloutHelper.TYPE_RULE);
        RuleDefinition oRuleDef = oRule.getRuleDefinition();
        Integer rulMapId = oRuleDef.getRulMapId();
        String rulId = null;
        boolean ignoreParm1 = false;
        if (RULE_MAX_SHIFTS_PER_DAY.equals(rulMapId) && isMaxShiftsPerDayRuleOff()){
            rulId = SMALL_PFS_RULE_MAX_SHIFTS_PER_DAY;
        } else if (RULE_ENFORCE_MAX_HOURS.equals(rulMapId) && isMaxHoursRuleOff()){
            rulId = SMALL_PFS_RULE_ENFORCE_MAX_HOURS;
            ignoreParm1 = true;
        } else if (RULE_MAX_CONSECUTIVE_DAYS.equals(rulMapId) && isMaxConsecutiveDaysRuleOff()){
            rulId = SMALL_PFS_RULE_MAX_CONSECUTIVE_DAYS;    
        } else if (RULE_MAX_SHIFTS_PER_SCHEDULE.equals(rulMapId) && isMaxShiftsPerScheduleRuleOff()){
            rulId = SMALL_PFS_RULE_MAX_SHIFTS_PER_SCHEDULE;
        } else if (RULE_MAX_HOURS_PER_DAY.equals(rulMapId) && isMaxHoursPerDayRuleOff()){
            rulId = SMALL_PFS_RULE_MAX_HOURS_PER_DAY;
        } else if (RULE_MAX_HOURS_PER_SCHEDULE.equals(rulMapId) && isMaxHoursPerScheduleRuleOff()){
            rulId = SMALL_PFS_RULE_MAX_HOURS_PER_SCHEDULE;
        }
	    	
	    if (rulId != null) {
	        // Write a substitute rule entry in the alpha rules file
	        Integer ruleGroupId = oRule.getRulegrpId();
            String strParm1 = ignoreParm1 ? "" : oRule.getEmpgrprulRulprmv1();
	    	Integer iMod = oRule.getEmpgrprulModOvr();
	    	if (iMod == null) {
	    		rulesFile.writeRule(rulId,ruleGroupId.toString(),"1",strParm1,"","");
            }
            else {
            	rulesFile.writeRule(rulId,ruleGroupId.toString(),"1",strParm1,"",iMod.toString());
		    }
		    return false;
        }
        return true;
    }
    
    /**
     * Access the registry to check if small events should contribute to max shifts per day 
     * @return if the registry is set to true/Y/yes, return false otherwise
     */
    private static boolean isMaxShiftsPerDayRuleOff() {
        return Registry.getVarBoolean(SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_SHIFTS_PER_DAY, false);
    }
    
    /**
     * Access the registry to check if small events should contribute to max hours
     * Note this is constraint for max hours define for individual employee 
     * @return if the registry is set to true/Y/yes, return false otherwise
     */
    private static boolean isMaxHoursRuleOff() {
        return Registry.getVarBoolean(SMALL_EVENTS_NOT_CONTRIBUTE_TO_EMPLOYEE_MAX_HOURS, false);
    }
    
    /**
     * Access the registry to check if small events should contribute to max consecutive work days 
     * @return if the registry is set to true/Y/yes, return false otherwise
     */
    private static boolean isMaxConsecutiveDaysRuleOff() {
        return Registry.getVarBoolean(SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_CONSECUTIVE_DAYS, false);
    }
    
    /**
     * Access the registry to check if small events should contribute to max shifts per 
     * schedule 
     * @return if the registry is set to true/Y/yes, return false otherwise
     */
    private static boolean isMaxShiftsPerScheduleRuleOff() {
        return Registry.getVarBoolean(SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_SHIFTS_PER_SCHEDULE, false);
    }  
    
    /**
     * Access the registry to check if small events should contribute to max hours per 
     * day
     * @return if the registry is set to true/Y/yes, return false otherwise
     */
    private static boolean isMaxHoursPerDayRuleOff() {
        return Registry.getVarBoolean(SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_HOURS_PER_DAY, false);
    }  
    
    /**
     * Access the registry to check if small events should contribute to max hours per 
     * schedule 
     * @return if the registry is set to true/Y/yes, return false otherwise
     */
    private static boolean isMaxHoursPerScheduleRuleOff() {
        return Registry.getVarBoolean(SMALL_EVENTS_NOT_CONTRIBUTE_TO_MAX_HOURS_PER_SCHEDULE, false);
    }  

    /**
     * Determines minimum shift length for the shifts starting at <b>time</b> on <b>soDay</b>
     * based on shift rules for employee schedule group. 
     * @param empgrp 
     * @param soDay 
     * @param time the time when shift starts
     * @param minShiftLengths 
     * @param empgrpShiftRules 
     * @return minimum shift length
     * @throws RetailException 
     */
    static double getMinShiftLength(EmployeeGroup empgrp,
                                    final SODay soDay, Date time,
                                    Map empgrpShiftRules,
                                    Map minShiftLengths) throws RetailException {
        
        Integer empGrpId = empgrp.getID();

        StringBuffer key = new StringBuffer();
        key.append(empGrpId.toString()).
        append('|').
        append(soDay.intValue()).
        append('|').
        append(time.toString());

        // check if we have cached value
        Double minLength = (Double) minShiftLengths.get(key.toString());
        if(minLength != null){
            // found it
            return minLength.doubleValue();
        }
        
        // get shift rules from cache
        Collection shiftRules = (List) empgrpShiftRules.get(empGrpId);
        
        if(shiftRules == null){
            shiftRules = empgrp.getShiftRulesList();
            empgrpShiftRules.put(empGrpId, shiftRules);
        }
        
        // sorted set that sorts rules by min shift length
        Set matchingRules = new TreeSet(new Comparator(){

            public int compare(Object o1, Object o2) {
                ShiftRule r1 = (ShiftRule) o1;
                ShiftRule r2 = (ShiftRule) o2;
                // never return 'equals'
                try {
                    return (r1.getMinShiftLength(soDay) - r2.getMinShiftLength(soDay) < 0 ? -1 : 1);
                } catch (RetailException e) {
                    return 1;
                }
            }
            });
          
        // add rules that apply to day to a sorted set 
        ShiftRule shiftRule;
        for (Iterator iter = shiftRules.iterator(); iter.hasNext();) {
            shiftRule = (ShiftRule) iter.next();
            //is matching day ?
            if(SODaySet.get( shiftRule.getEgsrDayApplies() ).contain(soDay)){
                Date startTime = shiftRule.getEgsrWnStartTime();
                Date endTime = shiftRule.getEgsrWnEndTime();
                // is matching time ?
                // note that if startTime is same as endTime then the rules applied to 24 hours 
                boolean isMatchTime = startTime.equals(endTime) || 
                                      ((startTime.equals(time) || startTime.before(time)) && (endTime.after(time))); 
                
                if(isMatchTime){
                    matchingRules.add(shiftRule);
                }    
            }
        }
        
        // first matching rule has the shortest min shift length
        Iterator iter = matchingRules.iterator();
        if(iter.hasNext()){
            shiftRule = (ShiftRule) iter.next();
            double minShiftLength = shiftRule.getMinShiftLength(soDay);
            
            minShiftLengths.put(key.toString(), new Double(minShiftLength));

            return minShiftLength;
        }
        
        // minimum shift length not found. Return zero
        return 0;
    }

    
    /**
     * This method checks for PFS that are below the minimum length to be considered a
     * full shift and adds them to a custom temp file if they are. This method must be
     * called from <code>writeShiftIntoFilePostAction</code>. The minimum shift length
     * is determined by the shift rules specified for the given day.  If there are multiple
     * rules found for a day, it will pick the shortest.
     * 
     * @param soContext
     * @throws RetailException
     */
    public static void addSmallPFSToFile(SOData soContext) throws RetailException {
    	
    	boolean[] status = (boolean [])soContext.getLocalVariable(SO_CONTEXT_KEY_STATUS);
    	if (status == null) {
     	    return;
    	}
     	if (status[ WRITE_STATUS_IDX ] && status[ PARTIAL_FIXED_SHIFT_STATUS_IDX ]) {
    	    
            ShiftCover shiftCover = CalloutHelper.getShiftCover(soContext, ScheduleCallout.INCLUDE_SHIFT_CHOICE); 
  
            Employee employee = (Employee) soContext.getLocalVariable(SO_CONTEXT_KEY_EMPLOYEE);

            /** Cached shift rule lists. Employee group ID is the key */ 
            Map empgrpShiftRules =  (Map) soContext.getLocalVariable(SO_CONTEXT_KEY_EMPGRP_SHIFT_RULES);
            
            if(empgrpShiftRules == null){
                empgrpShiftRules = new HashMap();
                soContext.setLocalVariable(SO_CONTEXT_KEY_EMPGRP_SHIFT_RULES, empgrpShiftRules);
            }

            /** Cached minimum shift lengths. Key = 'empgrpID|Day_of_week|time' */ 
            Map minShiftLengths =  (Map) soContext.getLocalVariable(SO_CONTEXT_KEY_MIN_SHIFT_LENGTHS);
            
            if(minShiftLengths == null){
                minShiftLengths = new HashMap();
                soContext.setLocalVariable(SO_CONTEXT_KEY_MIN_SHIFT_LENGTHS, minShiftLengths);
            }
            
            
            SODay soDay = SODay.get( soContext.getScheduleStartDate().getDayOfWeek() );
            double minShiftLength = getMinShiftLength(employee.getEmpgrp(), soDay, 
                                                      shiftCover.getStartShiftTime().toDateObject(),
                                                      empgrpShiftRules, minShiftLengths);

            double shiftLength = shiftCover.m_fLength * shiftCover.m_IntervalTypeInMinutes;
            
            if ( shiftLength < minShiftLength) {
                HashSet oSetOfSmallPFS = (HashSet)soContext.getCustomData(SMALL_PFS_LIST);
                if (oSetOfSmallPFS == null) {
                    oSetOfSmallPFS = new HashSet();
                }
                if (!oSetOfSmallPFS.contains(shiftCover.m_strKey)) {
                    oSetOfSmallPFS.add(shiftCover.m_strKey);
                    soContext.addCustomData(SMALL_PFS_LIST,oSetOfSmallPFS);
                    FileManager oFileManager = soContext.getLocalFileManager();
                    Integer FileIdx = (Integer)soContext.getCustomData(SMALL_PFS_FILE_IDX);
                    oFileManager.write(FileIdx, "'"+shiftCover.m_strKey+"'\n");
                }
            }
    	}
    }

    /**
     * Removes shifs details created for small events from schedule.
     * Collects those removed shifts in a list stored in soContext   
     * @param soContext
     * @throws CalloutException
     */
    public static void populateSchedulePostAction(SOData soContext) throws CalloutException
    {
        try{
            
            ShiftDetail smallEvtShiftDet = (ShiftDetail)soContext.getLocalVariable(SHIFT_DETAIL_KEY);
            Set smallEventIds = (Set) soContext.getCustomData(SmallEventsScheduleCallout.SMALL_PFS_LIST);
            String[] row = (String[])soContext.getLocalVariable(ROW_KEY);
            Schedule skd = soContext.getSchedule();
            
            if(skd !=null && smallEvtShiftDet != null && smallEventIds != null && row != null && row.length >1 ){
                String shiftKey = row[1];
                
                if(smallEventIds.contains(shiftKey)){
                    
                    addSmallEventShift(soContext, smallEvtShiftDet);
                    skd.removeShiftDetail(smallEvtShiftDet);
                    
                    if(logger.isDebugEnabled()){
                        logger.debug("Small event shift removed:" + shiftKey);
                    }
                }
            }
        }catch(RetailException e){
            throw new CalloutException(e);
        }
    }
    
    /**
     * Loops through shift details (small events) removed in {@link #populateSchedulePostAction(SOData)}
     * and blends them with overlapping shifts.<br> 
     * If there are no overlapping shifts then the removed  shift detail is restored.<br>
     * If overlapping shift detail start or end time changed after merging, new shift is created
     * for such shift detail 
     * @param soContext
     * @throws CalloutException
     */
    public static void populateSchedulePostLoop(SOData soContext) throws CalloutException
    {
        try {
            
            Schedule skd = soContext.getSchedule();
            List smallEventShifts = getSmallEventShifts(soContext);
            
            for (Iterator iter = smallEventShifts.iterator(); iter.hasNext();) {
                
                ShiftDetail smallEvtShiftDet = (ShiftDetail) iter.next();
                
                Vector smallEvtActivities = smallEvtShiftDet.getShiftActivityList();
                
                if(smallEvtActivities == null || smallEvtActivities.isEmpty() || smallEvtActivities.size() > 1){
                    throw new CalloutException("Shift created for small event must have only one activity");
                }
                
                ShiftActivity evtActivity = (ShiftActivity)smallEvtActivities.get(0);
                
                boolean isEventApplied = false;
                
                Integer empId = smallEvtShiftDet.getEmpId();
                if(empId == null){
                    continue; // small events must be assigned
                }

                // apply filter
                Set overlappingShiftDetails = getOverlappingShiftDetailsForEmp(
                        skd, 
                        evtActivity.getShftactDate(),
                        evtActivity.getShftactStartTime(), 
                        evtActivity.getShftactEndTime(), 
                        empId );
                
                // blend small events with overlapping shifts
                boolean firstShift = true;
                for (Iterator itShift = overlappingShiftDetails.iterator(); itShift.hasNext(); ) {
                    ShiftDetail shftDet = (ShiftDetail) itShift.next();
                    
                    // add evtActivity once, to the first overlapping shftDet. 
                    // In case of transition shifts the second overlapping shftDet
                    // will only have activity times adjusted
                    if(firstShift){
                        shftDet.addShiftActivity(evtActivity);
                    }
                    applyEventToShiftDetail(shftDet, evtActivity);
                    
                    if(!firstShift){
                        /** event has already been applied to the first shift
                            readjust the shiftdetail to exclude event activity */
                        excludeAcitityTimes(shftDet, evtActivity);
                    }
                    firstShift = false;                   
                    isEventApplied = true;
                }
                
                if(!isEventApplied){
                    // small event shift does not overlap with normal shifts -> add it back to the schedule
                    skd.addShiftDetail(smallEvtShiftDet);                   
                    if(logger.isDebugEnabled()){
                        logger.debug("Non overlapping small event restored " + smallEvtShiftDet);
                    }
                }
            }
        }
        catch (RetailException e) {
            throw new CalloutException(e);
        }

    }
    
    /**
     * Readjust the shift detail start/end time to exclude activity 
     * @param shftDet
     * @param evtActivity
     * @throws RetailException
     */
    private static void excludeAcitityTimes(ShiftDetail shftDet, ShiftActivity evtActivity) throws RetailException {        
        if(evtActivity.getShftactStartTime().equals(shftDet.getShftdetStartTime())){
            shftDet.setShftdetStartTime(evtActivity.getShftactEndTime());
        }
        else if(evtActivity.getShftactEndTime().equals(shftDet.getShftdetEndTime())){
            shftDet.setShftdetEndTime(evtActivity.getShftactStartTime());
        }
    }
    
    /**
     * Applies event activity to shift detail by adjusting times of shift's activities.<br>
     * Note: Only working activities are adjusted 
     * @param shftDet Shift detail which activities will be adjusted 
     * @param evtActivity activity that is being inserted
     * @return true if shftDet start or end time were adjusted as a result of applying event
     * @throws RetailException
     */
    private static void applyEventToShiftDetail(ShiftDetail shftDet, ShiftActivity evtActivity) throws RetailException {
        
        List shftActivities = shftDet.getShiftActivityList();
        
        for (Iterator actIter = shftActivities.iterator(); actIter.hasNext();) {
            ShiftActivity act = (ShiftActivity) actIter.next();
            if(act != evtActivity){ //same event activity
                if(act.getActivity().isActWorking()){
                    if(isOverlap(act, evtActivity)){
                        PostProcessor.applyEvent(shftDet, act, evtActivity);
                    }
                }
            }
        }
    }

    /**
     * Returns true if two activities overlap
     * @param act
     * @param evtActivity
     * @return true if two activities overlap, false otherwise
     * @throws RetailException
     */
    private static boolean isOverlap(ShiftActivity act, ShiftActivity evtActivity) throws RetailException {
        
        Date actStartTime = act.getShftactStartTime();
        Date actEndTime = act.getShftactEndTime();
        
        Date evtStartTime = evtActivity.getShftactStartTime();
        Date evtEndTime = evtActivity.getShftactEndTime();
        
        return DateHelper.isBetween(evtStartTime, actStartTime, actEndTime) ||
               DateHelper.isBetween(evtEndTime, actStartTime, actEndTime) ;
    }

    /**
     * Retrieves all shift details from schedule that overlap with 
     * provided date, startTime and endTime, and belong to scpecified employee
     * @param skd
     * @param date
     * @param startTime
     * @param endTime
     * @param empId employee ID
     * @return list of ShiftDetail objects
     * @throws RetailException
     */
    private static Set getOverlappingShiftDetailsForEmp(Schedule skd, Date date, Date startTime, Date endTime, Integer empId) throws RetailException{
        if(startTime == null || endTime == null){
            logger.error("startTime and endTime cannot be null");
            throw new IllegalArgumentException("startTime and endTime cannot be null");
        }

        // Shift detail is sorted from the earliest to the latest shift
        Set filteredDetails = new TreeSet(new Comparator(){
            public int compare(Object o1, Object o2) {
                ShiftDetail det1 = (ShiftDetail) o1;
                ShiftDetail det2 = (ShiftDetail) o2;
                // never return 'equals'
                try {
                    return (det1.getShftdetStartTime().getTime() - det2.getShftdetStartTime().getTime() < 0 ? -1 : 1);
                } catch (RetailException e) {
                    return 1;
                }
            }
        });
        
        Vector shiftDetails = skd.getShiftDetailList(new SODate(date));
        
        for (int i=0; i < shiftDetails.size(); i++) {
            ShiftDetail shiftDet = (ShiftDetail) shiftDetails.get(i);

            if(! empId.equals(shiftDet.getEmpId())){
                continue;
            }
            Date start = shiftDet.getShftdetStartTime();
            Date end   = shiftDet.getShftdetEndTime();
            
            if(DateHelper.isBetween(startTime, start, end) ||
               DateHelper.isBetween(endTime, start, end )) {
                
                filteredDetails.add(shiftDet);
            }
        }
        return filteredDetails;
    }

    /**
     * Stores a shifts detail into a list of shifts details created to cover small events
     * @param soContext
     * @param shift
     */
    private static void addSmallEventShift(SOData soContext, ShiftDetail shift){
        
        List smallEventShifts = (List) soContext.getLocalVariable(SMALL_EVENT_SHIFTS);
        
        if(smallEventShifts == null){
            smallEventShifts = new ArrayList();
            soContext.addLocalVariable(SMALL_EVENT_SHIFTS, smallEventShifts);
        }
        smallEventShifts.add(shift);
    }

    /**
     * Retrieves list of shifts details created to cover small events from   soContext
     * @param soContext
     * @return list of shifts details
     */
    private static List getSmallEventShifts(SOData soContext){
        List smallEventShifts = (List) soContext.getLocalVariable(SMALL_EVENT_SHIFTS);
        
        if(smallEventShifts == null){
            smallEventShifts = Collections.EMPTY_LIST;
        }
        return smallEventShifts;
    }

}
