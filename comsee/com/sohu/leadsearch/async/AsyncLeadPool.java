/**
 * 
 */
package com.sohu.leadsearch.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sohu.common.connectionpool.udp.AsyncGenericConnectionPool;
import com.sohu.common.connectionpool.udp.AsyncRequest;
import com.sohu.leadsearch.LeadRequestFactory;

/**
 * @author liumingzhu
 *
 */
public class AsyncLeadPool extends AsyncGenericConnectionPool {

	private static int LeadId = 0x533;
	private static Object leadId_lock = new Object();
	public static int getLeadId(){
		int t;
		synchronized( leadId_lock ){
			t = LeadId ++;
		}
		return t;
	}

	private static final Log logger = LogFactory.getLog(AsyncLeadPool.class);
	/* (non-Javadoc)
	 * @see com.sohu.common.connectionpool.udp.AsyncGenericConnectionPool#getLogger()
	 */
	@Override
	protected Log getLogger() {
		return logger;
	}
	

	/* (non-Javadoc)
	 * @see com.sohu.common.connectionpool.udp.AsyncGenericConnectionPool#sendRequest(com.sohu.common.connectionpool.udp.AsyncRequest)
	 */
	@Override
	public int sendRequest(AsyncRequest request) {
		if( request.getRequestId() == 0 ){
			long id = getLeadId();
			request.setRequestId(id);
		}
		return super.sendRequest(request);
	}


	public AsyncLeadPool(){
		super( new AsyncLeadFactory(), "lead", new LeadRequestFactory() );
		// Æô¶¯Checker
		Checker checker = new Checker(this);
		checker.startThread();
		
	}
}
