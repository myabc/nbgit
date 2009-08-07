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
package org.nbgit.util.exclude;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import org.nbgit.StatusCache;
import org.nbgit.Git;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.queries.SharabilityQuery;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.spearce.jgit.lib.Repository;

/**
 * Provides support for .gitignore files.
 * 
 * To keep the querying interface fast, a cache of patterns are maintained.
 */
public class Excludes {

    private Excludes() {
    }

    // IGNORE SUPPORT GIT: following file patterns are added to {Git repos}/.gitignore and Git will ignore any files
    // that match these patterns, reporting "I"status for them // NOI18N
    //private static final String [] GIT_IGNORE_FILES = { ".orig", "\\.orig\\..*$", "\\.chg\\..*$", ".rej", "\\.conflict\\~$"}; // NOI18N
    private static final String[] GIT_IGNORE_FILES = {".orig"};
    private static final String GIT_IGNORE_ORIG_FILES = "\\.orig$"; // NOI18N
    private static final String GIT_IGNORE_ORIG_ANY_FILES = "\\.orig\\..*$"; // NOI18N
    private static final String GIT_IGNORE_CHG_ANY_FILES = "\\.chg\\..*$"; // NOI18N
    private static final String GIT_IGNORE_REJ_ANY_FILES = "\\.rej$"; // NOI18N
    private static final String GIT_IGNORE_CONFLICT_ANY_FILES = "\\.conflict\\~$"; // NOI18N
    private static final String FILENAME_GITIGNORE = ".gitignore"; // NOI18N
    private static HashMap<String, List<PathPattern>> ignorePatterns;

    private static void resetIgnorePatterns(File file) {
        if (ignorePatterns == null) {
            return;
        }
        String key = file.getAbsolutePath();
        ignorePatterns.remove(key);
    }

    private static List<PathPattern> getIgnorePatterns(File file) {
        if (ignorePatterns == null) {
            ignorePatterns = new HashMap<String, List<PathPattern>>();
        }
        String key = file.getAbsolutePath();
        List<PathPattern> patterns = ignorePatterns.get(key);
        if (patterns == null) {
            patterns = readIgnorePatterns(file);
			if (!patterns.isEmpty())
				ignorePatterns.put(key, patterns);
        }
        return patterns;
    }

    /**
     * Check if a file should can be shared between projects as defined by
     * NetBeans SharabilityQuery interface.
     *
     * @param file to check
     * @return true, if the file can be shared.
     */
    public static boolean isSharable(File file) {
        return SharabilityQuery.getSharability(file) != SharabilityQuery.NOT_SHARABLE;
    }

    /**
     * Checks to see if a file is ignored.
     *
     * @param file to check
     * @return true if the file is ignored
     */
    public static boolean isIgnored(File file) {
        return isIgnored(file, true);
    }

    public static boolean isIgnored(File file, boolean checkSharability) {
        // FIXME Disabled for now.
        /*
        if (true != false) {
            return false;
        }
         */
        if (file == null) {
            return false;
        }

        File topFile = Git.getInstance().getTopmostManagedParent(file);
        // We assume that the toplevel directory should not be ignored.
        if (topFile == null || topFile.equals(file)) {
            return false;        // We assume that the Project should not be ignored.
        }
        if (file.isDirectory()) {
            ProjectManager projectManager = ProjectManager.getDefault();
            if (projectManager.isProject(FileUtil.toFileObject(file))) {
                return false;
            }
        }

        Repository repo = Git.getInstance().getRepository(topFile);
		File workDir = repo.getWorkDir();
		String absoluteRootPath = workDir.getAbsolutePath();
		String path = Repository.stripWorkDir(workDir, file);
        PathPattern pattern;
        for (File i = file.getParentFile();
             i.getAbsolutePath().startsWith(absoluteRootPath);
             i = i.getParentFile()) {
            File ignoreFile = new File(i, FILENAME_GITIGNORE);
            if (!ignoreFile.exists())
                continue;
            String relPath = stripWorkDir(workDir, i);
            pattern = matchIgnorePatterns(path, file.isDirectory(), ignoreFile, relPath);
            if (pattern != null)
                return pattern.isExclude();
		}

        File repoExcludeFile = new File(repo.getDirectory(), "info/exclude");
        pattern = matchIgnorePatterns(path, file.isDirectory(), repoExcludeFile, "/");
        if (pattern != null)
            return pattern.isExclude();

        String userExcludePath = repo.getConfig().getString("core", null, "excludesfile");
        if (userExcludePath != null && userExcludePath.length() > 0) {
            File excludeFile = new File(userExcludePath);
            pattern = matchIgnorePatterns(path, file.isDirectory(), excludeFile, "/");
            if (pattern != null)
                return pattern.isExclude();
        }

        if (checkSharability && !isSharable(file)) {
            return true;
        }
        return false;
    }

    private static String stripWorkDir(File wd, File f) {
        int skip = f.getPath().length() > wd.getPath().length() ? 1 : 0;
		String relName = f.getPath().substring(wd.getPath().length() + skip);
        if (File.separatorChar != '/')
    		relName = relName.replace(File.separatorChar, '/');
		return relName;
	}

    private static PathPattern matchIgnorePatterns(String path, boolean isDir,
            File excludeFile, String relPath) {
        for (PathPattern pattern : getIgnorePatterns(excludeFile)) {
            if (pattern.matches(path, isDir, relPath)) {
                return pattern;
            }
        }
        return null;
    }

    /**
     * Create a .gitignore file in the repository in which the given
     * file belongs. The .gitignore file will contain the ignore
     * patterns specified by {@link #GIT_IGNORE_FILES}.
     *
     * @param path to repository to place .gitignore file
     */
    /*
    public static void createIgnored(File path, String ignorePatterns) {
        if (path == null) {
            return;
        }
        BufferedWriter fileWriter = null;
        Git git = Git.getInstance();
        File root = git.getTopmostManagedParent(path);
        if (root == null) {
            return;
        }
        File ignore = new File(root, FILENAME_GITIGNORE);

        try {
            if (!ignore.exists()) {
                fileWriter = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(ignore)));
                for (String name : GIT_IGNORE_FILES) {
                    fileWriter.write(name + "\n"); // NOI18N
                }
            } else {
                addToExistingIgnoredFile(ignore);
            }
        } catch (IOException ex) {
            Git.LOG.log(Level.FINE, "createIgnored(): File {0} - {1}", // NOI18N
                    new Object[]{ignore.getAbsolutePath(), ex.toString()});
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
                git.getStatusCache().refresh(ignore, StatusCache.REPOSITORY_STATUS_UNKNOWN);
            } catch (IOException ex) {
                Git.LOG.log(Level.FINE, "createIgnored(): File {0} - {1}", // NOI18N
                        new Object[]{ignore.getAbsolutePath(), ex.toString()});
            }
        }
    }
    private static int GIT_NUM_PATTERNS_TO_CHECK = 5;

    private static void addToExistingIgnoredFile(File gitIgnoreFile) {
        if (gitIgnoreFile == null || !gitIgnoreFile.exists() || !gitIgnoreFile.canWrite()) {
            return;
        }
        File tempFile = null;
        BufferedReader br = null;
        PrintWriter pw = null;
        boolean bOrigAnyPresent = false;
        boolean bOrigPresent = false;
        boolean bChgAnyPresent = false;
        boolean bRejAnyPresent = false;
        boolean bConflictAnyPresent = false;

        // If new patterns are added to GIT_IGNORE_FILES, following code needs to
        // check for these new patterns
        assert (GIT_IGNORE_FILES.length == GIT_NUM_PATTERNS_TO_CHECK);

        try {
            tempFile = new File(gitIgnoreFile.getAbsolutePath() + ".tmp"); // NOI18N
            if (tempFile == null) {
                return;
            }
            br = new BufferedReader(new FileReader(gitIgnoreFile));
            pw = new PrintWriter(new FileWriter(tempFile));

            String line = null;
            while ((line = br.readLine()) != null) {
                if (!bOrigAnyPresent && line.equals(GIT_IGNORE_ORIG_ANY_FILES)) {
                    bOrigAnyPresent = true;
                } else if (!bOrigPresent && line.equals(GIT_IGNORE_ORIG_FILES)) {
                    bOrigPresent = true;
                } else if (!bChgAnyPresent && line.equals(GIT_IGNORE_CHG_ANY_FILES)) {
                    bChgAnyPresent = true;
                } else if (!bRejAnyPresent && line.equals(GIT_IGNORE_REJ_ANY_FILES)) {
                    bRejAnyPresent = true;
                } else if (!bConflictAnyPresent && line.equals(GIT_IGNORE_CONFLICT_ANY_FILES)) {
                    bConflictAnyPresent = true;
                }
                pw.println(line);
                pw.flush();
            }
            // If not found add as required
            if (!bOrigAnyPresent) {
                pw.println(GIT_IGNORE_ORIG_ANY_FILES);
                pw.flush();
            }
            if (!bOrigPresent) {
                pw.println(GIT_IGNORE_ORIG_FILES);
                pw.flush();
            }
            if (!bChgAnyPresent) {
                pw.println(GIT_IGNORE_CHG_ANY_FILES);
                pw.flush();
            }
            if (!bRejAnyPresent) {
                pw.println(GIT_IGNORE_REJ_ANY_FILES);
                pw.flush();
            }
            if (!bConflictAnyPresent) {
                pw.println(GIT_IGNORE_CONFLICT_ANY_FILES);
                pw.flush();
            }

        } catch (IOException ex) {
            // Ignore
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }
                if (br != null) {
                    br.close();
                }
                boolean bAnyAdditions = !bOrigAnyPresent || !bOrigPresent ||
                        !bChgAnyPresent || !bRejAnyPresent || !bConflictAnyPresent;
                if (bAnyAdditions) {
                    if (!GitUtils.confirmDialog(GitUtils.class, "MSG_IGNORE_FILES_TITLE", "MSG_IGNORE_FILES")) { // NOI18N
                        tempFile.delete();
                        return;
                    }
                    if (tempFile != null && tempFile.isFile() && tempFile.canWrite() && gitIgnoreFile != null) {
                        gitIgnoreFile.delete();
                        tempFile.renameTo(gitIgnoreFile);
                    }
                } else {
                    tempFile.delete();
                }
            } catch (IOException ex) {
                // Ignore
            }
        }
    }
     */

    private static List<PathPattern> readIgnorePatterns(File gitIgnore) {
		Vector<PathPattern> patterns = new Vector<PathPattern>(5);
        Set<String> shPatterns;
        try {
            shPatterns = readIgnoreEntries(gitIgnore);
        } catch (IOException e) {
            // ignore invalid entries
            return patterns;
        }
        for (String shPattern : shPatterns) {
			PathPattern pattern = PathPattern.create(shPattern);
			if (pattern.isExclude())
				patterns.add(pattern);
			else
				patterns.add(0, pattern);
        }
		return patterns;
    }

    private static Set<String> readIgnoreEntries(File gitIgnore) throws IOException {
        Set<String> entries = new HashSet<String>(5);
        if (!gitIgnore.canRead()) {
            return entries;
        }
        String line;
        BufferedReader r = null;
        try {
            r = new BufferedReader(new FileReader(gitIgnore));
            while ((line = r.readLine()) != null) {
                if (line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                entries.add(line);
            }
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                }
            }
        }
        return entries;
    }

    /*
    private static String computePatternToIgnore(File directory, File file) {
        String name = file.getAbsolutePath().substring(directory.getAbsolutePath().length() + 1);
        return name.replace(' ', '?').replace(File.separatorChar, '/');
    }

    private static void writeIgnoreEntries(File directory, Set<String> entries) throws IOException {
        File gitIgnore = new File(directory, FILENAME_GITIGNORE);
        FileObject fo = FileUtil.toFileObject(gitIgnore);

        if (entries.size() == 0) {
            if (fo != null) {
                fo.delete();
            }
            return;
        }

        if (fo == null || !fo.isValid()) {
            fo = FileUtil.toFileObject(directory);
            fo = fo.createData(FILENAME_GITIGNORE);
        }
        FileLock lock = fo.lock();
        PrintWriter w = null;
        try {
            w = new PrintWriter(fo.getOutputStream(lock));
            for (String entry : entries) {
                w.println(entry);
            }
        } finally {
            lock.releaseLock();
            if (w != null) {
                w.close();
            }
            resetIgnorePatterns(directory);
        }
    }
     */

    /**
     * addIgnored - Add the specified files to the .gitignore file in the
     * specified repository.
     *
     * @param directory for repository for .gitignore file
     * @param files an array of Files to be added
     */
    /*
    public static void addIgnored(File directory, File[] files) throws IOException {
        Set<String> entries = readIgnoreEntries(directory);
        for (File file : files) {
            String patterntoIgnore = computePatternToIgnore(directory, file);
            entries.add(patterntoIgnore);
        }
        writeIgnoreEntries(directory, entries);
    }
    */

    /**
     * removeIgnored - Remove the specified files from the .gitignore file in
     * the specified repository.
     *
     * @param directory for repository for .gitignore file
     * @param files an array of Files to be removed
     */
    /*
    public static void removeIgnored(File directory, File[] files) throws IOException {
        Set<String> entries = readIgnoreEntries(directory);
        for (File file : files) {
            String patterntoIgnore = computePatternToIgnore(directory, file);
            entries.remove(patterntoIgnore);
        }
        writeIgnoreEntries(directory, entries);
    }
    */
}
