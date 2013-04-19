/*
 * Created on 2003-11-24
 *
 */
package com.sohu.tinysearch;

import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.BaseKeyedPoolableObjectFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import com.sohu.common.connectionpool.GenericConnectionPool;
import com.sohu.common.connectionpool.QueryClient;

/**
 * 连接池
 * @author MingzhuLiu(mingzhuliu@sohu-inc.com)
 *
 */
public class TinySearchConnectionPool extends GenericConnectionPool {
	/// random
	private static final Random random = new Random();
	
	private static Log logger = LogFactory.getLog(TinySearchConnectionPool.class); 
	
	/// socket 超时设置
	private int connectTimeoutMillis = 50;
	private int socketTimeoutMillis = 500;
	/// 配置对象
	private static GenericKeyedObjectPool.Config poolConfig =
		new GenericKeyedObjectPool.Config();
	static{
		// 每个服务允许的最大连接数
		poolConfig.maxActive = 1;
		// 取连接对象时的最大互斥等待时间
		poolConfig.maxWait = 1000;
		poolConfig.testOnBorrow = true;
		poolConfig.testOnReturn = true;
		poolConfig.testWhileIdle = true;
		// idle多长时间以上的连接会被杀掉
		poolConfig.minEvictableIdleTimeMillis = 300000;
		// 每次扫描会干掉几个连接
		poolConfig.numTestsPerEvictionRun = 5;
		// 多长时间扫描一次idle连接
		poolConfig.timeBetweenEvictionRunsMillis = 600000;
	}
	/// 单体实例
	
	private static class TinySearchConnectionFactory extends BaseKeyedPoolableObjectFactory {
		/* (non-Javadoc)
		* @see org.apache.commons.pool.KeyedPoolableObjectFactory#makeObject(java.lang.Object)
		*/
		public Object makeObject(Object key){
			return new TinyClientImpl();
		}

		/* (non-Javadoc)
		 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#validateObject(java.lang.Object, java.lang.Object)
		 */
		public boolean validateObject(Object key, Object obj) {
			if(  ( (TinyClientImpl)obj).in == true ) {
				System.err.println("tinysearch fuck it ");
			}
			return obj != null; 
		}

		/* (non-Javadoc)
		 * @see org.apache.commons.pool.KeyedPoolableObjectFactory#destroyObject(java.lang.Object, java.lang.Object)
		 */
		public void destroyObject(Object key, Object obj) throws Exception {
			if( obj != null )
				((QueryClient) obj).close();
		}

	}

	/**
	 *  公用的单体实例
	 */
	private static final TinySearchConnectionPool instance = 
		new TinySearchConnectionPool();

	/**
	 * 创建新实例
	 */
	TinySearchConnectionPool( KeyedPoolableObjectFactory factory, GenericKeyedObjectPool.Config poolConfig) {
		
		super( factory, poolConfig );
	}

	/**
	 * 允许创建新实例.
	 *
	 */
	public TinySearchConnectionPool(){
		this( new TinySearchConnectionFactory(),poolConfig );
	}

	public int getServerId(Object obj){
		int serverIdCount = getServerIdCount();
		if( serverIdCount > 0 ){
			return random.nextInt( serverIdCount );
		} else {
			return -1;
		}
	}
	/**
	 * 取单体的实例
	 * @return
	 */
	public static TinySearchConnectionPool getInstance() {
		return instance;
	}
	public Log getLogger() {
		return logger;
	}
	public int getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public void setConnectTimeoutMillis(int connectTimeOutMillseconds) {
		this.connectTimeoutMillis = connectTimeOutMillseconds;
	}

	public int getSocketTimeoutMillis() {
		return this.socketTimeoutMillis;
	}

	public void setSocketTimeoutMillis(int socketTimeoutMillseconds) {
		this.socketTimeoutMillis = socketTimeoutMillseconds;
	}
}
