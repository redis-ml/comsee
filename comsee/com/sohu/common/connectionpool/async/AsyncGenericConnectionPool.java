/*
 * Created on 2006-11-24
 *
 */
package com.sohu.common.connectionpool.async;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;

/**
 * 连接池
 * 
 * 1. 提供出错处理功能. 工作过程中,对外不返回任何的错误信息.
 * 2. 可在线配置. 包括服务器地址.
 * 3. 支持多线程
 * 
 * @author LiuMingzhu (mingzhuliu@sohu-inc.com)
 * 
 */
public abstract class AsyncGenericConnectionPool extends ServerConfig {

    // / random
    protected static final Random random                   = new Random();

    // / 保存服务器状态信息
    protected ServerStatus[]      status;
    // / socket连接失败时，会自动选择一个替代连接，替代品在inplaceConnectionLife次query后自动断开
    protected int                 inplaceConnectionLife    = 500;

    Selector                      selector;

    protected Receiver            recver;
    protected Sender              sender;
    protected Checker             checker;

    protected Object              recverLock               = new Object();
    protected Object              senderLock               = new Object();

    protected AsyncClientFactory  factory;

    /**
     * 当AsyncGenericConnectionPool被用作连接cache server时，可以在某一环坏掉的情况下，自动将请求转向另一环（按一定的规则转向，保证相同的请求仍打到相同的Server上）
     * 这个功能默认是开启的
     * 如果AsyncGenericConnectionPool被用作连DB Server时，某一环挂了，再换别的环也没用，这种情况下可由派生类将此功能关掉
     */
    private boolean               isAutoSwitchToNextServer = true;

    /**
     * 创建新实例
     */
    protected AsyncGenericConnectionPool(AsyncClientFactory factory, String name) {
        this.factory = factory;
        if (name != null) {
            this.name = name;
        }
    }

    public void init() throws Exception {

        ArrayList servers = new ArrayList();

        if (this.servers == null)
            throw new IllegalArgumentException("config is NULL");

        String[] list = pat.split(this.servers);

        for (int i = 0; i < list.length; i++) {
            ServerStatus ss = new ServerStatus(list[i], this);
            servers.add(servers.size(), ss);
        }

        ServerStatus[] serverStatus = (ServerStatus[]) servers.toArray(new ServerStatus[servers.size()]);

        selector = Selector.open();

        this.status = serverStatus;

        recver = new Receiver(this);
        sender = new Sender(this);

        recver.startThread();
        sender.startThread();

        if (this.maxResponseTime > 0) {
            checker = new Checker(this);
            checker.startThread();
        }
    }

    /**
     * 获得记录器实例
     * 
     * @return
     */
    protected abstract Log getLogger();

    public int sendRequest(AsyncRequest request) {

        if (request == null) {
            return -1;
        }

        if (!request.isValid()) {
            request.illegalRequest();
            return -2;
        }

        int serverCount = this.getServerIdCount();
        int ret = request.getServerId(serverCount);

        if (!isServerAvaliable(ret) && request.clonableRequest && request.connectType == AsyncRequest.NORMAL_REQUEST
                && isServerShouldRerty(ret)) {
            // debug bart
            System.out.println("[pool " + request.ruid + "]Retry server " + getStatus(ret).serverInfo);
            // 发送一个重试请求，必定发送给本机
            AsyncRequest req = request.clone();
            req.connectType = AsyncRequest.RETRY_REQUEST;
            ServerStatus ss = getStatus(ret);
            if (ss != null) {
                ss.retryCount++;
            }
            sendRequest(req);
        }

        // 如果当前请求是普通请求，且分环的目标服务器down了，并且isAutoSwitchToNextServer配置为不自动选择下一个可用环，则直接返回错误
        if (!isServerAvaliable(ret) && request.connectType == AsyncRequest.NORMAL_REQUEST && !isAutoSwitchToNextServer) {
            request.serverDown("No server available, and no alternatives will be picked");
            return -1;
        }

        /**
         * 以下代码块用来寻找一个可能的下一环服务器，并将请求发送过去，这个功能适用两个情况：
         * 1. 当前请求是一个影子请求（即由ServerStatus内为检测连接状态而发出的），这种请求不发给本来应该发的目标，而是下一个可用的服务器
         * 2. 原请求是普通请求，但是目标的cache server不available，所以要另选下一个可用的环来发送请求。
         * 寻找下一有效环的策略可以保证在down掉的server不变的情况下，不同查询的新目标机分布在整个分环空间，而同样查询的目标是固定的。
         * 
         * 对于第2种适用情况，如果本pool连接的目标是非cache的，可以通过将isAutoSwitchToNextServer设为false来关掉自动寻找下一环的功能，
         * 这时如果原请求的目标server挂了，直接返回失败。
         */
        if (request.connectType == AsyncRequest.SHADOW_NORMAL_REQUEST
                || request.connectType == AsyncRequest.SHADOW_QUEUE_REQUEST ||
                (!isServerAvaliable(ret) && request.connectType != AsyncRequest.RETRY_REQUEST)) {

            // System.out.println("server is not avaliable");
            int avaliableServerCount = 0;
            for (int i = 0; i < getServerIdCount(); i++) {
                if (isServerAvaliable(i)) {
                    avaliableServerCount++;
                }
            }

            // 对于影子请求，不能发送给本机
            if ((request.connectType == AsyncRequest.SHADOW_NORMAL_REQUEST || request.connectType == AsyncRequest.SHADOW_QUEUE_REQUEST)
                    && isServerAvaliable(ret)) {
                avaliableServerCount--;
            }

            if (avaliableServerCount <= 0) {
                request.serverDown("检前无可用server");
                return -1;
            }

            // 测试次数.
            int inc = (request.getServerId(avaliableServerCount)) + 1;

            int finalIndex = ret;

            int i = 0;
            do {
                int j = 0;
                boolean find = false;
                do {
                    finalIndex = (finalIndex + 1) % serverCount;
                    if (isServerAvaliable(finalIndex) && (finalIndex != ret)) {
                        find = true;
                        break;
                    }
                    j++;
                } while (j < serverCount);

                if (!find) {
                    request.serverDown("检后无可用server");
                    return -1;
                }

                i++;
            } while (i < inc);

            ret = finalIndex;
        }

        int serverId = ret;

        if (serverId < 0 || serverId >= this.getServerIdCount()) {
            request.serverDown("ServerId计算出错");
            return -1;
        }

        ServerStatus ss = getStatus(serverId);

        if (ss == null) {
            request.serverDown("不能获取server状态");
            return -2;
        }

        request.setServer(ss);
        request.setServerInfo(ss.getServerInfo());
        request.queueSend();
        sender.senderSendRequest(request);

        return 0;
    }

    /**
     * 将某台server暂时挂起，不再向其发送请求
     * 或者将某台server重新激活
     * 
     * @param ip
     *            :服务器ip或地址, action:启动与否
     * @return 成功与否
     */
    public boolean holdServer(String ip, boolean action) {
        String addr = null;
        int i = 0;
        for (; i < getServerIdCount(); i++) {
            addr = status[i].addr.toString();
            if (addr.indexOf(ip) >= 0) {
                break;
            }
        }
        return holdServer(i, action);
    }

    /**
     * 将某台server暂时挂起，不再向其发送请求
     * 或者将某台server重新激活
     * 
     * @param key
     *            :服务器的key, action:启动与否
     * @return 成功与否
     */
    public boolean holdServer(int key, boolean action) {
        ServerStatus ss = null;
        if (status != null && key >= 0 && key < status.length) {
            ss = status[key];
        }
        StringBuffer sb = new StringBuffer();
        
        if (ss == null) {
        	sb.append("swithed nothing");
        	System.out.println(sb.toString());
            return false;
        }

        ss.swithcer = action;
        
        
        try{
			sb.append(ss.addr.toString());
			sb.append(" is switched ");
			sb.append(action?"on":"off");
			sb.append(" at ");
			sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }catch(Exception e){
        	sb.append("swithed error");
        }
        System.out.println(sb.toString());

		return true;
    }

    /**
     * @return
     */
    public int getServerIdBits() {
        return 0;
    }

    /**
     * @return
     */
    public int getServerIdMask() {
        return 0;
    }

    /**
     * @param i
     * @return 第i台服务器
     */
    public InetSocketAddress getServer(int i) {
        return status[i].getAddr();
    }

    /**
     * @return Returns the inplaceConnectionLife.
     */
    public int getInplaceConnectionLife() {
        return inplaceConnectionLife;
    }

    /**
     * @param inplaceConnectionLife
     *            The inplaceConnectionLife to set.
     */
    public void setInplaceConnectionLife(int inplaceConnectionLife) {
        this.inplaceConnectionLife = inplaceConnectionLife;
    }

    private static Pattern pat = Pattern.compile("\\s+");

    public ServerStatus[] getAllStatus() {
        return status;
    }

    /**
     * 返回指定序号的服务器的状态对象.
     * 
     * @param i
     * @return 如果指定序号的服务器不存在,则返回null
     */
    public ServerStatus getStatus(int i) {
        if (status != null
                && i >= 0
                && i < status.length) {
            return status[i];
        }
        else {
            return null;
        }
    }

    public boolean isServerShouldRerty(int i) {
        ServerStatus ss = null;
        if (status != null && i >= 0 && i < status.length) {
            ss = status[i];
        }
        if (ss == null) {
            return false;
        }

        return ss.isServerShouldRerty();
    }

    public boolean isServerAvaliable(int i) {
        ServerStatus ss = null;
        if (status != null && i >= 0 && i < status.length) {
            ss = status[i];
        }
        if (ss == null) {
            return false;
        }

        boolean ret = ss.isServerAvaliable();
        if (!ret) {
            Log logger = getLogger();
            if (logger != null && logger.isTraceEnabled())
                logger.trace("server is not avaliable:" + ss.getServerInfo());
        }
        return ret;
    }

    /**
     * 返回序号i对应的服务器在连接池中对应的键值.
     * 如果对应的服务器非法(不存在),则返回null;
     * 
     * @param i
     * @return
     */
    public Object getServerKey(int i) {
        if (status != null
                && i >= 0
                && i < status.length
                && status[i] != null
                && status[i].key != null) {
            return status[i].key;
        }
        else {
            return null;
        }
    }

    /**
     * 返回已注册的目标服务器总数
     * 
     * @return
     */
    public int getServerIdCount() {
        if (status == null) {
            return 0;
        }
        else {
            return status.length;
        }
    }

    public InetSocketAddress getSocketAddress(int i) {
        if (status == null
                || i < 0
                || i >= status.length
                || status[i] == null) {
            return null;
        }
        else {
            return status[i].getAddr();
        }
    }

    public void finalize() {
        destroy();
    }

    public void destroy() {
        sender.stopThread();
        sender = null;
        recver.stopThread();
        recver = null;
        ServerStatus[] temp = status;
        status = null;
        if (temp != null) {
            for (int i = 0; i < temp.length; i++) {
                ServerStatus ss = temp[i];
                if (ss == null)
                    continue;
                ss.destroy();
            }
        }
        try {
            this.selector.close();
        }
        catch (IOException e) {

        }
    }

    public String status() {
        StringBuffer sb = new StringBuffer();
        sb.append("\nPool Status: ");
        sb.append(this.getName());
        sb.append('\n');

        for (int i = 0; i < this.status.length; i++) {
            status[i].status(sb);
        }

        if (getLogger().isInfoEnabled()) {
            getLogger().info(sb.toString());
        }
        return sb.toString();
    }

    protected boolean getIsAutoSwitchToNextServer() {
        return this.isAutoSwitchToNextServer;
    }

    protected void setAutoSwitchToNextServer(boolean isAutoSwitchToNextServer) {
        this.isAutoSwitchToNextServer = isAutoSwitchToNextServer;
    }

}
