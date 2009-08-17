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
package org.nbgit.util.exclude;

import java.io.File;
import java.util.Vector;
import org.nbgit.util.MonitoredFileMap;
import org.spearce.jgit.lib.Repository;

class ExcludeCache {

    private final MonitoredFileMap<PathPatternList> map = MonitoredFileMap.create();
    private final Repository repo;

    public static ExcludeCache create(Repository repository) {
        return new ExcludeCache(repository);
    }

    private ExcludeCache(Repository repository) {
        this.repo = repository;
    }

    /**
     * Check if a file is excluded.
     *
     * The algorithm for matching the path ROOT/dir1/dir2/file is:
     *
     * (1) ROOT: []dir1
     *
     * (2) DIR1: [dir1/]dir2
     *     ROOT: []dir1/dir2
     *
     * (3) DIR2: [dir1/dir2/]file
     *     DIR1: [dir1/]dir2/file
     *     ROOT: []dir1/dir2/file
     *
     * @param file to query for exclusion.
     * @return true if the file is excluded by a path pattern.
     */
    public boolean isExcluded(File file) {
        File curDir = repo.getWorkDir();
        String filePath = ExcludeUtils.getRelativePath(curDir, file);
        Vector<PathPatternList> stack = new Vector<PathPatternList>(10);
        StringBuilder pathBuilder = new StringBuilder(filePath.length());

        addDefaultPatterns(stack, curDir);

        while (true) {
            int length = pathBuilder.length();
            int offset = filePath.indexOf('/', pathBuilder.length() + 1);
            boolean isDirectory = true;

            if (offset == -1) {
                offset = filePath.length();
                isDirectory = file.isDirectory();
            }

            pathBuilder.insert(pathBuilder.length(), filePath, pathBuilder.length(), offset);
            String currentPath = pathBuilder.toString();

            for (int i = stack.size() - 1; i >= 0; i--) {
                PathPatternList patterns = stack.get(i);
                PathPattern pattern = patterns.findPattern(currentPath, isDirectory);
                if (pattern != null) {
                    return pattern.isExclude();
                }
            }

            if (!isDirectory || pathBuilder.length() >= filePath.length()) {
                return false;
            }

            curDir = new File(curDir, pathBuilder.substring(length, offset));
            PathPatternList patterns = getDirectoryPattern(curDir, currentPath);
            if (patterns != null) {
                stack.add(patterns);
            }
        }
    }

    private void addDefaultPatterns(Vector<PathPatternList> stack, File curDir) {
        PathPatternList patterns = getUserPatternList();
        if (patterns != null)
            stack.add(patterns);
        patterns = getRepoPatternList();
        if (patterns != null)
            stack.add(patterns);
        patterns = getDirectoryPattern(curDir, "");
        if (patterns != null)
            stack.add(patterns);
    }

    private PathPatternList getDirectoryPattern(File dir, String dirPath) {
        return getPatternList(new File(dir, ".gitignore"), dirPath);
    }

    private PathPatternList getRepoPatternList() {
        File gitInfoExclude = new File(new File(repo.getDirectory(), "info"), "exclude");
        return getPatternList(gitInfoExclude, "");
    }

    private PathPatternList getUserPatternList() {
        String userExcludePath = repo.getConfig().getString("core", null, "excludesfile");
        if (userExcludePath == null || userExcludePath.length() == 0)
            return null;
        File excludeFile = new File(userExcludePath);
        return getPatternList(excludeFile, "");
    }

    private PathPatternList getPatternList(File file, String basePath) {
        PathPatternList list = map.get(file);
        if (list == null) {
            list = ExcludeUtils.readExcludeFile(file, basePath);
            if (list == null)
                return null;
            map.put(file, list);
        }
        return list;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        builder.append(repo.getDirectory());
        builder.append(";");
        builder.append(repo.getWorkDir());
        builder.append("]");
        return builder.toString();
    }
}
