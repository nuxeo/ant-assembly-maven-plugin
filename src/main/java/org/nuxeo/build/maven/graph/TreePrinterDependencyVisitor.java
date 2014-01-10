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
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
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

    /**
     * @param output
     * @param format 0 = standard GAV ; 1 = File + GAV
     * @param scopes
     */
    public TreePrinterDependencyVisitor(OutputStream output, int format,
            List<String> scopes) {
        super(scopes);
        this.output = output;
        this.format = format;
    }

    @Override
    protected void doVisit(DependencyNode node, boolean newNode) {
        try {
            print(node);
            tabs += TAB_STR;
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    @Override
    public boolean visitLeave(DependencyNode node) {
        boolean visit = super.visitLeave(node);
        tabs = tabs.substring(0, tabs.length() - TAB_STR.length());
        return visit;
    }

    protected void print(DependencyNode node)
            throws UnsupportedEncodingException, IOException {
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
            sb.append(" (conflicts with ");
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
