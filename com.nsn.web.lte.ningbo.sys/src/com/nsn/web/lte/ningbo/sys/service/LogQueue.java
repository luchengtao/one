package com.nsn.web.lte.ningbo.sys.service;

import java.sql.Timestamp;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import com.nsn.logger.Logger;
import com.nsn.web.lte.Const;
import com.nsn.web.lte.beans.User;
import com.nsn.web.lte.db.DSType;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.Record;
import com.nsn.web.lte.utils.GsonUtil;
import com.nsn.web.lte.utils.Idw;
import com.nsn.web.lte.utils.ReqUtil;

public class LogQueue {
	private static Logger log = Logger.getLogger(LogQueue.class);
	private static BlockingQueue<Record> logs = new LinkedBlockingQueue<>();

	public static void put(Record rd) {
		try {
			logs.put(rd);
		} catch (Exception e) {
			log.error("LogQueue->put, failt to put log to queue!" + e, e);
		}
	}

	public static void log(HttpServletRequest request, Object result) {
		HttpSession ssn = request.getSession();
		User user = (User) ssn.getAttribute(Const.LOGIN_USER);
		String strParams = GsonUtil.toJson(ReqUtil.params(request));
		final Record rlogs = new Record();
		rlogs.set("id", Idw.id());
		if (Objects.nonNull(user)) {
			rlogs.set("user_id", user.getId());
			rlogs.set("user_name", user.getName());
		} else {
			rlogs.set("user_id", "0");
			rlogs.set("user_name", "anonamous");
		}
		//日志过大。有结果记录success，无结果记录empty
		String resultFlag = "null";
		if(result!=null){
			resultFlag = "success";
			if(result instanceof String){
				if(StringUtils.isEmpty((String)result)){
					resultFlag = "empty";
				}
			}
		}
		//rlogs.set("result", result);
		rlogs.set("result", resultFlag);
		rlogs.set("module", request.getRequestURI());
		rlogs.set("ip", ReqUtil.getRemoteAddr(request));
		rlogs.set("params", strParams);
		rlogs.set("create_time", new Timestamp(System.currentTimeMillis()));
		put(rlogs);
	}

	public static void start() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Record rd = logs.take();
						if (Objects.nonNull(rd)) {
							Db.use(DSType.H2LOG).save("sys_logs", rd);
						} else {
							Thread.sleep(3000);
						}
					} catch (InterruptedException e) {
						log.error("LogQueue->save, failt to save log to database!" + e, e);
					}
				}
			}
		}).start();
	}
}
