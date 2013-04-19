package com.sohu.common.blacklist;

import com.sohu.common.encoding.CharMap;

public class CharMapWrapper {
	private static CharMap charMap = new CharMap();
	public static void init(String charMapFile){
		try{
			charMap.load(charMapFile);
		}catch(Exception e){}
	}
	public static String map(String str){
		return charMap.map(str);
	}
	public static char map(char str){
		return charMap.map(str);
	}
}
