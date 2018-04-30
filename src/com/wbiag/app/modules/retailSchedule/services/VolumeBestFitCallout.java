package com.wbiag.app.modules.retailSchedule.services;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;

public class VolumeBestFitCallout extends ScheduleCallout {
    
    public boolean addIRToMatrixPreAction(SOData soContext) throws CalloutException{
        VolumeBestFitProcess process = new VolumeBestFitProcess(); 
  	    return process.addIRToMatrixPreAction(soContext);
    }

    public boolean buildBestFitRequirementPreAction(SOData soContext) throws CalloutException{
        VolumeBestFitProcess process = new VolumeBestFitProcess();
        return process.buildBestFitRequirementPreAction(soContext);
    }    
    
    public void addIRToMatrixPostLoop(SOData soContext) throws CalloutException {
        VolumeBestFitProcess process = new VolumeBestFitProcess();
        process.addIRToMatrixPostLoop(soContext);
    }

    public boolean modifyIRPostOptimizationPreAction(SOData soContext) throws CalloutException {
        VolumeBestFitProcess process = new VolumeBestFitProcess();
        return process.modifyIRPostOptimizationPreAction(soContext);
    }

}
