/*---------------------------------------------------------------------------
   (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.app.modules.retailSchedule.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import com.wbiag.app.modules.retailSchedule.db.CsdDetailAccess;
import com.workbrain.app.modules.retailSchedule.IRetailScheduleConstants;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.utils.SQLUtils;
import com.workbrain.app.ta.model.RecordData;

/**
 * This class represents a data row in the SO_CSD_DETAIL table. Each row in
 * this table represents additional detail data related to location staffing 
 * requirements.
 *
 * @author James Tam
 * @see com.workbrain.app.ta.model.RecordData
 */
public class CorporateEntityStaffRequirementDetail extends RecordData {
    private static Logger logger = Logger.getLogger(CorporateEntityStaffRequirementDetail.class);

    private double csddetCompressVl;
    private int csdId;
    private int csddetId;
    private String csddetFlag1;
    private String csddetFlag2;
    private String csddetFlag3;
    private String csddetFlag4;
    private String csddetFlag5;
    private String csddetUdf1;
    private String csddetUdf2;
    private String csddetUdf3;
    private String csddetUdf4;
    private String csddetUdf5;

    /**
     * Default Constructor for this class.
     */
    public CorporateEntityStaffRequirementDetail() {
    }

    /**
     * Constructor for this class.
     *
     * @param id The primary key value for the row of data
     */
    public CorporateEntityStaffRequirementDetail(int id) {
        csddetId = id;
    }

    /**
     * Gets a new instance.
     *
     * @return A new instance
     */
    public RecordData newInstance() {
        return new CorporateEntityStaffRequirementDetail();
    }

    /**
     * Adds non reflected properties names.
     */
    protected void addNotReflectedPropertyNames(List l) {
        super.addNotReflectedPropertyNames(l);
        l.add("ID");
        l.add("tableName");
        l.add("primaryKey");
        l.add("primaryKeySeq");
    }

    /**
     * Gets the primary key property value.
     *
     * @return The primary key value
     */
    public Integer getID() {
        return new Integer(csddetId);
    }

    /**
     * Gets the compression value property value.
     *
     * @return The compression value
     */
    public double getCsddetCompressVl() {
        return csddetCompressVl;
    }

    /**
     * Gets the property value for the id of the staffing requirement.
     *
     * @return The id of the staffing requirement
     */
    public int getCsdId() {
        return csdId;
    }

    /**
     * Gets the primary key property value.
     *
     * @return The primary key value
     */
    public int getCsddetId() {
        return csddetId;
    }

    /**
     * Gets the csddetFlag1 property value.
     *
     * @return The csddetFlag1 value
     */
    public String getCsddetFlag1() {
        return csddetFlag1;
    }

    /**
     * Gets the csddetFlag2 property value.
     *
     * @return The csddetFlag2 value
     */
    public String getCsddetFlag2() {
        return csddetFlag2;
    }

    /**
     * Gets the csddetFlag3 property value.
     *
     * @return The csddetFlag3 value
     */
    public String getCsddetFlag3() {
        return csddetFlag3;
    }

    /**
     * Gets the csddetFlag4 property value.
     *
     * @return The csddetFlag4 value
     */
    public String getCsddetFlag4() {
        return csddetFlag4;
    }

    /**
     * Gets the csddetFlag5 property value.
     *
     * @return The csddetFlag5 value
     */
    public String getCsddetFlag5() {
        return csddetFlag5;
    }

    /**
     * Gets the csddetUdf1 property value.
     *
     * @return The csddetUdf1 value
     */
    public String getCsddetUdf1() {
        return csddetUdf1;
    }

    /**
     * Gets the csddetUdf2 property value.
     *
     * @return The csddetUdf2 value
     */
    public String getCsddetUdf2() {
        return csddetUdf2;
    }

    /**
     * Gets the csddetUdf3 property value.
     *
     * @return The csddetUdf3 value
     */
    public String getCsddetUdf3() {
        return csddetUdf3;
    }

    /**
     * Gets the csddetUdf4 property value.
     *
     * @return The csddetUdf4 value
     */
    public String getCsddetUdf4() {
        return csddetUdf4;
    }

    /**
     * Gets the csddetUdf5 property value.
     *
     * @return The csddetUdf5 value
     */
    public String getCsddetUdf5() {
        return csddetUdf5;
    }

    /**
     * Sets the property value for the compression value.
     *
     * @param value The compression value
     */
    public void setCsddetCompressVl(double value) {
        csddetCompressVl = value;
    }

    /**
     * Sets the property value for the id of the staffing requirement.
     *
     * @param id The id of the staffing requirement
     */
    public void setCsdId(int id) {
        csdId = id;
    }

    /**
     * Sets the property value for the primary key field.
     *
     * @param value The primary key value
     */
    public void setCsddetId(int value) {
        csddetId = value;
    }

    /**
     * Sets the property value for csddetFlag1
     *
     * @param value The value of the csddetFlag1 property
     */
    public void setCsddetFlag1(String value) {
        csddetFlag1 = value;
    }

    /**
     * Sets the property value for csddetFlag2
     *
     * @param value The value of the csddetFlag2 property
     */
    public void setCsddetFlag2(String value) {
        csddetFlag2 = value;
    }

    /**
     * Sets the property value for csddetFlag3
     *
     * @param value The value of the csddetFlag3 property
     */
    public void setCsddetFlag3(String value) {
        csddetFlag3 = value;
    }

    /**
     * Sets the property value for csddetFlag4
     *
     * @param value The value of the csddetFlag4 property
     */
    public void setCsddetFlag4(String value) {
        csddetFlag4 = value;
    }

    /**
     * Sets the property value for csddetFlag5
     *
     * @param value The value of the csddetFlag5 property
     */
    public void setCsddetFlag5(String value) {
        csddetFlag5 = value;
    }

    /**
     * Sets the property value for csddetUdf1
     *
     * @param value The value of the csddetUdf1 property
     */
    public void setCsddetUdf1(String value) {
        csddetUdf1 = value;
    }

    /**
     * Sets the property value for csddetUdf2
     *
     * @param value The value of the csddetUdf2 property
     */
    public void setCsddetUdf2(String value) {
        csddetUdf2 = value;
    }

    /**
     * Sets the property value for csddetUdf3
     *
     * @param value The value of the csddetUdf3 property
     */
    public void setCsddetUdf3(String value) {
        csddetUdf3 = value;
    }

    /**
     * Sets the property value for csddetUdf4
     *
     * @param value The value of the csddetUdf4 property
     */
    public void setCsddetUdf4(String value) {
        csddetUdf4 = value;
    }

    /**
     * Sets the property value for csddetUdf5
     *
     * @param value The value of the csddetUdf5 property
     */
    public void setCsddetUdf5(String value) {
        csddetUdf5 = value;
    }

    /**
     * Returns a string representation of the instance and its properties.
     *
     * @return a string containing the class name and the property name:value
     * pairs
     */
    public String toString() {
        StringBuffer sb = new StringBuffer("");
        sb.append("CLASS: CorporateEntityStaffRequirementDetail");
        sb.append("\ncsddetId: ").append(csddetId);
        sb.append("\ncsdId: ").append(csdId);
        sb.append("\ncsddetCompressVl: ").append(csddetCompressVl);
        sb.append("\ncsddetFlag1: ").append(csddetFlag1);
        sb.append("\ncsddetFlag2: ").append(csddetFlag2);
        sb.append("\ncsddetFlag3: ").append(csddetFlag3);
        sb.append("\ncsddetFlag4: ").append(csddetFlag4);
        sb.append("\ncsddetFlag5: ").append(csddetFlag5);
        sb.append("\ncsddetUdf1: ").append(csddetUdf1);
        sb.append("\ncsddetUdf2: ").append(csddetUdf2);
        sb.append("\ncsddetUdf3: ").append(csddetUdf3);
        sb.append("\ncsddetUdf4: ").append(csddetUdf4);
        sb.append("\ncsddetUdf5: ").append(csddetUdf5);

        return sb.toString();
    }

    /**
     * Sets the property values based on the values in a result set.
     *
     * @param rs The result set containing the property values
     * @throws SQLException If an error occurs while reading the result set
     */
    public void assignByName(ResultSet rs) throws SQLException {
        csddetId = SQLUtils.getIntegerFromRs(rs, "csddet_id").intValue();
        csdId = SQLUtils.getIntegerFromRs(rs, "csd_id").intValue();
        csddetCompressVl = SQLUtils.getDoubleFromRs(rs, "csddet_compress_vl").doubleValue();
        csddetFlag1 = rs.getString("csddet_flag1");
        csddetFlag2 = rs.getString("csddet_flag2");
        csddetFlag3 = rs.getString("csddet_flag3");
        csddetFlag4 = rs.getString("csddet_flag4");
        csddetFlag5 = rs.getString("csddet_flag5");
        csddetUdf1 = rs.getString("csddet_udf1");
        csddetUdf2 = rs.getString("csddet_udf2");
        csddetUdf3 = rs.getString("csddet_udf3");
        csddetUdf4 = rs.getString("csddet_udf4");
        csddetUdf5 = rs.getString("csddet_udf5");
    }

    /**
     * Returns a clone of the instance.
     *
     * @return A clone of the instance
     */
    public Object clone() {
        CorporateEntityStaffRequirementDetail corpEntStaffReqDet = new CorporateEntityStaffRequirementDetail();
        corpEntStaffReqDet.setCsddetId(getCsddetId());
        corpEntStaffReqDet.setCsdId(getCsdId());
        corpEntStaffReqDet.setCsddetCompressVl(getCsddetCompressVl());
        corpEntStaffReqDet.setCsddetFlag1(getCsddetFlag1());
        corpEntStaffReqDet.setCsddetFlag2(getCsddetFlag2());
        corpEntStaffReqDet.setCsddetFlag3(getCsddetFlag3());
        corpEntStaffReqDet.setCsddetFlag4(getCsddetFlag4());
        corpEntStaffReqDet.setCsddetFlag5(getCsddetFlag5());
        corpEntStaffReqDet.setCsddetUdf1(getCsddetUdf1());
        corpEntStaffReqDet.setCsddetUdf2(getCsddetUdf2());
        corpEntStaffReqDet.setCsddetUdf3(getCsddetUdf3());
        corpEntStaffReqDet.setCsddetUdf4(getCsddetUdf4());
        corpEntStaffReqDet.setCsddetUdf5(getCsddetUdf5());

        return corpEntStaffReqDet;
    }

    /**
     * Returns whether or not an object is equal to the instance.
     *
     * @param obj The object to be compared to the instance
     * @return Whether or not the object is equal to the instance
     */
    public boolean equals(Object obj) {
        boolean retValue = true;

        if (obj instanceof CorporateEntityStaffRequirementDetail) {
            CorporateEntityStaffRequirementDetail corpEntStaffReqDet1 = this;
            CorporateEntityStaffRequirementDetail corpEntStaffReqDet2 = (CorporateEntityStaffRequirementDetail)obj;
            if (corpEntStaffReqDet1.getCsddetId()!=corpEntStaffReqDet2.getCsddetId()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsdId()!=corpEntStaffReqDet2.getCsdId()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetCompressVl()!=corpEntStaffReqDet2.getCsddetCompressVl()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetFlag1()!=corpEntStaffReqDet2.getCsddetFlag1()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetFlag2()!=corpEntStaffReqDet2.getCsddetFlag2()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetFlag3()!=corpEntStaffReqDet2.getCsddetFlag3()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetFlag4()!=corpEntStaffReqDet2.getCsddetFlag4()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetFlag5()!=corpEntStaffReqDet2.getCsddetFlag5()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetUdf1()!=corpEntStaffReqDet2.getCsddetUdf1()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetUdf2()!=corpEntStaffReqDet2.getCsddetUdf2()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetUdf3()!=corpEntStaffReqDet2.getCsddetUdf3()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetUdf4()!=corpEntStaffReqDet2.getCsddetUdf4()) {
                retValue = false;
            } else if (corpEntStaffReqDet1.getCsddetUdf5()!=corpEntStaffReqDet2.getCsddetUdf5()) {
                retValue = false;
            }
        } else {
            retValue = false;
        }

        return retValue;
    }

    /**
     * Deletes the related row of data from the database.
     *
     * @throws RetailException If the deletion from the database fails
     */
    public void dbDelete() throws RetailException {
        if (csddetId==IRetailScheduleConstants.NULL_INT) {
            logger.error("Cannot delete row from db because CorporateEntityStaffRequirementDetail object not initialized.");
            throw (new RetailException("CorporateEntityStaffRequirementDetail object not initialized."));
        }
        CsdDetailAccess access = new CsdDetailAccess(DBInterface.getCurrentConnection());
        access.deleteRecordData(getTableName(), getPrimaryKey(), csddetId);
    }

    /**
     * Inserts the row of data into the database.
     *
     * @return The primary key value for this row of data
     * @throws RetailException If the insertion into the database fails
     */
    public Integer dbInsert() throws RetailException {
        CsdDetailAccess access = new CsdDetailAccess(DBInterface.getCurrentConnection());
        if (csddetId==IRetailScheduleConstants.NULL_INT) {
            csddetId = access.createCorporateEntityStaffRequirementDetail().getCsddetId();
        }
        access.insertRecordData(this, getTableName());

        return getID();
    }

    /**
     * Updates the related row of data in the dabatase.
     *
     * @throws RetailException If the update in the database fails
     */
    public void dbUpdate() throws RetailException {
        CsdDetailAccess access = new CsdDetailAccess(DBInterface.getCurrentConnection());
        access.updateRecordData(this, getTableName(), getPrimaryKey());
    }

    /**
     * Gets the database table name.
     *
     * @return The name of the database table
     */
    public String getTableName() {
        return CsdDetailAccess.TABLE_NAME;
    }

    /**
     * Gets the primary key field name for the table.
     *
     * @return The name of the primary key field for the table
     */
    public String getPrimaryKey() {
        return CsdDetailAccess.PRIMARY_KEY;
    }

    /**
     * Gets the name of the sequence for the primary key field.
     *
     * @return The name of the sequence for the primary key field
     */
    public String getPrimaryKeySeq() {
        return CsdDetailAccess.PRI_KEY_SEQ;
    }

}