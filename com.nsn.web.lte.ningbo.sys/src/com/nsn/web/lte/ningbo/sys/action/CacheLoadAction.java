package com.nsn.web.lte.ningbo.sys.action;

import java.io.File;
import java.sql.Timestamp;

import org.apache.commons.lang3.StringUtils;

import com.nsn.web.lte.cache.CacheManager;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.Page;
import com.nsn.web.lte.db.Record;
import com.nsn.web.lte.mvc.ReqContext;
import com.nsn.web.lte.ningbo.sys.service.CacheLoadTask;
import com.nsn.web.lte.utils.HomeUtil;
import com.nsn.web.lte.utils.Idw;
import com.nsn.web.lte.utils.PropUtil;
import com.nsn.web.lte.utils.Ret;

public class CacheLoadAction {
	public String index(){
		return "cacheLoad.html";
	}
	
	public void loadCache() {
		PropUtil prop = PropUtil.use(new File(HomeUtil.etc() + "web.properties"));
	    String loadUri = prop.get("cache.load.uri");
	    cleanAllCache();
	    new CacheLoadTask(loadUri).execute();
	}
	
	public Page<Record> getPage(ReqContext rc){
		String where = rc.where();
		String sql = "select * from sys_cache";
		if(StringUtils.isNotBlank(where)){
			sql += " where " + where;
		}
		Page<?> pageParam = rc.page();
		if(pageParam.isOrderBy()){			
			sql += " order by " + pageParam.orderBy();
		}
		Page<Record> page = Db.page(sql, pageParam); 
		return  page;
	}
	
	public Record getCache(ReqContext rc){
		String id = rc.param("id");
		String sql = "select * from sys_cache where id = ?";
		Record record = Db.read(sql, id);
		return record;
	}
	
	public Ret saveCache(ReqContext rc){
		Record rd = rc.form();
		if(StringUtils.isBlank(rd.get("id"))){
			rd.set("id", Idw.id());
			rd.set("create_time", new Timestamp(System.currentTimeMillis()));
			Db.save("sys_cache", rd);
		}else{
			Db.update("sys_cache","id", rd);
		}
		return Ret.ok().set("msg","缓存管理保存成功！");
	}
	
	public Ret delCache(ReqContext rc){
		long cid = rc.paramToLong("id");
		String sql = "delete from sys_cache where id = ?";
		Db.update(sql, cid);
		return Ret.ok().set("msg", "缓存管理删除成功！");
	}
	
	public Ret cleanAllCache(){
		try{
			CacheManager.clearAll();
		}catch(Exception e){
			e.printStackTrace();
			return Ret.fail();
		}
		return Ret.ok();
	}
}
