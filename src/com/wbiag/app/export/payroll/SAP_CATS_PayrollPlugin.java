package com.wbiag.app.export.payroll;

import com.workbrain.app.export.payroll.PayrollExportPlugin;
import com.workbrain.app.export.payroll.data.*;

public class SAP_CATS_PayrollPlugin extends PayrollExportPlugin {
	
    public boolean beforeRowFormat(Row r){
    	
    	int fldWrkdMinutes 	= getFieldIndex("WRKD_MINUTES");
    	int fldSortOrder	= getFieldIndex("SORT_ORDER");
    	
    	double wrkdMinutes = Double.parseDouble( String.valueOf(r.get(fldWrkdMinutes) ));    	
    	if ( wrkdMinutes < 0 ) {
    		r.set( fldSortOrder		, "DELETE" );
    		r.set( fldWrkdMinutes	, String.valueOf(-wrkdMinutes) );
    	} else {
    		r.set( fldSortOrder		, "" );
    	}
    	
        return true;
    }

}