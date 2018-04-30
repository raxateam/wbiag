package com.wbiag.app.modules.leaverequest;

import com.workbrain.sql.*;
import com.workbrain.util.*;
import java.util.*;
import com.workbrain.app.ta.db.OverrideAccess;
import com.workbrain.app.ta.model.OverrideData;
import com.workbrain.tool.overrides.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


public class LeaveCanceller extends LeaveProcessCommon {

  private static org.apache.log4j.Logger logger =
    org.apache.log4j.Logger.getLogger(LeaveCanceller.class);

  private String cancelOvrString = null;
  private List cancelOvrList = null;

  public LeaveCanceller(DBConnection con , String ovrListString) throws Exception {
    super(con);
    this.cancelOvrString = ovrListString;
    this.createCancelOvrList();
  }

  public void createCancelOvrList(){
    if (StringHelper.isEmpty(cancelOvrString)) {
      cancelOvrList = new ArrayList();
      return;
    }
    OverrideAccess oa = new OverrideAccess (this.getConnection());
    cancelOvrList =  oa.loadRecordData(new OverrideData(),
        OverrideAccess.OVERRIDE_TABLE ,
        " ovr_id in (" + cancelOvrString + ") AND ovr_status = 'APPLIED'");
    oa = null;
  }

  public List getCancelOvrList(){
    return cancelOvrList;
  }

  public void process() throws Exception {
    if ((this.cancelOvrList == null) || (this.cancelOvrList.size() == 0)){
      this.setError(this.localize(LeaveMsgHelper.CANCEL_LVE_NO_LEAVE_OVRS));
      throw new Exception(this.getError());
    }
    try {
      this.setOverridesStatus();
      // *** already taken care by OverrideBuilder
      //this.calculateScheduleForCancel(cancelOvrList);
      if (logger.isEnabledFor(org.apache.log4j.Level.DEBUG)) { logger.debug("Successfully CANCELLED " + cancelOvrList.size() + " override(s)");}
      cancelOvrList.clear();
      cancelOvrList = null;

    } catch(Exception e){
      if (logger.isEnabledFor(org.apache.log4j.Level.ERROR)) { logger.error("com.workbrain.app.modules.vacationrequest.common.CancelVacation.class" , e);}
      this.setError(this.localize(LeaveMsgHelper.CANCEL_LVE_FAILED_DELETE_OVERRIDES) +
          this.getEmployeeFullName());
      throw new Exception(this.getError() + "\n" + e.getMessage());
    }
  }

  private void setOverridesStatus() throws Exception {
    OverrideBuilder ob = new OverrideBuilder(this.getConnection());

    Date createDate = null;
    int size = cancelOvrList.size();
    StringBuffer premOvrIds = new StringBuffer("");
    for (int i = 0; i < size; i++) {
      OverrideData ovrData = (OverrideData)cancelOvrList.get(i);
      DeleteOverride dov = new DeleteOverride();
      dov.setWbuNameBoth(getUserName() , getUserName());
      dov.setOverrideId(ovrData.getOvrId());
      /* get the related premium override ids for pay currents */
      if (!StringHelper.isEmpty(ovrData.getOvrUdf1())) {
        if (i > 0) premOvrIds.append(",");
        premOvrIds.append(ovrData.getOvrUdf1());
      }
      log("Cancelled " + size + " LTA overrides");
      ob.add(dov);
    }
    /* cancel related premium overrides if any*/
    if (premOvrIds.length() > 0) {
      String sql = "SELECT ovr_id FROM override"
        + " WHERE ovr_id IN (" + premOvrIds.toString() + ")"
        + " AND ovr_status = 'APPLIED'";
      PreparedStatement ps = null;
      ResultSet rs = null;
      try {
        ps = getConnection().prepareStatement(sql);
        rs = ps.executeQuery();
        int cnt = 0;
        while (rs.next()) {
          DeleteOverride dov = new DeleteOverride();
          dov.setOverrideId(rs.getInt(1));
          dov.setWbuNameBoth(getUserName() , getUserName());
          ob.add(dov);
          cnt++;
        }
        log("Cancelled " + cnt + " premium overrides");
      } finally {
        if (ps!=null) ps.close();
        if (rs!=null) rs.close();
      }
    }

    ob.execute(true , false);
    ob = null;
  }

}
