package com.sohu.common.encoding;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;

/**
 * singleton模式的拼音词表, 字符(char)-字符串(String)对应关系
 * 使用前需要初始化. PinYin.getInstance().init( {词表文件} );
 * 如果没有初始化, 则getPinYin方法得到空串.
 * 
 * @author liumingzhu
 * @version 1.0
 *
 */
public class PinYin {
	
	// 永久存在的数组
	private String[] map = new String[65536];
	// 单体实例
	private static PinYin instance = new PinYin();
	// 配置文件名, 方便reload()方法.
	String defaultFileName = null;
	
	/**
	 * public的构造方法, 便于实现多个实例
	 *
	 */
	public PinYin(){
	}
	
	// 取得实例引用
	public static PinYin getInstance(){
		return instance;
	}
	
	/**
	 * 使用缓存的文件名进行reload操作
	 *
	 */
	public void reload(){
		String filename = this.defaultFileName;
		if( filename != null ){
			init( filename );
		}
	}
	/**
	 * 加载拼音词表, 同时缓存词表的文件名.
	 * 词表格式:{一个GBK字符}{对应拼音的ascii串}\n
	 * @param filename 加载的文件名
	 * @return 加载状态, 0 - 正常加载; -1 - 文件名为null; -2 - IO问题
	 */
	public synchronized int init(String filename ){
		
		this.defaultFileName = filename;
		
		if( filename == null ) return -1;
		int ret = -1;
		
		BufferedReader reader = null;
		
		System.arraycopy(staticMap, 0, map,0,staticMap.length );
		try{
			reader = new BufferedReader(
					new InputStreamReader( new FileInputStream(filename), "GBK")
				);
			String line;
			
			while( (line=reader.readLine())!= null ){
				line = line.trim();
				if( line.length()==0 ) continue;
				char c = line.charAt(0);
				String pinyin = line.substring(1).trim();
				if(pinyin.length()==0 ) continue;
				map[(int)c] = pinyin;
			}
			ret = 0;
		}catch( IOException e){
			ret = -2;
			e.printStackTrace();
		}finally {
			if( reader != null ){
				try{
					reader.close();
				}catch( IOException e){}
				reader = null;
			}
		}
		return ret;
	}
	/**
	 * 得到给定字符串的拼音表达串
	 * @param line 原始串
	 * @return 如果参数为null, 则返回null, 否则返回对应的拼音
	 */
	public String getPinYin(String line ){
		if( line == null ) return null;
		
		StringBuffer sb = new StringBuffer( line.length()*4 );
		for( int i=0; i< line.length();i++){
			char c = line.charAt(i);
			sb.append( getPinYin(c) );
		}
		return sb.toString();
	}

	/**
	 * 得到给定字符的拼音
	 * @param c 指定字符
	 * @return 相应的拼音串. 如果该字符没有拼音, 则返回"";
	 */
	public String getPinYin( char c ){
		String list = map[(int)c];
		if( list != null  ){
			return list;
		} else {
			return "";
		}
		
	}
	
	/**
	 * 用于实时修改拼音词表
	 * @param c 要修改拼音的字符
	 * @param s 修改的结果
	 */
	public void setPinYin( char c , String s ){
		map[ (int)c ] = s;
	}
	
	// 原词表里没有数字和字母的拼音, 这里补上
	private static String[] staticMap = new String[127];
	static {
		for( int i='a'; i<='z'; i++ ){
			staticMap[i] = ""+((char)i);
		}
		for( int i='A'; i<='Z'; i++ ){
			staticMap[i] = ""+(char)((char)i-'A'+'a');
		}
		staticMap[(int)'0'] = "ling";
		staticMap[(int)'1'] = "yi";
		staticMap[(int)'2'] = "er";
		staticMap[(int)'3'] = "san";
		staticMap[(int)'4'] = "si";
		staticMap[(int)'5'] = "wu";
		staticMap[(int)'6'] = "liu";
		staticMap[(int)'7'] = "qi";
		staticMap[(int)'8'] = "ba";
		staticMap[(int)'9'] = "jiu";

	}
	
	public void dump(Writer out )throws IOException{
		for(int i=0; i<map.length; i ++){
			String list = map[i];
			if( list != null ){
				out.append((char)i);
				out.append('\t');
				out.append( list );
			}
		}
	}

}
