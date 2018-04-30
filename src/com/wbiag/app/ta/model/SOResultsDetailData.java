/*
 * Created on Dec 12, 2004
 *
 */
package com.wbiag.app.ta.model;

import com.workbrain.app.ta.model.*;
import com.workbrain.util.Datetime;

/** Title:         SOResultsDetailData
 * Description:    SO_Results_Detail data object
 * Copyright:      Copyright (c) 2003
 * Company:        Workbrain Inc
 * @author         Kevin Tsoi
 * @version 1.0
 */
public class SOResultsDetailData extends RecordData{

	private int resdetId;
	private int skdgrpId;
	private Datetime resdetDate;
	private Datetime resdetTime;
	private double resdetVolume;
	private int invtypId;
	private Integer voltypId;

	public RecordData newInstance()
	{
		return new SOResultsDetailData();
	}

	public int getResdetId()
	{
		return resdetId;
	}

	public void setResdetId(int resdetId)
	{
		this.resdetId = resdetId;
	}

	public int getSkdgrpId()
	{
		return skdgrpId;
	}

	public void setSkdgrpId(int skdgrpId)
	{
		this.skdgrpId = skdgrpId;
	}

	public Datetime getResdetDate()
	{
		return resdetDate;
	}

	public void setResdetDate(Datetime resdetDate)
	{
		this.resdetDate = resdetDate;
	}

	public Datetime getResdetTime()
	{
		return resdetTime;
	}

	public void setResdetTime(Datetime resdetTime)
	{
		this.resdetTime = resdetTime;
	}

	public double getResdetVolume()
	{
		return resdetVolume;
	}

	public void setResdetVolume(double resdetVolume)
	{
		this.resdetVolume = resdetVolume;
	}

	public int getInvtypId()
	{
		return invtypId;
	}

	public void setInvtypId(int invtypId)
	{
		this.invtypId = invtypId;
	}

    public Integer getVoltypId()
    {
        return voltypId;
    }

    public void setVoltypId(Integer voltypId)
    {
        this.voltypId = voltypId;
    }

	/**
	 * Returns a string listing all fields in the record with their values.
	 */
	public String toString() {
		String s = "SOResultsDetailData:\n" +
			"  resDetId = " + resdetId + "\n" +
			"  skdGrpId = " + skdgrpId + "\n" +
			"  resDetDate = " + resdetDate + "\n" +
			"  resDetTime = " + resdetTime + "\n" +
			"  resDetVolume = " + resdetVolume + "\n" +
			"  invTypId = " + invtypId + "\n" +
            "  volTypId = " + voltypId + "\n";
		return s;
	}

}
