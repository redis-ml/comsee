/**
 * @author Mingzhu Liu (mingzhuliu@sohu-inc.com)
 * 
 *  Created on 2006-09-23
 *   
 */
package com.sohu.common.connectionpool.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;

/**
 * cache的异步连接客户端
 * 每个异步连接池的开发者都要实现该虚类的几个abstract方法。
 * 每个开发者主要处理ByteBuffer的填充、接收，自行设计buffer的组织形式
 */
public abstract class AsyncGenericQueryClient {

	public static final int STS_OPEN = 0;
	public static final int STS_CONN = 1;
	public static final int STS_BUSY = 2;
	public static final int STS_FREE = 3;
	public static final int STS_CLOS = 4;
	
	volatile Long time_connect = 0l;
	volatile Long time_request = 0l;
//	boolean needRequest = false;
	boolean requestSent = true;
	
	public long getTime_connect() {
		return time_connect;
	}

	public void setTime_connect() {
		this.time_connect = System.currentTimeMillis();
	}

	public long getTime_request() {
		return time_request;
	}

	public void setTime_request() {
		this.time_request = System.currentTimeMillis();
	}

	public int getStatus(){
		SocketChannel channel;
		boolean using;
		synchronized( channelLock) {
			channel = this.channel;
			using = this.using;
		}
			if( channel == null || !channel.isOpen() ){
				return STS_CLOS;
			} else if( ! channel.isConnected() ){
				return STS_OPEN;
			} else if( channel.isConnectionPending() ){
				return STS_CONN;
			} else if( !using ){
				return STS_FREE;
			} else {
				return STS_BUSY;
			}
	}
	// 记录器
	private Log logger = getLogger();

	private Object channelLock = new Object();
	private volatile SocketChannel channel = null;
	protected ServerStatus serverStatus;
	
	protected boolean using = false;
	
	// 连接的生命值.低于0表示不可靠.
	protected int life = 0;
	
	private volatile AsyncRequest request ;
	
	public void setLife(int lf) {
		this.life = lf;
	}
	
	public int getLife(){
		return this.life;
	}

	/**
	 * 关闭连接
	 */
	final public void close() {
		SocketChannel channel;
		synchronized( channelLock ){
			channel = this.channel;
			this.channel = null;
		}
		if (channel != null) {
			try {
				channel.close();
			} catch (IOException e) {
				logger.debug(this, e);
			}
		}
	}

	public void finalize(){
		close();
	}

	public boolean isValid() {
		synchronized( channelLock ){
			return ( channel != null 
					&& channel.isOpen()	 );
		}
	}
	public boolean isRegistered() {
		synchronized( channelLock ){
			return ( channel != null 
					&& channel.isRegistered()	 );
		}
	}
	
	public boolean isConnected(){
		synchronized( channelLock ){
			return ( channel != null 
					&& channel.isConnected() );
		}
	}

	public boolean isActive(){
		synchronized( channelLock ){
			return ( channel != null 
					&& channel.isOpen()
					&& channel.isConnected() );
		}
	}
	
	public boolean isConnectionPending(){
		synchronized( channelLock ){
			return ( channel != null 
					&& channel.isConnectionPending() );
		}
		
	}


	/**
	 * callback function, 被连接池的Receiver线程调用，每个实例连接池的实现者必须实现此方法。
	 * 在一次响应数据的接受过程中，该方法会被多次调用。实现将数据写入Buffer的过程。
	 * 由于连接池虚类不限制client端的Buffer对象实现方式，所以必须每个开发者自行完成。
	 * 另外注意：不要把已有的数据清除。
	 * @return true  - if a complete reponse body has been received
	 * @throws Exception  - IOException is thrown if there is any IO problem.
	 *                      or NullPointerException for hell code.
	 */
	protected abstract int handleInput() throws IOException;
	/**
	 * callback function , by Sender
	 * @param request
	 * @return bytes sent.
	 *  >0 request has been successfully sent
	 *  =0 request needn't to be sent
	 *  <0 some Exception happeds, need reset
	 */
	public abstract int sendRequest() throws IOException;
	/**
	 * callback function, 被连接池的Receiver线程调用，每个实例连接池的实现者必须实现此方法。
	 * 在一次响应数据接收过程中，该方法会被多次调用，实现者必须每次都要检查接收数据已经完整，
	 * 是的话，就返回true，否则返回false。同时注意，Buffer对象是有状态的，不要将已经接收到的数据覆盖掉。
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean finishResponse() throws IOException;

	/**
	 * callback function, by receiver or sender, if some Exceptions thrown.
	 * client被释放后被调用，用于执行清理操作，以备下一个请求复用。一般在这里做输入、输出buffer的清理操作。
	 *
	 */
	public abstract void reset() throws IOException;
	

	public void connect(InetSocketAddress addr) throws IOException {
		SocketChannel channel = SocketChannel.open(); 
		channel.configureBlocking(false);
		channel.connect(addr);
		channel.finishConnect();
		
		synchronized( channelLock ){
			this.channel = channel;
		}
	}
	protected abstract Log getLogger();

	public AsyncRequest getRequest() {
		return request;
	}

	
	public void setRequest(AsyncRequest request) {
		this.request = request;
	}

	/**
	 * called by Receiver. when getting data from recver-queue.
	 * to determin if it has send request by itself
	 * @return
	 */
	public boolean requestSent() {
		return requestSent;
	}

	/**
	 * set by user-thread. before putting request to recver-queue.
	 * @param needRequest
	 */
	public void requestSent(boolean needRequest) {
		this.requestSent = needRequest;
	}

	public SocketChannel getChannel() {
		return channel;
	}

//	public void setChannel(SocketChannel channel) {
//		this.channel = channel;
//	}
//
}
