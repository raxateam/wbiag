package com.wbiag.app.modules.retailSchedule.services;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntityStaffRequirement;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirement;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager.Record;
import com.workbrain.app.modules.retailSchedule.services.FileManager;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.IntervalRequirementsHelper.BestFitInfo;
import com.workbrain.app.modules.retailSchedule.services.model.CalloutHelper;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.app.modules.retailSchedule.services.utils.RecurrencePattern;
import com.workbrain.app.modules.retailSchedule.type.InternalType;
import com.workbrain.app.modules.retailSchedule.type.IntervalType;
import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.app.modules.retailSchedule.utils.SOHashtable;
import com.workbrain.app.modules.retailSchedule.utils.SOTime;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.wbiag.app.modules.retailSchedule.services.volumeBestFit.*;


public class VolumeBestFitProcess extends ScheduleCallout {
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(VolumeBestFitProcess.class);
	
	private static final String NV_FCASTVOL_TRIGGER = "system/modules/scheduleOptimization/NV_FCASTVOL_TRIGGER";
	private static final int NON_VOLUME_DRIVER_TYPE = 1;
	private static final String BEST_FIT_MAPPING = "BEST_FIT_MAPPING";
	private static final String REQUIREMENT_DATA_MAP = "WOW_WIN_WRKLD_MAP";
	
	public VolumeBestFitProcess() {	    
	}
    
    /**
     * Modifies Best Fit requirements based on custom windows
     * 
     * Process all departments except the first one.  The first department's windows will be
     * outputted to the bestfit text file via the normal core implementation. 
     *
     * @param  soContext    global context object used to access internal SO data.    
     * @return              false, to skip this Interval Requirement.
     */
    public boolean addIRToMatrixPreAction(SOData soContext) throws CalloutException{
    	boolean result = true;
    	DBConnection conn = null;
  		BestFitMapping bestFitMapping = null;
  		
  	  	try {
  	    	conn = soContext.getDBconnection();
	  	  	Schedule sched = soContext.getSchedule();
	  	  		
  	    	// Retrieve soContext stored variables
  	  		IntervalRequirement tmp = (IntervalRequirement)soContext.getLocalVariable(ScheduleCallout.ADD_IR_TO_MATRIX+CalloutHelper.TYPE_IR);
  	  		Map bestFitMap = (Map)soContext.getCustomData(BEST_FIT_MAPPING);
  	  		Map requirementDataMap = (Map)soContext.getCustomData(REQUIREMENT_DATA_MAP);
			if (requirementDataMap == null){
				requirementDataMap = new HashMap();
  	  	    	soContext.addCustomData(REQUIREMENT_DATA_MAP, requirementDataMap);
			}
  	  		  	  		
  	  		CorporateEntityStaffRequirement csr = tmp.getStaffReq();
  	  		
  	  		if (csr == null){
  	  			if (logger.isDebugEnabled()){ logger.debug("WOW -addIRTomatrixPreAction- IR staff requirement is NULL"); }
  	  			return result;
  	  		}

  	  		// Only process staff requirements that are Non-Volume 

  	  		if (!(csr.getCsdNonvlmFlag().intValue() == NON_VOLUME_DRIVER_TYPE)){
  	  			if (logger.isDebugEnabled()){ logger.debug("WOW -addIRTomatrixPreAction- requirement is not VDBF :"+csr.toString()); }
  	  			return result;
  	  		}  	
  	  		

  	  		SODate irDate = tmp.getDate();
  	  		SOTime irTime = tmp.getTime();

	  		if (!((irTime.getCalendar().get(Calendar.HOUR_OF_DAY) == 23) &&
	  				(irTime.getCalendar().get(Calendar.MINUTE) == 0))){ 
	  			return result; 
	  		}
            
            //if the current schedule end date is before the interval requirement date, just return
            if (sched.getSkdToDate().before(irDate.getCalendar())){
                return result;
            }

            
  	  		int dayIndex = irDate.getDayOfWeek()-1;
  	        int startDOW = soContext.getScheduleStartDate().getDayOfWeek()-1;
  	  		IntervalType intervalType = IntervalType.get(tmp.getInvtypId());
            
            

  	  		int workloadSwitch = Registry.getVarInt(NV_FCASTVOL_TRIGGER, 9999);
  	  		if(csr.getCsdWrkldHrs().intValue() != workloadSwitch){
                if (!sched.ishonourSkillsRuleOn()) {
                    tmp.setStsklId(null);
                }
                
                //***String positionKey = tmp.getIdentifier();
                String positionKey = IntervalRequirement.getIdentifier(new Integer(tmp.getSkdgrpId()), tmp.getJobId(), tmp.getStsklId(), tmp.getActId());
                
				Date start = getStartTime(csr.getCsdStartTime(), irDate, csr);
				Date end = getEndTime(csr.getCsdEndTime(), irDate, csr);
                
                //***long startIntrvl = getStartInterval(start, intervalType.getIntervalsPerHour());
                //***long irLength = getIRLength(start, end, intervalType.getIntervalsPerHour());

				long startIntrvl = getStartInterval(start, intervalType);
  				long irLength = getIRLength(start, end, intervalType);

  				//***createBFReqData(requirementDataMap, irDate, dayIndex, start, csr.getCsdWrkldHrs().doubleValue() * intervalType.getIntervalsPerHour(), positionKey, startIntrvl, irLength);
  	  			createBFReqData(requirementDataMap, irDate, dayIndex, start, csr.getCsdWrkldHrs().doubleValue()* 60/intervalType.getLengthInMinutes(), positionKey, startIntrvl, irLength);
                
  	  			soContext.addCustomData(REQUIREMENT_DATA_MAP, requirementDataMap);
  	  			
  	  			return result;
  	  		}
  	  		if (bestFitMap != null){
  	  			bestFitMapping = (BestFitMapping)bestFitMap.get(csr.getSkdgrpId());
  	  		}
  	  		
			if ((bestFitMap == null) || (bestFitMapping == null)){

  	  			if (bestFitMap == null) bestFitMap = new HashMap();

  	  	  		SOHashtable htDeptsPerScheduleArea = new SOHashtable();
                Map stdVolHoursMap = new HashMap();
  	  	  		//***ScheduleIRCalculator.getScheduleAreas(soContext.getScheduleCE(), htDeptsPerScheduleArea, sched);  	
  	  	  		getScheduleAreas(soContext.getScheduleCE(), htDeptsPerScheduleArea, sched, stdVolHoursMap);     
  	  			
  	  			bestFitMapping = new BestFitMapping(conn, 
  	  												   csr.getSkdgrpId().intValue(),
  	  												   soContext.getIntervalRequirements().getRequirementsList(),
  	  												   soContext.getSchedule(),
  	  												   htDeptsPerScheduleArea,
  	  												   stdVolHoursMap);
  	  			bestFitMap.put(csr.getSkdgrpId(), bestFitMapping);
  	  			
  	  			soContext.addCustomData(BEST_FIT_MAPPING, bestFitMap);
 	  			
  	  		}   	  		
			
  	  		// Retrieve all departments that belong to this ScheduleArea
  	  		List deptList = bestFitMapping.getDeptList();
  	  		
            if (deptList.size() == 0) return result;
  	  		  	  		
  	  		// Loop through all the departments and create the window records in the BF file
  	  		// for all the appropriate staff requirements
  	  		for (Iterator iter = deptList.iterator(); iter.hasNext();){
  	  			CorporateEntity curDept = (CorporateEntity)iter.next();
  	  	  		//CorporateEntity curDept = (CorporateEntity)deptList.get(i);
  	  	  		Integer curDeptSkdGrpId = (Integer)curDept.getSkdgrpData().getSkdgrpId();  	 
  	  	  		
  	  	  		List windowTimes = bestFitMapping.getWindowList(csr, curDeptSkdGrpId, adjustDOW(dayIndex,startDOW,false));
  	  	  		
  	  	  		if (windowTimes == null) continue;
  	  	  		
  	  			Iterator windowTimesIt = windowTimes.iterator();
  	  			while (windowTimesIt.hasNext()){
  	  				Map curWindow = (Map)windowTimesIt.next();
  	  				Date curStart = (Date)curWindow.get(BestFitWindowUtil.WINDOW_START);
  	  				Date curEnd = (Date)curWindow.get(BestFitWindowUtil.WINDOW_END);

  	  				if (logger.isDebugEnabled()){ 
  	  					logger.debug("WOW -addIRTomatrixPreAction- window="+curWindow);
  	  				}	  	  			  			
  	  				
  	  				  	  				
	  	  			double workload = getWorkloadByDay(bestFitMapping,curDeptSkdGrpId,csr,adjustDOW(dayIndex,startDOW,false));
//	  	  			boolean onlyConsiderEqualWindows = true;
//	  	  			double workload = getTotalWorkloadByDay(bestFitMapping,
//	  	  													 curDeptSkdGrpId,
//	  	  													 curStart,
//	  	  													 curEnd,
//	  	  													 deptList,
//	  	  													 csr,
//	  	  													 adjustDOW(dayIndex,startDOW,false),
//	  	  													 dayIndex,
//	  	  													 conn,
//	  	  													 onlyConsiderEqualWindows);	  	  					
  					  					
                    // Copied from core com.workbrain.app.modules.retailSchedule.services.PreProcessor
  					// NS - Skill will always be written out in the event one of multiple staff groups has the rule turned on	  	  					
                    if (!sched.ishonourSkillsRuleOn()) {
                        tmp.setStsklId(null);
                    }
                    
                    //***String positionKey = tmp.getIdentifier();
                    String positionKey = IntervalRequirement.getIdentifier(new Integer(tmp.getSkdgrpId()), tmp.getJobId(), tmp.getStsklId(), tmp.getActId());
                    
                    //***long startIntrvl = getStartInterval(curStart, intervalType.getIntervalsPerHour());
                    //***long irLength = getIRLength(curStart, curEnd, intervalType.getIntervalsPerHour());                     
                    
  					long startIntrvl = getStartInterval(curStart, intervalType);
                    long irLength = getIRLength(curStart, curEnd, intervalType);
                    
  	  				// Write the window details to the map
  	  				if (logger.isDebugEnabled()){ 
  	  					logger.debug("WOW -addIRTomatrixPreAction- curStart="+curStart); 
  	  					logger.debug("WOW -addIRTomatrixPreAction- irDate="+irDate); 
  	  					logger.debug("WOW -addIRTomatrixPreAction- irLength="+irLength); 
  	  					logger.debug("WOW -addIRTomatrixPreAction- workload="+workload); 
  	  				}

  	  				createBFReqData(requirementDataMap, irDate, dayIndex, curStart, workload, positionKey, startIntrvl, irLength);
  	  			}
  	  		}
  	    	soContext.addCustomData(REQUIREMENT_DATA_MAP, requirementDataMap);
  	  		
  	  } catch (Exception e) {
  		  throw new CalloutException ("addIRToMatrixPreAction error", e);
  	  }

  	  if (logger.isDebugEnabled()){ logger.debug("WOW -addIRTomatrixPreAction- return TRUE"); }
  	  return result;
    }

    /**
     * Modifies Best Fit Requirement based on custom windows
     * 
     * Modifies the outputted data for the first department in the list based on the defined windows
     * 
     * @param  soContext    global context object used to access internal SO data.
     * @return              false, to skip this Interval Requirment or modify result on IR inclusion.
     */
    public boolean buildBestFitRequirementPreAction(SOData soContext) throws CalloutException{
        List startInterval = null;
        Integer numberOfWindows = null;
        List length = null;
        List required = null;
        Integer maxRequired = null;
        String key;
        int numberOfInterval = 60/soContext.getIntervalType().getLengthInMinutes();
        
        
        key = (String) soContext.getLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE+CalloutHelper.TYPE_STRING);
        startInterval = (List) soContext.getLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE + CalloutHelper.TYPE_LIST1);
        numberOfWindows = (Integer) soContext.getLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE+ CalloutHelper.TYPE_INT);
        length = (List) soContext.getLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE+ CalloutHelper.TYPE_LIST2);
        required = (List) soContext.getLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE+ CalloutHelper.TYPE_INT2);
        maxRequired = (Integer) soContext.getLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE+ CalloutHelper.TYPE_INT3);

       
        Map requirementDataMap = (Map)soContext.getCustomData(REQUIREMENT_DATA_MAP);
        

        int workloadSwitch = Registry.getVarInt(NV_FCASTVOL_TRIGGER, 9999); 
        int triggerNumberToCompare = workloadSwitch*numberOfInterval;
        
        List newRequired = new ArrayList();
        List newStartIntervals = new ArrayList();
        int newNumberOfWindows = 0;
        List newLength = new ArrayList();
        
        List windowsAdded = new ArrayList();
        if (requirementDataMap != null) {

            Set keyList = (Set)requirementDataMap.keySet();
            for (Iterator it = keyList.iterator(); it.hasNext();) {
                String key1 = (String) it.next();
                BFRequirementData bfData = (BFRequirementData)requirementDataMap.get(key1);
                if (key.equals(bfData.positionKey)) {
                    
                    for (int j = 0; j<numberOfWindows.intValue(); j++) {
                        if (((Integer)required.get(j)).intValue() == triggerNumberToCompare) {
                            
                            //find out which day it is processing
                            try {
                                int dayIndex = findDayIndexFromStartInterval(soContext.getIntervalType(), (Integer) startInterval.get(j));
                                int dayIndexForThisBFData = findDayIndexFromScheduleAndCurrentDate(soContext.getScheduleStartDate(), bfData.getIrDate());
                                if (dayIndex!=dayIndexForThisBFData) {
                                    continue;
                                }
                            } catch (RetailException re) {
                                throw new CalloutException(re);
                            }
                                
                            newRequired.add(new Integer((int)bfData.getWorkload()));
                            
                            
                            try {
                                Integer startIntervalFor9999NVReq = convertToStartInterval(soContext.getScheduleStartDate(), bfData.getIrDate(), bfData.getStart(), soContext.getIntervalType());
                                newStartIntervals.add(startIntervalFor9999NVReq);
                                windowsAdded.add(startIntervalFor9999NVReq+"_"+bfData.getLength());
                            } catch (RetailException re) {
                                throw new CalloutException(re);
                            }
                            newLength.add(new Integer((int)bfData.getLength()));
                            newNumberOfWindows++;
                            
                            
                        } //end of if statement
                        
                    }
                    

                }
                
            }
            
            //for all the windows not added, need to add them back
            //for the bestfit, some of them have already been included with the volume-driven best fit requirement, so with the 
            //same startinterval and length as the added volume-driven best fit requirement, we can bypass them
            for (int j = 0; j<numberOfWindows.intValue(); j++) {
                if (((Integer)required.get(j)).intValue() != triggerNumberToCompare) {
                    String tempKey = startInterval.get(j) + "_" + length.get(j);
                    if (!windowsAdded.contains(tempKey)) {
                        newRequired.add(required.get(j));
                        newLength.add(length.get(j));
                        newStartIntervals.add(startInterval.get(j));
                        newNumberOfWindows++;
                    }
                }
            }

            soContext.setLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE + CalloutHelper.TYPE_LIST1, newStartIntervals);
            soContext.setLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE+ CalloutHelper.TYPE_INT, new Integer(newNumberOfWindows));
            soContext.setLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE+ CalloutHelper.TYPE_LIST2, newLength);
            soContext.setLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE+ CalloutHelper.TYPE_INT2, newRequired);
            soContext.setLocalVariable(ScheduleCallout.BUILD_BESTFITREQUIREMENTS_FILE+ CalloutHelper.TYPE_INT3, maxRequired);
            
            soContext.addCustomData(REQUIREMENT_DATA_MAP, requirementDataMap);

            return true;
      }

      return true;

    }    
    
    private Integer convertToStartInterval(SODate scheduleStartDate, SODate intervalDate, Date intervalTime, IntervalType intervalType) throws RetailException {

        int totalNumberOfIntervalInOneDay = 60*24 / intervalType.getLengthInMinutes();
        int index = findDayIndexFromScheduleAndCurrentDate(scheduleStartDate, intervalDate);
        Calendar calendarTmp = Calendar.getInstance();
        calendarTmp.setTime(intervalTime);
        int numberOfMinutesIntoTheDay= calendarTmp.get(Calendar.HOUR_OF_DAY)*60 + calendarTmp.get(Calendar.MINUTE);
        int indexIntoTheDay = (int)numberOfMinutesIntoTheDay/intervalType.getLengthInMinutes();
        return new Integer(index*totalNumberOfIntervalInOneDay +indexIntoTheDay +1 );
        
    }
    
    private int findDayIndexFromStartInterval(IntervalType intervalType, Integer startInterval) {
        int numberOfIntervalsPerDay = 24*60/intervalType.getLengthInMinutes();
        int dayIndex = startInterval.intValue()/numberOfIntervalsPerDay;
        return dayIndex;
    }
    
    private int findDayIndexFromScheduleAndCurrentDate(SODate scheduleStartDate, SODate intervalDate) throws RetailException{
        int index = SODate.getDaysDiff(intervalDate, scheduleStartDate)-1;
        return index;
    }

    /**
     * Modifies requirements for shift generation based on custom windows
     * 
     * @param  soContext    global context object used to access internal SO data.
     * @return              false, to skip this Interval Requirment or modify result on IR inclusion.
     */    
    public void addIRToMatrixPostLoop(SOData soContext) throws CalloutException {
        List bestFit = new ArrayList(32);
        DBConnection conn = null;
        BestFitMapping bestFitMapping = null;
    	  
  	  	try {
  	  		//writeBestFitFile(soContext);
  	  		  	  		
  	    	conn = soContext.getDBconnection();
    		IntervalRequirementsManager irm = soContext.getIntervalRequirements();
  	  		Schedule sched = soContext.getSchedule();
            List IRRecords = irm.getRequirementsList();
            IntervalType intervalType = irm.getIRIntervalType();
            int intervalLength = intervalType.getLengthInMinutes();
//            Date midnight = DateHelper.convertStringToDate("00:00","HH:mm");
            //***int interval = 60/soContext.getPreProcessor().getIntervalTypeInMinutes();
            //***int startDOW = soContext.getScheduleStartDate().getDayOfWeek()-1;
            int startDOW = soContext.getScheduleStartDate().getDayOfWeek()-1;
            int interval = 60/intervalLength;
            int numberOfDaysInSchedule = sched.getLengthInDays();
            
            Map bestFitMap = (Map)soContext.getCustomData(BEST_FIT_MAPPING);
    		
            boolean allNonVolumeBestFit = true;
            
            // Store only the best fit requirements and return if there aren't any
            for (Iterator iter = IRRecords.iterator(); iter.hasNext();) {
            	IntervalRequirementsManager.Record rec = (IntervalRequirementsManager.Record) iter.next();
                if(rec.m_isBestFit){
                    bestFit.add(rec);
                    
                    CorporateEntityStaffRequirement csr = rec.m_staffReq;
          	  		if (csr == null) continue;
                    
          	  		// Only process staff requirements that are Non-Volume and have workload set to NV_FCASTVOL_TRIGGER value
          	  		int workloadSwitch = Registry.getVarInt(NV_FCASTVOL_TRIGGER, 9999);                
          	  		if (!((csr.getCsdNonvlmFlag().intValue() == NON_VOLUME_DRIVER_TYPE) && 
          	  	  			(csr.getCsdWrkldHrs().floatValue() == workloadSwitch))) {
          	  			allNonVolumeBestFit = false;
          	  		}
                } else {
                	allNonVolumeBestFit = false;
                }
            }
            
    		
            if (bestFit.size() == 0) {
            	if (logger.isDebugEnabled()){ logger.debug("WOW -addIRToMatrixPostLoop- no best fit requirements"); }
                return;
            }    		
            if (logger.isDebugEnabled()){ logger.debug("WOW -addIRToMatrixPostLoop- all requirements are Non Volume Best Fit? "+allNonVolumeBestFit); }
  	  		
            for (Iterator iter = bestFit.iterator(); iter.hasNext();) {
            	IntervalRequirementsManager.Record ir = (IntervalRequirementsManager.Record) iter.next();
                CorporateEntityStaffRequirement csr = ir.m_staffReq;
                
      	  		if (csr == null){
      	  			if (logger.isDebugEnabled()){ logger.debug("WOW -addIRToMatrixPostLoop- IR staff requirement is NULL"); }
      	  			continue;
      	  		}

      	  		// Only process staff requirements that are Non-Volume and have workload set to NV_FCASTVOL_TRIGGER value
      	  		int workloadSwitch = Registry.getVarInt(NV_FCASTVOL_TRIGGER, 9999);                
      	  		if (!((csr.getCsdNonvlmFlag().intValue() == NON_VOLUME_DRIVER_TYPE) && 
      	  	  			(csr.getCsdWrkldHrs().floatValue() == workloadSwitch))) continue;                 
      	  		
	      	  	if (bestFitMap != null){
	      			bestFitMapping = (BestFitMapping)bestFitMap.get(csr.getSkdgrpId());
	      		}
	      		
	      		if ((bestFitMap == null) || (bestFitMapping == null)){
	
	      			if (bestFitMap == null) bestFitMap = new HashMap();

	        		SOHashtable htDeptsPerScheduleArea = new SOHashtable();
                    Map stdVolHoursMap = new HashMap();
                    
	        		//***ScheduleIRCalculator.getScheduleAreas(soContext.getScheduleCE(), htDeptsPerScheduleArea, sched);  	  		
	      			getScheduleAreas(soContext.getScheduleCE(), htDeptsPerScheduleArea, sched, stdVolHoursMap); 
                    
	      			bestFitMapping = new BestFitMapping(conn, 
	      												   csr.getSkdgrpId().intValue(),
	      							       				   soContext.getIntervalRequirements().getRequirementsList(),
	      							       				   soContext.getSchedule(),
	      							       				   htDeptsPerScheduleArea,
	      							       				   stdVolHoursMap);
	      			bestFitMap.put(csr.getSkdgrpId(), bestFitMapping);
	      			
	      			soContext.addCustomData(BEST_FIT_MAPPING, bestFitMap);
	      		}      	  		
      	  		
       	  		int job_id = csr.getJobId().intValue();
      	  		Integer skill_id = csr.getStsklId();
      	  		Integer act_id = csr.getActId();                
      	  	      	  		
      	  		List deptList = bestFitMapping.getDeptList();
      	  		
                long dayLength = interval * 24;
                
                // Copied from core com.workbrain.app.modules.retailSchedule.services.PreProcessor
  				// NS - Skill will always be written out in the event one of multiple staff groups has the rule turned on	  	  					
                int skill = skill_id == null ? -1 : skill_id.intValue();
                int actId = act_id == null ? -1 : act_id.intValue();
                if (!sched.ishonourSkillsRuleOn()) {
                    skill = -1;
                }
  				String positionKey = createKey(csr.getSkdgrpId().intValue(), job_id, skill, actId);
                
                

  				// Zero out all values greater than 0 ONLY if all best fit reqs are non-volume best fit
  				if (allNonVolumeBestFit){
  	  				for (int j = 0; j < numberOfDaysInSchedule*dayLength; j ++) {
  	                	//***int value = (int)soContext.getPreProcessor().m_oRequirements.get(j, positionKey);
  	                	int value=soContext.getRequirementFromMatrix(j, positionKey);
                        if (value > 0){
  	                        //***soContext.getPreProcessor().m_oRequirements.put(j, positionKey, 0);
                            soContext.addRequirementToMatrix(j, positionKey, 0);
                        }
  	                }
  				}      	  		
      	  		
  				// Process each day at a time
      	  		for (int dayIndex=0; dayIndex<numberOfDaysInSchedule; dayIndex++){      	  		
      	  		
	                long dayStart = dayIndex * dayLength;
	                	  				
      	  			// Loop through each dept and process all valid requirements
      	  			for (int i=0; i<deptList.size(); i++){      	  		
      	  		
	      	  	  		CorporateEntity curDept = (CorporateEntity)deptList.get(i);
	      	  	  		Integer curDeptSkdGrpId = (Integer)curDept.getSkdgrpData().getSkdgrpId();  	  
	      	  	  			      	  	  		
	      	  	  		List windowTimes = bestFitMapping.getWindowList(csr, curDeptSkdGrpId, dayIndex);
    	  	  			if ((windowTimes == null) || (windowTimes.size() == 0)) continue;
    	  	  			
  	  					// Evaluate workload values within the windows
	            	  	Date curStart = null;
	                	Date curEnd = null; 
	            	  	Iterator windowTimesIt = windowTimes.iterator();		                    
	            	  	while (windowTimesIt.hasNext()){
	            	  		Map curWindow = (Map)windowTimesIt.next();
	            			curStart = (Date)curWindow.get(BestFitWindowUtil.WINDOW_START);
	            			curEnd = (Date)curWindow.get(BestFitWindowUtil.WINDOW_END);      	  			
	            			
    	  	  				if (logger.isDebugEnabled()){ 
    	  	  					logger.debug("WOW -addIRToMatrixPostLoop- window="+curWindow);
    	  	  				}
	            			
	            			//double workload = getWorkloadByDay(deptStfReq,curDeptSkdGrpId,csr,dayIndex);
	            			boolean onlyConsiderEqualWindows = true;
	    	  	  			double workload = getTotalWorkloadByDay(bestFitMapping,
	    	  	  												     curDeptSkdGrpId,
	    	  	  												     curStart,
	    	  	  												     curEnd,
	    	  	  												     deptList,
	    	  	  												     csr,
	    	  	  												     dayIndex,
	    	  	  												     adjustDOW(dayIndex,startDOW),
	    	  	  												     conn,
	    	  	  												     onlyConsiderEqualWindows);	
	            				                        
	                        long startIntrvl = getStartInterval(curStart, interval);
	                        long dayStartIntrvl = startIntrvl + dayStart;	            			
	                        long irLength = getIRLength(curStart, curEnd, interval);
	                          
	                        if (logger.isDebugEnabled()){ 
    	  	  					logger.debug("WOW -addIRToMatrixPostLoop- dayStartIntrvl="+dayStartIntrvl);
    	  	  					logger.debug("WOW -addIRToMatrixPostLoop- irLength="+irLength);
    	  	  					logger.debug("WOW -addIRToMatrixPostLoop- workload="+workload);
    	  	  				}
	                        
	                        for (int k = (int)dayStartIntrvl; k < dayStartIntrvl+irLength; k++){
		                    	//***int value = (int)soContext.getPreProcessor().m_oRequirements.get(k, positionKey);
	                        	int value=soContext.getRequirementFromMatrix(k, positionKey);
                                if (value == -1) value = (int)workload;
	                        	else value += workload;
		                        //***soContext.getPreProcessor().m_oRequirements.put(k, positionKey, value);
                                soContext.addRequirementToMatrix(k, positionKey, value);
	                        }
	                    } 		                    
      	  			}
      	  		}
            }
    		
    	} catch (Exception e) {
   		  	throw new CalloutException ("addIRToMatrixPostLoop error", e);
    	} 
    	if (logger.isDebugEnabled()){ 
    		//***logger.debug("WOW -addIRToMatrixPostLoop- m_oRequirements: "+soContext.getPreProcessor().m_oRequirements.toString()); 
        }
    }

    /**
     * Modifies Interval Requirements based on custom windows
     * 
     * @param  soContext    global context object used to access internal SO data.
     * @return              false, to skip this Interval Requirment or modify result on IR inclusion.
     */
    public boolean modifyIRPostOptimizationPreAction(SOData soContext) throws CalloutException {
    	boolean result = false;
        List bestFit = new ArrayList(32);
        DBConnection conn = null;
        int interval = 4; // m_TotalNeeded and m_bestFitDetails deals in 15 min intervals
        BestFitMapping bestFitMapping = null;
    	  
  	  	try {
  	    	conn = soContext.getDBconnection();
    		IntervalRequirementsManager irm = soContext.getIntervalRequirements();
  	  		Schedule sched = soContext.getSchedule();
            List IRRecords = irm.getRequirementsList();
            int startDOW = soContext.getScheduleStartDate().getDayOfWeek()-1;
            SODate startOfWeek = soContext.getScheduleStartDate();
            
            Map bestFitMap = (Map)soContext.getCustomData(BEST_FIT_MAPPING);
  	  		Map requirementDataMap = (Map)soContext.getCustomData(REQUIREMENT_DATA_MAP);
    		
            for (Iterator iter = IRRecords.iterator(); iter.hasNext();) {
                Record rec = (Record) iter.next();
                if(rec.m_isBestFit){
                    bestFit.add(rec);
                }
            }
            
            if (bestFit.size() == 0) {
            	if (logger.isDebugEnabled()){ 
            		logger.debug("WOW -modifyIRPostOptimizationPreAction- no best fit requirements"); 
            	}
                return result;
            }    		

			if (requirementDataMap == null){
				requirementDataMap = new HashMap();
  	  	    
	            for (Iterator iter = bestFit.iterator(); iter.hasNext();) {
	            	IntervalRequirementsManager.Record ir = (IntervalRequirementsManager.Record) iter.next();
	                CorporateEntityStaffRequirement csr = ir.m_staffReq;

	      	  		if (csr == null){
	      	  			if (logger.isDebugEnabled()){ 
	      	  				logger.debug("WOW -modifyIRPostOptimizationPreAction- IR staff requirement is NULL"); 
	      	  			}
	      	  			continue;
	      	  		}

	      	  		// Only process staff requirements that are Non-Volume
	      	  		if (!(csr.getCsdNonvlmFlag().intValue() == NON_VOLUME_DRIVER_TYPE)) 
	      	  	  		continue;                

	      	  		int workloadSwitch = Registry.getVarInt(NV_FCASTVOL_TRIGGER, 9999);
	      	  		if(csr.getCsdWrkldHrs().intValue() != workloadSwitch){
	      	  			int skill_id = csr.getStsklId() != null ? csr.getStsklId().intValue() : -1;
	      	            if (!sched.ishonourSkillsRuleOn()) {
	      	            	skill_id = -1;
	      	            }
	      	  			String positionKey = createKey(csr.getSkdgrpId().intValue(),csr.getJobId().intValue(),skill_id,csr.getActId().intValue());

	      	  			for (int dayIndex=0; dayIndex<7; dayIndex++){
	      					SODate curDate = new SODate(DateHelper.addDays(startOfWeek.toDateObject(),dayIndex));
	      	  				Date start = getStartTime(csr.getCsdStartTime(), curDate, csr);
	      					Date end = getEndTime(csr.getCsdEndTime(), curDate, csr);
	      					
		    				long startIntrvl = getStartInterval(start, interval);
		      				long irLength = getIRLength(start, end, interval);
	
		      	  			createBFReqData(requirementDataMap, curDate, adjustDOW(dayIndex,startDOW), start, csr.getCsdWrkldHrs().doubleValue() * interval, positionKey, startIntrvl, irLength);
	      	  			}
	      	  			continue;
	      	  		}	      	  		
	      	  		
	        		if (bestFitMap != null){
	        			bestFitMapping = (BestFitMapping)bestFitMap.get(csr.getSkdgrpId());
	        		}
        		
	        		if ((bestFitMap == null) || (bestFitMapping == null)){

	        			if (bestFitMap == null) bestFitMap = new HashMap();
        			
                        
	        			SOHashtable htDeptsPerScheduleArea = new SOHashtable();
                        HashMap stdVolHoursMap = new HashMap();
	        			//***ScheduleIRCalculator.getScheduleAreas(soContext.getScheduleCE(), htDeptsPerScheduleArea, sched);  
	        			getScheduleAreas(soContext.getScheduleCE(), htDeptsPerScheduleArea, sched, stdVolHoursMap);  
                        
                        
	        			bestFitMapping = new BestFitMapping(conn, 
        								  				   csr.getSkdgrpId().intValue(),
        								  				   soContext.getIntervalRequirements().getRequirementsList(),
        								  				   soContext.getSchedule(),
        								  				   htDeptsPerScheduleArea,
        								  				   stdVolHoursMap);
	        			bestFitMap.put(csr.getSkdgrpId(), bestFitMapping);
        			
	        			soContext.addCustomData(BEST_FIT_MAPPING, bestFitMap);
	        		}      
	        		
	        		List deptList = bestFitMapping.getDeptList();
	        		
	        		for (Iterator iter1 = deptList.iterator(); iter1.hasNext();){
	      	  			CorporateEntity curDept = (CorporateEntity)iter1.next();
	      	  	  		Integer curDeptSkdGrpId = (Integer)curDept.getSkdgrpData().getSkdgrpId();  	 
	      	  	  		
	      	  	  		for (int dayIndex=0; dayIndex<7; dayIndex++){
		      	  	  		List windowTimes = bestFitMapping.getWindowList(csr, curDeptSkdGrpId, dayIndex);
		      	  	  		
		      	  	  		if (windowTimes == null) continue;

		      	  			Iterator windowTimesIt = windowTimes.iterator();
		      	  			while (windowTimesIt.hasNext()){
		      	  				Map curWindow = (Map)windowTimesIt.next();
		      	  				Date curStart = (Date)curWindow.get(BestFitWindowUtil.WINDOW_START);
		      	  				Date curEnd = (Date)curWindow.get(BestFitWindowUtil.WINDOW_END);
	
		    	  	  			double workload = getWorkloadByDay(bestFitMapping,curDeptSkdGrpId,csr,dayIndex);
		                        int skill_id = csr.getStsklId() != null ? csr.getStsklId().intValue() : -1;
			      	            if (!sched.ishonourSkillsRuleOn()) {
			      	            	skill_id = -1;
			      	            }
		    	  	  			String positionKey = createKey(csr.getSkdgrpId().intValue(),csr.getJobId().intValue(),skill_id,csr.getActId().intValue());
		      					long startIntrvl = getStartInterval(curStart, interval);
		      	  				long irLength = getIRLength(curStart, curEnd, interval);  	  				
		      	  				
		      	  				createBFReqData(requirementDataMap, new SODate(DateHelper.addDays(startOfWeek.toDateObject(),dayIndex)), adjustDOW(dayIndex,startDOW), curStart, workload, positionKey, startIntrvl, irLength);
		      	  			}

	      	  	  		}
	        		}
	            }				
			}            
            
			List bestFitInfoList = new ArrayList();
			
            for (Iterator iter = bestFit.iterator(); iter.hasNext();) {
            	IntervalRequirementsManager.Record ir = (IntervalRequirementsManager.Record) iter.next();
                CorporateEntityStaffRequirement csr = ir.m_staffReq;

      	  		if (csr == null){
      	  			if (logger.isDebugEnabled()){ 
      	  				logger.debug("WOW -modifyIRPostOptimizationPreAction- IR staff requirement is NULL"); 
      	  			}
      	  			continue;
      	  		}

      	  		// Only process staff requirements that are Non-Volume
      	  		if (!(csr.getCsdNonvlmFlag().intValue() == NON_VOLUME_DRIVER_TYPE)) 
      	  	  		continue;                

      	  		int workloadSwitch = Registry.getVarInt(NV_FCASTVOL_TRIGGER, 9999);      	  		
      	  		if (csr.getCsdWrkldHrs().floatValue() != workloadSwitch){
                    int skill_id = csr.getStsklId() != null ? csr.getStsklId().intValue() : -1;
      	            if (!sched.ishonourSkillsRuleOn()) {
      	            	skill_id = -1;
      	            }
	      	  		String positionKey = createKey(csr.getSkdgrpId().intValue(),csr.getJobId().intValue(),skill_id,csr.getActId().intValue());
	
					// Zero out all workload numbers
	      	  		ir.resetTotalNeeded();
	                resetBestFitDetails((IntervalRequirementsManager.Record)ir);  				
	  				
	                String recurrencePatternStr  = csr.getCsdRecurPatt();
	                RecurrencePattern recurrencePattern = new RecurrencePattern(recurrencePatternStr);
	                
	                for (int dayIndex=0; dayIndex < 7; dayIndex++){
	  					SODate curDate = new SODate(DateHelper.addDays(startOfWeek.toDateObject(),dayIndex));
	  					
	  					if (recurrencePattern!=null && !recurrencePattern.isValidDay(curDate.toDateObject())) {
	  						//if user has defined a weekly pattern, and today is not included in the pattern, skip it
	  						continue;
	  					}
	  					
	  	  				Date start = getStartTime(csr.getCsdStartTime(), curDate, csr);
	  					Date end = getEndTime(csr.getCsdEndTime(), curDate, csr);
		      	  		
		      	  		long startIntrvl = getStartInterval(start, interval);
		  				long irLength = getIRLength(start, end, interval);

                        
	  					String key = createKey(positionKey, 
	  										   startIntrvl, 
	  										   irLength, 
	  										   adjustDOW(dayIndex, startDOW), 
	  										   curDate);
	  					
	  					BFRequirementData bfData = (BFRequirementData)requirementDataMap.get(key);
	  					if (bfData != null){
	  		                String bestFitDetailKey = IntervalRequirementsManager.buildKey(ir.m_skdgrp.intValue(),ir.job.intValue(),null,ir.m_Activity.toString());
	  		      	  	    
	  						double workload = bfData.workload;
	  						long dayStartIntrvl = startIntrvl + (dayIndex * interval * 24);
	  						
//		                    for (int j = 0; j < irLength; j ++) {
//		                        if (dayStartIntrvl + j < ir.m_TotalNeeded.length) {
//		                            ir.m_TotalNeeded[(int)(dayStartIntrvl + j)] = (int)Math.round(workload);
//		                            //ir.m_bestFitDetails[(int)(dayStartIntrvl + j)] = ir.m_Key;
//		                            ir.m_bestFitDetails[(int)(dayStartIntrvl + j)] = bestFitDetailKey;
//		                        }
//			                } // for

	  						irLength += (int) dayStartIntrvl;
	  						if (irLength > ir.m_bestFitDetails.length) irLength = ir.m_bestFitDetails.length;
	  						//***bestFitInfoList.add(new BestFitInfo(ir, (int)dayStartIntrvl, (int)irLength-1, workload));
                            bestFitInfoList.add(new BestFitInfo(ir, (int)dayStartIntrvl, (int)irLength-1, workload, (int)workload, true));
                            
	  						requirementDataMap.remove(key);
	  					}
	  				}
      	  		} else {
	        		if (bestFitMap != null){
	        			bestFitMapping = (BestFitMapping)bestFitMap.get(csr.getSkdgrpId());
	        		}
        		
	        		if ((bestFitMap == null) || (bestFitMapping == null)){

	        			if (bestFitMap == null) bestFitMap = new HashMap();
        			
	        			SOHashtable htDeptsPerScheduleArea = new SOHashtable();
                        Map volumeBestFitMap = new HashMap();
	        			//***ScheduleIRCalculator.getScheduleAreas(soContext.getScheduleCE(), htDeptsPerScheduleArea, sched);  
	        			getScheduleAreas(soContext.getScheduleCE(), htDeptsPerScheduleArea, sched, volumeBestFitMap);  
                        
                        
	        			bestFitMapping = new BestFitMapping(conn, 
        								  				   csr.getSkdgrpId().intValue(),
        								  				   soContext.getIntervalRequirements().getRequirementsList(),
        								  				   soContext.getSchedule(),
        								  				   htDeptsPerScheduleArea,
                                                           volumeBestFitMap);
	        			bestFitMap.put(csr.getSkdgrpId(), bestFitMapping);
        			
	        			soContext.addCustomData(BEST_FIT_MAPPING, bestFitMap);
	        		} 
	        		
					// Zero out all workload numbers
	      	  		ir.resetTotalNeeded();
	                resetBestFitDetails((IntervalRequirementsManager.Record)ir);  
	        		
	        		List deptList = bestFitMapping.getDeptList();
	        		
	        		for (Iterator iter1 = deptList.iterator(); iter1.hasNext();){
	      	  			CorporateEntity curDept = (CorporateEntity)iter1.next();
	      	  	  		Integer curDeptSkdGrpId = (Integer)curDept.getSkdgrpData().getSkdgrpId(); 
	      	  	  	
	      	  	  		for (int dayIndex=0; dayIndex < 7; dayIndex++){
	      	  	  			List windowTimes = bestFitMapping.getWindowList(csr,curDeptSkdGrpId,dayIndex);
	  				
	      	  	  			if (windowTimes == null) continue;
  	  			
	                        int skill_id = csr.getStsklId() != null ? csr.getStsklId().intValue() : -1;
		      	            if (!sched.ishonourSkillsRuleOn()) {
		      	            	skill_id = -1;
		      	            }
      	  	  				String positionKey = createKey(csr.getSkdgrpId().intValue(),csr.getJobId().intValue(),skill_id,csr.getActId().intValue());
      	  	  				
	      	  	  			Iterator windowTimesIt = windowTimes.iterator();
	      	  	  			while (windowTimesIt.hasNext()){
	      	  	  				Map curWindow = (Map)windowTimesIt.next();
	      	  	  				Date curStart = (Date)curWindow.get(BestFitWindowUtil.WINDOW_START);
	      	  	  				Date curEnd = (Date)curWindow.get(BestFitWindowUtil.WINDOW_END);

	      	  	  				long startIntrvl = getStartInterval(curStart, interval);
	      	  	  				long irLength = getIRLength(curStart, curEnd, interval);
	      	  	  				String key = createKey(positionKey, 
	      	  	  									   startIntrvl, 
	      	  	  									   irLength, 
	      	  	  									   adjustDOW(dayIndex, startDOW),
	      	  	  									   new SODate(DateHelper.addDays(startOfWeek.toDateObject(),dayIndex)));

	    	  					BFRequirementData bfData = (BFRequirementData)requirementDataMap.get(key);
	    	  					if (bfData != null){
	    	  		                String bestFitDetailKey = IntervalRequirementsManager.buildKey(ir.m_skdgrp.intValue(),ir.job.intValue(),null,ir.m_Activity.toString());
	    	  		      	  	    
	    	  						double workload = bfData.workload;
	    	  						long dayStartIntrvl = startIntrvl + (dayIndex * interval * 24);
	    	  						
//	    		                    for (int j = 0; j < irLength; j ++) {
//	    		                        if (dayStartIntrvl + j < ir.m_TotalNeeded.length) {
//	    		                            ir.m_TotalNeeded[(int)(dayStartIntrvl + j)] = (int)Math.round(workload);
//	    		                            //ir.m_bestFitDetails[(int)(dayStartIntrvl + j)] = ir.m_Key;
//	    		                            ir.m_bestFitDetails[(int)(dayStartIntrvl + j)] = bestFitDetailKey;
//	    		                        }
//	    			                } // for
	    	  						irLength += (int) dayStartIntrvl;
	    	  						if (irLength > ir.m_bestFitDetails.length) irLength = ir.m_bestFitDetails.length;
	    	  						//***bestFitInfoList.add(new BestFitInfo(ir, (int)dayStartIntrvl, (int)irLength - 1, workload));
	    	  						bestFitInfoList.add(new BestFitInfo(ir, (int)dayStartIntrvl, (int)irLength - 1, workload, (int)workload, true));
                                    
                                    requirementDataMap.remove(key);
	    	  					}

	      	  	  			}
	      	  	  		}
	      	  		}
      	  		}
            }
    		
        	BestFitWindowUtil.modifyIRForBestFitLocations(soContext.getSchedule(), bestFitInfoList);
    	} catch (Exception e) {
   		  	throw new CalloutException ("modifyIRPostOptimizationPreAction error", e);
    	} finally {
    		soContext.removeCustomData(BEST_FIT_MAPPING);
    		soContext.removeCustomData(REQUIREMENT_DATA_MAP);

    	}   		
    	if (logger.isDebugEnabled()){ logger.debug("WOW -modifyIRPostOptimizationPreAction- return TRUE"); }
    	return result;
    }

    /**
     * Check to see if the staff requirement time is null, if so, use the start hours of operation time for the day
     * @param time
     * @param curDate
     * @param requirement
     * @return
     * @throws RetailException
     */
    private Date getStartTime(Date time, SODate curDate, CorporateEntityStaffRequirement requirement) throws RetailException {
        return getTime(time, curDate, requirement, true);
    }

    private Date getEndTime(Date time, SODate curDate, CorporateEntityStaffRequirement requirement) throws RetailException {
        return getTime(time, curDate, requirement, false);
    }
    
    /**
     * Check to see if the staff requirement time is null, if so, use the end hours of operation time for the day
     * @param time
     * @param curDate
     * @param requirement
     * @return
     * @throws RetailException
     */
    private Date getTime(Date time, SODate curDate, CorporateEntityStaffRequirement requirement, boolean isStart) throws RetailException {
    	Date result = time;
    	
        if (result == null) {
            CorporateEntity corpEntity = CorporateEntity.getCorporateEntity(requirement.getSkdgrpId());
            if (isStart){
            	result = corpEntity.getOpenTime(curDate).toDateObject();
            } else {
            	result = corpEntity.getCloseTime(curDate).toDateObject();
            }
        } 
        return result;

    }

    
    
    
    private long getStartInterval(Date start, int interval){
        Date midnight = DateHelper.convertStringToDate("00:00","HH:mm");
        long minFromMidnight = DateHelper.getMinutesBetween(start, midnight);
        return interval * (minFromMidnight / 60);

    }
    private long getStartInterval(Date start, IntervalType intervalType){
        Date midnight = DateHelper.convertStringToDate("00:00","HH:mm");
        long minFromMidnight = DateHelper.getMinutesBetween(start, midnight);
        int intervalLength = intervalType.getLengthInMinutes();
        return (minFromMidnight / intervalLength);
    }
    
    private long getIRLength(Date start, Date end, int interval){
		long minBetween = DateHelper.getMinutesBetween(end, start);
		
		if (minBetween <= 0){ 
			end = DateHelper.addDays(end,1);
			minBetween = DateHelper.getMinutesBetween(end, start); 
		} 				                
        
        return (minBetween * interval) / 60;
    }
    private long getIRLength(Date start, Date end, IntervalType intervalType){
        long minBetween = DateHelper.getMinutesBetween(end, start);
        
        if (minBetween <= 0){ 
            end = DateHelper.addDays(end,1);
            minBetween = DateHelper.getMinutesBetween(end, start); 
        }                               
        int intervalLength = intervalType.getLengthInMinutes();
        return (minBetween / intervalLength) ;
    }
    private String createKey(String positionKey, long startIntrvl, long irLength, int dayIndex, SODate irDate){
    	return positionKey+"-"+dayIndex+"-"+startIntrvl+"-"+irLength+"-"+irDate;
    }

	/**
	 * @param requirementDataMap
	 * @param irDate
	 * @param dayIndex
	 * @param curStart
	 * @param workload
	 * @param positionKey
	 * @param startIntrvl
	 * @param irLength
	 */
	private void createBFReqData(Map requirementDataMap, SODate irDate, int dayIndex, Date curStart, double workload, String positionKey, long startIntrvl, long irLength) {
		String key = createKey(positionKey, startIntrvl, irLength, dayIndex, irDate); 

		if (requirementDataMap.containsKey(key)){
			BFRequirementData bfData = (BFRequirementData)requirementDataMap.get(key);
			
			workload += bfData.workload;
			requirementDataMap.remove(key);
		}

		BFRequirementData newBFData = new BFRequirementData();
		newBFData.positionKey = positionKey;
		newBFData.irDate = irDate;
		newBFData.start = curStart;
		newBFData.irLength = irLength;
		newBFData.workload = workload;
		
		requirementDataMap.put(key, newBFData);
	}    
    
 
    

    
    
    
    /**
     * Adjust the dayIndex to be relative to the startOfWeek value
     * 
     * @param dayIndex
     * @param startOfWeek
     * @return
     */
    private int adjustDOW(int dayIndex, int startOfWeek){
    	
    	return adjustDOW(dayIndex, startOfWeek, true);
    }
    
    /**
     * Adjust the dayIndex to be relative to the startOfWeek value
     * 
     * @param dayIndex
     * @param startOfWeek
     * @param adjustRelativeDOWToSchedStart
     * @return
     */
    private int adjustDOW(int dayIndex, int startOfWeek, boolean adjustRelativeDOWToSchedStart){
    	int result = 0;
    	
    	if (adjustRelativeDOWToSchedStart){
    		result = dayIndex + startOfWeek;
    	
    		if (result >= 7) result -= 7;
    	} else { // adjust Actual DOW to SchedStart
    		result = dayIndex - startOfWeek;
        	
    		if (result < 0) result += 7;    		
    	}
    	
    	return result;
    }    
    
    /**
     * Return the workload for the specified day from the deptStfReq map
     * 
     * @param deptStfReq
     * @param skdgrp_id
     * @param csd_id
     * @param dayIndex
     * @return
     * @throws RetailException
     */
    private double getWorkloadByDay(BestFitMapping bestFitMapping, 
    							      Integer deptSkdgrp_id, 
    							      CorporateEntityStaffRequirement csr, 
    							      int dayIndex) throws RetailException{
    	double result = -1;
    	
    	Map deptStfReq = bestFitMapping.getWorkloadByStaffReq(csr);
    	
    	if (deptStfReq != null){
    		Map staffReq = (Map)deptStfReq.get((Integer)deptSkdgrp_id);
    		
    		if (staffReq != null){
                int sklId = csr.getStsklId() != null ? csr.getStsklId().intValue() : -1;            
                int actId = csr.getActId() != null ? csr.getActId().intValue() : -1;    		    
    			String jsaKey = createKey(-1, csr.getJobId().intValue(), sklId, actId);
    			double dailyWorkloads[] = (double[])staffReq.get(jsaKey);
    			
    			result = dailyWorkloads[dayIndex];
    		}
    	}
    	
    	return result;
    }

    /**
     * Return the total workload for all windows that are connected to the window that 
     * belong to the passed in details. 
     * 
     * If equals = true, only the workload with the exact same window start/end values will be totalled  
     * If equals = false, workload for adjacent/overlapping/equal windows will be totalled 
     * 
     * @param deptStfReq
     * @param deptSkdgrp_id
     * @param start
     * @param end
     * @param deptList
     * @param csr
     * @param windowDayIndex
     * @param wowBFWinUtil
     * @param conn
     * @param equals
     * @return
     * @throws RetailException
     */
    private double getTotalWorkloadByDay(BestFitMapping bestFitMapping, 
    									   Integer deptSkdgrp_id, 
    									   Date start,
    									   Date end,
    									   List deptList,
    									   CorporateEntityStaffRequirement csr, 
    									   int workloadDayIndex,
    									   int windowDayIndex,
    									   DBConnection conn,
    									   boolean equals) throws RetailException{
    	double result = -1;

  	  	Map deptStfReq = bestFitMapping.getWorkloadByStaffReq(csr);
  	  	
 	  	// Get the workload for deptSkdgrp_id
    	if (deptStfReq != null){
    		Map staffReq = (Map)deptStfReq.get((Integer)deptSkdgrp_id);

    		if (staffReq != null){
                int sklId = csr.getStsklId() != null ? csr.getStsklId().intValue() : -1;            
                int actId = csr.getActId() != null ? csr.getActId().intValue() : -1;                    		    
    			String jsaKey = createKey(-1,csr.getJobId().intValue(), sklId , actId);
    			double dailyWorkloads[] = (double[])staffReq.get(jsaKey);

    			result = dailyWorkloads[workloadDayIndex];
    		}
    	}    	
    	
    	// Loop through each dept and process all valid requirements that do not belong to deptSkdgrp_id
    	for (int i=0; i<deptList.size(); i++){      	  		
    		
    		CorporateEntity curDept = (CorporateEntity)deptList.get(i);
    		Integer curDeptSkdGrpId = (Integer)curDept.getSkdgrpData().getSkdgrpId();  	  	
    		
    		if (deptSkdgrp_id == curDeptSkdGrpId) continue;

    		List windowTimes = bestFitMapping.getWindowList(csr, curDeptSkdGrpId, windowDayIndex);
    		
    		if (windowTimes == null) continue;
    		
    		Iterator windowTimesIt = windowTimes.iterator();		                    
    	  	while (windowTimesIt.hasNext()){
    	  		Map curWindow = (Map)windowTimesIt.next();
    	  		Date curStart = (Date)curWindow.get(BestFitWindowUtil.WINDOW_START);
    	  		Date curEnd = (Date)curWindow.get(BestFitWindowUtil.WINDOW_END);
    	  		
    	  		if (!equals){
        	  		if (((curStart != null) && (DateHelper.isBetween(curStart, start, end) || DateHelper.equals(curStart, start))) ||
            	  			((curEnd != null) && (DateHelper.isBetween(curEnd, start, end) || DateHelper.equals(curEnd, end)))){
            	  			result += getWorkloadByDay(bestFitMapping,curDeptSkdGrpId,csr,workloadDayIndex);
            	  		}
    	  		} else {
        	  		if (((curStart != null) && DateHelper.equals(curStart, start)) &&
            	  			((curEnd != null) && DateHelper.equals(curEnd, end))){
            	  			result += getWorkloadByDay(bestFitMapping,curDeptSkdGrpId,csr,workloadDayIndex);
            	  		}
    	  			
    	  		}
    	  		
    	  	}
    	}


    	return result;
    }    
    
    /**
     * Create a key based on the skdgrp_id, job_id, skill_id, and activity_id
     * 
     * @param skdgrpId
     * @param jobId
     * @param skillId
     * @param actId
     * @return
     */
    private String createKey(int skdgrpId, int jobId, int skillId, int actId){
    	String delim = "*";
    	String skill = "NULL";
    	
    	if (skillId != -1) skill = String.valueOf(skillId);
    	
    	String result = jobId + delim + skill + delim + actId;
    	
    	if (skdgrpId != -1) result= skdgrpId + delim + result;
    	
    	return result;
    }
    
    /**
     * Reset the best fit details for the requirement
     * 
     * @param rec
     * @throws RetailException
     */
    private void resetBestFitDetails(IntervalRequirementsManager.Record rec) throws RetailException {
    	for (int i = 0; i < rec.m_bestFitDetails.length; i++){
            rec.m_bestFitDetails[i] = null;
        }
    }
    
    public class BFRequirementData
    {
        public SODate irDate;
        public Date start;
        public double workload;
        public String positionKey;
        public long irLength;
        
        public SODate getIrDate() {
            return irDate;
        }
        public void setIrDate(SODate irDate) {
            this.irDate = irDate;
        }
        public long getLength() {
            return irLength;
        }
        public void setLength(long irLength) {
            this.irLength = irLength;
        }
        public String getPositionKey() {
            return positionKey;
        }
        public void setPositionKey(String positionKey) {
            this.positionKey = positionKey;
        }
        public Date getStart() {
            return start;
        }
        public void setStart(Date start) {
            this.start = start;
        }
        public double getWorkload() {
            return workload;
        }
        public void setWorkload(double workload) {
            this.workload = workload;
        }
        
        
    }
    
    /**
     *  Helper method to return the list of scheduling areas for these
     *  schedule.  The <code>scheduleAreaToDriversMap</code> is populated
     *  with the list of drivers for each location of interest.
     */
    private List getScheduleAreas(CorporateEntity rootEntity, SOHashtable scheduleAreaToDriversMap, Schedule skd, Map volumeBestFitMap) throws RetailException
    {
        // first, build the list of locations for which we are scheduling,
        // depending on whether the user has selected to schedule for sublocations
        List locations = null;
        List allDrivers = new ArrayList();
        if (skd.getSkdTree() == 1)
        {
            // retrieve list of sub locations
            locations = rootEntity.getCorporateEntityList(CorporateEntity.ALL_SUBTREE, true);
            allDrivers = rootEntity.getCorporateEntityList(CorporateEntity.PAR_DRV, true);
            logger.debug("Using sublocations.....");
        }
        else
        {
            locations = new Vector();
            locations.add(rootEntity);
        }

        // second, separate the locations into two buckets:
        // - those locations that are schedule areas
        // - those locations that are departments
        Vector scheduleAreas = new Vector();
        for (int i = 0; i < locations.size(); i++)
        {
            CorporateEntity location = (CorporateEntity) locations.get(i);
            InternalType locationType = location.getInternalType();

            if (locationType.isScheduleArea())
            {
                scheduleAreas.addElement(location);
                List drivers = BestFitWindowUtil.getDriversOfDepartmentFromStfRequirement(location.getID().intValue(),  DBInterface.getCurrentConnection(),  allDrivers, volumeBestFitMap);
                scheduleAreaToDriversMap.put(location.getID(), drivers);
            }
            
        }

        return scheduleAreas;
    }


}
