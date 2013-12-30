/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileCleaningTracker;
import org.apache.maven.plugin.logging.Log;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.DemuxInputStream;
import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;
import org.apache.tools.ant.taskdefs.Typedef;
import org.codehaus.plexus.util.IOUtil;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AntClient {

    public final static String MAVEN_CLIENT_REF = "maven.client.ref";

    protected ClassLoader loader;

    protected Project project;

    protected boolean allowInput = false;

    protected Log mavenLog;

    private static final FileCleaningTracker FILE_CLEANING_TRACKER = new FileCleaningTracker();

    public boolean isAllowInput() {
        return allowInput;
    }

    public void setAllowInput(boolean allowInput) {
        this.allowInput = allowInput;
    }

    public AntClient(Log logger) {
        this(null, logger);
    }

    public AntClient(ClassLoader loader, Log logger) {
        if (loader == null) {
            loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = AntClient.class.getClassLoader();
            }
        }
        this.loader = loader;
        this.mavenLog = logger;
        project = new Project();
        project.setCoreLoader(loader);
        project.setKeepGoingMode(false);
        project.addBuildListener(createLogger());
        project.init();
        initTasks();
    }

    // xmlns:nx="urn:nuxeo-build"
    // xmlns:artifact="urn:nuxeo-artifact">
    // <taskdef resource="org/nuxeo/build/antlib.xml" uri="urn:nuxeo-build" />
    // <taskdef resource="org/nuxeo/build/artifact/antlib.xml"
    // uri="urn:nuxeo-artifact" />
    /**
     * @since 2.0
     */
    protected void initTasks() {
        mavenLog.debug("Initialize Ant Tasks");
        Typedef typedef = new Typedef();
        typedef.setProject(project);
        typedef.setResource("org/nuxeo/build/antlib.xml");
        typedef.execute();
        typedef = new Typedef();
        typedef.setProject(project);
        typedef.setResource("org/nuxeo/build/artifact/antlib.xml");
        typedef.execute();
    }

    public Project getProject() {
        return project;
    }

    public void run(File buildFile) throws BuildException {
        run(buildFile, (List<String>) null);
    }

    public void run(URL buildFile) throws BuildException {
        run(saveURL(buildFile), null);
    }

    public void run(URL buildFile, List<String> targets) throws BuildException {
        run(saveURL(buildFile), targets);
    }

    public void run(File buildFile, List<String> targets) throws BuildException {
        PrintStream previousErr = System.err;
        PrintStream previousOut = System.out;
        InputStream in = System.in;
        InputStream newIn = null;
        PrintStream newOut = null;
        PrintStream newErr = null;
        try {
            // Configure IO
            InputHandler handler = new DefaultInputHandler();
            project.setInputHandler(handler);
            if (allowInput) {
                project.setDefaultInputStream(System.in);
            }
            newIn = new DemuxInputStream(project);
            System.setIn(newIn);
            newOut = new PrintStream(new DemuxOutputStream(project, false));
            System.setOut(newOut);
            newErr = new PrintStream(new DemuxOutputStream(project, true));
            System.setErr(newErr);

            project.setUserProperty("ant.file", buildFile.getPath());
            ProjectHelper.configureProject(project, buildFile);

            project.fireBuildStarted();
            if (targets != null) {
                project.getExecutor().executeTargets(project,
                        targets.toArray(new String[targets.size()]));
            } else {
                project.getExecutor().executeTargets(project,
                        new String[] { project.getDefaultTarget() });
            }
            project.fireBuildFinished(null);
        } catch (BuildException e) {
            project.fireBuildFinished(e);
            throw e;
        } finally {
            System.setOut(previousOut);
            System.setErr(previousErr);
            System.setIn(in);
            IOUtil.close(newIn);
            IOUtil.close(newOut);
            IOUtil.close(newErr);
        }
    }

    protected BuildLogger createLogger() {
        BuildLogger logger = new DefaultLogger();
        logger.setOutputPrintStream(System.out);
        logger.setErrorPrintStream(System.err);
        // logger.setEmacsMode(false);
        if (mavenLog == null) {
            logger.setMessageOutputLevel(Project.MSG_INFO);
        } else if (mavenLog.isDebugEnabled()) {
            logger.setMessageOutputLevel(Project.MSG_DEBUG);
        } else if (mavenLog.isInfoEnabled()) {
            logger.setMessageOutputLevel(Project.MSG_INFO);
        } else if (mavenLog.isWarnEnabled()) {
            logger.setMessageOutputLevel(Project.MSG_WARN);
        } else if (mavenLog.isErrorEnabled()) {
            logger.setMessageOutputLevel(Project.MSG_ERR);
        } else { // TRACE logs
            logger.setMessageOutputLevel(Project.MSG_VERBOSE);
        }
        return logger;
    }

    private File saveURL(URL url) {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            File file = File.createTempFile("ant_client_url_", ".tmp");
            FILE_CLEANING_TRACKER.track(file, this);
            in = url.openStream();
            out = new FileOutputStream(file);
            IOUtil.copy(in, out);
            return file;
        } catch (IOException e) {
            throw new BuildException(e);
        } finally {
            IOUtil.close(in);
            IOUtil.close(out);
        }
    }

    protected void setLog(Log log) {
        this.mavenLog = log;
    }
}
