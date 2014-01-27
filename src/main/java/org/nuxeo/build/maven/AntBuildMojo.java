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
 *     bstefanescu, jcarsique, slacoin
 */

package org.nuxeo.build.maven;

import java.io.File;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.settings.Settings;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.graph.manager.DependencyManagerUtils;
import org.eclipse.aether.util.graph.selector.AndDependencySelector;
import org.eclipse.aether.util.graph.selector.ExclusionDependencySelector;
import org.eclipse.aether.util.graph.selector.OptionalDependencySelector;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.ant.artifact.Expand;
import org.nuxeo.build.ant.profile.AntProfileManager;
import org.nuxeo.build.maven.filter.TrueFilter;
import org.nuxeo.build.maven.graph.Graph;

/**
 * TODO NXBT-258
 */
@Mojo(name = "build", threadSafe = true, defaultPhase = LifecyclePhase.PACKAGE, //
requiresDependencyCollection = ResolutionScope.TEST, //
requiresDependencyResolution = ResolutionScope.TEST)
public class AntBuildMojo extends AbstractMojo {

    private static final ThreadLocal<AntBuildMojo> instance = new ThreadLocal<>();

    protected Graph graph;

    protected AntProfileManager antProfileManager;

    /**
     * Location of the build file, if unique
     */
    @Parameter
    protected File buildFile;

    /**
     * Location of the build files.
     */
    @Parameter
    protected File[] buildFiles;

    /**
     * Ant target to call on build file(s).
     */
    @Parameter
    protected String target;

    /**
     * Ant targets to call on build file(s).
     *
     * @since 1.6
     */
    @Parameter
    protected String[] targets;

    /**
     * How many levels the graph must be expanded before running Ant.
     */
    @Parameter(defaultValue = "0")
    protected String expand;

    @Component
    protected RepositorySystem system;

    public RepositorySystem getSystem() {
        return system;
    }

    @Parameter(property = "repositorySystemSession", readonly = true)
    protected RepositorySystemSession repositorySystemSession;

    protected DefaultRepositorySystemSession session;

    public DefaultRepositorySystemSession getSession() {
        if (session == null) {
            session = new DefaultRepositorySystemSession(
                    repositorySystemSession);
            DependencySelector depSelector = session.getDependencySelector();
            getLog().debug("Replace DependencySelector " + depSelector);
            DependencySelector depFilter = new AndDependencySelector(
                    new org.nuxeo.build.maven.graph.ScopeDependencySelector(
                            "provided", "test"),
                    new OptionalDependencySelector(),
                    new ExclusionDependencySelector());
            session.setDependencySelector(depFilter);
            session.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE,
                    false);
            session.setConfigProperty(
                    DependencyManagerUtils.CONFIG_PROP_VERBOSE, true);
            session.setReadOnly();
            repositorySystemSession = session;
        }
        return session;
    }

    @Component
    protected MavenProject project;

    @Component
    protected MavenProjectHelper projectHelper;

    @Component
    protected ArtifactHandlerManager artifactHandlerManager;

    public ArtifactHandlerManager getArtifactHandlerManager() {
        return artifactHandlerManager;
    }

    /**
     * Prefix for property names.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = "maven.")
    protected String propertyPrefix;

    @Parameter(property = "localRepository")
    protected ArtifactRepository localRepository;

    /**
     * If true, Ant properties are propagated to Maven.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = "false")
    protected boolean exportAntProperties;

    /**
     * If set, only the listed properties will be set back to Maven from Ant.
     *
     * @since 2.0
     */
    @Parameter
    protected Set<String> exportedAntProperties;

    @Parameter(property = "project.remoteProjectRepositories")
    protected List<RemoteRepository> remoteRepositories;

    public List<RemoteRepository> getRemoteRepositories() {
        return remoteRepositories;
    }

    /**
     * The character encoding scheme to be applied.
     */
    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    protected String encoding;

    public String getEncoding() {
        if (StringUtils.isEmpty(encoding)) {
            getLog().warn(
                    "File encoding has not been set, using platform encoding "
                            + ReaderFactory.FILE_ENCODING
                            + ", i.e. build is platform dependent!");
            encoding = ReaderFactory.FILE_ENCODING;
        }
        return encoding;
    }

    @Parameter(property = "settings")
    protected Settings settings;

    /**
     * If 'false', the Maven build will proceed even if the Ant build fails.
     * Default is 'true'.
     *
     * @since 2.0
     */
    @Parameter(defaultValue = "true")
    protected boolean failOnError;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        instance.set(this);
        settings.setInteractiveMode(false);
        AntClient ant = new AntClient(getLog());
        ant.getProject().setBaseDir(project.getBasedir());
        try {
            setAntReferencesFromMaven(ant.getProject());
        } catch (BuildException | DependencyResolutionRequiredException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        setAntPropertiesFromMaven(ant.getProject());
        if (buildFile != null && buildFiles != null && buildFiles.length > 0) {
            throw new MojoExecutionException(
                    "The configuration parameters 'buildFile' and 'buildFiles' cannot both be used.");
        }
        if (buildFiles == null || buildFiles.length == 0) {
            if (buildFile == null) {
                buildFile = new File("build.xml");
            }
            buildFiles = new File[] { buildFile };
        }

        if (target != null && targets != null && targets.length > 0) {
            throw new MojoExecutionException(
                    "The configuration parameters 'target' and 'targets' cannot both be used.");
        }
        if ((targets == null || targets.length == 0) && target != null) {
            targets = new String[] { target };
        }
        for (File file : buildFiles) {
            try {
                if (targets != null && targets.length > 0) {
                    ant.run(file, Arrays.asList(targets));
                } else {
                    ant.run(file);
                }
                if (exportAntProperties) {
                    setMavenPropertiesFromAnt(ant);
                }
            } catch (BuildException e) {
                String errMsg = String.format(
                        "Error occurred while running %s@%d:%d\n%s",
                        file.getPath(), e.getLocation().getLineNumber(),
                        e.getLocation().getColumnNumber(), e.getMessage());
                if (failOnError) {
                    throw new MojoExecutionException(errMsg, e);
                } else {
                    getLog().error(errMsg, e);
                }
            }
        }
    }

    /**
     * @since 1.10.2
     */
    public Graph newGraph() {
        graph = new Graph();
        graph.addRootNode(project);
        expandGraph(graph);
        return graph;
    }

    /**
     * @param key artifact GAV
     * @return a {@link Graph} which root is artifact resolved from {@code key}
     *
     * @since 2.0
     */
    public Graph newGraph(String key) {
        graph = new Graph();
        graph.addRootNode(key);
        expandGraph(graph);
        return graph;
    }

    protected void expandGraph(Graph newGraph) {
        int depth = Expand.readExpand(expand);
        if (depth > 0) {
            newGraph.resolveDependencies(new TrueFilter(), depth);
        }
    }

    /**
     * @since 2.0
     */
    protected void setMavenPropertiesFromAnt(AntClient ant) {
        Hashtable<String, Object> antProps = ant.getProject().getUserProperties();
        Set<String> keySet;
        if (exportedAntProperties != null && exportedAntProperties.size() > 0) {
            keySet = exportedAntProperties;
        } else {
            keySet = antProps.keySet();
        }
        for (String key : keySet) {
            if (!(antProps.get(key) instanceof String)) {
                continue;
            }
            project.getProperties().setProperty(key, (String) antProps.get(key));
        }
    }

    /**
     * @throws DependencyResolutionRequiredException
     * @throws BuildException
     * @since 2.0
     */
    protected void setAntReferencesFromMaven(Project antProject)
            throws BuildException, DependencyResolutionRequiredException {
        Path p = new Path(antProject);
        p.setPath(StringUtils.join(
                project.getCompileClasspathElements().iterator(),
                File.pathSeparator));
        antProject.addReference(propertyPrefix + "compile.classpath", p);

        p = new Path(antProject);
        p.setPath(StringUtils.join(
                project.getRuntimeClasspathElements().iterator(),
                File.pathSeparator));
        antProject.addReference(propertyPrefix + "runtime.classpath", p);

        p = new Path(antProject);
        p.setPath(StringUtils.join(
                project.getTestClasspathElements().iterator(),
                File.pathSeparator));
        antProject.addReference(propertyPrefix + "test.classpath", p);

        antProject.addReference(propertyPrefix + "project", project);
        antProject.addReference(propertyPrefix + "project.helper",
                projectHelper);
        antProject.addReference(propertyPrefix + "local.repository",
                localRepository);
    }

    /**
     * @since 2.0
     */
    protected void setAntPropertiesFromMaven(Project antProject) {
        for (String key : project.getProperties().stringPropertyNames()) {
            antProject.setUserProperty(key,
                    project.getProperties().getProperty(key));
        }
        antProject.setProperty(propertyPrefix + "basedir",
                project.getBasedir().getPath());
        antProject.setProperty(propertyPrefix + "project.groupId",
                project.getGroupId());
        antProject.setProperty(propertyPrefix + "project.artifactId",
                project.getArtifactId());
        antProject.setProperty(propertyPrefix + "project.version",
                project.getVersion());
        antProject.setProperty(propertyPrefix + "project.name",
                project.getName());
        antProject.setProperty(propertyPrefix + "project.description",
                project.getDescription());
        antProject.setProperty(propertyPrefix + "project.packaging",
                project.getPackaging());
        antProject.setProperty(propertyPrefix + "project.id", project.getId());
        antProject.setProperty(propertyPrefix + "project.build.directory",
                project.getBuild().getDirectory());
        antProject.setProperty(
                propertyPrefix + "project.build.outputDirectory",
                project.getBuild().getOutputDirectory());
        antProject.setProperty(
                (propertyPrefix + "project.build.testOutputDirectory"),
                project.getBuild().getTestOutputDirectory());
        antProject.setProperty(
                (propertyPrefix + "project.build.sourceDirectory"),
                project.getBuild().getSourceDirectory());
        antProject.setProperty(
                (propertyPrefix + "project.build.testSourceDirectory"),
                project.getBuild().getTestSourceDirectory());
        antProject.setProperty((propertyPrefix + "localRepository"),
                localRepository.toString());
        antProject.setProperty((propertyPrefix + "settings.localRepository"),
                localRepository.getBasedir());
        antProject.setProperty(propertyPrefix + "project.build.finalName",
                project.getBuild().getFinalName());
        antProject.setProperty(propertyPrefix + "offline",
                settings.isOffline() ? "-o" : "");

        // add active Maven profiles to Ant
        antProfileManager = new AntProfileManager();
        List<Profile> profiles = getActiveProfiles();
        for (Profile profile : profiles) {
            antProfileManager.activateProfile(profile.getId(), true);
            // define a property for each activated profile
            antProject.setProperty(
                    propertyPrefix + "profile." + profile.getId(), "true");
            // add profile properties (overriding project ones)
            for (String key : profile.getProperties().stringPropertyNames()) {
                antProject.setUserProperty(key,
                        profile.getProperties().getProperty(key));
            }
        }
        // Finally add System properties (overriding project and profile ones)
        for (String key : System.getProperties().stringPropertyNames()) {
            antProject.setUserProperty(key, System.getProperty(key));
        }
    }

    public List<Profile> getActiveProfiles() {
        return project.getActiveProfiles();
    }

    public MavenProject getProject() {
        return project;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public MavenProjectHelper getProjectHelper() {
        return projectHelper;
    }

    public Graph getGraph() {
        if (graph == null) {
            graph = newGraph();
        }
        return graph;
    }

    public AntProfileManager getAntProfileManager() {
        return antProfileManager;
    }

    /**
     * @since 2.0
     */
    public static AntBuildMojo getInstance() {
        return instance.get();
    }

}
