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
public class VersionFilter extends AbstractFilter {

    @Override
    public String toString() {
        return super.toString() + " [" + matcher + "]";
    }

    protected SegmentMatch matcher;

    public VersionFilter(String pattern) {
        this(SegmentMatch.parse(pattern));
    }

    public VersionFilter(SegmentMatch matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean accept(Artifact artifact) {
        return result(matcher.match(artifact.getVersion()), artifact.toString());
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        return result(matcher.match(node.getVersion().toString()),
                node.toString());
    }
}
