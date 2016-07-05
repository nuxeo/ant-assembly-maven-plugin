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
 *     bstefanescu, jcarsique, slacoin
 */
package org.nuxeo.build.maven.graph;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Exclusion;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.JavaScopes;

import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.filter.Filter;

/**
 * TODO NXBT-258
 */
public class Graph {

    public final TreeMap<String, Node> nodes = new TreeMap<>();

    public final List<Node> roots = new LinkedList<>();

    private AntBuildMojo mojo = AntBuildMojo.getInstance();

    public List<Node> getRoots() {
        return roots;
    }

    public Collection<Node> getNodes() {
        return nodes.values();
    }

    /**
     * That methods looks for the pattern and returns the first matching node. It is now deprecated since there are no
     * use case for it. Use instead {@link #findFirst(String, boolean)} which will fail if two artifacts match the
     * pattern.
     *
     * @deprecated Since 2.0.4. This method is not very interesting.
     */
    @Deprecated
    public Node findFirst(String pattern) {
        return findFirst(pattern, false);
    }

    public Node findFirst(String pattern, boolean stopIfNotUnique) {
        if (pattern.contains("::")) {
            // Pattern requiring completion: #findNode(ArtifactDescriptor) must be used instead
            return null;
        }
        SortedMap<String, Node> map = nodes.subMap(pattern + ':', pattern + ((char) (':' + 1)));
        int size = map.size();
        if (size == 0) {
            return null;
        }
        if (stopIfNotUnique && size > 1) {
            AntClient.getInstance().log(
                    String.format("Pattern '%s' cannot be resolved to a unique node. Matching nodes are: %s", pattern,
                            map.values()), Project.MSG_DEBUG);
            return null;
        }
        return map.get(map.firstKey());
    }

    public Collection<Node> find(String pattern) {
        SortedMap<String, Node> map = nodes.subMap(pattern + ':', pattern + ((char) (':' + 1)));
        return map.values();
    }

    /**
     * Add a root node given a Maven Project POM. This can be used to initialize the graph.
     */
    public Node addRootNode(MavenProject pom) {
        Node node = nodes.get(Node.genNodeId(pom.getArtifact()));
        if (node == null) {
            node = collectRootNode(pom);
        }
        return addRootNode(node);
    }

    public Node addRootNode(String key) {
        ArtifactDescriptor ad = new ArtifactDescriptor(key);
        return addRootNode(ad.getDependency());
    }

    /**
     * @since 2.0
     */
    public Node addRootNode(Dependency dependency) {
        Node node = nodes.get(Node.genNodeId(dependency));
        if (node == null) {
            node = collectRootNode(dependency);
        } else {
            roots.add(node);
            AntClient.getInstance().log("Added root node: " + node, Project.MSG_DEBUG);
        }
        return node;
    }

    /**
     * @since 2.0
     */
    public Node addRootNode(Node node) {
        if (!nodes.containsKey(node.id)) {
            node = collectRootNode(node.getDependency());
        } else if (!roots.contains(node)) {
            roots.add(node);
            AntClient.getInstance().log("Added root node: " + node, Project.MSG_DEBUG);
        }
        return nodes.get(node.id);
    }

    public Node collectRootNode(Dependency dependency) {
        DependencyNode root = collectDependencies(dependency);
        return addRootNode(root);
    }

    /**
     * @since 2.0.4
     */
    public Node addRootNode(DependencyNode root) {
        Node node = new Node(this, root);
        roots.add(node);
        AntClient.getInstance().log("Added root node: " + node, Project.MSG_DEBUG);
        addNode(node);
        return node;
    }

    /**
     * @since 2.0.4
     */
    public Node collectRootNode(MavenProject pom) {
        DependencyNode root = collectDependencies(pom);
        return addRootNode(root);
    }

    private Node resolveRootNode(Node root, Filter filter, int depth) {
        if (!filter.accept(root, null)) {
            return null;
        }
        DependencyResult result = DependencyUtils.resolveDependencies(root, filter, depth);
        Node node = new Node(this, result.getRoot());
        roots.add(node);
        AntClient.getInstance().log("Added resolved root node: " + node, Project.MSG_DEBUG);
        addNode(node);
        return node;
    }

    /**
     * @since 2.0
     */
    public void addNode(Node node) {
        if (nodes.containsKey(node.getId())) {
            return;
        }
        nodes.put(node.getId(), node);
        AntClient.getInstance().log("Added node: " + node, Project.MSG_DEBUG);
        // Add children
        for (DependencyNode child : node.getChildren()) {
            Node childNode = nodes.get(Node.genNodeId(child));
            if (childNode == null) {
                childNode = new Node(this, child);
                addNode(childNode);
            }
            childNode.addParent(node);
        }
    }

    public Node findNode(ArtifactDescriptor ad) {
        if (ad.isEmpty()) {
            return null;
        }
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
            if (ad.getArtifactId() != null && !ad.getArtifactId().equals(artifact.getArtifactId())) {
                continue;
            }
            if (ad.getGroupId() != null && !ad.getGroupId().equals(artifact.getGroupId())) {
                continue;
            }
            if (ad.getVersion() != null && !ad.getVersion().equals(artifact.getVersion())) {
                continue;
            }
            if (ad.getType() != null && !ad.getType().equals(artifact.getType())) {
                continue;
            }
            if (ad.getClassifier() != null && !ad.getClassifier().equals(artifact.getClassifier())
                    || ad.getClassifier() == null && artifact.hasClassifier()) {
                continue;
            }
            try {
                if (returnNode != null
                        && artifact.getSelectedVersion().compareTo(returnNode.getMavenArtifact().getSelectedVersion()) < 0) {
                    continue;
                }
            } catch (OverConstrainedVersionException e) {
                mojo.getLog().error("Versions comparison failed on " + artifact, e);
            }
            returnNode = node;
        }
        return returnNode;
    }

    public DependencyNode collectDependencies(Dependency dependency) {
        AntClient.getInstance().log(String.format("Collecting " + dependency), Project.MSG_DEBUG);
        CollectRequest collectRequest = new CollectRequest(dependency, mojo.getRemoteRepositories());
        collectRequest.setRequestContext("AAMP graph");
        return collect(collectRequest);
    }

    protected DependencyNode collect(CollectRequest collectRequest) {
        try {
            CollectResult result = mojo.getSystem().collectDependencies(mojo.getSession(), collectRequest);
            DependencyNode node = result.getRoot();
            AntClient.getInstance().log("Collect result: " + result, Project.MSG_DEBUG);
            AntClient.getInstance().log("Collect exceptions: " + result.getExceptions(), Project.MSG_DEBUG);
            AntClient.getInstance()
                     .log("Direct dependencies: " + String.valueOf(node.getChildren()), Project.MSG_DEBUG);
            return node;
        } catch (DependencyCollectionException e) {
            throw new BuildException("Cannot collect dependency tree for " + collectRequest, e);
        }
    }

    /**
     * @since 2.0.4
     */
    @SuppressWarnings("deprecation")
    public DependencyNode collectDependencies(MavenProject project) {
        AntClient.getInstance().log(String.format("Collecting " + project), Project.MSG_DEBUG);
        CollectRequest collectRequest = new CollectRequest();
        Artifact rootArtifact = project.getArtifact();
        rootArtifact.setFile(project.getFile());
        collectRequest.setRootArtifact(RepositoryUtils.toArtifact(rootArtifact));
        collectRequest.setRoot(RepositoryUtils.toDependency(rootArtifact, null));
        collectRequest.setRequestContext("AAMP graph");
        collectRequest.setRepositories(project.getRemoteProjectRepositories());

        ArtifactTypeRegistry stereotypes = mojo.getSession().getArtifactTypeRegistry();
        // BEGIN Code copy from
        // org.apache.maven.project.DefaultProjectDependenciesResolver.resolve(DependencyResolutionRequest)
        if (project.getDependencyArtifacts() == null) {
            for (org.apache.maven.model.Dependency dependency : project.getDependencies()) {
                if (StringUtils.isEmpty(dependency.getGroupId()) || StringUtils.isEmpty(dependency.getArtifactId())
                        || StringUtils.isEmpty(dependency.getVersion())) {
                    // guard against case where best-effort resolution for invalid models is requested
                    continue;
                }
                collectRequest.addDependency(RepositoryUtils.toDependency(dependency, stereotypes));
            }
        } else {
            Map<String, org.apache.maven.model.Dependency> dependencies = new HashMap<>();
            for (org.apache.maven.model.Dependency dependency : project.getDependencies()) {
                String classifier = dependency.getClassifier();
                if (classifier == null) {
                    ArtifactType type = stereotypes.get(dependency.getType());
                    if (type != null) {
                        classifier = type.getClassifier();
                    }
                }
                String key = ArtifactIdUtils.toVersionlessId(dependency.getGroupId(), dependency.getArtifactId(),
                        dependency.getType(), classifier);
                dependencies.put(key, dependency);
            }
            for (Artifact artifact : project.getDependencyArtifacts()) {
                String key = artifact.getDependencyConflictId();
                org.apache.maven.model.Dependency dependency = dependencies.get(key);
                Collection<Exclusion> exclusions = dependency != null ? dependency.getExclusions() : null;
                org.eclipse.aether.graph.Dependency dep = RepositoryUtils.toDependency(artifact, exclusions);
                if (!JavaScopes.SYSTEM.equals(dep.getScope()) && dep.getArtifact().getFile() != null) {
                    // enable re-resolution
                    org.eclipse.aether.artifact.Artifact art = dep.getArtifact();
                    art = art.setFile(null).setVersion(art.getBaseVersion());
                    dep = dep.setArtifact(art);
                }
                collectRequest.addDependency(dep);
            }
        }

        DependencyManagement depMngt = project.getDependencyManagement();
        if (depMngt != null) {
            for (org.apache.maven.model.Dependency dependency : depMngt.getDependencies()) {
                collectRequest.addManagedDependency(RepositoryUtils.toDependency(dependency, stereotypes));
            }
        }
        // END
        return collect(collectRequest);
    }

    /**
     * Resolve graph starting from its root nodes.
     *
     * @since 2.0
     */
    public void resolveDependencies(Filter filter, int depth) {
        List<Node> oldRoots = new LinkedList<>(roots);
        roots.clear();
        nodes.clear();
        for (Node root : oldRoots) {
            resolveRootNode(root, filter, depth);
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
    protected void tryResolutionOnLocalBaseVersion(Artifact artifact, ArtifactNotFoundException e)
            throws ArtifactNotFoundException {
        String resolvedVersion = artifact.getVersion();
        artifact.updateVersion(artifact.getBaseVersion(), mojo.getLocalRepository());
        File localFile = new File(mojo.getLocalRepository().getBasedir(), mojo.getLocalRepository().pathOf(artifact));
        if (localFile.exists()) {
            mojo.getLog().warn(
                    String.format("Couldn't resolve %s, fallback on local install of unique version %s.",
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

    /**
     * @param key
     * @throws BuildException
     * @since 2.0.4
     */
    public Node findNode(String key) throws BuildException {
        return findNode(key, ArtifactDescriptor.parseQuietly(key));
    }

    /**
     * @param key
     * @param ad
     * @throws BuildException
     * @since 2.0.4
     */
    public Node findNode(String key, ArtifactDescriptor ad) throws BuildException {
        Node node = null;
        if (key != null) {
            node = findFirst(key, true);
        }
        if (node == null && ad != null) {
            node = findNode(ad);
        }
        if (node == null) {
            throw new BuildException("Artifact with pattern " + (key != null ? key : ad) + " was not found in graph");
        }
        return node;
    }

}
