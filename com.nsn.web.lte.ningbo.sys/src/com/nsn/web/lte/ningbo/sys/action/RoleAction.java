package com.nsn.web.lte.ningbo.sys.action;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.nsn.web.lte.Const;
import com.nsn.web.lte.beans.Menu;
import com.nsn.web.lte.beans.User;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.Page;
import com.nsn.web.lte.db.Record;
import com.nsn.web.lte.mvc.ReqContext;
import com.nsn.web.lte.ningbo.sys.service.PermService;
import com.nsn.web.lte.utils.Idw;

public class RoleAction {
	public String index(){
		return "role.html";
	}
	
	/**
	 * 获取当前登陆用户创建的角色列表
	 * @param rc
	 * @return
	 * @throws IOException
	 */
	public Page<Record> getPage(ReqContext rc) throws IOException{
		StringBuffer sb = new StringBuffer("select * from sys_role WHERE ");
		//仅当前用户创建的用户
		User loginUser = rc.sessionAttr(Const.LOGIN_USER);
		sb.append(UserAction.rootpass("create_by = ? ",loginUser));
		//过滤条件
		String where = rc.where(Const.SCH_PREFIX);
		if(StringUtils.isNoneBlank(where)){
			sb.append(" AND ").append(where);
		}
		Page<?> pageParam = rc.page();
		sb.append(" ORDER BY ").append("ID");
		Page<Record> page = Db.page(sb.toString(), pageParam, loginUser.getId());
		return page;
	}
	
	/*
	 * 查询角色
	 */
	public Map<String, Object> getRole(ReqContext rc){
		Map<String, Object> result = new HashMap<String, Object>();
		Record rd = new Record();
		if(StringUtils.isNotBlank(rc.param("id"))){
			try{
				rd = Db.read("select * from sys_role where id = ?", rc.param("id"));
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
				//校验角色名
				String name = rc.param("name");
				if(StringUtils.isBlank(name)){
					result.put("msg", "角色名为空！");
					return result;
				}
				Record exist = Db.read("select * from sys_role where name = ?", name);
				if(exist!=null && exist.getColumns()!=null){
					result.put("msg", "角色名已存在！");
					return result;
				}
				
				Record rd = new Record();
				rd.remove("op");
				
				rd.set("id", String.valueOf(Idw.id()));
				rd.set("name", rc.param("name"));
				rd.set("type", rc.param("type"));
				rd.set("enabled", rc.paramToInt("enabled"));
				rd.set("remark", rc.param("remark",""));
				rd.set("create_time", new Timestamp(System.currentTimeMillis()));
				rd.set("create_by", loginUser.getId());
				
				Db.save("sys_role", rd);
				
				result.put("flag", "succ");
				result.put("msg", "操作成功！");
			} else if("edit".equals(op)) {
				String roleId = rc.param("id");
				Record rd = Db.read("select * from sys_role where id = ?", roleId);
				if(rd!=null && rd.getColumns()!=null){

					//角色名
					String name = rc.param("name");
					if(StringUtils.isBlank(name)){
						result.put("msg", "角色名为空！");
						return result;
					}
					Record exist = Db.read("select * from sys_role where name = ? and id<>?", name, roleId);
					if(exist!=null && exist.getColumns()!=null){
						result.put("msg", "角色名已存在！");
						return result;
					}
					rd.set("name", name);
					rd.set("type", rc.param("type"));
					rd.set("enabled", rc.paramToInt("enabled"));
					if("1".equals(rc.param("type"))){
						rd.set("remark", rc.param("remark",""));
					}else{
						rd.set("remark", null);
					}
					
					
					Db.update("sys_role","id", rd);
					
					result.put("flag", "succ");
					result.put("msg", "操作成功！");
				}else{
					result.put("msg", "角色不存在！");
				}
			}
			
		} catch (Exception e) {
			result.put("msg", "操作失败！");
			e.printStackTrace();
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
		if(StringUtils.isNotBlank(rc.param("id"))){
			try{
				Db.update("delete from sys_role where id = ?",rc.param("id"));
				//删除关联
				Db.update("delete from sys_user_role_do where role_id = ?",rc.param("id"));
				
				result.put("flag", "succ");
				result.put("msg", "操作成功！");
			}catch (Exception e) {
				result.put("msg", "操作失败！");
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/*
	 * 获取角色的用户列表
	 */
	public Map<String, Object> getRelateList(ReqContext rc){
		Map<String, Object> result = new HashMap<String, Object>();
		List<Record> relateLits = new ArrayList<Record>();//已关联
		List<Record> unRelateLits = new ArrayList<Record>();//未关联
		try{
			//校验rid
			if(StringUtils.isNotBlank(rc.param("id"))){
				User loginUser = rc.sessionAttr(Const.LOGIN_USER);
				String rid = rc.param("id");
				
				StringBuffer sb = new StringBuffer();
				sb.append("select u.id uid,u.account from sys_user_do u ");
				sb.append("right join sys_user_role_do ur on u.id=ur.user_id ");
				sb.append("where ur.role_id=? order by u.id");
				relateLits = Db.query(sb.toString(), rid);
				
				sb.setLength(0);
				sb.append("select u.id uid, u.account from sys_user_do u where ");
				sb.append(UserAction.rootpass("u.create_by=? ", loginUser));//仅限当前用户创建的用户
				sb.append("and not exists (select ur.* from sys_user_role_do ur ");
				sb.append("where ur.role_id = ? and ur.user_id = u.id) order by uid");
				unRelateLits = Db.query(sb.toString(), loginUser.getId(), rid);
				
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
		if(StringUtils.isNotBlank(rc.param("rid"))){
			try{
				User loginUser = rc.sessionAttr(Const.LOGIN_USER);
				String rid = rc.param("rid");//角色id
				//清空该角色原有的关联用户
				Db.update("delete from sys_user_role_do where role_id=?", rid);
				//添加新关联
				if(StringUtils.isNoneBlank(rc.param("uids"))){
					String[] _uids = rc.param("uids").split(",");
					for(String uid : _uids){
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
				result.put("msg", "操作失败！");
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/*
	 * 获取当前用户拥有的资源菜单
	 */
	public List<Menu> menu(ReqContext rc){
		User loginUser = rc.sessionAttr(Const.LOGIN_USER);
		//List<String> loginUserPerms =  loginUser.getPerms();//登陆者拥有的资源
		List<Menu> menus = PermService.menu(loginUser.getAccount(), loginUser.getPerms());
		//childs("", loginUserPerms);
		//System.out.println(GsonUtil.toJson(menus,false));
		return menus;
	}
	
//	private Set<SystemMenu> childs(String parentId, List<String> loginUserPerms){
//		Set<SystemMenu> mns = new HashSet<>();
//		List<SystemMenu> menus = DoSystem.getWebSystem().getSystemMenu(parentId, Locale.CHINESE);
//		for(int i=0;i<menus.size();i++){
//			SystemMenu m = menus.get(i);
//			if(!loginUserPerms.contains(m.id())){
//				//menus.remove(i);
//				continue;
//			}
//			if(m.module()==null){
//		    	m.childs(false).addAll(childs(parentId + "/"+m.id(), loginUserPerms));
//		    }else{
//		    	m.module(null);
//		    }
//			mns.add(m);
//		}
//		return mns;
//	}
	
	/*
	 * 获取角色的菜单
	 */
	public List<Record> getRoleMenu(ReqContext rc){
		List<Record> list = new ArrayList<Record>();
		try{
			String rid = rc.param("id");
			if(StringUtils.isNotBlank(rid)){
				StringBuffer sb = new StringBuffer();
				sb.append("select rm.id, rm.module from sys_role_module rm ");
				sb.append("where rm.role_id=?");
				list = Db.query(sb.toString(), rid);
			}
		}catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
	
	/*
	 * 角色关联菜单
	 */
	public Map<String, Object> auth(ReqContext rc){
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("flag", "fail");
		String rid = rc.param("rid");//角色id
		if(StringUtils.isNotBlank(rid)){
			try{
				User loginUser = rc.sessionAttr(Const.LOGIN_USER);
				//清空该角色原有的关联用户
				Db.update("delete from sys_role_module where role_id=?", rid);
				
				//添加新关联
				if(StringUtils.isNotBlank(rc.param("mids"))){
					String[] mids = rc.param("mids").split(",");
					for(String mid : mids){
						Record rd = new Record();
						rd.set("id", String.valueOf(Idw.id()));
						rd.set("role_id", rid);
						rd.set("module", mid);
						rd.set("create_time", new Timestamp(System.currentTimeMillis()));
						rd.set("create_by", loginUser.getId());
						
						Db.save("sys_role_module", rd);
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
}
