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

package com.bc.fiduceo.matchup.writer;

import com.bc.fiduceo.IOTestRunner;
import com.bc.fiduceo.TestUtil;
import com.bc.fiduceo.util.TimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(IOTestRunner.class)
public class MmdWriter_IO_Test {

    private File testDir;

    @Before
    public void setUp() throws Exception {
        testDir = TestUtil.createTestDirectory();
    }

    @After
    public void tearDown() throws Exception {
        TestUtil.deleteTestDirectory();
    }

    @Test
    public void testCreate() throws IOException {
        final MmdWriter mmdWriter = new MmdWriter();
        final File mmdFile = new File(testDir, "test_mmd.nc");

        try {
            mmdWriter.create(mmdFile);
        } finally {
            mmdWriter.close();
        }

        assertTrue(mmdFile.isFile());

        NetcdfFile mmd = null;
        try {
            mmd = NetcdfFile.open(mmdFile.getPath());

            assertGlobalAttribute("title", "FIDUCEO multi-sensor match-up dataset (MMD)", mmd);
            assertGlobalAttribute("institution", "Brockmann Consult GmbH", mmd);
            assertGlobalAttribute("contact", "Tom Block (tom.block@brockmann-consult.de)", mmd);
            assertGlobalAttribute("license", "This dataset is released for use under CC-BY licence and was developed in the EC FIDUCEO project \"Fidelity and Uncertainty in Climate Data Records from Earth Observations\". Grant Agreement: 638822.", mmd);

            assertGlobalDateAttribute("creation_date", TimeUtils.createNow(), mmd);
        } finally {
            if (mmd != null) {
                mmd.close();
            }
        }
    }

    private void assertGlobalDateAttribute(String name, Date expected, NetcdfFile mmd) {
        final Attribute creation_date = mmd.findGlobalAttribute(name);
        assertNotNull(creation_date);
        final String dateStringValue = creation_date.getStringValue();
        final Date actual = TimeUtils.parse(dateStringValue, "yyyy-MM-dd hh:mm:ss");
        TestUtil.assertWithinLastMinute(expected, actual);
    }

    private void assertGlobalAttribute(String name, String value, NetcdfFile mmd) {
        Attribute globalAttribute = mmd.findGlobalAttribute(name);
        assertNotNull(globalAttribute);
        assertEquals(value, globalAttribute.getStringValue());
    }
}
