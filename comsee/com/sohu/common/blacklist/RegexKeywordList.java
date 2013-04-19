package com.sohu.common.blacklist;

import java.util.ArrayList;

import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Matcher;

public class RegexKeywordList{
	ArrayList list;
	
	public RegexKeywordList(){
		this(10);
	}
	public RegexKeywordList(int size ){
		this.list = new ArrayList( size );
	}

	public boolean add(BlackListEntry entry ){
		return list.add( entry );
	}
	public BlackListEntry findMatch( String key, Perl5Matcher matcher ){
		return findMatch( key, null, matcher );
	}
	public BlackListEntry findMatch( String key, StringBuffer sb, Perl5Matcher matcher ){
		
		if( key == null
				|| matcher == null	)
		{
			return null;
		}
		int key_len = key.length();
		if( sb == null ){
			sb = new StringBuffer( key_len );
		} else {
			sb.delete(0, sb.length() );
		}
		
		// 过滤无效的符号
		for( int i=0; i<key_len; i ++ ){
			char ch = key.charAt( i );
			int type = CharFilter.charType( ch );
			if( type != CharFilter.SPACE 
					&& type != CharFilter.WHITE ){
				sb.append( ch );
			}
		}
		if( sb.length() > 0 ){
			String tmp = sb.toString();
			for( int i=0; i<list.size(); i++ ){
				BlackListEntry entry = (BlackListEntry)list.get(i);
				if( entry == null ) continue;
				Pattern pat = (Pattern)entry.key;
				if( matcher.matches(tmp, pat )){
					return entry;
				}
			}
		}
		return null;
	}
}
