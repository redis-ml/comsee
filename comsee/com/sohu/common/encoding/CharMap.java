package com.sohu.common.encoding;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;

/**
 * 字符映射表 (Unicode char -> Unicode char)
 * @author liumingzhu
 *
 */
public class CharMap {

	private static final int MAP_LENGTH = 65536;
	
	protected boolean inited = false;
	protected char[] map = new char[MAP_LENGTH];
	
	public CharMap(){	
	}
	
     /**
     * 初始化过程。将表装入内存.
     * 文件格式: 65536*2的二进制文件, 依次为unicode n 的映射字符的unicode值
     * @param filename 映射表的数据文件
     */
    public void load(String filename) throws IllegalArgumentException {
    	try{
    		load(new FileInputStream(filename));
    	} catch ( FileNotFoundException e){
			throw new IllegalArgumentException("CharMap source file NOT EXISTS or UNREADABLE: "+filename);
		} catch ( SecurityException e){
			throw new IllegalArgumentException("CharMap source file UNREADABLE(Security Reason): "+filename);
		} catch ( Exception e) {
			throw new IllegalArgumentException("CharMap source file NOT EXISTS or UNREADABLE: "+filename);
		}
    }
    public void load(InputStream stream) throws Exception {
    	//FileInputStream fis = null;
    	BufferedInputStream fis = null;
		try {
			//fis = new FileInputStream(filename);
			fis = new BufferedInputStream(stream);
			for (int i = 0; i < MAP_LENGTH; i++) {
				int l = fis.read();
				int h = fis.read();
				map[i] = (char) (h * 256 + l);
			}
			inited = true;
		} catch ( NullPointerException e){
			throw new IllegalArgumentException("CharMap source file is NULL!");
		} catch ( IOException e) {
			throw e;
		}finally {
			if( fis != null ){
				try{
					fis.close();
				}catch(IOException e){}
				fis = null;
			}
		}
	}

    /**
     * 存储过程。将表保存为磁盘文件.
     * 文件格式: 65536*2的二进制文件, 依次为unicode n 的映射字符的unicode值
     * @param filename 映射表的数据文件
     */
    public void save(String filename) throws IllegalArgumentException {
    	FileOutputStream fis = null;
		try {
			fis = new FileOutputStream(filename);
			for (int i = 0; i < MAP_LENGTH; i++) {
				fis.write((int)map[i] );
				fis.write( ((int)map[i]) >>> 8 );
			}
			fis.flush();
			fis.close();
		} catch ( NullPointerException e){
			throw new IllegalArgumentException("CharMap source file is NULL!");
		} catch ( FileNotFoundException e){
			throw new IllegalArgumentException("CharMap source file NOT EXISTS or UNREADABLE: "+filename);
		} catch ( SecurityException e){
			throw new IllegalArgumentException("CharMap source file UNREADABLE(Security Reason): "+filename);
		} catch ( IOException e) {
			throw new IllegalArgumentException("CharMap source file NOT EXISTS or UNREADABLE: "+filename);
		}finally {
			if( fis != null ){
				try{
					fis.close();
				}catch(IOException e){}
				fis = null;
			}
		}
	}

    /**
     * 根据码表, 得到映射字符
     * @param c
     * @return
     */
    public char map(char c){
    	if( inited )
    		return map[(int)c];
    	else
    		return c;
    }
    /**
     * 根据码表映射关系, 将字符串批量转换
     * @param str
     * @return
     */
    public String map(CharSequence str){
    	if( str == null ){
    		return null;
    	}
    	StringBuffer sb = new StringBuffer( str.length() );
    	
    	for( int i=0; i<str.length();i++){
    		sb.append( map(str.charAt(i)) );
    	}
    	return sb.toString();
    }
    /**
     * 将字符串批量映射并输出
     * @param str
     * @param out
     * @throws IOException
     */
    public void convert( CharSequence str, Writer out) throws IOException{
    	if( str == null ) return ;
    	for( int i=0; i<str.length(); i++){
    		out.write( map(str.charAt(i)) );
    	}
    }
    /**
     * 对码表中的某个字符进行修改
     * @param o
     * @param n
     */
    public void correct( char o, char n ){
    	map[(int)o] = n;
    }
    public void correct( char[]src){
    	correct( src, 0, 0, src.length);
    }
    public void correct( char[]src, int dstIndex ){
    	correct( src, 0, dstIndex, src.length);
    }
    public void correct( char[]src, int startIndex, int destIndex, int length ){
    	System.arraycopy(src, startIndex, map, destIndex, length);
    }
}
