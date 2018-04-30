package com.wbiag.app.modules.retailSchedule.services.volumeBestFit;

import java.util.*;

import org.apache.log4j.Logger;

import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ForecastDetail;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntityStaffRequirement;
import com.workbrain.app.modules.retailSchedule.model.IntervalRequirementsManager;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
//***import com.workbrain.app.modules.retailSchedule.services.WorkloadDistribution;
import com.workbrain.app.modules.retailSchedule.type.ForecastParameterType;
import com.workbrain.app.modules.retailSchedule.type.IntervalType;
import com.workbrain.app.modules.retailSchedule.type.TimeUnitType;
import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.app.modules.retailSchedule.utils.SODateInterval;
import com.workbrain.app.modules.retailSchedule.utils.SOHashtable;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.wbiag.app.modules.retailSchedule.services.volumeBestFit.model.*;

/**
 * Object that stores Woolworths best fit volume windows
 * 
 */
public class BestFitMapping {

    public static Logger logger = Logger.getLogger(BestFitMapping.class);
    private static final String NV_FCASTVOL_TRIGGER = "system/modules/scheduleOptimization/NV_FCASTVOL_TRIGGER";
	private static final int NON_VOLUME_DRIVER_TYPE = 1;
	
    protected DBConnection conn = null;
    protected int workloadSwitch = 9999; 
    protected int schedAreaSkdgrp_id = -1;
    protected List deptList;
    protected List bestFitReqList;
    protected boolean allNonVolumeBestFitReqs = true;
    protected Map windowMap;
    protected Map workloadMap;
    
    public BestFitMapping(DBConnection _conn, 
    						  int _schedAreaSkdgrp_id,
    						  List IRRecords, 
    						  Schedule sched,
    						  SOHashtable htDeptsPerScheduleArea,
                              Map volumeBestFitMap){
    	if (logger.isDebugEnabled()){ logger.debug("***** BestFitMapping Constructor *****"); }
    	
    	conn = _conn;
    	schedAreaSkdgrp_id = _schedAreaSkdgrp_id;
	  	workloadSwitch = Registry.getVarInt(NV_FCASTVOL_TRIGGER, 9999); 

	  	buildBestFitReqList(IRRecords);
	  	deptList = BestFitWindowUtil.getDepartments(htDeptsPerScheduleArea, new Integer(schedAreaSkdgrp_id));
    	buildWorkloadMap(htDeptsPerScheduleArea, sched, volumeBestFitMap);
    	buildWindowMap();
    }
    
    
    /* Get methods */
    public int getSchedAreaSkdgrpId(){
    	return schedAreaSkdgrp_id;
    }

    public List getBestFitReqList(){
    	return bestFitReqList;
    }

    public boolean getAllNonVolumeBestFitReqs(){
    	return allNonVolumeBestFitReqs;
    }
    
    public List getDeptList(){
    	return deptList;
    }
    
    public Map getWorkloadByStaffReq(CorporateEntityStaffRequirement csr){
    	return (Map)workloadMap.get(csr);
    }
    
    public List getWindowList(CorporateEntityStaffRequirement csr, Integer deptSkdgrpId, int dayIndex){
    	List result = null;
    	try {
	    	Map windowsPerDept = (Map)windowMap.get(csr);
	    	if (windowsPerDept == null) return result;
	    	
	    	Map windowsPerStaffReq = (Map)windowsPerDept.get(deptSkdgrpId);
	    	if (windowsPerStaffReq == null) return result;
	    	int sklId = csr.getStsklId() != null ? csr.getStsklId().intValue() : -1; 	    	
            int actId = csr.getActId() != null ? csr.getActId().intValue() : -1;	    	
	    	String key = csr.getJobId().intValue() + "-" + sklId + "-" + actId;
	    	Map windowsPerDay = (Map)windowsPerStaffReq.get(key);
	    	if (windowsPerDay == null) return result;
	    	
	    	result = (List)windowsPerDay.get(new Integer(dayIndex));
    	} catch (RetailException e){
    		logger.error("BestFitMapping.getWindowList", e);
    	}
    	return result;
    }
    
    public Integer doesWindowExist(CorporateEntityStaffRequirement csr, Integer deptSkdgrpId, int dayIndex, Date start, Date end){
    	Integer result = null;
    	try {
	    	Map windowsPerDept = (Map)windowMap.get(csr);
	    	
	    	for (int i=0; i<deptList.size(); i++){
  	  	  		CorporateEntity curDept = (CorporateEntity)deptList.get(i);
  	  	  		Integer curDeptSkdGrpId = (Integer)curDept.getSkdgrpData().getSkdgrpId(); 
  	  	  	
  	  	  		if (!curDeptSkdGrpId.equals(deptSkdgrpId)){
  	  	  		   	Map windowsPerStaffReq = (Map)windowsPerDept.get(curDeptSkdGrpId);
  	  	  		   	int sklId = csr.getStsklId() != null ? csr.getStsklId().intValue() : -1;            
  	  	  		   	int actId = csr.getActId() != null ? csr.getActId().intValue() : -1;              	  	  		   	
  	  	  		   	String key = csr.getJobId().intValue() + "-" + sklId + "-" + actId;
  	  	  		   	Map windowsPerDay = (Map)windowsPerStaffReq.get(key);
  	  	  		   	List curWindowList = (List)windowsPerDay.get(new Integer(dayIndex));
  	  	  		   	
  	  	  	  		if (curWindowList == null) continue;
  	  	  	  		
  	  	  			Iterator windowTimesIt = curWindowList.iterator();
  	  	  			while (windowTimesIt.hasNext()){
  	  	  				Map curWindow = (Map)windowTimesIt.next();
  	  	  				Date curStart = (Date)curWindow.get(BestFitWindowUtil.WINDOW_START);
  	  	  				Date curEnd = (Date)curWindow.get(BestFitWindowUtil.WINDOW_END);
  	  	  				
  	  	  				if ((curStart.equals(start)) && (curEnd.equals(end))){
  	  	  					result = curDeptSkdGrpId;
  	  	  					break;
  	  	  				}
  	  	  			}
  	  	  		}
  	  	  		if (result != null) break;
	    	}
  	  	  		   	
    	} catch (RetailException e){
    		logger.error("BestFitMapping.getWindowList", e);
    	}
    	return result;    	
    }
    
    private void buildBestFitReqList(List IRRecords){
    	
    	try{
    		allNonVolumeBestFitReqs = true;
    		
    		if (bestFitReqList == null) bestFitReqList = new ArrayList();
    		
	    	for (Iterator iter = IRRecords.iterator(); iter.hasNext();) {
	            IntervalRequirementsManager.Record ir = (IntervalRequirementsManager.Record) iter.next();
	            CorporateEntityStaffRequirement csr = ir.m_staffReq;
	            
	  	  		if (csr == null){
	  	  			continue;
	  	  		}
	            
	            if(ir.m_isBestFit){
	            	bestFitReqList.add(ir);

		  	  		if (!((csr.getCsdNonvlmFlag().intValue() == NON_VOLUME_DRIVER_TYPE) && 
		  	  	  			(csr.getCsdWrkldHrs().floatValue() == workloadSwitch))){
		  	  			allNonVolumeBestFitReqs = false;
		  	  		}	            
	            } else {
	            	allNonVolumeBestFitReqs = false;
	            }
	        }
    	} catch (RetailException e){
    		logger.error("BestFitMapping.buildBestFitReqList", e);
    	}
    }
    
    private void buildWorkloadMap(SOHashtable htDeptsPerScheduleArea, Schedule sched, Map volumeBestFitMap){
    	try{
    		if (workloadMap == null) workloadMap = new HashMap();
    		
	    	for (Iterator iter = bestFitReqList.iterator(); iter.hasNext();){
	            IntervalRequirementsManager.Record ir = (IntervalRequirementsManager.Record) iter.next();
	            CorporateEntityStaffRequirement csr = ir.m_staffReq;
	
	    		Map deptStfReq = new HashMap();
	  	  		if ((csr.getCsdNonvlmFlag().intValue() == NON_VOLUME_DRIVER_TYPE) && 
	  	  				(csr.getCsdWrkldHrs().floatValue() == workloadSwitch)){
	  	  				//***WorkloadDistribution.getNVFcastVol(csr,htDeptsPerScheduleArea,sched,deptStfReq);
	  	  		        getNVFcastVol(csr,htDeptsPerScheduleArea,sched,deptStfReq, volumeBestFitMap);
                
                }
	    		
	    		workloadMap.put(csr, deptStfReq);
	    	}
    	} catch (RetailException e){
    		logger.error("BestFitMapping.buildWorkloadMap", e);
    	}
    }
    
    private float [] getNVFcastVol(CorporateEntityStaffRequirement skdAreaStaffReq, SOHashtable htDepsPerScheduleArea, Schedule skd, Map depStfReqVol, Map volumeBestFitMap) throws RetailException {

        List lstDepartments= (List)htDepsPerScheduleArea.get(skdAreaStaffReq.getSkdgrpId());
        
        Integer nonVolumeBFCsdId = skdAreaStaffReq.getCsdId();
        
        float[] skdAreaReqDlyWorkload = new float[skd.getLengthInDays()];
        SODate m_scheduleStart = new SODate(skd.getSkdFromDate());
        SODate m_scheduleEnd = new SODate(skd.getSkdToDate());
        Integer skdAreaStfReqDistId = skdAreaStaffReq.getDistId();
        IntervalType intervalType = IntervalType.get(skd.getCorporateEntity().getSkdgrpData().getSkdgrpFcastInt());
        
        
        List lstIds = new ArrayList();
        for (int d = 0; d < lstDepartments.size(); d++){
            lstIds.add(((CorporateEntity)lstDepartments.get(d)).getID());
        }
        
        //now, this map corresponds to all the days connected to the one non-volume-driven 
        //staff requirement.  
        Map oneNonVolumeCSD = (Map) volumeBestFitMap.get(nonVolumeBFCsdId);
        
        if (oneNonVolumeCSD!= null) {
            
        
            //... for each department
            for (int d = 0; d < lstDepartments.size(); d++){
            
                CorporateEntity department = (CorporateEntity) lstDepartments.get(d);
            
                Map oneDriverBFData = (Map) oneNonVolumeCSD.get(department.getSkdgrpData().getSkdgrpId());
            
                // ... for each staffing requirement in the department
                Map stfReqVol = new HashMap();
                double[] deptDailyWorkload = null;
            
                deptDailyWorkload = new double[skd.getLengthInDays()];
            
                if (oneDriverBFData != null) {
                    
                
                    SODateInterval oRange = new SODateInterval(m_scheduleStart, m_scheduleEnd);
                    double deptFcastVolumePerDay[] = new double[skd.getLengthInDays()];
                    // get forecast details
                    List lstTotal;
                    if (department.isActivityBasedSchedulingLocation()) {
                        List lstLocations = department.getCorporateEntityList(CorporateEntity.ALL_SUBTREE, true);
                        lstTotal = new ArrayList();
                        for (int i = 0; i < lstLocations.size(); i++){
                            CorporateEntity oTmpCE = (CorporateEntity)lstLocations.get(i);
                        
                            if (oTmpCE.getSkdgrpData().getSkdgrpSalesBased() == ScheduleGroupData.SALES_BASED_TRUE){
                                lstTotal.addAll(oTmpCE.getForecastDetailsListConsideringInheritForecast(oRange));
                            }
                        }
                    }else{    
                        lstTotal = department.getForecastDetailsListConsideringInheritForecast(oRange);
                    }
                
                    // pick up volume only for best fit days
                    ForecastDetail fcastDetail;
                    for (int i = 0; i < lstTotal.size(); i++){
                        fcastDetail = (ForecastDetail)lstTotal.get(i);
                        Integer fcastDistId = fcastDetail.getDistId();
                        if(fcastDistId.equals(skdAreaStfReqDistId)) { // match the daily forecast dist id  with sa req dist id
                            int iIndex = m_scheduleStart.getDaysDiff(fcastDetail.getFcastDate()) - 1;
                            deptFcastVolumePerDay[iIndex] += fcastDetail.getAdjustedVolume().doubleValue();
                        }
                    }
                     
                    SODate curDate = (SODate)m_scheduleStart.clone();
                    // convert daily volume to workload
                    for (int i=0;i<deptFcastVolumePerDay.length;i++){
                         
                        VolumeBestFitData VolumeBestFitData = (VolumeBestFitData) oneDriverBFData.get(new Integer(i));
                    
                        double storeHours=department.getNumberOfHoursIsOpen(curDate);
                        double deptVolumeProductivityPerDay = 1;
                        
                        if (VolumeBestFitData != null) {
                            deptVolumeProductivityPerDay = VolumeBestFitData.getWrkldStdvolHour()*storeHours;
                            double dailyWorkload = deptFcastVolumePerDay[i]/deptVolumeProductivityPerDay;
                            double numIntervalsPerDay = (storeHours*60d)/intervalType.getLengthInMinutes();
                            deptDailyWorkload[i] = Math.ceil(dailyWorkload*numIntervalsPerDay);
                        } else {
                            deptDailyWorkload[i] = 0;
                        }

                        
                        curDate.add(Calendar.DATE,1);
                        
                        
                    }

                    // Add the department workload to the Schedule Area's total best fit workload
                    for (int i=0;i<deptDailyWorkload.length;i++){
                        skdAreaReqDlyWorkload[i] += deptDailyWorkload[i];
                    }

                
                    //     store vol into staff req map
                    stfReqVol.put(skdAreaStaffReq.getJobId() + "*" + skdAreaStaffReq.getStsklId() + "*" + skdAreaStaffReq.getActId(), deptDailyWorkload);
                }
            
            
                // store staff req vol into dep map
                depStfReqVol.put(department.getID(), stfReqVol);
            }
            
        }
        return skdAreaReqDlyWorkload;
    }
    
    
    private void buildWindowMap(){
    	try{
    		if (windowMap == null) windowMap = new HashMap();
    		
	    	for (Iterator iter = bestFitReqList.iterator(); iter.hasNext();){
	        	IntervalRequirementsManager.Record ir = (IntervalRequirementsManager.Record) iter.next();
	            CorporateEntityStaffRequirement csr = ir.m_staffReq;
	  	  		
	  	  		int job_id = csr.getJobId().intValue();
	  	  		// *** support null skills
	  	  		Integer skill_id = csr.getStsklId();
	  	  		if (skill_id == null) {
	  	  		    skill_id = new Integer(-1);
	  	  		}
	  	  		Integer act_id = csr.getActId();                
                if (act_id == null) {
                    act_id = new Integer(-1);
                }	  	  	        	
	  	  		Map windowsPerDeptMap = new HashMap();
	  	  		
	  	  		
	  	  		if ((csr.getCsdNonvlmFlag().intValue() == NON_VOLUME_DRIVER_TYPE) && 
	  	  				(csr.getCsdWrkldHrs().floatValue() == workloadSwitch)){
	  	  		
			  		// Loop through each dept and process all valid requirements
			  		for (int i=0; i<deptList.size(); i++){      	  		
			  		
			  		    List csdIdList = new ArrayList();
			  	  		CorporateEntity curDept = (CorporateEntity)deptList.get(i);
			  	  		Integer curDeptSkdGrpId = (Integer)curDept.getSkdgrpData().getSkdgrpId();  	
                        
                        //these are the non-volume-driven CSD referenced by the driver in wbaig_volume_bestfit table
			  			Vector allCSD_IDs = BestFitWindowUtil.getClientStfDefId(curDeptSkdGrpId.intValue(),
			  					                                           job_id, 
			  					                                           skill_id.intValue(), 
			  					                                           act_id.intValue(),
			  					                                           conn);
			
			  			String key = job_id + "-" + skill_id.intValue() + "-" + act_id.intValue();
			  			
			  			Map windowsPerStaffReqMap = new HashMap();
			  			
			  			Iterator allCSD_IDit = allCSD_IDs.iterator();
			  			while(allCSD_IDit.hasNext()){
			
			  				Integer curCSD_ID = (Integer)allCSD_IDit.next();
			  				
			  				if (!csdIdList.contains(curCSD_ID)){
			  				
				  				Map windowsPerDayMap = new HashMap();
				  				
				  				// Process each day at a time
				      	  		for (int dayIndex=0; dayIndex<7; dayIndex++){
				
				      	  			List windowsList = BestFitWindowUtil.getBFWindowByDay(curCSD_ID.intValue(), curDeptSkdGrpId.intValue(), dayIndex, conn);
					  	  			
					  	  			if (windowsList.size() > 0){
					  	  				windowsPerDayMap.put(new Integer(dayIndex), windowsList);
					  	  			}
					      	  	}
				      	  	
				      	  		csdIdList.add(curCSD_ID);
				      	  		windowsPerStaffReqMap.put(key, windowsPerDayMap);
			  				}
			  			}
			  			windowsPerDeptMap.put(curDeptSkdGrpId, windowsPerStaffReqMap);	
			  		}
	  	  		} 
	  	  		
		  		windowMap.put(csr, windowsPerDeptMap);
	        }
    	} catch (RetailException e){
    		logger.error("BestFitMapping.buildWindowMap", e);
    	}
    }
    

}