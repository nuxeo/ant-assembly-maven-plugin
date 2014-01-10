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
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.SubArtifact;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.filter.Filter;

/**
 * TODO NXBT-258
 */
public class Graph {

    public final TreeMap<String, Node> nodes = new TreeMap<>();

    public final LinkedList<Node> roots = new LinkedList<>();

    private AntBuildMojo mojo = AntBuildMojo.getInstance();

    public List<Node> getRoots() {
        return roots;
    }

    public Collection<Node> getNodes() {
        return nodes.values();
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
        return addRootNode(ad.getDependency());
    }

    /**
     * @since 1.10.2
     */
    public Node addRootNode(MavenProject pom, Artifact artifact) {
        Node node = nodes.get(Node.genNodeId(artifact));
        if (node == null) {
            org.eclipse.aether.artifact.Artifact aetherArtifact = DependencyUtils.mavenToAether(artifact);
            org.eclipse.aether.artifact.Artifact pomArtifact = new SubArtifact(
                    aetherArtifact, null, "pom", pom.getFile());
            Dependency dependency = new Dependency(pomArtifact,
                    artifact.getScope());
            addRootNode(dependency);
        }
        return node;
    }

    /**
     * @since 2.0
     */
    public Node addRootNode(Dependency dependency) {
        Node node = nodes.get(Node.genNodeId(dependency));
        if (node == null) {
            DependencyNode newNode = new DefaultDependencyNode(dependency);
            node = addRootNode(newNode);
        }
        return node;
    }

    /**
     * @since 2.0
     */
    public Node addRootNode(Node node) {
        if (!nodes.containsKey(node.id)) {
            node = addRootNode((DependencyNode) node);
        }
        return nodes.get(node.id);
    }

    private Node addRootNode(DependencyNode dependencyNode) {
        CollectResult collectResult = collectDependencies(dependencyNode);
        Node node = new Node(this, collectResult.getRoot());
        addNode(node);
        roots.add(node);
        AntClient.getInstance().log("Added root node: " + node,
                Project.MSG_DEBUG);
        return node;
    }

    /**
     * @since 2.0
     */
    private void addNode(Node node) {
        nodes.put(node.getId(), node);
        AntClient.getInstance().log("Added node: " + node, Project.MSG_DEBUG);
        for (DependencyNode child : node.getChildren()) {
            Node childNode = new Node(this, child);
            childNode.addParent(node);
            addNode(childNode);
        }
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
            Artifact artifact = node.getMavenArtifact();
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
                                returnNode.getMavenArtifact().getSelectedVersion()) < 0) {
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

    public CollectResult collectDependencies(DependencyNode node) {
        AntClient.getInstance().log(String.format("Collecting " + node),
                Project.MSG_DEBUG);
        CollectRequest collectRequest = new CollectRequest(
                node.getDependency(), mojo.getRemoteRepositories());
        try {
            CollectResult result = mojo.getSystem().collectDependencies(
                    mojo.getSession(), collectRequest);
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
                    mojo.getSession(), dependencyRequest);
            AntClient.getInstance().log("Dependency result: " + result,
                    Project.MSG_DEBUG);
            AntClient.getInstance().log(
                    "Dependency exceptions: " + result.getCollectExceptions(),
                    Project.MSG_DEBUG);
            return result;
        } catch (DependencyResolutionException e) {
            throw new BuildException("Cannot resolve dependency tree for "
                    + node, e);
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

    /**
     * @since 2.0
     */
    public Node getNode(Dependency dependency) {
        return nodes.get(Node.genNodeId(dependency));
    }

}
