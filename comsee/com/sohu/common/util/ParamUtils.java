package com.sohu.common.util;

import java.util.Map;

import javax.servlet.http.*;
/**
 * This class assists skin writers in getting parameters.
 */
public class ParamUtils {
	
	/**
	 * 将QueryString中的各个参数组织成一个Map，方便选取
	 * 注意：1. 对QueryString串没有处理'#'
	 *      2. "&=&"将解释为参数为空字符串，值为空字符串的参数
	 * @param url 需要解析的QueryString
	 * @param startIndex 开始的位置
	 * @param map 用于存放参数的Map
	 * @return -1 url参数非法，为null;
	 *         -2 map参数非法，为null;
	 *         -3 startIndex参数非法，越界
	 *        >=0 解析出来的参数个数
	 */
	public static int parseParameters(CharSequence url, int startIndex, Map map ){
		if( url == null )
			return -1;
		if( map == null )
			return -2;
		
		int url_len = url.length();
		if( startIndex < 0
				|| startIndex >= url_len )
			return -3;
		
		int count = 0;
		
		while(startIndex < url_len ){
			int start = startIndex;
			int middle = -1;
			int end = -1;
			for(;end == -1 && startIndex<url_len;startIndex++){
				char c = url.charAt(startIndex);
				switch(c){
				case '=':
					if( middle == -1 )
						middle = startIndex;
					break;
				case '&':
					end = startIndex;
					break;
				}
			}
			if( startIndex == url_len
					&& end == -1 ){
				end = url_len;
			}

			if( end > start ){
				if( middle >= start ){
					String key = url.subSequence(start, middle).toString();
					String val = url.subSequence(middle+1, end).toString();
					map.put(key, val);
				} else {
					String key = url.subSequence(start, end).toString();
					map.put( key, null);
				}
				count ++;
			}
		}
		return count;
		
	}
	/**
	 * 从指定位置解析给定的QueryString（即url参数串）中，特定的参数值。
	 * @param url 给定的QueryString，比如，对于http://www.sogou.com/?qeb=s，QueryString为"qeb=s"
	 * @param name 指定的参数值
	 * @param startIndex 从何处开始解析
	 * @return 如果存在合法的给定参数的值，那么返回该参数值字符串（不会自动解码）；否则返回null
	 */
	public static String getParameter(CharSequence url, CharSequence name, int startIndex ){
		if( url == null 
				|| name == null 
				|| startIndex < 0 ){ 
			return null;
		}
		int name_len = name.length();
		if ( name_len == 0 ) return null;
		
		int url_len = url.length();
		
		boolean newStart = true;
		
		for( ; startIndex < url_len ; startIndex ++ ){
			char c = url.charAt(startIndex);
			if( c == '&') {
				newStart = true;
				continue;
			}
			if (!newStart) {
				continue;
			}
			int index = 0;
			for (; startIndex < url_len && index < name_len
					&& url.charAt(startIndex) == name.charAt(index); index++, startIndex++)
				;
			if (index != name_len)
				continue;
			if (startIndex >= url_len)
				return null;
			c = url.charAt(startIndex);
			if (c == '=') {
				startIndex++;
				int temp = startIndex;
				for (; startIndex < url_len && url.charAt(startIndex) != '&'; startIndex++)
					;
				return url.subSequence(temp, startIndex).toString();
			} else if (c == '&') {
				return null;
			} else {
				continue;
			}
		}
		return null;
	}
	/**
	 * 从URL串中解析特定的参数值
	 * @param url 给定的标准url，该方法从"?"处解析参数
	 * @param name 给定的参数名
	 * @return 如果url中"?"后的部分含有指定参数名的参数，那么返回参数值部分（注意，不会自动解码）；
	 *         否则返回null
	 */
	public static String getParameter(CharSequence url, CharSequence name ){
		if( url == null 
				|| name == null ){
			return null;
		}
		for( int index=0; index < url.length(); index++ ){
			char c = url.charAt(index);
			if( c == '?'){
				return getParameter( url, name, index+1); 
			}
		}
		return null;
	}
    /**
     * Gets a parameter as a string.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the parameter you want to get
     * @return The value of the parameter or null if the parameter was not
     *      found or if the parameter is a zero-length string.
     */
    public static String getParameter(HttpServletRequest request, String name) {
        return getParameter(request, name, false);
    }

    /**
     * Gets a parameter as a string.
     * @param request The HttpServletRequest object, known as "request" in a
     * JSP page.
     * @param name The name of the parameter you want to get
     * @param emptyStringsOK Return the parameter values even if it is an empty string.
     * @return The value of the parameter or null if the parameter was not
     *      found.
     */
    public static String getParameter(HttpServletRequest request,
            String name, boolean emptyStringsOK)
    {
        String temp = request.getParameter(name);
        if (temp != null) {
            if (temp.equals("") && !emptyStringsOK) {
                return null;
            }
            else {
                return temp;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Gets a parameter as a boolean.
     * @param request The HttpServletRequest object, known as "request" in a
     * JSP page.
     * @param name The name of the parameter you want to get
     * @return True if the value of the parameter was "true", false otherwise.
     */
    public static boolean getBooleanParameter(HttpServletRequest request,
            String name)
    {
        return getBooleanParameter(request, name, false);
    }

    /**
     * Gets a parameter as a boolean.
     * @param request The HttpServletRequest object, known as "request" in a
     * JSP page.
     * @param name The name of the parameter you want to get
     * @return True if the value of the parameter was "true", false otherwise.
     */
    public static boolean getBooleanParameter(HttpServletRequest request,
            String name, boolean defaultVal)
    {
        String temp = request.getParameter(name);
        if ("true".equals(temp) || "on".equals(temp)) {
            return true;
        }
        else if ("false".equals(temp) || "off".equals(temp)) {
            return false;
        }
        else {
            return defaultVal;
        }
    }

    /**
     * Gets a parameter as an int.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the parameter you want to get
     * @return The int value of the parameter specified or the default value if
     *      the parameter is not found.
     */
    public static int getIntParameter(HttpServletRequest request,
            String name, int defaultNum)
    {
        String temp = request.getParameter(name);
        if(temp != null && !temp.equals("")) {
            int num = defaultNum;
            try {
                num = Integer.parseInt(temp);
            }
            catch (Exception ignored) {}
            return num;
        }
        else {
            return defaultNum;
        }
    }

    /**
     * Gets a list of int parameters.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the parameter you want to get
     * @param defaultNum The default value of a parameter, if the parameter
     * can't be converted into an int.
     */
    public static int[] getIntParameters(HttpServletRequest request,
            String name, int defaultNum)
    {
        String[] paramValues = request.getParameterValues(name);
        if (paramValues == null) {
            return null;
        }
        if (paramValues.length < 1) {
            return new int[0];
        }
        int[] values = new int[paramValues.length];
        for (int i=0; i<paramValues.length; i++) {
            try {
                values[i] = Integer.parseInt(paramValues[i]);
            }
            catch (Exception e) {
                values[i] = defaultNum;
            }
        }
        return values;
    }

    /**
     * Gets a parameter as a double.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the parameter you want to get
     * @return The double value of the parameter specified or the default value
     *      if the parameter is not found.
     */
    public static double getDoubleParameter(HttpServletRequest request,
            String name, double defaultNum)
    {
        String temp = request.getParameter(name);
        if(temp != null && !temp.equals("")) {
            double num = defaultNum;
            try {
                num = Double.parseDouble(temp);
            }
            catch (Exception ignored) {}
            return num;
        }
        else {
            return defaultNum;
        }
    }

    /**
     * Gets a parameter as a long.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the parameter you want to get
     * @return The long value of the parameter specified or the default value if
     *      the parameter is not found.
     */
    public static long getLongParameter(HttpServletRequest request,
            String name, long defaultNum)
    {
        String temp = request.getParameter(name);
        if (temp != null && !temp.equals("")) {
            long num = defaultNum;
            try {
                num = Long.parseLong(temp);
            }
            catch (Exception ignored) {}
            return num;
        }
        else {
            return defaultNum;
        }
    }

    /**
     * Gets a list of long parameters.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the parameter you want to get
     * @param defaultNum The default value of a parameter, if the parameter
     * can't be converted into a long.
     */
    public static long[] getLongParameters(HttpServletRequest request,
            String name, long defaultNum)
    {
        String[] paramValues = request.getParameterValues(name);
        if (paramValues == null) {
            return null;
        }
        if (paramValues.length < 1) {
            return new long[0];
        }
        long[] values = new long[paramValues.length];
        for (int i=0; i<paramValues.length; i++) {
            try {
                values[i] = Long.parseLong(paramValues[i]);
            }
            catch (Exception e) {
                values[i] = defaultNum;
            }
        }
        return values;
    }

    /**
     * Gets a parameter as a string.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the parameter you want to get
     * @return The value of the parameter or null if the parameter was not
     *      found or if the parameter is a zero-length string.
     */
    public static String getAttribute(HttpServletRequest request, String name) {
        return getAttribute (request, name, false);
    }

    /**
     * Gets a parameter as a string.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the parameter you want to get
     * @param emptyStringsOK Return the parameter values even if it is an empty string.
     * @return The value of the parameter or null if the parameter was not
     *      found.
     */
    public static String getAttribute(HttpServletRequest request,
            String name, boolean emptyStringsOK)
    {
        String temp = (String)request.getAttribute(name);
        if (temp != null) {
            if (temp.equals("") && !emptyStringsOK) {
                return null;
            }
            else {
                return temp;
            }
        }
        else {
            return null;
        }
    }

    /**
     * Gets an attribute as a boolean.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the attribute you want to get
     * @return True if the value of the attribute is "true", false otherwise.
     */
    public static boolean getBooleanAttribute(HttpServletRequest request,
            String name)
    {
        String temp = (String)request.getAttribute(name);
        if (temp != null && temp.equals("true")) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Gets an attribute as a int.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the attribute you want to get
     * @return The int value of the attribute or the default value if the
     *      attribute is not found or is a zero length string.
     */
    public static int getIntAttribute(HttpServletRequest request,
            String name, int defaultNum)
    {
        String temp = (String)request.getAttribute(name);
        if (temp != null && !temp.equals("")) {
            int num = defaultNum;
            try {
                num = Integer.parseInt(temp);
            }
            catch (Exception ignored) {}
            return num;
        }
        else {
            return defaultNum;
        }
    }

    /**
     * Gets an attribute as a long.
     * @param request The HttpServletRequest object, known as "request" in a
     *      JSP page.
     * @param name The name of the attribute you want to get
     * @return The long value of the attribute or the default value if the
     *      attribute is not found or is a zero length string.
     */
    public static long getLongAttribute(HttpServletRequest request,
            String name, long defaultNum)
    {
        String temp = (String)request.getAttribute(name);
        if (temp != null && !temp.equals("")) {
            long num = defaultNum;
            try {
                num = Long.parseLong(temp);
            }
            catch (Exception ignored) {}
            return num;
        }
        else {
            return defaultNum;
        }
     }
}
