package com.david.websocket;

/**
 * =================================
 * Created by David on 2018/11/11.
 * mail:    17610897521@163.com
 * 描述:
 */

import com.david.tool.WebSocketTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@ServerEndpoint("/{user}")
public class WebSocketServer {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 在线人数
     */
    private static int onlineCount = 0;

    /**
     * concurrent包的线程安全map，用来存放Session对象
     */
    public static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap();

    /**
     * 定义锁对象 用于发送消息/获取当前连接IP
     */
    private Lock lock = new ReentrantLock();

    /**
     * 建立连接/会话
     *
     * @param user    用户
     * @param session 会话
     */
    @OnOpen
    public void onOpen(@PathParam("user") String user, Session session) {
        //添加在线人数
        addOnlineCount();
        //定义断开连接消息
        String disconnectMsg = null;
        //定义连接消息
        String connectMessage = null;
        Session connectedSession = getConnSession(user, session);
        //判断是否存在已连接会话
        if (connectedSession == null) {//不存在
            //拼接连接消息
            connectMessage = "连接成功";
        } else {//存在
            //拼接重新连接消息
            connectMessage = "重新连接";
            //拼接断开连接消息
            disconnectMsg = "被挤下线";
        }
        //发送消息给当前连接
        sendMessage(connectMessage, session);
        //已连接会话不为空,断开已连接会话并发消息告知
        if (connectedSession != null) {
            //发送消息给已连接会话
            sendMessage(disconnectMsg, connectedSession);
            try {
                //断开已连接会话
                connectedSession.close();
            } catch (IOException e) {
                logger.error("[onOpen] user: {}; IOException: {}", user, e.toString());
            }
        }
    }

    /**
     * 获得已连接会话,并将当前会话保存在sessionMap中
     *
     * @param user
     * @param session
     * @return
     */
    private synchronized Session getConnSession(String user, Session session) {
        //获得已连接会话
        Session connectedSession = sessionMap.get(user);
        //保存当前会话
        sessionMap.put(user, session);
        return connectedSession;
    }

    /**
     * 接收到客户端消息(字符串类型)
     *
     * @param message : 客户端消息
     * @param session : 客户端会话
     *                接收bytes数组  public void onMessage(byte[] messages, Session session) {
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        //将收到的消息返回
        sendMessage(new StringBuffer().append("服务器收到消息: ").append(message).append(";当前在线人数:").append(onlineCount).toString(), session);
    }

    /**
     * 断开连接
     *
     * @param user 用户
     */
    @OnClose
    public void onClose(@PathParam("user") String user, Session session) {
        //通过用户获取session会话
        Session userSession = sessionMap.get(user);
        //如果sessionMap中存在该Session对象,移除该Session对象
        if (userSession != null && userSession == session) sessionMap.remove(user);
        //发送断开连接消息
        sendMessage("连接断开", session);
        //断开连接
        //session.close();TODO
        //在线人数减一
        subOnlineCount();
    }

    /**
     * 连接出错回调
     *
     * @param session 会话
     * @param error   异常
     */
    @OnError
    public void onError(Session session, Throwable error) {
        logger.warn("onError: {}", error);
    }


    /**
     * 发送消息到指定会话
     *
     * @param message
     * @param session
     */
    public void sendMessage(String message, Session session) {
        try {
            lock.lock();//加锁
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(message, true);
            }
        } catch (IOException e) {
            logger.info("sendMessage IOException cause: {}", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获得IP地址
     *
     * @param session
     * @return
     */
    public String getIPAddress(Session session) {
        String hostName = null;
        if (session == null) return hostName;
        lock.lock();//加锁
        InetSocketAddress address = WebSocketTool.getRemoteAddress(session);
        hostName = address.getHostName();
        lock.unlock();//释放锁
        return hostName;
    }

    /**
     * 在线人数添加
     */
    public static synchronized void addOnlineCount() {
        onlineCount++;
    }

    /**
     * 在线人数减少
     */
    public static synchronized void subOnlineCount() {
        onlineCount--;
    }

    /**
     * 获取当前在线人数
     *
     * @return
     */
    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

}

