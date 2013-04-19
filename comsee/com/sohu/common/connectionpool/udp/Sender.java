package com.sohu.common.connectionpool.udp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Sender implements Runnable{

	private static final Log log = LogFactory.getLog(Sender.class);
	
	private String generation = "(ThreadErr)"; 
	private volatile Thread _thread = null;
	AsyncGenericConnectionPool pool;
	
	private long yieldTime = 50;
	private long minSleepTime = 500;
	private long maxSleepTime = 1200000l;
	private long sleepTime;
	
	private long requestCount = 0;
	private Object requestCountLock = new Object();
	
	private volatile boolean needRestart = false;
	
	private static int GENERATION = 0;
	private static Object GENERATION_LOCK = new Object();
	private static int newGeneration(){
		int ret = 0;
		synchronized( GENERATION_LOCK ){
			ret = ++ GENERATION;
		}
		return ret;
	}
	
	
	Sender( AsyncGenericConnectionPool sc ){
		this.pool = sc;
	}
	
	/**
	 * 线程状态检测
	 * @return true - 如果发送线程还在存活.
	 */
	public boolean isAlive(){
		return this._thread !=null && this._thread.isAlive();
	}
	
	/**
	 * external API.
	 *  called by Receiver or User-Thread
	 *  to make sure the Sender Thread is Alive
	 */
	public void checkSenderThread(){
		boolean toRestart = false;
		synchronized( requestCountLock ){
			if( requestCount > 0 ){
				toRestart = needRestart;
				if( needRestart ){
					needRestart = false;
				}else{
					requestCountLock.notifyAll();
				}
			}
		}
		if( toRestart ){
			startThread();
		}
	}
	/**
	 * External API.
	 *  called by Anyone at anytime
	 *  to get a snapshot of request count num.
	 * @return
	 */
	public long getRequestCount(){
		return requestCount;
	}

	/**
	 * 线程启动
	 *
	 */
	public void startThread(){
		sleepTime = minSleepTime;
		this.generation = pool.name+"(Sender" + newGeneration() + ")";
		_thread = new Thread(this, this.generation);
		_thread.start();
	}
	/**
	 * 线程停止
	 * *注意* 运行stopThread并不意味着线程马上停止
	 *
	 */
	public void stopThread(){
		_thread = null;
	}
	
	public void run() {
		while (true) {
			if( _thread != Thread.currentThread() ){
				if( log.isInfoEnabled() ){
					log.info( this.generation + "EXIST. NOT CURRTHREAD");
				}
				break;
			}
			long cycleStart = System.currentTimeMillis();
			if( log.isTraceEnabled() ){
				log.trace(generation +"CycleStart:time:"+cycleStart );
			}
			boolean doneSomething = false;
			do{
				
				ServerStatus[] sss = null;
				sss = pool.getAllStatus();
				if (sss == null) {
					break;
				}
				for (int i = 0; i < sss.length; i++) {
					long now = System.currentTimeMillis();
					if( log.isTraceEnabled() ){
						log.trace(generation + "CheckServer:"+i+" ,time:"+now);
					}
					ServerStatus ss = sss[i];
					if (ss == null){
						if( log.isTraceEnabled() ){
							log.trace(generation + "CheckServer:"+i+",ServerStatus is NULL, toContinue");
						}
						continue;
					}

					while(true) {

						if( log.isTraceEnabled() ){
							log.trace(generation + "CheckServer:"+i+",to innerSendRequest()");
						}
						int status = ss.innerSendRequest();
						if( log.isTraceEnabled() ){
							log.trace(generation + "CheckServer:"+i+",innerSendRequest() returns " + status + ", free:" + ss.freeChannelList.size());
						}

						if (status == 1) {
							// 可用连接全部已被占用, 或没有请求
							break;
						} else if (status <= 0) {
							synchronized (requestCountLock) {
								requestCount--;
							}
							doneSomething = true;
						}
					}
					if( log.isTraceEnabled() ){
						log.trace(generation +"SenderServerEnd:"+i+" ,time:"+(System.currentTimeMillis()-now) +", hasFree:" + (!doneSomething));
					}
					Thread.yield();
				}
			} while (false);
			
			if (doneSomething) {
				sleepTime = minSleepTime;
			}
			if( log.isTraceEnabled() ){
				log.trace(generation +"CycleEnd:time:"+(System.currentTimeMillis()-cycleStart)
						+ ",sleepTime:"	+ sleepTime + ",minSleepTime:"  + minSleepTime 
						+ ",maxSleepTime:" + maxSleepTime );
			}
			
			// spare-time task
			if (sleepTime >= maxSleepTime) {
				if( log.isInfoEnabled() ){
					log.info(generation +"sleepTime Too Long, to Exsit Thread");
				}
				synchronized (requestCountLock) {
					if( requestCount == 0 ){
						needRestart = true;
						_thread = null;
						return;
					}
				}
				if( log.isInfoEnabled() ){
					log.info(generation +"sleepTime Too Long, doesn't Exsit Thread");
				}
				sleepTime = minSleepTime;
			}
			try {
				long now = System.currentTimeMillis();
				if( log.isInfoEnabled() ){
					log.info(generation +"CheckSleep(2),requestCount:" + requestCount + ",start:" + now);
				}
				synchronized( requestCountLock ){
					if( requestCount == 0 ){
						requestCountLock.wait(sleepTime);
						sleepTime <<= 1;
					} else {
						requestCountLock.wait(yieldTime);
					}
				}
				if( log.isInfoEnabled() ){
					log.info(generation +"CheckSleep(2)End, time:" + (System.currentTimeMillis()-now) );
				}
			} catch (InterruptedException e) {
				// 未知状况, 不知道该怎么做.
				Thread.interrupted();
				_thread = null;
				needRestart = true;
				e.printStackTrace();
				if( log.isWarnEnabled() ){
					log.warn(generation +"Interrupted. Farewell");
				}
			}
			if( log.isInfoEnabled() ){
				log.info(generation +"CycleLoopback, time:" + (System.currentTimeMillis()-cycleStart) );
			}
		}
	}
	/**
	 * 发送请求
	 * 由用户线程通过AsyncConnectionPool.sendRequest调用
	 * @param request
	 * @return
	 */
	int senderSendRequest( AsyncRequest request ){
		
		ServerStatus ss = request.getServer();
		
		assert( ss != null );
		
		int ret = ss.serverSendRequest(request);
		
		if( ret == 1 ){
			synchronized( requestCountLock ){
				requestCount ++;
			}
			if( log.isTraceEnabled() ){
				log.trace(this.generation+"request Count is "+ requestCount);
			}
			checkSenderThread();
		}
		request.setConnectionErrorStatus(ret);
		return ret;
	}


	public long getMaxSleepTime() {
		return maxSleepTime;
	}


	public void setMaxSleepTime(long maxSleepTime) {
		this.maxSleepTime = maxSleepTime;
	}


	public long getMinSleepTime() {
		return minSleepTime;
	}


	public void setMinSleepTime(long minSleepTime) {
		this.minSleepTime = minSleepTime;
	}


	public long getYieldTime() {
		return yieldTime;
	}


	public void setYieldTime(long yieldTime) {
		this.yieldTime = yieldTime;
	}
	
}
