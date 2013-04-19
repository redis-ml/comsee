package com.sohu.common.connectionpool;

//import java.io.IOException;
//import java.net.InetAddress;
import java.net.InetSocketAddress;
//import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ServerConfig {
	
	ServerStatus [] status = null;
	/// 发生多少次错误后,启动sleep机制
	protected int maxErrorsBeforeSleep = 2;
	///	发生错误后，多长时间内不再重新尝试
	protected long sleepMillisecondsAfterTimeOutError = 60000;
	protected long sleepMillisecondsAfterQueueTimeOut = 0;
	
	private static Pattern pat = Pattern.compile("\\s+");
	
	public ServerConfig(){
		
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
	
	public void initServerConfig(String config) throws IllegalArgumentException {
		
		ArrayList servers = new ArrayList();
		
		if ( config == null ) throw new IllegalArgumentException("config is NULL");
		
			String[] list = pat.split( config );
	
			for (int i = 0 ; i < list.length ; i++ ) {
				ServerStatus ss = new ServerStatus( list[i] );
				servers.add( servers.size() , ss);

			}

			ServerStatus serverStatus []= (ServerStatus[])servers.toArray( new ServerStatus[servers.size()] );
			
		this.setStatus( serverStatus );
		
	}
	
	public synchronized int getServerIdCount(){
		if( this.status == null ){
			return 0;
		}else {
			return this.status.length;
		}
	}
	public synchronized InetSocketAddress getSocketAddress( int i ){
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

	public void setStatus(ServerStatus[] status) {
		this.status = status;
	}
	public boolean isServerAvaliable(int i){
		long now = System.currentTimeMillis();
		
		ServerStatus ss = null;
		if( status !=null && i>=0 && i< status.length){
			ss = status[i];
		}
		if( ss == null ){
			return false;
		} else {
			synchronized( ss ){
				// 如果出现请求排队超时, 则应该sleep较长的时间.
				// 因为排队超时不易出现, 一旦出现, 会有较多用户受到影响.
				if( ss.queueTimeoutNumber > maxErrorsBeforeSleep 
						&& (now - ss.queueDownTime ) < sleepMillisecondsAfterQueueTimeOut ){
					return false;
				} else { 
					ss.queueTimeoutNumber = 0;

					return ( ss.recentErrorNumber <= maxErrorsBeforeSleep
						|| (now - ss.downtime ) >= sleepMillisecondsAfterTimeOutError);
				}
			}
		}
	}

	public int getMaxErrorsBeforeSleep() {
		return maxErrorsBeforeSleep;
	}

	public void setMaxErrorsBeforeSleep(int maxErrorsBeforeSleep) {
		this.maxErrorsBeforeSleep = maxErrorsBeforeSleep;
	}

	public long getSleepMillisecondsAfterTimeOutError() {
		return sleepMillisecondsAfterTimeOutError;
	}


	public long getSleepMillisecondsAfterQueueTimeOut() {
		return sleepMillisecondsAfterQueueTimeOut;
	}

	public void setSleepMillisecondsAfterQueueTimeOut(
			long sleepMillisecondsAfterQueueTimeOut) {
		this.sleepMillisecondsAfterQueueTimeOut = sleepMillisecondsAfterQueueTimeOut;
	}

	public void setSleepMillisecondsAfterTimeOutError(
			long sleepMillisecondsAfterTimeOutError) {
		this.sleepMillisecondsAfterTimeOutError = sleepMillisecondsAfterTimeOutError;
	}

}