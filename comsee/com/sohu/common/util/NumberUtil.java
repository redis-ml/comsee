package com.sohu.common.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumberUtil {
	private static final String[] hz = {"零","一","二","三","四","五","六","七","八","九"};
    private static final Map<String, Integer> hzsz = new HashMap<String, Integer>();
    private static final Map<String, Integer> hzradix = new HashMap<String, Integer>();
    
	private final static String pattern = "[\u4E00-\u9FA5]+";
	private final static Pattern p = Pattern.compile(pattern);	   
    
    static{
        for(int i = 0; i < hz.length; i ++){
            hzsz.put(hz[i], i);
        }
        hzradix.put("十", 10);
        hzradix.put("百", 100);
        hzradix.put("千", 1000);
        hzradix.put("万", 10000);
        hzradix.put("亿", 100000000);
    }
   
    public static long ch2Num(String chNum){
    	Matcher m = p.matcher(chNum);
    	if(m.matches())
    		return exchange1(chNum);
    	else
    		return -1;
    }
    
    public static void main(String[] args){
        String[] srcs = {"二十三"};
        for(int i = 0; i < srcs.length; i ++){
            System.out.println(exchange1(srcs[i])+"\n");

        }
    }
    
    public static long exchange1(String src){
        String []tmp1 = src.split("亿");
        if(tmp1.length == 2){
            return (long)exchange2(tmp1[0]) * hzradix.get("亿") + exchange2(tmp1[1]);
        }else{
            return exchange2(tmp1[0]);
        }
    }       
   
    public static int exchange2(String src){
        String []tmp1 = src.split("万");
        if(tmp1.length == 2){
            return exchange(tmp1[0]) * hzradix.get("万") + exchange(tmp1[1]);
        }else{
            return exchange(tmp1[0]);
        }
    }    
 
    public static int exchange(String src){//十、百、千
        int bg = 0;
        for(int i = 0; i < src.length(); i ++){
            String c1 = ""+src.charAt(i);
            if(hzsz.containsKey(c1)){
                if(c1.equals("零")){
                    continue;
                }
                if(i + 1 < src.length()){
                    String c2 = ""+src.charAt(i+1);
                    if(hzradix.containsKey(c2)){
                        bg += hzsz.get(c1) * hzradix.get(c2);
                    }
                }else{
                    bg += hzsz.get(c1);
                }
            }else if(i == 0 && c1.equals("十") ||//10-19之间的习惯用法
                     i > 0 && c1.equals("十") && ((""+src.charAt(i-1)).equals("零"))){
                bg += 10;
            }
        }
        return bg;
    }
                
}
