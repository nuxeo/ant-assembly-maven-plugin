/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.build.ant;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * version classifier is not took into account when comparing versions
 *
 * @deprecated Since 1.14. Use org.nuxeo.connect.update.Version instead
 * @since 1.15
 *
 */
@Deprecated
public class OldVersion implements Comparable<OldVersion> {

    protected static final Pattern PATTERN = Pattern.compile("([0-9]+[0-9\\.]*)");

    public final static OldVersion ZERO = new OldVersion(0);

    protected int major;

    protected int minor;

    protected int patch;

    protected String classifier;

    public OldVersion(String version) {
        Matcher m = PATTERN.matcher(version);
        if (m.find()) {
            if (!m.hitEnd()) {
                classifier = version.substring(m.end());
                if (classifier.startsWith("-")) {
                    classifier = classifier.substring(1);
                }
            }
            version = m.group(1);
            if (version.endsWith(".")) {
                version = version.substring(0, version.length() - 1);
            }
        } else {
            throw new IllegalArgumentException("Not a version: " + version);
        }
        int p = version.lastIndexOf('-');
        if (p > 0) { // classifier found
            classifier = version.substring(p + 1) + classifier;
            version = version.substring(0, p);
        } else {
            p = version.lastIndexOf('_');
            if (p > 0) { // classifier found
                classifier = version.substring(p + 1) + classifier;
                version = version.substring(0, p);
            }
        }
        p = version.indexOf('.', 0);
        if (p > -1) {
            major = Integer.parseInt(version.substring(0, p));
            int q = version.indexOf('.', p + 1);
            if (q > -1) {
                minor = Integer.parseInt(version.substring(p + 1, q));
                int r = version.indexOf('.', q + 1);
                if (r > 0) {
                    patch = Integer.parseInt(version.substring(q + 1, r));
                } else {
                    try {
                        patch = Integer.parseInt(version.substring(q + 1));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                minor = Integer.parseInt(version.substring(p + 1));
            }
        } else {
            major = Integer.parseInt(version);
        }
    }

    public OldVersion(int major) {
        this(major, 0, 0);
    }

    public OldVersion(int major, int minor) {
        this(major, minor, 0);
    }

    public OldVersion(int major, int minor, int patch) {
        this(major, minor, patch, null);
    }

    public OldVersion(int major, int minor, int patch, String classifier) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.classifier = classifier;
    }

    public int major() {
        return major;
    }

    public int minor() {
        return minor;
    }

    public int patch() {
        return patch;
    }

    public String classifier() {
        return classifier;
    }

    public boolean lessThan(OldVersion v) {
        return compareTo(v) < 0;
    }

    public boolean lessOrEqualsThan(OldVersion v) {
        return compareTo(v) <= 0;
    }

    public boolean equalsTo(OldVersion v) {
        return compareTo(v) == 0;
    }

    public boolean greaterThan(OldVersion v) {
        return compareTo(v) > 0;
    }

    public boolean greaterOrEqualThan(OldVersion v) {
        return compareTo(v) >= 0;
    }

    @Override
    public int compareTo(OldVersion o) {
        int d = major - o.major;
        if (d != 0) {
            return d;
        }
        d = minor - o.minor;
        if (d != 0) {
            return d;
        }
        d = patch - o.patch;
        if (d != 0) {
            return d;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof OldVersion) {
            OldVersion v = (OldVersion) obj;
            return v.major == major && v.minor == minor && v.patch == patch;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (major << 16) | (minor << 8) | patch;
    }

    @Override
    public String toString() {
        if (classifier == null) {
            return major + "." + minor + "." + patch;
        } else {
            return major + "." + minor + "." + patch + "-" + classifier;
        }
    }

}
