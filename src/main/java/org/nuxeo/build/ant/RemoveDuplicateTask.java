/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.build.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Remove duplicate jars (with different versions) and preserve only the latest
 * version. The pattern to detect duplicates is: (.*)-([0-9]+.*).jar. The
 * versions are compared and the lower versions are removed.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RemoveDuplicateTask extends Task {

    static final Pattern PATTERN = Pattern.compile("-([0-9]+.*)\\.jar");

    protected File dir;

    public void setDir(File dir) {
        this.dir = dir;
    }

    @Override
    public void execute() throws BuildException {
        if (dir == null) {
            throw new BuildException("dir attribute is required");
        }
        String[] names = dir.list();
        if (names == null) {
            return;
        }
        HashMap<String, List<Entry>> map = new HashMap<String, List<Entry>>();
        for (String name : names) {
            Matcher m = PATTERN.matcher(name);
            if (m.find()) {
                String key = name.substring(0, m.start());
                String v = m.group(1);
                Entry entry = new Entry(new File(dir, name), Version.parse(v));
                List<Entry> list = map.get(key);
                if (list == null) {
                    list = new ArrayList<RemoveDuplicateTask.Entry>();
                    map.put(key, list);
                }
                list.add(entry);
            }
        }
        for (List<Entry> list : map.values()) {
            removeAllButLatest(list);
        }
    }

    public static Entry getLatest(List<Entry> list) {
        if (list.isEmpty()) {
            return null;
        }
        Iterator<Entry> it = list.iterator();
        if (!it.hasNext()) {
            return null;
        }
        Entry latest = it.next();
        while (it.hasNext()) {
            Entry p = it.next();
            if (p.version.greaterThan(latest.version)) {
                latest = p;
            }
        }
        return latest;
    }

    public void removeAllButLatest(List<Entry> list) {
        Entry latest = getLatest(list);
        StringBuilder buf = new StringBuilder();
        for (Entry p : list) {
            if (p != latest) {
                buf.append(p.file.getName()).append(" ");
                p.file.delete();
            }
        }
        if (buf.length() > 0) {
            log("removed duplicates: " + buf.toString() + "; preserved: "
                    + latest.file.getName(), Project.MSG_INFO);
        }
    }

    static class Entry {
        protected File file;

        protected Version version;

        Entry(File file, Version version) {
            this.file = file;
            this.version = version;
        }
    }

}
