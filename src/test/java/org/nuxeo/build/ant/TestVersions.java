/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Julien Carsique
 *
 */

package org.nuxeo.build.ant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

/**
 * @since 1.15
 */
public class TestVersions {

    private static final Log log = LogFactory.getLog(TestVersions.class);

    @Test
    public void testVersionString() throws Exception {
        // Some various filenames found in Nuxeo server
        String[] filenames = new String[] { "nuxeo-1.0-SNAPSHOT.jar",
                "nuxeo-1-SNAPSHOT.jar", "nuxeo-1.0.0-SNAPSHOT.jar",
                "bcmail-jdk15-1.45.jar", "geronimo-connector-2.2.1-NX1.jar",
                "gmbal-api-only-3.1.0-b001.jar",
                "hibernate-annotations-3.4.0.GA.jar", "java-cup-0.11a.jar",
                "jboss-el-1.0_02.CR2.jar", "jboss-seam-2.1.0.SP1.jar",
                "jena-2.6.4-NX.jar", "jersey-core-1.11-minimal.jar",
                "jmd-0.8.1-tomasol-3e60e36137.jar",
                "jodconverter-core-3.0-NX7.jar", "xpp3-1.1.4c-clean.jar",
                "xsom-20081112.jar" };
        Version version100SNAP = new Version(1, 0, 0);
        version100SNAP.setSnapshot(true);
        Version[] versions = new Version[] { version100SNAP, version100SNAP,
                version100SNAP, new Version(1, 45),
                new Version(2, 2, 1, "NX1"), new Version(3, 1, 0, "b001"),
                new Version(3, 4, 0, "GA"), new Version(0, 11, 0, "a"),
                new Version(1, 0, 0, "_02.CR2"), new Version(2, 1, 0, "SP1"),
                new Version(2, 6, 4, "NX"), new Version(1, 11, 0, "minimal"),
                new Version(0, 8, 1, "tomasol-3e60e36137"),
                new Version(3, 0, 0, "NX7"), new Version(1, 1, 4, "c-clean"),
                new Version(20081112) };
        for (int i = 0; i < filenames.length; i++) {
            Version version = Version.parse(getVersionFromFilename(filenames[i]));
            assertEquals("Error parsing " + filenames[i], versions[i], version);
        }
    }

    private String getVersionFromFilename(String filename) {
        Matcher m = RemoveDuplicateTask.PATTERN.matcher(filename);
        assertTrue(m.find());
        String key = filename.substring(0, m.start());
        String v = m.group(1);
        log.debug(filename + " => key=" + key + " version=" + v);
        return v;
    }

}
