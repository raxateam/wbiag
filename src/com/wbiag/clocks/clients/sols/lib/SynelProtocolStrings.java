package com.wbiag.clocks.clients.sols.lib;

/**
 * Helper class that contains all the string used in the Synel protocol.
 *
 * @author Octavian Tarcea
 */
public class SynelProtocolStrings {

    /**
     * The ack byte gets sent by the clock when acking a transaction.
     */
    public static final byte ACK = 0x06;

    /**
     * The "b" transaction gets sent by the clock when it's busy processing a previous request.
     */
    public static final char BUSY_TERMINAL = 'b';

    /**
     * The "t" transaction gets sent by the clock when it acknowledges an "I" command.
     */
    public static final char ACK_I = 't';

    /**
     * The delayed ack is sent to the clock to acknowledge that all delayed transaction have been received.
     * <BR>
     * This will force the reader to move his pointer.
     */
    public static final String DELAYED_ACK = "C0";

    /**
     * Sent to the reader after an F0 to ask for more data.
     * <BR>
     * This technique is used to "drain" the reader of offline transactions.
     */
    public static final String DELAYED_ASK_FOR_MORE_DATA = "B0";

    /**
     * Sent to the reader after a B0 to ask for more data.
     * <BR>
     * This technique is used to "drain" the reader of offline transactions.
     */
    public static final String DELAYED_ACK_INTERMEDIATE = "F0";

    /**
     * The HALT command is sent to reader to put it in "PROGRAMMING" mode.
      */
    public static final String HALT = "K0";

    /**
     * The RUN command is sent to reader to put it back in "Running" mode.
      */
    public static final String RUN = "L0";

    /**
     * "SOHN". The preamble to all DHCP related transactions. When sent by itself by the host, the 
     * reader replies with the S0 message which contains the MAC address of the terminal.
      */
    public static final String DHCP_PREAMBLE = "S0HN";
    
    /**
     * "V". The acknowledge message for a DHCP status message.
     */
    public static final String DHCP_ACK = "V";

    /**
     * This is sent by the reader in reply to a B0 request when we get to the end of the buffer.
     */
    public static final String DELAYED_NO_MORE_DATA = "n0";

    /**
     * This is the heartbeat that the reader sends to the clock server.
     * <BR>
     * "00" Logical ON/OFF    Format is 00YYYYMMDDhhmmss
     * <BR>
     * Tran to database:  none
     */
    public static final String HEARTBEAT_TYPE = "00";

    /**
     * Clock server will figure out if it is an ON or an OFF transaction.
     * <BR>
     * "10" Logical ON/OFF    Format is 10YYYYMMDDhhmmssbbbbbb
     * <BR>
     * Tran to database:  CLK_TYPE=1 for on,  2 for off.
     */
    public static final String ON_OFF_SWIPE_TYPE = "10";

    /**
     * Clock server will figure out if it is an ON or an OFF transaction.
     * <BR>
     * "10" Logical ON/OFF    With PIN Format is A0YYYYMMDDhhmmssbbbbbbpppppppppppppppp
     * <BR>
     * pppppppppppppppp is 16 characters left justified (padded with blanks to the right)
     * <BR>
     * Tran to database:  CLK_TYPE=1
     */
    public static final String ON_OFF_WITH_PIN_SWIPE_TYPE = "A0";

    /**
     * Clock server will figure out if it is an ON or an OFF transaction.
     * <BR>
     * "11" Logical Supervisor ON/OFF    Format is 11YYYYMMDDhhmmssbbbbbbssssss
     * <BR>
     * Tran to database:  CLK_TYPE=1 for on,  2 for off.
     */
    public static final String SUPERVISOR_ON_OFF_SWIPE_TYPE = "11";

    /**
     * Clock server will figure out if it is an ON or an OFF transaction.
     * <BR>
     * "11" Logical ON/OFF    With PIN Format is A1YYYYMMDDhhmmssbbbbbbpppppppppppppppp
     * <BR>
     * pppppppppppppppp is 16 characters left justified (padded with blanks to the right)
     * <BR>
     * Tran to database:  CLK_TYPE=1
     */
    public static final String SUPERVISOR_ON_OFF_WITH_PIN_SWIPE_TYPE = "A1";

    /**
     * Explicit ON transaction.
     * <BR>
     * "12"  ON or OFF     Format is 12YYYYMMDDhhmmssbbbbbbD
     * <BR>
     *    D=1 for on,  D=2 for off
     * <BR>
     * Tran to database:  CLK_TYPE=1
     */
    public static final String ON_SWIPE_TYPE = "12";

    /**
     * Explicit ON transaction that has a pin.
     * <BR>
     * "A2"  ON or OFF     With a PIN Format is A2YYYYMMDDhhmmssbbbbbbDpppppppppppppppp
     * <BR>
     *    D=1 for on,  D=2 for off
     * <BR>
     * pppppppppppppppp is 16 characters left justified (padded with blanks to the right)
     * <BR>
     * Tran to database:  CLK_TYPE=1
     */
    public static final String ON_WITH_PIN_SWIPE_TYPE = "A2";

    /**
     * Explicit ON transaction generated by a supervisor.
     * <BR>
     * "13"  Supervisor ON or OFF     Format is 13YYYYMMDDhhmmssbbbbbbssssssD
     * <BR>
     *    D=1 for on,  D=2 for off
     * <BR>
     * Tran to database:  CLK_TYPE=1
     */
    public static final String SUPERVISOR_ON_SWIPE_TYPE = "13";

    /**
     * Explicit ON transaction, generated by a supervisor, that has a pin.
     * <BR>
     * "A3"  Supervisor ON or OFF with a PIN    Format is A3YYYYMMDDhhmmssbbbbbbssssssDpppppppppppppppp
     * <BR>
     *    D=1 for on,  D=2 for off
     * <BR>
     * pppppppppppppppp is 16 characters left justified (padded with blanks to the right)
     * <BR>
     * Tran to database:  CLK_TYPE=1
     */
    public static final String SUPERVISOR_ON_WITH_PIN_SWIPE_TYPE = "A3";

    /**
     * Explictit OFF transaction.
     * <BR>
     * "12"  ON or OFF     Format is 12YYYYMMDDhhmmssbbbbbbD
     * <BR>
     *    D=1 for on,  D=2 for off
     * <BR>
     * Tran to database:  CLK_TYPE=2
     */
    public static final String OFF_SWIPE_TYPE = "12";

    /**
     * Explicit OFF transaction that has a pin.
     * <BR>
     * "A2"  ON or OFF     With a PIN Format is A2YYYYMMDDhhmmssbbbbbbDpppppppppppppppp
     * <BR>
     *    D=1 for on,  D=2 for off
     * <BR>
     * pppppppppppppppp is 16 characters left justified (padded with blanks to the right)
     * <BR>
     * Tran to database:  CLK_TYPE=1
     */
    public static final String OFF_WITH_PIN_SWIPE_TYPE = "A2";

    /**
     * Explicit OFF transaction generated by a supervisor.
     * <BR>
     * "13"  Supervisor ON or OFF     Format is 13YYYYMMDDhhmmssbbbbbbssssssD
     * <BR>
     *    D=1 for on,  D=2 for off
     * <BR>
     * Tran to database:  CLK_TYPE=2
     */
    public static final String SUPERVISOR_OFF_SWIPE_TYPE = "13";

    /**
     * Explicit OFF transaction, generated by a supervsior, that has a pin.
     * <BR>
     * "A3"  Supervisor ON or OFF with a PIN    Format is A3YYYYMMDDhhmmssbbbbbbssssssDpppppppppppppppp
     * <BR>
     *    D=1 for on,  D=2 for off
     * <BR>
     * pppppppppppppppp is 16 characters left justified (padded with blanks to the right)
     * <BR>
     * Tran to database:  CLK_TYPE=1
     */
    public static final String SUPERVISOR_OFF_WITH_PIN_SWIPE_TYPE = "A3";

    /**
     * Directionless transaction. 
     * <BR>
     * Clock Processing will figure out what to do with it.
     * <BR>
     * "14"  Unknown ON or OFF Format:  14YYYYMMDDhhmmssbbbbbb
     * <BR>
     * Tran to database:  CLK_TYPE=11
     */
    public static final String UNKNOWN_SWIPE_TYPE = "14";

    /**
     * Directionless transaction with pin. 
     * <BR>
     * Clock Processing will figure out what to do with it.
     * <BR>
     * "A4"  Supervisor Unknown ON or OFF Format:  A4YYYYMMDDhhmmssbbbbbbpppppppppppppppp
     * <BR>
     * pppppppppppppppp is 16 characters left justified (padded with blanks to the right)
     * <BR>
     * Tran to database:  CLK_TYPE=11
     */
    public static final String UNKNOWN_WITH_PIN_SWIPE_TYPE = "A4";

    /**
     * Directionless transaction generated by a supervisor. 
     * <BR>
     * Clock Processing will figure out what to do with it.
     * <BR>
     * "15"  Supervisor Unknown ON or OFF Format:  15YYYYMMDDhhmmssbbbbbbssssss
     * <BR>
     * Tran to database:  CLK_TYPE=11
     */
    public static final String SUPERVISOR_UNKNOWN_SWIPE_TYPE = "15";

    /**
     * Directionless transaction, generated by a supervisor, that has a pin. 
     * <BR>
     * Clock Processing will figure out what to do with it.
     * <BR>
     * "A5"  Supervisor Unknown ON or OFF Format:  A5YYYYMMDDhhmmssbbbbbbpppppppppppppppp
     * <BR>
     * pppppppppppppppp is 16 characters left justified (padded with blanks to the right)
     * <BR>
     * Tran to database:  CLK_TYPE=11
     */
    public static final String SUPERVISOR_UNKNOWN_WITH_PIN_SWIPE_TYPE = "A5";

    /**
     * This is setting a specific UDF field to a certain value.
     * <BR>
     * "16"  UDF Swipe:  16YYYYMMDDhhmmssbbbbbbUDFx=vvvvvvvvvvv
     * <BR>
     * vvvvvvvvvvv is the value of the udf padded to the right with spaces until
     * <BR>
     * it fills 16 chars (UDFx= are also part of the 16 chars)
     * <BR>
     * Tran to database:  None
     */
    public static final String UDF_SWIPE_TYPE = "16";

    /**
     * This is setting a specific UDF field to a certain value.
     * <BR>
     * "16"  UDF Swipe:  17YYYYMMDDhhmmssbbbbbbssssssUDFx=vvvvvvvvvvv
     * <BR>
     * vvvvvvvvvvv is the value of the udf padded to the right with spaces until
     * <BR>
     * it fills 16 chars (UDFx= are also part of the 16 chars)
     * <BR>
     * Tran to database:  None
     */
    public static final String SUPERVISOR_UDF_SWIPE_TYPE = "17";

    /**
     * Access transaction. 
     * <BR>
     * "18"    Access Transaction.    Format: 18YYYYMMDDhhmmssbbbbbb
     * <BR>
     * Tran to database:  CLK_TYPE=18
     */
    public static final String ACCESS_SWIPE_TYPE = "18";

    /**
     * Access transaction generated by a supervisor.
     * <BR>
     * "19"    Access Transaction.    Format: 19YYYYMMDDhhmmssbbbbbbssssss
     * <BR>
     * Tran to database:  CLK_TYPE=18
     */
    public static final String SUPERVISOR_ACCESS_SWIPE_TYPE = "19";

    /**
     * Job transaction.
     * <BR>
     * "20"  Job transfer Format: 20YYYYMMDDhhmmssbbbbbbaaaaaaaaaaaaaaaaaa = job code of 16 characters
     * <BR>
     * Tran to database: CLK_TYPE=3,  JOB=xxxx
     */
    public static final String JOB_SWIPE_TYPE = "20";

    /**
     * Job transaction generated by a supervisor.
     * <BR>
     * "21"  Supervisor Job transfer Format: 21YYYYMMDDhhmmssbbbbbbssssssaaaaaaaaaaaaaaaaaa = job code of 16 characters
     * <BR>
     * Tran to database: CLK_TYPE=3,  JOB=xxxx
     */
    public static final String SUPERVISOR_JOB_SWIPE_TYPE = "21";

    /**
     * Docket transaction.
     * <BR>
     *  "40"    Start docket.    Format: 40YYYYMMDDhhmmssbbbbbbaaaaaaaaaaaaaaaaaa = docket  code of 16 characters
     * <BR>
              *  Tran to database: CLK_TYPE=4, DKT=xxxxx
     */
    public static final String DOCKET_SWIPE_TYPE = "40";

    /**
     * Docket transaction generated by a supervisor.
     * <BR>
     *  "41"    Supervisor Start docket.    Format: 41YYYYMMDDhhmmssbbbbbbssssssaaaaaaaaaaaaaaaaaa = docket  code of 16 characters
     * <BR>
              *  Tran to database: CLK_TYPE=4, DKT=xxxxx
     */
    public static final String SUPERVISOR_DOCKET_SWIPE_TYPE = "41";

    /**
     * Department transaction.
     * <BR>
     * "50"    Department transfer Format: 50YYYYMMDDhhmmssbbbbbbaaaaaaaaaaaaa
     * <BR>
              *  Tran to database: CLK_TYPE=7, DEPT=xxxxx
     */
    public static final String DEPARTMENT_SWIPE_TYPE = "50";

    /**
     * Department transaction generated by a supervisor.
     * <BR>
     * "51"    Supervisor Department transfer Format: 51YYYYMMDDhhmmssbbbbbbssssssaaaaaaaaaaaaa
     * <BR>
              *  Tran to database: CLK_TYPE=7, DEPT=xxxxx
     */
    public static final String SUPERVISOR_DEPARTMENT_SWIPE_TYPE = "51";

    /**
     * Docket with time transaction (timesheet).
     * <BR>
     * "60"    Docket with elapsed time Format: 60YYYYMMDDhhmmssbbbbbbaaaaaaaaaaaaaaaaaaHHMM = docket code of 16 characters
     * <BR>
           * Tran to database: CLK_TYPE=9, DKT=xxxxx, TIME=mmmm(time must be converted to minutes)
     */
    public static final String DOCKET_WITH_TIME_SWIPE_TYPE = "60";

    /**
     * Docket with time transaction (timesheet) generated by a supervisor.
     * <BR>
     * "61"    Supervisor Docket with elapsed time Format: 61YYYYMMDDhhmmssbbbbbbssssssaaaaaaaaaaaaaaaaaaHHMM = docket code of 16 characters
     * <BR>
           * Tran to database: CLK_TYPE=9, DKT=xxxxx, TIME=mmmm(time must be converted to minutes)
     */
    public static final String SUPERVISOR_DOCKET_WITH_TIME_SWIPE_TYPE = "61";

    /**
     * Project transaction.
     * <BR>
     * "70"    Project transfer Format: 70YYYYMMDDhhmmssbbbbbbaaaaaaaaaaaaaaaaaa = project  code of 16 characters
     * <BR>
           *    Tran to database: CLK_TYPE=8, PROJ=xxxxx
     */
    public static final String PROJECT_SWIPE_TYPE = "70";

    /**
     * Project transaction generated by a supervisor.
     * <BR>
     * "71"    Supervisor Project transfer Format: 71YYYYMMDDhhmmssbbbbbbssssssaaaaaaaaaaaaaaaaaa = project  code of 16 characters
     * <BR>
           *    Tran to database: CLK_TYPE=8, PROJ=xxxxx
     */
    public static final String SUPERVISOR_PROJECT_SWIPE_TYPE = "71";

    /**
     * Timecode transaction.
     * <BR>
     * "80"     Time Code change Format: 80YYYYMMDDhhmmssbbbbbbaaaaaaaaaaaaaaaaaa = time code of 16 characters
     * <BR>
           *    Tran to database: CLK_TYPE=6, TCODE=xxxxx
     */
    public static final String TIMECODE_SWIPE_TYPE = "80";

    /**
     * Timecode transaction generated by a supervisor.
     * <BR>
     * "81"     Supervisor Time Code change Format: 81YYYYMMDDhhmmssbbbbbbssssssaaaaaaaaaaaaaaaaaa = time code of 16 characters
     * <BR>
           *    Tran to database: CLK_TYPE=6, TCODE=xxxxx
     */
    public static final String SUPERVISOR_TIMECODE_SWIPE_TYPE = "81";

    /**
     * Transaction generated by an employee to query special fields.
      */
    public static final String EMPLOYEE_QUERY_TYPE = "90";

    /**
     * Delayed N Swipe (On).
     */
    public static final String DELAYED_N_SWIPE_TYPE = "N";
 
    /**
     * Delayed F Swipe (Off). 
     */
    public static final String DELAYED_F_SWIPE_TYPE = "F";

    /**
     * Delayed X Swipe - All Swipes.
     */
    public static final String DELAYED_X_SWIPE_TYPE = "X";

    /**
     * Delayed P Swipe - All Swipes with Pin.
     */
    public static final String DELAYED_P_SWIPE_TYPE = "P";

    /**
     *  Used by the synel reader at startup. It asks for the biometric information (slave mode).
     */
    public static final String BIO_SLAVE_REQUEST = "V0";

    /**
     * Sent by the reader to know if it needs to ask for bio info or not.
     */
    public static final String BIO_CHECK = "V1";

    /**
     * Sent by the reader when it needs to inform the host that it has a new template.
     */
    public static final String BI0_NEW_TEMPLATE = "V2";

    /**
     * Not sure when this is used. 
     */
    public static final String BIO_NO_TEMPLATE_VERIFICATION = "V3";
                  
    /**
     * If employee not found in reader, the reader will send a template request.
     */
    public static final String BIO_TEMPLATE_REQUEST = "V5";

    /**
     *  Used by the synel reader at startup. It asks for the biometric information (master mode).
     */
    public static final String BIO_MASTER_REQUEST = "V9";

    /**
     *  Used to find out the version of the database the DT600 uses.
     */
    public static final String DB_VERSION = "01";

    /**
     * Prefix for all online transactions. The "0" is in fact the multidrop number but we only use "0".
     */
    public static final String QUERY_STRING = "q0";

    /**
     *  Prefixed used by the bio script device in the synel reader when sending a template.
     */
    public static final String BIO_THIS_IS_A_TEMPLATE = "v0";

    /**
     * String used by the emulator to identify itself. Each reader places its eprom number in these five chars.
     */
    public static final String EPROM_VERSION = "VIRT*";

    /**
     * Prefix for delayed transactions. The "0" is in fact the multidrop number but we only use "0".
     */
    public static final String DELAYED_STRING = "d03";

    /**
     * Denotes the length of the string that holds the eprom version. Helper for the swipe parsers. 
     */
    public static final int EPROM_VERSION_LENGTH = 5;

    /**
     * Denotes the length of the string that holds the labour metric part of a swipe. Helper for the swipe parsers. 
     */
    public static final int LABOUR_METRIC_LENGTH = 16;

    /**
     * Denotes the length of the string that holds the labour metric extension of a swipe (for timesheet). Helper for the swipe parsers. 
     */
    public static final int LABOUR_METRIC_EXTENSION_LENGTH = 4; //HHMM

    /**
     * Denotes the length of the string that holds the datetime of a swipe. Helper for the swipe parsers. 
     */
    public static final int DATE_AND_TIME_LENGTH = 14; //"YYYYMMDDHHMMSS"

    /**
     * Denotes the length of the string that holds the date of a swipe. Helper for the swipe parsers. 
     */
    public static final int DATE_LENGTH = 8; //"YYYYMMDD"

    /**
     * Denotes the length of the string that holds the time of a swipe. Helper for the swipe parsers. 
     */
    public static final int TIME_LENGTH = 6; //"HHMMSS"

    /**
     * Each message sent to the reader is padded with blanks to the right until reaching this length.
     */
    public static final int SERVER_MESSAGE_DEFAULT_LENGTH = 16;

    /**
     * The preable appended to every message sent to the reader when NOT using long swipes.
     */
    public static final String SERVER_PREAMBLE = "Q0Y";

    /**
     * The preable appended to every message sent to the reader when using long swipes.
     */
    public static final String SERVER_PREAMBLE_LONG = "Q0LY33";

    /**
     * The preable appended to every message sent to the reader when using long swipes.
     */
    public static final String SERVER_PREAMBLE_ENROLLMENT = "V0";

    /**
     * Success string that is prefixing the response sent to the reader.
     */
    public static final String READER_RESPONSE_SUCCESS = "0";

    /**
     * Error string that is prefixing the response sent to the reader.
     */
    public static final String READER_RESPONSE_ERROR = "1";

    /**
     * String that is prefixing the response sent to the reader when the supervisor is needed.
     */
    public static final String READER_RESPONSE_SEE_SUPERVISOR = "2";

    /**
     * Bio string that is prefixing the response sent to the reader.
     */
    public static final String READER_RESPONSE_BIO_REQUEST = "3";
}