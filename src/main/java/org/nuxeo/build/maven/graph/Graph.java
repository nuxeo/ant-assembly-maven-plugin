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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.DefaultArtifactCollector;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.filter.Filter;
import org.nuxeo.build.maven.filter.VersionManagement;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencyCollectionException;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResolutionException;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.graph.DefaultDependencyNode;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Graph {

    public final TreeMap<String, Node> nodes = new TreeMap<>();

    public final LinkedList<Node> roots = new LinkedList<>();

    protected Map<String, Artifact> file2artifacts = new HashMap<>();

    // manage versions from dependency management -> lazy initialized when
    // required. (by calling artifact:resolveFile without a version)
    protected VersionManagement vmgr = new VersionManagement();

    public VersionManagement getVersionManagement() {
        return vmgr;
    }

    protected boolean shouldLoadDependencyManagement = false;

    private AntBuildMojo mojo;

    public void setShouldLoadDependencyManagement(
            boolean shouldLoadDependencyManagement) {
        this.shouldLoadDependencyManagement = shouldLoadDependencyManagement;
    }

    public boolean shouldLoadDependencyManagement() {
        return shouldLoadDependencyManagement;
    }

    public List<Node> getRoots() {
        return roots;
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    public Artifact getArtifactByFile(String fileName) {
        return file2artifacts.get(fileName);
    }

    public Graph() {
        mojo = AntBuildMojo.getInstance();
    }

    public void collectNodes(Collection<Node> nodesToCollect) {
        for (Node node : roots) {
            node.collectNodes(nodesToCollect);
        }
    }

    public void collectNodes(Collection<Node> nodesToCollect, Filter filter) {
        for (Node node : roots) {
            node.collectNodes(nodesToCollect, filter);
        }
    }

    public Node[] getNodesArray() {
        return nodes.values().toArray(new Node[nodes.size()]);
    }

    public Node findFirst(String pattern) {
        return findFirst(pattern, false);
    }

    public Node findFirst(String pattern, boolean stopIfNotUnique) {
        SortedMap<String, Node> map = nodes.subMap(pattern + ':', pattern
                + ((char) (':' + 1)));
        int size = map.size();
        if (size == 0) {
            return null;
        }
        if (stopIfNotUnique && size > 1) {
            throw new BuildException(
                    String.format(
                            "Pattern '%s' cannot be resolved to a unique node. Matching nodes are: %s",
                            pattern, map.values()));
        }
        return map.get(map.firstKey());
    }

    public Collection<Node> find(String pattern) {
        SortedMap<String, Node> map = nodes.subMap(pattern + ':', pattern
                + ((char) (':' + 1)));
        return map.values();
    }

    /**
     * Add a root node given an artifact POM. This can be used to initialize the
     * graph with the current POM.
     */
    public Node addRootNode(MavenProject pom) {
        return addRootNode(pom, pom.getArtifact());
    }

    public Node addRootNode(String key) {
        ArtifactDescriptor ad = new ArtifactDescriptor(key);
        Artifact artifact = ad.getArtifact();
        return addRootNode(artifact);
    }

    public Node addRootNode(Artifact artifact) {
        MavenProject pom = load(artifact);
        return addRootNode(pom, artifact);
    }

    /**
     * @since 1.10.2
     */
    public Node addRootNode(MavenProject pom, Artifact artifact) {
        Node node = nodes.get(Node.genNodeId(artifact));
        if (node == null) {
            String scope = artifact.getScope() != null ? artifact.getScope()
                    : "compile";
            AntClient.getInstance().log("artifact.getScope(): " + artifact.getScope(),
                    Project.MSG_DEBUG);
            DefaultDependencyNode newNode = new DefaultDependencyNode(
                    new Dependency(new DefaultArtifact(artifact.getGroupId(),
                            artifact.getArtifactId(), artifact.getClassifier(),
                            artifact.getType(), artifact.getVersion()), scope));
            CollectResult collectResult = collectDependencies(newNode);
            DependencyNode root = collectResult.getRoot();
            node = new Node(this, artifact, pom, root);
            nodes.put(node.getId(), node);
            nodesByArtifact.put(artifact, node);
            roots.add(node);
            AntClient.getInstance().log("Added root node: " + node,
                    Project.MSG_DEBUG);
        }
        return node;
    }

    private Node lookup(String id) {
        return nodes.get(id);
    }

    private Node lookup(Artifact artifact) {
        return lookup(Node.genNodeId(artifact));
    }

    public Node findNode(ArtifactDescriptor ad) {
        String key = ad.getNodeKeyPattern();
        Collection<Node> nodesToParse = null;
        if (key == null) {
            nodesToParse = getNodes();
        } else {
            nodesToParse = find(key);
        }
        Node returnNode = null;
        for (Node node : nodesToParse) {
            Artifact artifact = node.getArtifact();
            if (ad.artifactId != null
                    && !ad.artifactId.equals(artifact.getArtifactId())) {
                continue;
            }
            if (ad.groupId != null && !ad.groupId.equals(artifact.getGroupId())) {
                continue;
            }
            if (ad.version != null && !ad.version.equals(artifact.getVersion())) {
                continue;
            }
            if (ad.type != null && !ad.type.equals(artifact.getType())) {
                continue;
            }
            try {
                if (returnNode != null
                        && artifact.getSelectedVersion().compareTo(
                                returnNode.getArtifact().getSelectedVersion()) < 0) {
                    continue;
                }
            } catch (OverConstrainedVersionException e) {
                mojo.getLog().error(
                        "Versions comparison failed on " + artifact, e);
            }
            returnNode = node;
        }
        return returnNode;
    }

    @Deprecated
    public MavenProject loadPom(Artifact artifact) {
        if ("system".equals(artifact.getScope()))
            return null;
        try {
            return mojo.getMavenProjectBuilder().buildFromRepository(
                    // this create another Artifact instance whose type is 'pom'
                    mojo.getArtifactFactory().createProjectArtifact(
                            artifact.getGroupId(), artifact.getArtifactId(),
                            artifact.getVersion()),
                    mojo.getRemoteArtifactRepositories(),
                    mojo.getLocalRepository());
        } catch (ProjectBuildingException e) {
            mojo.getLog().error(e.getMessage(), e);
            return null;
        }
    }

    public final IdentityHashMap<Artifact, Node> nodesByArtifact = new IdentityHashMap<>();

    // TODO NXBT-258
    public void test(DependencyNode node) {
        // Convert org.apache.maven.artifact.Artifact to
        // org.sonatype.aether.artifact.Artifact
        // Artifact artifact = pom.getArtifact();
        // org.sonatype.aether.artifact.Artifact aetherArtifact = new
        // DefaultArtifact(
        // artifact.getGroupId(), artifact.getArtifactId(),
        // artifact.getClassifier(), artifact.getType(),
        // artifact.getVersion());
        // CollectRequest collectRequest = new CollectRequest();
        // collectRequest.setRoot(new Dependency(aetherArtifact,
        // artifact.getScope()));
        CollectRequest collectRequest = new CollectRequest(
                node.getDependency(), mojo.getRemoteRepositories());
        DependencyNode root;
        try {
            CollectResult collectResult = mojo.getSystem().collectDependencies(
                    mojo.getRepositorySystemSession(), collectRequest);
            AntClient.getInstance().log("collectResult: " + collectResult,
                    Project.MSG_DEBUG);
            root = collectResult.getRoot();
            AntClient.getInstance().log("Root: " + root, Project.MSG_DEBUG);
            AntClient.getInstance().log(
                    "root.getChildren(): "
                            + Arrays.toString(root.getChildren().toArray()),
                    Project.MSG_DEBUG);
            PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            node.accept(nlg);
            AntClient.getInstance().log(
                    "nlg.getDependencies(true): "
                            + Arrays.toString(nlg.getDependencies(true).toArray()),
                    Project.MSG_DEBUG);
            AntClient.getInstance().log(
                    "node.getChildren(): "
                            + Arrays.toString(node.getChildren().toArray()),
                    Project.MSG_DEBUG);
            DependencyRequest dependencyRequest = new DependencyRequest(root,
                    null);
            DependencyResult dependencyResult = mojo.getSystem().resolveDependencies(
                    mojo.getRepositorySystemSession(), dependencyRequest);
            AntClient.getInstance().log(
                    "dependencyResult: " + dependencyResult, Project.MSG_DEBUG);
        } catch (DependencyCollectionException | DependencyResolutionException e) {
            throw new BuildException(e);
        }
        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        root.accept(nlg);
        AntClient.getInstance().log(
                "nlg.getDependencies(true): "
                        + Arrays.toString(nlg.getDependencies(true).toArray()),
                Project.MSG_DEBUG);
        AntClient.getInstance().log(
                "root.getChildren(): "
                        + Arrays.toString(root.getChildren().toArray()),
                Project.MSG_DEBUG);
    }

    public CollectResult collectDependencies(DependencyNode node) {
        AntClient.getInstance().log(String.format("Collecting " + node),
                Project.MSG_DEBUG);
        CollectRequest collectRequest = new CollectRequest(
                node.getDependency(), mojo.getRemoteRepositories());
        try {
            CollectResult result = mojo.getSystem().collectDependencies(
                    mojo.getRepositorySystemSession(), collectRequest);
            node = result.getRoot();
            AntClient.getInstance().log("Collect result: " + result,
                    Project.MSG_DEBUG);
            AntClient.getInstance().log(
                    "Collect exceptions: " + result.getExceptions(),
                    Project.MSG_DEBUG);
            AntClient.getInstance().log(
                    "Direct dependencies: "
                            + String.valueOf(node.getChildren()),
                    Project.MSG_DEBUG);
            return result;
        } catch (DependencyCollectionException e) {
            throw new BuildException("Cannot collect dependency tree for "
                    + node, e);
        }
    }

    public DependencyResult resolveDependencies(DependencyNode node,
            Filter filter, int depth) {
        AntClient.getInstance().log(
                String.format("Resolving %s with filter %s and depth %d", node,
                        filter, depth), Project.MSG_DEBUG);
        DependencyRequest dependencyRequest = new DependencyRequest(node,
                filter);
        try {
            DependencyResult result = mojo.getSystem().resolveDependencies(
                    mojo.getRepositorySystemSession(), dependencyRequest);
            AntClient.getInstance().log("Dependency result: " + result,
                    Project.MSG_DEBUG);
            AntClient.getInstance().log(
                    "Dependency exceptions: " + result.getCollectExceptions(),
                    Project.MSG_DEBUG);
            // PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            // node.accept(nlg);
            // AntClient.getInstance().log(.debug(
            // "All dependencies: "
            // + Arrays.toString(nlg.getDependencies(true).toArray()));
            // AntClient.getInstance().log(.debug(
            // "Direct dependencies: "
            // + String.valueOf(node.getChildren()));
            return result;
        } catch (DependencyResolutionException e) {
            throw new BuildException("Cannot resolve dependency tree for "
                    + node, e);
        }
    }

    /**
     * TODO NXBT-258 (3)
     */
    @Deprecated
    public void resolveDependencyTree(Artifact artifact, ArtifactFilter filter,
            ResolutionListener listener) throws ProjectBuildingException {
        MavenProject mavenProject = mojo.getMavenProjectBuilder().buildFromRepository(
                artifact, mojo.getRemoteArtifactRepositories(),
                mojo.getLocalRepository());
        ArtifactCollector collector = new DefaultArtifactCollector();
        collector.collect(mavenProject.getDependencyArtifacts(),
                mavenProject.getArtifact(),
                mavenProject.getManagedVersionMap(), mojo.getLocalRepository(),
                mavenProject.getRemoteArtifactRepositories(),
                mojo.getMetadataSource(), filter,
                Collections.singletonList(listener));
    }

    @Deprecated
    public void resolve(Artifact artifact,
            List<ArtifactRepository> remoteRepositories)
            throws ArtifactNotFoundException {
        if (artifact.isResolved()) {
            return;
        }
        try {
            mojo.getResolver().resolve(artifact, remoteRepositories,
                    mojo.getLocalRepository());
        } catch (ArtifactResolutionException e) {
            throw new RuntimeException(e);
        } catch (ArtifactNotFoundException e) {
            tryResolutionOnLocalBaseVersion(artifact, e);
        }
    }

    public void resolve(Artifact artifact) throws ArtifactNotFoundException {
        resolve(artifact, mojo.getRemoteArtifactRepositories());
    }

    /**
     * TODO NXBT-258 (1)
     */
    @Deprecated
    public MavenProject load(Artifact artifact) {
        if ("system".equals(artifact.getScope())) {
            return null;
        }
        try {
            resolve(artifact);
            // TODO NXBT-258 use {@link ProjectBuilder} instead
            return mojo.getMavenProjectBuilder().buildFromRepository(artifact,
                    mojo.getRemoteArtifactRepositories(),
                    mojo.getLocalRepository());
        } catch (ProjectBuildingException | ArtifactNotFoundException e) {
            mojo.getLog().error("Error loading POM of " + artifact, e);
            return null;
        }
    }

    /**
     * Try to locally resolve an artifact with its "unique" version.
     *
     * @since 1.11.1
     * @param artifact Artifact to resolve with its unique version
     * @param e ArtifactNotFoundException originally thrown
     * @throws ArtifactNotFoundException If alternate resolution failed.
     * @see Artifact#getBaseVersion()
     */
    protected void tryResolutionOnLocalBaseVersion(Artifact artifact,
            ArtifactNotFoundException e) throws ArtifactNotFoundException {
        String resolvedVersion = artifact.getVersion();
        artifact.updateVersion(artifact.getBaseVersion(),
                mojo.getLocalRepository());
        File localFile = new File(mojo.getLocalRepository().getBasedir(),
                mojo.getLocalRepository().pathOf(artifact));
        if (localFile.exists()) {
            mojo.getLog().warn(
                    String.format(
                            "Couldn't resolve %s, fallback on local install of unique version %s.",
                            resolvedVersion, artifact.getBaseVersion()));
            artifact.setResolved(true);
        } else {
            // No success, set back the previous version and raise an error
            artifact.updateVersion(resolvedVersion, mojo.getLocalRepository());
            mojo.getLog().warn("Cannot resolve " + artifact, e);
            throw e;
        }
    }

}
