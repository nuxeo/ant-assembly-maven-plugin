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
 *     Kohsuke Kawaguchi - initial implementation
 *     Nuxeo - bstefanescu, jcarsique, slacoin
 */

package org.nuxeo.build.ant.artifact;

import java.io.File;

import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;

/**
 * Attaches the artifact to Maven.
 *
 * @author Kohsuke Kawaguchi
 */
public class AttachArtifactTask extends Task {

    private File file;

    private String classifier;

    private String type;

    /**
     * The file to be treated as an artifact.
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Optional classifier. If left unset, the task will attach the main
     * artifact.
     */
    public void setClassifier(String classifier) {
        this.classifier = "".equals(classifier) ? null : classifier;
    }

    /**
     * @deprecated since 2.0 Attach now only works on the current
     *             {@link MavenProject}
     * @see AntBuildMojo#getProject()
     */
    @Deprecated
    public void setTarget(String artifactKey) {
        AntClient.getInstance().log(
                "The target parameter is deprecated and ignored. The attach task now only applies to the current project.",
                Project.MSG_WARN);
    }

    /**
     * Artifact type. Think of it as a file extension. Optional, and if omitted,
     * inferred from the file extension.
     */
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public void execute() throws BuildException {
        MavenProject pom = AntBuildMojo.getInstance().getProject();
        log("Attaching " + file + " to " + pom, Project.MSG_INFO);
        if (type == null) {
            type = getExtension(file.getName());
            log("Unspecified type, using: " + type, Project.MSG_WARN);
        }
        AntBuildMojo.getInstance().getProjectHelper().attachArtifact(pom, type,
                classifier, file);
    }

    /**
     * Guess type from the file extension whereas the default implementation of
     * {@link MavenProjectHelper} defaults to "jar".
     */
    private String getExtension(String name) {
        int idx = name.lastIndexOf('.');
        return name.substring(idx + 1);
    }
}
