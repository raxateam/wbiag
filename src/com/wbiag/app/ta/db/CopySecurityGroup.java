package com.wbiag.app.ta.db;

import java.sql.*;

import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.sql.*;

/**
 *
 * @deprecated Core as of 5.0, use Core Security
 */
public class CopySecurityGroup
{
    //private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(CopySecurityGroup.class);

    public CopySecurityGroup() {
    }

    public void Copy(String OldSecGroup, String NewSecGroup, DBConnection conn) throws SQLException
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {

            int OLDwbg_id;
            int NEWwbg_id;

            conn.setAutoCommit(false);

            stmt = conn.prepareStatement("select wbg_id, wbg_lockdown, wbg_flag1 from workbrain_group where wbg_name = ?");
            stmt.setString(1, OldSecGroup.toUpperCase());

            rs = stmt.executeQuery();
            rs.next();

            OLDwbg_id = rs.getInt(1);
            NEWwbg_id = conn.getDBSequence("SEQ_WBG_ID").getNextValue();

            //insert into workbrain_group
            stmt = conn.prepareStatement("INSERT INTO workbrain_group(wbg_id, wbg_name, wbg_lockdown, wbg_flag1 )VALUES(?,?,?,?)" );
            stmt.setInt(1, NEWwbg_id);
            stmt.setString(2, NewSecGroup.toUpperCase());
            stmt.setString(3, rs.getString(2));
            stmt.setString(4, rs.getString(3));
            stmt.executeUpdate();

            //insert into maintenance_form_grp
            stmt = conn.prepareStatement("select mfrm_id, mfg_def_perm, wbp_id from maintenance_form_grp where wbg_id = ? and mfrm_id not in (select mfrm_id from maintenance_form_grp where wbg_id = ?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            stmt = conn.prepareStatement("insert into maintenance_form_grp(mfg_id, mfrm_id, wbg_id, mfg_def_perm, wbp_id)VALUES(?, ?, ?, ?, ?)");
            while(rs.next())
            {
                stmt.setInt(1, conn.getDBSequence("SEQ_MFG_ID").getNextValue());
                stmt.setInt(2, rs.getInt(1));
                stmt.setInt(3, NEWwbg_id);
                stmt.setString(4, rs.getString(2));
                stmt.setInt(5, rs.getInt(3));
                stmt.addBatch();
            }
            stmt.executeBatch();

            //insert into maintenance_form_element_prmsn
            stmt = conn.prepareStatement("select mfrm_id, mfep_element_name, mfep_permission_flag from maintenance_form_element_prmsn where wbg_id = ? and mfrm_id not in (select mfrm_id from maintenance_form_element_prmsn where wbg_id = ?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            stmt = conn.prepareStatement("insert into maintenance_form_element_prmsn (mfep_id, mfrm_id, wbg_id, mfep_element_name, mfep_permission_flag)VALUES(?, ?, ?, ?, ?)");
            while(rs.next())
            {
                stmt.setInt(1, conn.getDBSequence("SEQ_MFEP_ID").getNextValue());
                stmt.setInt(2, rs.getInt(1));
                stmt.setInt(3, NEWwbg_id);
                stmt.setString(4, rs.getString(2));
                stmt.setString(5, rs.getString(3));
                stmt.addBatch();
            }
            stmt.executeBatch();

            //insert into override_type_grp
            stmt = conn.prepareStatement("select ovrtyp_id, wbp_id from override_type_grp where wbg_id = ? and ovrtyp_id not in (select ovrtyp_id from override_type_grp where wbg_id = ?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            stmt = conn.prepareStatement("insert into override_type_grp (otg_id, wbg_id, ovrtyp_id, wbp_id)VALUES(?, ?, ?, ?)");
            while(rs.next())
            {
                stmt.setInt(1, conn.getDBSequence("SEQ_OTG_ID").getNextValue());
                stmt.setInt(2, NEWwbg_id);
                stmt.setInt(3, rs.getInt(1));
                stmt.setInt(4, rs.getInt(2));
                stmt.addBatch();
            }
            stmt.executeBatch();

            //insert into pop_up_grp
            stmt = conn.prepareStatement("select pop_id from pop_up_grp where wbg_id = ? and pop_id not in (select pop_id from pop_up_grp where wbg_id = ?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            stmt = conn.prepareStatement("insert into pop_up_grp (popgrp_id, pop_id, wbg_id)VALUES(?, ?, ?)");
            while(rs.next())
            {
                stmt.setInt(1, conn.getDBSequence("SEQ_POPGRP_ID").getNextValue());
                stmt.setInt(2, rs.getInt(1));
                stmt.setInt(3, NEWwbg_id);
                stmt.addBatch();
            }
            stmt.executeBatch();

            //insert into vr_toc_grp
            stmt = conn.prepareStatement("select vrtoc_id, wbp_id from vr_toc_grp where wbg_id = ? and vrtoc_id not in (select vrtoc_id from vr_toc_grp where wbg_id = ?)");
            stmt.setInt(1, OLDwbg_id);
            stmt.setInt(2, NEWwbg_id);
            rs = stmt.executeQuery();
            stmt = conn.prepareStatement("insert into vr_toc_grp (vrtocgrp_id, vrtoc_id, wbg_id, wbp_id)VALUES(?, ?, ?, ?)");
            while(rs.next())
            {
                stmt.setInt(1, conn.getDBSequence("SEQ_VRTOCGRP_ID").getNextValue());
                stmt.setInt(2, rs.getInt(1));
                stmt.setInt(3, NEWwbg_id);
                stmt.setInt(4, rs.getInt(2));
                stmt.addBatch();
            }
            stmt.executeBatch();

            conn.commit();
        }
        catch(SQLException e)
        {
            if(conn != null)
                conn.rollback();
            //logger.warn("Exception while copying security group", e);
            throw e;
        }
        finally
        {
            if(rs != null)
                rs.close();
            if(stmt != null)
                stmt.close();
            //releaseConnection(conn);
        }
    }

    protected void Delete(String group, DBConnection conn) throws SQLException
    {
        PreparedStatement stmt = null;

        try
        {
            stmt = conn.prepareStatement("delete from vr_toc_grp where wbg_id=(select wbg_id from workbrain_group where wbg_name=?)");
            stmt.setString(1, group);
            stmt.execute();
            stmt = conn.prepareStatement("delete from vr_toc_grp where wbg_id=(select wbg_id from workbrain_group where wbg_name=?)");
            stmt.setString(1, group);
            stmt.execute();
            stmt = conn.prepareStatement("delete from pop_up_grp where wbg_id=(select wbg_id from workbrain_group where wbg_name=?)");
            stmt.setString(1, group);
            stmt.execute();
            stmt = conn.prepareStatement("delete from override_type_grp where wbg_id=(select wbg_id from workbrain_group where wbg_name=?)");
            stmt.setString(1, group);
            stmt.execute();
            stmt = conn.prepareStatement("delete from maintenance_form_element_prmsn where wbg_id=(select wbg_id from workbrain_group where wbg_name=?)");
            stmt.setString(1, group);
            stmt.execute();
            stmt = conn.prepareStatement("delete from maintenance_form_grp where wbg_id=(select wbg_id from workbrain_group where wbg_name=?)");
            stmt.setString(1, group);
            stmt.execute();
            stmt = conn.prepareStatement("delete from workbrain_group where wbg_id=(select wbg_id from workbrain_group where wbg_name=?)");
            stmt.setString(1, group);
            stmt.execute();
            conn.commit();

        }
        catch (SQLException e)
        {
            if(conn != null)
                conn.rollback();
                        //logger.warn("Exception while copying security group", e);
                throw e;
        }

        finally
        {
            if(stmt != null)
                stmt.close();
            //releaseConnection(conn);
        }
    }

    public void Overwrite(String OldSecGroup, String NewSecGroup, DBConnection conn) throws SQLException
    {
        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {
            // Test to see if the destination security group actually exists
            // If not, just invoke the copy method to make the new group.
            stmt = conn.prepareStatement("select wbg_id from workbrain_group where wbg_name=?");
            stmt.setString(1, NewSecGroup);
            rs = stmt.executeQuery();
            if(!rs.next())
            {
                Copy(OldSecGroup, NewSecGroup, conn);
            }
            else
            {
                // Cache the wbg_id's
                int OLDwbg_id;
                int NEWwbg_id;

                conn.setAutoCommit(false);

                stmt = conn.prepareStatement("select wbg_id, wbg_lockdown, wbg_flag1 from workbrain_group where wbg_name = ?");
                stmt.setString(1, OldSecGroup.toUpperCase());

                rs = stmt.executeQuery();
                rs.next();

                OLDwbg_id = rs.getInt(1);

                stmt.setString(1, NewSecGroup.toUpperCase());

                rs = stmt.executeQuery();
                rs.next();

                NEWwbg_id = rs.getInt(1);

                // Delete the destination's security configuration settings
                stmt = conn.prepareStatement("delete from vr_toc_grp where wbg_id=?");
                stmt.setInt(1, NEWwbg_id);
                stmt.execute();
                stmt = conn.prepareStatement("delete from pop_up_grp where wbg_id=?");
                stmt.setInt(1, NEWwbg_id);
                stmt.execute();
                stmt = conn.prepareStatement("delete from override_type_grp where wbg_id=?");
                stmt.setInt(1, NEWwbg_id);
                stmt.execute();
                stmt = conn.prepareStatement("delete from maintenance_form_element_prmsn where wbg_id=?");
                stmt.setInt(1, NEWwbg_id);
                stmt.execute();
                stmt = conn.prepareStatement("delete from maintenance_form_grp where wbg_id=?");
                stmt.setInt(1, NEWwbg_id);
                stmt.execute();
                conn.commit();


                conn.setAutoCommit(false);

                //insert into maintenance_form_grp
                stmt = conn.prepareStatement("select mfrm_id, mfg_def_perm, wbp_id from maintenance_form_grp where wbg_id = ? and mfrm_id not in (select mfrm_id from maintenance_form_grp where wbg_id = ?)");
                stmt.setInt(1, OLDwbg_id);
                stmt.setInt(2, NEWwbg_id);
                rs = stmt.executeQuery();
                stmt = conn.prepareStatement("insert into maintenance_form_grp(mfg_id, mfrm_id, wbg_id, mfg_def_perm, wbp_id)VALUES(?, ?, ?, ?, ?)");
                while(rs.next())
                {
                    stmt.setInt(1, conn.getDBSequence("SEQ_MFG_ID").getNextValue());
                    stmt.setInt(2, rs.getInt(1));
                    stmt.setInt(3, NEWwbg_id);
                    stmt.setString(4, rs.getString(2));
                    stmt.setInt(5, rs.getInt(3));
                    stmt.addBatch();
                }
                stmt.executeBatch();

                //insert into maintenance_form_element_prmsn
                stmt = conn.prepareStatement("select mfrm_id, mfep_element_name, mfep_permission_flag from maintenance_form_element_prmsn where wbg_id = ? and mfrm_id not in (select mfrm_id from maintenance_form_element_prmsn where wbg_id = ?)");
                stmt.setInt(1, OLDwbg_id);
                stmt.setInt(2, NEWwbg_id);
                rs = stmt.executeQuery();
                stmt = conn.prepareStatement("insert into maintenance_form_element_prmsn (mfep_id, mfrm_id, wbg_id, mfep_element_name, mfep_permission_flag)VALUES(?, ?, ?, ?, ?)");
                while(rs.next())
                {
                    stmt.setInt(1, conn.getDBSequence("SEQ_MFEP_ID").getNextValue());
                    stmt.setInt(2, rs.getInt(1));
                    stmt.setInt(3, NEWwbg_id);
                    stmt.setString(4, rs.getString(2));
                    stmt.setString(5, rs.getString(3));
                    stmt.addBatch();
                }
                stmt.executeBatch();

                //insert into override_type_grp
                stmt = conn.prepareStatement("select ovrtyp_id, wbp_id from override_type_grp where wbg_id = ? and ovrtyp_id not in (select ovrtyp_id from override_type_grp where wbg_id = ?)");
                stmt.setInt(1, OLDwbg_id);
                stmt.setInt(2, NEWwbg_id);
                rs = stmt.executeQuery();
                stmt = conn.prepareStatement("insert into override_type_grp (otg_id, wbg_id, ovrtyp_id, wbp_id)VALUES(?, ?, ?, ?)");
                while(rs.next())
                {
                    stmt.setInt(1, conn.getDBSequence("SEQ_OTG_ID").getNextValue());
                    stmt.setInt(2, NEWwbg_id);
                    stmt.setInt(3, rs.getInt(1));
                    stmt.setInt(4, rs.getInt(2));
                    stmt.addBatch();
                }
                stmt.executeBatch();

                //insert into pop_up_grp
                stmt = conn.prepareStatement("select pop_id from pop_up_grp where wbg_id = ? and pop_id not in (select pop_id from pop_up_grp where wbg_id = ?)");
                stmt.setInt(1, OLDwbg_id);
                stmt.setInt(2, NEWwbg_id);
                rs = stmt.executeQuery();
                stmt = conn.prepareStatement("insert into pop_up_grp (popgrp_id, pop_id, wbg_id)VALUES(?, ?, ?)");
                while(rs.next())
                {
                    stmt.setInt(1, conn.getDBSequence("SEQ_POPGRP_ID").getNextValue());
                    stmt.setInt(2, rs.getInt(1));
                    stmt.setInt(3, NEWwbg_id);
                    stmt.addBatch();
                }
                stmt.executeBatch();

                //insert into vr_toc_grp
                stmt = conn.prepareStatement("select vrtoc_id, wbp_id from vr_toc_grp where wbg_id = ? and vrtoc_id not in (select vrtoc_id from vr_toc_grp where wbg_id = ?)");
                stmt.setInt(1, OLDwbg_id);
                stmt.setInt(2, NEWwbg_id);
                rs = stmt.executeQuery();
                stmt = conn.prepareStatement("insert into vr_toc_grp (vrtocgrp_id, vrtoc_id, wbg_id, wbp_id)VALUES(?, ?, ?, ?)");
                while(rs.next())
                {
                    stmt.setInt(1, conn.getDBSequence("SEQ_VRTOCGRP_ID").getNextValue());
                    stmt.setInt(2, rs.getInt(1));
                    stmt.setInt(3, NEWwbg_id);
                    stmt.setInt(4, rs.getInt(2));
                    stmt.addBatch();
                }
                stmt.executeBatch();

                conn.commit();
            }
        }
        catch(SQLException e)
        {
            if(conn != null)
                conn.rollback();
            //logger.warn("Exception while copying security group", e);
            throw e;
        }
        finally
        {
            if(rs != null)
                rs.close();
            if(stmt != null)
                stmt.close();
            //releaseConnection(conn);
        }
    }


}



