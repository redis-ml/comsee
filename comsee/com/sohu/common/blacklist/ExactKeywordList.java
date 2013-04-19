package com.sohu.common.blacklist;

import java.util.HashMap;

public class ExactKeywordList {


	HashMap map;
	
	public ExactKeywordList(){
		this(10);
	}
	public ExactKeywordList(int size ){
		this.map = new HashMap( size );
	}

	public boolean add(BlackListEntry entry ){
		map.put(entry.key, entry );
		return true;
	}
	public BlackListEntry findMatch( String key ){
		return findMatch( key, null);
	}

	public BlackListEntry findMatch( CharSequence key, StringBuffer sb){
		return findMatch( key, sb, false);
	}
	public BlackListEntry findMatch( CharSequence key, StringBuffer sb, boolean halfunified){
		if( key == null ) {
			return null;
		}
		
		if( sb == null ){
			sb = new StringBuffer ( key.length() );
		} else {
			sb.delete( 0, sb.length() ); 
		}
		return findMatchSpWithSpace( key , sb, halfunified);
		
	}
	private BlackListEntry findMatchSpWithSpace( CharSequence key, StringBuffer sb, boolean halfunified){

		BlackListEntry entry = null;

		entry = get( key.toString() );
		
		if( entry != null ){
			return entry;
		}
		if(halfunified)
			return entry;
		
		if( sb == null ){
			sb = new StringBuffer();
		} else {
			sb.delete(0, sb.length() );
		}
		
		for( int i=0; i<key.length(); i++){
			char ch = key.charAt( i );
			int type = CharFilter.charType( ch );
			if( type != CharFilter.WHITE
					&& type != CharFilter.SPACE ){
				sb.append( ch );
			}
		}
		return get( sb.toString() );
		
	}
	public BlackListEntry get( String key ){
		return (BlackListEntry) map.get(key);
	}
	
}
