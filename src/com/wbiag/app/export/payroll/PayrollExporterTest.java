package com.wbiag.app.export.payroll;

import java.util.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.workbrain.app.ta.ruleengine.*;
import com.workbrain.sql.*;
import com.workbrain.tool.overrides.InsertEmployeeOverride;
import com.workbrain.tool.overrides.OverrideBuilder;
import com.workbrain.util.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;

import junit.framework.*;

import com.wbiag.app.export.payroll.PayrollExporter;

/**
 * Unit test for PayrollExportProcessorWhere
 *
 */
public class PayrollExporterTest
    extends RuleTestCase {
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.
        getLogger(PayrollExporterTest.class);

    public PayrollExporterTest(String testName) throws Exception {
        super(testName);
    }

    public static TestSuite suite() {
        TestSuite result = new TestSuite();
        result.addTestSuite(PayrollExporterTest.class);
        return result;
    }
/*
    public void testOnBoardRetroRegExport()  throws Exception {
        DBConnection conn = this.getConnection();
        
        PayrollExporter payExpOB = new PayrollExporter(conn, null);
        payExpOB.setEmpId(30895);
        payExpOB.setCycle(PayrollExporter.CYCLE_ON_BOARD);
        payExpOB.setPayExpTsk(1);
        
        payExpOB.useDefaultDates();
        
        
        payExpOB.setWriteToFile(true);
        payExpOB.setWriteToTable(false);
        payExpOB.setAdjustDates(false);
        payExpOB.setMergeFiles(false);
        payExpOB.setDoExport(true);
        try {
            payExpOB.process();
        }
        catch (Exception e) {
            System.out.println(e);
            throw e;
        }
        
        
        PayrollExporter payExp = new PayrollExporter(conn, null);
        payExp.setEmpId(30895);
        payExp.setCycle(PayrollExporter.CYCLE_REGULAR);
        payExp.setPayExpTsk(1);
        payExp.useDefaultDates();
        
        
        payExp.setWriteToFile(true);
        payExp.setWriteToTable(false);
        payExp.setAdjustDates(false);
        payExp.setMergeFiles(false);
        payExp.setDoExport(true);
        try {
            payExp.process();
        }
        catch (Exception e) {
            System.out.println(e);
            throw e;
        }
        
         
        OverrideBuilder ovrBuilder = new OverrideBuilder(conn);
        InsertEmployeeOverride ieo = new InsertEmployeeOverride(conn);
        ieo.setEmpId(30895);
        ieo.setEmpFlag1("N");
        ieo.setOvrComment("JUNIT RESET");
		ieo.setOvrType(OverrideData.EMPLOYEE_OVERRIDE_TYPE);
		ieo.setStartDate(DateHelper.convertStringToDate("4/12/2005 12:00:00", "MM/dd/yyyy hh:mm:ss"));
		ieo.setEndDate(DateHelper.DATE_3000);
		ieo.setWbuNameBoth("WORKBRAIN", "WORKBRAIN");
		ovrBuilder.add(ieo);
		ovrBuilder.execute(true);
    }
    */
    public void testRegularExport() throws Exception {
           
        	DBConnection conn = this.getConnection();
           Date startDate = DateHelper.parseDate("2005-03-20", "yyyy-MM-dd");
           Date endDate = DateHelper.parseDate("2005-03-26", "yyyy-MM-dd");
           int[] payIds = new int[1];
           payIds[0] = 10041;//-99;
           System.out.println("Creating Exporter: " + conn.getName());
           PayrollExporter payExp = new PayrollExporter(conn, PayrollExporter.MODE_REGULAR);
           payExp.setCycle(PayrollExporter.CYCLE_REGULAR);
           payExp.setAllReadyWhere(" paygrp_name = 'JUNIT' ");
           payExp.setPaygrpIds(payIds);
           payExp.setDates(startDate, endDate);

           payExp.setPayExpTsk(1);
           payExp.setWriteToFile(true);
           payExp.setWriteToTable(false);
           payExp.setAdjustDates(false);
           payExp.setMergeFiles(false);
           payExp.setDoExport(true);
           try {
               payExp.process();
           }
           catch (Exception e) {
               System.out.println(e);
           }

       }

/*
    public void testOnBoardExport() throws Exception {
        DBConnection conn = this.getConnection();
        Date startDate = DateHelper.parseDate("2004-11-01", "yyyy-MM-dd");
        Date endDate = DateHelper.parseDate("2004-11-14", "yyyy-MM-dd");
        int[] payIds = new int[1];
        payIds[0] = 1;

        PayrollExporter payExp = new PayrollExporter(conn, PayrollExporter.MODE_REGULAR);
        payExp.setPaygrpIds(payIds);
        payExp.setLookBackDays(-14);
        payExp.setCycle(PayrollExporter.CYCLE_ON_BOARD);



        payExp.setPayExpTsk(1);
        payExp.setWriteToFile(true);
        payExp.setWriteToTable(false);
        payExp.setAdjustDates(false);
        payExp.setMergeFiles(false);
        payExp.setDoExport(false);
        try {
            payExp.process();
        }
        catch (Exception e) {
            System.out.println(e);
        }

    }

    public void testTermExport() throws Exception {
            DBConnection conn = this.getConnection();
            Date startDate = DateHelper.parseDate("2004-11-01", "yyyy-MM-dd");
            Date endDate = DateHelper.parseDate("2004-11-14", "yyyy-MM-dd");
            int[] payIds = new int[1];
            payIds[0] = 1;

            PayrollExporter payExp = new PayrollExporter(conn, PayrollExporter.MODE_REGULAR);
            payExp.setPaygrpIds(payIds);
            payExp.setCycle(PayrollExporter.CYCLE_TERM);


            payExp.setPayExpTsk(1);
            payExp.setWriteToFile(true);
            payExp.setWriteToTable(false);
            payExp.setAdjustDates(false);
            payExp.setMergeFiles(false);
            payExp.setDoExport(false);
            try {
                payExp.process();
            }
            catch (Exception e) {
                System.out.println(e);
            }

        }
        */
/* Don't do performance test every night.
    public void testReadinessPerformance() throws Exception {
        long startTime;
        long endTime;
        long durr;
        long avg;

        DBConnection conn = this.getConnection();
        Date startDate = DateHelper.parseDate("2004-11-01", "yyyy-MM-dd");
        Date endDate = DateHelper.parseDate("2004-11-14", "yyyy-MM-dd");

        int[] payIds = new int[1];
        payIds[0] = 1;
        PayrollExporter pexRegular = new PayrollExporter(conn, null);
        pexRegular.setCycle(PayrollExporter.CYCLE_REGULAR);
        pexRegular.setPaygrpIds(payIds);
        pexRegular.setDates(startDate, endDate);

        try {
            avg = 0;
            durr = 0;
            for (int i = 0; i < 10; i++) {

                startTime = System.currentTimeMillis();
                pexRegular.checkUnauth();
                endTime = System.currentTimeMillis();
                durr = (endTime - startTime) / 1000;
                if (avg == 0) {
                    avg = durr;
                }
                else {
                    avg = (avg + durr) / 2;
                }

            }
            System.out.println("Avg Durration of readiness (s): " + avg);
            this.assertTrue(durr < 600);
        }
        catch (Exception e) {
            System.out.println(e);
        }

    }

    public void testExportPerformance() throws Exception  {
        long startTime;
        long endTime;
        long durr;
        long avg;

        DBConnection conn = this.getConnection();
        Date startDate = DateHelper.parseDate("2004-11-01", "yyyy-MM-dd");
        Date endDate = DateHelper.parseDate("2004-11-14", "yyyy-MM-dd");

        int[] payIds = new int[1];
        payIds[0] = 1;
        PayrollExporter pexRegular = new PayrollExporter(conn, null);
        pexRegular.setCycle(PayrollExporter.CYCLE_REGULAR);
        pexRegular.setPaygrpIds(payIds);
        pexRegular.setDates(startDate, endDate);

        pexRegular.setPayExpTsk(1);
        pexRegular.setWriteToFile(true);
        pexRegular.setWriteToTable(false);
        pexRegular.setAdjustDates(false);
        pexRegular.setMergeFiles(false);

        try {
            avg = 0;
            durr = 0;
            for (int i = 0; i < 10; i++) {
                startTime = System.currentTimeMillis();
                pexRegular.process(); ;
                endTime = System.currentTimeMillis();
                durr = (endTime - startTime) / 1000;
                if (avg == 0) {
                    avg = durr;
                }
                else {
                    avg = (avg + durr) / 2;
                }
            }
            System.out.println("Avg Durration of Regular Export (s): " + avg);
            this.assertTrue(durr < 600);
        }
        catch (Exception e) {
            System.out.println(e);
        }

    }
*/
   

    private void createTestDefaultRecords(Date startDate, Date endDate,
                                          int payGroup) {
        DBConnection conn = this.getConnection();
        int[] empIds;
        HashSet empList = new HashSet();
        try {
            String SQL = "SELECT EMP_ID FROM EMPLOYEE WHERE PAYGRP_ID = '" +
                String.valueOf(payGroup) + "1'";
            PreparedStatement ps = conn.prepareStatement(SQL);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                empList.add(rs.getObject(1));
            }

            Iterator it = empList.iterator();
            empIds = new int[empList.size()];
            int index = 0;
            while (it.hasNext()) {
                empIds[index] = Integer.parseInt(it.next().toString());
                index++;
            }

            CreateDefaultRecords cdr = new CreateDefaultRecords(conn, empIds,
                startDate, endDate);
            System.out.println("Creating Records for " + empIds.length +
                               " Employees");
            cdr.execute();
            System.out.println("DONE");
        }
        catch (Exception e) {
            System.out.println("ERROR");
        }

    }
   

    public static void main(String[] args) throws Exception {
        junit.textui.TestRunner.run(suite());
    }

}
