/*---------------------------------------------------------------------------
   (C) Copyright Workbrain Inc. 2005
 --------------------------------------------------------------------------*/
package com.wbiag.app.modules.retailSchedule.db;

import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.wbiag.app.modules.retailSchedule.model.CorporateEntityStaffRequirementDetail;
import com.workbrain.app.modules.retailSchedule.exceptions.SQLRetailException;
import com.workbrain.app.ta.db.RecordAccess;
import com.workbrain.sql.DBConnection;

/**
 * This class is used to access the SO_CSD_DETAIL table, which is the table
 * used to store additional detail data related to location staffing
 * requirements.
 *
 * @author James Tam
 * @see com.workbrain.app.ta.db.RecordAccess
 */
public class CsdDetailAccess extends RecordAccess {
    private static Logger logger = Logger.getLogger(CsdDetailAccess.class);

    /**
     * Database table name
     */
    public final static String TABLE_NAME = "so_csd_detail";

    /**
     * Primary key field
     */
    public final static String PRIMARY_KEY = "csddet_id";

    /**
     * Primary key sequence
     */
    public final static String PRI_KEY_SEQ = "seq_csddet_id";

    /**
     * Constructor for this class.
     *
     * @param conn Database connection for accessing the table
     */
    public CsdDetailAccess(DBConnection conn) {
        super(conn);
    }

    /**
     * Returns a new instance of CorporateEntityStaffRequirementDetail with the
     * next id from the sequence.
     *
     * @return A new instance of CorporateEntityStaffRequirementDetail
     * @throws SQLRetailException If an error occurs while accessing the
     * sequence to retrieve the next id
     */
    public CorporateEntityStaffRequirementDetail createCorporateEntityStaffRequirementDetail() throws SQLRetailException {
        try {
            CorporateEntityStaffRequirementDetail data = new CorporateEntityStaffRequirementDetail(
                getDBConnection().getDBSequence(PRI_KEY_SEQ).getNextValue());
            return data;
        } catch (SQLException sqle) {
            logger.error(sqle.getMessage(), sqle);
            throw new SQLRetailException(sqle.getMessage(), sqle);
        }
    }

}