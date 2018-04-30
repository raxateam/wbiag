package com.wbiag.tool.autotest.chart;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.workbrain.server.registry.Registry;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;

/** 
 * Title:			Chart Util
 * Description:		Util class for chart generation
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Oct 19, 2005
 * @author         	Kevin Tsoi
 */
public class ChartUtil
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ChartUtil.class);
    
    //registry definitions
    public static final String REG_TIME_PRECISION = "system/autotest/TIME_PRECISION";
    public static final String REG_TIME_UNIT = "system/autotest/TIME_UNIT";
    
    /**
     * Deletes all files with extension e in directory d 
     * 
     * @param d
     * @param e
     */
    public static void deleteFiles(String d, String e)
    {
        ExtensionFilter filter = new ExtensionFilter(e);
        File dir = new File(d);

        String[] list = dir.list(filter);
        File file;
        if (list.length == 0)
            return;

        for (int i = 0; i < list.length; i++)
        {
            file = new File(d, list[i]);
            boolean isdeleted = file.delete();
        }
    }
    
    /**
     * Gets the registry value for a given registry name.
     * 
     * @param registryName
     * @return
     */
    public static String getRegistryValue(String registryName)
    {
        String registyValue = "";
        try
        {
            registyValue = (String)Registry.getVar(registryName);
        }
        catch(Exception e)
        {
            logger.debug(e);
        }               
        return registyValue;
    } 

    /**
     * Sets the date of 'date' to the date of 'time'
     * 
     * @param date
     * @param time
     * @return
     */
    public static Date setDateToTime(Date date, Date time)
    {
        Calendar dateCal = null;
        Calendar timeCal = null;
        
        dateCal = toCalendar(date);
        timeCal = toCalendar(time);        
        timeCal.set(dateCal.get(Calendar.YEAR), dateCal.get(Calendar.MONTH), dateCal.get(Calendar.DAY_OF_MONTH));
         
        return timeCal.getTime();
    }
    
    /**
     * Returns a Calendar object from a Date object
     * 
     * @param date
     * @return
     */
    public static Calendar toCalendar( Date date )
    {
        if ( date == null )
        {
            return null;
        }
            
        Calendar cal = DateHelper.getCalendar();
        cal.setTime( date );
        return cal;
    }
    
    /**
     * Converts a String to Date based on given format.
     *
     * @param str               str
     * @param inputDateFormat  format
     * @return                 formatted date
     */
    public static Date convertStringToDate(String str,
            String inputDateFormat) 
    {
        DateFormat from = new SimpleDateFormat(inputDateFormat);
                
        try 
        {            
            return from.parse(str);            
        } 
        catch (Exception e) 
        {
            throw new NestedRuntimeException ("Could not parse : " + str, e);
        }
    }
    
    /**
     * Returns a Timestamp object from a Date object
     * 
     * @param date
     * @return
     */
    public static Timestamp toTimestamp( Date date )
    {
        if ( date == null )
        {
            return null;
        }
        
        if ( date instanceof Timestamp )
        {
            return (Timestamp) date;
        }
            
        return new Timestamp(date.getTime());
    }
    
    static class ExtensionFilter implements FilenameFilter
    {
        private String extension;

        public ExtensionFilter(String extension)
        {
            this.extension = extension;
        }

        public boolean accept(File dir, String name)
        {
            return (name.endsWith(extension));
        }
    }
}