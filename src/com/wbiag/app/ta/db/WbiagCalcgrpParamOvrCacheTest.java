package com.wbiag.app.ta.db;

import com.wbiag.app.ta.model.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.test.*;
import java.sql.*;
import java.util.Date;

import org.apache.log4j.*;
import org.apache.log4j.BasicConfigurator;

import com.workbrain.sql.*;
import com.workbrain.tool.overrides.*;
import com.workbrain.util.*;
import junit.framework.*;

/**
 *  Title: WbiagCalcgrpParamOvrCacheTest
 *  Description: Test for WbiagCalcgrpParamOvrCache object
 *  Copyright: Copyright (c) 2005, 2006, 200nc.
 *
 * @author gtam@workbrain.com
*/
public class WbiagCalcgrpParamOvrCacheTest extends TestCaseHW{

    private static final Logger logger = Logger.getLogger(WbiagCalcgrpParamOvrCacheTest.class);

    public WbiagCalcgrpParamOvrCacheTest(String arg0) {
        super(arg0);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(WbiagStateCacheTest.class);

        // TODO - Remove this.
        BasicConfigurator.configure();

        return result;
    }

    /**
     * Tests cache methods.
     * @throws Exception
     */
    public void xGet() throws Exception {

        WbiagCalcgrpParamOvrCache cache = WbiagCalcgrpParamOvrCache.getInstance();

        insertCalcGrpParamOvr(0, (Date)DateHelper.getCurrentDate(), (Date)DateHelper.getCurrentDate(), "Yes", "True");

        WbiagCalcgrpParamOvrData pd = cache.getCalcgrpParamOvrByCalcgrpDateEff(getConnection(), 0, (Date)DateHelper.getCurrentDate());
        assertNotNull(pd);

    }


    private void insertCalcGrpParamOvr(int calcGrpId, Date startDate, Date endDate, 
            String allowPartialBreaks, String noSwipeForBreaks) throws Exception{

        Connection conn = getConnection();
        WbiagCalcgrpParamOvrCache wbiagCalcgrpParamOvrCache = WbiagCalcgrpParamOvrCache.getInstance();
        wbiagCalcgrpParamOvrCache.updateCacheContents(conn);

        int wcpoId = getConnection().getDBSequence("seq_wcpo_id").getNextValue();

        PreparedStatement pstmt = conn.prepareStatement("insert into wbiag_calcgrp_param_ovr "
                                                        + "(wcpo_id, calcgrp_id, wcpo_start_date, wcpo_end_date, wcpo_allow_partial_breaks, wcpo_no_swipe_for_breaks) "
                                                        + "values (?,?,?,?,?,?)");

        pstmt.setInt(1, wcpoId);
        pstmt.setInt(2, calcGrpId);
        pstmt.setDate(3, new java.sql.Date(startDate.getTime()));
        pstmt.setDate(4, new java.sql.Date(endDate.getTime()));
        pstmt.setString(5, allowPartialBreaks);
        pstmt.setString(6, noSwipeForBreaks);
        pstmt.executeUpdate();
        pstmt.close();

    }

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
