/*
 * Created on Sep 12, 2005
 *
 */
package com.wbiag.app.ta.db;

import com.workbrain.test.*;
import java.sql.*;
import com.workbrain.sql.*;
import junit.framework.*;

/**
 *  Title: LocalizationDataUpdateTest
 *  Description: JUnit to test LocalizationDataUpdate tool 
 *  Copyright: Copyright (c) 2005, 2006, 2007
 *  Company: Workbrain Inc.
 *
 * @author gtam@workbrain.com
*/
public class LocalizationDataUpdateTest extends TestCaseHW
{

    /**
     * @param arg0
     */
    public LocalizationDataUpdateTest(String arg0) {
        super(arg0);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(LocalizationDataUpdateTest.class);
        return result;
    }

    public void testUpdate() throws Exception
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        DBConnection conn = null;

        boolean flag = false;
        final String oldLocalization = "JUnit_OldLocalization";
        final String newLocalization = "JUnit_NewLocalization";
        final int tempId = -12346;
        final int locale = 1;
        

        LocalizationDataUpdate ldu = new LocalizationDataUpdate();

        try
        {
            conn = getConnection(); 

            // Insert into the tables which contain localization data            
            stmt = conn.prepareStatement("insert into maintenance_form values(?, 'LDU Test', ?, 'maintenance.asp', null, 'x', 'x', null, 'H', 20, null, null, -2, null, null, null, 'Y', null, null, 'X', ?, null, null, null, null, null, 0, null, null, null, null, null, null, null, null)");            
            stmt.setInt(1, tempId);
            stmt.setString(2, oldLocalization);
            stmt.setString(3, oldLocalization);
            stmt.execute();
            stmt.close();
            
            stmt = conn.prepareStatement("insert into wb_localzd_tblval values(?, 34, ?, ?, ?, ?)");
            stmt.setInt(1, tempId);
            stmt.setInt(2, locale);
            stmt.setInt(3, tempId);
            stmt.setString(4, oldLocalization);
            stmt.setString(5, oldLocalization);
            stmt.execute();
            stmt.close();

            stmt = conn.prepareStatement("insert into workbrain_field_locale_data values(?, ?, ?, ?)");
            stmt.setInt(1, tempId);
            stmt.setString(2, oldLocalization);
            stmt.setInt(3, locale);
            stmt.setString(4, oldLocalization);
            stmt.execute();
            stmt.close();
            
            stmt = conn.prepareStatement("insert into workbrain_msg_locale_data values(?, ?, ?, ?, 1, 1)");
            stmt.setInt(1, tempId);
            stmt.setString(2, oldLocalization);
            stmt.setString(3, oldLocalization);
            stmt.setInt(4, locale);
            stmt.execute();
            stmt.close();
            
            stmt = conn.prepareStatement("insert into wb_err_msg_loc_dat values(?, ?, ?, ?, 1)");
            stmt.setInt(1, tempId);
            stmt.setString(2, oldLocalization);
            stmt.setString(3, oldLocalization);
            stmt.setString(4, oldLocalization);
            stmt.execute();
            stmt.close();
            
            // Verify that the maintenance form has been entered correctly
            int mfrm_id = 0;
            stmt = conn.prepareStatement("select mfrm_id from maintenance_form " +
                    "where mfrm_desc like '%' || ? || '%'" +
                    "and mfrm_lang1 like '%' || ? || '%'");
            stmt.setString(1, oldLocalization);
            stmt.setString(2, oldLocalization);
            rs = stmt.executeQuery();
            if(rs.next())
                mfrm_id = rs.getInt(1);
            assertEquals(tempId, mfrm_id);
            rs.close();
            stmt.close();
            
            // Run the update function
            ldu.Update(oldLocalization, newLocalization, true, true, true, true, true, locale, conn);

            // Verify that the strings have been replaced
            stmt = conn.prepareStatement("select mfrm_id from maintenance_form " +
                    "where mfrm_desc like '%' || ? || '%'" +
                    "and mfrm_lang1 like '%' || ? || '%'");
            stmt.setString(1, oldLocalization);
            stmt.setString(2, oldLocalization);
            rs = stmt.executeQuery();
            flag = !rs.next();
            assertTrue(flag);
            rs.close();
            
            mfrm_id = 0;
            stmt.setString(1, newLocalization);
            stmt.setString(2, newLocalization);
            rs = stmt.executeQuery();
            if(rs.next())
                mfrm_id = rs.getInt(1);
            assertEquals(tempId, mfrm_id);            
            rs.close();
            stmt.close();
            
            stmt = conn.prepareStatement("select wbltv_id from wb_localzd_tblval " +
                    "where wbltv_data_name like '%' || ? || '%'" +
                    "and wbltv_data_desc like '%' || ? || '%'" +
                    "and wbl_id = ?");
            stmt.setString(1, oldLocalization);
            stmt.setString(2, oldLocalization);
            stmt.setInt(3, locale);
            rs = stmt.executeQuery();
            flag = !rs.next();
            assertTrue(flag);
            rs.close();

            mfrm_id = 0;
            stmt.setString(1, newLocalization);
            stmt.setString(2, newLocalization);
            rs = stmt.executeQuery();
            if(rs.next())
                mfrm_id = rs.getInt(1);
            assertEquals(tempId, mfrm_id);            
            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("select wbfld_id from workbrain_field_locale_data " +
                    "where wbfld_name like '%' || ? || '%'" +
                    "and wbll_id = ?");
            stmt.setString(1, oldLocalization);
            stmt.setInt(2, locale);
            rs = stmt.executeQuery();
            flag = !rs.next();
            assertTrue(flag);
            rs.close();

            mfrm_id = 0;
            stmt.setString(1, newLocalization);
            rs = stmt.executeQuery();
            if(rs.next())
                mfrm_id = rs.getInt(1);
            assertEquals(tempId, mfrm_id);            
            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("select wbmldt_id from workbrain_msg_locale_data " +
                    "where wbmldt_text like '%' || ? || '%'" +
                    "and wbll_id = ?");
            stmt.setString(1, oldLocalization);
            stmt.setInt(2, locale);
            rs = stmt.executeQuery();
            flag = !rs.next();
            assertTrue(flag);
            rs.close();

            mfrm_id = 0;
            stmt.setString(1, newLocalization);
            rs = stmt.executeQuery();
            if(rs.next())
                mfrm_id = rs.getInt(1);
            assertEquals(tempId, mfrm_id);            
            rs.close();
            stmt.close();

            stmt = conn.prepareStatement("select wbemldt_id from wb_err_msg_loc_dat " +
                    "where wbemldt_pattern like '%' || ? || '%'" +
                    "and wbl_id = ?");
            stmt.setString(1, oldLocalization);
            stmt.setInt(2, locale);
            rs = stmt.executeQuery();
            flag = !rs.next();
            assertTrue(flag);
            rs.close();

            mfrm_id = 0;
            stmt.setString(1, newLocalization);
            rs = stmt.executeQuery();
            if(rs.next())
                mfrm_id = rs.getInt(1);
            assertEquals(tempId, mfrm_id);            
            rs.close();
            stmt.close();

            
        }
        finally
        {
            if(rs != null)
                rs.close();
            if(stmt != null)
                stmt.close();
        }
    }


    public static void main(String[] args) throws Exception {
            junit.textui.TestRunner.run(suite());
    }
    
}
