/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * @see org.eclipse.aether.util.artifact.ArtifactIdUtils org.eclipse.aether.artifact.DefaultArtifact
 *      org.eclipse.aether.graph.Dependency
 */
public class ArtifactDescriptor {

    public static final Pattern PATTERN = Pattern.compile("(?<groupId>[^: ]+):(?<artifactId>[^: ]+)"
            + "(?::(?<version>[^: ]*)(?::(?<type>[^: ]*)(?::(?<classifier>[^: ]*)(?::(?<scope>[^: ]*))?)?)?)?");

    protected String groupId = null;

    protected String artifactId = null;

    protected String version = null;

    protected String type = null;

    protected String classifier = null;

    protected String scope = null;

    public ArtifactDescriptor() {
    }

    public ArtifactDescriptor(String groupId, String artifactId, String version, String type, String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.classifier = classifier;
        scope = JavaScopes.COMPILE;
    }

    /**
     * @param key Key for an artifact or a dependency with pattern
     *            &lt;groupId&gt;:&lt;artifactId&gt;[:&lt;version&gt;[:&lt;type
     *            &gt;[:&lt;classifier&gt;[:&lt;scope&gt;]]]]
     * @see #PATTERN
     */
    public ArtifactDescriptor(String key) {
        Matcher m = PATTERN.matcher(key);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format("Invalid key '%s', expected format is '%s'", key,
                    "<groupId>:<artifactId>[:<version>[:<type>[:<classifier>[:<scope>]]]]"));
        }
        groupId = m.group("groupId");
        artifactId = m.group("artifactId");
        if (m.group("version") != null && !m.group("version").isEmpty()) {
            version = m.group("version");
        }
        if (m.group("type") != null && !m.group("type").isEmpty()) {
            type = m.group("type");
        } else {
            type = "jar";
        }
        if (m.group("classifier") != null && !m.group("classifier").isEmpty()) {
            classifier = m.group("classifier");
        }
        if (m.group("scope") != null && !m.group("scope").isEmpty()) {
            scope = m.group("scope");
        } else {
            scope = JavaScopes.COMPILE;
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
     * Prefer use of {@link #getDependency()} if the artifact scope is needed.
     *
     * @since 2.0
     */
    public org.eclipse.aether.artifact.Artifact getAetherArtifact() {
        return getDependency().getArtifact();
    }

    /**
     * @since 2.0
     */
    public Dependency getDependency() {
        return new Dependency(new DefaultArtifact(groupId, artifactId, classifier, type, version), scope);
    }

    /**
     * @param key
     * @return an ArtifactDescriptor corresponding to the key, or the EMPTY_DESCRIPTOR if the parsing failed.
     * @since 2.0.4
     */
    public static ArtifactDescriptor parseQuietly(String key) {
        try {
            return new ArtifactDescriptor(key);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Ignore parsing issues and return an empty ArtifactDescriptor
            return new ArtifactDescriptor();
        }
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * @since 2.0.4
     */
    public boolean isEmpty() {
        return groupId == null && artifactId == null && groupId == null && scope == null && type == null
                && classifier == null;
    }
}
