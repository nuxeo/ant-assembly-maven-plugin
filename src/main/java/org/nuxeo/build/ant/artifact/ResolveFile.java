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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.build.ant.artifact;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileResource;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.graph.DependencyUtils;

/**
 * TODO NXBT-258
 */
public class ResolveFile extends FileResource {

    protected String key;

    protected String classifier;

    private File file = null;

    public void setKey(String pattern) {
        int p = pattern.lastIndexOf(';');
        if (p > -1) {
            key = pattern.substring(0, p);
            classifier = pattern.substring(p + 1);
        } else {
            key = pattern;
        }
    }

    /**
     * @deprecated since 1.8; put classifier in the key ("groupId:artifactId:version:type:classifier:scope")
     * @param classifier
     */
    @Deprecated
    public void setClassifier(String classifier) {
        log("The classifier parameter is deprecated, put it in the key.", Project.MSG_WARN);
        this.classifier = classifier;
    }

    protected File resolveFile() {
        ArtifactDescriptor ad = new ArtifactDescriptor(key);
        // Sync classifier set from key or from setClassifier()
        if (ad.classifier != null) {
            classifier = ad.classifier;
        } else if (classifier != null) {
            ad.classifier = classifier;
        }
        try {
            if (file != null) {
                return file;
            }
            Artifact artifact = ad.getAetherArtifact();
            if (artifact.getFile() != null) {
                file = artifact.getFile();
                return file;
            }
            if ("".equals(artifact.getVersion())) {
                artifact = DependencyUtils.setManagedVersion(artifact);
            }
            if ("".equals(artifact.getVersion())) {
                artifact = DependencyUtils.setNewestVersion(artifact);
            }
            artifact = DependencyUtils.resolve(artifact);
            file = artifact.getFile();
            return file;
        } catch (ArtifactResolutionException e) {
            throw new BuildException(String.format("Cannot resolve file with key '%s'", ad), e);
        }
    }

    @Override
    public File getFile() {
        if (isReference()) {
            return ((FileResource) getCheckedRef()).getFile();
        }
        return resolveFile();
    }

    @Override
    public File getBaseDir() {
        return isReference() ? ((FileResource) getCheckedRef()).getBaseDir() : getFile().getParentFile();
    }

}
