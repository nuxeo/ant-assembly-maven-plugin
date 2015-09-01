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
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.JavaScopes;

import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.filter.AncestorFilter;
import org.nuxeo.build.maven.filter.AndFilter;
import org.nuxeo.build.maven.filter.ArtifactIdFilter;
import org.nuxeo.build.maven.filter.ClassifierFilter;
import org.nuxeo.build.maven.filter.CompositeFilter;
import org.nuxeo.build.maven.filter.Filter;
import org.nuxeo.build.maven.filter.GroupIdFilter;
import org.nuxeo.build.maven.filter.IsOptionalFilter;
import org.nuxeo.build.maven.filter.NotFilter;
import org.nuxeo.build.maven.filter.ScopeFilter;
import org.nuxeo.build.maven.filter.TypeFilter;
import org.nuxeo.build.maven.filter.VersionFilter;
import org.nuxeo.build.maven.graph.DependencyUtils;
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

    protected Collection<Artifact> artifacts;

    private boolean scopeTest = false;

    private boolean scopeProvided = false;

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
        // Exclude test and provided scopes by default
        scopeTest = JavaScopes.TEST.equals(scope) || "*".equals(scope);
        scopeProvided = JavaScopes.PROVIDED.equals(scope) || "*".equals(scope);
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
        src = importFile;
    }

    public void addExpand(@SuppressWarnings("hiding") Expand expand) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        this.expand = expand;
    }

    public void addFile(ArtifactFile artifact) {
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
            throw new BuildException("Found an Includes that is defined more than once in an artifactSet");
        }
        this.includes = includes;
    }

    public void addExcludes(@SuppressWarnings("hiding") Excludes excludes) {
        if (isReference()) {
            throw noChildrenAllowed();
        }
        if (this.excludes != null) {
            throw new BuildException("Found an Excludes that is defined more than once in an artifactSet");
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

    protected Filter buildFilter() {
        AndFilter f = new AndFilter();
        if (!filter.isEmpty()) {
            if (!scopeTest) {
                filter.addFilter(new NotFilter(new ScopeFilter(JavaScopes.TEST)));
            }
            if (!scopeProvided) {
                filter.addFilter(new NotFilter(new ScopeFilter(JavaScopes.PROVIDED)));
            }
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

    protected Collection<Artifact> computeNodes() {
        Collection<Artifact> resultArtifacts = new ArrayList<>();
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
                resultArtifacts.addAll(arti.getArtifacts());
            }
        }
        Filter finalFilter = buildFilter();
        int depth = Integer.MAX_VALUE;
        if (expand != null) {
            expand.filter.addFilter(finalFilter);
            finalFilter = CompositeFilter.compact(expand.filter);
            depth = expand.depth;
        }
        roots.addAll(AntBuildMojo.getInstance().getGraph().getRoots());
        for (Node node : roots) {
            DependencyResult result = DependencyUtils.resolveDependencies(node, finalFilter, depth);
            for (ArtifactResult artifactResult : result.getArtifactResults()) {
                resultArtifacts.add(artifactResult.getArtifact());
            }
            // root artifact is always kept; filter it out if not acceptable
            if (!finalFilter.accept(result.getRoot(), null)) {
                Artifact root = result.getRoot().getArtifact();
                resultArtifacts.remove(root);
            }
        }
        return resultArtifacts;
    }

    public Collection<Artifact> getArtifacts() {
        if (isReference()) {
            return getRef(getProject()).getArtifacts();
        }
        if (artifacts == null) {
            artifacts = computeNodes();
        }
        AntClient.getInstance().log("ArtifactSet.getArtifacts() " + artifacts, new Error(), Project.MSG_DEBUG);
        if (id != null) { // avoid caching if artifactSet is referencable
            Collection<Artifact> copy = artifacts;
            artifacts = null;
            return copy;
        }
        return artifacts;
    }

    @Override
    public Iterator<Resource> iterator() {
        return createIterator(getArtifacts());
    }

    public static Iterator<Resource> createIterator(Collection<Artifact> collection) {
        List<Resource> files = new ArrayList<>();
        for (Artifact artifact : collection) {
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
        return getArtifacts().size();
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
            throw new BuildException("Failed to import artifacts file: " + src, e);
        }
    }

}
