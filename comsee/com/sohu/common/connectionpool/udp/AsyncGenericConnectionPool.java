/*
 * Created on 2006-11-24
 *
 */
package com.sohu.common.connectionpool.udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

import com.sohu.common.connectionpool.RequestFactory;

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
public abstract class AsyncGenericConnectionPool extends ServerConfig{

	/// random
	protected static final Random random = new Random();
	
	/// 保存服务器状态信息
	protected ServerStatus[] status ;
	///	socket连接失败时，会自动选择一个替代连接，替代品在inplaceConnectionLife次query后自动断开
	protected int inplaceConnectionLife = 500;

	Selector selector;
	
	protected Receiver recver;
	protected Sender sender;
	
	protected Object recverLock = new Object();
	protected Object senderLock = new Object();
	
	// 连接对象的factory
	protected AsyncClientFactory factory;
	// 请求体的factory
	protected RequestFactory requestFactory;

	/**
	 * 创建连接池实例
	 * @param factory 连接对象的factory，特定的连接池要自己实现连接对象
	 * @param name 连接池的名字，可以任意定义。置为null会按照"Pool"来处理。
	 */
	protected AsyncGenericConnectionPool(AsyncClientFactory factory, String name)
	{
		this(factory, name, null);
	}
	/**
	 * 创建新实例
	 * @param factory 连接对象的factory，特定的连接池要自己实现连接对象
	 * @param name 连接池的名字，可以任意定义。置为null会按照"Pool"来处理。
	 * @param reqestFactory 发送对象(Request)的factory
	 */
	protected AsyncGenericConnectionPool(AsyncClientFactory factory, String name, RequestFactory reqestFactory)
	{
		this.factory = factory;
		if( name != null){
			this.name = name;
		}
		this.requestFactory = reqestFactory;
	}

	public void init() throws Exception{
		
		ArrayList servers = new ArrayList();
		
		if ( this.servers == null ) throw new IllegalArgumentException("config is NULL");
		
		String[] list = pat.split( this.servers );
	
		for (int i = 0 ; i < list.length ; i++ ) {
			ServerStatus ss = new ServerStatus( list[i], this );
			servers.add( servers.size() , ss);
		}

		ServerStatus[] serverStatus = (ServerStatus[])servers.toArray( new ServerStatus[servers.size()] );

		selector = Selector.open();
		
		this.status = serverStatus;
		
		recver = new Receiver(this);
		sender = new Sender(this);
		
		recver.startThread();
		sender.startThread();
	}

	/**
	 * 获得记录器实例
	 * @return
	 */
	protected abstract Log getLogger();

	public int sendRequest( AsyncRequest request ){
		
		if( request == null ){			
			return -1;
		}
		
		if( ! request.isValid() ){
			request.illegalRequest();
			request.setConnectionErrorStatus(-2);
			return -2;
		}
		
		int serverCount = this.getServerIdCount();
		int ret = request.getServerId( serverCount );
		
		// 检测服务器状态
		if( ! isServerAvaliable( ret )){
//			System.out.println("server is not avaliable");
			int avaliableServerCount = 0;
			for(int i=0; i< getServerIdCount() ; i++){
				if( isServerAvaliable( i ) ){
					avaliableServerCount ++;
				}
			}
			if( avaliableServerCount <= 0 ){
				request.serverDown();
				request.setConnectionErrorStatus(-1);
				return -1;
			}
			// 测试次数.
			int inc = ( request.getServerId( avaliableServerCount) ) + 1;

			int finalIndex = ret ;

			int i=0;
			do{
				int j=0;
				boolean find = false;
				do {
					finalIndex = ( finalIndex +1 ) % serverCount;
					if( isServerAvaliable( finalIndex ) ){
						find = true;
						break;
					}
					j++;
				}while( j < serverCount );
				
				if( !find ){
					request.serverDown();
					request.setConnectionErrorStatus(-1);
					return -1;
				}

				i++;
			}while( i<inc);

			ret = finalIndex;
		}
		
		return sendRequestById(ret, request);
	}

	/**
	 * 向指定的Server发送请求。
	 * 会设置连接池的很多调试参数
	 * 慎用：适用于对连接池内部构造熟悉的开发者
	 * @param serverId
	 * @param request
	 * @return
	 */
	public int sendRequestById( int serverId, AsyncRequest request ){
		// 检查serverId的合法性
		assert( serverId >= 0 && serverId < this.getServerIdCount() );
		if( serverId<0 || serverId>=this.getServerIdCount() ){
			request.illegalRequest();
			request.setConnectionErrorStatus(-1);
			return -1;
		}
		
		request.setServerId(serverId);
		ServerStatus ss = getStatus(serverId);
		
		if( ss == null ){
			request.serverDown();
			request.setConnectionErrorStatus(-2);
			return -2;
		}
		
		request.setServer(ss);
		request.setServerInfo( ss.getServerInfo() );
		request.queueSend();
		sender.senderSendRequest(request);

		return 0;
		
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
	 * @param i
	 * @return	第i台服务器
	 */
	public InetSocketAddress getServer(int i) {
		return status[i].getAddr();
	}

	/**
	 * @return Returns the inplaceConnectionLife.
	 */
	public int getInplaceConnectionLife() {
		return inplaceConnectionLife;
	}
	
	/**
	 * @param inplaceConnectionLife The inplaceConnectionLife to set.
	 */
	public void setInplaceConnectionLife(int inplaceConnectionLife) {
		this.inplaceConnectionLife = inplaceConnectionLife;
	}
	
	private static Pattern pat = Pattern.compile("\\s+");
	

	public ServerStatus[] getAllStatus() {
		return status;
	}
	/**
	 * 返回指定序号的服务器的状态对象.
	 * @param i
	 * @return 如果指定序号的服务器不存在,则返回null
	 */
	public ServerStatus getStatus(int i ){
		if( status != null 
				&& i>=0 
				&& i<status.length ){
			return status[i];
		} else {
			return null;
		}
	}

	public boolean isServerAvaliable(int i){
		long now = System.currentTimeMillis();
		
		ServerStatus ss = null;
		if( status !=null && i>=0 && i< status.length){
			ss = status[i];
		}
		if( ss == null ){
			return false;
		}
		boolean ret = ( ss.recentErrorNumber <= this.getMaxErrorsBeforeSleep()
				|| (now - ss.downtime ) >= this.getSleepMillisecondsAfterTimeOutError() );
		if( !ret ){
			Log logger = getLogger();
			if( logger != null && logger.isTraceEnabled() )
				logger.trace("server is not avaliable:" + ss.getServerInfo() );
		}
		return ret;
	}
	
	/**
	 * 返回序号i对应的服务器在连接池中对应的键值.
	 * 如果对应的服务器非法(不存在),则返回null;
	 * @param i
	 * @return
	 */
	public Object getServerKey( int i ){
		if( status !=null 
				&& i >= 0
				&& i < status.length
				&& status[i] !=null
				&& status[i].key != null
			) {
			return status[i].key;
		} else {
			return null;
		}
	}

	/**
	 * 运行过程中, server可能会down掉, 需要动态调整服务器数量值.
	 * @return
	 */
	public int getServerIdCount(){
		if( status == null ){
			return 0;
		}else {
			return status.length;
		}
	}
	public InetSocketAddress getSocketAddress( int i ){
		if( status == null 
				|| i<0
				|| i>=status.length
				|| status[i] == null
		  ){
			return null;
		} else {
			return status[i].getAddr();
		}
	}
	public void finalize(){
		destroy();
	}
	
	/**
	 * 销毁连接池对象
	 */
	public void destroy(){
		sender.stopThread();
		sender = null;
		recver.stopThread();
		recver = null;
		ServerStatus[] temp = status;
		status = null;
		if( temp != null ){
			for(int i=0;i<temp.length; i++){
				ServerStatus ss = temp[i];
				if( ss == null ) continue;
				ss.destroy();
			}
		}
		try{
			this.selector.close();
		}catch( IOException e){
			// dummy
		}
	}
	public String status(){
		StringBuffer sb = new StringBuffer();
		sb.append( "\nPool Status: ");
		sb.append( this.getName() );
		sb.append( '\n' );
		
		for( int i=0; i< this.status.length; i++){
			status[i].status( sb );
		}
		
		if( getLogger().isInfoEnabled() ){
			getLogger().info( sb.toString() );
		}
		return sb.toString();
	}

	/**
	 * @return the requestFactory
	 */
	public RequestFactory getRequestFactory() {
		return requestFactory;
	}

	/**
	 * @param requestFactory the requestFactory to set
	 */
	public void setRequestFactory(RequestFactory requestFactory) {
		this.requestFactory = requestFactory;
	}

}
