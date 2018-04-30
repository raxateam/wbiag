package com.wbiag.app.export.scheduleout;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.DepartmentAccess;
import com.workbrain.app.ta.db.DocketAccess;
import com.workbrain.app.ta.db.JobAccess;
import com.workbrain.app.ta.model.DepartmentData;
import com.workbrain.app.ta.model.DocketData;
import com.workbrain.app.ta.model.EmployeeData;
import com.workbrain.app.ta.model.EmployeeDefaultLaborData;
import com.workbrain.app.ta.model.JobData;
import com.workbrain.app.ta.ruleengine.CalcSimulationAccess;
import com.workbrain.app.ta.ruleengine.CalcSimulationContext;
import com.workbrain.app.ta.ruleengine.CalcSimulationEmployee;
import com.workbrain.app.ta.ruleengine.EmployeeScheduleDetailData;
import com.workbrain.app.ta.ruleengine.RuleHelper;
import com.workbrain.server.data.AbstractRowCursor;
import com.workbrain.server.data.AbstractRowSource;
import com.workbrain.server.data.AccessException;
import com.workbrain.server.data.BasicRow;
import com.workbrain.server.data.Row;
import com.workbrain.server.data.RowCursor;
import com.workbrain.server.data.RowDefinition;
import com.workbrain.server.data.RowStructure;
import com.workbrain.server.data.type.StringType;
import com.workbrain.sql.DBConnection;
import com.workbrain.util.DateHelper;
import com.workbrain.util.NestedRuntimeException;



public class ScheduleDataSource extends AbstractRowSource
{
	public  static final String PARAM_BRK_CODE_LIST = "brkCodeList";
    
	private RowDefinition rowDefinition;
    private DBConnection conn;
    private ArrayList empIds = new ArrayList();
    CodeMapper cm; 
    private CalcSimulationContext csc; 
    private CalcSimulationAccess csa; 
    private Collection empsScheduleData = null;
    private int dayCounter = 0;
    boolean hasMore = true;
    private CalcSimulationEmployee empSkdData=null;
    private boolean emptyDay =false; //flag to determine if we have a day with schedules that do not
    								 //satisfy criterion.  When person is not paid.
    private String brkCodeList = null;

    private java.util.List rows = new ArrayList();
    
    
    private void initRowSource()
    {
        RowStructure rs = new RowStructure(29);
        rs.add("EMP_NAME",StringType.get());
        rs.add("EMP_LASTNAME",StringType.get());
        rs.add("EMP_FIRSTNAME",StringType.get());
        rs.add("WORK_DATE",StringType.get());
        rs.add("MINUTES_SCHEDULED",StringType.get());
        rs.add("SHIFT_1_START",StringType.get());
        rs.add("SHIFT_1_END",StringType.get());
        rs.add("SHIFT_2_START",StringType.get());
        rs.add("SHIFT_2_END",StringType.get());
        rs.add("SHIFT_3_START",StringType.get());
        rs.add("SHIFT_3_END",StringType.get());
        rs.add("SHIFT_4_START",StringType.get());
        rs.add("SHIFT_4_END",StringType.get());
        rs.add("DEPARTMENT_1",StringType.get());
        rs.add("DEPARTMENT_2",StringType.get());
        rs.add("DEPARTMENT_3",StringType.get());
        rs.add("DEPARTMENT_4",StringType.get());
        rs.add("PROJECT_1",StringType.get());
        rs.add("PROJECT_2",StringType.get());
        rs.add("PROJECT_3",StringType.get());
        rs.add("PROJECT_4",StringType.get());
        rs.add("DOCKET_1",StringType.get());
        rs.add("DOCKET_2",StringType.get());
        rs.add("DOCKET_3",StringType.get());
        rs.add("DOCKET_4",StringType.get());
        rs.add("JOB_1",StringType.get());
        rs.add("JOB_2",StringType.get());
        rs.add("JOB_3",StringType.get());
        rs.add("JOB_4",StringType.get());
        rowDefinition = new RowDefinition(-1,rs);
    }


    /**
     *
     *@param  c                    Connection
     *@param  alertParams          alert parameters
     *@exception  AccessException  Description of Exception
     */
    public ScheduleDataSource(DBConnection c , HashMap params) throws AccessException 
    {
        this.conn = c;      
     
        initRowSource();
        try 
        {
        	brkCodeList = (String )params.get(PARAM_BRK_CODE_LIST);
            loadAllEmployees();
        }
        catch (Exception e) {
            throw new NestedRuntimeException (e);
        }
    }
    
    
    public RowDefinition getRowDefinition() throws AccessException {
        return rowDefinition;
    }

    public RowCursor query(String queryString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String queryString, String orderByString) throws AccessException{
        return queryAll();
    }

    public RowCursor query(List keys) throws AccessException{
        return queryAll();
    }

    public RowCursor query(String[] fields, Object[] values) throws AccessException {
        return queryAll();
    }

    public RowCursor queryAll()  throws AccessException{

        return new AbstractRowCursor(getRowDefinition()){

           protected Row getCurrentRowInternal(){
               return counter >= 0 && counter < rows.size() ? (BasicRow)rows.get(counter) : null;
           }

           protected boolean fetchRowInternal() throws AccessException{
               return ++counter < rows.size();
           }

           public void close(){}

           // *** 11556 override fetchRow so that it loads one at a time
           public boolean fetchRow() throws AccessException{
               counter = -1;
               try {
                   loadRows();
               } catch (Exception e) {
                   //if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error(e.getMessage() , e) ;}
                   throw new NestedRuntimeException(e);
               }
               currentRow = null;
               if(!sourceExhausted && fetchRowInternal()){
                   currentRow = getCurrentRowInternal();
                   currentSaved = false;
                   return true;
               } else {
                   sourceExhausted = true;
               }
               return false;
           }

           public boolean hasMore(int rowCount) throws AccessException{
               return hasMore;
           }
       };
   }

    public boolean isReadOnly(){
       return true;
    }

    public int count() {
        return empIds.size();
    }

    public int count(String where) {
        return empIds.size();
    }

    
    private void loadAllEmployees() throws SQLException
    {
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try
        {
            String sql = "SELECT EMP_ID"
             + " FROM EMPLOYEE"
             + " WHERE EMPLOYEE.EMP_STATUS='A'";
        
            
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            //for each active employee
            while (rs.next())
            {
                empIds.add(rs.getString(1));
            }
 
        }    
        catch (Exception e)    
        {
            throw new NestedRuntimeException (e);
        }
        finally 
        {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
        }
        
      
    }
    
    /**
     * Method loads calc sim context for loadSize number of employees*/
    private void loadContext() throws Exception
    {
        int loadSize = 15;
        int [] empId= new int [loadSize];
        int empCounter = 0;
        
        Iterator empIdIter = empIds.iterator();
        cm = CodeMapper.createCodeMapper(conn);
        csc = new CalcSimulationContext(conn, cm);
        csa = new CalcSimulationAccess(csc);
        csa.clearEmployeeDates();
        
        for (int refreshArray = 0; refreshArray<loadSize; refreshArray++)
         {
            empId[refreshArray] = -100;
         }
        //get the employees
        while (empCounter<loadSize && empIdIter.hasNext())
        {    
            empId[empCounter]=(new Integer((String) empIdIter.next())).intValue();
            empCounter++;
            empIdIter.remove();
        }

        //if there are less than loadsize number, create another array
       if (empCounter<loadSize)
       {
               int [] empId2 = new int [empCounter];
               for (int empId2Index = 0; empId2Index<(empCounter); empId2Index++)
               {
                   empId2[empId2Index]=empId[empId2Index];
               }    
               
               csa.addEmployeeDate(empId2, DateHelper.getCurrentDate(), DateHelper.addDays(DateHelper.getCurrentDate(), 13));
           }    
       else
       {    
           csa.addEmployeeDate(empId, DateHelper.getCurrentDate(), DateHelper.addDays(DateHelper.getCurrentDate(), 13));
       }
           csa.load();
    }
    
    private void processScheduleDetailInfo(CalcSimulationEmployee empSkdData, Date currentDay) throws Exception
    {
        ArrayList exportDetails= new ArrayList();

        //get his/her data for one day
        List scheduleDetails =empSkdData.getScheduleDetails(currentDay); 
        if ((!scheduleDetails.isEmpty())&& (scheduleDetails != null))
        {        
            Iterator scheduleIter = scheduleDetails.iterator();
            while (scheduleIter.hasNext())
            {
                EmployeeScheduleDetailData esdd = (EmployeeScheduleDetailData)scheduleIter.next();
                setExportDetails(esdd, exportDetails, brkCodeList);
            }    
            //    the last schedule detail for a person when there are no more schedule details
            if (!exportDetails.isEmpty())
            {
                EmployeeData empDat = empSkdData.getEmployeeData();
                Row r = setRow(empDat.getEmpName(), empDat.getEmpLastname(), empDat.getEmpFirstname(),
                currentDay.toString(), exportDetails);
                if (r!=null)
                rows.add(r);    
                emptyDay=false;
            }
            else
            {
            	emptyDay = true;
            }	
            
        }
        else
        {
        	 EmployeeDefaultLaborData defaultLabor = empSkdData.getEmployeeDefaultLaborData();
        	 EmployeeData empDat = empSkdData.getEmployeeData();
        	 Row r = new BasicRow(getRowDefinition());
        	 r.setValue("EMP_NAME", empDat.getEmpName());
             r.setValue("EMP_LASTNAME", empDat.getEmpLastname());
             r.setValue("EMP_FIRSTNAME", empDat.getEmpFirstname());
             
             DepartmentData deptData = cm.getDepartmentById(defaultLabor.getDeptId());
             JobData jobData = cm.getJobById(defaultLabor.getJobId());
             DocketData docData = cm.getDocketById(defaultLabor.getDockId());
             
             r.setValue("WORK_DATE", currentDay);
             r.setValue(colName("DEPARTMENT", 0), deptData.getDeptName());
             r.setValue(colName("DOCKET", 0), docData.getDockName());
             r.setValue(colName("JOB", 0), jobData.getJobName());
             rows.add(r); 
             
        }	
        dayCounter++;
       
    }
       
    /**Array
     * @exception SQLException*/
    private void loadRows() throws SQLException
    {
     
     do{	 
    	Iterator empsScheduleDataIter=null;
        int date =13;
        rows.clear();
        try
        {
            //if collection containing two weeks of schedules for loadSize number of people have been processed
            //load context for the next loadSize number of people
            if (empsScheduleData==null || empsScheduleData.isEmpty())
            {
                loadContext();
                empsScheduleData = csa.getResults();
            }
      
        
            //current day is the day being processed
            
            //if it's the first time or if two weeks worth of records for employee has been processed, set the day to today
            if (dayCounter>date || dayCounter==0)
            {    
                if (dayCounter > date)
                {    
                    dayCounter=0;
                }    
                empsScheduleDataIter =empsScheduleData.iterator();
                if (empsScheduleDataIter.hasNext())
                {
                    empSkdData = (CalcSimulationEmployee) empsScheduleDataIter.next();
                }
            }    
            
            if (!empsScheduleData.isEmpty())
            {    
                Date currentDay = DateHelper.addDays(DateHelper.getCurrentDate(), dayCounter);
                //process the schedule detail info and delete the employee from collection if their schedules
                //have been completely processed
                processScheduleDetailInfo(empSkdData, currentDay);
              
                //Delete the employees that are done.
                if (dayCounter>date)
                {
                    if (!empsScheduleData.isEmpty()&& (empSkdData!=null))
                        empsScheduleData.remove(empSkdData);
                }    
            }    
        
            if (empIds.isEmpty() && (empsScheduleData.isEmpty()))
                hasMore=false;
        }  
       
      
        catch (Exception e)
        {
            throw new NestedRuntimeException (e);
        }
     } while ((emptyDay==true) && (hasMore==true)); 
    } 
     

    //export details contains shift information
    class ExportDetail
    {
        private Date startTime;
        private Date endTime;
        private String job;
        private String department;
        private String docket;
        private String project;
        private int minuteWorked;
        
        public int getMinuteWorked() {
            return minuteWorked;
        }
        public void setMinuteWorked(int minuteWorked) {
            this.minuteWorked = minuteWorked;
        }
        public ExportDetail() {
        }
        public String getDepartment() {
            return department;
        }
        public void setDepartment(String department) {
            this.department = department;
        }
        public String getDocket() {
            return docket;
        }
        public void setDocket(String docket) {
            this.docket = docket;
        }
        public Date getEndTime() {
            return endTime;
        }
        public void setEndTime(Date endTime) {
            this.endTime = endTime;
        }
        public String getJob() {
            return job;
        }
        public void setJob(String job) {
            this.job = job;
        }
        public String getProject() {
            return project;
        }
        public void setProject(String project) {
            this.project = project;
        }
        public Date getStartTime() {
            return startTime;
        }
        public void setStartTime(Date startTime) {
            this.startTime = startTime;
        }        
    }
    /**
     * @param startTime - start time of the scheduled shift
     * @param endTime - end time of the scheduled shift
     * @param emWrkDetailList - employee's work detail list for the day
     * @param exportDetails - arraylist that contains emport details, one detail for each of the day
     * */
    protected void setExportDetails(EmployeeScheduleDetailData esdd, ArrayList exportDetails, String brkCodeList)
    {
        //if (!esdd.getWrkdHtypeName().equalsIgnoreCase("UNPAID"))
    	if (!RuleHelper.isCodeInList(brkCodeList, esdd.getWrkdTcodeName()))
        {    
            //The following case is designed for business case 5:  If an associate is scheduled
            //for one shift but has to works two different jobs in that shift, then it counts as 
            //being two shifts.  Therefore, look through the work details of that day that falls within
            //the scheduled times and check to see if there is a job change.  If there is, create two
            //separate export schedule details for that day.
        
            ExportDetail ed = new ExportDetail();
            ed.setDepartment(esdd.getWrkdDeptName());
            ed.setDocket(esdd.getWrkdDockName());
            ed.setEndTime(esdd.getWrkdEndTime());
            ed.setStartTime(esdd.getWrkdStartTime());
            ed.setJob(esdd.getWrkdJobName());
            ed.setProject(esdd.getWrkdProjName());
            ed.setMinuteWorked(esdd.getWrkdMinutes());
            exportDetails.add(ed);    
        }
    }
    
    /**
     * The method sets a row of the row source with the scheduling information for one employee for one day
     * */
    protected Row setRow(String empName, String empLastName, String empFirstName, String workDate,
                        ArrayList exportDetails)throws AccessException
    {
        int minutesScheduled = 0;
        Row r = new BasicRow(getRowDefinition());
        if ((empName==null) || empName.equals(""))
            return null;
        r.setValue("EMP_NAME", empName);
        r.setValue("EMP_LASTNAME", empLastName);
        r.setValue("EMP_FIRSTNAME", empFirstName);
        r.setValue("WORK_DATE", workDate);
                
        for (int i=0; i<Math.min(exportDetails.size(),4); i++)
        {
            ExportDetail ed =  (ExportDetail)exportDetails.get(i);
            r.setValue(shiftName("START", i), ed.getStartTime());
            r.setValue(shiftName("END", i), ed.getEndTime());
            r.setValue(colName("DEPARTMENT", i), ed.getDepartment());
            r.setValue(colName("PROJECT", i), ed.getProject());
            r.setValue(colName("DOCKET", i), ed.getDocket());
            r.setValue(colName("JOB", i), ed.getJob());
            minutesScheduled = minutesScheduled+ ed.getMinuteWorked();
        }    
        
        r.setValue("MINUTES_SCHEDULED", new Integer(minutesScheduled).toString());
        return r;
        
    }
    
    protected String colName(String column, int i)
    {
        return column+"_"+(new Integer(i+1).toString());
    }
    protected String shiftName(String startEnd, int i)
    {
        return "SHIFT"+"_"+(new Integer(i+1).toString())+"_"+startEnd;
    }

}

