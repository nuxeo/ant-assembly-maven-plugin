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
 *     bstefanescu, slacoin, jcarsique
 */
package org.nuxeo.build.ant.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.Project;

import org.nuxeo.build.ant.AntClient;

/**
 * TODO NXBT-258
 */
public class AntProfileManager {

    protected Map<String, Profile> profiles = new HashMap<>();

    protected List<ProfileGroup> groups = new ArrayList<>();

    public AntProfileManager() {
    }

    public List<String> getActiveProfiles() {
        List<String> result = new ArrayList<>();
        for (Profile p : profiles.values()) {
            if (p.isActive()) {
                result.add(p.getName());
            }
        }
        return result;
    }

    public void addGroup(@SuppressWarnings("hiding") String[] profiles, String defaultProfile) {
        groups.add(new ProfileGroup(this, profiles, defaultProfile));
    }

    public void addProfile(Profile profile) {
        profiles.put(profile.getName(), profile);
    }

    public boolean isProfileActive(String profileName) {
        Profile profile = profiles.get(profileName);
        if (profile != null) {
            return profile.isActive();
        }
        return false;
    }

    public boolean isAnyProfileActive(List<String> profileNames) {
        for (String profileName : profileNames) {
            if (isProfileActive(profileName)) {
                return true;
            }
        }
        return false;
    }

    public void activateProfile(String profile, boolean isActive) {
        if (isActive) {
            AntClient.getInstance().log("Activating Ant profile: " + profile, Project.MSG_DEBUG);
        } else {
            AntClient.getInstance().log("Disabling Ant profile: " + profile, Project.MSG_DEBUG);
        }
        getOrCreateProfile(profile).setActive(isActive);
    }

    public Profile getOrCreateProfile(String profileName) {
        Profile profile = profiles.get(profileName);
        if (profile == null) {
            profile = new Profile(profileName);
            profiles.put(profileName, profile);
        }
        return profile;
    }

    public void activateProfiles(String config) {
        String[] ar = config.split("\\s*,\\s*");
        for (String key : ar) {
            if (key.startsWith("-")) {
                activateProfile(key.substring(1), false);
            } else if (key.startsWith("+")) {
                activateProfile(key.substring(1), true);
            } else {
                activateProfile(key, true);
            }
        }
    }

}
