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
 *     bstefanescu, slacoin, jcarsique
 */
package org.nuxeo.build.ant.artifact;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.filter.AndFilter;
import org.nuxeo.build.maven.filter.CompositeFilter;
import org.nuxeo.build.maven.filter.Filter;
import org.nuxeo.build.maven.graph.Graph;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ArtifactDependencies extends DataType implements
        ResourceCollection {

    protected Graph graph = AntBuildMojo.getInstance().getGraph();

    protected Node node;

    protected List<Artifact> nodes;

    public String key;

    public int depth = 1;

    public ArtifactDescriptor ad = ArtifactDescriptor.emptyDescriptor();

    public Includes includes;

    public Excludes excludes;

    public void setDepth(String depth) {
        if ("all".equals(depth)) {
            this.depth = Integer.MAX_VALUE;
        } else {
            this.depth = Integer.parseInt(depth);
            if (this.depth == 0) {
                throw new IllegalArgumentException(
                        "0 is not a valid value for depth");
            }
        }
    }

    public void addExcludes(@SuppressWarnings("hiding") Excludes excludes) {
        if (this.excludes != null) {
            throw new BuildException(
                    "Found an Excludes that is defined more than once in an artifact dependencies");
        }
        this.excludes = excludes;
    }

    public void addIncludes(@SuppressWarnings("hiding") Includes includes) {
        if (this.includes != null) {
            throw new BuildException(
                    "Found an Includes that is defined more than once in an artifact dependencies");
        }
        this.includes = includes;
    }

    public void setKey(String pattern) {
        key = pattern;
    }

    public void setArtifactId(String artifactId) {
        this.ad.artifactId = artifactId;
    }

    public void setGroupId(String groupId) {
        this.ad.groupId = groupId;
    }

    public void setType(String type) {
        this.ad.type = type;
    }

    public void setVersion(String version) {
        this.ad.version = version;
    }

    public Node getNode() {
        if (node == null) {
            if (key != null) {
                node = graph.findFirst(key);
            } else {
                node = graph.findNode(ad);
            }
            if (node == null) {
                throw new BuildException("Artifact with pattern "
                        + (key != null ? key : ad) + " was not found in graph");
            }
        }
        return node;
    }

    public List<Artifact> getArtifacts() {
        if (nodes == null) {
            Filter filter = null;
            if (includes != null || excludes != null) {
                AndFilter andf = new AndFilter();
                if (includes != null) {
                    andf.addFilter(includes.getFilter());
                }
                if (excludes != null) {
                    andf.addFilter(excludes.getFilter());
                }
                filter = CompositeFilter.compact(andf);
            }
            // make sure node is expanded
            // if not already expanded, this expand may not be done correctly
            DependencyResult result = graph.resolveDependencies(getNode(),
                    filter, Integer.MAX_VALUE);
            List<ArtifactResult> results = result.getArtifactResults();
            nodes = new ArrayList<>();
            for (ArtifactResult artifactResult : results) {
                nodes.add(artifactResult.getArtifact());
            }
        }
        return nodes;
    }

    @Override
    public Iterator<Resource> iterator() {
        return ArtifactSet.createIterator(getArtifacts());
    }

    @Override
    public int size() {
        return getArtifacts().size();
    }

    @Override
    public boolean isFilesystemOnly() {
        return true;
    }
}
