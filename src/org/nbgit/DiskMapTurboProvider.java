/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 * Portions Copyright 2008 Alexander Coles (Ikonoklastik Productions).
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
package org.nbgit;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.netbeans.modules.turbo.TurboProvider;
import org.openide.filesystems.FileUtil;

/**
 * Storage of file attributes with shortcut to retrieve all stored values.
 *
 * @author Maros Sandor
 */
class DiskMapTurboProvider implements TurboProvider {

    static final String ATTR_STATUS_MAP = "git.STATUS_MAP";  // NOI18N

    private static final int STATUS_VALUABLE = StatusInfo.STATUS_MANAGED & ~StatusInfo.STATUS_VERSIONED_UPTODATE;
    private static final String CACHE_DIRECTORY = "gitcache"; // NOI18N

    private File cacheStore;
    private int                             storeSerial;

    private int                             cachedStoreSerial = -1;
    private Map<File, StatusInfo> cachedValues;

    DiskMapTurboProvider() {
        initCacheStore();
    }

    synchronized Map<File, StatusInfo>  getAllModifiedValues() {
        if (cachedStoreSerial != storeSerial || cachedValues == null) {
            cachedValues = new HashMap<File, StatusInfo>();
            File [] files = cacheStore.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.getName().endsWith(".bin") == false) { // NOI18N
                    // on windows list returns already deleted .new files
                    continue;
                }
                DataInputStream dis = null;
                try {
                    int retry = 0;
                    while (true) {
                        try {
                            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
                            break;
                        } catch (IOException ioex) {
                            retry++;
                            if (retry > 7) {
                                throw ioex;
                            }
                            Thread.sleep(retry * 30);
                        }
                    }

                    for (;;) {
                        int pathLen = dis.readInt();
                        dis.readInt();
                        String path = readChars(dis, pathLen);
                        Map<File, StatusInfo> value = readValue(dis, path);
                        for (File f : value.keySet()) {
                            StatusInfo info = value.get(f);
                            if ((info.getStatus() & DiskMapTurboProvider.STATUS_VALUABLE) != 0) {
                                cachedValues.put(f, info);
                            }
                        }
                    }
                } catch (EOFException e) {
                    // reached EOF, no entry for this key
                } catch (Exception e) {
                    Git.LOG.log(Level.WARNING, null, e);
                } finally {
                    if (dis != null) try { dis.close(); } catch (IOException e) {}
                }
            }
            cachedStoreSerial = storeSerial;
            cachedValues = Collections.unmodifiableMap(cachedValues);
        }
        return cachedValues;
    }

    public boolean recognizesAttribute(String name) {
        return DiskMapTurboProvider.ATTR_STATUS_MAP.equals(name);
    }

    public boolean recognizesEntity(Object key) {
        return key instanceof File;
    }

    public synchronized Object readEntry(Object key, String name, MemoryCache memoryCache) {
        assert key instanceof File;
        assert name != null;

        boolean readFailed = false;
        File dir = (File) key;
        File store = getStore(dir);
        if (!store.isFile()) {
            return null;
        }

        String dirPath = dir.getAbsolutePath();
        int dirPathLen = dirPath.length();
        DataInputStream dis = null;
        try {

            int retry = 0;
            while (true) {
                try {
                    dis = new DataInputStream(new BufferedInputStream(new FileInputStream(store)));
                    break;
                } catch (IOException ioex) {
                    retry++;
                    if (retry > 7) {
                        throw ioex;
                    }
                    Thread.sleep(retry * 30);
                }
            }

            for (;;) {
                int pathLen = dis.readInt();
                int mapLen = dis.readInt();
                if (pathLen != dirPathLen) {
                    skip(dis, pathLen * 2 + mapLen);
                } else {
                    String path = readChars(dis, pathLen);
                    if (dirPath.equals(path)) {
                        return readValue(dis, path);
                    } else {
                        skip(dis, mapLen);
                    }
                }
            }
        } catch (EOFException e) {
            // reached EOF, no entry for this key
        } catch (Exception e) {
            Git.LOG.log(Level.INFO, null, e);
            readFailed = true;
        } finally {
            if (dis != null) try { dis.close(); } catch (IOException e) {}
        }
        if (readFailed) store.delete();
        return null;
    }

    public synchronized boolean writeEntry(Object key, String name, Object value) {
        assert key instanceof File;
        assert name != null;

        if (value != null) {
            if (!(value instanceof Map)) return false;
            if (!isValuable(value)) value = null;
        }

        File dir = (File) key;
        String dirPath = dir.getAbsolutePath();
        int dirPathLen = dirPath.length();
        File store = getStore(dir);

        if (value == null && !store.exists()) return true;

        File storeNew = new File(store.getParentFile(), store.getName() + ".new"); // NOI18N

        DataOutputStream oos = null;
        DataInputStream dis = null;
        try {
            oos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(storeNew)));
            if (value != null) {
                writeEntry(oos, dirPath, value);
            }
            if (store.exists()) {
                int retry = 0;
                while (true) {
                    try {
                        dis = new DataInputStream(new BufferedInputStream(new FileInputStream(store)));
                        break;
                    } catch (IOException ioex) {
                        retry++;
                        if (retry > 7) {
                            throw ioex;
                        }
                        Thread.sleep(retry * 30);
                    }
                }

                for (;;) {
                    int pathLen;
                    try {
                        pathLen = dis.readInt();
                    } catch (EOFException e) {
                        break;
                    }
                    int mapLen = dis.readInt();
                    if (pathLen == dirPathLen) {
                        String path = readChars(dis, pathLen);
                        if (dirPath.equals(path)) {
                            skip(dis, mapLen);
                        } else {
                            oos.writeInt(pathLen);
                            oos.writeInt(mapLen);
                            oos.writeChars(path);
                            DiskMapTurboProvider.copyStreams(oos, dis, mapLen);
                        }
                    } else {
                        oos.writeInt(pathLen);
                        oos.writeInt(mapLen);
                        DiskMapTurboProvider.copyStreams(oos, dis, mapLen + pathLen * 2);
                    }
                }
            }
        } catch (Exception e) {
            Git.LOG.log(Level.WARNING, "writeEntry(): Copy: {0} to: {1}", new Object[] {store.getAbsolutePath(), storeNew.getAbsolutePath()}); //NOI18N
            return true;
        } finally {
            if (oos != null) try { oos.close(); } catch (IOException e) {}
            if (dis != null) try { dis.close(); } catch (IOException e) {}
        }
        storeSerial++;
        store.delete();
        storeNew.renameTo(store);
        return true;
    }

    private void skip(InputStream is, long len) throws IOException {
        while (len > 0) {
            long n = is.skip(len);
            if (n < 0) throw new EOFException("Missing " + len + " bytes.");  // NOI18N
            len -= n;
        }
    }

    private String readChars(DataInputStream dis, int len) throws IOException {
        StringBuffer sb = new StringBuffer(len);
        while (len-- > 0) {
            sb.append(dis.readChar());
        }
        return sb.toString();
    }

    private Map<File, StatusInfo> readValue(DataInputStream dis, String dirPath) throws IOException {
        Map<File, StatusInfo> map = new HashMap<File, StatusInfo>();
        int len = dis.readInt();
        while (len-- > 0) {
            int nameLen = dis.readInt();
            String name = readChars(dis, nameLen);
            File file = new File(dirPath, name);
            int status = dis.readInt();
            StatusInfo info = new StatusInfo(status & 65535, status > 65535);
            map.put(file, info);
        }
        return map;
    }

    private void writeEntry(DataOutputStream dos, String dirPath, Object value) throws IOException {

        Map map = (Map) value;
        Set set = map.keySet();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(set.size() * 50);
        DataOutputStream temp = new DataOutputStream(baos);

        temp.writeInt(set.size());
        for (Iterator i = set.iterator(); i.hasNext();) {
            File file = (File) i.next();
            StatusInfo info = (StatusInfo) map.get(file);
            temp.writeInt(file.getName().length());
            temp.writeChars(file.getName());
            temp.writeInt(info.getStatus() + (info.isDirectory() ? 65536 : 0));
        }
        temp.close();
        byte [] valueBytes = baos.toByteArray();

        dos.writeInt(dirPath.length());
        dos.writeInt(valueBytes.length);
        dos.writeChars(dirPath);
        dos.write(valueBytes);
    }

    private boolean isValuable(Object value) {
        Map map = (Map) value;
        for (Iterator i = map.values().iterator(); i.hasNext();) {
            StatusInfo info = (StatusInfo) i.next();
            if ((info.getStatus() & DiskMapTurboProvider.STATUS_VALUABLE) != 0) return true;
        }
        return false;
    }

    private File getStore(File dir) {
        String dirPath = dir.getAbsolutePath();
        int dirHash = dirPath.hashCode();
        return new File(cacheStore, Integer.toString(dirHash % 173 + 172) + ".bin"); // NOI18N
    }

    private void initCacheStore() {
        String userDir = System.getProperty("netbeans.user"); // NOI18N
        File cacheRoot;
        if (userDir != null) {
            cacheRoot = new File(new File(userDir, "var"), "cache"); // NOI18N
        } else {
            cacheRoot = FileUtil.toFile(FileUtil.getConfigRoot());
        }
        cacheStore = new File(cacheRoot, DiskMapTurboProvider.CACHE_DIRECTORY); // NOI18N
        cacheStore.mkdirs();
    }

    private static void copyStreams(OutputStream out, InputStream in, int len) throws IOException {
        byte [] buffer = new byte[4096];
        for (;;) {
            int n = (len <= 4096) ? len : 4096;
            n = in.read(buffer, 0, n);
            if (n < 0) throw new EOFException("Missing " + len + " bytes.");  // NOI18N
            out.write(buffer, 0, n);
            if ((len -= n) == 0) break;
        }
        out.flush();
    }
}
