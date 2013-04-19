package com.sohu.common.platform;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.sohu.common.util.StringUtils;

public class DocId {

	byte[] docId;
	
	static int Flag_bits = 4;     /// gDocID_t中预留的Flag所占	的位数
	static int DomainID_bits = 48;
	static int HostID_bits = 36;
	static int UrlID_bits = 40;
	
	public static byte[] url2docId256(String url) {
		if (url == null)
			return null;

		UrlInfo info = new UrlInfo(url);
		byte[] ret = null;
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] b = new byte[16];
			byte[] out = new byte[32];
			info.getDomainSign(md, b, 0, b.length);
			// System.out.println(StringUtils.encodeHex(b));
			System.arraycopy(b, 0, out, 0, 8);
			info.getHostSign(md, b, 0, b.length);
			// System.out.println(StringUtils.encodeHex(b));
			System.arraycopy(b, 0, out, 8, 8);
			info.getUrlSign(md, b, 0, b.length);
			// System.out.println(StringUtils.encodeHex(b));
			System.arraycopy(b, 0, out, 16, 16);
			ret = out;
		} catch (NoSuchAlgorithmException e) {
		} catch (DigestException e) {
		}
		return ret;
	}
	
	public static byte[] url2docId(String url ){
		
		if( url == null ) return null;
		
		UrlInfo info = new UrlInfo(url);
		
		byte[] ret = null;
		try{
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] b = new byte[16];
			byte[] out = new byte[16];
			info.getDomainSign(md, b, 0, b.length);
//			System.out.println( "domain_md5: ");
//			System.out.println( StringUtils.encodeHex(b));
			maskFill( b, 0, out, 4, 48);
			info.getHostSign(md, b, 0, b.length);
//			System.out.println( "host_md5: ");
//			System.out.println( StringUtils.encodeHex(b));
			maskFill( b, 0, out, 48 + 4, 36);
			info.getUrlSign(md, b, 0, b.length);
//			System.out.println( "url_md5: ");
//			System.out.println( StringUtils.encodeHex(b));
			maskFill( b, 0, out, 48 + 4 + 36, 40);
			ret = out;
		} catch(NoSuchAlgorithmException e ){
		} catch( DigestException e){
		}
		return ret;
	}
	
	public static void maskFill( byte[]src, int src_start, byte[] dst, int dst_start, int bit_len){
		
		for( int i = 0; i < bit_len ; i ++ ){
			updateBit( src, src_start + i, dst, dst_start + i);
		}
		
	}
	
	static byte[] nega_mask = {
		(byte)0xFE, 
		(byte)0xFD,
		(byte)0xFB,
		(byte)0xF7,
		(byte)0xEF,
		(byte)0xDF,
		(byte)0xBF,
		(byte)0x7F,
		
	};
	static byte[] posi_mask = {
		(byte)0x01, 
		(byte)0x02,
		(byte)0x04,
		(byte)0x08,
		(byte)0x10,
		(byte)0x20,
		(byte)0x40,
		(byte)0x80,
		
	};
	static void updateBit( byte[] src, int src_start, byte[] dst, int dst_start){
		int src_i = (src_start & 0x07);
		int dst_i = (dst_start & 0x07);
		
		int src_index = (src_start >> 3);
		int dst_index = (dst_start >> 3);
		
		int bit = ( (int)src[ src_index ] >> src_i ) & 0x01;
		
		if( bit == 0 ){
			byte mask = nega_mask[ dst_i ];
			dst[ dst_index ] &= mask; 
		} else {
			byte mask = posi_mask[ dst_i ];
			dst[ dst_index ] |= mask;
		}
		
	}
	
	public static byte[] docId256_to_128(byte[] b){
		byte[] out = new byte[16];
		maskFill( b, 0, out, 4, 48);
//		System.out.println(StringUtils.encodeHex(out));
		maskFill( b, 64, out, 48 + 4, 36);
//		System.out.println(StringUtils.encodeHex(out));
		maskFill( b, 128, out, 48 + 4 + 36, 40);
//		System.out.println(StringUtils.encodeHex(out));
		return out;
	}
	
	public static void main(String[] args){
		String url = "www.sogou.com";
		byte[] docid256 = url2docId256(url);
		System.out.println(StringUtils.encodeHex(docid256));
		byte[] docid128 = url2docId(url);
		System.out.println(StringUtils.encodeHex(docid128));
		byte[] tmp = new byte[33];
		System.arraycopy(docid256, 0, tmp, 0, 32);
		System.out.println(StringUtils.encodeHex(tmp));
		System.out.println(StringUtils.encodeHex(docId256_to_128(tmp)));

	}

}
