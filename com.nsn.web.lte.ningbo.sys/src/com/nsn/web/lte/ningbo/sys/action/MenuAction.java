package com.nsn.web.lte.ningbo.sys.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.nsn.web.lte.Const;
import com.nsn.web.lte.beans.Menu;
import com.nsn.web.lte.mvc.ReqContext;
import com.nsn.web.lte.ningbo.sys.service.MenuService;
import com.nsn.web.lte.utils.GsonUtil;
import com.nsn.web.lte.utils.Idw;
import com.nsn.web.lte.utils.ReqUtil;

public class MenuAction {
	public String index(){
		return "menu.html";
	}
	
	public Menu defaultMenu(ReqContext rc){
		return MenuService.defaultMenu();
	}
	
	public Menu customMenu(ReqContext rc){
		Menu menu = MenuService.customMenu();
		if(menu==null){
			menu = new Menu();
			menu.setId("CUSTOM_MENU");
			menu.setName("自定义菜单");
		}
		return menu;
	}
	
	public void exportMenuJson(ReqContext rc){
		String menu = rc.param("menu");//视图信息
		Menu exportMenuJson = GsonUtil.parse(menu,Menu.class);
		
		FileInputStream fis = null;
		OutputStream os = null;
		String strPath = System.getProperty("java.io.tmpdir") + File.separator + Idw.id();
		File path = new File(strPath);
		if (!path.exists()) {
			path.mkdirs();
		}
		String filePath = strPath + File.separator + "menu.json";
		try {
			File jsonFile = new File(filePath);
			FileUtils.write(jsonFile, GsonUtil.toJson(exportMenuJson) ,"UTF-8");
			// 设置下载所需的配置信息
			String strName = new String(("menu.json").getBytes(Const.ENCODING), "ISO8859-1");
			if (ReqUtil.isIE(rc.request())) {
				strName = URLEncoder.encode(("menu.json"), "ISO8859-1");
			}
			HttpServletResponse rsp = rc.response();
			rsp.setHeader("Content-Disposition", "attachment; filename=" + strName);
			rsp.setHeader("Content-Length", String.valueOf(jsonFile.length()));
			rsp.setContentType("application/octet-stream");
			// 输出文件到浏览器
			os = rsp.getOutputStream();
			fis = new FileInputStream(jsonFile);
			IOUtils.copyLarge(fis, os);
			os.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(fis);
			IOUtils.closeQuietly(os);
			FileUtils.deleteQuietly(new File(filePath).getParentFile());
		}
	}
}
