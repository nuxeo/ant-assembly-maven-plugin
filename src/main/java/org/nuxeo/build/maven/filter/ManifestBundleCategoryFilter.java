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
import org.apache.maven.model.Dependency;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.graph.Edge;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author jcarsique
 *
 */
public class ManifestBundleCategoryFilter implements Filter {

    public static final String MANIFEST_BUNDLE_CATEGORY = "Bundle-Category";

    public static final String MANIFEST_BUNDLE_CATEGORY_TOKEN = ",";

    protected List<char[]> patterns = new ArrayList<char[]>();

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
        List<String> valuesToMatch = new ArrayList<String>();
        File file = artifact.getFile();
        if (file == null) {
            if (artifact.isResolved()) {
                AntBuildMojo.getInstance().getLog().warn(
                        "Artifact " + artifact + " doesn't contain a file");
            } else if (!Artifact.SCOPE_PROVIDED.equals(artifact.getScope())
                    && !"pom".equalsIgnoreCase(artifact.getType())) {
                // ignore provided artifacts; raise a warning for non provided
                AntBuildMojo.getInstance().getLog().warn(
                        "Artifact " + artifact + " unresolved");
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
                AntBuildMojo.getInstance().getLog().warn(
                        "Artifact " + artifact + " doesn't contain a manifest");
            }
        } catch (IOException e) {
            AntBuildMojo.getInstance().getLog().error(
                    "error while inspecting this jar manifest: "
                            + artifact.getFile(), e);
        } finally {
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    AntBuildMojo.getInstance().getLog().error(e.getMessage(), e);
                }
            }
        }
        return valuesToMatch;
    }

    public boolean accept(Node node) {
        return accept(node, true, true);
    }

    private boolean accept(Node node, boolean browseChildren,
            boolean browseParents) {
        // Exclude non Nuxeo artifacts
        if (!node.getArtifact().getGroupId().startsWith("org.nuxeo")) {
            return false;
        }
        if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
            AntBuildMojo.getInstance().getLog().debug(
                    "Filtering - " + getClass() + " looking at "
                            + node.getArtifact());
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
                AntBuildMojo.getInstance().getLog().debug(
                        "Filtering - check children of " + node);
            }
            Collection<Edge> children = node.getEdgesOut();
            // if (children!=null) {
            for (Edge edge : children) {
                if (accept(edge.out, true, false)) {
                    accept = true;
                    break;
                }
            }
            // }
        }
        if (!accept && browseParents) {
            // check if there's an acceptable/accepted parent
            if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
                AntBuildMojo.getInstance().getLog().debug(
                        "Filtering - check parents of " + node);
            }
            Collection<Edge> parents = node.getEdgesIn();
            // if (parents!=null) {
            for (Edge edge : parents) {
                if (accept(edge.in, false, true)) {
                    accept = true;
                    break;
                }
            }
            // }
        }
        if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
            AntBuildMojo.getInstance().getLog().debug(
                    "Filtering - result for " + node.getArtifact() + " : "
                            + accept);
        }
        return accept;
    }

    private boolean checkCategoryFromManifest(Node node) {
        boolean accept = false;
        for (String valueToMatch : getValuesToMatch(node.getArtifact())) {
            for (char[] pattern : patterns) {
                if (matchPattern(valueToMatch, pattern)) {
                    if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
                        AntBuildMojo.getInstance().getLog().debug(
                                "Filtering - match on "
                                        + String.valueOf(pattern));
                    }
                    accept = true;
                    node.setAcceptedCategory(pattern);
                    break;
                }
            }
        }
        return accept;
    }

    public boolean accept(Edge edge, Dependency dep) {
        throw new UnsupportedOperationException("Not supported");
    }

    public boolean accept(Edge edge) {
        throw new UnsupportedOperationException("Not supported");
    }

    /**
     * @deprecated prefer use of {@link #accept(Node)} as it remembers already
     *             parsed artifacts
     */
    public boolean accept(Artifact artifact) {
        boolean accept = matchPattern(getValuesToMatch(artifact));
        if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
            AntBuildMojo.getInstance().getLog().debug(
                    (accept ? "Accepts " : "Rejects ") + artifact);
        }
        return accept;
    }

    private boolean matchPattern(List<String> valuesToMatch) {
        for (String valueToMatch : valuesToMatch) {
            for (char[] pattern : patterns) {
                if (matchPattern(valueToMatch, pattern)) {
                    if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
                        AntBuildMojo.getInstance().getLog().debug(
                                "Filtering - match on "
                                        + String.valueOf(pattern));
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

}
