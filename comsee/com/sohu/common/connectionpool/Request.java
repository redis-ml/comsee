package com.sohu.common.connectionpool;

/**
 * 封装请求信息的数据包
 * 
 * @author liumingzhu
 *
 */
public interface Request extends Cloneable{

	public void setRequestId(long id);
	public long getRequestId();
	
	public long getTime();
	public void setTime(long t);
	
	public String getServerInfo();
	public void setServerInfo(String info);
	
	public void serverDown();
	public void socketTimeout();
	public int getStatus();
	public void setStatus(int status);
}