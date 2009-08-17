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

import junit.framework.TestCase;

public class PathPatternTest extends TestCase {

    private static final String[] patternsForModuleC = {
        "module.c",
        "*.c",
        "module.*",
        "*.?"
    };

    public void testNoWildCard() {
        String reject = CharacterSequence.WILDCARD_CHARS + "!/";
        for (String string : CharacterSequence.create(1, 256, reject)) {
            pattern(string).
                    matchesFile(string).
                    matchesDir(string);
            pattern(string + "/").
                    doesNotMatchFile(string).
                    matchesDir(string);
        }
    }

    public void testSingleGlob() {
        for (String string : CharacterSequence.create(0, 256, "!/")) {
            pattern(string).
                    matchesFile(string).
                    matchesDir(string);
            pattern(string + "/").
                    doesNotMatchFile(string).
                    matchesDir(string);
        }
    }

    public void testPatternsWithNoSlashes() {
        for (String pattern : patternsForModuleC) {
            pattern(pattern).from("/.gitignore").
                    matchesFile("module.c").
                    matchesFile("path/to/nested/module.c");
        }
        pattern("File.java").
                doesNotMatchFile("aFile.java").
                doesNotMatchFile("path/to/nested/someFile.java");
        pattern("*.java").
                doesNotMatchFile("File.javaX").
                doesNotMatchFile("path/to/nested/File.javaX");
        pattern("tmp.*").
                doesNotMatchFile("mytmp.file").
                doesNotMatchFile("path/to/nested/tmp2.javac");
        pattern("[?]").matchesFile("?");
        pattern("[*]").matchesFile("*");
        pattern(" a ").matchesFile(" a ");
        pattern(" ? file with * spaces ").matchesFile(" a file with many spaces ");
    }

    public void testMatchFromRoot() {
        for (String pattern : patternsForModuleC) {
            pattern("/" + pattern).from("/.gitignore").
                    matchesFile("module.c").
                    doesNotMatchFile("subdir/module.c");
        }
        for (String pattern : patternsForModuleC) {
            pattern("/" + pattern).from("/dir/.gitignore").
                    matchesFile("dir/module.c").
                    doesNotMatchFile("nested/dir/module.c").
                    doesNotMatchFile("dir/subdir/module.c");
        }
        pattern("/sha1*.?").
                matchesFile("sha1sum.c").
                matchesFile("sha1.c").
                doesNotMatchFile("mozilla/sha1.c");
        pattern("/*.?").
                matchesFile("sha1sum.c").
                doesNotMatchFile("mozilla/sha1.c");
    }

    public void testMatchSubPath() {
        pattern("path/to/File.java").
                matchesFile("path/to/File.java").
                doesNotMatchFile("nested/path/to/File.java");
        pattern("path/to/*.java").
                matchesFile("path/to/File.java").
                doesNotMatchFile("path/to/nested/File.java");
        pattern("/to/File.java").
                matchesFile("to/File.java").
                doesNotMatchFile("path/to/File.java");
        pattern("/to/*.java").
                matchesFile("to/File.java").
                doesNotMatchFile("to/nested/File.java");
    }

    public void testMatchDirectory() {
        pattern("path/").
                matchesDir("path").
                matchesDir("subdir/path").
                doesNotMatchFile("path/to/File.java").
                doesNotMatchFile("path");
        pattern("path/subdir/").
                matchesDir("path/subdir").
                doesNotMatchDir("path").
                doesNotMatchFile("path/subdir/File.java").
                doesNotMatchFile("some/path/subdir/File.java");
    }

    private TestBuilder pattern(String pattern) {
        return new TestBuilder(pattern);
    }

    private static class TestBuilder {

        private final PathPattern pattern;
        private String relativePatternDir = "";

        private TestBuilder(String pattern) {
            assertNotSame('!', pattern.charAt(0));
            this.pattern = PathPattern.create(pattern);
        }

        private TestBuilder from(String excludeOrigin) {
            assertTrue(excludeOrigin.startsWith("/"));
            StringBuilder builder = new StringBuilder(excludeOrigin);
            if (excludeOrigin.equals("/.git/info/exclude")) {
                builder.setLength(0);
            } else if (excludeOrigin.endsWith("/.gitignore")) {
                builder.setLength(builder.lastIndexOf("/"));
                if (builder.length() > 0)
                    builder.deleteCharAt(0);
            } else {
                fail("Unknown exclude origin: " + excludeOrigin);
            }
            relativePatternDir = builder.toString();
            return this;
        }

        private TestBuilder matchesDir(String filePath) {
            return match(true, filePath, true);
        }

        private TestBuilder doesNotMatchDir(String filePath) {
            return match(false, filePath, true);
        }

        private TestBuilder matchesFile(String filePath) {
            return match(true, filePath, false);
        }

        private TestBuilder doesNotMatchFile(String filePath) {
            return match(false, filePath, false);
        }

        private TestBuilder match(boolean expected, String filePath, boolean isDir) {
            if (expected != pattern.matches(filePath, isDir, relativePatternDir)) {
                fail(pattern + " does not match: " + filePath);
            }
            return this;
        }
    }
}
