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
import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryCollection;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.LineString;
import com.bc.fiduceo.geometry.TimeAxis;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.location.SwathPixelLocator;
import com.bc.fiduceo.math.TimeInterval;
import com.bc.fiduceo.util.TimeUtils;
import org.esa.snap.core.util.StringUtils;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

public class AVHRR_GAC_Reader implements Reader {

    private static final String[] SENSOR_KEYS = {"avhrr-n06", "avhrr-n07", "avhrr-n08", "avhrr-n09", "avhrr-n10", "avhrr-n11", "avhrr-n12", "avhrr-n13", "avhrr-n14", "avhrr-n15", "avhrr-n16", "avhrr-n17", "avhrr-n18", "avhrr-n19", "avhrr-m01", "avhrr-m02"};
    private static final String START_TIME_ATTRIBUTE_NAME = "start_time";
    private static final String STOP_TIME_ATTRIBUTE_NAME = "stop_time";

    private NetcdfFile netcdfFile;
    private BoundingPolygonCreator boundingPolygonCreator;
    private GeometryFactory geometryFactory;
    private ArrayCache arrayCache;

    @Override
    public void open(File file) throws IOException {
        netcdfFile = NetcdfFile.open(file.getPath());
        arrayCache = new ArrayCache(netcdfFile);
    }

    @Override
    public void close() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
            netcdfFile = null;
        }
    }

    @Override
    public String[] getSupportedSensorKeys() {
        return SENSOR_KEYS;
    }

    @Override
    public boolean checkSensorTypeName(String sensorType) {
        // @todo 1 tb/tb refactor this whole pattern 2016-02-25
        // the reader shall return an array of "sensor-platform"
        // the factory shall NOT call into the reader at this pouínt, instead the factory shall contain a hashmap with
        // all "sensor-platform" as keys and the readers as values
        return sensorType.contains("avhrr");
    }

    @Override
    public HashMap<String, String> getSensorTypes() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public AcquisitionInfo read() throws IOException {
        final AcquisitionInfo acquisitionInfo = new AcquisitionInfo();

        final Date startDate = parseDateAttribute(netcdfFile.findGlobalAttribute(START_TIME_ATTRIBUTE_NAME));
        acquisitionInfo.setSensingStart(startDate);

        final Date stopDate = parseDateAttribute(netcdfFile.findGlobalAttribute(STOP_TIME_ATTRIBUTE_NAME));
        acquisitionInfo.setSensingStop(stopDate);

        acquisitionInfo.setNodeType(NodeType.UNDEFINED);

        final Geometries geometries = calculateGeometries();
        acquisitionInfo.setBoundingGeometry(geometries.getBoundingGeometry());

        final Geometry timeAxesGeometry = geometries.getTimeAxesGeometry();
        if (timeAxesGeometry instanceof GeometryCollection) {
            final GeometryCollection axesCollection = (GeometryCollection) timeAxesGeometry;
            final Geometry[] axesGeometries = axesCollection.getGeometries();
            final TimeAxis[] timeAxes = new TimeAxis[axesGeometries.length];
            final TimeInterval timeInterval = new TimeInterval(startDate, stopDate);
            final TimeInterval[] timeSplits = timeInterval.split(axesGeometries.length);
            for (int i = 0; i < axesGeometries.length; i++) {
                final LineString axisGeometry = (LineString) axesGeometries[i];
                final TimeInterval currentTimeInterval = timeSplits[i];
                timeAxes[i] = geometryFactory.createTimeAxis(axisGeometry, currentTimeInterval.getStartTime(), currentTimeInterval.getStopTime());
            }
            acquisitionInfo.setTimeAxes(timeAxes);
        } else {
            final TimeAxis timeAxis = geometryFactory.createTimeAxis((LineString) timeAxesGeometry, startDate, stopDate);
            acquisitionInfo.setTimeAxes(new TimeAxis[]{timeAxis});
        }


        return acquisitionInfo;
    }

    private Geometries calculateGeometries() throws IOException {
        final BoundingPolygonCreator boundingPolygonCreator = getBoundingPolygonCreator();
        final Geometries geometries = new Geometries();

        final Array longitudes = arrayCache.get("lon");
        final Array latitudes = arrayCache.get("lat");
        Geometry timeAxisGeometry;
        Geometry boundingGeometry = boundingPolygonCreator.createBoundingGeometry(longitudes, latitudes);
        if (!boundingGeometry.isValid()) {
            boundingGeometry = boundingPolygonCreator.createBoundingGeometrySplitted(longitudes, latitudes, 2);
            checkForValidity((GeometryCollection) boundingGeometry);
            timeAxisGeometry = boundingPolygonCreator.createTimeAxisGeometrySplitted(longitudes, latitudes, 2);
        } else {
            timeAxisGeometry = boundingPolygonCreator.createTimeAxisGeometry(longitudes, latitudes);
        }

        geometries.setBoundingGeometry(boundingGeometry);
        geometries.setTimeAxesGeometry(timeAxisGeometry);
        return geometries;
    }

    private BoundingPolygonCreator getBoundingPolygonCreator() {
        if (boundingPolygonCreator == null) {
            final GeometryFactory geometryFactory = getGeometryFactory();

            // @todo 2 tb/tb move intervals to config 2016-03-02
            boundingPolygonCreator = new BoundingPolygonCreator(new Interval(40, 100), geometryFactory);
        }

        return boundingPolygonCreator;
    }

    private GeometryFactory getGeometryFactory() {
        if (geometryFactory == null) {
            // @todo 1 tb/tb inject geometry factory 2016-03-02
            geometryFactory = new GeometryFactory(GeometryFactory.Type.S2);
        }

        return geometryFactory;
    }

    @Override
    public PixelLocator getGeoCoding() throws IOException {
        final ArrayFloat lonStorage = (ArrayFloat) arrayCache.get("lon");
        final ArrayFloat latStorage = (ArrayFloat) arrayCache.get("lat");
        final int[] shape = lonStorage.getShape();
        final int width = shape[1];
        final int height = shape[0];
        return new SwathPixelLocator(lonStorage, latStorage, width, height);
    }

    @Override
    public String getRegEx() {
        return "[0-9]{14}-ESACCI-L1C-AVHRR([0-9]{2}|MTA)_G-fv\\d\\d.\\d.nc";
    }

    // package access for testing only tb 2016-03-02
    static Date parseDateAttribute(Attribute timeAttribute) throws IOException {
        if (timeAttribute == null) {
            throw new IOException("required global attribute '" + START_TIME_ATTRIBUTE_NAME + "' not present");
        }
        final String startTimeString = timeAttribute.getStringValue();
        if (StringUtils.isNullOrEmpty(startTimeString)) {
            throw new IOException("required global attribute '" + START_TIME_ATTRIBUTE_NAME + "' contains no data");
        }
        return TimeUtils.parse(startTimeString, "yyyyMMdd'T'HHmmss'Z'");
    }

    // package access for testing only tb 2016-03-03
    static void checkForValidity(GeometryCollection boundingGeometry) {
        final Geometry[] geometries = boundingGeometry.getGeometries();
        for (final Geometry geometry : geometries) {
            if (!geometry.isValid()) {
                throw new RuntimeException("Invalid geometry detected");
            }
        }
    }

    private class Geometries {
        private Geometry boundingGeometry;
        private Geometry timeAxesGeometry;

        public Geometry getBoundingGeometry() {
            return boundingGeometry;
        }

        public void setBoundingGeometry(Geometry boundingGeometry) {
            this.boundingGeometry = boundingGeometry;
        }

        public Geometry getTimeAxesGeometry() {
            return timeAxesGeometry;
        }

        public void setTimeAxesGeometry(Geometry timeAxesGeometry) {
            this.timeAxesGeometry = timeAxesGeometry;
        }
    }
}
