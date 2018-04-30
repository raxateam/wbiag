package com.wbiag.app.wbalert.source;

import java.util.HashMap;

import com.workbrain.app.wbalert.AbstractWBAlertSourceBuilder;
import com.workbrain.server.data.RowSource;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.NestedRuntimeException;

public class WorkedNMinutesWithoutBreakAlertSourceBuilder extends AbstractWBAlertSourceBuilder {
	private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WorkedNMinutesWithoutBreakAlertSourceBuilder.class);

	public WorkedNMinutesWithoutBreakAlertSourceBuilder() {}
	
	public String getTaskParametersUI() {
	        return "/jobs/wbalert/workedNMinutesWithoutBreakAlertParams.jsp";
	}
	
	public RowSource getRowSource(java.io.Serializable param, DBConnection conn) {
	        HashMap exportParam = (HashMap)param;

	        try 
	        {
	            return new WorkedNMinutesWithoutBreakAlertSource(conn, exportParam);
	        } catch (Exception ex) {
	            if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(ex.getMessage() , ex);}
	            throw new NestedRuntimeException(ex);
	        }
	    }

}
