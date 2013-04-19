package com.sohu.tinysearch.async;

import com.sohu.common.connectionpool.async.AsyncClientFactory;
import com.sohu.common.connectionpool.async.AsyncGenericQueryClient;


public class AsyncTinySearchClientFactory implements AsyncClientFactory {

	public AsyncGenericQueryClient newInstance() {
		return new AsyncTinySearchClient();
	}

}
