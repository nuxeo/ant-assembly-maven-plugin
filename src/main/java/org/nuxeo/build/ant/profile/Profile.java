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
 *     bstefanescu, slacoin
 */
package org.nuxeo.build.ant.profile;

/**
 * TODO NXBT-258
 */
public class Profile {

    protected ProfileGroup group;

    protected String name;

    private boolean isActive;

    public Profile(String name) {
        this.name = name;
    }

    public Profile(ProfileGroup group, String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        if (group != null) {
            group.activateProfile(this, isActive);
        } else {
            _setActive(isActive);
        }
    }

    public String getName() {
        return name;
    }

    final void _setActive(@SuppressWarnings("hiding") boolean isActive) {
        this.isActive = isActive;
    }

}
