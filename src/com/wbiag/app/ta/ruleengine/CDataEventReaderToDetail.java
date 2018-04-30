package com.wbiag.app.ta.ruleengine;


import com.workbrain.app.ta.ruleengine.*;
import com.wbiag.app.ta.db.*;
import com.workbrain.server.registry.Registry;
import com.workbrain.sql.DBConnection;
import com.workbrain.app.wbinterface.db.ImportData;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.clockInterface.processing.*;
import com.workbrain.util.*;
import java.sql.*;
import org.apache.log4j.*;



/**
 * Title: CDataEventReaderToClockField.java <br>
 * Description: This data event is used to set reader information from Import data to a clock data feild (One of DKT, UDF1 or UDF2)<br>
 *
 * Created: Sep 28, 2005
 * @author cleigh
 * <p>
 * Revision History <br>
 * Sep 28, 2005 -  First Draft<br>
 * Sep 29, 2005 -  Added Registry variable and moved to SLQP
 * Oct 10, 2005 -  Added Reader Cache (bceyhan)<br>
 * <p>
 * */
public class CDataEventReaderToDetail extends DataEvent {

    private static Logger logger = Logger.getLogger(CDataEventReaderToDetail.class);


    public static final String REG_CLOCK_READER_FIELD = "system/clockprocessing/CLOCK_READER_FIELD";
    public static final String REG_CLOCK_DATA_FIELD = "system/clockprocessing/CLOCK_DATA_FIELD";

    public static final String REGVAL_CLOCK_READER_FIELD_RDRGRP = "RDRGRP_NAME";
    public static final String REGVAL_CLOCK_READER_FIELD_RDR = "RDR_NAME";

    /* (non-Javadoc)
     * @see com.workbrain.app.ta.ruleengine.DataEvent#beforeProcessOneClock(com.workbrain.app.ta.model.Clock, int, com.workbrain.sql.DBConnection, com.workbrain.app.wbinterface.db.ImportData)
     */
    public void beforeProcessOneClock(Clock clockData, int empId,
            DBConnection c, ImportData data) throws java.lang.Exception {
       if (logger.isDebugEnabled()){
           logger.debug("beforeProcessOneClock DataEvent begin ");
       }

       processReader(clockData, c, data);
    }

    protected void processReader(Clock clockData, DBConnection c, ImportData data) throws Exception {

        if (logger.isDebugEnabled()){ logger.debug("beforeProcessOneClock DataEvent processReader begin ");       }

        RdrStuff rdrStuff = validate(c, data);
        if (rdrStuff == null) {
            return;
        }

        String fieldValue = null;
        if (rdrStuff.clkRdrField.equalsIgnoreCase(REGVAL_CLOCK_READER_FIELD_RDR)) {
            fieldValue = rdrStuff.rdrName;
        }
        else if (rdrStuff.clkRdrField.equalsIgnoreCase(REGVAL_CLOCK_READER_FIELD_RDRGRP)) {
            ReaderCache cache = ReaderCache.getInstance();
            ReaderData rd = cache.getReaderData(c, rdrStuff.rdrName);
            if (rd != null) {
                String rdrgrpName = cache.getReaderGroupName(c, rd.getRdrgrpId());
                fieldValue = rdrgrpName;
            }
        }

        if (StringHelper.isEmpty(fieldValue)) {
            if (logger.isDebugEnabled()) logger.debug("Reader name or reader group name from clock was empty, no further processing in CDataEventReaderToClockField");
            return;
        }
        if (rdrStuff.clkDataField.equalsIgnoreCase(Clock.CLOCKDATA_DOCKET)) {
            CodeMapper cm = CodeMapper.createCodeMapper(c);
            DocketData dd = cm.getDocketByName(fieldValue) ;
            if (dd == null) {
                throw new RuleEngineException("Corresponding docket not found for: " + rdrStuff.rdrName);
            }
        }

        StringBuffer tempClockDataStr = new StringBuffer(100);
        tempClockDataStr.append(rdrStuff.clkDataField).append("=").append(fieldValue).toString();

        clockData.addClockData(tempClockDataStr.toString());

        if (logger.isDebugEnabled()){
            logger.debug("beforeProcessOneClock DataEvent processReader end ");
        }
    }

    private RdrStuff validate(DBConnection c, ImportData data) throws Exception{
        RdrStuff rdrStuff = new RdrStuff ();
        rdrStuff.clkRdrField = Registry.getVarString(REG_CLOCK_READER_FIELD , "");
        rdrStuff.clkDataField = Registry.getVarString(REG_CLOCK_DATA_FIELD , "" );

        if (StringHelper.isEmpty(rdrStuff.clkRdrField)
            || StringHelper.isEmpty(rdrStuff.clkDataField)){
            if (logger.isDebugEnabled()) logger.debug("CLOCK_READER_FIELD or CLOCK_DATA_FIELD is empty, no further processing in CDataEventReaderToClockField");
            return null;
        }
        else {
            if (!rdrStuff.clkRdrField.equalsIgnoreCase(REGVAL_CLOCK_READER_FIELD_RDR)
                && !rdrStuff.clkRdrField.equalsIgnoreCase(REGVAL_CLOCK_READER_FIELD_RDRGRP)) {
                throw new RuleEngineException("CLOCK_READER_FIELD must be " + REGVAL_CLOCK_READER_FIELD_RDR + " or " + REGVAL_CLOCK_READER_FIELD_RDRGRP);
            }
            if (!rdrStuff.clkDataField.equalsIgnoreCase(Clock.CLOCKDATA_DOCKET)
                && !rdrStuff.clkDataField.equalsIgnoreCase(Clock.CLOCKDATA_UDF1)
                && !rdrStuff.clkDataField.equalsIgnoreCase(Clock.CLOCKDATA_UDF2) ) {
                throw new RuleEngineException("CLOCK_DATA_FIELD must be " + Clock.CLOCKDATA_DOCKET + " or " + Clock.CLOCKDATA_UDF1 + " or " + Clock.CLOCKDATA_UDF2);
            }
        }

        rdrStuff.rdrName = ClockTransactionMapper.getReaderName(data);
        if (StringHelper.isEmpty(rdrStuff.rdrName)) {
            if (logger.isDebugEnabled()) logger.debug("Reader name from clock was empty, no further processing in CDataEventReaderToClockField");
            return null;
        }


        return rdrStuff;
    }

    private class RdrStuff {
        String clkRdrField;
        String clkDataField;
        String rdrName;
    }
}
