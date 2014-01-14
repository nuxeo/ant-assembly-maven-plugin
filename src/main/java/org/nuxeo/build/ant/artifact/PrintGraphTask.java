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
package org.nuxeo.build.ant.artifact;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.graph.AbstractDependencyVisitor;
import org.nuxeo.build.maven.graph.FlatPrinterDependencyVisitor;
import org.nuxeo.build.maven.graph.Node;
import org.nuxeo.build.maven.graph.TreePrinterDependencyVisitor;

/**
 * TODO NXBT-258
 */
public class PrintGraphTask extends Task {

    private String output;

    private String mode = PrintGraphTask.MODE_TREE;

    /**
     * Default format with GAV: group:artifact:version:type:classifier
     */
    public static final int FORMAT_GAV = 0;

    /**
     * Key-value format: FILENAME=GAV
     */
    public static final int FORMAT_KV_F_GAV = 1;

    private int format = FORMAT_GAV;

    private boolean append = false;

    private String source;

    private List<String> scopes = Arrays.asList(new String[] {
            JavaScopes.COMPILE, JavaScopes.RUNTIME, JavaScopes.SYSTEM });

    /**
     * In sdk mode, root nodes are not printed
     *
     * @since 1.10.2
     */
    public static final String MODE_SDK = "sdk";

    /**
     * In flat mode, root nodes are not printed
     *
     * @since 1.10.2
     */
    public static final String MODE_FLAT = "flat";

    /**
     * @since 1.10.2
     */
    public static final String MODE_TREE = "tree";

    @Override
    public void execute() throws BuildException {
        List<Node> roots;
        if (source != null) {
            // TODO NXBT-258
            // ExpandTask expandTask = new NuxeoExpandTask();
            // ExpandTask expandTask = new ExpandTask();
            // expandTask.setDepth("all");
            // expandTask.execute(AntBuildMojo.getInstance().newGraph(source));
            roots = new ArrayList<>();
            roots.add(AntBuildMojo.getInstance().getGraph().addRootNode(source));
        } else {
            roots = AntBuildMojo.getInstance().getGraph().getRoots();
        }
        OutputStream out = System.out;
        try {
            if (output != null) {
                out = new FileOutputStream(output, append);
            }
            // TODO NXBT-258 see DependencyFilterUtils.classpathFilter(
            // JavaScopes.COMPILE );
            // system.resolveDependencies( session, dependencyRequest
            // ).getArtifactResults();
            AbstractDependencyVisitor pdv;
            if (PrintGraphTask.MODE_TREE.equals(mode)) {
                pdv = new TreePrinterDependencyVisitor(out, format, scopes);
            } else {
                pdv = new FlatPrinterDependencyVisitor(out, format, scopes);
            }
            // Ignore roots in flat mode
            if (pdv instanceof FlatPrinterDependencyVisitor) {
                pdv.addIgnores(roots);
            }
            for (Node node : roots) {
                log("Visiting " + node, Project.MSG_DEBUG);
                node.accept(pdv);
            }
            if (pdv instanceof FlatPrinterDependencyVisitor) {
                ((FlatPrinterDependencyVisitor) pdv).print();
            }
            log("All dependencies: "
                    + String.valueOf(pdv.getDependencies(true)),
                    Project.MSG_DEBUG);
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * @since 1.10.2
     * @see PrintDependencyVisitor
     */
    public void setMode(String mode) {
        if (PrintGraphTask.MODE_SDK.equals(mode)) {
            this.format = FORMAT_KV_F_GAV;
        }
        this.mode = mode;
    }

    /**
     * Defines output format
     *
     * @param format
     * @see PrintDependencyVisitor
     * @since 1.10.2
     */
    public void setFormat(int format) {
        this.format = format;
    }

    /**
     * Output append mode
     *
     * @param append
     * @since 1.10.2
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * If set, print another graph than the current one
     *
     * @param source GAV key of root node to expand as a graph
     * @since 1.10.2
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @since 1.10.2
     * @param scopes Comma separated list of scopes to include. Defaults to
     *            "compile,runtime,system".
     */
    public void setScopes(String scopes) {
        StringTokenizer st = new StringTokenizer(scopes, ",");
        this.scopes = new ArrayList<>();
        while (st.hasMoreTokens()) {
            this.scopes.add(st.nextToken());
        }
    }

}
