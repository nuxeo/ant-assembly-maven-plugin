/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.build.maven.graph;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.Project;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.nuxeo.build.ant.AntClient;

/**
 * Prints the graph while traversing it.
 * Do not print nodes provided through {@link #addIgnores(Collection)}.
 * Inspired from
 * {@link org.eclipse.aether.util.graph.visitor.AbstractDepthFirstNodeListGenerator}
 *
 * @since 2.0
 */
public abstract class AbstractDependencyVisitor implements DependencyVisitor {

    private final Map<DependencyNode, Object> visitedNodes;

    protected final List<DependencyNode> nodes;

    /**
     * Gets the list of dependency nodes that was generated during the graph
     * traversal.
     *
     * @return The list of dependency nodes, never {@code null}.
     */
    public List<DependencyNode> getNodes() {
        return nodes;
    }

    protected final List<DependencyNode> ignores;

    protected List<String> scopes = null;

    /**
     * @param nodesToIgnore Nodes to ignore during the visit
     */
    public void addIgnores(Collection<? extends DependencyNode> nodesToIgnore) {
        this.ignores.addAll(nodesToIgnore);
    }

    /**
     * @param scopes Included scopes (if null, all scopes are included).
     */
    public AbstractDependencyVisitor(List<String> scopes) {
        nodes = new ArrayList<>(128);
        visitedNodes = new IdentityHashMap<>(512);
        ignores = new ArrayList<>();
        this.scopes = scopes;
    }

    /**
     * Marks the specified node as being visited and determines whether the node
     * has been visited before.
     *
     * @param node The node being visited, must not be {@code null}.
     * @return {@code true} if the node has not been visited before,
     *         {@code false} if the node was already visited.
     */
    protected boolean setVisited(DependencyNode node) {
        return visitedNodes.put(node, Boolean.TRUE) == null;
    }

    @Override
    public boolean visitEnter(DependencyNode node) {
        AntClient.getInstance().log("enter: " + node, Project.MSG_DEBUG);
        boolean newNode = setVisited(node);
        boolean visitChildren = newNode;
        boolean ignoreNode = false;
        Dependency dependency = node.getDependency();
        if (dependency == null) {
            AntClient.getInstance().log(
                    "Ignored node with null dependency: " + node);
            ignoreNode = true;
        } else if (dependency.getScope() == null
                || "".equals(dependency.getScope())) {
            AntClient.getInstance().log(
                    String.format("Found node %s with null scope!", node));
        } else if (scopes != null && !scopes.contains(dependency.getScope())) {
            AntClient.getInstance().log(
                    String.format("Ignored node %s which scope is %s", node,
                            dependency.getScope()));
            ignoreNode = true;
        }
        if (ignores != null && ignores.contains(node)) {
            AntClient.getInstance().log("Ignored node as requested: " + node);
            ignoreNode = true;
        }
        if (ignoreNode) {
            visitChildren = false;
            ignores.add(node);
        } else {
            DependencyNode winner = (DependencyNode) node.getData().get(
                    ConflictResolver.NODE_DATA_WINNER);
            if (winner != null
                    && !ArtifactIdUtils.equalsId(node.getArtifact(),
                            winner.getArtifact())) {
                // This is a conflicting node, not really a new one
                newNode = false;
            }
            if (newNode) {
                nodes.add(node);
            }
            doVisit(node, newNode);
        }
        return visitChildren;
    }

    /**
     * Actions to perform when visiting a node. The method is not called if the
     * node was "ignored" (ie included in {@link #ignores}.
     *
     * @param newNode True if visiting the node for the first time
     */
    protected abstract void doVisit(DependencyNode node, boolean newNode);

    /**
     * Note that method is always called, even if
     * {@link #doVisit(DependencyNode, boolean)} was not. Check {@link #ignores}
     * if needed.
     */
    @Override
    public boolean visitLeave(DependencyNode node) {
        AntClient.getInstance().log("leave: " + node, Project.MSG_DEBUG);
        return true;
    }

    /**
     * Gets the dependencies seen during the graph traversal.
     *
     * @param includeUnresolved Whether unresolved dependencies shall be
     *            included in the result or not.
     * @return The list of dependencies, never {@code null}.
     */
    public List<Dependency> getDependencies(boolean includeUnresolved) {
        List<Dependency> dependencies = new ArrayList<>(getNodes().size());
        for (DependencyNode node : getNodes()) {
            Dependency dependency = node.getDependency();
            if (dependency != null
                    && (includeUnresolved || dependency.getArtifact().getFile() != null)) {
                dependencies.add(dependency);
            }
        }
        return dependencies;
    }

    /**
     * Gets the artifacts associated with the list of dependency nodes generated
     * during the graph traversal.
     *
     * @param includeUnresolved Whether unresolved artifacts shall be included
     *            in the result or not.
     * @return The list of artifacts, never {@code null}.
     */
    public List<Artifact> getArtifacts(boolean includeUnresolved) {
        List<Artifact> artifacts = new ArrayList<>(getNodes().size());
        for (DependencyNode node : getNodes()) {
            Artifact artifact = node.getArtifact();
            if (artifact != null
                    && (includeUnresolved || artifact.getFile() != null)) {
                artifacts.add(artifact);
            }
        }
        return artifacts;
    }

    /**
     * Gets the files of resolved artifacts seen during the graph traversal.
     *
     * @return The list of artifact files, never {@code null}.
     */
    public List<File> getFiles() {
        List<File> files = new ArrayList<>(getNodes().size());
        for (DependencyNode node : getNodes()) {
            if (node.getDependency() != null) {
                File file = node.getDependency().getArtifact().getFile();
                if (file != null) {
                    files.add(file);
                }
            }
        }
        return files;
    }

    /**
     * Gets a class path by concatenating the artifact files of the visited
     * dependency nodes. Nodes with unresolved
     * artifacts are automatically skipped.
     *
     * @return The class path, using the platform-specific path separator, never
     *         {@code null}.
     */
    public String getClassPath() {
        StringBuilder buffer = new StringBuilder(1024);
        for (Iterator<DependencyNode> it = getNodes().iterator(); it.hasNext();) {
            DependencyNode node = it.next();
            Artifact artifact = node.getArtifact();
            if (artifact != null && artifact.getFile() != null) {
                buffer.append(artifact.getFile().getAbsolutePath());
                if (it.hasNext()) {
                    buffer.append(File.pathSeparatorChar);
                }
            }
        }
        return buffer.toString();
    }
}
