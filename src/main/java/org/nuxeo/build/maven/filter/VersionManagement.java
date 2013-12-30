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
package org.nuxeo.build.maven.filter;

import java.util.HashMap;

import org.apache.maven.model.Dependency;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.graph.Graph;
import org.nuxeo.build.maven.graph.Node;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class VersionManagement {

    protected HashMap<String, String> versions; // key to versions map

    public String getVersion(ArtifactDescriptor ad) {
        return getVersion(ad.groupId, ad.artifactId, ad.type, ad.classifier);
    }

    public String getVersion(String groupId, String artifactId) {
        return getVersion(groupId, artifactId, "", "");
    }

    public String getVersion(String groupId, String artifactId, String type) {
        return getVersion(groupId, artifactId, type, "");
    }

    public String getVersion(String groupId, String artifactId, String type,
            String classifier) {
        String key = makeKey(groupId, artifactId, type, classifier);
        return getVersion(key);
    }

    protected String getVersion(String key) {
        load();
        return versions.get(key);
    }

    protected synchronized void load() {
        if (versions != null) {
            return;
        }
        versions = new HashMap<String, String>();
        Graph graph = AntBuildMojo.getInstance().getGraph();
        for (Node root : graph.getRoots()) {
            org.apache.maven.model.DependencyManagement mgr = root.getPom().getDependencyManagement();
            for (Dependency dep : mgr.getDependencies()) {
                versions.put(
                        makeKey(dep.getGroupId(), dep.getArtifactId(),
                                dep.getType(), dep.getClassifier()),
                        dep.getVersion());
            }
        }
    }

    private final String makeKey(String groupId, String artifactId,
            String type, String classifier) {
        if (artifactId == null || groupId == null) {
            throw new IllegalArgumentException(
                    "Cannot make key from "
                            + groupId
                            + ":"
                            + artifactId
                            + ":"
                            + type
                            + ":"
                            + classifier
                            + "\nBoth artifactId and groupId are required when requesting a version from dependency management.");
        }
        if (type == null) {
            type = "";
        }
        if (classifier == null) {
            classifier = "";
        }
        return artifactId + ":" + groupId + ":" + type + ":" + classifier;
    }

}
