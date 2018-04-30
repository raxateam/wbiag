package com.wbiag.server.signoninterface;

import java.sql.*;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.server.registry.*;
import com.workbrain.server.signoninterface.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;

/**
 * Title:			Generic Single Sign On
 * Description:		Generic single sign on that can be used by clients
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Jul 12, 2005
 * @author         	Kevin Tsoi
 */
public class GenericSingleSignOn implements WebSignOnInterface
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(GenericSingleSignOn.class);

    public static final String PARAM_NAME_USER_NAME = "system/wbiag/singlesignon/PARAM_NAME_USER_NAME";
    public static final String PARAM_NAME_ACTUAL_USER_NAME = "system/wbiag/singlesignon/PARAM_NAME_ACTUAL_USER_NAME";
    public static final String PARAM_NAME_ACTUAL_USER_FLAG = "system/wbiag/singlesignon/PARAM_NAME_ACTUAL_USER_FLAG";
    public static final String PARAM_NAME_CLIENT_ID = "system/wbiag/singlesignon/PARAM_NAME_CLIENT_ID";
    public static final String DEFAULT_PARAM_USER_NAME = "user_name";
    public static final String DEFAULT_CLIENT_ID = "1";
    public static final String FLAG_TRUE = "TRUE";

	public WebSignOnCredentials getCredentials(javax.servlet.jsp.PageContext pc, java.sql.Connection conn)
		throws SignOnException
	{
    	WorkbrainUserAccess wbUserAccess = null;
    	WorkbrainUserData wbUserData = null;
    	WebSignOnCredentials credentials = null;
    	String paramUserName = null;
    	String paramActualUserName = null;
    	String paramActualUserFlag = null;
    	String paramClientId = null;
    	String userName = null;
    	String actualUserName = null;
    	String actualUserFlagStr = null;
    	String clientId = null;
    	String userId = null;
    	String actualUserId = null;
    	boolean actualUserFlag = false;

    	//set default client id so can retrieve registry override values
    	com.workbrain.security.SecurityService.setCurrentClientId(DEFAULT_CLIENT_ID);

    	//retrieve param names
    	paramUserName = Registry.getVarString(PARAM_NAME_USER_NAME, null);
    	paramActualUserName = Registry.getVarString(PARAM_NAME_ACTUAL_USER_NAME, null);
    	paramActualUserFlag = Registry.getVarString(PARAM_NAME_ACTUAL_USER_FLAG, "false");
    	paramClientId = Registry.getVarString(PARAM_NAME_CLIENT_ID , "1");

    	//default param name for user name to DEFAULT_PARAM_USER_NAME
    	if(StringHelper.isEmpty(paramUserName))
    	{
    	    paramUserName = DEFAULT_PARAM_USER_NAME;
    	}

    	//retrieve param values
    	userName = pc.getRequest().getParameter(paramUserName);
    	actualUserName = pc.getRequest().getParameter(paramActualUserName);
    	actualUserFlagStr = pc.getRequest().getParameter(paramActualUserFlag);
    	clientId = pc.getRequest().getParameter(paramClientId);

    	//default client id DEFAULT_CLIENT_ID
    	if(StringHelper.isEmpty(clientId))
    	{
    	    clientId = DEFAULT_CLIENT_ID;
    	}
    	//sets actualUserFlag if param is true
    	if(FLAG_TRUE.equalsIgnoreCase(actualUserFlagStr))
    	{
    	    actualUserFlag = true;
    	}

		try
		{
		    wbUserAccess = new WorkbrainUserAccess((DBConnection)conn);

	    	if(!StringHelper.isEmpty(userName))
	    	{
				//load userId
				wbUserData = wbUserAccess.loadByWbuName(userName);
				if(wbUserData != null)
				{
				    userId = String.valueOf(wbUserData.getWbuId());
				}
	    	}

			//set as user name if ActualWbuIdIsText, otherwise, load actual user id
			if(actualUserFlag)
			{
			    actualUserId = actualUserName;
			}
			else if(!StringHelper.isEmpty(actualUserName))
			{
				//load actualUserId
				wbUserData = wbUserAccess.loadByWbuName(actualUserName);
				if(wbUserData != null)
				{
				    actualUserId = String.valueOf(wbUserData.getWbuId());
				}
			}
			credentials = new WebSignOnCredentials();
			credentials.setEffectiveWbuId(userId);
			credentials.setActualWbuId(actualUserId);
			credentials.setActualWbuIdIsText(actualUserFlag);
			credentials.setClientId(clientId);

			return credentials;
		}
		catch(SQLException e)
		{
		    logger.error("Error in GenericSingleSignOn", e);
			throw new SignOnException(e);
		}
	}

}
