
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

package com.bc.fiduceo.db;

import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.core.SatelliteObservation;
import com.bc.fiduceo.core.Sensor;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.util.TimeUtils;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PostGISDriver extends AbstractDriver {

    private WKBWriter wkbWriter;
    private WKBReader wkbReader;

    private GeometryFactory geometryFactory;

    public PostGISDriver() {
        wkbWriter = new WKBWriter();
        wkbReader = new WKBReader();
    }

    @Override
    public String getUrlPattern() {
        return "jdbc:postgresql";
    }


    @Override
    public void setGeometryFactory(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    @Override
    public boolean isInitialized() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void initialize() throws SQLException {
        Statement statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE SATELLITE_OBSERVATION (ID SERIAL PRIMARY KEY, " +
                "StartDate TIMESTAMP," +
                "StopDate TIMESTAMP," +
                "NodeType SMALLINT," +
                "GeoBounds GEOMETRY, " +
                "SensorId INT," +
                "DataFile VARCHAR(256)," +
                "TimeAxisStartIndex INT, " +
                "TimeAxisEndIndex INT)");

        statement = connection.createStatement();
        statement.executeUpdate("CREATE TABLE SENSOR (ID SERIAL PRIMARY KEY, " +
                "Name VARCHAR(64))");
    }

    @Override
    public void insert(SatelliteObservation observation) throws SQLException {
        final Sensor sensor = observation.getSensor();
        Integer sensorId = getSensorId(sensor.getName());
        if (sensorId == null) {
            sensorId = insert(sensor);
        }

        final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO SATELLITE_OBSERVATION VALUES(default, ?, ?, ?, ST_GeomFromWKB(?), ?, ?, ?, ?)");
        preparedStatement.setTimestamp(1, TimeUtils.toTimestamp(observation.getStartTime()));
        preparedStatement.setTimestamp(2, TimeUtils.toTimestamp(observation.getStopTime()));
        preparedStatement.setByte(3, (byte) observation.getNodeType().toId());
        // @todo 3 tb/tb extend to support multiple geometries
        final Geometry[] geoBounds = observation.getGeoBounds();
        if (geoBounds.length != 1) {
            throw new RuntimeException("Support for multiple geometries not implemented yet");
        }
        preparedStatement.setObject(4, geometryFactory.toStorageFormat(geoBounds[0]));
        preparedStatement.setInt(5, sensorId);
        preparedStatement.setString(6, observation.getDataFilePath().toString());
        preparedStatement.setInt(7, observation.getTimeAxisStartIndex());
        preparedStatement.setInt(8, observation.getTimeAxisEndIndex());

        preparedStatement.executeUpdate();
    }

    @Override
    public List<SatelliteObservation> get() throws SQLException {
        final Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        final String sql = createSql();
        final ResultSet resultSet = statement.executeQuery(sql);
        resultSet.last();
        final int numValues = resultSet.getRow();
        resultSet.beforeFirst();

        final List<SatelliteObservation> resultList = new ArrayList<>(numValues);
        while (resultSet.next()) {
            final SatelliteObservation observation = new SatelliteObservation();

            final Timestamp startDate = resultSet.getTimestamp("StartDate");
            observation.setStartTime(TimeUtils.toDate(startDate));

            final Timestamp stopDate = resultSet.getTimestamp("StopDate");
            observation.setStopTime(TimeUtils.toDate(stopDate));

            final int nodeTypeId = resultSet.getInt("NodeType");
            observation.setNodeType(NodeType.fromId(nodeTypeId));

            final byte[] geoBoundsBytes = resultSet.getBytes("ST_AsBinary");
            final Geometry geometry = geometryFactory.fromStorageFormat(geoBoundsBytes);
            // @todo 3 tb/tb implement support for multiple geometries 2016-03-01
            observation.setGeoBounds(new Geometry[]{geometry});

            final int sensorId = resultSet.getInt("SensorId");
            final Sensor sensor = getSensor(sensorId);
            observation.setSensor(sensor);

            final String dataFile = resultSet.getString("DataFile");
            observation.setDataFilePath(dataFile);

            final int timeAxisStartIndex = resultSet.getInt("TimeAxisStartIndex");
            observation.setTimeAxisStartIndex(timeAxisStartIndex);

            final int timeAxisEndIndex = resultSet.getInt("TimeAxisEndIndex");
            observation.setTimeAxisEndIndex(timeAxisEndIndex);

            resultList.add(observation);
        }

        return resultList;
    }

    private String createSql() {
        return "SELECT StartDate, StopDate, NodeType, ST_AsBinary(GeoBounds), SensorId, DataFile, TimeAxisStartIndex, TimeAxisEndIndex FROM SATELLITE_OBSERVATION";
    }

    @Override
    public List<SatelliteObservation> get(QueryParameter parameter) throws SQLException {
        throw new RuntimeException("not implemented");
    }
}
