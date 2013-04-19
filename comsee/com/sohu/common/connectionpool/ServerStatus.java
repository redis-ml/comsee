package com.sohu.common.connectionpool;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class ServerStatus {

	Object key;
	int recentErrorNumber;
	int queueTimeoutNumber;
	long downtime;
	long queueDownTime;
	InetSocketAddress addr;
	String serverInfo;
	
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

	public ServerStatus(){}
	
	public ServerStatus(String line ) throws IllegalArgumentException{
		if( line == null ) throw new IllegalArgumentException();
		
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
			throw new IllegalArgumentException( "invalid line:" + line , e );
		}catch( SecurityException e ){
			throw new IllegalArgumentException( "invalid line:" + line , e );
		}catch( IllegalArgumentException e){
			throw new IllegalArgumentException( "invalid line:" + line , e );
		}
		
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
		this.recentErrorNumber ++;
		this.downtime = System.currentTimeMillis();
	}
	public synchronized void transferTimeout() {
		this.recentErrorNumber ++;
		this.downtime = System.currentTimeMillis();
	}
	public synchronized void queueTimeout() {
		this.queueTimeoutNumber ++;
		this.queueDownTime = System.currentTimeMillis();
	}
	public synchronized void success() {
		if( queueTimeoutNumber <= 0 )
			this.recentErrorNumber /= 2;
	}

	public InetSocketAddress getAddr() {
		return addr;
	}

	public void setAddr(InetSocketAddress addr) {
		this.addr = addr;
	}

	public int getQueueTimeoutNumber() {
		return queueTimeoutNumber;
	}

	public void setQueueTimeoutNumber(int queueTimeoutNumber) {
		this.queueTimeoutNumber = queueTimeoutNumber;
	}

}