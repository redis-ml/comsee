package com.sohu.leadsearch;

import java.nio.channels.SelectionKey;

import com.sohu.common.connectionpool.ServerConfig;


public interface AsyncQueryClient {

	/**
	 * 处理接收数据的handler,
	 * 由Selector线程调用, 
	 * 用于将数据从系统缓冲区读到本地,并对数据加以验证,保证数据包的有效性.
	 * 注意: 绝对不能阻塞或抛出异常.
	 * 实时时注意同输出线程的互斥
	 * @param key
	 */
	public void handleInput( SelectionKey key );
	
//	/**
//	 * 向服务器发送请求的handler,
//	 * @param obj
//	 */
//	public void handleOutput( Object obj );
//	
//	public void notifyInput();
	/**
	 * 关闭. 释放掉所有的资源.
	 */
	public void close();
	
	/**
	 * 服务器配置方法.
	 * 用于设置服务器的配置参数.
	 * @param config 通用配置参数的存储结构
	 * @param i 当前服务器的索引号
	 */
	public void setServerConfig(ServerConfig config, int i);
	
	public int getIndex();
}
