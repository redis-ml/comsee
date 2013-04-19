package com.sohu.leadsearch.async;

import com.sohu.common.connectionpool.RequestFactory;
import com.sohu.common.connectionpool.udp.AsyncRequest;
import com.sohu.common.connectionpool.udp.ServerStatus;

public class Checker implements Runnable {

	protected AsyncLeadPool pool = null;
	protected Thread _thread = null;
	protected Object _threadLock = new Object();
	
	protected Checker(AsyncLeadPool pool){
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
		long probeId = 0;
		while(true){
			// check if thread has been stopped
			if( _thread != Thread.currentThread())
				break;
			
			do{ // 多条件判断使用do .. while(false)循环减少嵌套
				ServerStatus[] sss = this.pool.getAllStatus();
				if( sss == null ) break;
				
				RequestFactory factory = this.pool.getRequestFactory();
				if( factory == null ){
					break;
				}
				
				long now = System.currentTimeMillis();
				// 逐个检查各个server
				for(int i=0; i<sss.length; i++){
					ServerStatus ss = sss[i];
					// 跳过正常的server
					if( ss.isServerAvaliable() )
						continue;
//					if( now - ss.getDowntime() < pool.getMinProbeTime() )
//						continue;
					
					AsyncRequest req = (AsyncRequest)factory.newProbeRequest();
					if( req == null ) continue;
					req.setRequestId(probeId++);
					req.setProbe(true);
					this.pool.sendRequestById(i,req);
				}
			} while( false ); // 多条件判断do .. while循环
			
			// 执行完一轮任务后休眠
			try{
				synchronized(_threadLock){
					_threadLock.wait(pool.getMinProbeTime());
				}
			}catch(Exception e){}
		}
	}
	/**
	 * @return the pool
	 */
	public AsyncLeadPool getPool() {
		return pool;
	}
	/**
	 * @param pool the pool to set
	 */
	public void setPool(AsyncLeadPool pool) {
		this.pool = pool;
	}
}
