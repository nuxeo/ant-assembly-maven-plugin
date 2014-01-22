/*******************************************************************************
 * Copyright (c) 2010, 2013-2014 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype, Inc. - initial API and implementation
 *    Julien Carsique
 *******************************************************************************/
package org.nuxeo.build.maven.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.graph.Dependency;

/**
 * A dependency selector that filters transitive dependencies based on their
 * scope. Direct dependencies are always
 * included regardless of their scope. <em>Note:</em> This filter does not
 * assume any relationships between the scopes.
 * In particular, the filter is not aware of scopes that logically include other
 * scopes.
 *
 * Copy from org.eclipse.aether.util.graph.selector.ScopeDependencySelector,
 * change is in {@link #deriveChildSelector(DependencyCollectionContext)}.
 *
 * @see Dependency#getScope()
 */
public final class ScopeDependencySelector implements DependencySelector {

    private final boolean transitive;

    private final Collection<String> included;

    private final Collection<String> excluded;

    /**
     * Creates a new selector using the specified includes and excludes.
     *
     * @param included The set of scopes to include, may be {@code null} or
     *            empty to include any scope.
     * @param excluded The set of scopes to exclude, may be {@code null} or
     *            empty to exclude no scope.
     */
    public ScopeDependencySelector(Collection<String> included,
            Collection<String> excluded) {
        transitive = false;
        this.included = clone(included);
        this.excluded = clone(excluded);
    }

    private static Collection<String> clone(Collection<String> scopes) {
        Collection<String> copy;
        if (scopes == null || scopes.isEmpty()) {
            // checking for null is faster than isEmpty()
            copy = null;
        } else {
            copy = new HashSet<>(scopes);
            if (copy.size() <= 2) {
                // contains() is faster for smallish array (sorted for
                // equals()!)
                copy = new ArrayList<>(new TreeSet<>(copy));
            }
        }
        return copy;
    }

    /**
     * Creates a new selector using the specified excludes.
     *
     * @param excluded The set of scopes to exclude, may be {@code null} or
     *            empty to exclude no scope.
     */
    public ScopeDependencySelector(String... excluded) {
        this(null, (excluded != null) ? Arrays.asList(excluded) : null);
    }

    private ScopeDependencySelector(boolean transitive,
            Collection<String> included, Collection<String> excluded) {
        this.transitive = transitive;
        this.included = included;
        this.excluded = excluded;
    }

    @Override
    public boolean selectDependency(Dependency dependency) {
        if (!transitive) {
            return true;
        }

        String scope = dependency.getScope();
        return (included == null || included.contains(scope))
                && (excluded == null || !excluded.contains(scope));
    }

    @Override
    public DependencySelector deriveChildSelector(
            DependencyCollectionContext context) {
        if (this.transitive || context.getDependency() == null) {
            return this;
        }

        // Here, we consider the deriveChildSelector as transitive only if the
        // dependency related to the current selector has a defined scope.
        return new ScopeDependencySelector(
                context.getDependency().getScope().length() > 0, included,
                excluded);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (null == obj || !getClass().equals(obj.getClass())) {
            return false;
        }

        ScopeDependencySelector that = (ScopeDependencySelector) obj;
        return transitive == that.transitive && eq(included, that.included)
                && eq(excluded, that.excluded);
    }

    private static <T> boolean eq(T o1, T o2) {
        return (o1 != null) ? o1.equals(o2) : o2 == null;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (transitive ? 1 : 0);
        hash = hash * 31 + (included != null ? included.hashCode() : 0);
        hash = hash * 31 + (excluded != null ? excluded.hashCode() : 0);
        return hash;
    }

}
