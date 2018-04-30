package com.wbiag.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.workbrain.util.NameValue;

/**
 * @author bviveiros
 * 
 * Helper methods not available in the core StringHelper class.
 *
 */
public class StringHelper {

	/**
	 * Create a CSV from a String array.
	 * 
	 * @param strArray
	 * @return
	 */
	public static String createCSVForArray(String[] strArray, boolean includeEmptyStrings) {
		
		if (strArray == null) {
			return null;
		}
		
		StringBuffer csvStr = new StringBuffer();

		for (int i=0; i < strArray.length; i++) {
			if (!com.workbrain.util.StringHelper.isEmpty(strArray[i])
					|| (com.workbrain.util.StringHelper.isEmpty(strArray[i]) && includeEmptyStrings)) {
				csvStr.append(strArray[i]);
				csvStr.append(",");
			}
		}
		
		csvStr.deleteCharAt(csvStr.length()-1);
		
		return csvStr.toString();
	}
	
	
	/**
	 * Takes a comma seperated list sourceList and creates a sublist by removing
	 * any elements that exist in the toRemove list.  The new sublist is returned.
	 * 
	 * @param sourceList
	 * @param toRemove
	 * @return
	 */
	public static String getSublist(String sourceList, String toRemove, String delimiter) {
		
		StringBuffer subList = new StringBuffer();
		StringTokenizer tokenizer = new StringTokenizer(sourceList, delimiter);
		String token = null;
		
		while (tokenizer.hasMoreTokens()) {
			token = tokenizer.nextToken();
			if (!com.workbrain.util.StringHelper.isItemInList(toRemove, token)) {
				subList.append(token);
				subList.append(delimiter);
			}
		}
		
		// Delete the trailing comma.
		if (subList.length() > 0) {
			subList.deleteCharAt(subList.length()-1);
		}
		
		return subList.toString();
	}
	
	
    /**
     * Converts a nameValue input to a list of <code>NameValue</code> objects.
     * nameValue pairs are delimited by <code>pairDelim<code> and name values are delimited
     * by <code>nameValDelim<code>. <p>
     * i.e name1<nameValDelim>value1<pairDelim>name2<nameValDelim>value2
     *
     * @param input         input string
     * @param pairDelim     pairDelim
     * @param nameValDelim  nameValDelim
     * @param trimsItems    whether each item will be trimmed
     * @return
     */
    public static List detokenizeStringAsNameValueList(String input,
									            String pairDelim,
									            String nameValDelim,
									            boolean trimsItems) {

    	// This method is used by DailyOvertime24HourSkdRollRule.
    	// It was not available in 4.0 so it existed here.
    	// In 4.1 it is in the core StringHelper class so just wrap
    	// the method.  This way, there is only 1 version of the rule
    	// and it is shared between the 2 projects.  But there are 2 versions
    	// of the IAG String Helper which is much easier to maintain.
    	return com.workbrain.util.StringHelper.
				detokenizeStringAsNameValueList(input,
    											pairDelim,
												nameValDelim,
												trimsItems);
    }

}
