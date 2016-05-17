/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     jcarsique
 */
package org.nuxeo.build.maven.filter;

import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

import org.nuxeo.build.maven.graph.DependencyUtils;
import org.nuxeo.build.maven.graph.Node;

/**
 * Filters a given collection of {@link DependencyUtils#toMavenArtifact}.
 *
 * @since 2.0.6
 */
public class DependencyNodeFilter extends AbstractFilter implements Filter {

    private Collection<Node> excludedNodes;

    /**
     * @param excludedNodes nodes collection to exclude
     */
    public DependencyNodeFilter(Collection<Node> excludedNodes) {
        this.excludedNodes = excludedNodes;
    }

    @Override
    public boolean accept(Artifact artifact) {
        beforeAccept(artifact, " against %s", excludedNodes);
        for (Node node : excludedNodes) {
            if (artifact.equals(node.getMavenArtifact())) {
                return result(false, artifact.toString());
            }
        }
        return result(true, artifact.toString());
    }

    @Override
    public boolean accept(DependencyNode dependencyNode, List<DependencyNode> parents) {
        beforeAccept(dependencyNode, " against %s", excludedNodes);
        for (Node node : excludedNodes) {
            if (node.equals(dependencyNode)) {
                return result(false, dependencyNode.toString());
            }
        }
        return result(true, dependencyNode.toString());
    }

}
