<!-- Declares typelibs etc. -->
<%@ include file="/system/wbheader.jsp"%>

<!-- Imports -->
<%@ page import="java.util.List" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.sql.PreparedStatement" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.SOTestCase" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.db.DBInterface" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.db.ScheduleGroupAccess" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.model.CorporateEntity" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.SOTestCase" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.db.DBInterface" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.db.ScheduleGroupAccess" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.model.CorporateEntity" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData" %>
<%@ page import="com.workbrain.app.modules.retailSchedule.exceptions.RetailException" %>
<%@ page import="com.workbrain.sql.DBConnection" %>
<%@ page import="com.workbrain.sql.SQLHelper" %>
<%@ page import="com.workbrain.util.DateHelper" %>
<%@ page import="com.workbrain.util.StringHelper" %>
<%@ page import="com.workbrain.server.jsp.JSPHelper" %>
<%@ page import="com.wbiag.app.modules.pts.PTSHelper" %>


<!-- ********************************************************************************************************************
//***************************************************************************************************************************
//
//	PURPOSE: Display PTS information.
//
//	DEPENDENCIES: None
//
//	CREATED DATE : 03/17/2005
//	AUTHOR       : Kevin Tsoi
//
-->
<!-- Begin the page. -->
<wb:page type="reportQuery" maintenanceFormId='<%= request.getParameter("mfrm_id") %>' >
<wba:table caption="Payroll To Sales Report" captionLocalizeIndex='PayrollToSalesReport'>
<wb:define id="mfrm_id"><wb:get id="mfrm_id" scope="parameter" default=""/></wb:define>
<%
		String mfrmIdStr = mfrm_id.toString();

	    DBConnection conn = null;
	    PreparedStatement ps = null;
	    ResultSet rs = null;
	    ScheduleGroupAccess scheduleGroupAccess = null;
	    ScheduleGroupData scheduleGroupData = null;
	    CorporateEntity corporateEntity = null;
	    List skdGrpIdList = null;
	    List scheduleGroupDataList = null;
	    Iterator it = null;
	    Date startDate = null;
	    Date endDate = null;
	    Date dateCursor = null;
	    String currentDepartment = null;
	    String currentStore = null;
	    String wbtField = null;
	    String teamWORevField = null;
	    Date currentWorkDate = null;
	    StringBuffer sql = null;
	    boolean noResults = true;
	    boolean checkDeptWORevenue = false;
	    int daysForecasted = 0;
	    int params = 0;
	    int count = 0;
	    int numberOfDays = 0;
	    int countDays = 0;

	    //report results
	    Date reportWorkDate = null;
	    String reportStoreLastUpdated = null;
	    String reportStore = null;
	    String reportDepartment = null;
	    String reportCategory = null;
	    String reportType = null;
	    String deptWORevenueStr = null;
	    boolean deptWORevenue = false;
	    double[] reportValues = null;
	    double[] storeActualCostTotal = null;
	    double[] storeActualEarningsTotal = null;
	    double[] storeBudgetCostTotal = null;
	    double[] storeBudgetEarningsTotal = null;
	    double[] actualCostTotal = null;
	    double[] actualEarningsTotal = null;
	    double[] budgetCostTotal = null;
	    double[] budgetEarningsTotal = null;
	    BigDecimal reportDailyActual = null;
	    BigDecimal reportDailyBudget = null;
	    BigDecimal reportWeeklyActual = null;
	    BigDecimal reportWeeklyBudget = null;
	    BigDecimal reportDailyActualStoreTotal = null;
	    BigDecimal reportDailyBudgetStoreTotal = null;
	    BigDecimal reportDailyActualTotal = null;
	    BigDecimal reportDailyBudgetTotal = null;
	    BigDecimal storeWeeklyActualTotal = null;
	    BigDecimal storeWeeklyBudgetTotal = null;
	    BigDecimal weeklyActualTotal = null;
	    BigDecimal weeklyBudgetTotal = null;
	    BigDecimal percentBD = null;
	    double reportWeeklyActualCost = 0;
	    double reportWeeklyActualEarnings = 0;
	    double reportWeeklyBudgetCost = 0;
	    double reportWeeklyBudgetEarnings = 0;
	    double storeWeeklyActualCostTotal = 0;
	    double storeWeeklyActualEarningsTotal = 0;
	    double storeWeeklyBudgetCostTotal = 0;
	    double storeWeeklyBudgetEarningsTotal = 0;
	    double weeklyActualCostTotal = 0;
	    double weeklyActualEarningsTotal = 0;
	    double weeklyBudgetCostTotal = 0;
	    double weeklyBudgetEarningsTotal = 0;


	    //params
	    String wbtIdStr = null;
	    String inputDateStr = null;
	    String realOrForecastStr = null;
	    String showDailyStr = null;
	    String showWeeklyStr = null;
	    String showLastUpdatedStr = null;
	    String scaleStr = null;
	    String percentStr = null;
	    int wbtId = 0;
	    Date inputDate = null;
	    boolean realOrForecast = false;
	    boolean showDaily = false;
	    boolean showWeekly = false;
	    boolean showLastUpdated = false;
	    boolean percent = false;
		int scale = 0;

       	//set params
       	wbtIdStr = request.getParameter("WBT_ID_0");
	    if(!StringHelper.isEmpty(wbtIdStr))
	    {
	    	wbtId = Integer.parseInt(wbtIdStr);
        }
        inputDateStr = request.getParameter("START_DATE_0");
        if(!StringHelper.isEmpty(inputDateStr))
        {
        	inputDate = DateHelper.convertStringToDate(inputDateStr, "yyyyMMdd HHmmss");
        }
        else
        {
        	inputDate = DateHelper.truncateToDays(new Date());
        }
        realOrForecastStr = request.getParameter("REAL_OR_FORECAST");
        if(!StringHelper.isEmpty(realOrForecastStr))
	    {
        	realOrForecast = Boolean.valueOf(realOrForecastStr).booleanValue();
        }
        showDailyStr = request.getParameter("SHOW_DAILY");
        if(!StringHelper.isEmpty(showDailyStr))
	    {
        	showDaily = Boolean.valueOf(showDailyStr).booleanValue();
        }
        showWeeklyStr = request.getParameter("SHOW_WEEKLY");
        if(!StringHelper.isEmpty(showWeeklyStr))
	    {
        	showWeekly = Boolean.valueOf(showWeeklyStr).booleanValue();
		}
		showLastUpdatedStr = request.getParameter("SHOW_LAST_UPDATED");
		if(!StringHelper.isEmpty(showLastUpdatedStr))
	    {
        	showLastUpdated = Boolean.valueOf(showLastUpdatedStr).booleanValue();
		}
		scaleStr = request.getParameter("SCALE");
		if(!StringHelper.isEmpty(scaleStr))
	    {
        	scale = Integer.parseInt(scaleStr);
		}
		//default scale to 2
		else
		{
			scale = 2;
		}
		percentStr = request.getParameter("PERCENT");
		if(!StringHelper.isEmpty(percentStr))
	    {
        	percent = Boolean.valueOf(percentStr).booleanValue();
		}
		if(percent)
		{
			percentBD = new BigDecimal(100);
		}
		else
		{
			percentBD = new BigDecimal(1);
		}

		try
        {
	        conn = JSPHelper.getConnection( request );
	        DBInterface.init(conn);
	        scheduleGroupAccess = new ScheduleGroupAccess(conn);
	        scheduleGroupDataList = scheduleGroupAccess.loadRecordData(new ScheduleGroupData(), "SO_SCHEDULE_GROUP", "WBT_ID", wbtId);
	        if(scheduleGroupDataList.size() <= 0)
	        {
	        	throw new RetailException("Team does not map to a schedule group.");
	        }

	        scheduleGroupData = (ScheduleGroupData)scheduleGroupDataList.get(0);

	        if(realOrForecast)
	        {
	            //TODO remove comment
	            inputDate = DateHelper.truncateToDays(new Date());
	            endDate = inputDate;
	        }
	        startDate = PTSHelper.getForecastStartDate(scheduleGroupData, inputDate, -7);
	        if(!realOrForecast)
	        {
	            daysForecasted = PTSHelper.getDaysForecasted(scheduleGroupData);
	            if(daysForecasted == -1)
	            {
	            	daysForecasted = 7;
	            }
	            endDate = DateHelper.addDays(startDate, daysForecasted-1);
	        }
%>
	<tr>
<%
			if(showLastUpdated)
			{
%>
		<wba:th rowspan='2'><wb:localize id="PTS_Last_Updated" overrideId="<%=mfrmIdStr%>">Last Updated</wb:localize></wba:th>
<%
			}
%>
		<wba:th rowspan='2'><wb:localize id="PTS_Store" overrideId="<%=mfrmIdStr%>">Store</wb:localize></wba:th>
		<wba:th rowspan='2'><wb:localize id="PTS_Department" overrideId="<%=mfrmIdStr%>">Department</wb:localize></wba:th>
<%
	        dateCursor = startDate;
	        while(!dateCursor.after(endDate))
	        {
	        	if(showDaily)
	        	{
%>
		<wba:th colspan='2'><%=DateHelper.convertDateString(dateCursor, "MM/dd/yyyy") %></wba:th>
<%
				}
				dateCursor = DateHelper.addDays(dateCursor, 1);
				//remember number of days
				numberOfDays++;
			}
			if(showWeekly)
			{
%>
		<wba:th colspan='2'><wb:localize id="PTS_WeeklyPTS" overrideId="<%=mfrmIdStr%>">Weekly PTS</wb:localize></wba:th>
<%
			}
%>
	</tr>
	<tr>
<%
	        dateCursor = startDate;
	        while(!dateCursor.after(endDate))
	        {
	        	if(showDaily)
	        	{
%>
		<wba:th><wb:localize id="PTS_Actual" overrideId="<%=mfrmIdStr%>">Actual</wb:localize></wba:th>
		<wba:th><wb:localize id="PTS_Budget" overrideId="<%=mfrmIdStr%>">Budgeted</wb:localize></wba:th>
<%
				}
				dateCursor = DateHelper.addDays(dateCursor, 1);
			}
				if(showWeekly)
				{
%>
		<wba:th><wb:localize id="PTS_Actual" overrideId="<%=mfrmIdStr%>">Actual</wb:localize></wba:th>
		<wba:th><wb:localize id="PTS_Budget" overrideId="<%=mfrmIdStr%>">Budgeted</wb:localize></wba:th>
<%
				}
%>
	</tr>
<%
			//initialize totals
			storeActualCostTotal = new double[numberOfDays];
	    	storeActualEarningsTotal = new double[numberOfDays];
	    	storeBudgetCostTotal = new double[numberOfDays];
	    	storeBudgetEarningsTotal = new double[numberOfDays];
	    	actualCostTotal = new double[numberOfDays];
	    	actualEarningsTotal = new double[numberOfDays];
	    	budgetCostTotal = new double[numberOfDays];
	    	budgetEarningsTotal = new double[numberOfDays];

	        corporateEntity = new CorporateEntity(scheduleGroupData);
	        skdGrpIdList = corporateEntity.getCorporateEntityIDList(CorporateEntity.ALL_SUBTREE);

			//get wb
			wbtField = PTSHelper.getRegistryValue(PTSHelper.REG_PTS_WBT_FIELD_FOR_CALC_DATE);
			teamWORevField = PTSHelper.getRegistryValue(PTSHelper.REG_PTS_MARK_TEAMS_WO_REVENUE_FIELD);
		    if(!StringHelper.isEmpty(teamWORevField))
		    {
		        checkDeptWORevenue = true;
		    }

	        sql = new StringBuffer();
	        sql.append("SELECT ");
	    	sql.append("P.pts_store_skdgrp_name, ");
	    	sql.append("SG.skdgrp_name, ");
	    	sql.append("P.pts_value, ");
	    	sql.append("P.pts_category, ");
	    	sql.append("P.pts_type, ");
	    	sql.append("P.pts_workdate, ");

	    	if(checkDeptWORevenue)
	    	{
	    		sql.append("T.");
	    		sql.append(teamWORevField);
	    		sql.append(", ");
	    	}

	    	sql.append("T.");
	    	sql.append(wbtField);
	    	sql.append(" ");

	    	sql.append("FROM ");
	    	sql.append("payroll_to_sales P, ");
	    	sql.append("so_schedule_group SG, ");
	    	sql.append("workbrain_team T ");

	    	sql.append("WHERE ");
	    	sql.append("P.skdgrp_id = SG.skdgrp_id ");
	    	sql.append("AND T.wbt_id = SG.wbt_id ");
	    	sql.append("AND SG.skdgrp_intrnl_type = ? ");
	    	sql.append("AND P.pts_workdate between ? AND ? ");

			sql.append(" AND P.skdgrp_id in (");
        	for (int i = 0; i < skdGrpIdList.size(); i++)
        	{
            sql.append(i > 0 ? ",?" : "?");
        	}
        	sql.append(")");

	    	sql.append("Group by ");
	        sql.append("P.pts_store_skdgrp_name, ");
	    	sql.append("SG.skdgrp_name, ");
	    	sql.append("P.pts_workdate, ");
	    	sql.append("P.pts_category, ");
	    	sql.append("P.pts_type, ");
	        sql.append("P.pts_value, ");

	        if(checkDeptWORevenue)
	    	{
	    		sql.append("T.");
	    		sql.append(teamWORevField);
	    		sql.append(", ");
	    	}

	        sql.append("T.");
	    	sql.append(wbtField);
	    	sql.append(" ");

	        ps = conn.prepareStatement(sql.toString());

	        params = 1;

	        ps.setInt(params++, 11);
	        ps.setTimestamp(params++, DateHelper.toDatetime(startDate));
	        ps.setTimestamp(params++, DateHelper.toDatetime(endDate));

	        it = skdGrpIdList.iterator();
	        while(it.hasNext())
	        {
	        	ps.setInt(params++, Integer.parseInt((String)it.next()));
	        }

	        rs = ps.executeQuery();

	        reportValues = new double[4];
	        while(rs.next())
	        {
	        	deptWORevenue = false;
	        	noResults = false;
	        	reportStoreLastUpdated = rs.getString(wbtField);
	            reportStore = rs.getString("pts_store_skdgrp_name");
	            reportDepartment = rs.getString("skdgrp_name");
	            reportCategory = rs.getString("pts_category");
	            reportType = rs.getString("pts_type");
	            reportWorkDate = rs.getDate("pts_workdate");
				if(checkDeptWORevenue)
	    		{
					deptWORevenueStr = rs.getString(teamWORevField);
					if( !StringHelper.isEmpty(deptWORevenueStr) &&
						"Y".equalsIgnoreCase(deptWORevenueStr) )
					{
						deptWORevenue = true;
					}
				}
				if(currentWorkDate == null)
				{
					currentWorkDate = reportWorkDate;
				}
				else if(!currentWorkDate.equals(reportWorkDate))
				{
					currentWorkDate = reportWorkDate;

					//reset daily pts values
	                reportValues[0] = 0;
	                reportValues[1] = 0;
	                reportValues[2] = 0;
	                reportValues[3] = 0;

	                //reset count
	                count = 0;
				}

	            if(currentDepartment == null)
	            {
	                //first row
	                currentDepartment = reportDepartment;
	                currentStore = reportStore;
	                dateCursor = startDate;
	                countDays = 0;
%>
	<tr>
<%
                    if(showLastUpdated)
                    {
%>
			<wba:td><%=reportStoreLastUpdated %></wba:td>
<%
					}
%>
		<wba:td><%=reportStore %></wba:td>
		<wba:td><%=reportDepartment %></wba:td>
<%
	            }
	            else if(!currentDepartment.equals(reportDepartment))
	            {
	            	currentDepartment = reportDepartment;

					//move weekly to appropriate column
					while(countDays < numberOfDays)
	                {
		                if(showDaily)
		                {
%>
		<wba:td></wba:td>
		<wba:td></wba:td>
<%
						}
	                    countDays++;
	                }
	                //print weekly pts
	                if(reportWeeklyActualEarnings > 0)
	                {
	                	reportWeeklyActual = new BigDecimal(reportWeeklyActualCost/reportWeeklyActualEarnings);
	                	reportWeeklyActual = reportWeeklyActual.multiply(percentBD);
	                }
	                else
	                {
	                	reportWeeklyActual = new BigDecimal(0);
	                }
	                if(reportWeeklyBudgetEarnings > 0)
	                {
	                	reportWeeklyBudget = new BigDecimal(reportWeeklyBudgetCost/reportWeeklyBudgetEarnings);
	                	reportWeeklyBudget = reportWeeklyBudget.multiply(percentBD);
	                }
	                else
	                {
	                	reportWeeklyBudget = new BigDecimal(0);
	                }
	                if(showWeekly)
	                {
%>
		<wba:td><%=reportWeeklyActual.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
		<wba:td><%=reportWeeklyBudget.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
<%
					}
%>
	</tr>
<%
	                //reset weekly pts
	                reportWeeklyActual = null;
	                reportWeeklyBudget = null;
	                reportWeeklyActualCost = 0;
	                reportWeeklyActualEarnings = 0;
	                reportWeeklyBudgetCost = 0;
	                reportWeeklyBudgetEarnings = 0;

	                //new row
	                countDays = 0;
	                dateCursor = startDate;
%>
	<tr>
<%
	                if(reportStore.equals(currentStore))
	                {
	                    if(showLastUpdated)
	                    {
%>
			<wba:td></wba:td>
<%
						}
%>
		<wba:td></wba:td>
<%
	                }
	                else
	                {
%>
		<tr>
<%
							if(showLastUpdated)
							{
%>
			<wba:td></wba:td>
<%
							}
%>
			<wba:td><wb:localize id="PTS_Store_Total" overrideId="<%=mfrmIdStr%>">total:</wb:localize></wba:td>
			<wba:td></wba:td>
<%
						for(int i=0 ; i<numberOfDays ; i++)
						{
							if(storeActualEarningsTotal[i] > 0)
							{
								reportDailyActualStoreTotal = new BigDecimal(storeActualCostTotal[i]/storeActualEarningsTotal[i]);
								reportDailyActualStoreTotal = reportDailyActualStoreTotal.multiply(percentBD);
							}
							else
							{
								reportDailyActualStoreTotal = new BigDecimal(0);
							}
							if(storeBudgetEarningsTotal[i] > 0)
							{
								reportDailyBudgetStoreTotal = new BigDecimal(storeBudgetCostTotal[i]/storeBudgetEarningsTotal[i]);
								reportDailyBudgetStoreTotal = reportDailyBudgetStoreTotal.multiply(percentBD);
							}
							else
							{
								reportDailyBudgetStoreTotal = new BigDecimal(0);
							}
							storeWeeklyActualCostTotal += storeActualCostTotal[i];
	    					storeWeeklyActualEarningsTotal += storeActualEarningsTotal[i];
	    					storeWeeklyBudgetCostTotal += storeBudgetCostTotal[i];
	    					storeWeeklyBudgetEarningsTotal += storeBudgetEarningsTotal[i];

%>
			<wba:td><%=reportDailyActualStoreTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
			<wba:td><%=reportDailyBudgetStoreTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
<%
	                	}
	                	if(storeWeeklyActualEarningsTotal > 0)
	                	{
	               			storeWeeklyActualTotal = new BigDecimal(storeWeeklyActualCostTotal/storeWeeklyActualEarningsTotal);
	               			storeWeeklyActualTotal = storeWeeklyActualTotal.multiply(percentBD);
	               		}
	               		else
	               		{
	               			storeWeeklyActualTotal = new BigDecimal(0);
	               		}
	               		if(storeWeeklyBudgetEarningsTotal > 0)
	                	{
	               			storeWeeklyBudgetTotal = new BigDecimal(storeWeeklyBudgetCostTotal/storeWeeklyBudgetEarningsTotal);
	               			storeWeeklyBudgetTotal = storeWeeklyBudgetTotal.multiply(percentBD);
	               		}
	               		else
	               		{
	               			storeWeeklyBudgetTotal = new BigDecimal(0);
	               		}
%>
			<wba:td><%=storeWeeklyActualTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
			<wba:td><%=storeWeeklyBudgetTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
		</tr>
<%
						//reset store total values
						storeActualCostTotal = new double[numberOfDays];
				    	storeActualEarningsTotal = new double[numberOfDays];
				    	storeBudgetCostTotal = new double[numberOfDays];
				    	storeBudgetEarningsTotal = new double[numberOfDays];
				    	storeWeeklyActualCostTotal = 0;
    					storeWeeklyActualEarningsTotal = 0;
    					storeWeeklyBudgetCostTotal = 0;
    					storeWeeklyBudgetEarningsTotal = 0;

	                    currentStore = reportStore;
	                    if(showLastUpdated)
	                    {
%>
			<wba:td><%=reportStoreLastUpdated %></wba:td>
<%
						}
%>
		<wba:td><%=reportStore %></wba:td>
<%
	                }
%>
		<wba:td><%=reportDepartment %></wba:td>
<%
	               	//reset daily pts values
	                reportValues[0] = 0;
	                reportValues[1] = 0;
	                reportValues[2] = 0;
	                reportValues[3] = 0;

	                //reset count
	                count = 0;
	            }
            	//calculate and print daily actual and budget
            	reportValues[count++] = rs.getDouble("pts_value");

	            if(count == 4)
	            {
	                if(reportValues[1] > 0)
	                {
	                    reportDailyActual = new BigDecimal(reportValues[0]/reportValues[1]);
	                    reportDailyActual = reportDailyActual.multiply(percentBD);
	                }
	                else
	                {
	                	reportDailyActual = new BigDecimal(0);
	                }
	                if(reportValues[3] > 0)
	                {
	                    reportDailyBudget = new BigDecimal(reportValues[2]/reportValues[3]);
	                    reportDailyBudget = reportDailyBudget.multiply(percentBD);
	                }
	                else
	                {
	                	reportDailyBudget = new BigDecimal(0);
	                }
	                reportWeeklyActualCost += reportValues[0];
	                reportWeeklyActualEarnings += reportValues[1];
	                reportWeeklyBudgetCost += reportValues[2];
	                reportWeeklyBudgetEarnings += reportValues[3];

	              	//make sure results print on the correct day
					while(dateCursor.before(reportWorkDate))
	                {
		                if(showDaily)
		                {
%>
		<wba:td></wba:td>
		<wba:td></wba:td>
<%
						}
	                    dateCursor = DateHelper.addDays(dateCursor, 1);
	                    countDays++;
	                }
	              	//print daily pts
	                if(showDaily)
	                {
%>
		<wba:td><%=reportDailyActual.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
		<wba:td><%=reportDailyBudget.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
<%
					}
          //accumulate total
          if(!deptWORevenue) {

      	    //store total actuals
      	    storeActualEarningsTotal[countDays] += reportValues[1];

      	    //grand total actuals
      	    actualEarningsTotal[countDays] += reportValues[1];

      	    //store total budgeted
      	    storeBudgetEarningsTotal[countDays] += reportValues[3];

      	    //grand total budgeted
      	    budgetEarningsTotal[countDays] += reportValues[3];
          }
          storeActualCostTotal[countDays] += reportValues[0];
          actualCostTotal[countDays] += reportValues[0];
          storeBudgetCostTotal[countDays] += reportValues[2];
          budgetCostTotal[countDays] += reportValues[2];

					//move cursor to next day
	                dateCursor = DateHelper.addDays(dateCursor, 1);
	                countDays++;

	                //reset daily pts values
	                reportValues[0] = 0;
	                reportValues[1] = 0;
	                reportValues[2] = 0;
	                reportValues[3] = 0;

	                //reset count
	                count = 0;
	            }
	        }
	        if(!noResults)
	        {
				//move weekly to appropriate column
				while(countDays < numberOfDays)
	            {
	                if(showDaily)
	                {
%>
		<wba:td></wba:td>
		<wba:td></wba:td>
<%
					}
	                countDays++;
	            }
		      	//print last weekly pts
		      	if(reportWeeklyActualEarnings > 0)
	            {
		      		reportWeeklyActual = new BigDecimal(reportWeeklyActualCost/reportWeeklyActualEarnings);
		      		reportWeeklyActual = reportWeeklyActual.multiply(percentBD);
	            }
	            else
	            {
	            	reportWeeklyActual = new BigDecimal(0);
	            }
	            if(reportWeeklyBudgetEarnings > 0)
	            {
		        	reportWeeklyBudget = new BigDecimal(reportWeeklyBudgetCost/reportWeeklyBudgetEarnings);
		        	reportWeeklyBudget = reportWeeklyBudget.multiply(percentBD);
		        }
	            else
	            {
	            	reportWeeklyBudget = new BigDecimal(0);
	            }
	            if(showWeekly)
	            {
%>
		<wba:td><%=reportWeeklyActual.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
		<wba:td><%=reportWeeklyBudget.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
<%
				}
%>
	</tr>
		<tr>
<%
							if(showLastUpdated)
							{
%>
			<wba:td></wba:td>
<%
							}
%>
			<wba:td><wb:localize id="PTS_Store_Total" overrideId="<%=mfrmIdStr%>">total:</wb:localize></wba:td>
			<wba:td></wba:td>
<%
						for(int i=0 ; i<numberOfDays ; i++)
						{
							if(storeActualEarningsTotal[i] > 0)
							{
								reportDailyActualStoreTotal = new BigDecimal(storeActualCostTotal[i]/storeActualEarningsTotal[i]);
								reportDailyActualStoreTotal = reportDailyActualStoreTotal.multiply(percentBD);
							}
							else
							{
								reportDailyActualStoreTotal = new BigDecimal(0);
							}
							if(storeBudgetEarningsTotal[i] > 0)
							{
								reportDailyBudgetStoreTotal = new BigDecimal(storeBudgetCostTotal[i]/storeBudgetEarningsTotal[i]);
								reportDailyBudgetStoreTotal = reportDailyBudgetStoreTotal.multiply(percentBD);
							}
							else
							{
								reportDailyBudgetStoreTotal = new BigDecimal(0);
							}
							storeWeeklyActualCostTotal += storeActualCostTotal[i];
	    					storeWeeklyActualEarningsTotal += storeActualEarningsTotal[i];
	    					storeWeeklyBudgetCostTotal += storeBudgetCostTotal[i];
	    					storeWeeklyBudgetEarningsTotal += storeBudgetEarningsTotal[i];
%>
			<wba:td><%=reportDailyActualStoreTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
			<wba:td><%=reportDailyBudgetStoreTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
<%
	                	}
	                	if(storeWeeklyActualEarningsTotal > 0)
	                	{
	               			storeWeeklyActualTotal = new BigDecimal(storeWeeklyActualCostTotal/storeWeeklyActualEarningsTotal);
	               			storeWeeklyActualTotal = storeWeeklyActualTotal.multiply(percentBD);
	               		}
	               		else
	               		{
	               			storeWeeklyActualTotal = new BigDecimal(0);
	               		}
	               		if(storeWeeklyBudgetEarningsTotal > 0)
	                	{
	               			storeWeeklyBudgetTotal = new BigDecimal(storeWeeklyBudgetCostTotal/storeWeeklyBudgetEarningsTotal);
	               			storeWeeklyBudgetTotal = storeWeeklyBudgetTotal.multiply(percentBD);
	               		}
	               		else
	               		{
	               			storeWeeklyBudgetTotal = new BigDecimal(0);
	               		}
%>
			<wba:td><%=storeWeeklyActualTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
			<wba:td><%=storeWeeklyBudgetTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
		</tr>
		<tr>
<%
							if(showLastUpdated)
							{
%>
			<wba:td></wba:td>
<%
							}
%>
			<wba:td><wb:localize id="PTS_Total" overrideId="<%=mfrmIdStr%>">TOTAL:</wb:localize></wba:td>
			<wba:td></wba:td>
<%
						for(int i=0 ; i<numberOfDays ; i++)
						{
							if(actualEarningsTotal[i] > 0)
							{
								reportDailyActualTotal = new BigDecimal(actualCostTotal[i]/actualEarningsTotal[i]);
								reportDailyActualTotal = reportDailyActualTotal.multiply(percentBD);
							}
							else
							{
								reportDailyActualTotal = new BigDecimal(0);
							}
							if(budgetEarningsTotal[i] > 0)
							{
								reportDailyBudgetTotal = new BigDecimal(budgetCostTotal[i]/budgetEarningsTotal[i]);
								reportDailyBudgetTotal = reportDailyBudgetTotal.multiply(percentBD);
							}
							else
							{
								reportDailyBudgetTotal = new BigDecimal(0);
							}
							weeklyActualCostTotal += actualCostTotal[i];
	    					weeklyActualEarningsTotal += actualEarningsTotal[i];
							weeklyBudgetCostTotal += budgetCostTotal[i];
	    					weeklyBudgetEarningsTotal += budgetEarningsTotal[i];
%>
			<wba:td><%=reportDailyActualTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
			<wba:td><%=reportDailyBudgetTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
<%
	                	}
	                	if(weeklyActualEarningsTotal > 0)
	                	{
	               			weeklyActualTotal = new BigDecimal(weeklyActualCostTotal/weeklyActualEarningsTotal);
	               			weeklyActualTotal = weeklyActualTotal.multiply(percentBD);
	               		}
	               		else
	               		{
	               			weeklyActualTotal = new BigDecimal(0);
	               		}
	               		if(weeklyBudgetEarningsTotal > 0)
	                	{
	               			weeklyBudgetTotal = new BigDecimal(weeklyBudgetCostTotal/weeklyBudgetEarningsTotal);
	               			weeklyBudgetTotal = weeklyBudgetTotal.multiply(percentBD);
	               		}
	               		else
	               		{
	               			weeklyBudgetTotal = new BigDecimal(0);
	               		}
%>
			<wba:td><%=weeklyActualTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>
			<wba:td><%=weeklyBudgetTotal.setScale(scale, BigDecimal.ROUND_HALF_UP) %></wba:td>

		</tr>
<%
			}
			else
			{
%>
<tr>
<wba:td>
<wb:localize id="PTS_No_Results" overrideId="<%=mfrmIdStr%>">No results returned.</wb:localize>
</wba:td>
</tr>
<%
			}
        }
        catch(RetailException e)
        {
%>
<tr>
<wba:td>
<%=e.getMessage()%>
</wba:td>
</tr>
<%
        }
        catch(SQLException e)
        {
            throw e;
        }
        finally
        {
            SQLHelper.cleanUp(ps, rs);
        }
%>

</wba:table>

</wb:page>