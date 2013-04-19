package com.sohu.leadsearch;

import com.sohu.common.connectionpool.Result;
import com.sohu.common.connectionpool.udp.AsyncRequest;

public class LeadRequest extends AsyncRequest {
	volatile String query = null;

	volatile int page = 0;

	volatile String uri = "ad";
	
	volatile String pid = null;
	//url中的p参数，为了给广告做一些p参数的判断
	volatile String p = null;
	//url中的p参数，为了给广告做一些p参数的判断
	volatile String w = null;

	volatile String cpc = null;

	volatile String ip = null;
		
	volatile boolean isDebug = false;

	volatile boolean isInitiate = false;

	volatile String extra = null;

	volatile int number = 0;

	volatile String referer = null;

	volatile boolean isResultValid = false; // result是否有效
	
	volatile int area_code = 0;

	volatile String leadcookie = null;
	
	volatile String suidcookie = null;
	
	volatile String userAgent = null;
	
	volatile int policyno = 0;
	
	volatile String xforwardfor = null;
	
	volatile String sig="";
	
	public int getPolicyno() {
		return policyno;
	}
	public void setPolicyno(int policyno) {
		this.policyno = policyno;
	}
	public String getXforwardfor() {
		return xforwardfor;
	}
	public void setXforwardfor(String xforwardfor) {
		this.xforwardfor = xforwardfor;
	}
	/**
	 * @return the userAgent
	 */
	public String getUserAgent() {
		return userAgent;
	}
	/**
	 * @param userAgent the userAgent to set
	 */
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	/// 本次查询的唯一标识。同pvinsight等统计绑定起来
	volatile String uuid = null;

	volatile int start = 1;
	/**
	 * 向广告服务器传递的样式版本信息
	 */
	volatile int ver = 0;
	
	public int getArea_code() {
		return area_code;
	}
	public void setArea_code(int area_code) {
		this.area_code = area_code;
	}
	public Result getResult() {
		return (Result)getResult(0);
	}
	
	/**
	 * 创建request对象, 填入必需参数.
	 * 
	 * @param sen 查询词
	 * @param uri 查询类型
	 * @param page 页码
	 * @param num  广告数量
	 * @param ip   来访者ip
	 * @param pid  联盟pid
	 * @param cpc  联盟id
	 */
//	area_code	0-231	0	unsigned int
//	pid	字符串	“sohu”	
//	ip	0.0.0.0-255.255.255.255	无	用户IP
//	referrer	字符串	无	编码过的Referer
//	cookie	字符串	无	名为ld的cookie值
//	start	1-1000	1	起始的结果排名
//	page	1-100	1	页码
//	num	1-100	10	每页条数
	public LeadRequest( String query, int area_code, String pid, String p , String w , String ip,
			String referer,	String cookie, int start ,int page, int num, boolean isDebug, long requestId){
		this.query = query;
		this.area_code = area_code;
		this.pid = pid;
		this.p = p;
		this.w = w;
		this.ip = ip;
		this.referer = referer;
		this.leadcookie = cookie;
		this.start = start;
		this.page = page;
		this.number = num;
		this.isDebug = isDebug;
		setRequestId( requestId);
	}
	/**
	 * 兼容旧的构造函数
	 * @param query 查询词
	 * @param area_code 广告区域掩码
	 * @param pid 外部传入的pid参数(不是p参数)
	 * @param ip 访问者的ip
	 * @param referer 访问的referer
	 * @param cookie cookie
	 * @param start 广告的起始位置
	 * @param page 广告的页码
	 * @param num 一页的数量
	 * @param isDebug 是否是调试
	 * @param requestId 查询id
	 */
	public LeadRequest( String query, int area_code, String pid, String ip,
			String referer,	String cookie, int start ,int page, int num, boolean isDebug, long requestId){
		this(query, area_code, pid, null, null, ip, referer, cookie, start, page, num, isDebug, requestId);
	}

	public LeadRequest( String query, String uri,int page, int num, String pid, String p ,String w , 
			String cpc, String ip){
		this.query = query;
		this.uri = uri;
		this.page = page;
		this.number = num;
		this.pid = pid;
		this.p = p;
		this.w = w;
		this.cpc = cpc;
		this.ip = ip;
	}
	public boolean isResultValid() {
		return isResultValid;
	}
	public void setResultValid(boolean isResultValid) {
		this.isResultValid = isResultValid;
	}
	public String getExtra() {
		return extra;
	}
	public void setExtra(String extra) {
		this.extra = extra;
	}
	private static int flowId = 1;
	private static final Object flowIdLock = new Object();
	/**
	 * 返回一个非负整数.用于轮询操作.
	 * @return
	 */
	private static final int getFlowId(){
		int ret;
		synchronized(flowIdLock){
			flowId ++;
			if( flowId < 0 ){
				flowId = 1;
			}
			ret = flowId;
		}
		return ret;
	}
	@Override
	public int getServerId(int total) {
		if( total > 0 ){
			return getFlowId() % total;
		} else {
			return -1;
		}
	}
	@Override
	public boolean isValid() {
		return true;
	}
	public String getCpc() {
		return cpc;
	}
	public void setCpc(String cpc) {
		this.cpc = cpc;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	
	public String getP() {
		return p;
	}
	public void setP(String p) {
		this.p = p;
	}
	public String getW() {
		return w;
	}
	public void setW(String w) {
		this.w = w;
	}
	public String getLeadcookie() {
		return leadcookie;
	}
	public void setLeadcookie(String leadcookie) {
		this.leadcookie = leadcookie;
	}
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public int getPage() {
		return page;
	}
	public void setPage(int page) {
		this.page = page;
	}
	public String getPid() {
		return pid;
	}
	public void setPid(String pid) {
		this.pid = pid;
	}
	public String getReferer() {
		return referer;
	}
	public void setReferer(String referer) {
		this.referer = referer;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		this.start = start;
	}
	public boolean isDebug() {
		return isDebug;
	}
	public void setDebug(boolean isDebug) {
		this.isDebug = isDebug;
	}
	/**
	 * @return the suidcookie
	 */
	public String getSuidcookie() {
		return suidcookie;
	}
	/**
	 * @param suidcookie the suidcookie to set
	 */
	public void setSuidcookie(String suidcookie) {
		this.suidcookie = suidcookie;
	}
	/**
	 * @return the isInitiate
	 */
	public boolean isInitiate() {
		return isInitiate;
	}
	/**
	 * @param isInitiate the isInitiate to set
	 */
	public void setInitiate(boolean isInitiate) {
		this.isInitiate = isInitiate;
	}
	/**
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}
	/**
	 * @param uuid the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	
	public int getVer() {
		return this.ver;
	}
	public void setVer(int ver) {
		this.ver = ver;
	}
	/* qt表示query 类型*/
	volatile String qt = "";

	public String getQt() {
		return qt;
	}
	public void setQt(String qt) {
		this.qt = qt;
	}
	
	public void setSig(String sig){
		if(sig!=null&&sig.length()>0){
			this.sig=sig;
		}
	}
	
	public String getSig(){
		return this.sig;
	}
}
