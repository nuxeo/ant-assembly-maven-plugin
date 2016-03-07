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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.taskdefs.optional.testing.BuildTimeoutException;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;

/**
 * Wrap the Ant executions to catch their build result and store a Surefire/Failsafe compliant summary file. Typical
 * usage is the execution of Ant commands performing some tests.
 *
 * @see VerifyMojo
 */
@Mojo(name = "integration-test", defaultPhase = LifecyclePhase.INTEGRATION_TEST, threadSafe = true, //
requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class IntegrationTestMojo extends AntBuildMojo {

    private static final String FAILSAFE_IN_PROGRESS_CONTEXT_KEY = "failsafe-in-progress";

    /**
     * That property is ignored.
     *
     * @deprecated Since 2.0. Now never fail during integration-test and rely on verify for exception raise.
     */
    @Deprecated
    @Parameter(defaultValue = "true", property = "maven.test.failure.ignore")
    protected boolean testFailureIgnore;

    /**
     * The summary file to write integration test results to.
     */
    @Parameter(defaultValue = "${project.build.directory}/nxtools-reports/nxtools-summary.xml", required = true)
    protected File summaryFile;

    /**
     * Set this to {@code true} to skip running integration tests.
     *
     * @since 2.0
     */
    @Parameter(property = "skipITs")
    protected boolean skipITs;

    /**
     * The character encoding scheme to be applied. Defaults to {@link AntBuildMojo#getEncoding()} if not defined.
     *
     * @since 2.0
     * @see AntBuildMojo#encoding
     */
    @Parameter(defaultValue = "${project.reporting.outputEncoding}")
    protected String reportingEncoding;

    @Override
    public String getEncoding() {
        if (StringUtils.isEmpty(reportingEncoding)) {
            reportingEncoding = super.getEncoding();
        }
        return reportingEncoding;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipITs) {
            getLog().info("Tests are skipped.");
            return;
        }
        RunResult result = RunResult.noTestsRun();
        boolean failFast = failOnError;
        // Ensure exception raise from AntBuildMojo#execute
        failOnError = true;
        File[] testBuildFiles = getBuildFiles();
        for (File file : testBuildFiles) {
            buildFiles = null;
            buildFile = file;
            try {
                super.execute();
                result = result.aggregate(new RunResult(1, 0, 0, 0));
            } catch (MojoFailureException | MojoExecutionException e) {
                if (e.getCause() instanceof ExitStatusException) {
                    ExitStatusException exit = (ExitStatusException) e.getCause();
                    getLog().debug("Result: " + exit.getStatus());
                    getLog().debug(exit.getMessage());
                    result = result.aggregate(new RunResult(1, 1, 0, 0));
                } else {
                    Throwable cause = e.getCause();
                    if (cause == null) {
                        cause = e;
                    }
                    getLog().error(cause.getMessage(), cause);
                    result = result.aggregate(new RunResult(1, 0, 1, 0, cause.getMessage(),
                            cause instanceof BuildTimeoutException));
                }
                if (failFast) {
                    break;
                }
            }
        }
        writeSummary(result);
    }

    /**
     * @since 2.0.5
     */
    protected void writeSummary(RunResult summary) throws MojoExecutionException {
        if (!summaryFile.getParentFile().isDirectory()) {
            summaryFile.getParentFile().mkdirs();
        }
        FileOutputStream fout = null;
        FileInputStream fin = null;
        try {
            Object token = getPluginContext().get(FAILSAFE_IN_PROGRESS_CONTEXT_KEY);
            summary.writeSummary(summaryFile, token != null, getEncoding());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } finally {
            IOUtil.close(fin);
            IOUtil.close(fout);
        }
        getPluginContext().put(FAILSAFE_IN_PROGRESS_CONTEXT_KEY, FAILSAFE_IN_PROGRESS_CONTEXT_KEY);
    }

}
