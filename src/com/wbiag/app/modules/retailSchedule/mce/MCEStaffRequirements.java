package com.wbiag.app.modules.retailSchedule.mce;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import com.workbrain.server.data.AccessException;
import com.workbrain.server.data.BasicRow;
import com.workbrain.server.data.FieldType;
import com.workbrain.server.data.NotValidDataException;
import com.workbrain.server.data.Row;
import com.workbrain.server.data.RowCursor;
import com.workbrain.server.data.RowDefinition;
import com.workbrain.server.data.sql.QueryRowSource;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.DBSequence;
import com.workbrain.util.StringHelper;
import com.workbrain.app.modules.retailSchedule.model.*;
import com.workbrain.app.ta.db.CodeMapper;


public class MCEStaffRequirements extends MCEConfiguration{
    public static final String TASK_NAME = "TaskName";
    public static final String TASK_NAME_PREFIX = "taskNamePrefix";
    public static final String START_DATE = "StartDate";
    public static final String END_DATE = "EndDate";
    public static final String ACTIVITY = "Activity";
    public static final String JOB = "Job";
    public static final String SKILL = "Skill";
    public static final String SKILL_LEVEL = "SkillLevel";
    public static final String MIN_REQ = "MinReq";
    public static final String MAX_REQ = "MaxReq";
    public static final String SHIFT_START_OFF = "ShiftStartOffset";
    public static final String SHIFT_END_OFF = "ShiftEndOffset";
    private static final String REQ_TYPE = "REQ_TYPE";
    private static final String VOLUME_DRIVEN = "volume-driven";
    private static final String NON_VOLUME_DRIVEN = "Non-volume-driven";
    private static final String DEPT_IDENTIFIER = "-DEPT";

    private static final String AUDIT_SQL = "INSERT INTO audit_log (audlog_key_id, audlog_old_value, audlog_new_value,"
            + "audlog_id, wbu_name, wbu_name_actual, audlog_change_date, audlog_tablename, audlog_action,"
            + "audlog_fieldname) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";

    public static Logger logger = Logger.getLogger(MCEStaffRequirements.class);
    private HttpServletRequest request;
    private PreparedStatement ps;
    Statement stmt;
    Integer counter = new Integer(0);

    public MCEStaffRequirements(HttpServletRequest requestObj){
       super(requestObj);
       request = null;
       stmt = null;
    }

    public void UpdateStaffingRequirements(DBConnection conn,
            HttpServletRequest requestObj) throws Exception{
    	
        request = requestObj;
        counter = (!"".equals(request.getParameter("counter"))) ? new Integer(
                request.getParameter("counter")) : new Integer(-1);

        boolean checked = false;

        try{
            conn.setAutoCommit(false);
            stmt = conn.createStatement(); 
            ps = conn.prepareStatement(AUDIT_SQL);
            // for each staffing requirement, loop through and check if anything
            // was edited
            for ( int i = 1; i <= counter.intValue(); i++ ){
               if ( "Y".equals(request.getParameter("chkbx" + i)) ){
                    // a staffing requirement was changed, locate what field and
                    // create a sql statement
                	
                    checked = createUpdateStatements(i, conn);         
                    // if any statements were found, add to global list
               }
            }
            if ( checked == true ){           	
            	stmt.executeBatch();//updating data
            	ps.executeBatch();  //auditing
                conn.commit();  
            }
        }
        catch ( Exception e ){
        	//System.out.println(e.toString()); 
           if ( logger.isDebugEnabled() ){
              logger.debug(e.toString());                      
           }
           throw new Exception(
                    "MCEStaffRequirments: Edits could not be processed");
        }
        finally {
           if ( stmt != null ){
              stmt.close();
           }
           if ( ps != null ){
              ps.close();
           }
        }
    }

    private boolean createUpdateStatements(int count, DBConnection conn)
            throws Exception {
        StringBuffer UpdateSQL = new StringBuffer("UPDATE SO_CLIENT_STFDEF SET"); 
        boolean chked = false;
        boolean retVal = false;
        String sCount = String.valueOf(count);
        List rowDataList = new ArrayList();
        
        // check all checkboxes from the staffing requirements
        if ( "Y".equals(request.getParameter("chkbx_1_" + sCount)) ){
            // update query
            UpdateSQL.append(" CSD_DESC = \'");
            UpdateSQL.append(request.getParameter(TASK_NAME_PREFIX + sCount) + request.getParameter(TASK_NAME + sCount)).append("\'");
           
            // update new row
            rowDataList.add(new rowData("CSD_DESC", request.getParameter(TASK_NAME_PREFIX + sCount) + request.getParameter( TASK_NAME + sCount ) ) );
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_2_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" CSD_EFF_START_DATE = to_date(\'");
            UpdateSQL.append(request.getParameter(START_DATE + sCount)).append(
                    "\',\'yyyyMMdd HH24miss\')");

            // update new row
            rowDataList.add(new rowData("CSD_EFF_START_DATE", request
                    .getParameter(START_DATE + sCount)));
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_3_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" CSD_EFF_END_DATE = to_date(\'");
            UpdateSQL.append(request.getParameter(END_DATE + sCount)).append(
                    "\',\'yyyyMMdd HH24miss\')");

            // update new row
            rowDataList.add(new rowData("CSD_EFF_END_DATE", request
                    .getParameter(END_DATE + sCount)));
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_4_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" ACT_ID = ");
            UpdateSQL.append(request.getParameter(ACTIVITY + sCount));

            // update new row
            rowDataList.add(new rowData("ACT_ID", request.getParameter(ACTIVITY
                    + sCount)));
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_5_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" JOB_ID = ");
            UpdateSQL.append(request.getParameter(JOB + sCount));

            // update new row
            rowDataList.add(new rowData("JOB_ID", request.getParameter(JOB
                    + sCount)));
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_6_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" STSKL_ID = ");
            UpdateSQL.append(request.getParameter(SKILL + sCount));

            // update new row
            rowDataList.add(new rowData("STSKL_ID", request.getParameter(SKILL
                    + sCount)));
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_7_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" SKILL_LEVEL = ");
            UpdateSQL.append(request.getParameter(SKILL_LEVEL + sCount));

            // update new row
            rowDataList.add(new rowData("SKILL_LEVEL", request
                    .getParameter(SKILL_LEVEL + sCount)));
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_8_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" CSD_MIN_REQ = ");
            UpdateSQL.append(request.getParameter(MIN_REQ + sCount));

            // update new row
            rowDataList.add(new rowData("CSD_MIN_REQ", request
                    .getParameter(MIN_REQ + sCount)));
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_9_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" CSD_MAX_REQ = ");
            UpdateSQL.append(request.getParameter(MAX_REQ + sCount));

            // update new row
            rowDataList.add(new rowData("CSD_MAX_REQ", request
                    .getParameter(MAX_REQ + sCount)));
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_10_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" CSD_SHFTSTR_OFS = ");
            UpdateSQL.append(request.getParameter(SHIFT_START_OFF + sCount));

            // update new row
            rowDataList.add(new rowData("CSD_SHFTSTR_OFS", request
                    .getParameter(SHIFT_START_OFF + sCount)));
            chked = true;
        }
        if ( "Y".equals(request.getParameter("chkbx_11_" + sCount)) ){
            if ( chked == true ){
                UpdateSQL.append(",");
            }
            UpdateSQL.append(" CSD_SHFTEND_OFS = ");
            UpdateSQL.append(request.getParameter(SHIFT_END_OFF + sCount));

            // update new row
            rowDataList.add(new rowData("CSD_SHFTEND_OFS", request
                    .getParameter(SHIFT_END_OFF + sCount)));
            chked = true;
        }
        if ( chked == true ){
            retVal = true;
            UpdateSQL.append(" WHERE CSD_ID = ");
            UpdateSQL.append(request.getParameter("csdId" + sCount));
            stmt.addBatch(UpdateSQL.toString());

            // add audit querys
            String tableName = "SO_CLIENT_STFDEF";
            QueryRowSource rs = getQueryRowSource(conn, tableName, "CSD_ID",
                    Integer.parseInt(request.getParameter("csdId" + sCount)),
                    false);
            RowCursor rc = rs.queryAll();
            Row newRow = null;
            oldRows.clear();
            newRows.clear();
            while ( rc.fetchRow() ){
               oldRows.add(new BasicRow(rc));
               newRow = new BasicRow(rc);
               for ( int z = 0; z < rowDataList.size(); z++ ){
                    newRow.setValue(((rowData) rowDataList.get(z)).fieldName,
                            ((rowData) rowDataList.get(z)).fieldValue);
                    newRows.add(newRow);
               }
            }
            rc.close();
            
            for ( int i = 0; i < oldRows.size(); i++ ){
               addAuditToBatch(conn, tableName, UPDATE, (Row) oldRows.get(i),
                        (Row) newRows.get(i));
            }     
        }

        Integer driverCounter = (!"".equals(request
                .getParameter("driverCounter" + sCount))) ? new Integer(request
                .getParameter("driverCounter" + sCount)) : new Integer(-1);
        rowDataList = null;
        rowDataList = new ArrayList();
        
        // now check drivers corresponding to this
        for ( int i = 1; i <= driverCounter.intValue(); i++ ){
            String sI = String.valueOf(i);
            UpdateSQL = null;
            
            //update so_volume_workload here
            UpdateSQL = new StringBuffer("UPDATE SO_VOLUME_WORKLOAD SET ");
            rowDataList = new ArrayList();
            chked = false;
            
            StringBuffer deleteSql = new StringBuffer("DELETE FROM SO_VOLUME_WORKLOAD ");
            boolean deleteChked = false;
            
            if ( "Y".equals(request.getParameter("chkbx1" + sCount + sI)) ){
               UpdateSQL.append(" WRKLD_DESC = \'");
               UpdateSQL.append(request.getParameter("Description" + sCount + sI)).append("\'");
                
                // update new row
                rowDataList.add(new rowData("WRKLD_DESC", request.getParameter("Description" + sCount + sI)));
                chked = true;
                
            }
            if ( "Y".equals(request.getParameter("chkbx2" + sCount + sI)) ){
                if ( chked == true ){
                    UpdateSQL.append(",");
                }
                UpdateSQL.append(" SKDGRP_ID = ");
                UpdateSQL.append(request.getParameter("DriverName" + sCount + sI));

                // update new row
                rowDataList.add(new rowData("SKDGRP_ID", request.getParameter("DriverName" + sCount + sI)));
                chked = true;
            }
            
            if ( "Y".equals(request.getParameter("chkbx3" + sCount + sI)) ){
               if ( chked == true ){
                    UpdateSQL.append(",");
               }
                UpdateSQL.append(" WRKLD_STDVOL_HOUR = ");
                UpdateSQL.append(request.getParameter("Productivity" + sCount + sI));

                // update new row
                rowDataList.add(new rowData("WRKLD_STDVOL_HOUR", request.getParameter("Productivity" + sCount + sI)));
                chked = true;
            }
            
            if ( "Y".equals(request.getParameter("chkbx4" + sCount + sI)) ){
               if ( chked == true ){
                    UpdateSQL.append(",");
               }
                UpdateSQL.append(" DIST_ID = ");
                UpdateSQL.append(request.getParameter("Distribution" + sCount + sI));

                // update new row
                rowDataList.add(new rowData("DIST_ID", request.getParameter("Distribution" + sCount + sI)));
                chked = true;
            }
            
            if ( chked == true ){
                retVal = true;
                // if anything was checked, add statement to list
                UpdateSQL.append(" WHERE WRKLD_ID = ");
                UpdateSQL.append(request.getParameter("wrkldId" + sCount + sI));
                stmt.addBatch(UpdateSQL.toString());
                
                // now add audit sql statement
                // add audit querys
                String tableName = "SO_VOLUME_WORKLOAD";
                QueryRowSource rs = getQueryRowSource(conn, tableName,
                        "WRKLD_ID", Integer.parseInt(request.getParameter("wrkldId" + sCount + sI)), false);
                RowCursor rc = rs.queryAll();
                Row newRow = null;
                oldRows.clear();
                newRows.clear();
                while ( rc.fetchRow() ){
                   oldRows.add(new BasicRow(rc));
                   newRow = new BasicRow(rc);
                   for ( int z = 0; z < rowDataList.size(); z++ ){
                   	newRow.setValue(
                                ((rowData) rowDataList.get(z)).fieldName,
                                ((rowData) rowDataList.get(z)).fieldValue);
                        newRows.add(newRow);
                   }  
                }
                rc.close();
                for ( int z = 0; z < oldRows.size(); z++ ){
                   addAuditToBatch(conn, tableName, UPDATE, (Row) oldRows.get(z), (Row) newRows.get(z));
                }
            }
            
            //Deleting workload record..
            if ( "Y".equals(request.getParameter("chkbx5" + sCount + sI)) ){
               deleteChked = true;
               deleteSql.append(" WHERE WRKLD_ID = ");
               deleteSql.append(request.getParameter("wrkldId" + sCount + sI));
            }
            if ( deleteChked == true ){	
               stmt.addBatch(deleteSql.toString());
            }
        }
        
        //adding new volume workload
        boolean newWorkLoad = false;
        if ( "Y".equals(request.getParameter("chkbxNew_1_" + sCount)) ){
        	newWorkLoad = true;	
        }
        if (newWorkLoad) {
           //insertObject iObj = new insertObject();
           StringBuffer UpdateSQLFields = new StringBuffer(
            "INSERT INTO SO_VOLUME_WORKLOAD (WRKLD_ID,CSD_ID");
           int wrkldId = conn.getDBSequence("SEQ_WRKLD_ID").getNextValue();
            
           StringBuffer UpdateSQLEnd = new StringBuffer(") VALUES (")
		   .append(String.valueOf(wrkldId)).append(",")
           .append(request.getParameter("csdId" + sCount));
            if ( "Y".equals(request.getParameter("chkbxNew_2_" + sCount )) ){
               UpdateSQLFields.append(", SKDGRP_ID");
               UpdateSQLEnd.append("," + request.getParameter("DriverName" + sCount));
            }
            if ( "Y".equals(request.getParameter("chkbxNew_3_" + sCount)) ){
               UpdateSQLFields.append(", WRKLD_STDVOL_HOUR");
               UpdateSQLEnd.append("," + request.getParameter("Productivity" + sCount));
            }
            if ( "Y".equals(request.getParameter("chkbxNew_4_" + sCount)) ){
               UpdateSQLFields.append(", DIST_ID");
               UpdateSQLEnd.append("," + request.getParameter( "Distribution" + sCount ) );
            }
            if ( "Y".equals(request.getParameter("chkbxNew_1_" + sCount)) ){
               UpdateSQLFields.append(", WRKLD_DESC");
               UpdateSQLEnd.append(",\'" + request.getParameter("Description" + sCount ) + "\'");
            }
            StringBuffer finalSQL = UpdateSQLFields.append(UpdateSQLEnd).append(")");
            stmt.addBatch(finalSQL.toString());
        } 
        return retVal;
    }

    public void AddStaffingRequirements(DBConnection conn,
            HttpServletRequest requestObj) throws Exception {

        request = requestObj;
        //PreparedStatement ps = null;
        try{
           stmt = conn.createStatement();
           conn.setAutoCommit(false);

           String skdgrpList = (!"".equals(request
                .getParameter("selected_skdgrp_list"))) ? new String(request
                .getParameter("selected_skdgrp_list")) : "";
        
        if ( !StringHelper.isEmpty(skdgrpList) ){
            StringTokenizer st = new StringTokenizer(skdgrpList, ",");
            List insertedObjectList = new ArrayList();

            while ( st.hasMoreTokens() ) {
                insertObject iObj = new insertObject();
                String skdgrpId = st.nextToken();
                StringBuffer UpdateSQLFields = new StringBuffer(
                        "INSERT INTO SO_CLIENT_STFDEF (CSD_ID, SKDGRP_ID");
                StringBuffer UpdateSQLMiddle = new StringBuffer(") VALUES (");
                StringBuffer UpdateSQLEnd = new StringBuffer().append(skdgrpId);
                if ( "Y".equals(request.getParameter("chkbx1")) ){
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", CSD_DESC");
                    UpdateSQLEnd.append(", \'"
                            + getCsdDesc( conn, request.getParameter(TASK_NAME), skdgrpId ) + "\'");
                }
                
                //adding staff requirement type
                int nonVlmFlag = -1;
                String reqType = (String)request.getParameter(REQ_TYPE);  
                if( VOLUME_DRIVEN.equals(reqType)) {
                	nonVlmFlag = 1;	
                }
                else if ( NON_VOLUME_DRIVEN.equals( reqType ) ) {
                	nonVlmFlag = 0;	
                }
                if (nonVlmFlag != -1) {
                   UpdateSQLFields.append(", CSD_NONVLM_FLAG");
                   UpdateSQLEnd.append(", " + nonVlmFlag );
                }
                
                if ( "Y".equals(request.getParameter("chkbx2")) ){
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", CSD_EFF_START_DATE");
                    UpdateSQLEnd.append(", to_date(\'").append(
                            request.getParameter(START_DATE)).append(
                            "\',\'yyyyMMdd HH24miss\')");
                }
                if ( "Y".equals(request.getParameter("chkbx3")) ){
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", CSD_EFF_END_DATE");
                    UpdateSQLEnd.append(", to_date(\'").append(
                            request.getParameter(END_DATE)).append(
                            "\',\'yyyyMMdd HH24miss\')");
                }
                if ( "Y".equals(request.getParameter("chkbx4")) )
                {
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", ACT_ID");
                    UpdateSQLEnd.append(", " + request.getParameter(ACTIVITY));
                }
                if ( "Y".equals(request.getParameter("chkbx5")) )
                {
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", JOB_ID");
                    UpdateSQLEnd.append(", " + request.getParameter(JOB));
                }
                if ( "Y".equals(request.getParameter("chkbx6")) )
                {
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", STSKL_ID");
                    UpdateSQLEnd.append(", " + request.getParameter(SKILL));
                }
                if ( "Y".equals(request.getParameter("chkbx7")) )
                {
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", SKILL_LEVEL");
                    UpdateSQLEnd.append(", "
                            + request.getParameter(SKILL_LEVEL));
                }
                if ( "Y".equals(request.getParameter("chkbx8")) )
                {
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", CSD_MIN_REQ");
                    UpdateSQLEnd.append(", " + request.getParameter(MIN_REQ));
                }
                if ( "Y".equals(request.getParameter("chkbx9")) )
                {
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", CSD_MAX_REQ");
                    UpdateSQLEnd.append(", " + request.getParameter(MAX_REQ));
                }
                if ( "Y".equals(request.getParameter("chkbx10")) )
                {
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", SHFTSTR_ID");
                    UpdateSQLEnd.append(", "
                            + request.getParameter(SHIFT_START_OFF));
                }
                if ( "Y".equals(request.getParameter("chkbx11")) )
                {
                    iObj.StaffRecChecked = true;
                    UpdateSQLFields.append(", SHFTEND_ID");
                    UpdateSQLEnd.append(", "
                            + request.getParameter(SHIFT_END_OFF));
                }

                if ( iObj.StaffRecChecked == true ){
                    // create full sql statement
                    // first get sequence for later use
                    DBSequence curSeq = conn.getDBSequence("SEQ_CSD_ID");
                    iObj.seqNum = curSeq.getNextValue();

                    StringBuffer finalSQL = UpdateSQLFields.append(UpdateSQLMiddle).append(String.valueOf(iObj.seqNum))
                        .append(",").append(UpdateSQLEnd).append(")");
                    stmt.addBatch(finalSQL.toString());
                   
                    // now check and add new volume workload
                    if ( "Y".equals(request.getParameter("chkbxNew1")) && 
                    	 "Y".equals(request.getParameter("chkbxNew2")) && 
						 "Y".equals(request.getParameter("chkbxNew3"))){
                       iObj.DriverChecked = true;
                    }
                    
                    if ( iObj.DriverChecked == true ) {
                        UpdateSQLFields = null;
                        UpdateSQLEnd = null;
                      
                        UpdateSQLFields = new StringBuffer(
                        "INSERT INTO SO_VOLUME_WORKLOAD (WRKLD_ID,CSD_ID");
                        iObj.wrkldId = conn.getDBSequence("SEQ_WRKLD_ID")
                        .getNextValue();
                        
                        UpdateSQLEnd = new StringBuffer(") VALUES (").append(
                                String.valueOf(iObj.wrkldId)).append(",")
                                .append(String.valueOf(iObj.seqNum));
                        
                        if ( "Y".equals(request.getParameter("chkbxNew2")) ){
                            UpdateSQLFields.append(", SKDGRP_ID");
                            UpdateSQLEnd.append(","
                                    + request.getParameter("DriverName"));
                        }
                        if ( "Y".equals(request.getParameter("chkbxNew3")) ){
                            UpdateSQLFields.append(", WRKLD_STDVOL_HOUR");
                            UpdateSQLEnd.append(","
                                    + request.getParameter("Productivity"));
                        }
                        if ( "Y".equals(request.getParameter("chkbxNew4")) ){
                            UpdateSQLFields.append(", DIST_ID");
                            UpdateSQLEnd.append(","
                                    + request.getParameter("Distribution"));
                        }
                        if ( "Y".equals(request.getParameter("chkbxNew1")) ){
                            UpdateSQLFields.append(", WRKLD_DESC");
                            UpdateSQLEnd.append(",\'"
                                    + request.getParameter("Description") + "\'");
                        }
                        finalSQL = UpdateSQLFields.append(UpdateSQLEnd).append(")");                      
                        stmt.addBatch(finalSQL.toString());
                    }
                }
                insertedObjectList.add(iObj);
            }
            stmt.executeBatch();
            conn.commit();

            // perform audit 
            for ( int x = 0; x < insertedObjectList.size(); x++ ){
                insertObject iObj = (insertObject) insertedObjectList.get(x);
                if ( iObj.StaffRecChecked == true ){
                    auditInsert(conn, "SO_CLIENT_STFDEF", "CSD_ID", iObj.seqNum);
                }
                if ( iObj.DriverChecked == true ){
                	auditInsert(conn, "SO_VOLUME_WORKLOAD", "WRKLD_ID",iObj.wrkldId);
                }
            }
            
        }
        }catch ( Exception e ){
        	//System.out.println(e.toString()); 
            if ( logger.isDebugEnabled() ){
               logger.debug(e.toString());                      
            }
            throw new Exception(
                     "MCEStaffRequirments: Edits could not be processed");
         }
         finally {
            if ( stmt != null ){
               stmt.close();
            }
         }
    }

    /*
     * This method adds audit_log entries for insert,deletes and updates Insert:
     * new row Delete: old row Update: new and old row
     */
    
    public void addAuditToBatch(DBConnection conn, String tableName, int type,
            Row oldRow, Row newRow) throws Exception{
       String op;
       String opLetter;
       RowDefinition rd;
       
       try{
          if ( newRow != null )
             rd = newRow.getRowDefinition();
          else
             rd = oldRow.getRowDefinition();

            switch ( type ){
            case MCEConfiguration.UPDATE:
                op = "UPDATE";
                opLetter = "U";
                break;
            case MCEConfiguration.DELETE:
                op = "DELETE";
                opLetter = "D";
                break;
            case MCEConfiguration.INSERT:
                op = "INSERT";
                opLetter = "I";
                break;
            default:
                return;
            }
            
            // public static final String AUDIT_SQL = "INSERT INTO audit_log (
            // 1: audlog_id //2: wbu_name
            // 3: wbu_name_actual //4: audlog_change_date
            // 5: audlog_tablename //6: audlog_action
            // 7: audlog_key_i //8: audlog_fieldname
            // 9: audlog_old_value //10: audlog_new_value

            for ( int i = 0; i < rd.getRowSize(); i++ ){
                FieldType ft = rd.getFieldType(i);

                if ( type == MCEConfiguration.INSERT ){
                    ps.setString(1, newRow.getValue(rd.getKeyField())
                            .toString()); // key id
                    ps.setString(2, op); // old value
                    ps.setString(3, getAuditValue(rd.getFieldType(i), newRow
                            .getValue(i), false)); // new value
                }
                else if ( type == MCEConfiguration.DELETE ){
                    try{
                        ft.convert(oldRow.getValue(i));
                    }
                    catch ( NotValidDataException e ){
                        continue;
                    }
                    ps.setString(1, oldRow.getValue(rd.getKeyField())
                            .toString()); // key id
                    ps.setString(2, getAuditValue(rd.getFieldType(i), oldRow
                            .getValue(i), false)); // old value
                    ps.setString(3, op); // new value
                }
                else if ( type == MCEConfiguration.UPDATE ){
                    try{
                        ft.convert(oldRow.getValue(i));
                    }
                    catch ( NotValidDataException e ){
                        continue;
                    }
                    if ( oldRow.getValue(i) == null
                            && newRow.getValue(i) == null )
                        continue;
                    if ( oldRow.getValue(i) != null )
                        if ( oldRow.getValue(i).equals(newRow.getValue(i)) )
                            continue;

                    ps.setString(1, oldRow.getValue(rd.getKeyField())
                            .toString()); // key id
                    ps.setString(2, getAuditValue(rd.getFieldType(i), oldRow
                            .getValue(i), false)); // old value
                    ps.setString(3, getAuditValue(rd.getFieldType(i), newRow
                            .getValue(i), false)); // new value
                }
                
                ps.setInt(4, conn.getDBSequence("seq_audlog_id").getNextValue());
                ps.setString(5, userName);
                ps.setString(6, actualUserName);
                ps.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                ps.setString(8, tableName);
                ps.setString(9, opLetter);
                ps.setString(10, rd.getName(i));
                ps.addBatch();
            }
        }catch ( Exception e ){ 
        	//System.out.println(e.toString());
           if ( logger.isDebugEnabled() ){
                logger.debug("Could not get audit sql statement");
           }
              throw new Exception("Could not get audit sql Statement");
        }
    }
    
    //Method returns csd_desc based on taskName and skdGrpId
    private String getCsdDesc( DBConnection conn, String taskName, String skdGrpId ) {
       String csdDesc = null;
       String skdGrpName = null;
       try {
          CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
          ScheduleGroupData skdgrpData = codeMapper.getScheduleGroupById(Integer.parseInt(skdGrpId));
          if (skdgrpData != null) {
             skdGrpName = skdgrpData.getSkdgrpName();	
          }
          
          csdDesc = skdGrpName + DEPT_IDENTIFIER + taskName;
          
       }catch(SQLException e) {
       	   if ( logger.isDebugEnabled() ){
              logger.debug(e.toString());
           }
       }
       return csdDesc;
    }

    private class rowData
    {
        protected String fieldName = null;

        protected String fieldValue = null;

        public rowData(String fN, String fV)
        {
            fieldName = fN;
            fieldValue = fV;
        }
    }

    private class insertObject
    {
        protected boolean StaffRecChecked = false;

        protected boolean DriverChecked = false;

        protected boolean DistChecked = false;

        protected int seqNum = -1;

        protected int skdgrpId = -1;

        protected int wrkldId = -1;
    }

}
