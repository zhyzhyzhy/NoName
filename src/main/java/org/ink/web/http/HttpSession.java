package org.ink.web.http;

import io.netty.channel.Channel;
import org.ink.security.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhuyichen on 2017/8/15.
 */
public class HttpSession {

    //sessionId
    private String sessionId;

    //对应的身份验证，如果无就是null
    private User user;

    //for personal attributes, lazy initialization
    private Map<String, Object> attributes;

    private Channel channel;

    public Channel channel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    //过期时间，用于清除
    private long expiredTime = System.currentTimeMillis() + 10000;

    public long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(long expiredTime) {
        this.expiredTime = expiredTime;
    }

    public HttpSession(){}

    public HttpSession(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    //lazy init
    public void addAttribute(String key, String value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
    }

}
