/*
 * Created on 2003-11-24
 *
 */
package com.sohu.common.connectionpool;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author Mingzhu Liu( mingzhuliu@sohu-inc.com) 
 *
 */
public interface QueryClient {
	public static final int ITEMS_PER_PAGE = 10;//20;
	public static final int MAX_ITEMS_PER_PAGE = 100;
	
	public static final int ITEMS_PER_CACHE = 10;
	
	public static final int MAX_ITEMS = 1000;
	
	/**
	 * 关闭连接
	 */
	public abstract void close();
	/**
	 * 向cache服务器提交查询
	 * @param request
	 * @return
	 */
	public abstract Result query(Request request) throws IOException;
	/**
	 * 是否已关闭
	 * @return
	 */
	public abstract boolean isValid();
	/**
	 * 连接服务器
	 * @param addr
	 * @param connTimeoutMills
	 * @param socketTimeoutMillis
	 */
	public void connect(InetSocketAddress addr, int connectTimeoutMillis, int socketTimeoutMillis) throws IOException;
	
	/**
	 * 连接的寿命
	 * @param life
	 */
	public void setLife(int life);
	

}