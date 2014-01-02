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
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.nuxeo.build.maven.AntBuildMojo;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.graph.AbstractDepthFirstNodeListGenerator;

/**
 * Prints the graph while traversing it. Do not write twice a given node.
 * Do not print nodes provided through {@link #setIgnores(List)}.
 *
 * @since 2.0
 */
public class PrintDependencyVisitor extends AbstractDepthFirstNodeListGenerator {

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

    @Override
    public boolean visitEnter(DependencyNode node) {
        AntBuildMojo.getInstance().getLog().debug("enter: " + node);
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
            AntBuildMojo.getInstance().getLog().info(
                    "Ignored node with null dependency: " + node);
            return false;
        }
        if (scopes != null && !scopes.contains(dependency.getScope())) {
            AntBuildMojo.getInstance().getLog().info(
                    String.format("Ignored node %s which scope is %s", node,
                            dependency.getScope()));
            return false;
        }
        nodes.add(node);
        try {
            if (ignores == null || !ignores.contains(node)) {
                print(dependency);
            } else {
                AntBuildMojo.getInstance().getLog().info(
                        "Unprinted node: " + node);
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
        return true;
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        AntBuildMojo.getInstance().getLog().debug("leave: " + node);
        if (modeTree) {
            tabs = tabs.substring(0, tabs.length() - TAB_STR.length());
        }
        return true;
    }

    protected void print(Dependency dependency) throws IOException {
        if (!modeTree && "pom".equals(dependency.getArtifact().getExtension())) {
            return;
        }
        String toString = tabs + toString(dependency)
                + System.getProperty("line.separator");
        output.write(toString.getBytes(AntBuildMojo.getInstance().getEncoding()));
    }

    /**
     * @return String representation depending on {@link #format}
     * @see #FORMAT_GAV
     * @see #FORMAT_KV_F_GAV
     */
    public String toString(Dependency dependency) {
        Artifact artifact = dependency.getArtifact();
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
            sb.append(':');
            if (artifact.getClassifier() != null) {
                sb.append(artifact.getClassifier());
            }
            sb.append(':').append(dependency.getScope());
            return sb.toString();
        default:
            return "Unknown format: " + format + "!";
        }
    }

}
