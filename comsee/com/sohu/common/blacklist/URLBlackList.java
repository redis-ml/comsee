package com.sohu.common.blacklist;

import java.io.IOException;

import com.sohu.common.platform.DocId;
import com.sohu.common.util.LongHashSet;

public class URLBlackList {

	String filename;
	LongHashSet set;
	
	private static final URLBlackList instance = new URLBlackList();
	
	public static final URLBlackList getInstance() {
		return instance;
	}
	
	public void init() throws IOException {
		if( filename == null ){
			return;
		}
		set = new LongHashSet(filename );
	}
	
	public int reload(){
		if( filename == null ){
			return -1;
		}
		try{
			set = new LongHashSet(filename );
			return 0;
		}catch( IOException e){}
		
		return -2;
	}
	
	/**
	 * 判断某URL对应的docid是否在列表中
	 * @param key
	 * @return
	 */
	public boolean contains(long key){
		if( set != null ){
			int ret = set.contains(key);
			if( ret == 1 ){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断某个URL是否在该列表中
	 * @param url
	 * @return
	 */
	public boolean contains(String url){
		long key = url2key(url);
		
		return contains(key);
	}
	
	private static final long url2key(String url ){
		byte[] out = DocId.url2docId(url);
		return byte2long(out, 8); 
	}
	
	public static final long byte2long( byte[] out , int start){
		long ret = 0;
		for( int i=0; i<8;i++){
			ret |= ( (((long)out[i+start])&0xFFl) << (i<<3) );
		}
		return ret;
	}
	
	public boolean insert( long key ){
		if( set != null ){
			int ret = set.add( key );
			if( ret >= 0 ){
				return true;
			}
		}
		return false;
	}
	
	public boolean delete( long key ){
		if( set != null ){
			int ret = set.delete( key );
			if( ret == 1 ){
				return true;
			}
		}
		return false;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

}
