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

    public void testPatternsWithNoSlashes() {
        pattern("*.java").
                matchesFile("File.java").
                matchesFile("path/to/nested/File.java");
        pattern("[?]").matchesFile("?");
        pattern("[*]").matchesFile("*");
        pattern(" a ").matchesFile(" a ");
        pattern(" ? file with * spaces ").matchesFile(" a file with many spaces ");
    }

    public void testPatternsFromRoot() {
        pattern("/*.java").
                matchesFile("File.java").
                doesNotMatchFile("nested/File.java");
        pattern("/*.?").
                matchesFile("sha1sum.c").
                doesNotMatchFile("mozilla/sha1.a");
    }

    public void testMatchFromSubRoot() {
        pattern("/*.java").from("/dir/.gitignore").
                matchesFile("dir/File.java").
                doesNotMatchFile("different/dir/File.java").
                doesNotMatchFile("dir/subdir/File.java");
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
                matchesFile("path/to/File.java").
                matchesDir("path/to").
                matchesDir("path").
                doesNotMatchFile("path");
        pattern("path/subdir/").
                matchesFile("path/subdir/File.java").
                matchesDir("path/subdir").
                doesNotMatchDir("path").
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
            StringBuilder builder = new StringBuilder(excludeOrigin).deleteCharAt(0);
            if (excludeOrigin.equals("/.git/info/exclude")) {
                builder.setLength(0);
            } else if (excludeOrigin.endsWith("/.gitignore")) {
                builder.setLength(builder.lastIndexOf("/") + 1);
            } else {
                fail("Unknown exclude origin: " + excludeOrigin);
            }
            relativePatternDir = builder.toString();
            return this;
        }

        private TestBuilder matchesDir(String filePath) {
            assertTrue(pattern.matches(filePath, true, relativePatternDir));
            return this;
        }

        private TestBuilder doesNotMatchDir(String filePath) {
            assertFalse(pattern.matches(filePath, true, relativePatternDir));
            return this;
        }

        private TestBuilder matchesFile(String filePath) {
            assertTrue(pattern.matches(filePath, false, relativePatternDir));
            return this;
        }

        private TestBuilder doesNotMatchFile(String filePath) {
            assertFalse(pattern.matches(filePath, false, relativePatternDir));
            return this;
        }
    }
}
