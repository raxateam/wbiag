package com.wbiag.tool.autotest.chart;

import java.io.File;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.workbrain.sql.DBConnection;
import com.workbrain.sql.SQLHelper;
import com.workbrain.util.DateHelper;
import com.workbrain.util.StringHelper;

/** 
 * Title:			Generate Charts
 * Description:		Class that creates autotest chart based on parameters
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Oct 18, 2005
 * @author         	Kevin Tsoi
 */
public class GenerateCharts
{       
    public static final String CHART_NAME = "ATChart";
    
    public static final String DATE_FORMAT = "MM/dd/yyyy";
    public static final String DATE_TIME_FORMAT = "yyyyMMdd HHmmss";
    public static final String TIME_FORMAT = "HHmm";
    
    public static final String TIME_UNIT_MILLISECONDS = "ms";
    private static final int DEFAULT_PRECISION = 2;
    
    private String testName = null;
    private String atParentName = null;
    private Date lastRun = null;    
    private double lastRunDuration = 0;
    private double durationAvg = 0;
    private int numOfPoints = 0;    
    private int atParentId = 0;   
    
    /**
     * Generates a chart with the given parameters
     * 
     * @param conn
     * @param realPath
     * @param atId
     * @param fromDateStr
     * @param toDateStr
     * @param startWindowStr
     * @param endWindowStr
     * @param timePrecisionStr
     * @param timeUnit
     * @param threshold
     * @param endPoints
     * @return
     * @throws Exception
     */
    public String GenerateXYChart(DBConnection conn,
            String realPath,
            int atId, 
            String fromDateStr, 
            String toDateStr,
            String startWindowStr,
            String endWindowStr,
            String timePrecisionStr,
            String timeUnit,
            int threshold,
            boolean endPoints)
    	throws Exception
    {
        PreparedStatement ps = null;
        ResultSet rs = null;
        StringBuffer sql = null;
        Date testDateTime = null;
        Date testTime = null;
        Date testTimeNextDay = null;
        Date defaultDate = null;
        Date fromDate = null;
        Date toDate = null;
        Date startWindow = null;
        Date endWindow = null;
        BigDecimal BDLastRunDuration = null;
        BigDecimal BDDurationAvg = null;
        String filename = null;      
        String yAxisText = null;
        boolean windowCrossMidnight = false;
        double testDuration = 0;                
        int precision = DEFAULT_PRECISION;
        
        //initialize
        sql = new StringBuffer();        
        fromDate = ChartUtil.convertStringToDate("01/01/1900", DATE_FORMAT);
        toDate = ChartUtil.convertStringToDate("01/01/3000", DATE_FORMAT);        
        defaultDate = fromDate;
        
        //set dates if passed in
        if(!StringHelper.isEmpty(fromDateStr))
        {            
            fromDate = ChartUtil.convertStringToDate(fromDateStr, DATE_TIME_FORMAT);            
        }
        if(!StringHelper.isEmpty(toDateStr))
        {                        
            toDate = ChartUtil.convertStringToDate(toDateStr, DATE_TIME_FORMAT);
            toDate = DateHelper.addDays(toDate, 1);
        }
        
        //sets start and end window if passed in
        if(!StringHelper.isEmpty(startWindowStr))
        {            
            startWindow = ChartUtil.convertStringToDate(startWindowStr, TIME_FORMAT);            
            startWindow = ChartUtil.setDateToTime(defaultDate, startWindow);
        }
        if(!StringHelper.isEmpty(endWindowStr))
        {                        
            endWindow = ChartUtil.convertStringToDate(endWindowStr, TIME_FORMAT);
            endWindow = ChartUtil.setDateToTime(defaultDate, endWindow);
        }
        
        //moves endWindow if cross midnight
        if(startWindow != null && endWindow != null)
        {
            if(!endWindow.after(startWindow))
            {
                endWindow = DateHelper.addDays(endWindow, 1);
                windowCrossMidnight = true;
            }
        }
        
        //set precision
        if(!StringHelper.isEmpty(timePrecisionStr))
        {
            precision = Integer.parseInt(timePrecisionStr);
        }
        
        //adjust threshold for time unit
        if(!TIME_UNIT_MILLISECONDS.equalsIgnoreCase(timeUnit))
        {
            threshold *=1000;
        }
        
        sql.append(" SELECT ");
        sql.append(" AT1.WAT_NAME, ");
        sql.append(" AT1.WAT_PARENT_ID, ");
        sql.append(" WATD_DATETIME, ");
        sql.append(" WATD_DURATION, ");
        sql.append(" AT2.WAT_NAME PARENT_NAME ");
        sql.append(" FROM ");
        sql.append(" WBIAG_AUTOTEST AT1, ");
        sql.append(" WBIAG_AUTOTEST AT2, ");
        sql.append(" WBIAG_AUTOTEST_DETAIL ATD ");
        sql.append(" WHERE ");
        sql.append(" AT1.WAT_ID = ATD.WAT_ID ");
        sql.append(" AND ");
        sql.append(" AT1.WAT_ID = ? ");
        sql.append(" AND ");
        sql.append(" WATD_DATETIME BETWEEN ? AND ? ");
        sql.append(" AND ");
        sql.append(" WATD_DURATION > ? ");
        sql.append(" AND ");
        sql.append(" AT1.WAT_PARENT_ID = AT2.WAT_ID ");        
                
        try
        {
	        ps = conn.prepareStatement(sql.toString());        
	        ps.setInt(1, atId);
	        ps.setTimestamp(2, ChartUtil.toTimestamp(fromDate));
	        ps.setTimestamp(3, ChartUtil.toTimestamp(toDate));
	        ps.setInt(4, threshold);
	        	      
	        rs = ps.executeQuery();
	        	        
	        XYSeries dataSeries = new XYSeries("");
	        
	        //set dataseries for chart	        
	        while(rs.next())
	        {
	            testDateTime = rs.getTimestamp("WATD_DATETIME");
	            testDuration = rs.getDouble("WATD_DURATION");
	            atParentId = rs.getInt("WAT_PARENT_ID");
	            
	            //get atNames
	            testName = rs.getString("WAT_NAME");
	            atParentName = rs.getString("PARENT_NAME");
	            
	            testTime = ChartUtil.setDateToTime(defaultDate, testDateTime);
	            
	            //use this for cross midnight windows
	            testTimeNextDay = DateHelper.addDays(testTime, 1);
	            
	            //only consider times within window
	            if(((startWindow == null || !testTime.before(startWindow))
	            	&& (endWindow == null || !testTime.after(endWindow)))
	            	||
	            	(windowCrossMidnight && 
	            	((startWindow == null || !testTimeNextDay.before(startWindow))
	    	        && (endWindow == null || !testTimeNextDay.after(endWindow))))
	            	)
	            	
	            {	            
		            //set last run info
		            if(lastRun == null || testDateTime.after(lastRun))
		            {
		                lastRun = testDateTime;
		                lastRunDuration = testDuration;
		            }
		            
		            //keep count and total
		            durationAvg += testDuration;
		            numOfPoints++;
		            		            
		            //adjust for time unit
		            if(!TIME_UNIT_MILLISECONDS.equalsIgnoreCase(timeUnit))
		            {
		                testDuration /=1000;
		            }
		            
		            //add data to dataSeries
		            dataSeries.add(testDateTime.getTime(), testDuration);
	            }
	        }	      	       
	        
	        //find avg
	        durationAvg = durationAvg/numOfPoints;
	        
	      	//adjust for time unit
            if(!TIME_UNIT_MILLISECONDS.equalsIgnoreCase(timeUnit))
            {
                lastRunDuration /=1000;
                durationAvg /=1000;
            }
            
            //adjust for time precision
            if(!Double.isInfinite(lastRunDuration) && !Double.isNaN(lastRunDuration))
            {
                BDLastRunDuration = new BigDecimal(lastRunDuration);
                BDLastRunDuration = BDLastRunDuration.setScale(precision, BigDecimal.ROUND_HALF_UP);
                lastRunDuration = BDLastRunDuration.doubleValue();
            }
            
            if(!Double.isInfinite(durationAvg) && !Double.isNaN(durationAvg))
            {
                BDDurationAvg = new BigDecimal(durationAvg);
                BDDurationAvg = BDDurationAvg.setScale(precision, BigDecimal.ROUND_HALF_UP);
                durationAvg = BDDurationAvg.doubleValue();
            }
            
	        XYSeriesCollection xyDataset = new XYSeriesCollection(dataSeries);	        	        
			
	        //adjust time unit on y axis
	        yAxisText = "Duration (ms)";	        
	        if(!TIME_UNIT_MILLISECONDS.equalsIgnoreCase(timeUnit))
            {
	            yAxisText ="Duration (s)";
            }
	        
			//Create the chart object			
			JFreeChart chart = ChartFactory.createTimeSeriesChart(
			        "AUTOTEST RESULTS",
			        "Date", yAxisText,
			        xyDataset,
			        false,
			        false,
			        false
			      );						

            //Display data points or just the lines?
			XYPlot plot = chart.getXYPlot();
			
			if (endPoints)
            {
                XYItemRenderer renderer = plot.getRenderer();
                if (renderer instanceof XYLineAndShapeRenderer)
                {
                    XYLineAndShapeRenderer rr = (XYLineAndShapeRenderer) renderer;
                    rr.setLinesVisible(false);
                    rr.setShapesFilled(true);
                    rr.setShapesVisible(true);
                }
            }							 					
														
			filename = CHART_NAME + System.currentTimeMillis();							
			filename = filename+".jpeg";
			
			File jpegFile = new File(realPath, filename);						
			
			ChartUtilities.saveChartAsJPEG(jpegFile, chart, 640, 480);			
		}
		finally
		{
		    SQLHelper.cleanUp(ps, rs);
		}
		
		//if not results returned, still need to set AT name
        if(StringHelper.isEmpty(testName))
        {
            sql = new StringBuffer();
            
            sql.append(" SELECT ");
            sql.append(" A1.WAT_NAME, ");
            sql.append(" A1.WAT_PARENT_ID, ");
            sql.append(" A2.WAT_NAME PARENT_NAME ");
            sql.append(" FROM ");
            sql.append(" WBIAG_AUTOTEST A1, ");
            sql.append(" WBIAG_AUTOTEST A2 ");
            sql.append(" WHERE ");
            sql.append(" A1.WAT_ID = ? ");
            sql.append(" AND ");
            sql.append(" A1.WAT_PARENT_ID = A2.WAT_ID ");
            
            try
            {
	            ps = conn.prepareStatement(sql.toString());        
		        ps.setInt(1, atId);
		        
		        rs = ps.executeQuery();
		        if(rs.next())
		        {
		            testName = rs.getString("WAT_NAME");
		            atParentId = rs.getInt("WAT_PARENT_ID");
		            atParentName = rs.getString("PARENT_NAME");
		        }	        
            }
            finally
            {
                SQLHelper.cleanUp(ps, rs);
            }                       
        }
        return filename;
    }    
    /**
     * @return Returns the lastRun.
     */
    public Date getLastRun()
    {
        return lastRun;
    }
    /**
     * @return Returns the lastRunDuration.
     */
    public double getLastRunDuration()
    {
        return lastRunDuration;
    }
    /**
     * @return Returns the testName.
     */
    public String getTestName()
    {
        return testName;
    }
    /**
     * @return Returns the durationAvg.
     */
    public double getDurationAvg()
    {
        return durationAvg;
    }
    /**
     * @return Returns the numOfPoints.
     */
    public int getNumOfPoints()
    {
        return numOfPoints;
    }
    /**
     * @return Returns the atParentId.
     */
    public int getAtParentId()
    {
        return atParentId;
    }
    /**
     * @return Returns the atParentName.
     */
    public String getAtParentName()
    {
        return atParentName;
    }
}
