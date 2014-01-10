/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu, jcarsique, slacoin
 */
package org.nuxeo.build.maven.filter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.Project;
import org.nuxeo.build.ant.AntClient;
import org.nuxeo.build.maven.ArtifactDescriptor;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class CompositeFilter extends AbstractFilter {

    protected List<Filter> filters = new ArrayList<>();

    public CompositeFilter() {
    }

    public CompositeFilter(List<Filter> filters) {
        this.filters.addAll(filters);
    }

    public void addFilter(Filter filter) {
        filters.add(filter);
    }

    public void removeFilter(Filter filter) {
        filters.remove(filter);
    }

    public void addFilters(List<Filter> filtersToAdd) {
        this.filters.addAll(filtersToAdd);
    }

    public void addFilters(Filter... filtersToAdd) {
        this.filters.addAll(Arrays.asList(filtersToAdd));
    }

    public void removeFilters(@SuppressWarnings("hiding") List<Filter> filters) {
        this.filters.removeAll(filters);
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }

    public void addFiltersFromPattern(String pattern) {
        if (pattern != null) {
            addFiltersFromDescriptor(new ArtifactDescriptor(pattern));
        }
    }

    public void addFiltersFromDescriptor(ArtifactDescriptor ad) {
        if (ad.groupId != null && !ad.groupId.equals("*")) {
            addFilter(GroupIdFilter.class, ad.groupId);
        }
        if (ad.artifactId != null && !ad.artifactId.equals("*")) {
            addFilter(ArtifactIdFilter.class, ad.artifactId);
        }
        if (ad.version != null && !ad.version.equals("*")) {
            addFilter(VersionFilter.class, ad.version);
        }
        if (ad.type != null && !ad.type.equals("*")) {
            addFilter(TypeFilter.class, ad.type);
        }
        if (ad.classifier != null && !ad.classifier.equals("*")) {
            addFilter(ClassifierFilter.class, ad.classifier);
        }
        if (ad.scope != null && !ad.scope.equals("*")) {
            addFilter(ScopeFilter.class, ad.scope);
        }
    }

    public static Filter compact(CompositeFilter filter) {
        Filter result = filter;
        CompositeFilter cf = filter;
        if (cf != null && cf.filters.size() == 1) {
            result = cf.filters.get(0);
            if (result instanceof CompositeFilter) {
                result = compact((CompositeFilter) result);
            }
        }
        return result;
    }

    /**
     * Add a filter managing negation in pattern with '!'
     *
     * @param filterClass Filter class implementation to use
     * @param pattern Pattern given to Filter implementation
     */
    public void addFilter(Class<? extends Filter> filterClass, String pattern) {
        if (pattern == null) {
            return;
        }
        Constructor<? extends Filter> filterConstructor = null;
        try {
            filterConstructor = filterClass.getConstructor(String.class);
            if (pattern.startsWith("!")) {
                addFilter(new NotFilter(
                        filterConstructor.newInstance(pattern.substring(1))));
            } else {
                addFilter(filterConstructor.newInstance(pattern));
            }
        } catch (SecurityException e) {
            AntClient.getInstance().log(
                    "Couldn't get constructor for " + filterClass, e,
                    Project.MSG_ERR);
        } catch (NoSuchMethodException e) {
            AntClient.getInstance().log(
                    "Couldn't get constructor for " + filterClass, e,
                    Project.MSG_ERR);
        } catch (IllegalArgumentException e) {
            AntClient.getInstance().log(
                    "Couldn't call constructor for " + filterClass, e,
                    Project.MSG_ERR);
        } catch (InstantiationException e) {
            AntClient.getInstance().log(
                    "Couldn't call constructor for " + filterClass, e,
                    Project.MSG_ERR);
        } catch (IllegalAccessException e) {
            AntClient.getInstance().log(
                    "Couldn't call constructor for " + filterClass, e,
                    Project.MSG_ERR);
        } catch (InvocationTargetException e) {
            AntClient.getInstance().log(
                    "Couldn't call constructor for " + filterClass, e,
                    Project.MSG_ERR);
        }

    }
}
