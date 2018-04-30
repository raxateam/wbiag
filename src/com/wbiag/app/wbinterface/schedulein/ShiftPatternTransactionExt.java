package com.wbiag.app.wbinterface.schedulein;

import java.sql.*;
import java.util.*;

import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.schedulein.*;
import com.workbrain.app.wbinterface.schedulein.db.*;
import com.workbrain.app.wbinterface.schedulein.model.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
/**
 * Transaction type for Extended Shift Pattern data processing.
 *
 **/
public class ShiftPatternTransactionExt extends ShiftPatternTransaction{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ShiftPatternTransactionExt.class);

    public static final String CONTINUES_FLAG = "*";
    public static final int CONTINUE_SANITY_MAX = 20;
    public static final String SANITY_MSG = "Maximum number of consecutive continue-next records is reached : " + CONTINUES_FLAG + ", no shift pattern creation/assignment will be done";
    public static final String NO_PROCESS_MSG = "No shift pattern creation/assignment was done as import data was marked to continue on next record";
    private ShiftPatternMapper spBefore = null;
    private int continueSanityCnt = 0;


    public ShiftPatternTransactionExt() {
    }

    /**
    * Processes a given row from WBINT_IMPORT.
    **/
    public void process(ImportData data, DBConnection conn)
                throws WBInterfaceException, SQLException{

        log( "ShiftPatternTransactionExt.process" );
        this.data = data;
        long oneMeter = System.currentTimeMillis();
        try {
            preProcess (data , conn);
            boolean continuesNext = !StringHelper.isEmpty(data.getField(RESERVED))
                && CONTINUES_FLAG.equals(data.getField(RESERVED));
            ShiftPatternMapper spThis = setValidateFieldValues();
            if (spBefore == null) {
                spBefore = spThis;
            }
            else {
                Iterator iter = spThis.getShiftPatternShiftList().iterator();
                while (iter.hasNext()) {
                    ShiftPatternMapper.ShiftPatternShiftsThis item
                        = (ShiftPatternMapper.ShiftPatternShiftsThis)iter.next();
                    spBefore.addToShiftPatternShiftList(item);
                }
            }
            if (!continuesNext) {
                createOverride(spBefore);
                spBefore = null;
                continueSanityCnt = 0;
            }
            else {
                continueSanityCnt++;
                if (continueSanityCnt > CONTINUE_SANITY_MAX) {
                    spBefore = null;
                    throw new RuntimeException(SANITY_MSG);
                }
                else {
                    message = NO_PROCESS_MSG;
                    log(message);
                }
            }
            postProcess (data , spThis , conn);
        } catch( Throwable t ) {
            status = ImportData.STATUS_ERROR;
            message = t.getMessage();
            WBInterfaceException.throwWBInterfaceException(t);
        }
    }

    protected void createOverride(ShiftPatternMapper spMapper) throws Exception {
        ShiftPatternManager.ShiftPatternKey keyData = spManager.
            retrieveShiftPattern(spMapper);
        StringBuffer ovrNewValue = new StringBuffer(200);
        if (keyData != null) {
            int matchingSpOffset = keyData.getOffset();
            String matchingSpName = keyData.getShftpatName();
            if (createsEmpOverride) {

                ovrNewValue.append(OverrideData.formatToken(EmployeeData.EMP_SHFTPAT_NAME, matchingSpName));
                if (matchingSpOffset != 0) {
                    ovrNewValue.append(OverrideData.formatToken(EmployeeData.EMP_SHFTPAT_OFFSET, matchingSpOffset));
                }
            }
            message = "Shift pattern exists, with name : " + matchingSpName +
                " and offset : " + matchingSpOffset;
            log(message);
        }
        else {
            String shftPatName = createShiftPattern(spMapper);
            if (createsEmpOverride) {
                ovrNewValue.append(OverrideData.formatToken(EmployeeData.EMP_SHFTPAT_NAME, shftPatName));
            }
            message =  "Shift pattern does not exist, new pattern created with name : " + shftPatName;
            log(message);
        }
        if (ovrNewValue.length() > 0) {
            // *** get rid of last comma
            if (ovrNewValue.charAt(ovrNewValue.length() - 1) == OverrideData.OVR_DELIM.charAt(0)) {
                ovrNewValue.deleteCharAt(ovrNewValue.length() - 1);
            }
            processOverride(ovrNewValue.toString());
        }
    }

}



