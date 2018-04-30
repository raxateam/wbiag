package com.wbiag.app.modules.retailSchedule.services;

import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.Schedule;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;

public class PreScheduledMealsScheduleCallout extends ScheduleCallout {

    /**
     * End of post optimization process which reads in result file from
     * Optimizer. At this point, the result file has been read in entirely and
     * schedule populated.
     * 
     * This callout takes the processed schedule and examines each shift detail.
     * Fixed employee's meals are adjusted according to the meal
     * 
     * @author acaplan
     * @param soContext
     *            global context object used to access internal SO data.
     */
    public void populateSchedulePostLoop(SOData soContext) throws CalloutException {
        try {
            PreScheduledMealProcessor mealProcessor = new PreScheduledMealProcessor();
            Schedule skd = soContext.getSchedule();
            mealProcessor.init(skd, soContext.getDBconnection());
            mealProcessor.process(skd, soContext.getDBconnection());
        } catch (RetailException e) {
            throw new CalloutException(e.getMessage(), e);
        }
    }
}
