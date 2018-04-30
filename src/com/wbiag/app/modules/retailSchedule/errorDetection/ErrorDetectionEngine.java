package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import com.workbrain.app.modules.mvse.exception.MVSEException;
import com.workbrain.app.modules.mvse.validator.ValidationEngine;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.sql.DBConnection;

public class ErrorDetectionEngine {

    private static Logger logger = Logger.getLogger(ValidationEngine.class);

    private static final String ERROR_DETECTION_REGISTRY_PATH = "/system/customers/modules/scheduleOptimization/errorDetection/";

    /**
     * Creates a new instance of <code>ErrorDetectionEngine</code>.
     */
    public ErrorDetectionEngine() {
    }
    
    public void validate(ErrorDetectionRegisty errorDetectionRegistry) throws Exception {

        // Execute validation process
        DBConnection conn = DBConnection.getCurrentConnection();
        CodeMapper codeMapper = getCodeMapper(conn);
        
        try {
	        // Validate the registered validation rules
	        List ruleList = errorDetectionRegistry.getRegisteredRuleList();
	        if (ruleList==null || ruleList.size()==0) {
	            logger.error("No detection rules provided to the detection engine.");
	            throw new MVSEException("No detection rules provided to the detection engine.");
	        }
	        
	        // Create context and validate rules
	        List detectionList = new ArrayList();
	        ErrorDetectionContext context = createContext(conn, errorDetectionRegistry.getSkdgrpId(),
	        		errorDetectionRegistry.getEmpgrpId(), errorDetectionRegistry.getEffDate());
	        
	        for (int i = 0; i<ruleList.size(); i++) {
	        	ErrorDetectionRule rule = (ErrorDetectionRule)ruleList.get(i);
	        	detectionList.add(rule.detect(context));
	         }
	  
	        // Filter violations by team security and store in registry
	        errorDetectionRegistry.setErrorDetectionListList(detectionList);
        } catch (Exception e){
        	if (logger.isEnabledFor(Level.ERROR)){
        		logger.error(e);
        	}
        	throw e;
        }
    }
    
    /* Builds the context for the registered rules. */
    private ErrorDetectionContext createContext(DBConnection conn, String skdgrpId, String empgrpId, Date startDate) {

    	ErrorDetectionContext context = new ErrorDetectionContext(conn, skdgrpId, empgrpId, startDate);
         
         return context;
    }
    

    /* Retrieves a CodeMapper instance. */
    private CodeMapper getCodeMapper(DBConnection conn) throws MVSEException {
        CodeMapper codeMapper = null;
        try {
            codeMapper = CodeMapper.createCodeMapper(conn);
        } catch (SQLException sqle) {
            logger.error("Unable to create CodeMapper.", sqle);
            throw new MVSEException("Unable to create CodeMapper.", sqle);
        }
        return codeMapper;
    }
}
