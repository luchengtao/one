package com.nsn.web.lte.ningbo.sys.action;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.nsn.web.lte.Const;
import com.nsn.web.lte.db.DSType;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.Page;
import com.nsn.web.lte.db.Record;
import com.nsn.web.lte.mvc.ReqContext;

public class LogsAction {
	public String index(){
		return "logs.html";
	}
	
	/**
	 * 查询日志
	 * @param rc
	 * @return
	 * @throws IOException
	 */
	public Page<Record> getPage(ReqContext rc) throws IOException{
		StringBuffer sb = new StringBuffer("select user_id,user_name,module,ip,to_char(params) params,to_char(result) result,remark,create_time from sys_logs ");
		//过滤条件
		String where = rc.where(Const.SCH_PREFIX);
		if(StringUtils.isNoneBlank(where)){
			sb.append(" WHERE ").append(where);
		}
		Page<?> pageParam = rc.page();
		if(pageParam.isOrderBy()){			
			sb.append(" ORDER BY ").append(pageParam.orderBy());
		}
		Page<Record> page = Db.use(DSType.H2LOG).page(sb.toString(), pageParam);
		return page;
	}
	
}
