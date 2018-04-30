/*
 * Created on Dec 8, 2004
 *
 */
package com.wbiag.app.wbinterface.pos;

import com.workbrain.util.Datetime;

/** Title:         POSData
 * Description:    POS data object
 * Copyright:      Copyright (c) 2003
 * Company:        Workbrain Inc
 * @author         Kevin Tsoi
 * @version 1.0
 */

public class POSData
{
	private String skdgrpName;
	private int invtypId;
	private Datetime resdetDate;
	private Datetime resdetTime;
	private float resdetVolume;
	private int inputInvtypId;
	private String oldSkdgrpName;
	private int recordType;
	private int skdgrpId;
	private int resdetId;
	private Integer voltypId;

	public POSData()
	{
//		resDetDate = new GregorianCalendar(1900, 1, 1, 12, 0).getTime();
//		resDetTime = new GregorianCalendar(1900, 1, 1, 12, 0).getTime();
	}

	public String getSkdgrpName()
	{
		return skdgrpName;
	}

	public void setSkdgrpName(String skdgrpName)
	{
		this.skdgrpName = skdgrpName;
	}

	public int getInvtypId()
	{
		return invtypId;
	}

	public void setInvtypId(int invtypId)
	{
		this.invtypId = invtypId;
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

	public float getResdetVolume()
	{
		return resdetVolume;
	}

	public void setResdetVolume(float resdetVolume)
	{
		this.resdetVolume = resdetVolume;
	}

	public int getInputInvtypId()
	{
		return inputInvtypId;
	}

	public void setInputInvtypId(int inputInvtypId)
	{
		this.inputInvtypId = inputInvtypId;
	}

	public String getOldSkdgrpName()
	{
		return oldSkdgrpName;
	}

	public void setOldSkdgrpName(String oldSkdgrpName)
	{
		this.oldSkdgrpName = oldSkdgrpName;
	}

	public int getRecordType()
	{
		return recordType;
	}

	public void setRecordType(int recordType)
	{
		this.recordType = recordType;
	}

	public int getSkdgrpId()
	{
		return skdgrpId;
	}

	public void setSkdgrpId(int skdgrpId)
	{
		this.skdgrpId = skdgrpId;
	}

	public int getResdetId()
	{
		return resdetId;
	}

	public void setResdetId(int resdetId)
	{
		this.resdetId = resdetId;
	}

    public Integer getVoltypId()
    {
        return voltypId;
    }

    public void setVoltypId(Integer voltypId)
    {
        this.voltypId = voltypId;
    }

}
