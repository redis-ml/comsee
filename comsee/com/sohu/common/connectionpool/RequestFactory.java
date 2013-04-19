package com.sohu.common.connectionpool;

public interface RequestFactory {
	
	/**
	 * 用于生成实际发送请求的Request
	 * @return
	 */
	public Request newRequest();

	/**
	 * 用于生成探测用的Request
	 * @return
	 */
	public Request newProbeRequest();
}
