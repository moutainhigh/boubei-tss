/* ==================================================================   
 * Created [2009-08-29] by Jon.King 
 * ==================================================================  
 * TSS 
 * ================================================================== 
 * mailTo:boubei@163.com
 * Copyright (c) boubei.com, 2015-2018 
 * ================================================================== 
 */

package com.boubei.tss.um.sso.online;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.boubei.tss.framework.sso.Environment;
import com.boubei.tss.framework.sso.context.Context;
import com.boubei.tss.framework.sso.online.IOnlineUserManager;
import com.boubei.tss.modules.param.ParamConstants;
import com.boubei.tss.um.dao.IUserDao;
import com.boubei.tss.um.entity.Group;
import com.boubei.tss.util.EasyUtils;
import com.boubei.tss.util.URLUtil;

/**
 * 在线用户库（数据库）
 */
@Service("DBOnlineUserService")
public class DBOnlineUserService implements IOnlineUserManager {
	
	@Autowired private IUserDao dao;
	
    /*     
     * 如果在线用户库中没有相同的用户存在， 则在在线用户库中添加此记录。
     * 
     * 1、同一IP，同一用户，只能有一个登录session（开多个浏览器不被支持？）
     * 2、同一用户，只能在PC、微信、H5上分别登录一个session
     */
    public void register(String token, String appCode, String sessionId, Long userId, String userName) {
        List<?> list = queryExists(userId);
        
        DBOnlineUser ou;
        if( list.isEmpty() ) {
        	ou = new DBOnlineUser(userId, sessionId, appCode, token, userName);
        	dao.createObject(ou);
        	
        	dao.setLastLoginTime(userId);
        } 
        else {
        	ou = (DBOnlineUser) list.get(0);
        	
        	// 移动端登录/API访问, 不干扰PC端
        	HttpSession session = Context.sessionMap.get(ou.getSessionId());
        	if( session != null && !URLUtil.isWeixin() && !Context.getRequestContext().isApiCall() ) {
        		session.invalidate(); // 销毁当前用户已经登录的session（登录在其它电脑上的），控制账号在多地登录
        		
        		dao.setLastLoginTime(userId);
        	}
        	
        	ou.setSessionId(sessionId);
        	ou.setLoginCount( EasyUtils.obj2Int(ou.getLoginCount()) + 1 );
        	ou.setLoginTime(new Date());
        	ou.setClientIp(Environment.getClientIp());
        	ou.setOrigin( Environment.getOrigin() );
        }
        
        // 设置域信息（每次登陆domain可能已经发生了变化，重新设置）
        String hql = "from Group where id in (select groupId from GroupUser where userId = ?) and groupType = ?";
    	List<?> groups = dao.getEntities(hql, userId, Group.MAIN_GROUP_TYPE);
    	if( groups.size() > 0 ) {
    		Group group = (Group) groups.get(0);
        	ou.setDomain(group.getDomain());
    	}
    	
    	dao.update(ou);
    }
    
    private List<?> queryExists(Long userId) {
    	// 对系统级账号不做限制
    	if( userId < 0 ) {
            return new ArrayList<Object>(); 
    	}
    	
    	// 移动端登录不干扰PC端
    	if( URLUtil.isWeixin() ) {
    		String hql = " from DBOnlineUser o where o.userId = ? and o.origin = ? ";
            return dao.getEntities(hql, userId, URLUtil.QQ_WX);
    	}
    	
    	// 检查域信息配置，判断当前用户所在域是否支持一个账号多地同时登陆（API call 也不踢人）
    	Object multilogin = Environment.getDomainInfo("multilogin");
    	multilogin = EasyUtils.checkNull(multilogin, Environment.getInSession("admin_su"));
    	
		if( ParamConstants.TRUE.equals( multilogin ) || Context.getRequestContext().isApiCall() ) {
    		String hql = " from DBOnlineUser o where o.userId = ? and o.origin = ? and clientIp = ? ";
            return dao.getEntities(hql, userId, Environment.getOrigin(), Environment.getClientIp());
    	}
    	
    	// 一个账号只能登录一台电脑
    	String hql = " from DBOnlineUser o where o.userId = ? ";
        return dao.getEntities(hql, userId);
    }

	public void logout(Long userId) {
		List<?> list = queryExists(userId);		
		dao.deleteAll(list);
	}
 
    /*
     * session超时销毁调用。
     * 根据 SessionId，找到用户并将用户的sessionId置为Null，表示已经注销。
     */
    public String logout(String appCode, String sessionId) {
    	String hql = " from DBOnlineUser o where o.appCode = ? and o.sessionId = ? ";
        List<?> list = dao.getEntities(hql, appCode, sessionId);
        
        String token = null;
    	for(Object entity : list) {
    		DBOnlineUser ou = (DBOnlineUser) dao.delete(entity);
        	token = ou.getToken();
    	}
    	
    	// 将12小时前的在线信息删除（应该都是漏删除的了）
    	long nowLong = new Date().getTime(); 
        Date time = new Date(nowLong - (long) (12 * 60 * 60 * 1000)); 
    	dao.deleteAll(dao.getEntities("from DBOnlineUser o where o.loginTime < ?", time));
    	
		return token;
    }

    public boolean isOnline(String token) {
        List<?> list = dao.getEntities("from DBOnlineUser o where o.token = ? ", token);
		return list.size() > 0;
    }
}
