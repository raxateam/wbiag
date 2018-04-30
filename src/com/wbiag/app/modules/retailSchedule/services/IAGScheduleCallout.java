package com.wbiag.app.modules.retailSchedule.services;

import java.util.*;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import com.workbrain.app.modules.retailSchedule.exceptions.CalloutException;
import com.workbrain.app.modules.retailSchedule.services.EmployeeAssignmentOptions;
import com.workbrain.app.modules.retailSchedule.services.PreProcessorFSThread;
import com.workbrain.app.modules.retailSchedule.services.ScheduleCallout;
import com.workbrain.app.modules.retailSchedule.services.model.SOData;
import com.workbrain.app.modules.retailSchedule.services.model.CalloutHelper;
import com.workbrain.app.modules.retailSchedule.model.Employee;
import com.workbrain.app.modules.retailSchedule.utils.SOTime;
import com.workbrain.app.modules.retailSchedule.utils.SOTimeInterval;
import com.workbrain.app.modules.retailSchedule.exceptions.RetailException;
import com.workbrain.app.modules.retailSchedule.model.ScheduleGroupData;
import com.workbrain.app.modules.retailSchedule.model.CorporateEntity;
import com.workbrain.app.modules.retailSchedule.model.ShiftPatternReader;
import com.workbrain.app.ta.model.ShiftPatternShiftLaborData;
import com.workbrain.app.ta.model.ShiftData;
import com.workbrain.app.modules.retailSchedule.services.ShiftPattern;
import com.workbrain.app.modules.retailSchedule.utils.SODate;
import com.workbrain.sql.*;
import com.workbrain.app.ta.db.*;
import com.workbrain.app.ta.model.*;
import com.workbrain.app.ta.db.CodeMapper;
import com.workbrain.app.ta.db.ShiftPatternShiftLaborAccess;
import com.workbrain.app.ta.db.ShiftPatternShiftsAccess;
import com.workbrain.app.ta.model.ShiftPatternShiftsData;
import com.workbrain.server.registry.*;
import com.workbrain.util.DateHelper;

/**
 * Title: IAGScheduleCallout.java <br>
 * Description: TT44574 Call out class used to adjust the shift and shift
 * pattern shift labor according to store operation hours using the shift ype
 * and offset data predefined. <br>
 *
 * Created: Jun 20, 2005
 *
 * @author cleigh
 *         <p>
 *         Revision History <br>
 *         Jun 20, 2005 - Initial code base from version 0.1 of the technical
 *         documentation <br>
 *         <p>
 */
public class IAGScheduleCallout extends ScheduleCallout {

    public static final String DEF_SHIFT_TYPE_UDF = "SHFTPATSHFT_UDF1";
    public static final String DEF_SHIFT_OFFSET_UDF = "SHFTPATSHFT_UDF2";
    public static final String REG_PATH_CALLOUT = "/system/modules/scheduleOptimization/WBIAGScheduleCallout/";
    public static final String REG_SHIFT_TYPE_UDF = "ShiftTypeUDF";
    public static final String REG_SHIFT_OFFSET_UDF = "OffsetUDF";
    public static final String TYPE_OPEN = "OPEN";
    public static final String TYPE_CLOSE = "CLOSE";

    private DBConnection conn;
    private CodeMapper codeMapper;

    /*
     * (non-Javadoc)
     *
     * @see com.workbrain.app.modules.retailSchedule.services.ScheduleCallout#findWorkloadCoverForFixedShiftPostAction(com.workbrain.app.modules.retailSchedule.services.model.SOData)
     */
    public synchronized void findWorkloadCoverForFixedShiftPostAction(
            SOData soContext) throws CalloutException {

    	if (logger.isDebugEnabled()) {
            logger
                    .debug("IAGScheduleCallout.findWorkloadCoverForFixedShiftPostAction begin");
        }

        try {
            Employee emp;

            emp = CalloutHelper.getEmployee(soContext,
                    ScheduleCallout.FIND_WORKLOAD_COVER_FOR_FIXED_SHIFT);

            if (emp != null) {
                Date workDate;
                int shiftDateIndex;
                int offsetHOO;
                String shiftType;
                ShiftPatternShiftsData spsd;

                if (logger.isDebugEnabled()) {
                    logger.debug("Evaluating employee Id " + emp.getEmpId());
                }

                conn = soContext.getDBconnection();
                codeMapper = CodeMapper.createCodeMapper(conn);
                shiftDateIndex = CalloutHelper.getShiftDateIndx(soContext,
                        ScheduleCallout.FIND_WORKLOAD_COVER_FOR_FIXED_SHIFT);
                workDate = DateHelper.addDays(soContext.getSchedule()
                        .getSkdFromDate().getTime(), shiftDateIndex);

                if (logger.isDebugEnabled()){
                    logger.debug(new StringBuffer().append("Shift Date Index ").append(shiftDateIndex)
                            .append(" Work Date ").append(workDate));
                }

                spsd = getShiftPatternShiftDataByEmployee(emp.getEmpId(),
                        soContext, workDate);
                if (spsd == null) {
                    if (logger.isDebugEnabled()) {
                        logger
                                .debug("No shift pattern shifts found for employee");
                    }
                    return;
                }


                offsetHOO = getOffSetValue(spsd);
                shiftType = getShiftTypeValue(spsd);

                if ( shiftType != null) {

                    ScheduleGroupData sgd;
                    CorporateEntity ce;
                    Date ceOpenTime;
                    Date ceCloseTime;
                    int adjustMins;
                    List spsIdList;
                    List shiftTimes;
                    SOTimeInterval singleShift;

                    if (logger.isDebugEnabled()) {
                        logger.debug(new StringBuffer().append("Offset: ")
                                .append(offsetHOO).append(" Shift Type ")
                                .append(shiftType));
                    }
                    spsIdList = getShiftPatternShiftLabor(spsd);
                    shiftTimes = (ArrayList) soContext
                            .getLocalVariable(CalloutHelper.TYPE_LIST_EMP_ASSIGN_OPTION_MAPPING_TIMES);

                    singleShift = new SOTimeInterval();

                    if (shiftTimes != null){
                        shiftTimes.clear();
                    } else {
                        shiftTimes = new ArrayList();
                    }


                    sgd = CalloutHelper
                            .getScheduleGroupData(
                                    soContext,
                                    ScheduleCallout.FIND_WORKLOAD_COVER_FOR_FIXED_SHIFT);

                    if (sgd == null) {
                        if (logger.isDebugEnabled()){
                            logger.debug("Schedule Group Data is Null. Can not get Corporate Entity");
                        }

                    } else {
                        ce = sgd.getCorporateEntity();
                        SOTime ot = ce.getOpenTime(new SODate(workDate));
                        SOTime ct = ce.getCloseTime(new SODate(workDate));

                        if (ot == null || ct == null) {
                            if (logger.isDebugEnabled()) logger.debug("Store " + ce.getName() + " is closed on :" + workDate + ", exiting");
                            return;
                        }
                        ceOpenTime = ot.toDateObject();
                        ceCloseTime = ct.toDateObject();

                        if (logger.isDebugEnabled()) {
                            logger.debug(new StringBuffer().append(
                                    "Found Corporate Entity open / close times ")
                                    .append(ceOpenTime.toString()).append(" / ")
                                    .append(ceCloseTime));
                        }

                        int spsIdListSize = spsIdList.size();
                        if (spsIdListSize > 0) {
                            if (logger.isDebugEnabled()) {
                                logger
                                        .debug("Found shift pattern shifts list size: "
                                                + spsIdListSize);
                            }
                            for (int i = 0; i < spsIdListSize; i++) {
                                ShiftPatternShiftLaborData spsld = (ShiftPatternShiftLaborData) spsIdList
                                        .get(i);
                                adjustMins = getAdjustMinutes(offsetHOO, shiftType,
                                        ceOpenTime, ceCloseTime, spsld
                                                .getSpslabStartTime(), spsld
                                                .getSpslabEndTime());
                                singleShift.setStart(DateHelper.addMinutes(spsld
                                        .getSpslabStartTime(), adjustMins));
                                singleShift.setEnd(DateHelper.addMinutes(spsld
                                        .getSpslabEndTime(), adjustMins));
                                shiftTimes.add(singleShift);

                                if (logger.isDebugEnabled()) {
                                    logger.debug(new StringBuffer().append(
                                            "Added Shift Times: ").append(
                                            singleShift.toDisplayString()));
                                }
                            }

                        } else {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Using shift data for shift times");
                            }
                            ShiftData sd = codeMapper
                                    .getShiftById(spsd.getShftId());

                            adjustMins = getAdjustMinutes(offsetHOO, shiftType,
                                    ceOpenTime, ceCloseTime, sd.getShftStartTime(),
                                    sd.getShftEndTime());
                            singleShift.setStart(DateHelper.addMinutes(sd
                                    .getShftStartTime(), adjustMins));
                            singleShift.setEnd(DateHelper.addMinutes(sd
                                    .getShftEndTime(), adjustMins));
                            shiftTimes.add(singleShift);
                            if (logger.isDebugEnabled()) {
                                logger.debug(new StringBuffer().append(
                                        "Added Shift Times: ").append(
                                        singleShift.toDisplayString()));
                            }
                        }
                        soContext
                                .addLocalVariable(
                                        CalloutHelper.TYPE_LIST_EMP_ASSIGN_OPTION_MAPPING_TIMES,
                                        shiftTimes);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Added local variable to soContext "
                                    + shiftTimes.toString());
                        }
                    }

                	// bchan 04/05/2006 - Fix for TT86
                    List list = (List)soContext.getLocalVariable(PreProcessorFSThread.VAR_LIST_EMP_ASSIGN_OPTIONS);
                	for (int i = 0; i < list.size(); i ++) {
                    	EmployeeAssignmentOptions eao = (EmployeeAssignmentOptions)list.get(i);
                    	eao.setOptionInterval(singleShift);
                	}

                } else if (logger.isDebugEnabled()){
                    logger.debug(" Shift Type is null for shift pattern shift " + spsd.getShftpatshftId());
                }
            }

        } catch (RetailException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("An error has occured in IAGScheduleCallout", e);
            }
            throw new CalloutException("A Retail Exception occured", e);
        } catch (SQLException e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("Could not create code mapper", e);
            }
            throw new CalloutException("Error creating code mapper", e);
        } catch (Exception e) {
            if (logger.isEnabledFor(Level.ERROR)) {
                logger.error("An error occured IAGSchedule", e);
            }
            throw new CalloutException("Error occured in IAGSchedule", e);
        } finally {
            if (logger.isDebugEnabled()) {
                logger
                        .debug("IAGScheduleCallout.findWorkloadCoverForFixedShiftPostAction end");
            }
        }

    }

    private ShiftPatternShiftsData getShiftPatternShiftDataByEmployee(
            int empId, SOData soContext, Date workDate)
            throws CalloutException, SQLException, RetailException {
        ShiftPatternShiftsData result = null;
        int shftPatId;
        int shiftPatOffset;
        EmployeeAccess ea = new EmployeeAccess(conn, codeMapper);
        ShiftPatternShiftsAccess spsa = new ShiftPatternShiftsAccess(conn);

        if (logger.isDebugEnabled()) {
            logger
                    .debug("IAGScheduleCallout.getShiftPatternShiftDataByEmployee begin");
        }

        //ShiftPattern sp = CalloutHelper.getShiftPattern(soContext,
        // ScheduleCallout.FIND_WORKLOAD_COVER_FOR_FIXED_SHIFT);
        EmployeeData ed = ea.load(empId, workDate);
        shiftPatOffset = ed.getEmpShftpatOffset();

        if (logger.isDebugEnabled()) {
            logger.debug(new StringBuffer().append("Shift Pattern offset: ").append(
                    shiftPatOffset));
        }

        shftPatId = ed.getShftpatId();
        result = spsa.loadByShiftPatDatOffset(shftPatId, workDate,
                shiftPatOffset);

        if (logger.isDebugEnabled()) {
            logger
                    .debug("IAGScheduleCallout.getShiftPatternShiftDataByEmployee end");
        }
        return result;
    }

    private List getShiftPatternShiftLabor(ShiftPatternShiftsData spsd) {
        List result = null;
        int shftPatId;
        int shftPatShftId;

        if (logger.isDebugEnabled()) {
            logger.debug("IAGScheduleCallout.getShiftPatternShiftLabor begin");
        }
        shftPatId = spsd.getShftpatId();
        shftPatShftId = spsd.getShftpatshftId();
        if (logger.isDebugEnabled()) {
            logger.debug(new StringBuffer().append(
                    "Using Shift Pattern Id/Shift Pattern Shift Id ").append(
                    shftPatId).append(" / ").append(shftPatShftId));
        }
        result = codeMapper.getShiftPatternShiftLaborByShftpatShftId(shftPatId,
                shftPatShftId);
        if (logger.isDebugEnabled()) {
            logger.debug("IAGScheduleCallout.getShiftPatternShiftLabor end");
        }
        return result;
    }

    private int getAdjustMinutes(int offset, String shiftType, Date openTime,
            Date closeTime, Date startTime, Date endTime) {
        int adjustMin = 0;

        if (logger.isDebugEnabled()) {
            logger.debug("IAGScheduleCallout.getAdjustMinutes begin");
        }
        if (TYPE_OPEN.equals(shiftType)) {
            if (logger.isDebugEnabled()) {
                logger.debug(new StringBuffer().append(
                        "Calc adj. mins for TYPE_OPEN. Shift: ").append(
                        startTime).append(" - ").append(endTime).append(" Store: ").append(
                        openTime).append(" - ").append(closeTime).append(
                        " Offset ").append(offset));
            }
            adjustMin = (int) DateHelper.getMinutesBetween(openTime, startTime) + offset;
        } else if (TYPE_CLOSE.equals(shiftType)) {
            if (logger.isDebugEnabled()) {
                logger.debug(new StringBuffer().append(
                        "Calc adj mins for TYPE_CLOSE. Shift: ").append(
                        startTime).append(" - ").append(endTime).append(" Store: ").append(
                        openTime).append(" - ").append(closeTime).append(
                        " Offset ").append(offset));
            }
            adjustMin = (int) DateHelper.getMinutesBetween(closeTime, endTime) + offset;
        }

        if (logger.isDebugEnabled()) {
            logger.debug(new StringBuffer().append("Found adjust minutes: ")
                    .append(adjustMin));
            logger.debug("IAGScheduleCallout.getAdjustMinutes end");
        }
        return adjustMin;
    }

    private int getOffSetValue(ShiftPatternShiftsData spsd) {
        int offset = 0;
        String feild;
        Object tmpValue;

        feild = Registry.getVarString(REG_PATH_CALLOUT + REG_SHIFT_OFFSET_UDF,
                DEF_SHIFT_OFFSET_UDF);
        tmpValue = spsd.getField(feild);
        if (tmpValue != null) {
            offset = Integer.parseInt(tmpValue.toString());
        }

        return offset;
    }

    private String getShiftTypeValue(ShiftPatternShiftsData spsd) {
        String type = null;
        String feild;
        Object tmpValue;

        feild = Registry.getVarString(REG_PATH_CALLOUT + REG_SHIFT_TYPE_UDF,
                DEF_SHIFT_TYPE_UDF);
        tmpValue = spsd.getField(feild);
        if (tmpValue != null) {
            type = tmpValue.toString();
        }

        return type;
    }

}
