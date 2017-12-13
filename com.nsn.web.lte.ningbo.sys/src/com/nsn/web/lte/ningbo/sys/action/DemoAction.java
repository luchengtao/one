package com.nsn.web.lte.ningbo.sys.action;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.OutParameter;

import com.nsn.web.lte.aop.Cache;
import com.nsn.web.lte.aop.Enhancer;
import com.nsn.web.lte.db.DSType;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.Record;
import com.nsn.web.lte.db.SqlItem;
import com.nsn.web.lte.db.SqlMap;
import com.nsn.web.lte.mvc.ReqContext;
import com.nsn.web.lte.ningbo.sys.service.TestService;
import com.nsn.web.lte.utils.GsonUtil;
import com.nsn.web.lte.utils.Idw;

public class DemoAction {
	private TestService service = Enhancer.enhance(TestService.class);
	
	public String index(){
		return "demo.html";
	}
	
	public String gis(ReqContext rc){
		return "gisDemo.html";
	}
	
	public void ftlSql(ReqContext rc){
		String sqlKey = rc.param("sqlId");
		Map<String, Object> pramas = rc.params();
		if (SqlMap.containsKey(sqlKey)) {
			SqlItem item = SqlMap.get(sqlKey);
			String sql = item.parse(pramas);
			List<Record> list = Db.use(item.getDs()).query(sql);
			System.out.println(list);
		}
	}
	
	public void test(ReqContext rc,String name,int age,String address){
		System.out.println(name);
		System.out.println(age);
		System.out.println(address);
		
	}
	

	@Cache(region = "1d", key = "sss")
	public List<Record> ora(){
		List<Record> list = Db.use(DSType.DO_LTE).query("select * from cfg_atu_user");
		return list;
	}
	
	public void proc(){
		OutParameter<String> stringParam = new OutParameter<String>(Types.VARCHAR, String.class);
		Db.use(DSType.DO_LTE).procList("call sp_tmp1(?,?)","dddddd",stringParam);
		System.out.println(stringParam.getValue());
		List<Record> list = Db.use(DSType.DO_LTE).procList("call sp_tmp2(?,?)","cfg_atu_user");
		System.out.println(GsonUtil.toJson(list));
	}
	
	public void procGp(){
		 String sql = "{?=call sp_user()}";// 调用的sql 
		 List<Record> list = Db.procList(sql);
		 System.out.println(list);
	}
	//Db.use()
	public void procOra(){
		 List<Record> list = Db.use(DSType.VO_LTE).procList("call sp_tmp2(?,?)","cfg_atu_user");
		 System.out.println(list);
	}
	
	public List<Record> set(){
		List<Record> list = Db.use().queryCache("5min","select * from sys_user_do"); 
		System.out.println(list); 
		long count = Db.stat("select count(*) from sys_user_role_do"); 
		System.out.println(count);
		return list;
	}
	
	public List<Record> get(){
		List<Record> list = Db.use().queryCache("5min","select * from sys_user_do"); 
		System.out.println(list); 
		long count = Db.stat("select count(*) from sys_user_role_do"); 
		System.out.println(count);
		return list;
	}
	
//	public List<Record> get(){
//		List<Record> list = Db.queryCache("5min","test_cache","select * from sqmdb_rpt.sys_user"); 
//		System.out.println(list); 
//		long count = Db.stat("select count(*) from sqmdb_rpt.sys_user_role"); 
//		System.out.println(count);
//		return list;
//	}
	
	public String testCache(ReqContext rc) {
		String str = service.test("jack");
		return str;
	}
	
	public void redirect(ReqContext rc){
		try {
			rc.redirect("/sys/user");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void add() throws SQLException{
		System.out.println(System.currentTimeMillis());
		Record rd = new Record();
		rd.set("id", 1);
		rd.set("user_id", Idw.id());
		rd.set("role_id", Idw.id());
		rd.set("create_time", new Timestamp(System.currentTimeMillis()));
		rd.set("create_by", Idw.id());
		long aa = Db.save("sys_user_role_do", rd);
		System.out.println(aa);
		/*String sql = "insert into sys_user_role(id, user_id, role_id, create_time, create_by) values(?, 2, 3, '2017-02-20 10:40:00', 4)";
		Connection conn = Db.use().getConnection();
		PreparedStatement stm = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		stm.setObject(1, 1);
		int b = stm.executeUpdate();
		long l = Db.insert(sql);
		System.out.println(b);*/
	}
	
	public void update() throws SQLException{
		/*System.out.println(System.currentTimeMillis());
		Record rd = new Record();
		rd.set("id", 1);
		rd.set("user_id", 2);
		rd.set("role_id", 3);
		rd.set("create_time", LocalDateTime.now());
		rd.set("create_by", 4);
		long aa = Db.update("sys_user_role","id", rd);
		System.out.println(aa);*/
		String sql = "insert into sys_user_role_do (id, user_id, role_id, create_time, create_by) values(1, 2, 3, '2017-02-20 10:40:00', 4)";
		Connection conn = Db.use().getConn();
		Statement stm = conn.createStatement();
		boolean b = stm.execute(sql);
		System.out.println(b);
	}
	
	public void webModule(){
		/*List<WebModule> wmList = DoSystem.getWebSystem().listWebModule(); // 查询所有插件
		System.out.println(wmList);
		if(null != wmList && !wmList.isEmpty()){
			for(WebModule wm : wmList){
				SystemMenuPath menuPath = wm.getSystemMenu(Locale.CHINESE);
				System.out.println(menuPath);
			}
		}*/
	}
}
