/*
 * Copyright (C) 2015 Brockmann Consult GmbH
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
import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Point;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.geometry.s2.S2GeometryFactory;
import com.google.common.geometry.S2Loop;
import com.google.common.geometry.S2Point;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoundingPolygonCreator {

    private final int intervalX;
    private final int intervalY;
    private final GeometryFactory geometryFactory;

    public BoundingPolygonCreator(Interval interval, GeometryFactory geometryFactory) {
        if ((interval.getX() <= 0) || (interval.getY() <= 0)) {
            throw new RuntimeException("invalid interval");
        }
        this.intervalX = interval.getX();
        this.intervalY = interval.getY();

        this.geometryFactory = geometryFactory;

    }

    static void closePolygon(List<Point> coordinates) {
        if (coordinates.size() > 1) {
            coordinates.add(coordinates.get(0));
        }
    }

    public static boolean isPointValidation(List<Polygon> polygonList) {
        boolean valid = true;
        for (Polygon polygon : polygonList) {

            List<Point> points = Arrays.asList(polygon.getCoordinates());
            List<S2Point> s2Points = S2GeometryFactory.extractS2Points(points);
            S2Loop s2Loop = new S2Loop(s2Points);
            valid = s2Loop.isValid();
            if (!valid) {
                return false;
            }
        }
        return valid;
    }

    public static String plotMultiPolygon(List<Polygon> polygonList) {
        final StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("MULTIPOLYGON((");

        for (int j = 0; j < polygonList.size(); j++) {
            Polygon polygon = polygonList.get(j);
            final Point[] points = polygon.getCoordinates();
            stringBuffer.append("(");
            for (int i = 0; i < points.length; i++) {
                Point coordinate = points[i];
                stringBuffer.append(coordinate.getLon());
                stringBuffer.append(" ");
                stringBuffer.append(coordinate.getLat());
                if (i < points.length - 1) {
                    stringBuffer.append(",");
                }
            }
            stringBuffer.append(")");
            if (j < polygonList.size() - 1) {
                stringBuffer.append(",");
            }
        }
        stringBuffer.append("))");
        return stringBuffer.toString();
    }

    public List<Point> allBoundingPoint(ArrayDouble.D2 arrayLatitude, ArrayDouble.D2 arrayLongitude,
                                        NodeType nodeType,
                                        int intervalX, int intervalY) {
        final int[] shape = arrayLatitude.getShape();
        int width = shape[1] - 1;
        int height = shape[0] - 1;


        List<Point> coordinates = new ArrayList<>();

        int[] timeAxisStart = new int[2];
        int[] timeAxisEnd = new int[2];
        if (nodeType == NodeType.ASCENDING) {
            for (int x = 0; x < width; x += intervalX) {
                final double lon = arrayLongitude.get(0, x);
                final double lat = arrayLatitude.get(0, x);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }

            timeAxisStart[0] = coordinates.size();
            timeAxisEnd[0] = timeAxisStart[0];
            for (int y = 0; y < height; y += intervalY) {
                final double lon = arrayLongitude.get(y, width);
                final double lat = arrayLatitude.get(y, width);
                coordinates.add(geometryFactory.createPoint(lon, lat));
                ++timeAxisEnd[0];
            }

            for (int x = width; x > 0; x -= intervalX) {
                final double lon = arrayLongitude.get(height, x);
                final double lat = arrayLatitude.get(height, x);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }

            for (int y = height; y > 0; y -= intervalY) {
                final double lon = arrayLongitude.get(y, 0);
                final double lat = arrayLatitude.get(y, 0);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }
        } else {
            timeAxisStart[0] = 0;
            timeAxisEnd[0] = 0;
            for (int y = 0; y < height; y += intervalY) {
                final double lon = arrayLongitude.get(y, width);
                final double lat = arrayLatitude.get(y, width);
                coordinates.add(geometryFactory.createPoint(lon, lat));
                ++timeAxisEnd[0];
            }

            for (int x = width; x > 0; x -= intervalX) {
                final double lon = arrayLongitude.get(height, x);
                final double lat = arrayLatitude.get(height, x);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }

            for (int y = height; y > 0; y -= intervalY) {
                final double lon = arrayLongitude.get(y, 0);
                final double lat = arrayLatitude.get(y, 0);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }

            for (int x = 0; x < width; x += intervalX) {
                final double lon = arrayLongitude.get(0, x);
                final double lat = arrayLatitude.get(0, x);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }
        }
        if (GeometryFactory.Type.JTS == geometryFactory.getType()) {
            coordinates.add(coordinates.get(0));
        }
        return coordinates;
    }

    public List<Polygon> createPolygonsBounding(ArrayDouble.D2 arrayLatitude, ArrayDouble.D2 arrayLongitude,
                                                int width, int totalHeight, int dept) {

        int intervalX = 50;
        int intervalY = 50;

        List<Point> coordinatesSecond = new ArrayList<>();
        List<Polygon> polygonList = new ArrayList<>();

        int[] timeAxisStart = new int[2];
        int[] timeAxisEnd = new int[2];
        timeAxisEnd[0] = timeAxisStart[0];

        int maxHeight = 0;
        int initialHeight = 0;

        while (true) {
            List<Point> coordinatesFirst = new ArrayList<>();
            maxHeight = maxHeight + (totalHeight / dept);
            if (maxHeight > totalHeight) {
                maxHeight = totalHeight;
            }

            for (int x = 0; x < width; x += intervalX) {
                final double lon = arrayLongitude.get(initialHeight, x);
                final double lat = arrayLatitude.get(initialHeight, x);
                coordinatesFirst.add(geometryFactory.createPoint(lon, lat));
            }

            timeAxisStart[0] = coordinatesFirst.size();
            timeAxisEnd[0] = timeAxisStart[0];

            for (int y = initialHeight; y < maxHeight; y += intervalY) {
                final double lon = arrayLongitude.get(y, width);
                final double lat = arrayLatitude.get(y, width);
                coordinatesFirst.add(geometryFactory.createPoint(lon, lat));
                ++timeAxisEnd[0];
            }

            for (int x = width; x > 0; x -= intervalX) {
                final double lon = arrayLongitude.get(maxHeight, x);
                final double lat = arrayLatitude.get(maxHeight, x);
                coordinatesFirst.add(geometryFactory.createPoint(lon, lat));
            }

            for (int y = maxHeight; y > initialHeight; y -= intervalY) {
                final double lon = arrayLongitude.get(y, 0);
                final double lat = arrayLatitude.get(y, 0);
                coordinatesFirst.add(geometryFactory.createPoint(lon, lat));
            }
            if (GeometryFactory.Type.JTS == geometryFactory.getType()) {
                coordinatesSecond.add(coordinatesSecond.get(0));
            }
            polygonList.add(geometryFactory.createPolygon(coordinatesFirst));

            initialHeight = maxHeight;
            if (maxHeight == totalHeight) {
                break;
            }
        }
        return polygonList;
    }

    //todo mba : include the NodeType.
    public AcquisitionInfo createBoundingPolygon(ArrayDouble.D2 arrayLatitude, ArrayDouble.D2 arrayLongitude) {
        final int[] shape = arrayLatitude.getShape();
        List<Polygon> polygonsBounding = new ArrayList<>();
        int width = shape[1] - 1;
        int height = shape[0] - 1;

        int[] timeAxisStart = new int[2];
        int[] timeAxisEnd = new int[2];


        for (int i = 1; i <= 4; i++) {
            polygonsBounding = createPolygonsBounding(arrayLatitude, arrayLongitude, width, height, i);
            if (isPointValidation(polygonsBounding)) {
                break;
            }
        }
        try {
            if (polygonsBounding.size() == 0) {
                throw new Exception("There is no point is create from the boundary");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        final AcquisitionInfo acquisitionInfo = new AcquisitionInfo();
        List<Point> pointList = new ArrayList<>();
        for (Polygon polygon:polygonsBounding){
            pointList.addAll(Arrays.asList(polygon.getCoordinates()));
        }
        acquisitionInfo.setCoordinates(pointList);
        acquisitionInfo.setPolygons(polygonsBounding);
        acquisitionInfo.setTimeAxisStartIndices(timeAxisStart);
        acquisitionInfo.setTimeAxisEndIndices(timeAxisEnd);

        return acquisitionInfo;
    }

    public AcquisitionInfo createPixelCodedBoundingPolygon(ArrayDouble.D2 arrayLatitude, ArrayDouble.D2 arrayLongitude, NodeType nodeType) {
        int[] timeAxisStart = new int[2];
        int[] timeAxisEnd = new int[2];

        final List<Point> coordinates = allBoundingPoint(arrayLatitude, arrayLongitude, nodeType, intervalX, intervalY);

        final AcquisitionInfo acquisitionInfo = new AcquisitionInfo();
        acquisitionInfo.setCoordinates(coordinates);
        acquisitionInfo.setTimeAxisStartIndices(timeAxisStart);
        acquisitionInfo.setTimeAxisEndIndices(timeAxisEnd);

        return acquisitionInfo;
    }

    // @todo 1 tb/tb add time axis tracking
    public AcquisitionInfo createIASIBoundingPolygon(ArrayFloat.D2 arrayLatitude, ArrayFloat.D2 arrayLongitude) {
        final int geoXTrack = arrayLatitude.getShape()[1] - 1;
        final int geoTrack = arrayLatitude.getShape()[0] - 1;
        final List<Point> coordinates = new ArrayList<>();

        float lon = arrayLongitude.get(0, 0);
        float lat = arrayLatitude.get(0, 0);
        coordinates.add(geometryFactory.createPoint(lon, lat));

        for (int x = 1; x < geoXTrack; x += intervalX) {
            lon = arrayLongitude.get(0, x);
            lat = arrayLatitude.get(0, x);
            coordinates.add(geometryFactory.createPoint(lon, lat));
        }

        for (int y = 0; y <= geoTrack; y += intervalY) {
            lon = arrayLongitude.get(y, geoXTrack);
            lat = arrayLatitude.get(y, geoXTrack);
            coordinates.add(geometryFactory.createPoint(lon, lat));
            if ((y + intervalY) > geoTrack) {
                lon = arrayLongitude.get(geoTrack, geoXTrack);
                lat = arrayLatitude.get(geoTrack, geoXTrack);
                coordinates.add(geometryFactory.createPoint(lon, lat));
            }
        }

        for (int x = geoXTrack - 1; x > 0; x -= intervalX) {
            lon = arrayLongitude.get(geoTrack, x);
            lat = arrayLatitude.get(geoTrack, x);
            coordinates.add(geometryFactory.createPoint(lon, lat));
        }

        for (int y = geoTrack; y >= 0; y -= intervalY) {
            lon = arrayLongitude.get(y, 0);
            lat = arrayLatitude.get(y, 0);
            coordinates.add(geometryFactory.createPoint(lon, lat));
        }

        closePolygon(coordinates);

        final AcquisitionInfo acquisitionInfo = new AcquisitionInfo();
        acquisitionInfo.setCoordinates(coordinates);
        return acquisitionInfo;
    }
}
