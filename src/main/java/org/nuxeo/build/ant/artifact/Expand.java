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
 *     bstefanescu, jcarsique
 */
package org.nuxeo.build.ant.artifact;

/**
 * TODO NXBT-258
 */
public class Expand extends FiltersCollection {

    public int depth = Integer.MAX_VALUE;

    public void setDepth(String expand) {
        depth = readExpand(expand);
    }

    public static int readExpand(String expand) {
        int exp;
        if ("all".equals(expand) || "true".equals(expand)) {
            exp = Integer.MAX_VALUE;
        } else if ("false".equals(expand)) {
            exp = 0;
        } else {
            exp = Integer.parseInt(expand);
        }
        return exp;
    }

}
