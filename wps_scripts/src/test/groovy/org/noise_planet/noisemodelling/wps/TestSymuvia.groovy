/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */


package org.noise_planet.noisemodelling.wps

import org.noise_planet.noisemodelling.wps.Database_Manager.Display_Database
import org.noise_planet.noisemodelling.wps.Experimental.Import_Symuvia
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Test parsing of zip file using H2GIS database
 */
class TestSymuvia extends JdbcTestCase {
    Logger LOGGER = LoggerFactory.getLogger(TestSymuvia.class)

    void testTutorial() {
        // Check empty database
        Object res = new Display_Database().exec(connection, [])

        assertEquals("", res)
        // Import OSM file
        res = new Import_Symuvia().exec(connection,
                ["pathFile": TestSymuvia.getResource("symuvia.xml").getPath(),
                "defaultSRID" : 2154])

        res = new Display_Database().exec(connection, [])

        assertTrue(res.contains("SYMUVIA_TRAJ"))
    }

}
