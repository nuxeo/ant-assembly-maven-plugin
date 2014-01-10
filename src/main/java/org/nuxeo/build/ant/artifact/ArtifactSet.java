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
package org.nuxeo.build.ant.artifact;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.graph.visitor.PostorderNodeListGenerator;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.filter.AncestorFilter;
import org.nuxeo.build.maven.filter.AndFilter;
import org.nuxeo.build.maven.filter.ArtifactIdFilter;
import org.nuxeo.build.maven.filter.ClassifierFilter;
import org.nuxeo.build.maven.filter.CompositeFilter;
import org.nuxeo.build.maven.filter.Filter;
import org.nuxeo.build.maven.filter.GroupIdFilter;
import org.nuxeo.build.maven.filter.IsOptionalFilter;
import org.nuxeo.build.maven.filter.ScopeFilter;
import org.nuxeo.build.maven.filter.TypeFilter;
import org.nuxeo.build.maven.filter.VersionFilter;
import org.nuxeo.build.maven.graph.Graph;
import org.nuxeo.build.maven.graph.Node;

/**
 * TODO NXBT-258
 */
public class ArtifactSet extends DataType implements ResourceCollection {

    public AndFilter filter = new AndFilter();

    public String id;

    public File src;

    public Expand expand;

    public List<ArtifactFile> artifactFiles;

    public List<ArtifactSet> artifactSets;

    public Includes includes;

    public Excludes excludes;

    protected Collection<Node> nodes;

    public void setGroupId(String groupId) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new GroupIdFilter(groupId));
    }

    public void setArtifactId(String artifactId) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new ArtifactIdFilter(artifactId));
    }

    public void setVersion(String version) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new VersionFilter(version));
    }

    public void setClassifier(String classifier) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new ClassifierFilter(classifier));
    }

    public void setType(String type) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new TypeFilter(type));
    }

    public void setScope(String scope) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new ScopeFilter(scope));
    }

    public void setOptional(boolean isOptional) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new IsOptionalFilter(isOptional));
    }

    public void setPattern(String pattern) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFiltersFromPattern(pattern);
    }

    public void setAncestor(String ancestor) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        filter.addFilter(new AncestorFilter(ancestor));
    }

    public void setId(String id) {
        if (isReference()) {
            throw tooManyAttributes();
        }
        this.id = id;
    }

    public void setSrc(File importFile) {
        this.src = importFile;
    }

    public void addExpand(@SuppressWarnings("hiding") Expand expand) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        this.expand = expand;
    }

    public void addArtifact(ArtifactFile artifact) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (artifactFiles == null) {
            artifactFiles = new ArrayList<>();
        }
        artifactFiles.add(artifact);
    }

    public void addArtifactSet(ArtifactSet set) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (artifactSets == null) {
            artifactSets = new ArrayList<>();
        }
        artifactSets.add(set);
    }

    public void addIncludes(@SuppressWarnings("hiding") Includes includes) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.includes != null) {
            throw new BuildException(
                    "Found an Includes that is defined more than once in an artifactSet");
        }
        this.includes = includes;
    }

    public void addExcludes(@SuppressWarnings("hiding") Excludes excludes) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.excludes != null) {
            throw new BuildException(
                    "Found an Excludes that is defined more than once in an artifactSet");
        }
        this.excludes = excludes;
    }

    @Override
    public void setRefid(Reference ref) {
        super.setRefid(ref);
    }

    protected ArtifactSet getRef(Project p) {
        return (ArtifactSet) getCheckedRef(p);
    }

    protected List<Node> createInputNodeList() {
        if (includes == null && excludes == null) {
            return new ArrayList<>();
        }
        final AndFilter ieFilter = new AndFilter();
        if (includes != null) {
            ieFilter.addFilter(includes.getFilter());
        }
        if (excludes != null) {
            ieFilter.addFilter(excludes.getFilter());
        }
        return new ArrayList<Node>() {
            private static final long serialVersionUID = 1L;

            Filter f = CompositeFilter.compact(ieFilter);

            @Override
            public boolean add(Node node) {
                if (!f.accept(node.getMavenArtifact())) {
                    return false;
                }
                return super.add(node);
            }

            @Override
            public boolean addAll(Collection<? extends Node> c) {
                for (Node node : c) {
                    if (f.accept(node.getMavenArtifact())) {
                        super.add(node);
                    }
                }
                return true;
            }
        };
    }

    protected Filter buildFilter() {
        AndFilter f = new AndFilter();
        if (!filter.isEmpty()) {
            f.addFilters(filter.getFilters());
        }
        if (includes != null) {
            f.addFilter(includes.getFilter());
        }
        if (excludes != null) {
            f.addFilter(excludes.getFilter());
        }
        return f.isEmpty() ? Filter.ANY : CompositeFilter.compact(f);
    }

    protected Collection<Node> computeNodes() {
        Collection<Node> roots = new ArrayList<>();
        if (src != null) {
            collectImportedNodes(roots);
        }
        if (artifactFiles != null) {
            for (ArtifactFile arti : artifactFiles) {
                roots.add(arti.getNode());
            }
        }
        if (artifactSets != null) {
            for (ArtifactSet arti : artifactSets) {
                roots.addAll(arti.getNodes());
            }
        }
        Graph graph;
        if (roots.isEmpty()) {
            graph = AntBuildMojo.getInstance().newGraph();
        } else {
            graph = new Graph();
            for (Node root : roots) {
                graph.addRootNode(root);
            }
        }
        Filter finalFilter = buildFilter();
        int depth = Integer.MAX_VALUE;
        if (expand != null) {
            expand.filter.addFilter(finalFilter);
            finalFilter = CompositeFilter.compact(expand.filter);
            depth = expand.depth;
        }
        PostorderNodeListGenerator nlg = new PostorderNodeListGenerator();
        for (Node node : graph.getRoots()) {
            DependencyResult result = graph.resolveDependencies(node,
                    finalFilter, depth);
            result.getRoot().accept(nlg);
        }
        Collection<Node> resultNodes = new ArrayList<>();
        for (Dependency dependency : nlg.getDependencies(true)) {
            resultNodes.add(graph.getNode(dependency));
        }
        return resultNodes;
    }

    public Collection<Node> getNodes() {
        if (isReference()) {
            return getRef(getProject()).getNodes();
        }
        if (nodes == null) {
            nodes = computeNodes();
        }
        if (id != null) { // avoid caching if artifactSet is referencable
            Collection<Node> copy = nodes;
            nodes = null;
            return copy;
        }
        return nodes;
    }

    @Override
    public Iterator<Resource> iterator() {
        return createIterator(getNodes());
    }

    public static Iterator<Resource> createIterator(Collection<Node> nodes) {
        List<Resource> files = new ArrayList<>();
        for (Node node : nodes) {
            File file = node.getFile();
            if (file != null) {
                FileResource fr = new FileResource(file);
                fr.setBaseDir(file.getParentFile());
                files.add(fr);
            }
        }
        return files.iterator();
    }

    /**
     * @since 2.0
     */
    public static Iterator<Resource> createIterator(List<Artifact> artifacts) {
        List<Resource> files = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            File file = artifact.getFile();
            if (file != null) {
                FileResource fr = new FileResource(file);
                fr.setBaseDir(file.getParentFile());
                files.add(fr);
            }
        }
        return files.iterator();
    }

    @Override
    public int size() {
        return getNodes().size();
    }

    @Override
    public boolean isFilesystemOnly() {
        return true;
    }

    public void collectImportedNodes(Collection<Node> nodesCollection) {
        try {
            ArtifactSetParser parser = new ArtifactSetParser(getProject());
            parser.parse(src, nodesCollection);
        } catch (IOException e) {
            throw new BuildException("Failed to import artifacts file: " + src,
                    e);
        }
    }

}
