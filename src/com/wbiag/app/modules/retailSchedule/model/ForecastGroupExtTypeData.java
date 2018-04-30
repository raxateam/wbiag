package com.wbiag.app.modules.retailSchedule.model;

import com.workbrain.app.ta.model.RecordData;

/**
 * @author bchan
 *
 */
public class ForecastGroupExtTypeData extends RecordData {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ForecastGroupExtTypeData.class);

    private int fcastgrptypId;
    private String fcastgrptypName;
    private String fcastgrptypDesc;
    private String fcastgrptypFormula;
    
	/**
	 * @return Returns the fcastgrptypDesc.
	 */
	public String getFcastgrptypDesc() {
		return fcastgrptypDesc;
	}

	/**
	 * @param fcastgrptypDesc The fcastgrptypDesc to set.
	 */
	public void setFcastgrptypDesc(String fcastgrptypDesc) {
		this.fcastgrptypDesc = fcastgrptypDesc;
	}

	/**
	 * @return Returns the fcastgrptypFormula.
	 */
	public String getFcastgrptypFormula() {
		return fcastgrptypFormula;
	}

	/**
	 * @param fcastgrptypFormula The fcastgrptypFormula to set.
	 */
	public void setFcastgrptypFormula(String fcastgrptypFormula) {
		this.fcastgrptypFormula = fcastgrptypFormula;
	}

	/**
	 * @return Returns the fcastgrptypId.
	 */
	public int getFcastgrptypId() {
		return fcastgrptypId;
	}

	/**
	 * @param fcastgrptypId The fcastgrptypId to set.
	 */
	public void setFcastgrptypId(int fcastgrptypId) {
		this.fcastgrptypId = fcastgrptypId;
	}

	/**
	 * @return Returns the fcastgrptypName.
	 */
	public String getFcastgrptypName() {
		return fcastgrptypName;
	}

	/**
	 * @param fcastgrptypName The fcastgrptypName to set.
	 */
	public void setFcastgrptypName(String fcastgrptypName) {
		this.fcastgrptypName = fcastgrptypName;
	}

	/**
     * Creates a new instance of this class.
     */
    public RecordData newInstance() {
        return new ForecastGroupExtTypeData();
    }
    
    /**
     * default constructor
     */
    public ForecastGroupExtTypeData() {
    }

    /**
     * Returns a string listing all fields in the record with their values.
     */
	public String toString() {
        String s = "ForecastGroupExtTypeData:\n" +
        	"  fcastgrptypId = " + fcastgrptypId + "\n" +
        	"  fcastgrptypName = " + fcastgrptypName + "\n" +
        	"  fcastgrptypDesc = " + fcastgrptypDesc + "\n" +
        	"  fcastgrptypFormula = " + fcastgrptypFormula + "\n";
        return s;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
