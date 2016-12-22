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

package com.bc.fiduceo.post.plugin.nwp;


import com.bc.fiduceo.TestUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class NwpPostProcessingPluginTest {

    @Test
    public void testCreateConfiguration_deleteOnExit() throws JDOMException, IOException {
        final String XML = "<nwp>" +
                "    <delete-on-exit>false</delete-on-exit>" +
                "    <cdo-home>we need this, its mandatory</cdo-home>" +
                "</nwp>";
        final Element rootElement = TestUtil.createDomElement(XML);

        final Configuration configuration = NwpPostProcessingPlugin.createConfiguration(rootElement);
        assertFalse(configuration.isDeleteOnExit());
    }

    @Test
    public void testCreateConfiguration_cdoHome() throws JDOMException, IOException {
        final String XML = "<nwp>" +
                "    <cdo-home>/in/this/directory</cdo-home>" +
                "</nwp>";
        final Element rootElement = TestUtil.createDomElement(XML);

        final Configuration configuration = NwpPostProcessingPlugin.createConfiguration(rootElement);
        assertEquals("/in/this/directory", configuration.getCDOHome());
    }

    @Test
    public void testCreateConfiguration_missing_cdoHome() throws JDOMException, IOException {
        final String XML = "<nwp>" +
                "</nwp>";
        final Element rootElement = TestUtil.createDomElement(XML);

        try {
            NwpPostProcessingPlugin.createConfiguration(rootElement);
            fail("RuntimeException expected");
        } catch (RuntimeException expected) {
        }
    }

    @Test
    public void testCreateConfiguration_analysisSteps() throws JDOMException, IOException {
        final String XML = "<nwp>" +
                "    <analysis-steps>19</analysis-steps>" +
                "    <cdo-home>we need this, its mandatory</cdo-home>" +
                "</nwp>";
        final Element rootElement = TestUtil.createDomElement(XML);

        final Configuration configuration = NwpPostProcessingPlugin.createConfiguration(rootElement);
        assertEquals(19, configuration.getAnalysisSteps());
    }

    @Test
    public void testCreateConfiguration_forecastSteps() throws JDOMException, IOException {
        final String XML = "<nwp>" +
                "    <forecast-steps>27</forecast-steps>" +
                "    <cdo-home>we need this, its mandatory</cdo-home>" +
                "</nwp>";
        final Element rootElement = TestUtil.createDomElement(XML);

        final Configuration configuration = NwpPostProcessingPlugin.createConfiguration(rootElement);
        assertEquals(27, configuration.getForecastSteps());
    }
}
