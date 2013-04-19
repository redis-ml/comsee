package com.sohu.common.blacklist;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.sohu.common.util.text.AdAhoCorasick;

public class AdAhoCorasickMultiBlackList extends AdAhoCorasick{

	private static final AdAhoCorasickMultiBlackList instance = new AdAhoCorasickMultiBlackList();
	
	public static final AdAhoCorasickMultiBlackList getInstance() {
		return instance;
	}
	
	public AdAhoCorasickMultiBlackList(){
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

	public ArrayList<BlackListEntry> findMatch(String key) {

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
		ArrayList<BlackListEntry> ret = new ArrayList<BlackListEntry>();
		rwLock.readLock().lock();
		search(kb, 0, kb.length, ret);
		rwLock.readLock().unlock();
		
		return ret;
	}

	@Override
	public int hit_report(int iPattern, int iPos, Object obj){
		ArrayList<BlackListEntry> ret = (ArrayList<BlackListEntry>)obj;
		if( ret == null ){
			return 1;
		}
		Entry ent = entryList.get(iPattern);
		
		/*for (int i = 0; i < ret.size(); i++){
			BlackListEntry tmp = ret.get(i);
			if (tmp == ent.blEntry){
				//no need to add more than 1 instance
				return 1;
			}
			if ((String)tmp.key != null && ((String)tmp.key).equals((String)ent.blEntry.key)){
				//same key, only use lowest mask
				if (tmp.mask > ent.blEntry.mask){
					tmp.mask = ent.blEntry.mask;
				}
				return 1;
			}
		}*/
		if (!ret.contains(ent.blEntry))
			ret.add(ent.blEntry);
		return 1;
	}
}
