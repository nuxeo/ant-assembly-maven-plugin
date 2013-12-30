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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class VersionFilter implements Filter {

    @Override
    public String toString() {
        return "" + getClass() + " [" + matcher + "]";
    }

    protected SegmentMatch matcher;

    public VersionFilter(String pattern) {
        this(SegmentMatch.parse(pattern));
    }

    public VersionFilter(SegmentMatch matcher) {
        this.matcher = matcher;
    }

    public boolean match(String segment) {
        return matcher.match(segment);
    }

    public boolean accept(Edge edge, Dependency dep) {
        return matcher.match(dep.getVersion());
    }

    public boolean accept(Edge edge) {
        return matcher.match(edge.out.getArtifact().getVersion());
    }

    public boolean accept(Artifact artifact) {
        return matcher.match(artifact.getVersion());
    }

    public boolean accept(Node node) {
        return accept(node.getArtifact());
    }
}
