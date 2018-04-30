package com.wbiag.clocks.clients.sols.lib;
import org.apache.log4j.*;

import com.wbiag.clocks.clients.sols.lib.InitializationException;
import com.wbiag.clocks.clients.sols.lib.ReaderServerException;
import com.wbiag.clocks.clients.sols.lib.Utilities;
import com.wbiag.clocks.clients.sols.lib.WbSmartText;


import java.util.*;
import java.io.*;

/**
 * <p>Title: </p>
 * <p>Description: system properties are defined using SYSTEM_.... settings (for database for example) </p>
 * <p>Copyright: Workbrain (c) 2004</p>
 * <p>Company: Workbrain</p>
 * @author Octavian Tarcea & Andrei Kovacs
 * @version 1.0
 */
public class Settings {
    private static Logger logger = Logger.getLogger(Settings.class);

    /**
     * This is the singleton instance.
     */
    static private Settings instance = null;

    private HashMap hashmap = null;
    private HashMap message = null;
    private String settingsFilePath = "settings.conf";

    //=========================================  HASHMAP KEYS  ===================================

    /**
     * for setting options bollean option (strign representation of true).
     */
    public static final String SET = "Y";
    /**
     * for setting options bollean option (strign representation of false).
     */
    public static final String NOT_SET = "F";

    /**
     * for setting processor load.
     */
    public static final String FREE_PROCESSOR_SLEEP_TIME = "technician.freeprocessor";
    /**
     * timeout for starting database.
     */
    public static final String DATABASE_START_TIMEOUT = "technician.db.start.timeout";

    /**
     * server name.
     */
    public static final String CLOCK_SERVER_NAME = "server.name";
    /**
     * plant prefix (if used).
     */
    public static final String PLANT_PREFIX = "server.plant.prefix";
    /**
     * set length of badge (if used).
     */
    public static final String FIX_BADGE_LENGTH = "server.badge.length";
    /**
     * server badge delimiter.
     */
    public static final String BADGE_DELIMITER = "server.badge.delimiter";
    /**
     * use of local employee definition.
     */
    public static final String LOCAL_EMPLOYEE = "server.employee.local";
    /**
     * use of employee propagation accross readers.
     */
    public static final String EMPLOYEE_PROPAGATION = "server.employee.propagation";
    /**
     * class definition for local db access.
     */
    public static final String STORAGE_CLASS = "server.db.class";
    /**
     * Start grace in minutes - used for schedules.
     */
    private static final String START_GRACE = "server.schedule.startgrace";
    /**
     * End grace in minutes - used for schedules.
     */
    private static final String END_GRACE = "server.schedule.endgrace";
    /**
     * Maximal differenc in time.
     */
    private static final String CLOCK_TIME_DIFFERENCE = "server.clock.time.difference";
    /**
     * Minimal difference for on-line swipe.
     */
    private static final String ON_LINE_SWIPE_TIME = "server.online.swipe";

    /**
     * file location beginning.
     */
    public static final String FILE_LOCATION = "file.location";
    /**
     * file location for database files (beginning).
     */
    public static final String DB_FILE_LOCATION = "file.location.db";
    /**
     * database purge interval.
     */
    public static final String PURGE_JOURNAL_INTERVAL = "db.journal.purge.interval";
    /**
     * Archive purged journal entries.
     */
    public static final String ARCHIVE_PURGED_JOURNAL_ENTRIES = "db.journal.purge.archive";
    /**
     * database purge interval.
     */
    public static final String MESSAGE_TIMEOUT_INTERVAL = "db.message.timeout.interval";
    /**
     * database cache size.
     */
    public static final String DB_CACHE_SIZE = "db.cache.size";
    /**
     * database connection pool size.
     */
    public static final String DB_POOL_SIZE = "db.pool.size";
    /**
     * database connection pool size expand.
     */
    public static final String DB_POOL_EXPAND = "db.pool.expand";
    /**
     * database driver name.
     */
    public static final String DB_DRIVER = "db.driver";
    /**
     * database backup command.
     */
    public static final String DB_BACKUP = "db.backup.command";
    /**
     * database backup command.
     */
    public static final String DB_REORGANIZE = "db.reorganize.command";
    /**
     * database connection string.
     */
    public static final String DB_CONNECTION = "db.connection";
    /**
     * database user name.
     */
    public static final String DB_USER = "db.user";
    /**
     * database user password.
     */
    public static final String DB_PASSWORD = "db.password";
    /**
     * xml file location.
     */
    private static final String XML_INBOUND_FILES_LOCATION = "file.location.xml.inbound";
    /**
     * xml outbound file location.
     */
    private static final String XML_OUTBOUND_FILES_LOCATION = "file.location.xml.outbound";
    /**
     * xml bad file location.
     */
    private static final String XML_UNPARSABLE_FILES_LOCATION = "file.location.xml.bad";
    /**
     * xml archive file location.
     */
    private static final String XML_ARCHIVE_FILES_LOCATION = "file.location.xml.archive";
    /**
     * xml archive interval.
     */
    private static final String XML_ARCHIVE_DAYS_INTERVAL = "server.communication.xmlbackup.interval";
    /**
     * xml archive deleted files.
     */
    private static final String XML_ARCHIVE_DELETE_FILES = "server.communication.xmlbackup.deletefiles";
    /**
     * classes definition beginning.
     */
    public static final String CLASSES = "class";
    /**
     * thread class definition.
     */
    public static final String THREAD_CLASSES = "class.thread";
    /**
     * reader class definition.
     */
    public static final String READERS_CLASSES = "class.reader";
    /**
     * validation class definition.
     */
    public static final String VALIDATION_CLASSES = "class.validation";
    /**
     * pre-post transaction process class definition.
     */
    public static final String PRE_POST_PROCESS_CLASSES = "class.swipe.process";
    /**
     * thread class definition.
     */
    public static final String DATABASE_PROXY_CLASSES = "class.proxy";
    /**
     * thread settings beginning.
     */
    public static final String THREADS_PARAMS = "thread";
    /**
     * reader setting beginning.
     */
    public static final String READERS = "reader";
    /**
     * message settings beginning.
     */
    public static final String MESSAGE = "message";
    /**
     *
     */
    private static final String DAY_START_MORNING = "timeofday.morning.start";
    /**
     *
     */
    private static final String DAY_START_AFTERNOON = "timeofday.afternoon.start";
    /**
     *
     */
    private static final String DAY_START_EVENING = "timeofday.evening.start";
    /**
     *
     */
    private static final String EXPORT_STATUS_FILE_NAME = "server.communication.exportstatusfilename";
    /**
     * Used for the outbound Export Swipe message - will create the XML.
     */
    private static final String SWIPE_PARSER_CLASS = "class.communication.parser.swipe";
    /**
     * Used for the outbound Export Reader Status message - will create the XML.
     */
    private static final String READER_STATUS_PARSER_CLASS = "class.communication.parser.readerstatus";
    /**
     * Used for the outbound Export Table Dump message - will create the XML.
     */
    private static final String TABLE_DUMP_PARSER_CLASS = "class.communication.parser.tabledump";
    /**
     * Used for the outbound Store Template message - will create the XML.
     */
    private static final String TEMPLATE_PARSER_CLASS = "class.communication.parser.template";
    /**
     * Specify the workbrain communication protocol class - might be file, SOAP, JMS and so on.
     */
    private static final String COMMUNICATION_PROTOCOL_CLASS = "class.communication.protocol.file";
    /**
     * Specify the workbrain communication class - might be file, SOAP, JMS and so on.
     */
    public static final String INBOUND_CLASS = "class.communication.inbound";
    /**
     * Specify the workbrain communication class - might be file, SOAP, JMS and so on.
     */
    public static final String OUTBOUND_CLASS = "class.communication.outbound";
    /**
     * Used to find GUICommand classes.
     */
    private static final String EXPORT_REJECTED_SWIPES = "server.exportrejectedswipes";

    /**
     * What message to display if there is no validation done at the clock.
     */
    private static final String NO_VALIDATION_REQUIRED_MESSAGE = "message.no_validation_required_message";
    /**
     * Timeout for validation messages displayed at the clock (seconds).
     */
    private static final String VALIDATION_MESSAGE_TIMEOUT = "server.validation.messagetimeout";
    /**
     * Message for enrollment ok.
     */
    private static final String ENROLLMENT_OK_MESSAGE = "server.enrollment.ok_message";
    /**
     * Message for enrollment ok.
     */
    private static final String ENROLLMENT_BAD_MESSAGE = "server.enrollment.bad_message";
    /**
     * Message for enrollment timeout.
     */
    private static final String ENROLLMENT_TIMEOUT_MESSAGE = "server.enrollment.timeout_message";

    /**
     * prefix to define gui commands.
     */
    private static final String GUI_COMMANDS_PREFIX = "gui.command.";
    /**
     * class definition for notification e-mail formatter.
     */
    public static final String EMAIL_FORMATTER_CLASS = "mail.formatter.class";

    /**
     *  Server setting for the "longest on" value. The value of this setting is in minutes.
      */
    public static final String LONGEST_ON = "server.longest_on";

    /**
     *  Number of days for which to get schedule table dump.
      */
    public static final String SCHEDULE_DAYS = "wbsynch.schedules.days";
    
    /**
     * Encoding used to store data on application side (US-ASCII, ISO-8859-1 a.k.a ISO-LATIN-1, UTF-8, UTF-16BE, UTF-16LE, UTF-16)
     */
    public static final String APP_DATA_ENCODING = "wbsynch.appDataEncoding";

    //private constructor - we do not allow outside access
    private Settings() {
        this.hashmap = getMyHashMap();
    };

    //Need to overwrite the get methods so that it trims the values
    private static HashMap getMyHashMap() {
        return new HashMap() {
            public Object get(Object key) {
                if (null != super.get(key)) {
                    return ((String) super.get(key)).trim();
                } else
                    return null;

            }
        }; //end MyHashMap
    } //end getMyHashMap

    /**
     * Init settings from file.
     * @param settingsFileName String
     * @throws InitializationException
     */
    public static void init(String settingsFileName) throws InitializationException {

        //One we create a Setting singleton we use it all the time
        synchronized (Settings.class) {
            if(instance == null){
                Settings.instance = new Settings();
            }
        }

        if (null == settingsFileName) {
            logger.error("Null Settings file name received in init() ");
            throw new InitializationException("Null Settings file name received in init() ");
        } else {
            instance.initInstance(settingsFileName);
        }
    } //end init

    /**
     * Init settings from Map. If settings was initialized before
     * new values from map will be exchanged, if was not initialized
     * settings will be initialized empty and polupated with values from
     * given map.
     * @param map Map
     * @param clear - true/false, clear settings before add map.
     * @throws InitializationException
     */
    public static void init(Map map, boolean clear) throws InitializationException {

        //One we create a Setting singleton we use it all the time
        synchronized (Settings.class) {
            if(instance == null){
                Settings.instance = new Settings();
            }
        }

        if (null == map) {
            logger.error("Settings initialized as empty ");
        } else {
            instance.initInstance(map, clear);
        }
    } //end init

    /**
     * return an instance of the Settings Singleton.
     * @return Settings
     */
    public static Settings instance() {
        if (null == instance) {
            logger.fatal("Null Settings file returned. Call Settings.init() before Settings.instance() !!");
        }
        return instance;
    }

    /**
     * Checks if settings instance is null.
     * @return boolean value which marks if settings instance is null
     */
    public static boolean isNull(){
        return (instance==null);
    }

    private void validate() throws InitializationException {
        //check if all of them have values.
        for (Iterator i = hashmap.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (null == key) {
                logger.fatal("Null key value for Settings Hashmap!");
                throw new InitializationException();
            }

            String value = (String) hashmap.get(key);
            if (null == value) {
                logger.fatal("ERROR: Null value for setting key " + key);
                throw new InitializationException();
            }

        } //end for

    }

    /**
     * It will load the Settings from map.
     * @param map Map
     * @throws InitializationException
     */
    synchronized void initInstance(Map map, boolean clear) throws InitializationException {
        if(null == this.hashmap || clear){
            this.hashmap= getMyHashMap();
        }
        //Now we move the data from the Properties object to out hashsmap
        this.hashmap.putAll((Map) map);

        //Validate the results
        validate();
        //build message structure.
        loadProperties(map, WbSmartText.getDefaultLanguage());
        if(logger.isDebugEnabled()){
            logger.debug("Settings reloaded: " + hashmap);
        }
    }
    /**
     * It will load the Settings from the specified properties file.
     * @param settingsFileName a String
     * @throws InitializationException
     */
    public synchronized void initInstance(String settingsFileName) throws InitializationException {
        //Clean the hashmap (in case we reinitialize during runtime)
        this.hashmap = getMyHashMap();
        //read the Properties file
        //For reading we only use
        FileInputStream fis = null;
        try {
            this.settingsFilePath = settingsFileName;

            File file = new File(settingsFilePath);

            if (!file.exists()) {
                throw new InitializationException("Could not find Settings file: " + settingsFilePath);
            }
            fis = new FileInputStream(file);

            logger.debug("BEFORE Settings reloaded: " + hashmap);

            Properties prop = new Properties();
            prop.load(fis);
            //Now we move the data from the Properties object to out hashsmap
            this.hashmap.putAll((Map) prop);

            //Validate the results
            validate();
            //build message structure.
            loadProperties(prop, WbSmartText.getDefaultLanguage());
            try {
                String s = (String)prop.get("message.languages");
                if (s != null) {
                    StringTokenizer st = new StringTokenizer(s, ";,");
                    while (st.hasMoreTokens()) {
                        String lan = st.nextToken().trim();
                        //load from conf file
                        String start = file.getName();
                        String end = "";
                        int index = start.lastIndexOf(".");
                        if (index > 0) {
                            end = start.substring(index);
                            start = start.substring(0, index);
                        }
                        loadProperties(new File(file.getParentFile(), start + "_" + lan + end), lan);
                    }
                }
            } catch (Throwable ioe) {
                logger.error(ioe.getMessage(), ioe);
                throw new InitializationException("Cannot load language specific configuration file.\n" + ioe.getMessage());
            }

            logger.debug("Settings reloaded: " + hashmap);

        } catch (IOException ioex) {
            ioex.printStackTrace();
            throw new InitializationException("Exception trying to read setings file: " + settingsFilePath);
        } finally {
            try {
                if (null != fis) {
                    fis.close();
                }
            } catch (IOException ioex) {
                logger.error("IOExcpetion while closing setttings file input stream", ioex);
            }

        }
        //end trycatch
    } //end initInstance
    private void loadProperties(File file, String language) throws InitializationException {
        try {
            Properties properties = new Properties();
            FileInputStream propertiesStream = new FileInputStream(file);
            properties.load(propertiesStream);
            loadProperties(properties, language);
        } catch (Throwable ioe) {
            logger.error(ioe.getMessage(), ioe);
            throw new InitializationException("file:" + file.getAbsolutePath() + " \n" + ioe.getMessage());
        }
    }

    /**
     * load messages from the provided properties map for the specified language
     * (the code has been modified to load keys from the provided map instead of the default map)
     * @param p
     * @param language
     */
    private void loadProperties(Map p, String language) {
        if(message == null){
            message = new HashMap();
        }
        // get iterator for the provided map
        // Iterator iter = keySet();
        Iterator iter = p.keySet().iterator();

        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (key == null) {
                continue;
            }
            if(!key.startsWith(MESSAGE)){
                continue;
            }
            if(key.endsWith(".color")){
                continue;
            }
            if(key.endsWith(".beep")){
                continue;
            }
            WbSmartText l = (WbSmartText) message.get(key);
            if (l == null) {
                l = new WbSmartText();
                message.put(key, l);
            }
            String s = (String)p.get(key);
            if (s != null) {
                l.setText(language, s);
            }
            s = (String)p.get(key+".color");
            if(s != null){
                l.setColor(s);
            }
            s = (String)p.get(key+".beep");
            if(s != null){
                l.setNumberOfBeeps(s);
            }
        }
    }


    /**
     * Get all available settings key set.
     * @return Iterator
     */
    public Iterator keySet() {
        return hashmap.keySet().iterator();
    }

    /**
     * get key value.
     * @param name String
     * @return String
     * @see #set(String, String)
     */
    public String get(String name) {
        return (String) hashmap.get(name);
    }
    /**
     * set key value.
     * @param name String
     * @param value String
     * @see #get(String)
     */
    public void set(String name, String value) {
        if(hashmap == null){
            this.hashmap = getMyHashMap();
        }
        hashmap.put(name, value);
    }

    /**
     * Get all key namess starting with name.
     * @param name String
     * @return array of string
     */
    public String[] getAllKeyNamesStarting(String name) {
        TreeSet sort = new TreeSet();
        Iterator iter = keySet();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (key == null) {
                continue;
            }
            if (key.startsWith(name)) {
                sort.add(key);
            }
        }
        //now sort the elements and place in table;
        iter = sort.iterator();
        String[] ret = new String[sort.size()];
        int i = 0;
        while (iter.hasNext()) {
            String key = (String) iter.next();
            ret[i++] = key;
        }
        return ret;
    }

    /**
     * Get all key values starting with name.
     * @param name String
     * @return array of string
     */
    public String[] getAllKeyValuesStarting(String name) {
        TreeSet sort = new TreeSet();
        Iterator iter = keySet();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (key == null) {
                continue;
            }
            if (key.startsWith(name)) {
                sort.add(key);
            }
        }
        //now sort the elements and place in table;
        iter = sort.iterator();
        String[] ret = new String[sort.size()];
        int i = 0;
        while (iter.hasNext()) {
            String key = (String) iter.next();
            ret[i++] = get(key);
        }
        return ret;
    }

    /**
     * Get directory location for local table.
     * @param name String table name.
     * @return String
     */
    public String getDbLocation(String name) {
        String root = (String) hashmap.get(DB_FILE_LOCATION);
        if (name == null) {
            return Utilities.addFinalSlash(root);
        }
        String ret = (String) hashmap.get(DB_FILE_LOCATION + "." + name);
        if (ret != null) {
            return Utilities.addFinalSlash(ret);
        } else {
            return Utilities.addFinalSlash(root);
        }
    }


    /**
     * Set xml inbound file location.
     * @param location String
     * @throws InitializationException
     * @see #getXMLInboundFilesLocation()
     */
    public void setXMLInboundFilesLocation(String location) throws InitializationException {
        if (null == location) {
            throw new InitializationException("null value received in Settings.setXMLInboundFilesLocation");
        }
        hashmap.put(Settings.XML_INBOUND_FILES_LOCATION, location);
    }

    /**
     * set xml bad file location.
     * @param location String
     * @throws InitializationException
     * @see #getXMLUnparsableFilesLocation()
     */
    public void setXMLUnparsableFilesLocation(String location) throws InitializationException {
        if (null == location) {
            throw new InitializationException("null value received in Settings.setXMLUnparsableFilesLocation");
        }
        hashmap.put(Settings.XML_UNPARSABLE_FILES_LOCATION, location);
    }

    /**
     * set start grace in minutes.
     * @param startGrace an int
     * @see #getStartGrace()
     */
    public void setStartGrace(int startGrace) {
        hashmap.put(Settings.START_GRACE, "" + startGrace);
    }

    /**
     * Set End grace in minutes.
     * @param endGrace an int
     * @see #getEndGrace()
     */
    public void setEndGrace(int endGrace) {
        hashmap.put(Settings.END_GRACE, "" + endGrace);
    }

    /**
     * get xml inbound file location.
     * @return String
     * @see #setXMLInboundFilesLocation(String)
     */
    public String getXMLInboundFilesLocation() {
        return Utilities.addFinalSlash((String) hashmap.get(Settings.XML_INBOUND_FILES_LOCATION));
    }

    /**
     * get xml outbound file location.
     * @return String
     */
    public String getXMLOutboundFilesLocation() {
        return Utilities.addFinalSlash((String) hashmap.get(Settings.XML_OUTBOUND_FILES_LOCATION));
    }

    /**
     * Set xml bad files location.
     * @return String
     * @see #setXMLUnparsableFilesLocation(String)
     */
    public String getXMLUnparsableFilesLocation() {
        return Utilities.addFinalSlash((String) hashmap.get(Settings.XML_UNPARSABLE_FILES_LOCATION));
    }

    /**
     * get xml archive file location.
     * @return String
     */
    public String getXMLArchiveFilesLocation() {
        return Utilities.addFinalSlash((String) hashmap.get(Settings.XML_ARCHIVE_FILES_LOCATION));
    }

    /**
     * Get xml archive interval in days.
     * @return int
     */
    public int getXMLArchiveDaysInterval() {
        String str = (String) hashmap.get(Settings.XML_ARCHIVE_DAYS_INTERVAL);
        if (null != str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                logger.error("Could not parse Settings.XML_ARCHIVE_DAYS_INTERVAL into int", nfe);
                return 1;
            }

        } else {
            return 1;
        } //end if else
    }

    /**
     * Gets the value of the server.longest_on settings.
     * @return the value of the server.longest_on settings.
     */
    public int getLongestOn() {
        String str = (String) hashmap.get(Settings.LONGEST_ON);
        if (null != str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                logger.warn("Could not parse Settings.LONGEST_ON into int. Defaulting to 0 - which disables longest on.", nfe);
                return 0;
            }

        } else {
            logger.warn("Could not find Settings.LONGEST_ON. Defaulting to 0 - which disables longest on.");
            return 0;
        } //end if else
    }

    /**
     * Get xml archive delete files flag.
     * @return True if the value of server.communication.archive.deletefiles is "T", "Y" or "TRUE"
     * (ignoring case considerations).
     */
    public boolean getXMLArchiveDeleteFilesFlag() {
        return getBooleanSetting(Settings.XML_ARCHIVE_DELETE_FILES);
    }

    /**
     * Get morning start time.
     * @return String
     */
    public String getMorningStart() {
        return (String) hashmap.get(Settings.DAY_START_MORNING);
    }

    /**
     * Get afternoon start time.
     * @return String
     */
    public String getAfternoonStart() {
        return (String) hashmap.get(Settings.DAY_START_AFTERNOON);
    }

    /**
     * Get evening start time.
     * @return String
     */
    public String getEveningStart() {
        return (String) hashmap.get(Settings.DAY_START_EVENING);
    }

    /**
     * Used by wbsync in order to avoid the processor going to 100%.
     * Default value is 1 ms.
     * @return int
     */
    public int getFreeProcessorSleepTime() {
        String str = (String) hashmap.get(Settings.FREE_PROCESSOR_SLEEP_TIME);
        if ((null == str) || (str.equals(""))) {
            logger.warn(FREE_PROCESSOR_SLEEP_TIME + " setting missing! Using default 1 ms");
            //return default value of 1 ms
            return 1;
        } else {
            try {
                //Convert to Integer
                int i = Integer.parseInt(str);
                return i;
            } catch (Exception ex) {
                logger.warn("Could not convert settings " + FREE_PROCESSOR_SLEEP_TIME + " to int:" + str + " Using default 1 ms");
                return 1;
            }
        } //end if else
    } //end getFreeProcessorSleepTime

    /**
     * get purge journal interval.
     * @return int - value in days - how often to purge the journal table
     */
    public int getPurgeJournalInterval() {
        int purgingIntervalInDays = 0;
        try {
            purgingIntervalInDays = Integer.parseInt((String) hashmap.get(Settings.PURGE_JOURNAL_INTERVAL));
        } catch (NumberFormatException nfe){
            logger.warn("Settings warning - Couldn't parse the value for \"purgingIntervalInDays\""+
                                 " into the settings table. Assuming 30 days...", nfe) ;
            purgingIntervalInDays = 30;
        }
        return purgingIntervalInDays;
    }

    /**
     * get messages timeout interval - messages older than x days will be deleted
     * from the message table.
     * @return int - value in days - for how long can a message stay in the message
     *               table befor it "expires"
     */
    public int getMessagesTimeoutInterval() {
        String messageTimeoutInDays = (String) hashmap.get(Settings.MESSAGE_TIMEOUT_INTERVAL);
        try {
            return Integer.parseInt(messageTimeoutInDays);
        } catch (NumberFormatException nfe){
            logger.warn("Settings warning - Couldn't parse the value for settings db.message.timeout.interval: "+
                                messageTimeoutInDays+" .Assuming 10 days...", nfe) ;
            return 10;
        }
    }


    /**
     * Get xml outbound reader status file name.
     * @return String
     */
    public String getXMLOutboundStatusFileName() {
        return (String) hashmap.get(Settings.EXPORT_STATUS_FILE_NAME);
    }

    /**
     * Get swipe parser class.
     * @return String
     */
    public String getSwipeParserClass() {
        return (String) hashmap.get(Settings.SWIPE_PARSER_CLASS);
    }

    /**
     * Get reader status parser class.
     * @return String
     */
    public String getReaderStatusParserClass() {
        return (String) hashmap.get(Settings.READER_STATUS_PARSER_CLASS);
    }

    /**
     * get table dump parser class.
     * @return String
     */
    public String getTableDumpParserClass() {
        return (String) hashmap.get(Settings.TABLE_DUMP_PARSER_CLASS);
    }

    /**
     * get teamplate parser class.
     * @return String
     */
    public String getTemplateParserClass() {
        return (String) hashmap.get(Settings.TEMPLATE_PARSER_CLASS);
    }

    /**
     * Get communication protocol class.
     * @return String
     */
    public String getCommunicationProtocolClass() {
        return (String) hashmap.get(Settings.COMMUNICATION_PROTOCOL_CLASS);
    }

    /**
     * Get export rejected swipes flag.
     * @return boolean
     */
    public boolean getExportRejectedSwipes() {
        return getBooleanSetting(Settings.EXPORT_REJECTED_SWIPES);
    }

    /**
     * Helper method used for getting boolean settings.
     * @param key The name of the setting.
     * @return True if the setting exists and the value is one of the following:
     * <B>True</B>, <B>T </B>or <B>Y</B> (ignoring case consideration); False otherwise.
      */
    public boolean getBooleanSetting(String key){
        return Utilities.getBooleanFromString((String) hashmap.get(key));
    }
    
    /**
     * Helper method used for getting boolean settings.
     * @param key The name of the setting.
     * @param defaultValue The value to be returned if setting does not exist.
     * @return True if the setting exists and the value is one of the following:
     * <B>True</B>, <B>T </B>or <B>Y</B> (ignoring case consideration); defaultValue if seeting not found;
     * False otherwise.
      */
    public boolean getBooleanSetting(String key, boolean defaultValue){
        String s = (String) hashmap.get(key);
        if (s!=null){
            return Utilities.getBooleanFromString(s);
        } else {
            logger.debug("Setting : " + key + " - not found. Defaulting to : " + defaultValue);
            return defaultValue;
        }
    }
    /**
     * Get no validation required message.
     * @return String
     */
    public String getNoValidationRequiredMessage() {
        String str = (String) hashmap.get(Settings.NO_VALIDATION_REQUIRED_MESSAGE);
        if (null == str) {
            logger.warn("Settings: "+NO_VALIDATION_REQUIRED_MESSAGE+" not set");
            return "";
        } else {
            return str;
        }

    }

    /**
     * Get validation message timeout.
     * @return String
     */
    public String getValidationMessageTimeout() {
        String str = (String) hashmap.get(Settings.VALIDATION_MESSAGE_TIMEOUT);
        if (null == str || str.equals("")) {
           return "2";
        } else {
            return str;
        }

    }

    /**
     * Get enrollment OK message.
     * @return String
     */
    public String getEnrollmentOkMessage() {
        String str = (String) hashmap.get(Settings.ENROLLMENT_OK_MESSAGE);
        if (null == str) {
            logger.warn("Settings: "+ENROLLMENT_OK_MESSAGE+" not set");
            return "";
        } else {
            return str;
        }
    }

    /**
     * get enrollment bad message.
     * @return String
     */
    public String getEnrollmentBadMessage() {
        String str = (String) hashmap.get(Settings.ENROLLMENT_BAD_MESSAGE);
        if (null == str) {
            logger.warn("Settings: "+ENROLLMENT_BAD_MESSAGE+" not set");
            return "";
        } else {
            return str;
        }

    }

    /**
     * get enrollment timeout message.
     * @return String
     */
    public String getEnrollmentTimeoutMessage() {
        String str = (String) hashmap.get(Settings.ENROLLMENT_TIMEOUT_MESSAGE);
        if (null == str) {
            logger.warn("Settings: "+ENROLLMENT_TIMEOUT_MESSAGE+" not set");
            return "";
        } else {
            return str;
        }

    }

    /**
     * Start grace in minutes.
     * @return int
     * @see #setStartGrace(int)
     */
    public int getStartGrace() {
        String str = (String) hashmap.get(Settings.START_GRACE);
        int retVal = 15;
        if (null != str) {
            try {
                retVal = Integer.parseInt(str);
            } catch (NumberFormatException nfex) {
                logger.error("Could not parse stratGrace into int: " + str, nfex);
                logger.error("Grace Start Period not defined -" + " returning default start grace period - 15 minutes");
            }
        }
        return retVal;
    }

    /**
     * End grace in minutes.
     * @return int
     * @see #setEndGrace(int)
     */
    public int getEndGrace() {
        String str = (String) hashmap.get(Settings.END_GRACE);
        int retVal = 15;
        if (null != str) {
            try {
                retVal = Integer.parseInt(str);
            } catch (NumberFormatException nfex) {
                logger.error("Could not parse endGrace into int: " + str, nfex);
                logger.error("Grace End Period not defined -" + " returning default end grace period - 15 minutes");
            }

        }
        return retVal;
    }
    /**
     * Get number of seconds swipe is treated as on-line.
     * @return int
     */
    public int getOnLineSwipeTime() {
        String str = (String) hashmap.get(Settings.ON_LINE_SWIPE_TIME);

        if (null != str) {
            try {
                int retVal = Integer.parseInt(str);
                return retVal;
            } catch (NumberFormatException nfex) {
                logger.error("Could not parse on line swipe into int: " + str, nfex);
                logger.error("Online swipe time not defined -" + " returning default end grace period - 10 seconds");
                return 10;
            }

        } else {
            return 10;
        }
    }

    /**
     * Get maximum difference in seconds between server and clock.
     * @return int
     */
    public int getClockTimeDifference() {
        String str = (String) hashmap.get(Settings.CLOCK_TIME_DIFFERENCE);

        if (null != str) {
            try {
                int retVal = Integer.parseInt(str);
                return retVal;
            } catch (NumberFormatException nfex) {
                logger.error("Could not parse clock difference into int: " + str, nfex);
                logger.error("Clock difference time not defined -" + " returning default end grace period - 5 seconds");
                return 5;
            }

        } else {
            return 5;
        }
    }

    /**
     * Get fixed badge length (default is 6).
     * @return int
     * @see #setFixedBadgeLength(int)
     */
    public int getFixedBadgeLength() {
        String str = ((String) hashmap.get(Settings.FIX_BADGE_LENGTH));
        if (null != str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                logger.error("Could not parse Settings.FIXED_BADGE_LENGT into int - defaulting to 6", nfe);
                return 6;
            }
        } else {
            logger.error("Settings.FIXED_BADGE_LENGT not defined - defaulting to 6");
            return 6;
        }

    }

    /**
     * get fixed badge length.
     * @param length int
     * @see #getFixedBadgeLength()
     */
    public void setFixedBadgeLength(int length) {
        hashmap.put(Settings.FIX_BADGE_LENGTH, "" + length);
    }

    /**
     * Get gui commands supported (defined) for reader type.
     * @param readerType a String
     * @return array of string
     */
    public String[] getGUICommands(String readerType) {
        ArrayList ret = new ArrayList();

        String select = null;

        if (readerType != null && readerType.trim().length() > 0) {
            select = GUI_COMMANDS_PREFIX + readerType + ".";
        } else {
            select = GUI_COMMANDS_PREFIX;
        }

        String commandName = null;
        Iterator iter = Settings.instance().keySet();
        while (iter.hasNext()) {
            String key = (String) iter.next();
            if (key == null) {
                continue;
            }
            if (key.startsWith(select)) {
                commandName = (String) Settings.instance().get(key);
                if (commandName == null) {
                    continue;
                } else {
                    ret.add(commandName);
                }
            }
        } //end while

        String[] strRet = new String[ret.size()];
        int i = 0;
        for (Iterator it = ret.iterator(); it.hasNext(); i++) {
            strRet[i] = (String) it.next();
        }

        //return as a String Array
        return strRet;

    }

    /**
     * IMPORTANT - this method will actually return a copy of the Hashmap!!!.
     * To update a Setting you will need to go through the setters
     * (maybe add a general setter in the future?.
     * @return HashMap
     */
    public HashMap getSettingsHashMap() {
        HashMap returned = new HashMap(hashmap);
        return returned;
    }

    /**
     * Get message for validation class.
     * @param validationClassName Class validation class.
     * @param value String value
     * @return String
     */
    public String getValidationValue(Class validationClassName, String value) {
        StringTokenizer st = new StringTokenizer(validationClassName.getName(), ".");
        String shortClassName = null;
        while (st.hasMoreTokens()) {
            shortClassName = (String) st.nextElement();
        }
        return get("message.validation." + shortClassName + "." + value);
    }

    /**
     * Get database proxy class for the specific Thread.
     * @param type String representing the type
     * @return String
     */
    public String getDatabaseProxyClass(String type) {
        return get(DATABASE_PROXY_CLASSES + "." + type);
    }



    /**
     * helper method to determine if the validation message has been found
     * @param obj the object which is expected to contain the validation message
     * @return true if found or false if not found
     */
    private boolean isMessageFound(Object obj){
        if(obj==null){
            return false;
        }
        else if(obj instanceof WbSmartText){
            if(((WbSmartText)obj).getContent()!=null){
                if(logger.isDebugEnabled()){
                    logger.debug("found message:"+obj);
                }
                return true;
            }
            else{
                return false;
            }
        }
        else if(obj instanceof String){
            if(logger.isDebugEnabled()){
                logger.debug("found message:"+obj);
            }
            return true;
        }
        return false;
    }

    /**
     * Get message for validation class.
     * @param name String value
     * @return WbSmartText
     */
    public WbSmartText getValidationMessage(String name) {
        WbSmartText text = null;
        if(message != null){
            text = (WbSmartText)message.get(name);
        }
        if(text == null){
            text = new WbSmartText(get(name));
        }
        String textMessage = text.getText();
        logger.debug("textMessage: " + textMessage);
        if (textMessage != null && textMessage.startsWith("\"") && textMessage.endsWith("\"")) {
            text.setText(textMessage.substring(1, textMessage.length() -1));
        }
        return text;
    }

    /**
     * Returns the key name containing the specified value.
     * @param value value to be found
     * @return key containing the value, <b>null</b> if key is not found
     */
    public String getKeyByValue(String value) {
        //check if all of them have values.
        for (Iterator i = hashmap.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            if (null == key) {
                return null;
            }

            if (value.equals((String) hashmap.get(key))) {
                return key;
            }

        } //end for
        return null;
    }

    /**
     * Get number of days for schedule table dumps.
     * @return int
     */
    public int getScheduleDays() {
        String str = (String) hashmap.get(Settings.SCHEDULE_DAYS);

        if (null != str) {
            try {
                int retVal = Integer.parseInt(str);
                return retVal;
            } catch (NumberFormatException nfex) {
                logger.error("Could not parse wbsynch.schedule.days into int. Defaulting to 2.");
                return 2;
            }

        } else {
            return 2;
        }
    }
    
    public String getAppDataEncoding() {
        
        String str = (String) hashmap.get(Settings.APP_DATA_ENCODING);
        if (str != null) {
            return str;
        } else {
            return "UTF-8";
        }
    }

    /**
    * Read the Settings file and return it as a String.
    * @return String    Setting file as String
    * @throws ReaderServerException - can't load Settings file in a String
    */
    public synchronized String getSettingsFileAsString() throws ReaderServerException {
        BufferedReader reader = null;
        try {
            StringBuffer strReturn = new StringBuffer();
            //We have the path top the setting file in this.settingsFilePath
            File file = new File(this.settingsFilePath);

            if (!file.exists()) {
                throw new InitializationException("Could not find Settings file: " + settingsFilePath);
            }
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line = reader.readLine();
            while (null != line) {
                strReturn.append(line);
                strReturn.append("\n");
                line = reader.readLine();
            } //end while

            return strReturn.toString();
        } catch (InitializationException iex) {
            logger.error("Exception while reading settings file as String: " + settingsFilePath, iex);
            throw new ReaderServerException("InitializationExeption while loading Settings file as String " + settingsFilePath);
        } catch (FileNotFoundException fnex) {
            logger.error("FileNotFoundException while reading settings file as String: " + settingsFilePath, fnex);
            throw new ReaderServerException("InitializationExeption while loading Settings file as String " + settingsFilePath);
        } catch (IOException ioex) {
            logger.error("IOException while reading settings file as String: " + settingsFilePath, ioex);
            throw new ReaderServerException("IOException while loading Settings file as String " + settingsFilePath);
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException ioex) {
                    logger.error("Error closing file: " + settingsFilePath, ioex);
                    throw new ReaderServerException("IOException while closing Settings file " + settingsFilePath);

                }

            }
        } //end try catch

    }

    /**
    * Replace the Settings file with the new String.
    * @param data String   New settings file as a String
    * @throws ReaderServerException - can't update the Settings file
    */
    public synchronized void replaceSettingsFileData(String data) throws ReaderServerException {
        Utilities.replaceFileData(this.settingsFilePath, data);

    }

    /**
     * Returns a string representation of the object.
     * @return a string representation of the object.
     * @see java.lang.Object#toString()
     */
    public String toString() {

        StringBuffer returnString = new StringBuffer();
        for (Iterator i = hashmap.keySet().iterator(); i.hasNext();) {
            String key = (String) i.next();
            String value = (String) hashmap.get(key);
            returnString.append(key).append(" = ").append(value).append("\n");
        }
        return returnString.toString();
    }
    
    public int getIntValue(String key, int defValue) {
        String str = ((String) hashmap.get(key));
        if (null != str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException nfe) {
                logger.warn("Could not parse " + key + " into int - defaulting to " + defValue);
                return defValue;
            }
        } else {
            logger.error("Settings.FIXED_BADGE_LENGT not defined - defaulting to " + defValue);
            return defValue;
        }

    }

}