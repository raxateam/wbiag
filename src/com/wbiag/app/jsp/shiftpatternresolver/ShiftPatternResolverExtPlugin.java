package com.wbiag.app.jsp.shiftpatternresolver;

import java.util.Date;

import com.workbrain.app.wbinterface.schedulein.model.ShiftPatternMapper;

/**
 * Title:			ShiftPatternResolverExt Plugin
 * Description:		Allows customers to manipulate the data before processing	
 * Copyright:    	Copyright (c) 2005
 * Company:      	Workbrain Inc
 * @author 			Kevin Tsoi
 * @version 		1.0
 */
public interface ShiftPatternResolverExtPlugin 
{
	public void changeShiftPattern(String employeesNames, Date ovrStartDate, 
	        Date ovrEndDate, ShiftPatternMapper spMapper);
}
