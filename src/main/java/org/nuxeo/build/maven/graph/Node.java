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
 *     bstefanescu, jcarsique, slacoin
 */
package org.nuxeo.build.maven.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.Project;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.nuxeo.build.ant.AntClient;

/**
 * TODO NXBT-258
 */
public class Node implements DependencyNode {

    protected final Graph graph;

    protected final String id;

    @Deprecated
    private List<char[]> acceptedCategories;

    private List<DependencyNode> parents = new ArrayList<>();

    protected DependencyNode dependencyNode;

    /**
     * @deprecated since 2.0
     */
    @Deprecated
    public List<char[]> getAcceptedCategories() {
        if (acceptedCategories == null) {
            acceptedCategories = new ArrayList<>();
        }
        return acceptedCategories;
    }

    public static String genNodeId(Artifact artifact) {
        StringBuilder sb = new StringBuilder();
        sb.append(artifact.getGroupId());
        sb.append(':').append(artifact.getArtifactId());
        sb.append(':').append(artifact.getVersion());
        sb.append(':').append(artifact.getType());
        sb.append(':');
        if (artifact.getClassifier() != null) {
            sb.append(artifact.getClassifier());
        }
        sb.append(':').append(artifact.getScope());
        return sb.toString();
    }

    public static String genNodeId(Dependency dependency) {
        org.eclipse.aether.artifact.Artifact artifact = dependency.getArtifact();
        StringBuilder sb = new StringBuilder();
        sb.append(artifact.getGroupId());
        sb.append(':').append(artifact.getArtifactId());
        sb.append(':').append(artifact.getVersion());
        sb.append(':').append(artifact.getExtension());
        sb.append(':');
        if (artifact.getClassifier() != null) {
            sb.append(artifact.getClassifier());
        }
        sb.append(':').append(dependency.getScope());
        return sb.toString();
    }

    public Node(Node node) {
        this.dependencyNode = node.dependencyNode;
        this.id = node.id;
        this.graph = node.graph;
        this.acceptedCategories = node.acceptedCategories;
        this.parents = node.parents;
    }

    /**
     * @since 2.0
     */
    public Node(Graph graph, DependencyNode dependencyNode) {
        this.graph = graph;
        this.dependencyNode = dependencyNode;
        this.id = genNodeId(dependencyNode.getDependency());
    }

    public Artifact getMavenArtifact() {
        return DependencyUtils.toMavenArtifact(getDependency());
    }

    public File getFile() {
        File file = getArtifact().getFile();
        if (file == null) {
            try {
                file = DependencyUtils.resolve(getArtifact()).getFile();
            } catch (ArtifactResolutionException e) {
                AntClient.getInstance().log(e.getMessage(), e, Project.MSG_ERR);
                return null;
            }
        }
        return file;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Node) {
            return ((Node) obj).id.equals(this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getArtifact().toString();
    }

    /**
     * @param pattern
     * @deprecated since 2.0
     */
    @Deprecated
    public void setAcceptedCategory(char[] pattern) {
        getAcceptedCategories().add(pattern);
    }

    /**
     * @param patterns
     * @return true if at least one pattern has been accepted
     * @deprecated since 2.0
     */
    @Deprecated
    public boolean isAcceptedCategory(List<char[]> patterns) {
        for (char[] pattern : patterns) {
            if (getAcceptedCategories().contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<DependencyNode> getChildren() {
        return dependencyNode.getChildren();
    }

    @Override
    public Dependency getDependency() {
        return dependencyNode.getDependency();
    }

    @Override
    public void setArtifact(org.eclipse.aether.artifact.Artifact artifact) {
        dependencyNode.setArtifact(artifact);

    }

    @Override
    public List<? extends org.eclipse.aether.artifact.Artifact> getRelocations() {
        return dependencyNode.getRelocations();
    }

    @Override
    public Collection<? extends org.eclipse.aether.artifact.Artifact> getAliases() {
        return dependencyNode.getAliases();
    }

    @Override
    public VersionConstraint getVersionConstraint() {
        return dependencyNode.getVersionConstraint();
    }

    @Override
    public Version getVersion() {
        return dependencyNode.getVersion();
    }

    @Override
    public void setScope(String scope) {
        dependencyNode.setScope(scope);
    }

    @Override
    public List<RemoteRepository> getRepositories() {
        return dependencyNode.getRepositories();
    }

    @Override
    public String getRequestContext() {
        return dependencyNode.getRequestContext();
    }

    @Override
    public void setRequestContext(String context) {
        dependencyNode.setRequestContext(context);
    }

    @Override
    public Map<?, ?> getData() {
        return dependencyNode.getData();
    }

    @Override
    public void setData(Object key, Object value) {
        dependencyNode.setData(key, value);
    }

    @Override
    public boolean accept(DependencyVisitor visitor) {
        return dependencyNode.accept(visitor);
    }

    @Deprecated
    public List<DependencyNode> getParents() {
        return this.parents;
    }

    /**
     * @since 2.0
     */
    public void addParent(Node node) {
        parents.add(node);
    }

    @Override
    public void setChildren(List<DependencyNode> children) {
        dependencyNode.setChildren(children);
    }

    @Override
    public void setOptional(Boolean optional) {
        dependencyNode.setOptional(optional);
    }

    @Override
    public int getManagedBits() {
        return dependencyNode.getManagedBits();
    }

    @Override
    public void setData(Map<Object, Object> data) {
        dependencyNode.setData(data);
    }

    @Override
    public org.eclipse.aether.artifact.Artifact getArtifact() {
        return dependencyNode.getArtifact();
    }
}
