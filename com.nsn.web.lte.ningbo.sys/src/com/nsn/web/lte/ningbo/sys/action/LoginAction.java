package com.nsn.web.lte.ningbo.sys.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.nsn.web.lte.Const;
import com.nsn.web.lte.beans.User;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.Record;
import com.nsn.web.lte.mvc.ReqContext;
import com.nsn.web.lte.ningbo.sys.service.PermService;
import com.nsn.web.lte.utils.EncryptUtil;
import com.nsn.web.lte.utils.GsonUtil;
import com.nsn.web.lte.utils.Ret;

public class LoginAction {
	public String index(ReqContext rc) throws IOException {
		if (rc.sessionAttr(Const.LOGIN_USER) == null) {
			rc.attr("msg", rc.sessionAttr(Const.RE_LOGIN_ERROR));
			rc.removeAttr(Const.RE_LOGIN_ERROR);
			if(Const.dbCity()) {
				return "loginCity.html";
			}else {
				return "login.html";
			}
		} else {
			rc.redirect(Const.indexUrl());
			return null;
		}
	}
	
	public Ret login(ReqContext rc){
		// 校验用户名
		String account = rc.param("account");
		String passwd = rc.param("passwd");
		if(StringUtils.isBlank(account)){
			return Ret.fail().set("msg","账号不能为空");
		}
		if(StringUtils.isBlank(passwd)){
			return Ret.fail().set("msg","账号不能为空");
		}
		User.relogin(account);
		User user = Db.read(User.class, "select * from sys_user_do where status = '1' and account = ?", account);
		if (user!=null && user.getPassword().equals(EncryptUtil.md5(passwd))) {
			//登陆时一些特殊处理
			if(!checkPerm(user.getAccount())){
				//强制修改初始密码
				if(EncryptUtil.md5("000000").equals(EncryptUtil.md5(passwd))){
					return Ret.fail().set("modifyPwd", Boolean.TRUE);
				}
				//city
//				List<String> citis = Arrays.asList(StringUtils.split(user.getCity(),","));  
//				if(citis==null || citis.isEmpty()){
//					return Ret.fail().set("msg","此账号没有城市信息，请联系管理员");
//				}else if(citis.size()==1){
//					rc.sessionAttr(Const.CITY, citis.get(0));
//				}else{
//					String city = rc.param("city");
//					if(StringUtils.isNotBlank(city) && citis.contains(city)){
//						rc.sessionAttr(Const.CITY, city);
//					}else{
//						return Ret.fail().set("cityList", citis(user));
//					}
//				}
			}
			if(Const.dbCity()) {
				String city = rc.param("city");
				List<Record> cityList = cities(user);
				if(cityList==null || cityList.size()<1){
					return Ret.fail().set("msg","此账号没有城市信息，请联系管理员");
				}
				Record chooseCity = null;
				List<Record> chooseCityList = new ArrayList<Record>();
				if(StringUtils.isNotBlank(city)){
					for(Record c :cityList){
						if(city.equals(c.getStr("city_id"))){
							chooseCity = c;break;
						}
					}
				}
				chooseCityList.add(chooseCity);
				
				if(chooseCity!=null || cityList.size()==1){ //选中地市 或 该用户只有一个地市
					if(chooseCity!=null){
						rc.sessionAttr(Const.CITY, GsonUtil.toJson(chooseCityList));
						rc.sessionAttr(Const.CITY_ID, chooseCity.getStr("city_id"));
					}else {
						rc.sessionAttr(Const.CITY, GsonUtil.toJson(cityList));
						rc.sessionAttr(Const.CITY_ID, cityList.get(0).getStr("city_id"));
					}
				}else{
					return Ret.fail().set("cityList", cityList);
				}
			}else {
				rc.sessionAttr(Const.CITY,GsonUtil.toJson(cities(user)));
			}
			String sql = "select module from sys_role_module where role_id in (select role_id from sys_user_role_do a,sys_role b where a.role_id = b.id and b.enabled = '1' and a.user_id = ?) group by module"; 
			List<String> perms = Db.query(String.class, sql, user.getId());
			user.setPerms(perms);
			rc.sessionAttr(Const.LOGIN_USER, user);
			rc.sessionAttr(Const.MENUS, PermService.menu(account,perms));
			rc.sessionAttr(Const.DESENSITIZATION, desensitization(user));
			return Ret.ok();
		} else {
			return Ret.fail().set("msg","登录失败，请确认是否输入正确的用户名和密码");
		}
	}

	public void logout(ReqContext rc) throws IOException {
		rc.removeSessionAttr(Const.MENUS);
		rc.removeSessionAttr(Const.CITY);
		rc.removeSessionAttr(Const.CITY_ID);
		rc.removeSessionAttr(Const.LOGIN_USER);
		rc.removeSessionAttr(Const.DESENSITIZATION);
		rc.redirect(Const.LOGIN_URL);
	}
	
	public Ret editPasswd(ReqContext rc){
		String passwd = rc.param("passwd");
		String oldPasswd = rc.param("oldPasswd");
		User user = rc.sessionAttr(Const.LOGIN_USER);
		if(user.getPassword().equals(EncryptUtil.md5(oldPasswd))){
			String sql = "update sys_user_do set password = ? where id = ?";
			int rst = Db.update(sql, EncryptUtil.md5(passwd),user.getId());
			if(rst >= 0){
				return Ret.ok().set("msg", "新密码更新成功，请退出重新登录！");				
			}else{
				return Ret.fail().set("msg", "修改密码失败，请联系管理员！");
			}
		}else{
			return Ret.fail().set("msg", "输入的旧密码不正确，请重新输入！");
		}
	}
	
	/**
	 * 修改默认密码
	 * @param rc
	 * @return
	 */
	public Ret editDefaultPasswd(ReqContext rc){
		String passwd = rc.param("passwd");
		String account = rc.param("account");
		User user = Db.read(User.class, "select * from sys_user_do where account = ?", account);
		if(user!=null && !"000000".equals(passwd) && user.getPassword().equals(EncryptUtil.md5("000000"))){
			String sql = "update sys_user_do set password = ? where id = ?";
			int rst = Db.update(sql, EncryptUtil.md5(passwd),user.getId());
			if(rst >= 0){
				return Ret.ok().set("msg", "新密码更新成功，请退出重新登录！");				
			}else{
				return Ret.fail().set("msg", "修改密码失败，请联系管理员！");
			}
		}else{
			return Ret.fail().set("msg", "修改密码失败，请联系管理员！");
		}
	}
	
	public Ret switchCity(ReqContext rc) throws IOException{
		User loginUser = rc.sessionAttr(Const.LOGIN_USER);
		if(Const.dbCity()) {
			String actCity = rc.sessionAttr(Const.CITY_ID);
			List<Record> cityList = cities(loginUser);
			List<Record> chooseCityList = new ArrayList<Record>();
			if (rc.isGet()) {//获取城市列表
				if(cityList==null || cityList.size()<=1){
					return Ret.fail().set("msg", "无可切换城市");
				}else{
					return Ret.ok().set("citis", cityList).set("actCity",actCity);
				}
			} else if (rc.isPost()) {//切换，写入session
				Record chooseCity = null;
				for(Record c :cityList){
					if(rc.param("city_id").equals(c.getStr("city_id"))){
						chooseCity = c;break;
					}
				}
				chooseCityList.add(chooseCity);
				rc.sessionAttr(Const.CITY, GsonUtil.toJson(chooseCityList));
				rc.sessionAttr(Const.CITY_ID, chooseCity.getStr("city_id"));
				return Ret.ok().set("msg", "切换成功");
			}
		}else {
			String actCity = rc.sessionAttr(Const.CITY);
			if (rc.isGet()) {//获取城市列表
				List<String> citis = Arrays.asList(StringUtils.split(loginUser.getCity(),","));  
				if(citis==null || citis.size()<=1){
					return Ret.fail().set("msg", "无可切换城市");
				}else{
					return Ret.ok().set("citis", cities(loginUser)).set("actCity",actCity);
				}
			} else if (rc.isPost()) {//切换，写入session
				rc.sessionAttr(Const.CITY, rc.param("city_id"));
				return Ret.ok().set("msg", "切换成功");
			}
		}
		return null;		
	}
	private List<Record> cities(User loginUser){
		String strCitis = loginUser.getCity();
		String citis = "'" + StringUtils.join(StringUtils.split(loginUser.getCity(), ","), "','") + "'";//eg: '1','2','0'
		String sql = "SELECT city_id,city_cn FROM cfg_city c WHERE c.city_id in("+citis+") ORDER BY c.city_id";
		if("root".equalsIgnoreCase(loginUser.getAccount()) || strCitis.contains("-1")){			
			sql = "select city_id,city_cn from cfg_city c order by c.city_id";
		}
		List<Record> cities = Db.query(sql);
		//“其他”移动到最后
		if(cities!=null && cities.size()>0){
			for(int i = 0; i < cities.size(); i++){
				Record city = cities.get(i);
				if("99".equals(city.getStr("city_id"))){
					cities.add(cities.remove(i));
					break;
				}
			}
		}
		return cities;
	}
	
	/**
	 * 是否脱敏。
	 * 默认脱敏，只要持有任意不脱敏角色则为不脱敏。
	 * 不脱敏角色remark中的标记：sensitive（敏感）
	 */
	public boolean desensitization(User user){
		boolean flag = true;
		if(!checkPerm(user.getAccount())){
			StringBuffer sb = new StringBuffer();
			sb.append("select r.* from sys_role r ");
			sb.append("right join sys_user_role_do ur on r.id=ur.role_id ");
			sb.append("where ur.user_id=?");
			List<Record> rList = Db.query(sb.toString(), user.getId());
			for(Record r : rList){
				if(r.getInt("type") == 1){
					String[] remarks = StringUtils.split(r.getStr("remark"), ",");
					if(remarks!=null && Arrays.asList(remarks).contains("sensitive")){
						flag = false;
					}
				}
			}
		}else{
			flag = false;
		}
		return flag;
	}

	
	private static boolean checkPerm(String userName) {
		if (userName.equalsIgnoreCase("root")) {
			return true;
		}
		return false;
	}
}
