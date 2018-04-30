package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;


import com.workbrain.app.ta.db.WorkbrainRegistryAccess;
import com.workbrain.app.ta.model.WorkbrainRegistryData;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NameValue;
import com.workbrain.util.StringHelper;

abstract public class ErrorDetectionRegisty implements Serializable {

	private static Logger logger = Logger.getLogger(ErrorDetectionRegisty.class);
	
	public final static String DETECTION_REGISTRY_REG_PATH = "/system/customer/modules/scheduleOptimization/errorDetection/registryClassname";
	  
    private static final String DETECTION_RULES_FOLER_REG_PATH = "/system/customer/modules/scheduleOptimization/errorDetection/rules/";
    private static final String RULE_CLASSNAME_REGISTRY_NAME  = "classname";
    private static final String RULE_ENABLES_REGISTRY_NAME ="isEnabled";
    private static final String RULE_PARAMETERS_REGISTRY_NAME = "parameters";
    private static final String WBIAG_RULE_CLASS = "com.wbiag.";
   
    public static String [] standardRuleList = new String[] {"DashLicenseCheck",                                                                                                                          
    	"DefaultDistributionRule",                                                                                                                
    	"DuplicatePOSRule",                                                                  
    	"EmployeeAvailabilityRule",                                                          
    	"EmployeeGroupAssignmentRule",                                                       
    	"EmployeeJobHasSR",
    	"EmployeeNoStaffGroupRule",                                                          
    	"EmployeePreferredJobRule",                                                          
    	"EmployeeShiftPatternsRule",                                                         
    	"EmployeeSkillsRule",                                                                
    	"EmployeeSORecordRule",                                                              
    	"EmptyStaffGroupRule",                                                               
    	"FiscalYearMatchRule",                                                               
    	"ForecastMethodRule",                                                                
    	"HOPDetailsRule",                                                                    
    	"HOPEffDateRule",                                                                    
    	"HOPRules",                                                                          
    	"IntervalsRule",                                                                     
    	"JobRateIndexRule",                                                                  
    	"JobTeamDefRule",                                                                    
    	"LevelsRule",                                                                        
    	"LocationParentCheckRule",                                                           
    	"LocationPropertiesRule",                                                            
    	"MoselCodeRule",                                                                     
    	"NonVolumeStartEndTimesRule",                                                        
    	"NonZeroForecastRule",                                                               
    	"NumberofLocationsRule",                                                             
    	"ProductivityNumbersRule",                                                           
    	"RootNodeCheckRule",                                                                 
    	"ShiftinShiftParrernsRule",                                                          
    	"StaffingRequirementsRule",                                                          
    	"StartDayRule",                                                                      
    	"TeamHOPRule",                                                                       
    	"TeamParrentCheckRule",                                                              
    	"TempFilesDirRule",                                                                  
    	"VolumeProductivityTooLowRule",                                                      
    	"VolumeTypeRule",                                                                                                                       
    	"WBTeamStructureMatchRule"   };
    public static String standardRulePath = "com.wbiag.app.modules.retailSchedule.errorDetection.";
    
    protected int[] sortCriteria = new int[] {};
    
    private List registeredRuleList;
    private List errorDetectionList;
    private String skdgrpId = null;
    private String empgrpId = null;
    private Date effDate = null;
   
    
    /**
     * Creates a new instance of <code>ErrorDetectionRegisty</code> with the configured
     * error detection rules loaded.
     */
    public ErrorDetectionRegisty() {

    	ArrayList checkTypes = new ArrayList();
    	checkTypes.add(ErrorDetectionRule.BUG_TYPE);
    	checkTypes.add(ErrorDetectionRule.SYSTEM_TYPE);
    	checkTypes.add(ErrorDetectionRule.FORECAST_TYPE);
    	checkTypes.add(ErrorDetectionRule.SCHEDULE_TYPE);

        loadConfiguredRules(checkTypes);
    }

    public ErrorDetectionRegisty(ArrayList checkTypes) {

    	
        loadConfiguredRules(checkTypes);
    }
    
    public void filterConfiguredRules(ArrayList checkTypes){
    	
        List filteredRuleList = new ArrayList();;
    	for (Iterator it = registeredRuleList.iterator(); it.hasNext();){
    		ErrorDetectionRule rule = (ErrorDetectionRule) it.next();
        	if (rule!=null) {
        		Integer type = rule.getRuleType();
        		if (checkTypes.contains(type)){
        			filteredRuleList.add(rule);
        		}
            }
        }
    	registeredRuleList = filteredRuleList;
    }
    /**
     * Returns the list of registered rules.
     * 
     * @return The list of registered rules where each rule is modelled by a
     * <code>ValidationRule</code> object
     */
    public List getRegisteredRuleList() {
        return registeredRuleList;
    }

    /**
     * Returns the names of the registered rules sorted lexically.
     * 
     * @return The names of the registered rules sorted lexically
     */
    public String[] getRegisteredRuleNameArray() {
        String[] names = new String[registeredRuleList.size()];
        ErrorDetectionRule rule;
        for (int i = 0; i<registeredRuleList.size(); i++) {
            rule = (ErrorDetectionRule)registeredRuleList.get(i);
            names[i] = rule.getRuleName();
        }
        Arrays.sort(names);
        return names;
    }
    
    /**
     * Returns the names of the registered rules sorted lexically.
     * 
     * @return The names of the registered rules sorted lexically
     */
    public String[] getRegisteredRuleNameArray(Integer type) {
        List names = new ArrayList();
        ErrorDetectionRule rule;
        for (int i = 0; i<registeredRuleList.size(); i++) {
            rule = (ErrorDetectionRule)registeredRuleList.get(i);
            if (rule.getRuleType().equals(type)){
            	names.add(rule.getLocalizedRuleName());
            }
        }
        String[] ruleList = (String[]) names.toArray(new String[names.size()]);
        Arrays.sort(ruleList);
        return ruleList;
    }
    
    
    public Date getEffDate(){
    	return effDate;
    }
    public void setEffDate(Date d){
    	effDate = d;
    }
    public String getSkdgrpId(){
    	return skdgrpId;
    }
    
    public String getEmpgrpId(){
    	return empgrpId;
    }
    
    public void setSkdgrpId(String s){
    	 skdgrpId = s;
    }
    
    public void setEmpgrpId(String e){
    	 empgrpId = e;
    }
    public List getErrorDetectionList(){
    	return errorDetectionList;
    }
    
    /**
     * Sets the list of violations.
     * 
     * @param violationList The list of violations where each violation is modelled by a
     * <code>Violation</code> object
     */
    public void setErrorDetectionListList(List errorDetectionList) {
         this.errorDetectionList = errorDetectionList;
    }
 
    /**
     * Sorts the violation list based on the current sort criteria.
     */
    public void sortViolationList() {
        if (errorDetectionList!=null && errorDetectionList.size()>0) {
        	ErrorDetectionComparator comparator = new ErrorDetectionComparator(sortCriteria);
            Collections.sort(errorDetectionList, comparator);
        }
    }
    
    /* Loads the error detection rules configured in the registry. */
    private void loadConfiguredRules(ArrayList checkTypes) {
        // Build the "WHERE" clause for retrieving the regisry folders under the "rules"
        // folder. The is currently no utility to retrieve all folders under a specific
        // folder in the registry, so using a query. The query does do many self-joins but
        // the WORKBRAIN_REGISTRY table is extremely small and indexed so there is no
        // performance implicaitons. Once a utility is provided, this query should be removed
        // and the utility should be used.
        String [] folders = new String[] {"customer", "modules", "scheduleOptimization", "errorDetection", "rules"};
        StringBuffer whereSB = new StringBuffer("wbreg_class IS NULL AND wbreg_parent_id=");
        for (int i = folders.length - 1; i>=0; i--) {
            whereSB.append("(SELECT wbreg_id FROM ").append(WorkbrainRegistryAccess.REGISTRY_TABLE);
            whereSB.append(" WHERE wbreg_name='").append(folders[i]);
            whereSB.append("' AND wbreg_class IS NULL AND wbreg_parent_id =");
        }
        whereSB.append("(SELECT wbreg_id FROM ").append(WorkbrainRegistryAccess.REGISTRY_TABLE);
        whereSB.append(" WHERE wbreg_name ='system' AND wbreg_class IS NULL AND wbreg_parent_id=0)");
        for (int j = 0; j<folders.length; j++) {
            whereSB.append(")");
        }
        // Load the list of rules configured in the workbrain registry
        WorkbrainRegistryAccess access = new WorkbrainRegistryAccess(DBConnection.getCurrentConnection());
        List ruleList = access.loadRecordData(new WorkbrainRegistryData(), WorkbrainRegistryAccess.REGISTRY_TABLE,
                                              whereSB.toString());
        // Instantiate and register configured rules
        registeredRuleList = new ArrayList();
        //Add core rules
        for (int i = 0; i < standardRuleList.length; i++){
        	String className = standardRulePath + standardRuleList[i];
        	String parameters = null;
        	ErrorDetectionRule rule = instanstiateRule(className, parameters);
        	if (rule!=null) {
        		Integer type = rule.getRuleType();
        		if (checkTypes.contains(type)){
        			registeredRuleList.add(instanstiateRule(className, parameters));
        		}
            }
        }
        //Add custom rules
        for (int i = 0; i<ruleList.size(); i++) {
            WorkbrainRegistryData registryData = (WorkbrainRegistryData)ruleList.get(i);
            String path = DETECTION_RULES_FOLER_REG_PATH + registryData.getWbregName() + "/";
            if (Registry.getVarBoolean(path + RULE_ENABLES_REGISTRY_NAME, false)) {
                String className = Registry.getVarString(path + RULE_CLASSNAME_REGISTRY_NAME, "");
                if (!className.startsWith(standardRulePath)){
	                String parameters = Registry.getVarString(path + RULE_PARAMETERS_REGISTRY_NAME, "");
	                ErrorDetectionRule rule = instanstiateRule(className, parameters);
	                if (rule!=null) {
	                	Integer type = rule.getRuleType();
	                	if (checkTypes.contains(type)){
	                		registeredRuleList.add(instanstiateRule(className, parameters));
	                	}
	                }
                }
            }
        }
    }
    
    private final ErrorDetectionRule instanstiateRule(String className, String parameters) {
        // Build the map of parameters for the rule
        Map parameterMap = buildParameterMap(parameters);
        // Instanstiate the rule
        ErrorDetectionRule rule = null;        
        rule = createErrorDetectionRule(className, parameterMap);

        return rule;
    }
    
    /* Builds the map of parameters for a rule. */
    private Map buildParameterMap(String parameters) {
        Map parameterMap = new HashMap();
        if (!StringHelper.isEmpty(parameters)) {
            StringHelper.detokenizeStringAsNameValueList(parameters, ",", "=", true);
            List parameterList = StringHelper.detokenizeStringAsNameValueList(parameters, ",",
                                                                              "=", true);
            for (int i = 0; i<parameterList.size(); i++) {
               NameValue parameter = (NameValue)parameterList.get(i);
               parameterMap.put(parameter.getName().toUpperCase().trim(),
                                parameter.getValue().trim());
            }
        }
        return parameterMap;
    }
    
    private final ErrorDetectionRule createErrorDetectionRule(String className, Map parameters)  {
    	
    	ErrorDetectionRule newRule = null;
    	
    	try {
    		newRule = (ErrorDetectionRule)Class.forName(className).newInstance();
    	} catch (Exception e){
    		if (logger.isEnabledFor(Level.WARN)){
    			logger.warn("Could not load rule: " + className, e);
    		}
    	}
    	
    	return newRule;
    }
}
