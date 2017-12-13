package com.nsn.web.lte.ningbo.sys;

import java.io.File;

import org.h2.jdbcx.JdbcDataSource;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.nsn.logger.Logger;
import com.nsn.scheduler.SchedulerService;
import com.nsn.web.SystemMenu;
import com.nsn.web.SystemMenuPath;
import com.nsn.web.lte.Const;
import com.nsn.web.lte.DoSystem;
import com.nsn.web.lte.DoWebApplication;
import com.nsn.web.lte.DoWebModule;
import com.nsn.web.lte.db.DSType;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.DbPro;
import com.nsn.web.lte.db.SqlMap;
import com.nsn.web.lte.db.dialect.H2Dialect;
import com.nsn.web.lte.mvc.Actions;
import com.nsn.web.lte.ningbo.sys.action.CacheLoadAction;
import com.nsn.web.lte.ningbo.sys.action.DemoAction;
import com.nsn.web.lte.ningbo.sys.action.GisCellAction;
import com.nsn.web.lte.ningbo.sys.action.IndexAction;
import com.nsn.web.lte.ningbo.sys.action.LoginAction;
import com.nsn.web.lte.ningbo.sys.action.LogsAction;
import com.nsn.web.lte.ningbo.sys.action.MenuAction;
import com.nsn.web.lte.ningbo.sys.action.RoleAction;
import com.nsn.web.lte.ningbo.sys.action.UserAction;
import com.nsn.web.lte.ningbo.sys.service.CacheLoadTask;
import com.nsn.web.lte.ningbo.sys.service.LogQueue;
import com.nsn.web.lte.utils.HomeUtil;
import com.nsn.web.lte.utils.IconUtil;
import com.nsn.web.lte.utils.PropUtil;

public class Activator implements BundleActivator {
	private Logger log = Logger.getLogger(this.getClass().getName());
	private final static String MODULE_ID = "perm_id";
	private final static String MODULE_NAME = "系统管理";
	private final static String ID = "/sys";

	@Override
	public void start(final BundleContext context) throws Exception {
		DoWebApplication webapp = new DoWebApplication(DoSystem.getWebSystem(), context.getBundle(), this.getClass().getClassLoader());
		webapp.setContextPath(ID);
		DoSystem.getWebSystem().setSecurityHandler(new DoSecurityHandler());
		// set actions list to base lte
		String userId = ID + "/user";
		String roleId = ID + "/role";
		String gisCellId = ID + "/gisCell";
		String logsId = ID + "/logs";
		String demoId = ID + "/demo";
		String cacheId = ID + "/cacheLoad";
		String menuId = ID + "/menu";
		/*String doxnzbId = ID + "/doxnzb";
		Actions.add(doxnzbId, DoXnzbAction.class);*/
		Actions.add(ID + "/index", IndexAction.class);
		Actions.add(ID + "/login", LoginAction.class);
		Actions.add(userId, UserAction.class);
		Actions.add(roleId, RoleAction.class);
		Actions.add(gisCellId, GisCellAction.class);
		Actions.add(logsId, LogsAction.class);
		Actions.add(demoId, DemoAction.class);
		Actions.add(cacheId, CacheLoadAction.class);
		Actions.add(menuId, MenuAction.class);
		// load sql xml to base lte
		SqlMap.loadSql(this.getClass(),"sql", DSType.MAIN);

		// 系统管理
		SystemMenu root = new SystemMenu().id(MODULE_ID).name(MODULE_NAME).clazz("glyphicon-cog");

		final String authName = "权限管理";
		SystemMenu auth = new SystemMenu().id("menuid").name(authName).icon("fa-user");

		final String userName = "用户管理";
		SystemMenuPath userPath = new SystemMenuPath().menu(root).next(true).menu(auth).next(true).menu(new SystemMenu().id(userId).name(userName).icon("fa-user"));
		DoWebModule userModule = new DoWebModule(webapp, userId, userName, userPath).setModuleUrl(userId);
		webapp.addWebModule(userModule);

		final String roleName = "角色管理";
		SystemMenuPath rolePath = new SystemMenuPath().menu(root).next(true).menu(auth).next(true).menu(new SystemMenu().id(roleId).name(roleName).icon("fa-users"));
		DoWebModule roleModule = new DoWebModule(webapp, roleId, roleName, rolePath).setModuleUrl(roleId);
		webapp.addWebModule(roleModule);

		final String gisCellName = "基站扇区";
		SystemMenuPath gisCellPath =  new SystemMenuPath().menu(root).next(true).menu(new SystemMenu().id(gisCellId).name(gisCellName).icon("fa-map"));
		DoWebModule gisCellModule = new DoWebModule(webapp, gisCellId, gisCellName, gisCellPath).setModuleUrl(gisCellId);
		webapp.addWebModule(gisCellModule);


	    final String cacheName = "缓存管理";
	    SystemMenuPath cachePath =  new SystemMenuPath().menu(root).next(true).menu(new SystemMenu().id(cacheId).name(cacheName).icon("fa-cogs"));
	    DoWebModule cacheModule = new DoWebModule(webapp, cacheId, cacheName, cachePath).setModuleUrl(cacheId);
	    webapp.addWebModule(cacheModule);

		final String logsName = "系统日志";
		SystemMenuPath logsPath =  new SystemMenuPath().menu(root).next(true).menu(new SystemMenu().id(logsId).name(logsName).icon("fa-address-book-o"));
		DoWebModule logsModule = new DoWebModule(webapp, logsId, logsName, logsPath).setModuleUrl(logsId);
		webapp.addWebModule(logsModule);

		final String menuName = "菜单设置";
		SystemMenuPath menuPath =  new SystemMenuPath().menu(root).next(true).menu(new SystemMenu().id(menuId).name(menuName).icon("fa-gears"));
		DoWebModule menuModule = new DoWebModule(webapp, menuId, menuName, menuPath).setModuleUrl(menuId);
		webapp.addWebModule(menuModule);

		/*final String doxnzbName = "系统监控";
		SystemMenuPath doxnzbPath = new SystemMenuPath(root.clone()).menu(new SystemMenu().id(doxnzbId).name(doxnzbName).icon("fa-thermometer-half"));
		DoWebModule doxnzbModule = new DoWebModule(webapp, doxnzbId, doxnzbName, doxnzbPath).setModuleUrl(doxnzbId);
		webapp.addWebModule(doxnzbModule);*/

		final String demoName = "Demo演示";
		SystemMenuPath demoPath =  new SystemMenuPath().menu(root).next(true).menu(new SystemMenu().id(demoId).name(demoName).icon(IconUtil.icon(demoName)));
		DoWebModule demoModule = new DoWebModule(webapp, demoId, demoName, demoPath).setModuleUrl(demoId);
		webapp.addWebModule(demoModule);
		//只有在缓存配置启动的时候，才启动缓存自动加载机制
		if(Const.cacheSwitch()){
			PropUtil prop = PropUtil.use(new File(HomeUtil.etc() + "web.properties"));
		    String loadExp = prop.get("cache.load.exp");
		    String loadUri = prop.get("cache.load.uri");
		    SchedulerService.getScheculer().registerTask("doweb", "task_cache_load", loadExp, new CacheLoadTask(loadUri));
		    SchedulerService.getScheculer().start();
		}
		/*// Demo管理
		final String moduleDemo = "Demo";
		DoWebModule demoModule = new DoWebModule(webapp, demoId, moduleRole, new SystemMenuPathFactory() {
			@Override
			public SystemMenuPath create(Locale locale) {
				return new SystemMenuPath()
						.menu(new SystemMenu().id(PSERMISS_ID).name(PSERMISS_NAME).title(PSERMISS_NAME)
								.clazz("glyphicon glyphicon-cog"))
						.next(true).menu(new SystemMenu().id(demoId).name(moduleDemo).title(moduleDemo)
								.icon(IconUtil.icon(moduleDemo)).clazz(IconUtil.bg_color(moduleDemo)));
			}
		});
		demoModule.setModuleUrl(demoId);
		webapp.addWebModule(demoModule);*/
		DoSystem.getWebSystem().addWebApplication(webapp);
		initH2Log();
		LogQueue.start();
		log.info("DO LTE Web System STARTED!");
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		log.info("DO LTE Web System STOPPED.");
	}
	
	private void initH2Log(){
		String dbPath = HomeUtil.home() + "log" + File.separator + "dolog";
		//jdbc:h2:file:D:/dolog;mode=mysql
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setUrl("jdbc:h2:file:"+dbPath);
		DbPro.init(DSType.H2LOG, dataSource, new H2Dialect(), false);
		Db.use(DSType.H2LOG).update("CREATE TABLE IF NOT EXISTS PUBLIC.SYS_LOGS (\n" + 
				"	ID VARCHAR(20) NOT NULL,\n" + 
				"	USER_ID VARCHAR(20) NOT NULL,\n" + 
				"	USER_NAME VARCHAR(100) NOT NULL,\n" + 
				"	MODULE VARCHAR(255) DEFAULT NULL,\n" + 
				"	IP VARCHAR(20) DEFAULT NULL,\n" + 
				"	PARAMS CLOB,\n" + 
				"	\"RESULT\" CLOB,\n" + 
				"	REMARK VARCHAR(255) DEFAULT NULL,\n" + 
				"	CREATE_TIME TIMESTAMP NOT NULL\n" + 
				")");
	}
}
