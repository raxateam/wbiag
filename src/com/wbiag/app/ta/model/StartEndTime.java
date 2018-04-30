package com.wbiag.app.ta.model;

import java.io.Serializable;
import java.util.Date;

/**
 * @author bviveiros
 *
 */
public class StartEndTime implements Serializable {

	private Date startTime = null;
	private Date endTime = null;
	
	public StartEndTime() {}
	
	public StartEndTime(Date startTime, Date endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	
	public Date getEndTime() {
		return endTime;
	}
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
}
