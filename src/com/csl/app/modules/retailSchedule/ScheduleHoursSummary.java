// File: ScheduleHoursSummary.java
// Date: Jan0509
// Author: Leonardo Custodio
package com.csl.app.modules.retailSchedule;

import java.util.Date;
import java.util.Vector;
import java.util.Locale;
import java.text.*;


import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.model.ShiftDetail;
import com.workbrain.util.callouts.scheduling.ScheduleUIContext;
import com.workbrain.util.callouts.scheduling.ScheduleUIHelper;
import com.workbrain.server.jsp.JSPHelper;
import com.workbrain.server.jsp.taglib.util.WebPageRequest;
import com.workbrain.server.jsp.locale.LocalizationDictionary;
import com.workbrain.util.DateHelper;
import com.workbrain.sql.DBConnection;
import com.workbrain.security.SecurityService;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.model.WorkbrainLocaleData;

/**
 * Schedule Hours Summary customization to be placed under
 * generateCustomJSData(ScheduleUIContext) of ScheduleUICallout class. This
 * customization displays three additional rows on the schedule display screen,
 * adding hours for each day of the week for <i>assigned</i>, <i>unassigned</i>
 * and <i>total</i> categories.
 * 
 * @author Leonardo Custodio (lcustodio)
 * 
 * Change history:
 * 20090323 qvuong: 
 *    Replace deprecate methods. Add language localization. First day can be now other 
 *    that Sunday, and schedule length can be more than 7 days. 
 * 
 */

public class ScheduleHoursSummary {
    /*
     * Versioning:
     *  FindBugs 1.3.4 (20080506)
     *  J2SDK 1.4.2.13
     */
    
    // customization-specific declarations
    static final String customizationName = "Schedule Hours Summary";
    static final String version = "1.0";
    static final String release = "5.0.5.2";
    static final String author = "Leonardo Custodio (lcustodio)";

    // Localized field in workbrain_msg_locale_data table that defines custom format 
    // for weekday header column. This format is also shared with core scheduling screen. 
    static final String WEEKDAY_DATE_FORMAT = "WEEKDAY_DATE_FORMAT";
    
    /** Category ids for display purposes */
    private static final int TOTAL = 0;
    private static final int ASSIGNED = 1;
    private static final int UNASSIGNED = 2;

    /** Logger for this class. */
    public static final Logger logger = Logger.getLogger(ScheduleHoursSummary.class);

    /**
     * Places summary information into page, using JS escape variable from
     * <i>context</i>.
     * 
     * @param context
     *            ScheduleUIContext element
     */
    public static void generateSummary(ScheduleUIContext context) {
        
        if (context == null) {
        	return;
        }
            
        // declarations for schedule/shift retrieval and hours
        Vector vShiftDetail = null;
        String[] daysOfWeek = null;            
        Date scheduleFirstDate = null;             
            
        // ****************************************************************
        // First step: retrieve the schedule and initializes array to store
        // hours, per category.
        // Also initialize week day column header.
        // ****************************************************************
        float[][] totalHours = new float[3][];
            
        try {
        	Schedule oSchedule = (context.getscheduleOutput()).getSingleSchedule();
            if (oSchedule != null) {                    
                vShiftDetail = (Vector) oSchedule.getShiftDetailList();
                scheduleFirstDate = oSchedule.getSkdFromDate().getTime();                    
                   
                if (vShiftDetail != null) {
                    for (int counter = 0; counter < 3; counter++) {
                        totalHours[counter] = new float[oSchedule.getLengthInDays()];
                        for (int counterTwo = 0; counterTwo < totalHours[counter].length; counterTwo++) {
                            totalHours[counter][counterTwo] = 0f;
                        }
                    }
                      
                    daysOfWeek = new String[oSchedule.getLengthInDays()];                    
                    getDaysOfWeekHeaderCellContent( scheduleFirstDate, daysOfWeek ); 
                }
            }
        } catch (RetailException re) {
            // At this level if an error occur we can simply skip processing
            // and return to caller. We should, nevertheless, print
            // information for traceability purposes.
            logger.warn("generateSummary(): Error: unable to retrieve schedule.");
            displayInfo(org.apache.log4j.Priority.WARN);
            logger.warn(re.toString());
            return;
        }

        // ****************************************************************
        // Second step: process every shift from this schedule, collect
        // hours from each shift per category
        // ****************************************************************
        for (int i = 0; i < (vShiftDetail == null ? 0 : vShiftDetail.size()); i++) {
        	ShiftDetail oShiftDetail = (ShiftDetail) vShiftDetail.get(i);
                
            try {
                // get shift date
                Date shiftStartDate = oShiftDetail.getShftDate();                    
                   
                // retrieve start and end times
                Date startTime = oShiftDetail.getShftdetStartTime();
                Date endTime   = oShiftDetail.getShftdetEndTime();
                       
                // calculate duration of shift
                float shiftDuration = (float)(DateHelper.getMinutesBetween(endTime, startTime)/60.0); 
                
                // set hours on array                    
                int dayIdx = DateHelper.dateDifferenceInDays(shiftStartDate, scheduleFirstDate);
                totalHours[TOTAL][dayIdx] += shiftDuration;
                if (oShiftDetail.getEmpId() == null) {
                    totalHours[UNASSIGNED][dayIdx] += shiftDuration;
                } else {
                    totalHours[ASSIGNED][dayIdx] += shiftDuration;
                }
                    
            } catch (RetailException re) {
                // At this level if an error occur we can simply skip processing
                // and return to caller. We should, nevertheless, print
                // information for traceability purposes.
                logger.warn("generateSummary(): Error: unable to retrieve shift information.");
                displayInfo(org.apache.log4j.Priority.WARN);
                logger.warn(re.toString());
                return;
            }
             
        }

            
        // ****************************************************************
        // Third step: generate output values, HTML based
        // ****************************************************************
        // temporary handling variable
        StringBuffer myStr = new StringBuffer();

        // define custom CSS, Header and rows
        createCustomCSS(myStr);
        displayHeader(myStr, daysOfWeek);                 
        displayRow(getMessageLocale("Assigned"), totalHours[ASSIGNED], myStr);
        addBlankLine(myStr);
        displayRow(getMessageLocale("Unassigned"), totalHours[UNASSIGNED], myStr);
        addBlankLine(myStr);
        displayRow(getMessageLocale("Total"), totalHours[TOTAL], myStr);
        addBlankLine(myStr);
             
        // convert to JSP page footer
        myStr = ScheduleUIHelper.createCustomFooterDeclaration(new StringBuffer(), myStr.toString());
        context.addCustomJSData(myStr.toString());
            
    }
    
    
    /**
     * Display versioning information.
     * 
     * @param priority Log4j priority level
     */
    private static void displayInfo(Priority priority) {
        logger.log(priority, "\n\tName: " + customizationName + "\n\tVersion: " + version + "\n\tRelease: " + release + "\n\tAuthor: " + author);         
    }


    /**
     * Generates custom CSS to be used on this customization.
     * 
     * @param myStr StringBuffer where HTML statement are to be appended to
     */
    private static void createCustomCSS(StringBuffer myStr) {
        myStr.append("<style type=\"text/css\">");
        myStr.append("td.bestStyle { border-bottom:2px solid #7B97E0 !important;"
                + "" + "color:#7B97E0;" + "font-weight:bold;"
                + "padding-top:1em !important;" + "}");
        myStr.append("td.bestStyleNoUnder {"
                + "" + "color:#7B97E0;" + "font-weight:bold;"
                + "padding-top:1em !important;" + "}");
        myStr.append("td.bestSecondStyle {"
                + "" + "color:#A9A9A9;" + "font-weight:bold;"
                + "padding-top:1em !important;" + "}");
        myStr.append("</style>");
        

        myStr.append("<br/>");
        myStr.append("<br/>");
        
    }

    
    /**
     * Generates custom header to be used on this customization.
     * 
     * @param myStr StringBuffer where HTML statement are to be appended to
     */
    private static void displayHeader(StringBuffer myStr, String[] daysOfWeek) {
        // Header
        myStr.append("<tr style=\"margin-top: 10pt; margin-bottom: 5pt;\">");
        myStr.append("<td class=\"bestStyle\" align=\"left\" colspan=\"2\">")
             .append(getMessageLocale("Hours Summary"))
             .append("</td>");
        
        for( int i=0; i < daysOfWeek.length; i++ ) {
            myStr.append("<td class=\"bestStyle\" align=\"center\">")
                 .append(daysOfWeek[i])
                 .append("</td>");
        }
            
        myStr.append("<td class=\"bestStyle\" align=\"center\">&#8721;</td>");
        myStr.append("</tr>");
        
    }


    /**
     * Generates custom row to be used on this customization.
     * 
     * @param myStr StringBuffer where HTML statement are to be appended to
     */
    private static void displayRow(String title, float[] fs, StringBuffer myStr) {
        // Row
        myStr.append("<tr style=\"margin-top: 10pt; margin-bottom: 5pt;\">");
        myStr.append("<td class=\"bestStyleNoUnder\" align=\"left\">");
        myStr.append("<a class=\"\">"+title+"</a>");
        myStr.append("</td>");
        myStr.append("<td id=\"position\"> </td>");
        float totalLocalHours = 0;
        for (int i = 0; i < fs.length; i++) {
            myStr.append("<td class=\"bestSecondStyle\" align=\"center\">"+ fs[i] +"</td>");
            totalLocalHours += fs[i];
        }
        myStr.append("<td class=\"bestSecondStyle\" align=\"center\">" + totalLocalHours + "</td>");
        myStr.append("</tr>");

        
    }


    /**
     * Adds a blank line to the schedule screen
     * 
     * @param myStr StringBuffer where HTML statement is to be appended to
     */
    private static void addBlankLine(StringBuffer myStr) {
        myStr.append("<td class=\"frame\" colspan=\"10\">");
        myStr.append("<img height=\"1\" width=\"1\" src=\"/modules/retailSchedule/images/spacer.gif\"/>");
        myStr.append("</td>");
    }


    /**
     * Populates weekday columns header. Use WEEKDAY_DATE_FORMAT value if exists (should be a 
     * pattern of SimpleDateFormat, e.g. EEE), else use default of first letter of weekday 
     * representing the date.  
     *
     * @param startDate First day of the schedule
     * @param daysOfWeek Placeholder for weekday header to be displayed
     * 
     */
    private static void getDaysOfWeekHeaderCellContent( Date startDate, String[] daysOfWeek ) {
        Locale locale = null; 
        DateFormat df = null;
        boolean isDefaultWeekdayFormat = true;

        try {        
            String dateFormat = getMessageLocale( WEEKDAY_DATE_FORMAT );      
            locale = getLocale();

            df = new SimpleDateFormat(dateFormat, locale);
            df.format( new Date() );
            isDefaultWeekdayFormat = false;
        }
        catch( Exception e ) {
            df = new SimpleDateFormat("E", locale);
        }
       
        for( int i = 0; i < daysOfWeek.length; i++ ) {
            Date curDate = DateHelper.addDays( startDate, i );           
            daysOfWeek[i] = isDefaultWeekdayFormat ? df.format(curDate).substring(0, 1) : df.format(curDate);
        }
        
    }
    
    /**
     * Return current wb_user's Locale
     * @return
     */
    private static Locale getLocale() {
        try {
            DBConnection conn = DBInterface.getCurrentConnection();
            int localeId = SecurityService.getCurrentUser().getUserLocale();
    
            CodeMapper codeMapper = CodeMapper.createCodeMapper(conn);
            WorkbrainLocaleData workbrainLocale = codeMapper.getLocaleById(localeId);
    
            return new Locale(workbrainLocale.getWbllLanguage());
        }
        catch (Exception exception) {
            throw new RuntimeException("Was unable to get user locale", exception);
        }
    } 
    
    
    /**
     * Return localized text from workbrain_msg_locale_data. Text can be configured in 
     * the application via System Administration > Workbrain Domains
     * 
     * @param msgKey 
     * @return
     */
    private static String getMessageLocale( String msgKey ) {        
        WebPageRequest wpr = JSPHelper.getCurrentWebPageRequest();
        LocalizationDictionary md = LocalizationDictionary.get();
        String locText = null;
        try {
            locText = md.localize( wpr, wpr.getDomain(), msgKey );
        }           
        catch( Exception e ) {
            if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) {              
                logger.debug( e.getMessage() );
            }
        }
        return (locText == null ? msgKey : locText);        
    }
    
}
