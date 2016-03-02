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

package com.bc.fiduceo.geometry.s2;


import com.bc.fiduceo.geometry.Geometry;
import com.bc.fiduceo.geometry.Point;
import com.google.common.geometry.S2LatLng;

class BcS2Point implements Point {

    private S2LatLng s2LatLng;

    BcS2Point(S2LatLng s2LatLng) {
        this.s2LatLng = s2LatLng;
    }

    @Override
    public double getLon() {
        return s2LatLng.lngDegrees();
    }

    @Override
    public void setLon(double lon) {
        s2LatLng = S2LatLng.fromDegrees(s2LatLng.latDegrees(), lon);
    }

    @Override
    public double getLat() {
        return s2LatLng.latDegrees();
    }

    @Override
    public void setLat(double lat) {
        s2LatLng = S2LatLng.fromDegrees(lat, s2LatLng.lngDegrees());
    }

    @Override
    public Geometry getIntersection(Geometry other) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean isEmpty() {
        return s2LatLng != null;
    }

    @Override
    public boolean isValid() {
        return s2LatLng.isValid();
    }

    @Override
    public Point[] getCoordinates() {
        return new Point[]{this};
    }

    @Override
    public Object getInner() {
        return s2LatLng;
    }

    @Override
    public String toString() {
        return "POINT(" + s2LatLng.lngDegrees() + " " + s2LatLng.latDegrees() + ")";
    }

    @Override
    public boolean equals(Point other) {
        return other == this || other.getLon() == getLon() && other.getLat() == getLat();
    }
}
