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
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ClassifierFilter extends AbstractFilter {

    @Override
    public String toString() {
        return "" + getClass() + " [" + matcher + "]";
    }

    protected SegmentMatch matcher;

    public ClassifierFilter(String pattern) {
        this(SegmentMatch.parse(pattern));
    }

    public ClassifierFilter(SegmentMatch matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean accept(Artifact artifact) {
        return result(match(artifact.getClassifier()), artifact.toString());
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        Dependency dependency = node.getDependency();
        if (dependency == null) {
            return result(matcher == SegmentMatch.ANY, node.toString());
        }
        return result(match(dependency.getArtifact().getClassifier()),
                node.toString());
    }

    private boolean match(String classifier) {
        if (classifier == null) {
            return matcher == SegmentMatch.ANY;
        }
        return matcher.match(classifier);
    }
}
