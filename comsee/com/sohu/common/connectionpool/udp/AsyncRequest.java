package com.sohu.common.connectionpool.udp;

import java.lang.ref.WeakReference;

import com.sohu.common.connectionpool.Request;


public abstract class AsyncRequest implements Request{
	Object identityObj;
	long requestId;
	String serverInfo;
	int serverId;
	boolean isProbe = false;
	
	/**
	 * 统计时间相关
	 */
	// set by pool, to indicate time status 
	protected Long time_start = Long.valueOf(0l); // 进入发送情求队列的时间
	
	protected Long time_enqueue = Long.valueOf(0l);
	protected Long time_enqueue_end = Long.valueOf(0l);
	protected Long time_outqueue = Long.valueOf(0l);
	
	protected Long time_waitqueue = Long.valueOf(0l);
	protected Long time_outwaitqueue = Long.valueOf(0l);
	
	protected Long time_connect = Long.valueOf(0l); // 发起连接
	protected Long time_connect_end = Long.valueOf(0l); // 连接建立的时间( 假如需要重连的情况下) 
	protected Long time_trySend = Long.valueOf(0l); // sender试图发送的时间
	protected Long time_request = Long.valueOf(0l); // 请求从socket发送出去的时间
	protected Long time_handleInput = Long.valueOf(0l); // 最后一次接收响应的时间
	protected Long time_end = Long.valueOf(0l); // 响应接收完全的时间
	protected Long time_ioend = Long.valueOf(0l);
	protected Long endDumpTime = Long.valueOf(0l);
	// set by user thread. to indicate time status
	Long cancelledTime = Long.valueOf(0l);
	
	protected  Long startLocktime=Long.valueOf(0l);
	protected  Long endLocktime=Long.valueOf(0l);
	
	
	/**
	 * 搜索结果相关
	 */
	Object result;
	boolean isResultReady = false;
	boolean isResultReadOnly = false;
	Object resultLock = new Object();
	
	/**
	 * 请求相关
	 */
	ServerStatus server = null;
	
	WeakReference ref;
	
	//  0, 初始状态
	// -1, 排队超时
	// -3, 零字节发送
	// -2, socket超时
	// -4, 服务器故障
	// -5, connect超时
	public int status = 0;
	String reason;
	
	public void queueSend(){
		this.time_start = System.currentTimeMillis();
	
	}
//	public void messageSent(){
//		this.time_request = System.currentTimeMillis();
//	}
	public void messageRecv(){
		this.time_end = System.currentTimeMillis();
	}
	public void startConnect(){
		this.time_connect = System.currentTimeMillis();
	}
	public void requestTime(){
		this.time_request = System.currentTimeMillis();
	}
	
	public void setResult(Object obj){
		this.time_end = System.currentTimeMillis();
		synchronized ( resultLock ){
			if( ! isResultReadOnly ){
				result = obj;
				isResultReady = true;
				isResultReadOnly = true;
				resultLock.notify();
			}
		}
	}
	
	public Object getResult(long time) {
		synchronized( resultLock ){
			if( ! isResultReady && !isResultReadOnly ){
				if( time > 0 ){
					try{
						resultLock.wait(time);
					} catch( InterruptedException e){}
					cancelledTime = System.currentTimeMillis();
				}
			}
			isResultReadOnly = true;
		}
		return result;
		
	}
	
	/**
	 * callback函数
	 * 当请求在请求队列中超时时调用.
	 *
	 */
	public void waitTimeout(){
		setResult( null );
		this.status = -1;
		this.reason = "排队超时";
		this.time_outwaitqueue();
	}
	public void serverDown(){
		setResult(null);
		this.status = -4;
		this.reason = "服务器不可用";
	}
	public void illegalRequest(){
		setResult(null );
		this.status = -3;
		this.reason = "非法请求";
	}
	public void connectTimeout(){
		setResult(null);
		this.status = -5;
		this.reason = "连接超时";
		this.time_connect_end();
	}
	public void socketTimeout(){
		setResult(null);
		this.status = -2;
		this.reason = "socket超时";
		this.timeIoend();
	}
	public void decodeFailed(){
		setResult(null);
		this.status = -6;
		this.reason = "解码失败";
		this.timeIoend();
	}
	public void poolDown(){
		setResult(null);
		this.status = -7;
		this.reason = "连接池状态异常";
	}

	
	public long getEndTime() {
		return time_end;
	}

	public WeakReference getRef() {
		return ref;
	}
	public void setRef(WeakReference ref) {
		this.ref = ref;
	}
	public long getStartTime() {
		return time_start;
	}

	public String getServerInfo() {
		return serverInfo;
	}

	public final long getTime() {
		long ret = getIoTime(Long.MIN_VALUE);
		if( ret == Long.MIN_VALUE ){
			ret = userWaitTime();
		}
		return ret;
	}

	public void setRequestId(long id) {
		this.requestId = id;
	}
	
	public long getRequestId(){
		return this.requestId;
	}

	public void setServerInfo(String info) {
		this.serverInfo = info;
	}

	public final void setTime(long t) {
		
	}
	public abstract boolean isValid();
	public abstract int getServerId(int total);
	public long getCancelledTime() {
		return cancelledTime;
	}
	public void setCancelledTime(long cancelledTime) {
		this.cancelledTime = cancelledTime;
	}
	public long getConnectEndTime() {
		return time_connect_end;
	}
	public long getEndDumpTime() {
		return endDumpTime;
	}
	public long getRequestTime() {
		return time_request;
	}
	
	public String dumpTimeStatus(){
		return ( "status:" + status + ", id:" + this.requestId + ", start:"+ time_start
				+ ", wait_q:" + ((time_waitqueue==0)?"NUL":String.valueOf(time_waitqueue-time_start))
				+ ", wait_q_out:" + ((time_outwaitqueue==0)?"NUL":String.valueOf(time_outwaitqueue-time_start))
				+ ", reg_q:" + ((time_enqueue==0)?"NUL":String.valueOf(time_enqueue-time_start))
				+ ", reg_q_end:" + ((time_enqueue_end==0)?"NUL":String.valueOf(time_enqueue_end-time_start))
				+ ", reg_q_out:" + ((time_outqueue==0)?"NUL":String.valueOf(time_outqueue-time_start))
				+ ", req:" +  ((time_request==0)?"NUL":String.valueOf(time_request-time_start))
				+ ", ioend:" + getIoTime()
				+ ", conn:" + ((time_connect==0)?"NUL":String.valueOf(time_connect-time_start))
				+ ", conn_end:" + ((time_connect_end==0)?"NUL":String.valueOf(time_connect_end-time_start))
				+ ", end:" +  ((time_end==0)?"NUL":String.valueOf(time_end-time_start))
				+ ", userEnd:" + ((cancelledTime==0)?"NUL":String.valueOf(cancelledTime - time_start))
				+ ", reason:" + reason
				+ ", serverInfo" + this.getServerInfo()
				+ ", start:" + time_start
				+ ", wait_q:" + time_waitqueue
				+ ", wait_q_out:" + time_outwaitqueue
				+ ", reg_q:" + time_enqueue
				+ ", reg_q_end:" + time_enqueue_end
				+ ", reg_q_out:" + time_outqueue
				+ ", try_send:" + time_trySend
				+ ", req:" + time_request
				+ ", last_in:" + time_handleInput
				+ ", ioend:" + time_ioend
				+ ", conn:" + time_connect
				+ ", conn_end:" + time_connect_end
				+ ", cancelTime:" + cancelledTime
				);

	}
	public ServerStatus getServer() {
		return server;
	}
	public void setServer(ServerStatus server) {
		this.server = server;
	}
	public void timeIoend(){
		this.time_ioend = System.currentTimeMillis();
	}
	public long getIoTime(){
		return getIoTime(0);
	}
	public long getIoTime(long defaultValue){
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
	public long getConnectTime(){
		return ((time_connect_end==0||time_connect==0)?0:(time_connect_end-time_connect));
	}

	public void time_connect() {
		this.time_connect = System.currentTimeMillis();
	}

	public void time_enqueue() {
		this.time_enqueue = System.currentTimeMillis();
	}

	public void time_enqueue_end() {
		this.time_enqueue_end = System.currentTimeMillis();
	}

	public void time_outqueue() {
		this.time_outqueue =System.currentTimeMillis();
	}
	public void time_connect_end(){
		this.time_connect_end = System.currentTimeMillis();
	}
	public void time_waitqueue() {
		this.time_waitqueue =System.currentTimeMillis();
	}
	public void time_outwaitqueue(){
		this.time_outwaitqueue = System.currentTimeMillis();
	}
	public void setQueueTime(long t ){
		
	}
	public long getQueueTime(){
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
	public long userWaitTime(){
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
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	/**
	 * @return the time_handleInput
	 */
	public Long getTime_handleInput() {
		return time_handleInput;
	}
	/**
	 * @param time_handleInput the time_handleInput to set
	 */
	public void time_handleInput() {
		this.time_handleInput = System.currentTimeMillis();
	}
	/**
	 * @return the time_trySend
	 */
	public Long getTime_trySend() {
		return time_trySend;
	}
	/**
	 * @param time_trySend the time_trySend to set
	 */
	public void time_trySend() {
		this.time_trySend = System.currentTimeMillis();
	}
	/**
	 * @return the identityObj
	 */
	public Object getIdentityObj() {
		return identityObj;
	}
	/**
	 * @param identityObj the identityObj to set
	 */
	public void setIdentityObj(Object identityObj) {
		this.identityObj = identityObj;
	}
	/**
	 * @return the serverId
	 */
	public int getServerId() {
		return serverId;
	}
	/**
	 * @param serverId the serverId to set
	 */
	public void setServerId(int serverId) {
		this.serverId = serverId;
	}
	/**
	 * @return the isProbe
	 */
	public boolean isProbe() {
		return isProbe;
	}
	/**
	 * @param isProbe the isProbe to set
	 */
	public void setProbe(boolean isProbe) {
		this.isProbe = isProbe;
	}
	/**
	 * @dinghui
	 */
	int ConnectionErrorStatus=0;
	
	public void setConnectionErrorStatus(int ConnectionErrorStatus){
		this.ConnectionErrorStatus=ConnectionErrorStatus;
	}
	
	public int getConnectionErrorStatus(){
		return this.ConnectionErrorStatus;
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
