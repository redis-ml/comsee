/*
 * Created on 2003-11-24
 *
 */
package com.sohu.common.connectionpool;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
//import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

//import com.sohu.websearch.Request;
//import com.sohu.websearch.Result;
//import com.sohu.websearch.ServerConfig;
//import com.sohu.websearch.ServerStatus;

/**
 * 连接池
 * 
 * 1. 提供出错处理功能. 工作过程中,对外不返回任何的错误信息.
 * 2. 可在线配置. 包括服务器地址.
 * 3. 支持多线程
 * 
 * @author LiuMingzhu (mingzhuliu@sohu-inc.com)
 *
 */
public abstract class GenericConnectionPool {

	private Log logger = getLogger();
	
	/// random
	protected static final Random random = new Random();
	
	/// 保存服务器状态信息
	protected ServerStatus[] servers ;
	protected ServerConfig serverConfig = new ServerConfig();
	///	socket连接失败时，会自动选择一个替代连接，替代品在inplaceConnectionLife次query后自动断开
//	protected int inplaceConnectionLife = 500;
	///	client超时的毫秒数
	private int timeOutMillseconds = 5000;
	/// client连接超时毫秒数
	private int connectTimeOutMillseconds = 100;

	/// 连接池
	private KeyedObjectPool pool  =null ;
	
	/**
	 * 创建新实例
	 */
	protected GenericConnectionPool(KeyedPoolableObjectFactory factory, GenericKeyedObjectPool.Config config){
		this.pool= new GenericKeyedObjectPool( factory, config);
	}
	/**
	 * 服务器的选择策略.如Cache服务器根据hash值分配服务器
	 * @param obj
	 * @return
	 */
	public abstract int getServerId(Object obj);
	/**
	 * 获得记录器实例
	 * @return
	 */
	protected abstract Log getLogger();

	/* (non-Javadoc)
	 * @see com.sohu.websearch.pool.SetServersAble#setServers(java.lang.String)
	 */
	public void setServers(String multiservers) throws IllegalArgumentException {
		
		serverConfig.initServerConfig(multiservers);
		
		synchronized (this){

			this.servers = serverConfig.getAllStatus();

		}
	}

	/**
	 * 确保连接已建立
	 * 注意，连接策略不同时,需要重载此函数。
	 * 本函数中的连接策略:
	 * 若应连服务器无法连接,则随机选择另一个服务器服务器连接.
	 * 1. 随机选择的服务器不能是原来的服务器,否则要重试,最多重试六次,六次不成功,就当作出错.
	 * 2. 若后来选择的服务器连接成功,则设定一个比较短的生存时间.
	 * 3. 若后来选择的服务器也不成功,则将异常返回给调用者
	 * @param request
	 * @return
	 * @throws IOException
	 */
	protected void ensureConnection( int serverId , QueryClient client)
			throws IOException
			{
		if ( client.isValid() ) return;
		
		client.connect(	servers[serverId].getAddr(), getConnectTimeoutMillis(),getSocketTimeoutMillis() );
		client.setLife(Integer.MAX_VALUE);
	}
	/**
	 * 执行查询
	 * @param request
	 * @return
	 * @throws IOException
	 */
	public Result query(Request request) 
	{
		int serverId = getServerId( request );
		
		if( serverId <0 || serverId>=this.getServerIdCount() ) return null;
		ServerStatus serverStatus = servers[serverId];
		
		request.setServerInfo( serverStatus.getServerInfo() );

		if( logger!=null && logger.isDebugEnabled() ){
			logger.debug("should connect to " + serverId + "th server " + request.getRequestId());
		}
		long timeAfterError =
			System.currentTimeMillis() - serverStatus.getDowntime();
		//	当发生多次错误后，休息一段时间
		if( ! serverConfig.isServerAvaliable(serverId) ){
			
			if( logger!=null && logger.isDebugEnabled() ){
				logger.debug("recovering from socket Error("+ timeAfterError+ "ms before) " + request.getRequestId());
			}
			return null;
		}

		QueryClient client = null;
		boolean success = false;
		// 查询结果
		Result result = null;
		try {
			client = (QueryClient) pool.borrowObject( serverStatus.getKey() );
		} catch ( NoSuchElementException e ){
			serverStatus.queueTimeout();
			if( logger.isErrorEnabled() ){
				logger.error("BORROW_OBJECT_FAILED", e);
			}
		} catch ( Exception e){
			serverStatus.queueTimeout();
			if( logger.isErrorEnabled() ){
				logger.error("BORROW_OBJECT_FAILED UNSPEC", e);
			}
		}

		// 检查是否拿到连接
		if( client == null ) return null;
		
		try{
			ensureConnection( serverId, client);
			result = client.query(request);
			success = true;
			serverStatus.success();
		} catch (ConnectException e) {
			request.serverDown();
			serverStatus.connectTimeout();
			if( logger!=null && logger.isWarnEnabled() ){
				logger.warn( "CONN_FAIL : KEY: "+ serverStatus.getKey()+ " : " + request.getRequestId(), e);
			}
		} catch (SocketTimeoutException e) {
			request.socketTimeout();
			serverStatus.transferTimeout();
			if( logger!=null && logger.isWarnEnabled() ){
				logger.warn( "TRANS_TIMEOUT : KEY: "+ serverStatus.getKey()+ " : " + request.getRequestId(), e);
			}
		} catch( IOException e ){
			request.serverDown();
			serverStatus.transferTimeout();
			if( logger!=null && logger.isWarnEnabled() ){
				logger.warn( "unexpected exception : " + request.getRequestId(), e);
			}
		}finally {
			if ( !success ){
				client.close();
			}
			try{
				returnClient( serverId , client);
			}catch(Exception e){
				if( logger!=null && logger.isWarnEnabled() ){
					logger.warn( "unexpected exception : " + request.getRequestId(), e);
				}
			}
			client = null;
		}
		return result;
	}

	/**
	 * @return		每个服务器允许多少个连接
	 */
	public int getMaxConnectionsPerServer() {
		return ((GenericKeyedObjectPool) pool).getMaxActive();
	}

	/**
	 * @param c		每个服务器允许多少个连接
	 */
	public void setMaxConnectionsPerServer(int c) {
		((GenericKeyedObjectPool) pool).setMaxActive(c);
	}

	/**
	 * @return
	 */
	public int getServerIdCount() {
		if( this.servers == null ){
			return 0;
		} else {
			return servers.length;
		}
	}

	/**
	 * @return
	 */
	public int getServerIdBits() {
		return 0;
	}

	/**
	 * @return
	 */
	public int getServerIdMask() {
		return 0;
	}

	/**
	 * @return
	 */
	public long getSleepMillisecondsAfterTimeOutError() {
		return serverConfig.getSleepMillisecondsAfterTimeOutError();
	}

	/**
	 * @param i
	 */
	public void setSleepMillisecondsAfterTimeOutError(int i) {
		serverConfig.setSleepMillisecondsAfterTimeOutError( i );
	}

	/**
	 * @return
	 */
	public int getTimeOutMillseconds() {
		return getSocketTimeoutMillis();
	}

	/**
	 * @param i
	 */
	public void setTimeOutMillseconds(int i) {
		setSocketTimeoutMillis(i);
	}

	/**
	 * @param i
	 */
	public void setPoolTimeOutMillseconds(long i) {
		((GenericKeyedObjectPool) pool).setMaxWait(i);
	}

	/**
	 * @return
	 */
	public int getMaxErrorsBeforeSleep() {
		return serverConfig.getMaxErrorsBeforeSleep();
	}

	/**
	 * @param i
	 */
	public void setMaxErrorsBeforeSleep(int i) {
		serverConfig.setMaxErrorsBeforeSleep(i);
	}

	/**
	 * @param i
	 */
	public long getPoolTimeOutMillseconds() {
		return ((GenericKeyedObjectPool) pool).getMaxWait();
	}

	/**
	 * @return pool
	 */
	public KeyedObjectPool getInternalPool() {
		return pool;
	}

	/**
	 * @param i
	 * @return	第i台服务器
	 */
	public InetSocketAddress getServer(int i) {
		return servers[i].getAddr();
	}

	public void returnClient(int serverId, QueryClient obj) throws Exception{
		pool.returnObject(servers[serverId].getKey(), obj);
	}
	

	/**
	 * @return	闲置多长的connection可以被扫描程序关闭
	 */
	public long getMinEvictableIdleTimeMillis() {
		return ((GenericKeyedObjectPool) pool).getMinEvictableIdleTimeMillis();
	}

	/**
	 * @return	每次扫描多少个闲置connection
	 */
	public int getNumTestsPerEvictionRun() {
		return ((GenericKeyedObjectPool) pool).getNumTestsPerEvictionRun();
	}

	/**
	 * @return	每隔多长时间扫描一次闲置connection
	 */
	public long getTimeBetweenEvictionRunsMillis() {
		return ((GenericKeyedObjectPool) pool)
			.getTimeBetweenEvictionRunsMillis();
	}

	/**
	 * @param minEvictableIdleTimeMillis	闲置多长的connection可以被扫描程序关闭
	 */
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		((GenericKeyedObjectPool) pool).setMinEvictableIdleTimeMillis(
			minEvictableIdleTimeMillis);
	}

	/**
	 * @param numTestsPerEvictionRun		每次扫描多少个闲置connection
	 */
	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
		((GenericKeyedObjectPool) pool).setNumTestsPerEvictionRun(
			numTestsPerEvictionRun);
	}

	/**
	 * @param timeBetweenEvictionRunsMillis	每隔多长时间扫描一次闲置connection
	 */
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		((GenericKeyedObjectPool) pool).setTimeBetweenEvictionRunsMillis(
			timeBetweenEvictionRunsMillis);
	}

	/**
	 * @return Returns the inplaceConnectionLife.
	 */
	public int getInplaceConnectionLife() {
		return -1;
//		return inplaceConnectionLife;
	}
	
	/**
	 * @param inplaceConnectionLife The inplaceConnectionLife to set.
	 */
	public void setInplaceConnectionLife(int inplaceConnectionLife) {
//		this.inplaceConnectionLife = inplaceConnectionLife;
	}
	public void setSleepMillisecondsAfterQueueTimeOut(long time){
		this.serverConfig.setSleepMillisecondsAfterQueueTimeOut(time);
	}
	/**
	 * 获得某台服务器在pool中的key值.
	 * @param i
	 * @return
	 */
	public Object getServerKey(int i){
		if( i>=0 && servers != null && servers.length>i){
			return servers[i].getKey();
		}
		return null;
	}
	public int getConnectTimeoutMillis() {
		return connectTimeOutMillseconds;
	}
	public void setConnectTimeoutMillis(int connectTimeOutMillseconds){
		this.connectTimeOutMillseconds = connectTimeOutMillseconds;
	}
	public int getSocketTimeoutMillis(){
		return this.timeOutMillseconds;
	}
	public  void setSocketTimeoutMillis(int socketTimeoutMillseconds){
		this.timeOutMillseconds = socketTimeoutMillseconds;
	}

}