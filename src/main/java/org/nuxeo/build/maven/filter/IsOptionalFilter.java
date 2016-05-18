/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.maven.filter;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

/**
 * TODO NXBT-258
 */
public class IsOptionalFilter extends AbstractFilter {

    protected boolean isOptional;

    public IsOptionalFilter(boolean isOptional) {
        this.isOptional = isOptional;
    }

    @Override
    public boolean accept(Artifact artifact) {
        beforeAccept(artifact);
        return result(isOptional == artifact.isOptional(), artifact.toString());
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        beforeAccept(node);
        org.eclipse.aether.graph.Dependency dependency = node.getDependency();
        return result(dependency != null && dependency.isOptional(), node.toString());
    }
}
