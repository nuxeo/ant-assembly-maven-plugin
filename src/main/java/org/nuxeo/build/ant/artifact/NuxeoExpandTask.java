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
 *     bstefanescu, slacoin, jcarsique
 */
package org.nuxeo.build.ant.artifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.nuxeo.build.maven.filter.Filter;

/**
 * TODO NXBT-258
 */
public class NuxeoExpandTask extends ExpandTask {

    private Map<String, Boolean> includedScopes = new HashMap<>();

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
        getIncludedScopes().put(JavaScopes.COMPILE, includeCompileScope);
        getIncludedScopes().put(JavaScopes.PROVIDED, includeProvidedScope);
        getIncludedScopes().put(JavaScopes.RUNTIME, includeRuntimeScope);
        getIncludedScopes().put(JavaScopes.TEST, includeTestScope);
        getIncludedScopes().put(JavaScopes.SYSTEM, includeSystemScope);

        filter.addFilter(new Filter() {

            @Override
            public boolean accept(Artifact artifact) {
                return true;
            }

            @Override
            public boolean accept(DependencyNode node,
                    List<DependencyNode> parents) {
                if (node.getDependency().isOptional()) {
                    return false;
                }
                if ("".equals(node.getDependency().getScope())) {
                    log("Missing scope, node accepted: " + node,
                            Project.MSG_WARN);
                    return true;
                }
                return getIncludedScopes().get(node.getDependency().getScope());
            }
        });
        super.execute();
    }

    @Override
    protected boolean acceptNode(DependencyNode node) {
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
    protected Map<String, Boolean> getIncludedScopes() {
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
