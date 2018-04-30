package com.wbiag.app.ta.model;


import com.workbrain.app.ta.model.*;

/**
 * This class creates a CustomerRegistryDataData object which represents one row (record) in the WorkbrainRegistry table of the Workbrain database.
 * A CustomerRegistryDataData object can hold all the information in the fields of the WorkbrainRegistry record. <p>The object has setter and getter methods corresponding to every field in the table.
 * To set the value of a given field in the record, use the "set[fieldname]( [value] )" method and pass in the desired value.
 * To get the value of a given field in the record, use the "get[fieldname]()" method.
 * Workbrain uses a convention where underscores are removed and first letter of each part of the column name is capitalized.  So the field name "WRKS_WORK_DATE" will result in the getter name "setWrksWorkDate()".
 */
public class CustomerRegistryData extends RecordData {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CustomerRegistryData.class);

    private Integer wbregId;
    private Integer wbregParentId;
    private String wbregName;
    private String wbregClass;
    private String wbregValue;

    /**
     * Creates a new instance of this class.
     */
    public RecordData newInstance() {
        return new CustomerRegistryData();
    }

    public void setWbregId( Integer wbregIdIn ) {
        wbregId = wbregIdIn;
    }

    public Integer getWbregId() {
        return wbregId;
    }

    public void setWbregParentId( Integer wbregParentIdIn ) {
        wbregParentId = wbregParentIdIn;
    }

    public Integer getWbregParentId() {
        return wbregParentId;
    }

    public void setWbregName( String wbregNameIn ) {
        wbregName = wbregNameIn;
    }

    public String getWbregName() {
        return wbregName;
    }

    public void setWbregClass( String wbregClassIn ) {
        wbregClass = wbregClassIn;
    }

    public String getWbregClass() {
        return wbregClass;
    }

    public void setWbregValue( String wbregValueIn ) {
        wbregValue = wbregValueIn;
    }

    public String getWbregValue() {
        return wbregValue;
    }

    /**
     * Returns a string listing all fields in the record with their values.
     */
    public String toString() {
        String s = "CustomerRegistryData:\n" +
            "  wbregId = " + wbregId + "\n" +
            "  wbregParentId = " + wbregParentId + "\n" +
            "  wbregName = " + wbregName + "\n" +
            "  wbregClass = " + wbregClass + "\n" +
            "  wbregValue = " + wbregValue + "\n";
        return s;
    }
}
