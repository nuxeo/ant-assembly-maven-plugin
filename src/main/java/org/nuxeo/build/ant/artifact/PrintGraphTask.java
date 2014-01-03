/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.graph.Node;
import org.nuxeo.build.maven.graph.PrintDependencyVisitor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class PrintGraphTask extends Task {

    private String output;

    private String mode = PrintDependencyVisitor.MODE_TREE;

    private int format = PrintDependencyVisitor.FORMAT_GAV;

    private boolean append = false;

    private String source;

    private List<String> scopes = Arrays.asList(new String[] { "compile",
            "runtime", "system" });

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
            PrintDependencyVisitor pdv = new PrintDependencyVisitor(out, mode,
                    format, scopes);
            // Ignore roots in flat mode
            if (PrintDependencyVisitor.MODE_FLAT.equalsIgnoreCase(mode)) {
                pdv.setIgnores(roots);
            }
            // DependencyVisitor tdv = new TreeDependencyVisitor(pdv);
            // PreorderNodeListGenerator nlg = new PreorderNodeListGenerator();
            for (Node node : roots) {
                // node.accept(nlg);
                log("Visiting " + node, Project.MSG_DEBUG);
                node.accept(pdv);
            }
            log("All dependencies: "
                    + String.valueOf(pdv.getDependencies(true)),
                    Project.MSG_DEBUG);
        } catch (FileNotFoundException e) {
            throw new BuildException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * @param mode print as tree if true, else print flat
     * @since 1.10.2
     */
    public void setMode(String mode) {
        if (PrintDependencyVisitor.MODE_SDK.equals(mode)) {
            this.mode = PrintDependencyVisitor.MODE_FLAT;
            this.format = PrintDependencyVisitor.FORMAT_KV_F_GAV;
        } else {
            this.mode = mode;
        }
    }

    /**
     * Defines output format
     *
     * @param format
     * @see PrintDependencyVisitor#FORMAT_GAV
     * @see PrintDependencyVisitor#FORMAT_KV_F_GAV
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
