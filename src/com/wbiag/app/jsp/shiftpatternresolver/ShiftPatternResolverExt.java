package com.wbiag.app.jsp.shiftpatternresolver;

import java.util.*;
import java.text.*;

import java.sql.SQLException;

import javax.servlet.jsp.PageContext;

import org.apache.log4j.Level;

import com.workbrain.util.*;
import com.workbrain.security.SecurityService;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.schedulein.*;
import com.workbrain.app.wbinterface.schedulein.model.*;
import com.workbrain.app.wbinterface.schedulein.db.*;
import com.workbrain.server.jsp.locale.*;
import com.workbrain.tool.locale.*;
import com.workbrain.app.jsp.shiftpatternresolver.*;
import com.workbrain.server.registry.Registry;

/**
 * Title:			ShiftPatternResolverExt
 * Description:		Added features to the core shift pattern resolver
 * Copyright:    	Copyright (c) 2004
 * Company:      	Workbrain Inc
 * @author 			Kevin Tsoi
 * @version 		1.0
 */
public class ShiftPatternResolverExt extends ShiftPatternProcessor
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPatternResolverExt.class);

    public static String DEFAULT_INCLUDE_IN_YAG = "Y";
    public static int DEFAULT_SHIFT_GROUP = 0;
    public static int DEFAULT_COLOR = 0;
    public static int DEFAULT_HOUR_TYPE = 0;
    public static int DEFAULT_TIME_CODE = 0;
    private static final int COMMIT_AFTER_EMPLOYEES = 25;

    private String actionType = ShiftPatternConstants.ACTION_PAINT;
    private boolean requestLoaded = false;
    private boolean isFirstTime = false;
    private String newOvrValue = "";
    private String wbuName = "";
    private String spName = "";
    private int spOffset = 0;
    private int numOfShifts = 1;

    private String employeesNames = "";
    private String teamName = "";
    private String calcGroupName = "";
    private String payGroupName = "";

    private boolean ovrIsPermanent = true;
    private String ovrStart = "";
    private Date ovrStartDate = null;
    private String ovrEnd = "";
    private Date ovrEndDate = null;

    private String existingShiftPatternName = "";

    private String newShiftPatternName = "";
    private String newShiftPatternDesc = "";
    private String effectiveDate = "";
    private String dayStartTime = "";
    private String newShiftPatternUdf1 = "";
    private String newShiftPatternUdf2 = "";
    private String newShiftPatternUdf3 = "";
    private String newShiftPatternUdf4 = "";
    private String newShiftPatternUdf5 = "";
    private String newShiftPatternVal1 = "";
    private String newShiftPatternVal2 = "";
    private String newShiftPatternVal3 = "";
    private String newShiftPatternVal4 = "";
    private String newShiftPatternVal5 = "";
    private String newShiftPatternFlag1 = "";
    private String newShiftPatternFlag2 = "";
    private String newShiftPatternFlag3 = "";
    private String newShiftPatternFlag4 = "";
    private String newShiftPatternFlag5 = "";
    private Date effectiveDateAsDate = null;
    //TODO: change back to private
    public ArrayList allDays = new ArrayList();
    private ArrayList allLabor = new ArrayList();
    protected int numberOfDays = ShiftPatternConstants.DEFAULT_NO_DAYS;

    public static final String FIELD_DAY_START_TIME = "dayStartTime";
    public static final String FIELD_NEW_SP_UDF1 = "newShiftPatternUdf1";
    public static final String FIELD_NEW_SP_UDF2 = "newShiftPatternUdf2";
    public static final String FIELD_NEW_SP_UDF3 = "newShiftPatternUdf3";
    public static final String FIELD_NEW_SP_UDF4 = "newShiftPatternUdf4";
    public static final String FIELD_NEW_SP_UDF5 = "newShiftPatternUdf5";
    public static final String FIELD_NEW_SP_VAL1 = "newShiftPatternVal1";
    public static final String FIELD_NEW_SP_VAL2 = "newShiftPatternVal2";
    public static final String FIELD_NEW_SP_VAL3 = "newShiftPatternVal3";
    public static final String FIELD_NEW_SP_VAL4 = "newShiftPatternVal4";
    public static final String FIELD_NEW_SP_VAL5 = "newShiftPatternVal5";
    public static final String FIELD_NEW_SP_FLAG1 = "newShiftPatternFlag1";
    public static final String FIELD_NEW_SP_FLAG2 = "newShiftPatternFlag2";
    public static final String FIELD_NEW_SP_FLAG3 = "newShiftPatternFlag3";
    public static final String FIELD_NEW_SP_FLAG4 = "newShiftPatternFlag4";
    public static final String FIELD_NEW_SP_FLAG5 = "newShiftPatternFlag5";    
    public static final String FIELD_RET_EMPLOYEES_NAMES = "retEmpName";
    public static final String FIELD_RET_START_DATE = "retStartDate";
    public static final String ACTION_SHFT_PAT_RETRIEVE = "shftPatRetrieve";
    public static final String ACTION_RETRIEVE = "retrieve";
    public static final String ACTION_ADDROW = "addRow";
    public static final String ACTION_GETINFO = "getInfo";
    public static final String FIELD_LAB_START = "LabStartTime_";
    public static final String FIELD_LAB_END = "LabEndTime_";
    public static final String FIELD_LAB_DOCKET = "LabDocket_";
    public static final String FIELD_LAB_HTYPE = "LabHType_";
    public static final String FIELD_LAB_JOB = "LabJob_";
    public static final String FIELD_LAB_TCODE = "LabTCode_";
    public static final String FIELD_LAB_PROJ = "LabProj_";
    public static final String FIELD_LAB_DEPT = "LabDept_";
    public static final String FIELD_LAB_ACT = "LabAct_";
    public static final String FIELD_LAB_TEAM = "LabWbt_";
    public static final String FIELD_LAB_FLAG1 = "LabFlag1_";
    public static final String FIELD_LAB_FLAG2 = "LabFlag2_";
    public static final String FIELD_LAB_FLAG3 = "LabFlag3_";
    public static final String FIELD_LAB_FLAG4 = "LabFlag4_";
    public static final String FIELD_LAB_FLAG5 = "LabFlag5_";
    public static final String FIELD_LAB_FLAG6 = "LabFlag6_";
    public static final String FIELD_LAB_FLAG7 = "LabFlag7_";
    public static final String FIELD_LAB_FLAG8 = "LabFlag8_";
    public static final String FIELD_LAB_FLAG9 = "LabFlag9_";
    public static final String FIELD_LAB_FLAG10 = "LabFlag10_";
    public static final String FIELD_LAB_UDF1 = "LabUdf1_";
    public static final String FIELD_LAB_UDF2 = "LabUdf2_";
    public static final String FIELD_LAB_UDF3 = "LabUdf3_";
    public static final String FIELD_LAB_UDF4 = "LabUdf4_";
    public static final String FIELD_LAB_UDF5 = "LabUdf5_";
    public static final String FIELD_LAB_UDF6 = "LabUdf6_";
    public static final String FIELD_LAB_UDF7 = "LabUdf7_";
    public static final String FIELD_LAB_UDF8 = "LabUdf8_";
    public static final String FIELD_LAB_UDF9 = "LabUdf9_";
    public static final String FIELD_LAB_UDF10 = "LabUdf10_";
    public static final String SHIFT_NUM_POSFIX = "_shift_";
    public static final String AUTO_POPULATE_LABOR_PARAM = "system/shift pattern/AUTO_POPULATE_LABOR";
    public static final String CHECK_GAPS_PARAM = "system/shift pattern/CHECK_GAPS";
    public static final String MAX_NUM_DAYS_PARAM = "system/shift pattern/MAX_NUM_DAYS";
    public static final String ENABLE_GET_INFO_PARAM = "system/shift pattern/ENABLE_GET_INFO";
    public static final String DAY_OF_WEEK_LOCALIZATION_PARAM = "system/shift pattern/DAY_OF_WEEK_LOCALIZATION";
    public static final String DEFAULT_NUM_DAYS_PARAM = "system/shift pattern/DEFAULT_NUM_DAYS";
    public static final String DISABLE_SET_DAYS_PARAM = "system/shift pattern/DISABLE_SET_DAYS";
    public static final String DISABLE_ADD_ROWS_PARAM = "system/shift pattern/DISABLE_ADD_ROWS";
    public static final String DISABLE_SHOW_HIDE_DEFAULTS_PARAM = "system/shift pattern/DISABLE_SHOW_HIDE_DEFAULTS";
    public static final String NUM_OF_SHIFTS_PARAM = "system/shift pattern/NUM_OF_SHIFTS";
    public static final String DISABLE_SP_NAME_CHANGE_PARAM = "system/shift pattern/DISABLE_SP_NAME_CHANGE";
    public static final String ENABLE_CUSTOM_VALIDATION_PARAM = "system/shift pattern/ENABLE_CUSTOM_VALIDATION";

	// SC - 10/19/2005 - SPR Enhancements
    public static final String ENABLE_WEEK_LABEL_PARAM = "system/shift pattern/ENABLE_WEEK_LABEL";
    public static final String DISABLE_LABOR_METRICS_PARAM = "system/shift pattern/DISABLE_LABOR_METRICS";
    public static final String DISABLE_CREATE_BUTTON_PARAM = "system/shift pattern/DISABLE_CREATE_BUTTON";
    public static final String DISABLE_CREATE_ASSIGN_BUTTON_PARAM = "system/shift pattern/DISABLE_CREATE_ASSIGN_BUTTON";
    public static final String DEFAULT_TIME_CODE_PARAM = "system/shift pattern/DEFAULT_TIME_CODE";
    public static final String DEFAULT_HOUR_TYPE_PARAM = "system/shift pattern/DEFAULT_HOUR_TYPE";


    private int empId;
    private int preferredJobId;
    private Date retDate;
    private ArrayList shiftsList = new ArrayList();
    private ArrayList shiftLaborsList = new ArrayList();
    private ArrayList shiftBreaksList = new ArrayList();
    
    //for testing
    public Map paramsMap = null;
    
    //for plugin
	private String className = null;
	private Class pluginClass = null;
	private Object pluginObject = null;	

    public int getPreferredJobId()
    {
        return preferredJobId;
    }

    public ArrayList getShiftsList()
    {
        return shiftsList;
    }

    public ArrayList getShiftLaborsList()
    {
        return shiftLaborsList;
    }

    public ArrayList getShiftBreaksList()
    {
        return shiftBreaksList;
    }

    // accessors
    public String getActionType(){
      return actionType;
    }
    public void setActionType(String action){
      actionType = action;
    }

    public String getEmployeesNames(){
      return employeesNames;
    }
    public void setEmployeesNames(String names){
      employeesNames = names;
    }

    public String getTeamName(){
      return teamName;
    }
    public void setTeamName(String names){
      teamName = names;
    }

    public String getCalcGroupName(){
      return calcGroupName;
    }
    public void setCalcGroupName(String names){
      calcGroupName = names;
    }

    public String getPayGroupName(){
      return payGroupName;
    }
    public void setPayGroupName(String names){
      payGroupName = names;
    }

    public boolean getOvrIsPermanent(){
      return ovrIsPermanent;
    }
    public void setOvrIsPermanent(boolean checked){
      ovrIsPermanent = checked;
    }

    public String getOvrStartDate(){
      return ovrStart;
    }
    public void setOvrStartDate(String date){
      ovrStart = date;
    }

    public String getOvrEndDate(){
      return ovrEnd;
    }
    public void setOvrEndDate(String date){
      ovrEnd=date;
    }

    public String getExistingShiftPatternName(){
      return existingShiftPatternName;
    }
    public void setExistingShiftPatternName(String name){
      existingShiftPatternName = name;
    }

    public String getNewShiftPatternName(){
      return newShiftPatternName;
    }
    public void setNewShiftPatternName(String name){
      newShiftPatternName = name;
    }

    public String getNewShiftPatternDesc(){
      return newShiftPatternDesc;
    }
    public void setNewShiftPatternDesc(String desc){
      newShiftPatternDesc = desc;
    }

    public String getEffectiveDate(){
      return effectiveDate;
    }
    public void setEffectiveDate(String date){
      effectiveDate = date;
    }

	public String getNewShiftPatternFlag1() {
		return newShiftPatternFlag1;
	}

	public void setNewShiftPatternFlag1(String newShiftPatternFlag1) {
		this.newShiftPatternFlag1 = newShiftPatternFlag1;
	}

	public String getNewShiftPatternFlag2() {
		return newShiftPatternFlag2;
	}

	public void setNewShiftPatternFlag2(String newShiftPatternFlag2) {
		this.newShiftPatternFlag2 = newShiftPatternFlag2;
	}

	public String getNewShiftPatternFlag3() {
		return newShiftPatternFlag3;
	}

	public void setNewShiftPatternFlag3(String newShiftPatternFlag3) {
		this.newShiftPatternFlag3 = newShiftPatternFlag3;
	}

	public String getNewShiftPatternFlag4() {
		return newShiftPatternFlag4;
	}

	public void setNewShiftPatternFlag4(String newShiftPatternFlag4) {
		this.newShiftPatternFlag4 = newShiftPatternFlag4;
	}

	public String getNewShiftPatternFlag5() {
		return newShiftPatternFlag5;
	}

	public void setNewShiftPatternFlag5(String newShiftPatternFlag5) {
		this.newShiftPatternFlag5 = newShiftPatternFlag5;
	}

	public String getNewShiftPatternUdf1() {
		return newShiftPatternUdf1;
	}

	public void setNewShiftPatternUdf1(String newShiftPatternUdf1) {
		this.newShiftPatternUdf1 = newShiftPatternUdf1;
	}

	public String getNewShiftPatternUdf2() {
		return newShiftPatternUdf2;
	}

	public void setNewShiftPatternUdf2(String newShiftPatternUdf2) {
		this.newShiftPatternUdf2 = newShiftPatternUdf2;
	}

	public String getNewShiftPatternUdf3() {
		return newShiftPatternUdf3;
	}

	public void setNewShiftPatternUdf3(String newShiftPatternUdf3) {
		this.newShiftPatternUdf3 = newShiftPatternUdf3;
	}

	public String getNewShiftPatternUdf4() {
		return newShiftPatternUdf4;
	}

	public void setNewShiftPatternUdf4(String newShiftPatternUdf4) {
		this.newShiftPatternUdf4 = newShiftPatternUdf4;
	}

	public String getNewShiftPatternUdf5() {
		return newShiftPatternUdf5;
	}

	public void setNewShiftPatternUdf5(String newShiftPatternUdf5) {
		this.newShiftPatternUdf5 = newShiftPatternUdf5;
	}

	public String getNewShiftPatternVal1() {
		return newShiftPatternVal1;
	}

	public void setNewShiftPatternVal1(String newShiftPatternVal1) {
		this.newShiftPatternVal1 = newShiftPatternVal1;
	}

	public String getNewShiftPatternVal2() {
		return newShiftPatternVal2;
	}

	public void setNewShiftPatternVal2(String newShiftPatternVal2) {
		this.newShiftPatternVal2 = newShiftPatternVal2;
	}

	public String getNewShiftPatternVal3() {
		return newShiftPatternVal3;
	}

	public void setNewShiftPatternVal3(String newShiftPatternVal3) {
		this.newShiftPatternVal3 = newShiftPatternVal3;
	}

	public String getNewShiftPatternVal4() {
		return newShiftPatternVal4;
	}

	public void setNewShiftPatternVal4(String newShiftPatternVal4) {
		this.newShiftPatternVal4 = newShiftPatternVal4;
	}

	public String getNewShiftPatternVal5() {
		return newShiftPatternVal5;
	}

	public void setNewShiftPatternVal5(String newShiftPatternVal5) {
		this.newShiftPatternVal5 = newShiftPatternVal5;
	}
	
    public String getDayStartTime()
    {
        return dayStartTime;
    }

    public void setDayStartTime(String dayStartTime)
    {
        this.dayStartTime = dayStartTime;
    }
	
    public int getNumberOfDays(){
      return numberOfDays;
    }
    public void setNumberOfDays(int no){
      numberOfDays = no;
    }

    public List getAllDays(){
      return allDays;
    }

    public List getAllLabor(){
      return allLabor;
    }

    public boolean hasErrors(){
      return super.hasErrors();
    }

    public String getStatusMessage(){
      return super.getStatusMessage();
    }

    public String getErrorMessage(){
      return super.getErrorMessage();
    }

    private boolean isRequestLoaded(){
      return requestLoaded;
    }

    public int getNumOfShifts()
    {
        return numOfShifts;
    }
    
    public boolean getAutoPopulateLabor()
    {
        boolean flag = false;
        try
        {
            flag = "TRUE".equals(((String)Registry.getVar(AUTO_POPULATE_LABOR_PARAM)).toUpperCase());
        }
        catch(Exception e)
        {
            flag = false;
        }
        return flag;
    }

    public boolean getCheckGaps()
    {
        boolean flag = false;
        try
        {
            flag = "TRUE".equals(((String)Registry.getVar(CHECK_GAPS_PARAM)).toUpperCase());

        }
        catch(Exception e)
        {
            flag = false;
        }
        return flag;
    }

    public boolean getEnableGetInfo()
    {
        boolean flag = false;
        try
        {
            flag = "TRUE".equals(((String)Registry.getVar(ENABLE_GET_INFO_PARAM)).toUpperCase());

        }
        catch(Exception e)
        {
            flag = false;
        }
        return flag;
    }

    public String getMaxNumDays()
    {
        try
        {
            return (String)Registry.getVar(MAX_NUM_DAYS_PARAM);

        }
        catch(Exception e)
        {
            return "";
        }
    }

    public boolean getDayOfWeekLocalization()
    {
        boolean flag = false;
        try
        {
            flag = "TRUE".equals(((String)Registry.getVar(DAY_OF_WEEK_LOCALIZATION_PARAM)).toUpperCase());

        }
        catch(Exception e)
        {
            flag = false;
        }
        return flag;
    }

    public boolean getEnableSetDays() {
    	
        boolean flag = false;

        try {
            flag = "FALSE".equals(((String)Registry.getVar(DISABLE_SET_DAYS_PARAM)).toUpperCase());
        } catch(Exception e) {
            flag = true;
        }
        return flag;
    }
    
    public boolean getEnableAddRows() {
    	
        boolean flag = false;

        try {
            flag = "FALSE".equals(((String)Registry.getVar(DISABLE_ADD_ROWS_PARAM)).toUpperCase());
        } catch(Exception e) {
            flag = true;
        }
        return flag;
    }
    
    public boolean getEnableShowHideDefaults() {
    	
        boolean flag = false;

        try {
            flag = "FALSE".equals(((String)Registry.getVar(DISABLE_SHOW_HIDE_DEFAULTS_PARAM)).toUpperCase());
        } catch(Exception e) {
            flag = true;
        }
        return flag;
    }
    
    public boolean getDisableSPNameChange() {
    	
        boolean flag = false;

        try {
            flag = "TRUE".equals(((String)Registry.getVar(DISABLE_SP_NAME_CHANGE_PARAM)).toUpperCase());
        } catch(Exception e) {
            flag = false;
        }
        return flag;
    }    
    
    public boolean getEnableCustomValidation() {
    	
        boolean flag = false;

        try {
            flag = "TRUE".equals(((String)Registry.getVar(ENABLE_CUSTOM_VALIDATION_PARAM)).toUpperCase());
        } catch(Exception e) {
            flag = false;
        }
        return flag;
    }

    public boolean getEnableWeekLabel() {
    	
        boolean flag = false;

        try {
            flag = "TRUE".equals(((String)Registry.getVar(ENABLE_WEEK_LABEL_PARAM)).toUpperCase());
        } catch(Exception e) {
            flag = false;
        }
        return flag;
    }

    public boolean getDisableLaborMetrics() {
    	
        boolean flag = false;

        try {
            flag = "TRUE".equals(((String)Registry.getVar(DISABLE_LABOR_METRICS_PARAM)).toUpperCase());
        } catch(Exception e) {
            flag = false;
        }
        return flag;
    }

    public boolean getDisableCreateButton() {
    	
        boolean flag = false;

        try {
            flag = "TRUE".equals(((String)Registry.getVar(DISABLE_CREATE_BUTTON_PARAM)).toUpperCase());
        } catch(Exception e) {
            flag = false;
        }
        return flag;
    }

    public boolean getDisableCreateAssignButton() {
    	
        boolean flag = false;

        try {
            flag = "TRUE".equals(((String)Registry.getVar(DISABLE_CREATE_ASSIGN_BUTTON_PARAM)).toUpperCase());
        } catch(Exception e) {
            flag = false;
        }
        return flag;
    }

    public int getDefaultTimeCode() {
    	
    	int defaultTCodeId = 0;
    	
    	// Check the registry.
        try {
            String defaultTCodeNameStr = (String)Registry.getVar(DEFAULT_TIME_CODE_PARAM);
            TimeCodeData tcd = getCodeMapper().getTimeCodeByName(defaultTCodeNameStr);
            defaultTCodeId = tcd.getTcodeId();
        } catch(Exception e) {
        	defaultTCodeId = DEFAULT_TIME_CODE;
        }
        
        return defaultTCodeId;
   }    
    
    public int getDefaultHourType() {
    	
    	int defaultHTypeId = 0;
    	
    	// Check the registry.
        try {
            String defaultHTypeNameStr = (String)Registry.getVar(DEFAULT_HOUR_TYPE_PARAM);
            HourTypeData htd = getCodeMapper().getHourTypeByName(defaultHTypeNameStr);
            defaultHTypeId = htd.getHtypeId();
        } catch(Exception e) {
        	defaultHTypeId = DEFAULT_HOUR_TYPE;
        }
        
        return defaultHTypeId;
   }          
    
	private int getDefaultNumberOfDays() {
    	
    	int defaultDays = 0;
    	
    	// Check the registry.
        try {
            defaultDays = Integer.parseInt((String)Registry.getVar(DEFAULT_NUM_DAYS_PARAM));
        } catch(Exception e) {
            defaultDays = ShiftPatternConstants.DEFAULT_NO_DAYS;
        }
        
        return defaultDays;
   }    
    
    public int getNumOfShiftsFromReg() {
    	
    	int numOfShiftsFromReg = 0;
    	
    	// Check the registry.
        try {
            numOfShiftsFromReg = Integer.parseInt((String)Registry.getVar(NUM_OF_SHIFTS_PARAM));
        } catch(Exception e) {
            numOfShiftsFromReg = 1;
        }
        
        return numOfShiftsFromReg;
   }
    
    private void resetDefaults(){
      requestLoaded = false;
      isFirstTime = false;
      newOvrValue = "";
      errorsFound = false;
      errorMessage = "";
      statusMessage = "";
      spName="";
      spOffset=0;
      
      this.numberOfDays = getDefaultNumberOfDays();
      
    } // end resetDefaults

    private ArrayList loadShiftPatternLabors()
    {
        ArrayList laborList = new ArrayList();
        ArrayList spLabors;
        ShiftPatternMapper.ShiftPatternShiftLaborThis spslt;
        String labStartTime;
        String labEndTime;
        String dockId;
        String htypeId;
        String jobId;
        String tcodeId;
        String projId;
        String deptId;
        String actId;
        String wbtId;
        String spslabFlag1;
        String spslabFlag2;
        String spslabFlag3;
        String spslabFlag4;
        String spslabFlag5;
        String spslabFlag6;
        String spslabFlag7;
        String spslabFlag8;
        String spslabFlag9;
        String spslabFlag10;
        String spslabUdf1;
        String spslabUdf2;
        String spslabUdf3;
        String spslabUdf4;
        String spslabUdf5;
        String spslabUdf6;
        String spslabUdf7;
        String spslabUdf8;
        String spslabUdf9;
        String spslabUdf10;

        int count = 0;

        for(int i=0;i<this.numberOfDays;i++)
        {
            spLabors = new ArrayList();
            labStartTime = request.getParameter(FIELD_LAB_START + String.valueOf(i+1) + "_" + String.valueOf(count+1));

            while(labStartTime != null && !"".equals(labStartTime))
            {
                spslt = new ShiftPatternMapper.ShiftPatternShiftLaborThis();

                labEndTime = request.getParameter(FIELD_LAB_END + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                dockId = request.getParameter(FIELD_LAB_DOCKET + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                htypeId = request.getParameter(FIELD_LAB_HTYPE + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                jobId =request.getParameter(FIELD_LAB_JOB + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                tcodeId = request.getParameter(FIELD_LAB_TCODE + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                projId = request.getParameter(FIELD_LAB_PROJ + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                deptId = request.getParameter(FIELD_LAB_DEPT + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                actId = request.getParameter(FIELD_LAB_ACT + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                wbtId = request.getParameter(FIELD_LAB_TEAM + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                
                spslabFlag1 = request.getParameter(FIELD_LAB_FLAG1 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabFlag2 = request.getParameter(FIELD_LAB_FLAG2 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabFlag3 = request.getParameter(FIELD_LAB_FLAG3 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabFlag4 = request.getParameter(FIELD_LAB_FLAG4 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabFlag5 = request.getParameter(FIELD_LAB_FLAG5 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabFlag6 = request.getParameter(FIELD_LAB_FLAG6 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabFlag7 = request.getParameter(FIELD_LAB_FLAG7 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabFlag8 = request.getParameter(FIELD_LAB_FLAG8 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabFlag9 = request.getParameter(FIELD_LAB_FLAG9 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabFlag10 = request.getParameter(FIELD_LAB_FLAG10 + String.valueOf(i+1) + "_" + String.valueOf(count+1));

                spslabUdf1 = request.getParameter(FIELD_LAB_UDF1 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabUdf2 = request.getParameter(FIELD_LAB_UDF2 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabUdf3 = request.getParameter(FIELD_LAB_UDF3 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabUdf4 = request.getParameter(FIELD_LAB_UDF4 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabUdf5 = request.getParameter(FIELD_LAB_UDF5 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabUdf6 = request.getParameter(FIELD_LAB_UDF6 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabUdf7 = request.getParameter(FIELD_LAB_UDF7 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabUdf8 = request.getParameter(FIELD_LAB_UDF8 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabUdf9 = request.getParameter(FIELD_LAB_UDF9 + String.valueOf(i+1) + "_" + String.valueOf(count+1));
                spslabUdf10 = request.getParameter(FIELD_LAB_UDF10 + String.valueOf(i+1) + "_" + String.valueOf(count+1));

                spslt.setSpslabStartTime(DateHelper.convertStringToDate(labStartTime, ShiftPatternConstants.FULL_TIME_FMT));
                spslt.setSpslabEndTime(DateHelper.convertStringToDate(labEndTime, ShiftPatternConstants.FULL_TIME_FMT));

                if(dockId != null && !"".equals(dockId))
                {
                    spslt.setDockId(Integer.valueOf(dockId));
                }
                if(htypeId != null && !"".equals(htypeId))
                {
                    spslt.setHtypeId(Integer.valueOf(htypeId));
                }
                if(jobId != null && !"".equals(jobId))
                {
                    spslt.setJobId(Integer.valueOf(jobId));
                }
                if(tcodeId != null && !"".equals(tcodeId))
                {
                    spslt.setTcodeId(Integer.valueOf(tcodeId));
                }
                if(projId != null && !"".equals(projId))
                {
                    spslt.setProjId(Integer.valueOf(projId));
                }
                if(deptId != null && !"".equals(deptId))
                {
                    spslt.setDeptId(Integer.valueOf(deptId));
                }
                if(actId != null && !"".equals(actId))
                {
                    spslt.setActId(Integer.valueOf(actId));
                }
                if(wbtId != null && !"".equals(wbtId))
                {
                    spslt.setWbtId(Integer.valueOf(wbtId));
                }

                if(spslabFlag1 != null && !"".equals(spslabFlag1))
                {
                    spslt.setSpslabFlag1(spslabFlag1);
                }
                if(spslabFlag2 != null && !"".equals(spslabFlag2))
                {
                    spslt.setSpslabFlag2(spslabFlag2);
                }
                if(spslabFlag3 != null && !"".equals(spslabFlag3))
                {
                    spslt.setSpslabFlag3(spslabFlag3);
                }
                if(spslabFlag4 != null && !"".equals(spslabFlag4))
                {
                    spslt.setSpslabFlag4(spslabFlag4);
                }
                if(spslabFlag5 != null && !"".equals(spslabFlag5))
                {
                    spslt.setSpslabFlag5(spslabFlag5);
                }
                if(spslabFlag6 != null && !"".equals(spslabFlag6))
                {
                    spslt.setSpslabFlag6(spslabFlag6);
                }
                if(spslabFlag7 != null && !"".equals(spslabFlag7))
                {
                    spslt.setSpslabFlag7(spslabFlag7);
                }
                if(spslabFlag8 != null && !"".equals(spslabFlag8))
                {
                    spslt.setSpslabFlag8(spslabFlag8);
                }
                if(spslabFlag9 != null && !"".equals(spslabFlag9))
                {
                    spslt.setSpslabFlag9(spslabFlag9);
                }
                if(spslabFlag10 != null && !"".equals(spslabFlag10))
                {
                    spslt.setSpslabFlag10(spslabFlag10);
                }

                if(spslabUdf1 != null && !"".equals(spslabUdf1))
                {
                    spslt.setSpslabUdf1(spslabUdf1);
                }
                if(spslabUdf2 != null && !"".equals(spslabUdf2))
                {
                    spslt.setSpslabUdf2(spslabUdf2);
                }
                if(spslabUdf3 != null && !"".equals(spslabUdf3))
                {
                    spslt.setSpslabUdf3(spslabUdf3);
                }
                if(spslabUdf4 != null && !"".equals(spslabUdf4))
                {
                    spslt.setSpslabUdf4(spslabUdf4);
                }
                if(spslabUdf5 != null && !"".equals(spslabUdf5))
                {
                    spslt.setSpslabUdf5(spslabUdf5);
                }
                if(spslabUdf6 != null && !"".equals(spslabUdf6))
                {
                    spslt.setSpslabUdf6(spslabUdf6);
                }
                if(spslabUdf7 != null && !"".equals(spslabUdf7))
                {
                    spslt.setSpslabUdf7(spslabUdf7);
                }
                if(spslabUdf8 != null && !"".equals(spslabUdf8))
                {
                    spslt.setSpslabUdf8(spslabUdf8);
                }
                if(spslabUdf9 != null && !"".equals(spslabUdf9))
                {
                    spslt.setSpslabUdf9(spslabUdf9);
                }
                if(spslabUdf10 != null && !"".equals(spslabUdf10))
                {
                    spslt.setSpslabUdf10(spslabUdf10);
                }

                spLabors.add(spslt);
                count++;
                labStartTime = request.getParameter(FIELD_LAB_START + String.valueOf(i+1) + "_" + String.valueOf(count+1));
            }
            laborList.add(spLabors);
            count = 0;
        }
        return laborList;
    }

    // read all Shift Pattern days
    //TODO: change back to private
    public ArrayList loadShiftPatternDays() 
    	throws WBInterfaceException
    {
        ArrayList spDays = new ArrayList();        
        for(int i=0 ; i <this.numberOfDays ; i++)
        {
            ArrayList multiShiftsList = new ArrayList();
            for(int shiftNum = 1 ; shiftNum <= getNumOfShifts() ; shiftNum++)
            {
		        ShiftPatternDay day = new ShiftPatternDay();
		        day.setDay("" + (i+1));
		        day.setShiftStartTime(getTime(request.getParameter(ShiftPatternConstants.FIELD_SHIFT_START + (i+1) + SHIFT_NUM_POSFIX + shiftNum)));
		        day.setShiftEndTime(getTime(request.getParameter(ShiftPatternConstants.FIELD_SHIFT_END + (i+1) + SHIFT_NUM_POSFIX + shiftNum)));
		
		        if(request.getParameter(ShiftPatternConstants.FIELD_SHIFT_YAG + (i + 1) + SHIFT_NUM_POSFIX + shiftNum)==null)
		        {
		          // new days added
		          day.setShiftYAG(DEFAULT_INCLUDE_IN_YAG);
		        } 
		        else 
		        {
		          day.setShiftYAG(request.getParameter(ShiftPatternConstants.FIELD_SHIFT_YAG + (i + 1) + SHIFT_NUM_POSFIX + shiftNum + "_dummy"));
		          if(day.getShiftYAG()==null)
		          {
		            // when uncheck the Checkbox the value is not submited at all so has to be set to "N"
		            day.setShiftYAG("N");
		          }
		        }
		        if(notEmpty(request.getParameter(ShiftPatternConstants.FIELD_SHIFT_COLOR + (i+1) + SHIFT_NUM_POSFIX + shiftNum)))
		        {
		          day.setShiftColor(new Integer(request.getParameter(ShiftPatternConstants.FIELD_SHIFT_COLOR + (i+1) + SHIFT_NUM_POSFIX + shiftNum)).intValue());
		        } 
		        else 
		        {
		          day.setShiftColor(DEFAULT_COLOR);
		        }
		        if(notEmpty(request.getParameter(ShiftPatternConstants.FIELD_SHIFT_GROUP + (i+1) + SHIFT_NUM_POSFIX + shiftNum)))
		        {
		          day.setShiftGroup(new Integer(request.getParameter(ShiftPatternConstants.FIELD_SHIFT_GROUP + (i+1) + SHIFT_NUM_POSFIX + shiftNum)).intValue());
		        } 
		        else 
		        {
		          day.setShiftGroup(DEFAULT_SHIFT_GROUP);
		        }
		        day.setBreakStartTime(getTime(request.getParameter(ShiftPatternConstants.FIELD_BREAK_START + (i+1) + SHIFT_NUM_POSFIX + shiftNum)));
		        day.setBreakEndTime(getTime(request.getParameter(ShiftPatternConstants.FIELD_BREAK_END + (i+1) + SHIFT_NUM_POSFIX + shiftNum)));
		        if(notEmpty(request.getParameter(ShiftPatternConstants.FIELD_BREAK_HOUR_TYPE + (i+1) + SHIFT_NUM_POSFIX + shiftNum)))
		        {
		          day.setBreakHourType(new Integer(request.getParameter(ShiftPatternConstants.FIELD_BREAK_HOUR_TYPE + (i+1) + SHIFT_NUM_POSFIX + shiftNum)).intValue());
		        } 
		        else 
		        {
		          day.setBreakHourType(DEFAULT_HOUR_TYPE);
		        }
		        if(notEmpty(request.getParameter(ShiftPatternConstants.FIELD_BREAK_TIME_CODE + (i+1) + SHIFT_NUM_POSFIX + shiftNum))) 
		        {
		          day.setBreakTimeCode(new Integer(request.getParameter(ShiftPatternConstants.FIELD_BREAK_TIME_CODE + (i+1) + SHIFT_NUM_POSFIX + shiftNum)).intValue());
		        } 
		        else 
		        {
		          day.setBreakTimeCode(DEFAULT_TIME_CODE);
		        }
		        multiShiftsList.add(day);		        
            }
            spDays.add(multiShiftsList);            
      }
      logger.debug("Shift pattern days loaded:" + spDays);
      return spDays;
    } // end loadShiftPatternDays

    // load the parameters from request
    private void loadFromRequest()
        throws WBInterfaceException, SQLException {
      actionType = request.getParameter(ShiftPatternConstants.FIELD_ACTION_TYPE);
      // load default action if null
      if(actionType==null){
        actionType = ShiftPatternConstants.ACTION_PAINT;
        isFirstTime = true;
        logger.debug("Defalut action to be performed: " + actionType);
      } else {
        logger.debug("Action to be performed: " + actionType);
      }
      employeesNames = request.getParameter(ShiftPatternConstants.FIELD_EMPLOYEES_NAMES);
      teamName = request.getParameter(ShiftPatternConstants.FIELD_TEAM_NAME);
      calcGroupName = request.getParameter(ShiftPatternConstants.FIELD_CALCGROUP_NAME);
      payGroupName = request.getParameter(ShiftPatternConstants.FIELD_PAYGROUP_NAME);
      ovrIsPermanent = (request.getParameter(ShiftPatternConstants.FIELD_OVR_PERMANENT)!=null)?true:false;
      if(isFirstTime) ovrIsPermanent = true;
      ovrStart = request.getParameter(ShiftPatternConstants.FIELD_OVR_START_DATE);
      ovrStartDate = toDate(ovrStart, DateHelper.getCurrentDate());
      if(isEmpty(ovrStart)){
        ovrStart = getDateFormatter().format(ovrStartDate) + " 000000";
      }
      ovrEnd = request.getParameter(ShiftPatternConstants.FIELD_OVR_END_DATE);
      ovrEndDate = toDate(ovrEnd, DateHelper.DATE_3000);
      if(isEmpty(ovrEnd)){
        ovrEnd = getDateFormatter().format(ovrEndDate) + " 000000";
      }
      if(ovrIsPermanent) {
        //ovrStartDate = DateHelper.getCurrentDate();
        ovrEndDate = DateHelper.DATE_3000;
      }
      existingShiftPatternName = getNotNull(request.getParameter(ShiftPatternConstants.FIELD_OLD_SP_NAME), "");
      this.spName = getNotNull(request.getParameter(ShiftPatternConstants.FIELD_OLD_SP_NAME + "_label"), "");
      newShiftPatternName = getNotNull(request.getParameter(ShiftPatternConstants.FIELD_NEW_SP_NAME), "");
      newShiftPatternDesc = getNotNull(request.getParameter(ShiftPatternConstants.FIELD_NEW_SP_DESC), "");
      effectiveDate = request.getParameter(ShiftPatternConstants.FIELD_EFFECTIVE_DATE);
      dayStartTime = getNotNull(request.getParameter(FIELD_DAY_START_TIME), "");
      newShiftPatternUdf1 = getNotNull(request.getParameter(FIELD_NEW_SP_UDF1), "");
      newShiftPatternUdf2 = getNotNull(request.getParameter(FIELD_NEW_SP_UDF2), "");
      newShiftPatternUdf3 = getNotNull(request.getParameter(FIELD_NEW_SP_UDF3), "");
      newShiftPatternUdf4 = getNotNull(request.getParameter(FIELD_NEW_SP_UDF4), "");
      newShiftPatternUdf5 = getNotNull(request.getParameter(FIELD_NEW_SP_UDF5), "");
      newShiftPatternVal1 = getNotNull(request.getParameter(FIELD_NEW_SP_VAL1), "");
      newShiftPatternVal2 = getNotNull(request.getParameter(FIELD_NEW_SP_VAL2), "");
      newShiftPatternVal3 = getNotNull(request.getParameter(FIELD_NEW_SP_VAL3), "");
      newShiftPatternVal4 = getNotNull(request.getParameter(FIELD_NEW_SP_VAL4), "");
      newShiftPatternVal5 = getNotNull(request.getParameter(FIELD_NEW_SP_VAL5), "");
      newShiftPatternFlag1 = getNotNull(request.getParameter(FIELD_NEW_SP_FLAG1), "");
      newShiftPatternFlag2 = getNotNull(request.getParameter(FIELD_NEW_SP_FLAG2), "");
      newShiftPatternFlag3 = getNotNull(request.getParameter(FIELD_NEW_SP_FLAG3), "");
      newShiftPatternFlag4 = getNotNull(request.getParameter(FIELD_NEW_SP_FLAG4), "");
      newShiftPatternFlag5 = getNotNull(request.getParameter(FIELD_NEW_SP_FLAG5), "");
      effectiveDateAsDate = toDate(effectiveDate, DateHelper.getCurrentDate());
      if(isEmpty(effectiveDate)){
        effectiveDate = getDateFormatter().format(effectiveDateAsDate) + " 000000";
      }
      numberOfDays = (request.getParameter(ShiftPatternConstants.FIELD_NO_DAYS)!=null)?
          new Integer(request.getParameter(ShiftPatternConstants.FIELD_NO_DAYS)).intValue():
          getDefaultNumberOfDays();
      wbuName = SecurityService.getCurrentUser().getUserName();

      //set number of shifts
      numOfShifts = getNumOfShiftsFromReg();
      
      allDays = loadShiftPatternDays();
      allLabor = loadShiftPatternLabors();
      if(ShiftPatternConstants.ACTION_ASSIGN.equalsIgnoreCase(actionType)) {
        if((isEmpty(employeesNames) && isEmpty(teamName) && isEmpty(calcGroupName) && isEmpty(payGroupName)) || isEmpty(existingShiftPatternName)) {
          this.errorMessage = "{ML}SPDA_ASSIGN_ActionMessage{/ML}For ASSIGN action, you must select an employee or group of employees and specify the existing shift pattern.";
          this.errorsFound = true;
        }
      }
      if(ShiftPatternConstants.ACTION_CREATE_ASSIGN.equalsIgnoreCase(actionType)) {
        if(isEmpty(employeesNames) && isEmpty(teamName) && isEmpty(calcGroupName) && isEmpty(payGroupName)) {
          this.errorMessage = "{ML}SPDA_CREATE_ASSIGN_ActionMessage{/ML}For CREATE_ASSIGN action, you must select an employee or group of employees and specify shift time information";
          this.errorsFound = true;
        }
      }
      logger.debug(this.toString());
      requestLoaded = true;
    } // end loadFromRequest

    private ShiftPatternMapper populateShiftPatternMapper()
        throws WBInterfaceException, SQLException{
      ShiftPatternMapper spm = new ShiftPatternMapper();
      if(!requestLoaded){
        loadFromRequest();
      }
      spm.setShiftPatternData(populateShiftPatternData());
      spm.setShiftPatternShiftList(populateAllShifts());
      return spm;
    } // end populateShiftPatternMapper

    //populate Shift Pattern Data
    private ShiftPatternData populateShiftPatternData()
        throws WBInterfaceException, SQLException {
      ShiftPatternData spd = new ShiftPatternData();
      spd.setShftpatName(this.newShiftPatternName);
      spd.setShftpatDesc(this.newShiftPatternDesc);
      spd.setShftpatStartDate(this.effectiveDateAsDate);
      if(!StringHelper.isEmpty(dayStartTime))
          spd.setShftpatDayStartTime(DateHelper.convertStringToDate(this.dayStartTime, ShiftPatternConstants.FULL_TIME_FMT));
      if(!StringHelper.isEmpty(newShiftPatternUdf1))
      	spd.setShftpatUdf1(this.newShiftPatternUdf1);
      if(!StringHelper.isEmpty(newShiftPatternUdf2))
      	spd.setShftpatUdf2(this.newShiftPatternUdf2);
      if(!StringHelper.isEmpty(newShiftPatternUdf3))
      	spd.setShftpatUdf3(this.newShiftPatternUdf3);
      if(!StringHelper.isEmpty(newShiftPatternUdf4))
      	spd.setShftpatUdf4(this.newShiftPatternUdf4);
      if(!StringHelper.isEmpty(newShiftPatternUdf5))
      	spd.setShftpatUdf5(this.newShiftPatternUdf5);
      if(!StringHelper.isEmpty(newShiftPatternVal1))
      	spd.setShftpatVal1(Integer.parseInt(this.newShiftPatternVal1));
      if(!StringHelper.isEmpty(newShiftPatternVal2))
      	spd.setShftpatVal2(Integer.parseInt(this.newShiftPatternVal2));
      if(!StringHelper.isEmpty(newShiftPatternVal3))
      	spd.setShftpatVal3(Integer.parseInt(this.newShiftPatternVal3));
      if(!StringHelper.isEmpty(newShiftPatternVal4))
      	spd.setShftpatVal4(Integer.parseInt(this.newShiftPatternVal4));
      if(!StringHelper.isEmpty(newShiftPatternVal5))
      	spd.setShftpatVal5(Integer.parseInt(this.newShiftPatternVal5));
      if(!StringHelper.isEmpty(newShiftPatternFlag1))
      	spd.setShftpatFlag1(this.newShiftPatternFlag1);
      if(!StringHelper.isEmpty(newShiftPatternFlag2))
      	spd.setShftpatFlag2(this.newShiftPatternFlag2);
      if(!StringHelper.isEmpty(newShiftPatternFlag3))
      	spd.setShftpatFlag3(this.newShiftPatternFlag3);
      if(!StringHelper.isEmpty(newShiftPatternFlag4))
      	spd.setShftpatFlag4(this.newShiftPatternFlag4);
      if(!StringHelper.isEmpty(newShiftPatternFlag5))
      	spd.setShftpatFlag5(this.newShiftPatternFlag5);

      // extract GROUP_ID for the default group
      spd.setShftgrpId(DEFAULT_SHIFT_GROUP);
      logger.debug("Shift Pattern Data populated:" + spd.toString());
      return spd;
    } // end populateShiftPatternData

    //TODO:  change back to public
	public List populateAllShifts()
		throws WBInterfaceException, SQLException 
	{		
		ShiftPatternMapper.ShiftPatternShiftsThis spsThis = null;
		ShiftPatternDay day = null;      
		ShiftData offShift = null;
		ArrayList allShifts = null;
		ArrayList multiShiftsList = null;
		Iterator itShifts = null;
		Iterator itAllDays = null;
		Iterator itAllLabor = null;		
		String breaksStr = null;
		int shiftNo = 0;
				
		offShift = getOFFShift();
		allShifts = new ArrayList();		
		
		if (logger.isDebugEnabled())
		{
		    logger.debug("DAYS TO BE LOADED: " + this.allDays.size());
		}
		
		itAllDays = allDays.iterator();
		itAllLabor = allLabor.iterator();
		while(itAllDays.hasNext())
		{        
			spsThis = new ShiftPatternMapper.ShiftPatternShiftsThis();
			multiShiftsList = (ArrayList)itAllDays.next(); 
			itShifts = multiShiftsList.iterator();
			shiftNo = 1;
			while(itShifts.hasNext())
			{
				day = (ShiftPatternDay)itShifts.next();	
				if(day != null)
				{
					spsThis.setShftpatshftDay(WBInterfaceUtil.getInt(day.getDay(),
					mfErrorParse.format(new String[] {"Day Number"})));
					ShiftWithBreaksMapper swbMapper = new ShiftWithBreaksMapper();
			        // check for OFF shifts
			        if(("00:00".equals(day.getShiftStartTime()) && "00:00".equals(day.getShiftEndTime()))
			                ||(isEmpty(day.getShiftStartTime()) && isEmpty(day.getShiftEndTime())))
			        {        
			            // off shift
			            logger.debug("OFF SHIFT identified.");
			            swbMapper.setShift(offShift);
			            swbMapper.setShiftBreaks(new ArrayList());  
			        } 
			        else if(notEmpty(day.getShiftStartTime()) && notEmpty(day.getShiftEndTime())) 
					{
						// not off shift
					    if (logger.isDebugEnabled())
					    {
					        logger.debug("No OFF Shift identified");
					    }
					    ShiftData sd = new ShiftData();
						sd.setShftStartTime( WBInterfaceUtil.parseDateTime(day.getShiftStartTime(), ShiftPatternConstants.TIME_FMT,
						mfErrorParse.format(new String[] {"Shift Start Time"})));
						sd.setShftEndTime( WBInterfaceUtil.parseDateTime(day.getShiftEndTime(), ShiftPatternConstants.TIME_FMT,
						mfErrorParse.format(new String[] {"Shift End Time"})));
						sd.setShftYag(day.getShiftYAG());
						sd.setColrId(day.getShiftColor());
						sd.setShftgrpId(day.getShiftGroup());
						swbMapper.setShift(sd);
						// add break only if any data in both start and end break time!
						if(notEmpty(day.getBreakStartTime()) && notEmpty(day.getBreakEndTime()))
						{
							breaksStr = getBreaksAsString(day);
							List breaks = ScheduleHelper.getInterfaceShiftBreakList(breaksStr, getCodeMapper(), ShiftPatternConstants.TIME_FMT);
							swbMapper.setShiftBreaks(breaks);
						}
					} 					 
					else 
					{
						// this case should never happen - JavaScript validation in place
						this.errorMessage += "<br>For day " + day.getDay() + " both Shift Start Time and Shift End Time have to be populated or empty!";
						this.errorsFound = true;
						throw new WBInterfaceException("For day " + day.getDay() + " both Shift Start Time and Shift End Time have to be populated or empty!");
					}	
					spsThis.assignShiftWithBreaks(shiftNo, swbMapper);
				}
				shiftNo++;
			}
			spsThis.assignShiftPatLaborList((List)itAllLabor.next());
			allShifts.add(spsThis);		
		}
		if (logger.isDebugEnabled())
		{
		    logger.debug("All Shifts populated: " + allShifts);
		}
		return allShifts;
	} // end populateAllShifts

    public void doActionGetInfo()
        throws SQLException
    {
        int getInfoEmpId = Integer.parseInt(request.getParameter(ShiftPatternConstants.FIELD_EMPLOYEES_NAMES));
            
        EmployeeJobData ejd;
        EmployeeJobAccess eja = new EmployeeJobAccess(getConnection());
        List jobList = eja.loadByEmpId(getInfoEmpId);
        Iterator it = jobList.iterator();
        while(it.hasNext())
        {
            ejd = (EmployeeJobData)it.next();
            if("Y".equals(ejd.getEmpjobPreferred()))
            {
                preferredJobId = ejd.getJobId();
                break;
            }
        }
    }

    private void doActionRetrieve(int shftPatId)
        throws SQLException
    {
        EmployeeAccess ea;
        EmployeeData ed;

        ShiftPatternShiftsAccess spsa;
        ShiftPatternShiftsData spsd;
        List spsList;
        Iterator it;

        ShiftAccess sa;
        ShiftData sd;
        List shiftDataList;
        List multiShiftsList;

        ShiftBreakAccess sba;
        ShiftBreakData sbd;
        List shiftBreakDataList;
        List multiShiftBreaksList;

        ShiftPatternShiftLaborAccess spsla;
        ShiftPatternShiftLaborData spsld;
        List spslList;
        List tempList;        
        
        
        Iterator itShiftData;
        
        Integer shiftId1;
        Integer shiftId2;
        Integer shiftId3;
        Integer shiftId4;
        Integer shiftId5;
        
        StringBuffer whereClause;        

        if(shftPatId == -1)
        {
            empId = Integer.parseInt(request.getParameter(FIELD_RET_EMPLOYEES_NAMES));
            retDate = DateHelper.parseDate(request.getParameter(FIELD_RET_START_DATE), ShiftPatternConstants.DATE_FMT);

            //load employee to get shift pattern
            ea = new EmployeeAccess(getConnection(), getCodeMapper());
            ed = ea.load(empId, retDate);
            shftPatId = ed.getShftpatId();
        }

        //load shift pattern shifts
        spsa = new ShiftPatternShiftsAccess(getConnection());
        spsList = spsa.loadByShftPatId(shftPatId);
        it = spsList.iterator();

        while(it.hasNext())
        {
            multiShiftsList = new ArrayList();
            multiShiftBreaksList = new ArrayList();
            
            spsd = (ShiftPatternShiftsData)it.next();

            //gets shiftIds
            shiftId1 = new Integer(spsd.getShftId());
            shiftId2 = spsd.getShftId2();
            shiftId3 = spsd.getShftId3();
            shiftId4 = spsd.getShftId4();
            shiftId5 = spsd.getShftId5();

            //build where clause for loads
            whereClause = buildWhereClause(" shft_id = ", shiftId1, shiftId2, shiftId3, shiftId4, shiftId5);
            
            //load shift data
            sa = new ShiftAccess(getConnection());
            shiftDataList = sa.loadRecordData(new ShiftData(), ShiftAccess.SHIFT_TABLE, whereClause.toString());            
            
            multiShiftsList.add(getShiftData(shiftId1, shiftDataList));
            multiShiftsList.add(getShiftData(shiftId2, shiftDataList));
            multiShiftsList.add(getShiftData(shiftId3, shiftDataList));
            multiShiftsList.add(getShiftData(shiftId4, shiftDataList));
            multiShiftsList.add(getShiftData(shiftId5, shiftDataList));            
            shiftsList.add(multiShiftsList);
            
            //load shift break data -- assumes only 1 break                                   
            sba = new ShiftBreakAccess(getConnection());
            shiftBreakDataList = sba.loadRecordData(new ShiftBreakData(), ShiftBreakAccess.SHIFT_BREAK_TABLE, whereClause.toString());            
            
            multiShiftBreaksList.add(getShiftBreakData(shiftId1, shiftBreakDataList));
            multiShiftBreaksList.add(getShiftBreakData(shiftId2, shiftBreakDataList));
            multiShiftBreaksList.add(getShiftBreakData(shiftId3, shiftBreakDataList));
            multiShiftBreaksList.add(getShiftBreakData(shiftId4, shiftBreakDataList));
            multiShiftBreaksList.add(getShiftBreakData(shiftId5, shiftBreakDataList));  
            shiftBreaksList.add(multiShiftBreaksList);

            //load shift labor list
            spsla = new ShiftPatternShiftLaborAccess(getConnection());
            spslList = spsla.loadByShftPatShftId(spsd.getShftpatshftId());
            
            shiftLaborsList.add(spslList);            
        }
    }

    private void doActionAssign()
		throws WBInterfaceException, SQLException, ParseException, DataLocException 
	{
		ArrayList employees = getEmployeesID(this.employeesNames, this.teamName, this.calcGroupName, this.payGroupName);
		this.statusMessage += "<BR>Assign the Shift Pattern:" +
			  LocalizationDictionary.get().localizeData(getConnection(), this.spName,
			  "SHIFT_PATTERN", "SHFTPAT_NAME");
		this.statusMessage += "<BR>Using offset:" + this.spOffset;     
	  	for(int i=0;i<employees.size();i++) 
	  	{
	  		logger.debug("Overide epmloyee: " + (String)employees.get(i));
	  		processOverride(this.newOvrValue, new Integer((String)employees.get(i)).intValue(), ovrStartDate, ovrEndDate, wbuName);
	  		if (i % COMMIT_AFTER_EMPLOYEES == 0 ) 
	  		{
	  			if (logger.isEnabledFor(Level.DEBUG)) 
	  			{
	  				logger.debug("Committing after " + i + " employees.");
	  			}
  				getConnection().commit();
	  		}
  		}
	  	this.statusMessage +="<BR>Successfully assigned to " + employees.size() + " employees.";
	}

    private String doActionCreate()
    	throws WBInterfaceException, SQLException, DataLocException 
	{
    	logger.debug("Started the action: Create.");
    	this.statusMessage +="<BR>Checking for Shift Pattern...";
    	// create ShiftPattern Mapper
    	this.spMapper = populateShiftPatternMapper();
    	//check if the shift pattern exists
    	ShiftPatternManager.ShiftPatternKey spKey = getShiftPatternManager().retrieveShiftPattern(this.spMapper);
    	if(spKey!=null)
    	{
    		//Shift Pattern exists
    		this.statusMessage +="<BR>Shift Pattern found with the name: '"
    					+ LocalizationDictionary.get().localizeData(getConnection(),
    					spKey.getShftpatName(), "SHIFT_PATTERN", "SHFTPAT_NAME")
						+ "' and offset: " + spKey.getOffset();
    		this.spName = spKey.getShftpatName();
    		this.spOffset = spKey.getOffset();
    		logger.debug("Shift Pattern found with the name: '" + spKey.getShftpatName() +
    						"' and offset: " + spKey.getOffset());
    	} 
    	else 
    	{
    		//try to call preActionCreate from plugin
    		ShiftPatternResolverExtPlugin resolverExtPlugin = getInterface();    		
	      	if(resolverExtPlugin != null)
	      	{      		
	      		resolverExtPlugin.changeShiftPattern(this.employeesNames, 
	      		        this.ovrStartDate, this.ovrEndDate, this.spMapper);
	      	}
	      	//check to see if shift pattern name already exist
	      	String shiftPatternName = spMapper.getShiftPatternData().getShftpatName();
	      	ShiftPatternData shiftPattern = getCodeMapper().getShiftPatternByName(shiftPatternName);
	      	
	      	//if shift pattern name exist and disable SP name change is true
	      	if(shiftPattern != null && getDisableSPNameChange())
	      	{
	      	  this.statusMessage +="<BR> No Shift Pattern found, but shift pattern name already exist.  Shift pattern will not be created.";
	      	  logger.debug("No Shift Pattern found, but shift pattern name already exist.  Shift pattern will not be created.");	      	    
	      	}
	      	else
	      	{
		      	// new Shift Pattern	      	
		      	this.spName = this.createShiftPattern(this.spMapper);
		      	this.spOffset = 0;
		      	this.statusMessage +="<BR> No Shift Pattern found! A new one will be created with the name: '"
		      				+ LocalizationDictionary.get().localizeData(getConnection(), spName,
		      				"SHIFT_PATTERN", "SHFTPAT_NAME") + "'";
		      	logger.debug("No Shift Pattern found! A new one will be created with the name: '" + spName + "'");
	      	}
    	}
    	return spName;
    } // doActionCreate

    public void init(PageContext context)
        throws WBInterfaceException, SQLException{
      resetDefaults();
      this.request = context.getRequest();
      paramsMap = request.getParameterMap();      
      // read input informations
      loadFromRequest();
    }

    public boolean processRetrieve(int shftPatId)
        throws SQLException
    {
        doActionRetrieve(shftPatId);
        return true;
    }

    public boolean processRequest()
          throws WBInterfaceException, SQLException, java.text.ParseException,
          DataLocException {
      if(!this.isRequestLoaded()){
        logger.error("Cannot perform processRequest without inititializing the request");
        this.statusMessage = "The init(PageContext) method has to be called before processRequest()! Please inform system administrator! Action was canceled";
        throw new WBInterfaceException("Misuse of the Shift Pattern Resolver. The init(PageContext) method has to be called before processRequest()");
      }
      if(ShiftPatternConstants.ACTION_ASSIGN.equalsIgnoreCase(actionType)){
        newOvrValue = getNewOvrValue(request.getParameter(ShiftPatternConstants.FIELD_OLD_SP_NAME + "_label"),0);
        doActionAssign();
      }
      else if(ShiftPatternConstants.ACTION_CREATE.equalsIgnoreCase(actionType)){
        doActionCreate();
      }
      else if(ShiftPatternConstants.ACTION_CREATE_ASSIGN.equalsIgnoreCase(actionType)){
        doActionCreate();
        this.newOvrValue = getNewOvrValue(spName, spOffset);
        doActionAssign();
      } else {
      	this.statusMessage += "<BR>Unknown Action Type.  No changes made.";
      }
      this.statusMessage += "<BR>Process complete.";
      logger.info("STATUS MESSAGE:\n" + this.statusMessage);
      return true;
    } // end processRequest

    public String getNotNull(String val, String defaultVal){
      if(val!=null) return val;
      else return defaultVal;
    } // end getNotNull

    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("Shift Pattern Resolver [toString()]:\n");
      sb.append("employeesNames=").append(employeesNames).append("\n");
      sb.append("teamName=").append(teamName).append("\n");
      sb.append("calcGroupName=").append(calcGroupName).append("\n");
      sb.append("payGroupName=").append(payGroupName).append("\n");
      sb.append("ovrIsPermanent=").append(ovrIsPermanent).append("\n");
      sb.append("ovrStart=").append(ovrStart).append("\n");
      sb.append("ovrEnd=").append(ovrEnd).append("\n");
      sb.append("existingShiftPatternName=").append(existingShiftPatternName).append("\n");
      sb.append("newShiftPatternName=").append(newShiftPatternName).append("\n");
      sb.append("newShiftPatternDesc=").append(newShiftPatternDesc).append("\n");
      sb.append("effectiveDate=").append(effectiveDate).append("\n");
      sb.append("allDays=").append(allDays.toString()).append("\n");
      return sb.toString();
    }

    public ShiftPatternResolverExtPlugin getInterface() 
    {
        //tries to retrieve the class name from the Registry
        try 
		{
            className = getClassNameFromRegistry();
            if (className == null)
            {	
               return null;
            }
        } 
        catch (Exception e) 
		{
        	logger.error(e);
        	return null;
        }
        //tries to load the class
        if (pluginClass == null) 
        {
            try 
			{                
            	pluginClass = Class.forName(className);
            } 
            catch(Exception e) 
			{                
                logger.error("Unable to load the class " + className, e);
                return null;
            }
        }
        //tries to instantiate the class
        if (pluginObject == null) 
        {
            try 
			{                
            	pluginObject = pluginClass.newInstance();
            } 
            catch (Exception ee) 
			{                
                logger.error("Unable to instantiate the class " + className, ee);
                return null;
            }
            if ((pluginObject instanceof ShiftPatternResolverExtPlugin) == false) 
            {                                    
                logger.error("The class loaded is not an implementation of the ShiftPatternResolverExtPlugin.");
                return null;
            }
        }
        return (ShiftPatternResolverExtPlugin)pluginObject;
    }    
        
    private String getClassNameFromRegistry() 
    	throws Exception 
	{
        Object obj = Registry.getVar("/system/customer/SHIFTPATTERNRESOLVEREXT_PLUGIN");
        if ((obj instanceof String) == false) 
        {
            return null;
        }
        return (String)obj;
    }
    
    public StringBuffer buildWhereClause(String column,
			Integer value1,
			Integer value2,
			Integer value3,
			Integer value4,
			Integer value5)
    {
        StringBuffer whereClause = null;
        
        whereClause = new StringBuffer();
        if(value1 != null)
        {
            whereClause.append(column + value1.intValue());
        }
        if(value2 != null)
        {
            whereClause.append(" OR ");
            whereClause.append(column + value2.intValue());
        }
        if(value3 != null)
        {
            whereClause.append(" OR ");
            whereClause.append(column + value3.intValue());
        }
        if(value4 != null)
        {
            whereClause.append(" OR ");
            whereClause.append(column + value4.intValue());
        }
        if(value5 != null)
        {
            whereClause.append(" OR ");
            whereClause.append(column + value5.intValue());
        }        
        return whereClause;
    }
    
    public ShiftData getShiftData(Integer shiftId, List shiftDataList)
    {
        ShiftData shiftData = null;
        Iterator itShiftData = null;
        boolean shiftFound = false;        
        
        itShiftData = shiftDataList.iterator();
        while(itShiftData.hasNext())
        {
            shiftData = (ShiftData)itShiftData.next();
            if(shiftId != null && shiftId.intValue() == shiftData.getShftId())
            {
                shiftFound = true;
                break;
            }
        }
        if(!shiftFound)
        {
            shiftData = new ShiftData();
        }
        
        return shiftData;
    }
    
    public ShiftBreakData getShiftBreakData(Integer shiftId, List shiftBreakDataList)
    {
        ShiftBreakData shiftBreakData = null;
        Iterator itShiftBreakData = null;
        boolean shiftBreakFound = false;        
        
        itShiftBreakData = shiftBreakDataList.iterator();
        while(itShiftBreakData.hasNext())
        {
            shiftBreakData = (ShiftBreakData)itShiftBreakData.next();
            if(shiftId != null && shiftId.intValue() == shiftBreakData.getShftId())
            {
                shiftBreakFound = true;
                break;
            }
        }
        if(!shiftBreakFound)
        {
            shiftBreakData = new ShiftBreakData();
        }
        
        return shiftBreakData;
    }    										
}
