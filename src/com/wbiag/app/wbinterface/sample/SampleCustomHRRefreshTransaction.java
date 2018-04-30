package com.wbiag.app.wbinterface.sample;

import java.sql.*;
import java.util.*;
import java.util.Date;

import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.hr2.engine.*;
import com.workbrain.sql.*;
import com.workbrain.util.*;
import com.wbiag.app.wbinterface.hr2.*;

/**
 * Sample CustomHRRefreshTransaction that uses one or more customizations.
 *
 **/
public class SampleCustomHRRefreshTransaction extends  HRRefreshTransaction {
    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SampleCustomHRRefreshTransaction.class);

    public static final int ST_EMP_SKILL_COL = 90;
    public static final int SO_EXTRA_COL = 91;

    /**
     * Override this class to customize after process batch events.
     * At this time, all HRRefreshTransactionData data has been processed.
     * All processed is available through <code>HRRefreshCache</code>.
     *
     * @param conn                          DBConnection
     * @param hrRefreshTransactionDataList  List of <code>HRRefreshTransactionData</code>
     * @param process                       HRRefreshProcessor
     * @throws Exception
     */
    protected void postProcessBatch(DBConnection conn,
                                    List hrRefreshTransactionDataList,
                                    HRRefreshProcessor process) throws Exception {
        HRRefreshTransactionSTEmpSkill st = new HRRefreshTransactionSTEmpSkill();
        st.setCustomColInds(new int[]{ST_EMP_SKILL_COL});
        st.postProcessBatch(conn, hrRefreshTransactionDataList , process);
        HRRefreshTransactionSOExtra so = new HRRefreshTransactionSOExtra();
        so.setCustomColInds(new int[]{SO_EXTRA_COL});
        so.postProcessBatch(conn, hrRefreshTransactionDataList , process);
    }


}