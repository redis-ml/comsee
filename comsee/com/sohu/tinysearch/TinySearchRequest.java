package com.sohu.tinysearch;

import com.sohu.common.connectionpool.async.AsyncRequest;

public class TinySearchRequest extends AsyncRequest{

	private volatile String query = null;
	private volatile String type = null;
	private volatile String reqType = null;
	private volatile int pornlevel = 0;

	private volatile boolean isIME = false;
	
	
	/**
	 * @param isIME 设置
	 */
	public void setIME(boolean v) {
		this.isIME = v;
	}
	/**
	 * @return the isIME
	 */
	public boolean isIME() {
		return isIME;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	public String getQuery(){
		return query;
	}
	public void setQuery(String query){
		this.query = query;
	}

	public int getServerId(int total) {
		if( total <= 0 ) return -1;
		String query = this.query;
		if( query == null ) // 这里允许查询词为空，isValid来决定需不需要空的查询词
			return 0;
		else
			return Math.abs(query.hashCode() % total);
	}
	public boolean isValid() {
		return this.query!=null && this.query.length()>0;
	}
	public int getPornlevel() {
		return pornlevel;
	}
	public void setPornlevel(int pornlevel) {
		this.pornlevel = pornlevel;
	}
	public String getReqType() {
		return reqType;
	}
	public void setReqType(String reqType) {
		this.reqType = reqType;
	}
	
	private String uuid;	
	public String getUUID(){
		return uuid;
	}
	public void setUUID(String uuid){
		this.uuid=uuid;
	}	
}
