/*
 * (C) Copyright 2013-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.DependencyManagement;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.resolution.VersionRangeRequest;
import org.eclipse.aether.resolution.VersionRangeResolutionException;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.filter.Filter;

/**
 * Utility class for dealing with {@link org.apache.maven.artifact.Artifact},
 * {@link org.eclipse.aether.artifact.Artifact} and {@link org.eclipse.aether.graph.Dependency}
 *
 * @since 2.0
 */
public class DependencyUtils {

    private DependencyUtils() {
    }

    public static org.apache.maven.artifact.Artifact aetherToMaven(Artifact aetherArtifact, String scope,
            ArtifactHandler artifactHandler) {
        org.apache.maven.artifact.Artifact mavenArtifact = new org.apache.maven.artifact.DefaultArtifact(
                aetherArtifact.getGroupId(), aetherArtifact.getArtifactId(), aetherArtifact.getVersion(), scope,
                aetherArtifact.getExtension(), aetherArtifact.getClassifier(), artifactHandler);
        mavenArtifact.setFile(aetherArtifact.getFile());
        mavenArtifact.setResolved(aetherArtifact.getFile() != null);
        return mavenArtifact;
    }

    public static org.apache.maven.artifact.Artifact toMavenArtifact(Dependency dependency) {
        return aetherToMaven(
                dependency.getArtifact(),
                dependency.getScope(),
                AntBuildMojo.getInstance()
                            .getArtifactHandlerManager()
                            .getArtifactHandler(dependency.getArtifact().getExtension()));
    }

    public static org.apache.maven.artifact.Artifact aetherToMaven(Artifact aetherArtifact, String scope) {
        return aetherToMaven(aetherArtifact, scope, AntBuildMojo.getInstance()
                                                                .getArtifactHandlerManager()
                                                                .getArtifactHandler(aetherArtifact.getExtension()));
    }

    /**
     * @deprecated Since 2.0.4. Use {@link RepositoryUtils#toArtifact(Artifact)} instead.
     */
    @Deprecated
    public static Artifact mavenToAether(org.apache.maven.artifact.Artifact artifact) {
        return RepositoryUtils.toArtifact(artifact);
    }

    /**
     * @throws org.eclipse.aether.resolution.ArtifactResolutionException
     */
    public static Artifact resolve(Artifact artifact) throws org.eclipse.aether.resolution.ArtifactResolutionException {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        return resolve(artifact, mojo.getRemoteRepositories());
    }

    /**
     * @throws org.eclipse.aether.resolution.ArtifactResolutionException
     */
    public static Artifact resolve(Artifact artifact, List<RemoteRepository> remoteRepositories)
            throws org.eclipse.aether.resolution.ArtifactResolutionException {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        ArtifactResult result = mojo.getSystem().resolveArtifact(mojo.getSession(),
                new ArtifactRequest(artifact, remoteRepositories, null));
        artifact = result.getArtifact();
        AntClient.getInstance().log(artifact + " resolved to  " + artifact.getFile(), Project.MSG_DEBUG);
        return artifact;
    }

    /**
     * @throws ArtifactResolutionException
     * @deprecated Prefer use of {@link #resolve(org.eclipse.aether.artifact.Artifact)}
     * @see #mavenToAether(org.apache.maven.artifact.Artifact)
     */
    @Deprecated
    public static void resolve(org.apache.maven.artifact.Artifact artifact) throws ArtifactResolutionException {
        resolve(RepositoryUtils.toArtifact(artifact));
    }

    /**
     * The generated dependency has no exclusions.
     *
     * @deprecated Since 2.0.4. Use
     *             {@link RepositoryUtils#toDependency(org.apache.maven.artifact.Artifact, java.util.Collection)} or
     *             {@link RepositoryUtils#toDependency(org.apache.maven.model.Dependency, org.eclipse.aether.artifact.ArtifactTypeRegistry)}
     *             instead
     */
    @Deprecated
    public static Dependency mavenToDependency(org.apache.maven.artifact.Artifact artifact) {
        // String scope = artifact.getScope() != null ? artifact.getScope()
        // : JavaScopes.COMPILE;
        return new Dependency(RepositoryUtils.toArtifact(artifact), artifact.getScope(), artifact.isOptional());
    }

    /**
     * FIXME JC doesn't work because of depth < 2 in org.eclipse.aether.collection.DependencyManager
     *
     * @return The dependency on which the dependencyManagement has been applied.
     */
    public static Dependency getManagedDependency(Dependency dependency) {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        DependencyManagement depMgt = mojo.getSession().getDependencyManager().manageDependency(dependency);
        if (depMgt != null) {
            if (depMgt.getVersion() != null) {
                Artifact artifact = dependency.getArtifact();
                dependency = dependency.setArtifact(artifact.setVersion(depMgt.getVersion()));
            }
            if (depMgt.getProperties() != null) {
                Artifact artifact = dependency.getArtifact();
                dependency = dependency.setArtifact(artifact.setProperties(depMgt.getProperties()));
            }
            if (depMgt.getScope() != null) {
                dependency = dependency.setScope(depMgt.getScope());
            }
            if (depMgt.getOptional() != null) {
                dependency = dependency.setOptional(depMgt.getOptional());
            }
            if (depMgt.getExclusions() != null) {
                dependency = dependency.setExclusions(depMgt.getExclusions());
            }
        }
        return dependency;
    }

    /**
     * Look for a version in the project dependencyManagement for the given artifact and set it.
     *
     * @return The new artifact if the version changed, else the original one
     */
    public static Artifact setManagedVersion(Artifact artifact) {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        List<org.apache.maven.model.Dependency> managedDeps = AntBuildMojo.getInstance()
                                                                          .getProject()
                                                                          .getDependencyManagement()
                                                                          .getDependencies();
        for (org.apache.maven.model.Dependency dependency : managedDeps) {
            Artifact managedArtifact = RepositoryUtils.toDependency(dependency,
                    mojo.getSession().getArtifactTypeRegistry()).getArtifact();
            if (ArtifactIdUtils.equalsVersionlessId(managedArtifact, artifact)) {
                artifact = artifact.setVersion(managedArtifact.getVersion());
                AntClient.getInstance().log("Managed version set on " + artifact);
                break;
            }
        }
        return artifact;
    }

    /**
     * Look for the newest available version in the configured repositories.
     *
     * @return The new artifact if the version changed, else the original one
     */
    public static Artifact setNewestVersion(Artifact artifact) {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        artifact = artifact.setVersion("[0,)");
        VersionRangeRequest rangeRequest = new VersionRangeRequest(artifact, AntBuildMojo.getInstance()
                                                                                         .getRemoteRepositories(), null);
        try {
            VersionRangeResult rangeResult = mojo.getSystem().resolveVersionRange(mojo.getSession(), rangeRequest);
            AntClient.getInstance().log(
                    String.format("Versions found for %s: %s", artifact, rangeResult.getVersions()), Project.MSG_DEBUG);
            artifact = artifact.setVersion(rangeResult.getHighestVersion().toString());
            AntClient.getInstance().log("Highest version found set on " + artifact);
        } catch (VersionRangeResolutionException e) {
            AntClient.getInstance().log(e.getMessage(), e, Project.MSG_ERR);
        }
        return artifact;
    }

    /**
     * TODO NXBT-696 manage depth limit
     */
    public static DependencyResult resolveDependencies(DependencyNode node, Filter filter, int depth) {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        AntClient.getInstance().log(String.format("Resolving %s with filter %s and depth %d", node, filter, depth),
                Project.MSG_DEBUG);
        DependencyRequest dependencyRequest = new DependencyRequest(node, filter);
        try {
            DependencyResult result = mojo.getSystem().resolveDependencies(mojo.getSession(), dependencyRequest);
            AntClient.getInstance().log("Dependency result: " + result, new Error(), Project.MSG_DEBUG);
            AntClient.getInstance().log("Dependency exceptions: " + result.getCollectExceptions(), Project.MSG_DEBUG);
            return result;
        } catch (DependencyResolutionException e) {
            throw new BuildException("Cannot resolve dependency tree for " + node, e);
        }
    }

}
