package com.wbiag.app.ta.db;

import com.workbrain.test.*;
import java.sql.*;
import com.workbrain.sql.*;
import junit.framework.*;

/**
 *
 * @deprecated Core as of 5.0, use Core Security Manager
 */

public class CopySecurityGroupTest extends TestCaseHW
{

    /**
     * @param arg0
     */
    public CopySecurityGroupTest(String arg0) {
        super(arg0);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(CopySecurityGroupTest.class);
        return result;
    }

    public void xCopy() throws Exception
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        DBConnection conn = null;

        boolean flag = false;
        int NEWwbg_id = 0;
        int OLDwbg_id = 0;
        String OLD = "EMPLOYEE";
        String NEW = "JUNITTESTCOPY";

        CopySecurityGroup cp = new CopySecurityGroup();

        try
        {
            cp.Copy(OLD, NEW, getConnection());

            conn = getConnection();

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_name=?");
            stmt.setString(1, NEW);
            rs = stmt.executeQuery();
            if(rs.next())
                NEWwbg_id = rs.getInt(1);

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_name=?");
            stmt.setString(1, OLD);
            rs = stmt.executeQuery();
            if(rs.next())
                OLDwbg_id = rs.getInt(1);

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_id=? and (wbg_lockdown, wbg_flag1) not in (select wbg_lockdown, wbg_flag1 from workbrain_group where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next();

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_id=? and (wbg_lockdown, wbg_flag1) not in (select wbg_lockdown, wbg_flag1 from workbrain_group where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_grp where wbg_id=? and (mfrm_id, mfg_def_perm, wbp_id) not in (select mfrm_id, mfg_def_perm, wbp_id from maintenance_form_grp where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_grp where wbg_id=? and (mfrm_id, mfg_def_perm, wbp_id) not in (select mfrm_id, mfg_def_perm, wbp_id from maintenance_form_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_element_prmsn where wbg_id=? and (mfrm_id, mfep_element_name, mfep_permission_flag) not in (select mfrm_id, mfep_element_name, mfep_permission_flag from maintenance_form_element_prmsn where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_element_prmsn where wbg_id=? and (mfrm_id, mfep_element_name, mfep_permission_flag) not in (select mfrm_id, mfep_element_name, mfep_permission_flag from maintenance_form_element_prmsn where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from override_type_grp where wbg_id=? and (ovrtyp_id, wbp_id) not in (select ovrtyp_id, wbp_id from override_type_grp where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from override_type_grp where wbg_id=? and (ovrtyp_id, wbp_id) not in (select ovrtyp_id, wbp_id from override_type_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from pop_up_grp where wbg_id=? and pop_id not in (select pop_id from pop_up_grp where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from pop_up_grp where wbg_id=? and pop_id not in (select pop_id from pop_up_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from vr_toc_grp where wbg_id=? and (vrtoc_id, wbp_id) not in (select vrtoc_id, wbp_id from vr_toc_grp where wbg_id=?) ");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from vr_toc_grp where wbg_id=? and (vrtoc_id, wbp_id) not in (select vrtoc_id, wbp_id from vr_toc_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag && (NEWwbg_id!=OLDwbg_id);

            assertTrue(flag);
        }
        finally
        {
            if(rs != null)
                rs.close();
            if(stmt != null)
                stmt.close();
        }
    }

    /*
     * Test the overwrite method using the created group from testCopy -
     * must be run after testCopy and before testDelete.
     */
    public void xOverwriteExistingGroup () throws Exception
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        DBConnection conn = null;
        boolean flag = false;
        int NEWwbg_id = 0;
        int OLDwbg_id = 0;
        String OLD = "EMPLOYEE";
        String NEW = "JUNITTESTCOPY";

        CopySecurityGroup cp = new CopySecurityGroup();

        try
        {
            cp.Overwrite(OLD, NEW, getConnection());
            conn = getConnection();

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_name=?");
            stmt.setString(1, NEW);
            rs = stmt.executeQuery();
            if(rs.next())
                NEWwbg_id = rs.getInt(1);

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_name=?");
            stmt.setString(1, OLD);
            rs = stmt.executeQuery();
            if(rs.next())
                OLDwbg_id = rs.getInt(1);

            // Test that the maintenance_form_grp entries are identical for both security groups.
            stmt = conn.prepareStatement("select wbg_id from maintenance_form_grp where wbg_id=? and (mfrm_id, mfg_def_perm, wbp_id) not in (select mfrm_id, mfg_def_perm, wbp_id from maintenance_form_grp where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next();

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_grp where wbg_id=? and (mfrm_id, mfg_def_perm, wbp_id) not in (select mfrm_id, mfg_def_perm, wbp_id from maintenance_form_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            // Test that the maintenance_form_element_prmsn entries are identical for both security groups.
            stmt = conn.prepareStatement("select wbg_id from maintenance_form_element_prmsn where wbg_id=? and (mfrm_id, mfep_element_name, mfep_permission_flag) not in (select mfrm_id, mfep_element_name, mfep_permission_flag from maintenance_form_element_prmsn where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_element_prmsn where wbg_id=? and (mfrm_id, mfep_element_name, mfep_permission_flag) not in (select mfrm_id, mfep_element_name, mfep_permission_flag from maintenance_form_element_prmsn where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            // Test that the override_type_grp entries are identical for both security groups.
            stmt = conn.prepareStatement("select wbg_id from override_type_grp where wbg_id=? and (ovrtyp_id, wbp_id) not in (select ovrtyp_id, wbp_id from override_type_grp where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from override_type_grp where wbg_id=? and (ovrtyp_id, wbp_id) not in (select ovrtyp_id, wbp_id from override_type_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            // Test that the pop_up_grp entries are identical for both security groups.
            stmt = conn.prepareStatement("select wbg_id from pop_up_grp where wbg_id=? and pop_id not in (select pop_id from pop_up_grp where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from pop_up_grp where wbg_id=? and pop_id not in (select pop_id from pop_up_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            // Test that the vr_toc_grp entries are identical for both security groups.
            stmt = conn.prepareStatement("select wbg_id from vr_toc_grp where wbg_id=? and (vrtoc_id, wbp_id) not in (select vrtoc_id, wbp_id from vr_toc_grp where wbg_id=?) ");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from vr_toc_grp where wbg_id=? and (vrtoc_id, wbp_id) not in (select vrtoc_id, wbp_id from vr_toc_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag && (NEWwbg_id!=OLDwbg_id);

            assertTrue(flag);

        }
        finally
        {
            if(rs != null)
                rs.close();
            if(stmt != null)
                stmt.close();
        }
    }


    public void xDelete() throws Exception
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        DBConnection conn = null;

        String NEW = "JUNITTESTCOPY";

        CopySecurityGroup cp = new CopySecurityGroup();

        try
        {
            cp.Delete(NEW, getConnection());
            conn = getConnection();
            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_name=?");
            stmt.setString(1, NEW);
            rs = stmt.executeQuery();
            assertTrue(!rs.next());
        }
        finally
        {
            if(rs != null)
                rs.close();
            if(stmt != null)
                stmt.close();
        }
    }

    public void xCopyInvalidGroup () throws Exception
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        DBConnection conn = null;

        String OLD = "JUNITINVALIDGROUP";
        String NEW = "JUNITTESTINVALIDGROUP";

        CopySecurityGroup cp = new CopySecurityGroup();

        try
        {
            cp.Copy(OLD, NEW, getConnection());
        }
        catch(Exception e)
        {
            assertTrue("java.sql.SQLException: Exhausted Resultset".equals(e.toString()));
        }
        try
        {
            conn = getConnection();
            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_name=?");
            stmt.setString(1, NEW);
            rs = stmt.executeQuery();
            assertTrue(!rs.next());
        }
        finally
        {
            if(rs != null)
                rs.close();
            if(stmt != null)
                stmt.close();
        }
    }


    public void xCopyIntoExistingGroup () throws Exception
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        DBConnection conn = null;

        String OLD = "EMPLOYEE";
        String NEW = "EMPLOYEE";

        CopySecurityGroup cp = new CopySecurityGroup();

        try
        {
            cp.Copy(OLD, NEW, getConnection());
        }
        catch(Exception e)
        {
            assertTrue(e.toString().indexOf("unique constraint") != -1);
        }
    }

    public void xOverwriteNonExistingGroup() throws Exception
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        DBConnection conn = null;

        boolean flag = false;
        int NEWwbg_id = 0;
        int OLDwbg_id = 0;
        String OLD = "EMPLOYEE";
        String NEW = "JUNITNEWOVERWRITE";

        CopySecurityGroup cp = new CopySecurityGroup();

        try
        {
            cp.Overwrite(OLD, NEW, getConnection());

            conn = getConnection();

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_name=?");
            stmt.setString(1, NEW);
            rs = stmt.executeQuery();
            if(rs.next())
                NEWwbg_id = rs.getInt(1);

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_name=?");
            stmt.setString(1, OLD);
            rs = stmt.executeQuery();
            if(rs.next())
                OLDwbg_id = rs.getInt(1);

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_id=? and (wbg_lockdown, wbg_flag1) not in (select wbg_lockdown, wbg_flag1 from workbrain_group where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next();

            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_id=? and (wbg_lockdown, wbg_flag1) not in (select wbg_lockdown, wbg_flag1 from workbrain_group where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_grp where wbg_id=? and (mfrm_id, mfg_def_perm, wbp_id) not in (select mfrm_id, mfg_def_perm, wbp_id from maintenance_form_grp where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_grp where wbg_id=? and (mfrm_id, mfg_def_perm, wbp_id) not in (select mfrm_id, mfg_def_perm, wbp_id from maintenance_form_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_element_prmsn where wbg_id=? and (mfrm_id, mfep_element_name, mfep_permission_flag) not in (select mfrm_id, mfep_element_name, mfep_permission_flag from maintenance_form_element_prmsn where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from maintenance_form_element_prmsn where wbg_id=? and (mfrm_id, mfep_element_name, mfep_permission_flag) not in (select mfrm_id, mfep_element_name, mfep_permission_flag from maintenance_form_element_prmsn where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from override_type_grp where wbg_id=? and (ovrtyp_id, wbp_id) not in (select ovrtyp_id, wbp_id from override_type_grp where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from override_type_grp where wbg_id=? and (ovrtyp_id, wbp_id) not in (select ovrtyp_id, wbp_id from override_type_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from pop_up_grp where wbg_id=? and pop_id not in (select pop_id from pop_up_grp where wbg_id=?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from pop_up_grp where wbg_id=? and pop_id not in (select pop_id from pop_up_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from vr_toc_grp where wbg_id=? and (vrtoc_id, wbp_id) not in (select vrtoc_id, wbp_id from vr_toc_grp where wbg_id=?) ");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag;

            stmt = conn.prepareStatement("select wbg_id from vr_toc_grp where wbg_id=? and (vrtoc_id, wbp_id) not in (select vrtoc_id, wbp_id from vr_toc_grp where wbg_id=?)");
            stmt.setInt(1, NEWwbg_id);
            stmt.setInt(2, OLDwbg_id);
            rs = stmt.executeQuery();
            flag = !rs.next() && flag && (NEWwbg_id!=OLDwbg_id);

            assertTrue(flag);
        }
        finally
        {
            cp.Delete(NEW, getConnection());        //Clean up the created security group
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
