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
package org.nbgit.junit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import org.nbgit.OutputLogger;
import org.netbeans.junit.Filter;
import org.netbeans.junit.NbTestCase;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.FileBasedConfig;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

/**
 * Base test case for testing with repositories.
 *
 * By default test with names ending in SupportsExecutable is omitted when
 * supportExecutable() returns false, in short when the filesystem does not
 * support the executable file mode. Also, files ending in .exe is set
 * executable when copied from the test data directory to make it easy to
 * restore this information which is lost when NetBeans copies files from
 * test/unit/data to build/test/unit/data.
 */
public class RepositoryTestCase extends NbTestCase {

    private final long TIME_INITIAL = 1112911993L;
    private final int TIME_INCREMENT = 60;
    private final int TIME_ZONE = -7 * 60;
    private final File dataRoot = new File(getDataDir(), getClass().getCanonicalName());
    private ArrayList<Filter.IncludeExclude> excludes = new ArrayList<Filter.IncludeExclude>();
    protected Repository repository;
    protected OutputLogger logger;
    protected ArrayList<String> loggerMessages;
    protected long time;
    protected File dataDir;
    protected File gitDir;
    protected File workDir;

    public RepositoryTestCase(String name) {
        super(name);
        excludeTestIf(!supportExecutable(), "No executable file mode", "*SupportsExecutable");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        clearWorkDir();
        SystemReader.setInstance(new MockSystemReader(new File(gitDir, "userconfig")));
        dataDir = new File(dataRoot, getName());
        workDir = new File(getWorkDir(), "repo");
        gitDir = new File(workDir, ".git");
        repository = new Repository(gitDir);
        repository.create();
        loggerMessages = new ArrayList<String>();
        logger = MockOutputLogger.create(loggerMessages);
        time = TIME_INITIAL;

        copyRepositoryFiles("default", repository);
        repository.getConfig().load();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    protected int getTimeZone() {
        return TIME_ZONE;
    }

    protected long getTime() {
        return time * 1000;
    }

    protected long timeTick() {
        time += TIME_INCREMENT;
        return getTime();
    }

    protected Collection<File> toFiles(File base, String... paths) {
        Collection<File> files = new ArrayList<File>(paths.length);
        for (int i = 0; i < paths.length; i++)
            files.add(toFile(base, paths[i]));
        return files;
    }

    /**
     * Build an abstract File from a base file and a relative path, e.g.:
     * <pre>
     *  toFile(repository.getWorkDir(), "a/b")
     * </pre>.
     */
    protected File toFile(File base, String path) {
        return new File(base, path.replace('/', File.separatorChar));
    }

    /**
     * Build an abstract File using workDir as the base.
     * @see RepositoryTestCase#toFile(java.io.File, java.lang.String)
     */
    protected File toWorkDirFile(String path) {
        return toFile(workDir, path);
    }

    /**
     * Build an abstract File using gitDir as the base.
     * @see RepositoryTestCase#toFile(java.io.File, java.lang.String)
     */
    protected File toGitDirFile(String path) {
        return toFile(gitDir, path);
    }

    protected boolean supportExecutable() {
        return FS.INSTANCE.supportsExecute();
    }

    protected boolean isExecutable(File file) {
        return FS.INSTANCE.canExecute(file);
    }

    protected boolean setExecutable(File file, boolean executable) {
        return FS.INSTANCE.setExecute(file, executable);
    }

    protected void excludeTestIf(boolean condition, String reason, String... tests) {
        if (!condition)
            return;
        for (String test : tests)
            excludes.add(new Filter.IncludeExclude(test, reason));
        Filter filter = new Filter();
        filter.setExcludes(excludes.toArray(new Filter.IncludeExclude[excludes.size()]));
        setFilter(filter);
    }

    protected void compareIndexFiles() throws Exception {
        refDirCache(DirCache.read(repository));
        compareReferenceFiles();
    }

    private void refDirCache(DirCache index) {
        for (int i = 0; i < index.getEntryCount(); i++)
            refDirCacheEntry(index.getEntry(i));
    }

    private void refDirCacheEntry(DirCacheEntry entry) {
        StringBuilder builder = new StringBuilder();
        builder.append(entry.getFileMode().toString()).
                append(" ").append(entry.getObjectId().name()).
                append(" ").append(entry.getStage()).
                append("\t").append(entry.getPathString());
        ref(builder.toString());
    }

    protected void copyRepositoryFiles(String name, Repository repo) throws IOException {
        copyDataFile(dataRoot, name + ".git", repo.getDirectory());
        copyDataFile(dataRoot, name + ".workdir", repo.getWorkDir());
        copyDataFile(dataDir, name + ".git", repo.getDirectory());
        copyDataFile(dataDir, name + ".workdir", repo.getWorkDir());
    }

    protected void copyDataFile(File dir, String name, File dst) throws IOException {
        File src = new File(dir, name);
        if (src.exists())
            copyFile(src, dst);
    }

    protected void copyFile(File src, File dst) throws IOException {
        if (src.isDirectory()) {
            dst.mkdir();
            for (File file : src.listFiles()) {
                File fileDst = new File(dst, file.getName());
                copyFile(file, fileDst);
            }
        } else {
            copyStream(new FileInputStream(src), dst);
            // Hack to set the execute bit, lost when NetBeans copies the files.
            if (isExecutable(src) || src.getName().endsWith(".exe"))
                setExecutable(dst, true);
        }
    }

    private void copyStream(InputStream in, File dst) throws IOException {
        FileOutputStream out = new FileOutputStream(dst);
        try {
            byte[] buf = new byte[4096];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
        } finally {
            out.close();
        }
    }

    private static class MockSystemReader extends SystemReader {

        private final File userConfigFile;

        public MockSystemReader(File userConfigFile) {
            this.userConfigFile = userConfigFile;
        }

        @Override
        public String getHostname() {
            return "localhost";
        }

        @Override
        public String getenv(String name) {
            return null;
        }

        @Override
        public String getProperty(String arg0) {
            return null;
        }

        @Override
        public FileBasedConfig openUserConfig() {
            return new FileBasedConfig(userConfigFile);
        }

        @Override
        public long getCurrentTime() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getTimezone(long when) {
            // TODO Auto-generated method stub
            return 0;
        }
    }
}
