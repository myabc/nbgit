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

public abstract class PathPattern {

    protected final String pattern;
    private final boolean exclude;
    protected final boolean matchFileName;
    private final boolean matchDir;

    public static PathPattern create(String pattern) {
        if (hasNoWildcards(pattern, 0, pattern.length())) {
            return new NoWildcardPathPattern(pattern);
        } else {
            return new WildcardPathPattern(pattern);
        }
    }

    public static boolean isWildcard(char c) {
        return c == '*' || c == '[' || c == '?' || c == '\\';
    }

    private static boolean hasNoWildcards(String pattern, int from, int to) {
        for (int i = from; i < to; i++) {
            if (isWildcard(pattern.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private PathPattern(String pattern) {
        StringBuilder builder = new StringBuilder(pattern);
        this.exclude = pattern.charAt(0) != '!';
        if (!exclude) {
            builder.deleteCharAt(0);
        }
        this.matchDir = pattern.endsWith("/");
        if (matchDir) {
            builder.deleteCharAt(builder.length() - 1);
        }

        if (builder.charAt(0) == '/') {
            builder.deleteCharAt(0);
            this.matchFileName = false;
        } else {
            this.matchFileName = builder.indexOf("/") == -1;
        }
        this.pattern = builder.toString();
    }

    public boolean isExclude() {
        return exclude;
    }

    public boolean matches(String path, boolean isDirectory, String basePath) {
        if (matchDir && !isDirectory) {
            return matchesParentDirectory(path, basePath);
        }
        if (matchFileName && matchesFileName(path)) {
            return true;
        }
        if (basePath.length() > 0 && !path.startsWith(basePath)) {
            return false;
        }
        if (matchesPathName(path, basePath)) {
            return true;
        }
        return matchesParentDirectory(path, basePath);
    }

    private boolean matchesParentDirectory(String path, String basePath) {
        int end = path.lastIndexOf('/');
        if (end == -1 || end <= basePath.length()) {
            return false;
        }
        return matches(path.substring(0, end), true, basePath);
    }

    protected abstract boolean matchesFileName(String path);

    protected abstract boolean matchesPathName(String path, String basePath);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(exclude ? "EX" : "IN").append("CLUDE(");
        builder.append(pattern);
        if (!matchFileName) {
            builder.append(", path");
        }
        if (matchDir) {
            builder.append(", dirs");
        }
        return builder.append(")").toString();
    }

    private static class NoWildcardPathPattern extends PathPattern {

        private NoWildcardPathPattern(String pattern) {
            super(pattern);
        }

        @Override
        protected boolean matchesFileName(String path) {
            if (path.length() > pattern.length() &&
                    path.charAt(path.length() - pattern.length() - 1) != '/') {
                return false;
            }
            return path.endsWith(pattern);
        }

        @Override
        protected boolean matchesPathName(String path, String basePath) {
            return path.length() - basePath.length() == pattern.length() &&
                    path.startsWith(pattern, basePath.length());
        }
    }

    private static class WildcardPathPattern extends PathPattern {

        private final int regionFrom, regionLength;

        private WildcardPathPattern(String patternString) {
            super(patternString);
            int from = 0, to = 0;
            if (matchFileName) {
                if (pattern.startsWith("*")) {
                    from = 1;
                    to = pattern.length();
                } else if (pattern.endsWith("*")) {
                    from = 0;
                    to = pattern.length() - 1;
                }
                if (hasNoWildcards(pattern, from, to)) {
                    this.regionFrom = from;
                    this.regionLength = to - from;
                    return;
                }
            }
            this.regionFrom = 0;
            this.regionLength = 0;
        }

        @Override
        protected boolean matchesFileName(String path) {
            int from = path.lastIndexOf('/') + 1;
            if (from >= path.length()) {
                return false;
            }
            if (regionLength > 0) {
                int offset = regionFrom > 0
                        ? path.length() - regionLength : from;
                return offset >= from &&
                        path.regionMatches(offset, pattern, regionFrom, regionLength);
            }
            return FnMatch.fnmatch(pattern, path, from);
        }

        @Override
        protected boolean matchesPathName(String path, String basePath) {
            return FnMatch.fnmatch(pattern, path, basePath.length(), FnMatch.Flag.PATHNAME);
        }
    }
}
