package com.sohu.common.blacklist;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sohu.common.util.text.AdAhoCorasick;

public class AdAhoCorasickBlackList extends AdAhoCorasick{

	private static final AdAhoCorasickBlackList instance = new AdAhoCorasickBlackList();
	
	public static final AdAhoCorasickBlackList getInstance() {
		return instance;
	}
	
	public AdAhoCorasickBlackList(){
		// dummy
	}
	
	public class Entry{
		BlackListEntry blEntry;
		String term;
		byte[]termBytes;
	}
	
	private ArrayList<Entry>entryList;
	private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

	public void init(ArrayList<BlackListEntry> list){
		if( list == null ) return;
		
		ArrayList<Entry>temp = new ArrayList<Entry>(list.size());
		byte[][] terms = new byte[list.size()][];
		for(int i=0;i<list.size();i++){
			BlackListEntry entry = list.get(i);
			Entry ent = new Entry();
			temp.add(ent);
			
			ent.blEntry = entry;
			String value = (String)entry.key;
			try {
				ent.termBytes = value.getBytes("GBK");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				continue;
			}
			ent.term = value;
			terms[i] = ent.termBytes;
		}
		
		boolean isOk = false;
		rwLock.writeLock().lock();
		try{
			initialize(terms, 0, terms.length);
			isOk = true;
		}finally{
			if( ! isOk ){
				entryList = null;
			} else {
				entryList = temp;
			}
			rwLock.writeLock().unlock();
		}
	}

	public BlackListEntry findMatch(String key) {

		if (key == null) {
			return null;
		}
		String tmp = key;
		byte[] kb;
		try {
			kb = tmp.getBytes("GBK");
		} catch (Exception e) {
			return null;
		}
		BlackListEntry ret = null;
		rwLock.readLock().lock();
		Entry[] sts = new Entry[1];
		search(kb, 0, kb.length, sts);
		Entry ent = sts[0];
		rwLock.readLock().unlock();
		if (ent != null) {
			ret = ent.blEntry;
		}
		return ret;
	}

	@Override
	public int hit_report(int iPattern, int iPos, Object obj){
		Entry[]sts = (Entry[])obj;
		if( sts == null ){
			return 1;
		}
		Entry ent = entryList.get(iPattern);
		
		if( sts[0] == null ){
			sts[0] = ent;
		} else {
			if( ent.blEntry.mask < sts[0].blEntry.mask){
				sts[0] = ent;
			}
		}
		return 1;
	}
}
