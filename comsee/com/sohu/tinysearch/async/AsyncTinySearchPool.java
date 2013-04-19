package com.sohu.tinysearch.async;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sohu.common.connectionpool.async.AsyncGenericConnectionPool;

public class AsyncTinySearchPool extends AsyncGenericConnectionPool {

	public AsyncTinySearchPool(String name) {
		super(new AsyncTinySearchClientFactory(), name);
	}

	private static final Log logger = LogFactory
			.getLog(AsyncTinySearchPool.class);

	protected Log getLogger() {
		return logger;
	}
}
