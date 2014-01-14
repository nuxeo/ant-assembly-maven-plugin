/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.build.ant.artifact;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.DataType;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileResource;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.graph.DependencyUtils;

/**
 * Resolve multiple files from a properties list
 *
 * @since 1.10.2
 */
public class ResolveFiles extends DataType implements ResourceCollection {

    private Properties source;

    private String classifier;

    private List<Resource> artifacts;

    /**
     * Set source of artifacts to resolve
     *
     * @param source Properties files with artifacts list
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void setSource(String source) throws FileNotFoundException,
            IOException {
        this.source = new Properties();
        File sourceFile = new File(source);
        File[] files = sourceFile.getParentFile().listFiles(
                (FileFilter) new WildcardFileFilter(sourceFile.getName()));
        for (File file : files) {
            log("Loading " + file, Project.MSG_DEBUG);
            this.source.load(new FileInputStream(file));
        }
    }

    /**
     * Change classifier of all artifacts to resolve
     *
     * @param classifier Maven classifier
     */
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    @Override
    public boolean isFilesystemOnly() {
        return true;
    }

    @Override
    public Iterator<Resource> iterator() {
        if (isReference()) {
            return ((ResourceCollection) getCheckedRef()).iterator();
        }
        if (artifacts == null) {
            artifacts = new ArrayList<>();
            for (Iterator<?> it = source.values().iterator(); it.hasNext();) {
                String artifactKey = (String) it.next();
                try {
                    artifacts.add(resolveFile(artifactKey));
                } catch (ArtifactResolutionException e) {
                    log(e.getMessage(), Project.MSG_WARN);
                }
            }
        }
        return artifacts.iterator();
    }

    private FileResource resolveFile(String artifactKey)
            throws ArtifactResolutionException {
        ArtifactDescriptor ad = new ArtifactDescriptor(artifactKey);
        if (classifier != null) {
            ad.classifier = classifier;
        }
        Artifact artifact = ad.getAetherArtifact();
        File file;
        if (artifact.getFile() != null) {
            file = artifact.getFile();
        } else {
            if ("".equals(artifact.getVersion())) {
                artifact = DependencyUtils.setManagedVersion(artifact);
            }
            if ("".equals(artifact.getVersion())) {
                artifact = DependencyUtils.setNewestVersion(artifact);
            }
            artifact = DependencyUtils.resolve(artifact);
            file = artifact.getFile();
        }
        FileResource fr = new FileResource(file);
        fr.setBaseDir(file.getParentFile());
        return fr;
    }

    @Override
    public int size() {
        return artifacts.size();
    }

}
