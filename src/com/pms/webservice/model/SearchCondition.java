package com.pms.webservice.model;

import java.util.List;

public class SearchCondition {
	private String tableName;
	private String totalNum;
	private String onceNum;
	private String isAsyn;
	private String CONDITION;
	private List<String> CONDITIONITEMS;
	
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getTotalNum() {
		return totalNum;
	}
	public void setTotalNum(String totalNum) {
		this.totalNum = totalNum;
	}
	public String getOnceNum() {
		return onceNum;
	}
	public void setOnceNum(String onceNum) {
		this.onceNum = onceNum;
	}
	public String getIsAsyn() {
		return isAsyn;
	}
	public void setIsAsyn(String isAsyn) {
		this.isAsyn = isAsyn;
	}
	public String getCONDITION() {
		return CONDITION;
	}
	public void setCONDITION(String cONDITION) {
		CONDITION = cONDITION;
	}
	public List<String> getCONDITIONITEMS() {
		return CONDITIONITEMS;
	}
	public void setCONDITIONITEMS(List<String> cONDITIONITEMS) {
		CONDITIONITEMS = cONDITIONITEMS;
	}
	
}
