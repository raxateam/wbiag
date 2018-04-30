
package com.wbiag.app.modules.retailSchedule.services.volumeBestFit.model;

import java.util.Date;

import com.workbrain.app.ta.model.RecordData;

public class VolumeBestFitData extends RecordData {

    private int vbfId;
    private int csdId;
    private int vbfDayIndex;
    private Date startTime;
    private Date endTime;
    private int driverSkdgrpId;
    private double wrkldStdvolHour;
    
    public RecordData newInstance() {
        return new VolumeBestFitData();
    }

    public int getCsdId() {
        return csdId;
    }

    public void setCsdId(int csdId) {
        this.csdId = csdId;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

   
    public int getDriverSkdgrpId() {
        return driverSkdgrpId;
    }

    public void setDriverSkdgrpId(int driverSkdgrpId) {
        this.driverSkdgrpId = driverSkdgrpId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public double getWrkldStdvolHour() {
        return wrkldStdvolHour;
    }

    public void setWrkldStdvolHour(double wrkldStdvolHour) {
        this.wrkldStdvolHour = wrkldStdvolHour;
    }

    public int getVbfDayIndex() {
        return vbfDayIndex;
    }

    public void setVbfDayIndex(int vbfDayIndex) {
        this.vbfDayIndex = vbfDayIndex;
    }

    public int getVbfId() {
        return vbfId;
    }

    public void setVbfId(int vbfId) {
        this.vbfId = vbfId;
    }
    

}
