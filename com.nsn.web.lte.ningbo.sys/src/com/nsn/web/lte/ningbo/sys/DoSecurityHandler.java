package com.nsn.web.lte.ningbo.sys;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.nsn.web.SecurityHandler;
import com.nsn.web.lte.Const;
import com.nsn.web.lte.beans.User;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.ningbo.sys.service.LogQueue;

public class DoSecurityHandler implements SecurityHandler {
	@Override
	public boolean secure(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String uri = request.getRequestURI();
		if (uri.startsWith(Const.LOGIN_URL) || uri.startsWith(Const.LOGIN_URL + "/logout")) {
			//doLog(request,null);//记录日志
			return true;
		}
		String ext = FilenameUtils.getExtension(uri);
		if (Const.IGNORE_EXTS.contains(ext)) {
			return true;
		}
		for(String str : Const.IGNORE){
			if (uri.startsWith(str) || uri.contains(str)) {
				return true;
			}
		}
		HttpSession ssn = request.getSession();
		User user = (User) ssn.getAttribute(Const.LOGIN_USER);
		if (Objects.nonNull(user)) {
			LogQueue.log(request, "");//记录日志
			if(uri.equals("/") || uri.equals(Const.CONTEXT_PATH)){
				response.sendRedirect(Const.indexUrl());
			}
			/*if (accessible(ssn, URI)) {
				return true;
			} else {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "无权访问！");
				return false;
			}*/
			return true;
		} else {//如果是ajax请求响应头会有，x-requested-with
			String reqHeader = request.getHeader("x-requested-with"); 
			if (reqHeader != null && reqHeader.equalsIgnoreCase("XMLHttpRequest")) {
				response.setHeader("sessionstatus", "timeout");//在响应头设置session状态  
			} else {
				response.sendRedirect(Const.LOGIN_URL);
			}
			return false;
		}
	}

	@Override
	public Boolean accessible(HttpSession session, String function) {
		Boolean rstFlag = Boolean.FALSE;
		if (StringUtils.isBlank(function)) {
			return rstFlag;
		}
		User user = (User) session.getAttribute(Const.LOGIN_USER);
		if (null == user) {
			return rstFlag;
		}
		String account = user.getAccount();
		if ("root".equalsIgnoreCase(account)) {
			return Boolean.TRUE;
		}
		//sql查询通用工具和地图加载通用工具
		if(function.startsWith(Const.CONTEXT_PATH+"/index") ||
				function.startsWith(Const.CONTEXT_PATH+"/page") || 
				function.startsWith(Const.CONTEXT_PATH+"/tile")){
			return Boolean.TRUE;
		}
		List<String> perms = user.getPerms();
		for (String perm : perms) {
			if (function.startsWith(perm)) {
				return Boolean.TRUE;
			}
		}
		return rstFlag;
	}

	@Override
	public String user(HttpSession session) {
		User user = (User) session.getAttribute(Const.LOGIN_USER);
		if (user == null) {
			return "anonamous";
		}
		return user.getAccount();
	}

	@Override
	public Set<String> groups(HttpSession session) {
		Set<String> set = new HashSet<String>();
		User user = (User) session.getAttribute(Const.LOGIN_USER);
		List<String> list = Db.query(String.class,"select name from sqmdb_rpt.sys_role where enabled = 1 and id = (select role_id from sqmdb_rpt.sys_user_role where user_id = ?)", user.getId());
		set.addAll(list);
		return set;
	}
}