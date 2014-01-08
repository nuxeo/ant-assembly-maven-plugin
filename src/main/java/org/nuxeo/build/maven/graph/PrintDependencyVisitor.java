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
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.DependencyVisitor;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;

//import org.eclipse.aether.util.graph.visitor.AbstractDepthFirstNodeListGenerator;

/**
 * Prints the graph while traversing it. Do not write twice a given node.
 * Do not print nodes provided through {@link #setIgnores(List)}.
 *
 * @since 2.0
 */
public class PrintDependencyVisitor implements DependencyVisitor {

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

    /**
     * Tabulation used in {@link #MODE_TREE} mode.
     */
    public static final String TAB_STR = " |-- ";

    /**
     * Default format with GAV: group:artifact:version:type:classifier
     */
    public static final int FORMAT_GAV = 0;

    /**
     * Key-value format: FILENAME=GAV
     */
    public static final int FORMAT_KV_F_GAV = 1;

    public static final String MODE_TREE = "tree";

    /**
     * In flat mode, root nodes are not printed
     */
    public static final String MODE_FLAT = "flat";

    public static final String MODE_SDK = "sdk";

    protected OutputStream output;

    protected boolean modeTree = true;

    protected int format;

    protected List<Node> ignores;

    protected String tabs = "";

    protected List<String> scopes = null;

    /**
     * @param nodes Nodes to ignore during the visit
     */
    public void setIgnores(List<Node> nodes) {
        this.ignores = nodes;
    }

    /**
     * @param output
     * @param mode
     * @param format
     * @param scopes Printed scopes (if null, all scopes are printed).
     */
    public PrintDependencyVisitor(OutputStream output, String mode, int format,
            List<String> scopes) {
        nodes = new ArrayList<>(128);
        visitedNodes = new IdentityHashMap<>(512);
        this.output = output;
        if (MODE_TREE.equalsIgnoreCase(mode)) {
            modeTree = true;
        } else if (MODE_FLAT.equalsIgnoreCase(mode)) {
            modeTree = false;
        } else {
            throw new BuildException("Unknown mode: " + mode);
        }
        this.format = format;
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
        boolean visit = doVisit(node);
        if (modeTree) {
            tabs += TAB_STR;
        }
        return visit;
    }

    protected boolean doVisit(DependencyNode node) {
        if (!setVisited(node)) {
            return false;
        }
        Dependency dependency = node.getDependency();
        if (dependency == null) {
            AntClient.getInstance().log(
                    "Ignored node with null dependency: " + node);
            return false;
        }
        if (dependency.getScope() == null || "".equals(dependency.getScope())) {
            AntClient.getInstance().log(
                    String.format("Found node %s with null scope!", node));
        } else if (scopes != null && !scopes.contains(dependency.getScope())) {
            AntClient.getInstance().log(
                    String.format("Ignored node %s which scope is %s", node,
                            dependency.getScope()));
            return false;
        }
        nodes.add(node);
        try {
            if (ignores == null || !ignores.contains(node)) {
                print(node);
            } else {
                AntClient.getInstance().log("Unprinted node: " + node);
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        AntClient.getInstance().log("leave: " + node, Project.MSG_DEBUG);
        if (modeTree) {
            tabs = tabs.substring(0, tabs.length() - TAB_STR.length());
        }
        return true;
    }

    protected void print(DependencyNode node) throws IOException {
        if (!modeTree && "pom".equals(node.getArtifact().getExtension())) {
            return;
        }
        String toString = tabs + toString(node)
                + System.getProperty("line.separator");
        output.write(toString.getBytes(AntBuildMojo.getInstance().getEncoding()));
    }

    /**
     * @return String representation depending on {@link #format}
     * @see #FORMAT_GAV
     * @see #FORMAT_KV_F_GAV
     */
    public String toString(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        StringBuilder sb = new StringBuilder();
        switch (format) {
        case PrintDependencyVisitor.FORMAT_KV_F_GAV:
            sb.append(artifact.getFile().getName());
            sb.append('=');
            // fall through
        case PrintDependencyVisitor.FORMAT_GAV:
            sb.append(artifact.getGroupId());
            sb.append(':').append(artifact.getArtifactId());
            sb.append(':').append(artifact.getVersion());
            sb.append(':').append(artifact.getExtension());
            sb.append(':').append(artifact.getClassifier());
            sb.append(':').append(node.getDependency().getScope());
            break;
        default:
            return "Unknown format: " + format + "!";
        }
        if (modeTree) {
            if (node.getDependency().isOptional()) {
                sb.append(" [optional]");
            }
            String premanaged = DependencyManagerUtils.getPremanagedVersion(node);
            if (premanaged != null
                    && !premanaged.equals(node.getArtifact().getBaseVersion())) {
                sb.append(" (version managed from ").append(premanaged).append(
                        ")");
            }
            premanaged = DependencyManagerUtils.getPremanagedScope(node);
            if (premanaged != null
                    && !premanaged.equals(node.getDependency().getScope())) {
                sb.append(" (scope managed from ").append(premanaged).append(
                        ")");
            }
            DependencyNode winner = (DependencyNode) node.getData().get(
                    ConflictResolver.NODE_DATA_WINNER);
            if (winner != null
                    && !ArtifactIdUtils.equalsId(node.getArtifact(),
                            winner.getArtifact())) {
                Artifact w = winner.getArtifact();
                sb.append(" (conflicts with ");
                if (ArtifactIdUtils.toVersionlessId(node.getArtifact()).equals(
                        ArtifactIdUtils.toVersionlessId(w))) {
                    sb.append(w.getVersion());
                } else {
                    sb.append(w);
                }
                sb.append(")");
            }
        }
        return sb.toString();
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
