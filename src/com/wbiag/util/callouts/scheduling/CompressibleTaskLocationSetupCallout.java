/*---------------------------------------------------------------------------
  (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.util.callouts.scheduling;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.log4j.Logger;

import com.wbiag.app.modules.retailSchedule.db.CsdDetailAccess;
import com.workbrain.app.modules.retailSchedule.db.DBInterface;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.exceptions.SQLRetailException;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntityStaffRequirement;
import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.callouts.scheduling.LocationSetupContext;

/**
 * This class contains the implementation code related to the Compressible Task 
 * rule to be called by the Location Setup Callout implemetation.
 *
 * @author James Tam
 */
public class CompressibleTaskLocationSetupCallout {
    private static Logger logger = Logger.getLogger(CompressibleTaskLocationSetupCallout.class);

    /**
     * Deletes all compression factor (staffing requirement details) records
     * for a given location from the db.
     *
     * @param context Context object containing objects related to location
     * setup callouts
     * @throws RetailException If an error occurs while deleting records
     */
    public void deleteAllLocCompressionFactors(LocationSetupContext context) throws RetailException {
        CorporateEntity cE = context.getCorporateEntity();

        if (cE==null) {
            logger.error("CorporateEntity object not set in context.");
            throw (new RetailException("CorporateEntity object not set in context."));            
        }

        String locId = cE.getSkdgrpData().getSkdgrpId().toString();
        deleteAllStaffReqDetailsForLoc(locId);
    }

    /**
     * Updates compression factors (i.e. delete compression factors related to
     * staffing requirements that are being removed) for a location.
     *
     * @param context Context object containing objects related to location
     * setup callouts
     * @throws RetailException If an error occurs while deleting records
     */
    public void updateLocCompressionFactors(LocationSetupContext context) throws RetailException {
        CorporateEntity cE = context.getCorporateEntity();

        if (cE==null) {
            logger.error("CorporateEntity object not set in context.");
            throw (new RetailException("CorporateEntity object not set in context."));            
        }

        List currStaffReqs = cE.getStaffReqList();
        String locId = cE.getSkdgrpData().getSkdgrpId().toString();

        if (currStaffReqs.size()==0) {
            deleteAllStaffReqDetailsForLoc(locId);
        } else {
            CorporateEntityStaffRequirement staffReq = null;
            DBConnection conn = DBInterface.getCurrentConnection();
            Statement stat = null;
            StringBuffer sbCurrStaffReqIds = new StringBuffer();

            for (int i=0; i<currStaffReqs.size(); i++ ) {
                if (i>0) {
                    sbCurrStaffReqIds.append(",");
                }
                staffReq = (CorporateEntityStaffRequirement)currStaffReqs.get(i);
                sbCurrStaffReqIds.append(staffReq.getCsdId().toString());
            }

            StringBuffer sbStat = new StringBuffer("DELETE FROM ");
            sbStat.append(CsdDetailAccess.TABLE_NAME);
            sbStat.append(" WHERE csd_id IN (SELECT csd_id FROM ");
            sbStat.append(CorporateEntityStaffRequirement.TABLE_NAME);
            sbStat.append(" WHERE skdgrp_id = ").append(locId);
            sbStat.append(") AND csd_id NOT IN (");
            sbStat.append(sbCurrStaffReqIds.toString()).append(")");

            try {
                stat = conn.createStatement();
                stat.executeUpdate(sbStat.toString());
            } catch (SQLException sqle) {
                logger.error("Cannot delete compression factor records: " + sqle.getMessage(), sqle);
                throw new SQLRetailException("Cannot delete compression factor records: " + sqle.getMessage(), sqle);
            } finally {
                SQLHelper.cleanUp(stat);
            }
        }
    }

    /* Deletes all staffing requirement detail records from the DB for a location */
    private void deleteAllStaffReqDetailsForLoc(String locId) throws SQLRetailException {
        DBConnection conn = DBInterface.getCurrentConnection();
        Statement stat = null;
        StringBuffer sbStat = new StringBuffer("DELETE FROM ");
        sbStat.append(CsdDetailAccess.TABLE_NAME);
        sbStat.append(" WHERE csd_id IN (SELECT csd_id FROM ");
        sbStat.append(CorporateEntityStaffRequirement.TABLE_NAME);
        sbStat.append(" WHERE skdgrp_id = ").append(locId).append(")");

        try {
            stat = conn.createStatement();
            stat.executeUpdate(sbStat.toString());
        } catch (SQLException sqle) {
            logger.error("Cannot delete compression factor records:  " + sqle.getMessage(), sqle);
            throw new SQLRetailException("Cannot delete compression factor records: " + sqle.getMessage(), sqle);
        } finally {
            SQLHelper.cleanUp(stat);
        }
    }

}