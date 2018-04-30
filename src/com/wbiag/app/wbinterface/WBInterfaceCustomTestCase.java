package com.wbiag.app.wbinterface;

import java.io.*;
import java.util.*;

import com.workbrain.app.wbinterface.*;
import com.workbrain.app.wbinterface.db.*;
import com.workbrain.app.wbinterface.hr2.*;
import com.workbrain.app.wbinterface.schedulein.*;
import com.workbrain.test.*;
import junit.framework.*;
import java.sql.*;
/**
 * Common Test Case Methods for WBInterface Custom Tests.
 */
public class WBInterfaceCustomTestCase extends WBInterfaceTestCase {

    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(WBInterfaceCustomTestCase.class);


    public WBInterfaceCustomTestCase(String testName) throws Exception {
        super(testName);
    }

    /**
     * Creates DefaultHRTransactionParams
     * @return param
     * @throws Exception
     */
    public HashMap createDefaultHRTransactionParams() throws Exception{
        HashMap param = new HashMap();
        param.put(HRRefreshTransaction.PARAM_CALCGRP_MAP_NAME, "Default CalcGroup Mapping" );
        param.put(HRRefreshTransaction.PARAM_PAYGRP_MAP_NAME, "Default PayGroup Mapping" );
        param.put(HRRefreshTransaction.PARAM_SECGRP_MAP_NAME, "Default SecurityGroup Mapping" );
        param.put(HRRefreshTransaction.PARAM_SHFPAT_MAP_NAME, "Default ShiftPattern Mapping" );
        param.put(HRRefreshTransaction.PARAM_BATCH_PROCESS_SIZE , "20");

        return param;
    }

    public void updateWbintTypeClass(int typId, String classPath) throws SQLException {
        PreparedStatement ps = null;
        try {
            String sql = "UPDATE wbint_type SET wbityp_javaclass = ? WHERE wbityp_id = ?";
            ps = getConnection().prepareStatement(sql);
            ps.setString(1 , classPath);
            ps.setInt(2 , typId);
            ps.executeUpdate();
        }
        finally {
            if (ps != null) ps.close();
        }

    }

}
