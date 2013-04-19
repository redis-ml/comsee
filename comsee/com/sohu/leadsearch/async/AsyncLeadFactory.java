/**
 * 
 */
package com.sohu.leadsearch.async;

import com.sohu.common.connectionpool.udp.AsyncClientFactory;
import com.sohu.common.connectionpool.udp.AsyncGenericQueryClient;

/**
 * @author liumingzhu
 *
 */
public class AsyncLeadFactory implements AsyncClientFactory {

	/* (non-Javadoc)
	 * @see com.sohu.common.connectionpool.udp.AsyncClientFactory#newInstance()
	 */
	public AsyncGenericQueryClient newInstance() {
		// TODO Auto-generated method stub
		return new AsyncLeadClient();
	}

}
