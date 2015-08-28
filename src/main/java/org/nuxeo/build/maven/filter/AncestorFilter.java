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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.nuxeo.build.maven.ArtifactDescriptor;

/**
 * TODO NXBT-258
 */
public class AncestorFilter extends AbstractFilter {

    protected ArtifactDescriptor ad;

    protected List<Filter> filters;

    public AncestorFilter(String pattern) {
        ad = new ArtifactDescriptor(pattern);
        filters = new ArrayList<>();
        if (ad.groupId != null && !ad.groupId.equals("*")) {
            addFilter(new GroupIdFilter(ad.groupId));
        }
        if (ad.artifactId != null && !ad.artifactId.equals("*")) {
            addFilter(new ArtifactIdFilter(ad.artifactId));
        }
        if (ad.version != null && !ad.version.equals("*")) {
            addFilter(new VersionFilter(ad.version));
        }
        if (ad.type != null && !ad.type.equals("*")) {
            addFilter(new TypeFilter(ad.type));
        }
        if (ad.classifier != null && !ad.classifier.equals("*")) {
            addFilter(new ClassifierFilter(ad.classifier));
        }
        if (ad.scope != null && !ad.scope.equals("*")) {
            addFilter(new ScopeFilter(ad.scope));
        }
    }

    protected void addFilter(Filter filter) {
        filters.add(filter);
    }

    @Override
    public boolean accept(Artifact artifact) {
        throw new UnsupportedOperationException("Ancestor folder cannot be applied on artifact objects");
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        for (DependencyNode parent : parents) {
            if (accept(parent)) {
                return true;
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
