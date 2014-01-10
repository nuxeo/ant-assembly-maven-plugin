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
 *     bstefanescu
 */
package org.nuxeo.build.ant.artifact;

import org.apache.tools.ant.types.DataType;
import org.nuxeo.build.maven.filter.NotFilter;
import org.nuxeo.build.maven.filter.OrFilter;

/**
 * TODO NXBT-258
 */
public class Excludes extends DataType {

    private OrFilter orFilter = new OrFilter();

    private NotFilter filter = new NotFilter(orFilter);

    public NotFilter getFilter() {
        return filter;
    }

    public void addArtifact(ArtifactPattern f) {
        orFilter.addFilter(f.getFilter());
    }
}
