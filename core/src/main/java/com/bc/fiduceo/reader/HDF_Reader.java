package com.bc.fiduceo.reader;

import com.bc.fiduceo.core.Interval;
import com.bc.fiduceo.core.NodeType;
import com.bc.fiduceo.geometry.GeometryFactory;
import org.esa.snap.core.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.ArrayInt;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author muhammad.bc
 */
public class HDF_Reader implements Reader {

    private static final DateFormat DATEFORMAT = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
    private final static int IntervalX = 10;
    private final static int IntervalY = 10;
    private final BoundingPolygonCreator boundingPolygonCreator;
    private NetcdfFile netcdfFile;


    public HDF_Reader() {
        final GeometryFactory geometryFactory = new GeometryFactory(GeometryFactory.Type.JTS);
        boundingPolygonCreator = new BoundingPolygonCreator(new Interval(IntervalX, IntervalY), geometryFactory);
    }

    @Override
    public void open(File file) throws IOException {
        netcdfFile = NetcdfFile.open(file.getPath());
    }

    @Override
    public void close() throws IOException {
        netcdfFile.close();
    }

    @Override
    public AcquisitionInfo read() throws Exception {
        Array latitude = null;
        Array longitude = null;
        int startTime = 0;
        int endTime = 0;
        int startYear = 0;
        int endYear = 0;
        int startDay = 0;
        int endDay = 0;

        List<Variable> geolocation = netcdfFile.findGroup("Geolocation").getVariables();
        for (Variable geo : geolocation) {
            if (geo.getShortName().equals("Latitude")) {
                latitude = geo.read();
            } else if (geo.getShortName().equals("Longitude")) {
                longitude = geo.read();
            }
        }
        if (latitude==null || longitude ==null){
            throw new Exception("The H5 file is courupted");
        }

        AcquisitionInfo acquisitionInfo = boundingPolygonCreator.createPixelCodedBoundingPolygon((ArrayInt.D2) latitude, (ArrayInt.D2) longitude, NodeType.ASCENDING);
        Array read;
        List<Variable> variables = netcdfFile.findGroup("Data").getVariables();

        for (Variable data : variables) {
            if (data.getShortName().equals("scnlintime")) {
                read = data.read();
                startTime = read.getInt(0);
                endTime = read.getInt((int) data.getSize() - 1);
            } else if (data.getShortName().equals("scnlinyr")) {
                read = data.read();
                startYear = read.getInt(0);
                endYear = read.getInt((int) data.getSize() - 1);
            } else if (data.getShortName().equals("scnlindy")) {
                read = data.read();
                startDay = read.getInt(0);
                endDay = read.getInt((int) data.getSize() - 1);
            }
        }
        try {
            acquisitionInfo.setSensingStart(getDate(startYear, startDay, startTime));
            acquisitionInfo.setSensingStop(getDate(endYear, endDay, endTime));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return acquisitionInfo;
    }


    private Date getDate(int year, int day_of_yr, int time) throws ParseException {
        Calendar calendar = Calendar.getInstance();
        Date timeConvert = new SimpleDateFormat("HHmmssSSSSSS").parse(String.valueOf(time));
        calendar.setTime(timeConvert);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, day_of_yr);

        String hour = String.valueOf(calendar.get(Calendar.HOUR));
        String min = String.valueOf(calendar.get(Calendar.MINUTE));
        String second = String.valueOf(calendar.get(Calendar.SECOND));
        String mlSecond = String.valueOf(calendar.get(Calendar.MILLISECOND));
        String dy = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        String mn = String.valueOf(calendar.get(Calendar.MONTH));
        String yr = String.valueOf(calendar.get(Calendar.YEAR));
        return DATEFORMAT.parse(yr + "-" + mn + "-" + dy + " " + hour + ":" + min + ":" + second + "." + mlSecond);
    }
}
