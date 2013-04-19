package com.sohu.common.blacklist;

import java.util.ArrayList;

public class ActionBlackList {
	AdAhoCorasickMultiBlackList ahoMatcher = null; //行动类核心关键词
	ArrayList<BlackListEntry> assistMatcher = null; //行动类辅助关键词
	private static int MINFOUND = 3; //找到3个关键词，则认为命中模糊条件，无论是其他的什么词组合都算命中
	
	public ActionBlackList(AdAhoCorasickMultiBlackList aho, ArrayList<BlackListEntry> assist){
		this.ahoMatcher = aho;
		this.assistMatcher = assist;
	}
	
	public BlackListEntry findMatch( String key ){
		return findMatch( key, null);
	}
	
	public BlackListEntry findMatch( CharSequence key, StringBuffer sb){
		if( key == null ) {
			return null;
		}
		
		if( sb == null ){
			sb = new StringBuffer ( key.length() );
		} else {
			sb.delete( 0, sb.length() ); 
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
		BlackListEntry ret = null;
		int reason = -4;
		do{
			if (key == null || key.length()==0){
				reason = -3;
				break;
			}
			if (this.ahoMatcher == null){
				reason = -2;
				break;
			}
			ArrayList<BlackListEntry> matched = this.ahoMatcher.findMatch(key);
			if (matched == null || matched.size() <= 0){
				reason = -1;
				break;
			}

			ret = new BlackListEntry();
			ret.key = "";
			ret.mask = matched.get(0).mask;
			ret.startTime = matched.get(0).startTime;
			ret.endTime = matched.get(0).endTime;
			boolean found = false;
			//两个列表，分别记录不包含其他黑名单的绝对黑名单元素和会包含的
			//比如命中的黑名单分别是A, C, ABC, CD, BC
			//unclude中包含A,C
			//include中包含ABC,CD, BC
			ArrayList<BlackListEntry> uncludeList = new ArrayList<BlackListEntry>();
			ArrayList<BlackListEntry> includeList = new ArrayList<BlackListEntry>();
			for (int i = 0; i < matched.size(); i++){
				//找到最小mask
				if (ret.rule > matched.get(i).rule){
					ret.mask = matched.get(i).mask;
					ret.key = matched.get(i).key;
				}
				//无论是否是最小的mask，一律取最严格的时间
				if (ret.startTime != 0 && (matched.get(i).startTime == 0 || ret.startTime > matched.get(i).startTime)){
					ret.startTime = matched.get(i).startTime;
				}
				if (ret.endTime != 0 && (matched.get(i).endTime == 0 || ret.endTime < matched.get(i).endTime)){
					ret.endTime = matched.get(i).endTime;
				}
				String tmpkey = (String)matched.get(i).key;
				ret.key = ret.key + "core:"+tmpkey;
				if (key.equals(tmpkey)){
					found = true;
				}
				//过滤一遍找到的黑名单列表，将类似ABC, A, C三个滤成 A C两个
				//否则ABCD将因为命中了ABC, A, C，而被判定为命中了3个，实际应该是命中了两个
				boolean isContainingOther = false;
				for (int j = 0; j < matched.size(); j++){
					if (i != j){
						if (tmpkey.indexOf((String)matched.get(j).key) >= 0){
							isContainingOther = true;
							break;
						}
					}
				}
				if (!isContainingOther){
					uncludeList.add(matched.get(i));
				}else{
					includeList.add(matched.get(i));
				}
			}
			if (found){
				//已找到完全匹配项
				reason = 5;
				break;
			}
			if (uncludeList.size() >= MINFOUND || uncludeList.size() == 0){
				//或者命中的核心词>2个
				reason = 1;
				break;
			}
			
			String remainingString = key;
			//将查询词中所有命中替换掉
			for (int i = 0; i < includeList.size(); i++){
				remainingString = remainingString.replace((String)includeList.get(i).key, "");
			}
			for (int i = 0; i < uncludeList.size(); i++){
				remainingString = remainingString.replace((String)uncludeList.get(i).key, "");
			}
			if ("".equals(remainingString)){
				//完全命中了黑名单的组合
				reason = 4;
				break;
			}
			//看看剩余字符是否是命中的子串，是的话表示完全匹配
			found = false;
			for (int i = 0; i < matched.size(); i++){
				//找到最小mask
				String tmpkey = (String)matched.get(i).key;
				if (tmpkey.indexOf(remainingString) >= 0){
					found = true;
					break;
				}
			}
			if (found){
				reason = 2;
				break;
			}
			int nowFound = uncludeList.size();
			if (this.assistMatcher == null){
				//没有辅助词表，直接退出
				ret = null;
				reason = -5; 
				break;
			}
			found = false;
			for (int i = 0; i < this.assistMatcher.size(); i++){
				BlackListEntry tmp = this.assistMatcher.get(i);
				String tmpKey = (String)tmp.key;
				if (remainingString.indexOf(tmpKey) >= 0){
					ret.key = ret.key + "assist:"+tmpKey;
					//剩余查询词等于该辅助词，说明完全匹配
					remainingString = remainingString.replace(tmpKey, "");
					if ("".equals(remainingString)){
						reason = 6;
						found = true;
						break;
					}
					//查看是否已经找到3个匹配的关键词
					nowFound ++;
					if (nowFound >= MINFOUND){
						reason = 7;
						found = true;
						break;
					}
				}
			}
			if (found){
				break;
			}
			//未找到
			ret = null;
		}while(false);
		return ret;
	}
}
