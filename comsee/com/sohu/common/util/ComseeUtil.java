package com.sohu.common.util;

import com.sohu.common.connectionpool.Request;
/**
 * Comsee公用包的一些方法，这些方法都依赖于comsee包，是对comsee包的操作。
 * 因此，如果只依赖于JDK的方法，请不要放入这里面。
 * @author Cui Weibing
 * @date 2009.6.1
 */
public class ComseeUtil {
	/**
	 * 根据传入的请求信息，更新指定的server信息，用于输出日志。
	 * @param qr 封装请求相关的信息
	 * @param serverInfoBuffer 存放server信息的buffer
	 */
	public static void updateServerInfo(Request qr, StringBuffer serverInfoBuffer){
		String tmp = null;
		String status = "-1000";
		String time = "-1";
		if( qr != null ){
			tmp = qr.getServerInfo();
			status = String.valueOf(qr.getStatus());
			time = String.valueOf(qr.getTime());
		}
		if( tmp == null ){
			tmp = "null";
		}
		serverInfoBuffer.append( tmp );
		serverInfoBuffer.append( '&' );
		serverInfoBuffer.append( status );
		serverInfoBuffer.append( '&' );
		serverInfoBuffer.append( time );
		serverInfoBuffer.append( '_' );
	}

}
