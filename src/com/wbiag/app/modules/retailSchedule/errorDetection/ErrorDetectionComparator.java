package com.wbiag.app.modules.retailSchedule.errorDetection;

import java.util.Comparator;

import com.workbrain.app.modules.mvse.validator.Violation;

public class ErrorDetectionComparator implements Comparator {

	private int [] sortProperties;
	
    /**
     * Constant for specifying to sort by rule name
     */
    public final static int SORT_BY_RULE = 1;
    
    /**
     * Creates a <code>ViolationComparator</code> object.
     * 
     * @param sortProperties The properties to sort by
     */
    public ErrorDetectionComparator(int [] sortProperties) {
        this.sortProperties = sortProperties;
    }
    
    public int compare(Object obj1, Object obj2) {
        int value = 0;
        // Place nulls at the end
        if (obj1==null && obj2==null) {
            value = 0;
        } else if (obj1!=null && obj2==null) {
            value = 1;
        } else if (obj1==null && obj2!=null) {
            value = -1;
        } else if (!(obj1 instanceof Violation) ||!(obj2 instanceof Violation)) {
            throw new IllegalArgumentException ("Method only compares Violation objects.");
        } else {
        	//TODO
        }
        return value;
    }

}
