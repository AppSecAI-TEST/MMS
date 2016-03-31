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

package com.bc.fiduceo.reader.avhrr_gac;


import com.bc.fiduceo.IOTestRunner;
import com.bc.fiduceo.TestUtil;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryCollection;
import com.bc.fiduceo.geometry.Point;
import com.bc.fiduceo.geometry.TimeAxis;
import com.bc.fiduceo.reader.AcquisitionInfo;
import com.bc.fiduceo.reader.TimeLocator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ucar.ma2.Array;
import ucar.ma2.Index;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.*;

@SuppressWarnings("ThrowFromFinallyBlock")
@RunWith(IOTestRunner.class)
public class AVHRR_GAC_Reader_IO_Test {

    private File testDataDirectory;
    private AVHRR_GAC_Reader reader;

    @Before
    public void setUp() throws IOException {
        testDataDirectory = TestUtil.getTestDataDirectory();
        reader = new AVHRR_GAC_Reader();
    }

    @Test
    public void testReadAcquisitionInfo_NOAA17() throws IOException {
        final File file = createAvhrrNOAA17Path();

        try {
            reader.open(file);

            final AcquisitionInfo acquisitionInfo = reader.read();
            assertNotNull(acquisitionInfo);

            final Date sensingStart = acquisitionInfo.getSensingStart();
            TestUtil.assertCorrectUTCDate(2007, 4, 1, 3, 34, 54, 0, sensingStart);

            final Date sensingStop = acquisitionInfo.getSensingStop();
            TestUtil.assertCorrectUTCDate(2007, 4, 1, 5, 28, 48, 0, sensingStop);

            final NodeType nodeType = acquisitionInfo.getNodeType();
            assertEquals(NodeType.UNDEFINED, nodeType);

            final Geometry boundingGeometry = acquisitionInfo.getBoundingGeometry();
            assertNotNull(boundingGeometry);
            assertTrue(boundingGeometry instanceof GeometryCollection);
            final GeometryCollection geometryCollection = (GeometryCollection) boundingGeometry;
            final Geometry[] geometries = geometryCollection.getGeometries();
            assertEquals(2, geometries.length);

            Point[] coordinates = geometries[0].getCoordinates();
            assertEquals(161, coordinates.length);
            assertEquals(-66.97299194335938, coordinates[0].getLon(), 1e-8);
            assertEquals(-5.238999843597412, coordinates[0].getLat(), 1e-8);

            assertEquals(-74.2349853515625, coordinates[23].getLon(), 1e-8);
            assertEquals(61.316001892089844, coordinates[23].getLat(), 1e-8);

            coordinates = geometries[1].getCoordinates();
            assertEquals(161, coordinates.length);
            assertEquals(69.81500244140625, coordinates[0].getLon(), 1e-8);
            assertEquals(-12.913000106811522, coordinates[0].getLat(), 1e-8);

            assertEquals(7.164999961853029, coordinates[23].getLon(), 1e-8);
            assertEquals(-67.44300079345703, coordinates[23].getLat(), 1e-8);

            final TimeAxis[] timeAxes = acquisitionInfo.getTimeAxes();
            assertEquals(2, timeAxes.length);
            coordinates = geometries[0].getCoordinates();
            Date time = timeAxes[0].getTime(coordinates[0]);
            TestUtil.assertCorrectUTCDate(2007, 4, 1, 3, 34, 54, 0, time);

            coordinates = geometries[1].getCoordinates();
            time = timeAxes[1].getTime(coordinates[0]);
            TestUtil.assertCorrectUTCDate(2007, 4, 1, 4, 32, 4, 754, time);

        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadAcquisitionInfo_NOAA18() throws IOException {
        final File file = createAvhrrNOAA18Path();

        try {
            reader.open(file);

            final AcquisitionInfo acquisitionInfo = reader.read();
            assertNotNull(acquisitionInfo);

            final Date sensingStart = acquisitionInfo.getSensingStart();
            TestUtil.assertCorrectUTCDate(2007, 4, 1, 8, 4, 12, 0, sensingStart);

            final Date sensingStop = acquisitionInfo.getSensingStop();
            TestUtil.assertCorrectUTCDate(2007, 4, 1, 9, 46, 11, 0, sensingStop);

            final NodeType nodeType = acquisitionInfo.getNodeType();
            assertEquals(NodeType.UNDEFINED, nodeType);

            final Geometry boundingGeometry = acquisitionInfo.getBoundingGeometry();
            assertNotNull(boundingGeometry);
            assertTrue(boundingGeometry instanceof GeometryCollection);
            final GeometryCollection geometryCollection = (GeometryCollection) boundingGeometry;
            final Geometry[] geometries = geometryCollection.getGeometries();
            assertEquals(2, geometries.length);

            Point[] coordinates = geometries[0].getCoordinates();
            assertEquals(147, coordinates.length);
            assertEquals(-110.22799682617189, coordinates[17].getLon(), 1e-8);
            assertEquals(38.00899887084961, coordinates[17].getLat(), 1e-8);

            assertEquals(177.14199829101562, coordinates[58].getLon(), 1e-8);
            assertEquals(-66.05000305175781, coordinates[58].getLat(), 1e-8);

            coordinates = geometries[1].getCoordinates();
            assertEquals(147, coordinates.length);
            assertEquals(153.96200561523438, coordinates[0].getLon(), 1e-8);
            assertEquals(-66.73899841308594, coordinates[0].getLat(), 1e-8);

            assertEquals(83.5780029296875, coordinates[23].getLon(), 1e-8);
            assertEquals(-16.940000534057617, coordinates[23].getLat(), 1e-8);

            final TimeAxis[] timeAxes = acquisitionInfo.getTimeAxes();
            assertEquals(2, timeAxes.length);
            coordinates = geometries[0].getCoordinates();
            Date time = timeAxes[0].getTime(coordinates[0]);
            TestUtil.assertCorrectUTCDate(2007, 4, 1, 8, 4, 13, 616, time);

            coordinates = geometries[1].getCoordinates();
            time = timeAxes[1].getTime(coordinates[0]);
            TestUtil.assertCorrectUTCDate(2007, 4, 1, 8, 55, 11, 500, time);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testGetTimeLocator_NOAA17() throws IOException {
        final File file = createAvhrrNOAA17Path();

        try {
            reader.open(file);
            final TimeLocator timeLocator = reader.getTimeLocator();
            assertNotNull(timeLocator);

            final long referenceTime = 1175398494000L;
            assertEquals(referenceTime, timeLocator.getTimeFor(167, 0));
            assertEquals(referenceTime + 6500, timeLocator.getTimeFor(168, 13));
            assertEquals(referenceTime + 507000, timeLocator.getTimeFor(169, 1014));
            assertEquals(referenceTime + 1007501, timeLocator.getTimeFor(170, 2015));
            assertEquals(referenceTime + 6834501, timeLocator.getTimeFor(171, 13669));
        } finally {
            reader.close();
        }
    }

    @Test
    public void testGetTimeLocator_NOAA18() throws IOException {
        final File file = createAvhrrNOAA18Path();

        try {
            reader.open(file);
            final TimeLocator timeLocator = reader.getTimeLocator();
            assertNotNull(timeLocator);

            final long referenceTime = 1175414652000L;
            assertEquals(referenceTime, timeLocator.getTimeFor(301, 0));
            assertEquals(referenceTime + 7000, timeLocator.getTimeFor(303, 14));
            assertEquals(referenceTime + 1007501, timeLocator.getTimeFor(303, 2015));
            assertEquals(referenceTime + 2007999, timeLocator.getTimeFor(304, 4016));
            assertEquals(referenceTime + 6118997, timeLocator.getTimeFor(171, 12238));
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadRaw_windowCenter() throws Exception {
        final File file = createAvhrrNOAA18Path();
        reader.open(file);
        try {
            final Array array = reader.readRaw(4, 4, new Interval(3, 3), "lon");
            assertNotNull(array);
            assertEquals(9, array.getSize());

            assertEqualDoubleValueFromArray(-152.41099548339844, 0, 0, array);
            assertEqualDoubleValueFromArray(-151.1510009765625, 1, 0, array);
            assertEqualDoubleValueFromArray(-149.8489990234375, 2, 0, array);
            assertEqualDoubleValueFromArray(-152.1490020751953, 0, 1, array);
            assertEqualDoubleValueFromArray(-150.88499450683594, 1, 1, array);
            assertEqualDoubleValueFromArray(-149.57899475097656, 2, 1, array);
            assertEqualDoubleValueFromArray(-151.88999938964844, 0, 2, array);
            assertEqualDoubleValueFromArray(-150.62100219726562, 1, 2, array);
            assertEqualDoubleValueFromArray(-149.31100463867188, 2, 2, array);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadRaw_bottomWindowOut() throws Exception {
        final File avhrrNOAA18Path = createAvhrrNOAA18Path();
        reader.open(avhrrNOAA18Path);

        try {
            final Array array = reader.readRaw(5, 12235, new Interval(3, 13), "lat");
            assertNotNull(array);
            assertEquals(39, array.getSize());

            assertEqualDoubleValueFromArray(85.36900329589844, 0, 0, array);
            assertEqualDoubleValueFromArray(85.53700256347656, 1, 0, array);
            assertEqualDoubleValueFromArray(85.697998046875, 2, 0, array);
            assertEqualDoubleValueFromArray(85.50499725341797, 1, 8, array);
            assertEqualDoubleValueFromArray(85.48999786376953, 1, 9, array);
            assertEqualDoubleValueFromArray(-32768.0, 1, 10, array);
            assertEqualDoubleValueFromArray(-32768.0, 1, 11, array);
            assertEqualDoubleValueFromArray(-32768.0, 1, 12, array);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadRaw_topWindowOut() throws Exception {
        final File avhrrNOAA18Path = createAvhrrNOAA18Path();
        reader.open(avhrrNOAA18Path);

        try {
            final Array array = reader.readRaw(2, 1, new Interval(3, 5), "relative_azimuth_angle");
            assertNotNull(array);
            assertEquals(15, array.getSize());

            assertEqualDoubleValueFromArray(-32768.0, 0, 0, array);
            assertEqualDoubleValueFromArray(-32768.0, 1, 0, array);
            assertEqualDoubleValueFromArray(-32768.0, 2, 0, array);

            assertEqualDoubleValueFromArray(6683.0, 0, 1, array);
            assertEqualDoubleValueFromArray(6682.0, 1, 1, array);
            assertEqualDoubleValueFromArray(6682.0, 2, 1, array);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadRaw_leftWindowOut() throws Exception {
        final File avhrrNOAA18Path = createAvhrrNOAA18Path();
        reader.open(avhrrNOAA18Path);

        try {
            final Array array = reader.readRaw(2, 117, new Interval(9, 9), "ch1");
            assertNotNull(array);
            assertEquals(81, array.getSize());

            assertEqualDoubleValueFromArray(-32768.0, 0, 0, array);
            assertEqualDoubleValueFromArray(-32768.0, 1, 0, array);
            assertEqualDoubleValueFromArray(14.0, 2, 0, array);
            assertEqualDoubleValueFromArray(14.0, 3, 0, array);
            assertEqualDoubleValueFromArray(19.0, 4, 0, array);

            assertEqualDoubleValueFromArray(-32768.0, 0, 7, array);
            assertEqualDoubleValueFromArray(-32768.0, 1, 7, array);
            assertEqualDoubleValueFromArray(14.0, 2, 7, array);
            assertEqualDoubleValueFromArray(14.0, 3, 7, array);
            assertEqualDoubleValueFromArray(19.0, 4, 7, array);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadRaw_rightWindowOut() throws Exception {
        final File avhrrNOAA18Path = createAvhrrNOAA18Path();
        reader.open(avhrrNOAA18Path);

        try {
            final Array array = reader.readRaw(407, 240, new Interval(9, 9), "ch2");
            assertNotNull(array);
            assertEquals(81, array.getSize());

            assertEqualDoubleValueFromArray(435, 4, 0, array);
            assertEqualDoubleValueFromArray(369, 5, 0, array);
            assertEqualDoubleValueFromArray(-32768, 6, 0, array);
            assertEqualDoubleValueFromArray(-32768, 7, 0, array);
            assertEqualDoubleValueFromArray(-32768, 8, 0, array);

            assertEqualDoubleValueFromArray(405, 4, 7, array);
            assertEqualDoubleValueFromArray(393, 5, 7, array);
            assertEqualDoubleValueFromArray(-32768, 6, 7, array);
            assertEqualDoubleValueFromArray(-32768, 7, 7, array);
            assertEqualDoubleValueFromArray(-32768, 8, 7, array);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadRaw_topLeftWindowOut() throws Exception {
        final File avhrrNOAA18Path = createAvhrrNOAA18Path();
        reader.open(avhrrNOAA18Path);

        try {
            final Array array = reader.readRaw(2, 3, new Interval(9, 9), "cloud_mask");
            assertNotNull(array);
            assertEquals(81, array.getSize());

            assertEqualDoubleValueFromArray(-128.0, 0, 0, array);
            assertEqualDoubleValueFromArray(-128.0, 4, 0, array);
            assertEqualDoubleValueFromArray(-128.0, 8, 0, array);

            assertEqualDoubleValueFromArray(-128.0, 0, 1, array);
            assertEqualDoubleValueFromArray(-128.0, 1, 1, array);
            assertEqualDoubleValueFromArray(7.0, 2, 1, array);

            assertEqualDoubleValueFromArray(-128.0, 0, 2, array);
            assertEqualDoubleValueFromArray(-128.0, 1, 2, array);
            assertEqualDoubleValueFromArray(7.0, 2, 2, array);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadRaw_bottomRightWindowOut() throws Exception {
        final File avhrrNOAA18Path = createAvhrrNOAA18Path();
        reader.open(avhrrNOAA18Path);

        try {
            Array array = reader.readRaw(405, 12235, new Interval(9, 9), "dtime");
            assertNotNull(array);

            assertEqualDoubleValueFromArray(6118.99658203125, 5, 7, array);
            assertEqualDoubleValueFromArray(6118.99658203125, 6, 7, array);
            assertEqualDoubleValueFromArray(6118.99658203125, 7, 7, array);
            assertEqualDoubleValueFromArray(Float.MIN_VALUE, 8, 7, array);

            assertEqualDoubleValueFromArray(Float.MIN_VALUE, 5, 8, array);
            assertEqualDoubleValueFromArray(Float.MIN_VALUE, 6, 8, array);
            assertEqualDoubleValueFromArray(Float.MIN_VALUE, 7, 8, array);
            assertEqualDoubleValueFromArray(Float.MIN_VALUE, 8, 8, array);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadRaw_bottomLeftWindowOut() throws Exception {
        final File avhrrNOAA18Path = createAvhrrNOAA18Path();
        reader.open(avhrrNOAA18Path);

        try {
            Array array = reader.readRaw(2, 12235, new Interval(9, 9), "qual_flags");
            assertNotNull(array);

            assertEqualDoubleValueFromArray(-128.0, 0, 0, array);
            assertEqualDoubleValueFromArray(-128.0, 1, 0, array);
            assertEqualDoubleValueFromArray(17.0, 2, 0, array);
            assertEqualDoubleValueFromArray(17.0, 3, 0, array);

            assertEqualDoubleValueFromArray(-128.0, 0, 7, array);
            assertEqualDoubleValueFromArray(-128.0, 1, 7, array);
            assertEqualDoubleValueFromArray(18.0, 2, 7, array);
            assertEqualDoubleValueFromArray(18.0, 3, 7, array);

            assertEqualDoubleValueFromArray(-128.0, 0, 8, array);
            assertEqualDoubleValueFromArray(-128.0, 1, 8, array);
            assertEqualDoubleValueFromArray(-128.0, 2, 8, array);
            assertEqualDoubleValueFromArray(-128.0, 3, 8, array);
        } finally {
            reader.close();
        }
    }

    @Test
    public void testReadRaw_topRightWindowOut() throws Exception {
        final File avhrrNOAA18Path = createAvhrrNOAA18Path();
        reader.open(avhrrNOAA18Path);

        try {
            Array array = reader.readRaw(407, 3, new Interval(9, 9), "relative_azimuth_angle");
            assertNotNull(array);

            assertEqualDoubleValueFromArray(-32768.0, 4, 0, array);
            assertEqualDoubleValueFromArray(-32768.0, 5, 0, array);
            assertEqualDoubleValueFromArray(-32768.0, 6, 0, array);
            assertEqualDoubleValueFromArray(-32768.0, 7, 0, array);
            assertEqualDoubleValueFromArray(-32768.0, 8, 0, array);

            assertEqualDoubleValueFromArray(-11094.0, 4, 1, array);
            assertEqualDoubleValueFromArray(-11089.0, 5, 1, array);
            assertEqualDoubleValueFromArray(-32768.0, 6, 1, array);
            assertEqualDoubleValueFromArray(-32768.0, 7, 1, array);
            assertEqualDoubleValueFromArray(-32768.0, 8, 1, array);

            assertEqualDoubleValueFromArray(-11101.0, 4, 8, array);
            assertEqualDoubleValueFromArray(-11098.0, 5, 8, array);
            assertEqualDoubleValueFromArray(-32768.0, 6, 8, array);
            assertEqualDoubleValueFromArray(-32768.0, 7, 8, array);
            assertEqualDoubleValueFromArray(-32768.0, 8, 8, array);
        } finally {
            reader.close();
        }
    }

    private File createAvhrrNOAA17Path() {
        final String testFilePath = TestUtil.assembleFileSystemPath(new String[]{"avhrr-n17", "1.01", "2007", "04", "01", "20070401033400-ESACCI-L1C-AVHRR17_G-fv01.0.nc"}, false);
        final File file = new File(testDataDirectory, testFilePath);
        assertTrue(file.isFile());
        return file;
    }

    private File createAvhrrNOAA18Path() {
        final String testFilePath = TestUtil.assembleFileSystemPath(new String[]{"avhrr-n18", "1.02", "2007", "04", "01", "20070401080400-ESACCI-L1C-AVHRR18_G-fv01.0.nc"}, false);
        final File file = new File(testDataDirectory, testFilePath);
        assertTrue(file.isFile());
        return file;
    }

    private void assertEqualDoubleValueFromArray(double expected, int x, int y, Array array) {
        final Index index = array.getIndex();
        index.set(y, x);
        assertEquals(expected, array.getDouble(index), 1e-8);
    }
}
