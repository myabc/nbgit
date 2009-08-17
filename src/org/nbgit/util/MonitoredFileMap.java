/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2009 Imran M Yousuf <imyousuf@smartitengineering.com>
 * Copyright 2009 Jonas Fonseca <fonseca@diku.dk>
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file.
 *
 * This particular file is subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.nbgit.util;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;

public class MonitoredFileMap<T> {

    private final Map<String, T> map = Collections.synchronizedMap(new HashMap<String, T>());
    private final int delay;
    private final FileMonitor monitor;
    private FileMonitorTask monitorTask;

    private MonitoredFileMap(FileMonitor monitor, int delay) {
        this.monitor = monitor;
        this.delay = delay;
    }

    private MonitoredFileMap() {
        this.monitor = new FileMonitorImpl();
        this.delay = FileMonitorTask.DEFAULT_DELAY;
    }

    public static <T> MonitoredFileMap<T> create() {
        return new MonitoredFileMap<T>();
    }

    public static <T> MonitoredFileMap<T> create(FileMonitor monitor, int delay) {
        return new MonitoredFileMap<T>(monitor, delay);
    }

    public T get(File file) {
        synchronized (map) {
            return map.get(file.getPath());
        }
    }

    public void put(File file, T value) {
        final String key = file.getPath();
        synchronized (map) {
            map.put(key, value);
            if (monitorTask == null)
                monitorTask = new FileMonitorTask(delay);
        }
        monitor.monitor(file);
    }

    public T remove(File file) {
        synchronized (map) {
            T value = map.remove(file.getPath());
            if (map.isEmpty())
                monitorTask = null;
            return value;
        }
    }

    public int size() {
        synchronized (map) {
            return map.size();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("size=" + map.size());
        return builder.toString();
    }

    private class FileMonitorTask extends TimerTask {

        private final static int DEFAULT_DELAY = 10000;
        private final Timer timer = new Timer();
        private long lastModified;

        public FileMonitorTask(int delay) {
            timer.schedule(this, delay, delay);
        }

        @Override
        public void run() {
            Set<File> filesToRefresh = new HashSet<File>();
            Set<String> paths = map.keySet();

            synchronized (map) {
                if (monitorTask != this)
                    cancel();
                for (String path : paths) {
                    final File file = new File(path);
                    if (!file.exists() || file.lastModified() < lastModified) {
                        filesToRefresh.add(file);
                    }
                }
            }
            for (File file : filesToRefresh) {
                monitor.refresh(file);
            }
            lastModified = System.currentTimeMillis();
        }
    }

    public interface FileMonitor {

        void monitor(File file);

        void refresh(File file);
    }

    private class FileMonitorImpl extends FileChangeAdapter implements FileMonitor {

        public void monitor(File file) {
            FileObject fileObject = FileUtil.toFileObject(file);
            fileObject.addFileChangeListener(this);
        }

        public void refresh(File file) {
            FileUtil.toFileObject(file).refresh(true);
        }

        private void evictFile(FileObject file) {
            remove(FileUtil.toFile(file));
            file.removeFileChangeListener(this);
        }

        @Override
        public void fileChanged(FileEvent fe) {
            evictFile(fe.getFile());
        }

        @Override
        public void fileAttributeChanged(FileAttributeEvent fe) {
            evictFile(fe.getFile());
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            evictFile(fe.getFile());
        }

        @Override
        public void fileRenamed(FileRenameEvent fe) {
            evictFile(fe.getFile());
        }
    }

}
