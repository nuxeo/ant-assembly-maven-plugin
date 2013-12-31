/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.maven.filter;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;
import org.sonatype.aether.graph.DependencyNode;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class IsOptionalFilter extends AbstractFilter {

    protected boolean isOptional;

    public IsOptionalFilter(boolean isOptional) {
        this.isOptional = isOptional;
    }

    @Override
    public boolean accept(Edge edge) {
        return result(isOptional == edge.isOptional, edge.toString());
    }

    @Override
    public boolean accept(Artifact artifact) {
        return result(isOptional == artifact.isOptional(), artifact.toString());
    }

    @Override
    public boolean accept(Node node) {
        return accept(node.getArtifact());
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        org.sonatype.aether.graph.Dependency dependency = node.getDependency();
        return result(dependency != null && dependency.isOptional(),
                node.toString());
    }
}
