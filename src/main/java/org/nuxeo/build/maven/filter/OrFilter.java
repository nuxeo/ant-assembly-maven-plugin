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
 *     bstefanescu, slacoin
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
public class OrFilter extends CompositeFilter {

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("" + getClass());
        for (Filter filter : filters) {
            sb.append(System.getProperty("line.separator") + filter);
        }
        return sb.toString();
    }

    public OrFilter() {
        super();
    }

    public OrFilter(List<Filter> filters) {
        super(filters);
    }

    @Override
    public boolean accept(Edge edge) {
        for (Filter filter : filters) {
            if (filter.accept(edge)) {
                return result(true, edge.toString());
            }
        }
        return result(false, edge.toString());
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
    public boolean accept(Node node) {
        for (Filter filter : filters) {
            if (filter.accept(node)) {
                return result(true, node.toString());
            }
        }
        return result(false, node.toString());
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        for (Filter filter : filters) {
            if (filter.accept(node, parents)) {
                return result(true, node.toString());
            }
        }
        return result(false, node.toString());
    }

}
