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

package com.bc.fiduceo.geometry;


import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class GeometryFactoryTest {

    @Test
    public void testCreateFromType() {
        GeometryFactory geometryFactory = new GeometryFactory(GeometryFactory.Type.JTS);
        assertNotNull(geometryFactory);

        geometryFactory = new GeometryFactory(GeometryFactory.Type.S2);
        assertNotNull(geometryFactory);
    }

    @Test
    public void testCreateFromString() {
        GeometryFactory geometryFactory = new GeometryFactory("S2");
        assertNotNull(geometryFactory);

        geometryFactory = new GeometryFactory("JTS");
        assertNotNull(geometryFactory);

        try {
            new GeometryFactory("Wurstwasser");
            fail("IllegalArgumentException expected");
        } catch(IllegalArgumentException expected){
        }
    }
}
