package com.wbiag.app.wbinterface.hr2;

import java.sql.*;
import java.util.*;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * This class has been developed with conformity to the functional specification of the Solution Centre's
 * HR Refresh module. Please find func and tech specs in CVS.
 *
 * This class handles the importing of Employee School Records. (SC_EMP_SCHOOL table)
 *
 * author:          Ali Ajellu (Solution Center)
 * date:            Jan 11 2006
 * Copyright:       Workbrain Inc. (2006)
 * CHANGE HISTORY:
 */
public class HRRefreshTransactionSCEmpSchool extends HRRefreshTransaction {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(HRRefreshTransactionSCEmpSchool.class);

    public static final int SC_EMP_SCHOOL_COL               = 91;
    private int[] custColInds                               = new int[] {SC_EMP_SCHOOL_COL};

    private static final String SCHOOL_SEP                  = "~";
    private static final String SCHOOL_ABSOLUTE_PREFIX      = "~~";


    //key: name of all schools in db
    //value: id of each school
    private Map schools = null;

    public static final String SC_SCHOOL_SELECT_SQL
                                    = "SELECT scsch_id, scsch_name FROM sc_school";

    /**
     * Sets custom column indexes to be used by the customization. Indexes start at 0
     * @param inds
     */
    public void setCustomColInds(int[] inds) {
        custColInds = inds;
    }

    /**
     * Override this class to customize after process batch events.
     * At this time, all HRRefreshTransactionData data has been processed.
     * All processed is available through <code>HRRefreshCache</code>.
     *
     * @param conn                          DBConnection
     * @param hrRefreshTransactionDataList  List of <code>HRRefreshTransactionData</code>
     * @param process                       HRRefreshProcessor
     * @throws Exception
     */
    public void postProcessBatch(   DBConnection conn,
                                    List hrRefreshTransactionDataList,
                                    HRRefreshProcessor process)             throws Exception {

        if (hrRefreshTransactionDataList == null
                || hrRefreshTransactionDataList.size() == 0) {
            return;
        }


        if (custColInds == null || custColInds.length != 1) {
            throw new WBInterfaceException(
                    "Custom column index not supplied or too many, must be 1");
        }

        this.conn = conn;

        //iterator through each record and process it using processSCEmpSchool
        Iterator iter = hrRefreshTransactionDataList.iterator();
        while (iter.hasNext()) {
            HRRefreshTransactionData data = (HRRefreshTransactionData) iter.next();
            if (data.isError()) {
                continue;
            }
            try {
                processSCEmpSchool(data.getImportData().getField(custColInds[0]),
                                   data, conn);
            }
            catch (Exception ex) {
                if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("Error in HRRefreshTransactionSCEmpSchool." , ex);}
                data.error("Error in HRRefreshTransactionSCEmpSchool." + ex.getMessage() );
            }
        }
    }

    /**
     * The bulk of processing of the record is done here.
     * @param val           record's import data.
     * @param data          transaction data
     * @param c             dbconnection
     * @throws SQLException
     * @throws WBInterfaceException
     */
    protected void processSCEmpSchool(  String val,
                                       HRRefreshTransactionData data,
                                       DBConnection c)                  throws SQLException, WBInterfaceException {

        //just return if val is empty
        if (StringHelper.isEmpty(val)) {
            if (logger.isDebugEnabled()){
                logger.debug("Field was empty. ending record's processing");
            }
            return;
        }

        int empId = data.getEmpId();
        boolean isAbsolute = val.startsWith(SCHOOL_ABSOLUTE_PREFIX);
        if (logger.isDebugEnabled()){
            logger.debug("is absolute ? " + isAbsolute);
        }

        //is absolute list?
        if (isAbsolute) {
            //delete first two characters
            val = val.substring(SCHOOL_ABSOLUTE_PREFIX.length());
        }

        //retrieve all of the employee's school names
        List allExistingEmpSchools = new ArrayList();
        if (!data.isNewEmployee()) {
            allExistingEmpSchools = getExistingEmpSchools(data.getEmpId());
        }

        //all schools that HR Refresh must register the employee in
        String[] schoolsImported = StringHelper.detokenizeString(val , SCHOOL_SEP);
        if (logger.isDebugEnabled()){
            logger.debug(schoolsImported.length + " schools to be registered in");
        }

        Set processedSchoolIds = new HashSet();

        //iterate through all schools retrieved from val
        for (int i=0;i<schoolsImported.length;i++){
            String schoolName = schoolsImported[i];

            if (logger.isDebugEnabled()){
                logger.debug("Processing school name: '" + schoolName + "'.");
            }

            //if the curr school name is empty or null, continue
            if (StringHelper.isEmpty(schoolName)) {
                if (logger.isDebugEnabled()){
                    logger.debug("Skip current school");
                }
                continue;
            }

            //if the curr school name is not in SC_SCHOOL throw an error
            int schoolId = getSchoolId(schoolName);
            if (schoolId == -1) {
                throw new RuntimeException ("School name not found : " + schoolName);
            }

            //we processed the record, so add its schoolId for the list of processed school.
            //this will be used if we are dealing with absolute records.
            processedSchoolIds.add(new Integer(schoolId));

            List schoolsOfTypeSchoolId = filterBySchoolId(allExistingEmpSchools, schoolId);

            //if there are no Employee School records matching the current schoolName,
            //then add a record
            if (schoolsOfTypeSchoolId.size() == 0){
                if (logger.isDebugEnabled()){
                    logger.debug("No previous records of school " + schoolName + " found. Inserting");
                }
                insertEmployeeSchool(empId, schoolId, data.getOvrStartDate(), data.getOvrEndDate());

            //if there is ONLY 1 Employee School record matching the current schoolName,
            //then update it with the new dates
            }else if(schoolsOfTypeSchoolId.size() == 1){
                if (logger.isDebugEnabled()){
                    logger.debug("One previous record of school " + schoolName + " found. Updating");
                }
                int empSchoolId = ((ScEmpSchoolData)schoolsOfTypeSchoolId.get(0)).scesch_id;
                updateEmployeeSchool(empSchoolId, data.getOvrStartDate(), data.getOvrEndDate());

            //if there is MORE THAN 1 Employee School record matching the current schoolname,
            //then append a warning and move on.
            }else{
                if (logger.isDebugEnabled()){
                    logger.debug("More than one previous records of school " + schoolName + " found. Skipping");
                }
                data.appendWarning("More than one school existed for school : "+ schoolName +". No update was done");
                continue;
            }
        }

        if (isAbsolute) {
            processAbsolute(allExistingEmpSchools, processedSchoolIds);
        }
    }

    /**
     * Deletes the appropriate employee school records based on empSchools and processedSchoolIds.
     * empSchools - processedSchoolsIds = empSchoolsToDelete
     * empSchools must be a list of ScEmpSchool objects
     *
     * @param empSchools A list of all emp school records as ScEmpSchoolData objects(not necessarily including the ones that were just inserted)
     * @param processedSchoolIds A set of Integer school Ids.
     * @throws SQLException
     */
    private void processAbsolute(List empSchools, Set processedSchoolIds)
                                                                                       throws SQLException {

        if ((empSchools == null || empSchools.size() == 0)
                || (processedSchoolIds == null || processedSchoolIds.size() == 0)) {
            return;
        }

        //empSchools - processedSchoolsIds = empSchoolsToDelete
        IntegerList empSchoolsToDelete = new IntegerList();
        Iterator iter = empSchools.iterator();
        while (iter.hasNext()) {
            ScEmpSchoolData item = (ScEmpSchoolData) iter.next();
            if (!processedSchoolIds.contains(new Integer(item.scsch_id))) {
                empSchoolsToDelete.add(item.scesch_id);
            }
        }

        if (empSchoolsToDelete.size() > 0){
            StringBuffer deletionSQL = new StringBuffer();
            deletionSQL.append("DELETE FROM SC_EMP_SCHOOL WHERE ");
            for (int i=0;i<empSchoolsToDelete.size();i++){
                int currEmpSchoolId = empSchoolsToDelete.getInt(i);
                deletionSQL.append("SCESCH_ID = ").append(currEmpSchoolId);
                if (i != empSchoolsToDelete.size() - 1){
                    deletionSQL.append(" OR ");
                }
            }

            if (logger.isDebugEnabled()){
                logger.debug("deletion SQL: " + deletionSQL);
            }
            PreparedStatement prepStmnt = null;

            try {
                prepStmnt = conn.prepareStatement(deletionSQL.toString());
                prepStmnt.executeUpdate();
            } catch(Exception e){
                if (logger.isDebugEnabled()){
                    logger.error(e);
                }
            }finally{
                SQLHelper.cleanUp(prepStmnt);
            }
        }
    }

    /**
     * Insert a record into SC_EMP_SCHOOL given the following field info
     * @param empId             field: EMP_ID
     * @param schoolId          field: SCSCH_ID
     * @param startAttend       field: SCESCH_ATTEND_ST
     * @param endAttend         field: SCESCH_ATTEND_END
     * @throws SQLException
     */
    private void insertEmployeeSchool(int empId, int schoolId, java.util.Date startAttend, java.util.Date endAttend)
                                                                                 throws SQLException{
        if (logger.isDebugEnabled()){
            logger.debug("Inserting emp school: " );
            logger.debug("empId: " + empId + " schoolId: " + schoolId + " attend start: " + startAttend
                            + " attend end: " + endAttend);
        }
        String insertionSQL = "INSERT INTO SC_EMP_SCHOOL (SCESCH_ID, EMP_ID, SCSCH_ID, SCESCH_ATTEND_ST, SCESCH_ATTEND_END) VALUES ( SEQ_SCESCH_ID.NEXTVAL, ?, ?, ?, ?)";

        if (logger.isDebugEnabled()){
            logger.debug("insertion query: " + insertionSQL.toString());
        }
        PreparedStatement prepStmnt = null;

        try {
            prepStmnt = conn.prepareStatement(insertionSQL);
            int paramIndex=1;
            prepStmnt.setInt(paramIndex++, empId);
            prepStmnt.setInt(paramIndex++, schoolId);
            prepStmnt.setDate(paramIndex++, new java.sql.Date(startAttend.getTime()));
            prepStmnt.setDate(paramIndex++, new java.sql.Date(endAttend.getTime()));

            prepStmnt.executeUpdate();
        } catch(Exception e){
          if (logger.isDebugEnabled()){
              logger.error(e);
          }
        } finally {
            SQLHelper.cleanUp(prepStmnt);
        }
    }

    /**
     * update the SC_EMP_SCHOOL record with id=empSchoolId with the startAttend
     * and endAttend dates.
     * @param empSchoolId       id of the record to update
     * @param startAttend       new SCESCH_ATTEND_ST value
     * @param endAttend         new SCESCH_ATTEND_END value
     * @throws SQLException
     */
    private void updateEmployeeSchool(int empSchoolId, java.util.Date startAttend, java.util.Date endAttend)
                                                                               throws SQLException{
        if (logger.isDebugEnabled()){
            logger.debug("Updating emp school: " );
            logger.debug("empSchoolId: " + empSchoolId + " attend start: " + startAttend
                            + " attend end: " + endAttend);
        }
        String updateSQL = "UPDATE SC_EMP_SCHOOL SET SCESCH_ATTEND_ST = ? , SCESCH_ATTEND_END = ? WHERE SCESCH_ID = ? ";

        if (logger.isDebugEnabled()){
            logger.debug("update query: " + updateSQL.toString());
        }
        PreparedStatement prepStmnt = null;

        try {
            prepStmnt = conn.prepareStatement(updateSQL);
            prepStmnt.setDate(1, new java.sql.Date(startAttend.getTime()));
            prepStmnt.setDate(2, new java.sql.Date(endAttend.getTime()));
            prepStmnt.setInt(3, empSchoolId);
            prepStmnt.executeUpdate();
        } catch(Exception e){
            if (logger.isDebugEnabled()){
                logger.error(e);
            }
        } finally {
            SQLHelper.cleanUp(prepStmnt);
        }
    }

    /**
     * Returns a list of all schools in allSchools whose school id is schoolId.
     *
     * @param allSchools A list of ScEmpSchoolData objects
     * @param schoolId The id by which allSchools will be filtered down.
     * @return
     */
    protected List filterBySchoolId(List allSchools, int schoolId){
        List filtered = new ArrayList();

        Iterator iter = allSchools.iterator();
        while(iter.hasNext()){
            ScEmpSchoolData currItem = (ScEmpSchoolData)iter.next();
            if (currItem.scsch_id == schoolId){
                filtered.add(currItem);
            }
        }

        return filtered;
    }

    /**
     * Retrieves the employee's school records in a List. Each element is an ScEmpSchoolData object.
     * See inner class for details.
     *
     * @param empId
     * @return a list of ScEmpSchoolData object
     * @throws SQLException
     */
    private List getExistingEmpSchools(int empId) throws SQLException{
        return getExistingEmpSchools(empId, conn);
    }

    /**
     * Retrieves the employee's school records in a List. Each element is an ScEmpSchoolData object.
     * See inner class for details.
     *
     * @param empId
     * @param conn
     * @return
     * @throws SQLException
     */
    protected static List getExistingEmpSchools(int empId, DBConnection conn) throws SQLException{
        List empSchools = new ArrayList();

        if (empId < 0){
            return empSchools;
        }

        String SELECT_EMP_SCHOOLS_SQL = "SELECT * FROM SC_EMP_SCHOOL WHERE emp_id = ?";

        PreparedStatement preStmnt = null;
        ResultSet rs = null;

        try {
            preStmnt = conn.prepareStatement(SELECT_EMP_SCHOOLS_SQL);
            preStmnt.setInt(1,empId);
            rs = preStmnt.executeQuery();

            //iterate in result set and fill the return list (empSchools) with ScEmpSchoolData objects
            while (rs.next()) {
                ScEmpSchoolData currSchool = new ScEmpSchoolData();
                currSchool.scesch_id = rs.getInt("SCESCH_ID");
                currSchool.emp_id = empId;
                currSchool.scsch_id = rs.getInt("SCSCH_ID");
                currSchool.scesch_attend_st = rs.getDate("SCESCH_ATTEND_ST");
                currSchool.scesch_attend_end = rs.getDate("SCESCH_ATTEND_END");
                currSchool.client_id = rs.getInt("CLIENT_ID");
                if (logger.isDebugEnabled()){
                    logger.debug("Existing employee school: " + currSchool);
                }
                empSchools.add(currSchool);
            }
        } catch (Exception e){
            if (logger.isDebugEnabled()){
                logger.error(e);
            }
        }finally {
            SQLHelper.cleanUp(preStmnt, rs);
        }

        return empSchools;
    }

    /**
     * Returns the id of the school with the name of schoolName
     * @param schoolName
     * @return
     * @throws SQLException
     */
    private int getSchoolId(String schoolName) throws SQLException{
        int ret = -1;

        /*** caching mechanism. */
        //if schools hasn't been populated yet, then query the DB.
        if (schools == null) {
            schools = new HashMap();
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                ps = conn.prepareStatement(SC_SCHOOL_SELECT_SQL);
                rs = ps.executeQuery();
                while (rs.next()) {
                    schools.put(rs.getString(2) , new Integer(rs.getInt(1)));
                }
            }catch(Exception e){
                if (logger.isDebugEnabled()){
                    logger.error(e);
                }
            }finally {
                SQLHelper.cleanUp(ps, rs);
            }

        }

        //search in map for the name
        if (schools.containsKey(schoolName)) {
            ret = ((Integer)schools.get(schoolName)).intValue();
        }

        return ret;
    }


    /**
     * This inner class encapsulates a SC_EMP_SCHOOL record
     * No Data class exists for this table, hence the need for this class.
     * @author aajellu
     */
    protected static class ScEmpSchoolData{
        public int scesch_id;
        public int emp_id;
        public int scsch_id;
        public java.sql.Date scesch_attend_st;
        public java.sql.Date scesch_attend_end;
        public int client_id;

        public String toString(){
            String returnStr = "";
            if (scesch_attend_st != null && scesch_attend_end != null){

                returnStr = scesch_id + "\t" + emp_id + "\t" + + scsch_id + "\t"
                            + scesch_attend_st + "\t" + scesch_attend_end + "\t"
                            + client_id + "\t";
            }
            return returnStr;
        }
    }
}
