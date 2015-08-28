/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.build.ant.artifact;

import java.io.File;

import org.apache.tools.ant.types.resources.FileResource;

import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.graph.Graph;
import org.nuxeo.build.maven.graph.Node;

/**
 * TODO NXBT-258
 */
public class ArtifactFile extends FileResource {

    protected Graph graph = AntBuildMojo.getInstance().getGraph();

    protected Node node;

    public String key;

    public ArtifactDescriptor ad = new ArtifactDescriptor();

    public void setKey(String key) {
        this.key = key;
        ad = ArtifactDescriptor.parseQuietly(key);
    }

    public void setArtifactId(String artifactId) {
        ad.setArtifactId(artifactId);
    }

    public void setGroupId(String groupId) {
        ad.setGroupId(groupId);
    }

    public void setType(String type) {
        ad.setType(type);
    }

    public void setVersion(String version) {
        ad.setVersion(version);
    }

    public void setClassifier(String classifier) {
        ad.setClassifier(classifier);
    }

    public Node getNode() {
        if (node == null) {
            node = graph.findNode(key, ad);
        }
        return node;
    }

    @Override
    public File getFile() {
        if (isReference()) {
            return ((FileResource) getCheckedRef()).getFile();
        }
        return getNode().getFile();
    }

    @Override
    public File getBaseDir() {
        return isReference() ? ((FileResource) getCheckedRef()).getBaseDir() : getFile().getParentFile();
    }

}
