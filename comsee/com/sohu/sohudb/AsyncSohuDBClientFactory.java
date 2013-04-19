package com.sohu.sohudb;

import com.sohu.common.connectionpool.async.AsyncClientFactory;
import com.sohu.common.connectionpool.async.AsyncGenericQueryClient;


public class AsyncSohuDBClientFactory implements AsyncClientFactory {

	public AsyncGenericQueryClient newInstance() {
		return new AsyncSohuDBClient();
	}

}
