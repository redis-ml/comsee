package com.sohu.common.platform;

import java.nio.ByteBuffer;

public class DocIdUtil {

	public static final byte[] parseDocid256(String data){
		if( data == null ){
			return null;
		}
		
		data = data.trim();
		if( data.startsWith("http")){ // url形式的，docid中一定没有't'
//			//只需后128位
			byte[] tmp = DocId.url2docId256(data);
			byte[] ret = new byte[16];
			System.arraycopy(tmp, 16, ret, 0, ret.length);
			return ret;
		} else{
			ByteBuffer bb = ByteBuffer.allocate(data.length()/2);
			int i = 0;
			while(i<data.length()-1){
				if( data.charAt(i) == '-'){
					i++;
				} else {
					try{
						int a = Integer.parseInt(data.substring(i, i+2), 16);
						bb.put((byte)a);
						i+= 2;
					}catch(Exception e){
						break;
					}
				}
			}
			if( i != data.length() || bb.position() == 0){
				return null;
			} else {
				byte[] tmp = bb.array();
				if(tmp.length == 16)//传入的是128位docid
					return tmp;
				byte[] ret = new byte[16];
				System.arraycopy(tmp, 16, ret, 0, ret.length);
				return ret;
			}
		}
	}
	
	public static final byte[] parseDocid(String data){
		if( data == null ){
			return null;
		}
		
		data = data.trim();
		if( data.startsWith("http")){ // url形式的，docid中一定没有't'
			return DocId.url2docId(data);
		} else{
			ByteBuffer bb = ByteBuffer.allocate(data.length()/2);
			int i = 0;
			while(i<data.length()-1){
				if( data.charAt(i) == '-'){
					i++;
				} else {
					try{
						int a = Integer.parseInt(data.substring(i, i+2), 16);
						bb.put((byte)a);
						i+= 2;
					}catch(Exception e){
						break;
					}
				}
			}
			if( i != data.length() || bb.position() == 0){
				return null;
			} else {
				// revert byte arrar before return
				byte[] out = bb.array();
				if(out.length == 16){	//传入的是128位docid
					for(int idx=0;idx<out.length/2;idx++){
						byte tmp = out[idx];
						out[idx] = out[out.length - 1 - idx];
						out[out.length-1-idx] = tmp;
					}
					return out;
				}
				byte[] ret = DocId.docId256_to_128(out);
				return ret;
			}
		}
	}
}
