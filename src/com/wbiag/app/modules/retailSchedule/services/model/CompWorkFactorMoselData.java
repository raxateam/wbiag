/*---------------------------------------------------------------------------
   (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.app.modules.retailSchedule.services.model;

import com.workbrain.app.modules.retailSchedule.services.model.IMoselData;

/**
 * This class is used to represent a data entry (row of delimited values) in
 * the Mosel compress work factor input file, which is used for the
 * compressible task rule.
 *
 * @author James Tam
 * @see com.workbrain.app.modules.retailSchedule.services.model.IMoselData
 */
public class CompWorkFactorMoselData implements IMoselData {
    private static final String DELIMITER = ",";
    private static final String END_OF_LINE = "\n";

    /**
     * The name of the Mosel compress work factor input file.
     */
    public static final String FILENAME = "_A_CompressWorkFactor";

    /**
     * The staff rule id for the compressible task rule.
     */
    public static final String RULE_ID = "1400";

    private double compressFacVal;
    private String requirementId;

    /**
     * Constructor for this class.
     *
     * @param requirementId The unique identifier of the staffing requirement
     * @param compressFacVal The value of the compression factor
     */
    public CompWorkFactorMoselData(String requirementId, double compressFacVal) {
        this.compressFacVal = compressFacVal;
        this.requirementId = requirementId;
    }

    /**
     * Gets the value of the compression factor.
     *
     * @return The value of the compression factor
     */
    public double getCompressFacVal() {
        return compressFacVal;
    }

    /**
     * Gets the unique identifier of the staffing requirement.
     *
     * @return The unique identifier of the staffing requirement
     */
    public String getRequirementId() {
        return requirementId;
    }

    /**
     * Sets the value of the compression factor.
     *
     * @param value The value of the compression factor
     */
    public void setCompressFacVal(double value) {
        compressFacVal = value;
    }

    /**
     * Sets the unique identifier of the staffing requirement.
     *
     * @param id The unique identifier of the staffing requirement
     */
    public void setRequirementId(String id) {
        requirementId = id;
    }

    /**
     * Builds and returns the data entry (row of delimited values) for the
     * Mosel compress work factor input file. The order of the values are as
     * follows: requirement id and then compression factor value.
     *
     * @return The data entry (row) for the compress work factor of a staffing
     * requirement
     */
    public String toString() {
        StringBuffer row = new StringBuffer("'");
        row.append(requirementId).append("'").append(DELIMITER);
        row.append(compressFacVal).append(END_OF_LINE);
        return row.toString();
    }

}