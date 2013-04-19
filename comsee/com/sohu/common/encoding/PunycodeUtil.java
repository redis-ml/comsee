package com.sohu.common.encoding;
public class PunycodeUtil {
	//中文域名的后缀数组
	public static final String[] domainSuffixArray = new String[]{".cn", ".中国", ".公司", ".网络"};
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		System.out.println(containHanzi("wo1你w"));
		System.out.println(ChDomain2PunyDomain("宁波妇科。中国"));
		System.out.println("Aa".toLowerCase());
	}
	/**
	 * 是否包含汉字字符
	 * @param str
	 * @return
	 */
	private static boolean containHanzi(String str) {
		boolean isGB2312 = false;
		if(str==null)
			return isGB2312;
		for (int i = 0; i < str.length(); i++) {
			int temp = str.charAt(i);
			if ( temp > 255 ) {
				isGB2312 = true;
				break;
			}
		}
		return isGB2312;
	}
	/**
	 * 将中文域名转化为小码域名，如果不满足如下中文域名规则，则返回null：
	 * “中文.cn / 中文.中国 / 中文.公司/中文.网络” （其中“.”与“。”等效）
	 * @param HanziStr
	 * @return
	 * @throws Exception 
	 */
	public static String ChDomain2PunyDomain(String ChDomain) throws Exception {
		if(ChDomain == null)
			return null;
		
		//归一化用户输入
		ChDomain = ChDomain.replace("。", ".");//归一化域名分割符
		ChDomain = ChDomain.replace("　", " ");
		ChDomain = ChDomain.trim();
		ChDomain = ChDomain.toLowerCase();//大转小
		
		//后缀部分没有匹配到任一中文域名
		int i;
		for (i = 0; i < domainSuffixArray.length; i++) 
			if(ChDomain.endsWith(domainSuffixArray[i]))
				break;
		if(i==domainSuffixArray.length)
			return null;
		//域名里面不能带空格
		if (ChDomain.indexOf(" ") >= 0) {
			return null;
		}
		
		//除后缀部分外的部分是否包含中文
		int pos = ChDomain.lastIndexOf(".");
		if(pos == -1)	return null;
		String ChDomainContent = ChDomain.substring(0, pos);
		if(!containHanzi(ChDomainContent))	return null;
		
		//不能以点为开头字符，否则判断为非法中文域名
		if(ChDomain.startsWith("."))
			return null;

		String[] ChDomainParts = ChDomain.split("\\.");
		StringBuffer punyDomain_sb = new StringBuffer();
		for (int j = 0; j < ChDomainParts.length; j++) {
			String temp = ChDomainParts[j];
			if(j!=0)
				punyDomain_sb.append(".");
			punyDomain_sb.append( Punycode.encodeUrl(temp) );
		}
		if("".equals( punyDomain_sb.toString() ) )
			return null;
		return "http://"+punyDomain_sb.toString();
	}
	
}
