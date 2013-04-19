/*
 * Created on 2005-4-11
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sohu.common.encoding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sohu.common.encoding.CharMap;
/**
 * @author liumingzhu
 *
 */
public class CharsetConverter {

    // log对象
    private static Log logger = LogFactory.getLog(CharsetConverter.class);
    
    private CharsetConverter(){
    }
    /**
     * @param n 用户自己指定码表
     */
    public CharsetConverter(int n){
    	files = new String[n];
    	maps = new CharMap[n];
    }
    
	private String[] files = null;
	private CharMap[] maps = null; 
	
	public void setFiles(String[] f){
		if( f == null ) return;
		for( int i=0; i<files.length && i<f.length; i++){
			files[i] = f[i];
		}
	}
	/**
	 * 初始化过程。
	 * 将简->繁,繁->简的码表分别装入内存.
	 */
	public  void init() {
		String[] fileNames = files;

		for( int i=0; i<fileNames.length && i<maps.length; i++ ){
			String filename = fileNames[i];
			if( filename == null ) {
			if( logger.isWarnEnabled() ){
				logger.warn("NOT LOADING MAP:" + i);
			}
			continue;
			}
			try {
				if( maps[i] == null ){
					maps[i] = new CharMap();
				}
				maps[i].load(filename);
				if (logger.isInfoEnabled()) {
					logger.info("loaded Unicode map from "+ filename);
				}
			} catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("Invalid Char Mapping file: " + filename, e);
				}
			}
		}
	}
	
	/**
	 * @param idx 码表序号
	 * @param ch 需要转码的字符
	 * @return 根据码表转码后的字符
	 */
	public  char map(int idx, char ch ){
		CharMap map = (idx < maps.length) ? maps[idx] : null;
		return (map == null) ? ch : map.map(ch);
	}
	
	/**
	 * @param idx 码表序号
	 * @param str 需要转码的字符串
	 * @return 转码后的字符串
	 */
	public String map(int idx,String str){
		if(str == null) return null;
		
		StringBuffer buff = new StringBuffer(str.length());
		for(int i=0;i<str.length();i=i+1)
			buff.append(map(idx,str.charAt(i)));
		return buff.toString();
	}
}
