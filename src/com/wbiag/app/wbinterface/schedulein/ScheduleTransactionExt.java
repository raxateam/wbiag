package com.wbiag.app.wbinterface.schedulein;

import java.util.*;
import java.sql.SQLException;
import com.workbrain.util.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.schedulein.ScheduleTransaction;
import com.workbrain.app.wbinterface.schedulein.ScheduleTransactionMapper;
import com.workbrain.app.wbinterface.schedulein.ScheduleProcessor;
import com.workbrain.app.ta.model.*;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.wbinterface.db.*;
/**
 * Transaction type for Schedule data processing.
 *
 * <p>Copyright: Copyright (c) 2002 Workbrain Inc.</p>
 *
 **/
public class ScheduleTransactionExt extends ScheduleTransaction  {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ScheduleTransactionExt.class);


    public final static int SCHEDULE_DETAIL = 23;
    public static final char SKDDET_SEPARATOR_CHAR = '~';
    private static List skdDetFields = new ArrayList();
    static {
        skdDetFields.add("WRKD_RATE");
        skdDetFields.add("WRKD_DEPT_NAME");
        skdDetFields.add("WRKD_JOB_NAME");
        skdDetFields.add("WRKD_TCODE_NAME");
        skdDetFields.add("WRKD_HTYPE_NAME");
        skdDetFields.add("WRKD_PROJ_NAME");        
        skdDetFields.add("WRKD_DOCK_NAME");        
        skdDetFields.add("WRKD_WBT_NAME");
        skdDetFields.add("ACT_NAME"); 
        skdDetFields.add("WRKD_COMMENTS");
        skdDetFields.add("SHFT_BRK");
        skdDetFields.add("JT_START_TIME");        
        skdDetFields.add("JT_END_TIME");        
        skdDetFields.add("WRKD_TYPE");        
        for (int i=1; i<=10; i++) {
            skdDetFields.add("WRKD_FLAG" + 1);
        }
        for (int i=1; i<=10; i++) {
            skdDetFields.add("WRKD_UDF" + 1);
        }
        
    }
    
    public ScheduleTransactionExt() {
    }

    /**
    * Processes a given row from WBINT_IMPORT.
    **/
    public void process(ImportData data, DBConnection conn)
                throws WBInterfaceException, SQLException{

        log( "ScheduleTransactionExt.process" );
        this.conn = conn;
        this.data = data;
        try {
            preProcess(data , conn);
            String skdDetColumn = data.getField(SCHEDULE_DETAIL);
            ScheduleTransactionMapper stMapper = new ScheduleTransactionMapper();
            setValidateFieldValues(stMapper);            
            if (StringHelper.isEmpty(skdDetColumn)) {
                getScheduleProcessor().process(stMapper);
            }
            else {
                stMapper.setOvrtypId(OverrideData.SCHEDULE_DETAIL_TYPE);
                String ovrNewVal = validateSkdDetail(skdDetColumn, stMapper);
                addOverride(OverrideData.SCHEDULE_DETAIL_TYPE, ovrNewVal,stMapper );
            }
            postProcess(data , stMapper , conn);
        } catch( Throwable t ) {
            status = ImportData.STATUS_ERROR;
            message = t.getMessage();
            WBInterfaceException.throwWBInterfaceException(t);
        }
    }    
    /**
     * Create ovrNewValue for Schedule Detail. Get skdStartTime EndTimes from core columns
     *     i.e format of string
     *     "WRKD_RATE=0.0","WRKD_DEPT_NAME=STORE 53","WRKD_JOB_NAME=HEAD CASHIER","WRKD_TCODE_NAME=LNCH","WRKD_HTYPE_NAME=UNPAID","WRKD_WBT_NAME=STORE 53","ACT_NAME=LUNCH","WRKD_COMMENTS=Created In Schedule Optimizer","SHFT_BRK=Y"
     *     "JT_START_TIME=20080415 1530","JT_END_TIME=20080415 1930"
     * @param skdDetColumn
     * @return
     */
    private String validateSkdDetail(String skdDetColumn, ScheduleTransactionMapper stMapper) throws WBInterfaceException{
        final DatetimeFormat OVERRIDE_TIME_FORMAT = new DatetimeFormat( OverrideData.OVERRIDE_TIME_FORMAT_STR);        
        StringBuffer sb = new StringBuffer();
        // *** add skd times
        EmployeeScheduleData esd = getEmpSkdTimes(stMapper);
       
        if (esd.getEmpskdActStartTime() != null) {
            sb.append(OverrideData.formatToken(EmployeeScheduleData.EMPSKD_ACT_START_TIME,
                OVERRIDE_TIME_FORMAT.format(esd.getEmpskdActStartTime())));
        }
        if (esd.getEmpskdActEndTime() != null) {
            sb.append(OverrideData.formatToken(EmployeeScheduleData.EMPSKD_ACT_END_TIME ,
                OVERRIDE_TIME_FORMAT.format(esd.getEmpskdActEndTime())));
        }

        for (int i=2; i <=5 ; i++) {
            if (esd.retrieveShiftStartTime(i-1) != null) {
                sb.append(OverrideData.formatToken(EmployeeScheduleData.EMPSKD_ACT_START_TIME + i ,
                    OVERRIDE_TIME_FORMAT.format(esd.retrieveShiftStartTime(i-1))));
            }
            if (esd.retrieveShiftEndTime(i-1) != null) {
                sb.append(OverrideData.formatToken(EmployeeScheduleData.EMPSKD_ACT_END_TIME + i,
                    OVERRIDE_TIME_FORMAT.format(esd.retrieveShiftEndTime(i-1))));
            }
        }            
        // *** add skd detail stuff
        List allValues = StringHelper.detokenizeStringAsList(skdDetColumn,
                String.valueOf(SKDDET_SEPARATOR_CHAR), true);        
        for (int i = 0; i < allValues.size(); i++) {
            String item = (String)allValues.get(i);
            List nameValList = StringHelper.detokenizeStringAsList(item, "=");
            String fld =(String)nameValList.get(0);   
            String val =(String)nameValList.get(1);            
            if (!skdDetFields.contains(fld)) {
                throw new WBInterfaceException("Schedule Detail field not known :" + item);
            }
            if (!fld.equals("JT_START_TIME") && !fld.equals("JT_END_TIME") ) {
                sb.append(OverrideData.formatToken(fld, val));
            }
            else {
                Date date =  WBInterfaceUtil.parseDateTime(val,
                        TIME_FMT , "Error parsing JT_START_TIME");
                sb.append(OverrideData.formatToken(fld, OVERRIDE_TIME_FORMAT.format(date)));                
            }
            if (i == allValues.size() - 1) sb.append(OverrideData.OVR_DELIM);
        }      
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        
        return sb.toString();
    }
    
    protected void addOverride(int ovrTypId, String ovrNewValue, ScheduleTransactionMapper stMapper)
        throws Exception{
        if (ovrNewValue == null) return;
        OverrideData ovr = new OverrideData();
        ovr.setEmpId( stMapper.getEmpId() );
        ovr.setOvrCreateDate( new Date() );
        ovr.setOvrStartDate( stMapper.getScheduleDate()  );
        ovr.setOvrEndDate( stMapper.getScheduleDate() );
        ovr.setOvrStatus( OverrideData.PENDING );
        ovr.setOvrtypId( ovrTypId );
        ovr.setOvrNewValue( ovrNewValue);
        ovr.setWbuNameBoth(ovrWbuName, ovrWbuName);
        if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG))  logger.debug("Created override with new value : " + ovrNewValue);
        new OverrideAccess(conn).insert( ovr , true);
   }    
    
    protected EmployeeScheduleData getEmpSkdTimes(ScheduleTransactionMapper stMapper ){
        EmployeeScheduleData esd =  new EmployeeScheduleData (); 
        for (int i=0 ; i<5 ; i++) {
            Date skdStart = stMapper.getScheduleStart(i);
            Date skdEnd = stMapper.getScheduleEnd(i);
            List brkList = stMapper.getScheduleBreakList(i);
            /* no more processing when both nulls are encountered  */
            if (skdStart == null && skdEnd == null) {
                break;
            }
            esd.changeShiftStartTime(skdStart , i);
            esd.changeShiftEndTime(skdEnd , i);
            if (brkList != null && brkList.size() > 0) {
                esd.assignEmpskdBrks(i , esd.convertBreakListToString(brkList));
            }
        }
        
        return esd;
    }    
}
