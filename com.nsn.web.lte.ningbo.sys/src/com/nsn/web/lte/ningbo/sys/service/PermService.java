package com.nsn.web.lte.ningbo.sys.service;

import java.util.Iterator;
import java.util.List;

import com.nsn.web.lte.beans.Menu;

public class PermService {
	
	public static List<Menu> menu(String userName, List<String> perms) {
		Menu rootMenu =MenuService.menu();
		List<Menu> menus = rootMenu.getChilds();
		childs(menus, userName, perms);
		return menus;
	}

	private static void childs(List<Menu> menus, String userName, List<String> perms) {
		Iterator<Menu> it = menus.iterator();
		while (it.hasNext()) {
			Menu mn = it.next();
			if (mn.getChilds() != null) {
				childs(mn.getChilds(), userName, perms);//递归
				if (mn.getChilds()==null || mn.getChilds().size()<1){
					//删除空菜单
					//it.remove();
				} 
			}else{
				if (!checkPerm(mn,userName,perms)){
					//删除没有权限的末级菜单
					it.remove();
				} 
			}
		}
	}
	
	private static boolean checkPerm(Menu menu, String userName, List<String> perms) {
		if (userName.equalsIgnoreCase("root") || perms.contains(menu.getId())) {
			return true;
		}
		return false;
	}
}
