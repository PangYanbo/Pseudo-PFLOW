package pseudo.acs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import jp.ac.ut.csis.pflow.geom2.LonLat;
import jp.ac.ut.csis.pflow.geom2.Mesh;
import jp.ac.ut.csis.pflow.geom2.MeshUtils;
import jp.ac.ut.csis.pflow.routing4.res.Network;
import jp.ac.ut.csis.pflow.routing4.res.Node;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;


import pseudo.res.City;
import pseudo.res.ECity;
import pseudo.res.ELabor;
import pseudo.res.EPTCity;
import pseudo.res.Facility;
import pseudo.res.Country;
import pseudo.res.GMesh;

public class DataAccessor {
	
	public static Network loadLocationData(String filename){
		Network res = new Network();
		try (BufferedReader br = new BufferedReader(new FileReader(filename))){
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
            	String[] items = line.split(",");
            	String id = items[0];
            	double x = Double.parseDouble(items[1]);
            	double y = Double.parseDouble(items[2]);
                res.addNode(new Node(id, x, y));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load location data: " + filename, e);
        }
		return res;
	}

	public static int loadPreSchoolData(String filename, Country japan){
		try (BufferedReader br = new BufferedReader(new FileReader(filename))){
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
            	String[] items = line.split(",");
            	String gcode = items[0];
            	int id = Integer.parseInt(items[1]);
            	double x = Double.parseDouble(items[2]);
            	double y = Double.parseDouble(items[3]);
            	Facility school = new Facility(id, x, y, gcode, 300);
                City city = japan.getCity(gcode);
                if (city != null) {
                	city.addSchools(ELabor.PRE_SCHOOL, school);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load preschool data: " + filename, e);
        }
		return 1;
	}

	public static int loadSchoolData(String filename, Country japan){
		try (BufferedReader br = new BufferedReader(new FileReader(filename))){
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
            	String[] items = line.split(",");
            	int id = Integer.parseInt(items[0]);
            	String gcode = items[1];
            	String type = items[2];
            	double x = Double.parseDouble(items[3]);
            	double y = Double.parseDouble(items[4]);
            	Facility school = new Facility(id, x, y, gcode, 0);
                City city = japan.getCity(gcode);
                if (city != null) {
	                switch (type) {
	                case("16001"):city.addSchools(ELabor.PRIMARY_SCHOOL, school);break;
	                case("16002"):city.addSchools(ELabor.SECONDARY_SCHOOL, school);break;
	                case("16003"):city.addSchools(ELabor.SECONDARY_SCHOOL, school);break;
	                case("16004"):city.addSchools(ELabor.HIGH_SCHOOL, school);break;
	                case("16005"):city.addSchools(ELabor.HIGH_SCHOOL, school);break;
	                case("16006"):city.addSchools(ELabor.JUNIOR_COLLEGE, school);break;
	                case("16007"):city.addSchools(ELabor.COLLEGE, school);break;
	                default:continue;
	                }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load school data: " + filename, e);
        }
		return 1;
	}

	public static int loadCityData(String filename, Country japan){
		try (BufferedReader br = new BufferedReader(new FileReader(filename));){
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
            	String[] items = line.split(",");
            	String gcode = items[0];
            	boolean daitoshi = Boolean.parseBoolean(items[1]);
            	EPTCity ptcity = daitoshi ? EPTCity.BIG3 : EPTCity.NO_BIG3;
        
            	double city_pop = Double.parseDouble(items[2]);
            	ECity type = ECity.UNDER10;
            	if (city_pop >= 500000) {
            		type = ECity.UPPER50;
            	}else if (city_pop >= 100000) {
            		type = ECity.UPPER10; // UPPER50->UPPER10
            	}
            	double area = Double.parseDouble(items[3]);
            	double pop_ratio = Double.parseDouble(items[4]);
            	double office_ratio = Double.parseDouble(items[5]);
            	double lon = Double.parseDouble(items[6]);
            	double lat = Double.parseDouble(items[7]);
            	new City(japan, gcode, ptcity, 
            			type, area, pop_ratio, office_ratio,
            			new LonLat(lon, lat));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load city data: " + filename, e);
        }
		return 1;
	}

	public static int loadHospitalData(String filename, Country japan) {
		try (BufferedReader br = new BufferedReader(new FileReader(filename));){
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
            	String[] items = line.split(",");
            	String gcode = items[0];
            	double lon = Double.parseDouble(items[1]);
            	double lat = Double.parseDouble(items[2]);
            	int type = Integer.parseInt(items[3]);
            	
            	Mesh mesh = MeshUtils.createMesh(3, lon, lat);
            	String mcode = mesh.getCode();
          
            	City city = japan.getCity(gcode);
            	if (city != null) {
            		GMesh gmesh = japan.hasMesh(mcode) ? japan.getMesh(mcode) : new GMesh(mesh);
            		double capacity = type != 1 ? 30 : 200;
            		Facility fac = new Facility(0, lon, lat, gcode, capacity);
            		gmesh.addHospital(fac);
            		city.addMesh(gmesh);
            	}
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load hospital data: " + filename, e);
        }
		return 1;
	}

	public static void loadRestaurantData(String filename, Country japan) throws FactoryException {
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4301"); // Replace <sourceCRSCode> with your original CRS code
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);

		GeometryFactory geometryFactory = (GeometryFactory) JTSFactoryFinder.getGeometryFactory();

		// restaurant data from TelePointDB 2018
		try(BufferedReader br = new BufferedReader((new FileReader(filename)));){
			String line;
			br.readLine();
			while((line = br.readLine()) != null){
				String[] items = line.split(",");
				if (items.length < 22) {
					System.err.println("DataAccessor.loadRestaurantData: skipping short row (" + items.length + " cols)");
					continue;
				}
				String gcode = items[7];  // admin code (Telepoint uses 4-digit for prefs 01-09)
				if (gcode.length() == 4 && Character.isDigit(gcode.charAt(0))) {
					gcode = "0" + gcode;
				}
				double lon = Double.parseDouble(items[20]);
				double lat = Double.parseDouble(items[21]);

				Coordinate coord = new Coordinate(lat, lon);
				Point point = geometryFactory.createPoint(coord);

				// Transform point
				Point transformedPoint = (Point) JTS.transform(point, transform);

				// Extract transformed coordinates
				double transformedLon = transformedPoint.getCoordinate().y;
				double transformedLat = transformedPoint.getCoordinate().x;

				Mesh mesh = MeshUtils.createMesh(3, transformedLon, transformedLat);
				String mcode = mesh.getCode();

				City city = japan.getCity(gcode);
				if (city != null) {
					GMesh gmesh = japan.hasMesh(mcode) ? japan.getMesh(mcode) : new GMesh(mesh);
					double capacity = 10000;
					Facility fac = new Facility(0, transformedLon, transformedLat, gcode, capacity);
					gmesh.addRestaurant(fac);
					city.addMesh(gmesh);
				}
			};
		} catch (Exception e) {
			throw new RuntimeException("Failed to load restaurant data: " + filename, e);
		}
	};

	public static void loadRetailData(String filename, Country japan) throws FactoryException {
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4301"); // Replace <sourceCRSCode> with your original CRS code
		CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
		MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);

		GeometryFactory geometryFactory = (GeometryFactory) JTSFactoryFinder.getGeometryFactory();
		// restaurant data from TelePointDB 2018
		try(BufferedReader br = new BufferedReader((new FileReader(filename)));){
			String line;
			br.readLine();
			while((line = br.readLine()) != null){
				String[] items = line.split(",");
				if (items.length < 22) {
					System.err.println("DataAccessor.loadRetailData: skipping short row (" + items.length + " cols)");
					continue;
				}
				String gcode = items[7];  // admin code (Telepoint uses 4-digit for prefs 01-09)
				if (gcode.length() == 4 && Character.isDigit(gcode.charAt(0))) {
					gcode = "0" + gcode;
				}
				double lon = Double.parseDouble(items[20]);
				double lat = Double.parseDouble(items[21]);

				Point point = geometryFactory.createPoint(new Coordinate(lat, lon));
				// Transform point to the target CRS
				Point transformedPoint = (Point) JTS.transform(point, transform);

				// Extract transformed coordinates
				double transformedLon = transformedPoint.getCoordinate().y;
				double transformedLat = transformedPoint.getCoordinate().x;

				Mesh mesh = MeshUtils.createMesh(3, transformedLon, transformedLat);
				String mcode = mesh.getCode();

				City city = japan.getCity(gcode);
				if (city != null) {
					GMesh gmesh = japan.hasMesh(mcode) ? japan.getMesh(mcode) : new GMesh(mesh);
					double capacity = 10000;
					Facility fac = new Facility(0, transformedLon, transformedLat, gcode, capacity);
					gmesh.addRetail(fac);
					city.addMesh(gmesh);
				}
			};
		} catch (Exception e) {
			throw new RuntimeException("Failed to load retail data: " + filename, e);
		}
	};

	public static int loadZenrinTatemono(String filename, Country japan, int scale){
		try (BufferedReader br = new BufferedReader(new FileReader(filename));){
            String line;
            br.readLine();
            int counter = 0;
            while ((line = br.readLine()) != null) {
            	String[] items = line.split(",");
            	String gcode = String.valueOf(items[0]);
            	double lon = Double.valueOf(items[1]);
            	double lat = Double.valueOf(items[2]);
            	double capacity = Double.valueOf(items[3]);
            	
            	Mesh mesh = MeshUtils.createMesh(3, lon, lat);
            	String mcode = mesh.getCode();
            	
            	if (counter++ % scale == 0) {
	            	City city = japan.getCity(gcode);
	            	if (city != null && japan.hasMesh(mcode)) {
	            		GMesh gmesh = japan.getMesh(mcode);
	            		Facility fac = new Facility(0, lon, lat, gcode, capacity);
	            		gmesh.addFacility(fac);
	            	}
            	}
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load Zenrin tatemono data: " + filename, e);
        }
		return 1;
	}

	public static int loadEconomicCensus(String filename, Country japan){
		try (BufferedReader br = new BufferedReader(new FileReader(filename))){
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
            	String[] items = line.split(",");
            	String mcode = items[0];
            	String gcode = items[1];
            	List<Double> values = new ArrayList<>();
         
            	for (int i = 2; i < items.length; i++){
            		values.add(Double.valueOf(items[i]));
            	}
            	
            	Mesh mesh = MeshUtils.createMesh(mcode);
            	City city = japan.getCity(gcode);
               	if (city != null) {
               		GMesh gmesh = japan.hasMesh(mcode) ? japan.getMesh(mcode) : new GMesh(mesh);
               		if (gmesh.getEconomics().isEmpty()) {
               			gmesh.setEconomics(values);
               		}
               		city.addMesh(gmesh);
               	}
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load economic census: " + filename, e);
        }
		return 1;
	}

}
