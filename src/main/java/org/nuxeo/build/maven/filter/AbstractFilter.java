/*
 * (C) Copyright 2013-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.tools.ant.Project;
import org.eclipse.aether.graph.DependencyNode;

import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.AntBuildMojo;

/**
 * @since 2.0
 */
public abstract class AbstractFilter implements Filter {

    /**
     * For debug purpose: log the fact that the current filter has accepted or refused the given 'id'
     *
     * @return return the same value as the given condition
     */
    protected boolean result(boolean condition, String id) {
        if (AntBuildMojo.getInstance().getLog().isDebugEnabled()) {
            AntClient.getInstance().log(
                    String.format("Filtering - %s => %s %s", toString(), (condition ? "accepted" : "refused"), id),
                    Project.MSG_DEBUG);
        }
        return condition;
    }

    /**
     * For debug purpose: log the fact that the current filter is about to evaluate the given candidate
     *
     * @param candidate Object about to be accepted or rejected; an {@link Artifact} or a {@link DependencyNode}
     * @param args if not empty, {@code args[0]} is used as "a format string" and the optional following values are used
     *            as arguments referenced by the format specifiers in the format string. {@code args} here contains both
     *            {@code format and args} for {@link String#format(String, Object...)}
     * @since 2.0.6
     */
    protected void beforeAccept(Object candidate, Object... args) {
        String msg = String.format("Filtering - %s on %s", this, candidate);
        if (args != null && args.length > 0) {
            msg += String.format(args[0].toString(), Arrays.copyOfRange(args, 1, args.length));
        }
        AntClient.getInstance().log(msg, Project.MSG_DEBUG);
    }

    /**
     * {@inheritDoc}<br>
     * For debug purpose, implementing method should call {@link #beforeAccept(Object, Object...)} at beginning and
     * {@link #result(boolean, String)} to wrap the return statement.
     *
     * @see #beforeAccept(Object, Object...)
     * @see #result(boolean, String)
     */
    @Override
    public abstract boolean accept(DependencyNode node, List<DependencyNode> parents);

    /**
     * {@inheritDoc}<br>
     * For debug purpose, implementing method should call {@link #beforeAccept(Object, Object...)} at beginning and
     * {@link #result(boolean, String)} to wrap the return statement.
     *
     * @see #beforeAccept(Object, Object...)
     * @see #result(boolean, String)
     */
    @Override
    public abstract boolean accept(Artifact artifact);

}
