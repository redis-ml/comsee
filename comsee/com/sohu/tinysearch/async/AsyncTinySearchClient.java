package com.sohu.tinysearch.async;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sohu.common.connectionpool.async.AsyncGenericQueryClient;
import com.sohu.common.connectionpool.async.AsyncRequest;
import com.sohu.tinysearch.TinyClientImpl;
import com.sohu.tinysearch.TinySearchRequest;
import com.sohu.tinysearch.TinySearchResult;
import com.sohu.tinysearch.TinySearchResultItem;

public class AsyncTinySearchClient extends AsyncGenericQueryClient {

	// 结果的条数限制
	private static final int MAX_TINY_ITEMS = 10;

	private static Log log = LogFactory.getLog( TinyClientImpl.class );
	
	private static Charset cs = Charset.forName("GBK");
	private static CharsetEncoder ce = cs.newEncoder();
	private static CharsetDecoder cd = cs.newDecoder();
	
	private ByteBuffer obb = ByteBuffer.allocate( 65536 );
	private ByteBuffer ibb = ByteBuffer.allocate( 65536 );
	private CharBuffer ocb = CharBuffer.allocate( 65536 );
	private CharBuffer icb = CharBuffer.allocate( 65536 );
	
	private String parity;

	protected Log getLogger() {
		return log;
	}

	private static int hash = 0;
	private static synchronized int getHash(){
		return hash++;
	}
	private int ObjectId  = getHash();
	public boolean in =false;
	
	public int sendRequest( ) throws IOException {
		this.life --;
		
		SocketChannel channel = getChannel();
		if( channel == null ) return 0;
		
		TinySearchRequest qr = (TinySearchRequest) this.getRequest();
		this.parity = "parity:" + ObjectId +' '+ qr.getRequestId();

		if( log.isDebugEnabled() ){
			log.debug(parity +" query: begin" );
		}

		ocb.clear();
		
		try{
			ocb.put( "cmd:query\n" );
			ocb.put( "query:" );
			
			String query = null;
			try {
				query = qr.getQuery().trim();
			}catch ( NullPointerException e){}
			// 避免查询词为一串空格的情况
			
			if( query == null || query.length() == 0 ) return -1;
			
			ocb.put( query );
			
			String reqType = qr.getReqType();
			if( reqType != null ){
				ocb.put("\nreq_type:");
				ocb.put(reqType);
			}
			
			
			//判断是否来自输入法，如果来自输入法的搜索助手的话，则需要获取查询词信息
			if ( qr.isIME() ) {
				ocb.put( "\ntinyengine:1");
			}
			
			
			String type = qr.getType();
			if( type != null ){
				ocb.put("\ntype:");
				ocb.put(type);
			}
			
			int pornlevel = qr.getPornlevel();
			
			ocb.put("\npornlevel:");
			ocb.put( String.valueOf(pornlevel) );
			
			String uuid=qr.getUUID();
			if(uuid!=null){
				ocb.put("\nuuid:");
				ocb.put(uuid);
			}
			
			ocb.flip();
			
			obb.clear();
			CoderResult cr = ce.encode( ocb, obb, true );
			
			if( cr.isOverflow() 
					|| cr.isError()	){
				if( log != null && log.isDebugEnabled() ){
					log.debug("suspicious invalid gbk code:"+query);
				}
				return -2;
			}
			obb.put((byte)'\n');
			obb.put((byte)'\n');
	
			obb.flip();
	
			// 发送数据
			int total_len = obb.remaining();
			// write返回0失败的次数
			int retry = 0;
			try{
				while( obb.remaining() > 0 ){
					long n = channel.write( obb );
					if( n == 0 ) retry ++;
					if( retry > 6 ) {
						// jvm可能有bug，曾检测到某个channel始终发送不出去。
						String msg = "Can't Send Data thru SChannel! Bug maybe... retry " + retry + " times.";
						if( log != null && log.isErrorEnabled() ){
							log.error("luke:" + msg);
						}
						throw new IOException(msg);
					}
				}
			}catch( IllegalArgumentException e){
				// 兼容jdk的bug, 该bug在1.6.0_u1才修复.
				throw (IOException)new IOException().initCause(e);
			}
	
			return total_len;
		}catch( BufferOverflowException  e){
			if( log != null && log.isTraceEnabled() ){
				log.trace("TinyBufferOverflow:",e);
			}
		}
		return 0;
	}

	/**
	 * 从服务器处接收查询结果.
	 * @return
	 */
	public int handleInput() throws IOException
	{
		int n=0;
		try{
			SocketChannel channel = getChannel();
			if( channel != null ){
				n = channel.read(ibb);
			}
		}catch( BufferOverflowException e){
			throw (IOException)new IOException().initCause(e);
		}
		return n;
	}
	public boolean finishResponse() throws IOException{
		
		byte[] b = ibb.array();
		int len = ibb.position();
		boolean finished = false;
		
		if( len > 2 ){
			if( b[len-1]==(byte)'\n'
					&& b[len-2]==(byte)'\n'
					&& b[len-3]==(byte)'\n'){
				finished = true;
			}
		}else {
			if( b[0]==(byte)'\n'
					&& b[1]==(byte)'\n'){
				finished = true;
			}
		}
		
		if( ! finished){
			return false;
		}
		AsyncRequest request = this.getRequest();
		if( request != null )
			request.timeIoend();
		else {
			return true;
		}
		ibb.flip();
		icb.clear();
		CoderResult cr = cd.decode(ibb, icb, true);
		
		while( cr.isError()
				&& ibb.remaining() > 0 
				&& !cr.isOverflow() ){
			ibb.get();
			cr = cd.decode(ibb, icb, true );
		}
		if( cr.isOverflow() 
				|| cr.isError() ){
			request.decodeFailed();
			return true;
		}
		
		icb.flip();

		TinySearchResult result = new TinySearchResult();

		// ////////////////////////////////////////
		// parity code
		boolean matched = false;
		matched = true;

		int itemsonpage = 0;
		// int ad_count = 0;

		// ///////////////////////////////////////////////////////////
		//
		// 1. 分析头部信息
		//
		String line = null;

		BufferedReader reader = new BufferedReader(new CharArrayReader(icb
				.array(), 0, icb.remaining() ));

		line = reader.readLine();
		//line = "query_category:0x10:0x01:北京	上海";

		while (line != null && (line.length() != 0 || matched == false) ) { // 当遇到空行的时候结束
			int p = line.indexOf(':');
			String value = "";
			if (p >= 0)
				value = line.substring(p + 1);
			try {
				if (line.startsWith("qc:") || line.startsWith("qr:")) {
					result.setQrHeader(line);
					int q = line.indexOf(':', p + 1);
					result.setQrNum(Integer.parseInt(line.substring(p + 1, q)));
					p = q;
					q = line.indexOf(':', q + 1);
					result
							.setQrType(Integer.parseInt(line
									.substring(p + 1, q)));
					result.setQr(line.substring(q + 1));
				} else if (line.startsWith("hint:")) {
					result.setHintHeader(line);
					int q = line.indexOf(':', p + 1);
					result.setHintNum(Integer
							.parseInt(line.substring(p + 1, q)));
					result.setHint(line.substring(q + 1));
				} else if (line.startsWith("tiny_count:")) {
					itemsonpage = Integer.parseInt(value);
				} else if (line.startsWith("query_category:")){
					/**
					 * 	tinyEngine
					 *	格式为query_category:0x00a0:0x00020:北京\t上海
					 *	第一个为64bit 长整数,每位为一个类别，以后可能会有多个类别，JAVA中为有符号整数，需要注意处理溢出
					 *	第二个为32bit 整数，每位为一个标志。可能有多个小标志，同注意处理溢出
					 *	第三部分为qc返回的查询词,用\t分隔，至多2个关键字，在EngineTab里面进行处理
					 */
					result.setQaHeader(line);
					int q = line.indexOf(':', p+1);
//					设置大类别
					//当为0x10000000000..的时候，表示查询词为问答类
					if (line.substring(p+3,p+4).equalsIgnoreCase("1")) {
						result.setQaCategory((long)-1);
					} else {
						//正常判断
						result.setQaCategory(Long.decode(line.substring(p+1, q)));
					}
					p = q;
//					设置关键特征
					q = line.indexOf(':', p+1);
					
					//出现了0xFFFFFFFF的情况，超过大小了
					Long sign = Long.decode(line.substring(p+1, q));
					if (sign > Integer.MAX_VALUE) {
						result.setQaSignid(-1);
					} else {
						result.setQaSignid(Integer.decode(line.substring(p+1, q)));
					}
					p = q;
//					获取关键字
					result.setQaWord(line.substring(p+1));
				}

				// ////////////////////////////////////////
				// parity code
				else if (line.startsWith("parity:")) {
					if (parity.equals(line)) {
						matched = true;
					} else {
						matched = false;
						if( log != null && log.isFatalEnabled() ){
							log.fatal(parity	+ " ##NON-Matched parity####");
						}
					}
				}
			} catch (NumberFormatException e) {
				// 忽略
			} catch (IndexOutOfBoundsException e) {
				// 忽略
			}
			line = reader.readLine();
		}

		int validItemNum = 0;
		int beginIndex;

		if (itemsonpage > 0) {
			if (itemsonpage > MAX_TINY_ITEMS)
				validItemNum = MAX_TINY_ITEMS;
			else
				validItemNum = itemsonpage;
		}
		beginIndex = 0;

		ArrayList items = null;

		if (validItemNum <= 0)
			items = null;
		else
			items = (new ArrayList(validItemNum));

		result.setItems(items);

		int itemindex = 0;
		int resultIndex = 0;

		// /////////////////////////////////////////////////////////////
		//
		// 2. 逐个分析tinysearch item
		//
		line = reader.readLine(); // 读空行后的一行
		while (line != null && (line.length() != 0 && resultIndex < itemsonpage) ) { // 当遇到两个空行的时候结束

			if (resultIndex >= beginIndex && items != null
					&& items.size() < validItemNum) { // 当结果为需要的结果，且还有存储空间的时候存储

				TinySearchResultItem item = new TinySearchResultItem();
				items.add(item);
				while (line != null && line.length() != 0) { // 当遇到空行的时候结束
					int p = line.indexOf(':');
					String value = "";
					if (p >= 0)
						value = line.substring(p + 1);
					if (line.startsWith("type:")) {
						try {
							item.setType(Integer.parseInt(value));
						} catch (NumberFormatException e) {
							// 忽略
						}
					} else if (line.startsWith("content:")) {
						item.setValue(value);
					}
					line = reader.readLine();
				}
				itemindex++;
			} else {
				while (line != null && line.length() != 0) { // 当遇到空行的时候结束
					line = reader.readLine();
				}
			}
			++resultIndex;
			line = reader.readLine(); // 读空行后的一行
		}

		// 遇到两个空行, 结束.
		if (line != null && line.length() == 0) {
			request.setResult(result);
			return true;
		}

		// ////////////////////////////////////////////////////////////////////////
		//
		// 3. 读入ad的header
		//
		while (line != null && line.length() != 0) { // 当遇到两个空行的时候结束

			int p = line.indexOf(':');
			if (p > 0) {
				// String value = line.substring(p+1);
				if (line.startsWith("ad_count:")) {

					// try{
					// ad_count = Integer.parseInt( value );
					// }catch( NumberFormatException e){
					//							
					// }
				}
			}
			line = reader.readLine(); // 读空行后的一行
		}

		// ////////////////////////////////////////////////////////////////////////
		//
		// 4. 读入AD内容
		//
		line = reader.readLine();
		// 设置ad的条数及读取数量
		resultIndex = 0;
		while (line != null && line.length() != 0) {
			TinySearchResultItem item = new TinySearchResultItem();
			while (line != null && line.length() != 0) { // 当遇到空行的时候结束
				int p = line.indexOf(':');
				String value = "";
				if (p >= 0)
					value = line.substring(p + 1);
				if (line.startsWith("type:")) {
					try {
						item.setType(Integer.parseInt(value));
					} catch (NumberFormatException e) {
						// 忽略
					}
				} else if (line.startsWith("content:")) {
					item.setValue(value);
				}
				line = reader.readLine();
			}
			result.setAdItem(item.getType(), item);

			line = reader.readLine(); // 读空行后的一行
		}

		// ////////////////////////////////////////////////////////////////////////
		// 
		// 结束
		//
		// // 清空接收缓冲区
		// while(reader.ready()){
		// line = reader.readLine();
		// log.fatal(parity+" From Hell:"+line);
		// }
		// ///////////////////// test lingyi ///////////////////////

		request.setResult(result);
		return true;
	}


	public void reset() throws IOException {
		this.ibb.clear();
		this.obb.clear();
		setRequest( null );
	}


}
