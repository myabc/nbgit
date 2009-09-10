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
import org.netbeans.junit.NbTestCase;
import org.spearce.jgit.lib.FileBasedConfig;
import org.spearce.jgit.lib.Repository;
import org.spearce.jgit.util.SystemReader;

/**
 * Base test case for testing with repositories.
 */
public class RepositoryTestCase extends NbTestCase {

    private final File dataRoot = new File(getDataDir(), getClass().getCanonicalName());
    protected Repository repository;
    protected OutputLogger logger;
    protected ArrayList<String> loggerMessages;
    protected File dataDir;
    protected File gitDir;
    protected File workDir;

    public RepositoryTestCase(String name) {
        super(name);
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

        copyRepositoryFiles("default", repository);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
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
    }
}
