package com.wbiag.app.ta.password.model;

import com.workbrain.app.ta.model.RecordData;

public class WbiagUserPassAnswerData extends RecordData {
	private int wupaId;
	private int wbuId;
	private int wpqId;
	private String wupaAnswer;
	
	
	/**
	 * @return Returns the wpqId.
	 */
	public int getWpqId() {
		return wpqId;
	}
	/**
	 * @param wpqId The wpqId to set.
	 */
	public void setWpqId(int wpqId) {
		this.wpqId = wpqId;
	}
	/**
	 * @return Returns the huId.
	 */
	public int getWupaId() {
		return wupaId;
	}
	/**
	 * @param huId The huId to set.
	 */
	public void setWupaId(int wupaId) {
		this.wupaId = wupaId;
	}
	/**
	 * @return Returns the huPassAns.
	 */
	public String getWupaAnswer() {
		return wupaAnswer;
	}
	/**
	 * @param huPassAns The huPassAns to set.
	 */
	public void setWupaAnswer(String wupaAnswer) {
		this.wupaAnswer = wupaAnswer;
	}
	/**
	 * @return Returns the wbuId.
	 */
	public int getWbuId() {
		return wbuId;
	}
	/**
	 * @param wbuId The wbuId to set.
	 */
	public void setWbuId(int wbuId) {
		this.wbuId = wbuId;
	}

	
	/**
	 * 
	 */
	public WbiagUserPassAnswerData() {
		super();
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see com.workbrain.app.ta.model.RecordData#newInstance()
	 */
	public RecordData newInstance() {
		// TODO Auto-generated method stub
		return new WbiagUserPassAnswerData();
	}

}
