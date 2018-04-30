package com.wbiag.app.ta.quickrules;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.workbrain.app.ta.model.ShiftBreakData;
import com.workbrain.app.ta.model.ShiftWithBreaks;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.model.WorkDetailList;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/**
 * Title:			Variable Break Rule
 * Description:		Similiar to IAG's ArrangeBreakMultipleRule, but can handle variable break and break windows
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Jan 18, 2006
 * @author         	Kevin Tsoi
 */
public class VariableBreakRule extends Rule
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(VariableBreakRule.class);

    public final static String PARAM_BREAK_TIMECODE = "BreakTimeCode";
    public final static String PARAM_BREAK_ARRIVEEARLY_TIMECODE = "BreakArriveEarlyTimeCode";
    public final static String PARAM_BREAK_ARRIVELATE_TIMECODE = "BreakArriveLateTimeCode";
    public final static String PARAM_BREAK_EXTRA_ARRIVEEARLY_TIMECODE = "BreakExtraArriveEarlyTimeCode";
    public final static String PARAM_BREAK_EXTRA_ARRIVEEARLY_MINUTES = "BreakExtraArriveEarlyMinutes";
    public final static String PARAM_BREAK_EXTRA_ARRIVELATE_TIMECODE = "BreakExtraArriveLateTimeCode";
    public final static String PARAM_BREAK_EXTRA_ARRIVELATE_MINUTES = "BreakExtraArriveLateMinutes";
    public final static String PARAM_UAT_TIMECODE = "UATTimeCode";
    public final static String PARAM_APPLY_TO_ALL_SHIFTS = "ApplyToAllShifts";





    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.RuleComponent#getParameterInfo(com.workbrain.sql.DBConnection)
     */
    public List getParameterInfo(DBConnection conn)
    {
        List result = new ArrayList();

        result.add(new RuleParameterInfo(PARAM_BREAK_TIMECODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_BREAK_ARRIVEEARLY_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_ARRIVELATE_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_EXTRA_ARRIVEEARLY_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_EXTRA_ARRIVEEARLY_MINUTES, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_EXTRA_ARRIVELATE_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_BREAK_EXTRA_ARRIVELATE_MINUTES, RuleParameterInfo.INT_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_UAT_TIMECODE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_APPLY_TO_ALL_SHIFTS, RuleParameterInfo.STRING_TYPE));

        return result;
    }

    /**
     * set parameters to member variables
     */
    public ParametersResolved getParameters(Parameters parameters)
    {
        ParametersResolved ret = new ParametersResolved();
        ret.breakTimeCode = parameters.getParameter(PARAM_BREAK_TIMECODE);
        ret.breakArriveEarlyTimeCode = parameters.getParameter(PARAM_BREAK_ARRIVEEARLY_TIMECODE);
        ret.breakArriveLateTimeCode = parameters.getParameter(PARAM_BREAK_ARRIVELATE_TIMECODE);
        ret.breakExtraArriveEarlyTimeCode = parameters.getParameter(PARAM_BREAK_EXTRA_ARRIVEEARLY_TIMECODE);
        ret.breakExtraArriveEarlyMinutes = parameters.getIntegerParameter(PARAM_BREAK_EXTRA_ARRIVEEARLY_MINUTES , 0);
        ret.breakExtraArriveLateTimeCode = parameters.getParameter(PARAM_BREAK_EXTRA_ARRIVELATE_TIMECODE);
        ret.breakExtraArriveLateMinutes = parameters.getIntegerParameter(PARAM_BREAK_EXTRA_ARRIVELATE_MINUTES , 0);
        ret.applyToAllShifts = Boolean.valueOf(parameters.getParameter(PARAM_APPLY_TO_ALL_SHIFTS,"false")).booleanValue();
        ret.uatTcodeName = parameters.getParameter(PARAM_UAT_TIMECODE, "UAT");
        return ret;
    }

    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.Rule#execute(com.workbrain.app.ta.ruleengine.WBData, com.workbrain.app.ta.ruleengine.Parameters)
     */
    public void execute(WBData wbData, Parameters parameters)
    	throws Exception
    {

        //set parameters to member variables
        ParametersResolved pars = getParameters(parameters);

        List swb = wbData.getShiftsWithBreaks();

        //iterate through list
        for (int k=0 , l=swb.size() ; k < l; k++)
        {
            if ( k > 0 && pars.applyToAllShifts == false )
            {
                break;
            }
            if (wbData.getShiftWithBreaks(k).isScheduledActual())
            {
                applyToSchedule(k, pars, wbData );
            }
        }
    }

    /**
     * Apply the appropriate overrides to schedule
     *
     * @param shiftIndex
     * @throws Exception
     */
    protected void applyToSchedule(int shiftIndex, ParametersResolved pars, WBData wbData)
		throws Exception
	{
	    ShiftWithBreaks shiftWithBreaks = null;
	    ShiftBreakData currentBreak = null;
	    List breaksList = null;
	    WorkDetailList workDetailList = null;
	    WorkDetailData workDetail = null;
	    Iterator itWDList = null;
	    Date scheduleStart = null;
	    Date scheduleEnd = null;
	    Date wdStart = null;
	    Date wdEnd = null;
	    String tcodeName = null;
	    String htypeName = null;
	    boolean detailAfterBreak = false;
	    boolean detailAfterALate = false;
	    boolean detailAfterAEarly = false;
	    int usedBreakMinutes = 0;
	    int unusedMinutes = 0;
	    int WindowMinutesRemaining = 0;
	    int breakMinutes = 0;
	    int detailMinutes = 0;
	    int arriveEarlyMinutes = 0;

	    //calculate breakExtraArriveEarlyMinutes
	    if (!StringHelper.isEmpty(pars.breakExtraArriveEarlyTimeCode))
	    {
	        if (pars.breakExtraArriveEarlyMinutes < 0)
	        {
	            int skdMins = wbData.getShiftWithBreaks(shiftIndex).
	            retrieveScheduledMinutesPaid(wbData.getCodeMapper());
	            pars.breakExtraArriveEarlyMinutes = (int) (skdMins *
	                    ( -1.0 * pars.breakExtraArriveEarlyMinutes / 100));
	        }
	    }

	    //calculate breakExtraArriveLateMinutes
	    if (!StringHelper.isEmpty(pars.breakExtraArriveLateTimeCode))
	    {
	        //<0 means extraArriveLateMinutes needs to be calculated as a percentage of the the scheduled duration
	        if ( pars.breakExtraArriveLateMinutes < 0 )
	        {
	            int skdMins = wbData.getShiftWithBreaks(shiftIndex).
	            retrieveScheduledMinutesPaid(wbData.getCodeMapper());
	           pars.breakExtraArriveLateMinutes = (int) (skdMins *
	                    (-1.0 * pars.breakExtraArriveLateMinutes / 100));
	        }
	    }

	    //get current shift with breaks
	    shiftWithBreaks = wbData.getShiftWithBreaks(shiftIndex);
	    breaksList = shiftWithBreaks.getShiftBreaks();

	    scheduleStart = shiftWithBreaks.getShftStartTime();
	    scheduleEnd = shiftWithBreaks.getShftEndTime();

	    //get work details that pertain to this shift
	    workDetailList = wbData.getRuleData().getWorkDetails();
	    workDetailList.splitAt(scheduleStart);
	    workDetailList.splitAt(scheduleEnd);

	    //loop through work details for this shift
	    itWDList = workDetailList.iterator();
	    while(itWDList.hasNext())
	    {
	        workDetail = (WorkDetailData)itWDList.next();
	        wdStart = workDetail.getWrkdStartTime();
	        wdEnd = workDetail.getWrkdEndTime();
	        detailMinutes = (int)DateHelper.getMinutesBetween(wdEnd, wdStart);

	        tcodeName = workDetail.getWrkdTcodeName();
	        htypeName = workDetail.getWrkdHtypeName();

	        //log info
	        log("work detail start: " + wdStart);
	        log("work detail end: " + wdEnd);
	        log("work detail minutes: " + detailMinutes);
	        log("work detail tcode name: " + tcodeName);
	        log("work detail htype name: " + htypeName);

	        //edit work detail after break to take arrive late and early into consideration
	        if(detailAfterBreak || detailAfterALate || detailAfterAEarly)
	        {
	            //arrive late
	            if(pars.uatTcodeName.equalsIgnoreCase(tcodeName))
	            {
	                log("arrive late");

	                //add extra arrive late tcode for work detail duration
	                if (!StringHelper.isEmpty(pars.breakExtraArriveLateTimeCode)
	                        && (detailMinutes >= pars.breakExtraArriveLateMinutes))
	                {
	                    wbData.setWorkDetailTcodeName(pars.breakExtraArriveLateTimeCode,
	                            wdStart,
	                            wdEnd,
	                            null,
	                            false,
	                            true);
	                }
	                //add arrive late tcode for work detail duration
	                else if(!StringHelper.isEmpty(pars.breakArriveLateTimeCode))
	                {
	                    wbData.setWorkDetailTcodeName(pars.breakArriveLateTimeCode,
	                            wdStart,
	                            wdEnd,
	                            null,
	                            false,
	                            true);
	                }
	                detailAfterALate = true;
	                detailAfterAEarly = false;
	            }
	            //arrive early
	            else if(unusedMinutes > 0)
	            {
	                log("arrive early");

	                arriveEarlyMinutes = Math.min(detailMinutes, unusedMinutes);
	                unusedMinutes -= arriveEarlyMinutes;

	                //add extra arrive early tcode for unused minutes duration
	                if (!StringHelper.isEmpty(pars.breakExtraArriveEarlyTimeCode)
	                        &&  (unusedMinutes >= pars.breakExtraArriveEarlyMinutes))
	                {
	                    wbData.setWorkDetailTcodeName(pars.breakExtraArriveEarlyTimeCode,
	                            wdStart,
	                            DateHelper.addMinutes(wdStart, arriveEarlyMinutes),
	                            null,
	                            false,
	                            true);
	                }
	                //add arrive early tcode for unused minutes duration
	                else if(!StringHelper.isEmpty(pars.breakArriveEarlyTimeCode))
	                {
	                    wbData.setWorkDetailTcodeName(pars.breakArriveEarlyTimeCode,
	                            wdStart,
	                            DateHelper.addMinutes(wdStart, arriveEarlyMinutes),
	                            null,
	                            false,
	                            true);
	                }
	                detailAfterAEarly = true;
	                detailAfterALate = false;
	            }
	            else
	            {
	                detailAfterALate = false;
	                detailAfterAEarly = false;
	            }

	            detailAfterBreak = false;
	        }

	        //find breaks
	        if(tcodeName.equalsIgnoreCase(pars.breakTimeCode))
	        {
	            //get shift break that belongs to this work detail
	            currentBreak = findBreak(breaksList, wdStart, wdEnd);

	            //get the minutes already used for current break
	            usedBreakMinutes = getUsedBreakMinutes(workDetailList,
	                    				currentBreak.getShftbrkStartTime(),
	                    				currentBreak.getShftbrkEndTime(), pars);

	            log("used break minutes: " + usedBreakMinutes);

	            //find minutes remaing in break window
	            WindowMinutesRemaining = (int)DateHelper.getMinutesBetween(currentBreak.getShftbrkEndTime(), wdEnd);

	            //determine unused minutes for break
	            unusedMinutes = currentBreak.getShftbrkMinutes() - usedBreakMinutes;
	            unusedMinutes = Math.min(unusedMinutes, WindowMinutesRemaining);

	            log("unused minutes: " + unusedMinutes);

	            //set flag so next detail will be processed as detail after a break
	            detailAfterBreak = true;
	        }
	    }
	}

	/**
	 * find shift break within this work details' start and end window
	 *
	 * @param shiftBreaks
	 * @param detailStart
	 * @param detailEnd
	 * @return
	 */
	protected ShiftBreakData findBreak(List shiftBreaks, Date detailStart, Date detailEnd)
	{
	    ShiftBreakData currentBreak = null;
	    Date breakStart = null;
	    Date breakEnd = null;
	    Iterator itShiftBreaks = null;

	    itShiftBreaks = shiftBreaks.iterator();
	    while(itShiftBreaks.hasNext())
	    {
	        currentBreak = (ShiftBreakData)itShiftBreaks.next();
	        breakStart = currentBreak.getShftbrkStartTime();
	        breakEnd = currentBreak.getShftbrkEndTime();

	        //see if current detail is current break
	        if(detailStart.before(breakEnd) && detailEnd.after(breakStart))
	        {
	            break;
	        }
	    }

	    return currentBreak;
	}

	/**
	 * Get break minutes already used within the break start and end window
	 *
	 * @param workDetailList
	 * @param breakStart
	 * @param breakEnd
	 * @return
	 */
	protected int getUsedBreakMinutes(WorkDetailList workDetailList,
                                      Date breakStart, Date breakEnd,
                                      ParametersResolved pars)
	{
	    WorkDetailData workDetail = null;
	    Iterator itWDList = null;
	    Date wdStart = null;
	    Date wdEnd = null;
	    String tcodeName = null;
	    String htypeName = null;
	    int usedMinutes = 0;

	    //loop through work details within this break window
	    itWDList = workDetailList.iterator();
	    while(itWDList.hasNext())
	    {
	        workDetail = (WorkDetailData)itWDList.next();
	        wdStart = workDetail.getWrkdStartTime();
	        wdEnd = workDetail.getWrkdEndTime();
	        tcodeName = workDetail.getWrkdTcodeName();
	        htypeName = workDetail.getWrkdHtypeName();

	        //consider only those details that fall within the break window
	        if(wdStart.before(breakEnd) && wdEnd.after(breakStart))
	        {
		        //add up all break , extra arrive early, and arrive early minutes already used
		        if(tcodeName.equalsIgnoreCase(pars.breakTimeCode) ||
		                tcodeName.equalsIgnoreCase(pars.breakExtraArriveEarlyTimeCode) ||
		                tcodeName.equalsIgnoreCase(pars.breakArriveEarlyTimeCode))
		        {
		            //get the times where the work detail was within the break window
		            wdStart = DateHelper.max(wdStart, breakStart);
		            wdEnd = DateHelper.min(wdEnd, breakEnd);
		            usedMinutes += DateHelper.getMinutesBetween(wdEnd, wdStart);
		        }
	        }
	    }

	    return usedMinutes;
	}

	/**
	 * Method to log msgs
	 *
	 * @param msg
	 */
	protected void log(String msg)
	{
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))
        {
            logger.debug(msg);
        }
    }

    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.RuleComponent#getComponentName()
     */
    public String getComponentName()
    {
        return "WBIAG: Variable Break Rule";
    }

    class ParametersResolved {
        // Rule Parameters.
        protected String breakTimeCode;
        protected String breakArriveEarlyTimeCode;
        protected String breakArriveLateTimeCode;
        protected String breakExtraArriveEarlyTimeCode;
        protected String breakExtraArriveLateTimeCode;
        protected String uatTcodeName;
        protected int  breakExtraArriveEarlyMinutes;
        protected int  breakExtraArriveLateMinutes;
        protected boolean applyToAllShifts;

    }
}
