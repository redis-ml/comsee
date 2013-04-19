/*
 * Created on 2003-11-23
 *
 */
package com.sohu.common.connectionpool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.logging.Log;

/**
 * 连接cache的客户端
 * 
 * @author Mingzhu Liu (mingzhuliu@sohu-inc.com)
 *  
 */
public abstract class QueryClientImpl implements QueryClient {

	// 记录器
	protected Log logger = getLogger();

	protected Socket socket = null;
	protected BufferedReader reader = null;
	protected OutputStream os = null;
	
	// 连接的生命值.低于0表示不可靠.
	protected int life = 0;
	
	public synchronized void setLife(int lf) {
		this.life = lf;
	}
	
	public synchronized int getLife(){
		return this.life;
	}

	/**
	 * 新建一个客户端
	 */
	public QueryClientImpl() {
	}


	/**
	 * 关闭连接
	 */
	public synchronized void close() {
		try {
			if (os != null){
				try{
					os.close();
				}catch(Exception e){
					logger.debug(this,e);
				}
			}
			if (reader != null){
				try{
					reader.close();
				}catch(Exception e){
					logger.debug(this,e);
				}
			}
			if (socket != null) {
				try {
					socket.close();
				} catch (Exception e) {
					logger.debug(this, e);
				}
			}
		} finally {
			if (socket != null) {
				if (!socket.isClosed())
					logger.debug("EERROR:socket: NOT isClosed");
				if (socket.isConnected())
					logger.debug("EERROR:socket:isConnected");
			} else {
				logger.debug("EERROR:socket:==NULL");
			}
			reader = null;
			os = null;
			socket = null;
		}
	}


	public synchronized boolean isValid() {
		if( life <= 0 || socket==null || socket.isClosed() || (!socket.isConnected() ))
			return false;
		else 
			return true;
	}

	public synchronized void connect(InetSocketAddress addr, int connectTimeoutMills, int socketTimeoutMillis) throws IOException {
		Socket tmpSocket = new Socket();
		tmpSocket.connect(addr, connectTimeoutMills);
		tmpSocket.setSoTimeout( socketTimeoutMillis );
		BufferedReader tmpReader = new BufferedReader( new InputStreamReader(tmpSocket.getInputStream() ,"GBK"));
//		PrintWriter tmpWriter = new PrintWriter( new OutputStreamWriter( tmpSocket.getOutputStream(), "GBK"), false );
		OutputStream tmpOs = tmpSocket.getOutputStream();
		
		this.socket = tmpSocket;
		this.reader = tmpReader;
		this.os = tmpOs;
	}
	protected abstract Log getLogger();
	/**
	 * 向cache服务器提交查询
	 * 
	 * @param request
	 * @return
	 */
	public abstract Result query(Request objRequest) throws IOException ;

}
