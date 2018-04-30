package com.wbiag.app.ta.quickrules;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import com.wbiag.app.modules.pts.PTSHelper;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.model.WorkDetailData;
import com.workbrain.app.ta.ruleengine.Parameters;
import com.workbrain.app.ta.ruleengine.Rule;
import com.workbrain.app.ta.ruleengine.RuleParameterInfo;
import com.workbrain.app.ta.ruleengine.WBData;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;

/** 
 * Title:			PTS Extend Work Time Rule	
 * Description:		Rule to extend the work time to calculation time if
 * 					work summary is marked as still ON CLOCK
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Apr 20, 2005
 * @author         	Kevin Tsoi
 */
public class PTSExtendWorkTimeRule extends Rule
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSExtendWorkTimeRule.class);
    public static final String PARAM_MINUTES_EXTEND = "MinutesToExtend";
    public static final String PARAM_TCODENAME_LIST = "TcodeNameList";
    public static final String PARAM_TCODE_INCLUSIVE = "TcodeInclusive";
    public static final String PARAM_HTYPENAME_LIST = "HtypeNameList";
    public static final String PARAM_HTYPE_INCLUSIVE = "HtypeInclusive";
    public static final String PARAM_EXTEND_TCODE = "extendTcode";
    public static final String PARAM_EXTEND_HTYPE = "extendHtype";
            
    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.Rule#execute(com.workbrain.app.ta.ruleengine.WBData, com.workbrain.app.ta.ruleengine.Parameters)
     */
    public void execute(WBData data, Parameters parameters)
    {
        List workDetailList = null;
        WorkDetailData workDetailData = null;
        WorkDetailData extendWorkDetail = null;
        Date newEndDate = null;
        StringTokenizer workTimeCodes = null;
        StringTokenizer workHourTypes = null;
        String workDetailTimeCodeName = null;
        CodeMapper codeMapper = null;
        String currentTCode = null;
        String currentHType = null;
        String extendTcode = null;
        String extendHtype = null;
        String tcodeNameList = null;
        String htypeNameList = null;        
        boolean tcodeInclusive = true;
        boolean htypeInclusive = true;
        boolean matchTcode = false;
        boolean matchHtype = false;
        int extendMinutes = 0;
                
        extendMinutes = parameters.getIntegerParameter(PARAM_MINUTES_EXTEND, 0);
        extendTcode = parameters.getParameter(PARAM_EXTEND_TCODE);
        extendHtype = parameters.getParameter(PARAM_EXTEND_HTYPE);
        
        tcodeNameList = parameters.getParameter(PARAM_TCODENAME_LIST,"");
        tcodeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_TCODE_INCLUSIVE,"true")).booleanValue();
        htypeNameList = parameters.getParameter(PARAM_HTYPENAME_LIST,"");
        htypeInclusive = Boolean.valueOf(parameters.getParameter(PARAM_HTYPE_INCLUSIVE,"true")).booleanValue();        
        
        codeMapper = data.getCodeMapper();
                        
        workDetailList = data.getRuleData().getWorkDetails();
        Iterator it = workDetailList.iterator();
        
        //iterator through all work details
        while(it.hasNext())
        {
            workDetailData = (WorkDetailData)it.next();
            workDetailData.setCodeMapper(codeMapper);            
            workTimeCodes = new StringTokenizer(tcodeNameList, PTSHelper.REG_DELIMITER);
            workHourTypes = new StringTokenizer(htypeNameList, PTSHelper.REG_DELIMITER);
            matchTcode = false;
            matchHtype = false;
            
            //check if work detail has appropriate time code
            while(workTimeCodes.hasMoreTokens())
            {
                currentTCode = workTimeCodes.nextToken();
                if(currentTCode.equalsIgnoreCase(workDetailData.getWrkdTcodeName()))
                {
                    matchTcode = true;
                    break;
                }	                    	              	                
            }
            //check if work detail has appropriate hour type
            while(workHourTypes.hasMoreTokens())
            {
                currentHType = workHourTypes.nextToken();
                if(currentHType.equalsIgnoreCase(workDetailData.getWrkdHtypeName()))
                {
                    matchHtype = true;
                    break;
                }	                    	              	                
            }

            //get work detail with timecode considered as work
            if(((tcodeInclusive && matchTcode) || (!tcodeInclusive && !matchTcode)) &&
                    ((htypeInclusive && matchHtype) || (!htypeInclusive && !matchHtype)))
            {
                extendWorkDetail = workDetailData;             
            }                                               
        }
        
        //extends the end time of the last work detail
        if(extendWorkDetail != null)
        {            
            newEndDate = DateHelper.addMinutes(extendWorkDetail.getWrkdStartTime(), extendMinutes);
            extendWorkDetail.setWrkdEndTime(newEndDate);
            extendWorkDetail.setWrkdMinutes((int)DateHelper.getMinutesBetween(extendWorkDetail.getWrkdEndTime(), extendWorkDetail.getWrkdStartTime()));
            extendWorkDetail.setWrkdTcodeName(extendTcode);
            extendWorkDetail.setWrkdHtypeName(extendHtype);
        }        
    }       
    
    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.RuleComponent#getParameterInfo(com.workbrain.sql.DBConnection)
     */
    public List getParameterInfo(DBConnection conn) 
    {
        List result = new ArrayList();      
        result.add(new RuleParameterInfo(PARAM_MINUTES_EXTEND, RuleParameterInfo.INT_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_EXTEND_TCODE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_EXTEND_HTYPE, RuleParameterInfo.STRING_TYPE, false));
        result.add(new RuleParameterInfo(PARAM_TCODENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_TCODE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPENAME_LIST, RuleParameterInfo.STRING_TYPE, true));
        result.add(new RuleParameterInfo(PARAM_HTYPE_INCLUSIVE, RuleParameterInfo.STRING_TYPE, true));        
        return result;
    }
    
    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.RuleComponent#getComponentName()
     */
    public String getComponentName()
    {
        return "WBIAG:  PTS Extend Work Time Rule";
    }    
}
