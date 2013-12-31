/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.project.MavenProject;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.filter.Filter;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.DefaultDependencyNode;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;

/**
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Node implements DependencyNode {

    protected final Graph graph;

    protected final String id;

    protected final Artifact artifact;

    protected final List<Edge> edgesIn = new ArrayList<>();

    protected final List<Edge> edgesOut = new ArrayList<>();

    protected final MavenProject pom;

    private List<char[]> acceptedCategories;

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

    public Node(Node node) {
        this.id = node.id;
        this.graph = node.graph;
        this.artifact = node.artifact;
        this.edgesIn.addAll(node.edgesIn);
        this.edgesOut.addAll(node.edgesOut);
        this.pom = node.pom;
        this.dependencyNode = node.dependencyNode;
    }

    protected Node(Graph graph, Artifact artifact, MavenProject pom) {
        this.id = genNodeId(artifact);
        this.graph = graph;
        this.artifact = artifact;
        this.pom = pom;
        // TODO NXBT-258 empty dependencyNode ok?
        this.dependencyNode = new DefaultDependencyNode(new Dependency(
                new DefaultArtifact(artifact.getGroupId(),
                        artifact.getArtifactId(),
                        // artifact.getClassifier(), "pom",
                        artifact.getClassifier(), artifact.getType(),
                        artifact.getVersion()), artifact.getScope()));
        // try {
        // this.dependencyNode = graph.collectDependencies(pom);
        // } catch (DependencyCollectionException e) {
        // throw new BuildException(e);
        // }
    }

    protected static final int UNKNOWN = 0;

    protected static final int INCLUDED = 1;

    protected static final int OMITTED = 2;

    protected static final int FILTERED = 3;

    protected int state = UNKNOWN;

    /**
     * Default format with GAV: group:artifact:version:type:classifier
     *
     * @since 1.10.2
     */
    public static final int FORMAT_GAV = 0;

    /**
     * Key-value format: FILENAME=GAV
     *
     * @since 1.10.2
     */
    public static final int FORMAT_KV_F_GAV = 1;

    public Artifact getArtifact() {
        return artifact;
    }

    public File getFile() {
        if (!artifact.isResolved()) {
            try {
                graph.resolve(artifact);
            } catch (ArtifactNotFoundException e) {
                AntBuildMojo.getInstance().getLog().error(e);
                return null;
            }
        }
        File file = artifact.getFile();
        if (file != null) {
            // TODO NXBT-258: getName or getAbsolutePath?!
            graph.file2artifacts.put(file.getName(), artifact);
        }
        return file;
    }

    public File getFile(String classifier) {
        Artifact ca = AntBuildMojo.getInstance().getArtifactFactory().createArtifactWithClassifier(
                artifact.getGroupId(), artifact.getArtifactId(),
                artifact.getVersion(), artifact.getType(), classifier);
        try {
            graph.resolve(ca);
            File file = ca.getFile();
            if (file != null) {
                // TODO NXBT-258: getName or getAbsolutePath?!
                graph.file2artifacts.put(file.getAbsolutePath(), ca);
            }
            return file;
        } catch (ArtifactNotFoundException e) {
            AntBuildMojo.getInstance().getLog().error(e);
            return null;
        }
    }

    public boolean isRoot() {
        return edgesIn.isEmpty();
    }

    public String getId() {
        return id;
    }

    public Collection<Edge> getEdgesOut() {
        return edgesOut;
    }

    public Collection<Edge> getEdgesIn() {
        return edgesIn;
    }

    protected void addEdgeIn(Edge edge) {
        edgesIn.add(edge);
    }

    protected void addEdgeOut(Edge edge) {
        edgesOut.add(edge);
    }

    public MavenProject getPom() {
        return pom;
    }

    public MavenProject getPomIfAlreadyLoaded() {
        return pom;
    }

    public List<Node> getTrail() {
        if (edgesIn.isEmpty()) {
            ArrayList<Node> result = new ArrayList<>();
            result.add(this);
            return result;
        }
        Edge edge = edgesIn.get(0);
        List<Node> path = edge.in.getTrail();
        path.add(this);
        return path;
    }

    public void collectNodes(Collection<Node> nodes, Filter filter) {
        for (Edge edge : edgesOut) {
            if (filter.accept(edge)) {
                nodes.add(edge.out);
            }
        }
    }

    public void collectNodes(Collection<Node> nodes) {
        for (Edge edge : edgesOut) {
            nodes.add(edge.out);
        }
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
        return artifact.toString();
    }

    /**
     * @param pattern
     */
    public void setAcceptedCategory(char[] pattern) {
        getAcceptedCategories().add(pattern);
    }

    /**
     * @param patterns
     * @return true if at least one pattern has been accepted
     */
    public boolean isAcceptedCategory(List<char[]> patterns) {
        for (char[] pattern : patterns) {
            if (getAcceptedCategories().contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private void expand(Filter filter, int depth) {
        graph.resolveDependencies(this, filter, depth);
    }

    /**
     * @param format output format
     * @return String representation depending on format
     * @see #FORMAT_GAV
     * @see #FORMAT_KV_F_GAV
     * @since 1.10.2
     */
    public String toString(int format) {
        switch (format) {
        case FORMAT_GAV:
            return toString();

        case FORMAT_KV_F_GAV:
            String toString;
            try {
                if (artifact.getFile() == null) {
                    graph.resolve(artifact);
                }
                toString = artifact.getFile().getName();
            } catch (ArtifactNotFoundException e) {
                toString = "ArtifactNotFound";
            }
            toString += "=" + id;
            return toString;

        default:
            return "Unknown format: " + format + "!";
        }
    }

    // TODO NXBT-258

    protected DependencyNode dependencyNode;

    @Override
    public List<DependencyNode> getChildren() {
        return dependencyNode.getChildren();
    }

    @Override
    public Dependency getDependency() {
        return dependencyNode.getDependency();
    }

    @Override
    public void setArtifact(org.sonatype.aether.artifact.Artifact artifact) {
        dependencyNode.setArtifact(artifact);
    }

    @Override
    public List<org.sonatype.aether.artifact.Artifact> getRelocations() {
        return dependencyNode.getRelocations();
    }

    @Override
    public Collection<org.sonatype.aether.artifact.Artifact> getAliases() {
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
    public String getPremanagedVersion() {
        return dependencyNode.getPremanagedVersion();
    }

    @Override
    public String getPremanagedScope() {
        return dependencyNode.getPremanagedScope();
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
    public Map<Object, Object> getData() {
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
}
