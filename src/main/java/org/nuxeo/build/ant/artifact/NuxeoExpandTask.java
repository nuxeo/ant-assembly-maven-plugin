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
 *     bstefanescu, slacoin
 */
package org.nuxeo.build.ant.artifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.filter.Filter;
import org.nuxeo.build.maven.filter.GroupIdFilter;
import org.nuxeo.build.maven.filter.NotFilter;
import org.nuxeo.build.maven.filter.VersionFilter;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;
import org.sonatype.aether.graph.DependencyNode;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoExpandTask extends ExpandTask {

    private HashMap<String, Boolean> includedScopes = new HashMap<>();

    private boolean includeCompileScope = true;

    private boolean includeProvidedScope = false;

    private boolean includeRuntimeScope = true;

    private boolean includeTestScope = false;

    private boolean includeSystemScope = true;

    private String[] groupPrefixes = new String[] { "org.nuxeo" };

    /**
     * @param groupPrefixes Comma separated list of accepted group prefixes
     * @since 1.11
     */
    public void setGroupPrefixes(String groupPrefixes) {
        List<String> prefixes = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(groupPrefixes, ",");
        while (st.hasMoreTokens()) {
            prefixes.add(st.nextToken());
        }
        this.groupPrefixes = prefixes.toArray(new String[0]);
    }

    public NuxeoExpandTask() {
        super();
        setDepth("all");
    }

    @Override
    public void execute() throws BuildException {
        getIncludedScopes().put("compile", includeCompileScope);
        getIncludedScopes().put("provided", includeProvidedScope);
        getIncludedScopes().put("runtime", includeRuntimeScope);
        getIncludedScopes().put("test", includeTestScope);
        getIncludedScopes().put("system", includeSystemScope);

        filter.addFilter(new Filter() {

            @Override
            public boolean accept(Artifact artifact) {
                return true;
            }

            @Override
            public boolean accept(Edge edge) {
                if (edge.isOptional) {
                    return false;
                }
                if (edge.scope == null) {
                    AntBuildMojo.getInstance().getLog().warn(
                            "Missing scope, set to compile : " + edge);
                    edge.scope = "compile";
                }
                return getIncludedScopes().get(edge.scope);
            }

            @Override
            public boolean accept(Node node) {
                return true;
            }

            @Override
            public boolean accept(DependencyNode node,
                    List<DependencyNode> parents) {
                if (node.getDependency().isOptional()) {
                    return false;
                }
                if (node.getDependency().getScope() == null) {
                    AntBuildMojo.getInstance().getLog().warn(
                            "Missing scope, set to compile : " + node);
                    node.getDependency().setScope("compile");
                }
                return getIncludedScopes().get(node.getDependency().getScope());
            }
        });
        filter.addFilter(new NotFilter(new VersionFilter("[*)")));
        filter.addFilter(new NotFilter(new GroupIdFilter("org.nuxeo.build")));
        super.execute();
    }

    @Override
    protected boolean acceptNode(Node node) {
        for (String prefix : groupPrefixes) {
            if (node.getArtifact().getGroupId().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @since 1.10.2
     */
    protected HashMap<String, Boolean> getIncludedScopes() {
        return includedScopes;
    }

    /**
     * @since 1.10.2
     */
    public void setIncludeCompileScope(boolean includeCompileScope) {
        this.includeCompileScope = includeCompileScope;
    }

    /**
     * @since 1.10.2
     */
    public void setIncludeProvidedScope(boolean includeProvidedScope) {
        this.includeProvidedScope = includeProvidedScope;
    }

    /**
     * @since 1.10.2
     */
    public void setIncludeRuntimeScope(boolean includeRuntimeScope) {
        this.includeRuntimeScope = includeRuntimeScope;
    }

    /**
     * @since 1.10.2
     */
    public void setIncludeTestScope(boolean includeTestScope) {
        this.includeTestScope = includeTestScope;
    }

    /**
     * @since 1.10.2
     */
    public void setIncludeSystemScope(boolean includeSystemScope) {
        this.includeSystemScope = includeSystemScope;
    }
}
