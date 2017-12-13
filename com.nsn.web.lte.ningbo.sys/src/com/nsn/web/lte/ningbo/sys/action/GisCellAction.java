package com.nsn.web.lte.ningbo.sys.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import com.nsn.gis.geojson.Geom;
import com.nsn.gis.geojson.JtsUtils;
import com.nsn.gis.geojson.ProjUtils;
import com.nsn.web.lte.db.Db;
import com.nsn.web.lte.db.Page;
import com.nsn.web.lte.db.Record;
import com.nsn.web.lte.db.SqlMap;
import com.nsn.web.lte.mvc.ReqContext;
import com.nsn.web.lte.utils.CsvUtil;
import com.nsn.web.lte.utils.ExcelUtil;
import com.nsn.web.lte.utils.Ret;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GisCellAction {
	
	public String index(){
		return "gisCell.html";
	}
	
	public Page<Record> getPage(ReqContext rc){
		String where = rc.where();
		Map<String, Object> p = new HashMap<>();
		String sql = "";
		if(StringUtils.isNotBlank(where)){
			sql += " where "+where;
		}
		Page<?> pageParam = rc.page();
		if(pageParam.isOrderBy()){			
			sql += " order by " + pageParam.orderBy();
		}
		
		p.put("where", sql);
		Page<Record> page = Db.page(SqlMap.getSql("sys_gis_cell_query", p), pageParam); 
		return  page;
	}
	
	public Record getCell(ReqContext rc){
		long cid = rc.paramToLong("id");
		Record record = Db.read(SqlMap.getSql("sys_gis_cell_getCell"), cid);
		return record;
	}
	
	public Ret saveCell(ReqContext rc){
		Record rd = rc.form();
		boolean isNew = rd.getBoolean("isNew");
		rd.remove("isNew");
		rd.remove("prj_type");
		long ci = Long.valueOf(rd.getStr("cell_id")==null?"0":rd.getStr("cell_id").replace("-",""));
		double lon = rd.getDouble("longitude");
		double lat = rd.getDouble("latitude");
		double azimuth = rd.getDouble("azimuth");
		double radius = rd.getDouble("radius");
		double beamwidth = rd.getDouble("beamwidth");
		String prjType = StringUtils.isNotBlank(rd.getStr("prj_type"))?rd.getStr("prj_type"):"WGS84";
		Point curPt = Geom.point(lon, lat);
        //对经纬度数据进行02加密处理
        Point pt = convertPoint(curPt, prjType);
        Polygon polygon = JtsUtils.cellSector(pt.getX(), pt.getY(), radius, azimuth, beamwidth, 20);
        String strGeom = JtsUtils.geometry2Wkt(polygon);
		rd.set("cell_id", ci);
		rd.set("longitude",lon);
		rd.set("latitude", lat);
		rd.set("azimuth", azimuth);
		rd.set("radius", radius);
		rd.set("beamwidth", beamwidth);
		rd.set("site_id", rd.getLong("site_id"));
		rd.set("tac", rd.getInt("tac"));
		rd.set("network", rd.getInt("network"));
		rd.set("c_lng", pt.getX());
		rd.set("c_lat", pt.getY());
		rd.set("cell_wkt", strGeom);
		//rd.set("geom", "public.ST_GeometryFromText('"+strGeom+"', 4326)");
		if(isNew){
			Db.save(SqlMap.getSql("sys_gis_cell_saveCell"), rd);
		}else{
			Db.update(SqlMap.getSql("sys_gis_cell_saveCell"),"cell_id", rd);
		}
		return Ret.ok().set("msg","基站扇区保存成功！");
	}
	
	public Ret impCell(ReqContext rc){
		File file = rc.file();
		List<Record> list = null;
		String ext = FilenameUtils.getExtension(file.getName());
		if ("xls".equalsIgnoreCase(ext) || "xlsx".equalsIgnoreCase(ext)) {
			list = ExcelUtil.readExcel(file);
		}else if("csv".equalsIgnoreCase(ext)){
			list = CsvUtil.readCsv(file);
		}
		String msg = "";
		if(Objects.nonNull(list) && list.size()>0){
			double rate = rc.paramToDouble("rate");
			Map<Point, List<Record>> dataMap = new HashMap<>();
			for(Record rd : list){
				Point sitePt = Geom.point(rd.getDouble("longitude"), rd.getDouble("laltitude"));
				List<Record> dtList = dataMap.get(sitePt);
				if(Objects.isNull(dtList)){
					dtList = new ArrayList<>();
					dataMap.put(sitePt, dtList);
				}
				dtList.add(rd);
			}
			List<Record> cellList = new ArrayList<>();
			List<String> _list = new ArrayList<>(list.size());
			for(Map.Entry<Point, List<Record>> ent : dataMap.entrySet()){
				List<Record> cList = ent.getValue();
				int size = cList.size();
				double da = Math.floor(360.0/size);//按照小区个数，评分360度
				//Point offsetPt;
				for(Record rd : cList){
					long ci = Long.valueOf(rd.getStr("cell_id")==null?"0":rd.getStr("cell_id").replace("-",""));
					double lon = rd.getDouble("longitude");
					double lat = rd.getDouble("latitude");
					double azimuth = rd.getDouble("azimuth");
					double radius = rd.getDouble("radius");
					double beamwidth = rd.getDouble("beamwidth");
					String prjType = StringUtils.isNotBlank(rd.getStr("prj_type"))?rd.getStr("prj_type"):"WGS84";
					if(ci == 0){
						msg += "小区："+ci+"为空，无法导入！";
						continue;
					}
					if(ProjUtils.outOfChina(lon, lat)){
						msg+="小区："+ci+"位置不在中国境内，无法导入！";
						continue;
					}
					Point curPt = Geom.point(lon, lat);
					//判断是否检查过攻占，如果检查过则不再进行检查
					if(Double.compare(beamwidth, 360) != 0){
						//如果小区平分的角度小于参数中的半功率角，则使用平分角度，否则会出现重叠
						if(Double.compare(beamwidth,da)>0){
							beamwidth = da;
						}
						int nShareCount = 0;
						for(Record r : cellList){
							Point lastPt = r.get("point");
							double lastAzimuth = r.getDouble("azimuth");
							double lastRadius = r.getDouble("radius");
							//如果坐标点、方位角、半径都相等的话，则进行相应的移动
							if(curPt.equals(lastPt) && Double.compare(lastAzimuth, azimuth) == 0
									&& Double.compare(lastRadius, radius)==0){
								nShareCount++;
							}
						}
						if(nShareCount>0){
							//缩短半径，利用半径区别攻占扇区
							radius = radius * (1-rate*nShareCount);
							beamwidth = beamwidth * (1-rate*nShareCount);
						}
						Record lastRd = new Record();
						lastRd.set("azimuth", azimuth);
						lastRd.set("radius", radius);
						lastRd.set("point", curPt);
						cellList.add(lastRd);
					}
					//对经纬度进行02加密处理
					Point pt = convertPoint(curPt, prjType);
					Polygon polygon = JtsUtils.cellSector(pt.getX(), pt.getY(), radius, azimuth, beamwidth, 20);
					String strGeom = JtsUtils.geometry2Wkt(polygon);
					_list.add(buildSql(rd, strGeom, pt));
					if(_list.size() >2000) {
						Db.batch(_list);
						_list.clear();
					}
				}
			}
			Db.batch(_list);
			return Ret.ok().set("msg",msg);
		}else{
			return Ret.fail().set("msg","数据导入失败，上传文件无数据或者无法读取！");
		}
	}
	
	public Ret delCell(ReqContext rc){
		long cid = rc.paramToLong("id");
		String sql =SqlMap.getSql("sys_gis_cell_delete");
		Db.update(sql, cid);
		return Ret.ok().set("msg", "基站扇区删除成功！");
	}
	
	public Ret clearCell(ReqContext rc){
		String sql =SqlMap.getSql("sys_gis_cell_clear");
		Db.update(sql);
		return Ret.ok().set("msg", "基站扇区清空成功！");
	}
	
	/**
     * 根据设定的坐标系，统一转化为GCJ02坐标进行计算
     * @param spt
     * @param prjType
     * @return
     */
    private Point convertPoint(Point spt, String prjType) {
        if ("WGS84".equals(prjType)) {
            return ProjUtils.wgs2gcj(spt);
        } else if ("CGJ02".equals(prjType)) {
            return spt;
        } else if ("BD09".equals(prjType)) {
            return ProjUtils.bd2gcj(spt);
        } else {
            return spt;
        }
    }
    
    /**
     * 根据参数生成对应的value插入字符串
     */
    private String buildSql(Record rd, String strGeom, Point pt) {
    	StringBuilder sbInsert = new StringBuilder(SqlMap.getSql("sys_gis_cell_insert"));
        sbInsert.append("'").append(rd.getStr("city","")).append("',");
        sbInsert.append("'").append(rd.getStr("area","")).append("',");
        sbInsert.append(rd.getLong("site_id")).append(",");
        sbInsert.append("'").append(rd.getStr("site_name","")).append("',");
        sbInsert.append(rd.getStr("cell_id")==null?"0":rd.getStr("cell_id").replace("-","")).append(",");
        sbInsert.append("'").append(rd.getStr("cell_name","")).append("',");
        sbInsert.append("'").append(rd.getStr("cover","")).append("',");
        sbInsert.append("'").append(rd.getStr("site_type","")).append("',");
        sbInsert.append("'").append(rd.getStr("site_band","")).append("',");
        sbInsert.append(rd.getDouble("longitude")).append(",");
        sbInsert.append(rd.getDouble("latitude")).append(",");
        sbInsert.append(rd.getDouble("azimuth")).append(",");
        sbInsert.append(rd.getDouble("radius")).append(",");
        sbInsert.append(rd.getDouble("beamwidth")).append(",");
        sbInsert.append(rd.getInt("tac")).append(",");
        sbInsert.append("'").append(strGeom).append("',");
        sbInsert.append(pt.getX()).append(",");
        sbInsert.append(pt.getY()).append(",");
        sbInsert.append(rd.getInt("network")).append(")");
        //sbInsert.append("public.ST_GeometryFromText('"+strGeom+"', 4326))");
        return sbInsert.toString();
    }
}
