/*
 * (C) Copyright 2009-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN, jcarsique
 */
package org.nuxeo.build.ant.artifact;

import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Sequential;
import org.eclipse.aether.artifact.Artifact;

/**
 * Iterate through an artifact set
 *
 * Usage:
 * {@code <artifact:foreach setref="bundles" artifactJarPathProperty="path" >
 * (...) </artifact:foreach>}
 *
 */
public class ArtifactForeach extends Sequential {

    public String property;

    public ArtifactSet artifactSet;

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public ArtifactSet getArtifactSet() {
        return artifactSet;
    }

    public void setSetref(String setref) {
        artifactSet = (ArtifactSet) getProject().getReference(setref);

    }

    @Override
    public void execute() throws BuildException {
        for (Artifact artifact : artifactSet.getArtifacts()) {
            String canonicalPath = null;
            try {
                canonicalPath = artifact.getFile().getCanonicalPath();
            } catch (IOException e) {
                throw new BuildException(
                        "Failed to artifact file canonical path for "
                                + artifact, e);
            }
            getProject().setProperty(property + ".file.path", canonicalPath);
            getProject().setProperty(property + ".archetypeId",
                    artifact.getArtifactId());
            getProject().setProperty(property + ".groupId",
                    artifact.getGroupId());
            getProject().setProperty(property + ".version",
                    artifact.getBaseVersion());
            try {
                super.execute();
            } catch (BuildException e) {
                throw new BuildException(
                        "Couldn't execute the task for artifact " + artifact, e);
            }
        }

    }
}
