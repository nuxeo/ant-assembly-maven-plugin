/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.build.ant;

import java.util.Hashtable;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.ExitStatusException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.UnknownElement;
import org.apache.tools.ant.taskdefs.Exit;
import org.apache.tools.ant.taskdefs.condition.Condition;

/**
 * Similar to {@link Exit} but allows to successfully end the build
 *
 * Exits the active build, giving an additional message
 * if available.
 *
 * The <code>if</code> and <code>unless</code> attributes make the
 * exit conditional -both probe for the named property being defined.
 * The <code>if</code> tests for the property being defined, the
 * <code>unless</code> for a property being undefined.
 *
 * If both attributes are set, then the task exits only if both tests
 * are true. i.e.
 *
 * <pre>
 * exit := defined(ifProperty) && !defined(unlessProperty)
 * </pre>
 *
 * A single nested<code>&lt;condition&gt;</code> element can be specified
 * instead of using <code>if</code>/<code>unless</code> (a combined
 * effect can be achieved using <code>isset</code> conditions).
 *
 * @ant.task name="exit" category="control"
 * @since 2.0.3
 */
public class ExitTask extends Exit {

    private final class False implements Condition {
        @Override
        public boolean eval() throws BuildException {
            return false;
        }
    }

    private False falseCondition = new False();

    @Override
    /**
     * Throw a <code>BuildException</code> to exit (fail) the build if {@code status!=0}.
     * If specified, evaluate conditions:
     * A single nested condition is accepted, but requires that the
     * <code>if</code>/<code>unless</code> attributes be omitted.
     * If the nested condition evaluates to true, or the
     * ifCondition is true or unlessCondition is false, the build will exit.
     *
     * The exit message is constructed from the text fields, from
     * the nested condition (if specified), or finally from
     * the if and unless parameters (if present).
     * @throws BuildException on error
     */
    public void execute() throws BuildException {
        String exitMsg = null;
        try {
            super.execute();
            // No raised error => condition failed, no exit
            return;
        } catch (ExitStatusException e) {
            if (e.getStatus() > 0) {
                throw e;
            } else {
                exitMsg = e.getMessage();
            }
        } catch (BuildException e) {
            exitMsg = e.getMessage();
        }
        exit(exitMsg);
    }

    protected void exit(String message) {
        Task doNothing = new Task() {
        };
        getProject().log(message, Project.MSG_INFO);
        Target owningTarget = getOwningTarget();
        Hashtable<String, Target> targets = getProject().getTargets();
        for (Target eachTarget : targets.values()) {
            eachTarget.setIf(falseCondition);
            if (eachTarget == owningTarget) {
                for (Task task : eachTarget.getTasks()) {
                    getProject().log("Invalidating tasks from " + eachTarget,
                            Project.MSG_DEBUG);
                    try {
                        if (task instanceof UnknownElement) {
                            UnknownElement currentTask = (UnknownElement) task;
                            if (currentTask.getRealThing() == this) {
                                continue;
                            }
                            getProject().log(
                                    "Invalidated " + currentTask.getTaskName(),
                                    Project.MSG_DEBUG);
                            currentTask.setRealThing(doNothing);
                        } else {
                            getProject().log(
                                    "Not an UnknownElement: "
                                            + task.getTaskName(),
                                    Project.MSG_DEBUG);
                        }
                    } catch (SecurityException | IllegalArgumentException e) {
                        throw new BuildException(e);
                    }
                }
            }
        }
    }

}
