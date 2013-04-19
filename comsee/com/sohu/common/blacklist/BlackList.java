package com.sohu.common.blacklist;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import com.sohu.common.encoding.PinYin;


public class BlackList {

	private static Log logger = LogFactory.getLog( BlackList.class );
	private static final int radix = 10;
	
	private static String defaultCharset = "GBK";
	
	ExactKeywordList exact = null; // 精确匹配词表
	RegexKeywordList regex = null; // 正则匹配词表
	RegexKeywordList pinyin = null; // 拼音正则匹配词表
	ExactKeywordList whiteExact = null; // 精确匹配词表
	RegexKeywordList whiteRegex = null; // 正则匹配词表
	
	private String[] filenames = new String[5];
	
	private static BlackList instance = new BlackList();
		
	public static BlackList getInstance() {
		return instance;
	}
	public BlackList(){
	}
	
	public void setFiles( String[] files ){
		if( files == null ) return;
		int n = files.length < filenames.length ? files.length : filenames.length;
		for( int i=0; i < n ; i ++ ){
			this.filenames[i] = files[i];
		}
	}
	public void loadFiles(){
		ExactKeywordList ext;
		RegexKeywordList reg;
		String fn = filenames[0];
		ext = initExact( fn, "BlackList.initExact(blacklist): " );
		if( ext != null ){
			this.exact = ext;
		}

		fn = filenames[1];
		reg = initRegex( fn ,"BlackList.initRegex(blacklist): " );
		if( reg != null ){
			this.regex = null;
		}

		fn = filenames[2];
		initPinyin( fn );
		
		fn = filenames[3];
		ext = initExact( fn, "BlackList.initExact(whitelist): " );
		if( ext != null ){
			this.whiteExact = ext;
		}
		
		fn = filenames[4];
		reg = initRegex( fn ,"BlackList.initRegex(whitelist): " );
		if( reg != null ){
			this.whiteRegex = null;
		}
		
	}
	
	public void initAll(){
	}
	
	public ExactKeywordList initExact( String filename, String LOG_HEAD ){
		if( filename == null ) return null;
		ExactKeywordList exact = null;
		try{
			BufferedReader reader = new BufferedReader (
					new InputStreamReader( new FileInputStream(filename), defaultCharset) );
			String line;
			ExactKeywordList pinyin = null;
			while( (line=reader.readLine() ) != null ){
				int p = line.indexOf('\t');
				if( p<=0 ){
					if( logger.isWarnEnabled() ){
						logger.warn( LOG_HEAD+ line);
					}
					continue;
				}
				String key = line.substring(0, p );
				key = preserve( key, true );
				String val = line.substring(p+1);
				try{
					int t = Integer.parseInt( val, radix );
					BlackListEntry entry = new BlackListEntry();
					entry.key = ( key );
					entry.mask = ( t );
					if( pinyin == null ){
						pinyin = new ExactKeywordList();
					}
					pinyin.add( entry );
				}catch( NumberFormatException  e){
					if( logger.isWarnEnabled() ){
						logger.warn( LOG_HEAD+ line, e);
					}
				}
			}
			exact = pinyin;
			if( logger.isInfoEnabled() ){
				logger.info( LOG_HEAD+ " loaded(SUCC) from "+filename);
			}
			
		}catch( IOException e){
			if( logger.isWarnEnabled() ){
				logger.warn( LOG_HEAD+ " loaded(FAIL) from "+filename, e);
			}
		
		}
		return exact;
		
	}

	public RegexKeywordList initRegex(String filename, String LOG_HEAD ){
		if( filename == null ) return null;
		RegexKeywordList regex = null;
		try{
			BufferedReader reader = new BufferedReader (
					new InputStreamReader( new FileInputStream(filename), defaultCharset) );
			String line;
			Perl5Compiler compiler = new Perl5Compiler();
			RegexKeywordList pinyin = null;
			while( (line=reader.readLine() ) != null ){
				int p = line.indexOf('\t');
				if( p<=0 ){
					if( logger.isWarnEnabled() ){
						logger.warn( LOG_HEAD+ line);
					}
					continue;
				}
				String key = line.substring(0, p );
				key = CharMapWrapper.map( key );
				String val = line.substring(p+1);
				try{
					int t = Integer.parseInt( val, radix );
					BlackListEntry entry = new BlackListEntry();
					entry.key = compiler.compile( key,
							Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.DEFAULT_MASK );
					entry.mask = t;
					if( pinyin == null ){
						pinyin = new RegexKeywordList();
					}
					pinyin.add( entry );
				}catch( MalformedPatternException e){
					if( logger.isWarnEnabled() ){
						logger.warn( LOG_HEAD+ line, e);
					}
				}catch( NumberFormatException  e){
					if( logger.isWarnEnabled() ){
						logger.warn( LOG_HEAD+ line, e);
					}
				}
			}
			regex = pinyin;
			if( logger.isInfoEnabled() ){
				logger.info( LOG_HEAD+ " loaded(SUCC) from "+filename);
			}
			
		}catch( IOException e){
			if( logger.isWarnEnabled() ){
				logger.warn( LOG_HEAD+ " loaded(FAIL) from "+filename, e);
			}
		
		}
		return regex;
		
	}

	public void initPinyin(String filename ){
		String LOG_HEAD=" NewBlackList.initPinyin: ";
		if( filename == null ) return;
		try{
			BufferedReader reader = new BufferedReader (
					new InputStreamReader( new FileInputStream(filename), defaultCharset) );
			String line;
			Perl5Compiler compiler = new Perl5Compiler();
			RegexKeywordList pinyin = null;
			while( (line=reader.readLine() ) != null ){
				int p = line.indexOf('\t');
				if( p<=0 ){
					if( logger.isWarnEnabled() ){
						logger.warn( LOG_HEAD+ line);
					}
					continue;
				}
				String key = line.substring(0, p );
				key = CharMapWrapper.map( key );
				String val = line.substring(p+1);
				try{
					int t = Integer.parseInt( val, radix );
					BlackListEntry entry = new BlackListEntry();
					entry.key = compiler.compile( key,
							Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.DEFAULT_MASK );
					entry.mask = ( t );
					if( pinyin == null ){
						pinyin = new RegexKeywordList();
					}
					pinyin.add( entry );
				}catch( MalformedPatternException e){
					if( logger.isWarnEnabled() ){
						logger.warn( LOG_HEAD+ line, e);
					}
				}catch( NumberFormatException  e){
					if( logger.isWarnEnabled() ){
						logger.warn( LOG_HEAD+ line, e);
					}
				}
			}
			this.pinyin = pinyin;
			if( logger.isInfoEnabled() ){
				logger.info( LOG_HEAD+ " loaded(SUCC) from "+filename);
			}
			
		}catch( IOException e){
			if( logger.isWarnEnabled() ){
				logger.warn( LOG_HEAD+ " loaded(FAIL) from "+filename, e);
			}
		
		}
		
	}
	
	public BlackListEntry pinyinMatches( String key , Perl5Matcher matcher){
		return pinyin.findMatch( key, matcher );
	}
	
	public BlackListEntry regexMatches( String key , Perl5Matcher matcher){
		return regex.findMatch( key, matcher );
	}
	public BlackListEntry exactMatches( String key ){
		return exact.findMatch( key );
	}
	public BlackListEntry exactMatches( String key , StringBuffer sb ){
		return exact.findMatch( key , sb);
	}
	
	public BlackListEntry matches( String key, Perl5Matcher matcher , StringBuffer sb ){
		BlackListEntry entry = null;
		
		if( exact != null ){
			entry = exact.findMatch( key, sb );
			if( entry != null ){
				return entry;
			}
		}
		
		if( regex != null ){
			entry = regex.findMatch( key, sb, matcher );
			if( entry != null ){
				return entry;
			}
		}

		if( pinyin != null ){
			String key_pinyin = PinYin.getInstance().getPinYin( key );
			if( key_pinyin.length() > 0 ){
				entry = this.pinyin.findMatch(key_pinyin , matcher );
			}
			if( entry != null ){
				return entry;
			}
		}

		return null;
	}
	public String preserve( CharSequence query, boolean check ){
		String ret = null;
		StringBuffer sb = new StringBuffer( query.length() );
		for( int i=0; i<query.length();i++){
			char ch = query.charAt(i);
			if( check ){
				ch = CharMapWrapper.map(ch);
			}
			if( Character.isLetterOrDigit(ch)
					|| ( ch>0x7f && ch !='《' && ch !='》'
						&& ch !='、' && ch != '—' && ch != '￥' 
						&& ch != '·' && ch != '…'  
						) ){
				sb.append( ch );
			}
		}
		ret = sb.toString();
		
		return ret;
		
	}

	
}
