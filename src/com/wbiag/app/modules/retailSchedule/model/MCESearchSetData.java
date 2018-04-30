/**
 * Description: WBIAG_MCE_SRCHSET table set/get
 * Date: June 1, 2006
 * Author: shlee
 */

package com.wbiag.app.modules.retailSchedule.model;
import com.workbrain.app.ta.model.RecordData;

	public class MCESearchSetData extends RecordData {
		private int ssId = -1000;				//PK of WBIAG_MCE_SRCHSET
		private String ssName;	
		private String ssDesc;                 // Search Set Description
		private Integer ssType;			//1 or 2
		private Integer ssHiernode;			//FK to the PK of SO_SCHEDULE_GROUP
		private Integer clientTypeId;		//FK to the PK of SO_CLIENT_TYPE
		private String ssLoclist;
		
		private Integer ss1Id;		 		//FK to PK of BBY_MCE_SRCHSET
		private Integer ss2Id;
		private Integer ss3Id;
		private Integer ss4Id;
		private Integer ss5Id;
					
		private Integer ssProp1Id;	//FK to PK of SO_PROPERTY
		private String ssMin1;
		private String ssMax1;
		
		private Integer ssProp2Id;	//FK to PK of SO_PROPERTY
		private String ssMin2;
		private String ssMax2;
		
		private Integer ssProp3Id;	//FK to PK of SO_PROPERTY
		private String ssMin3;
		private String ssMax3;
		
		private Integer ssProp4Id;	//FK to PK of SO_PROPERTY
		private String ssMin4;
		private String ssMax4;
		
		private Integer ssProp5Id;	//FK to PK of SO_PROPERTY
		private String ssMin5;
		private String ssMax5;
		
		private Integer ssProp6Id;	//FK to PK of SO_PROPERTY
		private String ssMin6;
		private String ssMax6;
		
		private Integer ssProp7Id;	//FK to PK of SO_PROPERTY
		private String ssMin7;
		private String ssMax7;
		
		/* (non-Javadoc)
		 * @see com.workbrain.app.ta.model.RecordData#newInstance()
		 */
		public RecordData newInstance() {
			return new MCESearchSetData();
		}
		
		public String toString(){
			String s = "MCESearchSetData:\n" +
			"  ssId = " + ssId + "\n" +
			"  ssName = " + ssName + "\n" +
			"  ssDesc = " + ssDesc + "\n" +
			"  ssType = " + ssType + "\n" +
			"  ssHiernode = " + ssHiernode + "\n" +
			"  clientTypeId = " + clientTypeId + "\n" +	
			"  ssLoclist = " + ssLoclist + "\n" +
			"  ss1Id = " + ss1Id + "\n" +
			"  ss2Id = " + ss2Id + "\n" +
			"  ss3Id = " + ss3Id + "\n" +
			"  ss4Id = " + ss4Id + "\n" +
			"  ss5Id = " + ss5Id + "\n" +
			"  ssProp1Id = " + ssProp1Id + "\n" +
			"  ssMin1 = " + ssMin1 + "\n" +
			"  ssMax1 = " + ssMax1 + "\n" +
			"  ssProp2Id = " + ssProp2Id + "\n" +
			"  ssMin2 = " + ssMin2 + "\n" +
			"  ssMax2 = " + ssMax2 + "\n" +
			"  ssProp3Id = " + ssProp3Id + "\n" +
			"  ssMin3 = " + ssMin3 + "\n" +
			"  ssMax3 = " + ssMax3 + "\n" +
			"  ssProp4Id = " + ssProp4Id + "\n" +
			"  ssMin4 = " + ssMin4 + "\n" +
			"  ssMax4 = " + ssMax4 + "\n" +
			"  ssProp5Id = " + ssProp5Id + "\n" +
			"  ssMin5 = " + ssMin5 + "\n" +
			"  ssMax5 = " + ssMax5 + "\n" +
			"  ssProp6Id = " + ssProp6Id + "\n" +
			"  ssMin6 = " + ssMin6 + "\n" +
			"  ssMax6 = " + ssMax6 + "\n" +
			"  ssProp7 = " + ssProp7Id + "\n" +
			"  ssMin7 = " + ssMin7 + "\n" +
			"  ssMax7 = " + ssMax7 + "\n";		
			return s;
		}
		
		/**
		 * @return Returns the MCESS_ID.
		 */
		public int getWimcessId() {
			return ssId;
		}
		/**
		 * @param MCESS_ID The MCESS_ID to set.
		 */
		public void setWimcessId(int mcessId) {
			this.ssId = mcessId;
		}
		/**
		 * @return Returns the MCESS_NAME.
		 */
		public String getWimcessName() {
			return ssName;
		}
		/**
		 * @param MCESS_NAME The MCESS_NAME to set.
		 */
		public void setWimcessName(String mcessName) {
			this.ssName = mcessName;
		}
		
		/**
		 * @return Returns the MCESS_DESC.
		 */
		public String getWimcessDesc() {
			return ssDesc;
		}
		/**
		 * @param MCESS_DESC The MCESS_DESC to set.
		 */
		public void setWimcessDesc(String mcessDesc) {
			this.ssDesc = mcessDesc;
		}
		
		/**
		 * @return Returns the MCESS_TYPE.
		 */
		public Integer getWimcessType() {
			return ssType;
		}
		/**
		 * @param MCESS_TYPE The MCESS_TYPE to set.
		 */
		public void setWimcessType(Integer mcessType) {
			this.ssType = mcessType;
		}
		
		/**
		 * @return Returns the MCESS_HIERNODE.
		 */
		public Integer getWimcessHiernode() {
			return ssHiernode;
		}
		/**
		 * @param MCESS_HIERNODE The MCESS_HIERNODE to set.
		 */
		public void setWimcessHiernode(Integer mcessHiernode) {
			this.ssHiernode = mcessHiernode;
		}

		/**
		 * @return Returns the CLNTTYP_ID.
		 */
		public Integer getClnttypId() {
			return clientTypeId;
		}
		/**
		 * @param CLNTTYPE_ID The CLNTTYP_ID to set.
		 */
		public void setClnttypId(Integer clntTypeId) {
			this.clientTypeId = clntTypeId;
		}
		
        public String getWimcessLoclist(){
            return this.ssLoclist;
        }
        
        public void setWimcessLoclist(String v){
            this.ssLoclist = v;
        }
		/**
		 * @return Returns the MCESS_SUB1_ID.
		 */
		public Integer getWimcessSub1Id() {
			return ss1Id;
		}
		/**
		 * @param MCESS_SUB1_ID The MCESS_SUB1_ID to set.
		 */
		public void setWimcessSub1Id(Integer mcessId) {
			this.ss1Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_SUB2_ID.
		 */
		public Integer getWimcessSub2Id() {
			return ss2Id;
		}
		/**
		 * @param MCESS_SUB2_ID The MCESS_SUB2_ID to set.
		 */
		public void setWimcessSub2Id(Integer mcessId) {
			this.ss2Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_SUB3_ID.
		 */
		public Integer getWimcessSub3Id() {
			return ss3Id;
		}
		/**
		 * @param MCESS_SUB3_ID The MCESS_SUB3_ID to set.
		 */
		public void setWimcessSub3Id(Integer mcessId) {
			this.ss3Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_SUB4_ID.
		 */
		public Integer getWimcessSub4Id() {
			return ss4Id;
		}
		/**
		 * @param MCESS_SUB4_ID The MCESS_SUB4_ID to set.
		 */
		public void setWimcessSub4Id(Integer mcessId) {
			this.ss4Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_SUB5_ID.
		 */
		public Integer getWimcessSub5Id() {
			return ss5Id;
		}
		/**
		 * @param MCESS_SUB5_ID The MCESS_SUB5_ID to set.
		 */
		public void setWimcessSub5Id(Integer mcessId) {
			this.ss5Id = mcessId;
		}
		
		/**
		 * @return Returns the MCESS_PROP1_ID.
		 */
		public Integer getWimcessProp1Id() {
			return ssProp1Id;
		}
		/**
		 * @param MCESS_PROP1_ID The MCESS_PROP1_ID to set.
		 */
		public void setWimcessProp1Id(Integer mcessId) {
			this.ssProp1Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_MIN1.
		 */
		public String getWimcessMin1() {
			return ssMin1;
		}
		/**
		 * @param MCESS_MIN1 The MCESS_MIN1 to set.
		 */
		public void setWimcessMin1(String min) {
			this.ssMin1 = min;
		}
		/**
		 * @return Returns the MCESS_MAX1.
		 */
		public String getWimcessMax1() {
			return ssMax1;
		}
		/**
		 * @param MCESS_MAX1 The MCESS_MAX1 to set.
		 */
		public void setWimcessMax1(String max) {
			this.ssMax1 = max;
		}
		/**
		 * @return Returns the MCESS_PROP2_ID.
		 */
		public Integer getWimcessProp2Id() {
			return ssProp2Id;
		}
		/**
		 * @param MCESS_PROP2_ID The MCESS_PROP2_ID to set.
		 */
		public void setWimcessProp2Id(Integer mcessId) {
			this.ssProp2Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_MIN2.
		 */
		public String getWimcessMin2() {
			return ssMin2;
		}
		/**
		 * @param MCESS_MIN2 The MCESS_MIN2 to set.
		 */
		public void setWimcessMin2(String min) {
			this.ssMin2 = min;
		}
		/**
		 * @return Returns the MCESS_MAX2.
		 */
		public String getWimcessMax2() {
			return ssMax2;
		}
		/**
		 * @param MCESS_MAX2 The MCESS_MAX2 to set.
		 */
		public void setWimcessMax2(String max) {
			this.ssMax2 = max;
		}
		
		
		/**
		 * @return Returns the MCESS_PROP3_ID.
		 */
		public Integer getWimcessProp3Id() {
			return ssProp3Id;
		}
		/**
		 * @param MCESS_PROP3_ID The MCESS_PROP3_ID to set.
		 */
		public void setWimcessProp3Id(Integer mcessId) {
			this.ssProp3Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_MIN3.
		 */
		public String getWimcessMin3() {
			return ssMin3;
		}
		/**
		 * @param MCESS_MIN3 The MCESS_MIN3 to set.
		 */
		public void setWimcessMin3(String min) {
			this.ssMin3 = min;
		}
		/**
		 * @return Returns the MCESS_MAX3.
		 */
		public String getWimcessMax3() {
			return ssMax3;
		}
		/**
		 * @param MCESS_MAX3 The MCESS_MAX3 to set.
		 */
		public void setWimcessMax3(String max) {
			this.ssMax3 = max;
		}
		
		/**
		 * @return Returns the MCESS_PROP4_ID.
		 */
		public Integer getWimcessProp4Id() {
			return ssProp4Id;
		}
		/**
		 * @param MCESS_PROP4_ID The MCESS_PROP4_ID to set.
		 */
		public void setWimcessProp4Id(Integer mcessId) {
			this.ssProp4Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_MIN4.
		 */
		public String getWimcessMin4() {
			return ssMin4;
		}
		/**
		 * @param MCESS_MIN4 The MCESS_MIN4 to set.
		 */
		public void setWimcessMin4(String min) {
			this.ssMin4 = min;
		}
		/**
		 * @return Returns the MCESS_MAX4.
		 */
		public String getWimcessMax4() {
			return ssMax4;
		}
		/**
		 * @param MCESS_MAX4 The MCESS_MAX4 to set.
		 */
		public void setWimcessMax4(String max) {
			this.ssMax4 = max;
		}
		
		/**
		 * @return Returns the MCESS_PROP5_ID.
		 */
		public Integer getWimcessProp5Id() {
			return ssProp5Id;
		}
		/**
		 * @param MCESS_PROP5_ID The MCESS_PROP5_ID to set.
		 */
		public void setWimcessProp5Id(Integer mcessId) {
			this.ssProp5Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_MIN5.
		 */
		public String getWimcessMin5() {
			return ssMin5;
		}
		/**
		 * @param MCESS_MIN5 The MCESS_MIN5 to set.
		 */
		public void setWimcessMin5(String min) {
			this.ssMin5 = min;
		}
		/**
		 * @return Returns the MCESS_MAX5.
		 */
		public String getWimcessMax5() {
			return ssMax5;
		}
		/**
		 * @param MCESS_MAX5 The MCESS_MAX5 to set.
		 */
		public void setWimcessMax5(String max) {
			this.ssMax5 = max;
		}
		
		/**
		 * @return Returns the MCESS_PROP6_ID.
		 */
		public Integer getWimcessProp6Id() {
			return ssProp6Id;
		}
		/**
		 * @param MCESS_PROP6_ID The MCESS_PROP6_ID to set.
		 */
		public void setWimcessProp6Id(Integer mcessId) {
			this.ssProp6Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_MIN6.
		 */
		public String getWimcessMin6() {
			return ssMin6;
		}
		/**
		 * @param MCESS_MIN6 The MCESS_MIN6 to set.
		 */
		public void setWimcessMin6(String min) {
			this.ssMin6 = min;
		}
		/**
		 * @return Returns the MCESS_MAX6.
		 */
		public String getWimcessMax6() {
			return ssMax6;
		}
		/**
		 * @param MCESS_MAX6 The MCESS_MAX6 to set.
		 */
		public void setWimcessMax6(String max) {
			this.ssMax6 = max;
		}
		
		
		/**
		 * @return Returns the MCESS_PROP7_ID.
		 */
		public Integer getWimcessProp7Id() {
			return ssProp7Id;
		}
		/**
		 * @param MCESS_PROP7_ID The MCESS_PROP7_ID to set.
		 */
		public void setWimcessProp7Id(Integer mcessId) {
			this.ssProp7Id = mcessId;
		}
		/**
		 * @return Returns the MCESS_MIN7.
		 */
		public String getWimcessMin7() {
			return ssMin7;
		}
		/**
		 * @param MCESS_MIN7 The MCESS_MIN7 to set.
		 */
		public void setWimcessMin7(String min) {
			this.ssMin7 = min;
		}
		/**
		 * @return Returns the MCESS_MAX7.
		 */
		public String getWimcessMax7() {
			return ssMax7;
		}
		/**
		 * @param MCESS_MAX7 The MCESS_MAX7 to set.
		 */
		public void setWimcessMax7(String max) {
			this.ssMax7 = max;
		}
		
}
