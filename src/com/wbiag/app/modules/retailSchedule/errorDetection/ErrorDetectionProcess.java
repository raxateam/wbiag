package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.naming.Context;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.workbrain.app.modules.mvse.exception.MVSEException;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.tasks.CreateScheduleTask;
import com.workbrain.app.scheduler.enterprise.SchedulerConfigParams;
import com.workbrain.app.scheduler.enterprise.jndi.EnterpriseContext;
import com.workbrain.security.SecurityService;
import com.workbrain.server.registry.Registry;
import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SingletonSQLSource;
import com.workbrain.util.StringHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.WorkbrainSystem;

public class ErrorDetectionProcess {

	public static final String DEFAULT_DETECTION_REGISTRY_REG = "com.wbiag.app.modules.retailSchedule.errorDetection.DefaultErrorDetectionRegisty";
	private static Logger logger = Logger.getLogger(ErrorDetectionProcess.class);

    /**
     * Ensure that the required system properties are set to the correct
     * values.
     */
    static void setupEnvironment() {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                EnterpriseContext.ENTERPRISE_CONTEXT_FACTORY);
        // set the class name of the standalone Sequencer
        System.setProperty(SchedulerConfigParams.SEQUENCER_STANDALONE,
                SchedulerConfigParams.SEQUENCER_CLASS_NAME);
        System.setProperty("soap.enable", "true"); //for the old Registry
        // implementation

    }
    
    /**
     * Ensure that the log4j logging framework is configured.
     */
    static void setupLog4J() {
        if ( StringHelper.isEmpty(System.getProperty("log4j.configuration")) ) {
            BasicConfigurator.configure();
        } else {
            PropertyConfigurator.configure(
                    System.getProperty("log4j.configuration") );
        }
    }
    
    /**
     * Set up the connection to the database to be used by this task.
     *
     * @throws SQLException if there is a problem trying to obtain the database
     * connection
     */
    static DBConnection setupDBConnection() throws SQLException {
        Connection conn = ConnectionManager.getConnection();
        
        DBConnection result;
        if ( conn instanceof DBConnection ) {
            result = (DBConnection) conn;
        } else {
            result = new DBConnection(conn);
        }
        
        WorkbrainSystem.bindDefault(new SingletonSQLSource(result));
        
        DBInterface.init(result);

        return result;
    }
    
    private static String encodeEmpty( String str) {
    	if (StringHelper.isEmpty(str)){
    		return null;
    	}
    	return str;
    }
    
    /* Creates an instance of the validation registry that is specified in the registry
     * and returns. */
     private static ErrorDetectionRegisty getErrorDetectionRegistry() throws MVSEException {
    	 ErrorDetectionRegisty registry = null;
    	 String registryClass = null;
         try {
          	registryClass = Registry.getVarString(ErrorDetectionRegisty.DETECTION_REGISTRY_REG_PATH);
          } catch (Exception e) {
          	registryClass = null;
          }
         try {
        	 if (StringHelper.isEmpty(registryClass)){
        		 registry = ((ErrorDetectionRegisty)Class.forName(DEFAULT_DETECTION_REGISTRY_REG).newInstance());
        	 } else {
        		 registry = ((ErrorDetectionRegisty)Class.forName(registryClass).newInstance());
        	 }
         } catch (ClassNotFoundException cnfe) {
             logger.error("Unable to find ErrorDetectionRegisty class: " + cnfe.getMessage(),
                          cnfe);
             throw new MVSEException("Unable to find ErrorDetectionRegisty class: "
                     + cnfe.getMessage(), cnfe);
         } catch (IllegalAccessException iae) {
             logger.error("Access violation occured while creating ErrorDetectionRegisty: " 
                     + iae.getMessage(), iae);
             throw new MVSEException("Access violation occured while creating " +
                     "ErrorDetectionRegisty: " + iae.getMessage(), iae);
         } catch (InstantiationException ie)  {
             logger.error("Unable to create ErrorDetectionRegisty: " + ie.getMessage(), ie);
             throw new MVSEException("Unable to create ErrorDetectionRegisty: "+ ie.getMessage(), ie);
         }
         return registry;
     }
     
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception  {
        setupEnvironment();
        setupLog4J();
        DBConnection conn = setupDBConnection();
        DBConnection.setCurrentConnection(conn);
        
        String skdgrpId = "1000000356"; //args[0]; //"1000000161";
        String empSkdgrpId = "1000000067,1000000068,1000000069,1000000070,1000000071,1000000072"; //args[1]; //"1000000041";
        String date = "04/15/2007";//args[2];
        
        try {

            SecurityService.setCurrentClientId("1");            
            SecurityService.setCurrentUser(SecurityService.createSystemUser());
            
        	ErrorDetectionRegisty registy = getErrorDetectionRegistry();
        	registy.setSkdgrpId(skdgrpId);
        	registy.setEmpgrpId(empSkdgrpId);
        	registy.setEffDate(DateHelper.convertStringToDate(date, "MM/dd/yyyy"));
        	ArrayList checkTypes = new ArrayList();
        	ErrorDetectionEngine ede = new ErrorDetectionEngine();
        	ede.validate(registy);
        	
        	List errorList = registy.getErrorDetectionList();
        	StringBuffer passed = new StringBuffer();
        	StringBuffer failed = new StringBuffer();
        	
        	for(Iterator it = errorList.iterator(); it.hasNext();){
        		ErrorDetectionScriptResult edsr = (ErrorDetectionScriptResult) it.next();
        		
        		if (edsr.getMessage().equals("OK")){
        			passed.append(">>>>").append(edsr.getHelpTitle()).append("\n");
        		} else {
        			failed.append(">>>>").append(edsr.getHelpTitle()).append("\n")
        				.append(edsr.getErrorMsg()).append("\n")
        				.append(edsr.getMessage()).append("\n");
        		}

        	}
        	
        	System.out.println("***************************************************");
        	System.out.println("*     ERROR DETECTION PROCESS REPORT              *");
        	System.out.println("***************************************************");
        	System.out.println("Rules Evalulated:\n");
        	
        	String[] ruleList = registy.getRegisteredRuleNameArray();
        	for(int i=0; i < ruleList.length; i++){
        		System.out.println("-" +  ruleList[i] + "\n");
        	}
        	System.out.println("\n\n\n");
        	System.out.println("***************************************************");
        	System.out.println("*     Rules Passed                                *");
        	System.out.println("***************************************************");
        	System.out.println(passed.toString());
        	System.out.println("\n\n\n");
        	System.out.println("***************************************************");
        	System.out.println("*     Rules Failed                                *");
        	System.out.println("***************************************************");
        	System.out.println(failed.toString());
            
            conn.commit();			
        } finally {
            if(conn != null){
                conn.close();
            }
        }
		
	}


}
