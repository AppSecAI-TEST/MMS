/*
 * Copyright (C) 2017 Brockmann Consult GmbH
 * This code was developed for the EC project "Fidelity and Uncertainty in
 * Climate Data Records from Earth Observations (FIDUCEO)".
 * Grant Agreement: 638822
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  more details.
 *
 *  A copy of the GNU General Public License should have been supplied along
 *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package com.bc.fiduceo.post.plugin.nwp;

import com.bc.fiduceo.IOTestRunner;
import com.bc.fiduceo.NCTestUtils;
import com.bc.fiduceo.TestUtil;
import com.bc.fiduceo.util.NetCDFUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.*;

@RunWith(IOTestRunner.class)
public class FileMerger_IO_Test {

    private File testDataDirectory;
    private NetcdfFile netcdfFile;
    private NetcdfFileWriter netcdfFileWriter;
    private Configuration configuration;
    private TemplateVariables templateVariables;

    @Before
    public void setUp() throws IOException, InvalidRangeException {
        // @todo 2 tb/tb clean up this messy set-up 2017-02-23
        final String tempDirPath = System.getProperty("java.io.tmpdir");
        final File tempDir = new File(tempDirPath);
        testDataDirectory = TestUtil.getTestDataDirectory();

        final String mmd06Path = TestUtil.assembleFileSystemPath(new String[]{testDataDirectory.getAbsolutePath(), "post-processing", "mmd06c", "animal-sst_amsre-aq", "mmd6c_sst_animal-sst_amsre-aq_2004-008_2004-014.nc"}, true);
        final File mmd06OriginalFile = new File(mmd06Path);
        assertTrue(mmd06OriginalFile.isFile());
        final File mmd06File = TestUtil.copyFileDir(mmd06OriginalFile, tempDir);
        final File renamedFile = new File(mmd06File.getParent(), new Date().getTime() + mmd06File.getName());
        if (!mmd06File.renameTo(renamedFile)) {
            fail("unable to rename file");
        }
        renamedFile.deleteOnExit();
        mmd06File.deleteOnExit();

        netcdfFile = NetCDFUtils.openReadOnly(mmd06OriginalFile.getAbsolutePath());
        netcdfFileWriter = NetcdfFileWriter.openExisting(renamedFile.getAbsolutePath());
        netcdfFileWriter.setRedefineMode(true);

        configuration = new Configuration();
        final TimeSeriesConfiguration timeSeriesConfiguration = new TimeSeriesConfiguration();
        timeSeriesConfiguration.setTimeVariableName("animal-sst_acquisition_time");
        configuration.setTimeSeriesConfiguration(timeSeriesConfiguration);

        final SensorExtractConfiguration sensorExtractConfiguration = new SensorExtractConfiguration();
        sensorExtractConfiguration.setTimeVariableName("amsre.acquisition_time");
        sensorExtractConfiguration.setX_Dimension(5);
        sensorExtractConfiguration.setX_DimensionName("amsre.nwp.nx");
        sensorExtractConfiguration.setY_Dimension(5);
        sensorExtractConfiguration.setY_DimensionName("amsre.nwp.ny");
        sensorExtractConfiguration.setZ_Dimension(60);
        sensorExtractConfiguration.setZ_DimensionName("amsre.nwp.nz");
        configuration.setSensorExtractConfiguration(sensorExtractConfiguration);

        templateVariables = new TemplateVariables(configuration);

        final NwpPostProcessing postProcessing = new NwpPostProcessing(configuration);
        postProcessing.prepare(netcdfFile, netcdfFileWriter);

        netcdfFileWriter.setRedefineMode(false);
    }

    @After
    public void tearDown() throws IOException {
        if (netcdfFile != null) {
            netcdfFile.close();
        }
        if (netcdfFileWriter != null) {
            netcdfFileWriter.close();
        }
    }

    @Test
    public void testMergeTimeSeriesAnalysisFile_MMD6() throws IOException, InvalidRangeException {
        final String analysisPath = TestUtil.assembleFileSystemPath(new String[]{testDataDirectory.getAbsolutePath(), "post-processing", "nwp_preprocessed", "analysis8419652569230325841.nc"}, true);
        final File analysisFile = new File(analysisPath);
        assertTrue(analysisFile.isFile());

        final FileMerger fileMerger = new FileMerger(configuration, templateVariables);

        try (NetcdfFile analysis = NetcdfFile.open(analysisFile.getAbsolutePath())) {
            final int[] centerTimes = fileMerger.mergeTimeSeriesAnalysisFile(netcdfFileWriter, analysis);
            assertEquals(9, centerTimes.length);
            assertEquals(1073692800, centerTimes[0]);
            assertEquals(1074103200, centerTimes[7]);

            netcdfFileWriter.flush();

            final NetcdfFile netcdfFile = netcdfFileWriter.getNetcdfFile();
            NCTestUtils.assert2DVariable("matchup.nwp.an.sea_ice_fraction", 0, 0, 0.0, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.an.sea_surface_temperature", 1, 1, 293.1509094238281, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.an.10m_east_wind_component", 2, 2, 3.2551252841949463, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.an.10m_north_wind_component", 3, 3, 4.974652290344238, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.an.total_column_water_vapour", 4, 4, 43.44807815551758, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.an.cloud_liquid_water_content", 5, 5, 0.0, netcdfFile);
        }
    }

    @Test
    public void testMergeTimeSeriesForecastFile_MMD6() throws IOException, InvalidRangeException {
        final String forecastPath = TestUtil.assembleFileSystemPath(new String[]{testDataDirectory.getAbsolutePath(), "post-processing", "nwp_preprocessed", "forecast6320193562301902094.nc"}, true);
        final File forecastFile = new File(forecastPath);
        assertTrue(forecastFile.isFile());

        final FileMerger fileMerger = new FileMerger(configuration, templateVariables);

        try (NetcdfFile analysis = NetcdfFile.open(forecastFile.getAbsolutePath())) {
            final int[] centerTimes = fileMerger.mergeForecastFile(netcdfFileWriter, analysis);
            assertEquals(9, centerTimes.length);
            assertEquals(1073692800, centerTimes[0]);
            assertEquals(1074092400, centerTimes[7]);

            netcdfFileWriter.flush();

            final NetcdfFile netcdfFile = netcdfFileWriter.getNetcdfFile();
            NCTestUtils.assert2DVariable("matchup.nwp.fc.sea_surface_temperature", 0, 0, 291.5697937011719, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.surface_sensible_heat_flux", 1, 1, 240214.265625, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.surface_latent_heat_flux", 2, 2, -662554.375, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.mean_sea_level_pressure", 3, 3, 100859.28125, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.boundary_layer_height", 4, 4, 395.1171875, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.10m_east_wind_component", 5, 5, -4.530746936798096, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.10m_north_wind_component", 6, 6, 5.478193283081055, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.2m_temperature", 7, 7, 291.12896728515625, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.2m_dew_point", 8, 8, 287.94354248046875, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.downward_surface_solar_radiation", 9, 0, 8987282.0, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.downward_surface_thermal_radiation", 10, 1, 1.3298496E7, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.surface_solar_radiation", 11, 2, 1.8746058E7, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.surface_thermal_radiation", 12, 3, -775141.375, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.turbulent_stress_east_component", 13, 4, -1091.51611328125, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.turbulent_stress_north_component", 14, 5, -5850.00830078125, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.evaporation", 15, 6, -4.936744808219373E-4, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.total_precipitation", 16, 7, 0.0037760320119559765, netcdfFile);
            NCTestUtils.assert2DVariable("matchup.nwp.fc.total_column_water_vapour", 17, 8, 48.54024124145508, netcdfFile);
        }
    }

    @Test
    public void testMergeSensorExtractAnalysisFile_MMD6() throws IOException, InvalidRangeException {
        final String analysisPath = TestUtil.assembleFileSystemPath(new String[]{testDataDirectory.getAbsolutePath(), "post-processing", "nwp_preprocessed", "analysis5081985403420613282.nc"}, true);
        final File analysisFile = new File(analysisPath);
        assertTrue(analysisFile.isFile());

        final FileMerger fileMerger = new FileMerger(configuration, templateVariables);

        try (NetcdfFile analysis = NetcdfFile.open(analysisFile.getAbsolutePath())) {
            fileMerger.mergeSensorExtractAnalysisFile(netcdfFileWriter, analysis);

            netcdfFileWriter.flush();

            final NetcdfFile netcdfFile = netcdfFileWriter.getNetcdfFile();
            NCTestUtils.assert3DVariable("amsre.nwp.10m_east_wind_component", 0, 0, 0, 4.763412952423096, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.10m_north_wind_component", 1, 0, 1, -9.792949676513672, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.2m_dew_point", 2, 0, 2, 277.72613525390625, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.2m_temperature", 3, 0, 3, 277.7873840332031, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.albedo", 4, 0, 4, 0.06999999, netcdfFile);

            NCTestUtils.assert4DVariable("amsre.nwp.cloud_ice_water", 0, 1, 5, 0, 0.0, netcdfFile);
            NCTestUtils.assert4DVariable("amsre.nwp.cloud_liquid_water", 1, 1, 6, 1, 0.0, netcdfFile);

            NCTestUtils.assert3DVariable("amsre.nwp.log_surface_pressure", 2, 1, 7, 11.511722564697266, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.mean_sea_level_pressure", 3, 1, 8, 99756.4765625, netcdfFile);

            NCTestUtils.assert4DVariable("amsre.nwp.ozone_profile", 4, 1, 0, 2, 1.1798357490988565E-6, netcdfFile);

            NCTestUtils.assert3DVariable("amsre.nwp.sea_surface_temperature", 0, 2, 1, 276.1256103515625, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.seaice_fraction", 1, 2, 2, 0.0, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.skin_temperature", 2, 2, 3, 276.2798767089844, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.snow_albedo", 3, 2, 4, 0.8499984741210938, netcdfFile);

            NCTestUtils.assert4DVariable("amsre.nwp.temperature_profile", 4, 2, 5, 3, 276.8033447265625, netcdfFile);
            NCTestUtils.assert4DVariable("amsre.nwp.water_vapour_profile", 0, 3, 6, 4, 3.81598465537536E-6, netcdfFile);

            NCTestUtils.assert3DVariable("amsre.nwp.total_cloud_cover", 1, 3, 7, 1.0, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.total_column_water_vapour", 2, 3, 8, 18.6469783782959, netcdfFile);
            NCTestUtils.assert3DVariable("amsre.nwp.total_precip", 3, 3, 0, 2.6574492221698165E-4, netcdfFile);
        }
    }
}
