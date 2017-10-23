/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     akervern
 */
package org.nuxeo.build.ant;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.FileProvider;
import org.apache.tools.ant.types.resources.FileResource;
import org.apache.tools.ant.util.ResourceUtils;
import org.nuxeo.studio.components.common.ContributionsExtractor;
import org.nuxeo.studio.components.common.ExtractorOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Converts contributions from Nuxeo's Bundles to the Studio Registry format.
 *
 * @since 2.1.0
 */
public class StudioExtraction extends Task {
    protected String extract = "*";

    protected File todir;

    protected String filename = "studio-registries.json";

    protected boolean failOnEmpty = false;

    protected Vector<ResourceCollection> rcs = new Vector<>();

    /**
     * Perform the extraction
     *
     * @throws BuildException if an error occurs.
     */
    @Override
    public void execute() throws BuildException {
        validateAttributes();
        try {

            new ContributionsExtractor(buildOptions()).publish();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

    public void validateAttributes() {
        if (todir == null) {
            throw new BuildException("todir attribute cannot be empty");
        }

        if (rcs.isEmpty()) {
            throw new BuildException("Use nested FileSet(s) to set jar files to extract");
        }
    }

    /**
     * Add a set of files to extract from a nested element.
     *
     * @param fileSet a file set to extract.
     */
    public void addFileSet(FileSet fileSet) {
        add(fileSet);
    }

    /**
     * Add a resource collection of files to extract from a nested element.
     *
     * @param rc a resource collection to extract.
     */
    public void add(ResourceCollection rc) {
        rcs.add(rc);
    }

    protected ExtractorOptions buildOptions() {
        ExtractorOptions opts = new ExtractorOptions();
        opts.setBuildDirectory(todir.getAbsolutePath());
        opts.setExtract(extract);
        opts.setFailOnEmpty(isFailOnEmpty());
        opts.setOutput(filename);

        List<String> files = new ArrayList<>();
        for (ResourceCollection rc : rcs) {
            if (rc instanceof FileSet) {
                FileSet fs = (FileSet) rc;
                File basedir = fs.getDirectoryScanner().getBasedir();
                for (String file : fs.getDirectoryScanner().getIncludedFiles()) {
                    files.add(new File(basedir, file).getAbsolutePath());
                }
            } else {
                if (!rc.isFilesystemOnly()) {
                    throw new BuildException(
                            "Only FileSystem resources are supported.");
                }

                for (Resource r : rc) {
                    if (!r.isExists()) {
                        log("Warning: Could not find resource " + r.toLongString());
                        continue;
                    }

                    final FileProvider fp = r.as(FileProvider.class);
                    if (fp != null) {
                        final FileResource fr = ResourceUtils.asFileResource(fp);
                        files.add(fr.getFile().getAbsolutePath());
                    }
                }
            }
        }
        opts.setJarFile(StringUtils.join(files, ","));
        return opts;
    }

    public void setTodir(File todir) {
        this.todir = todir;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setExtract(String extract) {
        this.extract = extract;
    }

    public boolean isFailOnEmpty() {
        return failOnEmpty;
    }

    public void setFailOnEmpty(boolean failOnEmpty) {
        this.failOnEmpty = failOnEmpty;
    }

}
