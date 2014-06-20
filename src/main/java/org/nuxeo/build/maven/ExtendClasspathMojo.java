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
 *     mguillaume, jcarsique
 */
package org.nuxeo.build.maven;

import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorException;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.nuxeo.build.maven.graph.DependencyUtils;

/**
 * Verify if a summary file exists (created by integration tests). If the file
 * exists and contains errors, then throw a {@link MojoFailureException}.
 *
 * @see IntegrationTestMojo
 */
@Mojo(name = "extendClasspath", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true, //
requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class ExtendClasspathMojo extends AntBuildMojo {

    @Parameter(property = "managed", required = true)
    public String managed;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        setInstance();
        Artifact artifact = new DefaultArtifact(managed);

        ArtifactDescriptorRequest descriptorRequest = new ArtifactDescriptorRequest();
        descriptorRequest.setArtifact(artifact);
        descriptorRequest.setRepositories(remoteRepositories);

        ArtifactDescriptorResult descriptorResult;
        try {
            descriptorResult = system.readArtifactDescriptor(getSession(),
                    descriptorRequest);
        } catch (ArtifactDescriptorException cause) {
            throw new MojoExecutionException("Cannot load " + artifact, cause);
        }

        final Model model = getProject().getModel();
        for (Dependency aether : descriptorResult.getManagedDependencies()) {
            org.apache.maven.model.Dependency mvn = DependencyUtils.aetherToMaven(aether);
            if (!"jar".equals(mvn.getType())) {
                continue;
            }
            if ("jar-with-dependencies".equals(mvn.getClassifier())) {
                continue;
            }
            model.addDependency(mvn);
        }
    }
}
