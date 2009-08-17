/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.nbgit.junit.RepositoryTestCase;

public class MonitoredFileMapTest extends RepositoryTestCase {

    private final MockFileMonitor fileMonitor = new MockFileMonitor();
    private MonitoredFileMap<Integer> map;

    public MonitoredFileMapTest() {
        super(MonitoredFileMapTest.class.getSimpleName());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        map = MonitoredFileMap.create(fileMonitor, 1000);
        fileMonitor.monitoredFiles.clear();
        fileMonitor.refreshedFiles.clear();
    }

    public void testInitialization() throws IOException {
        assertEquals("Initial size is zero", 0, map.size());
        assertNull("Initial state is empty", map.get(workDir));
        assertNull("Remove non-existing OK", map.remove(workDir));
        assertEquals("Nothing monitored", 0, fileMonitor.monitoredFiles.size());
        assertEquals("Nothing refreshed", 0, fileMonitor.refreshedFiles.size());
    }

    public void testPutAndGet() throws IOException {
        File fileA = new File(workDir, "a");
        Integer valueA = Integer.valueOf(42);
        fileA.createNewFile();
        map.put(fileA, valueA);
        assertEquals(valueA, map.get(fileA));
        assertTrue(fileMonitor.monitoredFiles.contains(fileA));

        assertEquals(valueA, map.remove(fileA));
        assertEquals(null, map.get(fileA));
        assertEquals(null, map.remove(fileA));
        assertEquals(0, map.size());
    }

    public void testRefreshingAfterDelete() throws IOException {
        File fileA = new File(workDir, "a");
        Integer valueA = Integer.valueOf(42);
        fileA.createNewFile();
        map.put(fileA, valueA);

        assertEquals(1, map.size());
        assertEquals(1, fileMonitor.monitoredFiles.size());

        fileA.delete();

        synchronized (fileMonitor) {
            try {
                fileMonitor.wait(5000);
            } catch (InterruptedException ex) {
            }
        }
        assertEquals(1, fileMonitor.refreshedFiles.size());
    }

    public void testRefreshingAfterModification() throws IOException {
        File fileA = new File(workDir, "a");
        Integer valueA = Integer.valueOf(42);
        FileWriter writer = new FileWriter(fileA);
        writer.append("initial");
        writer.flush();
        map.put(fileA, valueA);

        assertEquals(1, map.size());
        assertEquals(1, fileMonitor.monitoredFiles.size());

        writer.append("more");
        writer.close();

        synchronized (fileMonitor) {
            try {
                fileMonitor.wait(5000);
            } catch (InterruptedException ex) {
            }
        }
        assertEquals(1, fileMonitor.refreshedFiles.size());
    }

    class MockFileMonitor implements MonitoredFileMap.FileMonitor {

        private final Set<File> monitoredFiles = new HashSet<File>();
        private final Set<File> refreshedFiles = new HashSet<File>();

        public void monitor(File file) {
            monitoredFiles.add(file);
        }

        public void refresh(File file) {
            refreshedFiles.add(file);
            synchronized (this) {
                notifyAll();
            }
        }
    }

}
