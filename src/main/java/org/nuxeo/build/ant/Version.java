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
 *     jcarsique
 */
package org.nuxeo.build.ant;

/**
 * Bridge to {@link org.nuxeo.connect.update.Version}.
 *
 * @see #parse(String) for fallback in case of parse error
 */
public class Version extends org.nuxeo.connect.update.Version {

    /**
     * Prefer use of {@link #parse(String)}
     *
     * @param version
     */
    protected Version(String version) {
        super(version);
    }

    public Version(int major) {
        super(major);
    }

    public Version(int major, int minor) {
        super(major, minor);
    }

    public Version(int major, int minor, int patch) {
        super(major, minor, patch);
    }

    public Version(int major, int minor, int patch, String classifier) {
        super(major, minor, patch, classifier);
    }

    /**
     * Fallback on {@link OldVersion} in case of parsing error (
     * {@link OldVersion} was more tolerant even if not compliant with Nuxeo
     * versioning).
     */
    @SuppressWarnings("deprecation")
    public static Version parse(String string) {
        Version version;
        try {
            version = new Version(string);
        } catch (NumberFormatException e) {
            OldVersion oldVersion = new OldVersion(string);
            version = new Version(oldVersion.major, oldVersion.minor,
                    oldVersion.patch, oldVersion.classifier);
        }
        return version;
    }

}
