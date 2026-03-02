/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.locationtech.jts.geom.Envelope
 *  org.locationtech.jts.geom.Geometry
 *  org.locationtech.jts.geom.Point
 *  org.locationtech.jts.geom.Polygon
 *  org.locationtech.jts.geom.prep.PreparedGeometry
 *  org.locationtech.jts.geom.prep.PreparedGeometryFactory
 *  org.locationtech.jts.index.strtree.STRtree
 *  org.apache.commons.lang.text.StrTokenizer
 *  org.apache.logging.log4j.LogManager
 *  org.apache.logging.log4j.Logger
 */
package jp.ac.ut.csis.pflow.reallocation;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.index.strtree.STRtree;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import jp.ac.ut.csis.pflow.geom.DistanceUtils;
import jp.ac.ut.csis.pflow.geom.GeometryUtils;
import jp.ac.ut.csis.pflow.geom.LonLat;
import jp.ac.ut.csis.pflow.reallocation.AReallocator;
import jp.ac.ut.csis.pflow.reallocation.POI;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CsvReallocator
extends AReallocator {
    private static final Logger LOGGER = LogManager.getLogger(CsvReallocator.class);
    private STRtree _spatialIndex = null;
    private Random _random = new Random();

    protected CsvReallocator(File poiTsvFile) {
        this(poiTsvFile, AReallocator.Method.RANDOM);
    }

    protected CsvReallocator(File poiTsvFile, AReallocator.Method method) {
        super(method);
        this.init(poiTsvFile);
    }

    public void init(File poiTsvFile) {
        LOGGER.info(String.format("start loading POIs from %s", poiTsvFile.getAbsolutePath()));
        try {
            Throwable throwable = null;
            Object var3_5 = null;
            try (BufferedReader br = new BufferedReader(new FileReader(poiTsvFile));){
                int N = 0;
                this._spatialIndex = new STRtree();
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] tokens = StrTokenizer.getTSVInstance((String)line).getTokenArray();
                    Geometry geom = GeometryUtils.parseWKB(tokens[7]);
                    if (!(geom instanceof Point)) continue;
                    Point point = (Point)Point.class.cast(geom);
                    PreparedGeometry prepGeom = PreparedGeometryFactory.prepare((Geometry)point);
                    Envelope envelope = prepGeom.getGeometry().getEnvelopeInternal();
                    POI poi = new POI(tokens[0], tokens[3], Double.parseDouble(tokens[6]), point.getX(), point.getY());
                    this._spatialIndex.insert(envelope, (Object)poi);
                    ++N;
                }
                LOGGER.info(String.format("load %d POIs", N));
            }
            catch (Throwable throwable2) {
                if (throwable == null) {
                    throwable = throwable2;
                } else if (throwable != throwable2) {
                    throwable.addSuppressed(throwable2);
                }
                throw throwable;
            }
        }
        catch (IOException exp) {
            LOGGER.error("fail to load POI data", (Throwable)exp);
        }
    }

    @Override
    public POI reallocate(LonLat point, double radius) {
        double w = radius * 0.012 / 1000.0;
        double h = radius * 0.012 / 1000.0;
        Envelope search = new Envelope(point.getLon() - w, point.getLon() + w, point.getLat() - h, point.getLat() + h);
        List pois = this._spatialIndex.query(search);
        double sumArea = 0.0;
        int i = 0;
        while (i < pois.size()) {
            POI poi = (POI)pois.get(i);
            if (DistanceUtils.distance(point, poi) > radius) {
                pois.remove(i--);
            } else {
                sumArea += Double.isNaN(poi.getArea()) ? 0.0 : poi.getArea();
            }
            ++i;
        }
        return this.reallocate(pois, sumArea, point);
    }

    @Override
    public POI reallocate(Polygon areaPolygon) {
        Envelope search = areaPolygon.getEnvelopeInternal();
        List pois = this._spatialIndex.query(search);
        double sumArea = 0.0;
        int i = 0;
        while (i < pois.size()) {
            POI poi = (POI)pois.get(i);
            sumArea += Double.isNaN(poi.getArea()) ? 0.0 : poi.getArea();
            ++i;
        }
        Point centroid = areaPolygon.getCentroid();
        return this.reallocate(pois, sumArea, new LonLat(centroid.getX(), centroid.getY()));
    }

    private POI reallocate(List<POI> pois, double sumArea, LonLat point) {
        if (pois.isEmpty()) {
            return new POI(null, point.getLon(), point.getLat());
        }
        switch (this.getMethod()) {
            default: {
                return pois.get(this._random.nextInt(pois.size()));
            }
            case AREA_PROB: 
        }
        assert (sumArea > 0.0) : "POI area must be larger than zero";
        Collections.sort(pois, new Comparator<POI>(){

            @Override
            public int compare(POI a, POI b) {
                Double areaA = a.getArea();
                Double areaB = b.getArea();
                return areaB.compareTo(areaA);
            }
        });
        double sum = 0.0;
        double ratio = this._random.nextDouble();
        for (POI poi : pois) {
            double d;
            sum += poi.getArea() / sumArea;
            if (!(d >= ratio)) continue;
            return poi;
        }
        return pois.get(pois.size() - 1);
    }
}

