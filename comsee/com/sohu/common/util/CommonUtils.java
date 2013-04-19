package com.sohu.common.util;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;

public class CommonUtils{

/*
	public static String readTextFromFile(String filename){
		try{
			BufferedInputStream result = new BufferedInputStream( new FileInputStream(filename) );
			byte[] cont = new byte[1024];
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int conlen;
			while((conlen = result.read(cont))>=0){
					baos.write( cont , 0 , conlen);
			}
			result.close();
			return new String(baos.toByteArray());
		} catch(Exception e){
			return "";
		}
	}
*/
	
	public static String readTextFromFile(String filename){
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String str,res = "";
			while ((str = in.readLine()) != null) {
				str = str + "\n";
				res = res + str;
			}
			in.close();
			return res;
		} catch (IOException e) {
			return null;
		}
	}
	
	public static boolean writeTextToFile(String filename,String content){
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(filename));
			out.write(content);
			out.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public static String readTextFromURL(String urlstr){
		try {
			URL url = new URL(urlstr);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			String str,res = "";
			while ((str = in.readLine()) != null) {
				str = str + "\n";
				res = res + str;
			}
			in.close();
			return res;
		} catch (MalformedURLException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
	}

    public static String replace(String str, String pattern, String replace) {
        int s = 0;
        int e = 0;
        StringBuffer result = new StringBuffer();

        while ((e = str.indexOf(pattern, s)) >= 0) {
            result.append(str.substring(s, e));
            result.append(replace);
            s = e+pattern.length();
        }
        result.append(str.substring(s));
        return result.toString();
    }

}
