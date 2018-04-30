package com.wbiag.server.wbiagprocess;

import java.io.*;
import java.sql.*;

import com.workbrain.sql.*;
/**
 * Creates Release BNotes file based on WBIAG_SOLUTION itesm with status WIS_STATUS_COMPLETE and WIS_STATUS_RELEASED_TO_TESTING
 */
public class CreateReleaseNotesFile  {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(CreateReleaseNotesFile.class);

    private static final String SOLUTION_SELECT_SQL =
        "SELECT wis_tt_number, wis_tt_summary FROM wbiag_solution"
        + " WHERE wis_status in (?,?) ORDER BY wis_tt_number";
    private static final String HEADER = "This file lists all IAG solution TestTrack items. \n"
        + "- See http://wbiag for functional and technical specifications of each item. \n"
        + "- Check the related item's technical document for applicable versions. \n"
        + "- Items in COMPLETE and RELEASED_TO_TESTING status are released. "
        + " RELEASED_TO_TESTING are unit tested but have not been verified yet"
        + " and such items can be used by project teams based on their decision. \n"
        + "\n";

    public boolean execute(DBConnection conn, String filePath) throws Exception {
        PrintWriter pw = new PrintWriter(new FileOutputStream(filePath));
        pw.println(HEADER);

        PreparedStatement ps = null;
        ResultSet rs = null;
        int cnt = 1;
        try {
            ps = conn.prepareStatement(SOLUTION_SELECT_SQL);
            ps.setString(1, WbiagSolutionData.WIS_STATUS_COMPLETE);
            ps.setString(2, WbiagSolutionData.WIS_STATUS_RELEASED_TO_TESTING);
            rs = ps.executeQuery();
            while (rs.next()) {
                String line = rs.getString(1) + "\t" +  rs.getString(2);
                pw.println(line);
                cnt ++;
            }
        }
        finally {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }

        pw.println("\nTotal : (" + cnt + ")");
        pw.close();
        return true;
    }


    public static void main( String[] args ) throws Exception {
        System.setProperty("junit.db.username" , "workbrain");
        System.setProperty("junit.db.password" , "workbrain");
        System.setProperty("junit.db.url" , "jdbc:oracle:oci8:@IAG01SIT");
        System.setProperty("junit.db.driver" , "oracle.jdbc.driver.OracleDriver");


        CreateReleaseNotesFile dst = new CreateReleaseNotesFile();
        dst.execute(SQLHelper.connectTo() , "\\\\Toriag\\\\IAGBuild\\\\IAGReleaseNotes.txt");
    }

}



