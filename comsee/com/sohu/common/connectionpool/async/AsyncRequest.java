package com.sohu.common.connectionpool.async;

import java.lang.ref.WeakReference;

import com.sohu.common.connectionpool.Request;


public abstract class AsyncRequest implements Request{
	volatile long requestId;
	volatile String serverInfo; 
	protected volatile String ruid = "";   //uniq id per request
	
	public static final int NORMAL_REQUEST = 0;
	public static final int RETRY_REQUEST = 1;
	public static final int SHADOW_NORMAL_REQUEST = -1;
	public static final int SHADOW_QUEUE_REQUEST = -2;
	
	/**
	 * 统计时间相关
	 */
	// set by pool, to indicate time status 
	protected volatile Long time_start = Long.valueOf(0l); // 进入发送情求队列的时间
	
	protected volatile Long time_enqueue = Long.valueOf(0l);
	protected volatile Long time_enqueue_end = Long.valueOf(0l);
	protected volatile Long time_outqueue = Long.valueOf(0l);
	
	protected volatile Long time_waitqueue = Long.valueOf(0l);
	protected volatile Long time_outwaitqueue = Long.valueOf(0l);
	
	protected volatile Long time_connect = Long.valueOf(0l); // 发起连接
	protected volatile Long time_connect_end = Long.valueOf(0l); // 连接建立的时间( 假如需要重连的情况下) 
	protected volatile Long time_request = Long.valueOf(0l); // 请求从socket发送出去的时间
	protected volatile Long time_end = Long.valueOf(0l); // 响应接收完全的时间
	protected volatile Long time_ioend = Long.valueOf(0l);
	protected volatile Long endDumpTime = Long.valueOf(0l);
	// set by user thread. to indicate time status
	volatile Long cancelledTime = Long.valueOf(0l);
	
	protected volatile Long startLocktime=Long.valueOf(0l);
	protected volatile Long endLocktime=Long.valueOf(0l);
	
	/**
	 * 搜索结果相关
	 */
	volatile Object result;
	volatile boolean isResultReady = false;
	volatile boolean isResultReadOnly = false;
	Object resultLock = new Object();
	
	/**
	 * 为clone之后的object准备的子类request, 此request用来记录需要发送的各项信息
	 */
	volatile public AsyncRequest clonedFrom = null;
	volatile public AsyncRequest clonedTo = null;
	volatile public AsyncRequest actualRequest = null;
	volatile public boolean clonableRequest = false;
	
	//实际处理的请求
	public AsyncRequest timedRequest(){
		if (actualRequest != null){
			return actualRequest;
		}
		return this;
	}
	/*
	 * connectType取值 NORMAL_REQUEST：普通请求
	 *                RETRY_REQUEST: 总是发送给自己对应的server （用于重连尝试的请求）
	 *                SHADOW_REQUEST: 总是不发送给自己对应的server（用于在短超时时重试的请求）
	 */
	volatile int connectType = NORMAL_REQUEST; 
	public boolean cloned(){
		return clonableRequest&&(clonedFrom!=null||clonedTo!=null);
	}
	
	public AsyncRequest clone(){
		AsyncRequest clone = new AsyncRequest() {
			
			@Override
			public boolean isValid() {
				if (this.clonedFrom != null)
					return this.clonedFrom.isValid();
				return false;
			}
			
			@Override
			public int getServerId(int total) {
				if (this.clonedFrom != null)
					return this.clonedFrom.getServerId(total);
				return -1;
			}
		};
		clone.clonedFrom = this;
		this.clonedTo = clone;
		clone.requestId = this.requestId;
		clone.ruid = this.ruid;
		
		return clone;
	}
	
	/**
	 * 请求相关
	 */
	volatile ServerStatus server = null;
	
	@SuppressWarnings("rawtypes")
	WeakReference ref;
	
	//  0, 初始状态
	// -1, 排队超时
	// -3, 非法请求
	// -2, socket超时
	// -4, 服务器故障
	// -5, connect超时
	//-6, 解码失败
	//-7, 用户等待超时
	//当增加、修改、删除一个status时，应相应地增加、修改、删除ReturnTypeStatusMap中的映射关系
	public volatile int status = 0;
	String reason;
	
	public final void queueSend(){
		this.time_start = System.currentTimeMillis();
	
	}
//	public void messageSent(){
//		this.time_request = System.currentTimeMillis();
//	}
	public final void messageRecv(){
		this.time_end = System.currentTimeMillis();
	}
	public final void startConnect(){
		this.time_connect = System.currentTimeMillis();
	}
	public final void requestTime(){
		this.time_request = System.currentTimeMillis();
	}
	
	public final void setResult(Object obj){
		//debug bart
		if (this.clonableRequest||this.clonedFrom!=null){
			System.out.println("[pool "+this.ruid+" "+(System.currentTimeMillis()-this.time_start)+"]get result from "+this.getServerInfo()+((this.clonedFrom != null)?" cloned":" orignal"));
		}
		
		synchronized ( resultLock ){
			if (!isResultReady){
				//request完成，记录相关信息
				isResultReady = true;
				this.time_end = System.currentTimeMillis();
				markStats();
				
				//request未变为只读，修改result
				if (!isResultReadOnly){
					result = obj;
				}
				
				if (this.clonedFrom != null){
					//作为影子请求，需要赋值给主请求
					synchronized ( this.clonedFrom.resultLock ){
						if (!this.clonedFrom.isResultReadOnly){
							this.clonedFrom.result = obj;
						}
						//这里并不执行赋值，只是判断是否可以通知用户线程
						this.clonedFrom.requestDone(obj, true);
					}
				}else{
					//这里并不执行赋值，只是判断是否可以通知用户线程
					this.requestDone(obj, false);
				}
			}
		}
	
	}
	
	//isResultReadOnly为true，表示用户等待超时，或者已经有非空结果，或者原请求和clone请求均失败
	//                        这些情况都表示处理已完成，需要通知用户线程，并且结束计时
	//isResultReady为ture，表示该request已经完成，但是其clone节点未必完成自己的请求
	private final void requestDone(Object obj, boolean fromClone){
		if (!isResultReadOnly){
			boolean done = false;
			
			if (obj != null){
				//有结果，肯定结束
				if (fromClone){
					actualRequest = this.clonedTo;
				}
				done = true;
			}else{
				//空结果，当存在clone节点时，只有当两个节点都不为空，才触发nofify
				if (fromClone){
					if (this.isResultReady){
						actualRequest = this.clonedTo;
						done = true;
					}
				}else{
					if (this.clonedTo == null || this.clonedTo.isResultReady){
						done = true;
					}
				}
			}
			if (done){
				isResultReadOnly = true;
				resultLock.notify();
			}
		}
	}
	
	/*
	 * 统计本次请求
	 */
	private void markStats(){
		if (!clonableRequest&&this.clonedFrom==null){
			return;
		}
		ServerStatus ss = this.server;
		if (ss != null){
			ss.mark(this);
			//debug bart
			//System.out.println("[pool]"+ss.serverInfo+" cloned times: "+ss.totalClone);
			//System.out.println("[pool]"+ss.serverInfo+" avg time: "+ss.getServerAvgTime(1));
		}
	}
	
	public final Object getResult(long time) {
		long currTime = System.currentTimeMillis();
		synchronized( resultLock ){
			if( ! isResultReady && !isResultReadOnly ){
				if( time > 0 ){
					try{
						resultLock.wait(time);
					} catch( InterruptedException e){}
					cancelledTime = System.currentTimeMillis();
				}
				if(result == null && (System.currentTimeMillis() - currTime >= time)) {
					userWaitTimeOut();
				}
			}
			isResultReadOnly = true;
			return result;
		}
	}
	
	/**
	 * callback函数
	 * 当请求在请求队列中超时时调用.
	 *
	 */
	public final void waitTimeout(){
		this.status = -1;
		this.reason = "排队超时";
		this.time_outwaitqueue();
		setResult( null );
	}
	@Override
	public final void serverDown(){
		serverDown("未知原因");
	}
	public final void serverDown(String reason){
		this.status = -4;
		this.reason = "服务器不可用:"+reason;
		setResult(null);
	}
	public final void illegalRequest(){
		this.status = -3;
		this.reason = "非法请求";
		setResult(null );
	}
	public final void connectTimeout(){
		this.status = -5;
		this.reason = "连接超时";
		this.time_connect_end();
		setResult(null);
	}
	@Override
	public final void socketTimeout(){
		this.status = -2;
		this.reason = "socket超时";
		this.timeIoend();
		setResult(null);
	}
	public final void invalidResponse(String rr){
		this.status = -8;
		this.reason = "非法响应"+rr;
		this.timeIoend();
		setResult(null);
	}
	public final void decodeFailed(){
		this.status = -6;
		this.reason = "解码失败";
		this.timeIoend();
		setResult(null);
	}
	public final void userWaitTimeOut() {
		this.status = -7;
		this.reason = "用户等待超时";
	}
	
	public final long getEndTime() {
		return time_end;
	}

	@SuppressWarnings("rawtypes")
	public final WeakReference getRef() {
		return ref;
	}
	@SuppressWarnings("rawtypes")
	public final void setRef(WeakReference ref) {
		this.ref = ref;
	}
	public final long getStartTime() {
		return time_start;
	}

	@Override
	public final String getServerInfo() {
		return serverInfo;
	}

	@Override
	public final long getTime() {
		long ret = getIoTime(Long.MIN_VALUE);
		if( ret == Long.MIN_VALUE ){
			ret = userWaitTime();
		}
		return ret;
	}

	@Override
	public void setRequestId(long id) {
		this.requestId = id;
	}
	
	@Override
	public final long getRequestId(){
		return this.requestId;
	}

	@Override
	public final void setServerInfo(String info) {
		this.serverInfo = info;
	}

	@Override
	public final void setTime(long t) {
		// dummy time是算出来的，不能赋值。
	}
	public abstract boolean isValid();
	public abstract int getServerId(int total);
	public final long getCancelledTime() {
		return cancelledTime;
	}
	public final void setCancelledTime(long cancelledTime) {
		this.cancelledTime = cancelledTime;
	}
	public final long getConnectEndTime() {
		return time_connect_end;
	}
	public final long getEndDumpTime() {
		return endDumpTime;
	}
	public final long getRequestTime() {
		return time_request;
	}
	
	public final String dumpTimeStatus(){
		return ( "status: " + status + ", start: "+ time_start
				+ ", wait_q:" + ((time_outwaitqueue==0)?0:(time_outwaitqueue-time_start))
				+ ", q:" + ((time_enqueue==0)?0:(time_enqueue-time_start))
				+ ", q_end:" + ((time_enqueue_end==0)?0:(time_enqueue_end-time_start))
				+ ", q_out:" + ((time_outqueue==0)?0:(time_outqueue-time_start))
				+ ", req: " +  ((time_request==0)?0:(time_request-time_start))
				+ ", ioend:" + getIoTime()
				+ ", conn:" + ((time_connect==0)?0:(time_connect-time_start))
				+ ", conn_end:" + ((time_connect_end==0)?0:(time_connect_end-time_start))
				+ ", end: " +  ((time_end==0)?0:(time_end-time_start))
				+ ", userEnd:" + ((cancelledTime==0)?0:(cancelledTime - time_start))
				+ ", reason:" + reason
				);

	}
	public final ServerStatus getServer() {
		return server;
	}
	public final void setServer(ServerStatus server) {
		this.server = server;
	}
	public final void timeIoend(){
		this.time_ioend = System.currentTimeMillis();
	}
	public final long getIoTime(){
		return getIoTime(0);
	}
	public final long getIoTime(long defaultValue){
		if( time_request > 0 ){
			if( time_ioend > 0 ){
				return time_ioend - time_request;
			} else {
				return System.currentTimeMillis() - time_request;
			}
		} else {
			return defaultValue;
		}
	}
	public final long getConnectTime(){
		return ((time_connect_end==0||time_connect==0)?0:(time_connect_end-time_connect));
	}

	public final void time_connect() {
		this.time_connect = System.currentTimeMillis();
	}

	public final void time_enqueue() {
		this.time_enqueue = System.currentTimeMillis();
	}

	public final void time_enqueue_end() {
		this.time_enqueue_end = System.currentTimeMillis();
	}

	public final void time_outqueue() {
		this.time_outqueue =System.currentTimeMillis();
	}
	public final void time_connect_end(){
		this.time_connect_end = System.currentTimeMillis();
	}
	public final void time_waitqueue() {
		this.time_waitqueue =System.currentTimeMillis();
	}
	public final void time_outwaitqueue(){
		this.time_outwaitqueue = System.currentTimeMillis();
	}
	public final void setQueueTime(long t ){
		
	}
	public final long getQueueTime(){
		if( this.time_request > 0 ){ // 请求已经发送
			return this.time_request - this.time_start;
		} else if( this.time_waitqueue > 0 ){ // 请求进入队列
			if( this.time_outqueue > 0 ){
				return this.time_outqueue - this.time_start;
			} else if( this.time_enqueue > 0 ){
				return this.time_enqueue - this.time_start;
			} else if( this.time_outwaitqueue > 0 ){
				return this.time_outwaitqueue - this.time_start;
			} else {
				return System.currentTimeMillis() - this.time_start;
			}
					
		} else if( this.time_enqueue > 0 ) { // 请求已经进入注册队列
			if( this.time_outqueue > 0 ){
				return this.time_outqueue - this.time_start;
			} else {
				return System.currentTimeMillis() - this.time_start;
			}
		} else {
			return 0;
		}
	}
	public final long userWaitTime(){
		if( time_start > 0 ){
			
			if( cancelledTime > 0 ){
				return cancelledTime - time_start; 
			} else {
				return System.currentTimeMillis() - time_start;
			}
		} else {
			return 0;
		}
	}
	@Override
	public final int getStatus() {
		return status;
	}
	@Override
	public final void setStatus(int status) {
		this.status = status;
	}

	public boolean isResultReady() {
		return isResultReady;
	}

	public void setResultReady(boolean isResultReady) {
		this.isResultReady = isResultReady;
	}

	public boolean isResultReadOnly() {
		return isResultReadOnly;
	}

	public void setResultReadOnly(boolean isResultReadOnly) {
		this.isResultReadOnly = isResultReadOnly;
	}
	
	public void startLocktime(){
		this.startLocktime=System.currentTimeMillis();
	}
	
	public void endLocktime(){
		this.endLocktime=System.currentTimeMillis();
	}
	
	public long getLocktime(){
		if(startLocktime>0l&&endLocktime>0l){
			return endLocktime-startLocktime;
		}else if(startLocktime>0l){
			return System.currentTimeMillis()-startLocktime;
		}else{
			return 0l;
		}		
	}
}
