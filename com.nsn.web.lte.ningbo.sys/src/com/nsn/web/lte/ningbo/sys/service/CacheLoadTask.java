package com.nsn.web.lte.ningbo.sys.service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.time.DateFormatUtils;

import com.google.gson.reflect.TypeToken;
import com.nsn.logger.Logger;
import com.nsn.scheduler.SchedulerTask;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.Record;
import com.nsn.web.lte.utils.GsonUtil;
import com.nsn.web.lte.utils.OkHttpUtil;

import okhttp3.FormBody;

public class CacheLoadTask extends SchedulerTask {
	private Logger log = Logger.getLogger(getClass());
	private String uri;

	public CacheLoadTask(String uri) {
		if (uri.endsWith("/")) {
			this.uri = uri.substring(0, uri.length() - 1);
		} else {
			this.uri = uri;
		}
	}

	public void execute() throws RuntimeException {
		try {
			Map<String,String> pms = new HashMap<>();
			pms.put("account", "root");
			pms.put("passwd", "1qaz!QAZ");
			OkHttpUtil.postSyncAsString(this.uri + "/sys/login/login", pms);
			List<Record> cities = Db.query("select city_id,city_cn from cfg_city_do c order by c.city_id");
			List<Record> list = Db.query("select * from sys_cache_do");
			for (Record rd : list) {
				String params = rd.getStr("params");
				
				// 替换 sdate
				Calendar cal = Calendar.getInstance();
				cal.setTime(new Date());
				String today = DateFormatUtils.format(cal, "yyyy-MM-dd");
				params = params.replaceAll("#sdate#", today);
				cal.add(Calendar.DATE, -1);
				String yesterday = DateFormatUtils.format(cal, "yyyy-MM-dd");
				params = params.replaceAll("#yesterday#", yesterday);
				
				// 遍历替换 scity 并执行
				for (Record crd : cities) {
					FormBody.Builder frm = new FormBody.Builder();
					String cid = crd.getStr("city_id");
					String cname = crd.getStr("city_cn");
					String json = params.replaceAll("#scity#", cid);
					json = json.replaceAll("#cityname#", cname);
					Type targetType = new TypeToken<Map<String, String>>() {}.getType();
					Map<String, String> map = GsonUtil.parse(json, targetType);
					for (Map.Entry<String, String> e : map.entrySet()) {
						frm.add(e.getKey(), e.getValue());
					}
					OkHttpUtil.postAsync(this.uri + rd.getStr("uri"), map, null);
				}
			}
		} catch (IOException e) {
			log.error("CacheLoadTask->execute, failt to load caches exception " + e, e);
		}
	}
}
