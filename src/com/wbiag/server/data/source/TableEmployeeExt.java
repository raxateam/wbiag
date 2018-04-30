package com.wbiag.server.data.source;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.EmployeeAccess;
import com.workbrain.app.ta.db.WorkbrainUserAccess;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.server.WebLogin;
import com.workbrain.server.data.AccessException;
import com.workbrain.server.data.FieldType;
import com.workbrain.server.data.NotValidDataException;
import com.workbrain.server.data.Row;
import com.workbrain.server.data.RowCursor;
import com.workbrain.server.data.RowDefinition;
import com.workbrain.server.data.RowSet;
import com.workbrain.server.data.sql.TableRowSource;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.DBServer;
import com.workbrain.sql.SQLHelper;
import com.workbrain.tool.overrides.InsertEmployeeOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.tool.overrides.OverrideException;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;
import com.workbrain.server.data.source.TableEmployee;
import com.workbrain.tool.security.SecurityEmployee;
/**
 * Extension class to set newly created employee's home team to creator user's home team
 * as of create date 
 *
 */
public class TableEmployeeExt extends TableEmployee {
    private static Logger logger = Logger.getLogger(TableEmployeeExt.class);
    
    WebLogin wl;
    
    public TableEmployeeExt(Connection c, WebLogin wl)
        throws AccessException{
        super(c, wl);
        this.wl = wl;        
    }    
		    
    /**
     * Overloaded the <code>TableRowSource.save(RowCursor)</code> to use EmployeeAccess
     * instead of using direct JDBC.
     */
    public void save(RowCursor rc) throws AccessException{
        logger.debug("save(RowCursor)   --start--");
        if(isReadOnly()) {
            if (logger.isEnabledFor(org.apache.log4j.Priority.DEBUG)) {
                logger.debug("This RowSource is read only, table: " + getTableName());
            }
            throw new UnsupportedOperationException("This RowSource is read only");
        }
        try{
            final boolean isTableAudited = isTableAudited();
            if (logger.isEnabledFor(org.apache.log4j.Priority.DEBUG)) {
                logger.debug("TableRowSource.save() for table: " + getTableName());
            }
            RowDefinition rd     = rc.getRowDefinition();
            PreparedStatement ps = null;
            Row.Status status    = null;
            RowSet oldRows       = null;
            Row oldRow           = null;

            //Copy the current cursor into a rowset since we need to make two passes
            List rows = new ArrayList();
            while(rc.fetchRow()){
                if(rc.getStatus()==Row.NEW){
                    int keyId  = getConnection().getDBSequence(getKeySequenceName()).getNextValue();
                    rc.setValue(getRowDefinition().getKeyField(),new Integer(keyId));
                }
                rows.add(getRow(rc));
            }
            //create the set of old rows for to be deleted or udpated records from the database.
            oldRows = getOldRows(rows);
            EmployeeAccess empAccess = new EmployeeAccess( getConnection(), CodeMapper.createCodeMapper (getConnection()));
            OverrideBuilder ovrBuilder = new OverrideBuilder( getConnection() );
            EmployeeData empData = null;
            for(int ii=0;ii<rows.size();ii++){
                Row row = (Row)rows.get(ii);
                status = row.getStatus();
                oldRow = null;
                if (status == Row.UPDATED || status == Row.DELETED) {
                    Object key = row.getValue(rd.getKeyField());
                    oldRow = oldRows.getRow(key);
                }
                if (status == Row.NEW){
                    logger.debug("nwe employee insertion.");
                    empData = new EmployeeData();
                    empData.fillRequiredFields();
                    for(int i=0, j=rd.getRowSize();i<j;i++){
                        if (row.getValue(i)!=null) {
                            String name = rd.getName(i).toUpperCase();
                            Object value = row.getValue(i);
                            if(i==1
                               && name.endsWith("_NAME")
                               && !name.equals("WBFLD_NAME")
                               && !name.equals("WBMLDT_NAME")
                               && !name.equals("SELECT_NAME")
                               ){
                                value = value.toString().toUpperCase();
                            }
                            empData.setField(name, value);
                            if ( logger.isDebugEnabled() ) {
                                logger.debug( "setting field : " + name + " = " + value );
                            }

                            String newV = null;
                            if(isTableAudited || isFieldAudited(name)){
                                String auditSQL = getAuditHeader() +"'I',?,'"+name+"','INSERT',?)";
                                
                                try {
                                    ps = getConnection().prepareStatement(auditSQL);
                                    for(int c=0; (newV=getAuditValueChunk(rd.getFieldType(c),row.getValue(i),c))!=null;c++){
                                        ps.setObject (1, row.getValue(row.getRowDefinition().getKeyField()));
                                        ps.setObject (2, newV);
                                        ps.executeUpdate();
                                        ps.clearParameters();
                                    }
                                } finally {
                                    SQLHelper.cleanUp(ps);
                                }
                            }
                        }
                    }
                    insertingRow(row, true); // call before insert
                    empAccess.insert(empData);
                    // *** ASDA Specific ADD Logic to set employee's home team to user's home team
                    setEmployeeTeamFromUserHomeTeam(Integer.parseInt(wl.getUserId())
                            , empData.getEmpId());
                    if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  {logger.debug("Set emp's home team to creator user : " + wl.getUserName() + "'s home team"); }
                    //empAccess.insertDefaults(empData);
                    insertingRow(row, true); // call after insert
                } else if(status == Row.DELETED){
                    logger.debug("employee deletion.");
                    deletingRow(oldRow,true);
                    getKeyCondition(row);
                    if(oldRow != null){
                        String keyId  = ""+row.getValue(rd.getKeyField());
                        for(int i=0, j=rd.getRowSize();i<j;i++){
                            if(isTableAudited || isFieldAudited(rd.getName(i))){
                                FieldType ft = rd.getFieldType(i);
                                Object dbValue = null;
                                try{
                                    dbValue = ft.convert(oldRow.getValue(i));
                                }catch(NotValidDataException e){
                                    //fall through
                                    //pleasing PMD rule
                                    logger.debug("TableEmployee save() deleted exception converting old value. programicly falling thru");
                                }

                                if(dbValue!=null) {
                                    String auditSQL = getAuditHeader() +"'D',"+keyId+",?,?,'DELETED')";
                                    
                                    try {
                                        ps = getConnection().prepareStatement(auditSQL);
                                        ps.setString(1, rd.getName(i));
                                        ps.setObject(2, getAuditValue(rd.getFieldType(i),dbValue, false));
                                        ps.executeUpdate();
                                    } finally {
                                        SQLHelper.cleanUp(ps);
                                    }
                                }
                            }
                        }
                        empAccess.deleteRecordData(getTableName(), rd.getFieldName(rd.getKeyField()), empData.getEmpId());
                        deletingRow(oldRow,false);
                    }
                } else if(status == Row.UPDATED){
                    logger.debug("employee update.");
                    updatingRow(oldRow,row,true);
                    if(oldRow!=null){
                        logger.debug("employee update with oldRow.");
                        empData = new EmployeeData(); // for saving updated fields.
                        StringBuffer  sb = new StringBuffer();
                        String keyId  = String.valueOf( row.getValue(rd.getKeyField()) );
                        empData.setEmpId(Integer.parseInt(row.getValue("EMP_ID").toString()));
                        for(int i=0, j=rd.getRowSize();i<j;i++){
                            String name = rd.getName(i).toUpperCase();
                            FieldType ft = rd.getFieldType(i);
                            Object dbValue = null;
                            try{
                                dbValue = ft.convert(oldRow.getValue(i));
                            }catch(NotValidDataException e){
                                //fall through
                                //pleasing PMD rule
                                logger.debug("TableEmployee save() updated exception converting old value. programicly falling thru");
                            }
                            Object value   = row.getValue(i);
                            if( !rd.getFieldType(i).isValueEqual(value,dbValue)
                                 && !rd.getFieldName(i).endsWith("_ROWID") ) {
                                empData.setField(name, value);  // save the updated field.
                                if ( logger.isDebugEnabled() ) {
                                    logger.debug("field : " + name + " is set to : " + value);
                                }
                                if(rd.getFieldType(i) instanceof com.workbrain.server.data.type.ClobType){   // blob/clob type is not used in employee, but leave it for future.
                                    getConnection().updateClob(
                                         value.toString(),
                                         getTableName(),
                                         name,
                                         rd.getName(rd.getKeyField()),
                                         row.getValue(rd.getKeyField()).toString()
                                    );
                                } else {
                                    String sqlString  = rd.getFieldType(i).getSQLString(getConnection(),value);
                                    if(i==1 && name.endsWith("_NAME") && !name.equals("WBFLD_NAME") && !name.equals("WBMLDT_NAME")) sqlString = sqlString.toUpperCase();
                                    if(sb.length()>0) sb.append(',');
                                    sb.append(name).append('=').append(sqlString);
                                }
                                String oldV = null, newV = null;
                                if(isTableAudited || isFieldAudited(name)){
                                    for(int c=0; (oldV=getAuditValueChunk(ft,dbValue,c))!=null | (newV=getAuditValueChunk(ft,value,c))!=null;c++){
                                        try {
                                            ps = getConnection().prepareStatement(getAuditHeader() +"'U',"+
                                            keyId+",'"+name+"',?,?)");
                                            ps.setString(1,oldV);
                                            ps.setString(2,newV);
                                            ps.executeUpdate();
                                            ps.clearParameters();
                                        } finally {
                                            SQLHelper.cleanUp(ps);
                                        }
                                    }
                                }
                                // add updates to override batch
                                InsertEmployeeOverride empOverride = new InsertEmployeeOverride( empData, getConnection() );
                                empOverride.setStartDate(DateHelper.truncateToDays(new java.util.Date()));
                                empOverride.setEndDate(EmployeeData.PERM_DATE_OBJ);
                                empOverride.setWbuName(getWebLogin().getUserName());
                                empOverride.setWbuNameActual(getWebLogin().getActualUserName());
                                empOverride.setEmpId(empData.getEmpId());
                                ovrBuilder.add(empOverride);
                             }
                        } // end loop thru fields
                        try {
                            ovrBuilder.execute(false);
                        }
                        catch ( OverrideException oe )  {
                            logger.error(oe);
                            throw new AccessException(oe);
                        }

                    } // oldRow != null
                } else {
                    continue;
                }
            }
        } catch (IOException e){
            logger.error( "Error while saving employee!", e );
            throw new AccessException(e);
        } catch (SQLException e){
            logger.error( "Error while saving employee!", e );
            throw new AccessException(e);
        }
        logger.debug("save(RowCursor)   --return--");
    }

    /**
     * Sets employee's home team to user's home team as of current date
     * @param wbuId
     * @param empId
     * @throws SQLException
     */
    private void setEmployeeTeamFromUserHomeTeam(int wbuId, int empId) throws SQLException{
        String empIdForWbuId = WorkbrainUserAccess.retrieveEmpIdByWbuId(getConnection(), wbuId);
        if (!StringHelper.isEmpty(empIdForWbuId)) {
            int userHomeTeam = SecurityEmployee.getHomeTeamId(getConnection(), 
                    Integer.parseInt(empIdForWbuId), SQLHelper.getCurrDate());
            SecurityEmployee.setHomeTeam(getConnection(), new Integer(empId), new Integer(userHomeTeam),
                    SQLHelper.getCurrDate(), SQLHelper.getMaxDate(),false,
                    "","","","","",
                    "","","","","");
        }
            
            
    }
}
