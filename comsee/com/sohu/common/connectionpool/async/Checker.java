package com.sohu.common.connectionpool.async;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Checker implements Runnable {

	protected AsyncGenericConnectionPool pool = null;
	protected Thread _thread = null;
	protected Object _threadLock = new Object();
	
	protected Checker(AsyncGenericConnectionPool pool){
		this.pool = pool;
	}
	/**
	 * 启动当前的线程
	 * @param name 启动线程的名字
	 */
	public void startThread(){
		synchronized(_threadLock){
			if( _thread == null || !_thread.isAlive()){
				_thread = new Thread(this, this.pool.getName()+"(Checker)");
				_thread.start();
			}
		}
	}
	public void stopThread(){
		synchronized(_threadLock){
			_thread = null;
		}
		
	}
	public void run(){
		while(true){
			// check if thread has been stopped
			if( _thread != Thread.currentThread())
				break;
			
			do{ 
				StringBuffer sb = new StringBuffer();
				sb.append("[STATUS] [");
				sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
				sb.append("--\t\t\t");
				
				ServerStatus[] sss = this.pool.getAllStatus();
				if( sss == null ) break;
				
				int liveServers = 0;
				long totalTimes = 0;
				long maxTime = 0;
				int maxServer = -1;
				boolean hasLongDead = false;
				int hasFreeServer = 0; //判断是否有服务器队列较短，可以处理转发请求
				for (int i = 0; i < sss.length; i++) {
					//对各个server做检查，排除当前处理时间过长的server
					ServerStatus ss = sss[i]; 
					ss.statusLog(sb);
					if (ss != null && ss.isServerAlive()){
						if (ss.getServerAvgTime() > 0){
							if (ss.longRequestDead)
								hasLongDead = true; //最多只有一台因为时间长而被踢出
							liveServers ++;
							totalTimes +=  ss.getServerAvgTime();
							if (maxTime < ss.getServerAvgTime()){
								maxTime = ss.getServerAvgTime();
								maxServer = i;
							}
						}
						//是否有队列
						if (ss.waitQueue.size() < 2){
							hasFreeServer ++;
						}
					}
				}
				sb.append("]");
				System.err.println(sb.toString());
				long avgTime = 0;
				if (liveServers > 1){
					avgTime = (totalTimes-maxTime)/(liveServers-1);
				}
				
				//根据当前是否有空闲server决定所有服务器的可转发状态
				//如果所有服务器队列都已经满了，就应该停止转发
				boolean shouldClone = false;
				if (hasFreeServer >= sss.length/3){
					shouldClone = true;
				}else{
					System.out.println("server reach limit!");
				}
                // System.out.println("freeServer num:"+hasFreeServer);
					
				for (int i = 0; i < sss.length; i++) {
					ServerStatus ss = sss[i]; 
					if (ss != null){
						ss.shouldCloneFlag = shouldClone;
					}
				}
				
				if (avgTime > 0){
					for (int i = 0; i < sss.length; i++) {
						//检查目前因为平均时间过长而死掉的机器状态
						ServerStatus ss = sss[i]; 
						if (ss != null && ss.longRequestDead){
							if (ss.getServerAvgTime() < pool.getMaxResponseTime() || ss.getServerAvgTime()/avgTime < pool.getMaxResponseRadio()){
								//debug bart
								System.out.println("[pool]Server is back from long request dead: "+ss.serverInfo);
								ss.longRequestDead = false;
							}
						}
					}
					if (!hasLongDead && maxServer >= 0 && liveServers == sss.length && liveServers > 2){
						//目前server均可用
						if (maxTime >= pool.getMaxResponseTime() && maxTime/avgTime >= pool.getMaxResponseRadio()){
							//最大server响应时间是平均响应时间的两倍
							//踢掉此server
							//debug bart
							System.out.println("[pool]Server is dead because long request dead: "+sss[maxServer].serverInfo);
							sss[maxServer].longRequestDead = true;
						}
					}
				}
			} while( false ); // 多条件判断do .. while循环
			
			// 执行完一轮任务后休眠
			try{
				synchronized(_threadLock){
					_threadLock.wait(1000);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	/**
	 * @return the pool
	 */
	public AsyncGenericConnectionPool getPool() {
		return pool;
	}
	/**
	 * @param pool the pool to set
	 */
	public void setPool(AsyncGenericConnectionPool pool) {
		this.pool = pool;
	}
}
