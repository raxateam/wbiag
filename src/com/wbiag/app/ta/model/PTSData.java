package com.wbiag.app.ta.model;

import java.util.Date;

import com.workbrain.app.ta.model.RecordData;

/** 
 * Title:			PTS Data
 * Description:		This class creates a PTS Data object which represents one row in the PAYROLL_T0_SALES table
 * Copyright:		Copyright (c) 2005
 * Company:        	Workbrain Inc
 * Created: 		Apr 21, 2005
 * @author         	Kevin Tsoi
 */
public class PTSData extends RecordData
{
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PTSData.class);
    
    public static final String TABLE_NAME = "payroll_to_sales";
    public static final String PTS_ID = "pts_id";
    public static final String SKDGRP_ID = "skdgrp_id";
    public static final String PTS_STORE_SKDGRP_NAME = "pts_store_skdgrp_name";
    public static final String PTS_WORK_DATE = "pts_workdate";
    public static final String PTS_TYPE = "pts_type";
    public static final String PTS_CATEGORY = "pts_category";
    public static final String PTS_VALUE = "pts_value";
    public static final String PTS_FLAG1 = "pts_flag1";
    public static final String PTS_FLAG2 = "pts_flag2";
    public static final String PTS_FLAG3 = "pts_flag3";
    public static final String PTS_FLAG4 = "pts_flag4";
    public static final String PTS_FLAG5 = "pts_flag5";
    public static final String PTS_UDF1 = "pts_UDF1";
    public static final String PTS_UDF2 = "pts_UDF2";
    public static final String PTS_UDF3 = "pts_UDF3";
    public static final String PTS_UDF4 = "pts_UDF4";
    public static final String PTS_UDF5 = "pts_UDF5";
    public static final String PTS_UDF6 = "pts_UDF6";
    public static final String PTS_UDF7 = "pts_UDF7";
    public static final String PTS_UDF8 = "pts_UDF8";
    public static final String PTS_UDF9 = "pts_UDF9";
    public static final String PTS_UDF10 = "pts_UDF10";
    public static final String SEQ_PTS_ID = "seq_pts_id";
    
    //PTS table fields
    private int ptsId;
    private int skdgrpId;
    private String ptsStoreSkdgrpName;
    private Date ptsWorkdate;
    private String ptsType;
    private String ptsCategory;
    private double ptsValue;
    private String ptsFlag1;
    private String ptsFlag2;
    private String ptsFlag3;
    private String ptsFlag4;
    private String ptsFlag5;
    private String ptsUdf1;
    private String ptsUdf2;
    private String ptsUdf3;
    private String ptsUdf4;
    private String ptsUdf5;
    private String ptsUdf6;
    private String ptsUdf7;
    private String ptsUdf8;
    private String ptsUdf9;
    private String ptsUdf10;
    
    /* (non-Javadoc)
     * @see com.workbrain.app.ta.model.RecordData#newInstance()
     */
    public RecordData newInstance()
    {
        return new PTSData();       
    }
    

    /**
     * @return Returns the ptsCategory.
     */
    public String getPtsCategory()
    {
        return ptsCategory;
    }
    /**
     * @param ptsCategory The ptsCategory to set.
     */
    public void setPtsCategory(String ptsCategory)
    {
        this.ptsCategory = ptsCategory;
    }
    /**
     * @return Returns the ptsFlag1.
     */
    public String getPtsFlag1()
    {
        return ptsFlag1;
    }
    /**
     * @param ptsFlag1 The ptsFlag1 to set.
     */
    public void setPtsFlag1(String ptsFlag1)
    {
        this.ptsFlag1 = ptsFlag1;
    }
    /**
     * @return Returns the ptsFlag2.
     */
    public String getPtsFlag2()
    {
        return ptsFlag2;
    }
    /**
     * @param ptsFlag2 The ptsFlag2 to set.
     */
    public void setPtsFlag2(String ptsFlag2)
    {
        this.ptsFlag2 = ptsFlag2;
    }
    /**
     * @return Returns the ptsFlag3.
     */
    public String getPtsFlag3()
    {
        return ptsFlag3;
    }
    /**
     * @param ptsFlag3 The ptsFlag3 to set.
     */
    public void setPtsFlag3(String ptsFlag3)
    {
        this.ptsFlag3 = ptsFlag3;
    }
    /**
     * @return Returns the ptsFlag4.
     */
    public String getPtsFlag4()
    {
        return ptsFlag4;
    }
    /**
     * @param ptsFlag4 The ptsFlag4 to set.
     */
    public void setPtsFlag4(String ptsFlag4)
    {
        this.ptsFlag4 = ptsFlag4;
    }
    /**
     * @return Returns the ptsFlag5.
     */
    public String getPtsFlag5()
    {
        return ptsFlag5;
    }
    /**
     * @param ptsFlag5 The ptsFlag5 to set.
     */
    public void setPtsFlag5(String ptsFlag5)
    {
        this.ptsFlag5 = ptsFlag5;
    }
    /**
     * @return Returns the ptsId.
     */
    public int getPtsId()
    {
        return ptsId;
    }
    /**
     * @param ptsId The ptsId to set.
     */
    public void setPtsId(int ptsId)
    {
        this.ptsId = ptsId;
    }
    /**
     * @return Returns the ptsStoreSkdgrpName.
     */
    public String getPtsStoreSkdgrpName()
    {
        return ptsStoreSkdgrpName;
    }
    /**
     * @param ptsStoreSkdgrpName The ptsStoreSkdgrpName to set.
     */
    public void setPtsStoreSkdgrpName(String ptsStoreSkdgrpName)
    {
        this.ptsStoreSkdgrpName = ptsStoreSkdgrpName;
    }
    /**
     * @return Returns the ptsType.
     */
    public String getPtsType()
    {
        return ptsType;
    }
    /**
     * @param ptsType The ptsType to set.
     */
    public void setPtsType(String ptsType)
    {
        this.ptsType = ptsType;
    }
    /**
     * @return Returns the ptsUdf1.
     */
    public String getPtsUdf1()
    {
        return ptsUdf1;
    }
    /**
     * @param ptsUdf1 The ptsUdf1 to set.
     */
    public void setPtsUdf1(String ptsUdf1)
    {
        this.ptsUdf1 = ptsUdf1;
    }
    /**
     * @return Returns the ptsUdf10.
     */
    public String getPtsUdf10()
    {
        return ptsUdf10;
    }
    /**
     * @param ptsUdf10 The ptsUdf10 to set.
     */
    public void setPtsUdf10(String ptsUdf10)
    {
        this.ptsUdf10 = ptsUdf10;
    }
    /**
     * @return Returns the ptsUdf2.
     */
    public String getPtsUdf2()
    {
        return ptsUdf2;
    }
    /**
     * @param ptsUdf2 The ptsUdf2 to set.
     */
    public void setPtsUdf2(String ptsUdf2)
    {
        this.ptsUdf2 = ptsUdf2;
    }
    /**
     * @return Returns the ptsUdf3.
     */
    public String getPtsUdf3()
    {
        return ptsUdf3;
    }
    /**
     * @param ptsUdf3 The ptsUdf3 to set.
     */
    public void setPtsUdf3(String ptsUdf3)
    {
        this.ptsUdf3 = ptsUdf3;
    }
    /**
     * @return Returns the ptsUdf4.
     */
    public String getPtsUdf4()
    {
        return ptsUdf4;
    }
    /**
     * @param ptsUdf4 The ptsUdf4 to set.
     */
    public void setPtsUdf4(String ptsUdf4)
    {
        this.ptsUdf4 = ptsUdf4;
    }
    /**
     * @return Returns the ptsUdf5.
     */
    public String getPtsUdf5()
    {
        return ptsUdf5;
    }
    /**
     * @param ptsUdf5 The ptsUdf5 to set.
     */
    public void setPtsUdf5(String ptsUdf5)
    {
        this.ptsUdf5 = ptsUdf5;
    }
    /**
     * @return Returns the ptsUdf6.
     */
    public String getPtsUdf6()
    {
        return ptsUdf6;
    }
    /**
     * @param ptsUdf6 The ptsUdf6 to set.
     */
    public void setPtsUdf6(String ptsUdf6)
    {
        this.ptsUdf6 = ptsUdf6;
    }
    /**
     * @return Returns the ptsUdf7.
     */
    public String getPtsUdf7()
    {
        return ptsUdf7;
    }
    /**
     * @param ptsUdf7 The ptsUdf7 to set.
     */
    public void setPtsUdf7(String ptsUdf7)
    {
        this.ptsUdf7 = ptsUdf7;
    }
    /**
     * @return Returns the ptsUdf8.
     */
    public String getPtsUdf8()
    {
        return ptsUdf8;
    }
    /**
     * @param ptsUdf8 The ptsUdf8 to set.
     */
    public void setPtsUdf8(String ptsUdf8)
    {
        this.ptsUdf8 = ptsUdf8;
    }
    /**
     * @return Returns the ptsUdf9.
     */
    public String getPtsUdf9()
    {
        return ptsUdf9;
    }
    /**
     * @param ptsUdf9 The ptsUdf9 to set.
     */
    public void setPtsUdf9(String ptsUdf9)
    {
        this.ptsUdf9 = ptsUdf9;
    }
    /**
     * @return Returns the ptsValue.
     */
    public double getPtsValue()
    {
        return ptsValue;
    }
    /**
     * @param ptsValue The ptsValue to set.
     */
    public void setPtsValue(double ptsValue)
    {
        this.ptsValue = ptsValue;
    }
    /**
     * @return Returns the ptsWorkdate.
     */
    public Date getPtsWorkdate()
    {
        return ptsWorkdate;
    }
    /**
     * @param ptsWorkdate The ptsWorkdate to set.
     */
    public void setPtsWorkdate(Date ptsWorkdate)
    {
        this.ptsWorkdate = ptsWorkdate;
    }
    /**
     * @return Returns the skdgrpId.
     */
    public int getSkdgrpId()
    {
        return skdgrpId;
    }
    /**
     * @param skdgrpId The skdgrpId to set.
     */
    public void setSkdgrpId(int skdgrpId)
    {
        this.skdgrpId = skdgrpId;
    }
}
