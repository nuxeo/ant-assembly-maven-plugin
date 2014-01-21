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

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.eclipse.aether.util.graph.visitor.PreorderNodeListGenerator;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.ant.artifact.PrintGraphTask;
import org.nuxeo.build.maven.AntBuildMojo;

/**
 * Prints all dependencies as a tree. Duplicates and conflicting
 * versions mentioned but not removed. However, nodes are only expanded once.
 *
 * @since 2.0
 */
public class TreePrinterDependencyVisitor extends AbstractDependencyVisitor {

    /**
     * Tabulation used in {@link PrintGraphTask#MODE_TREE} mode.
     */
    public static final String TAB_STR = " |-- ";

    protected String tabs = "";

    protected int format;

    protected OutputStream output;

    protected Map<String, DependencyNode> dependencyMap = new HashMap<>();

    protected Map<String, List<Dependency>> directDepsByArtifact = new HashMap<>();

    /**
     * @param output
     * @param format 0 = standard GAV ; 1 = File + GAV
     * @param scopes
     * @param list
     */
    public TreePrinterDependencyVisitor(OutputStream output, int format,
            List<String> scopes, List<Node> roots) {
        super(scopes);
        this.output = output;
        this.format = format;
        PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
        for (Node node : roots) {
            node.accept(nlg);
        }
        for (DependencyNode node : nlg.getNodes()) {
            dependencyMap.put(node.getArtifact().toString(), node);
        }
    }

    @Override
    protected void doVisit(DependencyNode node, boolean newNode) {
        try {
            print(node);
            if (newNode) {
                addMissingChildren(node);
            }
            incTabs();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    /**
     * Add child nodes which were removed by the ConflictResolver (it lets no
     * duplicate)
     */
    protected void addMissingChildren(DependencyNode node) {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        incTabs();
        try {
            List<Dependency> directDeps = directDepsByArtifact.get(node.getArtifact().toString());
            if (directDeps == null) {
                ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest(
                        node.getArtifact(), mojo.getRemoteRepositories(), null);
                ArtifactDescriptorResult descriptorResult = mojo.getSystem().readArtifactDescriptor(
                        mojo.getSession(), descriptorRequest);
                directDeps = descriptorResult.getDependencies();
            }
            for (Dependency dependency : directDeps) {
                boolean removed = true;
                if (JavaScopes.TEST.equals(dependency.getScope())
                        && !JavaScopes.TEST.equals(node.getDependency().getScope())) {
                    continue;
                }
                for (DependencyNode childNode : node.getChildren()) {
                    if (childNode.getArtifact().toString().equals(
                            dependency.getArtifact().toString())) {
                        removed = false;
                    }
                }
                if (removed) {
                    DependencyNode childNode = dependencyMap.get(dependency.getArtifact().toString());
                    if (childNode == null) {
                        AntClient.getInstance().log(
                                "Ignored dependency " + dependency
                                        + " not in graph", Project.MSG_DEBUG);
                        // childNode = new DefaultDependencyNode(dependency);
                        continue;
                    } else {
                        int managedBits = 0;
                        childNode = new DefaultDependencyNode(childNode);
                        childNode.setScope(dependency.getScope());
                        // String managedScope = dependency.getScope();
                        // String scope = childNode.getDependency().getScope();
                        // if (!managedScope.equals(scope)) {
                        // managedBits |= DependencyNode.MANAGED_SCOPE;
                        // childNode.setData(
                        // DependencyManagerUtils.NODE_DATA_PREMANAGED_SCOPE,
                        // managedScope);
                        // }
                        String managedVersion = dependency.getArtifact().getBaseVersion();
                        String version = childNode.getArtifact().getBaseVersion();
                        if (!managedVersion.equals(version)) {
                            managedBits |= DependencyNode.MANAGED_VERSION;
                            childNode.setData(
                                    DependencyManagerUtils.NODE_DATA_PREMANAGED_VERSION,
                                    managedVersion);
                        }
                        ((DefaultDependencyNode) childNode).setManagedBits(managedBits);
                    }
                    childNode.setChildren(Collections.<DependencyNode> emptyList());
                    if (node.getChildren().isEmpty()) {
                        node.setChildren(new ArrayList<DependencyNode>());
                    }
                    node.getChildren().add(childNode);
                    // Mark added child as already visited to avoid expanding it
                    // again
                    setVisited(childNode);
                }
            }
        } catch (ArtifactDescriptorException e) {
            AntClient.getInstance().log(e.getMessage(), e, Project.MSG_ERR);
        } finally {
            decTabs();
        }
    }

    protected void incTabs() {
        tabs += TAB_STR;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        boolean visit = super.visitLeave(node);
        if (!ignores.contains(node)) {
            decTabs();
        }
        return visit;
    }

    protected void decTabs() {
        tabs = tabs.substring(0, tabs.length() - TAB_STR.length());
    }

    protected void print(DependencyNode node)
            throws UnsupportedEncodingException, IOException {
        String toString = tabs + toString(node)
                + System.getProperty("line.separator");
        output.write(toString.getBytes(AntBuildMojo.getInstance().getEncoding()));
    }

    /**
     * @return String representation depending on {@link #format}
     * @see PrintGraphTask#FORMAT_GAV
     * @see PrintGraphTask#FORMAT_KV_F_GAV
     */
    public String toString(DependencyNode node) {
        Artifact artifact = node.getArtifact();
        Dependency dependency = node.getDependency();
        StringBuilder sb = new StringBuilder();
        switch (format) {
        case 1:
            sb.append(artifact.getFile().getName());
            sb.append('=');
            // fall through
        case 0:
            sb.append(artifact.getGroupId());
            sb.append(':').append(artifact.getArtifactId());
            sb.append(':').append(artifact.getVersion());
            sb.append(':').append(artifact.getExtension());
            sb.append(':').append(artifact.getClassifier());
            sb.append(':').append(dependency.getScope());
            break;
        default:
            return "Unknown format: " + format + "!";
        }
        if (node.getDependency().isOptional()) {
            sb.append(" [optional]");
        }
        String premanaged = DependencyManagerUtils.getPremanagedVersion(node);
        if (premanaged != null && !premanaged.equals(artifact.getBaseVersion())) {
            sb.append(" (version managed from ").append(premanaged).append(")");
        }
        premanaged = DependencyManagerUtils.getPremanagedScope(node);
        if (premanaged != null && !premanaged.equals(dependency.getScope())) {
            sb.append(" (scope managed from ").append(premanaged).append(")");
        }
        DependencyNode winner = (DependencyNode) node.getData().get(
                ConflictResolver.NODE_DATA_WINNER);
        if (winner != null
                && !ArtifactIdUtils.equalsId(artifact, winner.getArtifact())) {
            Artifact w = winner.getArtifact();
            sb.append(" (superseded by ");
            if (ArtifactIdUtils.toVersionlessId(artifact).equals(
                    ArtifactIdUtils.toVersionlessId(w))) {
                sb.append(w.getVersion());
            } else {
                sb.append(w);
            }
            sb.append(")");
        }
        return sb.toString();
    }

}
