/**
 * NoiseMap is a scientific computation plugin for OrbisGIS developed in order to
 * evaluate the noise impact on urban mobility plans. This model is
 * based on the French standard method NMPB2008. It includes traffic-to-noise
 * sources evaluation and sound propagation processing.
 *
 * This version is developed at French IRSTV Institute and at IFSTTAR
 * (http://www.ifsttar.fr/) as part of the Eval-PDU project, funded by the
 * French Agence Nationale de la Recherche (ANR) under contract ANR-08-VILL-0005-01.
 *
 * Noisemap is distributed under GPL 3 license. Its reference contact is Judicaël
 * Picaut <judicael.picaut@ifsttar.fr>. It is maintained by Nicolas Fortin
 * as part of the "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/>.
 *
 * Copyright (C) 2011 IFSTTAR
 * Copyright (C) 2011-2012 IRSTV (FR CNRS 2488)
 *
 * Noisemap is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Noisemap is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Noisemap. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://www.orbisgis.org/>
 * or contact directly:
 * info_at_ orbisgis.org
 *
 * H2GIS is a library that brings spatial support to the H2 Database Engine
 * <http://www.h2database.com>. H2GIS is developed by CNRS
 * <http://www.cnrs.fr/>.
 *
 * This code is part of the H2GIS project. H2GIS is free software;
 * you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation;
 * version 3.0 of the License.
 *
 * H2GIS is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details <http://www.gnu.org/licenses/>.
 *
 *
 * For more information, please consult: <http://www.h2gis.org/>
 * or contact directly: info_at_h2gis.org
 */

package org.noise_planet.noisemodelling.pathfinder.utils;

import org.cts.CRSFactory;
import org.cts.IllegalCoordinateException;
import org.cts.crs.CRSException;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.crs.GeodeticCRS;
import org.cts.op.CoordinateOperation;
import org.cts.op.CoordinateOperationException;
import org.cts.op.CoordinateOperationFactory;
import org.cts.registry.EPSGRegistry;
import org.cts.registry.RegistryManager;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateFilter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.kml.KMLWriter;
import org.noise_planet.noisemodelling.pathfinder.ProfileBuilder;
import org.noise_planet.noisemodelling.pathfinder.PropagationPath;
import org.noise_planet.noisemodelling.pathfinder.Triangle;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Export objects of NoiseModelling into KML format.
 * Modified version of org.h2gis.functions.io.kml.KMLWriterDriver
 * @author Erwan Bocher
 * @author Nicolas Fortin 2019
 */
public class KMLDocument {
    //    private List<MeshBuilder.PolygonWithHeight> buildings = new ArrayList<>();
//    private List<TriMarkers> isoCountours = new ArrayList<>();
    private final XMLStreamWriter xmlOut;
    private final OutputStream outputStream;
    private Coordinate offset = new Coordinate(0, 0, 0);
    // 0.011 meters precision
    //https://gisjames.wordpress.com/2016/04/27/deciding-how-many-decimal-places-to-include-when-reporting-latitude-and-longitude/
    private int wgs84Precision = 7;
    private GeometryFactory geometryFactory = new GeometryFactory();
    private CoordinateOperation transform = null;
    // Color scale from 0 to 1
    private TreeMap<Double, Color> colorScale = new TreeMap<>();

    public KMLDocument(OutputStream outputStream) throws XMLStreamException {
        final XMLOutputFactory streamWriterFactory = XMLOutputFactory.newFactory();
        this.outputStream = outputStream;
        xmlOut = streamWriterFactory.createXMLStreamWriter(
                new BufferedOutputStream(outputStream), "UTF-8");
        setDefaultColorScale();
    }

    public void setDefaultColorScale() {
        colorScale.clear();
        // Set default color scale
        colorScale.put(0.0, new Color(130, 166, 173));
        colorScale.put(1 / 10.0, new Color(160, 186, 191));
        colorScale.put(2 / 10.0, new Color(184, 214, 209));
        colorScale.put(3 / 10.0, new Color(206, 228, 204));
        colorScale.put(4 / 10.0, new Color(226, 242, 191));
        colorScale.put(5 / 10.0, new Color(243, 198, 131));
        colorScale.put(6 / 10.0, new Color(232, 126, 77));
        colorScale.put(7 / 10.0, new Color(205, 70, 62));
        colorScale.put(8 / 10.0, new Color(161, 26, 77));
        colorScale.put(9 / 10.0, new Color(117, 8, 92));
        colorScale.put(1.0, new Color(67, 10, 74));
    }

    public Map<Double, Color> getColorScale() {
        return new HashMap<>(colorScale);
    }

    public void setColorScale(Map<Double, Color> colorScale) {
        this.colorScale = new TreeMap<>(colorScale);
    }

    /**
     * @return how-many-decimal-places-to-include-when-reporting-latitude-and-longitude
     */
    public int getWgs84Precision() {
        return wgs84Precision;
    }

    /**
     * @param wgs84Precision how-many-decimal-places-to-include-when-reporting-latitude-and-longitude
     */
    public void setWgs84Precision(int wgs84Precision) {
        this.wgs84Precision = wgs84Precision;
    }

    public void setInputCRS(String crs) throws CRSException, CoordinateOperationException {
        // Create a new CRSFactory, a necessary element to create a CRS without defining one by one all its components
        CRSFactory cRSFactory = new CRSFactory();

        // Add the appropriate registry to the CRSFactory's registry manager. Here the EPSG registry is used.
        RegistryManager registryManager = cRSFactory.getRegistryManager();
        registryManager.addRegistry(new EPSGRegistry());

        // CTS will read the EPSG registry seeking the 4326 code, when it finds it,
        // it will create a CoordinateReferenceSystem using the parameters found in the registry.
        CoordinateReferenceSystem crsKML = cRSFactory.getCRS("EPSG:4326");
        CoordinateReferenceSystem crsSource = cRSFactory.getCRS(crs);
        if(crsKML instanceof GeodeticCRS && crsSource instanceof GeodeticCRS) {
            transform = CoordinateOperationFactory.createCoordinateOperations((GeodeticCRS) crsSource, (GeodeticCRS) crsKML).iterator().next();
        }
    }

    public KMLDocument writeHeader() throws XMLStreamException {
        xmlOut.writeStartDocument("UTF-8", "1.0");
        xmlOut.writeStartElement("kml");
        xmlOut.writeDefaultNamespace("http://www.opengis.net/kml/2.2");
        xmlOut.writeNamespace("atom", "http://www.w3.org/2005/Atom");
        xmlOut.writeNamespace("kml", "http://www.opengis.net/kml/2.2");
        xmlOut.writeNamespace("gx", "http://www.google.com/kml/ext/2.2");
        xmlOut.writeNamespace("xal", "urn:oasis:names:tc:ciq:xsdschema:xAL:2.0");
        xmlOut.writeStartElement("Document");
        return this;
    }

    public KMLDocument writeFooter() throws XMLStreamException {
        xmlOut.writeEndElement();//Doc
        xmlOut.writeEndDocument();//KML
        xmlOut.close();
        return this;
    }

    public KMLDocument setOffset(Coordinate offset) {
        this.offset = offset;
        return this;
    }

    private Coordinate copyCoord(Coordinate in) {
        return new Coordinate(in.x + offset.x, in.y + offset.y, Double.isNaN(in.z) ? offset.z : in.z + offset.z);
    }

    public KMLDocument writeTopographic(List<Triangle> triVertices, List<Coordinate> vertices) throws XMLStreamException {
        // Write style
        xmlOut.writeStartElement("Style");
        xmlOut.writeAttribute("id", "mnt");
        xmlOut.writeStartElement("LineStyle");
        xmlOut.writeStartElement("width");
        xmlOut.writeCharacters("0");
        xmlOut.writeEndElement(); // width
        xmlOut.writeEndElement();// LineStyle
        xmlOut.writeStartElement("PolyStyle");
        xmlOut.writeStartElement("color");
        xmlOut.writeCharacters("640078FF");
        xmlOut.writeEndElement(); // color
        xmlOut.writeEndElement();// LineStyle
        xmlOut.writeEndElement();// Style

        xmlOut.writeStartElement("Schema");
        xmlOut.writeAttribute("name", "mnt");
        xmlOut.writeAttribute("id", "mnt");
        xmlOut.writeEndElement();//Write schema
        xmlOut.writeStartElement("Folder");
        xmlOut.writeStartElement("name");
        xmlOut.writeCharacters("mnt");
        xmlOut.writeEndElement();//Name
        xmlOut.writeStartElement("Placemark");
        xmlOut.writeStartElement("styleUrl");
        xmlOut.writeCharacters("#mnt");
        xmlOut.writeEndElement(); // styleUrl
        xmlOut.writeStartElement("name");
        xmlOut.writeCharacters("tri");
        xmlOut.writeEndElement();//Name
        Polygon[] polygons = new Polygon[triVertices.size()];
        int idTri = 0;
        for(Triangle triangle : triVertices) {
            Polygon poly = geometryFactory.createPolygon(new Coordinate[]{copyCoord(vertices.get(triangle.getA())),
                    copyCoord(vertices.get(triangle.getB())), copyCoord(vertices.get(triangle.getC())),
                    copyCoord(vertices.get(triangle.getA()))});
            if(Orientation.isCCW(poly.getCoordinates())) {
                poly = (Polygon) poly.reverse();
            }
            // Apply CRS transform
            doTransform(poly);
            polygons[idTri++] = poly;
        }
        //Write geometry
        writeRawXml(KMLWriter.writeGeometry(geometryFactory.createMultiPolygon(polygons), Double.NaN,
                wgs84Precision, false, KMLWriter.ALTITUDE_MODE_ABSOLUTE));
        xmlOut.writeEndElement();//Write Placemark
        xmlOut.writeEndElement();//Folder
        return this;
    }

    private void writeRawXml(String rowXml) throws XMLStreamException {
        xmlOut.flush();
        try {
            outputStream.write(rowXml.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    public KMLDocument writeBuildings(ProfileBuilder profileBuilder) throws XMLStreamException {
        xmlOut.writeStartElement("Schema");
        xmlOut.writeAttribute("name", "buildings");
        xmlOut.writeAttribute("id", "buildings");
        xmlOut.writeEndElement();//Write schema
        xmlOut.writeStartElement("Folder");
        xmlOut.writeStartElement("name");
        xmlOut.writeCharacters("buildings");
        xmlOut.writeEndElement();//Name
        List<ProfileBuilder.Building> buildings = profileBuilder.getBuildings();
        int idPoly = 0;
        KMLWriter buildingWriter = new KMLWriter();
        buildingWriter.setPrecision(wgs84Precision);
        buildingWriter.setExtrude(true);
        buildingWriter.setTesselate(true);
        buildingWriter.setAltitudeMode(profileBuilder.hasDem() ? KMLWriter.ALTITUDE_MODE_ABSOLUTE : KMLWriter.ALTITUDE_MODE_RELATIVETOGROUND);

        for(ProfileBuilder.Building building : buildings) {
            Coordinate[] original = building.getGeometry().getCoordinates();
            Coordinate[] coordinates = new Coordinate[original.length];
            double z = profileBuilder.getBuilding(idPoly ).getZ();
            // z is building height + average ground height
            for(int i = 0; i < coordinates.length; i++) {
                coordinates[i] = copyCoord(new Coordinate(original[i].x, original[i].y, z));
            }
            if(coordinates.length > 3 && coordinates[0].equals2D(coordinates[coordinates.length - 1])) {
                Polygon poly = geometryFactory.createPolygon(coordinates);
                if(!Orientation.isCCW(poly.getCoordinates())) {
                    poly = (Polygon) poly.reverse();
                }
                // Apply CRS transform
                doTransform(poly);
                xmlOut.writeStartElement("Placemark");
                xmlOut.writeStartElement("name");
                xmlOut.writeCharacters("building");
                xmlOut.writeEndElement();//Name
                //Write geometry
                buildingWriter.setZ(z);
                writeRawXml(buildingWriter.write(poly));
                xmlOut.writeEndElement();//Write Placemark
            }
            idPoly++;
        }
        xmlOut.writeEndElement();//Folder
        return this;
    }

    public KMLDocument writeProfile(String layerName, ProfileBuilder.CutProfile profile) throws XMLStreamException {
        xmlOut.writeStartElement("Schema");
        xmlOut.writeAttribute("name", "profile");
        xmlOut.writeAttribute("id", "profile");
        xmlOut.writeEndElement();//Write schema
        xmlOut.writeStartElement("Folder");
        xmlOut.writeStartElement("name");
        xmlOut.writeCharacters("profile");
        xmlOut.writeEndElement();//Name

        xmlOut.writeStartElement("Placemark");
        xmlOut.writeStartElement("name");
        xmlOut.writeCharacters(layerName);
        xmlOut.writeEndElement();//Name

        Coordinate[] coordinates = new Coordinate[profile.getCutPoints().size()];
        int i=0;

        for(ProfileBuilder.CutPoint cutPoint : profile.getCutPoints()) {

            coordinates[i++] = copyCoord(cutPoint.getCoordinate());
        }

        LineString lineString = geometryFactory.createLineString(coordinates);
        // Apply CRS transform
        doTransform(lineString);
        //Write geometry
        writeRawXml(KMLWriter.writeGeometry(lineString, Double.NaN,
                wgs84Precision, false, KMLWriter.ALTITUDE_MODE_ABSOLUTE));
        xmlOut.writeEndElement();//Write Placemark

        xmlOut.writeEndElement();//Folder
        return this;
    }

    private String formatColorEntry(double key) {
        return String.format(Locale.ROOT, "scale%g", key);
    }

    public KMLDocument writeRays(Collection<PropagationPath> rays) throws XMLStreamException {
        double minDb = Double.MAX_VALUE;
        double maxDb = -Double.MAX_VALUE;
        for(PropagationPath line : rays) {
            if(line.absorptionData.aGlobal != null && line.absorptionData.aGlobal.length > 0) {
                double attenuationLevel = PowerUtils.sumDbArray(line.absorptionData.aGlobal);
                minDb = Math.min(minDb, attenuationLevel);
                maxDb = Math.max(maxDb, attenuationLevel);
            }
        }
        for (Map.Entry<Double, Color> colorEntry : colorScale.entrySet()) {
            xmlOut.writeStartElement("Style");
            xmlOut.writeAttribute("id", formatColorEntry(colorEntry.getKey()));
            xmlOut.writeStartElement("LineStyle");
            xmlOut.writeStartElement("color");
            Color color = colorEntry.getValue();
            xmlOut.writeCharacters(String.format("#FF%02x%02x%02x", color.getBlue(), color.getGreen(), color.getRed()));
            xmlOut.writeEndElement(); // /color
            xmlOut.writeEndElement(); // /LineStyle
            xmlOut.writeEndElement(); // / Style
        }

        xmlOut.writeStartElement("Schema");
        xmlOut.writeAttribute("name", "rays");
        xmlOut.writeAttribute("id", "rays");
        xmlOut.writeEndElement();//Write schema
        xmlOut.writeStartElement("Folder");
        xmlOut.writeStartElement("name");
        xmlOut.writeCharacters("rays");
        xmlOut.writeEndElement();//Name
        for(PropagationPath line : rays) {
            double attenuationLevel = 0;
            xmlOut.writeStartElement("Placemark");
            xmlOut.writeStartElement("name");
            boolean hasGroundElevation = false;
            for(ProfileBuilder.CutPoint cutPoint : line.getCutPoints()) {
                if(!Double.isNaN(cutPoint.getzGround())) {
                    hasGroundElevation = true;
                    break;
                }
            }
            if(line.absorptionData.aGlobal != null && line.absorptionData.aGlobal.length > 0) {
                attenuationLevel = PowerUtils.sumDbArray(line.absorptionData.aGlobal);
                xmlOut.writeCharacters(String.format("%.1f dB R:%d S:%d",
                        attenuationLevel,line.getIdReceiver(), line.getIdSource()));
            } else {
                xmlOut.writeCharacters(String.format("R:%d S:%d", line.getIdReceiver(), line.getIdSource()));
            }
            xmlOut.writeEndElement();//Name
            if(line.absorptionData.aGlobal != null && line.absorptionData.aGlobal.length > 0) {
                Map.Entry<Double, Color> colorEntry =
                        colorScale.floorEntry((attenuationLevel - minDb) / (maxDb - minDb));
                if(colorEntry == null) {
                    colorEntry = colorScale.firstEntry();
                }
                xmlOut.writeStartElement("styleUrl");
                xmlOut.writeCharacters("#" + formatColorEntry(colorEntry.getKey()));
                xmlOut.writeEndElement(); //styleurl
            }
            LineString lineString = line.asGeom();
            // Apply CRS transform
            doTransform(lineString);
            //Write geometry
            writeRawXml(KMLWriter.writeGeometry(lineString, Double.NaN,
                    wgs84Precision, false,
                    hasGroundElevation ? KMLWriter.ALTITUDE_MODE_ABSOLUTE : KMLWriter.ALTITUDE_MODE_RELATIVETOGROUND));
            xmlOut.writeEndElement();//Write Placemark
        }
        xmlOut.writeEndElement();//Folder
        return this;
    }

    public void doTransform(Geometry geometry) {
        if(transform != null && geometry != null) {
            geometry.apply(new CRSTransformFilter(transform));
            // Recompute envelope
            geometry.setSRID(4326);
        }
    }

    public static final class CRSTransformFilter implements CoordinateFilter {
        private final CoordinateOperation coordinateOperation;

        public CRSTransformFilter(CoordinateOperation coordinateOperation) {
            this.coordinateOperation = coordinateOperation;
        }

        public void filter(Coordinate coord) {
            try {
                if (Double.isNaN(coord.z)) {
                    coord.z = 0.0D;
                }

                double[] xyz = this.coordinateOperation.transform(new double[]{coord.x, coord.y, coord.z});
                coord.x = xyz[0];
                coord.y = xyz[1];
                if (xyz.length > 2) {
                    coord.z = xyz[2];
                }
            } catch (IllegalCoordinateException | CoordinateOperationException var3) {
                Logger.getLogger(KMLDocument.class.getName()).log(Level.SEVERE, (String)null, var3);
            }

        }
    }
}
