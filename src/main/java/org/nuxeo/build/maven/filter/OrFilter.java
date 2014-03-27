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
 *     bstefanescu, slacoin, jcarsique
 */
package org.nuxeo.build.maven.filter;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.Project;
import org.eclipse.aether.graph.DependencyNode;
import org.nuxeo.build.ant.AntClient;

/**
 * TODO NXBT-258
 */
public class OrFilter extends CompositeFilter {

    @Override
    public String toString() {
        return super.toString() + " => ";
    }

    public OrFilter() {
        super();
    }

    public OrFilter(List<Filter> filters) {
        super(filters);
    }

    @Override
    public boolean accept(Artifact artifact) {
        for (Filter filter : filters) {
            if (filter.accept(artifact)) {
                return result(true, artifact.toString());
            }
        }
        return result(false, artifact.toString());
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        AntClient.getInstance().log("Filtering - " + super.toString() + "...",
                Project.MSG_DEBUG);
        for (Filter filter : filters) {
            if (filter.accept(node, parents)) {
                return result(true, node.toString());
            }
        }
        return result(false, node.toString());
    }

}
