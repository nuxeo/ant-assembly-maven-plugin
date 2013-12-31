/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.build.maven.filter;

import org.nuxeo.build.maven.AntBuildMojo;

/**
 * @since 2.0
 */
public abstract class AbstractFilter implements Filter {

    /**
     * For debug purpose: log the fact that the current filter has accepted or
     * refused the given 'id'
     *
     * @return return the same value as the given condition
     */
    protected boolean result(boolean condition, String id) {
        if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
            AntBuildMojo.getInstance().getLog().debug(
                    "Filtering - " + toString()
                            + (condition ? " accepted " : " refused ") + id);
        }
        return condition;
    }

}
