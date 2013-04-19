package com.sohu.common.connectionpool.udp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ServerStatus {
	
	private static Log logger = LogFactory.getLog(ServerStatus.class);

	Object key;
	
	/**
	 * Runtime Status
	 */
	volatile int recentErrorNumber;
	volatile Long downtime = 0l;
	
	/**
	 * Configuration
	 */
	InetSocketAddress addr;
	String serverInfo;
	
	protected LinkedList waitQueue = new LinkedList();
	protected LinkedList freeChannelList;
	ArrayList allClients;
	
	AsyncGenericConnectionPool pool;
	
	/**
	 * 构造函数
	 *
	 */
	public ServerStatus() {
	}

	/**
	 * called by Receiver to check if any client has reached a socket timeout
	 *
	 */
	public void checkTimeout (){
		long now = System.currentTimeMillis();
		for( int i=0; i< allClients.size();i++){
			AsyncGenericQueryClient c = (AsyncGenericQueryClient)allClients.get(i);
			
			do{
				if( !c.isValid() ) break;
				
				// check if Clinet is free.
				if( ! c.using ) break;

				// check time value.
				if( ( !c.requestSent
						|| c.getTime_request() == 0
						|| 	now - c.getTime_request() <= pool.getSocketTimeout() )
					 )
				{
//					System.out.println("Socket or IO not timeOut"+c.requestSent);
					break;
				}
				
				if( logger.isTraceEnabled() ){
					logger.trace("Socket TimeOut!!!"+c.requestSent+" "+c.getTime_request()
							+ " "+(now - c.getTime_request())+" "+pool.getSocketTimeout() 
							+ "\nIO TimeOut!!! "+c.getTime_connect()
							+ " "+(now - c.getTime_connect())+" "+ pool.getConnectTimeout());
				}
				AsyncRequest request = c.getRequest();
				if( request != null ){
					request.socketTimeout();
				}
				c.serverStatus.socketTimeout();
				
				c.close();
				try{
					c.reset();
				}catch( Exception e){
					// 忽略即可
				}
				freeClient( c );
			}while( false );
		}
	}

	/**
	 * 发送请求.
	 * 由用户线程通过AsyncConnectionPool.sendRequest掉用
	 * @param req
	 * @return
	 *  -3 No Resource Error
	 *  -2 server is Down
	 *  -1 request is Invalid
	 *   0 sent Successfully
	 *   1 Server Busy
	 */
	int serverSendRequest(AsyncRequest request) {
		// 如果不能马上发送, 就加入等待队列.
		boolean needQueue = false;
		
		synchronized( waitQueue ){
			if( this.waitQueue.size() > 0 ){
				needQueue = true;
			}
		}
		if( needQueue ){			
			queueRequest( request );
			if( logger.isTraceEnabled() ){
				logger.trace("put a request in the queue");
			}
			request.setConnectionErrorStatus(1);
			return 1;
		} else {
			int sts = innerSendRequest( request );
			
			if( sts > 0 ){
				if( logger.isTraceEnabled() ){
					logger.trace("No avaliable channal put request in the queue");
				}
				queueRequest( request );
			}
			return sts;
		}

	}
	/**
	 * 发送请求
	 * 由 Sender 线程调用
	 * @return
	 */
	int innerSendRequest() {
		// 如果不能马上发送, 就加入等待队列.
		int sts;
		AsyncRequest request = firstUserRequest();
		
		if( request == null ) return 1;
		
		sts = innerSendRequest( request );
		request.time_trySend();
		switch(sts){
		case 1:
			// 服务器忙,
			// 没有空闲的连接
			// do nothing
			break;
		case 0:
			// 发送成功,
			// 从队列移除请求.
			request.time_outwaitqueue();
			removeFirstUserRequest();
			break;
		case -1:
			// 非法请求,
			// 从队列移除请求, 并通知用户线程.
			request.time_outwaitqueue();
			removeFirstUserRequest();
			break;
		case -2:
			// 服务器不可用
			// 从队列移除请求, 并通知用户线程.
			request.time_outwaitqueue();
			removeFirstUserRequest();
			break;
		case -3:
			// 系统资源不足
			// 从队列移除请求, 并通知用户线程.
			request.time_outwaitqueue();
			removeFirstUserRequest();
			break;
		case -4:
			// 请求超时
			// 从队列移除请求, 并通知用户线程.
			request.time_outwaitqueue();
			removeFirstUserRequest();
			break;
		default:
			request.time_outwaitqueue();
			removeFirstUserRequest();
		}
		return sts;
	}
	/**
	 * 发送请求.
	 * @param req
	 * @return
	 *  -4 request timeout
	 *  -3 No Resource Error
	 *  -2 server is Down
	 *  -1 request is Invalid
	 *   0 sent Successfully
	 *   1 Server Busy
	 */
	int innerSendRequest(AsyncRequest request) {
		
		if (!isServerAvaliable() && !request.isProbe()) {
			if( logger.isTraceEnabled()) {
				logger.trace("server is down, Don't waste Time");
			}
			request.serverDown();
			request.setConnectionErrorStatus(-2);
			return -2;
		}
		
		long now = System.currentTimeMillis();
		
		if (now - request.getStartTime() > pool.getQueueTimeout()) { // 排队超时
			if( logger.isTraceEnabled() ){
				logger.trace("WaitQueue timeOut");
			}
			request.waitTimeout();
			request.setConnectionErrorStatus(-4);
			return -4;
		}

		do{
			AsyncGenericQueryClient sc = removeFirstFreeClient();

			if (sc == null){
				if( logger.isTraceEnabled() ){
					logger.trace("CLIENT RUNOUT!");
				}
				break;
			}

			/**
			 * @XXX 连接处理流程
			 */
			boolean isRegistered = sc.isRegistered();
			if( ! isRegistered ){ // NOT regiestered. 线程安全

				if ( ! sc.isValid() ) {
					boolean isClientValid = false;
					try {
						sc.setTime_connect();
						request.time_connect();
						sc.connect(addr);
						request.time_connect_end();
						isClientValid = true;
					} catch (IOException e) {
						if (logger.isWarnEnabled()) {
							logger.warn("Sender: open new SocketChannel ", e);
						}
					}
					if ( !isClientValid ) {
						request.serverDown();
						freeClient(sc);
						noResourceError();
						request.setConnectionErrorStatus(-3);
						return -3;
					}
				}
				
			}

			sc.setRequest(request);

			// 发送请求
			if (logger.isTraceEnabled()) {
				logger.trace("TO SEND REQ!");
			}

			request.time_connect_end();

			int status = -1;
			try {
				request.requestTime();
				sc.setTime_request();
				status = sc.sendRequest();
			} catch (IOException e) {
				sendError();
				if (logger.isWarnEnabled()) {
					logger.warn("IOE", e);
				}
				sc.close();
			} catch (RuntimeException e) {
				if (logger.isErrorEnabled()) {
					logger.error("RTE While sending Request(Non-IOE)", e);
				}
			} catch (Exception e) {
				if (logger.isErrorEnabled()) {
					logger.error("Exception While sending Request(Non-IOE)", e);
				}
			}

			if (status > 0) {

				/**
				 * 发送成功, 检查channel是否已经注册. 因为前边finishConnect可能已经链接成功,
				 * 这里先直接发送,然后完成正常的注册流程.
				 */
				if (logger.isTraceEnabled()) {
					logger.trace("SENT SUCCESS!");
				}

				sc.requestSent(true);

				if (!isRegistered) {
					if (logger.isTraceEnabled()) {
						logger.trace("NOT REGED!");
					}
					request.time_enqueue();
					pool.recver.queueChannel(sc);
					pool.selector.wakeup();
					request.time_enqueue_end();
				}
				request.setConnectionErrorStatus(0);	
				return 0;
			} else {
				/**
				 * 发送不成功 有两种可能: socket或request. 如果是socket有问题,
				 * 那么recver会正确处理(close). 如果是request有问题, 那么简单通知用户线程即可.
				 */
				if (logger.isTraceEnabled()) {
					logger.trace("ILLEGAL REQUEST!");
				}

				request.illegalRequest();
				try {
					sc.reset();
				} catch (Exception e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Exception while reset client", e);
					}
					// ignore
				}
				freeClient(sc);
				request.setConnectionErrorStatus(-1);
				return -1;
			}
			
		} while( false );
		request.setConnectionErrorStatus(1);
		return 1;
	}
	
	private AsyncGenericQueryClient removeFirstFreeClient(){
		AsyncGenericQueryClient client;
		synchronized( freeChannelList ){
			if( freeChannelList.size() > 0 ){
				if( logger.isTraceEnabled() ){
					logger.trace("Get One Client From " + freeChannelList.size());
				}
				client = (AsyncGenericQueryClient)freeChannelList.removeFirst( );
				client.using = true;
				client.requestSent = false;
			} else {
				if( logger.isTraceEnabled() ){
					logger.trace("no Free Client to get");
				}
				return null;
			}
		}
		return client;
	}
	void freeClient(AsyncGenericQueryClient client) {
		synchronized (freeChannelList) {
			if (client.using) {
				if( logger.isTraceEnabled() ){
					logger.trace("Free a Client");
				}
				client.using = false;
				freeChannelList.addFirst(client);
			}
		}
		/**
		 * @TODO check Sender Thread
		 */
		this.pool.sender.checkSenderThread();
	}

	private AsyncRequest firstUserRequest(){
		synchronized( waitQueue ){
			if( waitQueue.isEmpty() ){
				return null;
			} else {
				return (AsyncRequest)this.waitQueue.element();
			}
		}
	}

	private AsyncRequest removeFirstUserRequest(){
		AsyncRequest client;
		synchronized( waitQueue ){
			client = (AsyncRequest)waitQueue.removeFirst( );
		}
		if( logger.isTraceEnabled() ){
			logger.trace("Remove a request from the queue");
		}
		return client;
	}

	public boolean isServerAvaliable(){
		long now = System.currentTimeMillis();
		int queueSize = 0;
		synchronized( waitQueue ){
			queueSize = waitQueue.size();
		}
		return (( recentErrorNumber <= pool.maxErrorsBeforeSleep
					|| (now - downtime ) >= pool.sleepMillisecondsAfterTimeOutError 
				) && queueSize <= pool.maxQueueSize);
	}

	public String getServerInfo() {
		if( serverInfo == null 
				&& this.key!=null 
				&& this.addr != null ){
			serverInfo = this.key.toString() + '@'+this.addr.getAddress().getHostAddress()+':' + this.addr.getPort();
		}
		return serverInfo;
	}

	public void setServerInfo(String serverInfo) {
		this.serverInfo = serverInfo;
	}

//	public ServerStatus(){}
	
	public ServerStatus(String line, AsyncGenericConnectionPool pool ) throws IllegalArgumentException{
		if( line == null ) throw new IllegalArgumentException("ServerStatus Null Line Parameter");
		
		line = line.trim();
		
		int at = line.indexOf('@');
		if( at < 0 ) throw new IllegalArgumentException( "invalid line:" + line );
		
		int sc = line.indexOf(':', at + 1);
		if( sc < 0 ) throw new IllegalArgumentException( "invalid line:" + line );
		
		int port;
		try{	
			port = Integer.parseInt( line.substring(sc + 1) );
		}catch( NumberFormatException e){
			throw new IllegalArgumentException( e );
		}
		
		InetSocketAddress addr;
		try{
			addr = new InetSocketAddress( 
					InetAddress.getByName( line.substring( at+1, sc )),
					port );
		}catch( UnknownHostException e){
			throw new IllegalArgumentException( "ServerStatus:invalid host:" + line , e );
		}catch( SecurityException e ){
			throw new IllegalArgumentException( "ServerStatus:invalid line:" + line , e );
		}catch( IllegalArgumentException e){
			throw new IllegalArgumentException( "ServerStatus:invalid Server:" + line , e );
		}
		
		
		this.pool = pool;
		int count = pool.getMaxConnectionsPerServer();
		ArrayList al = new ArrayList( count );
		LinkedList deactiveChannelSet = new LinkedList();
		for(int i=0;i<count;i++){
			
			AsyncGenericQueryClient ace = pool.factory.newInstance();
			ace.serverStatus = this;
//			SocketChannel socketChannel =null;
//			ace.channel = socketChannel;
			al.add( ace );
			deactiveChannelSet.add( ace );
		}
		
		this.freeChannelList = deactiveChannelSet;
		this.allClients = al;

		
		this.key = line.substring(0, at);
		this.addr = addr;
		this.serverInfo = line;
	}
	
	public long getDowntime() {
		return downtime;
	}

	public void setDowntime(long downtime) {
		this.downtime = downtime;
	}

	public Object getKey() {
		return key;
	}

	public void setKey(Object key) {
		this.key = key;
	}

	public int getRecentErrorNumber() {
		return recentErrorNumber;
	}

	public void setRecentErrorNumber(int recentErrorNumber) {
		this.recentErrorNumber = recentErrorNumber;
	}

	public synchronized void connectTimeout(){
		if( logger.isTraceEnabled() ){
			logger.trace("errNumber is " + recentErrorNumber );
		}
		this.recentErrorNumber ++;
		this.downtime = System.currentTimeMillis();
	}
	public synchronized void socketTimeout() {
		if( logger.isTraceEnabled() ){
			logger.trace("sockettimeout "+ recentErrorNumber);
		}
		this.recentErrorNumber ++;
		this.downtime = System.currentTimeMillis();
	}
	public synchronized void sendError() {
		this.recentErrorNumber ++;
		this.downtime = System.currentTimeMillis();
	}
	public synchronized void noResourceError() {
		this.recentErrorNumber ++;
		this.downtime = System.currentTimeMillis();
	}

	public synchronized void success() {
		this.recentErrorNumber >>= 1;
	}

	public InetSocketAddress getAddr() {
		return addr;
	}

	public void setAddr(InetSocketAddress addr) {
		this.addr = addr;
	}

	protected final int queueRequest(AsyncRequest request){
		request.time_waitqueue();
		request.setServerInfo( this.getServerInfo() );
		synchronized(waitQueue){
			waitQueue.addLast( request );
		}
		return 0;
	}
	public CharSequence queueStatus(StringBuffer sb){
		synchronized( waitQueue){
			sb.append("\nServerStatus:");
			sb.append( this.getKey() );
			sb.append( ", addr: ");
			sb.append( this.addr );
			sb.append( '\n' );

			for( int i=0; i<waitQueue.size(); i++){
				AsyncRequest req = (AsyncRequest)waitQueue.get(i);
				sb.append( req.dumpTimeStatus() );
				sb.append('\n');
			}
		}
		return sb;
	}
	public String status(StringBuffer sb){
		sb.append( "\nServerStatus: key:");
		sb.append( this.getKey() );
		sb.append( ", addr: ");
		sb.append( this.addr );
		sb.append( '\n' );
		sb.append("\tsend_queue: ");
		sb.append( this.waitQueue.size() );
		sb.append( ", ava:");
		sb.append( this.isServerAvaliable() );
		sb.append( ", free:");
		sb.append( this.freeChannelList.size() );
		sb.append( '\n' );
		
		for( int i=0; i< this.allClients.size(); i++){
			sb.append( '\t' );
			AsyncGenericQueryClient ace = (AsyncGenericQueryClient)this.allClients.get(i);
			sb.append( ace.getStatus() );
			sb.append('\n');
		}
		
		if( logger.isInfoEnabled() ){
			logger.info( sb.toString() );
		}
		return sb.toString();
	}
	public void finalize(){
		destroy();
	}
	/**
	 * 清理释放资源
	 */
	public void destroy(){
		ArrayList allClients = this.allClients;
		for( int i=0; allClients != null && i< allClients.size();i++){
			AsyncGenericQueryClient c = (AsyncGenericQueryClient)allClients.get(i);
			c.close();
		}
	}
}