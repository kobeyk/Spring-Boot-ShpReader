package com.appleyk.entity;

/**
 * 以shp文件的内容，构建对应的Java实体类
 * @author yukun24@126.com
 * @blob   http://blog.csdn.net/appleyk
 * @date   2018年4月24日-上午9:11:27
 */
public class ShpGeometry {
	private int gid;
	private String osm_id;
	private String fclass;
	private int code;
	private String name;
	private String type;
	private Object geom;

	public ShpGeometry() {

	}

	public int getGid() {
		return gid;
	}

	public void setGid(int gid) {
		this.gid = gid;
	}

	public String getOsm_id() {
		return osm_id;
	}

	public void setOsm_id(String osm_id) {
		if (osm_id.equals("")) {
			this.osm_id = null;
		}else this.osm_id = osm_id;	
	}

	public String getFclass() {

		return fclass;
	}

	public void setFclass(String fclass) {
		if (fclass.equals("")) {
			this.fclass = null;
		}else this.fclass = fclass;
		
	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getName() {

		return name;
	}

	public void setName(String name) {
		
		if (name.equals("")) {
			this.name = null;			
		}else this.name = name;
		
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		if (type.equals("")) {
			this.type = null;
		}else this.type = type;		
	}

	public Object getGeom() {
		return geom;
	}

	public void setGeom(Object geom) {
		this.geom = geom;
	}
}
