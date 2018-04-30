package com.wbiag.app.modules.reports.hcprintschedule;

import com.lowagie.text.FontFactory;
import com.lowagie.text.Document;
import com.lowagie.text.Phrase;
import com.lowagie.text.PageSize;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import com.workbrain.util.DateHelper;

import java.io.*;
import java.awt.Color;
import java.awt.Font;
import java.util.*;
import java.text.*;
import java.text.DecimalFormat;

import com.workbrain.security.SecurityService;

/**
 * @author          Ali Ajellu
 * @version         1.0
 * Date:            Friday, June 16 206
 * Copyright:       Workbrain Corp.
 * TestTrack:       1630
 *
 * The View in the report's MVC Model. See class header comments in PrintingScheduleController.java
 * for more information.
 *
 * PrintingScheduleViewPDF creates a visual representation of the model in the PDF format. The main
 * method to call when a report is required is retrieveReport(model, outputStream) where model is a model
 * that is structure the way PrintingScheduleModel is defined and outputStream is where the report is
 * written to.
 *
 * No other variables need to be set beforehand.
 */
public class PrintingScheduleViewPDF extends PdfPageEventHelper implements PrintingScheduleView{
    private static org.apache.log4j.Logger logger =
        org.apache.log4j.Logger.getLogger(PrintingScheduleViewPDF.class);
    //Neshan Apr 03, 2007
	//private com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.BOLD, Color.BLACK);
	//private com.lowagie.text.Font regFont = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.PLAIN, Color.BLACK);
	private com.lowagie.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.BOLD, Color.BLACK);
	private com.lowagie.text.Font boldFont2 = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD, Color.BLACK);
	private com.lowagie.text.Font regFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.PLAIN, Color.BLACK);
	private com.lowagie.text.Font regFont2 = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.PLAIN, Color.BLACK);
	private com.lowagie.text.Font spacerFont = FontFactory.getFont(FontFactory.HELVETICA, 1, Font.PLAIN, Color.BLACK);
	private com.lowagie.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.PLAIN, Color.BLACK);
	private com.lowagie.text.Font titleFont2 = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.PLAIN, Color.BLACK);
	private com.lowagie.text.Font titleFont3 = FontFactory.getFont(FontFactory.HELVETICA, 18, Font.PLAIN, Color.BLACK);
	
    //Neshan Apr 03, 2007
	//private int headerHeight = 50;
	//private int leftMargin = 40;
	//private int rightMargin = 40;
	private int headerHeight = 90;
	private int leftMargin = 10;
	private int rightMargin = 10;
	private int topMargin = 45 + 50 + headerHeight; 
	private int bottomMargin = 50;

	private int empNameCellWidth = 50;
	private float pageWidth;
	private float dayUnitCellWidth;
	private static int MAX_DAYS = 42;

	private PdfPTable header = new PdfPTable(1);
	private PdfPTable colHeaders= new PdfPTable(1);

	private String reportTitle = "Nursing Unit Schedule";
	public String[] monthMap = {"January", "February", "March", "April", "May", "June", "July", "August",
								"September", "October", "November", "December"};
	public String[] weekMap  = {"S", "M", "T", "W", "TH", "F", "SA"};

	private int[][] staffCoverage = new int[4][];

    //the fe prefix stands for Floating Employee
	private float feTableWidthPercentage = 35;
	private PdfPCell feEmpName;
	private PdfPCell feJob;
	private PdfPCell feSendingTeam;
	private PdfPCell feReceivingTeam;
	private PdfPCell feDate;
	private PdfPCell feDetails;

	private Color weekendCellColour = new Color(39, 207, 216);
	private Color mainTableHeaderColour = Color.ORANGE;
	private Color coverageTableColour = new Color(128,255,0);
	private Color jobCellColur = Color.LIGHT_GRAY;

    //the sr prefix stands for Staff Requirements
	private DecimalFormat srDecimalFormat = new DecimalFormat("#####.##");
    //HUB:42198  TT:2964
	private static final String DATE_FORMAT = "MMMMM dd, yyyy hh:mm a";

//	/**
//	 * Main mehtod is for testing purposes only.
//	 */
//	public static void main(String[] args) {
//		PrintingScheduleView view = new PrintingScheduleViewPDF();
//
//		List floatingEmps1 = new ArrayList();
//
//		/** MODEL **/
//		List model = new ArrayList();
//		Team team1 = new Team(10041, "VUH 10N", new Date(), DateHelper.addDays(new Date(),41));
//		Team team2 = new Team(10042, "VUH 9N", DateHelper.addDays(new Date(), 0), DateHelper.addDays(new Date(), 8));
//
//		Daypart daypart1 = new Daypart(10041, "12 HR DAY", DateHelper.convertStringToDate("9:00", "HH:mm"),
//								DateHelper.convertStringToDate("17:00", "HH:mm"));
//
//		Daypart daypart2 = new Daypart(10042, "12 HR NGT", DateHelper.convertStringToDate("17:00", "HH:mm"),
//				DateHelper.convertStringToDate("9:00", "HH:mm"));
//
//		Job job1 = new Job(10001, "RN");
//		Job job2 = new Job(10001, "RN");
//
//		Employee emp1 = new Employee("Ajellu, Ali", "0.9");
//		Employee emp2 = new Employee("Haniff, Rabiya", "0.9");
//
//		Schedule sched1 = new Schedule(new Date(), "Ajellu, Ali", "RN", "900", "1000", 60, "WRK", "VUH 9N", "VUH 10N");
//		Schedule sched2 = new Schedule(DateHelper.addDays(new Date(),0), "Ajellu, Ali", "RN", "1000", "1100", 60, "WRK", "VUH 10N", "VUH 10N");
//		Schedule sched3 = new Schedule(DateHelper.addDays(new Date(),0), "Haniff, Rabiya", "RN", "900", "1100", 120, "WRK", "VUH 10N", "VUH 10N");
////		Schedule sched4 = new Schedule(DateHelper.addDays(new Date(),1), "Haniff, Rabiya", "RN", "1000", "1100", "WRK", "VUH 10N", "VUH 10N");
//
//		/** CONSTRUCT MODEL **/
//		emp1.addSchedule(sched1);
//		emp1.addSchedule(sched2);
//
//		emp2.addSchedule(sched3);
//
//		job1.addEmployee(emp1);
//		job1.addEmployee(emp2);
//		job1.staffRequired = new int[DateHelper.getAbsDifferenceInDays(team1.startDate, team1.endDate)+1];
////		job2.addEmployee(emp2);
//		job2.staffRequired = new int[DateHelper.getAbsDifferenceInDays(team1.startDate, team1.endDate)+1];
//		for (int i=0;i<job1.staffRequired.length;i++){
//			job1.staffRequired[i] = i;
//			job2.staffRequired[i] = i+job1.staffRequired.length;
//		}
//
////		floatingEmps1.add(sched2);
//
////		daypart1.addJob(job1);
////		daypart1.floatingSchedules = floatingEmps1;
//
//		daypart1.addJob(job1);
//		daypart1.addJob(job2);
////		daypart2.addJob(job2);
//
//		team1.addDaypart(daypart1);
////		team1.addDaypart(daypart2);
////		team2.addDaypart(daypart2);
////		team2.addDaypart(daypart2);
//
//		model.add(team1);
////		model.add(team2);
//
//		try{
//		view.retrieveReport(model, new FileOutputStream("C:\\itextTest.pdf"));
//		}catch(FileNotFoundException fnfe){
//			fnfe.printStackTrace();
//		}
//	}

    /**
     * Default constrcutor.
     */
	public PrintingScheduleViewPDF(){
		Phrase tempPhrase;

		tempPhrase = new Phrase("Employee Name", boldFont);
		feEmpName = new PdfPCell(tempPhrase);
		tempPhrase = new Phrase("Job", boldFont);
		feJob = new PdfPCell(tempPhrase);
		tempPhrase = new Phrase("Sending Team", boldFont);
		feSendingTeam = new PdfPCell(tempPhrase);
		tempPhrase = new Phrase("Receiving Team", boldFont);
		feReceivingTeam = new PdfPCell(tempPhrase);
		tempPhrase = new Phrase("Date", boldFont);
		feDate = new PdfPCell(tempPhrase);
		tempPhrase = new Phrase("Details", boldFont);
		feDetails = new PdfPCell(tempPhrase);
	}

    /**
     * The main method that has to be called by the Controller.
     *
     * @param modelData A model structure as it is defined in PrintingScheduleModel.
     * @param outputStream The output that the report should be written to
     * @return An iText Document object containing the generated report.
     */
	    public Document retrieveReport(java.util.List modelData, OutputStream outputStream ){
		   return null;	
		}
	    //HUB:42198  TT:2964
		public Document retrieveReport(java.util.List modelData, OutputStream outputStream, boolean showTotal){       
        Document doc = new Document(PageSize.LEGAL.rotate(), leftMargin, rightMargin, topMargin, bottomMargin);
        pageWidth = doc.getPageSize().width();
        dayUnitCellWidth = (pageWidth - leftMargin - rightMargin - (empNameCellWidth+5))/MAX_DAYS;
        log("Regular Cell Width: " + dayUnitCellWidth);

        Phrase currJobPhrase;
        Phrase currEmpPhrase;
        Phrase currSchedulePhrase;
        Phrase currStaffCoveragePhrase;
        PdfPCell currJobCell;
        PdfPCell currEmpCell;
        PdfPCell currScheduleCell;
        PdfPCell currStaffCOverageCell;

        int currNumOfDaysInReportRange = -1;
        PdfPTable mainTable = new PdfPTable(1);
        PdfPTable floatingEmployeeTable = new PdfPTable(6);
        PdfPCell currSchedulesCell;
        PdfPTable schedulesTable = new PdfPTable(1);

        try{

            PdfWriter writer = PdfWriter.getInstance(doc, outputStream);
            writer.setPageEvent(this);

            doc.open();


            Iterator teamsIter = modelData.iterator();
            /************************************************** TEAMS ***********************************************/
            while(teamsIter.hasNext()){
                Team currTeam = (Team)teamsIter.next();
                //add 1 because dates are inclusive
                currNumOfDaysInReportRange = DateHelper.getAbsDifferenceInDays(currTeam.startDate, currTeam.endDate)+1;

                colHeaders = createColHeadersTable(currTeam.startDate, currTeam.endDate);

                //team has some dayparts
                if (currTeam.dayparts != null && currTeam.dayparts.size() != 0){
                    Iterator daypartsIter = currTeam.dayparts.iterator();
                    /************************************************** DAY PARTS ***********************************************/
                    while(daypartsIter.hasNext()){
                        Daypart currDaypart = (Daypart)daypartsIter.next();

                        //team has some jobs
                        if (currDaypart.jobs != null && currDaypart.jobs.size() != 0){

                            float currDaypartMins = calcDaypartsMins(currDaypart);

                            String currJobNames = createCSStrFromList(currDaypart.jobs);
                            Iterator jobsIter = currDaypart.jobs.iterator();

                            //since day part has changed, must render header again.
                            header = createHeaderTable( reportTitle,
                                    currTeam.startDate,
                                    currTeam.endDate,
                                    currTeam.name,
                                    currDaypart.name,
                                    currJobNames);

                            //add 1 for the employee name col
                            float mainTableTotalWidthPerc = (empNameCellWidth+(dayUnitCellWidth*currNumOfDaysInReportRange))/(pageWidth-leftMargin-rightMargin)*100;
                            mainTable = new PdfPTable(currNumOfDaysInReportRange+1);
                            //can't use mainTable.setTotalWidth(...) for some reason. Yet another bug in iText.
                            //must calculate and set the width as a percentage of the page size.
                            mainTable.setWidthPercentage(mainTableTotalWidthPerc);
                            mainTable.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
                            mainTable.setWidths(createWidthsArray(this.empNameCellWidth, currNumOfDaysInReportRange));

                            /************************************************** JOBS ***********************************************/
                            while(jobsIter.hasNext()){
                                Job currJob = (Job)jobsIter.next();

                                currJobPhrase = new Phrase(currJob.name+"(s)", this.boldFont);
                                currJobCell = new PdfPCell(currJobPhrase);
                                currJobCell.setBackgroundColor(jobCellColur);
                                //add 1 for the employee name col
                                currJobCell.setColspan(currNumOfDaysInReportRange+1);
                                mainTable.addCell(currJobCell);

                                Iterator empsIter = currJob.employees.iterator();

                                //the first dim of staffCoverage represens the four rows of the Staff Coverage table (that must
                                //be printed at the end of every job)
                                staffCoverage[0] = new int[currNumOfDaysInReportRange];
                                staffCoverage[1] = new int[currNumOfDaysInReportRange];
                                staffCoverage[2] = new int[currNumOfDaysInReportRange];
                                staffCoverage[3] = new int[currNumOfDaysInReportRange];

                                /************************************************** EMPLOYEES ***********************************************/
                                while(empsIter.hasNext()){
                                    Employee currEmp = (Employee)empsIter.next();
                                    if(currEmp.getHomeTeam() != currTeam.id ){
                                    	// Hub 36230: shlee add (F) for a floating employee
                                    	currEmpPhrase = new Phrase(currEmp.name+ " (F)" + "("+currEmp.fteValue+")", this.regFont);
                                    }
                                    else{
                                    	currEmpPhrase = new Phrase(currEmp.name+"("+currEmp.fteValue+")", this.regFont);
                                    }
                                    currEmpCell = new PdfPCell(currEmpPhrase);
                                    mainTable.addCell(currEmpCell);

                                    Date currDate = (Date)currTeam.startDate.clone();
                                    int currDateIndex = 0;
                                    /************************************************** REPORT DATES ***********************************************/
                                    while(!currDate.after(currTeam.endDate)){

                                        boolean empDateHasOcn = false;
                                        schedulesTable = new PdfPTable(1);
                                        List currDatesSchedules = currEmp.getSchedules(currDate);

                                        if (currDatesSchedules != null){
                                            Iterator currDatesSchedulesIter = currDatesSchedules.iterator();

                                            int totalHoursWorked =0;
                                            /************************************************** REPORT DATE SCHEDULES ***********************************************/
                                            while(currDatesSchedulesIter.hasNext()){
                                                Schedule currSchedule = (Schedule)currDatesSchedulesIter.next();

                                                //floating employee.
                                                if(!currTeam.name.equals(currSchedule.team)){
                                                    currSchedulePhrase = new Phrase("F", this.regFont);
                                                //working employee.
                                                }else if("WRK".equals(currSchedule.timecode)){
                                                    //sum total hours worked today for staff coverage calc
                                                    totalHoursWorked += currSchedule.mins;
                                                    currSchedulePhrase = new Phrase(currSchedule.startTime+"\n"+currSchedule.endTime, this.regFont);
                                                //On Call employee.
                                                }else if("OCN".equals(currSchedule.timecode)){
                                                    empDateHasOcn = true;
                                                    currSchedulePhrase = new Phrase("OC", this.regFont);
                                                //non-floating, non-working, non-OnCall employee
                                                }else{
                                                    currSchedulePhrase = new Phrase(currSchedule.timecode, this.regFont);
                                                }

                                                currScheduleCell = new PdfPCell(currSchedulePhrase);
                                                currScheduleCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                                                //if it's a weekend, set the cell's colour
                                                if (DateHelper.dayOfWeek(currDate) == 0 || DateHelper.dayOfWeek(currDate) == 6){
                                                    currScheduleCell.setBackgroundColor(weekendCellColour);
                                                }
                                                schedulesTable.addCell(currScheduleCell);
                                            }
                                            staffCoverage[0][currDateIndex] += totalHoursWorked;

                                        //there are no schedules for today
                                        }else{
                                            //add empty cell
                                            currSchedulePhrase = new Phrase(" ", this.regFont);
                                            currScheduleCell = new PdfPCell(currSchedulePhrase);
                                            if (DateHelper.dayOfWeek(currDate) == 0 || DateHelper.dayOfWeek(currDate) == 6){
                                                currScheduleCell.setBackgroundColor(weekendCellColour);
                                            }
                                            schedulesTable.addCell(currScheduleCell);
                                        }

                                        if (empDateHasOcn){
                                            staffCoverage[3][currDateIndex] += 1;
                                        }

                                        currSchedulesCell = new PdfPCell(schedulesTable);
                                        mainTable.addCell(currSchedulesCell);

                                        currDate = DateHelper.addDays(currDate, 1);
                                        currDateIndex++;
                                    }
                                }

                                staffCoverage[1] = currJob.staffRequired;

                                /************************************************** STAFF COVERAGE TABLE ***********************************************/
                                //When rendering a job is over, we have to create the Staff Coverage mini report
                                //HUB:42198  TT:2964 This section will be shown based on the user selection
                                if( showTotal ) {
                                    for (int i=0;i<staffCoverage.length;i++){     

                                       switch (i){
                                          case 0: currStaffCoveragePhrase = new Phrase(currJob.name+"'s", this.regFont); break;
                                          case 1: currStaffCoveragePhrase = new Phrase(currJob.name+"'s Req", this.regFont);break;
                                          case 2: currStaffCoveragePhrase = new Phrase(currJob.name+" Variance", this.regFont);break;
                                          default: currStaffCoveragePhrase = new Phrase("OC", this.regFont);break;
                                       }
                                     
                                       currStaffCOverageCell = new PdfPCell(currStaffCoveragePhrase);
                                       currStaffCOverageCell.setBackgroundColor(this.coverageTableColour);
                                       mainTable.addCell(currStaffCOverageCell);
                                     
                                       for (int j=0;j<staffCoverage[i].length;j++){     

                                          switch (i){
                                             case 0:             //WRK coverage
                                                 String reqNumber = String.valueOf(srDecimalFormat.format(staffCoverage[0][j]/currDaypartMins));
                                                 currStaffCoveragePhrase = new Phrase(reqNumber, regFont); 
                                                 break;
                                             case 2:            //WRK variance
                                                 String varianceNumber = String.valueOf(srDecimalFormat.format((staffCoverage[0][j]/currDaypartMins)-staffCoverage[1][j]));
                                                 currStaffCoveragePhrase = new Phrase(varianceNumber, regFont); 
                                                 break;
                                             default:          //WRK staff required and OC coverage 
                                                 currStaffCoveragePhrase = new Phrase(String.valueOf(staffCoverage[i][j]), regFont); 
                                                 break;
                                           }
                                         
                                           currStaffCOverageCell = new PdfPCell(currStaffCoveragePhrase);
                                           currStaffCOverageCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
                                           currStaffCOverageCell.setBackgroundColor(this.coverageTableColour);
                                           mainTable.addCell(currStaffCOverageCell);
                                       }
                                    }
                                 }
                             }
                        //team has no jobs
                        }else{
                            header = createHeaderTable( reportTitle,
                                    currTeam.startDate,
                                    currTeam.endDate,
                                    currTeam.name,
                                    currDaypart.name,
                                    "");
                            Phrase jobDoesntExistPhrase = new Phrase("\n\n\n\nNo jobs were selected for team");
                            doc.add(jobDoesntExistPhrase);
                        }

                        /************************************************** FLOATING EMPLOYEES TABLE ***********************************************/
                        //When rendering a daypart is over, we have to create the Floating Employee Listing
                        floatingEmployeeTable = new PdfPTable(6);
                        floatingEmployeeTable.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);
                        floatingEmployeeTable.setWidthPercentage(feTableWidthPercentage);
                        addFloatingEmployeeTableHeaders(floatingEmployeeTable);
                        setFloatingEmployeeTableColWidth(floatingEmployeeTable, pageWidth);

                        Phrase spacerPhrase = new Phrase("\n", regFont);

                        Phrase tempPhrase;
                        PdfPCell tempCell;
                        List floatingSchedules = currDaypart.floatingSchedules;
                        if (floatingSchedules != null && floatingSchedules.size() != 0){
                            for (int feIndex=0;feIndex<floatingSchedules.size();feIndex++){
                                Schedule currSchedule = (Schedule)floatingSchedules.get(feIndex);
                                //emp
                                tempPhrase = new Phrase(currSchedule.empName, regFont);
                                tempCell = new PdfPCell(tempPhrase);
                                tempCell.setBackgroundColor(coverageTableColour);
                                floatingEmployeeTable.addCell(tempCell);

                                //job
                                tempPhrase = new Phrase(currSchedule.job, regFont);
                                tempCell = new PdfPCell(tempPhrase);
                                tempCell.setBackgroundColor(coverageTableColour);
                                floatingEmployeeTable.addCell(tempCell);

                                //sending team
                                tempPhrase = new Phrase(currSchedule.homeTeam, regFont);
                                tempCell = new PdfPCell(tempPhrase);
                                tempCell.setBackgroundColor(coverageTableColour);
                                floatingEmployeeTable.addCell(tempCell);

                                //receiving team
                                tempPhrase = new Phrase(currSchedule.team, regFont);
                                tempCell = new PdfPCell(tempPhrase);
                                tempCell.setBackgroundColor(coverageTableColour);
                                floatingEmployeeTable.addCell(tempCell);

                                //date
                                tempPhrase = new Phrase(DateHelper.convertDateString(currSchedule.workDate, "MM/dd/yyyy"), regFont);
                                tempCell = new PdfPCell(tempPhrase);
                                tempCell.setBackgroundColor(coverageTableColour);
                                floatingEmployeeTable.addCell(tempCell);

                                //Details
                                tempPhrase = new Phrase(currSchedule.startTime + " - " + currSchedule.endTime, regFont);
                                tempCell = new PdfPCell(tempPhrase);
                                tempCell.setBackgroundColor(coverageTableColour);
                                floatingEmployeeTable.addCell(tempCell);
                            }
                        }else{
                            tempPhrase = new Phrase("No floating employees for this daypart", regFont);
                            tempCell = new PdfPCell(tempPhrase);
                            tempCell.setBackgroundColor(this.coverageTableColour);
                            tempCell.setColspan(6);
                            floatingEmployeeTable.addCell(tempCell);
                        }

                        doc.add(mainTable);
                        doc.add(spacerPhrase);
                        doc.add(floatingEmployeeTable);
                        doc.newPage();
                    }
                //team has no day parts
                }else{
                    header = createHeaderTable( reportTitle,
                            currTeam.startDate,
                            currTeam.endDate,
                            currTeam.name,
                            "",
                            "");
                    Phrase daypartDoesntExistPhrase = new Phrase("\n\n\n\nNo day parts were selected for team");
                    doc.add(daypartDoesntExistPhrase);
                    doc.newPage();
                }
            }

            doc.close();


        }catch(DocumentException de){
            logger.error("An error occured while rendering PDF document");
            de.printStackTrace();
        }

        return doc;
    }

    /**
     * At the end of every page, the column headers must rendered again, in case the team's start/end dates have changed.
     *
     * @param startDate The team's start date
     * @param endDate   The team's end date
     * @return A column header table containing the Months, Dates, and Day of week between startDate and endDate
     */
	private PdfPTable createColHeadersTable(Date startDate, Date endDate){
		startDate = new Date(startDate.getTime());
		endDate = new Date(endDate.getTime());


		int numOfDays = DateHelper.getAbsDifferenceInDays(startDate, endDate)+1; //add 1. dates inclusive

		Date tempDate = null;
		PdfPTable headerTable = new PdfPTable(numOfDays);
		headerTable.setTotalWidth(dayUnitCellWidth*numOfDays);
		//insert the month columns

        int spanningIndex = 0;
        int[] spanningMonths = {-1,-1,-1};
        int[] spanningMonthsLengths = {0,0,0};
        tempDate = (Date)startDate.clone();
        while(!tempDate.after(endDate)){
            int currMonth = tempDate.getMonth();

            if (spanningMonths[spanningIndex] == -1){
                spanningMonths[spanningIndex] = currMonth;
                spanningMonthsLengths[spanningIndex]++;

            }else if(spanningMonths[spanningIndex] == currMonth ){
                spanningMonthsLengths[spanningIndex]++;
            }

            if(DateHelper.addDays(tempDate, 1).getMonth() != currMonth){              //new month
                spanningIndex++;
            }

            tempDate = DateHelper.addDays(tempDate, 1);
        }

        spanningIndex = 0;
        for (spanningIndex=0;spanningIndex<spanningMonths.length && spanningMonths[spanningIndex] != -1;spanningIndex++){
            int currMonth = spanningMonths[spanningIndex];
            int currMonthLength = spanningMonthsLengths[spanningIndex];
            Phrase currMonthPhrase = new Phrase(monthMap[currMonth], titleFont2);
            PdfPCell currMonthCell = new PdfPCell(currMonthPhrase);
            currMonthCell.setColspan(currMonthLength);
            currMonthCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
            currMonthCell.setBackgroundColor(mainTableHeaderColour);
            headerTable.addCell(currMonthCell);

        }


		//insert day of week columns
		tempDate = (Date)startDate.clone();
		while(!tempDate.after(endDate)){
			Phrase tempPhrase = new Phrase(String.valueOf(weekMap[DateHelper.dayOfWeek(tempDate)]), titleFont);
			PdfPCell tempCell = new PdfPCell(tempPhrase);
			tempCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
			tempCell.setBackgroundColor(mainTableHeaderColour);
			headerTable.addCell(tempCell);
			tempDate = DateHelper.addDays(tempDate, 1);
		}

		//insert date columns
		tempDate = (Date)startDate.clone();
		while(!tempDate.after(endDate)){
			Phrase tempPhrase = new Phrase(String.valueOf(tempDate.getDate()), titleFont);
			PdfPCell tempCell = new PdfPCell(tempPhrase);
			tempCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
			tempCell.setBackgroundColor(mainTableHeaderColour);
			headerTable.addCell(tempCell);
			tempDate = DateHelper.addDays(tempDate, 1);
		}

		return headerTable;
	}

    /**
     * At the end of every page, the page header must rendered again, because team names, day part names, job names and
     * team start/end dates might have changed.
     *
     * @param title         Report title
     * @param startDate     The page's start date (currently the team's start date)
     * @param endDate       The page's end date (currently the team's end date)
     * @param team          The team name
     * @param daypart       The day part name
     * @param job           The job name(s)
     * @return              A table that sits at the top of the page and contains the information passed to it.
     */
	private PdfPTable createHeaderTable(String title, Date startDate, Date endDate, String team, String daypart, String job){

        PdfPTable headerTable = new PdfPTable(1);
		String startYear = DateHelper.convertDateString(startDate, "yyyy");
		String endYear = DateHelper.convertDateString(endDate, "yyyy");
		String dateRange = monthMap[startDate.getMonth()]+" "+startDate.getDate()+", "+startYear+
		" - " + monthMap[endDate.getMonth()]+" "+endDate.getDate()+", "+endYear;

		headerTable.setWidthPercentage(100);
		headerTable.setHorizontalAlignment(PdfPTable.ALIGN_LEFT);

		Phrase titlePhrase = new Phrase(title, titleFont3);
		PdfPCell titleCell = new PdfPCell(titlePhrase);
		titleCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
		titleCell.setBorderWidth(0);
		
		Phrase dateRangePhrase = new Phrase(dateRange, titleFont2);
		PdfPCell dateRangeCell = new PdfPCell(dateRangePhrase);
		dateRangeCell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
		dateRangeCell.setBorderWidth(0);

		Phrase teamsPhrase = new Phrase("\nTeam(s):      ", boldFont2);
		Phrase teamsContentPhrase = new Phrase (team+"     ", regFont2);
		Phrase daypartPhrase = new Phrase("Day Part(s):     ", boldFont2);
		Phrase daypartContentPhrase = new Phrase(daypart+"     ", regFont2);
		Phrase jobsPhrase = new Phrase("Job(s):     ", boldFont2);
		Phrase jobsContentPhrase = new Phrase (job, regFont2);
		Phrase infoPhrase = new Phrase();
		infoPhrase.add(teamsPhrase);
		infoPhrase.add(teamsContentPhrase);
		infoPhrase.add(daypartPhrase);
		infoPhrase.add(daypartContentPhrase);
		infoPhrase.add(jobsPhrase);
		infoPhrase.add(jobsContentPhrase);
		PdfPCell infoCell = new PdfPCell(infoPhrase);
		infoCell.setBorderWidth(0);

		Phrase spacerPhrase = new Phrase("\n", spacerFont);
		PdfPCell spacerCell = new PdfPCell(spacerPhrase);
		spacerCell.setBorderWidth(0);

		headerTable.addCell(titleCell);
		headerTable.addCell(dateRangeCell);
		headerTable.addCell(infoCell);
		headerTable.addCell(spacerCell);

		return headerTable;
	}

    /**
     * Creates an array with length numOfCols+1 and whose first element = sizeOfFirstColumn.
     * The rest of the elements are filled with dayUnitCellWidth.
     * Used for setting mainTable's column widths.
     *
     * @param sizeOfFirstColumn size of the first column in the width array
     * @param numOfCols         the number of columns (excl. the first)
     * @return                  a widths array
     */
	private float[] createWidthsArray (int sizeOfFirstColumn, int numOfCols){
		//add 1 for the first col
		float[] result = new float[numOfCols+1];

		result[0] = sizeOfFirstColumn;
		for (int i=1;i<=numOfCols;i++){
			result[i] = dayUnitCellWidth;
		}

		return result;
	}


	/**
     * Calculates the duration of a daypart in minutes.
     *
     * @param currDaypart   The daypart whose duration we need
     * @return              Duration of currDaypart in minutes
	 */
	private int calcDaypartsMins(Daypart currDaypart){
		int mins=0;
		Date startTime = currDaypart.startTime;
		Date endTime = currDaypart.endTime;

		//if end < start, add 24 hours to end
		if (DateHelper.compareTimes(endTime, startTime) == -1 ){
			endTime.setHours(endTime.getHours() + 24);
		}

		mins = (int)DateHelper.getTimeBetween(endTime, startTime, true)/60000;


		return mins;
	}

    /**
     * Adds the proper headers to table.
     * @param table The Floating Employee table will be modified to contain the appropriate headers
     */
	private void addFloatingEmployeeTableHeaders(PdfPTable table){
		PdfPCell[] headerNames = {this.feEmpName, this.feJob, this.feSendingTeam, this.feReceivingTeam, this.feDate, this.feDetails};
		for (int i=0;i<headerNames.length;i++){
			PdfPCell tempCell = headerNames[i];
			tempCell.setBackgroundColor(coverageTableColour);
			table.addCell(tempCell);
		}
	}

    /**
     * Self evident method name
     * @param table
     * @param pageWidth
     */
	private void setFloatingEmployeeTableColWidth(PdfPTable table, float pageWidth){
		float[] widths = new float[6];
		widths[0] = this.empNameCellWidth+5;
		float tableWidth = pageWidth*(this.feTableWidthPercentage/100);
		float unitWidths = (tableWidth-(this.empNameCellWidth+5))/5;
		for (int i=1;i<widths.length;i++){
			widths[i] = unitWidths;
		}

		try{
		    table.setWidths(widths);
		}catch(DocumentException de){
			//thrown if number of widths doesn't match num of columns.
            logger.error("An error occured while setting floating employee table's column widths");
			logger.error(de);
		}
	}

    /**
     * We are extending PdfPageEventHelper which allows us to define event handlers. This one
     * is for when the page ends.
     * We add the header (title, date range, team, dayparts and job names), page number, creation date, and column headers
     * to the page once we are done rendering the contents.
     *
     * @param writer        Provided by events caller
     * @param document      Provided by events caller
     */
	public void onEndPage(PdfWriter writer, Document document){
		Rectangle page = document.getPageSize();

		/************** Render HEADER *****************/
		//WARNING: if the height of this table changes, this.headerHeight must be changed accordingly.
		header.setTotalWidth(page.width() - document.leftMargin() - document.rightMargin());
		header.writeSelectedRows(0, -1, document.leftMargin(), page.height() - header.getTotalHeight(),
                writer.getDirectContent());

		/************ Render PAGE NUMBER **************/
		try{
			BaseFont helv = BaseFont.createFont("Helvetica", BaseFont.WINANSI, false);
	        float textBase = document.bottom() - 20;
	        float textCeiling = document.top();
            //HUB:42198  TT:2964
	        //DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
	        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);     
	        
	        String pageNumber = String.valueOf(writer.getPageNumber());
			PdfContentByte cbPageNumber = writer.getDirectContent();
	        cbPageNumber.saveState();
	        cbPageNumber.beginText();
	        cbPageNumber.setFontAndSize(helv, 10);
	        cbPageNumber.setTextMatrix(page.width()/2, textBase);
	        cbPageNumber.showText(pageNumber);
	        cbPageNumber.endText();
	        
	    	/********** Render CREATED DATE and CREATED BY ***************/
            //HUB:42198  TT:2964
	        String currentUser = SecurityService.getUserNameActual();
	        String createdby = "Created by "+ currentUser;
	        PdfContentByte cbCreatedBy = writer.getDirectContent();
	        cbCreatedBy.saveState();
	        cbCreatedBy.beginText();
	        cbCreatedBy.setFontAndSize(helv, 10);
	        cbCreatedBy.setTextMatrix( document.leftMargin(), textCeiling + 100);
	        cbCreatedBy.showText(createdby);
	        cbCreatedBy.endText();
	        
	        String creationDate = "Created on "+ df.format(new Date());
	        //HUB:42198  TT:2964
	        //int maxCreationDateLength = 110;
	        int maxCreationDateLength = 200;
	        PdfContentByte cbCreationDate = writer.getDirectContent();
	        cbCreationDate.saveState();
	        cbCreationDate.beginText();
	        cbCreationDate.setFontAndSize(helv, 10);
	        cbCreationDate.setTextMatrix(page.width()-maxCreationDateLength-document.rightMargin(), textCeiling+100);
	        cbCreationDate.showText(creationDate);
	        cbCreationDate.endText();

		}catch(IOException ioe){
            logger.error("Error occured while creating font using BaseFont.createFont(...)");
		}catch(DocumentException de){
            logger.error("Error occured while creating font using BaseFont.createFont(...)");
        }

	   /******** Render DATE COLUMN HEADERS ***********/
			colHeaders.writeSelectedRows(0, -1, document.leftMargin()+this.empNameCellWidth,
												page.height() - document.topMargin() + colHeaders.getTotalHeight(),
												writer.getDirectContent());
	}

    /**
     * Creates a comma-separated string from a list of Job(s).
     * @param jobs a List Job(s) whose names will be in the return string.
     * @return a comma-separated string from jobs
     */
	private String createCSStrFromList(List jobs){
		String result = "";
		for (int i=0;i<jobs.size();i++){
			if (i==jobs.size()-1){
				result += ((Job)jobs.get(i)).name;
			}else{
				result += ((Job)jobs.get(i)).name+", ";
			}
		}
		return result;
	}

    private void log(String msg) {
        if (logger.isDebugEnabled()) logger.debug(msg);
    }
}