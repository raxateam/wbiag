package com.wbiag.app.ta.db;

import java.sql.*;

import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.sql.*;

/**
 *  Title: LocalizationDataUpdate
 *  Description: Tool to replace all occurances of one localization string with another. 
 *  Copyright: Copyright (c) 2005, 2006, 2007
 *  Company: Workbrain Inc.
 *
 * @author gtam@workbrain.com
*/
public class LocalizationDataUpdate 
{

    public LocalizationDataUpdate(){}

    /**
     * Replaces oldText with newText in the specified database tables.
     * @param oldText
     * @param newText
     * @param maintenanceForm
     * @param maintenanceFormData
     * @param fieldData
     * @param textData
     * @param errorData
     * @param locale
     * @param conn
     * @throws SQLException for errors while updating localization text
    */
    public int Update(String oldText, String newText, 
                       boolean maintenanceForm,
                       boolean maintenanceFormData,
                       boolean fieldData,
                       boolean textData,
                       boolean errorData,
                       int locale,
                       DBConnection conn) throws SQLException
    {
        PreparedStatement stmt = null;
        int rowsModified = 0;
        try
        {
            conn.setAutoCommit(false);

            /* MFRM FORM */
            if (maintenanceForm)
            {
                stmt = conn.prepareStatement("update maintenance_form " +
                                             "set mfrm_desc = replace(mfrm_desc, ?, ?)" +
                                             "where mfrm_desc like '%' || ? || '%'");
                stmt.setString(1, oldText);
                stmt.setString(2, newText);
                stmt.setString(3, oldText);     
                rowsModified += stmt.executeUpdate();
                stmt.close();
    
                stmt = conn.prepareStatement("update maintenance_form " +
                                             "set mfrm_lang1 = replace(mfrm_lang1, ?, ?)" +
                                             "where mfrm_lang1 like '%' || ? || '%'");
                stmt.setString(1, oldText);
                stmt.setString(2, newText);
                stmt.setString(3, oldText);     
                rowsModified += stmt.executeUpdate();
                stmt.close();
           }
         
            /* MFRM Data */
            if (maintenanceFormData)
            {           
                stmt = conn.prepareStatement("update wb_localzd_tblval " +
                                             "set wbltv_data_name = replace(wbltv_data_name, ?, ?)" +
                                             "where wbltv_data_name like '%' || ? || '%'" +
                                             "and wbl_id = ?");
                stmt.setString(1, oldText);
                stmt.setString(2, newText);
                stmt.setString(3, oldText);     
                stmt.setInt(4, locale);
                rowsModified += stmt.executeUpdate();
                stmt.close();
                  
                stmt = conn.prepareStatement("update wb_localzd_tblval " +
                                             "set wbltv_data_desc = replace(wbltv_data_desc, ?, ?)" +
                                             "where wbltv_data_desc like '%' || ? || '%'" +
                                             "and wbl_id = ?");
                stmt.setString(1, oldText);
                stmt.setString(2, newText);
                stmt.setString(3, oldText);     
                stmt.setInt(4, locale);
                rowsModified += stmt.executeUpdate();
                stmt.close();
            }
                
             /* FIELD */
            if (fieldData)
            {
                stmt = conn.prepareStatement("update workbrain_field_locale_data " +
                                             "set wbfld_name = replace(wbfld_name, ?, ?)" +
                                             "where wbfld_name like '%' || ? || '%'" +
                                             "and wbll_id = ?");
                stmt.setString(1, oldText);
                stmt.setString(2, newText);
                stmt.setString(3, oldText);     
                stmt.setInt(4, locale);
                rowsModified += stmt.executeUpdate();
                stmt.close();
           }
            
            /* TEXT */
            if (textData)
            {
                stmt = conn.prepareStatement("update workbrain_msg_locale_data " +
                                             "set wbmldt_text = replace(wbmldt_text, ?, ?)" +
                                             "where wbmldt_text like '%' || ? || '%'" +
                                             "and wbll_id = ?");
                stmt.setString(1, oldText);
                stmt.setString(2, newText);
                stmt.setString(3, oldText);     
                stmt.setInt(4, locale);
                rowsModified += stmt.executeUpdate();
                stmt.close();
            }
               
            /* ERR */
            if (errorData)
            {               
                stmt = conn.prepareStatement("update wb_err_msg_loc_dat " +
                                             "set wbemldt_pattern = replace(wbemldt_pattern, ?, ?)" +
                                             "where wbemldt_pattern like '%' || ? || '%'" +
                                             "and wbl_id = ?");
                stmt.setString(1, oldText);
                stmt.setString(2, newText);
                stmt.setString(3, oldText);     
                stmt.setInt(4, locale);
                rowsModified += stmt.executeUpdate();
                stmt.close();
            }
                   
        }
        catch(SQLException e)
        {
            if(conn != null)
            {
                conn.rollback();
            }
            //logger.warn("Exception while updating localization text", e);
            throw e;
        }
        finally
        {
            if(stmt != null)
            {
                stmt.close();
            }
        }
        return rowsModified;
    }
            
}
