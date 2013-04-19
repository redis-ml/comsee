package com.sohu.common.connectionpool.async;

public class ServerConfig {


/// 发生多少次错误后,启动sleep机制
protected int maxErrorsBeforeSleep = 4;
///	发生错误后，多长时间内不再重新尝试
protected int sleepMillisecondsAfterTimeOutError = 30000;
protected int maxConnectionsPerServer = 8;
protected long connectTimeout = 70;
protected long socketTimeout = 10000l;
//socketFailTimeout只有大于此值时，此请求才会真正失败，从而返回null
//一般情况下socketFailTimeout = socketTimeout
//所以只要超时就fail
protected int maxClonedRequest = 2;

protected long socketFailTimeout = 0l; 

//排队转发时间
protected long queueShortTimeout = 600l;
//排队超时时间
protected long queueTimeout = 3000l;
protected long robinTime = 500;
protected int maxQueueSize = 10000;

//平均响应时间参数
protected long maxResponseTime = 0l;
protected int maxResponseRadio = 5;

//影子连接重试
protected long shortRetryTime = 0l;

public long getShortRetryTime() {
	return shortRetryTime;
}

public void setShortRetryTime(long shortRetryTime) {
	this.shortRetryTime = shortRetryTime;
}

public long getMaxResponseTime() {
	return maxResponseTime;
}

public void setMaxResponseTime(long maxResponseTime) {
	this.maxResponseTime = maxResponseTime;
}

public int getMaxResponseRadio() {
	return maxResponseRadio;
}

public void setMaxResponseRadio(int maxResponseRadio) {
	this.maxResponseRadio = maxResponseRadio;
}




String servers;
String name = "Pool";

public String getServers() {
	return servers;
}

public void setServers(String servers) {
	this.servers = servers;
}

public int getMaxClonedRequest() {
	return maxClonedRequest;
}

public void setMaxClonedRequest(int maxClonedRequest) {
	this.maxClonedRequest = maxClonedRequest;
}

public int getMaxErrorsBeforeSleep() {
	return maxErrorsBeforeSleep;
}

public void setMaxErrorsBeforeSleep(int maxErrorsBeforeSleep) {
	this.maxErrorsBeforeSleep = maxErrorsBeforeSleep;
}

public int getSleepMillisecondsAfterTimeOutError() {
	return sleepMillisecondsAfterTimeOutError;
}

public synchronized void setSleepMillisecondsAfterTimeOutError(
		int sleepMillisecondsAfterTimeOutError) {
	this.sleepMillisecondsAfterTimeOutError = sleepMillisecondsAfterTimeOutError;
}

public int getMaxConnectionsPerServer() {
	return maxConnectionsPerServer;
}

public void setMaxConnectionsPerServer(int maxConnectionsPerServer) {
	this.maxConnectionsPerServer = maxConnectionsPerServer;
}

public long getConnectTimeout() {
	return connectTimeout;
}

public void setConnectTimeout(long connectTimeout) {
	this.connectTimeout = connectTimeout;
}

public long getQueueShortTimeout() {
	if (queueShortTimeout == 0l){
		return getQueueTimeout();
	}
	return queueShortTimeout;
}

public void setQueueShortTimeout(long queueShortTimeout) {
	this.queueShortTimeout = queueShortTimeout;
}

public long getSocketTimeout() {
	return socketTimeout;
}

public void setSocketTimeout(long socketTimeout) {
	this.socketTimeout = socketTimeout;
}

public long getSocketFailTimeout() {
	if (socketFailTimeout == 0l){
		return getSocketTimeout();
	}
	return socketFailTimeout;
}

public void setSocketFailTimeout(long socketFailTimeout) {
	this.socketFailTimeout = socketFailTimeout;
}

public String getName() {
	return name;
}

public void setName(String name) {
	this.name = name;
}

public long getQueueTimeout() {
	return queueTimeout;
}

public void setQueueTimeout(long queueTimeout) {
	this.queueTimeout = queueTimeout;
}

public long getRobinTime() {
	return robinTime;
}

public void setRobinTime(long robinTime) {
	this.robinTime = robinTime;
}

/**
 * @return the maxQueueSize
 */
public int getMaxQueueSize() {
	return maxQueueSize;
}

/**
 * @param maxQueueSize the maxQueueSize to set
 */
public void setMaxQueueSize(int maxQueueSize) {
	this.maxQueueSize = maxQueueSize;
}

}
