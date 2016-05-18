/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;

/**
 * TODO NXBT-258
 */
public class AncestorFilter extends CompositeFilter {

    public AncestorFilter(String pattern) {
        addFiltersFromPattern(pattern);
    }

    @Override
    public boolean accept(Artifact artifact) {
        throw new UnsupportedOperationException("Ancestor folder cannot be applied on artifact objects");
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        beforeAccept(node);
        for (DependencyNode parent : parents) {
            if (accept(parent)) {
                return result(true, node.toString());
            }
        }
        return result(false, node.toString());
    }

    /**
     * @since 2.0
     */
    protected boolean accept(DependencyNode parent) {
        for (Filter filter : filters) {
            if (!filter.accept(parent, Collections.<DependencyNode> emptyList())) {
                return false;
            }
        }
        return true;
    }

}
