/**
 * 用于接收服务器的响应内容. 检查超时查询, 注册新channel, 完成连接流程.
 * @author liumingzhu
 */
package com.sohu.common.connectionpool.async;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


class Receiver implements Runnable{
	
	private static final Log logger = LogFactory.getLog( Receiver.class);

	String generation = "(RecverErr)";
	volatile Thread _thread;
	private LinkedList selectionKeyQueue = new LinkedList();
	AsyncGenericConnectionPool pool ;
	
	private static int GENERATION = 0;
	private static Object GENERATION_LOCK = new Object();
	private static int newGeneration(){
		int ret = 0;
		synchronized( GENERATION_LOCK ){
			ret = ++ GENERATION;
		}
		return ret;
	}
	
	Receiver(AsyncGenericConnectionPool p){
		this.pool = p;
		this.generation = "(RecverErr)";
	}

	void queueChannel(AsyncGenericQueryClient obj){
		synchronized( selectionKeyQueue){
			selectionKeyQueue.offer( obj );
		}
		if( logger.isTraceEnabled() ){
			logger.trace(this.generation + "Add one Client to Busyqueue");
		}
	}
	
	public void startThread(){
		this.generation = this.pool.name + "(Recver" + newGeneration() + ")";
		_thread = new Thread(this, this.generation );
		_thread.start();
	}
	public void stopThread(){
		_thread = null;
	}

	/**
	 * 发送请求
	 * returns 
	 *  -1 如果发生socket异常, 则关闭该连接, 并通知user-thread
	 *   0 请求非法, 通知user-thread. 归还连接备用
	 *  
	 */
	private int sendRequest(AsyncGenericQueryClient sc){
		int status = -1;
		AsyncRequest request = sc.getRequest();
		
		if( request == null ) 
			return 0;
		try{
			status = sc.sendRequest();
		}catch( IOException e){
			if( logger.isInfoEnabled() ){
				logger.info(this.generation+"IOE Sending Request");
			}
			request.serverDown("[rcv]发送抛异常"+e.getMessage());
			sc.serverStatus.sendError();
			sc.close();
		} catch( RuntimeException e){
			if( logger.isErrorEnabled() ){
				logger.error(this.generation+"RTE Sending Request", e);
			}
			
		} catch( Exception e){
			if( logger.isInfoEnabled() ){
				logger.info(this.generation+"OtherException Sending Request", e);
			}
		}
		request.requestTime();
		// 记录请求发送时间, 以备检查超时
		sc.setTime_request();
		if( status <= 0 ){
			if(status == 0) {
				request.illegalRequest();
			}else {
				request.serverDown("[rcv]发送失败:"+status);
			}
			try{
				sc.reset();
			}catch( Exception e){
				if( logger.isWarnEnabled() ){
					logger.warn(this.generation+"Exception while reseting Request after sendRequest", e);
				}
			}
			sc.serverStatus.freeClient(sc);
		} else {
			sc.requestSent(true);
		}

		return status;

	}
	private void registerNewChannel(){
		long start = System.currentTimeMillis();
		if( logger.isTraceEnabled() ){
			logger.trace(this.generation+"Register:" + start );
		}
		
		synchronized( selectionKeyQueue ){
			do {

				AsyncGenericQueryClient sc = (AsyncGenericQueryClient)selectionKeyQueue.poll();
				
				if( sc == null ) break;
				
				AsyncRequest request = sc.getRequest();
				
				if( request == null ) break;
				
				request.time_outqueue();
				
				int option;
				if( sc.isConnected() ){
					request.time_connect_end();
					if( !sc.requestSent() ){
						if( sendRequest(sc) < 0 ){
							continue;
						}
					}
					option = SelectionKey.OP_READ;
				} else if( sc.isConnectionPending() ){
					option = SelectionKey.OP_CONNECT;
				} else {
					/**
					 *  not connected and no connection pending. 
					 *  This means client did send a request sometime before, but the server
					 *  refused it quickly. this happens when this client is in register_queue.
					 */
					request.serverDown("[reg]收到read/conn以外的消息");
					sc.close();
					sc.serverStatus.freeClient(sc);
					if( logger.isInfoEnabled() ){
						logger.info( this.generation + "QuicklyClosedConnection!");
					}
					continue;
				}

				boolean needReturnClient = true;
				try {
					SocketChannel channel = sc.getChannel();
					if( channel != null ){
						channel.register(pool.selector, option, sc);
						needReturnClient = false;
					}
				} catch (ClosedChannelException e) {
					/**
					 * 请求已经超时
					 */
					request.serverDown("[reg]socket被关闭");
					sc.serverStatus.sendError();
					if (logger.isWarnEnabled()) {
						logger.warn(this.generation
								+ "Register:Closed SocketChannel!", e);
					}

				} catch (IllegalBlockingModeException e) {
					// Should Never Happen!
					if (logger.isWarnEnabled()) {
						logger.warn(this.generation
								+ "Register:IllegalBlocking SocketChannel!", e);
					}
					// 忽略
				} catch (IllegalSelectorException e) {
					// Should Never Happen
					if (logger.isWarnEnabled()) {
						logger.warn(this.generation
								+ "Register:IllegalSelector Why?", e);
					}
					// 忽略
				} catch (CancelledKeyException e) {
					// Should Never Happen
					if (logger.isWarnEnabled()) {
						logger.warn(this.generation
								+ "Register:Cancelled SocketChannel", e);
					}
					// 忽略
				} catch (IllegalArgumentException e) {
					// Should Never Happen
					if (logger.isWarnEnabled()) {
						logger.warn(this.generation
								+ "Register:PLEASE CHECK CODE!", e);
					}
					// 忽略
				}
				if( needReturnClient ){
					sc.serverStatus.freeClient(sc);
				}
			} while (true);
		}
		long end = System.currentTimeMillis();
		if( logger.isTraceEnabled() ){
			logger.trace(this.generation+"Register:" + (end-start) );
		}

	}
	
	
	private void checkSocketTimeoutChannel(){
		long start = System.currentTimeMillis();
		if( logger.isTraceEnabled() ){
			logger.trace(this.generation+"CheckTimeout:" + start );
		}
		ServerStatus[] sss = this.pool.getAllStatus();
		if( sss != null ){
			for( int i =0; i<sss.length; i++ ){
				long tstart = System.currentTimeMillis();
				if( logger.isTraceEnabled() ){
					logger.trace(this.generation+"CheckTimeout:Server:"+ i +"At:" + tstart );
				}
				sss[i].checkTimeout();
				if( logger.isTraceEnabled() ){
					logger.trace(this.generation+"CheckTimeout:Server:"+ i +"Time:" + (System.currentTimeMillis()-tstart) );
				}
			}
		}
		long end = System.currentTimeMillis();
		if( logger.isTraceEnabled() ){
			logger.trace(this.generation+"CheckTimeoutEnd:" + (end-start) );
		}
	}

	/**
	 * 暂时不考虑cancel的情况.
	 */
	public void run(){
		while(_thread == Thread.currentThread() ){
			try{
				long cycleStart = System.currentTimeMillis();
				if( logger.isTraceEnabled() ){
					logger.trace(this.generation+"CycleStart:" + cycleStart);
				}
	//			Thread.yield();
				registerNewChannel();
				checkSocketTimeoutChannel();
				if( logger.isTraceEnabled() ){
					logger.trace(this.generation+"SelectAt:" + (System.currentTimeMillis()-cycleStart) + ",robin:" + pool.robinTime);
				}
				int num = pool.selector.select(pool.robinTime);
				if( logger.isTraceEnabled() ){
					logger.trace(this.generation+"SelectEnd,KeyNum( num = )" + num + ",At:" + (System.currentTimeMillis()-cycleStart) );
				}
				// caused by timeout
				if( num == 0 ){
					continue;
				}
	
				Set set = pool.selector.selectedKeys();
				Iterator it = set.iterator();
				while( it.hasNext() ){
					SelectionKey key = (SelectionKey)it.next();
					it.remove();
					try{
						if( key.isReadable() ){
							if( logger.isTraceEnabled() ){
								logger.trace(this.generation+"ReadKey:" + (System.currentTimeMillis()-cycleStart) );
							}
							AsyncGenericQueryClient conn = (AsyncGenericQueryClient )key.attachment();
							
							try{
								int status = conn.handleInput();
								if( status > 0 ){
									if( conn.finishResponse() ){
										//debug bart
										//System.out.println("[pool]finishResponse from "+conn.getRequest().getServerInfo());
										if( logger.isTraceEnabled() ){
											logger.trace(this.generation + "Handle InPut End");
										}
										conn.serverStatus.success();
										conn.reset();
										conn.serverStatus.freeClient(conn);
									}
								} else if( status < 0 ) {
									
										// socket Closed by Server
									AsyncRequest request = conn.getRequest();
									if( request != null ){
										// 一般是-1，这里搜狗对返回码做了扩展，
										// < -50 表示是自定义的错误码，通常都是服务器端的响应不合法
										if( status > -50 ){
											request.serverDown("[rcv]接收数据报错:"+status);
										} else {
											request.invalidResponse("[rcv]接收数据报错:"+status);
										}
									}
									if( conn.requestSent() ){
										conn.serverStatus.sendError();
									}
		
									if( logger.isTraceEnabled() ){
										logger.trace(this.generation + "Handle InPut End, Server Close");
									}
									conn.reset();
									conn.close();
									conn.serverStatus.freeClient(conn);
								}
							} catch ( Exception e){
								key.cancel();
								conn.serverStatus.sendError();
								AsyncRequest request = conn.getRequest();
								if( request != null ){
									request.serverDown("未知异常"+e.getMessage());
								}
								conn.close();
								conn.serverStatus.freeClient(conn);
								if(logger.isWarnEnabled() ){
									logger.warn(this.generation + "IOE while Handle Input",e);
								}
							}
							if( logger.isTraceEnabled() ){
								logger.trace(this.generation+"ReadKeyEnd:" + (System.currentTimeMillis()-cycleStart) );
							}
						} else if( key.isConnectable() ){
							if( logger.isTraceEnabled() ){
								logger.trace(this.generation+"ConnKey:" + (System.currentTimeMillis()-cycleStart) );
							}
		
							AsyncGenericQueryClient conn = (AsyncGenericQueryClient )key.attachment();
							
							AsyncRequest request = conn.getRequest();
							if( request != null ){
								request.time_connect_end();
							}
							int status = -1;
							
							try{
								if( ((SocketChannel)(key.channel())).finishConnect() ){
									status = 1;
									if( logger.isTraceEnabled() ){
										logger.trace(this.generation + "finishConnect");
									}
								}
							}catch( IOException e){
								if( logger.isWarnEnabled() ){
									logger.warn(this.generation + ":CONN FAILED: " + e.getMessage(), e );
								}
							}
							
							if( status == 1 ){
								status = sendRequest( conn );
								if( logger.isTraceEnabled() ){
									logger.trace( this.generation + "Send the Request");
								}
								if( status < 0 ){
									if( logger.isTraceEnabled() ){
										logger.trace( this.generation + "Cancel the key");
									}
									key.cancel();
								} else {
									if( logger.isTraceEnabled() ){
										logger.trace(this.generation + "Change the Status to OPREAD");
									}
									key.interestOps(SelectionKey.OP_READ );
								}
							} else {
								if( logger.isTraceEnabled() ){
									logger.trace(this.generation + "ServerDown");
								}
								if( request != null ){
									request.serverDown("连接失败");
								}
								conn.serverStatus.connectTimeout();
								conn.serverStatus.freeClient(conn);
							}
							if( logger.isTraceEnabled() ){
								logger.trace(this.generation+"ConnKeyEnd:" + (System.currentTimeMillis()-cycleStart) );
							}
						}
					}catch( CancelledKeyException e ){
						// ignore
						if( logger.isTraceEnabled() ){
							logger.trace(this.generation + "CancelledKey!");
						}
					}
				}
			}catch(IOException e){
				e.printStackTrace();
			}catch( ClosedSelectorException e){
				e.printStackTrace();
			}catch( IllegalArgumentException e){
				// shall never happen
				e.printStackTrace();
			} catch( Throwable e){
				e.printStackTrace();
			}
		}
	}
}
