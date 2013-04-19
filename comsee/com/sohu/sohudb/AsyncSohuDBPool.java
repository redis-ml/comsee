package com.sohu.sohudb;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sohu.common.connectionpool.async.AsyncGenericConnectionPool;

public class AsyncSohuDBPool extends AsyncGenericConnectionPool {

    public AsyncSohuDBPool(String name) {
        super(new AsyncSohuDBClientFactory(), name);

        // SohuDBPool连接的不是cache服务器，当一环失效后，不需要也不应该自动切换至另一环重试
        this.setAutoSwitchToNextServer(false);
    }

    public AsyncSohuDBPool() {
        this("SohuDB");
    }

    private static final Log logger = LogFactory
                                            .getLog(AsyncSohuDBPool.class);

    protected Log getLogger() {
        return logger;
    }
}
