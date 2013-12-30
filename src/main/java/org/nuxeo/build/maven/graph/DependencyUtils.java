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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;

/**
 * Utility class for managing Maven dependencies.
 *
 * @since 2.0
 */
public class DependencyUtils {

    private DependencyUtils() {
    }

    public static List<Artifact> collectAllDependencies(
            List<Dependency> dependencies) {
        List<Artifact> result = new ArrayList<Artifact>(dependencies.size());
        for (Dependency dependency : dependencies) {
            result.add(dependency.getArtifact());
        }
        return result;
    }

    public static List<Artifact> collectDirectDependencies(
            List<DependencyNode> dependencies) {
        List<Artifact> result = new ArrayList<Artifact>(dependencies.size());
        for (DependencyNode dependencyNode : dependencies) {
            result.add(dependencyNode.getDependency().getArtifact());
        }
        return result;
    }

    public CollectRequest getCollectRequest(MavenProject project,
            List<RemoteRepository> repos) {
        Artifact a = new DefaultArtifact(project.getArtifact().toString());
        DefaultArtifact pom = new DefaultArtifact(a.getGroupId(),
                a.getArtifactId(), "pom", a.getVersion());
        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot(new Dependency(pom, "compile"));
        collectRequest.setRepositories(repos);
        return collectRequest;
    }

}
