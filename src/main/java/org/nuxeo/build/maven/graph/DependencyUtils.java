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

package org.nuxeo.build.maven.graph;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.nuxeo.build.maven.AntBuildMojo;
import org.sonatype.aether.graph.Dependency;

/**
 * Utility class for managing Maven dependencies.
 *
 * @since 2.0
 */
public class DependencyUtils {

    private DependencyUtils() {
    }

    public static org.apache.maven.artifact.Artifact aetherToMavenArtifact(
            org.sonatype.aether.artifact.Artifact aetherArtifact, String scope,
            ArtifactHandler artifactHandler) {
        org.apache.maven.artifact.Artifact mavenArtifact = new org.apache.maven.artifact.DefaultArtifact(
                aetherArtifact.getGroupId(), aetherArtifact.getArtifactId(),
                aetherArtifact.getVersion(), scope,
                aetherArtifact.getExtension(), aetherArtifact.getClassifier(),
                artifactHandler);
        mavenArtifact.setFile(aetherArtifact.getFile());
        mavenArtifact.setResolved(aetherArtifact.getFile() != null);
        return mavenArtifact;
    }

    public static org.apache.maven.artifact.Artifact getMavenArtifact(
            Dependency dependency) {
        return aetherToMavenArtifact(
                dependency.getArtifact(),
                dependency.getScope(),
                AntBuildMojo.getInstance().getArtifactHandlerManager().getArtifactHandler(
                        dependency.getArtifact().getExtension()));
    }

    public static Artifact aetherToMavenArtifact(
            org.sonatype.aether.artifact.Artifact aetherArtifact, String scope) {
        return aetherToMavenArtifact(
                aetherArtifact,
                scope,
                AntBuildMojo.getInstance().getArtifactHandlerManager().getArtifactHandler(
                        aetherArtifact.getExtension()));
    }

}
