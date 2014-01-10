/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu, jcarsique
 */
package org.nuxeo.build.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.nuxeo.build.maven.graph.DependencyUtils;

/**
 * @see org.eclipse.aether.util.artifact.ArtifactIdUtils
 *      org.eclipse.aether.artifact.DefaultArtifact
 *      org.eclipse.aether.graph.Dependency
 */
public class ArtifactDescriptor {

    protected static final String KEY_PATTERN = "(?<groupId>[^: ]+):(?<artifactId>[^: ]+)"
            + "(?::(?<version>[^: ]*)(?::(?<type>[^: ]*)(?::(?<classifier>[^: ]*)(?::(?<scope>[^: ]*))?)?)?)?";

    public String groupId = null;

    public String artifactId = null;

    public String version = null;

    public String type = "jar";

    public String classifier = null;

    public String scope = JavaScopes.COMPILE;

    protected final Pattern adPattern = Pattern.compile(KEY_PATTERN);

    public static ArtifactDescriptor emptyDescriptor() {
        ArtifactDescriptor ad = new ArtifactDescriptor();
        ad.scope = null;
        ad.type = null;
        return ad;
    }

    private ArtifactDescriptor() {
    }

    public ArtifactDescriptor(String groupId, String artifactId,
            String version, String type, String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.classifier = classifier;
    }

    /**
     * @param key Key for an artifact or a dependency with pattern
     *            &lt;groupId&gt
     *            ;:&lt;artifactId&gt;[:&lt;version&gt;[:&lt;type&gt
     *            ;[:&lt;classifier&gt;[:&lt;scope&gt;]]]]
     * @see #KEY_PATTERN
     */
    public ArtifactDescriptor(String key) {
        Matcher m = adPattern.matcher(key);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    String.format("Invalid key '%s', expected format is '%s'",
                            key,
                            "<groupId>:<artifactId>[:<version>[:<type>[:<classifier>[:<scope>]]]]"));
        }
        groupId = m.group("groupId");
        artifactId = m.group("artifactId");
        if (m.group("version") != null && !m.group("version").isEmpty()) {
            version = m.group("version");
        }
        if (m.group("type") != null && !m.group("type").isEmpty()) {
            type = m.group("type");
        }
        if (m.group("classifier") != null && !m.group("classifier").isEmpty()) {
            classifier = m.group("classifier");
        }
        if (m.group("scope") != null && !m.group("scope").isEmpty()) {
            scope = m.group("scope");
        }
    }

    public String getNodeKeyPattern() {
        if (groupId != null) {
            StringBuilder buf = new StringBuilder();
            buf.append(groupId);
            if (artifactId != null) {
                buf.append(':').append(artifactId);
                if (version != null) {
                    buf.append(':').append(version);
                    if (type != null) {
                        buf.append(':').append(type);
                    }
                }
            }
            return buf.toString();
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(groupId).append(':').append(artifactId);
        buf.append(':').append(version);
        buf.append(':').append(type);
        buf.append(':');
        if (classifier != null) {
            buf.append(classifier);
        }
        buf.append(':').append(scope);
        return buf.toString();
    }

    /**
     * @return a "Maven" artifact (versus AetherArtifact)
     * @since 1.10.2
     */
    public Artifact getMavenArtifact() {
        org.eclipse.aether.artifact.Artifact aetherArtifact = getAetherArtifact();
        return DependencyUtils.aetherToMaven(aetherArtifact, scope);
    }

    /**
     * Prefer use of {@link #getDependency()} to also get the artifact scope.
     *
     * @since 2.0
     */
    public org.eclipse.aether.artifact.Artifact getAetherArtifact() {
        return new DefaultArtifact(groupId, artifactId, classifier, type,
                version);
    }

    /**
     * @since 2.0
     */
    public Dependency getDependency() {
        return new Dependency(new DefaultArtifact(groupId, artifactId,
                classifier, type, version), scope);
    }

}
