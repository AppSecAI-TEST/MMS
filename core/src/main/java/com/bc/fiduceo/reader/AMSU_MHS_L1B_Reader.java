/*
 * Copyright (C) 2016 Brockmann Consult GmbH
 * This code was developed for the EC project "Fidelity and Uncertainty in
 * Climate Data Records from Earth Observations (FIDUCEO)".
 * Grant Agreement: 638822
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * A copy of the GNU General Public License should have been supplied along
 * with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package com.bc.fiduceo.reader;

import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.geometry.*;
import com.bc.fiduceo.geometry.GeometryUtils;
import com.bc.fiduceo.geometry.GeometryFactory;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.ma2.MAMath;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


public class AMSU_MHS_L1B_Reader implements Reader {

    private static final String GEOLOCATION_GROUP_NAME = "Geolocation";
    private static final String LONGITUDE_VARIABLE_NAME = "Longitude";
    private final static int IntervalX = 50;
    private final static int IntervalY = 50;
    public static final String SCALE_ATTRIBUTE_NAME = "Scale";

    private final BoundingPolygonCreator boundingPolygonCreator;
    private NetcdfFile netcdfFile;
    private final GeometryFactory geometryFactory;


    public AMSU_MHS_L1B_Reader() {
        // @todo 2 tb/tb inject geometry factory 2016-02-25
        geometryFactory = new GeometryFactory(GeometryFactory.Type.S2);
        boundingPolygonCreator = new BoundingPolygonCreator(new Interval(IntervalX, IntervalY), geometryFactory);
        netcdfFile = null;
    }

    @Override
    public GeoCoding getGeoCoding() throws IOException {
        // @todo 1 tb/tb continue here 2016-02-25
        final Array longitudes = getLongitudes(netcdfFile);
        return new AMSU_MHS_GeoCoding();
    }

    @Override
    public String[] getSupportedSensorKeys() {
        throw new RuntimeException("not implemented");
    }

    static Array getLongitudes(NetcdfFile netcdfFile) throws IOException {
        final Group geolocationGroup = netcdfFile.findGroup(GEOLOCATION_GROUP_NAME);
        if (geolocationGroup == null) {
            throw new IOException("File does not contain the group '" + GEOLOCATION_GROUP_NAME + "' that is required");
        }

        final Variable longitudesVariable = geolocationGroup.findVariable(LONGITUDE_VARIABLE_NAME);
        if (longitudesVariable == null) {
            throw new IOException("File does not contain the variable '" + LONGITUDE_VARIABLE_NAME + "' that is required");
        }
        final Attribute scaleAtribute = longitudesVariable.findAttribute(SCALE_ATTRIBUTE_NAME);
        if (scaleAtribute == null) {
            throw new IOException("The variable '" + LONGITUDE_VARIABLE_NAME + "' does not contain the required attribute '" + SCALE_ATTRIBUTE_NAME +"'");
        }

        final Array longitudes = longitudesVariable.read();
        final MAMath.ScaleOffset scaleOffset = new MAMath.ScaleOffset(scaleAtribute.getNumericValue().doubleValue(), 0.0);
        return MAMath.convert2Unpacked(longitudes, scaleOffset);
    }

    static ArrayDouble.D2 rescaleCoordinate(ArrayInt.D2 coodinate, double scale) {
        int[] coordinates = (int[]) coodinate.copyTo1DJavaArray();
        int[] shape = coodinate.getShape();
        ArrayDouble arrayDouble = new ArrayDouble(shape);

        for (int i = 0; i < coordinates.length; i++) {
            arrayDouble.setDouble(i, ((coordinates[i] * scale)));
        }
        return (ArrayDouble.D2) arrayDouble.copy();
    }

    public static List<ArrayDouble.D2> getLat_Long(NetcdfFile netcdfFile) throws IOException {
        Array latitude = null;
        Array longitude = null;
        float latScale = 1;
        float longScale = 1;
        List<Variable> geolocation = netcdfFile.findGroup(GEOLOCATION_GROUP_NAME).getVariables();


        for (Variable geo : geolocation) {
            if (geo.getShortName().equals("Latitude")) {
                latitude = geo.read();
                latScale = (float) geo.findAttribute(SCALE_ATTRIBUTE_NAME).getNumericValue();
            } else if (geo.getShortName().equals(LONGITUDE_VARIABLE_NAME)) {
                longitude = geo.read();
                longScale = (float) geo.findAttribute("Scale").getNumericValue();
            }
        }
        List<ArrayDouble.D2> d2List = new ArrayList<>();

        ArrayDouble.D2 arrayLong = AMSU_MHS_L1B_Reader.rescaleCoordinate((ArrayInt.D2) longitude, longScale);
        ArrayDouble.D2 arrayLat = AMSU_MHS_L1B_Reader.rescaleCoordinate((ArrayInt.D2) latitude, latScale);

        d2List.add(arrayLong);// Index 0 Longitude
        d2List.add(arrayLat);// Index 0 Latitude
        return d2List;
    }

    @Override
    public String getRegEx() {
        return "'?[A-Z].+[AMBX|MHSX].+[NK|M1].D\\d{5}.S\\d{4}.E\\d{4}.B\\d{7}.+[GC|WI].h5";
    }

    @Override
    public void open(File file) throws IOException {
        netcdfFile = NetcdfFile.open(file.getPath());
    }

    @Override
    public void close() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    @Override
    public boolean checkSensorTypeName(String sensorType) {
        return getSensorTypes().containsKey(sensorType);
    }

    @Override
    public HashMap<String, String> getSensorTypes() {
        HashMap<String, String> sensorListHashMap = new HashMap<>();

        sensorListHashMap.put("amsub-nn	", "TN");
        sensorListHashMap.put("amsub-n06", "NA");
        sensorListHashMap.put("amsub-n07", "NC");
        sensorListHashMap.put("amsub-n08", "NE");
        sensorListHashMap.put("amsub-n09", "NF");
        sensorListHashMap.put("amsub-n10", "NG");
        sensorListHashMap.put("amsub-n11", "NH");
        sensorListHashMap.put("amsub-n12", "ND");
        sensorListHashMap.put("amsub-n14", "NJ");
        sensorListHashMap.put("amsub-n15", "NK");
        sensorListHashMap.put("amsub-n16", "NL");
        sensorListHashMap.put("amsub-n17", "NM");
        sensorListHashMap.put("amsub-n18", "NN");
        sensorListHashMap.put("amsub-n19", "NPrime");

        return sensorListHashMap;
    }

    @Override
    public AcquisitionInfo read() throws IOException {

        List<ArrayDouble.D2> lat_long = getLat_Long(netcdfFile);
        ArrayDouble.D2 arrayDoubleLongitude = lat_long.get(0);
        ArrayDouble.D2 arrayDoubleLatitude = lat_long.get(1);

        final List<Point> boundaryPoints = boundingPolygonCreator.extractBoundaryPoints(arrayDoubleLatitude, arrayDoubleLongitude);
        final Polygon polygon = geometryFactory.createPolygon(boundaryPoints);
        final Polygon testPolygon = (Polygon) geometryFactory.parse("POLYGON((-1 -10, 1 10, 10 10, 10 -1, -1 -10))");
        final Geometry intersection = polygon.intersection(testPolygon);

        System.out.println("wkt = " + GeometryUtils.plotPolygon(Arrays.asList(testPolygon.getCoordinates())));
        System.out.println("wkt = " + GeometryUtils.plotPolygon(boundaryPoints));
        System.out.println("wkt = " + GeometryUtils.plotPolygon(Arrays.asList(intersection.getCoordinates())));
        final boolean empty = intersection.isEmpty();
        System.out.println("empty = " + empty);
        if (!polygon.isValid()) {
            // @todo 1 tb/tb handle refinement of geometry 2016-02-24
            throw new RuntimeException("code branch not implemented");
        }

        final AcquisitionInfo acquisitionInfo = boundingPolygonCreator.createBoundingPolygon(arrayDoubleLatitude,
                arrayDoubleLongitude);

        final int startYear = getGlobalAttributeAsInteger("startdatayr");
        final int startDay = getGlobalAttributeAsInteger("startdatady");
        final int startTime = getGlobalAttributeAsInteger("startdatatime_ms");

        final int endYear = getGlobalAttributeAsInteger("enddatayr");
        final int endDay = getGlobalAttributeAsInteger("enddatady");
        final int endTime = getGlobalAttributeAsInteger("enddatatime_ms");

        acquisitionInfo.setSensingStart(getDate(startYear, startDay, startTime));
        acquisitionInfo.setSensingStop(getDate(endYear, endDay, endTime));

        return acquisitionInfo;
    }

    private int getGlobalAttributeAsInteger(String attributeName) throws IOException {
        final Attribute attribute = netcdfFile.findGlobalAttribute(attributeName);
        if (attribute == null) {
            throw new IOException("Global attribute '" + attributeName + "' not found.");
        }
        return attribute.getNumericValue().intValue();
    }

    private Date getDate(int year, int day_of_yr, int time) {
        final Calendar calendar = ProductData.UTC.createCalendar();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, day_of_yr);

        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.MILLISECOND, time);
        return calendar.getTime();
    }
}
