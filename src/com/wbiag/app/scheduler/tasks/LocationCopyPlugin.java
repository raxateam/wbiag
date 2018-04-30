package com.wbiag.app.scheduler.tasks;

import java.util.List;

import com.workbrain.sql.DBConnection;

/** 
 * Title:			Location Copy Plugin	
 * Description:		Allows project teams to make modifications to the locations just created
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Jun 21, 2005
 * @author         	Kevin Tsoi
 */
public interface LocationCopyPlugin
{
    public void editLocations(DBConnection conn, List locationIds) throws Exception;        
}
