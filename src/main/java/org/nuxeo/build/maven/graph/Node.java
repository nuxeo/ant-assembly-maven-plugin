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
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.Project;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.filter.Filter;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.repository.RemoteRepository;
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

    @Deprecated
    protected final Artifact artifact;

    @Deprecated
    protected final List<Edge> edgesIn = new ArrayList<>();

    @Deprecated
    protected final List<Edge> edgesOut = new ArrayList<>();

    protected final MavenProject pom;

    private List<char[]> acceptedCategories;

    private DependencyNode dependencyNode;

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
        this.dependencyNode = node.dependencyNode;
        this.id = node.id;
        this.graph = node.graph;
        this.artifact = node.artifact;
        this.edgesIn.addAll(node.edgesIn);
        this.edgesOut.addAll(node.edgesOut);
        this.pom = node.pom;
    }

    protected Node(Graph graph, Artifact artifact, MavenProject pom,
            DependencyNode dependencyNode) {
        this.dependencyNode = dependencyNode;
        this.id = genNodeId(artifact);
        this.graph = graph;
        this.artifact = artifact;
        this.pom = pom;
    }

    protected static final int UNKNOWN = 0;

    protected static final int INCLUDED = 1;

    protected static final int OMITTED = 2;

    protected static final int FILTERED = 3;

    protected int state = UNKNOWN;

    public Artifact getArtifact() {
        return aetherToMavenArtifact(
                getDependency().getArtifact(),
                getDependency().getScope(),
                AntBuildMojo.getInstance().getArtifactHandlerManager().getArtifactHandler(
                        getDependency().getArtifact().getExtension()));
    }

    public File getFile() {
        File file = getDependency().getArtifact().getFile();
        if (file != null) {
            // TODO NXBT-258: getName or getAbsolutePath?!
            graph.file2artifacts.put(file.getName(), getArtifact());
        }
        return file;
    }

    public File getFile(String classifier) {
        Artifact artifact = getArtifact();
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
            AntClient.getInstance().log(e.getMessage(), e, Project.MSG_ERR);
            return null;
        }
    }

    @Deprecated
    public boolean isRoot() {
        return edgesIn.isEmpty();
    }

    public String getId() {
        return id;
    }

    @Deprecated
    public Collection<Edge> getEdgesOut() {
        return edgesOut;
    }

    @Deprecated
    public Collection<Edge> getEdgesIn() {
        return edgesIn;
    }

    @Deprecated
    protected void addEdgeIn(Edge edge) {
        edgesIn.add(edge);
    }

    @Deprecated
    protected void addEdgeOut(Edge edge) {
        edgesOut.add(edge);
    }

    public MavenProject getPom() {
        return pom;
    }

    public MavenProject getPomIfAlreadyLoaded() {
        return pom;
    }

    @Deprecated
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

    @Deprecated
    public void collectNodes(Collection<Node> nodes, Filter filter) {
        for (Edge edge : edgesOut) {
            if (filter.accept(edge)) {
                nodes.add(edge.out);
            }
        }
    }

    @Deprecated
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
        return getArtifact().toString();
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

    /**
     * @deprecated
     */
    @Deprecated
    private void expand(Filter filter, int depth) {
        graph.resolveDependencies(this, filter, depth);
    }

    public static Artifact aetherToMavenArtifact(
            org.sonatype.aether.artifact.Artifact aetherArtifact, String scope,
            ArtifactHandler artifactHandler) {
        org.apache.maven.artifact.DefaultArtifact mavenArtifact = new org.apache.maven.artifact.DefaultArtifact(
                aetherArtifact.getGroupId(), aetherArtifact.getArtifactId(),
                aetherArtifact.getVersion(), scope,
                aetherArtifact.getExtension(), aetherArtifact.getClassifier(),
                artifactHandler);
        mavenArtifact.setFile(aetherArtifact.getFile());
        // TODO NXBT-258 is already resolved?
        mavenArtifact.setResolved(true);
        return mavenArtifact;
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
