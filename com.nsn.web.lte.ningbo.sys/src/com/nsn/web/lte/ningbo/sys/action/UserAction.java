package com.nsn.web.lte.ningbo.sys.action;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.nsn.web.lte.Const;
import com.nsn.web.lte.beans.User;
import com.nsn.web.lte.db.DSType;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.Page;
import com.nsn.web.lte.db.Record;
import com.nsn.web.lte.mvc.ExpContext;
import com.nsn.web.lte.mvc.ReqContext;
import com.nsn.web.lte.utils.EncryptUtil;
import com.nsn.web.lte.utils.Idw;

public class UserAction {
	//private TestService service = Enhancer.enhance(TestService.class);
	public String index(){
		//service.test();
		return "user.html";
	}
	
	/**
	 * 获取当前登陆用户创建的用户列表
	 * @param rc
	 * @return
	 * @throws IOException
	 */
	public Page<Record> getPage(ReqContext rc) throws IOException{
		StringBuffer sb = new StringBuffer("select * from sys_user_do where account <> 'root' and ");
		//仅当前用户创建的用户
		User loginUser = rc.sessionAttr(Const.LOGIN_USER);
		sb.append(rootpass("create_by = ? ",loginUser));
		//过滤条件
		String where = rc.where(Const.SCH_PREFIX);
		if(StringUtils.isNoneBlank(where)){
			sb.append(" AND ").append(where);
		}
		Page<?> pageParam = rc.page();
		if(pageParam.isOrderBy()){			
			sb.append(" ORDER BY ").append(pageParam.orderBy());
		}
		Page<Record> page = Db.page(sb.toString(), pageParam, loginUser.getId());
		return page;
	}
	
	/*
	 * 查询用户
	 */
	public Map<String, Object> getUser(ReqContext rc){
		Map<String, Object> result = new HashMap<String, Object>();
		Record rd = new Record();
		String userId = rc.param("id");
		if(StringUtils.isNotBlank(userId)){
			try{
				rd = Db.read("select * from sys_user_do where id = ?", userId);
				result = rd.getColumns();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/*
	 * 保存 
	 * op=add新增; op=edit修改
	 */
	public Map<String, Object> save(ReqContext rc){
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("flag", "fail");
		String op = rc.param("op");
		try{
			User loginUser = rc.sessionAttr(Const.LOGIN_USER);
			if("add".equals(op)){//新增
				//校验用户名
				String account = rc.param("account");
				if(StringUtils.isBlank(account)){
					result.put("msg", "用户名为空！");
					return result;
				}
				Record existUser = Db.read("select * from sys_user_do where account = ?", account);
				if(existUser!=null && existUser.getColumns()!=null){
					result.put("msg", "用户名已存在！");
					return result;
				}
				
				Record rd = rc.form();
				rd.remove("op");
				
				rd.set("id", String.valueOf(Idw.id()));
				rd.set("password", EncryptUtil.md5(rc.param("password","000000")));
				rd.set("create_time", new Timestamp(System.currentTimeMillis()));
				rd.set("create_by", loginUser.getId());
				
				Db.save("sys_user_do", rd);
				
				result.put("flag", "succ");
				result.put("msg", "操作成功！");
			} else if("edit".equals(op)) {
				String userId = rc.param("id");
				Record rd = Db.read("select * from sys_user_do where id = ?", userId);
				if(rd!=null && rd.getColumns()!=null){
					//rd.set("password", EncryptUtil.md5(rc.param("password","000000")));
					rd.set("name", rc.param("name",""));
					rd.set("sex", rc.param("sex",""));
					rd.set("status", rc.paramToInt("status"));
					rd.set("type", rc.param("type",""));
					rd.set("city", rc.param("city",""));
					rd.set("email", rc.param("email",""));
					rd.set("mobile", rc.param("mobile",""));
					rd.set("skin", rc.param("skin",""));
					rd.set("remark", rc.param("remark",""));
					
					Db.update("sys_user_do","id", rd);
					
					result.put("flag", "succ");
					result.put("msg", "操作成功！");
				}else{
					result.put("flag", "fail");
					result.put("msg", "用户名不存在！");
				}
			}
		}catch (Exception e) {
			result.put("msg", "操作失败");
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * 重置密码 000000
	 * @param rc
	 * @return
	 */
	public Map<String, Object> resetpwd(ReqContext rc){
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("flag", "fail");
		String DEFAULT_PASSWORD = "000000";
		if(StringUtils.isNotBlank(rc.param("id"))){
			try{
				String userId = rc.param("id");
				Record rd = Db.read("select * from sys_user_do where id = ?", userId);
				if(rd!=null && rd.getColumns()!=null){
					rd.set("password", EncryptUtil.md5(DEFAULT_PASSWORD));
					Db.update("sys_user_do","id", rd);
					
					result.put("flag", "succ");
					result.put("msg", "操作成功！密码重置为:"+DEFAULT_PASSWORD);
				}else{
					result.put("flag", "fail");
					result.put("msg", "用户不存在！");
				}
				
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return result;
	}
	
	/**
	 * 物理删除
	 * @param rc
	 * @return
	 */
	public Map<String, Object> delete(ReqContext rc){
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("flag", "fail");
		String userId = rc.param("id");
		if(StringUtils.isNotBlank(userId)){
			try{
				Db.update("delete from sys_user_do where id = ?",userId);
				//删除关联
				Db.update("delete from sys_user_role_do where user_id = ?",userId);
				
				result.put("flag", "succ");
				result.put("msg", "操作成功！");
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * 获取被操作用户取已关联和未关联的角色。且只能是当前操作用户自己拥有的角色。
	 * @param 
	 * @return relateLits & unRelateLits
	 */
	public Map<String, Object> getRelateList(ReqContext rc){
		Map<String, Object> result = new HashMap<String, Object>();
		List<Record> relateLits = new ArrayList<Record>();//已关联
		List<Record> unRelateLits = new ArrayList<Record>();//未关联
		try{
			String uid = rc.param("id");
			if(StringUtils.isNotBlank(uid)){//user_id
				User loginUser = rc.sessionAttr(Const.LOGIN_USER);
				
				//被操作用户已有的角色
				StringBuffer sb = new StringBuffer();
				sb.append("SELECT r.id rid,r.name FROM sys_role r ");
				sb.append("RIGHT JOIN sys_user_role_do ur ON r.id=ur.role_id ");
				sb.append("WHERE ur.user_id=?");
				relateLits = Db.query(sb.toString(), uid);
				
				//被操作用户没有的，且当前登陆用户拥有的角色
				sb.setLength(0);
				sb.append("SELECT r.id rid, r.name FROM sys_role r WHERE ");
				sb.append(rootpass("r.create_by=? ", loginUser));//仅限当前用户创建的角色
				sb.append("AND NOT EXISTS (SELECT * FROM sys_user_role_do ur ");
				sb.append("WHERE ur.user_id=? AND ur.role_id = r.id)");
				unRelateLits = Db.query(sb.toString(), loginUser.getId(), uid);
				
				result.put("relate", relateLits);
				result.put("unRelate", unRelateLits);
			}
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
	
	/*
	 * 角色关联用户
	 */
	public Map<String, Object> relate(ReqContext rc){
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("flag", "fail");
		String uid = rc.param("uid");//角色id
		if(StringUtils.isNotBlank(uid)){
			try{
				User loginUser = rc.sessionAttr(Const.LOGIN_USER);
				//清空该角色原有的关联用户
				Db.update("delete from sys_user_role_do where user_id=?", uid);
				//添加新关联
				if(StringUtils.isNoneBlank(rc.param("rids"))){
					String[] _rids = rc.param("rids").split(",");
					for(String rid : _rids){
						Record rd = new Record();
						rd.set("id", String.valueOf(Idw.id()));
						rd.set("user_id", uid);
						rd.set("role_id", rid);
						rd.set("create_time", new Timestamp(System.currentTimeMillis()));
						rd.set("create_by", loginUser.getId());
						
						Db.save("sys_user_role_do", rd);
					}
				}
				result.put("flag", "succ");
				result.put("msg", "操作成功！");
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	public void exp(ReqContext rc){
		User loginUser = rc.sessionAttr(Const.LOGIN_USER);
		ExpContext exp = ExpContext.begin("用户",DSType.DO_LTE);
		Map<String,String> head = new HashMap<>();
		head.put("id", "编号");
		head.put("account", "账号");
		//head.put("password", "密码");
		head.put("name", "姓名");
		head.put("status", "状态");
		head.put("city", "城市");
		head.put("email", "邮箱");
		head.put("mobile", "手机");
		head.put("create_time", "创建时间");
		StringBuffer sb = new StringBuffer("select id,account,name,(CASE status WHEN '1' THEN '启用' ELSE '禁用' END) status,city,email,mobile,create_time from sys_user_do ");
		if(!"root".equals(loginUser.getAccount())){
			sb.append("WHERE create_by = '"+loginUser.getId()+"'");
		}
		sb.append(" order by id ");
		exp.expExcel(sb.toString(), head, "用户信息");
		Map<String,String> headCity = new HashMap<>();
		headCity.put("city_id", "ID");
		headCity.put("city_cn", "名称");
		exp.expExcel("select city_id,city_cn from cfg_city ORDER BY city_id", headCity, "城市配置表");
		exp.end(rc);
	}
	
	/**
	 * 获取城市列表
	 * @param rc
	 * @return
	 */
	public List<Record> getCity(ReqContext rc){
		String sql = "SELECT * FROM cfg_city c ORDER BY c.city_id";
		List<Record> list = Db.query(sql);
		return list;
	}
	
	/**
	 * 用户的城市
	 * @param rc
	 * @return
	 */
	public List<Record> cities(ReqContext rc){
		User loginUser = rc.sessionAttr(Const.LOGIN_USER);
		String sql = "SELECT city_id,city_cn FROM cfg_city c ORDER BY c.city_id";
		if (!"root".equalsIgnoreCase(loginUser.getAccount())) {
			if(StringUtils.isBlank(loginUser.getCity())){
				return null;
			}
			String[] _citis = StringUtils.split(loginUser.getCity(), ",");
			if(!Arrays.asList(_citis).contains("-1")){
				String citis = "'" + StringUtils.join(_citis, "','") + "'";//eg: '1','2','0'
				sql = "SELECT city_id,city_cn FROM cfg_city c WHERE c.city_id in("+citis+") ORDER BY c.city_id";
			}
		}
		return Db.query(sql);
	}
	
	/**
	 * root用户不检查
	 * @param sql 以用户id隔离数据的sql  eg: " create_by = ?"
	 * @return 恒等式   eg: " root的id = ? "
	 */
	protected static String rootpass(String sql, User loginUser){
		if("root".equals(loginUser.getAccount())){
			sql = "'"+loginUser.getId()+"' = ? ";//root查询全部
		}
		return sql;
	}
}
