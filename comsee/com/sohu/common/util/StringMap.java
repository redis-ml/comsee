package com.sohu.common.util;

import java.io.PrintWriter;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StringMap {

	protected HashMap<String, String> map = new HashMap<String, String>();
	
	protected boolean inited = false;
	
	private final String DEFAULT_CHARSET = "GBK";
	
	private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	
	public StringMap(){
		
	}
		
	public void load(String filename){
		load(filename, DEFAULT_CHARSET);
	}
	
	public void save(String filename){
		save(filename, DEFAULT_CHARSET);
	}
	
	public void load(String filename, String charset) throws IllegalArgumentException {
		if(charset==null){
			charset = DEFAULT_CHARSET;
		}
		HashMap<String, String> tmap = new HashMap<String, String>();
		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try{
			fis = new FileInputStream(filename);
			isr = new InputStreamReader(fis, charset);
			br = new BufferedReader(isr);
			String line;
			while( (line=br.readLine() ) != null ){
				 int divIndex = line.indexOf('\t');
				 if( divIndex<0 )
					 continue;
				 String key = line.substring(0, divIndex);
				 String value = line.substring(divIndex+1);
				 tmap.put(key, value);
				 }
			map = tmap;
			inited = true;
			} catch ( NullPointerException e){
				throw new IllegalArgumentException("StringMap source file is NULL!");
			} catch ( FileNotFoundException e){
				throw new IllegalArgumentException("StringMap source file NOT EXISTS or UNREADABLE: "+filename);
			} catch ( SecurityException e){
				throw new IllegalArgumentException("StringMap source file UNREADABLE(Security Reason): "+filename);
			} catch ( UnsupportedEncodingException e){
				throw new IllegalArgumentException("Charset is UNSUPPORTED!");
			} catch ( IOException e) {
				throw new IllegalArgumentException("StringMap source file NOT EXISTS or UNREADABLE: "+e.getMessage());
			}finally {
				if( br != null ){
					try{
						br.close();
					}catch(IOException e){}
					br = null;
				}
				if( isr != null ){
					try{
						isr.close();
					}catch(IOException e){}
					isr = null;
				}
				if( fis != null ){
					try{
						fis.close();
					}catch(IOException e){}
					fis = null;
				}
			}
	}
	
    public void save(String filename, String charset) throws IllegalArgumentException {
    	if(charset==null){
			charset = DEFAULT_CHARSET;
		}
    	FileOutputStream fos = null;
    	OutputStreamWriter osw = null;
    	PrintWriter pw = null;
		try {
			fos = new FileOutputStream(filename);
			osw = new OutputStreamWriter(fos, charset);
			pw = new PrintWriter(osw); 
			Object[] keys = map.keySet().toArray();
			for (int i = 0; i < keys.length; i++) {
				pw.println((String)keys[i]+"\t"+(String)map.get(keys[i]));
				pw.flush();
			}
		} catch ( NullPointerException e){
			throw new IllegalArgumentException("StringMap source file is NULL!");
		} catch ( FileNotFoundException e){
			throw new IllegalArgumentException("StringMap source file NOT EXISTS or UNWRITABLE: "+filename);
		} catch ( SecurityException e){
			throw new IllegalArgumentException("StringMap source file UNWRITABLE(Security Reason): "+filename);
		} catch ( UnsupportedEncodingException e ){
			throw new IllegalArgumentException("Charset is UNSUPPORTED!");
		} catch ( IOException e) {
			throw new IllegalArgumentException("StringMap source file NOT EXISTS or UNWRITABLE: "+filename);
		} finally {
			if( pw != null ){
				try{
					pw.close();
				}catch(Exception e){}
				pw = null;
			}
			if( osw != null ){
				try{
					osw.close();
				}catch(IOException e){}
				osw = null;
			}
			if( fos != null ){
				try{
					fos.close();
				}catch(IOException e){}
				fos = null;
			}
		}
	}
    
	public String getValue(String key){
		if(inited){
			rwLock.readLock().lock();
			String val = (String)map.get(key);
			rwLock.readLock().unlock();
			return val;
		}
		else
			return null;
	}
	
	public void setValue(String key, String value){
		rwLock.writeLock().lock();
		map.put(key, value);
		rwLock.writeLock().unlock();
	}
	
	public boolean contains(String key){
		if(map.containsKey(key))
			return true;
		else
			return false;
	}
}
	