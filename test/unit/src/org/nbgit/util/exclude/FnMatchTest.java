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

public class FnMatchTest extends TestCase {

    public void testCompareNoFlags() {
        assertMatches("file.exe", "file.exe");
        assertMatches(".gitignore", ".gitignore");
        assertMatches("Abc.123", "Abc.123");
        assertNotMatches("ABc.123", "Abc.123");
    }

    public void testEscapeNoFlags() {
        assertMatches("\\[", "[");
        assertMatches("\\#", "#");
        assertMatches("\\?", "?");
        assertNotMatches("\\?", "a");
        assertMatches("\\*", "*");
        assertNotMatches("\\*", "a");
    }

    public void testSingleGlobNoFlags() {
        assertMatches("?.git", "a.git");
        assertMatches("?i?", "git");
        assertMatches("?/target", "a/target");
        assertMatches("tmp.?", "tmp.X");
        assertNotMatches("?a?", "git");
        assertNotMatches("tmp.?", "tmp.XX");
    }

    public void testMultiGlobNoFlags() {
        assertMatches("*", "123ax*a.x?[.a]a.x23");
        assertMatches("*.exe", "file.exe");
        assertMatches("***********.exe", "file.exe");
        assertMatches("***********.exe", ".exe");
        assertMatches("*e*", "file.exe");
        assertMatches("*/target", "/path/to/target");
        assertMatches("tmp.*", "tmp.XXXXXX");
        assertNotMatches("*a*", "file.exe");
        assertNotMatches("tmp.*", "tmp_XXXXXX");
    }

    public void testRangeNoFlags() {
        String[] simpleRangeNames = {
            "file.ad", "file.bd", "file.cd"
        };
        for (String name : simpleRangeNames) {
            assertMatches("file.[abc]d", name);
        }
        for (String name : simpleRangeNames) {
            assertMatches("file.[a-c]d", name);
        }
        for (String name : simpleRangeNames) {
            assertNotMatches("file.[!a-c]d", name);
        }
        assertNotMatches("file.[abc]d", "file.dd");
    }

    public void testRangeSpecialCharNoFlags() {
        assertMatches("file.ext[abc-]", "file.ext-");
        assertMatches("file.ext[-abc]", "file.ext-");
        assertMatches("file.ext[abc-]", "file.ext-");
        assertMatches("file.ext[?*]", "file.ext?");
        assertMatches("file.ext[?*]", "file.ext*");
        assertMatches("file.ext[*?]", "file.ext?");
        assertMatches("file.ext[*?]", "file.ext*");
        assertMatches("file.ext[!*?]", "file.ext]");
        // TODO: Match of ]
    }

    public void testCombinationsNoFlags() {
        assertMatches("ba[rz].*", "bar.123");
        assertMatches("ba[rz].*", "baz.233");
    }

    private void assertMatches(String pattern, String input) {
        assertTrue(FnMatch.fnmatch(pattern, input));
    }

    private void assertNotMatches(String pattern, String input) {
        assertFalse(FnMatch.fnmatch(pattern, input));
    }
}
