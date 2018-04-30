package com.wbiag.app.modules.reports.hcprintschedule;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.server.sql.ConnectionManager;
import com.workbrain.security.SecurityService;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;
import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfWriter;

public class PrintScheduleServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(PrintScheduleServlet.class);


    /**
     * The doGet method of the servlet. <br>
     *
     * This method calls upon process(ByteArrayOutputStream, HttpServletRequest),
     * to generate the content of the PDF document.
     * It is important to set the response content length to avoid certain
     * problems in IE.
     *
     * @param request the request send by the client to the server
     * @param response the response send by the server to the client
     * @throws ServletException if an error occurred
     * @throws IOException if an error occurred
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        //System.out.println("\n\n inside doGet....\n\n\n");
            try {
                process(request, response);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    /**
     * The doPost method of the servlet. <br>
     *
     * Calls doGet(HttpServletRequest, HttpServletResponse)
     *
     * @param request the request send by the client to the server
     * @param response the response send by the server to the client
     * @throws ServletException if an error occurred
     * @throws IOException if an error occurred
     */
    public void doPost(HttpServletRequest request,HttpServletResponse response)
        throws ServletException, IOException {
        //System.out.println("\n\n inside doPost....\n\n\n");
        try {
            process(request,response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void process(HttpServletRequest request, HttpServletResponse response)
    {
        DBConnection conn = null;
        //System.out.println("\n\n inside process....\n\n\n");
        try {

         // set the current user in the security service
           SecurityService.setCurrentUser(request.getSession());

         // create database connection
            conn = new DBConnection(ConnectionManager.getConnection());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document reportPDFDocument = (Document)generateReport(request,conn,baos);
            PdfWriter.getInstance(reportPDFDocument, baos);

            // setting some response headers
			response.setHeader("Expires", "0");
			response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
			response.setHeader("Pragma", "public");
			// setting the content type
			response.setContentType("application/pdf");
			// the contentlength is needed for MSIE!!!
			response.setContentLength(baos.size());

            /*String today = DateHelper.convertDateString(new Date(), "MM-dd-yyyy_HHmm");
            response.setHeader(
                "Content-disposition",
                "inline; filename=PrintedSchedule_" + today +".pdf" );
            */
			// write ByteArrayOutputStream to the ServletOutputStream
			ServletOutputStream out = response.getOutputStream();
			baos.writeTo(out);
			out.flush();
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        } finally {

            // clean up database connection
            try {
                if (conn != null) {
                    conn.commit();
                }
                SQLHelper.cleanUp(conn);//Each thread should close its db connection

            } catch (Exception e) {
                logger.error(e.getMessage(),e);
            }
        }

    }

	public Object generateReport(HttpServletRequest request, DBConnection conn , ByteArrayOutputStream baos) throws SQLException{
		logger.debug("Inside  generateReport");
		String 	teams_p =  request.getParameter("WBT_IDS_0");
		String dayparts_p = request.getParameter("DAYPART_IDS_0");
		String daypartsSortDir_p = request.getParameter("DAYPART_SORT_DIR_0");
		String jobs_p = request.getParameter("JOB_IDS_0");
		String jobsSortDir_p = request.getParameter("JOB_SORT_DIR_0");
		String emps_p = request.getParameter("EMP_IDS_0");
		//String reportOutputFormat_p = "PDF";
		//HUB 36227 shlee: fix when start and end dates are empty, use current team pay period
		Date startDate_p = null;
		Date endDate_p = null;
		if(!StringHelper.isEmpty(request.getParameter("START_DATE_0")) &&
				!StringHelper.isEmpty(request.getParameter("END_DATE_0"))){
			startDate_p = DateHelper.convertStringToDate(request.getParameter("START_DATE_0"), "yyyyMMdd HHmmss");
			endDate_p = DateHelper.convertStringToDate(request.getParameter("END_DATE_0"), "yyyyMMdd HHmmss");
		}
		
		// MR - Fix of using the show totals parameter, default to true
		boolean showTotals = true;
		String totals = request.getParameter("TOTALS");
		if(!StringHelper.isEmpty(totals) && 
			totals.equalsIgnoreCase(Boolean.toString(false))) {
			showTotals = false;
		}

		logger.debug("Received: " + teams_p + " " + startDate_p + " " + endDate_p + " " + dayparts_p
				+ " " + daypartsSortDir_p + " " + jobs_p + " " + jobsSortDir_p + " " + emps_p);

        PrintingScheduleModel model = new PrintingScheduleModel(conn);
		PrintingScheduleView view = new PrintingScheduleViewPDF();

		//set all the model's params
		model.setTeams(teams_p);
		model.setDayparts(dayparts_p);
		model.setDaypartsSortDir(daypartsSortDir_p);
		model.setJobs(jobs_p);
		model.setJobsSortDir(jobsSortDir_p);
		model.setEmps(emps_p);
		model.setStartDate(startDate_p);
		model.setEndDate(endDate_p);

		logger.debug("Retrieving model");
		//retieve the data from the model
		List modelData = model.retrieveModelData();

		logger.debug("Retrieving report pdf");
		//retrieve the report from the view (given the data)
		Object generatedReport = view.retrieveReport(modelData, baos, showTotals);

		return generatedReport;
	}

}
