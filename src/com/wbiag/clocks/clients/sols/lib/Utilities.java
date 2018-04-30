/*
 * Created on Dec 1, 2003
 *
 */
package com.wbiag.clocks.clients.sols.lib;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.w3c.dom.NamedNodeMap;

import sun.misc.BASE64Encoder;

//import com.workbrain.clocks.server.ClockServer;

import java.io.*;
import java.math.BigDecimal;

/**
 * Utilities class for Clock server.
 * @author rbujko
 *
 * @version %I% %G%
 */
public class Utilities {
    private static Logger logger = Logger.getLogger(Utilities.class);

    /**
     * number of hours per day.
     */
    public static final long hoursPerDay = 24;
    /**
     * number of minutes per day.
     */
    public static final long minutesPerDay = hoursPerDay * 60;
    /**
     * number of seconds per day.
     */
    public static final long secondsPerDay = minutesPerDay * 60;
    /**
     * number of miliseconds per day.
     */
    public static final long milisecondsPerDay = secondsPerDay * 1000;

    /**
     * left justification for messages formating.
     */
    public final static String PAD_LEFT = "LEFT";
    /**
     * right justification for messages formating.
     */
    public final static String PAD_RIGHT = "RIGHT";

    private static final ThreadLocal calCache = new ThreadLocal()
    {
        protected Object initialValue()
        {
            return Calendar.getInstance();
        }
    };


    /**
     * Will left pad or right pad the String using the specified pad character
     * In case that the input String size is BIGGER than the desired one it will 
     * left cut or right cut ONLY padding characters!!.
     * @param targetString String
     * @param desiredLength int
     * @param paddingType String
     * @param padChar char
     * @return String
     */
    public static String formatString(String targetString, int desiredLength, String paddingType, char padChar) {
        String s = null;
        if ((null == targetString) || (targetString.length() == 0)) {
            return padLeft("", desiredLength, padChar);
        }
        if (targetString.length() == desiredLength) {
            s = targetString;
        } else if (targetString.length() < desiredLength) {
            if (paddingType.equalsIgnoreCase(PAD_LEFT)) {
                s = padLeft(targetString, desiredLength, padChar);
            } else {
                s = padRight(targetString, desiredLength, padChar);
            }
        } else if (targetString.length() > desiredLength) {
            if (paddingType.equalsIgnoreCase(PAD_LEFT)) {
                s = leftCut(targetString, desiredLength, padChar);
            } else {
                s = rightCut(targetString, desiredLength, padChar);
            }
        }
        return s;
    }

    /**
     *  Formats an integer to an HH:MM string (Example: 200 will be transformed into 02:00).
     *  @param time The integer representing a time value that needs to be converted.
     *  @return The converted integer as an HH:MM string.
      */
    public static String formatIntToHHMM(int time) {
        String paddedString = Utilities.formatString(String.valueOf(time), 4, Utilities.PAD_LEFT, '0');
        return paddedString.substring(0, 2) + ":" + paddedString.substring(2);
    }

    private static String leftCut(String targetString, int desiredLength, char padChar) {
        String returnString = targetString;

        for (int i = targetString.length(); i > desiredLength; i--) {
            if (returnString.charAt(0) == padChar) {
                returnString = returnString.substring(1, returnString.length());
            } else {
                return returnString;
            }
        } //end for
        return returnString;
    }

    private static String rightCut(String targetString, int desiredLength, char padChar) {
        String returnString = targetString;

        for (int i = targetString.length(); i > desiredLength; i--) {
            if (returnString.charAt(returnString.length() - 1) == padChar) {
                returnString = returnString.substring(0, returnString.length() - 1);
            } else {
                return returnString;
            }
        } //end for
        return returnString;
    }

    private static String padLeft(String targetString, int desiredLength, char padChar) {
        String returnString = targetString;
        for (int i = targetString.length(); i < desiredLength; i++) {
            returnString = padChar + returnString;
        }
        return returnString;
    }

    private static String padRight(String targetString, int desiredLength, char padChar) {
        String returnString = targetString;
        for (int i = targetString.length(); i < desiredLength; i++) {
            returnString += padChar;
        }
        return returnString;
    }

    /**
     * Get current date and time as a string of format YYYYMMDDHHmmss.
     * @return String
     */
    public static String currentDateTimeToString() {
        GregorianCalendar cal = new GregorianCalendar();
        return cal.get(GregorianCalendar.YEAR)
            + pad(cal.get(GregorianCalendar.MONTH) + 1)
            + pad(cal.get(GregorianCalendar.DAY_OF_MONTH))
            + pad(cal.get(GregorianCalendar.HOUR_OF_DAY))
            + pad(cal.get(GregorianCalendar.MINUTE))
            + pad(cal.get(GregorianCalendar.SECOND));
    }

    /**
     * Helper method for the Synel protocol emulator.
     * This method returns the currentDateTimeToString if the inDST parameter is set to false.
     * If the inDST parameter is set to true, the first digit of the hour will converted to a p, q or r.
     * <P>
     * <B>Example: </B><BR>
     * January 3rd, 2004 15:00:59 returns 20040103150059 <BR>
     * January 3rd, 2004 15:00:59 DST returns 20040103q50059
     * @param inDST True if in DST, false if in ST.
     * @see #currentDateTimeToString()
     * @return The current date and time as a string formatted in the same way that the Synel readers would format it.
      */
    public static String currentSynelDateTimeToString(boolean inDST){
        if (!inDST) {
            return currentDateTimeToString();
        } else {
            StringBuffer currentDateTime = new StringBuffer(currentDateTimeToString());
            if (currentDateTime.charAt(8) == '0')  {
                currentDateTime.replace(8, 9, "p");
            } else if (currentDateTime.charAt(8) == '1')  {
                currentDateTime.replace(8, 9, "q");
            } else {
                currentDateTime.replace(8, 9, "r");
            }
            return currentDateTime.toString();
        }
    }
    
    public static String currentSynelDateTimeToString(){
        GregorianCalendar cal = new GregorianCalendar();
        return currentSynelDateTimeToString(cal.get(GregorianCalendar.DST_OFFSET)==0);
    }

    /**
     * Get current date as String.
     * @return The current date as a string in the format YYYYMMDD.
     */
    public static String currentDateToString() {
        GregorianCalendar cal = new GregorianCalendar();
        return cal.get(GregorianCalendar.YEAR) + pad(cal.get(GregorianCalendar.MONTH) + 1) + pad(cal.get(GregorianCalendar.DAY_OF_MONTH));
    }

    /**
     * Get current time as String.
     * @return The current time as a string in the format HHmmss
     */
    public static String currentTimeToString() {
        GregorianCalendar cal = new GregorianCalendar();
        return pad(cal.get(GregorianCalendar.HOUR_OF_DAY)) + pad(cal.get(GregorianCalendar.MINUTE)) + pad(cal.get(GregorianCalendar.SECOND));
    }

    /**
     * Helper method for the Synel protocol emulator.
     * This method returns the currentTimeToString if the inDST parameter is set to false.
     * If the inDST parameter is set to true, the first digit of the hour will converted to a p, q or r.
     * <P>
     * <B>Example: </B><BR>
     * 15:00:59 returns 150059 <BR>
     * 15:00:59 DST returns q50059
     * @param inDST True if in DST, false if in ST.
     * @see #currentTimeToString()
     * @return The current time as a string formatted in the same way that the Synel readers would format it.
      */
    public static String currentSynelTimeToString(boolean inDST) {
        if (!inDST) {
            return currentTimeToString();
        } else {
            StringBuffer currentTime = new StringBuffer(currentTimeToString());
            if (currentTime.charAt(0) == '0')  {
                currentTime.replace(0, 1, "p");
            } else if (currentTime.charAt(0) == '1')  {
                currentTime.replace(0, 1, "q");
            } else {
                currentTime.replace(0, 1, "r");
            }
            return currentTime.toString();
        }
    }

    /**
     * Helper method for the Synel protocol emulator.
     * This method returns the timestamp if the inDST parameter is set to false.
     * If the inDST parameter is set to true, the first digit of the hour will converted to a p, q or r.
     * <P>
     * <B>Example: </B><BR>
     * 15:00:59 returns 150059 <BR>
     * 15:00:59 DST returns q50059
     * @param inDST True if in DST, false if in ST.
     * @return The current time as a string formatted in the same way that the Synel readers would format it.
      */
    public static String formatSynelTimeToString(String timestamp, boolean inDST) {
        if (!inDST) {
            return timestamp;
        } else {
            StringBuffer currentTime = new StringBuffer(timestamp);
            if (currentTime.charAt(0) == '0')  {
                currentTime.replace(0, 1, "p");
            } else if (currentTime.charAt(0) == '1')  {
                currentTime.replace(0, 1, "q");
            } else {
                currentTime.replace(0, 1, "r");
            }
            return currentTime.toString();
        }
    }

    /**
     * Get date ant time as String.
     * @param year an int
     * @param month an int
     * @param day an int
     * @param hour an int
     * @param minutes an int
     * @param seconds an int
     * @return The string representation the date time values provided in the format YYYYMMDDHHmmss
     */
    public static String dateTimeToString(int year, int month, int day, int hour, int minutes, int seconds) {
        return year + pad(month) + pad(day) + pad(hour) + pad(minutes) + pad(seconds);
    }

    /**
     * Get Date as String.
     * @param date a Date
     * @return String
     */
    public static String dateTimeToString(Date date) {
        if (date == null) {
            return "";
        }
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return cal.get(GregorianCalendar.YEAR)
            + pad(cal.get(GregorianCalendar.MONTH) + 1)
            + pad(cal.get(GregorianCalendar.DAY_OF_MONTH))
            + pad(cal.get(GregorianCalendar.HOUR_OF_DAY))
            + pad(cal.get(GregorianCalendar.MINUTE))
            + pad(cal.get(GregorianCalendar.SECOND));
    }

    /**
     * Get date as String.
     * @param year an int
     * @param month an int
     * @param day an int
     * @return String
     */
    public static String dateToString(int year, int month, int day) {
        return year + pad(month) + pad(day);
    }

    /**
     * Get time as String.
     * @param hour an int
     * @param minutes an int
     * @param seconds an int
     * @return String
     */
    public static String timeToString(int hour, int minutes, int seconds) {
        return pad(hour) + pad(minutes) + pad(seconds);
    }

    private static String pad(int value) {
        if (value < 10) {
            return "0" + value;
        } else {
            return "" + value;
        }
    }
    /**
     * Get transaction header for old clocks.
     * @param elementName String element name
     * @return String
     */
    public static String getTransactionHeader(String elementName) {
        return "<CXIXML>\n\t<" + elementName + ">\n";
    }

    /**
     * Get transaction footer for old clocks.
     * @param elementName String element name
     * @return String
     */
    public static String getTransactionFooter(String elementName) {
        return "\t</" + elementName + ">\n</CXIXML>";
    }

    /**
     * Convert date and time in string version to date object.
     * @param timeValue String
     * @return Date
     */
    public static Date stringTimeToDate(String timeValue) {
        int year;
        int month;
        int day;
        int hour;
        int minute;
        int second;
        if (null == timeValue) {
            logger.error("Null TimeValue received in stringTimetoDate");
            return getDefaultDate();
        }
        try {
            year = Integer.parseInt(timeValue.substring(0, 4)) - 1900;
        } catch (NumberFormatException nfe) {
            year = 0;
        }
        try {
            month = Integer.parseInt(timeValue.substring(4, 6)) - 1;
        } catch (NumberFormatException nfe) {
            month = 0;
        }
        try {
            day = Integer.parseInt(timeValue.substring(6, 8));
        } catch (NumberFormatException nfe) {
            day = 1;
        }
        try {
            if (timeValue.length() > 8) {
                hour = Integer.parseInt(timeValue.substring(8, 10));
            } else {
                hour = 0;
            }
        } catch (Exception nfe) {
            hour = 0;
        }
        try {
            if (timeValue.length() > 10) {
                minute = Integer.parseInt(timeValue.substring(10, 12));
            } else {
                minute = 0;
            }
        } catch (Exception nfe) {
            minute = 0;
        }
        try {
            if (timeValue.length() > 12) {
                second = Integer.parseInt(timeValue.substring(12, 14));
            } else {
                second = 0;
            }
        } catch (Exception nfe) {
            second = 0;
        }

        return getDate(year + 1900, month, day, hour, minute, second);
    }

    /**
     * Will convert a String time to a Date and throw Exception if errors.
     * @param timeValue String
     * @return Date
     * @throws NumberFormatException
     */
    public static Date stringToDate(String timeValue) throws NumberFormatException {
        int year;
        int month;
        int day;
        int hour;
        int minute;
        int second;

        if (null == timeValue) {
            throw new NumberFormatException("Could not convert null String to Date!");
        }

        try {
            year = Integer.parseInt(timeValue.substring(0, 4)) - 1900;
            month = Integer.parseInt(timeValue.substring(4, 6)) - 1;
            day = Integer.parseInt(timeValue.substring(6, 8));

            if (timeValue.length() > 8) {
                hour = Integer.parseInt(timeValue.substring(8, 10));
            } else {
                hour = 0;
            }

            if (timeValue.length() > 10) {
                minute = Integer.parseInt(timeValue.substring(10, 12));
            } else {
                minute = 0;
            }

            if (timeValue.length() > 12) {
                second = Integer.parseInt(timeValue.substring(12, 14));
            } else {
                second = 0;
            }

        } catch (IndexOutOfBoundsException ioex) {
            throw new NumberFormatException("Could not convert " + timeValue + " to a Date object");
        }

        return getDate(year + 1900, month, day, hour, minute, second);
    } //end stringTodate

    /**
     * returns a default old date so tgat we know something was wrong.
     * @return Date
     */
    public static Date getDefaultDate() {
        return getDate(1900, 0, 1, 0, 0, 0);
    }
    /**
     * returns a date build from number values for year, month... to seconds.
     * @param year an int
     * @param month an int
     * @param day an int
     * @param hour an int
     * @param minute an int
     * @param second an int
     * @return Date
     */
    public static Date getDate(int year, int month, int day, int hour, int minute, int second) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        return cal.getTime();
    }

    /**
     * Copy file.
     * @param in File
     * @param out File
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void CopyFile(File in, File out) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(in);
        FileOutputStream fos = new FileOutputStream(out);
        if (null == in || null == out) {
            throw new FileNotFoundException("Null file object received!");
        }
        byte[] buf = new byte[1024];
        int i = 0;
        while ((i = fis.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
    }

    /**
     * Format String to given length.
     * @param targetString String
     * @param desiredStringLength int
     * @return String
     */
    public static String formatString(String targetString, int desiredStringLength) {
        String returnString = null;
        if (targetString.length() == desiredStringLength) {
            returnString = targetString;
        } else if (targetString.length() < desiredStringLength) {
            returnString = targetString;
            for (int i = targetString.length(); i < desiredStringLength; i++) {
                returnString += " ";
            }
        } else if (targetString.length() > desiredStringLength) {
            returnString = targetString.substring(0, desiredStringLength);
        }
        return returnString;
    }

    /**
     * Format string to two lines 16 char display.
     * @param firstLine String
     * @param secondLine String
     * @return String
     */
    public static String formatStringFor16CharDisplay(String firstLine, String secondLine) {
        return formatString(firstLine, 16) + formatString(secondLine, 16);
    }
    /**
     * Add final backslash to directory name.
     * @param aPath String
     * @return String
     */
    public static String addFinalSlash(String aPath) {
        if (aPath.endsWith("\\") || aPath.endsWith("/")) {
            return aPath;
        } else {
            return aPath + "/";
        }
    }

    /**
     * Converts a java date to a delphi TDateTime.
     * @param javaDate The date that needs to be converted
     * @return A Delphi TDateTime of the form (XXXXX.xxxxxxxxxx) where XXXXX is the number of
     * days since 30th of December 1899 and xxxxxxxxxxx is the fractions of the day.
     */
    public static double javaDateToDelphiDate(Date javaDate) {
        GregorianCalendar delphiCal = new GregorianCalendar(1899, 11, 30, 0, 0, 0);
        GregorianCalendar javaCal = new GregorianCalendar();
        javaCal.setTime(javaDate);
        long delphiMillis = delphiCal.getTime().getTime();
        long javaMillis = javaCal.getTime().getTime();
        long numberOfMiliseInBetweenDates = javaMillis - delphiMillis;
        long numberOfDaysInBetweenDates = numberOfMiliseInBetweenDates / milisecondsPerDay;
        long millisecondsRemainder = numberOfMiliseInBetweenDates - (numberOfDaysInBetweenDates * milisecondsPerDay);
        double portionsOfDay = (double) millisecondsRemainder / (double) milisecondsPerDay;
        BigDecimal result = new BigDecimal(numberOfDaysInBetweenDates + portionsOfDay);
        return result.setScale(10, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * Pass in the path to a file and it will return the list of commands as String
     * It will delete quotes "" and it wil ignore commented lines ; (; is the comment sign). 
     * @param path String
     * @return array of String
     */
    public static String[] getFileAsStrings(String path) {

        if (null == path) {
            return null;
        }

        ArrayList arrayList = new ArrayList();

        //Read the XML file
        File file = new File(path);
        if (null == file || (false == file.exists())) {
            logger.warn("Could not find file: " + path);
            return null;
        }

        String data = "";

        //Iterate through the file and insert one String per line
        try {
            FileReader fr = new FileReader(path);
            BufferedReader reader = new BufferedReader(fr);
            String line = null;
            line = reader.readLine();
            //Nedd to have at least 2 quotes : ""
            while (null != line) {
                line = line.trim();
                //Delete the quotes
                if ((line.length() > 1) && ('"' == line.charAt(0)) && ('"' == line.charAt(line.length() - 1))) {
                    data = line.substring(1, line.length() - 1);
                    if (!data.equals("")) {
                        arrayList.add(data);
                    }

                } //end if ;
                line = reader.readLine();
            } //end while
        } catch (FileNotFoundException fnfe) {
            logger.error("Could not find file " + path, fnfe);
            return null;
        } catch (IOException ioex) {
            logger.error("IOException while processing file: " + path, ioex);
            return null;
        }

        //Need to specify an array type as a parameter
        return (String[]) arrayList.toArray(new String[0]);

    }

    /**
     * Get element name from xml node.
     * @param node NamedNodeMap
     * @param desc String
     * @return String
     */
    public static String getElement(NamedNodeMap node, String desc) {
        if ((null == node) || (null == desc)) {
            return "";
        }
        String str = node.getNamedItem(desc) != null ? node.getNamedItem(desc).getNodeValue() : "";
        str = str.trim();
        return str;
    }

    /**
     * Force the message to appear in the provided logger by temporarily setting its level to INFO.
     * @param log Logger
     * @param message String
     * 
     * NOTE: This fix (TT81321) relies on current implementation of log4j which also locks 
     * the Logger object inside Category#callAppenders(LoggingEvent event) method.
     */
    public static void forceINFOLog(Logger log, String message) {
        // TT81321 - Protect against simultaneous attempt to send another message to the same logger while its level is changed
        synchronized(log){
            Level oldLevel = log.getLevel();
            log.setLevel(Level.INFO);
            log.info(message);
            log.setLevel(oldLevel);            
        }
    }

    /**
     * If null == previous or null == current will return false
     * Else will compare the two Strings for common prefixes endimg in "."
     * For example: class.reader1 and class.reader2 - will return true
     * class.validation.reader and class.parser.1 will return false.
     * @param current String
     * @param previous String
     * @return boolean
     */
    public static boolean isSameGroup(String current, String previous) {
        if ((null == previous) || (null == current)) {
            return false;
        }

        if (("".equals(previous)) || "".equals(current)) {
            return false;
        }

        //get the root up to the "." sign 
        int currentRoot = current.lastIndexOf(".");
        int previousRoot = previous.lastIndexOf(".");

        if ((currentRoot < 0) && (previousRoot < 0)) {
            return true;
        }

        if ((currentRoot > 0) && (previousRoot > 0)) {
            String strCurrentRoot = current.substring(0, currentRoot);
            String strPreviousRoot = previous.substring(0, previousRoot);
            if (strCurrentRoot.equalsIgnoreCase(strPreviousRoot)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    /**
     * Returns the last item from a String of type aaa.bbb.ccccc - returns ccccc.  
     * @param str A string in format aaa.bbb.cccc
     * @return The last item in the string (i.e. cccc).
     */
    public static String getLastItem(String str) {
        if ((null == str)) {
            return str;
        }

        int index = str.lastIndexOf('.');
        if (index < 0) {
            return str;
        }

        if (index >= str.length()) {
            //Empty after .
            return "";
        } else {
            return str.substring(index + 1);
        }
    } //end getLastItem

    /**
     * Replaces the last item from a String of type aaa.bbb.ccccc - returns ccccc.  
     * @param str A string in format aaa.bbb.cccc
     * @param lastItem The string the last item should be replaced with. 
     * @return The last item in the string (i.e. cccc).
     */
    public static String replaceLastItem(String str, String lastItem) {
        if ((null == str) || (null == lastItem)) {
            return str;
        }

        int index = str.lastIndexOf('.');
        if (index < 0) {
            return str;
        }

        if (index >= str.length()) {
            //Empty after .
            str = str + lastItem;
            return str;
        } else {
            String root = str.substring(0, index + 1);
            return root + lastItem;
        }
    } //end getLastItem

    /**
     * Use this function when you want to substitute certain patterns in a string.
     * The substitutionMap contains the replacement values
     * The string patterns need to be of the form '1stDelim''2ndDelim''AnyChar'
     * This routine looks for the '1stDelim', removes it, takes the second & keylength chars
     * and searches for their replacement in the substitution table.
     * <BR>
     * Here is an example:  <BR>
     * targetString: Welcome ^&F, ^&L, ^&S   <BR>
     * Map: &F=John &L=Doe &S=ON  <BR>
     * firstDelim: '^'  <BR>
     * secondDelim: '&' <BR>
     * <BR>
     * Return String: Welcome John, Doe, ON
     * <BR>
     * <B>NOTE:</B> If the substitutionTable is empty or null the return value is targetString. <BR>
     * <B>NOTE:</B> If this method does not find an entry in the substitutionTable for a key, it leaves the string unmodified for that key.
     * @param targetString The string to be formatted.
     * @param substitutionTable A map of substitution strings. All keys must have the same lengths and need to start 
     * with the same 2 characters. 
     * @param firstDelimiter The first delimiter character in the key.
     * @param secondDelimiter The second delimiter character in the key.
     * @param keyLength The length of the key in the substitution map. This key needs to have the same length for all substituted values.
     * <BR> <B>NOTE</B> The key in the map usually includes the second delimiter. 
     * @return The formatted string.
     */
    public static String formatSmartString(String targetString, Map substitutionTable, char firstDelimiter, char secondDelimiter, int keyLength) {
        if (targetString == null || substitutionTable == null || substitutionTable.size() == 0) {
            return targetString;
        }
        StringTokenizer st = new StringTokenizer(targetString, String.valueOf(firstDelimiter));
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreElements()) {
            String candidate = (String) st.nextElement();
            if (candidate.charAt(0) == secondDelimiter) {
                String stringToBeReplaced = (String) substitutionTable.get(candidate.substring(0, keyLength));
                if (stringToBeReplaced != null) {
                    sb.append(stringToBeReplaced).append(candidate.substring(keyLength));
                } else {
                    logger.debug("Cannot substitute: \"" + candidate.substring(0, keyLength) + "\" in this context");
                    sb.append(firstDelimiter).append(candidate);
                }
            } else {
                sb.append(candidate);
            }
        }
        return sb.toString();
    }

    /**
     * Parses the input String into an array using the delimiters as separators.
     * @param data String
     * @param delimiter String
     * @return array of String
     */
    public static String[] parseStringArray(String data, String delimiter) {
        if ((null == data) || (data.length() == 0)) {
            return new String[0];
        }
        if (null == delimiter) {
            String[] obj = new String[1];
            obj[0] = data;
            return obj;
        }
        ArrayList returnList = new ArrayList();
        int begin = 0;
        int end = data.indexOf(delimiter);
        if (-1 == end) {
            //We only have one element
            returnList.add(data);
            return (String[]) returnList.toArray(new String[returnList.size()]);
        }
        String str = null;
        while (end > 0) {
            logger.debug("begin=" + begin + ", end=" + end);
            str = data.substring(begin, end);
            returnList.add(str);
            begin = end + delimiter.length();
            end = data.indexOf(delimiter, begin);
        }

        logger.debug("end=" + end + "data.length=" + data.length());

        if ((end != data.length() - delimiter.length())) {
            //Add the last one
            str = data.substring(begin, data.length());
            if ((null != str) && (str.length() > 0)) {
                returnList.add(str);
            }
        }

        logger.debug("parseStringArray returning " + returnList);
        if (returnList != null && returnList.size() > 0) {
            String[] ret = new String[returnList.size()];
            returnList.toArray(ret);
            return ret;
        } else {
            return new String[0];
        }
    }
    /**
     * convert raw basge from reader (simple numbers) into standard form for 
     * our system.
     * @param badgeNumber a String
     * @param plantPrefix a String
     * @param badgeDelimiter a String
     * @param badgeLength a String
     * @return String
     */
    public static String formatBadge(String badgeNumber, String plantPrefix, String badgeDelimiter, String badgeLength) {
        int len = 0;
        try {
            len = Integer.parseInt(badgeLength.trim());
        } catch (Exception e) {
            len = 0;
        }
        return formatBadge(badgeNumber, plantPrefix, badgeDelimiter, len);
    }
    /**
     * convert raw basge from reader (simple numbers) into standard form for 
     * our system.
     * 
     * @param badgeNumber a String
     * @param plantPrefix a String
     * @param badgeDelimiter a String
     * @param badgeLength an int
     * @return String
     */
    public static String formatBadge(String badgeNumber, String plantPrefix, String badgeDelimiter, int badgeLength) {
        String p = plantPrefix;
        String d = badgeDelimiter;
        if (p == null) {
            p = "";
        }
        if (d == null) {
            d = "";
        }
        String ret = badgeNumber;
        if (badgeLength != 0) {
            StringBuffer sb = new StringBuffer("");
            if (badgeLength > badgeNumber.length()) {
                badgeLength -= badgeNumber.length();
                for (int i = 0; i < (badgeLength); i++) {
                    sb.append("0");
                }
                sb.append(badgeNumber);
            } else if (badgeLength < badgeNumber.length()) {
                badgeLength = badgeNumber.length() - badgeLength;
                int i = 0;
                for (; i < (badgeLength); i++) {
                    if (badgeNumber.charAt(i) != '0') {
                        break;
                    }
                }
                for (; i < (badgeNumber.length()); i++) {
                    sb.append((badgeNumber.charAt(i)));
                }
            } else {
                sb.append(badgeNumber);
            }
            ret = sb.toString();
        }
        return (p + d + ret);
    }

    /**
    * Replace the file with the new String. 
    * @param filePath The path where the file to be replaced resides.
    * @param data The new data that will overwrite the file.
    * @throws ReaderServerException - can't update the Settings file 
    */
    public static void replaceFileData(String filePath, String data) throws ReaderServerException {
        BufferedWriter writer = null;
        try {
            //We have the path top the setting file in this.settingsFilePath
            File file = new File(filePath);

            if (!file.exists()) {
                throw new InitializationException("Could not find file: " + filePath);
            }

            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
            writer.write(data);

        } catch (IOException ioex) {
            logger.error("IOException while reading file as String: " + filePath, ioex);
            throw new ReaderServerException("IOException while loading file as String " + filePath);
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException ioex) {
                    logger.error("Error closing file: " + filePath, ioex);
                    throw new ReaderServerException("IOException while closing file " + filePath);

                }

            }
        } //end try catch

    }

    /**
    * Get the file with the new String. 
    * @return A String with all the data from the file
    * @param filePath  The path where the file is located.
    * @throws ReaderServerException - can't read the file 
    */
    public static String getFileData(String filePath) throws ReaderServerException {
        BufferedReader reader = null;

        StringBuffer strBuff = new StringBuffer("");
        try {
            //We have the path top the setting file in this.settingsFilePath
            File file = new File(filePath);

            if (!file.exists()) {
                throw new InitializationException("Could not find file: " + filePath);
            }

            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));

            String str;
            while ((str = reader.readLine()) != null) {
                strBuff.append(str);
                strBuff.append("\n");
            }

        } catch (IOException ioex) {
            logger.error("IOException while reading file as String: " + filePath, ioex);
            throw new ReaderServerException("IOException while loading file as String " + filePath);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException ioex) {
                    logger.error("Error closing file: " + filePath, ioex);
                    throw new ReaderServerException("IOException while closing file " + filePath);

                }

            }
        } //end try catch

        return strBuff.toString();

    } //end getFileData
    
    /**
     * Read the Settings file and return it as a String.
     * @param fileName - settings file name
     * @return String    Setting file as String
     * @throws Exception - can't load Settings file in a String
     */
     private static String getSettingsFileAsString(String fileName) throws Exception {
         BufferedReader reader = null;
         
         StringBuffer strReturn = new StringBuffer();
         //We have the path top the setting file in this.settingsFilePath
         File file = new File(fileName);
         
         if (!file.exists()) {
             throw new IOException("Could not find Settings file: " + fileName);
         }
         reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
         String line = reader.readLine();
         while (null != line) {
             strReturn.append(line);
             strReturn.append("\n");
             line = reader.readLine();
         } //end while
         
         return strReturn.toString();
     }
    
    /**
     * Used for getting a list of settings values sorted by key.<BR>
     * <B>Example:</B><BR>
     * Map: <BR>
     * key: host.list.003, value: localhost <BR>
     * key: host.list.001, value: server1 <BR>
     * key: host.list.002, value: server2 <BR>
     * Substring: host.list. <BR>
     * Return: [server1, server2, localhost] <BR>
     * @param settingsMap A hashmap containing usually containing settings (all or just a subset).
     * @param keySubstring A substring specifying which subset of keys are we looking for. A valid example will 
     * be class.validation
     * @return A sorted array of strings that contains the values for the keys starting with the given substring.
     */
    public static String[] getSortedValuesByKey(Map settingsMap, String keySubstring){
        Hashtable subsetTable = new Hashtable();
        Iterator iter = settingsMap.keySet().iterator();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (key == null) {
                continue;
            }
            if (key.startsWith(keySubstring)) {
                subsetTable.put(key, settingsMap.get(key));
            }
        }
        //now sort the elements and creat the array of host names;
        TreeSet sortedSet = new TreeSet(subsetTable.keySet());
        Iterator sortedIterator = sortedSet.iterator();
        String[] returnArray = new String[sortedSet.size()];
        int arrayCounter = 0;
        while (sortedIterator.hasNext()) {
            String key = (String) sortedIterator.next();
            returnArray[arrayCounter++] = (String)settingsMap.get(key);
        }
        return returnArray;
    }
    
    /**
     * Reads an integer property from Properties by Key. 
     * @return integer value of the property or default value if property not found
     * @param props  Properties object
     * @param propertyKey String Key
     * @param defaultValue  int default value, if property not found
     */
    public static int getIntPropertyByKey(Properties props, String propertyKey, int defaultValue){
        try {
            return Integer.parseInt(props.getProperty(propertyKey,String.valueOf(defaultValue)));
        } catch (NumberFormatException nfex) {
            logger.warn(propertyKey + " is not an integer. Check the configuration file. Using defualt value " + String.valueOf(defaultValue));
            return defaultValue;
        }
    }

    /**
     * Reads a boolean property from Properties by Key.
     * @return boolean value of the property or default value if property not found
     * @param props  Properties object
     * @param propertyKey String Key
     * @param defaultValue boolean default value, if property not found
     */
    public static boolean getBooleanPropertyByKey(Properties props, String propertyKey, boolean defaultValue){
        String prop = (String) props.get(propertyKey);

        if(prop == null){
            return defaultValue;
        }
        else{
            return getBooleanFromString(prop.trim());
        }
    }

    /**
     * Returns the index of the string value in the string array  
     * @return integer index of value in the array or -1 if not found in the array
     * @param value String value to find
     * @param list String [], array in which method searches
     */
    public static int inStringArray(String value, String[] list){
        for (int i=0; i<list.length;i++){
            if (list[i].equals(value)){
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Moves particular item to the top of the array (index 0).  
     * @return String[] updated array
     * @param list - initial array
     * @param index - index of the element to be moved to the top of the array
     */
    public static String[] moveTop(String[] list, int index){
        if (index<=0 || index>=list.length){
            return list;
        }
        else {
            String [] result= new String[list.length];
            result[0]=list[index];
            int i;
            for(i=1; i<=index; i++){
                result[i]=list[i-1];
            }
            for(i=index+1;i<list.length;i++){
                result[i]=list[i];
            }
            return result;
        }
    }
    /**
     * Saves the list of string values as properties to the file. If properties already exists method will overwrite them.
     * @return True of False if operation successful or not.
     * @param props  Properties object to modify
     * @param pFile Files to store the properties
     * @param list String[] of values to store
     * @param key Key for the properties. The values will be stored in the same order as they appear in the list. "key.###" where ### is the index of the value in the list.
     */    
    public static boolean saveListOfProperties(Properties props, File pFile, String[] list, String key){
        String idx;
        try {
            FileOutputStream propertiesStream = new FileOutputStream(pFile);
            for (int i=0; i<list.length; i++){
                idx=String.valueOf(i);
                while (idx.length()<3){
                    idx="0"+idx;
                }
                props.setProperty(key+idx,list[i]);
            }
            props.store(propertiesStream,null);
            propertiesStream.close();
        } catch (IOException ioe) {
            if (logger.isEnabledFor(org.apache.log4j.Level.WARN)) {
                logger.warn(ioe.getMessage(), ioe);
            }
            return false;
        }
        return true;
    }

    
    /**
     * Helper method for converting back a Map.toString generated string into a map.
     * @return A map with all the entries in the input string. 
     * @param aString A string generated from a Map.toString method call.
      */
    public static Map stringToHashMap(String aString) {
        Map returnMap = new HashMap();
        StringTokenizer st = new StringTokenizer(aString.substring(1, aString.length() -1), ",");
        while (st.hasMoreElements()) {
            String token = (String) st.nextElement();
            if (token != null) {
                int marker = token.indexOf("=");
                if (marker != -1) {
                    returnMap.put(token.substring(0, marker).trim(), token.substring(marker + 1).trim());
                } else {
                    returnMap.put(token, null);
                }
            }
        }
        return returnMap;
    }

    /**
     * Helper method for waiting a few milliseconds.
     * @param millis The amount of milliseconds to wait.
      */
    public static void waitForXMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie){
            logger.error("Interupted while waiting " + millis + " milliseconds before retrying");
        }
    }
    
    // WORKBRAIN CLOCK SERVER reversed.
    private static final String encodeString = "NIARBKROWKCOLCREVRES";  
    
    /**
     * Checks if specified settings key is a password key (ends with .password).
     * @param keyName key to be checked
     * @return true if this is a password key
     */
    public static boolean isPasswordKey(String keyName) {
        final String passwordKey = ".password";
        int index = keyName.indexOf(passwordKey);
        if (index != -1 && keyName.length() == (index + passwordKey.length())) {
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * This method generates a boolean value from the given string.
     * @param str The boolean value as a string
     * @return true if the argument is "Y", "YES", "T", or "TRUE" (case-insensitive);
     * false otherwise, including null
      */
    public static boolean getBooleanFromString(String str){
        if ("Y".equalsIgnoreCase(str) || "YES".equalsIgnoreCase(str) || "T".equalsIgnoreCase(str) || "TRUE".equalsIgnoreCase(str)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * This method generates a boolean value from the given string.
     * @param str The boolean value as a string
     * @param defaultValue Default Value if string not equal to (case-insensitive) "Y", "YES", "T", "TRUE", "N", "NO", "F", or "FALSE"
     * @return true if the argument is "Y", "YES", "T", or "TRUE" (case-insensitive);
     * false otherwise, including null
      */
    public static boolean getBooleanFromString(String str, boolean defaultValue){
        
        if ("Y".equalsIgnoreCase(str) || "YES".equalsIgnoreCase(str) || "T".equalsIgnoreCase(str) || "TRUE".equalsIgnoreCase(str)) {
            return true;
        }

        if ("N".equalsIgnoreCase(str) || "NO".equalsIgnoreCase(str) || "F".equalsIgnoreCase(str) || "FALSE".equalsIgnoreCase(str)) {
            return false;
        }
        
        return defaultValue;
    }
    
    /**
     * Converts provided String value to int.
     * @param value String to convert to int.
     * @param defaultValue int value to return if conversion fails.
     * @return Result of conversion as int value.
     */
    public static int getStringAsInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            logger.error("Could not convert " + value + " to int. Using default:" + defaultValue, nfe);
            return defaultValue;
        }
    }

    /**
     * Returns the base 64 encoded hash of the specified data.
     *
     * @param data              The data for which the digest is to be calculated
     * @param hashAlgorithm     The hashing algorithm to use
     * @return                  The base 64 encoded digest
     * @throws NoSuchAlgorithmException If the specified hash algorithm is not available
     */
    public static String hashEncode(String data, String hashAlgorithm)
            throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance(hashAlgorithm);

        /*
         * Get the byte representation of the password.  Attempt to use
         * a specific encoding to insulate the authentication from changes
         * to the platform's default encoding.
         */
        byte[] dataBytes;
        try {
            dataBytes = data.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            dataBytes = data.getBytes();
        }

        // Hash the password
        md.update(dataBytes);
        BASE64Encoder uuE = new BASE64Encoder();
        String encodedString = uuE.encodeBuffer(md.digest());
        // Trim off "\r\n" BASE64Encoder.encodeBuffer adds to the end of encoded string
        return encodedString.substring(0, encodedString.length() - 2); 
    }
    /**
     * Truncates(rolls back) specific calendar field to zero.
     * @param cal
     * @param calField
     */
    public static void truncateFieldToZero(Calendar cal, int calField) {
        if (cal.get(calField) != 0) {
            if(calField == Calendar.HOUR_OF_DAY) {
                //for hours, just set the field directly to 0
                cal.set(calField, 0);
            } else {
                cal.add(calField, -cal.get(calField));
            }
        }
    }
    
    /**
     * Eliminates the hours, minutes, seconds and milliseconds by setting them to 0.
     * @param d  Date to be transformed
     * @return   newly formatted Datetime
     */
    public static Date truncateToDays(Date d) {
        if (d == null) {
            return null;
        }

        Calendar c = (Calendar) calCache.get();
        c.setTime(d);
        
        truncateFieldToZero(c, Calendar.MILLISECOND);
        truncateFieldToZero(c, Calendar.SECOND);
        truncateFieldToZero(c, Calendar.MINUTE);
        truncateFieldToZero(c, Calendar.HOUR_OF_DAY);
        
        return c.getTime();
    }

    /**
     * Eliminates the minutes, seconds and milliseconds by setting them to 0.
     * @param d  Date to be transformed
     * @return   newly formatted Datetime
     */
    public static Date truncateToHours(Date d) {
        if (d == null) {
            return null;
        }
        
        Calendar c = (Calendar) calCache.get();
        c.setTime(d);
        
        truncateFieldToZero(c, Calendar.MILLISECOND);
        truncateFieldToZero(c, Calendar.SECOND);
        truncateFieldToZero(c, Calendar.MINUTE);
        
        return c.getTime();
    }

    /**
     * Eliminates the seconds and milliseconds by setting them to 0.
     * @param d  Date to be transformed
     * @return   newly formatted Datetime
     */
    public static Date truncateToMinutes(Date d) {
        if (d == null) {
            return null;
        }
        
        Calendar c = (Calendar) calCache.get();
        c.setTime(d);
        
        truncateFieldToZero(c, Calendar.MILLISECOND);
        truncateFieldToZero(c, Calendar.SECOND);
        
        return c.getTime();
    }


    /**
     * Eliminates the milliseconds by setting them to 0.
     * @param d  Date to be transformed
     * @return   newly formatted Datetime
     */
    public static Date truncateToSeconds(Date d) {
        if (d == null) {
            return null;
        }
        
        Calendar c = (Calendar) calCache.get();
        c.setTime(d);
        
        truncateFieldToZero(c, Calendar.MILLISECOND);
        
        return c.getTime();
    }

} //end Utilities