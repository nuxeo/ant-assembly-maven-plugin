/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-21.html
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
import org.eclipse.aether.graph.DependencyNode;

/**
 * TODO NXBT-258
 */
public class AndFilter extends CompositeFilter {

    public AndFilter() {
    }

    public AndFilter(List<Filter> filters) {
        super(filters);
    }

    @Override
    public boolean accept(Artifact artifact) {
        for (Filter filter : filters) {
            if (!filter.accept(artifact)) {
                return result(false, artifact.toString());
            }
        }
        return result(true, artifact.toString());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("" + getClass());
        for (Filter filter : filters) {
            sb.append(System.getProperty("line.separator") + filter);
        }
        return sb.toString();
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        for (Filter filter : filters) {
            if (!filter.accept(node, parents)) {
                return result(false, node.toString());
            }
        }
        return result(true, node.toString());
    }

}
