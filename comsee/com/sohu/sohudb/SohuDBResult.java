package com.sohu.sohudb;

public class SohuDBResult {

	private volatile int status;
	
	private volatile int len;

	private volatile byte[] data;
	/**
	 * @return the status
	 */
	public int getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(int status) {
		this.status = status;
	}
	/**
	 * @return the data
	 */
	public byte[] getData() {
		return data;
	}
	/**
	 * @param data the data to set
	 */
	public void setData(byte[] data) {
		this.data = data;
	}
	/**
	 * @return the len
	 */
	public int getLen() {
		return len;
	}
	/**
	 * @param len the len to set
	 */
	public void setLen(int len) {
		this.len = len;
	}
	
}
