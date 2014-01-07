/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique, slacoin
 *
 */

package org.nuxeo.build.maven.filter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.Project;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.graph.Node;
import org.sonatype.aether.graph.DependencyNode;

/**
 * @author jcarsique
 *
 */
public class ManifestBundleCategoryFilter extends AbstractFilter {

    public static final String MANIFEST_BUNDLE_CATEGORY = "Bundle-Category";

    public static final String MANIFEST_BUNDLE_CATEGORY_TOKEN = ",";

    protected List<char[]> patterns = new ArrayList<>();

    protected boolean isDependOnCategory;

    private String patternsStr;

    public ManifestBundleCategoryFilter(String patterns,
            boolean isDependsOnCategory) {
        this.isDependOnCategory = isDependsOnCategory;
        StringTokenizer st = new StringTokenizer(patterns,
                MANIFEST_BUNDLE_CATEGORY_TOKEN);
        while (st.hasMoreTokens()) {
            this.patterns.add(st.nextToken().toCharArray());
        }
        this.patternsStr = patterns;
    }

    protected List<String> getValuesToMatch(Artifact artifact) {
        List<String> valuesToMatch = new ArrayList<>();
        File file = artifact.getFile();
        if (file == null) {
            if (artifact.isResolved()) {
                AntClient.getInstance().log(
                        "Artifact " + artifact + " doesn't contain a file",
                        Project.MSG_WARN);
            } else if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
                    && !"pom".equalsIgnoreCase(artifact.getType())) {
                // ignore provided artifacts; raise a warning for non provided
                AntClient.getInstance().log(
                        "Artifact " + artifact + " unresolved",
                        Project.MSG_WARN);
            }
            return valuesToMatch;
        }
        // ignore non jar files
        if (!file.getName().endsWith(".jar")) {
            return valuesToMatch;
        }
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(file, true);
            Manifest mf = jarFile.getManifest();
            if (mf != null) {
                Attributes attributes = mf.getMainAttributes();
                if (attributes != null) {
                    String bundleCategories = attributes.getValue(MANIFEST_BUNDLE_CATEGORY);
                    if (bundleCategories != null) {
                        StringTokenizer st = new StringTokenizer(
                                bundleCategories,
                                MANIFEST_BUNDLE_CATEGORY_TOKEN);
                        while (st.hasMoreTokens()) {
                            valuesToMatch.add(st.nextToken());
                        }
                    }
                }
            } else {
                AntClient.getInstance().log(
                        "Artifact " + artifact + " doesn't contain a manifest",
                        Project.MSG_WARN);
            }
        } catch (IOException e) {
            AntClient.getInstance().log(
                    "error while inspecting this jar manifest: "
                            + artifact.getFile(), e, Project.MSG_ERR);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    AntClient.getInstance().log(e.getMessage(), e,
                            Project.MSG_ERR);
                }
            }
        }
        return valuesToMatch;
    }

    /**
     * @deprecated 2.0
     */
    @Deprecated
    protected boolean accept(Node node, boolean browseChildren,
            boolean browseParents) {
        // Exclude non Nuxeo artifacts
        if (!node.getArtifact().getGroupId().startsWith("org.nuxeo")) {
            return false;
        }
        if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
            AntClient.getInstance().log(
                    "Filtering - " + getClass() + " looking at "
                            + node.getArtifact(), Project.MSG_DEBUG);
        }
        // quick check of already accepted nodes
        boolean accept = node.isAcceptedCategory(patterns);
        // else check artifact's Manifest
        if (!accept) {
            // accept=accept(node.getArtifact());
            accept = checkCategoryFromManifest(node);
        }

        if (!accept && isDependOnCategory && browseChildren) {
            // check if there's an acceptable/accepted child
            if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
                AntClient.getInstance().log(
                        "Filtering - check children of " + node,
                        Project.MSG_DEBUG);
            }
            Collection<DependencyNode> children = node.getChildren();
            // if (children!=null) {
            for (DependencyNode child : children) {
                if (accept((Node) child, true, false)) {
                    accept = true;
                    break;
                }
            }
            // }
        }
        if (!accept && browseParents) {
            // check if there's an acceptable/accepted parent
            if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
                AntClient.getInstance().log(
                        "Filtering - check parents of " + node,
                        Project.MSG_DEBUG);
            }
            Collection<DependencyNode> parents = node.getParents();
            // if (parents!=null) {
            for (DependencyNode parent : parents) {
                if (accept((Node) parent, false, true)) {
                    accept = true;
                    break;
                }
            }
            // }
        }
        if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
            AntClient.getInstance().log(
                    "Filtering - result for " + node.getArtifact() + " : "
                            + accept, Project.MSG_DEBUG);
        }
        return accept;
    }

    private boolean checkCategoryFromManifest(Node node) {
        boolean accept = false;
        for (String valueToMatch : getValuesToMatch(node.getArtifact())) {
            for (char[] pattern : patterns) {
                if (matchPattern(valueToMatch, pattern)) {
                    if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
                        AntClient.getInstance().log(
                                "Filtering - match on "
                                        + String.valueOf(pattern),
                                Project.MSG_DEBUG);
                    }
                    accept = true;
                    node.setAcceptedCategory(pattern);
                    break;
                }
            }
        }
        return accept;
    }

    /**
     * @deprecated prefer use of {@link #accept(Node)} as it remembers already
     *             parsed artifacts
     */
    @Deprecated
    @Override
    public boolean accept(Artifact artifact) {
        boolean accept = matchPattern(getValuesToMatch(artifact));
        if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
            AntClient.getInstance().log(
                    (accept ? "Accepts " : "Rejects ") + artifact,
                    Project.MSG_DEBUG);
        }
        return accept;
    }

    private boolean matchPattern(List<String> valuesToMatch) {
        for (String valueToMatch : valuesToMatch) {
            for (char[] pattern : patterns) {
                if (matchPattern(valueToMatch, pattern)) {
                    if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
                        AntClient.getInstance().log(
                                "Filtering - match on "
                                        + String.valueOf(pattern),
                                Project.MSG_DEBUG);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean matchPattern(String name, char[] pattern) {
        return matchPattern(name.toCharArray(), pattern);
    }

    public boolean matchPattern(char[] name, char[] pattern) {
        return matchPattern(name, 0, name.length, pattern);
    }

    public boolean matchPattern(char[] name, int offset, int len, char[] pattern) {
        int i = offset;
        boolean wildcard = false;
        for (char c : pattern) {
            switch (c) {
            case '*':
                wildcard = true;
                break;
            case '?':
                i++;
                break;
            default:
                if (wildcard) {
                    while (i < len) {
                        if (name[i++] == c) {
                            break;
                        }
                    }
                    if (i == len) {
                        return true;
                    }
                    wildcard = false;
                } else if (i >= len || name[i] != c) {
                    return false;
                } else {
                    i++;
                }
                break;
            }
        }
        return wildcard || i == len;
    }

    @Override
    public String toString() {
        return "" + getClass() + " patterns[" + patternsStr + "]";
    }

    public void setDependsOnCategory(boolean isDependsOnCategory) {
        this.isDependOnCategory = isDependsOnCategory;
    }

    @Override
    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
        throw new UnsupportedOperationException("Not supported");
    }

}
