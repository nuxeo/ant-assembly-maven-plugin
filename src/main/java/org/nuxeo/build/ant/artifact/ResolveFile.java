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
import org.apache.tools.ant.types.resources.FileResource;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.ArtifactDescriptor;

/**
 * TODO NXBT-258
 */
public class ResolveFile extends FileResource {

    public String key;

    public String classifier;

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
     * @deprecated since 1.8; put classifier in the key
     *             ("groupId:artifactId:version:type:classifier:scope")
     * @param classifier
     */
    @Deprecated
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    protected File resolveFile() {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        ArtifactDescriptor ad = new ArtifactDescriptor(key);
        // Sync classifier set from key or from setClassifier()
        if (ad.classifier != null) {
            classifier = ad.classifier;
        } else if (classifier != null) {
            ad.classifier = classifier;
        }
        Artifact artifact = ad.getAetherArtifact();
        ArtifactDescriptorRequest adRequest = new ArtifactDescriptorRequest(
                artifact, mojo.getRemoteRepositories(), null);
        try {
            ArtifactDescriptorResult adResult = mojo.getSystem().readArtifactDescriptor(
                    mojo.getSession(), adRequest);
            // The artifact after following any relocations
            artifact = adResult.getArtifact();
        } catch (ArtifactDescriptorException e) {
            throw new BuildException(String.format(
                    "Cannot resolve file with key '%s', failed request: %s",
                    ad, adRequest), e);
        }
        if (artifact.getFile() == null) {
            ArtifactRequest artifactRequest = new ArtifactRequest(artifact,
                    mojo.getRemoteRepositories(), null);
            try {
                ArtifactResult artifactResult = mojo.getSystem().resolveArtifact(
                        mojo.getSession(), artifactRequest);
                artifact = artifactResult.getArtifact();
            } catch (ArtifactResolutionException e) {
                throw new BuildException(
                        String.format(
                                "Cannot resolve file with key '%s', failed request: %s",
                                ad, artifactRequest), e);
            }
        }
        return artifact.getFile();
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
        return isReference() ? ((FileResource) getCheckedRef()).getBaseDir()
                : getFile().getParentFile();
    }

}
