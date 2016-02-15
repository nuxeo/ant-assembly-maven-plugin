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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.surefire.SurefireHelper;
import org.apache.maven.plugin.surefire.SurefireReportParameters;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.surefire.suite.RunResult;
import org.codehaus.plexus.util.StringUtils;

/**
 * Verify if a summary file exists (created by integration tests). If the file exists and contains errors, then throw a
 * {@link MojoFailureException}.
 *
 * @see IntegrationTestMojo
 */
@Mojo(name = "verify", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true, //
requiresProject = true, requiresDependencyResolution = ResolutionScope.TEST)
public class VerifyMojo extends AntBuildMojo implements SurefireReportParameters {

    /**
     * The summary file to read integration test results from.
     */
    @Parameter(defaultValue = "${project.build.directory}/nxtools-reports/nxtools-summary.xml", required = true)
    protected File summaryFile;

    /**
     * Additional summary files to read integration test results from.
     *
     * @since 2.0
     */
    @Parameter
    protected File[] summaryFiles;

    /**
     * Set this to {@code true} to skip running integration tests.
     *
     * @since 2.0
     */
    @Parameter(property = "skipITs")
    protected boolean skipITs;

    /**
     * Set this to {@code true} to skip running tests.
     *
     * @since 2.0.3
     */
    @Parameter(property = "skipTests")
    protected boolean skipTests;

    /**
     * The character encoding scheme to be applied.
     *
     * @since 2.0
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

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @since 2.0
     */
    @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
    protected boolean testFailureIgnore;

    /**
     * Set this to "true" to cause a failure if there are no tests to run.
     *
     * @since 2.0
     */
    @Parameter(property = "failIfNoTests")
    protected Boolean failIfNoTests;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (isSkipITs() || isSkipTests()) {
            getLog().info("Tests are skipped.");
            return;
        }

        RunResult summary;
        try {
            if (!summaryFile.isFile() && summaryFiles != null) {
                summary = RunResult.noTestsRun();
            } else {
                summary = readSummary(getEncoding(), summaryFile);
            }
            if (summaryFiles != null) {
                for (File file : summaryFiles) {
                    summary = summary.aggregate(readSummary(getEncoding(), file));
                }
            }
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        SurefireHelper.reportExecution(this, summary, getLog());
    }

    private RunResult readSummary(String sumEncoding, File sumFile) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(sumFile))) {
            return RunResult.fromInputStream(in, sumEncoding);
        }
    }

    /**
     * @since 2.0.3
     */
    public boolean isSkipITs() {
        return skipITs;
    }

    /**
     * @since 2.0.3
     */
    public void setSkipITs(boolean skipITs) {
        this.skipITs = skipITs;
    }

    @Override
    public boolean isSkipTests() {
        return skipTests;
    }

    @Override
    public void setSkipTests(boolean skipTests) {
        this.skipTests = skipTests;
    }

    /**
     * @deprecated Since 2.0.3. Use {@link #isSkipITs()} or {@link #isSkipTests()}
     */
    @Override
    @Deprecated
    public boolean isSkipExec() {
        return isSkipITs() || isSkipTests();
    }

    /**
     * @deprecated Since 2.0.3. Use {@link #setSkipITs(boolean)} or {@link #setSkipTests(boolean)}
     */
    @Override
    @Deprecated
    public void setSkipExec(boolean skipExec) {
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated Since 2.0.3. Use {@link #isSkipITs()} or {@link #isSkipTests()}
     */
    @Override
    @Deprecated
    public boolean isSkip() {
        return isSkipITs() || isSkipTests();
    }

    /**
     * @deprecated Since 2.0.3. Use {@link #setSkipITs(boolean)} or {@link #setSkipTests(boolean)}
     */
    @Override
    @Deprecated
    public void setSkip(boolean skip) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isTestFailureIgnore() {
        return testFailureIgnore;
    }

    @Override
    public void setTestFailureIgnore(boolean testFailureIgnore) {
        this.testFailureIgnore = testFailureIgnore;
    }

    @Override
    public File getBasedir() {
        return project.getBasedir();
    }

    @Override
    public void setBasedir(File basedir) {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getTestClassesDirectory() {
        return new File(project.getBuild().getTestOutputDirectory());
    }

    @Override
    public void setTestClassesDirectory(File testClassesDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getReportsDirectory() {
        return summaryFile.getParentFile();
    }

    @Override
    public void setReportsDirectory(File reportsDirectory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean getFailIfNoTests() {
        return failIfNoTests;
    }

    @Override
    public void setFailIfNoTests(boolean failIfNoTests) {
        throw new UnsupportedOperationException();
    }

}
