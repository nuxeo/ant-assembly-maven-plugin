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
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.Project;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.ant.artifact.PrintGraphTask;
import org.nuxeo.build.maven.AntBuildMojo;

/**
 * Prints all dependencies in a flat sorted output. Duplicates and conflicting
 * versions are removed.
 *
 * @since 2.0
 */
public class FlatPrinterDependencyVisitor extends AbstractDependencyVisitor {

    protected int format;

    protected OutputStream output;

    protected Set<String> lines = new TreeSet<>();

    /**
     * @param format 0 = standard GAV ; 1 = File + GAV
     */
    public FlatPrinterDependencyVisitor(OutputStream output, int format,
            List<String> scopes) {
        super(scopes);
        this.output = output;
        this.format = format;
    }

    @Override
    protected void doVisit(DependencyNode node, boolean newNode) {
        if (newNode) {
            print(node);
        }
    }

    /**
     * output must be sorted before write; see {@link #print()}
     */
    protected void print(DependencyNode node) {
        if ("pom".equals(node.getArtifact().getExtension())) {
            return;
        }
        Artifact artifact = node.getArtifact();
        DependencyNode winner = (DependencyNode) node.getData().get(
                ConflictResolver.NODE_DATA_WINNER);
        if (winner != null
                && !ArtifactIdUtils.equalsId(artifact, winner.getArtifact())) {
            Artifact w = winner.getArtifact();
            if (ArtifactIdUtils.toVersionlessId(artifact).equals(
                    ArtifactIdUtils.toVersionlessId(w))) {
                AntClient.getInstance().log(
                        String.format("Ignored conflicting node %s with %s",
                                node, w.getVersion()), Project.MSG_DEBUG);
            } else {
                AntClient.getInstance().log(
                        String.format("Ignored conflicting node %s with %s",
                                node, w), Project.MSG_DEBUG);
            }
        } else {
            String toString = toString(node)
                    + System.getProperty("line.separator");
            lines.add(toString);
        }
    }

    public void print() throws UnsupportedEncodingException, IOException {
        for (String line : lines) {
            output.write(line.getBytes(AntBuildMojo.getInstance().getEncoding()));
        }
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
        return sb.toString();
    }

}
