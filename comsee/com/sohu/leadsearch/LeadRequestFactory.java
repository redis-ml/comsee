package com.sohu.leadsearch;

import com.sohu.common.connectionpool.Request;
import com.sohu.common.connectionpool.RequestFactory;

public class LeadRequestFactory implements RequestFactory {

	/**
	 * 生成竞价的请求体
	 * 暂时没有实现，标识为不可用了
	 * @deprecated
	 */
	public Request newRequest() {
		Request ret = null;
		return ret;
	}
	

	public Request newProbeRequest(){
		Request ret = null;
		LeadRequest tmp = new LeadRequest(
				"bidprobe", 0, null, null, null, "127.0.0.1", 
				null, "", 0, 1, 10, false, 0);
		
		ret = tmp;
		return ret;
	}
}
