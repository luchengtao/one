package com.nsn.web.lte.ningbo.sys.service;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.nsn.web.SystemMenu;
import com.nsn.web.WebSystem;
import com.nsn.web.lte.Const;
import com.nsn.web.lte.DoSystem;
import com.nsn.web.lte.beans.Menu;
import com.nsn.web.lte.utils.GsonUtil;
import com.nsn.web.lte.utils.HomeUtil;

/**
 * 获取系统菜单
 */
public class MenuService {
	
	/**
	 * 获取系统菜单
	 * 1. 若存在配置文件，则从配置文件中读取。2. 反之根据SystemMenu生成
	 * @return
	 */
	public static Menu menu(){
		Menu menu = customMenu();
		if(menu==null){
			menu = defaultMenu();
		}
		return menu;
	}
	
	/**
	 * 获取自定义菜单
	 * @return
	 */
	public static Menu customMenu() {
		System.out.println("load custom menu...");
		Menu customMenus = null;
		File customMenuFile = new File(HomeUtil.etc() + "menu.json");
		if(customMenuFile.exists()){
			try {
				String customMenu = FileUtils.readFileToString(customMenuFile, Charset.forName(Const.ENCODING));
				if(StringUtils.isEmpty(customMenu)){
					throw new RuntimeException("faild to load custom menu.");
				}
				customMenus = GsonUtil.parse(customMenu,Menu.class);
			} catch (Exception e) {
				e.printStackTrace();
				customMenus = null;
			}
		}
		if(customMenus == null){
			System.out.println("can NOT load custom menu");
		}
		return customMenus;
	}
	
	/**
	 * 获取默认菜单
	 * @return
	 */
	public static Menu defaultMenu() {
		System.out.println("load default menu...");
		List<Menu> list = new ArrayList<>();
		WebSystem web = DoSystem.getWebSystem();
		List<SystemMenu> menus = web.getSystemMenu("", Locale.getDefault());
		for (SystemMenu menu : menus) {
			Menu m = Menu.load(menu);
			m.setChilds(childs(web, menu));
			list.add(m);
		}
		Menu defaultMenu = new Menu();
		defaultMenu.setId("DEFAULT_MENU");
		defaultMenu.setName("系统默认菜单");
		defaultMenu.setChilds(list);
		return defaultMenu;
	}
	private static List<Menu> childs(WebSystem web, SystemMenu menu) {
		List<Menu> list = new ArrayList<>();
		Locale ch = Locale.getDefault();
		List<SystemMenu> menus = web.getSystemMenu("/" + menu.id(), ch);
		for (SystemMenu mn : menus) {
			Menu m;
			if (mn.module() == null) {
				List<Menu> child = new ArrayList<>();
				List<SystemMenu> childs = web.getSystemMenu("/" + menu.id() + "/" + mn.id(), ch);
				for (SystemMenu mc : childs) {
					if (mc.module() != null) {
						Menu m1 = Menu.load(mc);
						child.add(m1);
					}
				}
				m = Menu.load(mn);
				m.setChilds(child);
			} else {
				m = Menu.load(mn);
			}
			list.add(m);
		}
		return list;
	}
}
