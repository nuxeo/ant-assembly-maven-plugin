/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.List;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.tools.ant.Project;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;

/**
 * Utility class for dealing with {@link org.apache.maven.artifact.Artifact},
 * {@link org.eclipse.aether.artifact.Artifact} and
 * {@link org.eclipse.aether.graph.Dependency}
 *
 * @since 2.0
 */
public class DependencyUtils {

    private DependencyUtils() {
    }

    public static org.apache.maven.artifact.Artifact aetherToMaven(
            Artifact aetherArtifact, String scope,
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

    public static org.apache.maven.artifact.Artifact toMavenArtifact(
            Dependency dependency) {
        return aetherToMaven(
                dependency.getArtifact(),
                dependency.getScope(),
                AntBuildMojo.getInstance().getArtifactHandlerManager().getArtifactHandler(
                        dependency.getArtifact().getExtension()));
    }

    public static org.apache.maven.artifact.Artifact aetherToMaven(
            Artifact aetherArtifact, String scope) {
        return aetherToMaven(
                aetherArtifact,
                scope,
                AntBuildMojo.getInstance().getArtifactHandlerManager().getArtifactHandler(
                        aetherArtifact.getExtension()));
    }

    public static Artifact mavenToAether(
            org.apache.maven.artifact.Artifact artifact) {
        return new DefaultArtifact(artifact.getGroupId(),
                artifact.getArtifactId(), artifact.getClassifier(),
                artifact.getType(), artifact.getVersion());
    }

    /**
     * @throws org.eclipse.aether.resolution.ArtifactResolutionException
     */
    public static Artifact resolve(Artifact artifact)
            throws org.eclipse.aether.resolution.ArtifactResolutionException {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        return resolve(artifact, mojo.getRemoteRepositories());
    }

    /**
     * @throws org.eclipse.aether.resolution.ArtifactResolutionException
     */
    public static Artifact resolve(Artifact artifact,
            List<RemoteRepository> remoteRepositories)
            throws org.eclipse.aether.resolution.ArtifactResolutionException {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        ArtifactResult result = mojo.getSystem().resolveArtifact(
                mojo.getSession(),
                new ArtifactRequest(artifact, remoteRepositories, null));
        artifact = result.getArtifact();
        AntClient.getInstance().log(
                artifact + " resolved to  " + artifact.getFile(),
                Project.MSG_DEBUG);
        return artifact;
    }

    /**
     * @throws ArtifactResolutionException
     * @deprecated Prefer use of
     *             {@link #resolve(org.eclipse.aether.artifact.Artifact)}
     * @see #mavenToAether(Artifact)
     */
    @Deprecated
    public static void resolve(org.apache.maven.artifact.Artifact artifact)
            throws ArtifactResolutionException {
        resolve(mavenToAether(artifact));
    }

    public static Dependency mavenToDependency(
            org.apache.maven.artifact.Artifact artifact) {
        // String scope = artifact.getScope() != null ? artifact.getScope()
        // : JavaScopes.COMPILE;
        return new Dependency(mavenToAether(artifact), artifact.getScope());
    }

}
