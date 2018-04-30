package com.wbiag.app.ta.quickrules;

import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.util.*;
import java.util.*;

import org.apache.log4j.Logger;

/**
 *
 */

public class GracesExtendedRule extends Rule {
	
    private static Logger logger = Logger.getLogger(GracesExtendedRule.class);

    public final static String PARAM_ELIGIBLE_TIMECODES = "EligibleTimeCodes";
    public final static String PARAM_GRACE_BEFORE_SHIFT_STARTS = "GraceBeforeShiftStarts";
    public final static String PARAM_GRACE_BEFORE_SHIFT_STARTS_2 = "GraceBeforeShiftStarts2";
    public final static String PARAM_GRACE_AFTER_SHIFT_STARTS = "GraceAfterShiftStarts";
    public final static String PARAM_GRACE_BEFORE_SHIFT_ENDS = "GraceBeforeShiftEnds";
    public final static String PARAM_GRACE_AFTER_SHIFT_ENDS = "GraceAfterShiftEnds";
    public final static String PARAM_TIMECODE_BEFORE_SHIFT_STARTS = "TimeCodeBeforeShiftStarts";
    public final static String PARAM_TIMECODE_BEFORE_SHIFT_STARTS_2 = "TimeCodeBeforeShiftStarts2";
    public final static String PARAM_TIMECODE_AFTER_SHIFT_STARTS = "TimeCodeAfterShiftStarts";
    public final static String PARAM_TIMECODE_BEFORE_SHIFT_ENDS = "TimeCodeBeforeShiftEnds";
    public final static String PARAM_TIMECODE_AFTER_SHIFT_ENDS = "TimeCodeAfterShiftEnds";
    public final static String PARAM_APPLY_TO_ALL_SHIFTS = "ApplyToAllShifts";
    public final static String PARAM_ALLOW_PARTIAL_GRACES = "AllowPartialGraces";

    public List getParameterInfo( DBConnection conn ) {
        List result = new ArrayList();
        result.add(new RuleParameterInfo(PARAM_ELIGIBLE_TIMECODES, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_GRACE_BEFORE_SHIFT_STARTS, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_GRACE_BEFORE_SHIFT_STARTS_2, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_GRACE_AFTER_SHIFT_STARTS, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_GRACE_BEFORE_SHIFT_ENDS, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_GRACE_AFTER_SHIFT_ENDS, RuleParameterInfo.INT_TYPE));
        result.add(new RuleParameterInfo(PARAM_TIMECODE_BEFORE_SHIFT_STARTS, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_TIMECODE_BEFORE_SHIFT_STARTS_2, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_TIMECODE_AFTER_SHIFT_STARTS, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_TIMECODE_BEFORE_SHIFT_ENDS, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_TIMECODE_AFTER_SHIFT_ENDS, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_APPLY_TO_ALL_SHIFTS, RuleParameterInfo.STRING_TYPE));
        result.add(new RuleParameterInfo(PARAM_ALLOW_PARTIAL_GRACES, RuleParameterInfo.STRING_TYPE));
        return result;
    }

    public String getComponentName() {
        return "WBIAG: Graces Extended Rule";
    }

    public void execute(WBData wbData, Parameters parameters) throws Exception {
        
        // *** Parameters ***
        String eligibleCodes = parameters.getParameter(PARAM_ELIGIBLE_TIMECODES, "WRK");
        int startShiftPreMin = parameters.getIntegerParameter(PARAM_GRACE_BEFORE_SHIFT_STARTS, 0);
        int startShiftPreMin2 = parameters.getIntegerParameter(PARAM_GRACE_BEFORE_SHIFT_STARTS_2, 0);
        int startShiftPostMin = parameters.getIntegerParameter(PARAM_GRACE_AFTER_SHIFT_STARTS, 0);
        int endShiftPreMin = parameters.getIntegerParameter(PARAM_GRACE_BEFORE_SHIFT_ENDS, 0);
        int endShiftPostMin = parameters.getIntegerParameter(PARAM_GRACE_AFTER_SHIFT_ENDS, 0);
        String startShiftPreCode = parameters.getParameter(PARAM_TIMECODE_BEFORE_SHIFT_STARTS);
        String startShiftPreCode2 = parameters.getParameter(PARAM_TIMECODE_BEFORE_SHIFT_STARTS_2);
        String startShiftPostCode = parameters.getParameter(PARAM_TIMECODE_AFTER_SHIFT_STARTS);
        String endShiftPreCode = parameters.getParameter(PARAM_TIMECODE_BEFORE_SHIFT_ENDS);
        String endShiftPostCode = parameters.getParameter(PARAM_TIMECODE_AFTER_SHIFT_ENDS);
        boolean applyToAllShifts = Boolean.valueOf(parameters.getParameter(PARAM_APPLY_TO_ALL_SHIFTS,"false")).booleanValue();
        boolean allowPartialGraces = Boolean.valueOf(parameters.getParameter(PARAM_ALLOW_PARTIAL_GRACES,"false")).booleanValue();

        // ** When the employee was supposed to clock In / Out ?
        if (!wbData.getEmployeeScheduleData().isEmployeeScheduledActual()) {
            return;
        }

        // Validate the parameters.
        if (startShiftPreMin2 != 0 && startShiftPreMin2 <= startShiftPreMin) {
        	throw new IllegalArgumentException(PARAM_GRACE_BEFORE_SHIFT_STARTS_2 
        										+ " must be greater than " + PARAM_GRACE_BEFORE_SHIFT_STARTS);
        }
        if (startShiftPreMin == 0 && startShiftPreMin2 != 0) {
        	throw new IllegalArgumentException("Cannot specify parameter: " + PARAM_GRACE_BEFORE_SHIFT_STARTS_2 
        										+ " without parameter: " + PARAM_GRACE_BEFORE_SHIFT_STARTS);
        }
        
        final Integer empId = new Integer(wbData.getEmpId());
        
        for (int k=0; k < WBData.MAXIMUM_SHIFT_COUNT ; k++) {
        
        	// Check if only applies to first shift.
        	if (k > 0 && applyToAllShifts == false) {
                break;
            }

        	// Check if there is a shift at this index.
            if (!wbData.getEmployeeScheduleData().retrieveShiftScheduled(k)) {
                continue;
            }

            Date scheduledStart = wbData.getEmployeeScheduleData().retrieveShiftStartTime(k);
            Date scheduledEnd = wbData.getEmployeeScheduleData().retrieveShiftEndTime(k);

            if (allowPartialGraces) {
                wbData.getRuleData().splitAt(scheduledStart);
                wbData.getRuleData().splitAt(scheduledEnd);
            }

            // ** Finding Records with minStartTime and maxEndTime
            long minBeforeShiftTime = Long.MAX_VALUE;
            int minBeforeShiftIndex = -1;
            long minBeforeShiftTime2 = Long.MAX_VALUE;
            int minBeforeShiftIndex2 = -1;
            Date lowerBound = DateHelper.addMinutes(scheduledStart, -startShiftPreMin) ;
            Date lowerBound2 = DateHelper.addMinutes(scheduledStart, -startShiftPreMin2) ;
            Date upperBound = DateHelper.addMinutes(scheduledEnd, endShiftPostMin) ;
            WorkDetailData wd = null;
            EmployeeIdStartEndDateSet eligibleTimeSet =  new EmployeeIdStartEndDateSet();
            
            //Neshan Kumar - Grace time codes are not correctly applied when more than one shift exists
            //Fixed for SkyWest #1970026
            eligibleTimeSet.setLeastDivisibleUnit(Calendar.MINUTE);
            
            wbData.getRuleData().splitAt(lowerBound);
            
            for (int i = 0, j = wbData.getRuleData().getWorkDetailCount(); i < j; i++) {
                
            	wd = wbData.getRuleData().getWorkDetail(i);
                
            	if ( RuleHelper.isCodeInList(eligibleCodes, wd.getWrkdTcodeName()) ) {
                
            		eligibleTimeSet.add(empId, wd.getWrkdStartTime(), wd.getWrkdEndTime());
                    
            		// Is the work detail between grace1 and shift start.
            		if (wd.getWrkdStartTime().compareTo(lowerBound) >= 0
                            && wd.getWrkdStartTime().compareTo(upperBound) < 0
							&& wd.getWrkdStartTime().getTime() < minBeforeShiftTime) {
                    
            			minBeforeShiftTime = wd.getWrkdStartTime().getTime();
                        minBeforeShiftIndex = i;
                    
                    // Is the work detail between grace2 and grace 1.
            		} else if (wd.getWrkdStartTime().compareTo(lowerBound2) >= 0
                            	&& wd.getWrkdStartTime().compareTo(lowerBound) < 0
								&& wd.getWrkdStartTime().getTime() < minBeforeShiftTime2) {
                    	
            			minBeforeShiftTime2 = wd.getWrkdStartTime().getTime();
                        minBeforeShiftIndex2 = i;
                    }
                }
            }

            // *** Shift Start ***
            
            // Before shift start 2.
            if (minBeforeShiftIndex2 > -1) {
                
            	wd = wbData.getRuleData().getWorkDetail(minBeforeShiftIndex2);
            	
                // Grace Before Shift Starts 2
                if (wd.getWrkdStartTime().before(scheduledStart) 
                	&& wd.getWrkdStartTime().getTime() >= lowerBound2.getTime() 
					&& !eligibleTimeSet.includes(empId, new Date(DateHelper.addMinutes(lowerBound2,-1).getTime()), scheduledStart)) {
                    
                	if (allowPartialGraces) {
						wbData.getRuleData().getWorkDetails().setWorkDetailTcodeId(
						                                      wbData.getRuleData().getCodeMapper().getTimeCodeByName(startShiftPreCode2).getTcodeId(),
						                                      wd.getWrkdStartTime(),
						                                      lowerBound,
						                                      eligibleCodes,
						                                      false,
						                                      true,
						                                      true);
                    } else {
                        wbData.insertWorkDetail(wd, startShiftPreCode2, wd.getWrkdStartTime().getTime(), lowerBound.getTime());
                    }
                }
            }
            
            Date boundToCheck = DateHelper.min(lowerBound, lowerBound2);
            
            // Before shift start 1.
            if (minBeforeShiftIndex > -1) {
                wd = wbData.getRuleData().getWorkDetail(minBeforeShiftIndex);
                
                // Grace Before Shift Starts
                if (wd.getWrkdStartTime().before(scheduledStart) 
                	&& wd.getWrkdStartTime().getTime() >= (scheduledStart.getTime() - startShiftPreMin * DateHelper.MINUTE_MILLISECODS) 
					&& !eligibleTimeSet.includes(empId, new Date(DateHelper.addMinutes(boundToCheck,-1).getTime()), scheduledStart)) {
                    
                	if (allowPartialGraces) {
						wbData.getRuleData().getWorkDetails().setWorkDetailTcodeId(
						                                      wbData.getRuleData().getCodeMapper().getTimeCodeByName(startShiftPreCode).getTcodeId(),
						                                      wd.getWrkdStartTime(),
						                                      scheduledStart,
						                                      eligibleCodes,
						                                      false,
						                                      true,
						                                      true);
                    } else {
                        wbData.insertWorkDetail(wd, startShiftPreCode, wd.getWrkdStartTime().getTime(), scheduledStart.getTime());
                    }
                    
                // Grace After Shift Starts
                } else if (wd.getWrkdStartTime().compareTo(scheduledStart) > 0 &&
                           wd.getWrkdStartTime().getTime() <= (scheduledStart.getTime() + startShiftPostMin * DateHelper.MINUTE_MILLISECODS)) {
                    
                	if (allowPartialGraces) {
						wbData.getRuleData().getWorkDetails().setWorkDetailTcodeId(
															  wbData.getRuleData().getCodeMapper().getTimeCodeByName(startShiftPostCode).getTcodeId(),
						                                      scheduledStart,
						                                      wd.getWrkdStartTime(),
															  eligibleCodes,
															  false,
															  true,
															  true);
                    } else {
                        wbData.insertWorkDetail(wd, startShiftPostCode, scheduledStart.getTime(), wd.getWrkdStartTime().getTime());
                    }
                }
            }
            
            
            // *** Shift End ***
            long maxTime = Long.MIN_VALUE;
            int maxI = -1;
            for (int i = 0, j = wbData.getRuleData().getWorkDetailCount(); i < j; i++) {
                wd = wbData.getRuleData().getWorkDetail(i);
                if (wd.getWrkdEndTime().getTime() > maxTime
                        && RuleHelper.isCodeInList(eligibleCodes, wd.getWrkdTcodeName())
                        && wd.getWrkdEndTime().compareTo(lowerBound) > 0
                        && wd.getWrkdEndTime().compareTo(upperBound) <= 0) {
                    maxTime = wd.getWrkdEndTime().getTime();
                    maxI = i;
                }
            }
            
            if (maxI > -1) {
                wd = wbData.getRuleData().getWorkDetail(maxI);
                
                // Before Shift End.
                if (wd.getWrkdEndTime().compareTo(scheduledEnd) < 0 &&
                    wd.getWrkdEndTime().getTime() >= (scheduledEnd.getTime() - endShiftPreMin * DateHelper.MINUTE_MILLISECODS)) {
                    if (allowPartialGraces) {
						wbData.getRuleData().getWorkDetails().setWorkDetailTcodeId(
															  wbData.getRuleData().getCodeMapper().getTimeCodeByName(endShiftPreCode).getTcodeId(),
						                                      wd.getWrkdEndTime(),
						                                      scheduledEnd,
															  eligibleCodes,
															  false,
															  true,
															  true);
                    } else {
                        wbData.insertWorkDetail(wd, endShiftPreCode, wd.getWrkdEndTime().getTime(), scheduledEnd.getTime());
                    }
                    
                // After Shift End.
                } else if (wd.getWrkdEndTime().after(scheduledEnd) &&
                           wd.getWrkdEndTime().getTime() <= (scheduledEnd.getTime() + endShiftPostMin * DateHelper.MINUTE_MILLISECODS) &&
                           !eligibleTimeSet.includes(empId, scheduledEnd, new Date(DateHelper.addMinutes(scheduledEnd,endShiftPostMin+1).getTime()))) {
                    if (allowPartialGraces) {
						wbData.getRuleData().getWorkDetails().setWorkDetailTcodeId(
															  wbData.getRuleData().getCodeMapper().getTimeCodeByName(endShiftPostCode).getTcodeId(),
						                                      scheduledEnd,
						                                      wd.getWrkdEndTime(),
															  eligibleCodes,
															  false,
															  true,
															  true);
                    } else {
                        wbData.insertWorkDetail(wd, endShiftPostCode, scheduledEnd.getTime(), wd.getWrkdEndTime().getTime());
                    }
                }
            }
        }
        
        // *** Sort them at the end ***
        wbData.getRuleData().getWorkDetails().sort();
    }

}
