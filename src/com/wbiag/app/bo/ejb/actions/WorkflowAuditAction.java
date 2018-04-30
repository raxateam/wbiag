/*
 * Created on Jun 9, 2005
 *
 */
package com.wbiag.app.bo.ejb.actions;

import java.sql.*;
import java.util.*;
import org.apache.log4j.*;

import com.workbrain.app.bo.BOFormInstance;
import com.workbrain.app.bo.ejb.actions.*;
import com.workbrain.app.workflow.*;
import com.workbrain.sql.*;


/**
 * Audit interaction form field and accept/reject actions
 * @author cxu
 */
public class WorkflowAuditAction extends AbstractActionProcess {
	
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkflowAuditAction.class);
    
    public static final String BUSOBJTYP_DESC = "BUSOBJ_DESC";
    public static final String BUSOBJ_CREATE_DATE = "BUSOBJ_CREATE_DATE";
    public static final String WBU_NAME = "WBU_NAME";
    public static final String CLIENT_ID = "CLIENT_ID";
    
    public ActionResponse processObject(Action data, WBObject object,
            Branch[] outputs, ActionResponse previous) 
    	throws WorkflowEngineException 
	{  
    	if (logger.isEnabledFor(Level.DEBUG)) {
    		logger.debug("\nWorkflowAuditAction starts");
    	}
    	String auditFormField = WorkflowUtil.getDataPropertyAsString(data , "AUDIT_FORM_FIELD", "TRUE");
    	String auditAction = WorkflowUtil.getDataPropertyAsString(data , "AUDIT_ACTION", "TRUE");
    	if (logger.isEnabledFor(Level.DEBUG)) {
	    	logger.debug("AUDIT_FORM_FIELD=" + auditFormField);
	    	logger.debug("AUDIT_ACTION=" + auditAction);
    	}   	
        BOFormInstance instance = (BOFormInstance) object;
        if (instance == null) {
            throw new WorkflowEngineException("Instance is null");
        }
        java.sql.Timestamp auditDate = new Timestamp(new java.util.Date().getTime()); 
    	DBConnection conn = this.getConnection();
    	try {
	    	Map busObjData = getBusObj(instance.getID(), conn);
	    	if ("TRUE".equalsIgnoreCase(auditFormField)) {
	    		auditFormField(data, object, outputs, previous, busObjData, conn, auditDate);
	    	}
	    	if ("TRUE".equalsIgnoreCase(auditAction)){
	    		auditAction(data, object, outputs, previous, busObjData, conn, auditDate);
	    	}
    	}finally {
    		SQLHelper.cleanUp(conn);
    	}
    	
    	if (logger.isEnabledFor(Level.DEBUG)) {
    		logger.debug("WorkflowAuditAction ends\n");
    	}
    	
    	return WorkflowUtil.createActionResponse(outputs, "Audited");
    }
    
    public void auditFormField(Action data, WBObject object, Branch[] outputs, 
    		                   ActionResponse previous, Map busObjData, 
							   DBConnection conn, Timestamp auditDate) 
    	throws WorkflowEngineException
    {
    	Map fieldMap = null;
    	Map field = null;
    	Map.Entry fieldEntry = null;
    	String fieldName = null;
    	String fieldValue = null;
    	String fieldValueMulti = null;
    	int empId = 0;
    	String strEmpId = null;
    	
        BOFormInstance instance = (BOFormInstance) object;
        if (instance == null) {
            throw new WorkflowEngineException("Instance is null");
        }
        
        StringTokenizer tokenizer = null;
        int tokenCount = 0;
        String formName = instance.getName();
        String formDesc = (String) busObjData.get(BUSOBJTYP_DESC);
        String creator = (String) busObjData.get(WBU_NAME);
        Timestamp createDate = (Timestamp) busObjData.get(BUSOBJ_CREATE_DATE);
        int busObjId = instance.getID();
        String userName = instance.getUserName();       
        fieldMap = instance.getNamesValuesMap();
        
        // If emp_id_field is specified and only one emp_id value in the field,
        // need to extract that emp_id and put into audit record
    	String empIdFldName = WorkflowUtil.getDataPropertyAsString(data , "EMP_ID_FIELD", "");
    	if (logger.isEnabledFor(Level.DEBUG)) {
    		logger.debug("EMP_ID_FIELD=" + empIdFldName);   
    	}
        if (empIdFldName != null) {
        	empIdFldName = empIdFldName.trim();
        	if (empIdFldName.length() != 0) {
        		field = (Map) fieldMap.get(empIdFldName);
        		if (field != null) {
        			fieldValue = (String) field.get("value");
        			if (fieldValue != null) {
        				tokenizer = new StringTokenizer(fieldValue, ",");
        				while (tokenizer.hasMoreTokens()) {
        					if (strEmpId != null) {
        						// more than one emp_id, do not audit
        						empId = 0;
        						break;
        					}
        					strEmpId = tokenizer.nextToken();
        					try {
        						empId = Integer.parseInt(strEmpId);
        					}catch(NumberFormatException e) {
        						// If it is not emp_id, it might be emp_name.
        						// Get emp_id from emp_name
        						empId = getEmpIdByEmpName(strEmpId, conn);
        					}
        				}
        			}
        		}
        	}
        	
        }
        
        
        // See if multi value delimiter is specified
    	String valueDelimiter = WorkflowUtil.getDataPropertyAsString(data , "MULTI_VALUE_DELIMITER", "");
    	if (logger.isEnabledFor(Level.DEBUG)) {
    		logger.debug("MULTI_VALUE_DELIMITER=" + valueDelimiter);
    	}
    	if (valueDelimiter != null) {
    		valueDelimiter = valueDelimiter.trim(); 
    	}
        
        // Loop through all fields, insert into workflow_audit_log table       
        Iterator iterator = fieldMap.entrySet().iterator();
        while (iterator.hasNext()) {
        	fieldEntry = (Map.Entry) iterator.next();
        	fieldName = (String) fieldEntry.getKey();       	
        	field = (Map) fieldEntry.getValue();
        	fieldValue = (String) field.get("value");
        	if (logger.isEnabledFor(Level.DEBUG)) {
        		logger.debug("field name=" + fieldName);
        		logger.debug(",  field value=");
	        	logger.debug(fieldValue==null?"null":fieldValue.toString()); 
        	}
        	
            // If multi value delimiter is specified, and each value is to be inserted as 
            // separate audit record
        	if (fieldValue != null) {
        		if (fieldValue.indexOf(valueDelimiter) > -1) {
	        		tokenizer = new StringTokenizer(fieldValue, valueDelimiter);
	        		tokenCount = 0;
	        		while (tokenizer.hasMoreTokens()) {
	        			tokenCount++;
	        			fieldValueMulti = (String) tokenizer.nextToken();
	    	        	// insert into audit table
	    	        	addAuditLog(formName, formDesc, busObjId, 
	    		                    creator, createDate, empId,
	    							fieldName+ "_" + tokenCount, fieldValueMulti, "", null,
	    						    auditDate, conn);
	        		}
        		} else {
    	        	addAuditLog(formName, formDesc, busObjId, 
		                    creator, createDate, empId,
							fieldName, fieldValue, "", null,
						    auditDate, conn);        			
        		}
        	} else {    	
	        	// insert into audit table
	        	addAuditLog(formName, formDesc, busObjId, 
		                    creator, createDate, empId,
							fieldName, fieldValue, "", null,
						    auditDate, conn);
        	}

        }

    }    
    
    
    public void auditAction(Action data, WBObject object, Branch[] outputs,
    		                ActionResponse previous, Map busObjData, 
							DBConnection conn, Timestamp auditDate) 
    	throws WorkflowEngineException
    {
        BOFormInstance instance = (BOFormInstance) object;
        if (instance == null) {
            throw new WorkflowEngineException("Instance is null");
        }

        String formName = instance.getName();
        String formDesc = (String) busObjData.get(BUSOBJTYP_DESC);
        String creator = (String) busObjData.get(WBU_NAME);
        Timestamp createDate = (Timestamp) busObjData.get(BUSOBJ_CREATE_DATE);
        int busObjId = instance.getID();        

        // if it is a validation node, not history yet
        if (busObjId == -1) {
        	return;
        }

        WorkflowProcessHistoryEntry wfHistEntry = null;
        List workflowHistory = null;
        String action = null;
        Timestamp actionDate = null;
        String actionUser = null;
        String comment = null;
        
        try {
        	workflowHistory = WorkflowUtil.getHistory(object);
        
        	/*
	        for (int i=0; i < workflowHistory.size(); i++) {
	        	wfHistEntry = (WorkflowProcessHistoryEntry) workflowHistory.get(i);
	        
		        if (wfHistEntry != null) {
		        	logger.debug("\nLast entry date =" + wfHistEntry.getEntryDate().toString());
		        	ActionResponse resp = wfHistEntry.getNodeResponse();
		        	logger.debug("Last Creator name=" + resp.getCreatorName());
		        	logger.debug("Last Comment=" + wfHistEntry.getComments());
		        	logger.debug("Last Routed From=" + resp.getRoutedFrom());
		        	logger.debug("Last Routed to=" + resp.getRoutedTo());
		        	logger.debug("Last Type=" + resp.getType());
		        	logger.debug("Last Value=" + resp.getValue());
		        	logger.debug("Last response=" + resp.toString());
		        }
	        
	        }
	        */
	        
	        if (workflowHistory.size() >= 2) {
	        	// last entry has action, action time and comment
	        	wfHistEntry = (WorkflowProcessHistoryEntry) workflowHistory.get(workflowHistory.size() - 1);
	        	action = wfHistEntry.getNodeResponse().getValue().toString();
	        	actionDate = new Timestamp(wfHistEntry.getEntryDate().getTime());
	        	comment = wfHistEntry.getComments();
	        	
	        	// User who performed action is in the second last node
	        	wfHistEntry = (WorkflowProcessHistoryEntry) workflowHistory.get(workflowHistory.size() - 2);
	        	actionUser = wfHistEntry.getNodeResponse().getCreatorName();
	        	addAuditLog(formName, formDesc, busObjId, 
		                creator, actionDate, 0,
						"COMMENT", comment, action, actionUser,
						auditDate, conn);
	        }
        } catch (Exception e) {
        	e.printStackTrace();
        }

 
    }
    
    public Map getBusObj(int busObjId, DBConnection conn) throws WorkflowEngineException
    {
    	Map busObjData = new HashMap();
		PreparedStatement stmt = null;
        ResultSet rs = null;
        String sqlStr = "SELECT BUSOBJTYP_DESC, BUSOBJ_CREATE_DATE, WBU_NAME" +
						" FROM BUSINESS_OBJECT, WORKBRAIN_USER, BUSINESS_OBJECT_TYPE" +
						" WHERE BUSINESS_OBJECT.WBU_ID_ORIGIN = WORKBRAIN_USER.WBU_ID" +
						  " AND BUSINESS_OBJECT.BUSOBJTYP_ID = BUSINESS_OBJECT_TYPE.BUSOBJTYP_ID" +
						  " AND BUSOBJ_ID = ?";

        try {
            stmt = conn.prepareStatement(sqlStr);
            stmt.setInt(1, busObjId);
            rs = stmt.executeQuery();
            if (rs.next())
            	busObjData.put(BUSOBJTYP_DESC, rs.getString(1));
            	busObjData.put(BUSOBJ_CREATE_DATE, rs.getTimestamp(2));
            	busObjData.put(WBU_NAME, rs.getString(3));
        } catch (Exception e) {
            throw new WorkflowEngineException(e);
        } finally {
            SQLHelper.cleanUp(rs);
            SQLHelper.cleanUp(stmt);
        }

        return busObjData;

    }
    
    
    public int getEmpIdByEmpName(String empName, DBConnection conn) throws WorkflowEngineException
    {
		PreparedStatement stmt = null;
        ResultSet rs = null;
        String sqlStr = "SELECT EMP_ID FROM EMPLOYEE WHERE EMP_NAME = ?";
        int empId = 0;

        try {
            stmt = conn.prepareStatement(sqlStr);
            stmt.setString(1, empName);
            rs = stmt.executeQuery();
            if (rs.next()) {
            	empId = rs.getInt(1);
            }
        } catch (Exception e) {
            throw new WorkflowEngineException(e);
        } finally {
            SQLHelper.cleanUp(rs);
            SQLHelper.cleanUp(stmt);
        }

        return empId;

    }
    
    
    public void addAuditLog(String formName, String formDesc, int busObjId, 
    		                String creator, Timestamp createDate, int empId,
							String fieldName, String fieldValue, String action, String actionUserName,
							Timestamp auditDate, DBConnection conn) throws WorkflowEngineException
    {
		PreparedStatement stmt = null;
        ResultSet rs = null;
        String sqlStr = "INSERT INTO WORKFLOW_AUDIT_LOG (wfad_id, wfad_form_name, wfad_form_desc, " +
		                "busobj_id, wfad_creator, wfad_create_date, emp_id, wfad_fld_name, " +
						"wfad_fld_value, wfad_action, wbu_name, wfad_audit_date)" +
						" VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";

        try {
        	int seq = conn.getDBSequence("SEQ_WFAD_ID").getNextValue();
            stmt = conn.prepareStatement(sqlStr);
            stmt.setInt(1, seq);
            stmt.setString(2, formName);
            stmt.setString(3, formDesc);
            stmt.setInt(4, busObjId);
            stmt.setString(5, creator);
            stmt.setTimestamp(6, createDate);
            stmt.setInt(7, empId);
            stmt.setString(8, fieldName);
            stmt.setString(9, fieldValue);
            stmt.setString(10, action);
            stmt.setString(11, actionUserName);
            stmt.setTimestamp(12, auditDate);
            
            int rows = stmt.executeUpdate();
            conn.commit();
        } catch (Exception e) {
            throw new WorkflowEngineException(e);
        } finally {
            SQLHelper.cleanUp(rs);
            SQLHelper.cleanUp(stmt);
        }

    }    
}
