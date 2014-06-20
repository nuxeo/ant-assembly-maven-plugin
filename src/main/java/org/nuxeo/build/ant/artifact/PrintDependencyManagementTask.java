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
 *     jcarsique
 */
package org.nuxeo.build.ant.artifact;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.StringUtils;
import org.nuxeo.build.maven.AntBuildMojo;
import org.nuxeo.build.maven.ArtifactDescriptor;
import org.nuxeo.build.maven.graph.DependencyUtils;

/**
 * Print the dependency management of a POM
 * TODO NXBT-258
 *
 * @since 2.0.2
 */
public class PrintDependencyManagementTask extends Task {

    private String output;

    public enum FORMAT {
        // GAV: group:artifact:version:type:classifier
        GAV,
        // Key-value format: FILENAME=GAV
        KV_F_GAV;
    }

    private FORMAT format = FORMAT.KV_F_GAV;

    private boolean append = false;

    private String key;

    private boolean check;

    @Override
    public void execute() throws BuildException {
        AntBuildMojo mojo = AntBuildMojo.getInstance();
        OutputStream out = System.out;
        try {
            if (output != null) {
                out = new FileOutputStream(output, append);
            }
            Artifact artifact;
            if (key == null) {
                artifact = DependencyUtils.mavenToAether(mojo.getProject().getArtifact());
            } else {
                ArtifactDescriptor ad = new ArtifactDescriptor(key);
                artifact = ad.getAetherArtifact();
            }
            if (StringUtils.isEmpty(artifact.getVersion())) {
                artifact = DependencyUtils.setManagedVersion(artifact);
            }
            if (StringUtils.isEmpty(artifact.getVersion())) {
                artifact = DependencyUtils.setNewestVersion(artifact);
            }
            ArtifactDescriptorRequest request = new ArtifactDescriptorRequest();
            request.setArtifact(artifact);
            request.setRepositories(mojo.getRemoteRepositories());
            ArtifactDescriptorResult result = mojo.getSystem().readArtifactDescriptor(
                    mojo.getSession(), request);
            Throwable checks = new Throwable();
            for (Dependency dependency : result.getManagedDependencies()) {
                if (check) {
                    try {
                        DependencyUtils.resolve(dependency.getArtifact());
                    } catch (ArtifactResolutionException e) {
                        checks.addSuppressed(e);
                    }
                }
                out.write(toString(dependency).getBytes(
                        AntBuildMojo.getInstance().getEncoding()));
            }
            for (Throwable t : checks.getSuppressed()) {
                log(t.getMessage(), Project.MSG_WARN);
            }
        } catch (IOException | ArtifactDescriptorException e) {
            throw new BuildException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public String toString(Dependency dependency) {
        Artifact artifact = dependency.getArtifact();
        StringBuilder sb = new StringBuilder();
        switch (format) {
        case KV_F_GAV:
            String filename;
            if (artifact.getFile() == null) {
                filename = artifact.getArtifactId() + "-"
                        + artifact.getBaseVersion();
                if (!StringUtils.isEmpty(artifact.getClassifier())) {
                    filename += "-" + artifact.getClassifier();
                }
                filename += "." + artifact.getExtension();
            } else {
                filename = artifact.getFile().getName();
            }
            sb.append(filename);

            sb.append('=');
            // fall through
        case GAV:
            sb.append(artifact.getGroupId());
            sb.append(':').append(artifact.getArtifactId());
            sb.append(':').append(artifact.getVersion());
            sb.append(':').append(artifact.getExtension());
            sb.append(':').append(artifact.getClassifier());
            sb.append(':').append(dependency.getScope());
            break;
        default:
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    /**
     * Output file. If null, the console is used.
     *
     * @param output
     */
    public void setOutput(String output) {
        this.output = output;
    }

    /**
     * Defines output format
     *
     * @param format
     */
    public void setFormat(FORMAT format) {
        this.format = format;
    }

    /**
     * Output append mode
     *
     * @param append
     */
    public void setAppend(boolean append) {
        this.append = append;
    }

    /**
     * GAV of artifact to analyze. If null, the current project is used.
     *
     * @param key
     *
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Whether to check the artifact availability (using resolve)
     *
     * @param check
     *
     */
    public void setCheck(boolean check) {
        this.check = check;
    }

}
