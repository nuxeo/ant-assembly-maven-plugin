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
import org.eclipse.aether.graph.DependencyNode;

/**
 * TODO NXBT-258
 */
public class ScopeFilter extends AbstractFilter {

    @Override
    public String toString() {
        return "" + getClass() + " [" + matcher + "]";
    }

    protected SegmentMatch matcher;

    public ScopeFilter(String pattern) {
        this(SegmentMatch.parse(pattern));
    }

    public ScopeFilter(SegmentMatch matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean accept(Artifact artifact) {
        return result(match(artifact.getScope()), artifact.toString());
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        org.eclipse.aether.graph.Dependency dependency = node.getDependency();
        if (dependency == null) {
            return result(matcher == SegmentMatch.ANY, node.toString());
        }
        return result(match(dependency.getScope()), node.toString());
    }

    private boolean match(String scope) {
        if (scope == null) {
            return matcher == SegmentMatch.ANY;
        }
        return matcher.match(scope);
    }
}
