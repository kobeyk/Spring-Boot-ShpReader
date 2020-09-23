package com.appleyk.geotools;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureWriter;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Transaction;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.shapefile.files.ShpFiles;
import org.geotools.data.shapefile.shp.ShapefileReader;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.appleyk.entity.ShpGeometry;

public class GeoToolsUtils {

	static Connection connection = null;
	static DataStore pgDatastore = null;
	@SuppressWarnings("rawtypes")
	static FeatureSource fSource = null;
	static Statement statement = null;
	static GeometryCreator gCreator = GeometryCreator.getInstance();
	static GeometryFactory geometryFactory = new GeometryFactory();

	/**
	 * 1.连接postgrepsql数据库
	 * 
	 * @param ip
	 * @param port
	 * @param user
	 * @param password
	 * @param database
	 * @return
	 * @throws Exception
	 */
	private static boolean connDataBase(String ip, Integer port, String user, String password, String database)
			throws Exception {

		// "jdbc:postgresql://192.168.1.104:5432/test"
		// user=postgres
		// password=bluethink134

		// 拼接url
		String url = "jdbc:postgresql://" + ip + ":" + port + "/" + database;
		Class.forName("org.postgresql.Driver"); // 一定要注意和上面的MySQL语法不同
		connection = DriverManager.getConnection(url, user, password);
		if (connection != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 2.连接数据库 使用的postgis 链接代码如下：
	 * 
	 * @param dbtype
	 * @param host
	 * @param port
	 * @param database
	 * @param userName
	 * @param password
	 */
	private static void connPostGis(String dbtype, String host, int port, String database, String userName,
			String password) {

		Map<String, Object> params = new HashMap<String, Object>();

		params.put(PostgisNGDataStoreFactory.DBTYPE.key, dbtype);
		params.put(PostgisNGDataStoreFactory.HOST.key, host);
		params.put(PostgisNGDataStoreFactory.PORT.key, new Integer(port));
		params.put(PostgisNGDataStoreFactory.DATABASE.key, database);
		params.put(PostgisNGDataStoreFactory.SCHEMA.key, "public");
		params.put(PostgisNGDataStoreFactory.USER.key, userName);
		params.put(PostgisNGDataStoreFactory.PASSWD.key, password);
		try {
			pgDatastore = DataStoreFinder.getDataStore(params);
			if (pgDatastore != null) {
				System.out.println("系统连接到位于：" + host + "的空间数据库" + database + "成功！");
			} else {
				System.out.println("系统连接到位于：" + host + "的空间数据库" + database + "失败！请检查相关参数");
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("系统连接到位于：" + host + "的空间数据库" + database + "失败！请检查相关参数");
		}

	}

	/**
	 * 利用GeoTools工具包，打开一张shapfile文件，并显示
	 * 
	 * @throws Exception
	 */
	public static void openShpFile() throws Exception {

		// 1.数据源选择 shp扩展类型的
		File file = JFileDataStoreChooser.showOpenFile("shp", null);
		if (file == null) {
			return;
		}

		// 2.得到打开的文件的数据源
		FileDataStore store = FileDataStoreFinder.getDataStore(file);

		// 3.设置数据源的编码，防止中文乱码
		((ShapefileDataStore) store).setCharset(Charset.forName("UTF-8"));

		/**
		 * 使用FeatureSource管理要素数据 使用Style（SLD）管理样式 使用Layer管理显示
		 * 使用MapContent管理所有地图相关信息
		 */

		// 4.以java对象的方式访问地理信息
		// 简单地理要素
		SimpleFeatureSource featureSource = store.getFeatureSource();

		// 5.创建映射内容，并将我们的shapfile添加进去
		MapContent mapContent = new MapContent();

		// 6.设置容器的标题
		mapContent.setTitle("Appleyk's GeoTools");

		// 7.创建简单样式
		Style style = SLD.createSimpleStyle(featureSource.getSchema());

		// 8.显示【shapfile地理信息+样式】
		Layer layer = new FeatureLayer(featureSource, style);

		// 9.将显示添加进map容器
		mapContent.addLayer(layer);

		// 10.窗体打开，高大尚的操作开始
		JMapFrame.showMap(mapContent);

	}

	public static void readSHP(String path) throws Exception {
	
		// 一个数据存储实现，允许从Shapefiles读取和写入
		ShapefileDataStore shpDataStore = null;
		shpDataStore = new ShapefileDataStore(new File(path).toURI().toURL());
		shpDataStore.setCharset(Charset.forName("UTF-8"));
		
		// 获取这个数据存储保存的类型名称数组
		// getTypeNames:获取所有地理图层
		String typeName = shpDataStore.getTypeNames()[0];
		
		// 通过此接口可以引用单个shapefile、数据库表等。与数据存储进行比较和约束
		FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = null;
		featureSource = (FeatureSource<SimpleFeatureType, SimpleFeature>) shpDataStore.getFeatureSource(typeName);
		
		// 一个用于处理FeatureCollection的实用工具类。提供一个获取FeatureCollection实例的机制
		FeatureCollection<SimpleFeatureType, SimpleFeature> result = featureSource.getFeatures();

		FeatureIterator<SimpleFeature> iterator = result.features();
		
		// 迭代 特征 只迭代100个 太大了，一下子迭代完，非常耗时
		int stop = 0;
		List<ShpGeometry> geolist = new ArrayList<ShpGeometry>();
		while (iterator.hasNext()) {

			if (stop > 100) {
				break;
			}

			SimpleFeature feature = iterator.next();
			Collection<Property> p = feature.getProperties();
			Iterator<Property> it = p.iterator();
			// 构建实体
			ShpGeometry geo = new ShpGeometry();
			
			// 特征里面的属性再迭代,属性里面有字段
			String name;
			while (it.hasNext()) {
				
				Property pro = it.next();
				name = pro.getName().toString();

				/**
				 * 根据shp文件里面的属性值进行过滤
				 */
				if (name.equals("the_geom")) {
					geo.setGeom(pro.getValue());
				}

				if (name.equals("osm_id")) {
					geo.setOsm_id(pro.getValue().toString());
				}

				if (name.equals("code")) {
					geo.setCode(Integer.parseInt(pro.getValue().toString()));
				}

				if (name.equals("fclass")) {
					geo.setFclass(pro.getValue().toString());
				}

				if (name.equals("name")) {
					geo.setName(pro.getValue().toString());
				}

				if (name.equals("type")) {
					geo.setType(pro.getValue().toString());
				}

			} // end 里层while
			
			geolist.add(geo);
			stop++;
			
		} // end 最外层 while

		iterator.close();
		boolean bRes = true;
		for (ShpGeometry geo : geolist) {
			/**
			 * 存储对象geo，这里是循环插入，也可以做成mybatis的批量insert
			 */
			if (!shpSave(geo)) {
				bRes = false;
				break;
			}
		}

		if (bRes) {
			System.out.println("读取shapefile文件内容并插入数据库成功！");
		}
	}

	/**
	 * 存储shp文件的数据 == 文件内容映射成Java实体类存储在postgresql数据库中
	 * 
	 * @param geo
	 * @return
	 * @throws Exception
	 */
	public static boolean shpSave(ShpGeometry geo) throws Exception {

		boolean result = false;
		String sql = "insert into geotable (osm_id,code,fclass,name,type,geom) values('" + geo.getOsm_id() + "','"
				+ geo.getCode() + "','" + geo.getFclass() + "','" + geo.getName() + "','" + geo.getType() + "',"
				+ "st_geomfromewkt('" + geo.getGeom().toString() + "'))";

		PreparedStatement pstmt;
		pstmt = connection.prepareStatement(sql);

		// geometry = st_geomfromewkt(text WKT) ==
		// 对应postgresql中的几何WKT文本描述转换为几何数据

		System.out.println(sql);
		int i = pstmt.executeUpdate();
		if (i > 0) {
			result = true;
		}

		pstmt.close();
		return result;
	}

	/**
	 * 获取POSTGIS中所有的地理图层
	 * 
	 * @throws Exception
	 */
	public static void getAllLayers() throws Exception {
		String[] typeName = pgDatastore.getTypeNames();
		for (int i = 0; i < typeName.length; i++) {
			System.out.println((i + 1) + ":" + typeName[i]);
		}
	}

	/**
	 * 针对某个地理图层[相当于table表名字]，进行地理信息的读取
	 * 
	 * @param Schema
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void postGisReading(String Schema) throws Exception {

		fSource = pgDatastore.getFeatureSource(Schema);
		// 1.一个用于处理FeatureCollection的实用工具类。提供一个获取FeatureCollection实例的机制
		FeatureCollection<SimpleFeatureType, SimpleFeature> result = fSource.getFeatures();

		// 2.计算本图层中所有特征的数量
		System.out.println("特征totalCount = " + result.size());

		// 3.迭代特征
		FeatureIterator<SimpleFeature> iterator = result.features();

		// 4.迭代特征 只迭代30个 太大了，一下子迭代完，非常耗时
		int stop = 0;
		while (iterator.hasNext()) {

			if (stop > 30) {
				break;
			}

			SimpleFeature feature = iterator.next();
			Collection<Property> p = feature.getProperties();
			Iterator<Property> it = p.iterator();

			// 5.特征里面的属性再迭代,属性里面有字段
			System.out.println("================================");
			while (it.hasNext()) {
				Property pro = it.next();
				System.out.println(pro.getName() + "\t = " + pro.getValue());
			} // end 里层while
			stop++;
		} // end 最外层 while
		iterator.close();
	}

	/**
	 * 根据几何对象名称 查询几何对象信息 [Query]
	 * @param name
	 * @throws Exception
	 */
	public static void Query(String name) throws Exception{
		
		//String sql = "select st_astext(geom) from geotable where name ='"+name+"'";
		String sql = "select  geometrytype(geom) as type,st_astext(geom) as geom from geotable where name ='"+name+"'";
		statement = connection.createStatement();
		ResultSet result = statement.executeQuery(sql);
		if(result!=null){
			while(result.next()){
				Object val = result.getString(1);
				if(val.equals("MULTIPOLYGON")){
					System.out.println("几何对象类型：多多边形");					
					MultiPolygon mPolygon = gCreator.createMulPolygonByWKT(result.getString(2));
					System.out.println(mPolygon instanceof MultiPolygon);
					System.out.println("获取几何对象中的点个数："+mPolygon.getNumPoints());
				}
				
			}
		}			
	}

	/**
	 * 将几何对象信息写入一个shapfile文件并读取
	 * @throws Exception
	 */
	public static void writeSHP(String path, Geometry geometry) throws Exception{
		
		//String path="C:\\my.shp";
		
		// 1.创建shape文件对象
		File file =new File(path);
			
		Map<String, Serializable> params = new HashMap<String, Serializable>();
		
		// 2.用于捕获参数需求的数据类
		//URLP:url to the .shp file. 
		params.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
		
		// 3.创建一个新的数据存储——对于一个还不存在的文件。
		ShapefileDataStore ds = (ShapefileDataStore) new ShapefileDataStoreFactory().createNewDataStore(params);
		
			
		// 4.定义图形信息和属性信息
		//SimpleFeatureTypeBuilder 构造简单特性类型的构造器
		SimpleFeatureTypeBuilder tBuilder = new SimpleFeatureTypeBuilder();
		
		// 5.设置
		//WGS84:一个二维地理坐标参考系统，使用WGS84数据
		tBuilder.setCRS(DefaultGeographicCRS.WGS84);
		tBuilder.setName("shapefile");
		
		// 6.添加 一个多多边形  ==  这里也可以当做参数传过来，具体情况具体对待
		tBuilder.add("the_geom", MultiPolygon.class);
		// 7.添加一个id
		tBuilder.add("osm_id", Long.class);
		// 8.添加名称
		tBuilder.add("name", String.class);
			
		// 9.添加描述
		tBuilder.add("des", String.class);
		
		
		// 10.设置此数据存储的特征类型
		ds.createSchema(tBuilder.buildFeatureType());
		
		// 11.设置编码
		ds.setCharset(Charset.forName("UTF-8"));
		
		
		
		// 12.设置writer
		//为给定的类型名称创建一个特性写入器		
		FeatureWriter<SimpleFeatureType, SimpleFeature> writer = ds.getFeatureWriter(
				ds.getTypeNames()[0], Transaction.AUTO_COMMIT);
		
		
		
		//Interface SimpleFeature：一个由固定列表值以已知顺序组成的SimpleFeatureType实例。
		// 13.写一个点
		SimpleFeature feature = writer.next();				
		feature.setAttribute("the_geom", geometry);			
		feature.setAttribute("osm_id", 1234567890l);
		feature.setAttribute("name", "多多边形");
		feature.setAttribute("des", "国家大剧院");
	
		// 14.写入
		writer.write();
		
		// 15.关闭
		writer.close();
		
		// 16.释放资源
		ds.dispose();
		
		
		// 17.读取shapefile文件的图形信息
		ShpFiles shpFiles = new ShpFiles(path);
		/*ShapefileReader(
		 ShpFiles shapefileFiles,
		 boolean strict, --是否是严格的、精确的
		 boolean useMemoryMapped,--是否使用内存映射
		 GeometryFactory gf,     --几何图形工厂
		 boolean onlyRandomAccess--是否只随机存取
		 )
		*/
		ShapefileReader reader = new ShapefileReader(shpFiles,
				false, true, new GeometryFactory(), false);
		while(reader.hasNext()){
			System.out.println(reader.nextRecord().shape());
		}		
		reader.close();	
	}
	
	public static void main(String[] args) throws Exception {

		// 1.利用Provider连接 空间数据库
		if (!connDataBase("192.168.1.104", 5432, "postgres", "bluethink", "test")) {
			System.out.println("连接postgresql数据库失败，请检查参数！");
		}

		System.out.println("===============连接postgis空间数据库==============");
		connPostGis("postgis", "192.168.1.104", 5432, "test", "postgres", "bluethink");

		System.out.println("===============读取shp文件并存储至postgresql数据库==============");	
		// A.建筑物的shapefile，多边形 MULTIPOLYGON
		// String path = "E:\\china-latest-free\\gis.osm_buildings_a_free_1.shp";

		// B.路的shapefile，多线MULTILINESTRING
		// String path = "E:\\china-latest-free\\gis.osm_roads_free_1.shp";

		// C.建筑物的点坐标 以Point为主
		// String path = "E:\\china-latest-free\\gis.osm_pois_free_1.shp";
		
		String path = "E:\\china-latest-free\\gis.osm_buildings_a_free_1.shp";
		readSHP(path);
		
		System.out.println("===============读取图层geotable==============");
		postGisReading("geotable");
		
		System.out.println("===============获取所有图层【所有空间几何信息表】==============");
		getAllLayers();		
		
		System.out.println("===============创建自己的shp文件==============");
		String MPolygonWKT="MULTIPOLYGON(((116.3824004 39.9032955,116.3824261 39.9034733,116.382512 39.9036313,116.382718 39.9038025,116.3831643 39.903954,116.383602 39.9040198,116.3840827 39.9040001,116.3844003 39.9039211,116.3846921 39.903763,116.3848552 39.9035787,116.3848981 39.9033548,116.3848037 39.9031244,116.3845719 39.9029071,116.3842286 39.9027754,116.3837823 39.9027227,116.3833789 39.9027095,116.383027 39.902749,116.3828038 39.9028346,116.382615 39.90294,116.3824776 39.9030717,116.3824004 39.9032955)))";
		MultiPolygon multiPolygon = gCreator.createMulPolygonByWKT(MPolygonWKT);
	    System.out.println(multiPolygon.getGeometryType());		
		//首先得创建my这个目录
	    writeSHP("C:/my/multipol.shp",multiPolygon);	
	    System.out.println("===============打开shp文件==============");	
		GeoToolsUtils.openShpFile();
	}
}
