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

package com.bc.fiduceo.reader.iasi;

import com.bc.fiduceo.core.Dimension;
import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.GeometryFactory;
import com.bc.fiduceo.geometry.Polygon;
import com.bc.fiduceo.location.PixelLocator;
import com.bc.fiduceo.reader.*;
import com.bc.fiduceo.util.NetCDFUtils;
import ucar.ma2.*;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class IASI_Reader implements Reader {

    private static final String REG_EX = "IASI_xxx_1C_M0[1-3]_\\d{14}Z_\\d{14}Z_\\w_\\w_\\d{14}Z.nat";

    private static final int SNOT = 30;
    private static final int LON = 0;
    private static final int LAT = 1;

    private static final int G_EPS_DAT_IASI_OFFSET = 9122;
    private static final int G_GEO_SOND_LOC_OFFSET = 255893;

    private ImageInputStream iis;
    private GenericRecordHeader mphrHeader;
    private MainProductHeaderRecord mainProductHeaderRecord;
    private GiadrScaleFactors giadrScaleFactors;
    private IASI_TimeLocator timeLocator;
    private GeolocationData geolocationData;
    private IASI_PixelLocator pixelLocator;

    private final GeometryFactory geometryFactory;

    private long firstMdrOffset;
    private long mdrSize;
    private int mdrCount;
    private MDRCache mdrCache;
    private List<Variable> variableList;
    private HashMap<String, ReadProxy> proxiesMap;

    IASI_Reader(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
        iis = null;
    }

    @Override
    public void open(File file) throws IOException {
        if (iis != null) {
            throw new RuntimeException("Stream already opened");
        }

        iis = new FileImageInputStream(file);

        readHeader();
        mdrCache = new MDRCache(iis, firstMdrOffset);
        // @todo 3 move this to a factory when we extend the reader to support older/newer MDR versions
        proxiesMap = MDR_1C.getReadProxies();
    }

    @Override
    public void close() throws IOException {
        timeLocator = null;
        if (iis != null) {
            iis.close();
            iis = null;
        }
    }

    @Override
    public AcquisitionInfo read() throws IOException {
        final AcquisitionInfo acquisitionInfo = new AcquisitionInfo();

        acquisitionInfo.setSensingStart(mphrHeader.recordStartTime.getAsDate());
        acquisitionInfo.setSensingStop(mphrHeader.recordEndTime.getAsDate());

        acquisitionInfo.setNodeType(NodeType.UNDEFINED);

        final Geometries geometries = createGeometries();

        acquisitionInfo.setBoundingGeometry(geometries.getBoundingGeometry());
        ReaderUtils.setTimeAxes(acquisitionInfo, geometries, geometryFactory);

        return acquisitionInfo;
    }

    @Override
    public PixelLocator getPixelLocator() throws IOException {
        return getPixelLocator_internal();
    }

    @Override
    public String getRegEx() {
        return REG_EX;
    }

    @Override
    public PixelLocator getSubScenePixelLocator(Polygon sceneIndex) throws IOException {
        // @todo 1 tb/tb implement correct sub-scene locators here 2015-05-15
        return getPixelLocator_internal();
    }

    @Override
    public TimeLocator getTimeLocator() throws IOException {
        if (timeLocator == null) {
            final long[][] timeArray = readGEPSDatIasi();
            timeLocator = new IASI_TimeLocator(timeArray);
        }
        return timeLocator;
    }

    @Override
    public Array readRaw(int centerX, int centerY, Interval interval, String variableName) throws IOException, InvalidRangeException {
        final MDR_1C[] mdRs = getMDRs(centerY, interval.getY());
        final int xOffset = centerX - interval.getX() / 2;
        final int yOffset = centerY - interval.getY() / 2;
        final int[] shape = new int[]{interval.getY(), interval.getX()};
        final Dimension productSize = getProductSize();

        final ReadProxy readProxy = proxiesMap.get(variableName);
        final Number fillValue = NetCDFUtils.getDefaultFillValue(readProxy.getDataType());
        final Array array = Array.factory(readProxy.getDataType(), shape);

        final Index index = array.getIndex();
        for (int y = 0; y < interval.getY(); y++) {
            final int line = yOffset + y;

            for (int x = 0; x < interval.getX(); x++) {
                index.set(y, x);
                final int xPosition = xOffset + x;
                final Object data;
                if (line < 0 || line >= productSize.getNy() || xPosition < 0 || xPosition >= productSize.getNx()) {
                    data = fillValue;
                } else {
                    data = readProxy.read(xPosition, line % 2, mdRs[y]);
                }
                array.setObject(index, data);
            }
        }

        return array;
    }

    @Override
    public Array readScaled(int centerX, int centerY, Interval interval, String variableName) throws IOException, InvalidRangeException {
        final ReadProxy readProxy = proxiesMap.get(variableName);

        Array array = readRaw(centerX, centerY, interval, variableName);
        final double scaleFactor = readProxy.getScaleFactor();
        if (!Double.isNaN(scaleFactor)) {
            final MAMath.ScaleOffset scaleOffset = new MAMath.ScaleOffset(scaleFactor, 0.0);
            array = MAMath.convert2Unpacked(array, scaleOffset);
        }
        return array;
    }

    @Override
    public ArrayInt.D2 readAcquisitionTime(int x, int y, Interval interval) throws IOException, InvalidRangeException {
        final Array timeInMillis = readRaw(x, y, interval, "GEPSDatIasi");
        final Array timeInSeconds = Array.factory(int.class, timeInMillis.getShape());
        final long size = timeInMillis.getSize();
        for (int i = 0; i < size; i++) {
            final long millis = timeInMillis.getLong(i);
            final int seconds = (int) Math.round(millis * 0.001);
            timeInSeconds.setInt(i, seconds);

        }
        return (ArrayInt.D2) timeInSeconds;
    }

    @Override
    public List<Variable> getVariables() {
        if (variableList == null) {
            variableList = createVariableList();
        }
        return variableList;
    }

    private PixelLocator getPixelLocator_internal() throws IOException {
        if (pixelLocator == null) {
            final GeolocationData geolocationData = getGeolocationData();

            pixelLocator = new IASI_PixelLocator(geolocationData, geometryFactory);
        }
        return pixelLocator;
    }

    private void readHeader() throws IOException {
        mphrHeader = GenericRecordHeader.readGenericRecordHeader(iis);
        if (mphrHeader.recordClass != RecordClass.MPHR
                || mphrHeader.instrumentGroup != InstrumentGroup.GENERIC
                || mphrHeader.recordSubclass != 0) {
            throw new IOException("Illegal Main Product Header Record");
        }

        mainProductHeaderRecord = new MainProductHeaderRecord();
        mainProductHeaderRecord.readRecord(iis);

        final List<InternalPointerRecord> iprList = readInternalPointerRecordList();

        for (final InternalPointerRecord ipr : iprList) {
            if (ipr.targetRecordClass == RecordClass.GIADR) {
                if (ipr.targetRecordSubclass == 0) {
                    iis.seek(ipr.targetRecordOffset);
                    GiadrQuality giadrQuality = new GiadrQuality();
                    giadrQuality.readRecord(iis);
                } else if (ipr.targetRecordSubclass == 1) {
                    iis.seek(ipr.targetRecordOffset);
                    giadrScaleFactors = new GiadrScaleFactors();
                    giadrScaleFactors.readRecord(iis);
                }
            } else if (ipr.targetRecordClass == RecordClass.MDR) {
                firstMdrOffset = ipr.targetRecordOffset;
            }
        }

        determineMdrParameter(iis);
    }

    private List<InternalPointerRecord> readInternalPointerRecordList() throws IOException {
        final List<InternalPointerRecord> iprList = new ArrayList<>();
        for (; ; ) {
            final InternalPointerRecord ipr = InternalPointerRecord.readInternalPointerRecord(iis);
            iprList.add(ipr);
            if (ipr.targetRecordClass == RecordClass.MDR) {
                break;
            }
        }
        return iprList;
    }

    private void determineMdrParameter(ImageInputStream iis) throws IOException {
        iis.seek(firstMdrOffset);
        final GenericRecordHeader mdrHeader = GenericRecordHeader.readGenericRecordHeader(iis);

        checkRecordSubClass(mdrHeader);

        mdrSize = mdrHeader.recordSize;
        mdrCount = (int) ((iis.length() - firstMdrOffset) / mdrSize);
    }

    private void checkRecordSubClass(GenericRecordHeader mdrHeader) {
        if (mdrHeader.recordSubclassVersion != 5) {
            throw new RuntimeException("Unsupported processing version");
        }
    }

    private long[][] readGEPSDatIasi() throws IOException {
        final long[][] data = new long[mdrCount][];

        for (int i = 0; i < mdrCount; i++) {
            data[i] = readGEPSDatIasiMdr(i);
        }

        return data;
    }

    private long[] readGEPSDatIasiMdr(int mdrIndex) throws IOException {
        final long[] data = new long[SNOT];
        final long mdrOffset = getMdrOffset(mdrIndex);

        iis.seek(mdrOffset + G_EPS_DAT_IASI_OFFSET);

        for (int j = 0; j < SNOT; j++) {
            data[j] = EpsMetopUtil.readShortCdsTime(iis).getAsCalendar().getTimeInMillis();
        }
        return data;
    }

    private float[][][][] readGGeoSondLoc() throws IOException {
        final float[][][][] data = new float[mdrCount][SNOT][EpsMetopConstants.PN][2];
        final int[] mdrBlock = new int[SNOT * EpsMetopConstants.PN * 2];

        for (int mdrIndex = 0; mdrIndex < mdrCount; mdrIndex++) {
            final long mdrOffset = getMdrOffset(mdrIndex);
            final float[][][] scanLineData = data[mdrIndex];

            iis.seek(mdrOffset + G_GEO_SOND_LOC_OFFSET);
            iis.readFully(mdrBlock, 0, mdrBlock.length);

            for (int i = 0, j = 0; j < SNOT; j++) {
                final float[][] efovData = scanLineData[j];
                for (int k = 0; k < EpsMetopConstants.PN; k++) {
                    efovData[k][LON] = mdrBlock[i++] * EpsMetopConstants.G_GEO_SOND_LOC_SCALING_FACTOR;
                    efovData[k][LAT] = mdrBlock[i++] * EpsMetopConstants.G_GEO_SOND_LOC_SCALING_FACTOR;
                }
            }
        }

        return data;
    }

    private long getMdrOffset(int i) {
        return firstMdrOffset + (i * mdrSize);
    }

    private Geometries createGeometries() throws IOException {
        final Geometries geometries = new Geometries();

        final GeolocationData geolocationData = getGeolocationData();

        final BoundingPolygonCreator polygonCreator = new BoundingPolygonCreator(new Interval(6, 24), geometryFactory);
        final Geometry boundingGeometry = polygonCreator.createBoundingGeometrySplitted(geolocationData.longitudes, geolocationData.latitudes, 2, true);
        if (!boundingGeometry.isValid()) {
            throw new RuntimeException("Unable to extract valid bounding geometry");
        }
        geometries.setBoundingGeometry(boundingGeometry);

        final Geometry timeAxisGeometry = polygonCreator.createTimeAxisGeometrySplitted(geolocationData.longitudes, geolocationData.latitudes, 2);
        geometries.setTimeAxesGeometry(timeAxisGeometry);
        return geometries;
    }

    private GeolocationData getGeolocationData() throws IOException {
        if (geolocationData == null) {
            geolocationData = readGeolocationData();
        }

        return geolocationData;
    }

    private MDR_1C[] getMDRs(int centerY, int windowHeight) throws IOException {
        final int lineStart = centerY - windowHeight / 2;
        final int lineEnd = centerY + windowHeight / 2;

        final MDR_1C[] mdrs = new MDR_1C[windowHeight];
        int index = 0;
        for (int line = lineStart; line <= lineEnd; line++) {
            mdrs[index] = mdrCache.getRecord(line);
            ++index;
        }

        return mdrs;
    }

    private GeolocationData readGeolocationData() throws IOException {
        final float[][] longitudes = new float[2 * mdrCount][2 * SNOT];
        final float[][] latitudes = new float[2 * mdrCount][2 * SNOT];

        final float[][][][] doubles = readGGeoSondLoc();
        for (int line = 0; line < mdrCount; line++) {
            float[][][] lineData = doubles[line];

            final int targetLine = 2 * line;
            final float[] longitudeLine_0 = longitudes[targetLine];
            final float[] longitudeLine_1 = longitudes[targetLine + 1];

            final float[] latitudeLine_0 = latitudes[targetLine];
            final float[] latitudeLine_1 = latitudes[targetLine + 1];

            for (int efov = 0; efov < SNOT; efov++) {
                final float[][] efovData = lineData[efov];

                final int pixelIndex = 2 * efov;
                longitudeLine_0[pixelIndex] = efovData[3][LON];
                longitudeLine_1[pixelIndex] = efovData[2][LON];
                longitudeLine_0[pixelIndex + 1] = efovData[0][LON];
                longitudeLine_1[pixelIndex + 1] = efovData[1][LON];

                latitudeLine_0[pixelIndex] = efovData[3][LAT];
                latitudeLine_1[pixelIndex] = efovData[2][LAT];
                latitudeLine_0[pixelIndex + 1] = efovData[0][LAT];
                latitudeLine_1[pixelIndex + 1] = efovData[1][LAT];
            }
        }

        final GeolocationData geolocationData = new GeolocationData();
        geolocationData.longitudes = Array.factory(longitudes);
        geolocationData.latitudes = Array.factory(latitudes);
        return geolocationData;
    }

    private List<Variable> createVariableList() {
        List<Variable> variableList = new ArrayList<>();
        List<Attribute> attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Quality of MDR has been degraded from nominal due to an instrument degradation"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(byte.class)));
        variableList.add(new VariableProxy("DEGRADED_INST_MDR", DataType.BYTE, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Quality of MDR has been degraded from nominal due to a processing degradation"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(byte.class)));
        variableList.add(new VariableProxy("DEGRADED_PROC_MDR", DataType.BYTE, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Instrument mode"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("GEPSIasiMode", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Processing mode"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("GEPSOPSProcessingMode", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "On Board Time (Coarse time + Fine time)"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(long.class)));
        variableList.add(new VariableProxy("OBT", DataType.LONG, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Date of IASI measure (on board UTC)"));
        attributes.add(new Attribute("units", "s"));
        attributes.add(new Attribute("long_name", "On board UTC in in milliseconds since 1970-01-01 00:00:00"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(long.class)));
        variableList.add(new VariableProxy("OnboardUTC", DataType.LONG, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Date of IASI measure (corrected UTC)"));
        attributes.add(new Attribute("units", "ms"));
        attributes.add(new Attribute("long_name", "Corrected UTC in in milliseconds since 1970-01-01 00:00:00"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(long.class)));
        variableList.add(new VariableProxy("GEPSDatIasi", DataType.LONG, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Corner Cube Direction for all observational targets."));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(byte.class)));
        variableList.add(new VariableProxy("GEPS_CCD", DataType.BYTE, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Scan position for all observational targets."));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("GEPS_SP", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Detailed quality flag for the system"));
        attributes.add(new Attribute("standard_name", "status_flag"));
        attributes.add(new Attribute("flag_masks", "1 2 4 8 16 32 64 128 256 512 1024 2048 4096"));
        attributes.add(new Attribute("flag_meanings", "hardware spikes_b1 spikes_b2 spikes_b3 nzbd_cal_error onboard_general_qual overflow_underflow spectral_calib_error rad_post_calib_error summary_flag miss_sounder_data miss_iis_data miss_avhrr_data"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(short.class)));
        variableList.add(new VariableProxy("GQisFlagQualDetailed", DataType.SHORT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "System-TEC quality index for IIS"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("GQisSysTecIISQual", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "System-TEC quality index for sounder"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("GQisSysTecSondQual", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Location of pixel centre in geodetic coordinates for each sounder pixel (lon)"));
        attributes.add(new Attribute("standard_name", "longitude"));
        attributes.add(new Attribute("units", "degrees_east"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(float.class)));
        attributes.add(new Attribute("scale_factor", 1e-6));
        variableList.add(new VariableProxy("GGeoSondLoc_Lon", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Location of pixel centre in geodetic coordinates for each sounder pixel (lat)"));
        attributes.add(new Attribute("standard_name", "latitude"));
        attributes.add(new Attribute("units", "degrees_north"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(float.class)));
        attributes.add(new Attribute("scale_factor", 1e-6));
        variableList.add(new VariableProxy("GGeoSondLoc_Lat", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Measurement angles for each sounder pixel (zenith)"));
        attributes.add(new Attribute("standard_name", "sensor_zenith_angle"));
        attributes.add(new Attribute("units", "degree"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(float.class)));
        attributes.add(new Attribute("scale_factor", 1e-6));
        variableList.add(new VariableProxy("GGeoSondAnglesMETOP_Zenith", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Measurement angles for each sounder pixel (azimuth)"));
        attributes.add(new Attribute("standard_name", "sensor_azimuth_angle"));
        attributes.add(new Attribute("units", "degree"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(float.class)));
        attributes.add(new Attribute("scale_factor", 1e-6));
        variableList.add(new VariableProxy("GGeoSondAnglesMETOP_Azimuth", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Solar angles at the surface for each sounder pixel (zenith)"));
        attributes.add(new Attribute("standard_name", "solar_zenith_angle"));
        attributes.add(new Attribute("units", "degree"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(float.class)));
        attributes.add(new Attribute("scale_factor", 1e-6));
        variableList.add(new VariableProxy("GGeoSondAnglesSUN_Zenith", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Solar angles at the surface for each sounder pixel (azimuth)"));
        attributes.add(new Attribute("standard_name", "solar_azimuth_angle"));
        attributes.add(new Attribute("units", "degree"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(float.class)));
        attributes.add(new Attribute("scale_factor", 1e-6));
        variableList.add(new VariableProxy("GGeoSondAnglesSUN_Azimuth", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Distance of satellite from Earth centre"));
        attributes.add(new Attribute("units", "m"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("EARTH_SATELLITE_DISTANCE", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Number of the first sample of IASI 1C spectra (same as 1B)"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("IDefNsfirst1b", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Number of the last sample of IASI 1C spectra (same as 1B)"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("IDefNslast1b", DataType.INT, attributes));

        // @todo 1 tb/tb reanimate 2017-05-20
//        attributes = new ArrayList<>();
//        attributes.add(new Attribute("description", "Level 1C spectra"));
//        attributes.add(new Attribute("units", "W/m2/sr/m-1"));
//        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(short.class)));
//        variableList.add(new VariableProxy("GS1cSpect", DataType.SHORT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Radiance Analysis: Number of identified classes in the sounder FOV"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("GCcsRadAnalNbClass", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Radiance Analysis: Image used is from AVHRR or IIS imager (degraded cases)"));
        attributes.add(new Attribute("standard_name", "status_flag"));
        attributes.add(new Attribute("flag_masks", "1"));
        attributes.add(new Attribute("flag_meanings", "iis_image"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(int.class)));
        variableList.add(new VariableProxy("IDefCcsMode", DataType.INT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Radiance Analysis: Number of useful lines"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(short.class)));
        variableList.add(new VariableProxy("GCcsImageClassifiedNbLin", DataType.SHORT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Radiance Analysis: Number of useful columns"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(short.class)));
        variableList.add(new VariableProxy("GCcsImageClassifiedNbCol", DataType.SHORT, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Cloud fraction in IASI FOV from AVHRR 1B in IASI FOV"));
        attributes.add(new Attribute("units", "%"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(byte.class)));
        variableList.add(new VariableProxy("GEUMAvhrr1BCldFrac", DataType.BYTE, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Land and Coast fraction in IASI FOV from AVHRR 1B"));
        attributes.add(new Attribute("units", "%"));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(byte.class)));
        variableList.add(new VariableProxy("GEUMAvhrr1BLandFrac", DataType.BYTE, attributes));

        attributes = new ArrayList<>();
        attributes.add(new Attribute("description", "Quality indicator. If the quality is good, it gives the coverage of snow/ice."));
        attributes.add(new Attribute("_FillValue", NetCDFUtils.getDefaultFillValue(byte.class)));
        variableList.add(new VariableProxy("GEUMAvhrr1BQual", DataType.BYTE, attributes));
        return variableList;
    }

    @Override
    public Dimension getProductSize() {
        final Dimension size = new Dimension();
        size.setNx(2 * SNOT);
        size.setNy(2 * mdrCount);
        return size;
    }

}